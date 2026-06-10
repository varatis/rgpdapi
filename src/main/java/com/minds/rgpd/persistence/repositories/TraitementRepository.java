package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraitementRepository extends JpaRepository<Traitement, UUID> {

    Traitement findByIdFonctionnel(int id);

    @Query("SELECT MAX(idFonctionnel) FROM Traitement")
    Optional<Integer> findMaxIdFonctionnel();

    Optional<Traitement> findByIdFonctionnelAndNomAndClient(int idFonctionnel, String nom, Client client);
}
