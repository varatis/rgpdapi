# Comprendre le produit

[Accueil](../index.md) / Prendre ses repères

## Finalité et limites

RGPD API centralise des informations utiles au suivi de la protection des données personnelles. Le registre décrit les activités de traitement, leurs finalités, leurs données, leur conservation et leurs risques. L’application gère également les actions de préconisation, violations et demandes.

**Le code ne permet pas de conclure à une conformité réglementaire automatique.** Les durées légales, procédures CNIL, responsabilités, délais de réponse et politiques de conservation restent à faire valider par le DPO et les responsables métier. Un statut « traitée » n’est pas une preuve de conformité.

## Carte fonctionnelle

| Domaine | Rôle dans le produit | Point d’entrée technique |
| --- | --- | --- |
| Clients | Organisation SaaS, nom, statut, version et date du registre | `ClientController`, `ClientServiceImpl` |
| Logos | Identité visuelle du client, contenu séparé des métadonnées | `ClientLogoServiceImpl` |
| Établissements | Sites rattachés à un client, département et indicateur principal | `EtablissementController` |
| Traitements | Activités de traitement, finalités, données, risques et références | `TraitementController` |
| Préconisations | Actions proposées, priorité, complexité, avancement ; lien éventuel au traitement | `PreconisationController` |
| Violations | Nature, date, personnes/données concernées, mesures, information CNIL | `ViolationController` |
| Demandes | Réception, origine, description, réponse et traitement de demandes | `DemandeController` |
| Historique | Événements du registre ou d’un traitement avec auteur et date | `HistorisationController` |
| Utilisateurs | Recherche et administration des comptes Keycloak | `UtilisateurController` |
| Profils | Liste des profils de la base ; distincte des rôles Keycloak | `ProfilController` |
| Fichiers | Aperçu, import avec confirmation et export Excel | `FichierController` |

## Glossaire commun

| Terme | Signification dans ce dépôt |
| --- | --- |
| Client SaaS | Organisation gérée par l’API ; ne pas confondre avec un client OAuth2 Keycloak |
| Registre | Ensemble des traitements d’un client ; sa version est portée par `Client` |
| Traitement | Activité concernant des données personnelles ; ce n’est pas une requête informatique |
| UUID | Identifiant technique ; utilisé notamment pour clients, établissements et suppressions de traitements |
| Identifiant fonctionnel | Numéro entier d’un traitement, utilisé pour sa consultation, sa modification et son historique |
| DCP | Données à caractère personnel |
| DPO | Délégué à la protection des données ; un champ du registre, pas un rôle Spring implicite |
| PIA / étude d’impact | Information du registre sur l’étude d’impact ; pas un moteur complet d’évaluation réglementaire |
| Référentiel | Valeurs réutilisées : définitions, durées et responsables de traitement |
| Realm | Espace Keycloak contenant comptes, clients OAuth2, rôles et groupes |
| Groupe client | Groupe Keycloak situé sous `/clients`, utilisé pour rattacher une identité à une organisation |
| DTO | Objet échangé aux limites applicatives ; distinct de l’entité JPA |
| Specification | Construction de critères de recherche JPA ; à distinguer du contrat OpenAPI |

## Parcours métier typiques

### Consulter et faire évoluer le registre

1. L’utilisateur se connecte via le système d’identité et dispose d’un JWT.
2. La liste des traitements est filtrée par le client unique du jeton.
3. Le détail est lu par identifiant fonctionnel.
4. À la création, le service vérifie certains champs de doublon, résout les références et enregistre un événement.
5. À la modification, un instantané avant/après permet de produire un motif via `TraitementDiff` ; la date de mise à jour est actualisée.
6. Une suppression individuelle utilise l’UUID et historise l’action au niveau du registre.

!!! warning "Liste filtrée ≠ détail protégé par client"
    Le chemin de lecture du détail recherche actuellement par numéro fonctionnel sans paramètre client. Ne déduisez pas du filtrage de liste une isolation globale. Voir [sécurité](05-securite.md).

### Administrer des établissements

Le service gère l’unicité du nom par client ; un établissement utilisé par un traitement ne doit pas être supprimé (conflit `ResourceInUseException`). Le CRUD des traitements sélectionne des établissements existants, il ne les crée plus automatiquement. Les contraintes SQL et applicatives sont détaillées dans les sources référencées par le [modèle](../reference/donnees.md).

### Gérer une demande ou une violation

- Demande : statuts `EN_ATTENTE`, `TRAITEE` ; `PUT /demandes/{id}/traiter` positionne `TRAITEE`.
- Violation : statuts `EN_COURS`, `TRAITEE` ; dates, volumes et risque élevé sont recherchables.
- Aucun workflow réglementaire complet, moteur d’alerte ou notification CNIL automatique n’est démontré par ces contrôleurs.

### Importer un registre

Le nom du fichier désigne le client et la version. L’aperçu annonce les conséquences ; la confirmation autorise le remplacement des traitements, préconisations et violations. L’export de registre ne doit pas être assimilé à une sauvegarde complète. [Guide détaillé](06-import-export.md).

## Ce qui n’est pas dans ce dépôt

Frontend, configuration complète du realm Keycloak, sauvegardes, paramètres des outils internes, définition du Vault/SecretStore et politiques d’exploitation ne sont pas fournis comme une plateforme locale autonome. Les manifests décrivent une intégration à une infrastructure existante.

**Suite conseillée :** [Premier démarrage](02-demarrage.md) puis [Architecture](03-architecture.md).
