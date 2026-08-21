# Mapping BDD — Registre des activités de traitement

Ce document centralise la correspondance entre le fichier registre Excel, le modèle de
données (BDD / entités JPA) et les définitions métier associées.

- **Source des définitions** : `src/test/resources/rgpdFile/La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx`,
  onglet **FR_Définitions** (définitions) et onglet **Registre de traitement** ligne 6 (en-têtes de colonnes).
- **Report dans le code** :
  - migration Flyway `V7.0__Ajout_commentaires_metiers.sql` : commentaires PostgreSQL
    (`COMMENT ON TABLE / COLUMN`), visibles dans pgAdmin / DBeaver ;
  - Javadoc des entités `com.minds.rgpd.persistence.entities` ;
  - annotations `@Schema` des DTO `com.minds.rgpd.business.dtos` (Swagger / OpenAPI).

## Provenance des définitions

| Marqueur | Signification |
|---|---|
| **[REGISTRE]** | Définition reprise de l'onglet `FR_Définitions`, éventuellement épurée des mentions propres à Excel (ex. raccourci clavier) ou complétée du libellé exact de la colonne source. |
| **[PROPOSÉE]** | Définition **absente de `FR_Définitions`**, rédigée d'après l'usage constaté dans le registre et le contexte RGPD. **À faire valider par le métier.** |

## Table de correspondance — table `traitement`

Colonnes dans l'ordre du registre (onglet *Registre de traitement*, ligne 6).
Le mapping Excel → colonne est implémenté dans `ImportSpecifications.traitement(...)`.

