package com.minds.rgpd.business.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Registre des activités de traitement (Art. 30 RGPD) : un traitement de données à caractère
 * personnel d'un client (organisme).
 * <p>
 * Les définitions métier des champs sont issues de l'onglet « FR_Définitions » du fichier
 * registre ; la correspondance complète et la provenance de chaque définition sont documentées
 * dans {@code docs/mapping-bdd-registre.md}.
 */
@Builder
public record TraitementDTO(

        @Schema(description = "Identifiant technique unique du traitement (UUID généré par l'application).")
        @NotNull
        UUID identifiant,

        @Schema(description = "Numéro unique d'identification du traitement (colonne « ID » du registre) ; clé métier issue du fichier registre source.")
        @NotNull
        Integer idFonctionnel,

        @Schema(description = "Nom du traitement : suffisamment explicite pour que l'on comprenne de manière macro ce que fait le traitement.")
        @NotNull
        String nom,

        @Schema(description = "Personnes concernées par le traitement telles que saisies dans la colonne « Données concernées » du registre (liste libre, ex. « Demandeurs », « Familles et proches des demandeurs »).")
        String donneesConcernees,

        @Schema(description = "Établissement(s) concerné(s) par le traitement : si l'organisme a plusieurs établissements, le ou les établissements concernés par ce traitement.")
        List<EtablissementDTO> etablissements,

        @Schema(description = "Objectif final spécifique, explicite et légitime pour lequel le traitement a lieu.")
        String finalitePrincipale,

        @Schema(description = "Client (organisme) propriétaire du traitement.")
        ClientDTO client,

        @Schema(description = "Édition du fichier registre source à l'origine du traitement ; nulle pour un traitement créé hors import.")
        Integer version,

        @Schema(description = "Date à laquelle le traitement a été créé dans le registre.")
        @NotNull
        LocalDate dateIdentification,

        @Schema(description = "Date à laquelle les informations sur le traitement ont été mises à jour pour la dernière fois.")
        LocalDate dateMiseAJour,

        @Schema(description = "Liste de l'ensemble des modifications réalisées dans le registre sur le traitement, afin d'assurer une traçabilité des actions.")
        String historiqueModifications,

        @Schema(description = "Nom et coordonnées de la personne ayant le rôle de DPO pour le traitement.")
        String dataProtectionOfficer,

        @Schema(description = "Fonction de la personne ayant le rôle de responsable de traitement, c'est-à-dire qui détermine les finalités et les moyens du traitement ; souvent un membre de la Direction Générale de l'entreprise.")
        String responsableTraitement,

        @Schema(description = "Fonction du représentant de l'entité ou du service en charge de mettre en œuvre le traitement (ex. : pour le traitement des CV des candidats, le responsable de l'équipe recrutement).")
        String gestionnaireMiseEnOeuvre,

        @Schema(description = "Sous-finalités du traitement ; elles doivent être rattachées à la finalité principale.")
        String sousFinalites,

        @Schema(description = "Catégories de personnes concernées par le traitement. Exemples : employés, prestataires, clients, partenaires…")
        String categoriesPersonnesConcernees,

        @Schema(description = "Données d'identification traitées. Exemples : nom, prénom, adresse postale, numéro de téléphone, adresse e-mail, photos, vidéos…")
        String donneesIdentification,

        @Schema(description = "Données de connexion traitées. Exemples : adresse IP, logs, cookies, historique de navigation…")
        String donneesConnexion,

        @Schema(description = "Données de localisation traitées. Exemples : positionnement GPS, GSM…")
        String donneesLocalisation,

        @Schema(description = "Données sur le comportement et la vie personnelle. Exemples : situation familiale, habitudes de vie, habitudes de consommation…")
        String donneesComportementViePerso,

        @Schema(description = "Données économiques et financières. Exemples : revenus, situation financière, situation fiscale, numéro de carte bancaire, RIB…")
        String donneesEconomiquesFinancieres,

        @Schema(description = "Données professionnelles. Exemples : nom de l'employeur, statut dans l'entreprise, contrat de travail…")
        String donneesProfessionnelles,

        @Schema(description = "Catégories particulières de données (Art. 9 et 10 RGPD). Exemples : origine raciale ou ethnique, opinions politiques, convictions religieuses ou philosophiques ou appartenance syndicale, données génétiques, données biométriques, données de santé, vie sexuelle ou orientation sexuelle, condamnations pénales et infractions, NIR…")
        String categoriesParticulieresDonnees,

        @Schema(description = "Type(s) de données sensibles effectivement traitées (ex. « NIR (N° SS) », « Concernant la santé », « Pas de donnée sensible »…).")
        String sensibilite,

        @Schema(description = "Cas rendant une étude d'impact (PIA/AIPD) obligatoire pour le traitement (ex. données sensibles Art. 9, traitement à grande échelle…).")
        String etudeImpact,

        @Schema(description = "Moyens par lesquels les données sont récupérées / collectées pour le traitement. Par exemple : mail, formulaire papier, caméras…")
        String canauxCollecteDonnees,

        @Schema(description = "Base légale du traitement (Art. 6 RGPD) : consentement de la personne, exécution d'un contrat, respect d'une obligation légale, etc.")
        String licieteTraitement,

        @Schema(description = "Recours à un traitement non basé sur une intervention ou analyse humaine, y compris le profilage (évaluation ou prédiction automatisée : santé, situation économique, préférences, comportement, déplacements…).")
        Boolean recoursTraitementAutomatises,

        @Schema(description = "Lieu physique de conservation des données du traitement (ex. : bureau, armoire fermée à clé, archives papier…).")
        String emplacementPhysique,

        @Schema(description = "Dispositifs en place pour assurer la sécurité des données conservées physiquement (ex. : classeur dans une armoire fermée à clé ; référence possible au DAT, au PIA ou à toute documentation des mesures de sécurité de l'entreprise).")
        String dispositionsSecuriteDonneesPhysique,

        @Schema(description = "Localisation numérique des données du traitement (ex. : applications, serveurs de fichiers, messagerie…).")
        String emplacementNumerique,

        @Schema(description = "Dispositifs en place pour assurer la sécurité des données conservées numériquement (ex. : authentification par utilisateur + mot de passe ; référence possible au DAT, au PIA ou à toute documentation des mesures de sécurité de l'entreprise).")
        String dispositionsSecuriteDonneesNumerique,

        @Schema(description = "Hébergeur(s) des données du traitement (ex. : éditeurs, fournisseurs cloud, hébergement interne, État…).")
        String hebergement,

        @Schema(description = "Durée de conservation pour les archives dites courantes, réservées à l'utilisation courante des données par les services responsables de la mise en œuvre du traitement (ex. : « 5 ans », « Supprimer »).")
        String dureeConservation,

        @Schema(description = "Indique si les données font l'objet d'un archivage à l'issue de leur durée de conservation.")
        Boolean archivage,

        @Schema(description = "Durée de conservation des archives dites définitives, réservées aux archives présentant un intérêt historique, statistique ou scientifique.")
        String dureeArchivage,

        @Schema(description = "Toute personne, service, entreprise filiale ou tiers ayant un accès aux données à caractère personnel du traitement.")
        String categoriesDestinataires,

        @Schema(description = "Intérêts des destinataires à recevoir les données à caractère personnel transférées.")
        String raisonsTransfertDestinataires,

        @Schema(description = "Communications de données à caractère personnel qui font ou sont destinées à faire l'objet d'un traitement en dehors des États membres de l'Union Européenne.")
        Boolean transfertsHorsUE,

        @Schema(description = "Pays dans lesquels sont basés les catégories de destinataires à qui les données sont communiquées.")
        String paysDestinataires,

        @Schema(description = "Informations complémentaires en texte libre sur le traitement.")
        String commentaires
) {
}
