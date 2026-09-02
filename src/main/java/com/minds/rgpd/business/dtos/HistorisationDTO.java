package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entrée d'historique (RG1) : une modification datée, motivée et attribuée.
 * Utilisée aussi bien pour l'historique d'un traitement que pour celui du registre.
 */
@Builder
public record HistorisationDTO(
        Integer id,
        LocalDateTime date,
        String motif,
        String auteur
) {
}
