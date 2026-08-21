package com.minds.rgpd.business.dtos;

import java.util.UUID;

/**
 * Définition métier d'un champ du registre, extraite de l'onglet « FR_Définitions »
 * du fichier Excel à l'import (voir {@code docs/mapping-bdd-registre.md}).
 *
 * @param tableCible   table du modèle correspondant au champ, si elle existe (nulle sinon)
 * @param colonneCible colonne du modèle correspondant au champ, si elle existe
 *                     (nulle pour les définitions sans correspondance ou portant sur une relation)
 * @param ordre        numéro de ligne du champ dans l'onglet FR_Définitions
 */
public record DefinitionChampDTO(
        UUID id,
        String edition,
        String section,
        String libelle,
        String definition,
        String tableCible,
        String colonneCible,
        Integer ordre
) {
}
