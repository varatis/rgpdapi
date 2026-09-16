#!/usr/bin/env python3
"""Generate reviewable Markdown inventories from the current Java layout.

Deliberately a lightweight extractor, not a Java parser or an OpenAPI substitute.
Run from any directory; output is restricted to docs/reference/*.md.
"""
from pathlib import Path
import re
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/minds/rgpd'
OUT = ROOT / 'docs/reference'
OUT.mkdir(parents=True, exist_ok=True)


def link(path, label=None):
    return f'[{label or path.name}](../../{quote(path.relative_to(ROOT).as_posix())})'


def save(name, title, content):
    (OUT / name).write_text(
        f'# {title}\n\n[Accueil](../index.md) / Références\n\n'
        '> Référence extraite des sources par `tools/generate_docs_reference.py`. '
        'À relire après chaque évolution ; le code et le contrat runtime priment.\n\n'
        + re.sub(r'(?m)(\|[^\n]*\|)\n\n(?=\|)', r'\1\n', content), encoding='utf-8')


sections = ['## Comment lire ce catalogue\n\n'
            'Chaque ligne correspond à un mapping déclaré dans un contrôleur. '
            '« Authentifié (global) » signifie absence de restriction de méthode supplémentaire '
            'dans le contrôleur, **pas accès public**. Hors profil test, la chaîne Spring '
            'demande un JWT sur ces routes. Les annotations des services peuvent ajouter des restrictions.\n\n'
            '**Exception importante :** la recherche utilisateurs requiert ADMIN ou SUPERADMIN '
            'dans `UtilisateurServiceImpl`. SUPERADMIN n’hérite pas automatiquement d’ADMIN. '
            'Voir [sécurité](../guides/05-securite.md).\n\n'
            '**Identifiants :** GET/PUT et historique de traitement utilisent le numéro fonctionnel ; '
            'DELETE individuel utilise un UUID. Consulter les signatures sources.\n']
route_count = 0
for path in sorted((JAVA / 'web/controllers').glob('*.java')):
    text = path.read_text()
    class_pos = text.index('public class ')
    base_match = re.search(r'@RequestMapping\("([^"]*)"\)', text[:class_pos])
    base = base_match.group(1) if base_match else ''
    rows = []
    mappings = list(re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(([^\n]*)\))?', text[class_pos:]))
    body = text[class_pos:]
    for n, match in enumerate(mappings):
        args = match.group(2) or ''
        suffix = re.search(r'"([^"]*)"', args)
        route = base + (suffix.group(1) if suffix else '')
        end = mappings[n+1].start() if n+1 < len(mappings) else len(body)
        block = body[match.end():end]
        auth = re.search(r'@PreAuthorize\("([^"]+)"\)', block)
        method = re.search(r'public\s+ResponseEntity<[^\n]+?>\s+(\w+)\s*\(', block)
        if not method:
            raise ValueError(f'Unrecognized handler in {path}: {route}')
        role = f'`{auth.group(1)}`' if auth else 'Authentifié (global)'
        rows.append(f'| `{match.group(1).upper()}` | `{route}` | `{method.group(1)}` | {role} |')
    route_count += len(rows)
    sections.append(f'## {path.stem}\n\nSource : {link(path)}.\n\n'
                    '| Verbe | Chemin | Méthode Java | Contrôle déclaré au contrôleur |\n'
                    '| --- | --- | --- | --- |\n' + '\n'.join(rows) + '\n')
sections.insert(1, f'**Inventaire : {route_count} mappings dans 10 contrôleurs.**\n')
save('routes.md', 'Catalogue des routes HTTP', '\n'.join(sections))

sections = ['## Usage\n\nCes déclarations montrent les noms, types et contraintes des objets métier échangés. '
            'Les `FilterCriteria` modélisent des paramètres de recherche, pas des corps JSON. '
            'Les annotations de validation ne sont appliquées à une requête que si son parcours '
            'déclenche la validation (par exemple `@Valid`). Ne pas supposer que chaque DTO '
            'est accepté en écriture. Les propriétés calculées de classes complètes restent dans les sources.\n\n'
            'Commencer par [API et exemples](../guides/04-api.md), puis vérifier '
            'le schéma exact dans `/v3/api-docs` de la version exécutée.\n']
