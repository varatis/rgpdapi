# Premier démarrage

[Accueil](../index.md) / Prendre ses repères

## Objectif

Obtenir une API locale connectée à PostgreSQL et à un **Keycloak de développement isolé**, puis réussir une requête de lecture. Ce dépôt ne contient ni Maven Wrapper, ni realm Keycloak importable, ni frontend.

!!! warning "Le profil dev utilise un SSO distant"
    `application-dev.yaml` active la synchronisation Keycloak et cible une adresse interne distante. Ne lancez pas d’opération d’administration ou d’import avec un compte réel pour « essayer ». Demandez à l’équipe un realm et des données dédiés aux essais.

## 1. Vérifier les outils

```bash
java -version        # JDK 21
mvn -version         # doit utiliser le même JDK ; Maven 3.9.5 utilisé en CI
docker version       # client ET serveur doivent répondre
docker compose version
```

L’import du `pom.xml` dans l’IDE doit activer le traitement des annotations Lombok et MapStruct. Les classes générées se trouvent dans `target/generated-sources/annotations/` après compilation ; ne les modifiez pas à la main.

**Accès à obtenir auprès de l’équipe :** realm de test, client OAuth2 applicatif, compte de test avec rôles, client d’administration et secret via le canal sécurisé de l’entreprise. Pour les scripts CI, il faut en plus les accès réseau au registre interne et aux outils d’intégration.

## 2. Comprendre la base locale

Le fichier [docker-compose.yml](../../docker-compose.yml) fournit :

| Service | Accès depuis votre poste | Accès depuis le réseau Compose |
| --- | --- | --- |
| PostgreSQL `postgres:17-alpine` | `localhost:5434`, base `docker` | `postgres:5432` |
| PgAdmin | `http://localhost:8888` | service `pgadmin`, port 80 |

Les identifiants `docker` / `docker` et le compte PgAdmin `docker@docker.com` / `docker` sont **uniquement les valeurs de démonstration locales présentes dans Compose**. Ils ne doivent pas être réutilisés ailleurs. Les données sont dans les volumes `postgresdata_dev` et `pgadmindata_dev` (préfixés par le projet Compose à l’exécution).

Spring Boot dispose de `spring-boot-docker-compose` : avec le profil `dev`, il peut lancer les services du fichier racine. **Le daemon Docker doit déjà être démarré** ; l’automatisation ne démarre pas Docker Engine lui-même.

Pour une gestion explicite :

```bash
docker compose up -d
docker compose ps
```

Dans PgAdmin, ajouter un serveur : nom libre ; hôte **`postgres`**, port **5432**, base de maintenance **`docker`**, utilisateur et mot de passe locaux. PgAdmin est dans un conteneur : `localhost:5434` y désignerait le mauvais hôte.

## 3. Paramétrer Keycloak et les secrets

Ne créez pas de fichier de secrets suivi par Git. Un fichier `.env` n’est **pas** chargé automatiquement par Spring Boot et n’est actuellement **pas ignoré** par le `.gitignore` du dépôt. Préférez les variables injectées par l’IDE ou le gestionnaire de secrets de votre poste.

```bash
# Lecture silencieuse, pour ne pas écrire le secret dans l’historique du shell.
read -rsp 'Secret Keycloak de développement : ' KEYCLOAK_ADMIN_CLIENT_SECRET
printf '\n'
export KEYCLOAK_ADMIN_CLIENT_SECRET
```

Pour un realm isolé, configurer les propriétés ci-dessous avec ses URL et identifiants réels :

```bash
export KEYCLOAK_BASE_URL='https://sso-dev.example.invalid'
export KEYCLOAK_REALM='rgpd-dev'
export KEYCLOAK_ADMIN_CLIENT_ID='rgpd-admin-dev'
export KEYCLOAK_RESOURCE_CLIENT_ID='rgpd-app-dev'
export APPLICATION_SECURITY_JWT_RESOURCE_ID='rgpd-app-dev'
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='https://sso-dev.example.invalid/realms/rgpd-dev'
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI='https://sso-dev.example.invalid/realms/rgpd-dev/protocol/openid-connect/certs'
```

Ces adresses sont des **exemples non joignables à remplacer**, pas une infrastructure fournie.

