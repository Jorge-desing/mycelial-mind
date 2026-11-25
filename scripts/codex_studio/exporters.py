"""Multi-target exporters: Markdown Encyclopedia, Native Codex JSON, Modonomicon 1.123.0, and Vanilla Written Books."""

from __future__ import annotations

import glob
import json
from pathlib import Path
from typing import Dict, List, Tuple

from .coverage_audit import RECIPE_TYPE_TO_MODONOMICON, CoverageReport
from .schema import (
    BOOK_MC_VERSION,
    BOOK_MOD_VERSION,
    BOOK_NEOFORGE_VERSION,
    MAX_VANILLA_BOOK_CHARS,
    MAX_VANILLA_BOOK_LINES,
    MAX_VANILLA_BOOK_PAGES,
    VANILLA_BOOK_PX_WIDTH,
    CodexBook,
    CodexChapter,
    CodexEntry,
    CodexPage,
    paginate_vanilla_lines,
    strip_book_markup,
)


def _write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _load_recipe_details(repo_root: Path) -> Dict[str, dict]:
    out: Dict[str, dict] = {}
    for root in (
        repo_root / "src/main/resources/data/mycelialmind/recipe",
        repo_root / "src/generated/resources/data/mycelialmind/recipe",
    ):
        if root.is_dir():
            for p in sorted(glob.glob(str(root / "*.json"))):
                rp = Path(p)
                out[rp.stem] = json.loads(rp.read_text(encoding="utf-8"))
    return out


def _format_recipe_summary(recipe_id: str, recipes: Dict[str, dict], lang: str) -> str:
    rid = recipe_id.split(":", 1)[1] if ":" in recipe_id else recipe_id
    r = recipes.get(rid, {})
    rtype = r.get("type", "").split(":")[-1]
    res = r.get("result", {})
    res_id = res.get("id", f"mycelialmind:{rid}").split(":")[-1]
    cnt = res.get("count", 1)
    if "pattern" in r:
        keys = ", ".join(
            f"{k}={v.get('item', '').split(':')[-1]}"
            for k, v in sorted(r.get("key", {}).items())
        )
        pat = "/".join(r["pattern"])
        return f"[{rtype}] {pat} ({keys}) -> {cnt}x {res_id}"
    if "ingredients" in r:
        ings = " + ".join(i.get("item", "").split(":")[-1] for i in r["ingredients"])
        return f"[{rtype}] {ings} -> {cnt}x {res_id}"
    if "ingredient" in r:
        ing = r["ingredient"].get("item", "").split(":")[-1]
        return f"[{rtype}] {ing} -> {cnt}x {res_id}"
    label = "Receta" if lang == "es" else "Recipe"
    return f"[{label}] {rid} -> {cnt}x {res_id}"


def export_markdown(book: CodexBook, report: CoverageReport, repo_root: Path, out_path: Path) -> None:
    """Export complete Spanish master encyclopedia Markdown with [Libro: Cap.X / entry / Pág.Y] markers."""
    recipes = _load_recipe_details(repo_root)
    lines: List[str] = [
        f"# {book.title.es}",
        "",
        f"> **Versión:** `Mycelial Mind v{BOOK_MOD_VERSION}` | **Minecraft:** `{BOOK_MC_VERSION}` | "
        f"**NeoForge:** `{BOOK_NEOFORGE_VERSION}`  ",
        f"> **Cobertura Auditada:** `{report.stats['chapters']}` capítulos, `{report.stats['entries']}` "
        f"entradas y `{report.stats['pages']}` páginas de libro validadas al **100%** contra el código fuente.",
        "",
        "## Índice General",
        "",
    ]

    for ch in book.chapters:
        lines.append(f"- **Capítulo {ch.number} — {ch.title.es}** (`{ch.id}`): {ch.subtitle.es}")
        for entry in ch.entries:
            lines.append(f"  - [{entry.title.es}](#cap{ch.number}-{entry.id}) — _{entry.subtitle.es}_")
    lines.extend(["", "---", ""])

    for ch in book.chapters:
        lines.extend([
            f"## Capítulo {ch.number}: {ch.title.es}",
            "",
            f"_{ch.subtitle.es}_ | Puerta recomendada: `{ch.unlock_gate}` | Ícono: `{ch.icon_item}`",
            "",
        ])
        for entry in ch.entries:
            lines.extend([
                f'<a id="cap{ch.number}-{entry.id}"></a>',
                f"### {entry.order}. {entry.title.es}",
                "",
                f"- **ID de Entrada:** `{ch.id}/{entry.id}` | **Ícono:** `{entry.icon_item}` | "
                f"**Unlock:** `{entry.unlock_gate}`",
                f"- **Resumen:** {entry.subtitle.es}",
                "",
            ])
            for p_idx, page in enumerate(entry.pages, 1):
                tag = f"[Libro: Cap.{ch.number} / {entry.id} / Pág.{p_idx}]"
                lines.append(f"#### {tag} {page.title.es}")
                lines.append("")
                if page.page_type == "spotlight" and page.item_id:
                    lines.append(f"> **[Vista Ítem]** `{page.item_id}`")
                    lines.append("")
                elif page.page_type == "entity" and page.entity_id:
                    ename = page.entity_name.es if page.entity_name else page.entity_id
                    lines.append(f"> **[Modelo 3D Entidad]** `{page.entity_id}` ({ename}, escala `{page.entity_scale}`)")
                    lines.append("")
                elif page.page_type == "recipe" and page.recipe_id:
                    lines.append(f"> **[Receta 1]** `{_format_recipe_summary(page.recipe_id, recipes, 'es')}`")
                    if page.recipe_id_2:
                        lines.append(f"> **[Receta 2]** `{_format_recipe_summary(page.recipe_id_2, recipes, 'es')}`")
                    lines.append("")

                if page.is_spoiler:
                    lines.extend([
                        "<details>",
                        "<summary>Ver revelación exacta (SPOILER)</summary>",
                        "",
                        page.body.es,
                        "",
                        "</details>",
                        "",
                    ])
                else:
                    lines.extend([page.body.es, ""])
        lines.extend(["---", ""])

    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text("\n".join(lines), encoding="utf-8")


