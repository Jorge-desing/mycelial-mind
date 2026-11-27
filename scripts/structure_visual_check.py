"""Validates structure visual smoke captures with foreground checks, metadata contract,
SSIM/pHash/LPIPS regression against baselines, diff heatmaps, and a labeled 3x2 contact sheet.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.visual_regression import (  # noqa: E402
    compare_images,
    compute_lpips,
    save_diff_heatmap,
)

ISOLATED_SCREENSHOT_DIR = os.path.join(
    REPO_ROOT, "artifacts", "diagnostics", "structure-visual-client", "screenshots"
)
LEGACY_SCREENSHOT_DIR = os.path.join(REPO_ROOT, "run", "screenshots")
ARCHIVE_DIR = os.path.join(REPO_ROOT, "artifacts", "diagnostics", "structures")
DEFAULT_BASELINE_DIR = os.path.join(
    REPO_ROOT, "scripts", "model_studio", "exports", "structure_baselines"
)

EXPECTED_SHOTS: tuple[tuple[str, str], ...] = (
    ("structure_outbreak_and_hive.png", "Outbreak + Surface Hive"),
    ("outbreak_surface.png", "Surface outbreak"),
    ("outbreak_cave.png", "Cave outbreak"),
    ("structure_hive_sanctum.png", "Hive Sanctum (Exterior)"),
    ("structure_sanctum_interior.png", "Hive Sanctum (Interior)"),
    ("structure_matriarch_citadel.png", "Matriarch Citadel (Front)"),
    ("structure_citadel_interior.png", "Matriarch Citadel (Interior)"),
)


def _resolve_screenshot_dir(explicit: str | None) -> str:
    if explicit:
        return os.path.abspath(explicit) if os.path.isabs(explicit) else os.path.join(REPO_ROOT, explicit)
    marker = EXPECTED_SHOTS[0][0]
    iso_marker = os.path.join(ISOLATED_SCREENSHOT_DIR, marker)
    leg_marker = os.path.join(LEGACY_SCREENSHOT_DIR, marker)
    iso_mtime = os.path.getmtime(iso_marker) if os.path.isfile(iso_marker) else -1.0
    leg_mtime = os.path.getmtime(leg_marker) if os.path.isfile(leg_marker) else -1.0
    if leg_mtime > iso_mtime:
        return LEGACY_SCREENSHOT_DIR
    return ISOLATED_SCREENSHOT_DIR


def compute_foreground_ratio(rgb_arr: np.ndarray) -> float:
    """Return fraction of pixels that are neither flat-world green grass nor bright sky."""
    if rgb_arr.ndim != 3 or rgb_arr.shape[2] < 3 or rgb_arr.size == 0:
        return 0.0
    r = rgb_arr[..., 0].astype(np.int16)
    g = rgb_arr[..., 1].astype(np.int16)
    b = rgb_arr[..., 2].astype(np.int16)
    is_grass = (g > r + 10) & (g > b + 15) & (g < 175)
    is_sky = (b > r + 25) & (b > g + 5) & (r > 95)
    return float(np.mean(~(is_grass | is_sky)))


def _rel_or_abs(path: str) -> str:
    try:
        rel = os.path.relpath(path, REPO_ROOT)
        if not rel.startswith(".."):
            return rel.replace("\\", "/")
    except ValueError:
        pass
    return path.replace("\\", "/")


def build_contact_sheet(
    tiles: list[tuple[str, str, Image.Image, dict[str, Any]]],
    report: dict[str, Any],
    out_path: str,
) -> None:
    """Render a labeled 3x2 dark-theme contact sheet with a 6th summary tile."""
    cols, rows = 3, 2
    tile_w, tile_h = 480, 270
    header_h = 32
    pad = 12
    cell_w = tile_w
    cell_h = tile_h + header_h

    sheet_w = cols * cell_w + (cols + 1) * pad
    sheet_h = rows * cell_h + (rows + 1) * pad
    sheet = Image.new("RGB", (sheet_w, sheet_h), (20, 18, 28))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()

    for idx, (_fname, title, thumb, info) in enumerate(tiles[:5]):
        col = idx % cols
        row = idx // cols
        x0 = pad + col * (cell_w + pad)
        y0 = pad + row * (cell_h + pad)

        reg_ok = info.get("regression_ok", True)
        header_bg = (36, 30, 52) if reg_ok else (84, 24, 34)
        draw.rectangle([x0, y0, x0 + cell_w - 1, y0 + header_h - 1], fill=header_bg)
        fg_pct = info.get("foreground_ratio", 0.0) * 100.0
        if "ssim" in info:
            badge = f"SSIM={info['ssim']:.3f} fg={fg_pct:.0f}%"
        else:
            badge = f"fg={fg_pct:.0f}% std={info.get('std_luma', 0.0):.1f}"
        label = f"{idx + 1}. {title} | {badge}"
        draw.text((x0 + 8, y0 + 9), label, fill=(235, 232, 246), font=font)
        sheet.paste(thumb, (x0, y0 + header_h))

    # 6th slot: summary card
    sx0 = pad + 2 * (cell_w + pad)
    sy0 = pad + 1 * (cell_h + pad)
    overall_ok = bool(report.get("ok"))
    draw.rectangle([sx0, sy0, sx0 + cell_w - 1, sy0 + cell_h - 1], fill=(28, 24, 38), outline=(92, 68, 128))
    status_color = (110, 235, 160) if overall_ok else (255, 110, 120)
    status_text = "PASSED" if overall_ok else "FAILED"
    lines: list[tuple[str, tuple[int, int, int]]] = [
        (f"STRUCTURE VISUAL SMOKE: {status_text}", status_color),
        (f"Baseline mode: {report.get('baseline_mode', 'none')}", (220, 216, 235)),
        (f"Shots verified: {len(tiles)}/{len(EXPECTED_SHOTS)}", (220, 216, 235)),
    ]
    ssims = [info["ssim"] for _fn, _t, _im, info in tiles if "ssim" in info]
    if ssims:
        lines.append((f"Mean SSIM: {sum(ssims) / len(ssims):.4f} (min {min(ssims):.4f})", (200, 225, 255)))
    meta = report.get("meta")
    if isinstance(meta, dict):
        lines.append((
            f"Placed -> outbreak={meta.get('outbreak_placed')} hive={meta.get('hive_placed')}",
            (205, 200, 220),
        ))
        lines.append((
            f"          sanctum={meta.get('sanctum_placed')} citadel={meta.get('citadel_placed')}",
            (205, 200, 220),
        ))
        lines.append((f"Frozen Citadel guardians: {meta.get('frozen_guardians', 0)}", (205, 200, 220)))
    failed = report.get("failed_shots") or []
    if failed:
        lines.append((f"Failed: {', '.join(failed)}", (255, 130, 140)))
    errors = report.get("errors") or []
    for err in errors[:3]:
        lines.append((err[:62], (255, 145, 145)))

    for li, (text, color) in enumerate(lines):
        draw.text((sx0 + 16, sy0 + 18 + li * 26), text, fill=color, font=font)

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    sheet.save(out_path)


def validate_structure_shots(
    *,
    screenshot_dir: str | None = None,
    baseline_dir: str = DEFAULT_BASELINE_DIR,
    archive_dir: str = ARCHIVE_DIR,
    update_baseline: bool = False,
    require_baseline: bool = False,
    min_ssim: float = 0.90,
    max_hamming: int = 6,
    max_lpips: float = 0.35,
    min_foreground: float = 0.08,
    min_mtime: float = 0.0,
) -> tuple[int, dict[str, Any]]:
    resolved_shot_dir = _resolve_screenshot_dir(screenshot_dir)
    os.makedirs(archive_dir, exist_ok=True)

    errors: list[str] = []
    failed_shots: list[str] = []
    shots_report: dict[str, dict[str, Any]] = {}
    tiles: list[tuple[str, str, Image.Image, dict[str, Any]]] = []

    meta_path = os.path.join(resolved_shot_dir, "structure_smoke_meta.json")
    meta_data: dict[str, Any] | None = None
    if os.path.isfile(meta_path):
        try:
            with open(meta_path, "r", encoding="utf-8") as fh:
                meta_data = json.load(fh)
        except (OSError, json.JSONDecodeError) as exc:
            errors.append(f"Invalid metadata JSON {meta_path}: {exc}")
        else:
            if not meta_data.get("all_placed", False):
                errors.append(f"Metadata reports all_placed=False: {meta_data}")
            if meta_data.get("timed_out", False):
                errors.append("Metadata reports timed_out=True")

    # First pass: validate existence, freshness, dimensions, luminance, and foreground content
    valid_sources: dict[str, str] = {}
    for name, title in EXPECTED_SHOTS:
        src = os.path.join(resolved_shot_dir, name)
        if not os.path.isfile(src):
            errors.append(f"Missing structure screenshot: {src}")
            continue
        if min_mtime > 0.0 and os.path.getmtime(src) < min_mtime:
            errors.append(f"Stale screenshot (mtime < min_mtime): {name}")
            continue

        with Image.open(src) as im:
            rgb = im.convert("RGB")
            arr = np.asarray(rgb, dtype=np.float64)
            std = float(arr.std())
            mean = float(arr.mean())
            fg_ratio = compute_foreground_ratio(arr)
            w, h = rgb.size
            if w < 320 or h < 200 or std < 8.0:
                errors.append(f"Screenshot {name} looks blank/invalid (size={w}x{h}, std={std:.2f})")
                continue
            if fg_ratio < min_foreground:
                errors.append(
                    f"Screenshot {name} failed foreground guard ({fg_ratio:.3f} < {min_foreground:.3f}; empty terrain?)"
                )
                continue

            dst = os.path.join(archive_dir, name)
            shutil.copy2(src, dst)
            valid_sources[name] = src
            shots_report[name] = {
                "title": title,
                "width": w,
                "height": h,
                "mean_luma": round(mean, 2),
                "std_luma": round(std, 2),
                "foreground_ratio": round(fg_ratio, 4),
                "archived_path": _rel_or_abs(dst),
                "regression_ok": True,
            }
            thumb = rgb.resize((480, 270), Image.Resampling.LANCZOS)
            tiles.append((name, title, thumb, shots_report[name]))

    hard_failure = len(errors) > 0 or len(valid_sources) != len(EXPECTED_SHOTS)
    baseline_mode = "none"
    regression_failed = False

    if not hard_failure and update_baseline:
        os.makedirs(baseline_dir, exist_ok=True)
        for name, src in valid_sources.items():
            shutil.copy2(src, os.path.join(baseline_dir, name))
        baseline_mode = "updated"
    elif not hard_failure:
        compared_any = False
        missing_baselines = 0
        for name, src in valid_sources.items():
            base_path = os.path.join(baseline_dir, name)
            info = shots_report[name]
            if not os.path.isfile(base_path):
                missing_baselines += 1
                if require_baseline:
                    errors.append(f"Missing baseline for {name}: {base_path}")
                    info["regression_ok"] = False
                    failed_shots.append(name)
                    regression_failed = True
                continue

            compared_any = True
            ssim, phash_dist = compare_images(base_path, src)
            lpips_val = (
                None
                if (ssim >= 0.995 and phash_dist <= max_hamming)
                else compute_lpips(base_path, src)
            )
            info["ssim"] = round(ssim, 4)
            info["phash_hamming"] = int(phash_dist)
            info["lpips"] = round(lpips_val, 4) if lpips_val is not None else None

            effective_max_hamming = (
                max_hamming * 2
                if (ssim >= 0.985 and (lpips_val is None or lpips_val <= 0.05))
                else max_hamming
            )
            shot_ok = (
                ssim >= min_ssim
                and phash_dist <= effective_max_hamming
                and (lpips_val is None or lpips_val <= max_lpips)
            )
            if not shot_ok or ssim < 0.999:
                hm_path = os.path.join(archive_dir, f"diff_{name}")
                regions = save_diff_heatmap(base_path, src, hm_path)
                info["diff_regions"] = len(regions)
                info["heatmap_path"] = _rel_or_abs(hm_path)
            else:
                info["diff_regions"] = 0

            info["regression_ok"] = shot_ok
            if not shot_ok:
                regression_failed = True
                failed_shots.append(name)

        if compared_any and missing_baselines == 0:
            baseline_mode = "compared"
        elif compared_any:
            baseline_mode = "partial"
        else:
            baseline_mode = "missing_skipped" if not require_baseline else "missing_failed"

    overall_ok = (not hard_failure) and (not regression_failed)
    report: dict[str, Any] = {
        "ok": overall_ok,
        "screenshot_dir": _rel_or_abs(resolved_shot_dir),
        "baseline_dir": _rel_or_abs(baseline_dir),
        "baseline_mode": baseline_mode,
        "shots": shots_report,
        "failed_shots": failed_shots,
        "errors": errors,
    }
    if meta_data is not None:
        report["meta"] = meta_data

    sheet_path = os.path.join(archive_dir, "structures_contact_sheet.png")
    build_contact_sheet(tiles, report, sheet_path)
    report["contact_sheet"] = _rel_or_abs(sheet_path)

    manifest_path = os.path.join(archive_dir, "structure-visual-smoke-latest.json")
    with open(manifest_path, "w", encoding="utf-8") as fh:
        json.dump(report, fh, indent=2)

    if hard_failure:
        return 1, report
    if regression_failed:
        return 2, report
    return 0, report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Validate structure visual smoke screenshots and run SSIM/pHash/LPIPS regression."
    )
    parser.add_argument("--screenshot-dir", default=None, help="Directory containing structure_*.png captures")
    parser.add_argument("--baseline-dir", default=DEFAULT_BASELINE_DIR, help="Directory of golden baseline PNGs")
    parser.add_argument("--archive-dir", default=ARCHIVE_DIR, help="Output directory for archived shots and manifest")
    parser.add_argument("--update-baseline", action="store_true", help="Update golden baselines after passing checks")
    parser.add_argument("--require-baseline", action="store_true", help="Fail if any baseline PNG is missing")
    parser.add_argument("--min-ssim", type=float, default=0.90)
    parser.add_argument("--max-hamming", type=int, default=6)
    parser.add_argument("--max-lpips", type=float, default=0.35)
    parser.add_argument("--min-foreground", type=float, default=0.08)
    parser.add_argument("--min-mtime", type=float, default=0.0)
    args = parser.parse_args(argv)

    code, report = validate_structure_shots(
        screenshot_dir=args.screenshot_dir,
        baseline_dir=args.baseline_dir,
        archive_dir=args.archive_dir,
        update_baseline=args.update_baseline,
        require_baseline=args.require_baseline,
        min_ssim=args.min_ssim,
        max_hamming=args.max_hamming,
        max_lpips=args.max_lpips,
        min_foreground=args.min_foreground,
        min_mtime=args.min_mtime,
    )
    print(json.dumps(report, indent=2))
    if code == 0:
        print(
            f"PASSED: All {len(EXPECTED_SHOTS)} structure visual smoke screenshots verified "
            f"(baseline_mode={report['baseline_mode']})."
        )
    elif code == 2:
        print(f"FAILED: Structure visual regression threshold exceeded for {report['failed_shots']}.")
    else:
        print(f"ERROR: Structure visual smoke validation failed: {report['errors']}")
    return code


if __name__ == "__main__":
    sys.exit(main())
