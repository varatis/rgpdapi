# Checklist — déploiement des correctifs Keycloak

Actions restant **hors dépôt** (ops / administrateur Keycloak), après la PR
[#13](https://github.com/varatis/rgpdapi/pull/13). Les trois correctifs :

1. `POST /clients` en 403 → rôle client `superadmin` à (ré)assigner dans Keycloak ;
2. tests d'intégration Jenkins → placeholders `JWT_*` désormais résolus sans variables d'env ;
3. `GET /utilisateurs` en 400 sur les environnements k8s (`Illegal character in path`) →
   variables `KEYCLOAK_*` injectées par le chart + refus de démarrer si la configuration est incomplète.

---

## Étape 0 — Intégrer le code (une seule fois)

| # | Action | Contrôle |
| --- | --- | --- |
| 0.1 | Merger la PR #13 vers `develop` | la branche déployée contient les commits `d4ca673`, `7317494`, `65db36e`, `523800e`, `7763681` |
| 0.2 | Rejouer le build Jenkins `minds-rgpd/minds-rgpd-api` sur `develop` | les tests d'intégration passent (correctif #2) |
| 0.3 | Déployer l'image obtenue sur chaque environnement (étape 2) | — |

## Étape 1 — Vault : ajouter la clé `keycloak_admin_client_secret` (à faire **avant** le déploiement)

Le client d'administration `minds-rgpd-admin` est un *service account* : son *Client secret* ne peut pas
être déduit par l'API, il doit être stocké dans Vault **dans le même secret que `dbpassword`**
(`back.secret.path`), sous la clé `keycloak_admin_client_secret`.

**Où récupérer la valeur** : console Keycloak → `Clients` → `minds-rgpd-admin` → onglet `Credentials`
→ champ *Client secret* (bouton copier).

| Environnement | Chemin Vault | SSO / realm | Valeur |
| --- | --- | --- | --- |
| int | `saas_rgpd/int/k8s` | `https://sso.minds.k8s/auth` — realm `minds-rgpd` | secret de `minds-rgpd-admin` |
| valid | `saas_rgpd/valid/k8s` | **idem** | **la même valeur** |
| demo | `saas_rgpd/demo/k8s` | **idem** | **la même valeur** |
| prod | `minds-rgpd/k8s/config` | `https://sso.groupe-creative.fr/auth` — realm `minds-rgpd` | secret du `minds-rgpd-admin` **de prod** |

> int, valid et demo utilisent le **même** SSO et le même realm : **une seule valeur à copier**, collée
> aux trois chemins. Seule la prod a un autre Keycloak, donc une autre valeur.

**Interface Vault** : *Secrets engines* → moteur KV → chemin de l'environnement → **Create new version**
→ *Add* → clé `keycloak_admin_client_secret` → coller le secret → *Save*.
(`dbpassword` reste en place : on ajoute une ligne, on ne remplace rien.)

**En CLI** — le *mount* est celui du `SecretStore` `vault-backend` (`vault secrets list` pour le retrouver) :

```bash
vault kv patch <mount>/saas_rgpd/int/k8s     keycloak_admin_client_secret='<secret>'
vault kv patch <mount>/saas_rgpd/valid/k8s   keycloak_admin_client_secret='<secret>'
vault kv patch <mount>/saas_rgpd/demo/k8s    keycloak_admin_client_secret='<secret>'
vault kv patch <mount>/minds-rgpd/k8s/config keycloak_admin_client_secret='<secret prod>'
```

`kv patch` conserve `dbpassword`. Sur un moteur KV v1 (pas de `patch`), réécrire toutes les clés avec
`vault kv put`, `dbpassword` inclus.

**Contrôle** :

```bash
vault kv get <mount>/saas_rgpd/valid/k8s   # dbpassword ET keycloak_admin_client_secret
```

## Étape 2 — Redéployer

```bash
./.platforms/k8s/deploy.sh valid     # puis int, puis demo
```

Le script enchaîne `kubectl get pods -n minds` puis
`helm upgrade --install --wait --namespace minds minds-rgpd-api-<env> -f .platforms/k8s/values-<env>.yaml ...`
(namespace `minds`). Pour la **prod**, passer par la procédure de déploiement habituelle
(`deploy.sh` n'accepte que `int` / `valid` / `demo`) avec `values-prod.yaml` — SSO
`https://sso.groupe-creative.fr/auth` et chemin Vault `minds-rgpd/k8s/config` déjà configurés.

Rien d'autre à faire par environnement : la configuration Keycloak (`base-url`, `realm`, `admin-client-id`,
`resource-client-id`, `group-prefix`) est dans les values de chaque environnement.

## Étape 3 — Vérifier après déploiement

```bash
kubectl get externalsecret -n minds                      # minds-rgpd-api-<env>-keycloak : SecretSynced=True
kubectl get secret minds-rgpd-api-<env>-keycloak -n minds \
  -o jsonpath='{.data.KEYCLOAK_ADMIN_CLIENT_SECRET}' | base64 -d
kubectl get pods -n minds                                # pod Running (plus de CreateContainerConfigError)
kubectl logs -n minds deploy/minds-rgpd-api-<env> | tail -30
```

Puis, fonctionnellement : `GET /apimsutilisateurs/utilisateurs` ne doit plus répondre **400**
(`Illegal character in path`) — selon le jeton fourni : 200, 401 ou 403, mais plus cette erreur.

## Étape 4 — Keycloak : rôle `superadmin` et données du realm

Realm `minds-rgpd` du SSO `https://sso.minds.k8s/auth` (commun à int/valid/demo) :

| # | Action | Détail |
| --- | --- | --- |
| 4.1 | Assigner le rôle client `superadmin` | `Users` → `userrgpd` → `Role mapping` → `Assign role` → **`Filter by clients`** → `minds-saas-rgpd` → cocher `superadmin` |
| 4.2 | Se reconnecter | le jeton doit contenir `resource_access.minds-saas-rgpd.roles = ["superadmin"]` (vérifiable sur jwt.io) |
| 4.3 | Contrôler les groupes | onglet `Groups` de `userrgpd` : rattachement au bon sous-groupe de `/clients` (sinon `client_groups` vide dans le jeton) |
| 4.4 | Recréer les utilisateurs | ceux disparus lors du refresh du realm, via l'écran d'administration (`POST /utilisateurs`) — ou restaurer un export du realm si disponible |
| 4.5 | Rejouer `POST /clients` | doit répondre 2xx (plus de 403) |
| 4.6 | Idem en prod (plus tard) | realm du SSO `sso.groupe-creative.fr` : rôles clients `user` / `admin` / `superadmin` sur `minds-saas-rgpd`, groupe parent `/clients`, client `minds-rgpd-admin` |

> Rappel : le back ne lit **pas** `realm_access`. Un rôle de *realm* nommé `superadmin` ne donne
> jamais `ROLE_SUPERADMIN` — il faut le **rôle client** du client `minds-saas-rgpd`.

## Étape 5 — Poste de développement local

Le démarrage est désormais *fail-fast* : avec `keycloak.enabled=true`, l'API refuse de démarrer si
`KEYCLOAK_ADMIN_CLIENT_SECRET` (ou une `base-url` non résolue) est absente, avec un message explicite.
Vérifier que la variable est bien définie dans l'IDE / le `.env` local.

---

### En cas de problème

| Symptôme | Cause probable |
| --- | --- |
| Pod `CreateContainerConfigError` | clé absente dans Vault (étape 1) ou ExternalSecret non synchronisé → `kubectl describe externalsecret ... -n minds` |
| Démarrage refusé, message `keycloak.base-url non résolue ou invalide` | variable `KEYCLOAK_BASE_URL` non injectée (configMap / values) |
| Démarrage refusé, message `keycloak.admin-client-secret non défini` | secret Kubernetes non monté (étape 1/3) |
| Toujours 400 `Illegal character in path` | l'image déployée est antérieure au correctif (étape 0) |
| 403 sur `POST /clients` | rôle client `superadmin` non assigné, ou jeton émis avant l'assignation (se reconnecter) |
