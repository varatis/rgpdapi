# Développer, tester et contribuer

[Accueil](../index.md) / Développer

## Stratégie de tests présente dans le dépôt

| Niveau | Où regarder | Ce qui est testé | Dépendances |
| --- | --- | --- | --- |
| Unitaire services/utilitaires | `business/**/**Test.java` | Cas métier, résolveurs, diff, formats, mappers | JUnit, Mockito selon le test |
| Adaptateur identité | `KeycloakAdminClientTest`, `KeycloakIdentityGatewayTest` | REST simulé, jetons, pagination, rôles/groupes | MockRestServiceServer / Mockito ; pas de Keycloak réel |
| Repository / Specification | `persistence/**/*Test.java` | Recherche et persistance JPA | PostgreSQL via Testcontainers |
| Intégration HTTP | `integrationtest/**/*IT.java` | Contrôleurs, import, logo, référentiels, traitements | Application Spring et PostgreSQL de test |

L’[index des tests](../reference/code.md) donne la liste réelle ; ne pas utiliser les anciens exemples de classes absentes du dépôt.

## Commandes Maven

```bash
# Compiler et générer les mappers
mvn compile

# Tests sélectionnés par Surefire (inclut des tests JPA qui ont besoin de Docker)
mvn test

# Un test ciblé sans base réelle pour cette classe
mvn test -Dtest=TraitementDiffTest
mvn test -Dtest=KeycloakAdminClientTest

# Cycle complet avec les *IT via Failsafe
mvn verify -PIntegrationTests

# Un test d’intégration ciblé (les tests Surefire restent aussi dans le cycle)
mvn verify -PIntegrationTests -Dit.test=ClientLogoIT
```

Le profil Maven **`IntegrationTests`** active Failsafe avec l’inclusion `**/integrationtest/**/*IT.java`. `mvn test` ne lance donc pas automatiquement tous les `*IT`. Ne pas confondre ce profil Maven avec le profil **Spring `test`** sélectionné par les classes de test.

Les scripts CI traduisent `SKIP_TU` et `SKIP_TI` en options Maven ; leur interaction avec `skipTests=false` dans Failsafe doit être vérifiée avant de considérer les flags comme une garantie de sélection.

## Base et sécurité de test

`AbstractITSpring` démarre l’application sur un port aléatoire, active `test`, importe `TestContainersConfiguration` et utilise `@DirtiesContext`. Le conteneur est PostgreSQL `17-alpine`, avec `@ServiceConnection` pour la liaison à Spring. `withReuse(true)` est demandé ; la réutilisation effective dépend aussi de la configuration Testcontainers locale.

**Docker Engine est requis**, même si Testcontainers crée et arrête les conteneurs automatiquement. Aucun Keycloak réel n’est nécessaire pour les tests utilisant `NoopIdentityGateway`.

!!! warning "Ces IT ne prouvent pas la sécurité de production"
    `TestSecurityConfig` désactive CSRF/CORS et autorise toutes les requêtes. `SecurityConfig`, qui active la sécurité de méthode, est exclue par le profil test. Une réussite des IT ne démontre donc ni la validation des JWT, ni les droits, ni le cloisonnement réel. Ajouter des tests dédiés avec la chaîne de sécurité effective pour ces garanties.

Les tests utilisent un répertoire de fichiers sous `src/test/resources/rgpdFile`. Vérifier `git status` après les scénarios d’import afin de ne pas publier de données ou modifications de fixture involontaires.

## Rapports et preuves

| Rapport | Chemin usuel |
| --- | --- |
| Surefire | `target/surefire-reports/` |
| Failsafe | `target/failsafe-reports/` |
| JaCoCo | `target/site/jacoco/index.html` |
| Documentation statique | `target/documentation/` |

Le rapport JaCoCo est lié à la phase `test` dans le POM : ne pas annoncer une couverture consolidée incluant les IT sans vérifier la phase de génération et le fichier d’exécution utilisé. Aucun pourcentage de couverture ou résultat de tests applicatifs n’est affirmé par cette documentation.

## Ajouter une fonctionnalité pas à pas

1. **Définir le comportement** : données concernées, rôles, client autorisé, erreurs attendues et idempotence.
2. **Fixer le contrat** : route, verbe, DTO d’entrée/sortie, validation et exemples ; ne pas exposer directement une entité JPA.
3. **Implémenter le service** : cas d’usage, frontière transactionnelle, résolution des références, historique si pertinent.
4. **Adapter la persistance** : repository/Specification et nouvelle migration Flyway si nécessaire.
5. **Mettre à jour les mappers** : tester cycles et mise à jour d’une entité managée ; éviter de remplacer arbitrairement des associations.
6. **Sécuriser à deux niveaux** : rôle de l’action **et** appartenance de l’objet au client autorisé.
7. **Tester** : chemin nominal, entrée invalide, ressource absente, doublon, accès inter-client, échec externe et rollback.
8. **Documenter** : OpenAPI runtime, guide de domaine, paramètres éventuels et références statiques régénérées.

## Modifier le schéma

- Ajouter une migration versionnée dans `src/main/resources/db/migration`.
- Ne pas modifier une migration déjà appliquée sur un environnement partagé : les checksums Flyway doivent rester cohérents.
- Vérifier contraintes, index, defaults, nullabilité et reprise des données.
- Tester depuis une base vide **et** depuis un schéma/dataset représentatif de la version précédente.
- Pour une suppression/renommage, prévoir une migration compatible avec le déploiement progressif et une restauration testée.
- Ne pas exécuter `clean`, `repair` ou une correction SQL manuelle comme première réponse à un échec Flyway.

La séquence présente passe de V9 à V11 ; l’absence de V10 n’est pas, à elle seule, une erreur Flyway. [Inventaire des migrations](../reference/donnees.md).

## Conventions observées et recommandations

**Observé :** noms métier français, packages en couches, interfaces de services, implémentations avec injection constructeur, DTO records, annotations de mapping. Le package `business/Imports` comporte une majuscule : respecter le chemin existant tant qu’un refactoring contrôlé n’est pas décidé.

**Recommandé :** limiter les changements transverses, utiliser des noms explicites UUID/numéro fonctionnel, proscrire les données personnelles dans les fixtures nouvelles, éviter les exceptions techniques exposant des informations internes, documenter les décisions structurantes via ADR.

## Checklist de revue

- [ ] Fonctionnement et erreurs explicités ; impact destructif annoncé.
- [ ] Filtrage client vérifié sur lecture, écriture, suppression et références.
- [ ] DTO validé ; bornes de pagination/fichier examinées.
- [ ] Migration immuable et stratégie de compatibilité validées.
- [ ] Appels Keycloak non assimilés à une transaction SQL.
- [ ] Tests adaptés, rapports consultés, limites annoncées.
- [ ] Aucun secret, export réel ou classe générée dans le diff.
- [ ] Documentation et liens vérifiés ; modifications du code effectuées sous `src/main/java`, pas dans les copies racine.
