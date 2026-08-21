package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Établissement (site, service) d'un client ; un traitement du registre peut
 * concerner un ou plusieurs établissements.
 */
@Builder
@Data
@Entity
@Table(name = "ETABLISSEMENT")
@NoArgsConstructor
@AllArgsConstructor
public class Etablissement {

    /**
     * Identifiant technique de l'établissement (UUID généré lors de l'import ou de la création).
     */
    @Id
    UUID id;

    /**
     * Nom de l'établissement, tel qu'indiqué dans la colonne « Etablissement(s) » du registre.
     */
    @Column(name = "nom")
    String nom;

    /**
     * Client propriétaire de l'établissement (suppression en cascade).
     */
    @ManyToOne
    @JoinColumn(name = "id_client")
    Client client;
}
