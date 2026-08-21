package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.DefinitionChamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DefinitionChampRepository extends JpaRepository<DefinitionChamp, UUID> {

    /**
     * Supprime les définitions d'un client pour une édition donnée.
     * Appelé avant chaque réimport de la même édition (remplacement complet).
     */
    long deleteByClientIdAndEdition(UUID clientId, String edition);

    List<DefinitionChamp> findByClientNomOrderByOrdreAsc(String clientNom);

    List<DefinitionChamp> findByClientNomAndEditionOrderByOrdreAsc(String clientNom, String edition);
}