for path in sorted((JAVA / 'business/dtos').glob('*.java')):
    text = path.read_text()
    clean = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    # Record declaration up to the body; for classes, expose fields through source link.
    record = re.search(r'public record\s+\w+\s*\(', clean)
    sections.append(f'## {path.stem}\n\nSource : {link(path)}.\n')
    if record:
        start = record.start()
        depth = 0
        for i in range(clean.index('(', start), len(clean)):
            if clean[i] == '(':
                depth += 1
            elif clean[i] == ')':
                depth -= 1
                if depth == 0:
                    declaration = clean[start:i+1]
                    break
        sections.append('```java\n' + declaration.strip() + '\n```\n')
    else:
        fields = re.findall(r'^\s*(?:private|protected)\s+[^\n;]+;', clean, re.M)
        sections.append('```java\n' + '\n'.join(f.strip() for f in fields) + '\n```\n'
                        if fields else 'Consulter la déclaration complète dans la source.\n')
save('dto.md', 'Objets échangés et critères de recherche', '\n'.join(sections))

sections = ['## Où modifier le code ?\n\nLes sources compilées sont sous `src/main/java`. '
            'Les copies racine `business/` et `infrastructure/` ne sont pas des racines Maven '
            'déclarées. Cet index couvre aussi tests, ressources et scripts pour faciliter la navigation.\n']
paths = []
for folder in ['src/main', 'src/test', '.platforms', 'business', 'infrastructure']:
    paths += [p for p in (ROOT / folder).rglob('*') if p.is_file()]
groups = {}
for p in sorted(paths):
    groups.setdefault(p.parent.relative_to(ROOT).as_posix(), []).append(p)
for group, files in groups.items():
    sections.append(f'## `{group}`\n\n' + '\n'.join(f'- {link(p)}' for p in files) + '\n')
sections.append('## Racine et outillage\n\n' + '\n'.join(f'- {link(ROOT / p)}' for p in [
    'pom.xml', 'Jenkinsfile', 'docker-compose.yml', 'sonar-project.properties',
    'project-suppression-cve.xml', '.gitignore']))
save('code.md', 'Index navigable du code et des tests', '\n'.join(sections))

