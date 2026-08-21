package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Suivi du traitement d'un fichier registre Excel importé
 * (« <client>_<etablissement>_Registre RGPD_ed<édition>.xlsx »).
 */
@Builder(access = AccessLevel.PUBLIC)
public record InfoFichierDTO(
        @Schema(description = "Nom du fichier registre importé (ex. : « La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx ») ; le client et l'édition en sont extraits.")
        String nomFichier,

        @Schema(description = "Date et heure de réception du fichier par l'API.")
        LocalDateTime dateReception,

        @Schema(description = "Date et heure de fin de traitement (import) du fichier.")
        LocalDateTime dateFinTraitement,

        @Schema(description = "Statut du traitement du fichier (ex. : en cours, succès, erreur).")
        String statusFichier
) {
}
