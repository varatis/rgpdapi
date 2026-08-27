package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.HistorisationTraitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorisationTraitementRepository extends JpaRepository<HistorisationTraitement, Integer> {

    /**
     * Historique d'un traitement, du plus récent au plus ancien.
     */
    List<HistorisationTraitement> findByTraitementIdentifiantOrderByDateDesc(UUID traitementIdentifiant);
}
