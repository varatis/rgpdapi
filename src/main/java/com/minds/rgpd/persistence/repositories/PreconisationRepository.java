package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PreconisationRepository extends JpaRepository<Preconisation, UUID>, JpaSpecificationExecutor<Preconisation> {

    @Query("""
            SELECT p FROM Preconisation p
            WHERE p.client = :client
              AND p.libelle = :libelle
              AND ((:traitement IS NULL AND p.traitement IS NULL) OR p.traitement = :traitement)
            """)
    List<Preconisation> findDuplicates(
            @Param("client") Client client,
            @Param("libelle") String libelle,
            @Param("traitement") Traitement traitement
    );
}