package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResponsableTraitementRepository extends JpaRepository<ResponsableTraitement, Integer> {

    /**
     * Recherche un responsable de traitement deja enregistre pour ce client.
     * L'unicite du couple est garantie par une contrainte en base.
     */
    Optional<ResponsableTraitement> findByClientAndValeur(Client client, String valeur);
}
