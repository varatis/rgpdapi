package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraitementRepository extends JpaRepository<Traitement, Integer> {
}
