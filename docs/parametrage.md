# Paramétrage de l'application

Le paramétrage de l'application se fait à l'aide des fichiers application.yaml et application-{profile}.yaml
Avec {profile} qui prend la valeur du profil spring utilisé.

## Fichier application.yaml

Le fichier application.yaml contient le paramétrage par défaut, mais ces propriétés peuvent être surchargées dans le
fichier applications-{profile}.yaml.
Les propriétés pouvant être surchargées sont les suivantes :

| Clé                                 | Valeur par défaut      | Description                                                                                                            |
|-------------------------------------|------------------------|------------------------------------------------------------------------------------------------------------------------|
| spring.datasource.driver-class-name | org.h2.Driver          | Nom de la classe du pilote JDBC permettant de communiquer avec la base de données.                                     |
| spring.flyway.locations             | classpath:db/migration | Emplacement des scripts SQL lancé par Flyway au démarrage de l'application.                                            |
| spring.flyway.baseline-on-migrate   | 'true'                 | Indique s'il faut appeler automatiquement la ligne de base lors de l'execution de la migration sur un schéma non vide. |
| server.port                         | 8080                   | Port par défaut utilisé par l'application                                                                              |

## fichier application-{profile}.yaml

| Clé                         | Variable          | Type   | Description                                                  |
|-----------------------------|-------------------|--------|--------------------------------------------------------------|
| spring.datasource.url       | DATABASE_URL      | String | URL de la base de données.                                   |
| spring.datasource.username  | DATABASE_USERNAME | String | Nom d'utilisateur d'accès à la base de données.              |
| spring.datasource.password  | DATABASE_PASSWORD | String | Mot de passe d'accès à la base de données.                   |
| spring.flyway.user          | DATABASE_USERNAME | String | Nom d'utilisateur d'accès à la base de données.              |
| spring.flyway.password      | DATABASE_PASSWORD | String | Mot de passe d'accès à la base de données.                   |
| application.allowed-origins | ALLOWED_ORIGIN    | String | Adresse authorisée à contacter le microservice (CORS pilicy) |

## Synchronisation Keycloak (identités, rôles, clients)

Les utilisateurs, leurs rôles et leur rattachement à un client ne sont pas stockés par l'API : ils sont lus et écrits en
direct dans Keycloak via son Admin REST API. Le seul référentiel de ces données est donc le Keycloak du projet ; la table
`UTILISATEUR` de PostgreSQL n'est plus alimentée. Le paramétrage suit le même principe que ci-dessus.

| Clé                                              | Variable                            | Type    | Défaut                  | Description                                                                        |
|--------------------------------------------------|-------------------------------------|---------|-------------------------|------------------------------------------------------------------------------------|
| application.keycloak.enabled                     | KEYCLOAK_SYNC_ENABLED               | boolean | `true`                  | `false` remplace la passerelle par une implémentation neutre (lecture vide, écriture 503). |
| application.keycloak.base-url                    | KEYCLOAK_BASE_URL                   | String  | `https://sso.minds.k8s/auth` | Racine du serveur Keycloak, `/auth` inclus le cas échéant.                          |
| application.keycloak.realm                       | KEYCLOAK_REALM                      | String  | `minds-rgpd`            | Realm ciblé par l'Admin REST API.                                                  |
| application.keycloak.admin-client-id             | KEYCLOAK_ADMIN_CLIENT_ID            | String  | `minds-saas-rgpd-admin` | Client de service utilisé pour l'appel à l'Admin REST API.                          |
| application.keycloak.admin-client-secret         | KEYCLOAK_ADMIN_CLIENT_SECRET        | String  | (vide)                  | Secret du client de service. À placer dans Vault (`keycloakadminsecret`).           |
| application.keycloak.resource-client-id          | KEYCLOAK_RESOURCE_CLIENT_ID         | String  | `minds-saas-rgpd`       | Client porteur des rôles applicatifs lus dans `resource_access` par le jeton.       |
| application.keycloak.taille-page                 | APPLICATION_KEYCLOAK_TAILLE_PAGE        | int     | `100`                   | Taille des pages de l'Admin REST API lors des listes.                              |
| application.keycloak.nombre-max-resultats        | APPLICATION_KEYCLOAK_NOMBRE_MAX_RESULTATS | int | `2000`                  | Garde-fou de récupération : au-delà, la liste est tronquée et un WARNING est tracé. |
| application.keycloak.desactiver-verification-ssl | KEYCLOAK_DESACTIVER_VERIFICATION_SSL | boolean | `false`                 | Désactive uniquement la vérification du certificat pour les appels d'administration. |
| application.security.jwt.resource-id             | APPLICATION_SECURITY_JWT_RESOURCE_ID | String  | `minds-saas-rgpd`       | Client dont les rôles sont relus dans le jeton (déjà utilisé par `JwtAuthConverter`). |

### Prérequis côté Keycloak

Le realm doit exposer :

- un client `minds-saas-rgpd` (celui du resource server) portant les rôles applicatifs, par exemple `admin` et `user` ;
- un client de service confidentiel `minds-saas-rgpd-admin`, avec `serviceAccountsEnabled`, et les rôles de gestion du
  realm : `view-users`, `manage-users`, `view-clients`, `manage-client-roles`, `view-groups`, `manage-groups` ;
- un mapper de type `Group Membership` ajouté au client `minds-saas-rgpd`, exposant la claim `client_groups` avec les
  **noms** de groupes (option « full group path » désactivée) : c'est ce claim qui permet à l'API de retrouver le client
  d'un utilisateur, comme le fait déjà `FichierController`.

Un groupe racine par client métier porte exactement le `nom` de la ligne `CLIENT` : créer, renommer ou supprimer un
client crée, renomme ou supprime donc ce groupe, et la suppression emporte les utilisateurs qui en sont membres.
