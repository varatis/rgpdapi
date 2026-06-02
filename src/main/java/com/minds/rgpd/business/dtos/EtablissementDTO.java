package com.minds.rgpd.business.dtos;

import lombok.Builder;

@Builder
public record EtablissementDTO(
        Integer id,
        String nom,
        ClientDTO client
) {
}
