package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClientDTO(
        UUID id,
        String nom,
        String statut
) {
}