| # | Colonne du registre (en-tête) | Colonne BDD | Champ entité | Source déf. | Remarque |
|---|---|---|---|---|---|
| 1 | ID | `id_fonctionnel` | `idFonctionnel` | [REGISTRE] | Clé métier. La clé primaire technique `identifiant` (UUID) n'existe pas dans le registre. |
| 2 | Etablissement(s) | — (liaison `traitement_etablissement`) | `etablissements` | [REGISTRE] | Relation N-N, pas une colonne de `traitement`. |
| 3 | Données concernées | `donnees_concernees` | `donneesConcernees` | [PROPOSÉE] | Absente de `FR_Définitions`. Redondance possible avec `categories_personnes_concernees` (voir I3). |
| 4 | Nom du traitement | `nom` | `nom` | [REGISTRE] | |
| 5 | Date d'identification du traitement | `date_identification` | `dateIdentification` | [REGISTRE] | |
| 6 | Date de mise à jour | `date_mise_a_jour` | `dateMiseAJour` | [REGISTRE] | Définition épurée de la mention Excel « raccourci clavier CTRL ; ». |
| 7 | Historique des modifications | `historique_modifications` | `historiqueModifications` | [REGISTRE] | Libellé légèrement différent dans `FR_Définitions` : « Historique **de** modifications ». |
| 8 | Data Protection Officer | `data_protection_officer` | `dataProtectionOfficer` | [REGISTRE] | |
| 9 | Responsable de traitement | `responsable_traitement` | `responsableTraitement` | [REGISTRE] | |
| 10 | Gestionnaire de la mise en œuvre du traitement | `gestionnaire_mise_en_oeuvre` | `gestionnaireMiseEnOeuvre` | [REGISTRE] | Libellé différent dans `FR_Définitions` : « Représentant de l'entité responsable de la mise en œuvre du traitement » (voir I4). |
| 11 | Finalité principale | `finalite_principale` | `finalitePrincipale` | [REGISTRE] | |
| 12 | Sous-finalités | `sous_finalites` | `sousFinalites` | [REGISTRE] | |
| 13 | Catégories de personnes concernées par le traitement | `categories_personnes_concernees` | `categoriesPersonnesConcernees` | [REGISTRE] | |
| 14 | Données d'identification | `donnees_identification` | `donneesIdentification` | [REGISTRE] | |
| 15 | Données de connexion | `donnees_connexion` | `donneesConnexion` | [REGISTRE] | |
| 16 | Données de localisation | `donnees_localisation` | `donneesLocalisation` | [REGISTRE] | |
| 17 | Données sur le comportement et la vie personnelle | `donnees_comportement_vie_perso` | `donneesComportementViePerso` | [REGISTRE] | |
| 18 | Données économiques et financières | `donnees_economiques_financieres` | `donneesEconomiquesFinancieres` | [REGISTRE] | |
| 19 | Données professionnelles | `donnees_professionnelles` | `donneesProfessionnelles` | [REGISTRE] | |
| 20 | Catégories particulières de données (NIR, santé par exemple) | `categories_particulieres_donnees` | `categoriesParticulieresDonnees` | [REGISTRE] | Définition complétée du « NIR » présent dans le libellé de la colonne. |
| 21 | Sensibilité | `sensibilite` | `sensibilite` | [PROPOSÉE] | Absente de `FR_Définitions`. Valeurs = liste « Liste des données sensibles » de `FR_Définitions` (lignes 43-54). |
| 22 | Etude d'impact (PIA) | `etude_impact` | `etudeImpact` | [PROPOSÉE] | Absente de `FR_Définitions`. Valeurs = liste « PIA obligatoire dans les cas suivants » de `FR_Définitions` (lignes 66-76). |
| 23 | Canaux de collecte des données | `canaux_collecte_donnees` | `canauxCollecteDonnees` | [REGISTRE] | |
| 24 | Licéité du traitement | `liceite_traitement` | `licieteTraitement` | [REGISTRE] | Valeurs = liste « Licéité du traitement (Article 6) » de `FR_Définitions` (lignes 57-63). Coquille Java : `licieteTraitement` (voir I5). |
| 25 | Recours au traitements automatisés (y compris profilage) ? (Oui / Non) | `recours_traitements_automatises` | `recoursTraitementAutomatises` | [REGISTRE] | |
| 26 | Emplacement physique du traitement | `emplacement_physique` | `emplacementPhysique` | [PROPOSÉE] | Absente de `FR_Définitions`. |
| 27 | Dispositions existantes pour assurer la sécurité des données (1ʳᵉ occurrence) | `dispositions_securite_donnees_physique` | `dispositionsSecuriteDonneesPhysique` | [REGISTRE] | Colonne dupliquée dans le registre : la 1ʳᵉ concerne le physique (suffixe ajouté en V4.0). |
| 28 | Emplacement numérique du traitement | `emplacement_numerique` | `emplacementNumerique` | [PROPOSÉE] | Absente de `FR_Définitions`. |
| 29 | Dispositions existantes pour assurer la sécurité des données (2ᵉ occurrence) | `dispositions_securite_donnees_numerique` | `dispositionsSecuriteDonneesNumerique` | [REGISTRE] | Même définition, déclinée sur le périmètre numérique. |
| 30 | Hébergement | `hebergement` | `hebergement` | [PROPOSÉE] | Absente de `FR_Définitions`. |
| 31 | Durée de conservation | `duree_conservation` | `dureeConservation` | [REGISTRE] | Libellé différent dans `FR_Définitions` : « Durée d'archivage courant » (voir I6). |
| 32 | Archivage ? (Oui / Non) | `archivage` | `archivage` | [PROPOSÉE] | Absente de `FR_Définitions`. |
| 33 | Durée d'archivage | `duree_archivage` | `dureeArchivage` | [REGISTRE] | Libellé différent dans `FR_Définitions` : « Durée d'archivage définitif » (voir I6). |
| 34 | Catégories de destinataires | `categories_destinataires` | `categoriesDestinataires` | [REGISTRE] | |
| 35 | Raisons du transfert vers les catégories de destinataires | `raisons_transfert_destinataires` | `raisonsTransfertDestinataires` | [REGISTRE] | |
| 36 | Transferts hors UE (Oui / Non) | `transferts_hors_ue` | `transfertsHorsUE` | [REGISTRE] | |
| 37 | Pays destinataires | `pays_destinataires` | `paysDestinataires` | [REGISTRE] | |
| 38 | Commentaires | `commentaires` | `commentaires` | [PROPOSÉE] | Absente de `FR_Définitions`. |

### Colonnes techniques (hors registre)

