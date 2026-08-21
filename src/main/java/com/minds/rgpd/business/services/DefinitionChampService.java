package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.DefinitionChampDTO;

import java.util.List;

public interface DefinitionChampService {

    /**
     * Retourne les définitions métier des champs du registre pour un client,
     * dans l'ordre de l'onglet FR_Définitions.
     *
     * @param clientNom nom du client propriétaire du fichier registre
     * @param edition   édition du registre ; si nulle ou vide, toutes les éditions
     *                  importées sont retournées (dans l'ordre de l'onglet)
     */
    List<DefinitionChampDTO> getDefinitions(String clientNom, String edition);
}
