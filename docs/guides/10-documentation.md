# Entretenir la documentation

[Accueil](../index.md) / Références

## Principe : une source lisible, deux usages

Les fichiers Markdown de `docs/` se lisent directement dans Git et alimentent un site MkDocs Material : navigation par parcours, recherche plein texte en français, table des matières, copie des commandes, schémas Mermaid et thème clair/sombre. Les pages ont une feuille de style d’impression pour les exports navigateur.

Le README racine reste court : il oriente vers les guides au lieu de dupliquer toutes les consignes. `docs/parametrage.md` garde son chemin historique afin de préserver les liens existants.

## Installer et vérifier le site

```bash
python3 -m venv .venv
.venv/bin/pip install -r tools/requirements-docs.txt
.venv/bin/mkdocs build --strict
python3 tools/check_docs.py
.venv/bin/mkdocs serve -a 0.0.0.0:8000
```

`tools/check_docs.py` vérifie les cibles des liens Markdown, les liens/ancres du HTML construit et la cohérence des inventaires de routes/migrations, sans accès réseau. Les URL externes ne sont pas validées.

Le build statique est écrit dans `target/documentation/`, déjà ignoré par Git. Le site peut être publié sur un hébergement statique **interne** après validation des accès. Le serveur de développement de documentation n’est pas un serveur de production et ne démarre pas l’API.

Sur un environnement de prévisualisation distant, utiliser son URL proxy, pas le `localhost` du sandbox. Aucun backend ni secret n’est nécessaire pour consulter les guides.

### Liens vers les sources

Dans Git, les liens relatifs pointent vers les fichiers du dépôt. Dans le site, le hook `tools/docs_hooks.py` convertit les liens qui sortent de `docs/` vers le code GitHub à la révision de référence. Les sources ne sont pas copiées dans le site. Actualiser la révision du hook lors d’une nouvelle revue de code et vérifier les droits d’accès au dépôt cible.

Le site n’embarque pas automatiquement tous les fichiers de conception, schémas et fixtures : les archives exclues restent accessibles dans le dépôt via leurs liens. Les fichiers Markdown sont le support de référence hors ligne ; certains rendus Mermaid, polices et assets du thème peuvent dépendre de ressources navigateur.

## Régénérer les références

```bash
python3 tools/generate_docs_reference.py
.venv/bin/mkdocs build --strict
```

Le script produit :

- `reference/routes.md` : mappings HTTP et annotations des contrôleurs ;
- `reference/dto.md` : liens et déclarations des DTO ;
- `reference/code.md` : inventaire par répertoire ;
- `reference/donnees.md` : vue métier, tables/liens et migrations.

Il s’agit d’une extraction statique légère adaptée à la syntaxe actuelle, **pas d’un parseur Java ni d’une génération OpenAPI**. Relire les résultats, surtout après introduction de nouvelles formes de mapping ou d’annotations. Le catalogue donne les contrôles déclarés, pas une preuve d’autorisation effective.

## Procédure de mise à jour

| Changement | Pages à relire |
| --- | --- |
| Route, DTO, erreur | API, routes, DTO, sécurité |
| Migration / entité | Données, architecture, cas métier |
| Import / export | Guide Excel, diagnostic, tests |
| JWT / Keycloak / rôles | Sécurité, configuration, démarrage et vigilance |
| Propriété ou secret | Configuration, démarrage, chart et exploitation |
| Pipeline / image / déploiement | Exploitation, versions et commandes |
| Correction d’un point Vxx | Vigilance et pages qui mentionnent le comportement corrigé |

À chaque revue documentaire : mettre à jour la date, la référence du code et les limites de validation. Ne pas supprimer un avertissement sur la seule base d’une intention ou d’un commentaire : vérifier le code, les tests et, si nécessaire, l’environnement réel.

## Archives et documents externes

Le dossier historique `docs/adr/openapi` contient un contrat YAML, une page HTML et plusieurs schémas draw.io. Il ne contient pas une série complète de décisions ADR argumentées. Les conserver comme **documents de conception historiques** :

- [Contrat OpenAPI archivé](../adr/openapi/minds-saas-rgpd.yml).
- [Schéma draw.io](../adr/openapi/minds_saas_rgpd_erd.drawio).
- [Schéma v2, aperçu PNG](../adr/openapi/minds_saas_rgpd_erd_v2.drawio.png).

L’ancien README référençait également des espaces internes (Jira, SharePoint, SSO, Jenkins/GitLab). Leur disponibilité et leurs droits n’ont pas été vérifiés. Les liens exacts restent récupérables dans [le README historique](https://github.com/varatis/rgpdapi/blob/42e7a389be4cbc0c40fd03ace8daca7ff767aa26/README.md). Demander à l’équipe les références actuelles plutôt que traiter des URL historiques comme actives.

## Proposition de format ADR

Pour une décision future, ajouter `docs/adr/NNNN-titre.md` avec : statut, date, contexte, options, décision, conséquences, sécurité, migration, tests et liens. **Ne pas présenter une recommandation de cette documentation comme une décision déjà approuvée.**

## Limites de la revue initiale

- Revue des contrôleurs, services, DTO, entités, repositories, import, sécurité, adaptateur Keycloak, configurations, migrations, tests et chaîne de livraison présents dans le dépôt.
- Pas d’accès aux environnements métier, au SSO, à Vault, au cluster ou aux documents SharePoint.
- Java, Maven, Docker et Helm non disponibles dans l’environnement de cette rédaction : aucun succès de compilation, test applicatif, migration ou déploiement n’est affirmé.
- Validation du générateur de références et du build documentaire effectuée séparément ; elle ne remplace pas les vérifications applicatives.
- Les propriétés ou risques signalés comme « à valider » doivent faire l’objet de preuves sur l’environnement autorisé.
