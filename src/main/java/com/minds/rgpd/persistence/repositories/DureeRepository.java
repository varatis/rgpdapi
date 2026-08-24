package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DureeRepository extends JpaRepository<Duree, Integer> {

    /**
     * Recherche une duree deja enregistree pour ce client.
     * Le champ "estArchivage" distingue la duree de conservation de la duree
     * d'archivage ; l'unicite du triplet est garantie par un index en base.
     */
    Optional<Duree> findByClientAndEstArchivageAndValeur(Client client, boolean estArchivage, String valeur);
}
