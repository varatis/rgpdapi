package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Utilisateur de l'application, rattaché à un client et à un profil.
 */
public record UtilisateurDTO(
        @Schema(description = "Identifiant technique de l'utilisateur (UUID).")
        UUID id,

        @Schema(description = "Prénom de l'utilisateur.")
        String prenom,

        @Schema(description = "Nom de l'utilisateur.")
        String nom,

        @Schema(description = "Adresse e-mail de l'utilisateur ; unique, sert d'identifiant de connexion.")
        String email,

        @Schema(description = "Mot de passe de l'utilisateur (stocké hashé ; en écriture uniquement).")
        String password,

        @Schema(description = "Fonction de l'utilisateur au sein de l'organisme client (ex. : Responsable IT, DPO).")
        String fonction,

        @Schema(description = "Profil (droits) de l'utilisateur.")
        ProfilDTO profil,

        @Schema(description = "Client auquel l'utilisateur est rattaché.")
        ClientDTO client
) {
}
