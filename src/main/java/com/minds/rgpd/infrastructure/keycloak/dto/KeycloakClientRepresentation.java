package com.minds.rgpd.infrastructure.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakClientRepresentation(
        String id,
        String clientId,
        Boolean enabled,
        Boolean publicClient
) {
}
