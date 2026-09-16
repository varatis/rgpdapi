# RGPD API

**API SaaS de gestion des registres RGPD** — Java 21 · Spring Boot 4.0.5 · PostgreSQL 17 · Keycloak.

## La documentation, par où commencer ?

➡️ **[Ouvrir le guide du projet](docs/index.md)**

| Votre objectif | Guide |
| --- | --- |
| Comprendre le produit et le vocabulaire | [Vue métier](docs/guides/01-produit.md) |
| Installer et démarrer l’API | [Premier démarrage](docs/guides/02-demarrage.md) |
| Comprendre l’architecture | [Composants et flux](docs/guides/03-architecture.md) |
| Consommer les endpoints | [API et exemples](docs/guides/04-api.md) · [Catalogue complet](docs/reference/routes.md) |
| Comprendre les droits | [Identité et sécurité](docs/guides/05-securite.md) |
| Tester ou contribuer | [Guide de développement](docs/guides/07-developpement.md) |
| Configurer et exploiter | [Paramétrage](docs/parametrage.md) · [CI/CD](docs/guides/08-exploitation.md) |

> **Avant toute écriture sur un environnement partagé :** lire les [points de vigilance](docs/guides/09-vigilance.md). L’import remplace des données du registre ; la synchronisation d’un client peut supprimer des utilisateurs Keycloak. Le profil `dev` pointe par défaut sur un SSO distant.

## Démarrage en bref

Prérequis : **JDK 21**, **Maven** (3.9.5 utilisé en CI), **Docker Engine actif + Compose v2**, accès à un **realm Keycloak de développement isolé**.

```bash
# Après configuration des accès et du secret Keycloak : voir le guide de démarrage.
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Sur votre poste : [Swagger](http://localhost:8080/swagger-ui.html) · [OpenAPI JSON](http://localhost:8080/v3/api-docs) · [Health](http://localhost:8080/actuator/health).

```bash
mvn test                          # Tests Surefire ; certains nécessitent Docker
mvn verify -PIntegrationTests     # Inclut les tests *IT avec Failsafe
```

## Lire la documentation comme un site

La documentation Markdown reste lisible directement dans Git. Pour la navigation, la recherche, les schémas et le thème clair/sombre :

```bash
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements-docs.txt
.venv/bin/mkdocs serve -a 0.0.0.0:8000
```

Ouvrir `http://localhost:8000` sur votre poste. Construction statique : `.venv/bin/mkdocs build --strict` → `target/documentation/`.

**Périmètre :** documentation fondée sur le code du dépôt, référence initiale `42e7a389`, revue du 15 septembre 2026. Elle ne constitue ni une certification RGPD, ni une validation de production. [Méthode et entretien](docs/guides/10-documentation.md).