def export_native_book(book: CodexBook, out_dir: Path) -> Tuple[Path, Path, Path]:
    """Export standalone native JSON book data + ES/EN lang dictionaries ready for SimpleJsonResourceReloadListener."""
    lang_es: Dict[str, str] = {}
    lang_en: Dict[str, str] = {}

    def put_key(key: str, es_val: str, en_val: str) -> str:
        lang_es[key] = es_val
        lang_en[key] = en_val
        return key

    put_key("item.mycelialmind.mycelial_codex", "Códice de la Colmena Viva", "Codex of the Living Hive")
    put_key("tooltip.mycelialmind.mycelial_codex", book.tooltip.es, book.tooltip.en)

    chapters_json = []
    for ch in book.chapters:
        ch_prefix = f"codex.mycelialmind.chapter.{ch.id}"
        entries_json = []
        for entry in ch.entries:
            e_prefix = f"codex.mycelialmind.entry.{ch.id}.{entry.id}"
            pages_json = []
            for p_idx, p in enumerate(entry.pages, 1):
                p_prefix = f"{e_prefix}.page.{p_idx}"
                p_obj = {
                    "page_number": p_idx,
                    "id": p.id,
                    "type": p.page_type,
                    "title_key": put_key(f"{p_prefix}.title", p.title.es, p.title.en),
                    "text_key": put_key(f"{p_prefix}.text", p.body.es, p.body.en),
                    "is_spoiler": p.is_spoiler,
                }
                if p.item_id:
                    p_obj["item_id"] = p.item_id
                if p.recipe_id:
                    p_obj["recipe_id"] = p.recipe_id
                if p.recipe_id_2 and p.title_2:
                    p_obj["recipe_id_2"] = p.recipe_id_2
                    p_obj["title2_key"] = put_key(f"{p_prefix}.title2", p.title_2.es, p.title_2.en)
                if p.entity_id:
                    p_obj["entity_id"] = p.entity_id
                    p_obj["entity_scale"] = p.entity_scale
                    if p.entity_name:
                        p_obj["entity_name_key"] = put_key(f"{p_prefix}.entity", p.entity_name.es, p.entity_name.en)
                if p.accent_color:
                    p_obj["accent_color"] = p.accent_color
                if p.anchor:
                    p_obj["anchor"] = p.anchor
                pages_json.append(p_obj)

            entries_json.append({
                "id": entry.id,
                "order": entry.order,
                "title_key": put_key(f"{e_prefix}.name", entry.title.es, entry.title.en),
                "subtitle_key": put_key(f"{e_prefix}.desc", entry.subtitle.es, entry.subtitle.en),
                "icon_item": entry.icon_item,
                "x": entry.x,
                "y": entry.y,
                "parents": [f"{ch.id}/{parent_id}" for parent_id in entry.parents],
                "unlock_gate": entry.unlock_gate,
                "registry_refs": list(entry.registry_refs),
                "pages": pages_json,
            })

        chapters_json.append({
            "id": ch.id,
            "number": ch.number,
            "title_key": put_key(f"{ch_prefix}.name", ch.title.es, ch.title.en),
            "subtitle_key": put_key(f"{ch_prefix}.desc", ch.subtitle.es, ch.subtitle.en),
            "icon_item": ch.icon_item,
            "unlock_gate": ch.unlock_gate,
            "entries": entries_json,
        })

    book_json = {
        "book_id": f"mycelialmind:{book.id}",
        "mod_version": BOOK_MOD_VERSION,
        "mc_version": BOOK_MC_VERSION,
        "neoforge_version": BOOK_NEOFORGE_VERSION,
        "title_key": put_key("book.mycelialmind.mycelial_codex.name", book.title.es, book.title.en),
        "subtitle_key": put_key("book.mycelialmind.mycelial_codex.subtitle", book.subtitle.es, book.subtitle.en),
        "tooltip_key": put_key("book.mycelialmind.mycelial_codex.tooltip", book.tooltip.es, book.tooltip.en),
        "icon_item": book.icon_item,
        "chapters": chapters_json,
    }

    book_path = out_dir / "data/mycelialmind/codex/codex_book.json"
    es_path = out_dir / "assets/mycelialmind/lang/es_es.codex.json"
    en_path = out_dir / "assets/mycelialmind/lang/en_us.codex.json"
    _write_json(book_path, book_json)
    _write_json(es_path, dict(sorted(lang_es.items())))
    _write_json(en_path, dict(sorted(lang_en.items())))
    return book_path, es_path, en_path


