package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, UUID> {

}
