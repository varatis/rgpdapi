package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record TraitementDTO(

        @NotNull
        UUID identifiant,

        @NotNull
        Integer idFonctionnel,

        @NotNull
        String nom,

        String donneesConcernees,

        List<EtablissementDTO> etablissements,

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