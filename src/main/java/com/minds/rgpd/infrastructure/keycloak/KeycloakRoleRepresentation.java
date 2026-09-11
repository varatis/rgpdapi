package com.minds.rgpd.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Représentation d'un rôle client Keycloak telle que renvoyée par l'API
 * d'administration ({@code GET /admin/realms/{realm}/clients/{uuid}/roles}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakRoleRepresentation {

    private String id;

    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
