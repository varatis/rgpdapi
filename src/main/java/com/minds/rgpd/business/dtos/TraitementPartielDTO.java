package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Vue partielle d'un traitement (identification), destinée aux listes.
 * Voir {@link TraitementDTO} et {@code docs/mapping-bdd-registre.md} pour les définitions complètes.
 */
public record TraitementPartielDTO(
        @Schema(description = "Identifiant technique unique du traitement (UUID généré par l'application).")
        UUID identifiant,

        @Schema(description = "Numéro unique d'identification du traitement (colonne « ID » du registre) ; clé métier issue du fichier registre source.")
        Integer idFonctionnel,

        @Schema(description = "Nom du traitement : suffisamment explicite pour que l'on comprenne de manière macro ce que fait le traitement.")
        String nom,

        @Schema(description = "Fonction du représentant de l'entité ou du service en charge de mettre en œuvre le traitement.")
        String gestionnaireMiseEnOeuvre,

        @Schema(description = "Objectif final spécifique, explicite et légitime pour lequel le traitement a lieu.")
        String finalitePrincipale
) {
}
