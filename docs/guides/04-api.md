# Consommer l’API

[Accueil](../index.md) / Développer

## Contrat de référence

Le contrat vivant est produit par Springdoc depuis l’application :

| Ressource | Chemin local |
| --- | --- |
| Swagger UI | `/swagger-ui.html` |
| OpenAPI JSON | `/v3/api-docs` |
| Santé | `/actuator/health` |
| Informations de build | `/actuator/info` (contenu à vérifier sur le runtime) |

Le [catalogue des routes](../reference/routes.md) inventorie les mappings du code. Les [DTO](../reference/dto.md) donnent accès aux structures de requête/réponse. Les fichiers de `docs/adr/openapi` sont des **archives**, pas une garantie de synchronisation avec l’API actuelle.

En Kubernetes, l’Ingress fourni route `/apims` et retire ce préfixe avant l’API. Une URL externe peut donc être `https://<hôte>/apims/traitements` ; le mapping Java reste `/traitements`. Vérifier l’Ingress réellement déployé.

## Conventions HTTP

- JWT dans `Authorization: Bearer <jeton>` ; pas de session applicative serveur.
- Corps JSON avec `Content-Type: application/json` ; logo et import en `multipart/form-data`, champ **`file`**.
- `LocalDate` : utiliser `AAAA-MM-JJ` en JSON et pour les filtres de dates.
- Listes paginées : paramètres Spring `page` (base 0), `size`, `sort=champ,asc|desc`.
- Ne pas supposer que toutes les listes sont paginées : clients, demandes, profils, historiques et utilisateurs renvoient des listes.
- `UtilisateurController` accepte un `Pageable`, mais ne l’applique pas : la réponse est une `List`, pas une `Page`.
- Les rôles ne forment pas une hiérarchie implicite : `SUPERADMIN` ne satisfait pas automatiquement `hasRole('ADMIN')`.

### Identifiants : le piège principal

| Opération | Identifiant attendu |
| --- | --- |
| GET / PUT `/traitements/{id}` | Entier `idFonctionnel` |
| GET / POST `/traitements/{id}/historique` | Entier `idFonctionnel` |
| DELETE `/traitements/{id}` | UUID technique |
| Clients, établissements, utilisateurs, demandes, violations, préconisations par ID | UUID |
| `/traitements/nextId` | Retourne un entier calculé `max + 1`, **pas une réservation** |

Ne réutilisez pas le même `{id}` dans un GET et un DELETE traitement sans identifier explicitement le champ concerné. Le calcul `nextId` ne protège pas contre deux créations simultanées.

## Recherches disponibles

| Liste | Filtres | Pagination par défaut du contrôleur |
| --- | --- | --- |
| Traitements | `nom`, `gestionnaireMiseEnOeuvre`, `finalitePrincipale` (255 caractères max chacun) | 20, nom croissant |
| Établissements | `nom` (255), `departement` (3), `principal` | 20, nom croissant |
| Préconisations | `libelle` (255), `etatAvancement` (100), `idTraitement` UUID | 20, libellé croissant |
| Violations | `natureViolation`, `donneesConcernees` (255), `risqueEleveDroitsLibertes`, `statut`, `dateViolationDebut`, `dateViolationFin`, `nombrePersonnesConcerneesMin`, `nombrePersonnesConcerneesMax` | 20, dateViolation décroissante |
| Utilisateurs | `nom`, `prenom`, `clientId` | Aucune pagination effective |

Le détail des opérateurs de recherche se trouve dans les classes `persistence/specifications`. Les volumes min/max des violations sont non négatifs. Tester les bornes et valeurs nulles lors d’une évolution frontend.

## Exemples de lecture

Ces commandes sont destinées à un environnement autorisé, avec données de test. Définir `API` avec l’URL effective et lire le jeton sans le stocker dans un fichier du dépôt.

```bash
API='http://localhost:8080'
read -rsp 'JWT de test : ' TOKEN; printf '\n'

curl --fail-with-body -H "Authorization: Bearer $TOKEN" \
  "$API/traitements?page=0&size=20&sort=nom,asc"

curl --fail-with-body --get -H "Authorization: Bearer $TOKEN" \
  --data-urlencode 'nom=Gestion RH' "$API/traitements"

# 42 est un exemple d’identifiant fonctionnel, à remplacer.
curl --fail-with-body -H "Authorization: Bearer $TOKEN" \
  "$API/traitements/42/historique"
```

