package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HistorisationDTO(
        Integer id,
        LocalDateTime date,
        String motif,
        String auteur
) {
}