def export_modonomicon_book(
    book: CodexBook,
    recipes_map: Dict[str, str],
    out_dir: Path,
) -> Dict[str, object]:
    """Export Modonomicon 1.123.0 (MC 1.21.1) book, categories, entries, and ES/EN lang files."""
    lang_es: Dict[str, str] = {}
    lang_en: Dict[str, str] = {}

    def put_key(key: str, es_val: str, en_val: str) -> str:
        lang_es[key] = es_val
        lang_en[key] = en_val
        return key

    book_root = out_dir / "data/mycelialmind/modonomicon/books/mycelial_codex"
    put_key("book.mycelialmind.mycelial_codex.name", book.title.es, book.title.en)
    put_key("book.mycelialmind.mycelial_codex.tooltip", book.tooltip.es, book.tooltip.en)

    _write_json(
        book_root / "book.json",
        {
            "name": "book.mycelialmind.mycelial_codex.name",
            "tooltip": "book.mycelialmind.mycelial_codex.tooltip",
            "model": "mycelialmind:bio_scanner",
            "book_overview_texture": "modonomicon:textures/gui/book_overview.png",
            "book_content_texture": "modonomicon:textures/gui/book_content.png",
            "creative_tab": "mycelialmind:mycelial_tab",
            "display_mode": "node",
            "generate_book_item": False,
            "auto_add_read_conditions": False,
        },
    )

    cat_count = 0
    entry_count = 0
    for ch in book.chapters:
        cat_count += 1
        c_prefix = f"book.mycelialmind.mycelial_codex.{ch.id}"
        cat_obj = {
            "name": put_key(f"{c_prefix}.name", ch.title.es, ch.title.en),
            "description": put_key(f"{c_prefix}.description", ch.subtitle.es, ch.subtitle.en),
            "sort_number": ch.number - 1,
            "icon": {"item": ch.icon_item},
            "display_mode": "node",
            "background": "modonomicon:textures/gui/dark_slate_seamless.png",
            "entry_textures": "modonomicon:textures/gui/entry_textures.png",
            "background_width": 512,
            "background_height": 512,
            "max_scroll_x": 512,
            "max_scroll_y": 512,
            "background_texture_zoom_multiplier": 1.0,
            "show_category_button": True,
        }
        _write_json(book_root / f"categories/{ch.id}.json", cat_obj)

        for entry in ch.entries:
            entry_count += 1
            e_prefix = f"{c_prefix}.{entry.id}"
            pages_json = []
            for idx, p in enumerate(entry.pages, 1):
                p_prefix = f"{e_prefix}.page{idx}"
                t_key = put_key(f"{p_prefix}.title", p.title.es, p.title.en)
                body_key = put_key(f"{p_prefix}.text", p.body.es, p.body.en)

                if p.page_type in ("text", "callout"):
                    page_dict = {
                        "type": "modonomicon:text",
                        "title": t_key,
                        "use_markdown_title": True,
                        "show_title_separator": True,
                        "text": body_key,
                    }
                elif p.page_type == "spotlight":
                    page_dict = {
                        "type": "modonomicon:spotlight",
                        "title": t_key,
                        "item": {"item": p.item_id, "count": 1},
                        "text": body_key,
                    }
                elif p.page_type == "entity":
                    ename = p.entity_name or p.title
                    page_dict = {
                        "type": "modonomicon:entity",
                        "name": put_key(f"{p_prefix}.entity_name", ename.es, ename.en),
                        "entity_id": p.entity_id,
                        "scale": p.entity_scale,
                        "offset": 0.0,
                        "rotate": True,
                        "default_rotation": -45.0,
                        "text": body_key,
                    }
                elif p.page_type == "recipe":
                    assert p.recipe_id is not None
                    rid1 = p.recipe_id.split(":", 1)[1] if ":" in p.recipe_id else p.recipe_id
                    m_type = RECIPE_TYPE_TO_MODONOMICON[recipes_map[rid1]]
                    page_dict = {
                        "type": m_type,
                        "title1": t_key,
                        "recipe_id_1": p.recipe_id if ":" in p.recipe_id else f"mycelialmind:{rid1}",
                        "text": body_key,
                    }
                    if p.recipe_id_2 and p.title_2:
                        rid2 = p.recipe_id_2.split(":", 1)[1] if ":" in p.recipe_id_2 else p.recipe_id_2
                        page_dict["title2"] = put_key(f"{p_prefix}.title2", p.title_2.es, p.title_2.en)
                        page_dict["recipe_id_2"] = p.recipe_id_2 if ":" in p.recipe_id_2 else f"mycelialmind:{rid2}"
                else:
                    raise ValueError(f"Unsupported page_type for Modonomicon: {p.page_type}")

                if p.anchor:
                    page_dict["anchor"] = p.anchor
                pages_json.append(page_dict)

            entry_obj = {
                "category": f"mycelialmind:{ch.id}",
                "name": put_key(f"{e_prefix}.name", entry.title.es, entry.title.en),
                "description": put_key(f"{e_prefix}.description", entry.subtitle.es, entry.subtitle.en),
                "icon": {"item": entry.icon_item},
                "x": entry.x,
                "y": entry.y,
                "background_u_index": 0,
                "background_v_index": 0,
                "hide_while_locked": False,
                "show_when_any_parent_unlocked": True,
                "sort_number": entry.order,
                "parents": [
                    {
                        "entry": f"mycelialmind:{ch.id}/{pid}",
                        "draw_arrow": True,
                        "line_enabled": True,
                        "line_reversed": False,
                    }
                    for pid in entry.parents
                ],
                "pages": pages_json,
            }
            _write_json(book_root / f"entries/{ch.id}/{entry.id}.json", entry_obj)

    _write_json(out_dir / "assets/mycelialmind/lang/es_es.modonomicon.json", dict(sorted(lang_es.items())))
    _write_json(out_dir / "assets/mycelialmind/lang/en_us.modonomicon.json", dict(sorted(lang_en.items())))
    return {"categories": cat_count, "entries": entry_count}


