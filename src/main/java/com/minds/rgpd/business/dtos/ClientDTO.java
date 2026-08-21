package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

/**
 * Organisme client de la plateforme (tenant) : toutes les données métier
 * (utilisateurs, établissements, traitements) sont rattachées à un client.
 */
@Builder
public record ClientDTO(
        @Schema(description = "Identifiant technique unique du client (UUID généré par l'application).")
        UUID id,

        @Schema(description = "Nom (raison sociale ou nom d'usage) de l'organisme client.")
        String nom,

        @Schema(description = "Statut du client (ex. : actif, inactif).")
        String statut
) {
}
