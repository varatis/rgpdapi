# Rapport d'intégration Back → Front — couche identité Keycloak

> **Date :** 10/09/2026 (soir) · **État du code :** branche `arena/01a08c71-rgpdapi` — inclut les correctifs de diagnostic Keycloak (502 explicites) et l'affectation des rôles au POST
> **Audience :** développeurs front (Angular, `localhost:4200`) et intégrateurs.
> Ce document décrit **ce qui a changé côté back**, les **contrats d'API exacts** (payloads, statuts),
> et les **pièges à connaître** avant de brancher ou modifier le front.

---

## 1. TL;DR — ce qui a changé pour le front

1. **Les utilisateurs ne vivent plus dans la base de l'API** : ils sont gérés par Keycloak.
   L'API les lit et les modifie via une passerelle (`KeycloakIdentityGateway`).
2. **`UtilisateurDTO` a changé de structure** (voir §4) : `identifiant`, `roles` (minuscules),
   `clientId`, `clientNom` — l'ancien modèle lié à la table `utilisateur` a disparu.
3. **Un utilisateur appartient à un seul client SaaS**, matérialisé par un **groupe Keycloak**
   `/clients/{nom du client}`. Le champ `groupe` des payloads porte le **nom** du client.
4. **Les rôles applicatifs** sont des rôles client Keycloak : `admin`, `user` (minuscules dans l'API,
   `ROLE_ADMIN`/`ROLE_USER` dans le JWT). Le CRUD client exige en plus `superadmin`.
5. **Deux opérations sont destructrices** : supprimer un client supprime ses utilisateurs Keycloak ;
   recréer un client de même nom purge le groupe existant (membres supprimés). Voir §5.3.

---

## 2. Authentification

### 2.1 Fonctionnement

- L'API est un **serveur de ressources OAuth2 stateless** : chaque appel doit porter
  `Authorization: Bearer <JWT>` émis par Keycloak.
- **Émetteur (dev) :** `https://sso.minds.k8s/auth/realms/minds-rgpd`
- Endpoints publics sans token : `/actuator/**`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`.
- **CORS (dev) :** `http://localhost:4200` est autorisé (`application.allowed-origins`).
- CSRF désactivé (API stateless) ; sessions jamais créées.

### 2.2 Rôles portés par le JWT

Le convertisseur back (`JwtAuthConverter`) lit **deux claims** :

```json
{
  "preferred_username": "alice@alpha.com",
  "resource_access": {
    "minds-saas-rgpd": { "roles": ["admin"] }
  },
  "client_groups": ["/clients/Dupont"]
}
```

- `resource_access.minds-saas-rgpd.roles` → rôles applicatifs, **normalisés en MAJUSCULES** avec
  préfixe : `admin` → `ROLE_ADMIN`, `user` → `ROLE_USER`, `superadmin` → `ROLE_SUPERADMIN`.
