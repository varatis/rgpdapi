package com.minds.rgpd.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Représentation d'un groupe Keycloak telle que renvoyée par l'API
 * d'administration ({@code GET /admin/realms/{realm}/groups}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakGroupRepresentation {

    private UUID id;

    private String name;

    private String path;

    private List<KeycloakGroupRepresentation> subGroups;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<KeycloakGroupRepresentation> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<KeycloakGroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }
}