def _entry_to_vanilla_pages(ch: CodexChapter, entry: CodexEntry, recipes: Dict[str, dict], lang: str) -> List[str]:
    """Flatten one CodexEntry into strictly compliant MC 1.21.1 Vanilla written_book pages."""
    out_pages: List[str] = []
    first_hdr = f"Cap.{ch.number}: {entry.title.get(lang)}"
    for idx, p in enumerate(entry.pages, 1):
        prefix_lines = []
        if idx == 1:
            prefix_lines.append(first_hdr)
        prefix_lines.append(p.title.get(lang))
        if p.page_type == "recipe" and p.recipe_id:
            prefix_lines.append(_format_recipe_summary(p.recipe_id, recipes, lang))
            if p.recipe_id_2:
                prefix_lines.append(_format_recipe_summary(p.recipe_id_2, recipes, lang))
        elif p.page_type == "entity" and p.entity_id:
            prefix_lines.append(f"Entidad: {p.entity_id}" if lang == "es" else f"Entity: {p.entity_id}")
        elif p.page_type == "spotlight" and p.item_id:
            prefix_lines.append(f"Ítem: {p.item_id}" if lang == "es" else f"Item: {p.item_id}")

        combined = "\n".join(prefix_lines) + "\n" + strip_book_markup(p.body.get(lang))
        out_pages.extend(
            paginate_vanilla_lines(
                combined,
                max_px=VANILLA_BOOK_PX_WIDTH,
                max_lines=MAX_VANILLA_BOOK_LINES,
                max_chars=MAX_VANILLA_BOOK_CHARS,
            )
        )
    return out_pages


