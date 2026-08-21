# Mapping BDD — Registre des activités de traitement

Ce document décrit comment les définitions métier du registre (onglet **FR_Définitions**)
sont reliées au modèle de données, **sans duplication des valeurs dans le code**.

## Principe : les définitions vivent dans le fichier, pas dans le code

```
 Fichier registre Excel                    Application Spring Boot                  BDD PostgreSQL
 ┌───────────────────────────┐    POST    ┌───────────────────────────────┐        ┌──────────────────────┐
 │ FR_Définitions            │  /import   │ DefinitionsRegistreImportSvc  │        │ definition_champ     │
 │  B: libellé du champ      │  Fichier ─>│  (parse colonnes B/C)         │ save / │  (libelle, definition│
 │  C: définition métier     │  Rgpd      │ CorrespondanceChampRegistre   │ delete │   table_cible,       │
 │                           │            │  (libellé -> table/colonne)   │ ------>│   colonne_cible, ...)│
 │ Registre de traitement    │            │ ImportSpecifications          │        │ traitement           │
 │  (données)                │            │  (données des traitements)    │ ------->│  (40 colonnes)       │
 └───────────────────────────┘            └───────────────────────────────┘        └──────────────────────┘
```

- **À l'import** (`POST /importFichierRgpd`), les définitions de l'onglet `FR_Définitions`
  sont extraites et persistées dans la table **`definition_champ`** (remplacement complet
  par couple client + édition, dans la même transaction que l'import du registre : tout ou rien).
- **À la lecture**, elles sont exposées par `GET /definitions-champs?clientNom=…&edition=…`
  (édition optionnelle) et requêtables en SQL.
- **À l'export** (évolution future), le contenu de `definition_champ` permettra de
  régénérer l'onglet `FR_Définitions` du fichier (l'ordre de l'onglet est conservé via
  la colonne `ordre`).
- Les définitions ne figurent **nulle part en dur** : ni dans les migrations Flyway
  (`V7.0` ne crée que la structure), ni dans les entités/DTO.

## Parsing de l'onglet FR_Définitions

| Règle | Effet |
|---|---|
| Colonne B remplie + colonne C remplie | Définition de champ → ligne `definition_champ` |
| Colonne B remplie + colonne C vide | En-tête de section (ex. « Identification du traitement ») → mémorisée sur les champs qui suivent |
| Colonne B vide | Ignorée (listes de référence en colonne C : données sensibles, licéité Art. 6, PIA obligatoire — lignes 43 à 76) |
| Libellé inconnu de la correspondance | Importé quand même, avec `table_cible`/`colonne_cible` nulles (écart visible en base) |

## Correspondance libellé FR_Définitions → modèle de données

Implémentée dans `CorrespondanceChampRegistre` (connaissance **technique** — la seule
chose codée en dur). Libellés tels qu'écrits dans l'onglet, édition 3.25.

