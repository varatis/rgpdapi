package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
}
