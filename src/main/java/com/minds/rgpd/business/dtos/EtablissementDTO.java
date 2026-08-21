package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.UUID;

/**
 * Établissement (site, service) d'un client ; un traitement du registre peut
 * concerner un ou plusieurs établissements.
 */
@Builder
public record EtablissementDTO(
        @Schema(description = "Identifiant technique de l'établissement (UUID).")
        UUID id,

        @Schema(description = "Nom de l'établissement, tel qu'indiqué dans la colonne « Etablissement(s) » du registre.")
        String nom,

        @Schema(description = "Client propriétaire de l'établissement.")
        ClientDTO client
) {
}
