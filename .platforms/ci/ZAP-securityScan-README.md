# Guide Utilisateur - Scanner de Sécurité API Backend

## Vue d'ensemble

Ce guide décrit l'utilisation du scanner de sécurité OWASP ZAP pour les API REST backend dans l'environnement corporate Creative, avec auto-détection OpenAPI et authentification OAuth2.

## Architecture

```mermaid
graph TD
    A[API Backend] --> B[security-scan.sh]
    B --> C[Auto-détection]
    C --> D[OpenAPI /v3/api-docs]
    C --> E[Endpoints Actuator]
    C --> F[Swagger UI]

    G[OAuth2 Keycloak] --> H[JWT Token]
    H --> I[Scanner ZAP]

    B --> I
    D --> I
    E --> I
    F --> I
    I --> J[Rapports API]

    subgraph "Pipeline Jenkins"
        K[Build] --> L[Test] --> M[API Security Scan] --> N[Deploy]
    end

    subgraph "Credentials Jenkins"
        O[minds-rgpd-oauth]
        P[minds-rgpd-admin]
    end

    M --> O
    M --> P
```

## Prérequis

### Infrastructure
- Connexion VPN corporate active
- API REST déployée avec Spring Boot
- Keycloak configuré pour OAuth2
- Accès Jenkins avec credentials appropriés

### Technologies Supportées
- **Backend**: Spring Boot avec Spring Security
- **Documentation**: SpringDoc OpenAPI (v3/api-docs)
- **Authentification**: Keycloak OAuth2/OIDC
- **Endpoints**: Spring Boot Actuator

## Installation et Configuration

### 1. Structure du Projet Backend

```
votre-api-backend/
├── .platforms/
│   └── ci/
│       └── security-scan.sh    # Script principal API
├── src/
│   └── main/
│       ├── java/               # Code source
│       └── resources/
│           └── application.yml # Config Spring Boot
├── pom.xml                     # Configuration Maven
└── api-endpoints.txt           # (Optionnel) Endpoints personnalisés
```

### 2. Script security-scan.sh pour API

Créez le fichier `.platforms/ci/security-scan.sh`:

```bash
#!/bin/bash
# security-scan.sh - Scanner de sécurité API REST
source platforms/bootstrap.sh

SCANNER_IMAGE="srv-nexus:18444/outillage/zaproxy:stable"
PROJECT_NAME="mon-api-backend"

# Configuration des URLs par environnement
case "${1:-develop}" in
    "demo")
        BASE_URL="https://api-demo.mon-app.minds.k8s"
        ;;
    "valid")
        BASE_URL="https://api-valid.mon-app.minds.k8s"
        ;;
    *)
        echo "❌ Environnement inconnu: ${1}"
        echo "Disponibles: demo, valid"
        exit 1
        ;;
esac

# Configuration OAuth2
AUTH_URL="https://sso.minds.k8s/auth/realms/creative/protocol/openid-connect/token"
```

### 3. Configuration Spring Boot

#### application.yml

```yaml
# Configuration SpringDoc OpenAPI
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

# Configuration Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

#### Dépendances Maven (pom.xml)

```xml
<!-- SpringDoc OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
</dependency>

<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Spring Security + OAuth2 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

## Configuration Jenkins

### 1. Credentials OAuth2 Client

#### Créer OAuth2 Client Credentials

1. **Jenkins → Manage Jenkins → Manage Credentials**
2. **Add Credentials → Username with password**

```
ID: minds-rgpd-oauth
Username: minds-rgpd-api        # Client ID OAuth2
Password: [CLIENT_SECRET_VALUE]    # Client Secret OAuth2
Description: OAuth2 Client pour scanner API (Client Credentials)
```

### 2. Credentials Utilisateur (Fallback)

#### Créer User Credentials

```
ID: minds-rgpd-admin
Username: admin                    # Nom utilisateur Keycloak
Password: [USER_PASSWORD]          # Mot de passe utilisateur
Description: Utilisateur admin pour scanner API (Password Grant)
```

