package com.minds.rgpd.business.dtos;

import com.minds.rgpd.business.enums.ViolationStatut;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vue résumée d'une violation, pour la liste paginée : les colonnes affichées
 * dans le tableau du registre, sans les champs rédactionnels (conséquences,
 * mesures, commentaires) qui n'ont d'intérêt qu'au détail.
 */
public record ViolationPartielDTO(
        UUID identifiant,
        LocalDate dateViolation,
        String natureViolation,
        String donneesConcernees,
        Integer nombrePersonnesConcernees,
        Boolean risqueEleveDroitsLibertes,
        ViolationStatut statut
) {
}
