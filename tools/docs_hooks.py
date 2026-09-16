"""Keep repository-relative links in Markdown, use pinned source links in the site."""
from pathlib import Path
import re
from urllib.parse import quote, unquote

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
REVISION = "42e7a389be4cbc0c40fd03ace8daca7ff767aa26"
SOURCE_URL = f"https://github.com/varatis/rgpdapi/blob/{REVISION}/"


def on_page_markdown(markdown, page, **kwargs):
    def rewrite(match):
        target = match.group(2)
        if "://" in target or target.startswith(("#", "/", "mailto:")):
            return match.group(0)
        path, separator, fragment = target.partition("#")
        resolved = (Path(page.file.abs_src_path).parent / unquote(path)).resolve()
        if resolved.is_relative_to(ROOT) and not resolved.is_relative_to(DOCS):
            return f"{match.group(1)}{SOURCE_URL}{quote(resolved.relative_to(ROOT).as_posix())}{separator}{fragment})"
        # Excluded design archives remain links to the repository, not broken site assets.
        if resolved.is_relative_to(DOCS / "adr"):
            return f"{match.group(1)}{SOURCE_URL}{quote(resolved.relative_to(ROOT).as_posix())}{separator}{fragment})"
        return match.group(0)

    return re.sub(r"(\]\()([^\s)]+)\)", rewrite, markdown)