| Libellé FR_Définitions (section) | Table cible | Colonne cible | Remarque |
|---|---|---|---|
| ID (Identification) | `traitement` | `id_fonctionnel` | Clé métier ; la PK technique `identifiant` (UUID) n'existe pas dans le registre. |
| Etablissement(s) (Identification) | `traitement_etablissement` | — (nulle) | Porte sur la relation N-N, pas une colonne de `traitement`. |
| Nom du traitement (Identification) | `traitement` | `nom` | En-tête registre identique. |
| Date d'identification du traitement (Identification) | `traitement` | `date_identification` | |
| Date de mise à jour (Identification) | `traitement` | `date_mise_a_jour` | |
| Historique de modifications (Identification) | `traitement` | `historique_modifications` | En-tête registre : « Historique **des** modifications » (I4). |
| Data Protection Officer (Identification) | `traitement` | `data_protection_officer` | |
| Responsable de traitement (Identification) | `traitement` | `responsable_traitement` | |
| Représentant de l'entité responsable de la mise en œuvre du traitement (Identification) | `traitement` | `gestionnaire_mise_en_oeuvre` | En-tête registre + BDD : « Gestionnaire de la mise… » (I4). |
| Responsable(s) conjoint(s) du traitement (Identification) | — nulle | — nulle | **Sans correspondance** (I1) : importée, cibles nulles. |
| Finalité principale (Identification) | `traitement` | `finalite_principale` | |
| Sous-finalités (Identification) | `traitement` | `sous_finalites` | |
| Catégories de personnes concernées par le traitement (Données personnelles) | `traitement` | `categories_personnes_concernees` | Voir I3 (redondance avec `donnees_concernees`). |
| Données d'identification (Données personnelles) | `traitement` | `donnees_identification` | |
| Données de connexion (Données personnelles) | `traitement` | `donnees_connexion` | |
| Données de localisation (Données personnelles) | `traitement` | `donnees_localisation` | |
| Données sur le comportement et la vie personnelle (Données personnelles) | `traitement` | `donnees_comportement_vie_perso` | |
| Données économiques et financières (Données personnelles) | `traitement` | `donnees_economiques_financieres` | |
| Données professionnelles (Données personnelles) | `traitement` | `donnees_professionnelles` | |
| Catégories particulières de données (Données personnelles) | `traitement` | `categories_particulieres_donnees` | En-tête registre complété : « …(NIR, santé par exemple) ». |
| Canaux de collecte des données (Description) | `traitement` | `canaux_collecte_donnees` | |
| Licéité du traitement (Description) | `traitement` | `liceite_traitement` | Coquille Java `licieteTraitement` (I5). |
| Recours au traitement automatisé (y compris profilage) (Description) | `traitement` | `recours_traitements_automatises` | |
| Applications support du traitement (Description) | — nulle | — nulle | **Sans correspondance** (I2) : importée, cibles nulles. |
| Dispositions existantes pour assurer la sécurité des données (Description) | `traitement` | `dispositions_securite_donnees_physique` **+** `dispositions_securite_donnees_numerique` | 1 définition → 2 lignes (colonne dupliquée dans le registre, renommée/scindée en V4.0). |
| Durée d'archivage courant (Description) | `traitement` | `duree_conservation` | En-tête registre : « Durée **de conservation** » (I6). |
| Durée d'archivage définitif (Description) | `traitement` | `duree_archivage` | En-tête registre : « Durée **d'archivage** » (I6). |
| Catégories de destinataires (Description) | `traitement` | `categories_destinataires` | |
| Raisons du transfert vers les catégories de destinataires (Description) | `traitement` | `raisons_transfert_destinataires` | |
| Transferts hors UE (Description) | `traitement` | `transferts_hors_ue` | |
| Pays destinataires (Description) | `traitement` | `pays_destinataires` | |

## Colonnes du registre sans définition dans FR_Définitions

