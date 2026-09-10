package com.minds.rgpd.infrastructure.keycloak;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedMultiValueMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedHashMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class KeycloakAdminClient {

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

    private String clientRoleMappingsUrl(String clientUuid) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/clients/" + clientUuid + "/role-mappings";
    }

    private String roleUsersUrl(String clientUuid, String roleName) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/clients/" + clientUuid + "/roles/" + roleName + "/users";
    }

    private String groupByNameUrl(String name) {
        return groupsUrl() + "?search=" + encode(name) + "&exact=true";
    }

    private String groupMembersUrl(String groupId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups/" + groupId + "/members";
    }

    private String userUrl(UUID userId) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId;
    }

    private String userRoleMappingsUrl(UUID userId, String clientUuid) {
        return properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId + "/role-mappings/clients/" + clientUuid;
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

        RequestEntity<LinkedMultiValueMap<String, String>> request = RequestEntity.post()
                .url(tokenUrl())
                .body(body)
                .build();

        ResponseEntity<KeycloakTokenResponse> response = restTemplate.exchange(request, KeycloakTokenResponse.class);
        this.accessToken = response.getBody().getAccessToken();
        this.tokenExpiration = Instant.now().plusSeconds(response.getBody().getExpiresIn());
    }

    private RequestEntity<?> buildRequestEntity(HttpMethod method, String url, Object body) {
        if (body == null) {
            return RequestEntity.method(method, URI.create(url)).build();
        }
        return RequestEntity.method(method, URI.create(url)).body(body).build();
    }

    private <T> Optional<T> performRequest(HttpMethod method, String url, Object body, Class<T> responseType) {
        ensureToken();
        RequestEntity<?> request = buildRequestEntity(method, url, body);
        try {
            ResponseEntity<T> response = restTemplate.exchange(request, responseType);
            return Optional.of(response.getBody());
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                fetchToken();
                request = buildRequestEntity(method, url, body);
                ResponseEntity<T> response = restTemplate.exchange(request, responseType);
                return Optional.of(response.getBody());
            }
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String url, Class<T> responseType) {
        return performRequest(HttpMethod.GET, url, null, responseType);
    }

    public <T> Optional<List<T>> getList(String url, Class<?> componentType) {
        return get(url, (Class) List.class).map(l -> (List<T>) l);
    }

    public Optional<KeycloakUserRepresentation> getUserById(UUID userId) {
        String url = userUrl(userId);
        return get(url, KeycloakUserRepresentation.class);
    }

    public Optional<KeycloakUserRepresentation> getUserByEmail(String email) {
        String url = usersUrl() + "?email=" + encode(email) + "&exact=true&max=1";
        return get(url, KeycloakUserRepresentation.class);
    }

    public UUID createUser(Map<String, Object> representation) {
        RequestEntity<Map<String, Object>> request = RequestEntity.post()
                .url(usersUrl())
                .body(representation)
                .build();
        ResponseEntity<KeycloakUserRepresentation> response = restTemplate.exchange(request, KeycloakUserRepresentation.class);
        return response.getBody().getId();
    }

    public void deleteUser(UUID userId) {
        String url = userUrl(userId);
        restTemplate.delete(URI.create(url));
    }

    public void updateUser(UUID userId, Map<String, Object> representation) {
        String url = userUrl(userId);
        RequestEntity<Map<String, Object>> request = RequestEntity.put()
                .url(url)
                .body(representation)
                .build();
        restTemplate.exchange(request, Void.class);
    }

    public List<KeycloakRoleRepresentation> getClientRoles(String clientUuid) {
        String url = clientRoleMappingsUrl(clientUuid);
        return getList(url, KeycloakRoleRepresentation.class).orElse(List.of());
    }

    public Set<String> getClientRoleNames(String clientUuid) {
        List<KeycloakRoleRepresentation> roles = getClientRoles(clientUuid);
        Set<String> names = java.util.Set.of();
        for (KeycloakRoleRepresentation role : roles) {
            names = java.util.Set.copyOf(java.util.stream.Stream.of(role.getName()).collect(java.util.collectors.toSet()));
        }
        return names;
    }

    public void assignClientRoles(UUID userId, String clientUuid, List<String> roleNames) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("roles", roleNames);
        String url = userRoleMappingsUrl(userId, clientUuid);
        RequestEntity<Map<String, Object>> request = RequestEntity.post()
                .url(url)
                .body(body)
                .build();
        restTemplate.exchange(request, Void.class);
    }

    public void removeClientRoles(UUID userId, String clientUuid, List<String> roleNames) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("roles", roleNames);
        String url = userRoleMappingsUrl(userId, clientUuid);
        RequestEntity<Map<String, Object>> request = RequestEntity.delete()
                .url(url)
                .body(body)
                .build();
        restTemplate.exchange(request, Void.class);
    }

    public Optional<KeycloakGroupRepresentation> getGroupByName(String name) {
        String url = groupByNameUrl(name);
        return get(url, KeycloakGroupRepresentation.class);
    }

    public UUID createGroup(String name, String parentPath) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        if (parentPath != null) {
            body.put("path", parentPath);
        }
        RequestEntity<Map<String, Object>> request = RequestEntity.post()
                .url(groupsUrl())
                .body(body)
                .build();
        ResponseEntity<KeycloakGroupRepresentation> response = restTemplate.exchange(request, KeycloakGroupRepresentation.class);
        return response.getBody().getId();
    }

    public void deleteGroup(UUID groupId) {
        String url = properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/groups/" + groupId;
        restTemplate.delete(URI.create(url));
    }

    public void addUserToGroup(UUID userId, UUID groupId) {
        String url = properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId + "/groups/" + groupId;
        restTemplate.put(URI.create(url), Void.class);
    }

    public void removeUserFromGroup(UUID userId, UUID groupId) {
        String url = properties.getBaseUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + userId + "/groups/" + groupId;
        restTemplate.delete(URI.create(url));
    }

    public List<KeycloakUserRepresentation> getGroupMembers(UUID groupId) {
        String url = groupMembersUrl(groupId);
        return getList(url, KeycloakUserRepresentation.class).orElse(List.of());
    }

    public List<KeycloakClientRepresentation> getClientsByResourceId(String resourceId) {
        String url = clientUrl() + "?clientId=" + resourceId;
        return getList(url, KeycloakClientRepresentation.class).orElse(List.of());
    }

    public String getClientUuidByResourceId(String resourceId) {
        List<KeycloakClientRepresentation> clients = getClientsByResourceId(resourceId);
        if (clients.isEmpty()) {
            return null;
        }
        return clients.get(0).getId();
    }

    public List<KeycloakRoleRepresentation> getRoleByName(String clientUuid, String roleName) {
        String url = roleUsersUrl(clientUuid, roleName);
        return getList(url, KeycloakRoleRepresentation.class).orElse(List.of());
    }
}