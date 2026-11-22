"""Canonical schema, strict Minecraft page budget validator, and text pagination helpers."""

from __future__ import annotations

from dataclasses import dataclass, field
import re
from typing import Dict, List, Optional, Tuple

BOOK_MOD_ID = "mycelialmind"
BOOK_MOD_VERSION = "2.0.0"
BOOK_MC_VERSION = "1.21.1"
BOOK_NEOFORGE_VERSION = "21.1.176"

VALID_PAGE_TYPES = ("text", "spotlight", "recipe", "entity", "callout")

# Minecraft default font pixel widths (character + 1px kerning spacing).
_NARROW_2 = set("i!.,:;'|¡")
_NARROW_3 = set("l`")
_NARROW_4 = set("I[]t \"")
_NARROW_5 = set("fk()*<>°¿")
_WIDE_7 = set("@~")

# Native / Modonomicon codex page panel width (~136px effective column width)
CODEX_PAGE_PX_WIDTH = 136
# Vanilla written_book_content effective width & per-page engine caps
VANILLA_BOOK_PX_WIDTH = 114
MAX_VANILLA_BOOK_LINES = 14
MAX_VANILLA_BOOK_CHARS = 250
MAX_VANILLA_BOOK_PAGES = 100

# Strict budgets per CodexPage type: (max_lines_at_136px, max_visible_chars)
PAGE_BUDGETS: Dict[str, Tuple[int, int]] = {
    "text": (14, 460),
    "callout": (14, 440),
    "spotlight": (7, 235),
    "recipe": (7, 235),
    "entity": (7, 235),
}

_MD_DECOR_RE = re.compile(r"\*\*|\$\([^)]+\)")


@dataclass(frozen=True)
class LocalizedText:
    """Bilingual ES/EN string pair validated for symmetry."""

    es: str
    en: str

    def get(self, lang: str) -> str:
        if lang.startswith("es"):
            return self.es
        if lang.startswith("en"):
            return self.en
        raise ValueError(f"Unsupported language: {lang}")


@dataclass(frozen=True)
class CodexPage:
    """One rendered page inside a CodexEntry."""

    id: str
    page_type: str
    title: LocalizedText
    body: LocalizedText
    item_id: Optional[str] = None
    recipe_id: Optional[str] = None
    recipe_id_2: Optional[str] = None
    title_2: Optional[LocalizedText] = None
    entity_id: Optional[str] = None
    entity_name: Optional[LocalizedText] = None
    entity_scale: float = 0.9
    accent_color: Optional[str] = None
    anchor: Optional[str] = None
    is_spoiler: bool = False

    def __post_init__(self) -> None:
        if self.page_type not in VALID_PAGE_TYPES:
            raise ValueError(f"Invalid page_type {self.page_type!r} in page {self.id!r}")
        if self.page_type == "spotlight" and not self.item_id:
            raise ValueError(f"Spotlight page {self.id!r} requires item_id")
        if self.page_type == "recipe" and not self.recipe_id:
            raise ValueError(f"Recipe page {self.id!r} requires recipe_id")
        if self.page_type == "entity" and not self.entity_id:
            raise ValueError(f"Entity page {self.id!r} requires entity_id")


@dataclass(frozen=True)
class CodexEntry:
    """A single topic card/node inside a CodexChapter."""

    id: str
    order: int
    title: LocalizedText
    subtitle: LocalizedText
    icon_item: str
    x: int
    y: int
    pages: Tuple[CodexPage, ...]
    parents: Tuple[str, ...] = ()
    unlock_gate: str = "always"
    registry_refs: Tuple[str, ...] = ()


@dataclass(frozen=True)
class CodexChapter:
    """Top-level chapter / Modonomicon category."""

    id: str
    number: int
    title: LocalizedText
    subtitle: LocalizedText
    icon_item: str
    entries: Tuple[CodexEntry, ...]
    unlock_gate: str = "always"


@dataclass(frozen=True)
class CodexBook:
    """Root container for the complete Mycelial Mind Codex."""

    id: str
    title: LocalizedText
    subtitle: LocalizedText
    tooltip: LocalizedText
    icon_item: str
    chapters: Tuple[CodexChapter, ...] = field(default_factory=tuple)


def char_pixel_width(ch: str) -> int:
    """Approximate Minecraft default font glyph advance in pixels (including 1px spacing)."""
    if ch in _NARROW_2:
        return 2
    if ch in _NARROW_3:
        return 3
    if ch in _NARROW_4:
        return 4
    if ch in _NARROW_5:
        return 5
    if ch in _WIDE_7:
        return 7
    return 6