Ces colonnes existent dans l'onglet *Registre de traitement* (et en BDD) mais n'ont
**pas** de définition dans `FR_Définitions` ed3.25 : rien n'est donc importé pour elles
dans `definition_champ` — constat remonté au métier (compléter l'onglet si souhaité) :

- `Données concernées` → `donnees_concernees` (colonne ajoutée en V6.0)
- `Sensibilité` → `sensibilite`
- `Etude d'impact (PIA)` → `etude_impact`
- `Emplacement physique du traitement` → `emplacement_physique`
- `Emplacement numérique du traitement` → `emplacement_numerique`
- `Hébergement` → `hebergement`
- `Durée de conservation` (voir I6) → `duree_conservation`
- `Archivage ? (Oui / Non)` → `archivage`
- `Durée d'archivage` (voir I6) → `duree_archivage`
- `Commentaires` → `commentaires`

Note : « Durée de conservation » et « Durée d'archivage » bénéficient tout de même d'une
définition, via la correspondance avec « Durée d'archivage courant/définitif » (I6).

## Incohérences remontées

| # | Objet | Constat | Proposition |
|---|---|---|---|
| **I1** | « Responsable(s) conjoint(s) du traitement » | Défini dans `FR_Définitions` (responsabilité conjointe, Art. 26 RGPD) mais **ni colonne du registre, ni colonne BDD**. Importé avec cibles nulles. | À trancher par le métier : ajouter la colonne au registre et au modèle, ou retirer la définition de l'onglet. |
| **I2** | « Applications support du traitement » | Défini dans `FR_Définitions` mais **sans colonne**. Les valeurs attendues (applications/logiciels) semblent partiellement portées par « Hébergement » (ex. « Microsoft », « Berger Levrault »). | À trancher : créer la colonne, ou clarifier le périmètre de « Hébergement ». |
| **I3** | `donnees_concernees` vs `categories_personnes_concernees` | Deux colonnes du registre décrivent les personnes concernées (liste libre vs catégories) ; aucune définition dans `FR_Définitions`. | Clarifier la différence d'usage dans le registre / compléter l'onglet. |
| **I4** | Écarts de libellés | `FR_Définitions` vs registre : « Représentant de l'entité… » vs « Gestionnaire de la mise en œuvre… » ; « Historique de » vs « Historique des » modifications. Correspondances assumées dans `CorrespondanceChampRegistre`. | Aligner les libellés dans le fichier. |
| **I5** | `licieteTraitement` | Coquille Java (`liciete` au lieu de `liceite`) ; la colonne BDD `liceite_traitement` est correcte. | Renommage Java dans un ticket dédié (impact API/JSON). |
| **I6** | Conservation / archivage | `FR_Définitions` décrit « archivage courant/définitif » ; registre+BDD portent « Durée de conservation » / « Archivage ? » / « Durée d'archivage ». Correspondance retenue : conservation ↔ courant, archivage ↔ définitif. La colonne « Durée d'archivage » contient des valeurs non temporelles (ex. « Editeur ») : **problème de qualité de données**. | Valider la correspondance ; corriger les données du registre. |
| **I7** | Édition du fichier | Le nom `…_Registre RGPD_ed3.25.xlsx` n'est pas stocké en entier : la regex de `FichierServiceImpl` (`[^.]+`) tronque l'édition au premier point → « 3 ». Cette valeur tronquée alimente `traitement.version` (INT) et `definition_champ.edition` (VARCHAR). L'édition complète « 3.25 » est perdue. | Ajuster la regex pour capturer l'édition complète (ex. `(?<edition>[\d.]+)` avant les extensions) et la stocker telle quelle (VARCHAR). Ticket dédié. |
| **I8** | Couverture de `FR_Définitions` | 10 colonnes du registre n'ont pas de définition dans l'onglet (liste ci-dessus). | Compléter l'onglet `FR_Définitions` : les définitions seront importées automatiquement au prochain import. |

## Requêtes utiles

```sql
-- Toutes les définitions d'un client, dans l'ordre de l'onglet
SELECT ordre, section, libelle, definition, table_cible, colonne_cible
FROM definition_champ d JOIN client c ON c.id = d.id_client
WHERE c.nom = 'La breteche'
ORDER BY d.ordre;

-- Définitions sans correspondance BDD (évolution du modèle à arbitrer)
SELECT libelle, definition FROM definition_champ WHERE colonne_cible IS NULL;

-- Colonnes de la table traitement (structure)
SELECT column_name FROM information_schema.columns WHERE table_name = 'traitement' ORDER BY ordinal_position;
```

## Maintenir la correspondance à jour

- **Une définition change dans le fichier** → rien à coder : elle sera réimportée.
- **Un nouveau champ** apparaît dans `FR_Définitions` → l'ajouter dans
  `CorrespondanceChampRegistre` si une colonne BDD lui correspond (sinon il sera
  importé avec cibles nulles, ce qui signale l'écart).
- **Une colonne est ajoutée/renommée** dans le modèle → mettre à jour la migration
  Flyway **et** `CorrespondanceChampRegistre`.
