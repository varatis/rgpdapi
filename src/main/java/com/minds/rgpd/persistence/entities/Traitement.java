package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "traitement")
@NoArgsConstructor
@AllArgsConstructor
public class Traitement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID identifiant;

    @Column(name="id_fonctionnel")
    @NotNull
    Integer idFonctionnel;

    @Column(name = "nom")
    @NotNull
    String nom;

    @Column(name = "donnees_concernees")
    String donneesConcernees;

    @ManyToOne
    @JoinColumn(name = "finalite_principale_id")
    FinalitePrincipale finalitePrincipale;

    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    @Column(name = "version")
    Integer version;

    @Column(name = "date_identification")
    @NotNull
    LocalDate dateIdentification;

    @Column(name = "date_mise_a_jour")
    LocalDate dateMiseAJour;

    @OneToMany(mappedBy = "traitement", cascade = CascadeType.ALL)
    List<HistorisationTraitement> historiqueTraitement;

    @Column(name = "historique_modifications")
    String historiqueModifications;

    @Column(name = "data_protection_officer")
    String dataProtectionOfficer;

    @ManyToOne
    @JoinColumn(name = "responsable_traitement_id")
    ResponsableTraitement responsableTraitement;

    @Column(name = "gestionnaire_mise_en_oeuvre")
    String gestionnaireMiseEnOeuvre;

    @Column(name = "sous_finalites")
    String sousFinalites;

    @Column(name = "categories_personnes_concernees")
    String categoriesPersonnesConcernees;

    @Column(name = "donnees_identification")
    String donneesIdentification;

    @Column(name = "donnees_connexion")
    String donneesConnexion;

    @Column(name = "donnees_localisation")
    String donneesLocalisation;

    @Column(name = "donnees_comportement_vie_perso")
    String donneesComportementViePerso;

    @Column(name = "donnees_economiques_financieres")
    String donneesEconomiquesFinancieres;

    @Column(name = "donnees_professionnelles")
    String donneesProfessionnelles;

    @Column(name = "categories_particulieres_donnees")
    String categoriesParticulieresDonnees;

    @ManyToOne
    @JoinColumn(name = "sensibilite_id")
    Sensibilite sensibilite;

    @ManyToOne
    @JoinColumn(name = "etude_impact_id")
    EtudeImpact etudeImpact;

    @Column(name = "canaux_collecte_donnees")
    String canauxCollecteDonnees;

    @ManyToOne
    @JoinColumn(name = "liceite_traitement_id")
    LiceiteTraitement licieteTraitement;

    @Column(name = "recours_traitements_automatises")
    Boolean recoursTraitementAutomatises;

    @Column(name = "emplacement_physique")
    String emplacementPhysique;

    @Column(name = "dispositions_securite_donnees_physique")
    String dispositionsSecuriteDonneesPhysique;

    @Column(name = "emplacement_numerique")
    String emplacementNumerique;

    @Column(name = "dispositions_securite_donnees_numerique")
    String dispositionsSecuriteDonneesNumerique;

    @Column(name = "hebergement")
    String hebergement;

    @ManyToOne
    @JoinColumn(name = "duree_conservation_id")
    Duree dureeConservation;

    @Column(name = "archivage")
    Boolean archivage;

    @ManyToOne
    @JoinColumn(name = "duree_archivage_id")
    Duree dureeArchivage;

    @Column(name = "categories_destinataires")
    String categoriesDestinataires;

    @Column(name = "raisons_transfert_destinataires")
    String raisonsTransfertDestinataires;

    @Column(name = "transferts_hors_ue")
    Boolean transfertsHorsUE;

    @Column(name = "pays_destinataires")
    String paysDestinataires;

    @Column(name = "commentaires")
    String commentaires;

    @Column(name = "impact_traitement")
    Integer impactTraitement;

    @Column(name = "detournement_finalite")
    Integer detournementFinalite;

    @Column(name = "score_detournement_finalite")
    Integer scoreDetournementFinalite;

    @Column(name = "collecte_dcp_inappropriees")
    Integer collecteDcpInappropriees;

    @Column(name = "score_collecte_dcp_inappropriees")
    Integer scoreCollecteDcpInappropriees;

    @Column(name = "conservation_excessive_dcp")
    Integer conservationExcessiveDcp;

    @Column(name = "score_conservation_excessive_dcp")
    Integer scoreConservationExcessiveDcp;

    @Column(name = "securisation_insuffisante_dcp")
    Integer securisationInsuffisanteDcp;

    @Column(name = "score_securisation_insuffisante_dcp")
    Integer scoreSecurisationInsuffisanteDcp;

    @Column(name = "vices_consentement")
    Integer vicesConsentement;

    @Column(name = "score_vices_consentement")
    Integer scoreVicesConsentement;

    @Column(name = "manque_transparence")
    Integer manqueTransparence;

    @Column(name = "score_manque_transparence")
    Integer scoreManqueTransparence;

    @Column(name = "incapacite_exercice_droits")
    Integer incapaciteExerciceDroits;

    @Column(name = "score_incapacite_exercice_droits")
    Integer scoreIncapaciteExerciceDroits;

    @Column(name = "transfert_tiers_mal_encadre")
    Integer transfertTiersMalEncadre;

    @Column(name = "score_transfert_tiers_mal_encadre")
    Integer scoreTransfertTiersMalEncadre;

    @Column(name = "transfert_hors_ue_abusif")
    Integer transfertHorsUeAbusif;

    @Column(name = "score_transfert_hors_ue_abusif")
    Integer scoreTransfertHorsUeAbusif;

    @Column(name = "defaut_preuve")
    Integer defautPreuve;

    @Column(name = "score_defaut_preuve")
    Integer scoreDefautPreuve;

    @Column(name = "score_global")
    Integer scoreGlobal;

    @Column(name = "commentaires_analyse")
    String commentairesAnalyse;

    @Column(name = "exposition_traitement")
    Integer expositionTraitement;

    @Column(name = "critere_evaluation_scoring")
    Boolean critereEvaluationScoring;

    @Column(name = "critere_decision_automatique")
    Boolean critereDecisionAutomatique;

    @Column(name = "critere_surveillance_systematique")
    Boolean critereSurveillanceSystematique;

    @Column(name = "critere_collecte_donnees_sensibles")
    Boolean critereCollecteDonneesSensibles;

    @Column(name = "critere_collecte_large_echelle")
    Boolean critereCollecteLargeEchelle;

    @Column(name = "critere_croisement_donnees")
    Boolean critereCroisementDonnees;

    @Column(name = "critere_personnes_vulnerables")
    Boolean criterePersonnesVulnerables;

    @Column(name = "critere_usage_innovant")
    Boolean critereUsageInnovant;

    @Column(name = "critere_exclusion_benefice_droit")
    Boolean critereExclusionBeneficeDroit;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "traitement_etablissement",
            joinColumns = @JoinColumn(name = "id_traitement"),
            inverseJoinColumns = @JoinColumn(name = "id_etablissement")
    )
    List<Etablissement> etablissements;

}