### 3. Configuration Pipeline

#### Jenkinsfile Complet

```groovy
@Library("jenkins-pipeline-library")

node() {
    String projectName = "mon-api-backend"
    String projectBranch = params.BRANCH_NAME
    String deployTo = params.DEPLOY_TO

    // Paramètres de sécurité
    boolean skipAPISecurity = params.SKIP_API_SECURITY_SCAN ?: false

    try {
        checkout scm

        stage('Build & Test') {
            sh "mvn clean compile test"
        }

        stage("Deploy") {
            if (!params.SKIP_DEPLOY) {
                withKubeConfig([credentialsId: 'kubeconfig-minds-admin']) {
                    sh "bash .platforms/k8s/deploy.sh ${deployTo}"
                }
            }
        }

        stage("API Security Test") {
            if (!skipAPISecurity) {
                boolean apiSecurityError = false
                try {
                    withCredentials([
                        // OAuth2 Client credentials (Client Credentials Flow)
                        usernamePassword(credentialsId: 'minds-rgpd-oauth',
                                       usernameVariable: 'OAUTH_CLIENT_ID',
                                       passwordVariable: 'OAUTH_CLIENT_SECRET'),
                        // User credentials (Password Grant fallback)
                        usernamePassword(credentialsId: 'minds-rgpd-admin',
                                       usernameVariable: 'ZAP_USERNAME',
                                       passwordVariable: 'ZAP_PASSWORD')
                    ]) {
                        println "🔒 Démarrage scan sécurité API: ${deployTo}"
                        sh """
                        OAUTH_CLIENT_ID='${OAUTH_CLIENT_ID}' \
                        OAUTH_CLIENT_SECRET='${OAUTH_CLIENT_SECRET}' \
                        ZAP_USERNAME='${ZAP_USERNAME}' \
                        ZAP_PASSWORD='${ZAP_PASSWORD}' \
                        bash .platforms/ci/security-scan.sh ${deployTo}
                        """
                    }
                } catch (err) {
                    apiSecurityError = true
                    println "❌ Échec scan sécurité API: ${err.getMessage()}"
                } finally {
                    // Archiver les rapports
                    archiveArtifacts artifacts: 'security-reports/**/*',
                                   allowEmptyArchive: true, excludes: null

                    // Publier rapports HTML
                    if (fileExists('security-reports/index.html')) {
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'security-reports',
                            reportFiles: 'index.html',
                            reportName: 'API Security Report'
                        ])
                    }

                    if (fileExists('security-reports/api-coverage.html')) {
                        publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'security-reports',
                            reportFiles: 'api-coverage.html',
                            reportName: 'API Coverage Report'
                        ])
                    }

                    if (apiSecurityError) {
                        currentBuild.result = 'UNSTABLE'
                        echo "⚠️ Scan sécurité API terminé avec des problèmes"
                    } else {
                        println "✅ Scan sécurité API terminé avec succès"
                    }
                }
            }
        }

    } catch (err) {
        currentBuild.result = 'FAILURE'
        throw err
    }
}
```

#### Paramètres Pipeline Requis

```groovy
properties([
    parameters([
        booleanParam(name: 'SKIP_API_SECURITY_SCAN',
                    defaultValue: false,
                    description: 'Ignorer le scan de sécurité API'),
        booleanParam(name: 'SKIP_DEPLOY',
                    defaultValue: false,
                    description: 'Ignorer le déploiement'),
        choice(name: 'DEPLOY_TO',
               choices: ['valid', 'demo', 'int', 'prod'],
               description: 'Environnement de déploiement')
    ])
])
```

## Utilisation

### 1. Auto-détection des Endpoints

Le scanner détecte automatiquement:

#### OpenAPI/Swagger
```bash
# Endpoints testés automatiquement:
/v3/api-docs          # Spécification OpenAPI JSON
/v3/api-docs.yaml     # Spécification OpenAPI YAML
/swagger-ui.html      # Interface Swagger UI
/swagger-ui/index.html # Interface Swagger UI alternative
```