- `client_groups` → également convertis en autorités (rarement utiles côté UI ; c'est le back
  métier qui exploite le rattachement client, pas l'autorisation).

**Vocabulaire des rôles (source de vérité : `GET /utilisateurs/roles`) :**

| Rôle (API, minuscule) | Autorité JWT | Peut faire |
|---|---|---|
| `user` | `ROLE_USER` | consultations ouvertes aux authentifiés |
| `admin` | `ROLE_ADMIN` | + CRUD utilisateurs |
| `superadmin` | `ROLE_SUPERADMIN` | + CRUD clients et logos |

> ⚠️ Dans les **payloads et réponses** de `/utilisateurs`, les rôles sont **en minuscules**
> (`"admin"`). La casse `ROLE_ADMIN` n'existe que dans les autorités du token. Le back valide
> les rôles reçus contre `GET /utilisateurs/roles` : toute valeur inconnue → erreur.

### 2.3 Statuts HTTP d'authentification

- `401` : token absent, expiré ou invalide → le front doit rafraîchir/rediriger vers le SSO.
- `403` : token valide mais rôle insuffisant → afficher un message « non autorisé ».

---

## 3. Conventions métier

| Concept | Règle |
|---|---|
| Identifiant utilisateur | = email (le `username` Keycloak est l'email) |
| Rattachement client | un groupe Keycloak `/clients/{nom du client}` ; **un seul client par utilisateur** |
| Changement de client | `PUT /utilisateurs/{id}` : le back **retire l'ancien groupe** avant d'affecter le nouveau |
| Rôles à la modification | **remplacés intégralement** (anciens retirés, nouveaux affectés) — envoyer la liste complète |
| `actif: false` | utilisateur désactivé dans Keycloak : il ne peut plus se connecter, mais reste listé |
| Suppression utilisateur | définitive côté Keycloak (pas de corbeille) |
| Client ↔ groupe | créer un client crée son groupe ; supprimer un client supprime le groupe **et ses membres** |

---

## 4. API Utilisateurs — `/utilisateurs`

### 4.1 Endpoints

| Méthode | URL | Rôle requis | Corps / params | Réponse |
|---|---|---|---|---|
| GET | `/utilisateurs` | `ADMIN` ou `SUPERADMIN` | query : `nom`, `prenom`, `clientId` | `200` `UtilisateurDTO[]` |
| GET | `/utilisateurs/roles` | authentifié | — | `200` `string[]` (ex. `["admin","user"]`) |
| GET | `/utilisateurs/{id}` | authentifié | — | `200` `UtilisateurDTO` |
| POST | `/utilisateurs` | `ADMIN` ou `SUPERADMIN` | `UtilisateurWriteDTO` | `201` `UtilisateurDTO` |
| PUT | `/utilisateurs/{id}` | `ADMIN` ou `SUPERADMIN` | `UtilisateurWriteDTO` | `200` `UtilisateurDTO` |
| DELETE | `/utilisateurs/{id}` | `ADMIN` ou `SUPERADMIN` | — | `204` |

> Note : le contrôleur affiche parfois `isAuthenticated()` mais le **service** resserre à
> `ADMIN/SUPERADMIN` (ex. le listing). En cas de doute, la ligne du tableau ci-dessus fait foi.

### 4.2 Schémas

**Réponse — `UtilisateurDTO` :**

```json
{
  "id": "d6dfd117-8047-4a9a-afca-f5268a38bfcf",
  "identifiant": "alice@alpha.com",
  "prenom": "Alice",
  "nom": "Dupont",
  "email": "alice@alpha.com",
  "actif": true,
  "roles": ["admin"],
  "clientId": "3f0f5b2e-…",
  "clientNom": "Dupont"
}
```

- `identifiant` = username Keycloak (= email).
- `clientId` / `clientNom` : déduits du **groupe** de l'utilisateur croisé avec les clients connus
  en base. Si l'utilisateur n'a pas de groupe (ou un groupe sans client correspondant) :
  `clientId = null` et `clientNom` = nom du groupe brut (ou `null`).

**Payload d'écriture — `UtilisateurWriteDTO` (POST et PUT) :**

```json
{
  "prenom": "Alice",
  "nom": "Dupont",
  "email": "alice@alpha.com",
  "roles": ["user"],
  "clientId": "3f0f5b2e-…",
  "groupe": "Dupont",
  "actif": true
}
```

- Validation : `prenom`/`nom`/`email` obligatoires (taille max 100/100/255), `roles` **non vide**.
- **`clientId` ET `groupe` sont attendus** : `groupe` (nom du client, ex. `"Dupont"`) pilote
  l'affectation Keycloak ; `clientId` sert à la réponse et à la validation. Ils doivent être
  **cohérents** (le groupe du client portant cet id).
- `actif` optionnel, défaut `true`.
- `roles` : minuscules, valeurs issues de `GET /utilisateurs/roles`.

### 4.3 Filtres du listing

- `nom`, `prenom` : correspondance **« contient »**, insensible à la casse.
- `clientId` : égalité exacte sur le client de rattachement.
- La pagination (`page`, `size`…) est **actuellement ignorée** par le back : la réponse renvoie
  toujours la liste complète. Ne pas bâtir d'infinite-scroll serveur pour l'instant (§7).

---

## 5. API Clients — `/clients`

### 5.1 Endpoints

| Méthode | URL | Rôle requis | Corps | Réponse |
|---|---|---|---|---|
| GET | `/clients` | authentifié | — | `200` `ClientDTO[]` |
| GET | `/clients/nom/{nom}` | authentifié | — | `200` `ClientDTO` |
| POST | `/clients` | `SUPERADMIN` | `ClientWriteDTO` | `201` `ClientDTO` |
| PUT | `/clients/{id}` | `SUPERADMIN` | `ClientWriteDTO` | `200` `ClientDTO` |
| DELETE | `/clients/{id}` | `SUPERADMIN` | — | `204` |
| GET | `/clients/{id}/logo` | authentifié | header `If-None-Match` optionnel | `200` binaire (+ETag, 304 possible) |
| GET | `/clients/{id}/logo/info` | authentifié | — | `200` métadonnées |
| PUT | `/clients/{id}/logo` | `SUPERADMIN` | multipart `file` | `200` métadonnées |
| DELETE | `/clients/{id}/logo` | `SUPERADMIN` | — | `204` |

### 5.2 Schémas

**`ClientWriteDTO` :**

```json
{ "nom": "Dupont", "statut": "ACTIF", "version": "3.25", "dateVersion": "2026-01-15" }
```

- `nom` obligatoire (max 255), unique — sinon **`409`**.
- `statut` (max 100), `version` (max 20), `dateVersion` (`YYYY-MM-DD`) optionnels.

**`ClientDTO` (réponse) :** `id`, `nom`, `statut`, `version`, `dateVersion`,
`durees[]`, `definitions[]`, `responsablesTraitement[]` (référentiels gérés par le domaine,
non modifiables via cet écran).

### 5.3 ⚠️ Effets de bord Keycloak (à comprendre avant de coder l'UI)

| Action front | Ce que fait le back |
|---|---|
| **Créer** un client | crée le groupe Keycloak `/clients/{nom}`. Si un groupe homonyme **existait déjà**, ses **membres sont supprimés** et le groupe recréé vide |
| **Renommer** un client | le groupe est synchronisé avec le nouveau nom |
| **Supprimer** un client | **supprime d'abord tous les utilisateurs Keycloak du groupe**, puis le groupe, puis le client en base |

Conséquences UI recommandées :
- Confirmer la suppression d'un client par un message explicite (« ses utilisateurs seront supprimés du SSO »).
- Après création/renommage, rafraîchir la liste des utilisateurs (leur `clientNom`/`groupe` change).

---

## 6. Gestion des erreurs

| Situation | Statut | Corps |
|---|---|---|
| Ressource inconnue (client, utilisateur, logo…) | `404` | texte brut (message) |
| Nom de client déjà pris | `409` | texte brut |
| Payload invalide (validation Bean) | `400` | **ProblemDetail JSON** (`title`, `detail`) |
| Argument illégal | `400` | texte brut |
| Fichier trop volumineux / format refusé | `413` / `400` | ProblemDetail JSON |
| Erreur fournisseur d'identité (Keycloak : jeton inaccessible, client/groupe introuvable, écriture refusée/permissions…) | `502` | **ProblemDetail JSON** (`title` + `detail` explicites) |
| Token absent/expiré | `401` | — |
| Rôle insuffisant | `403` | — |

> Le format d'erreur n'est **pas homogène** (texte brut vs ProblemDetail). Le front devrait
> tolérer les deux : si le corps n'est pas du JSON, afficher le texte brut.

---

## 7. Limitations connues et pièges (état actuel du back)

À connaître pour éviter des heures de débogage — toutes remontées par les échanges récents :

1. **Rôles affectés à la création** (`POST /utilisateurs`) — **corrigé côté back** : le back
   crée l'utilisateur, l'affecte à son groupe **puis lui attribue ses rôles**. Le contournement
   « POST puis PUT de rattrapage » n'est plus nécessaire : **à retirer du front**. (Le PUT
   immédiat restait de toute façon sans effet de bord : il remplace les rôles par la même liste.)
2. **`IdentityProviderException` → `502` ProblemDetail** — **corrigé côté back** : un handler
   dédié renvoie `{ "title": "Erreur du fournisseur d'identité (Keycloak)", "detail": "…" }`
   portant la cause exacte (jeton inaccessible — vérifiez `KEYCLOAK_ADMIN_CLIENT_SECRET` —,
   rôle invalide, client/groupe introuvable). Le parsing ProblemDetail du front le couvre déjà.
3. **Pagination ignorée** sur `GET /utilisateurs` (paramètre non branché).
4. **Double contrat `clientId`/`groupe`** dans `UtilisateurWriteDTO` : les deux champs doivent
   désigner le même client. Le back pourrait à terme déduire `groupe` de `clientId`.
5. **`keycloak.enabled: false` (profil test) n'a pas d'effet** : c'est le profil Spring qui
   substitue une passerelle factice (`NoopIdentityGateway`), pas ce flag.
6. Le nom d'utilisateur Keycloak (`identifiant`) est **figé à l'email de création** : un `PUT`
   qui change l'`email` met à jour l'email du compte Keycloak mais **pas son username** —
   la réponse affichera ensuite `identifiant` (ancien email) ≠ `email`. Si l'UI permet de
   modifier l'email, afficher les deux champs et prévenir de cette asymétrie.

---

## 8. Environnements et outillage

| Élément | Valeur (dev) |
|---|---|
| Origine front autorisée (CORS) | `http://localhost:4200` |
| SSO / realm | `https://sso.minds.k8s/auth/realms/minds-rgpd` |
| Client OAuth des rôles applicatifs | `minds-saas-rgpd` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Variable d'env requise côté back | `KEYCLOAK_ADMIN_CLIENT_SECRET` (démarrage dev) |

---

## 9. Checklist de recette front

- [ ] Listing utilisateurs : filtres `nom`/`prenom` (contient, insensible casse) et `clientId`.
- [ ] Détail utilisateur : `roles` minuscules, `clientId`/`clientNom` remplis si groupe connu.
- [ ] Création utilisateur : rôles attribués directement par le POST — retirer le PUT de rattrapage (cf. §7.1 corrigé).
- [ ] Modification : changement de client (l'ancien groupe est retiré), rôles remplacés.
- [ ] Suppression utilisateur : disparition de la liste (source Keycloak).
- [ ] CRUD client : `409` sur doublon de nom ; confirmation renforcée sur la suppression
      (utilisateurs du groupe supprimés).
- [ ] Habillage : upload logo (PNG/JPEG/WebP), affichage avec `ETag`/304.
- [ ] Parcours `ROLE_USER` : endpoints lecture seuls ; `403` propres sur les actions admin.
- [ ] Gestion `401` → refresh/redirection SSO ; affichage des erreurs 400/409/500 hétérogènes.

---

## 10. Annexe — architecture en deux phrases

Le port `business/identity/IdentityGateway` définit le contrat identité (utilisateurs, groupes,
rôles) ; l'adaptateur `infrastructure/keycloak/KeycloakIdentityGateway` l'implémente contre
l'API d'administration Keycloak (jeton `client_credentials`, pagination, requêtes inverses pour
reconstituer rôles et groupes). Les services métier (`UtilisateurServiceImpl`,
`ClientServiceImpl`) n'ont **aucune dépendance directe à Keycloak** : pour tester ou brancher
un autre fournisseur, il suffit d'une autre implémentation du port.
