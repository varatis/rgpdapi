package com.minds.rgpd.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Représentation d'un client Keycloak telle que renvoyée par l'API
 * d'administration ({@code GET /admin/realms/{realm}/clients?clientId=...}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakClientRepresentation {

    private UUID id;

    private String clientId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
