package com.minds.rgpd.business.dtos;

public record EtablissementDTO(
        Integer id,
        String nom,
        ClientDTO client
) {
}
