package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Utilisateur de l'application, rattaché à un client et à un profil.
 */
@Builder
@Data
@Entity
@Table(name = "UTILISATEUR")
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    /**
     * Identifiant technique de l'utilisateur (UUID).
     */
    @Id
    UUID id;

    /**
     * Prénom de l'utilisateur.
     */
    @Column(name = "prenom")
    @NotNull
    String prenom;

    /**
     * Nom de l'utilisateur.
     */
    @Column(name = "nom")
    @NotNull
    String nom;

    /**
     * Adresse e-mail de l'utilisateur ; unique, sert d'identifiant de connexion.
     */
    @Column(name = "email")
    @NotNull
    String email;

    /**
     * Mot de passe de l'utilisateur (stocké hashé).
     */
    @Column(name = "password")
    @NotNull
    String password;

    /**
     * Fonction de l'utilisateur au sein de l'organisme client (ex. : Responsable IT, DPO).
     */
    @Column(name = "fonction")
    String fonction;

    /**
     * Profil (droits) de l'utilisateur (suppression restreinte tant qu'un utilisateur y est rattaché).
     */
    @ManyToOne
    @JoinColumn(name = "id_profil")
    Profil profil;

    /**
     * Client auquel l'utilisateur est rattaché (suppression en cascade).
     */
    @ManyToOne
    @JoinColumn(name = "id_client")
    Client client;

}
