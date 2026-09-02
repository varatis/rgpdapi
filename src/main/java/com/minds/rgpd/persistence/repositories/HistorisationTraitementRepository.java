package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.HistorisationTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorisationTraitementRepository extends JpaRepository<HistorisationTraitement, Integer> {

    List<HistorisationTraitement> findByTraitementOrderByDateDesc(Traitement traitement);

    List<HistorisationTraitement> findByTraitement_IdFonctionnelOrderByDateDesc(Integer idFonctionnel);
}
