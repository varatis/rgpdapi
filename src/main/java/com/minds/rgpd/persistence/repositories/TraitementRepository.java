package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraitementRepository extends JpaRepository<Traitement, UUID>, JpaSpecificationExecutor<Traitement> {

    Traitement findByIdFonctionnel(int id);

    @Query("SELECT MAX(idFonctionnel) FROM Traitement")
    Optional<Integer> findMaxIdFonctionnel();

    // Retourner List au lieu de Optional
    // Raison : Il peut y avoir des doublons en base (données historiques).
    // Optional lance NonUniqueResultException si > 1 résultat.
    @Query("""
            SELECT t FROM Traitement t 
            WHERE t.nom = :nom 
              AND t.client = :client 
              AND t.gestionnaireMiseEnOeuvre = :gestionnaireMiseEnOeuvre
              AND t.finalitePrincipale = :finalitePrincipale
              AND t.dateIdentification = :dateIdentification
            """)
    List<Traitement> findByAllBusinessFields(
            @Param("nom") String nom,
            @Param("client") Client client,
            @Param("gestionnaireMiseEnOeuvre") String gestionnaireMiseEnOeuvre,
            @Param("finalitePrincipale") String finalitePrincipale,
            @Param("dateIdentification") LocalDate dateIdentification
    );

    @Query("""
            SELECT t FROM Traitement t 
            WHERE t.idFonctionnel IN (
                SELECT t2.idFonctionnel FROM Traitement t2 
                GROUP BY t2.idFonctionnel 
                HAVING COUNT(t2.idFonctionnel) > 1
            )
            """)
    List<Traitement> findDuplicateTraitements();
}
