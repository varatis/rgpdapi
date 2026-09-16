#!/usr/bin/env python3
"""Check local Markdown targets and the built site's links, without network access.

Run after `mkdocs build --strict`. External URLs are intentionally not fetched.
"""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
import re

ROOT = Path(__file__).resolve().parents[1]
SITE = ROOT / 'target/documentation'
errors = []
checked = 0

# Ignore historical documents; only check maintained guide pages and README.
markdowns = [ROOT / 'README.md', ROOT / 'docs/index.md', ROOT / 'docs/parametrage.md']
markdowns += sorted((ROOT / 'docs/guides').glob('*.md'))
markdowns += sorted((ROOT / 'docs/reference').glob('*.md'))
for path in markdowns:
    for href in re.findall(r'\]\(([^\s)]+)\)', path.read_text()):
        url = urlsplit(href)
        if url.scheme or url.netloc or not url.path:
            continue
        checked += 1
        target = (path.parent / unquote(url.path)).resolve()
        if not target.is_file():
            errors.append(f'{path.relative_to(ROOT)}: missing {href}')


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.links = []
        self.ids = set()

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if attrs.get('id'):
            self.ids.add(attrs['id'])
        for attr in ('href', 'src'):
            if attrs.get(attr):
                self.links.append(attrs[attr])


pages = {}
for path in SITE.rglob('*.html'):
    parsed = Links()
    parsed.feed(path.read_text())
    pages[path.resolve()] = parsed
if not pages:
    errors.append('No built pages. Run mkdocs build --strict first.')
for path, page in pages.items():
    for href in page.links:
        url = urlsplit(href)
        if url.scheme or url.netloc:
            continue
        # Material uses href="#" for UI controls, not a document anchor.
        if not url.path and not url.fragment:
            continue
        checked += 1
        target = (SITE / unquote(url.path.lstrip('/')) if url.path.startswith('/')
                  else path.parent / unquote(url.path) if url.path else path).resolve()
        if target.is_dir():
            target /= 'index.html'
        if not target.is_file():
            errors.append(f'{path.relative_to(SITE)}: missing {href}')
        elif target in pages and url.fragment and unquote(url.fragment) not in pages[target].ids:
            errors.append(f'{path.relative_to(SITE)}: missing anchor {href}')

# Sanity-check extracted route/migration inventory against the source files.
controllers = ROOT / 'src/main/java/com/minds/rgpd/web/controllers'
source_routes = sum(len(re.findall(r'@(Get|Post|Put|Delete|Patch)Mapping\b', p.read_text()))
                    for p in controllers.glob('*.java'))
catalogue = (ROOT / 'docs/reference/routes.md').read_text()
if len(re.findall(r'^\| `(GET|POST|PUT|DELETE|PATCH)`', catalogue, re.M)) != source_routes:
    errors.append('Route catalogue count differs from source mappings. Regenerate references.')
for migration in (ROOT / 'src/main/resources/db/migration').glob('*.sql'):
    if migration.name not in (ROOT / 'docs/reference/donnees.md').read_text():
        errors.append(f'Migration missing from reference: {migration.name}')

if errors:
    raise SystemExit('\n'.join(errors))
print(f'OK: {len(markdowns)} Markdown files, {len(pages)} HTML pages, '
      f'{checked} local links/anchors and {source_routes} route mappings checked.')
