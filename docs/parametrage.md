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