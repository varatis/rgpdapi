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

/**
 * Registre des activités de traitement (Art. 30 RGPD) : une ligne décrit un traitement
 * de données à caractère personnel d'un client (organisme).
 * <p>
 * Les définitions métier des champs sont reprises de l'onglet « FR_Définitions » du fichier
 * registre (voir {@code docs/mapping-bdd-registre.md} pour la correspondance complète et la
 * provenance de chaque définition) et sont également portées en base via les commentaires
 * PostgreSQL de la migration {@code V7.0__Ajout_commentaires_metiers.sql}.
 */
@Builder
@Data
@Entity
@Table(name = "traitement")
@NoArgsConstructor
@AllArgsConstructor
public class Traitement {

    /**
     * Identifiant technique unique du traitement (UUID généré par l'application).
     * À ne pas confondre avec {@link #idFonctionnel}, la clé métier issue du registre.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID identifiant;

    /**
     * Numéro unique d'identification du traitement (colonne « ID » du registre).
     * Clé métier issue du fichier registre source, distincte de l'identifiant technique UUID.
     */
    @Column(name = "id_fonctionnel")
    @NotNull
    Integer idFonctionnel;

    /**
     * Le nom du traitement doit être suffisamment explicite pour que l'on comprenne
     * de manière macro ce que fait le traitement.
     */
    @Column(name = "nom")
    @NotNull
    String nom;

    /**
     * Personnes concernées par le traitement telles que saisies dans la colonne
     * « Données concernées » du registre (liste libre, ex. « Demandeurs »,
     * « Familles et proches des demandeurs »).
     * Voir aussi {@link #categoriesPersonnesConcernees} pour la liste normalisée.
     */
    @Column(name = "donnees_concernees")
    String donneesConcernees;

    /**
     * Objectif final spécifique, explicite et légitime pour lequel le traitement a lieu.
     */
    @Column(name = "finalite_principale")
    String finalitePrincipale;

    /**
     * Client (organisme) propriétaire du traitement ; assure le cloisonnement
     * multi-clients des données du registre.
     */
    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    /**
     * Édition du fichier registre source à l'origine du traitement, extraite du nom de fichier
     * (« &lt;client&gt;_&lt;etablissement&gt;_Registre RGPD_ed&lt;édition&gt;.xlsx ») lors de
     * l'import ; nulle pour un traitement créé hors import.
     */
    @Column(name = "version")
    Integer version;

    /**
     * Date à laquelle le traitement a été créé dans le registre.
     */
    @Column(name = "date_identification")
    @NotNull
    LocalDate dateIdentification;

    /**
     * Date à laquelle les informations sur le traitement ont été mises à jour pour la dernière fois.
     */
    @Column(name = "date_mise_a_jour")
    LocalDate dateMiseAJour;

    /**
     * Liste de l'ensemble des modifications qui ont été réalisées dans le registre
     * sur le traitement, afin d'assurer une traçabilité des actions
     * (colonne « Historique des modifications » du registre).
     */
    @Column(name = "historique_modifications")
    String historiqueModifications;

    /**
     * Nom et coordonnées de la personne ayant le rôle de DPO pour le traitement.
     */
    @Column(name = "data_protection_officer")
    String dataProtectionOfficer;

    /**
     * Fonction de la personne ayant le rôle de responsable de traitement, c'est-à-dire
     * qui détermine les finalités et les moyens du traitement ; souvent un membre de
     * la Direction Générale de l'entreprise.
     */
    @Column(name = "responsable_traitement")
    String responsableTraitement;

    /**
     * Fonction du représentant de l'entité ou du service qui est en charge de mettre en œuvre
     * le traitement (ex. : pour le traitement des CV des candidats, le responsable de l'équipe
     * recrutement).
     * <p>
     * Colonne « Gestionnaire de la mise en œuvre du traitement » du registre, libellée
     * « Représentant de l'entité responsable de la mise en œuvre du traitement » dans
     * l'onglet FR_Définitions.
     */
    @Column(name = "gestionnaire_mise_en_oeuvre")
    String gestionnaireMiseEnOeuvre;

