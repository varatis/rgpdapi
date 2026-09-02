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

        String commentaires,

        Integer impactTraitement,

        Integer detournementFinalite,

        Integer scoreDetournementFinalite,

        Integer collecteDcpInappropriees,

        Integer scoreCollecteDcpInappropriees,

        Integer conservationExcessiveDcp,

        Integer scoreConservationExcessiveDcp,

        Integer securisationInsuffisanteDcp,

        Integer scoreSecurisationInsuffisanteDcp,

        Integer vicesConsentement,

        Integer scoreVicesConsentement,

        Integer manqueTransparence,

        Integer scoreManqueTransparence,

        Integer incapaciteExerciceDroits,

        Integer scoreIncapaciteExerciceDroits,

        Integer transfertTiersMalEncadre,

        Integer scoreTransfertTiersMalEncadre,

        Integer transfertHorsUeAbusif,

        Integer scoreTransfertHorsUeAbusif,

        Integer defautPreuve,

        Integer scoreDefautPreuve,

        Integer scoreGlobal,

        String commentairesAnalyse,

        Integer expositionTraitement,

        Boolean critereEvaluationScoring,

        Boolean critereDecisionAutomatique,

        Boolean critereSurveillanceSystematique,

        Boolean critereCollecteDonneesSensibles,

        Boolean critereCollecteLargeEchelle,

        Boolean critereCroisementDonnees,

        Boolean criterePersonnesVulnerables,

        Boolean critereUsageInnovant,

        Boolean critereExclusionBeneficeDroit,

        List<HistorisationDTO> historiqueTraitement
) {
}