L’appel de liste des traitements requiert un client unique dans le JWT. Les exemples ne constituent pas des tests de droits entre clients.

## Exemple d’écriture : client

!!! danger "Uniquement sur un realm jetable"
    Créer ou mettre à jour un client peut purger un groupe Keycloak existant et supprimer ses utilisateurs. Lire [le comportement exact](05-securite.md) avant l’appel ; ne pas réutiliser un nom réel.

`POST /clients`, rôle `SUPERADMIN`, retourne `201` et un `ClientDTO`. Corps indicatif :

```json
{
  "nom": "Organisation de test jetable",
  "statut": "TEST",
  "version": "1.0",
  "dateVersion": "2026-09-15"
}
```

`nom` est obligatoire et non blanc (255 max) ; `statut` est une chaîne libre (100 max), `version` 20 max. `TEST` est une valeur d’exemple, pas une enum du code. Les référentiels ne font pas partie de `ClientWriteDTO`.

## Logos : contenu, métadonnées et cache

- `GET /clients/{uuid}/logo/info` : métadonnées, sans les octets.
- `GET /clients/{uuid}/logo` : contenu avec MIME détecté, ETag SHA-256, cache privé d’une heure.
- `If-None-Match` exactement égal à l’ETag entre guillemets → `304` sans contenu.
- `PUT /clients/{uuid}/logo`, multipart `file` : PNG, JPEG ou WebP détecté par signature ; limite métier par défaut `1MB`.
- `DELETE /clients/{uuid}/logo` : `204` ; logo absent → `404`.

Les limites multipart Spring peuvent rejeter un fichier avant la limite métier. En cross-origin, `If-None-Match` ne figure pas dans les headers actuellement autorisés par `CorsConfig`, et l’exposition d’ETag n’y est pas configurée : valider/corriger ce parcours navigateur avant de promettre un cache conditionnel utilisable par le frontend.

## Réponses d’erreur : plusieurs formats coexistent

[Source : GlobalExceptionHandler](../../src/main/java/com/minds/rgpd/business/exceptions/GlobalExceptionHandler.java).

| Cas traité | HTTP | Corps |
| --- | --- | --- |
| Ressource introuvable | 404 | Texte ; certains contrôleurs renvoient un corps vide |
| Doublon / ressource utilisée | 409 | Texte |
| Argument illégal | 400 | Texte |
| Validation `@Valid` / contraintes | 400 | `ProblemDetail` |
| Logo invalide | 400 | `ProblemDetail`, titre « Fichier invalide » |
| Dépassement multipart | 413 | `ProblemDetail` |
| Erreur du fournisseur d’identité | 502 | `ProblemDetail`, y compris certains cas fonctionnels |
| IOException globale | 500 | Texte |
| Confirmation d’import requise | 409 | `InfoFichierDTO`, `confirmationRequise=true` |
| Certaines erreurs d’import | 200 | `InfoFichierDTO` portant le statut métier d’échec |

Exemple de **forme** ProblemDetail (les valeurs effectives peuvent différer) :

```json
{
  "type": "about:blank",
  "title": "Fichier invalide",
  "status": 400,
  "detail": "Le fichier envoyé est vide."
}
```

Le consommateur doit gérer le code HTTP **et** le type de contenu ; ne pas imposer un décodeur JSON unique à toutes les erreurs. Les erreurs d’authentification `401` et d’autorisation `403` peuvent être produites par Spring Security en dehors de ce handler.

## Checklist d’intégration frontend

- [ ] Modéliser UUID et numéro fonctionnel comme deux champs distincts.
- [ ] Vérifier les rôles réellement portés par le JWT et les droits par route.
- [ ] Ne pas déduire les droits d’un utilisateur uniquement des boutons affichés.
- [ ] Gérer page vs liste et erreurs texte vs ProblemDetail vs rapport d’import.
- [ ] Demander confirmation avant tout remplacement, ne jamais confirmer silencieusement.
- [ ] Configurer CORS avec l’origine exacte ; si proxy de développement, utiliser des URL relatives côté navigateur.
- [ ] Ne jamais exposer `KEYCLOAK_ADMIN_CLIENT_SECRET` au frontend.

**Suite :** [Sécurité](05-securite.md) · [Import/export](06-import-export.md).
