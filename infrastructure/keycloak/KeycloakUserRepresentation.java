package com.minds.rgpd.infrastructure.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * Représentation d'un utilisateur Keycloak telle que renvoyée par l'API
 * d'administration ({@code GET /admin/realms/{realm}/users}).
 *
 * <p>Les listes et le détail d'un utilisateur ne portent ni rôles ni groupes :
 * ces informations s'obtiennent par des endpoints dédiés
 * ({@code /users/{id}/role-mappings/clients/{uuid}} et {@code /users/{id}/groups}).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakUserRepresentation {

    private UUID id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private boolean enabled;

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
}