!!! danger "Limite actuelle du profil dev"
    Le bean `SecurityConfig.jwtDecoder()` des profils `dev`, `int`, `demo`, `valid` utilise une URL JWK codée en dur et installe une stratégie TLS permissive. Surcharger seulement les variables ci-dessus ne redirige donc pas ce décodeur. Pour un SSO isolé, utilisez la configuration sans ces profils décrite ci-dessous, ou faites corriger ce bean avant usage. Ne désactivez pas TLS pour contourner un problème de certificat.

## 4. Choisir le mode de lancement

### Mode A — Environnement dev de l’équipe

Seulement après validation des accès, du realm partagé et des risques signalés :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Le profil fournit la base Compose, l’origine `http://localhost:4200` et archive les fichiers dans `.`. Configurez de préférence un répertoire dédié avec `APPLICATION_FICHIER_UPLOAD_DIR` afin de ne pas déposer de classeurs dans le dépôt.

### Mode B — Configuration explicite, recommandé pour un realm isolé

Ne pas activer `dev`, `int`, `demo` ou `valid`. Spring peut alors configurer son décodeur à partir des propriétés OAuth2. Démarrer Compose manuellement et injecter toutes les propriétés requises :

```bash
docker compose up -d
export SPRING_DOCKER_COMPOSE_ENABLED=false
export DATABASE_URL='jdbc:postgresql://localhost:5434/docker'
export DATABASE_USERNAME='docker'
export DATABASE_PASSWORD='docker'
export APPLICATION_ALLOWED_ORIGINS='http://localhost:4200'
mkdir -p "$HOME/rgpd-local-uploads"
export UPLOAD_DIR="$HOME/rgpd-local-uploads"
# Configurer aussi Keycloak, issuer et JWK, comme à l’étape 3.
mvn spring-boot:run
```

Ce mode est une procédure déduite de la configuration, **à valider avec le realm de votre équipe**. Il n’a pas été exécuté durant cette revue documentaire.

### Mode JAR

```bash
mvn package
# Injecter la configuration explicite ci-dessus ; ne pas compter sur application-dev.yaml.
java -jar target/minds-rgpd-api-1.0-SNAPSHOT.jar
```

Le plugin JAR exclut les fichiers `application-dev.*`. Le profil `IntegrationTests` n’est pas activé par `mvn package` ; cette commande ne garantit pas l’exécution des `*IT`.

## 5. Vérifier le démarrage

1. Lire les logs : connexion PostgreSQL, exécution Flyway, démarrage HTTP port 8080.
2. Consulter `GET /actuator/health` ; une réponse ne valide pas à elle seule Keycloak ni les droits métier.
3. Ouvrir `/swagger-ui.html` et `/v3/api-docs`.
4. Obtenir un jeton par le parcours de connexion autorisé de l’équipe. Le secret du compte de service Keycloak **n’est pas** un jeton utilisateur.
5. Essayer `GET /clients` avec un JWT de test, puis la liste des traitements avec une claim client unique.

```bash
read -rsp 'JWT de test : ' TOKEN; printf '\n'
export TOKEN
curl --fail-with-body -H "Authorization: Bearer $TOKEN" http://localhost:8080/clients
unset TOKEN
```

Ne copiez pas les JWT dans les tickets, captures d’écran ou journaux partagés.

## 6. Arrêter proprement

Arrêter l’API avec `Ctrl+C`. Si vous avez lancé Compose vous-même :

```bash
docker compose stop
```

**Ne pas utiliser `docker compose down -v` pour un simple arrêt** : l’option `-v` détruit les volumes. Aucune opération de réinitialisation n’est nécessaire pour suivre ce guide.

## Dépannage immédiat

| Symptôme | Première vérification |
| --- | --- |
| Maven utilise Java 17 | `JAVA_HOME`, JDK de l’IDE et sortie `mvn -version` |
| Docker introuvable / accès refusé | daemon actif et permissions du socket |
| Port 5434 ou 8888 occupé | autres services du poste ; maintenir Compose et configuration cohérents |
| Placeholder non résolu | profil choisi, `UPLOAD_DIR`, base, origine et secret |
| Erreur TLS / récupération JWK | chaîne de confiance et URL du décodeur effectif, pas seulement la variable |
| 400 « Aucun client associé… » | claim `client_groups` du JWT de test |
| 502 Keycloak | client d’administration, secret, realm et permissions du compte de service |

**Suite :** [Architecture](03-architecture.md) · [API et exemples](04-api.md).
