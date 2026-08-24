package com.minds.rgpd.business.dtos;

import lombok.Builder;

@Builder
public record DureeDTO(
    Integer id,
    boolean estArchivage,
    String valeur
) {
}
