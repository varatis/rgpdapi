package com.minds.rgpd.infrastructure.keycloak;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Client REST minimal vers l'API d'administration de Keycloak
 * (https://www.keycloak.org/docs-api/latest/rest-api/).
 *
 * <p>Le jeton d'accès est obtenu via le flux {@code client_credentials} puis
 * transmis dans l'en-tête {@code Authorization} ; il est renouvelé à l'approche
 * de son expiration, ou après une réponse 401.</p>
 */
public class KeycloakAdminClient {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminClient.class);

    /** Garde-fou contre les boucles de pagination si le serveur renvoyait toujours des pages pleines. */
    private static final int MAX_PAGES = 100;

    private final KeycloakProperties properties;

    private final RestTemplate restTemplate;

    private String accessToken;

    private Instant tokenExpiration;

    public KeycloakAdminClient(KeycloakProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    private String tokenUrl() {
        return properties.getBaseUrl() + "/realms/" + properties.getRealm() + "/protocol/openid-connect/token";
    }

    private String usersUrl() {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users";
    }

    private String groupsUrl() {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups";
    }

    private String clientUrl() {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/clients";
    }

    private String clientRolesUrl(String clientUuid) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/clients/" + clientUuid + "/roles";
    }

    private String roleUsersUrl(String clientUuid, String roleName) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm()
                + "/clients/" + clientUuid + "/roles/" + encode(roleName) + "/users";
    }

    private String groupUrl(UUID groupId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups/" + groupId;
    }

    private String groupChildrenUrl(UUID groupId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups/" + groupId + "/children";
    }

    private String groupMembersUrl(UUID groupId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups/" + groupId + "/members";
    }

    private String userUrl(UUID userId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId;
    }

    private String userGroupsUrl(UUID userId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId + "/groups";
    }

    private String userGroupUrl(UUID userId, UUID groupId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId + "/groups/" + groupId;
    }

    private String userRoleMappingsUrl(UUID userId, String clientUuid) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm()
                + "/users/" + userId + "/role-mappings/clients/" + clientUuid;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    private boolean isTokenValid() {
        return accessToken != null && Instant.now().isBefore(tokenExpiration.minusSeconds(30L));
    }

    private void ensureToken() {
        if (!isTokenValid()) {
            fetchToken();
        }
    }

    private void fetchToken() {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", properties.getAdminClientId());
        body.add("client_secret", properties.getAdminClientSecret());

        RequestEntity<LinkedMultiValueMap<String, String>> request = RequestEntity
                .post(URI.create(tokenUrl()))
                .body(body);

        ResponseEntity<KeycloakTokenResponse> response;
        try {
            response = restTemplate.exchange(request, KeycloakTokenResponse.class);
        } catch (RestClientException e) {
            throw new IdentityProviderException("obtention du jeton client_credentials (client_id="
                    + properties.getAdminClientId() + ") : " + e.getMessage()
                    + " — vérifiez KEYCLOAK_ADMIN_CLIENT_SECRET, le type confidentiel du client"
                    + " et l'activation de son compte de service");
        }
        KeycloakTokenResponse token = response.getBody();
        if (token == null || token.getAccessToken() == null) {
            throw new IdentityProviderException("réponse invalide du point de jeton Keycloak"
                    + " (client_id=" + properties.getAdminClientId() + ")");
        }
        this.accessToken = token.getAccessToken();
        this.tokenExpiration = Instant.now().plusSeconds(token.getExpiresIn());
    }

    private RequestEntity<?> buildRequestEntity(HttpMethod method, String url, Object body) {
        RequestEntity.BodyBuilder builder = RequestEntity.method(method, URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return body != null ? builder.body(body) : builder.build();
    }

    private <T> ResponseEntity<T> execute(HttpMethod method, String url, Object body, Class<T> responseType) {
        ensureToken();
        try {
            return restTemplate.exchange(buildRequestEntity(method, url, body), responseType);
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                fetchToken();
                try {
                    return restTemplate.exchange(buildRequestEntity(method, url, body), responseType);
                } catch (RestClientException apresRenouvellement) {
                    throw echecKeycloak(method, url, apresRenouvellement);
                }
            }
            throw echecKeycloak(method, url, e);
        }
    }

    /**
     * Variante pour les types génériques (listes) : le {@link ParameterizedTypeReference}
     * préserve le type d'élément à l'exécution, ce que {@code List.class} ne permet pas
     * (Jackson produirait des {@code LinkedHashMap} au lieu des représentations typées).
     */
    private <T> ResponseEntity<T> execute(HttpMethod method, String url, Object body,
                                          ParameterizedTypeReference<T> responseType) {
        ensureToken();
        try {
            return restTemplate.exchange(buildRequestEntity(method, url, body), responseType);
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                fetchToken();
                try {
                    return restTemplate.exchange(buildRequestEntity(method, url, body), responseType);
                } catch (RestClientException apresRenouvellement) {
                    throw echecKeycloak(method, url, apresRenouvellement);
                }
            }
            throw echecKeycloak(method, url, e);
        }
    }

    /**
     * Les lectures tolèrent l'échec (résultat vide, journalisé en WARN) ; toute
     * écriture refusée remonte en revanche une erreur explicite — le cas le
     * plus courant étant un compte de service sans rôles realm-management.
     */
    private RuntimeException echecKeycloak(HttpMethod method, String url, RestClientException e) {
        if (HttpMethod.GET.equals(method)) {
            return e;
        }
        return new IdentityProviderException("appel Keycloak en échec [" + method + " " + url + "] : "
                + e.getMessage() + indiceRealmManagement(e), e);
    }

    /**
     * L'indice « rôles realm-management » n'a de sens que pour un refus
     * d'autorisation (401/403) : un 409 (doublon d'identifiant) ou un 400
     * (payload invalide) n'a rien à voir avec les permissions du compte de
     * service — l'afficher systématiquement égare le diagnostic.
     */
    private String indiceRealmManagement(RestClientException e) {
        if (e instanceof RestClientResponseException reponse
                && (HttpStatus.UNAUTHORIZED.equals(reponse.getStatusCode())
                    || HttpStatus.FORBIDDEN.equals(reponse.getStatusCode()))) {
            return " — vérifiez les rôles realm-management du compte de service"
                    + " (view-users, manage-users, view-clients, view-groups, manage-groups…)";
        }
        return "";
    }

    private <T> Optional<T> performRequest(HttpMethod method, String url, Object body, Class<T> responseType) {
        try {
            return Optional.ofNullable(execute(method, url, body, responseType).getBody());
        } catch (RestClientException e) {
            logger.warn("Appel Keycloak en échec [{} {}] : {}", method, url, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> Optional<List<T>> getList(String url, ParameterizedTypeReference<List<T>> responseType) {
        try {
            return Optional.ofNullable(execute(HttpMethod.GET, url, null, responseType).getBody());
        } catch (RestClientException e) {
            logger.warn("Appel Keycloak en échec [GET {}] : {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String url, Class<T> responseType) {
        return performRequest(HttpMethod.GET, url, null, responseType);
    }

    /**
     * Parcourt une ressource paginée (paramètres {@code first}/{@code max})
     * jusqu'à épuisement.
     */
    private <T> List<T> getAllPages(String url, ParameterizedTypeReference<List<T>> responseType) {
        List<T> result = new ArrayList<>();
        int pageSize = properties.getPageSize();
        int first = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            String separator = url.contains("?") ? "&" : "?";
            List<T> chunk = getList(url + separator + "first=" + first + "&max=" + pageSize, responseType)
                    .orElse(List.of());
            result.addAll(chunk);
            if (chunk.size() < pageSize) {
                break;
            }
            first += chunk.size();
        }
        return result;
    }

    public List<KeycloakUserRepresentation> getUsers() {
        return getAllPages(usersUrl(), new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
        });
    }

    /**
     * Titulaires d'un rôle client donné (sens inverse de l'affectation :
     * une requête par rôle plutôt qu'une par utilisateur).
     */
    public List<KeycloakUserRepresentation> getUsersByClientRole(String clientUuid, String roleName) {
        return getAllPages(roleUsersUrl(clientUuid, roleName),
                new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
                });
    }

    public Optional<KeycloakUserRepresentation> getUserById(UUID userId) {
        return get(userUrl(userId), KeycloakUserRepresentation.class);
    }

    public Optional<KeycloakUserRepresentation> getUserByEmail(String email) {
        String url = usersUrl() + "?email=" + encode(email) + "&exact=true&max=1";
        return getList(url, new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
                }).flatMap(users ->
                users.isEmpty() ? Optional.empty() : Optional.of(users.getFirst()));
    }

    /**
     * Crée l'utilisateur ; un 409 de Keycloak (identifiant déjà pris) est
     * traduit en doublon explicite (409 côté API) plutôt qu'en échec
     * technique 502 avec un indice « rôles » sans rapport.
     */
    public UUID createUser(Map<String, Object> representation) {
        try {
            ResponseEntity<Void> response = execute(HttpMethod.POST, usersUrl(), representation, Void.class);
            return uuidFromLocation(response, "de l'utilisateur");
        } catch (IdentityProviderException e) {
            if (e.getCause() instanceof RestClientResponseException reponse
                    && HttpStatus.CONFLICT.equals(reponse.getStatusCode())) {
                throw new DuplicateResourceException("Utilisateur", "identifiant", representation.get("username"));
            }
            throw e;
        }
    }

    public void deleteUser(UUID userId) {
        execute(HttpMethod.DELETE, userUrl(userId), null, Void.class);
    }

    /**
     * Définit (ou réinitialise) le mot de passe d'un utilisateur ; posé en
     * temporaire, Keycloak exige son remplacement à la prochaine connexion.
     */
    public void resetPassword(UUID userId, String motDePasse, boolean temporaire) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "password");
        body.put("value", motDePasse);
        body.put("temporary", temporaire);
        execute(HttpMethod.PUT, userUrl(userId) + "/reset-password", body, Void.class);
    }

    public void updateUser(UUID userId, Map<String, Object> representation) {
        execute(HttpMethod.PUT, userUrl(userId), representation, Void.class);
    }

    public List<KeycloakGroupRepresentation> getUserGroups(UUID userId) {
        return getList(userGroupsUrl(userId), new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {
        }).orElse(List.of());
    }

    public List<KeycloakRoleRepresentation> getClientRoles(String clientUuid) {
        return getList(clientRolesUrl(clientUuid), new ParameterizedTypeReference<List<KeycloakRoleRepresentation>>() {
        }).orElse(List.of());
    }

    /**
     * Rôles client effectivement affectés à un utilisateur
     * ({@code GET /users/{id}/role-mappings/clients/{uuid}}).
     */
    public List<KeycloakRoleRepresentation> getUserClientRoles(UUID userId, String clientUuid) {
        return getList(userRoleMappingsUrl(userId, clientUuid),
                new ParameterizedTypeReference<List<KeycloakRoleRepresentation>>() {
                }).orElse(List.of());
    }

    public void assignClientRoles(UUID userId, String clientUuid, List<String> roleNames) {
        execute(HttpMethod.POST, userRoleMappingsUrl(userId, clientUuid),
                roleRepresentations(clientUuid, roleNames), Void.class);
    }

    public void removeClientRoles(UUID userId, String clientUuid, List<String> roleNames) {
        execute(HttpMethod.DELETE, userRoleMappingsUrl(userId, clientUuid),
                roleRepresentations(clientUuid, roleNames), Void.class);
    }

    /**
     * Keycloak attend, pour l'affectation comme pour le retrait de rôles client,
     * un tableau JSON de représentations {@code {"id": ..., "name": ...}}.
     */
    private List<Map<String, Object>> roleRepresentations(String clientUuid, List<String> roleNames) {
        List<Map<String, Object>> representations = new ArrayList<>();
        for (KeycloakRoleRepresentation role : getClientRoles(clientUuid)) {
            if (roleNames.contains(role.getName())) {
                Map<String, Object> representation = new LinkedHashMap<>();
                representation.put("id", role.getId());
                representation.put("name", role.getName());
                representations.add(representation);
            }
        }
        return representations;
    }

    /**
     * Sous-groupes directs d'un groupe ({@code GET /groups/{id}/children}) :
     * endpoint des Keycloak RÉCENTS, où {@code GET /groups} n'imbrique plus
     * les sous-groupes. Inexistant sur les versions anciennes (405) — à
     * n'utiliser qu'en repli de la hiérarchie.
     */
    public List<KeycloakGroupRepresentation> getGroupChildren(UUID groupId) {
        return getList(groupChildrenUrl(groupId), new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {
        }).orElse(List.of());
    }

    /**
     * Hiérarchie des groupes racine ({@code GET /groups}) : les sous-groupes
     * sont imbriqués dans {@code subGroups}. C'est la seule lecture
     * d'enfants disponible sur toutes les versions de Keycloak —
     * {@code GET /groups/{id}/children} n'existe que sur les versions
     * récentes (405 Method Not Allowed sur les anciennes).
     */
    public List<KeycloakGroupRepresentation> getGroupHierarchy() {
        return getList(groupsUrl(), new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {
        }).orElse(List.of());
    }

    /**
     * Crée un groupe ; en tant que sous-groupe du groupe parent identifié par
     * {@code parentId} lorsqu'il est renseigné, sinon comme groupe racine.
     * Un 409 (sous-groupe homonyme déjà présent, parfois sous une casse ou
     * avec des espaces différents) est traduit en doublon explicite.
     */
    public UUID createGroup(String name, UUID parentId) {
        String url = parentId != null ? groupsUrl() + "/" + parentId + "/children" : groupsUrl();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        try {
            ResponseEntity<Void> response = execute(HttpMethod.POST, url, body, Void.class);
            return uuidFromLocation(response, "du groupe");
        } catch (IdentityProviderException e) {
            if (e.getCause() instanceof RestClientResponseException reponse
                    && HttpStatus.CONFLICT.equals(reponse.getStatusCode())) {
                throw new DuplicateResourceException("Groupe de client", "nom", name);
            }
            throw e;
        }
    }

    public void deleteGroup(UUID groupId) {
        execute(HttpMethod.DELETE, groupUrl(groupId), null, Void.class);
    }

    public void addUserToGroup(UUID userId, UUID groupId) {
        execute(HttpMethod.PUT, userGroupUrl(userId, groupId), null, Void.class);
    }

    public void removeUserFromGroup(UUID userId, UUID groupId) {
        execute(HttpMethod.DELETE, userGroupUrl(userId, groupId), null, Void.class);
    }

    public List<KeycloakUserRepresentation> getGroupMembers(UUID groupId) {
        return getAllPages(groupMembersUrl(groupId),
                new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
                });
    }

    public List<KeycloakClientRepresentation> getClientsByResourceId(String resourceId) {
        return getList(clientUrl() + "?clientId=" + encode(resourceId),
                new ParameterizedTypeReference<List<KeycloakClientRepresentation>>() {
                }).orElse(List.of());
    }

    public String getClientUuidByResourceId(String resourceId) {
        List<KeycloakClientRepresentation> clients = getClientsByResourceId(resourceId);
        return clients.isEmpty() ? null : clients.getFirst().getId().toString();
    }

    /**
     * Keycloak ne renvoie pas de corps lors d'une création (201 Created) :
     * l'identifiant de la ressource créée est extrait de l'en-tête {@code Location}.
     */
    private UUID uuidFromLocation(ResponseEntity<?> response, String libelle) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new RestClientException("Création " + libelle + " : en-tête Location absent de la réponse Keycloak");
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        return UUID.fromString(id);
    }
}
