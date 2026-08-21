package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Organisme client de la plateforme (tenant) : toutes les données métier
 * (utilisateurs, établissements, traitements) sont rattachées à un client.
 */
@Builder
@Data
@Entity
@Table(name = "CLIENT")
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    /**
     * Identifiant technique unique du client (UUID généré par l'application).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    /**
     * Nom (raison sociale ou nom d'usage) de l'organisme client.
     */
    @Column(name = "nom")
    @NotNull
    String nom;

    /**
     * Statut du client (ex. : actif, inactif).
     */
    @Column(name = "statut")
    String statut;

}
