package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Builder
@Data
@Entity
@Table(name = "traitement")
@NoArgsConstructor
@AllArgsConstructor
public class Traitement {

    @Id
    Integer id;

    @Column(name = "nom")
    @NotNull
    String nom;

    @Column(name = "gestionnaire")
    String gestionnaire;

    @Column(name = "finalite_principale")
    String finalitePrincipale;

    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    @Column(name = "version")
    Integer version;

    @Column(name = "date_identification")
    LocalDate dateIdentification;

    @Column(name = "date_mise_a_jour")
    LocalDate dateMiseAJour;

    @Column(name = "historique_modifications")
    String historiqueModifications;

    @Column(name = "data_protection_officer")
    String dataProtectionOfficer;

    @Column(name = "responsable_traitement")
    String responsableTraitement;

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

    @Column(name = "sensibilite")
    String sensibilite;

    @Column(name = "etude_impact")
    String etudeImpact;

    @Column(name = "canaux_collecte_donnees")
    String canauxCollecteDonnees;

    @Column(name = "liceite_traitement")
    String licieteTraitement;

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

    @Column(name = "duree_conservation")
    String dureeConservation;

    @Column(name = "archivage")
    Boolean archivage;

    @Column(name = "duree_archivage")
    String dureeArchivage;

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

    @ManyToMany
    @JoinTable(
            name = "traitement_etablissement",
            joinColumns = @JoinColumn(name = "id_traitement"),
            inverseJoinColumns = @JoinColumn(name = "id_etablissement")
    )
    List<Etablissement> etablissements;

}
