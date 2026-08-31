package com.minds.rgpd.business.dtos;

import com.minds.rgpd.business.enums.ViolationStatut;

import java.time.LocalDate;

/**
 * Critères de recherche du registre des violations. Les bornes de date et de
 * nombre de personnes concernées sont incluses et indépendantes : n'en renseigner
 * qu'une seule filtre d'un seul côté.
 */
public record ViolationFilterCriteria(
        String natureViolation,
        String donneesConcernees,
        Boolean risqueEleveDroitsLibertes,
        ViolationStatut statut,
        LocalDate dateViolationDebut,
        LocalDate dateViolationFin,
        Integer nombrePersonnesConcerneesMin,
        Integer nombrePersonnesConcerneesMax
) {
    public static ViolationFilterCriteria empty() {
        return new ViolationFilterCriteria(null, null, null, null, null, null, null, null);
    }
}
