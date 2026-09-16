# Identité, sécurité et cloisonnement

[Accueil](../index.md) / Développer

## Deux connexions Keycloak à ne pas confondre

| Usage | Principal | Configuration | Composants |
| --- | --- | --- | --- |
| Authentifier les requêtes entrantes | Utilisateur porteur d’un JWT | Issuer, JWK, client de lecture des rôles | `SecurityConfig`, `JwtAuthConverter` |
| Administrer utilisateurs / groupes / rôles | Compte de service OAuth2 | Base URL, realm, client ID et secret admin | `IdentityGateway`, `KeycloakIdentityGateway`, `KeycloakAdminClient` |

Un jeton valide ne prouve pas que les appels Admin API fonctionnent ; l’inverse est également vrai. La base de données métier peut être disponible alors que la gestion des comptes ne l’est pas.

## Chaîne de sécurité HTTP

Hors profil `test`, `SecurityConfig` active la sécurité de méthode, désactive CSRF et utilise une session **STATELESS**. Toutes les routes non exemptées demandent une authentification.

Les exemptions sont `/actuator/**`, Swagger, `/v3/api-docs` et sous-chemins, `/swagger-resources/**`, `/webjars/**`. La configuration expose tous les endpoints Actuator disponibles et demande les détails de santé. **Recommandation :** restreindre l’exposition et protéger l’accès management avant une mise en production.

### Rôles et claims

`JwtAuthConverter` conserve les autorités de scopes et cumule :

1. `resource_access.<resourceId>.roles`, où `resourceId` vient de `application.security.jwt.resource-id` (défaut `minds-saas-rgpd`) ;
2. `client_groups` si la claim est une collection de chaînes.

Chaque valeur est normalisée en `ROLE_` + majuscules, avec suppression d’un `/` initial. Le nom de l’authentification vient de `preferred_username`.

```json
{
  "preferred_username": "personne.test",
  "name": "Personne Test",
  "email": "personne@example.invalid",
  "resource_access": {
    "minds-saas-rgpd": { "roles": ["user"] }
  },
  "client_groups": ["Organisation de test"]
}
```

Extrait illustratif uniquement : ce n’est pas un JWT signé. Le client métier est recherché **par nom exact**. `AccessUserInformation` ne retire pas `/clients/` du nom ; un mapper Keycloak qui expose des chemins complets doit être vérifié.

!!! warning "Groupes et rôles sont cumulés"
    Une valeur de groupe est aussi convertie en autorité. Faire auditer les collisions entre noms de groupes et rôles privilégiés ; ne pas traiter ce mécanisme comme une séparation étanche des rôles et de l’appartenance métier.

### Client unique du jeton

`AccessUserInformation.getClientUniqueDuJeton` accepte une collection ou une chaîne contenant les groupes. Il rejette zéro ou plusieurs clients par `IllegalArgumentException` (`400`). Il est utilisé notamment pour la liste des traitements, l’export et l’historique du registre. Il ne constitue pas un filtre global sur les repositories.

## Autorisations : lire contrôleur ET service

Le [catalogue des routes](../reference/routes.md) expose les annotations de contrôleurs. Une restriction supplémentaire existe dans `UtilisateurServiceImpl.rechercher` : malgré `isAuthenticated()` sur le contrôleur, la recherche demande `ADMIN` ou `SUPERADMIN` dans le service.

- Écritures client / logo : `SUPERADMIN`.
- Administration utilisateurs et établissements : `ADMIN` ou `SUPERADMIN`.
- Préconisations : lecture `USER` ou `ADMIN`, écriture `ADMIN`.
- Suppressions individuelles de traitements / violations : `ADMIN`.
- Plusieurs écritures et la suppression de doublons de traitements ne déclarent pas de rôle supplémentaire au contrôle d’authentification global.

Il n’y a pas de hiérarchie de rôles déclarée qui ferait automatiquement hériter `SUPERADMIN` d’`ADMIN` ou d’`USER`.

## Cloisonnement entre clients : limite structurante

**Constaté :** la liste des traitements construit une Specification avec le nom client du JWT. En revanche, les lectures/modifications par identifiant fonctionnel de traitement et certaines autres opérations utilisent des recherches sans filtre de client du jeton. L’import et son aperçu utilisent le nom client du fichier, pas le client du JWT. La liste des clients et celle des demandes utilisent `findAll()`.

Il est donc incorrect d’affirmer que le SaaS dispose d’un cloisonnement systématique garanti par la couche de sécurité. Un audit d’autorisations objet par objet est nécessaire avant exposition de données de plusieurs organisations.

