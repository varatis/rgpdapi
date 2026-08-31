package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, UUID>, JpaSpecificationExecutor<Violation> {

    // Retourner List au lieu de Optional
    // Raison : Il peut y avoir des doublons en base (données historiques).
    // Optional lance NonUniqueResultException si > 1 résultat.
    @Query("""
            SELECT v FROM Violation v
            WHERE v.client = :client
              AND v.dateViolation = :dateViolation
              AND v.natureViolation = :natureViolation
              AND v.donneesConcernees = :donneesConcernees
            """)
    List<Violation> findByAllBusinessFields(
            @Param("client") Client client,
            @Param("dateViolation") LocalDate dateViolation,
            @Param("natureViolation") String natureViolation,
            @Param("donneesConcernees") String donneesConcernees
    );
}
