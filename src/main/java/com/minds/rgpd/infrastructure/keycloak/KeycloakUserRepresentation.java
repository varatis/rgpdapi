package com.minds.rgpd.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

/**
 * Représentation d'un utilisateur Keycloak telle que renvoyée par l'API
 * d'administration ({@code GET /admin/realms/{realm}/users}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {

    private UUID id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private boolean enabled;

    private List<String> realmRoles;

    private List<KeycloakGroupRepresentation> groups;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public List<KeycloakGroupRepresentation> getGroups() {
        return groups;
    }

    public void setGroups(List<KeycloakGroupRepresentation> groups) {
        this.groups = groups;
    }
}
