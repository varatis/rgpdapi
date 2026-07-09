package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EtablissementRepository extends JpaRepository<Etablissement, UUID> {

    Optional<Etablissement> findByNom(String nom);

    Optional<Etablissement> findByNomAndClient(String nom, Client client);
}
