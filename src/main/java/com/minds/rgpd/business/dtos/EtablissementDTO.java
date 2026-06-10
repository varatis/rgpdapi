package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record EtablissementDTO(
        UUID id,
        String nom,
        ClientDTO client
) {
}
