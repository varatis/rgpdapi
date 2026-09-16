# Configuration de l’application

[Accueil](index.md) / Exploiter

## Sources et priorités

La configuration commune est dans [application.yaml](../src/main/resources/application.yaml). Le profil [dev](../src/main/resources/application-dev.yaml) remplace notamment la base, les URL JWT, les propriétés Keycloak, l’origine CORS et le répertoire d’upload. Le profil `prod` désactive Docker Compose. Le profil `test` est défini dans les ressources de test et ne constitue pas un mode de lancement autonome du JAR.

Les propriétés peuvent être surchargées via l’environnement Spring ou la ligne de commande. **Distinction essentielle :** `KEYCLOAK_SYNC_ENABLED` est un placeholder lu dans `application.yaml`, tandis que `KEYCLOAK_ENABLED` cible directement la propriété Spring `keycloak.enabled`. Si un profil remplace la propriété par une valeur fixe, son placeholder commun n’est plus utilisé. Le même principe s’applique à `JWT_ISSUER_URI` et à `UPLOAD_DIR` quand un profil définit une valeur littérale.

Ne jamais passer de secrets sur une ligne de commande susceptible d’être enregistrée. Ne pas supposer qu’un `.env` est chargé par Spring Boot. Les scripts CI, eux, peuvent explicitement le sourcer.

## Variables principales

Les valeurs ci-dessous correspondent à la configuration commune ; les valeurs de profil priment lorsque présentes.

| Variable / propriété | Défaut commun | Rôle / précaution |
| --- | --- | --- |
| `DATABASE_URL` | Aucun | URL JDBC PostgreSQL, ex. `jdbc:postgresql://hote:5432/base` |
| `DATABASE_USERNAME` | Aucun | Compte JPA et Flyway |
| `DATABASE_PASSWORD` | Aucun | Secret JPA et Flyway |
| `UPLOAD_DIR` | Aucun | Répertoire des classeurs, accessible en écriture |
| `APPLICATION_FICHIER_UPLOAD_DIR` | Liaison directe Spring | Surcharge directe de `application.fichier.upload.dir` |
| `APPLICATION_ALLOWED_ORIGINS` | Aucun dans le YAML commun | Liste d’origines, séparées par virgules ; propriété requise par `CorsConfig` |
| `JWT_ISSUER_URI` | `https://sso.minds.k8s/auth/realms/minds-rgpd` | Placeholder pour issuer, sauf override de profil / bean personnalisé |
| `JWT_JWK_SET_URI` | même realm + `/protocol/openid-connect/certs` | Placeholder JWK, mêmes réserves |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Liaison directe Spring | Surcharge de propriété ; ne remplace pas la logique du bean dev codé en dur |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | Liaison directe Spring | Idem pour JWK |
| `APPLICATION_SECURITY_JWT_RESOURCE_ID` | `minds-saas-rgpd` | Client dont le convertisseur lit les rôles du JWT |
| `SERVER_PORT` | `8080` | Port HTTP |
| `SPRING_PROFILES_ACTIVE` | Aucun imposé | Sélection du profil |
| `SPRING_DOCKER_COMPOSE_ENABLED` | Fonctionnement de la dépendance Boot | `false` pour une infrastructure gérée manuellement ; prod le désactive |

**Correction par rapport à l’ancienne documentation :** le driver est `org.postgresql.Driver`, pas H2 ; la variable d’environnement attendue pour CORS est `APPLICATION_ALLOWED_ORIGINS`, pas `ALLOWED_ORIGIN` (aucun placeholder de ce nom n’est câblé).

## Administration Keycloak

| Variable | Défaut commun | Propriété / sens |
| --- | --- | --- |
| `KEYCLOAK_SYNC_ENABLED` | `true` | Placeholder de `keycloak.enabled` |
| `KEYCLOAK_ENABLED` | Liaison directe Spring | Surcharge directe du flag |
| `KEYCLOAK_BASE_URL` | `https://sso.minds.k8s/auth` | Base de l’API Keycloak, chemin de déploiement inclus si nécessaire |
| `KEYCLOAK_REALM` | `minds-rgpd` | Realm de gestion |
| `KEYCLOAK_ADMIN_CLIENT_ID` | `minds-rgpd-admin` | Client confidentiel du compte de service |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | Chaîne vide dans le YAML commun | Secret nécessaire à l’administration activée ; exigé sans défaut en dev |
| `KEYCLOAK_RESOURCE_CLIENT_ID` | `minds-saas-rgpd` | Client OAuth2 porteur des rôles administrés |
| `KEYCLOAK_GROUP_PREFIX` | `/clients` | Préfixe des groupes métier |
| `KEYCLOAK_PAGE_SIZE` | `200` | Taille de page des appels Admin API |
| `KEYCLOAK_TOKEN_EXPIRATION_MARGE` | `300000` | Marge de renouvellement du jeton, millisecondes |

