package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Conséquences d'un import, calculées <em>avant</em> son exécution (RG3).
 * <p>
 * L'import remplace l'intégralité des traitements du client (RG2) : l'interface
 * s'appuie sur cet aperçu pour afficher la modale d'avertissement, proposer un
 * export préalable, puis rappeler l'import avec confirmation.
 */
@Builder
public record ImportApercuDTO(
        String nomFichier,
        /** Client déduit du nom de fichier, {@code null} si celui-ci est invalide. */
        String clientNom,
        /** Le fichier est exploitable : nom conforme et client connu. */
        boolean fichierValide,
        /** Message d'erreur lorsque {@link #fichierValide()} est faux. */
        String messageErreur,
        /** Version du registre actuellement enregistrée pour le client. */
        String versionActuelle,
        LocalDate dateVersionActuelle,
        /** Version portée par le fichier importé (RG4). */
        String versionFichier,
        /** Vrai si l'import va effectivement écraser des données existantes. */
        boolean remplacementDonnees,
        long nombreTraitementsExistants,
        long nombrePreconisationsExistantes,
        long nombreViolationsExistantes,
        /** Avertissement prêt à afficher dans la modale de confirmation. */
        String avertissement,
        /** URL d'export du registre courant, à proposer avant remplacement. */
        String urlExportPrealable
) {
}
