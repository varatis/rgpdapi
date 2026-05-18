package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtablissementRepository extends JpaRepository<Etablissement,Integer> {
}