    /**
     * Les sous-finalités doivent être rattachées à la finalité principale du traitement.
     */
    @Column(name = "sous_finalites")
    String sousFinalites;

    /**
     * Catégories de personnes concernées par le traitement.
     * Exemples : employés, prestataires, clients, partenaires…
     */
    @Column(name = "categories_personnes_concernees")
    String categoriesPersonnesConcernees;

    /**
     * Données d'identification traitées.
     * Exemples : nom, prénom, adresse postale, numéro de téléphone, adresse e-mail,
     * photos, vidéos…
     */
    @Column(name = "donnees_identification")
    String donneesIdentification;

    /**
     * Données de connexion traitées.
     * Exemples : adresse IP, logs, cookies, historique de navigation…
     */
    @Column(name = "donnees_connexion")
    String donneesConnexion;

    /**
     * Données de localisation traitées.
     * Exemples : positionnement GPS, GSM…
     */
    @Column(name = "donnees_localisation")
    String donneesLocalisation;

    /**
     * Données sur le comportement et la vie personnelle.
     * Exemples : situation familiale, habitudes de vie, habitudes de consommation…
     */
    @Column(name = "donnees_comportement_vie_perso")
    String donneesComportementViePerso;

    /**
     * Données économiques et financières.
     * Exemples : revenus, situation financière, situation fiscale, numéro de carte
     * bancaire, RIB…
     */
    @Column(name = "donnees_economiques_financieres")
    String donneesEconomiquesFinancieres;

    /**
     * Données professionnelles.
     * Exemples : nom de l'employeur, statut dans l'entreprise, contrat de travail…
     */
    @Column(name = "donnees_professionnelles")
    String donneesProfessionnelles;

    /**
     * Catégories particulières de données (Art. 9 et 10 RGPD).
     * Exemples : origine raciale ou ethnique, opinions politiques, convictions religieuses
     * ou philosophiques ou appartenance syndicale, données génétiques, données biométriques,
     * données concernant la santé, la vie sexuelle ou l'orientation sexuelle, données
     * relatives aux condamnations pénales et aux infractions, NIR…
     */
    @Column(name = "categories_particulieres_donnees")
    String categoriesParticulieresDonnees;

    /**
     * Type(s) de données sensibles effectivement traitées, choisis dans la liste de référence
     * de l'onglet FR_Définitions du registre (ex. « NIR (N° SS) », « Concernant la santé »,
     * « Pas de donnée sensible »…).
     */
    @Column(name = "sensibilite")
    String sensibilite;

    /**
     * Cas rendant une étude d'impact (PIA/AIPD) obligatoire pour le traitement (ex. données
     * sensibles Art. 9, traitement à grande échelle…), choisi parmi les critères de référence
     * listés dans l'onglet FR_Définitions du registre (Art. 35 RGPD).
     */
    @Column(name = "etude_impact")
    String etudeImpact;

    /**
     * Moyens par lesquels les données sont récupérées / collectées pour le traitement.
     * Par exemple : mail, formulaire papier, caméras…
     */
    @Column(name = "canaux_collecte_donnees")
    String canauxCollecteDonnees;

    /**
     * Base légale du traitement (Art. 6 RGPD) : le traitement n'est licite que si la personne
     * concernée a donné son consentement au traitement de ses données, si le traitement est
     * nécessaire à l'exécution d'un contrat auquel la personne concernée est partie ou si le
     * traitement est nécessaire au respect d'une obligation légale.
     * <p>
     * Valeurs choisies dans la liste de référence « Licéité du traitement (Article 6) » de
     * l'onglet FR_Définitions du registre.
     */
    @Column(name = "liceite_traitement")
    String licieteTraitement;

    /**
     * Recours à un traitement non basé sur une intervention ou analyse humaine, y compris
     * toute forme de traitement automatisé consistant à utiliser les données à caractère
     * personnel pour évaluer ou prédire des éléments la concernant (santé, situation
     * économique, préférences personnelles, comportement, déplacements…).
     */
    @Column(name = "recours_traitements_automatises")
    Boolean recoursTraitementAutomatises;

