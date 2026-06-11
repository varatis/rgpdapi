package com.minds.rgpd.business.dtos;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PUBLIC)
public record InfoFichierDTO(
        String nomFichier,
        LocalDateTime dateReception,
        LocalDateTime dateFinTraitement,
        String statusFichier
) {
}