| Colonne BDD | Champ entité | Source déf. | Définition |
|---|---|---|---|
| `identifiant` | `identifiant` | [PROPOSÉE] | Identifiant technique unique du traitement (UUID généré par l'application). |
| `id_client` | `client` | [PROPOSÉE] | Client (organisme) propriétaire du traitement ; cloisonnement multi-clients. |
| `version` | `version` | [PROPOSÉE] | Édition du fichier registre source (extraite du nom de fichier). Voir I7. |

## Tables support (hors registre — toutes [PROPOSÉE])

| Table | Rôle |
|---|---|
| `client` | Organisme client de la plateforme (tenant) ; toutes les données métier y sont rattachées. |
| `etablissement` | Établissement (site, service) d'un client ; alimenté par la colonne « Etablissement(s) » du registre. |
| `traitement_etablissement` | Liaison N-N traitement ↔ établissements concernés. |
| `profil` | Profil fonctionnel d'un utilisateur (ADMIN, USER, DPO…). |
| `utilisateur` | Utilisateur de l'application, rattaché à un client et un profil. |
| `utilisateur_etablissement` | Affectation des utilisateurs aux établissements de leur client. |

## Définitions `FR_Définitions` sans correspondance dans le modèle

| Définition (`FR_Définitions`) | Statut |
|---|---|
| « Responsable(s) conjoint(s) du traitement » | Aucune colonne (registre et BDD). Voir I1. |
| « Applications support du traitement » | Aucune colonne (registre et BDD). Voir I2. |

## Incohérences remontées

| # | Objet | Constat | Proposition |
|---|---|---|---|
| **I1** | « Responsable(s) conjoint(s) du traitement » | Défini dans `FR_Définitions` (responsabilité conjointe, Art. 26 RGPD) mais **ni colonne du registre, ni colonne BDD**. | À trancher par le métier : ajouter la colonne au registre et `responsables_conjoints` au modèle, ou retirer la définition de l'onglet. |
| **I2** | « Applications support du traitement » | Défini dans `FR_Définitions` mais **ni colonne du registre, ni colonne BDD**. Les valeurs attendues (applications/logiciels) semblent partiellement portées par la colonne « Hébergement » (ex. « Microsoft », « Berger Levrault »). | À trancher : créer `applications_support` ou clarifier le périmètre de « Hébergement ». |
| **I3** | `donnees_concernees` vs `categories_personnes_concernees` | Deux colonnes du registre décrivent les personnes concernées : « Données concernées » (liste libre, ex. « Demandeurs », « Familles et proches des demandeurs ») et « Catégories de personnes concernées » (liste plus normalisée). Absente de `FR_Définitions`. | Clarifier la différence d'usage (saisie brute vs catégories) dans le registre. |
| **I4** | `gestionnaire_mise_en_oeuvre` | `FR_Définitions` libelle le champ « Représentant de l'entité responsable de la mise en œuvre du traitement », le registre « Gestionnaire de la mise en œuvre du traitement ». Même définition métier. | Aligner les libellés dans le fichier registre. |
| **I5** | `licieteTraitement` | Coquille dans le nom du champ Java (`liciete` au lieu de `liceite`) ; la colonne BDD `liceite_traitement` est correcte. Renommer touche tout le code (API incluse si le JSON suit le nom Java). | Renommage Java possible dans un ticket dédié. |
| **I6** | Durées de conservation / archivage | `FR_Définitions` décrit « Durée d'archivage courant » et « Durée d'archivage définitif » ; le registre (et la BDD) portent « Durée de conservation », « Archivage ? » et « Durée d'archivage ». Correspondance retenue : conservation ↔ archivage courant, archivage ↔ archivage définitif. De plus la colonne « Durée d'archivage » contient des valeurs non temporelles (ex. « Editeur ») : **problème de qualité de données** dans le registre. | Valider la correspondance retenue ; corriger les données du registre. |
| **I7** | `version` | Le numéro d'édition du fichier (ex. « 3.25 » dans `..._Registre RGPD_ed3.25.xlsx`) est stocké en `INT` : `ImportSpecifications.parseVersion()` rejette « 3.25 » (**non entier**) et retombe silencieusement sur `1`. L'information d'édition réelle est perdue. | Stocker l'édition en `VARCHAR`/`DECIMAL`, ou ne garder que la partie entière, selon le besoin métier. |
| **I8** | `FR_Définitions` ne couvre pas tout le registre | 10 colonnes du registre n'ont pas de définition dans l'onglet (`Données concernées`, `Sensibilité`, `Etude d'impact`, emplacements, hébergement, durée de conservation, archivage, commentaires…). Des définitions **[PROPOSÉE]** ont été rédigées dans ce document et la migration V7.0. | Faire valider les définitions proposées par le métier, puis éventuellement compléter l'onglet `FR_Définitions`. |

## Maintenir la documentation à jour

Toute évolution du modèle ou d'une définition doit être répercutée aux **4 endroits** :

1. migration Flyway (`COMMENT ON …`) — source de vérité BDD ;
2. Javadoc de l'entité concernée ;
3. `@Schema` du DTO correspondant ;
4. ce document (table de correspondance + provenance).
