# Diagnostic et points de vigilance

[Accueil](../index.md) / Exploiter

## Statut de cette revue

Cette page est une **revue statique documentée**, pas un audit de sécurité exhaustif, un pentest ou une validation de production. Les priorités ci-dessous sont proposées pour guider l’équipe ; elles ne représentent pas un backlog approuvé. Aucun comportement applicatif n’a été modifié par le travail documentaire.

## Points prioritaires avant données réelles

| ID | Priorité proposée | Constat et source | Impact / action recommandée |
| --- | --- | --- | --- |
| V01 | Critique à qualifier | `ClientServiceImpl.synchroniserGroupeClient` supprime les membres et recrée le groupe ; appelée aussi à la mise à jour | Perte de comptes ; remplacer par synchronisation non destructive, tests de renommage et de mise à jour sans changement de nom |
| V02 | Haute | `SecurityConfig.jwtDecoder` sur dev/int/demo/valid : JWK fixe, trust manager et hostname verifier permissifs | Protection TLS dégradée ; supprimer le contournement et tester configuration issuer/JWK effective |
| V03 | Haute | Filtrage client hétérogène ; détail traitement par numéro seul, import ciblé par nom du fichier, `findAll()` de plusieurs services | Risque d’accès inter-client ; audit objet par objet et tests d’autorisation négatifs |
| V04 | Haute | `/actuator/**` autorisé sans authentification ; exposition `*`, détails de santé | Réduire les endpoints et protéger management au niveau application/plateforme |
| V05 | Haute | `FichierController` construit le chemin depuis le nom reçu, écrase le fichier, copie après service sans vérifier un succès métier strict | Risques de chemin/écrasement et incohérence DB-disque ; validation de chemin, nom interne et cycle d’archivage sûr |
| V06 | Haute | Helm : `ecretKeyRef`, `{ { ... } }`, second ExternalSecret mal structuré | Injection du secret non fiable / rendu à corriger ; lint, template et validation de schéma |
| V07 | Haute | Appels HTTP Keycloak dans un service transactionnel JPA sans compensation | Des opérations externes persistent malgré rollback SQL ; concevoir réconciliation et reprise |
| V08 | Haute | Import supprime traitements, préconisations et violations, puis gère des exceptions en statut métier | Tester toutes les erreurs après suppression, rollback réel, concurrence et sauvegarde |

## Dette et limites complémentaires

| ID | Constat | Suite recommandée |
| --- | --- | --- |
| V09 | `business/` et `infrastructure/` racine hors sources Maven, avec classes dupliquées | Identifier leur origine et supprimer/synchroniser seulement après décision explicite |
| V10 | `GET/PUT traitements/{id}` entier, `DELETE` UUID | Uniformiser à terme ou versionner ; rendre la distinction explicite côté clients API |
| V11 | `nextId` calcule max+1 ; suppression de doublons sans rôle spécifique | Stratégie de concurrence et restriction d’accès à définir |
| V12 | Pas de hiérarchie de rôles ; groupes également convertis en autorités | Définir modèle d’autorisations et éviter collisions groupes/rôles |
| V13 | `Pageable` utilisateurs non appliqué ; pagination SSO plafonnée à 100 pages | Mesurer volumétrie, complétude et latence ; contrat de pagination explicite |
| V14 | RestTemplate Admin sans timeouts explicites | Fixer délais et politique de reprise, tester indisponibilité SSO |
| V15 | Profil test permissif, sans vraie chaîne JWT | Ajouter tests dédiés JWT, rôles, client et CORS |
| V16 | Formats d’erreur mixtes et certains échecs d’import en HTTP 200 | Stabiliser contrat d’erreur sans casser les consommateurs |
| V17 | Montage de volume inactif dans le chart | Assurer la persistance des classeurs et tester redémarrage de pod |
| V18 | Configuration tracing contradictoire / clés Spring suspectes | Valider propriétés effectives et instrumentation réelle |
| V19 | CORS n’autorise pas `If-None-Match`, n’expose pas ETag globalement | Tester le cache logo depuis l’origine frontend et adapter le contrat |
| V20 | `deploy.sh` refuse prod malgré un fichier values-prod | Définir un parcours production approuvé et testé |
| V21 | `run-test-package.sh` vise un fichier Compose sous un chemin qui ne correspond pas au fichier présent | Revoir les scripts historiques réellement utilisés |
| V22 | Les anciennes archives OpenAPI / draw.io ne sont pas générées depuis le code actuel | Conserver comme historique, privilégier runtime et migrations |
| V23 | `keycloak.enabled=false` ne désactive pas les beans ni les appels de la passerelle ; il évite la validation initiale | Clarifier le flag, conditionner réellement l’adaptateur et tester l’absence d’appels externes |

