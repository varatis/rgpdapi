# Le guide RGPD API

<div class="hero" markdown>
## Comprendre. Développer. Exploiter.

Une documentation pour passer du premier jour à une contribution maîtrisée sur l’API SaaS RGPD.
</div>

**Référence de lecture :** code du dépôt au commit initial `42e7a389` · **Revue :** 15 septembre 2026 · **Langue :** français.

## Votre parcours

| Vous arrivez sur le projet… | Commencez ici | Puis approfondissez |
| --- | --- | --- |
| Développement backend | [Premier démarrage](guides/02-demarrage.md) | [Architecture](guides/03-architecture.md), [tests](guides/07-developpement.md) |
| Développement frontend / intégration | [API et exemples](guides/04-api.md) | [Routes](reference/routes.md), [DTO](reference/dto.md), [sécurité](guides/05-securite.md) |
| Architecture / lead technique | [Architecture](guides/03-architecture.md) | [Données](reference/donnees.md), [vigilance](guides/09-vigilance.md) |
| DevOps / exploitation | [Configuration](parametrage.md) | [CI/CD et exploitation](guides/08-exploitation.md) |
| Produit / fonctionnel / QA | [Vue métier](guides/01-produit.md) | [Import-export](guides/06-import-export.md), [tests](guides/07-developpement.md) |

## Le projet en une minute

L’application expose une API REST de gestion de clients SaaS, établissements, traitements de données personnelles, préconisations, violations et demandes. Elle importe et exporte des registres Excel, historise certaines actions et délègue les comptes utilisateurs à Keycloak.

C’est **un seul service Spring Boot**, construit en JAR et déployable en conteneur ; ce dépôt ne contient pas le frontend. Les données métier résident dans PostgreSQL, les identités dans Keycloak et les classeurs archivés dans un répertoire disque configurable.

| Repère | Valeur constatée |
| --- | --- |
| Runtime | Java 21, Spring Boot 4.0.5 |
| Persistance | Spring Data JPA, PostgreSQL, Flyway |
| Interface | REST JSON, multipart et fichiers Excel/images |
| Contrat vivant | `/v3/api-docs` et `/swagger-ui.html` |
| Authentification | OAuth2 Resource Server, JWT Keycloak |
| Livraison | Maven, Jenkins, Docker, Helm/Kubernetes |

## Une première semaine réussie

1. **Jour 1 — Se repérer :** lire la vue métier, identifier le client SaaS et distinguer UUID / identifiant fonctionnel.
2. **Jour 2 — Exécuter :** disposer d’un environnement isolé, démarrer PostgreSQL et l’API, effectuer un appel authentifié en lecture.
3. **Jour 3 — Suivre un flux :** parcourir `TraitementController` → service → mapper / résolveurs → repository.
4. **Jour 4 — Tester :** exécuter un test unitaire ciblé, puis les tests avec PostgreSQL ; comprendre les limites de la sécurité simulée.
5. **Jour 5 — Contribuer :** proposer un changement restreint avec tests, migration si nécessaire et mise à jour documentaire.

## Comment lire les avertissements

- **Constaté :** observable dans les sources, sans supposer le comportement d’un environnement distant.
- **À valider :** dépend d’un runtime, d’un réseau, des droits SSO ou de l’infrastructure non disponibles pendant la revue.
- **Recommandation :** amélioration proposée, pas une fonctionnalité déjà livrée.

!!! warning "À lire avant un import ou une administration client"
    Certaines opérations sont destructives. L’isolation entre clients doit être revue endpoint par endpoint ; un JWT valide n’établit pas à lui seul le droit d’accéder à toutes les données. Voir les [points de vigilance](guides/09-vigilance.md).

## Références rapides

- [Catalogue des routes](reference/routes.md) : verbes, chemins, méthodes Java et contrôles déclarés.
- [Modèle et migrations](reference/donnees.md) : relations métier, schéma SQL et historique Flyway.
- [Objets échangés](reference/dto.md) : accès aux structures et contraintes des DTO.
- [Index du code](reference/code.md) : inventaire navigable des sources, tests et scripts.
- [Maintenance documentaire](guides/10-documentation.md) : génération, validation, limites et archives.
