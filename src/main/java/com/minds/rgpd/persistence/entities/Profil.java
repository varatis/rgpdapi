package com.minds.rgpd.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Profil fonctionnel d'un utilisateur, déterminant ses droits dans l'application
 * (ex. : ADMIN, USER, DPO).
 */
@Builder
@Data
@Entity
@Table(name = "PROFIL")
@NoArgsConstructor
@AllArgsConstructor
public class Profil {

    /**
     * Identifiant technique du profil (UUID).
     */
    @Id
    UUID id;

    /**
     * Code unique du profil (ex. : ADMIN, USER, DPO).
     */
    @Column(name = "code")
    String code;

    /**
     * Description du profil et de son périmètre de droits.
     */
    @Column(name = "description")
    String description;
}