Les liens vers chaque composant sont accessibles dans l’[index du code](../reference/code.md). Ces points sont indépendants : corriger le README ne corrige pas les comportements applicatifs.

## Arbre de diagnostic rapide

### L’API ne démarre pas

1. **Compilation ?** JDK21, Maven, annotation processors, dépendances accessibles.
2. **Placeholder / bean ?** profil effectif, DB, CORS, UPLOAD_DIR, configuration Keycloak et secret.
3. **Connexion DB ?** hôte, port, réseau, compte, disponibilité PostgreSQL.
4. **Flyway ?** version du schéma et checksum ; sauvegarder les informations avant toute action, ne pas lancer repair/clean sans analyse.
5. **Kubernetes ?** rendu des manifests, Secret/ConfigMap référencés, image et événements de pod.

### 401 ou 403

- 401 : vérifier expiration du jeton, signature, JWK et issuer effectifs ; ne jamais envoyer le secret admin comme bearer.
- 403 : vérifier les autorités calculées, le client dans `resource_access` et l’annotation du service autant que celle du contrôleur.
- Un compte `SUPERADMIN` seul peut être refusé sur une route réservée à `ADMIN`.
- Une prérequête CORS peut échouer avant le contrôleur ; vérifier origine, méthode et headers.

### 400 lié au client

`client_groups` doit permettre de déterminer exactement un client sur les routes qui utilisent ce mécanisme. Contrôler le nom métier exact, pas seulement la présence d’un groupe dans l’interface Keycloak. Un nom de chemin complet peut ne pas correspondre au nom en base.

### 502 Keycloak

Vérifier séparément accès au token endpoint, secret/compte de service, realm, client applicatif et permissions d’administration. Certaines erreurs fonctionnelles (rôle invalide, client absent) sont aussi transformées en erreur d’identité : lire le détail sans le publier s’il contient des informations sensibles.

### Import « terminé » mais données inattendues

- Vérifier le code HTTP **et** `statusFichier` / `confirmationRequise`.
- Relever le client extrait du nom, la version, les feuilles réellement présentes et les nombres remplacés/importés.
- Contrôler l’état SQL et le fichier archivé séparément.
- Ne pas relancer automatiquement avec confirmation ; préserver les preuves et solliciter l’équipe avant une nouvelle écriture.

### Logo invisible ou cache inopérant

Contrôler existence du logo, MIME, taille multipart, taille métier, ETag exact et règles CORS. L’image binaire et les métadonnées ont deux endpoints distincts.

## Informations utiles dans un ticket

Fournir : environnement, version/commit déployé, date/heure UTC, route et verbe, code HTTP, identifiant de test non sensible, scénario minimal et résultat attendu/obtenu. Joindre un extrait de log expurgé ; **jamais** secret, jeton, mot de passe, classeur réel ou export de données personnelles.

## Décisions à faire formaliser

1. Quel est le périmètre exact d’un admin et d’un superadmin ?
2. Quelle clé stable relie un client SaaS à son groupe IAM ?
3. Quelles garanties de rollback et de sauvegarde pour un import ?
4. Quel contrat d’erreur et quels identifiants pour une future version d’API ?
5. Qui possède l’exploitation, l’IAM, les données et la validation réglementaire ?

Ces réponses devraient devenir des ADR et des critères de tests, pas rester implicites dans le frontend ou les habitudes de l’équipe.
