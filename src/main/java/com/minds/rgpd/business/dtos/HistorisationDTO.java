package com.minds.rgpd.business.dtos;

import java.time.LocalDateTime;

/**
 * Entrée d'historisation d'un traitement ou d'une préconisation (RG1) :
 * date de la modification et motif saisi par l'utilisateur (CA4).
 */
public record HistorisationDTO(
        LocalDateTime date,
        String motif
) {
}