#### Spring Boot Actuator
```bash
# Endpoints de monitoring détectés:
/actuator/health      # État de santé de l'application
/actuator/info        # Informations sur l'application
/actuator/metrics     # Métriques de performance
/actuator/env         # Variables d'environnement
```

### 2. Execution Locale

#### Test de Connectivité
```bash
# Vérifier la disponibilité de l'API
./platforms/ci/security-scan.sh test valid

# Sortie attendue:
# 🔍 Testing connectivity and showing detection preview...
# Testing base URL: https://api-valid.mon-app.minds.k8s
# ✅ Base URL is accessible
#
# Testing common API patterns:
#   api-docs: Found
#   swagger-ui.html: Found
#   health: Found
```

#### Scan Complet
```bash
# Scanner l'environnement de validation
./platforms/ci/security-scan.sh valid

# Scanner la production (plus conservateur)
./platforms/ci/security-scan.sh prod
```

### 3. Configuration Endpoints Personnalisés

#### Fichier api-endpoints.txt (Optionnel)

```bash
# Format: METHOD:PATH[:CONTENT-TYPE[:BODY]]

# Endpoints GET simples
GET:/api/v1/users
GET:/api/v1/products
GET:/api/v1/orders

# Endpoints POST avec payload
POST:/api/v1/users:application/json:{"name":"test","email":"test@example.com"}
POST:/api/v1/products:application/json:{"name":"produit","price":100}

# Endpoints avec paramètres de path
GET:/api/v1/users/{id}
PUT:/api/v1/users/{id}:application/json:{"name":"updated"}
DELETE:/api/v1/users/{id}

# Endpoints avec headers personnalisés
GET:/api/v1/admin/users
POST:/api/v1/admin/reports:application/json:{"type":"security"}
```

## Authentification OAuth2

### 1. Flux Client Credentials (Recommandé)

```bash
# Configuration automatique:
OAUTH_CLIENT_ID="minds-rgpd-api"      # De Jenkins credentials
OAUTH_CLIENT_SECRET="[SECRET]"           # De Jenkins credentials
AUTH_URL="https://sso.minds.k8s/auth/realms/creative/protocol/openid-connect/token"

# Requête OAuth2 automatique:
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=minds-rgpd-api&client_secret=[SECRET]" \
  https://sso.minds.k8s/auth/realms/creative/protocol/openid-connect/token

# Token JWT utilisé automatiquement:
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6...
```

### 2. Flux Password Grant (Fallback)

```bash
# Si client_credentials échoue, tentative password grant:
ZAP_USERNAME="admin"                     # De Jenkins credentials
ZAP_PASSWORD="[PASSWORD]"                # De Jenkins credentials

curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=minds-rgpd-api&username=admin&password=[PASSWORD]" \
  https://sso.minds.k8s/auth/realms/creative/protocol/openid-connect/token
```

### 3. Configuration Keycloak

#### Client OAuth2 Requis

```json
{
  "clientId": "minds-rgpd-api",
  "enabled": true,
  "clientAuthenticatorType": "client-secret",
  "secret": "[CLIENT_SECRET]",
  "standardFlowEnabled": false,
  "serviceAccountsEnabled": true,
  "directAccessGrantsEnabled": true,
  "validRedirectUris": [],
  "protocolMappers": [
    {
      "name": "api-audience",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-audience-mapper",
      "config": {
        "included.client.audience": "minds-rgpd-api",
        "id.token.claim": "false",
        "access.token.claim": "true"
      }
    }
  ]
}
```

## Types de Tests Effectués

### 1. Tests d'Endpoints

#### Découverte Automatique
```bash
# Via OpenAPI:
- Import complet de la spécification
- Tous les endpoints documentés
- Méthodes HTTP correctes
- Paramètres et payloads

# Via patterns Spring Boot:
- Endpoints Actuator
- Endpoints de santé
- Endpoints de métriques
```