**Tests à ajouter/revalider :** compte client A lisant/modifiant/supprimant un objet B, compte A important un fichier nommé B, administrateur A affectant un utilisateur à B, référence d’établissement B fournie à un traitement A. Ces tests doivent utiliser la vraie chaîne de sécurité, pas seulement le profil `test` permissif.

## Port d’identité et adaptateur

Le port `business/identity/IdentityGateway` exprime les opérations indépendamment du fournisseur. L’adaptateur REST :

- obtient un jeton par `client_credentials` et le garde en mémoire jusqu’à expiration avec marge ;
- tente un renouvellement/rejeu lorsqu’une erreur contient `401` ;
- parcourt les listes avec `first/max`, taille de page 200 et garde-fou de 100 pages dans le client ;
- reconstitue les rôles via les utilisateurs associés aux rôles clients, et les appartenances via sous-groupes et membres ;
- gère création/modification/suppression d’utilisateurs, affectation de groupe et remplacement des rôles clients ;
- remonte les erreurs via les exceptions d’identité, converties notamment en `502`.

Le `RestTemplate` d’administration est construit sans timeouts explicites dans `KeycloakConfig`. Les volumes élevés et les indisponibilités SSO doivent donc faire l’objet de tests de charge et de résilience. Le garde-fou de pagination n’est pas une garantie de complétude au-delà de sa limite.

## Préparer un realm de développement

À réaliser par l’administrateur SSO, selon les règles de l’entreprise :

1. Créer ou réserver un realm isolé (défaut du code : `minds-rgpd`).
2. Créer un client d’administration confidentiel avec authentification client et service account ; utiliser le flux `client_credentials`.
3. Attribuer les permissions d’administration nécessaires aux opérations utilisateurs, groupes, clients et rôles. Les rôles `realm-management` exacts dépendent de la version/configuration Keycloak : vérifier les permissions effectives plutôt que recopier une liste supposée universelle. Ne pas donner un rôle global d’administration par facilité.
4. Créer le client applicatif (défaut `minds-saas-rgpd`) et les rôles réellement requis (`user`, `admin`, `superadmin` selon les comptes de test).
5. Créer le groupe parent `clients` ; les groupes métier se trouvent sous `/clients/{nom}`.
6. Configurer la claim `client_groups`, `preferred_username`, les claims de profil et `resource_access` attendus ; tester les jetons produits.
7. Injecter le secret admin côté backend seulement ; contrôler issuer/JWK et la chaîne de certificats.

Le dépôt ne fournit pas d’export complet de realm. Le provisionnement reste à valider avec l’équipe IAM.

## Synchronisation client : comportement destructif actuel

!!! danger "Une mise à jour client n’est pas un simple renommage Keycloak"
    `ClientServiceImpl.createClient` **et** `updateClient` appellent `synchroniserGroupeClient` avec le nom sauvegardé. Si ce groupe existe, le service tente de supprimer tous ses utilisateurs, supprime le groupe, puis le recrée vide. Même une mise à jour conservant le nom peut donc supprimer les comptes du groupe.

En cas de changement de nom, le service travaille sur le **nouveau nom** ; il ne transmet pas l’ancien nom à une opération de renommage. L’ancien groupe peut rester présent. La suppression d’un client tente aussi de supprimer ses utilisateurs Keycloak puis son groupe avant de supprimer l’entité SQL. Les échecs de suppression individuelle d’utilisateur sont journalisés et la boucle continue.

Aucun rollback PostgreSQL ne compense ces appels HTTP. **Recommandation prioritaire :** définir une synchronisation non destructive, conserver l’identifiant du groupe et concevoir réconciliation/compensation. Toute modification de ce comportement exige tests et validation métier.

## Profils et TLS

Les profils `dev`, `int`, `demo`, `valid` créent un décodeur avec JWK codée en dur et installent un trust manager ainsi qu’un hostname verifier permissifs. `SslConfig` a ses annotations de configuration commentées, mais cela **n’annule pas** ce comportement dans `SecurityConfig`.

Recommandation : utiliser la chaîne de confiance de l’entreprise, supprimer le contournement TLS, utiliser les propriétés configurées et vérifier explicitement les validations issuer/audience nécessaires. Cette documentation n’applique pas ces changements au code.

## CORS et données sensibles

`application.allowed-origins` est une liste explicite ; credentials autorisés. Méthodes : GET, POST, PUT, PATCH, DELETE, OPTIONS. Headers : Authorization, Content-Type, Cache-Control. CORS protège des usages navigateur, **pas** de requêtes serveur à serveur ni d’un client authentifié non autorisé sur un objet.

Ne pas journaliser de jetons, mots de passe, classeurs ou payloads personnels complets. Les réglages DEBUG du profil dev et les messages d’erreur doivent être réévalués sur des données réelles.
