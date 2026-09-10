package com.minds.rgpd.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakUserRepresentation(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        Boolean enabled,
        List<KeycloakGroupRepresentation> groups
) {

    public boolean actif() {
        return enabled == null || enabled;
    }
}
