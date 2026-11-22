"""Shared bilingual builders and strict pixel-budget auto-chunking helpers for Codex Studio."""

from __future__ import annotations

from typing import List, Optional, Sequence, Tuple

from .schema import (
    CODEX_PAGE_PX_WIDTH,
    CodexPage,
    LocalizedText,
    strip_book_markup,
    wrap_lines_px,
)


def lt(es: str, en: str) -> LocalizedText:
    return LocalizedText(es=es.strip(), en=en.strip())


def text_page(
    page_id: str,
    title_es: str,
    title_en: str,
    body_es: str,
    body_en: str,
    accent_color: Optional[str] = None,
    anchor: Optional[str] = None,
    is_spoiler: bool = False,
) -> CodexPage:
    return CodexPage(
        id=page_id,
        page_type="text",
        title=lt(title_es, title_en),
        body=lt(body_es, body_en),
        accent_color=accent_color,
        anchor=anchor,
        is_spoiler=is_spoiler,
    )


def callout_page(
    page_id: str,
    title_es: str,
    title_en: str,
    body_es: str,
    body_en: str,
    accent_color: str = "#9B4DFF",
    anchor: Optional[str] = None,
    is_spoiler: bool = False,
) -> CodexPage:
    return CodexPage(
        id=page_id,
        page_type="callout",
        title=lt(title_es, title_en),
        body=lt(body_es, body_en),
        accent_color=accent_color,
        anchor=anchor,
        is_spoiler=is_spoiler,
    )


def spot_page(
    page_id: str,
    item_id: str,
    title_es: str,
    title_en: str,
    body_es: str,
    body_en: str,
    accent_color: Optional[str] = None,
    anchor: Optional[str] = None,
) -> CodexPage:
    return CodexPage(
        id=page_id,
        page_type="spotlight",
        item_id=item_id,
        title=lt(title_es, title_en),
        body=lt(body_es, body_en),
        accent_color=accent_color,
        anchor=anchor,
    )


def recipe_page(
    page_id: str,
    recipe_id: str,
    title_es: str,
    title_en: str,
    body_es: str,
    body_en: str,
    recipe_id_2: Optional[str] = None,
    title2_es: Optional[str] = None,
    title2_en: Optional[str] = None,
) -> CodexPage:
    t2 = lt(title2_es, title2_en) if (title2_es and title2_en) else None
    return CodexPage(
        id=page_id,
        page_type="recipe",
        recipe_id=recipe_id,
        recipe_id_2=recipe_id_2,
        title=lt(title_es, title_en),
        title_2=t2,
        body=lt(body_es, body_en),
    )


def entity_page(
    page_id: str,
    entity_id: str,
    name_es: str,
    name_en: str,
    body_es: str,
    body_en: str,
    scale: float = 0.85,
) -> CodexPage:
    return CodexPage(
        id=page_id,
        page_type="entity",
        entity_id=entity_id,
        entity_name=lt(name_es, name_en),
        title=lt(name_es, name_en),
        body=lt(body_es, body_en),
        entity_scale=scale,
    )


def _fits_budget(es_txt: str, en_txt: str, max_lines: int, max_chars: int) -> bool:
    for txt in (es_txt, en_txt):
        if len(strip_book_markup(txt)) > max_chars:
            return False
        if len(wrap_lines_px(txt, max_px=CODEX_PAGE_PX_WIDTH)) > max_lines:
            return False
    return True


def chunk_paired_blocks(
    prefix: str,
    title_es: str,
    title_en: str,
    blocks: Sequence[Tuple[str, str]],
    *,
    max_lines: int = 13,
    max_chars: int = 435,
    page_type: str = "text",
    accent_color: Optional[str] = None,
    is_spoiler: bool = False,
    separator: str = "\n",
) -> List[CodexPage]:
    """Pack bilingual bullet/record pairs into guaranteed-fitting CodexPages with 1:1 ES/EN symmetry."""
    if not blocks:
        raise ValueError(f"chunk_paired_blocks({prefix}) received empty blocks")

    groups: List[Tuple[str, str]] = []
    cur_es: List[str] = []
    cur_en: List[str] = []

    for b_es, b_en in blocks:
        b_es = b_es.strip()
        b_en = b_en.strip()
        if not _fits_budget(b_es, b_en, max_lines, max_chars):
            raise ValueError(f"Single block in {prefix} exceeds page budget: {b_es[:80]!r}")

        cand_es = separator.join([*cur_es, b_es])
        cand_en = separator.join([*cur_en, b_en])
        if cur_es and not _fits_budget(cand_es, cand_en, max_lines, max_chars):
            groups.append((separator.join(cur_es), separator.join(cur_en)))
            cur_es = [b_es]
            cur_en = [b_en]
        else:
            cur_es.append(b_es)
            cur_en.append(b_en)

    if cur_es:
        groups.append((separator.join(cur_es), separator.join(cur_en)))

    total = len(groups)
    pages: List[CodexPage] = []
    for idx, (g_es, g_en) in enumerate(groups, 1):
        suffix = f" ({idx}/{total})" if total > 1 else ""
        pages.append(
            CodexPage(
                id=f"{prefix}_p{idx}",
                page_type=page_type,
                title=lt(f"{title_es}{suffix}", f"{title_en}{suffix}"),
                body=lt(g_es, g_en),
                accent_color=accent_color,
                is_spoiler=is_spoiler,
            )
        )
    return pages


def grid_xy(index: int, cols: int = 4, step_x: int = 2, step_y: int = 2) -> Tuple[int, int]:
    """Non-overlapping lattice coordinates for Modonomicon node tree."""
    col = index % cols
    row = index // cols
    return (col * step_x - ((cols - 1) * step_x) // 2, row * step_y)
