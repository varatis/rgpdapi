# Catalogue des routes HTTP

[Accueil](../index.md) / Références

> Référence extraite des sources par `tools/generate_docs_reference.py`. À relire après chaque évolution ; le code et le contrat runtime priment.

## Comment lire ce catalogue

Chaque ligne correspond à un mapping déclaré dans un contrôleur. « Authentifié (global) » signifie absence de restriction de méthode supplémentaire dans le contrôleur, **pas accès public**. Hors profil test, la chaîne Spring demande un JWT sur ces routes. Les annotations des services peuvent ajouter des restrictions.

**Exception importante :** la recherche utilisateurs requiert ADMIN ou SUPERADMIN dans `UtilisateurServiceImpl`. SUPERADMIN n’hérite pas automatiquement d’ADMIN. Voir [sécurité](../guides/05-securite.md).

**Identifiants :** GET/PUT et historique de traitement utilisent le numéro fonctionnel ; DELETE individuel utilise un UUID. Consulter les signatures sources.

**Inventaire : 49 mappings dans 10 contrôleurs.**

## ClientController

Source : [ClientController.java](../../src/main/java/com/minds/rgpd/web/controllers/ClientController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/clients` | `getClients` | `isAuthenticated()` |
| `GET` | `/clients/nom/{nom}` | `getClientByNom` | `isAuthenticated()` |
| `POST` | `/clients` | `createClient` | `hasAnyRole('SUPERADMIN')` |
| `PUT` | `/clients/{id}` | `updateClient` | `hasAnyRole('SUPERADMIN')` |
| `DELETE` | `/clients/{id}` | `deleteClient` | `hasAnyRole('SUPERADMIN')` |
| `GET` | `/clients/{id}/logo` | `getLogo` | `isAuthenticated()` |
| `GET` | `/clients/{id}/logo/info` | `getLogoInfo` | `isAuthenticated()` |
| `PUT` | `/clients/{id}/logo` | `updateLogo` | `hasAnyRole('SUPERADMIN')` |
| `DELETE` | `/clients/{id}/logo` | `deleteLogo` | `hasAnyRole('SUPERADMIN')` |

## DemandeController

Source : [DemandeController.java](../../src/main/java/com/minds/rgpd/web/controllers/DemandeController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/demandes` | `getDemandes` | `isAuthenticated()` |
| `GET` | `/demandes/{id}` | `getDemande` | `isAuthenticated()` |
| `POST` | `/demandes` | `createDemande` | `isAuthenticated()` |
| `PUT` | `/demandes/{id}/traiter` | `traiterDemande` | `hasAnyRole('ADMIN','SUPERADMIN')` |

## EtablissementController

Source : [EtablissementController.java](../../src/main/java/com/minds/rgpd/web/controllers/EtablissementController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/etablissements` | `getEtablissements` | `isAuthenticated()` |
| `GET` | `/etablissements/{id}` | `getEtablissement` | `isAuthenticated()` |
| `POST` | `/etablissements` | `postEtablissement` | `hasAnyRole('ADMIN','SUPERADMIN')` |
| `PUT` | `/etablissements/{id}` | `putEtablissement` | `hasAnyRole('ADMIN','SUPERADMIN')` |
| `DELETE` | `/etablissements/{id}` | `deleteEtablissement` | `hasAnyRole('ADMIN','SUPERADMIN')` |

## FichierController

Source : [FichierController.java](../../src/main/java/com/minds/rgpd/web/controllers/FichierController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/importFichierRgpd/apercu` | `apercuImport` | `isAuthenticated()` |
| `POST` | `/importFichierRgpd` | `importFichierRgpd` | `isAuthenticated()` |
| `GET` | `/importFichierRgpd/export` | `exportExcel` | `isAuthenticated()` |

## HistorisationController

Source : [HistorisationController.java](../../src/main/java/com/minds/rgpd/web/controllers/HistorisationController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/traitements/{id}/historique` | `getHistoriqueTraitement` | `isAuthenticated()` |
| `POST` | `/traitements/{id}/historique` | `ajouterHistoriqueTraitement` | `isAuthenticated()` |
| `GET` | `/registre/historique` | `getHistoriqueRegistre` | `isAuthenticated()` |
| `POST` | `/registre/historique` | `ajouterHistoriqueRegistre` | `isAuthenticated()` |

## PreconisationController

Source : [PreconisationController.java](../../src/main/java/com/minds/rgpd/web/controllers/PreconisationController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/preconisations` | `getPreconisations` | `hasAnyRole('USER', 'ADMIN')` |
| `GET` | `/preconisations/{id}` | `getPreconisation` | `hasAnyRole('USER', 'ADMIN')` |
| `POST` | `/preconisations` | `postPreconisation` | `hasRole('ADMIN')` |
| `PUT` | `/preconisations/{id}` | `putPreconisation` | `hasRole('ADMIN')` |
| `DELETE` | `/preconisations/{id}` | `delete` | `hasRole('ADMIN')` |

## ProfilController

Source : [ProfilController.java](../../src/main/java/com/minds/rgpd/web/controllers/ProfilController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/profils` | `getProfils` | `isAuthenticated()` |

## TraitementController

Source : [TraitementController.java](../../src/main/java/com/minds/rgpd/web/controllers/TraitementController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/traitements` | `getTraitements` | Authentifié (global) |
| `GET` | `/traitements/{id}` | `getTraitement` | Authentifié (global) |
| `POST` | `/traitements` | `postTraitement` | Authentifié (global) |
| `PUT` | `/traitements/{id}` | `putTraitement` | Authentifié (global) |
| `GET` | `/traitements/nextId` | `getNextIdFonctionnel` | Authentifié (global) |
| `DELETE` | `/traitements/duplicates` | `deleteDuplicateTraitements` | Authentifié (global) |
| `DELETE` | `/traitements/{id}` | `delete` | `hasRole('ADMIN')` |

## UtilisateurController

Source : [UtilisateurController.java](../../src/main/java/com/minds/rgpd/web/controllers/UtilisateurController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/utilisateurs` | `rechercher` | `isAuthenticated()` |
| `GET` | `/utilisateurs/roles` | `listerRoles` | `isAuthenticated()` |
| `GET` | `/utilisateurs/{id}` | `getUtilisateurParId` | `isAuthenticated()` |
| `POST` | `/utilisateurs` | `creer` | `hasAnyRole('SUPERADMIN', 'ADMIN')` |
| `PUT` | `/utilisateurs/{id}` | `modifier` | `hasAnyRole('SUPERADMIN', 'ADMIN')` |
| `DELETE` | `/utilisateurs/{id}` | `supprimer` | `hasAnyRole('SUPERADMIN', 'ADMIN')` |

## ViolationController

Source : [ViolationController.java](../../src/main/java/com/minds/rgpd/web/controllers/ViolationController.java).

| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |
| --- | --- | --- | --- |
| `GET` | `/violations` | `getViolations` | Authentifié (global) |
| `GET` | `/violations/{id}` | `getViolation` | Authentifié (global) |
| `POST` | `/violations` | `postViolation` | Authentifié (global) |
| `PUT` | `/violations/{id}` | `putViolation` | Authentifié (global) |
| `DELETE` | `/violations/{id}` | `delete` | `hasRole('ADMIN')` |
