"""CLI entry point for building, exporting, and strictly auditing the Mycelial Mind Codex."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

# Ensure repo root is on sys.path when executed as script
_SCRIPT_DIR = Path(__file__).resolve().parent
_REPO_ROOT = _SCRIPT_DIR.parent.parent
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))

from scripts.codex_studio.content import build_codex_book
from scripts.codex_studio.coverage_audit import run_coverage_audit, scan_repo_inventory
from scripts.codex_studio.exporters import export_all_targets


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build & audit Mycelial Mind Codex deliverables.")
    parser.add_argument("--all", action="store_true", help="Export Markdown, Native JSON, Modonomicon, and Vanilla books.")
    parser.add_argument("--check", action="store_true", help="Fail non-zero if any coverage, budget, or ref check fails.")
    parser.add_argument("--report", action="store_true", help="Print full JSON coverage report to stdout.")
    args = parser.parse_args(argv)

    book = build_codex_book()
    report = run_coverage_audit(book, _REPO_ROOT)
    inv = scan_repo_inventory(_REPO_ROOT)

    export_summary = None
    if args.all:
        export_summary = export_all_targets(book, report, _REPO_ROOT, inv["recipes_map"])

    if args.report:
        print(json.dumps({"coverage": report.to_dict(), "export": export_summary}, ensure_ascii=False, indent=2))
    else:
        print(
            f"[Codex Studio] chapters={report.stats['chapters']} "
            f"entries={report.stats['entries']} pages={report.stats['pages']} ok={report.ok}"
        )
        if export_summary:
            print(
                f"[Codex Export] modonomicon={export_summary['modonomicon']} "
                f"vanilla_volumes={export_summary['vanilla_volumes']}"
            )
        if not report.ok:
            if report.missing:
                print("[MISSING COVERAGE]:", json.dumps(report.missing, ensure_ascii=False, indent=2))
            if report.invalid_refs:
                print("[INVALID REFS]:", json.dumps(report.invalid_refs, ensure_ascii=False, indent=2))
            if report.recipe_pair_errors:
                print("[RECIPE PAIR ERRORS]:", json.dumps(report.recipe_pair_errors, ensure_ascii=False, indent=2))
            if report.budget_issues:
                print("[BUDGET OVERFLOWS]:", json.dumps(report.budget_issues, ensure_ascii=False, indent=2))

    if args.check and not report.ok:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
