package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.IdentityProviderException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakAdminClientTest {

    private static final String BASE = "https://sso.minds.k8s/auth";
    private static final String ADMIN = BASE + "/admin/realms/minds-rgpd";
    private static final String URL_JETON = BASE + "/realms/minds-rgpd/protocol/openid-connect/token";
    private static final UUID ID_ALICE = UUID.fromString("d6dfd117-8047-4a9a-afca-f5268a38bfcf");
    private static final ParameterizedTypeReference<Map<String, Object>> CARTE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTE_CARTE =
            new ParameterizedTypeReference<>() {
            };

    private MockRestServiceServer serveur;
    private KeycloakAdminClient client;
    private KeycloakProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KeycloakProperties();
        properties.setBaseUrl(BASE);
        properties.setRealm("minds-rgpd");
        properties.setAdminClientId("minds-saas-rgpd-admin");
        properties.setAdminClientSecret("un-secret");

        RestClient.Builder constructeur = RestClient.builder();
        serveur = MockRestServiceServer.bindTo(constructeur).build();
        client = new KeycloakAdminClient(properties, constructeur.build());
    }

    @Test
    void leJetonAdministrateurEstDemandePuisPorteParChaqueAppel() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jeton-abc"))
                .andRespond(withSuccess("{\"id\":\"%s\",\"username\":\"alice\"}".formatted(ID_ALICE),
                        MediaType.APPLICATION_JSON));

        Optional<Map<String, Object>> utilisateur = client.lire("/users/" + ID_ALICE, CARTE);

        assertThat(utilisateur).isPresent();
        assertThat(utilisateur.get()).containsEntry("username", "alice");
        serveur.verify();
    }

    @Test
    void leJetonEstMisEnCacheEntreDeuxAppels() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users?first=0&max=1")).andRespond(withSuccess("[{\"id\":\"u1\"}]",
                MediaType.APPLICATION_JSON));
        serveur.expect(requestTo(ADMIN + "/users?first=1&max=1")).andRespond(withSuccess("[]",
                MediaType.APPLICATION_JSON));

        assertThat(client.lire("/users?first=0&max=1", LISTE_CARTE)).hasValueSatisfying(ligne ->
                assertThat(ligne).singleElement().satisfies(carte -> assertThat(carte).hasSize(1)));
        assertThat(client.lire("/users?first=1&max=1", LISTE_CARTE)).hasValueSatisfying(vide -> assertThat(vide).isEmpty());

        serveur.verify();
    }

    @Test
    void unIntrouvableEnLectureDevientUnVide() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.lire("/users/" + ID_ALICE, CARTE)).isEmpty();
    }

    @Test
    void uneErreurServeurEstRemonteeAvecLeStatut() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.lire("/users/" + ID_ALICE, CARTE))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("GET")
                .extracting(e -> ((IdentityProviderException) e).getStatut())
                .isEqualTo(500);
    }

    @Test
    void laCreationExtraitLidentifiantDuHeaderLocation() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(Matchers.containsString("alice@exemple.fr")))
                .andRespond(withCreatedEntity(URI.create(ADMIN + "/users/" + ID_ALICE)));

        Optional<String> id = client.creer("/users", Map.of("email", "alice@exemple.fr"));

        assertThat(id).contains(ID_ALICE.toString());
    }

    @Test
    void laSuppressionDuneRessourceDejaPartieEstIgnoree() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        client.supprimer("/users/" + ID_ALICE);
        serveur.verify();
    }

    @Test
    void leJetonEstRenouvelePuisLaRequeteRelanceeSurUn401() {
        expecterJeton();
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        serveur.expect(requestTo(ADMIN + "/users/" + ID_ALICE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jeton-abc"))
                .andRespond(withSuccess("{\"id\":\"%s\"}".formatted(ID_ALICE), MediaType.APPLICATION_JSON));

        assertThat(client.lire("/users/" + ID_ALICE, CARTE)).isPresent();
        serveur.verify();
    }

    @Test
    void unEchecDauthentificationEstTraduitEnErreurDeSynchronisation() {
        serveur.expect(requestTo(URL_JETON))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(Matchers.containsString("client_id=minds-saas-rgpd-admin")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.lire("/users/" + ID_ALICE, CARTE))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("minds-saas-rgpd-admin");
    }

    @Test
    void lesNomsDeGroupesSontEncodesDansLesChemins() {
        assertThat(KeycloakAdminClient.encoder("La breteche")).isEqualTo("La%20breteche");
        assertThat(KeycloakAdminClient.encoder("a+b")).isEqualTo("a%2Bb");
        assertThat(KeycloakAdminClient.encoder("Été")).isEqualTo("%C3%89t%C3%A9");
    }

    @Test
    void lesUrlsAcceptentUneBarreObliqueFinale() {
        properties.setBaseUrl(BASE + "/");

        assertThat(properties.urlAdmin()).isEqualTo(ADMIN);
        assertThat(properties.urlJeton()).isEqualTo(URL_JETON);
    }

    private void expecterJeton() {
        serveur.expect(manyTimes(), requestTo(URL_JETON))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess(
                        "{\"access_token\":\"jeton-abc\",\"expires_in\":300,\"token_type\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON));
    }
}
