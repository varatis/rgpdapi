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

        DefinitionDTO finalitePrincipale,

        ClientDTO client,

        Integer version,

        @NotNull
        LocalDate dateIdentification,

        LocalDate dateMiseAJour,

        String historiqueModifications,

        /**
         * RG1 / CA4 : motif de la modification, renseigné par l'utilisateur à la
         * mise à jour ; alimente la table historisation_traitement. Non exploité
         * à la création ni restitué en lecture.
         */
        String motifModification,

        String dataProtectionOfficer,

        ResponsableTraitementDTO responsableTraitement,

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

        DefinitionDTO sensibilite,

        DefinitionDTO etudeImpact,

        String canauxCollecteDonnees,

        DefinitionDTO licieteTraitement,

        Boolean recoursTraitementAutomatises,

        String emplacementPhysique,

        String dispositionsSecuriteDonneesPhysique,

        String emplacementNumerique,

        String dispositionsSecuriteDonneesNumerique,

        String hebergement,

        DureeDTO dureeConservation,

        Boolean archivage,

        DureeDTO dureeArchivage,

        String categoriesDestinataires,

        String raisonsTransfertDestinataires,

        Boolean transfertsHorsUE,

        String paysDestinataires,

        String commentaires
) {
}