def export_vanilla_written_books(book: CodexBook, repo_root: Path, out_dir: Path) -> Dict[str, List[dict]]:
    """Export MC 1.21.1 written_book_content multi-volume packages (<= 100 pages/volume) in ES and EN."""
    recipes = _load_recipe_details(repo_root)
    results: Dict[str, List[dict]] = {}

    for lang_key, lang_dir in (("es", "es_es"), ("en", "en_us")):
        volumes: List[dict] = []
        cur_pages: List[str] = []
        cur_chapters: List[str] = []

        def flush_volume() -> None:
            if not cur_pages:
                return
            vol_num = len(volumes) + 1
            roman = ["I", "II", "III", "IV", "V", "VI", "VII", "VIII"][vol_num - 1]
            raw_title = f"Códice Micelial Vol.{roman}" if lang_key == "es" else f"Mycelial Codex Vol.{roman}"
            assert len(raw_title) <= 32
            vol_obj = {
                "volume": vol_num,
                "chapters": cur_chapters.copy(),
                "page_count": len(cur_pages),
                "component": {
                    "title": {"raw": raw_title},
                    "author": f"Mycelial Mind v{BOOK_MOD_VERSION}",
                    "generation": 0,
                    "resolved": True,
                    "pages": [{"raw": txt} for txt in cur_pages],
                },
            }
            volumes.append(vol_obj)
            _write_json(out_dir / f"{lang_dir}/vol_{vol_num:02d}.json", vol_obj)
            cur_pages.clear()
            cur_chapters.clear()

        for ch in book.chapters:
            ch_pages: List[str] = []
            for entry in ch.entries:
                ch_pages.extend(_entry_to_vanilla_pages(ch, entry, recipes, lang_key))

            if len(ch_pages) > MAX_VANILLA_BOOK_PAGES:
                # Split huge chapter by entry so no single volume exceeds 100 pages
                for entry in ch.entries:
                    e_pages = _entry_to_vanilla_pages(ch, entry, recipes, lang_key)
                    if cur_pages and len(cur_pages) + len(e_pages) > MAX_VANILLA_BOOK_PAGES:
                        flush_volume()
                    if ch.id not in cur_chapters:
                        cur_chapters.append(ch.id)
                    cur_pages.extend(e_pages)
            else:
                if cur_pages and len(cur_pages) + len(ch_pages) > MAX_VANILLA_BOOK_PAGES:
                    flush_volume()
                cur_chapters.append(ch.id)
                cur_pages.extend(ch_pages)

        flush_volume()
        results[lang_dir] = volumes

        # Ready-to-use /give snippets for command blocks / creative Lecterns
        cmd_lines = [
            f"# Minecraft 1.21.1 /give commands ({lang_dir}) — Execute from Command Block if length > 256 chars",
            "",
        ]
        for v in volumes:
            comp_str = json.dumps(v["component"], ensure_ascii=False, separators=(",", ":"))
            cmd_lines.append(f"## Vol.{v['volume']} ({v['page_count']} pages | chapters: {', '.join(v['chapters'])})")
            cmd_lines.append(f"/give @p minecraft:written_book[minecraft:written_book_content={comp_str}] 1")
            cmd_lines.append("")
        cmd_path = out_dir / f"give_commands_{lang_key}.txt"
        cmd_path.parent.mkdir(parents=True, exist_ok=True)
        cmd_path.write_text("\n".join(cmd_lines), encoding="utf-8")

    return results


def export_all_targets(book: CodexBook, report: CoverageReport, repo_root: Path, recipes_map: Dict[str, str]) -> dict:
    """Export all documentation and ready-to-integrate in-game book artifacts."""
    docs_codex = repo_root / "docs/codex"
    export_root = docs_codex / "book_export"

    md_path = docs_codex / "CODICE_MICELIAL.md"
    export_markdown(book, report, repo_root, md_path)

    native_paths = export_native_book(book, export_root / "native")
    modonomicon_stats = export_modonomicon_book(book, recipes_map, export_root / "modonomicon")
    vanilla_stats = export_vanilla_written_books(book, repo_root, export_root / "vanilla_written_books")
    _write_json(export_root / "coverage_report.json", report.to_dict())

    return {
        "markdown": str(md_path.relative_to(repo_root)),
        "native_book": str(native_paths[0].relative_to(repo_root)),
        "modonomicon": modonomicon_stats,
        "vanilla_volumes": {
            lang: [v["page_count"] for v in vols] for lang, vols in vanilla_stats.items()
        },
        "coverage_report": "docs/codex/book_export/coverage_report.json",
    }
