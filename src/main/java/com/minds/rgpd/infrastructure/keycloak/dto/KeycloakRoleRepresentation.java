package com.minds.rgpd.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KeycloakRoleRepresentation(
        String id,
        String name,
        String description,
        Boolean composite,
        Boolean clientRole,
        String containerId
) {

    public KeycloakRoleRepresentation sansComposites() {
        return new KeycloakRoleRepresentation(id, name, description, false, clientRole, containerId);
    }
}
