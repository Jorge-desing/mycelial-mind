"""Automated verification suite for Mycelial Mind Codex coverage, pagination, and in-game book exports."""

from __future__ import annotations

import glob
import json
from pathlib import Path
import sys
import unittest

REPO_ROOT = Path(__file__).resolve().parents[3]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.codex_studio.content import build_codex_book
from scripts.codex_studio.coverage_audit import RECIPE_TYPE_TO_MODONOMICON, run_coverage_audit
from scripts.codex_studio.schema import (
    MAX_VANILLA_BOOK_CHARS,
    MAX_VANILLA_BOOK_LINES,
    MAX_VANILLA_BOOK_PAGES,
    VANILLA_BOOK_PX_WIDTH,
    wrap_lines_px,
)


class CodexStudioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.book = build_codex_book()
        cls.report = run_coverage_audit(cls.book, REPO_ROOT)
        cls.export_root = REPO_ROOT / "docs/codex/book_export"

    def test_100_percent_live_repo_coverage(self) -> None:
        self.assertEqual(self.report.missing, {}, f"Missing coverage: {self.report.missing}")
        self.assertEqual(self.report.invalid_refs, [], f"Invalid refs: {self.report.invalid_refs}")
        self.assertEqual(self.report.recipe_pair_errors, [], f"Recipe pair errors: {self.report.recipe_pair_errors}")
        self.assertEqual(self.report.budget_issues, [], f"Budget issues: {self.report.budget_issues}")
        self.assertTrue(self.report.ok)
        self.assertEqual(self.report.stats["chapters"], 12)

    def test_native_book_export_and_lang_parity(self) -> None:
        native_dir = self.export_root / "native"
        book_path = native_dir / "data/mycelialmind/codex/codex_book.json"
        es_path = native_dir / "assets/mycelialmind/lang/es_es.codex.json"
        en_path = native_dir / "assets/mycelialmind/lang/en_us.codex.json"

        self.assertTrue(book_path.exists(), f"Missing {book_path}")
        self.assertTrue(es_path.exists(), f"Missing {es_path}")
        self.assertTrue(en_path.exists(), f"Missing {en_path}")

        book_data = json.loads(book_path.read_text(encoding="utf-8"))
        es_lang = json.loads(es_path.read_text(encoding="utf-8"))
        en_lang = json.loads(en_path.read_text(encoding="utf-8"))

        self.assertEqual(set(es_lang.keys()), set(en_lang.keys()))
        self.assertEqual(len(book_data["chapters"]), 12)

        for k in (book_data["title_key"], book_data["subtitle_key"], book_data["tooltip_key"]):
            self.assertIn(k, es_lang)
        for ch in book_data["chapters"]:
            self.assertIn(ch["title_key"], es_lang)
            self.assertIn(ch["subtitle_key"], es_lang)
            for entry in ch["entries"]:
                self.assertIn(entry["title_key"], es_lang)
                self.assertIn(entry["subtitle_key"], es_lang)
                for p in entry["pages"]:
                    self.assertIn(p["title_key"], es_lang)
                    self.assertIn(p["text_key"], es_lang)
                    if "title2_key" in p:
                        self.assertIn(p["title2_key"], es_lang)
                    if "entity_name_key" in p:
                        self.assertIn(p["entity_name_key"], es_lang)

    def test_modonomicon_schemas_coords_and_lang_parity(self) -> None:
        mod_dir = self.export_root / "modonomicon"
        book_root = mod_dir / "data/mycelialmind/modonomicon/books/mycelial_codex"
        es_lang = json.loads((mod_dir / "assets/mycelialmind/lang/es_es.modonomicon.json").read_text(encoding="utf-8"))
        en_lang = json.loads((mod_dir / "assets/mycelialmind/lang/en_us.modonomicon.json").read_text(encoding="utf-8"))

        self.assertEqual(set(es_lang.keys()), set(en_lang.keys()))
        book_json = json.loads((book_root / "book.json").read_text(encoding="utf-8"))
        self.assertIn(book_json["name"], es_lang)
        self.assertIn(book_json["tooltip"], es_lang)

        cat_files = sorted((book_root / "categories").glob("*.json"))
        self.assertEqual(len(cat_files), 12)
        valid_page_types = set(RECIPE_TYPE_TO_MODONOMICON.values()) | {
            "modonomicon:text",
            "modonomicon:spotlight",
            "modonomicon:entity",
        }

        all_entry_ids = set()
        for cf in cat_files:
            cat_id = cf.stem
            cat_obj = json.loads(cf.read_text(encoding="utf-8"))
            self.assertIn(cat_obj["name"], es_lang)
            self.assertIn(cat_obj["description"], es_lang)

            coords = set()
            entry_files = sorted((book_root / "entries" / cat_id).glob("*.json"))
            self.assertGreater(len(entry_files), 0)
            for ef in entry_files:
                all_entry_ids.add(f"mycelialmind:{cat_id}/{ef.stem}")
                e_obj = json.loads(ef.read_text(encoding="utf-8"))
                self.assertEqual(e_obj["category"], f"mycelialmind:{cat_id}")
                self.assertIn(e_obj["name"], es_lang)
                self.assertIn(e_obj["description"], es_lang)
                xy = (e_obj["x"], e_obj["y"])
                self.assertNotIn(xy, coords, f"Duplicate coordinate {xy} in category {cat_id}")
                coords.add(xy)

                for page in e_obj["pages"]:
                    self.assertIn(page["type"], valid_page_types)
                    self.assertIn(page["text"], es_lang)

        # Verify parent links resolve cleanly inside exported book
        for cf in cat_files:
            cat_id = cf.stem
            for ef in (book_root / "entries" / cat_id).glob("*.json"):
                e_obj = json.loads(ef.read_text(encoding="utf-8"))
                for par in e_obj.get("parents", []):
                    self.assertIn(par["entry"], all_entry_ids)

    def test_vanilla_written_books_engine_limits(self) -> None:
        v_dir = self.export_root / "vanilla_written_books"
        for lang_dir in ("es_es", "en_us"):
            vol_files = sorted((v_dir / lang_dir).glob("vol_*.json"))
            self.assertGreater(len(vol_files), 0, f"No vanilla volumes exported for {lang_dir}")
            for vf in vol_files:
                data = json.loads(vf.read_text(encoding="utf-8"))
                comp = data["component"]
                self.assertLessEqual(len(comp["title"]["raw"]), 32, f"Title too long in {vf}")
                pages = comp["pages"]
                self.assertLessEqual(len(pages), MAX_VANILLA_BOOK_PAGES, f"Volume >100 pages in {vf}")
                for idx, page in enumerate(pages, 1):
                    raw = page["raw"]
                    self.assertLessEqual(len(raw), MAX_VANILLA_BOOK_CHARS + 5, f"{vf} p{idx} too many chars")
                    wrapped = wrap_lines_px(raw, max_px=VANILLA_BOOK_PX_WIDTH)
                    self.assertLessEqual(
                        len(wrapped),
                        MAX_VANILLA_BOOK_LINES,
                        f"{vf} p{idx} exceeds {MAX_VANILLA_BOOK_LINES} wrapped lines ({len(wrapped)}): {wrapped}",
                    )

    def test_markdown_encyclopedia_and_spoilers(self) -> None:
        md_path = REPO_ROOT / "docs/codex/CODICE_MICELIAL.md"
        self.assertTrue(md_path.exists())
        text = md_path.read_text(encoding="utf-8")
        for num in range(1, 13):
            self.assertIn(f"[Libro: Cap.{num} /", text)
        self.assertIn("<details>", text)
        self.assertIn("</details>", text)


if __name__ == "__main__":
    unittest.main()