sections = ['''## Vue métier simplifiée

Ce schéma montre les associations structurantes, pas toutes les colonnes ni toutes les contraintes SQL. Les migrations et annotations JPA détaillées restent la référence ; plusieurs anciennes classes/structures coexistent avec les référentiels actuels.

```mermaid
erDiagram
    CLIENT ||--o{ TRAITEMENT : possede
    CLIENT ||--o{ ETABLISSEMENT : regroupe
    TRAITEMENT }o--o{ ETABLISSEMENT : concerne
    CLIENT ||--o{ PRECONISATION : suit
    TRAITEMENT o|--o{ PRECONISATION : associe
    CLIENT ||--o{ VIOLATION : recense
    CLIENT ||--o{ DEMANDE : recoit
    CLIENT ||--o| CLIENT_LOGO : affiche
    CLIENT ||--o{ DEFINITION : reference
    CLIENT ||--o{ DUREE : reference
    CLIENT ||--o{ RESPONSABLES_TRAITEMENT : reference
    CLIENT ||--o{ HISTORISATION_REGISTRE : historise
    TRAITEMENT ||--o{ HISTORISATION_TRAITEMENT : historise
```

### Clés, ownership et relations

- `Client` : UUID ; version et date de version du registre ; référentiels rattachés.
- `Traitement` : UUID technique `identifiant` et numéro entier `idFonctionnel`. Les routes ne les utilisent pas de façon uniforme.
- `Etablissement` : UUID, nom unique par client (`uq_etablissement_nom_client`), département et booléen principal.
- `ClientLogo` : relation un-à-un avec identifiant partagé via `@MapsId` ; contenu en base, MIME, ETag, nom et taille. L’ETag est le SHA-256 du contenu.
- `Definition` : type et valeur, référencée notamment par finalité, sensibilité, étude d’impact et licéité du traitement.
- `Duree` : valeur, indicateur archivage et client ; unicité client / archivage / valeur.
- `ResponsableTraitement` : valeur et informations complémentaires ; unicité client / valeur. Malgré un ancien commentaire SQL « 1-1 », le modèle permet plusieurs valeurs par client.
- Historiques : événements avec date, motif et auteur ; auteur issu du contexte utilisateur, repli « système » en l’absence d’utilisateur exploitable.
- Utilisateurs : le service courant utilise Keycloak via `IdentityGateway`. La présence de structures historiques ne signifie pas qu’elles pilotent l’administration des comptes.

### Suppressions et cascades

Ne pas assimiler une cascade JPA à une suppression réglementaire complète. Les migrations définissent aussi des actions SQL (`CASCADE`, `SET NULL`) sur certaines clés étrangères. La suppression d’un traitement peut emporter son historique lié ; un événement au niveau registre est alors écrit par le service de suppression individuelle. L’import supprime aussi préconisations et violations du client. Les actions Keycloak et disque sont hors des garanties transactionnelles SQL.

### Historisation : portée réelle

Le service de traitement historise création, différences de modification et suppression au niveau approprié ; le service d’import historise le registre. Des routes permettent l’ajout manuel d’événements avec auteur déterminé côté serveur. Le motif de modification vient de `TraitementDiff`. Cela ne démontre pas un journal d’audit immuable, inviolable ou couvrant toutes les entités : durcissement, conservation et accès doivent être définis séparément.

## Inventaire des types de persistance

Le tableau inclut les classes du package, **y compris celles sans `@Entity`**, pour éviter de confondre type Java et table réelle. Les entités héritées peuvent partager une table ; vérifier les annotations d’héritage.

| Type Java | Mapping déclaré | Source |
| --- | --- | --- |
''']
for path in sorted((JAVA / 'persistence/entities').glob('*.java')):
    text = path.read_text()
    table = re.search(r'@Table\(name\s*=\s*"([^"]+)"', text)
    mapping = f'`{table.group(1)}`' if table else ('`@Entity`, sans @Table explicite' if '@Entity' in text else 'Pas de @Entity déclaré')
    sections.append(f'| `{path.stem}` | {mapping} | {link(path)} |')
sections.append('''
## Historique Flyway

Ordre numérique des migrations présentes. Il n’y a pas de V10 dans ce dépôt ; les versions Flyway n’ont pas à être contiguës. Une table de suivi `flyway_schema_history` est gérée par Flyway au runtime. Aucun état de migration d’une base distante n’a été vérifié.

| Version | Objet annoncé par le fichier | Source SQL |
| --- | --- | --- |
''')
migrations = sorted((ROOT / 'src/main/resources/db/migration').glob('*.sql'),
                    key=lambda p: tuple(int(x) for x in p.name.split('__')[0][1:].split('.')))
for path in migrations:
    version, title = path.stem.split('__', 1)
    sections.append(f'| `{version}` | {title.replace("_", " ")} | {link(path)} |')
sections.append('''
## Lire une évolution de schéma

1. Lire toutes les migrations dans l’ordre, pas seulement V1.
2. Comparer le SQL final aux entités courantes et aux DTO.
3. Vérifier les contraintes et les chemins de suppression avant un import ou un delete.
4. Tester depuis une base vide et depuis une base de la version précédente.
5. Ne jamais réécrire une migration déjà partagée pour « arranger » un checksum.

Les schémas draw.io historiques sont référencés dans [Entretien de la documentation](../guides/10-documentation.md). Pour faire évoluer le modèle, suivre le [guide de développement](../guides/07-developpement.md).
''')
save('donnees.md', 'Modèle de données et migrations', '\n'.join(sections))
print(f'Generated four reference pages: {route_count} routes, {len(migrations)} migrations.')