def strip_book_markup(text: str) -> str:
    """Remove Modonomicon/Markdown inline formatting markers for rendered width checks."""
    return _MD_DECOR_RE.sub("", text)


def estimate_pixel_width(text: str) -> int:
    """Measure rendered horizontal pixel width of a single line."""
    clean = strip_book_markup(text)
    return sum(char_pixel_width(ch) for ch in clean)


def wrap_lines_px(text: str, max_px: int = CODEX_PAGE_PX_WIDTH) -> List[str]:
    """Word-wrap text preserving explicit newlines within a Minecraft UI pixel width."""
    wrapped: List[str] = []
    paragraphs = text.replace("\r\n", "\n").split("\n")
    for raw_para in paragraphs:
        para = raw_para.strip()
        if not para:
            wrapped.append("")
            continue
        words = para.split(" ")
        current = ""
        for word in words:
            candidate = word if not current else f"{current} {word}"
            if estimate_pixel_width(candidate) <= max_px:
                current = candidate
                continue
            if current:
                wrapped.append(current)
                current = ""
            # Handle ultra-long identifier tokens gracefully across lines
            if estimate_pixel_width(word) <= max_px:
                current = word
            else:
                chunk = ""
                for ch in word:
                    if chunk and estimate_pixel_width(chunk + ch) > max_px:
                        wrapped.append(chunk)
                        chunk = ch
                    else:
                        chunk += ch
                current = chunk
        if current:
            wrapped.append(current)
    # Trim trailing blank lines only
    while wrapped and wrapped[-1] == "":
        wrapped.pop()
    return wrapped or [""]


def paginate_vanilla_lines(text: str, max_px: int = VANILLA_BOOK_PX_WIDTH,
                           max_lines: int = MAX_VANILLA_BOOK_LINES,
                           max_chars: int = MAX_VANILLA_BOOK_CHARS) -> List[str]:
    """Split arbitrary text into strictly compliant MC 1.21.1 written_book_content pages."""
    lines = wrap_lines_px(strip_book_markup(text), max_px=max_px)
    pages: List[str] = []
    cur_lines: List[str] = []
    cur_chars = 0

    for line in lines:
        add_chars = len(line) + (1 if cur_lines else 0)
        if cur_lines and (len(cur_lines) + 1 > max_lines or cur_chars + add_chars > max_chars):
            pages.append("\n".join(cur_lines))
            cur_lines = [line]
            cur_chars = len(line)
        else:
            cur_lines.append(line)
            cur_chars += add_chars

    if cur_lines:
        pages.append("\n".join(cur_lines))
    return pages or [""]


def check_book_budgets(book: CodexBook) -> List[str]:
    """Return human-readable violations for any page exceeding Codex pixel/line/char budgets or coord clashes."""
    issues: List[str] = []
    seen_entry_ids: set[str] = set()

    for ch in book.chapters:
        seen_coords: set[Tuple[int, int]] = set()
        for entry in ch.entries:
            if entry.id in seen_entry_ids:
                issues.append(f"Duplicate entry id: {entry.id}")
            seen_entry_ids.add(entry.id)

            coord = (entry.x, entry.y)
            if coord in seen_coords:
                issues.append(f"Coord collision in chapter {ch.id}: {entry.id} at {coord}")
            seen_coords.add(coord)

            max_lines_all = PAGE_BUDGETS
            if not entry.pages:
                issues.append(f"Entry {ch.id}/{entry.id} has 0 pages")

            for idx, page in enumerate(entry.pages, 1):
                max_lines, max_chars = max_lines_all[page.page_type]
                for lang in ("es", "en"):
                    title_txt = page.title.get(lang).strip()
                    body_txt = page.body.get(lang).strip()
                    if not title_txt:
                        issues.append(f"Empty title: Cap.{ch.number}/{entry.id}/p{idx} ({lang})")
                    if not body_txt:
                        issues.append(f"Empty body: Cap.{ch.number}/{entry.id}/p{idx} ({lang})")
                    vis_chars = len(strip_book_markup(body_txt))
                    lines = wrap_lines_px(body_txt, max_px=CODEX_PAGE_PX_WIDTH)
                    if len(lines) > max_lines:
                        issues.append(
                            f"Line overflow Cap.{ch.number}/{entry.id}/p{idx} [{page.id}] ({lang}, {page.page_type}): "
                            f"{len(lines)} lines > {max_lines}"
                        )
                    if vis_chars > max_chars:
                        issues.append(
                            f"Char overflow Cap.{ch.number}/{entry.id}/p{idx} [{page.id}] ({lang}, {page.page_type}): "
                            f"{vis_chars} chars > {max_chars}"
                        )
    return issues
