package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Definition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DefinitionRepository extends JpaRepository<Definition, Integer> {

    /**
     * Recherche une definition deja enregistree pour ce client.
     * Le champ "type" est le discriminateur porte par les sous-classes.
     */
    Optional<Definition> findByClientAndTypeAndValeur(Client client, String type, String valeur);
}
