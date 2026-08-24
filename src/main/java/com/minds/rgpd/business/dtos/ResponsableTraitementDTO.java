package com.minds.rgpd.business.dtos;

import lombok.Builder;

@Builder
public record ResponsableTraitementDTO(
    Integer id,
    String valeur,
    String informationsComplementaires
) {
}
