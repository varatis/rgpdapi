package com.minds.rgpd.business.dtos;

import java.time.LocalDate;

public record TraitementDTO(
        Integer id,

        String nom,

        String gestionnaire,

        String finalitePrincipale,

        ClientDTO client,

        Integer version,

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

        String dispositionsSecuriteDonnees,

        String emplacementNumerique,

        String hebergement,

        Integer dureeConservation,

        Boolean archivage,

        Integer dureeArchivage,

        String categoriesDestinataires,

        String raisonsTransfertDestinataires,

        Boolean transfertsHorsUE,

        String paysDestinataires,

        String commentaires
) {
}