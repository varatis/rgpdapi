package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Definition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefinitionRepository extends JpaRepository<Definition, Integer> {

    /**
     * Recherche une definition deja enregistree pour ce client.
     * Le champ "type" est le discriminateur porte par les sous-classes.
     */
    Optional<Definition> findByClientAndTypeAndValeur(Client client, String type, String valeur);

    /** Valeurs distinctes d'un type de definition pour ce client, triees alphabetiquement. */
    @Query("""
            select distinct d.valeur from Definition d
            where d.client.nom = :clientNom and d.type = :type
            order by d.valeur
            """)
    List<String> findValuesByClientNomAndType(@Param("clientNom") String clientNom, @Param("type") String type);
}
