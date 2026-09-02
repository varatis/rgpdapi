package com.minds.rgpd.business.dtos;

import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PUBLIC)
public record InfoFichierDTO(
        String nomFichier,
        LocalDateTime dateReception,
        LocalDateTime dateFinTraitement,
        String statusFichier,

        boolean confirmationRequise,

        ImportApercuDTO apercu,

        String version,

        Integer nombreTraitementsRemplaces,

        Integer nombreTraitementsImportes
) {
}