`KEYCLOAK_RESOURCE_CLIENT_ID` et `APPLICATION_SECURITY_JWT_RESOURCE_ID` sont **deux propriétés différentes**. Si le client applicatif change, vérifier les deux. **Le flag `keycloak.enabled=false` n’est pas un interrupteur global des appels Keycloak** : dans le code courant, il court-circuite la validation de configuration du client Admin, mais ne désactive ni son bean ni la passerelle. `NoopIdentityGateway` est une substitution de test, pas un fournisseur local fourni au runtime. Ne pas compter sur ce flag pour isoler un environnement du SSO.

## Fichiers et multipart

| Propriété | Défaut dans le code du projet | Remarque |
| --- | --- | --- |
| `application.logo.taille-max` | `1MB` | Taille métier d’un logo (`APPLICATION_LOGO_TAILLE_MAX`) |
| `spring.servlet.multipart.max-file-size` | Non fixé par le projet | Dépend du défaut Spring Boot ou d’une surcharge externe |
| `spring.servlet.multipart.max-request-size` | Non fixé par le projet | À aligner avec limites Ingress / serveur / métier |
| `application.fichier.upload.dir` | `${UPLOAD_DIR}` ; `.` en dev | Le service export peut relire un classeur archivé comme modèle |

Ne pas augmenter uniquement la limite logo : le conteneur peut rejeter le multipart plus tôt (`413`). Les limites d’import Excel doivent être décidées à partir de tests mémoire, pas copiées des logos.

## JPA et Flyway

- `spring.jpa.open-in-view=false` : les conversions nécessitant des associations doivent se faire dans la transaction.
- `spring.flyway.locations=classpath:db/migration`.
- `spring.flyway.baseline-on-migrate=true` : attention à une base existante non suivie ; vérifier le schéma avant adoption.
- JPA et Flyway partagent les identifiants configurés. La séparation des comptes de migration et de runtime n’est pas formalisée ici.
- `spring.jpa.show-sql=true` dans le profil dev : attention aux logs et à la volumétrie.

## Observabilité et paramètres à vérifier

- Actuator : `management.endpoints.web.exposure.include="*"`, `health.show-details=always` ; routes autorisées sans authentification par la chaîne courante.
- Tracing : activé dans les propriétés communes avec échantillonnage 1.0, **mais** `otel.sdk.disabled=true`. Des variables OTEL existent dans Helm ; leur présence ne prouve pas une chaîne de traces opérationnelle.
- Logs : niveau racine `info`, sécurité en `DEBUG` en dev.
- Métadonnées projet injectées avec le délimiteur Maven `@...@` dans `application.yaml` ; `build-info` produit également des informations de build.
- Les clés présentes `spring.mvc.patchmatch.matching-strategy` et `server.forward-header-strategy` méritent vérification : elles ressemblent à des clés Spring mal orthographiées. Leur effet n’est pas garanti par cette documentation.

## Matrice des profils

| Profil | Configuration notable | Attention |
| --- | --- | --- |
| Aucun | Variables de base obligatoires, Keycloak activé par défaut | Configurer explicitement CORS, disque et DB |
| `dev` | Base Compose, origine localhost:4200, SQL / sécurité DEBUG, Keycloak distant | JWK codée en dur et TLS permissif ; YAML exclu du JAR |
| `int`, `demo`, `valid` | Sélectionnés par la plateforme ; valeurs d’environnement dans Helm | Décodeur personnalisé de `SecurityConfig`, pas de YAML applicatif dédié dans ce dépôt |
| `prod` | Docker Compose désactivé | Le script de déploiement n’accepte pas prod actuellement |
| `test` | Config et beans sous `src/test`, Testcontainers, identité simulée | Sécurité permissive ; pas un profil de production |

## Checklist de configuration avant livraison

- [ ] Réseau et certificats PostgreSQL/Keycloak validés.
- [ ] Secrets injectés par l’outillage autorisé, jamais suivis dans Git.
- [ ] Rôles JWT et client applicatif cohérents.
- [ ] Origines CORS exactes, sans joker avec credentials.
- [ ] Répertoire d’upload persistant, permissions et conservation définies.
- [ ] Taille des fichiers cohérente à toutes les couches.
- [ ] Actuator et debug restreints, comportement TLS corrigé/validé.
- [ ] Propriétés effectives vérifiées sans afficher de secrets.
