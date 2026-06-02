package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record TraitementDTO(

        @NotNull
        Integer id,

        @NotNull
        String nom,

        List<EtablissementDTO> etablissements,

        String gestionnaire,

        String finalitePrincipale,

        ClientDTO client,

        Integer version,

        @NotNull
        LocalDate dateIdentification,

        LocalDate dateMiseAJour,

        String historiqueModifications,

        String dataProtectionOfficer,

        String responsableTraitement,

        String gestionnaireMiseEnOeuvre,

        String sousFinalites,

        String categoriesPersonnesConcernees,

        String donneesIdentification,

        String donneesConnexion,

        String donneesLocalisation,

        String donneesComportementViePerso,

        String donneesEconomiquesFinancieres,

        String donneesProfessionnelles,

        String categoriesParticulieresDonnees,

        String sensibilite,

        String etudeImpact,

        String canauxCollecteDonnees,

        String licieteTraitement,

        Boolean recoursTraitementAutomatises,

        String emplacementPhysique,

        String dispositionsSecuriteDonneesPhysique,

        String emplacementNumerique,

        String dispositionsSecuriteDonneesNumerique,

        String hebergement,

        String dureeConservation,

        Boolean archivage,

        String dureeArchivage,

        String categoriesDestinataires,

        String raisonsTransfertDestinataires,

        Boolean transfertsHorsUE,

        String paysDestinataires,

        String commentaires
) {
}