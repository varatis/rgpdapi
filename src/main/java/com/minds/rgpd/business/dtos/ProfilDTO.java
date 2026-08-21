package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Profil fonctionnel d'un utilisateur, déterminant ses droits dans l'application
 * (ex. : ADMIN, USER, DPO).
 */
public record ProfilDTO(
        @Schema(description = "Identifiant technique du profil (UUID).")
        UUID id,

        @Schema(description = "Code unique du profil (ex. : ADMIN, USER, DPO).")
        String code,

        @Schema(description = "Description du profil et de son périmètre de droits.")
        String description
) {
}
