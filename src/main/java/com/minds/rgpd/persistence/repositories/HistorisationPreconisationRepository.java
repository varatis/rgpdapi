package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.HistorisationPreconisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistorisationPreconisationRepository extends JpaRepository<HistorisationPreconisation, Integer> {

    /**
     * Historique d'une préconisation, du plus récent au plus ancien.
     */
    List<HistorisationPreconisation> findByPreconisationIdentifiantOrderByDateDesc(UUID preconisationIdentifiant);
}