#### Tests de Sécurité par Endpoint
```bash
# Pour chaque endpoint détecté:
- Injection SQL
- XSS dans paramètres JSON
- Command Injection
- Path Traversal
- Authorization Bypass
- Parameter Pollution
- LDAP Injection (si applicable)
```

### 2. Tests d'Authentification

```bash
# Tests OAuth2:
- Token expiration handling
- Invalid token responses
- Scope validation
- Audience validation

# Tests de session:
- Session fixation
- Session hijacking
- CSRF protection
```

### 3. Tests API Spécifiques

```bash
# Tests JSON:
- Malformed JSON parsing
- JSON injection
- Schema validation bypass
- Content-Type confusion

# Tests REST:
- HTTP method override
- Accept header manipulation
- CORS misconfiguration
- API versioning bypass
```

## Rapports Générés

### Structure des Rapports

```
security-reports/
├── index.html              # Rapport principal HTML
├── api-coverage.html       # Rapport de couverture API
├── report.xml              # Rapport XML (intégration CI)
├── alerts.json             # Alertes au format JSON
├── discovered_urls.txt     # Endpoints découverts
└── summary.txt             # Résumé exécutif
```

### Rapport de Couverture API

Le fichier `api-coverage.html` contient:

```html
📋 Auto-Discovery Results
- OpenAPI Specification: ✅ https://api.mon-app.com/v3/api-docs
- Swagger UI: ✅ https://api.mon-app.com/swagger-ui.html
- Health Endpoint: ✅ https://api.mon-app.com/actuator/health

🌐 Discovered Endpoints (42 endpoints)
- GET /api/v1/users
- POST /api/v1/users
- GET /api/v1/users/{id}
- PUT /api/v1/users/{id}
- DELETE /api/v1/users/{id}
- ...

🔐 Authentication Status
- OAuth2 Token: ✅ Successfully acquired (client_credentials)
```

### Interprétation des Alertes API

#### Alertes Spécifiques aux API

| Type | Description | Action |
|------|-------------|--------|
| **JSON Injection** | Injection dans payload JSON | Validation input côté serveur |
| **API Rate Limiting** | Absence de limitation de débit | Implémenter rate limiting |
| **CORS Misconfiguration** | Configuration CORS permissive | Restreindre les origines |
| **OpenAPI Information Disclosure** | Informations sensibles exposées | Sécuriser la documentation |
| **Actuator Exposure** | Endpoints de monitoring publics | Sécuriser les endpoints Actuator |

#### Exemples d'Alertes

```json
{
  "alertname": "JSON Injection",
  "risk": "High",
  "confidence": "Medium",
  "url": "https://api.mon-app.com/api/v1/users",
  "param": "name",
  "attack": "{\"$ne\": null}",
  "evidence": "Database error revealed",
  "solution": "Implement JSON schema validation"
}
```

## Configuration Avancée

### 1. Paramètres de Performance

```bash
# Dans environments.conf - Production
get_api_scan_settings() {
    case "$env_type" in
        "production")
            echo "API_MAX_REQUESTS_PER_ENDPOINT=5"
            echo "API_SCAN_DELAY=1000"  # ms entre requêtes
            echo "API_FUZZ_PAYLOADS=minimal"
            ;;
        "development")
            echo "API_MAX_REQUESTS_PER_ENDPOINT=20"
            echo "API_SCAN_DELAY=100"
            echo "API_FUZZ_PAYLOADS=comprehensive"
            ;;
    esac
}
```

### 2. Exclusions de Sécurité

#### Fichier .zap-exclusions (Optionnel)

```bash
# Exclure certains endpoints du scan
echo "EXCLUDE_URL:/api/v1/admin/dangerous-endpoint" >> .zap-exclusions
echo "EXCLUDE_PARAM:internal_token" >> .zap-exclusions
echo "EXCLUDE_ALERT:Information Disclosure - Debug Error Messages" >> .zap-exclusions
```

### 3. Headers Personnalisés

