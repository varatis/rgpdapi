package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ImportApercuDTO(
        String nomFichier,
        String clientNom,
        boolean fichierValide,
        String messageErreur,
        String versionActuelle,
        LocalDate dateVersionActuelle,
        String versionFichier,
        boolean remplacementDonnees,
        long nombreTraitementsExistants,
        long nombrePreconisationsExistantes,
        long nombreViolationsExistantes,
        String avertissement,
        String urlExportPrealable
) {
}
