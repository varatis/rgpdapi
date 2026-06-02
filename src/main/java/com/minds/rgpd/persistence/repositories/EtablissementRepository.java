package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtablissementRepository extends JpaRepository<Etablissement, Integer> {

    Optional<Etablissement> findByNom(String nom);
}
