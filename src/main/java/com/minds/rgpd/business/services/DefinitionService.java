package com.minds.rgpd.business.services;

import java.util.List;

public interface DefinitionService {

    /**
     * Valeurs déjà enregistrées pour le client, proposées en liste dans le
     * formulaire d'un traitement.
     *
     * @param type clé d'URL du référentiel : {@code sensibilite},
     *             {@code etude-impact} ou {@code liceite-traitement}
     * @throws IllegalArgumentException si le type n'est pas un référentiel proposé en liste
     */
    List<String> getValues(String clientNom, String type);
}