    /**
     * Lieu physique de conservation des données du traitement (ex. : bureau, armoire fermée
     * à clé, archives papier…).
     */
    @Column(name = "emplacement_physique")
    String emplacementPhysique;

    /**
     * Dispositifs en place pour assurer la sécurité des données conservées physiquement
     * (ex. : classeur dans une armoire fermée à clé).
     * <p>
     * Il est possible de faire référence à des documents tels que le Dossier d'Architecture
     * Technique, le PIA (Privacy Impact Assessment) ou toute autre documentation des mesures
     * de sécurité « standard » existantes dans l'entreprise.
     */
    @Column(name = "dispositions_securite_donnees_physique")
    String dispositionsSecuriteDonneesPhysique;

    /**
     * Localisation numérique des données du traitement (ex. : applications, serveurs de
     * fichiers, messagerie…).
     */
    @Column(name = "emplacement_numerique")
    String emplacementNumerique;

    /**
     * Dispositifs en place pour assurer la sécurité des données conservées numériquement
     * (ex. : authentification par utilisateur + mot de passe).
     * <p>
     * Il est possible de faire référence à des documents tels que le Dossier d'Architecture
     * Technique, le PIA (Privacy Impact Assessment) ou toute autre documentation des mesures
     * de sécurité « standard » existantes dans l'entreprise.
     */
    @Column(name = "dispositions_securite_donnees_numerique")
    String dispositionsSecuriteDonneesNumerique;

    /**
     * Hébergeur(s) des données du traitement (ex. : éditeurs, fournisseurs cloud, hébergement
     * interne, État…).
     */
    @Column(name = "hebergement")
    String hebergement;

    /**
     * Durée de conservation pour les archives dites courantes : les archives courantes sont
     * réservées à l'utilisation courante des données par les services responsables de la mise
     * en œuvre du traitement (ex. : « 5 ans », « Supprimer »).
     * <p>
     * Colonne « Durée de conservation » du registre, correspondant à « Durée d'archivage
     * courant » dans l'onglet FR_Définitions.
     */
    @Column(name = "duree_conservation")
    String dureeConservation;

    /**
     * Indique si les données font l'objet d'un archivage à l'issue de leur durée de
     * conservation (colonne « Archivage ? (Oui / Non) » du registre).
     */
    @Column(name = "archivage")
    Boolean archivage;

    /**
     * Durée de conservation des archives dites définitives : les archives définitives sont
     * réservées aux archives présentant un intérêt historique, statistique ou scientifique.
     * <p>
     * Colonne « Durée d'archivage » du registre, correspondant à « Durée d'archivage
     * définitif » dans l'onglet FR_Définitions.
     */
    @Column(name = "duree_archivage")
    String dureeArchivage;

    /**
     * Toute personne, service, entreprise filiale ou tiers ayant un accès aux données
     * à caractère personnel du traitement.
     */
    @Column(name = "categories_destinataires")
    String categoriesDestinataires;

    /**
     * Intérêts des destinataires à recevoir les données à caractère personnel transférées.
     */
    @Column(name = "raisons_transfert_destinataires")
    String raisonsTransfertDestinataires;

    /**
     * Communications de données à caractère personnel qui font ou sont destinées à faire
     * l'objet d'un traitement en dehors des États membres de l'Union Européenne.
     */
    @Column(name = "transferts_hors_ue")
    Boolean transfertsHorsUE;

    /**
     * Pays dans lesquels sont basés les catégories de destinataires à qui les données sont
     * communiquées.
     */
    @Column(name = "pays_destinataires")
    String paysDestinataires;

    /**
     * Informations complémentaires en texte libre sur le traitement.
     */
    @Column(name = "commentaires")
    String commentaires;

    /**
     * Établissement(s) concerné(s) par le traitement : si l'organisme a plusieurs
     * établissements, indique le ou les établissements concernés par ce traitement
     * (colonne « Etablissement(s) » du registre).
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "traitement_etablissement",
            joinColumns = @JoinColumn(name = "id_traitement"),
            inverseJoinColumns = @JoinColumn(name = "id_etablissement")
    )
    List<Etablissement> etablissements;

}