```bash
# Dans le script, ajouter des headers
docker_env_vars="$docker_env_vars -e CUSTOM_HEADERS=X-API-Version:1.0,X-Client-ID:scanner"
```

## Bonnes Pratiques

### 1. Sécurité des Credentials

```bash
# ✅ Bonnes pratiques:
- Utiliser Jenkins Credential Store
- Rotation régulière des secrets OAuth2
- Service accounts dédiés au scan
- Scopes minimaux sur les tokens

# ❌ À éviter:
- Hardcoder les credentials dans les scripts
- Utiliser des comptes utilisateurs personnels
- Tokens avec scopes administrateur complets
```

### 2. Gestion des Environnements

```bash
# Production:
- Scan en heures creuses uniquement
- Notification équipes avant scan
- Paramètres conservateurs
- Exclusion des endpoints critiques

# Développement/Test:
- Scan automatique sur chaque déploiement
- Paramètres complets
- Tous les endpoints inclus
```

### 3. Intégration CI/CD

```bash
# Pipeline Gates:
- High Risk → Build failed
- Medium Risk → Build unstable
- Low Risk → Build success avec warnings

# Notifications:
- Slack/Teams sur échec sécurité
- Email résumé quotidien
- Dashboard de sécurité centralisé
```

## Dépannage

### Problèmes d'Auto-détection

#### OpenAPI non détecté
```bash
# Debug manuel:
curl -v https://votre-api.com/v3/api-docs

# Vérifier la configuration Spring Boot:
springdoc.api-docs.enabled=true
springdoc.api-docs.path=/v3/api-docs
```

#### Endpoints Actuator non accessibles
```bash
# Vérifier la configuration:
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# Test manuel:
curl https://votre-api.com/actuator/health
```

### Problèmes d'Authentification

#### Échec du Client Credentials Flow
```bash
# Vérifier la configuration Keycloak:
- Client ID correct
- Client Secret valide
- Service Accounts activé
- Grant Type: client_credentials

# Test manuel:
curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=YOUR_CLIENT&client_secret=YOUR_SECRET" \
  https://sso.minds.k8s/auth/realms/creative/protocol/openid-connect/token
```

#### Token JWT invalide
```bash
# Vérifier le token:
echo $JWT_TOKEN | base64 -d | jq .

# Vérifier l'expiration:
- exp: timestamp d'expiration
- aud: audience correcte
- scope: permissions suffisantes
```

### Performance et Timeout

```bash
# Réduire la portée du scan:
export API_MAX_REQUESTS_PER_ENDPOINT=3
export API_SCAN_DELAY=2000
export ZAP_TIMEOUT=300

# Exclure des endpoints lents:
echo "EXCLUDE_URL:/api/v1/reports/heavy" >> .zap-exclusions
```

## Limitations

### Techniques
- **Réseau**: Nécessite VPN corporate et connectivité K8s
- **Performance**: Impact possible sur l'API pendant le scan
- **Authentification**: Support limité à OAuth2/Keycloak

### Fonctionnelles
- **OpenAPI**: Nécessite spécification à jour et accessible
- **Endpoints**: Détection limitée aux patterns Spring Boot
- **Payloads**: Tests de charge limités pour éviter l'impact

### Support
- **Framework**: Spring Boot uniquement
- **Auth**: Keycloak OAuth2 uniquement
- **Documentation**: OpenAPI v3 recommandé

## Support et Formation

### Contacts
- **DevSecOps**: devsecops@groupe-creative.fr
- **Support API**: api-support@groupe-creative.fr
- **Sécurité**: security-team@groupe-creative.fr

### Ressources
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [Keycloak OAuth2 Guide](https://www.keycloak.org/docs/latest/securing_apps/)
- [Confluence Corporate - API Security](https://confluence.corporate/api-security)

### Formation
- **Session API Security**: Formation mensuelle
- **OWASP API Top 10**: Workshop trimestriel
- **Keycloak OAuth2**: Formation technique bi-annuelle
