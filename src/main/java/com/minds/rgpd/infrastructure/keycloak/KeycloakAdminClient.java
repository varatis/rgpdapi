package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.IdentityProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class KeycloakAdminClient {

    private final KeycloakProperties properties;
    private final RestClient restClient;

    private volatile String jeton;
    private volatile Instant expirationJeton = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakProperties properties) {
        this(properties, construireRepos(properties));
    }

    public KeycloakAdminClient(KeycloakProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public static String encoder(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public <T> Optional<T> lire(String chemin, ParameterizedTypeReference<T> type) {
        return Optional.ofNullable(excuter(HttpMethod.GET, chemin, null, true,
                requete -> requete.retrieve().body(type)));
    }

    public Optional<String> creer(String chemin, Object corps) {
        ResponseEntity<Void> reponse = excuter(HttpMethod.POST, chemin, corps, false,
                KeycloakAdminClient::sansCorps);
        if (reponse == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reponse.getHeaders().getLocation())
                .map(URI::getPath)
                .map(KeycloakAdminClient::dernierSegment);
    }

    public void ajouter(String chemin, Object corps) {
        excuter(HttpMethod.POST, chemin, corps, false, KeycloakAdminClient::sansCorps);
    }

    public void modifier(String chemin, Object corps) {
        excuter(HttpMethod.PUT, chemin, corps, false, KeycloakAdminClient::sansCorps);
    }

    public void supprimer(String chemin) {
        excuter(HttpMethod.DELETE, chemin, null, true, KeycloakAdminClient::sansCorps);
    }

    public void supprimer(String chemin, Object corps) {
        excuter(HttpMethod.DELETE, chemin, corps, true, KeycloakAdminClient::sansCorps);
    }

    String jeton() {
        if (expirationJeton.isBefore(Instant.now())) {
            synchronized (this) {
                if (expirationJeton.isBefore(Instant.now())) {
                    rafraichirJeton();
                }
            }
        }
        return jeton;
    }

    private static ResponseEntity<Void> sansCorps(RestClient.RequestBodySpec requete) {
        return requete.retrieve().toBodilessEntity();
    }

    private <T> T excuter(HttpMethod methode, String chemin, Object corps, boolean tolererIntrouvable,
                          Function<RestClient.RequestBodySpec, T> operation) {
        return excuter(methode, chemin, corps, tolererIntrouvable, true, operation);
    }

    private <T> T excuter(HttpMethod methode, String chemin, Object corps, boolean tolererIntrouvable,
                          boolean peutRenouvelerJeton, Function<RestClient.RequestBodySpec, T> operation) {
        try {
            return operation.apply(requete(methode, chemin, corps));
        } catch (RestClientResponseException e) {
            if (peutRenouvelerJeton && e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                log.warn("Jeton Keycloak refusé, renouvellement puis relance de {} {}", methode, chemin);
                expirationJeton = Instant.EPOCH;
                return excuter(methode, chemin, corps, tolererIntrouvable, false, operation);
            }
            if (tolererIntrouvable && e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw new IdentityProviderException(methode.name(), e.getStatusCode().value(), messageErreur(e, chemin));
        } catch (ResourceAccessException e) {
            throw new IdentityProviderException(methode.name(),
                    "Keycloak injoignable (%s) : %s".formatted(properties.getBaseUrl(), e.getMessage()), e);
        }
    }

    private RestClient.RequestBodySpec requete(HttpMethod methode, String chemin, Object corps) {
        RestClient.RequestBodySpec requete = restClient.method(methode)
                .uri(URI.create(properties.urlAdmin() + chemin));
        if (corps != null) {
            requete = requete.contentType(MediaType.APPLICATION_JSON).body(corps);
        }
        return requete.header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton());
    }

    private String messageErreur(RestClientResponseException e, String chemin) {
        String corps = e.getResponseBodyAsString();
        return "%s : %s".formatted(chemin, corps == null || corps.isBlank() ? e.getStatusText() : corps);
    }

    private void rafraichirJeton() {
        MultiValueMap<String, String> corps = new LinkedMultiValueMap<>();
        corps.add("grant_type", "client_credentials");
        corps.add("client_id", properties.getAdminClientId());
        corps.add("client_secret", properties.getAdminClientSecret());

        try {
            KeycloakTokenRepresentation jetonRecu = restClient.post()
                    .uri(URI.create(properties.urlJeton()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(corps)
                    .retrieve()
                    .body(KeycloakTokenRepresentation.class);

            if (jetonRecu == null || jetonRecu.accessToken() == null || jetonRecu.accessToken().isBlank()) {
                throw new IdentityProviderException("AUTHENTIFICATION", HttpStatus.UNAUTHORIZED.value(),
                        "aucun access_token renvoyé par le realm " + properties.getRealm());
            }
            jeton = jetonRecu.accessToken();
            expirationJeton = Instant.now().plus(Duration.ofSeconds(Math.max(jetonRecu.dureeValidite() - 15, 5)));
            log.debug("Jeton admin Keycloak obtenu pour le client {}", properties.getAdminClientId());
        } catch (RestClientResponseException e) {
            throw new IdentityProviderException("AUTHENTIFICATION", e.getStatusCode().value(),
                    "authentification du client admin %s refusée : %s"
                            .formatted(properties.getAdminClientId(), e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new IdentityProviderException("AUTHENTIFICATION",
                    "Keycloak injoignable (%s) : %s".formatted(properties.getBaseUrl(), e.getMessage()), e);
        }
    }

    private static String dernierSegment(String chemin) {
        int index = chemin.lastIndexOf('/');
        return index < 0 ? chemin : chemin.substring(index + 1);
    }

    static RestClient construireRepos(KeycloakProperties properties) {
        return RestClient.builder().requestFactory(fabriqueRequetes(properties)).build();
    }

    private static SimpleClientHttpRequestFactory fabriqueRequetes(KeycloakProperties properties) {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String methode) throws IOException {
                super.prepareConnection(connection, methode);
                if (properties.isDesactiverVerificationSsl() && connection instanceof HttpsURLConnection securise) {
                    securise.setSSLSocketFactory(contexteTousCertificats().getSocketFactory());
                    securise.setHostnameVerifier((hote, session) -> true);
                }
            }
        };
        fabrique.setConnectTimeout(properties.getDelaiConnexion());
        fabrique.setReadTimeout(properties.getDelaiLecture());
        return fabrique;
    }

    private static SSLContext contexteTousCertificats() {
        try {
            TrustManager[] managers = {
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certificats, String type) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certificats, String type) {
                        }
                    }
            };
            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(null, managers, new java.security.SecureRandom());
            return contexte;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le contexte SSL", e);
        }
    }
}
