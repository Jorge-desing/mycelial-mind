"""Unified CLI for Mycelial Mind's procedural HD texture studio."""

from __future__ import annotations

import argparse
import hashlib
import itertools
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.texture_studio.generators.reclaimed_blooms import generate_reclaimed_blooms
from scripts.texture_studio.generators.atlas_3d import ATLAS_SPECS, generate_3d_atlases
from scripts.texture_studio.generators.blocks import generate_blocks
from scripts.texture_studio.generators.entities import (
    generate_armor_layers,
    generate_armor_variants,
    generate_entities,
)
from scripts.texture_studio.generators.entity_skins import COLONY_SYMBIONT_CASTE_SKINS, ENTITY_SKINS, NO_RANK_ENTITIES, box_face_rects
from scripts.texture_studio.generators.gui import generate_gui_textures
from scripts.texture_studio.generators.items import generate_items, generate_mob_effects
from scripts.texture_studio.palette_oklab import export_gpl, export_hex

TEX_ROOT = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures")
EXPORTS_DIR = os.path.join(os.path.dirname(__file__), "exports")


def _draw_uv_box(draw: ImageDraw.ImageDraw, u: int, v: int, w: int, h: int, d: int, scale: int, col) -> None:
    u, v, w, h, d = u * scale, v * scale, w * scale, h * scale, d * scale
    rects = [
        (u + d, v, w, d),
        (u + d + w, v, w, d),
        (u, v + d, d, h),
        (u + d, v + d, w, h),
        (u + d + w, v + d, d, h),
        (u + d + w + d, v + d, w, h),
    ]
    for rx, ry, rw, rh in rects:
        draw.rectangle([rx, ry, rx + rw - 1, ry + rh - 1], outline=col)


def export_uv_guides() -> None:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    # Humanoid 128x128
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for (u, v, w, h, dep, col) in [
        (0, 0, 8, 8, 8, (80, 220, 255, 220)),
        (32, 0, 8, 8, 8, (255, 120, 255, 180)),
        (16, 16, 8, 12, 4, (120, 255, 140, 220)),
        (40, 16, 4, 12, 4, (255, 200, 90, 220)),
        (32, 48, 4, 12, 4, (255, 200, 90, 220)),
        (0, 16, 4, 12, 4, (180, 160, 255, 220)),
        (16, 48, 4, 12, 4, (180, 160, 255, 220)),
    ]:
        _draw_uv_box(d, u, v, w, h, dep, 2, col)
    im.save(os.path.join(EXPORTS_DIR, "uv_guide_humanoid_128.png"))

    # Spider 128x64
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for (u, v, w, h, dep, col) in [
        (0, 0, 6, 6, 6, (120, 255, 140, 220)),
        (0, 12, 10, 8, 12, (255, 120, 255, 220)),
        (18, 0, 16, 2, 2, (255, 200, 90, 220)),
        (32, 4, 8, 8, 8, (80, 220, 255, 220)),
    ]:
        _draw_uv_box(d, u, v, w, h, dep, 2, col)
    im.save(os.path.join(EXPORTS_DIR, "uv_guide_spider_128x64.png"))

    # Endermite 128x64
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for (u, v, w, h, dep, col) in [
        (0, 0, 4, 3, 2, (80, 220, 255, 220)),
        (0, 5, 6, 4, 5, (255, 120, 255, 220)),
        (0, 14, 3, 3, 1, (120, 255, 140, 220)),
        (0, 18, 1, 2, 1, (255, 200, 90, 220)),
    ]:
        _draw_uv_box(d, u, v, w, h, dep, 2, col)
    im.save(os.path.join(EXPORTS_DIR, "uv_guide_endermite_128x64.png"))

    # Silverfish 128x64
    im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for (u, v, w, h, dep, col) in [
        (0, 0, 3, 2, 2, (80, 220, 255, 220)),
        (0, 4, 4, 3, 2, (120, 255, 140, 220)),
        (0, 9, 6, 4, 3, (255, 120, 255, 220)),
        (0, 16, 3, 3, 3, (180, 160, 255, 220)),
        (0, 22, 2, 2, 3, (180, 160, 255, 220)),
        (11, 0, 2, 1, 2, (255, 200, 90, 220)),
        (13, 4, 1, 1, 2, (255, 200, 90, 220)),
        (20, 0, 10, 8, 2, (255, 140, 80, 220)),
        (20, 11, 6, 4, 2, (255, 140, 80, 220)),
        (20, 18, 4, 3, 2, (255, 140, 80, 220)),
    ]:
        _draw_uv_box(d, u, v, w, h, dep, 2, col)
    im.save(os.path.join(EXPORTS_DIR, "uv_guide_silverfish_128x64.png"))


def _checker_image(width: int, height: int) -> Image.Image:
    chk = Image.new("RGBA", (width, height), (40, 40, 50, 255))
    cdraw = ImageDraw.Draw(chk)
    for cy in range(0, height, 8):
        for cx in range(0, width, 8):
            color = (52, 52, 64, 255) if ((cx // 8 + cy // 8) % 2 == 0) else (36, 36, 44, 255)
            cdraw.rectangle([cx, cy, cx + 7, cy + 7], fill=color)
    return chk


def _load_entity_atlas(reg_id: str, rank_suffix: str) -> Image.Image:
    path = os.path.join(TEX_ROOT, "entity", f"{reg_id}{rank_suffix}.png")
    return Image.open(path).convert("RGBA")


def build_entity_contact_sheet() -> str:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    out_path = os.path.join(EXPORTS_DIR, "entity_contact_sheet.png")
    import json

    with open(os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json"), encoding="utf-8") as mf:
        manifest = json.load(mf)
    pad, label_h = 12, 16
    rows = []
    for reg_id, spec in manifest.items():
        ranks = ("",) if reg_id in NO_RANK_ENTITIES else ("", "_mutated", "_elite")
        images = []
        for suffix in ranks:
            im = _load_entity_atlas(reg_id, suffix)
            if im.width <= 128:
                im = im.resize((im.width * 2, im.height * 2), Image.NEAREST)
            images.append((f"{reg_id}{suffix or '_base'}", im))
        rows.append(images)
    max_cols = max(len(r) for r in rows)
    widths = [0] * max_cols
    heights = []
    for row in rows:
        for idx, (_, im) in enumerate(row):
            widths[idx] = max(widths[idx], im.width)
        heights.append(max(im.height for _, im in row))
    canvas_w = pad + sum(widths) + pad * max_cols
    canvas_h = pad + sum(h + label_h + pad for h in heights)
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (20, 20, 26, 255))
    draw = ImageDraw.Draw(canvas)
    y = pad
    for row, row_h in zip(rows, heights):
        x = pad
        for idx, (label, im) in enumerate(row):
            chk = _checker_image(im.width, im.height)
            chk.alpha_composite(im)
            canvas.paste(chk, (x, y))
            draw.rectangle([x - 1, y - 1, x + im.width, y + im.height], outline=(95, 95, 115, 255))
            draw.text((x, y + im.height + 2), label, fill=(190, 200, 215, 255))
            x += widths[idx] + pad
        y += row_h + label_h + pad
    canvas.save(out_path)
    return out_path


def _head_face_rect(spec: dict):
    for cube in spec["cubes"]:
        if cube.get("eye_pattern"):
            for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
                if rect.kind == "front":
                    return rect
    for cube in spec["cubes"]:
        if "head" in cube["name"] or "core" in cube["name"] or "crystal" in cube["name"]:
            for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
                if rect.kind == "front" and rect.w >= 3 and rect.h >= 3:
                    return rect
    return None


def build_entity_face_contact_sheet() -> str:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    out_path = os.path.join(EXPORTS_DIR, "entity_face_contact_sheet.png")
    import json

    with open(os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json"), encoding="utf-8") as mf:
        manifest = json.load(mf)
    pad, label_h = 10, 16
    rows = []
    for reg_id, spec in manifest.items():
        rect = _head_face_rect(spec)
        if rect is None:
            continue
        ranks = ("",) if reg_id in NO_RANK_ENTITIES else ("", "_mutated", "_elite")
        images = []
        for suffix in ranks:
            im = _load_entity_atlas(reg_id, suffix)
            crop = im.crop((rect.x, rect.y, rect.x + rect.w, rect.y + rect.h))
            zoom = max(4, min(12, 220 // max(rect.w, rect.h)))
            crop = crop.resize((rect.w * zoom, rect.h * zoom), Image.NEAREST)
            images.append((f"{reg_id}{suffix or '_base'} {rect.w}x{rect.h}", crop))
        rows.append(images)
    if not rows:
        raise RuntimeError("No head/core cube found for the entity face contact sheet")
    max_cols = max(len(r) for r in rows)
    widths = [0] * max_cols
    heights = []
    for row in rows:
        for idx, (_, im) in enumerate(row):
            widths[idx] = max(widths[idx], im.width)
        heights.append(max(im.height for _, im in row))
    canvas_w = pad + sum(widths) + pad * max_cols
    canvas_h = pad + sum(h + label_h + pad for h in heights)
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (20, 20, 26, 255))
    draw = ImageDraw.Draw(canvas)
    y = pad
    for row, row_h in zip(rows, heights):
        x = pad
        for idx, (label, im) in enumerate(row):
            chk = _checker_image(im.width, im.height)
            chk.alpha_composite(im)
            canvas.paste(chk, (x, y))
            draw.rectangle([x - 1, y - 1, x + im.width, y + im.height], outline=(95, 95, 115, 255))
            draw.text((x, y + im.height + 2), label, fill=(190, 200, 215, 255))
            x += widths[idx] + pad
        y += row_h + label_h + pad
    canvas.save(out_path)
    return out_path


def layout_contact_rows(entries, max_w: int, pad: int = 14, label_h: int = 16):
    """Pure two-pass contact-sheet layout that never overflows 512px atlases."""
    rows = []
    current = []
    current_w = 0
    current_h = 0
    for label, image, scale in entries:
        width = max(1, int(image.width * scale))
        height = max(1, int(image.height * scale))
        if current and current_w + width + pad > max_w:
            rows.append((current, current_w, current_h))
            current, current_w, current_h = [], 0, 0
        current.append((label, image, width, height))
        current_w += width + pad
        current_h = max(current_h, height)
    if current:
        rows.append((current, current_w, current_h))
    canvas_h = sum(height + label_h + pad for _, _, height in rows) + pad * 2
    return rows, max_w, canvas_h


def _paste_contact_rows(canvas, draw, y_cursor: int, rows, pad: int = 14, label_h: int = 16) -> int:
    for row, _row_w, row_h in rows:
        x_cursor = pad
        for label, image, width, height in row:
            scaled = image if image.size == (width, height) else image.resize((width, height), Image.NEAREST)
            chk = _checker_image(width, height)
            chk.alpha_composite(scaled)
            canvas.paste(chk, (x_cursor, y_cursor))
            draw.rectangle([x_cursor - 1, y_cursor - 1, x_cursor + width, y_cursor + height], outline=(95, 95, 115, 255))
            draw.text((x_cursor, y_cursor + height + 2), label, fill=(190, 200, 215, 255))
            x_cursor += width + pad
        y_cursor += row_h + label_h + pad
    return y_cursor


def build_contact_sheet() -> str:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    out_path = os.path.join(EXPORTS_DIR, "contact_sheet_studio.png")
    pad, label_h, max_w = 14, 16, 1120
    sections = []
    for title, folder, scale, crop_first in [
        ("STUDIO BLOCKS (16x16 -> 3x, Frame 0)", os.path.join(TEX_ROOT, "block"), 3, True),
        ("STUDIO ITEMS (16x16 -> 3x, Frame 0)", os.path.join(TEX_ROOT, "item"), 3, True),
        ("STUDIO ENTITIES + EMISSIVES (128x64 / 256x256 / 512x512)", os.path.join(TEX_ROOT, "entity"), 1, False),
    ]:
        entries = []
        for name in sorted(os.listdir(folder)):
            if not name.endswith(".png"):
                continue
            im = Image.open(os.path.join(folder, name)).convert("RGBA")
            if crop_first and im.height > im.width:
                im = im.crop((0, 0, im.width, im.width))
            image_scale = scale
            if title.startswith("STUDIO ENTITIES") and max(im.width, im.height) > 256:
                image_scale = 0.5
            entries.append((name.replace(".png", ""), im, image_scale))
        sections.append((title, entries))

    rank_units = (
        "sporeling", "leech_parasite", "pyro_shroom", "burrower", "acid_spitter",
        "chitin_brute", "mycelial_stalker", "synapse_weaver", "goliath_carrier", "colony_warden", "spore_architect", "spore_behemoth",
    )
    entity_folder = os.path.join(TEX_ROOT, "entity")
    rank_entries = []
    for unit_id in rank_units:
        for rank_suf, tag in (("", "BASE"), ("_mutated", "MUTATED"), ("_elite", "ELITE")):
            im = Image.open(os.path.join(entity_folder, f"{unit_id}{rank_suf}.png")).convert("RGBA")
            rank_entries.append((f"{unit_id} [{tag}]", im, 0.5 if im.width > 256 else 1.0))
    sections.append(("RANK VARIANTS (BASE | MUTATED | ELITE)", rank_entries))

    laid = []
    total_h = pad
    for title, entries in sections:
        rows, _canvas_w, canvas_h = layout_contact_rows(entries, max_w, pad=pad, label_h=label_h)
        laid.append((title, rows, canvas_h))
        total_h += 22 + canvas_h
    canvas = Image.new("RGBA", (max_w, total_h + pad), (20, 20, 26, 255))
    draw = ImageDraw.Draw(canvas)
    y_cursor = pad
    for title, rows, _canvas_h in laid:
        draw.text((pad, y_cursor), title, fill=(220, 235, 255, 255) if "RANK" not in title else (255, 210, 140, 255))
        y_cursor += 22
        y_cursor = _paste_contact_rows(canvas, draw, y_cursor, rows, pad=pad, label_h=label_h)
    canvas.save(out_path)
    return out_path


ITEM_ANIMATED = {
    "bio_scanner", "mycelial_codex", "enzyme_lance", "neutralizing_spore_launcher", "symbiotic_blade",
    "spore_antidote", "purifier_grenade", "hive_heart", "synaptic_suppressor", "sovereign_command_staff",
}
ITEM_MINIMUM = {
    "bio_scanner", "mycelial_codex", "enzyme_lance", "neutralizing_spore_launcher",
    "symbiotic_blade", "spore_antidote", "purifier_grenade", "tide_beacon",
    "bio_extractor", "chitin_helmet", "chitin_chestplate", "chitin_leggings",
    "chitin_boots", "chitin_plate", "concentrated_antidote", "counter_serum",
    "genetic_sample", "symbiotic_mutagen", "hive_heart", "latent_spore",
    "mycelial_biomass", "overmind_essence", "rift_catalyst", "synaptic_core",
    "overmind_crown", "purifier_drone_core", "synaptic_suppressor", "neural_echo_core",
    "synaptic_graft", "neural_echo_decoy", "schism_injector", "truce_pheromone",
    "quorum_seal", "chimera_ovum", "volatile_engram", "false_engram_injector",
    "sovereign_command_staff",
}


def _item_icon_arrays(check_pbr: bool = False) -> tuple[list[str], dict[str, np.ndarray]]:
    item_dir = os.path.join(TEX_ROOT, "item")
    names = sorted(
        name[:-4]
        for name in os.listdir(item_dir)
        if name.endswith(".png") and not name[:-4].endswith(("_n", "_s"))
    )
    missing = ITEM_MINIMUM - set(names)
    assert not missing, f"Missing item icons: {sorted(missing)}"
    assert len(names) >= 36, f"Expected at least 36 item icons, found {len(names)}"
    arrays: dict[str, np.ndarray] = {}
    for name in names:
        image = Image.open(os.path.join(item_dir, f"{name}.png")).convert("RGBA")
        if image.height > image.width:
            assert image.size == (16, 64), f"{name} expected 16x64 flipbook, got {image.size}"
            image = image.crop((0, 0, 16, 16))
        else:
            assert image.size == (16, 16), f"{name} expected 16x16, got {image.size}"
        arrays[name] = np.asarray(image)
        if check_pbr:
            for suffix in ("_n", "_s"):
                pbr = Image.open(os.path.join(item_dir, f"{name}{suffix}.png"))
                assert pbr.size == (16, 16), f"{name}{suffix}.png expected 16x16, got {pbr.size}"
    return names, arrays


BLOCK_ANIMATED = {
    "creeping_mycelium_top", "spore_vent_top", "purification_pylon_side", "mycelial_node",
    "incubator_node", "sentinel_node", "symbiotic_node", "mycelial_rift",
    "luminescent_amber_vein", "antibody_cyst", "sovereign_nexus_top",
}
BLOCK_CUTOUT = {
    "bio_chitin_lattice", "bioluminescent_mycelial_cap", "mycelial_tendrils", "spore_bulb",
    "hyphal_vein_dot", "hyphal_vein_line", "hyphal_vein_climb",
}


def check_block_textures(check_pbr: bool = False) -> None:
    """Validate every block family: dimensions, flipbook metadata, opacity rules and richness."""
    block_dir = os.path.join(TEX_ROOT, "block")
    names = sorted(
        name[:-4] for name in os.listdir(block_dir)
        if name.endswith(".png") and not name[:-4].endswith(("_n", "_s"))
    )
    assert len(names) >= 31, f"Expected at least 31 block textures, found {len(names)}"
    for name in names:
        path = os.path.join(block_dir, f"{name}.png")
        image = Image.open(path).convert("RGBA")
        if name in BLOCK_ANIMATED:
            assert image.size == (16, 64), f"{name} expected 16x64 flipbook, got {image.size}"
            assert os.path.exists(path + ".mcmeta"), f"Missing .mcmeta for animated block {name}"
            image = image.crop((0, 0, 16, 16))
        else:
            assert image.size == (16, 16), f"{name} expected 16x16, got {image.size}"
        rgba = np.asarray(image)
        alpha = rgba[..., 3] > 0
        coverage = float(alpha.mean())
        if name in BLOCK_CUTOUT:
            assert 0.05 <= coverage <= 0.95, f"{name} cutout coverage {coverage:.2f} outside [0.05, 0.95]"
        else:
            assert coverage == 1.0, f"{name} must be fully opaque (coverage {coverage:.2f})"
        luminance = 0.2126 * rgba[..., 0] + 0.7152 * rgba[..., 1] + 0.0722 * rgba[..., 2]
        values = luminance[alpha]
        contrast = float(np.percentile(values, 90) - np.percentile(values, 10))
        assert contrast >= 35.0, f"{name} contrast {contrast:.1f} below 35 (flat texture)"
        colors = len({tuple(int(c) for c in row) for row in rgba[alpha][:, :3]})
        assert colors >= 6, f"{name} only uses {colors} colours (needs >= 6)"
        if check_pbr:
            for suffix in ("_n", "_s"):
                pbr_path = os.path.join(block_dir, f"{name}{suffix}.png")
                pbr = Image.open(pbr_path)
                assert pbr.size == (16, 16), f"{pbr_path} expected 16x16, got {pbr.size}"
                pbr_rgba = np.asarray(pbr.convert("RGBA"))
                if suffix == "_n":
                    assert int(pbr_rgba[..., 3].min()) >= 16, f"{pbr_path} invalid POM alpha"
                else:
                    assert not np.any(pbr_rgba[..., 1] == 255), f"{pbr_path} invalid F0=255"
    print(f"Block family checks PASSED ({len(names)} textures, {len(BLOCK_ANIMATED)} animated, {len(BLOCK_CUTOUT)} cutout).")


def check_item_icon_language(check_pbr: bool = False) -> None:
    """Enforce the icon-language contract: readable silhouettes, unique shapes and rich detail."""
    names, arrays = _item_icon_arrays(check_pbr=check_pbr)
    masks: dict[str, np.ndarray] = {}
    for name, rgba in arrays.items():
        alpha = rgba[..., 3] > 0
        masks[name] = alpha
        ys, xs = np.nonzero(alpha)
        coverage = float(alpha.sum()) / 256.0
        assert 0.10 <= coverage <= 0.55, f"{name} silhouette coverage {coverage:.3f} outside [0.10, 0.55]"
        margin = min(int(xs.min()), int(ys.min()), 15 - int(xs.max()), 15 - int(ys.max()))
        assert margin >= 1, f"{name} touches the texture border (margin {margin})"
        if name not in ITEM_ANIMATED:
            assert float(alpha.mean()) < 0.6, f"{name} fills too much of the canvas"
        luminance = 0.2126 * rgba[..., 0] + 0.7152 * rgba[..., 1] + 0.0722 * rgba[..., 2]
        values = luminance[alpha]
        contrast = float(np.percentile(values, 90) - np.percentile(values, 10))
        assert contrast >= 90.0, f"{name} contrast {contrast:.1f} below 90 (muddy silhouette)"
        opaque = rgba[alpha][:, :3]
        unique_colors = len({tuple(int(c) for c in row) for row in opaque})
        assert unique_colors >= 5, f"{name} only uses {unique_colors} colours (needs >= 5)"
    worst = 0.0
    worst_pair = ("", "")
    for left, right in itertools.combinations(names, 2):
        intersection = np.logical_and(masks[left], masks[right]).sum()
        union = np.logical_or(masks[left], masks[right]).sum()
        iou = float(intersection) / float(union) if union else 0.0
        if iou > worst:
            worst, worst_pair = iou, (left, right)
    assert worst <= 0.95, f"Icons {worst_pair[0]} and {worst_pair[1]} share a silhouette (IoU {worst:.3f})"
    model_dirs = [
        d for d in (
            os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "models", "item"),
            os.path.join(REPO_ROOT, "src", "generated", "resources", "assets", "mycelialmind", "models", "item"),
        )
        if os.path.isdir(d)
    ]
    codex_model = None
    for model_dir in model_dirs:
        for filename in sorted(os.listdir(model_dir)):
            if not filename.endswith(".json"):
                continue
            with open(os.path.join(model_dir, filename), encoding="utf-8") as handle:
                model = json.load(handle)
            textures = model.get("textures", {})
            for key, value in textures.items():
                assert not value.startswith("minecraft:item/"), f"{filename} still uses vanilla texture {key}={value}"
            if filename == "mycelial_codex.json":
                codex_model = model
    assert codex_model is not None, "Missing mycelial_codex.json in main/generated models/item"
    assert codex_model["textures"]["layer0"] == "mycelialmind:item/mycelial_codex", "Codex icon must use the studio texture"
    print(f"Icon language checks PASSED ({len(names)} icons, worst silhouette IoU {worst:.3f}).")


def _build_texture_contact_sheet(texture_type: str, output_name: str) -> str:
    os.makedirs(EXPORTS_DIR, exist_ok=True)
    out_path = os.path.join(EXPORTS_DIR, output_name)
    texture_dir = os.path.join(TEX_ROOT, texture_type)
    names = sorted(
        name[:-4] for name in os.listdir(texture_dir)
        if name.endswith(".png") and not name[:-4].endswith(("_n", "_s"))
    )
    scale, cols, pad, label_h = 8, 6, 10, 14
    cell_w, cell_h = 16 * scale + pad, 16 * scale + label_h + pad
    rows = (len(names) + cols - 1) // cols
    canvas = Image.new("RGBA", (cols * cell_w + pad, rows * cell_h + pad), (20, 20, 26, 255))
    draw = ImageDraw.Draw(canvas)
    for index, name in enumerate(names):
        image = Image.open(os.path.join(texture_dir, f"{name}.png")).convert("RGBA")
        if image.height > image.width:
            image = image.crop((0, 0, 16, 16))
        image = image.resize((16 * scale, 16 * scale), Image.NEAREST)
        checker = _checker_image(image.width, image.height)
        checker.alpha_composite(image)
        x = pad + (index % cols) * cell_w
        y = pad + (index // cols) * cell_h
        canvas.paste(checker, (x, y))
        draw.rectangle([x - 1, y - 1, x + image.width, y + image.height], outline=(95, 95, 115, 255))
        draw.text((x, y + image.height + 2), name, fill=(200, 210, 225, 255))
    canvas.save(out_path)
    return out_path


def build_item_contact_sheet() -> str:
    return _build_texture_contact_sheet("item", "item_icon_contact_sheet.png")


def build_block_contact_sheet() -> str:
    return _build_texture_contact_sheet("block", "block_contact_sheet.png")


def run_checks(check_pbr: bool = False) -> None:
    expected_animated_blocks = [
        os.path.join(TEX_ROOT, "block", "creeping_mycelium_top.png"),
        os.path.join(TEX_ROOT, "block", "spore_vent_top.png"),
        os.path.join(TEX_ROOT, "block", "purification_pylon_side.png"),
        os.path.join(TEX_ROOT, "block", "mycelial_node.png"),
        os.path.join(TEX_ROOT, "block", "incubator_node.png"),
        os.path.join(TEX_ROOT, "block", "sentinel_node.png"),
        os.path.join(TEX_ROOT, "block", "symbiotic_node.png"),
        os.path.join(TEX_ROOT, "block", "mycelial_rift.png"),
    ]
    for p in expected_animated_blocks:
        im = Image.open(p)
        assert im.size == (16, 64), f"{p} expected (16, 64), got {im.size}"
        assert os.path.exists(p + ".mcmeta"), f"Missing .mcmeta for {p}"

    expected_animated_items = [
        os.path.join(TEX_ROOT, "item", f"{n}.png")
        for n in ("bio_scanner", "mycelial_codex", "enzyme_lance", "neutralizing_spore_launcher", "symbiotic_blade", "spore_antidote", "purifier_grenade", "hive_heart", "synaptic_suppressor")
    ]
    for p in expected_animated_items:
        im = Image.open(p)
        assert im.size == (16, 64), f"{p} expected (16, 64), got {im.size}"
        assert os.path.exists(p + ".mcmeta"), f"Missing .mcmeta for {p}"

    import json
    uv_manifest_path = os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json")
    with open(uv_manifest_path, encoding="utf-8") as mf:
        uv_manifest = json.load(mf)

    entity_summary = []
    boss_names = {"hive_core", "apex_matriarch", "spore_behemoth", "purifier_drone"}
    for name, spec in uv_manifest.items():
        assert name in ENTITY_SKINS, f"Missing EntitySkin profile for {name}"
        expected_size = tuple(spec["tex_size"])
        ranks = ("",) if name in NO_RANK_ENTITIES else ("", "_mutated", "_elite")
        rank_arrays = {}
        rank_emissive = {}
        for r in ranks:
            base_path = os.path.join(TEX_ROOT, "entity", f"{name}{r}.png")
            emis_path = os.path.join(TEX_ROOT, "entity", f"{name}{r}_emissive.png")
            base_im = Image.open(base_path).convert("RGBA")
            emis_im = Image.open(emis_path).convert("RGBA")
            assert base_im.size == expected_size, f"{base_path} expected {expected_size}, got {base_im.size}"
            assert emis_im.size == expected_size, f"{emis_path} expected {expected_size}, got {emis_im.size}"
            base = np.asarray(base_im)
            emis = np.asarray(emis_im)
            islands = np.zeros(base.shape[:2], dtype=bool)
            for cube in spec["cubes"]:
                for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
                    islands[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w] = True
            assert np.all(base[..., 3][islands] == 255), f"{base_path} has transparent texels inside UV islands"
            assert not np.any(base[..., 3][~islands] > 0), f"{base_path} has opaque texels outside UV islands"
            ealpha = emis[..., 3]
            assert not np.any(ealpha[~islands] > 0), f"{emis_path} has emissive texels outside UV islands"
            emissive_pixels = int(np.count_nonzero(ealpha > 8))
            assert emissive_pixels > 0, f"{emis_path} has no emissive texels"
            ratio = emissive_pixels / max(1, int(np.count_nonzero(islands)))
            limit = 0.50 if name in boss_names else 0.35
            assert ratio <= limit, f"{emis_path} emissive coverage {ratio:.3f} exceeds {limit:.2f}"
            rank_arrays[r] = base
            rank_emissive[r] = ratio
            if check_pbr:
                for suf in ("_n", "_s"):
                    pbr_path = os.path.join(TEX_ROOT, "entity", f"{name}{r}{suf}.png")
                    pbr_im = Image.open(pbr_path)
                    assert pbr_im.size == expected_size, f"{pbr_path} expected {expected_size}, got {pbr_im.size}"
                    pbr = np.asarray(pbr_im.convert("RGBA"))
                    if suf == "_n":
                        assert int(pbr[..., 3].min()) >= 16, f"{pbr_path} invalid POM alpha"
                    else:
                        assert not np.any(pbr[..., 1] == 255), f"{pbr_path} invalid F0=255"
        if len(rank_arrays) > 1:
            digests = [hashlib.sha1(arr.tobytes()).hexdigest() for arr in rank_arrays.values()]
            assert len(set(digests)) == len(digests), f"{name} rank variants are pixel-identical"
        entity_summary.append(
            name + ":" + ",".join(f"{key or 'base'}:{value:.3f}" for key, value in rank_emissive.items())
        )

        if name == "colony_symbiont":
            from scripts.texture_studio.palette_oklab import rgb_to_oklab

            caste_arrays = {}
            islands = np.zeros((expected_size[1], expected_size[0]), dtype=bool)
            for cube in spec["cubes"]:
                for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
                    islands[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w] = True
            for caste_id in COLONY_SYMBIONT_CASTE_SKINS:
                c_base_path = os.path.join(TEX_ROOT, "entity", f"colony_symbiont_{caste_id}.png")
                c_emis_path = os.path.join(TEX_ROOT, "entity", f"colony_symbiont_{caste_id}_emissive.png")
                c_base_im = Image.open(c_base_path).convert("RGBA")
                c_emis_im = Image.open(c_emis_path).convert("RGBA")
                assert c_base_im.size == expected_size, f"{c_base_path} expected {expected_size}, got {c_base_im.size}"
                assert c_emis_im.size == expected_size, f"{c_emis_path} expected {expected_size}, got {c_emis_im.size}"
                c_base = np.asarray(c_base_im)
                c_emis = np.asarray(c_emis_im)
                assert np.all(c_base[..., 3][islands] == 255), f"{c_base_path} has transparent texels inside UV islands"
                assert not np.any(c_base[..., 3][~islands] > 0), f"{c_base_path} has opaque texels outside UV islands"
                c_ealpha = c_emis[..., 3]
                assert not np.any(c_ealpha[~islands] > 0), f"{c_emis_path} has emissive texels outside UV islands"
                c_emis_px = int(np.count_nonzero(c_ealpha > 8))
                assert c_emis_px > 0, f"{c_emis_path} has no emissive texels"
                c_ratio = c_emis_px / max(1, int(np.count_nonzero(islands)))
                assert c_ratio <= 0.35, f"{c_emis_path} emissive coverage {c_ratio:.3f} exceeds 0.35"
                if check_pbr:
                    for suf in ("_n", "_s"):
                        c_pbr_path = os.path.join(TEX_ROOT, "entity", f"colony_symbiont_{caste_id}{suf}.png")
                        c_pbr_im = Image.open(c_pbr_path)
                        assert c_pbr_im.size == expected_size, f"{c_pbr_path} expected {expected_size}, got {c_pbr_im.size}"
                        c_pbr = np.asarray(c_pbr_im.convert("RGBA"))
                        if suf == "_n":
                            assert int(c_pbr[..., 3].min()) >= 16, f"{c_pbr_path} invalid POM alpha"
                        else:
                            assert not np.any(c_pbr[..., 1] == 255), f"{c_pbr_path} invalid F0=255"
                caste_arrays[caste_id] = c_base
            caste_keys = sorted(caste_arrays)
            for i in range(len(caste_keys)):
                for j in range(i + 1, len(caste_keys)):
                    a = caste_arrays[caste_keys[i]]
                    b = caste_arrays[caste_keys[j]]
                    m = a[..., 3] > 0
                    lab_a = rgb_to_oklab(a[..., :3][m].astype(np.float32) / 255.0)
                    lab_b = rgb_to_oklab(b[..., :3][m].astype(np.float32) / 255.0)
                    mean_delta = float(np.linalg.norm(lab_a - lab_b, axis=1).mean())
                    assert mean_delta > 0.04, f"Castes {caste_keys[i]} vs {caste_keys[j]} delta {mean_delta:.3f} too small"
    print("Entity atlas checks PASSED (" + str(len(entity_summary)) + " species):", "; ".join(entity_summary))
    assert Image.open(os.path.join(TEX_ROOT, "entity", "acid_spit.png")).size == (32, 32)
    for idx in (1, 2):
        for r in ("", "_mutated", "_elite"):
            assert Image.open(os.path.join(TEX_ROOT, "models", "armor", f"chitin_layer_{idx}{r}.png")).size == (64, 32)
    if check_pbr:
        pbr_blocks = (
            "creeping_mycelium_top", "creeping_mycelium_side", "creeping_mycelium_bottom",
            "spore_vent_top", "spore_vent_side", "spore_vent_bottom",
            "purification_pylon_top", "purification_pylon_side",
            "mycelial_node", "reinforced_bio_barrier",
        )
        for bname in pbr_blocks:
            for suf in ("_n", "_s"):
                np_path = os.path.join(TEX_ROOT, "block", f"{bname}{suf}.png")
                im = Image.open(np_path)
                assert im.size == (16, 16), f"{np_path} expected (16, 16), got {im.size}"
                if suf == "_n":
                    a_min, a_max = im.getextrema()[3]
                    assert a_min >= 16 and a_min < a_max, f"{np_path} invalid POM alpha range ({a_min}, {a_max})"
        pbr_items = (
            "bio_scanner", "enzyme_lance", "neutralizing_spore_launcher",
            "symbiotic_blade", "spore_antidote", "purifier_grenade",
        )
        for iname in pbr_items:
            for suf in ("_n", "_s"):
                ip_path = os.path.join(TEX_ROOT, "item", f"{iname}{suf}.png")
                im = Image.open(ip_path)
                assert im.size == (16, 16), f"{ip_path} expected (16, 16), got {im.size}"
        for ename in ("purifier_drone", "chitin_brute", "hive_core", "apex_matriarch"):
            esize = tuple(uv_manifest[ename]["tex_size"])
            for suf in ("_n", "_s"):
                ep_path = os.path.join(TEX_ROOT, "entity", f"{ename}{suf}.png")
                im = Image.open(ep_path)
                assert im.size == esize, f"{ep_path} expected {esize}, got {im.size}"
    atlas_dir = os.path.join(TEX_ROOT, "model_atlas")
    for aname in ATLAS_SPECS:
        ap = os.path.join(atlas_dir, f"{aname}.png")
        assert Image.open(ap).size == (64, 64), f"{ap} expected (64, 64)"
        if check_pbr:
            for suf in ("_n", "_s"):
                assert Image.open(os.path.join(atlas_dir, f"{aname}{suf}.png")).size == (64, 64)
    flow_p = os.path.join(atlas_dir, "crystal_pulse_flow.png")
    assert Image.open(flow_p).size == (64, 256), f"{flow_p} expected (64, 256)"
    assert os.path.exists(flow_p + ".mcmeta"), f"Missing .mcmeta for {flow_p}"
    for ename in ("mycelial_infection", "spore_rot", "synaptic_insight", "synaptic_vision"):
        ep = os.path.join(TEX_ROOT, "mob_effect", f"{ename}.png")
        assert os.path.exists(ep), f"Missing mob_effect icon: {ep}"
        im = Image.open(ep)
        assert im.size == (18, 18), f"{ep} expected (18, 18), got {im.size}"
        assert im.getextrema()[3][1] > 0, f"{ep} has no opaque pixels"
    codex_gui_variants = (
        "codex_book.png",
        "codex_book_awakened.png",
        "codex_book_adaptive.png",
        "codex_book_overmind.png",
    )
    gui_hashes = set()
    for fname in codex_gui_variants:
        codex_gui_path = os.path.join(TEX_ROOT, "gui", fname)
        assert os.path.exists(codex_gui_path), f"Missing Codex GUI texture: {codex_gui_path}"
        codex_gui_im = Image.open(codex_gui_path).convert("RGBA")
        assert codex_gui_im.size == (256, 256), f"{codex_gui_path} expected (256, 256), got {codex_gui_im.size}"
        gui_hashes.add(hashlib.sha1(np.asarray(codex_gui_im).tobytes()).hexdigest())
    assert len(gui_hashes) == len(codex_gui_variants), "Codex GUI evolution atlases must be visually distinct"
    check_block_textures(check_pbr=check_pbr)
    check_item_icon_language(check_pbr=check_pbr)
    print("All texture dimension, .mcmeta, and emissive pair checks PASSED.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Mycelial Mind Procedural HD Texture Studio")
    parser.add_argument("--all", action="store_true", help="Generate blocks, items, and entities")
    parser.add_argument("--blocks", action="store_true", help="Generate block textures")
    parser.add_argument("--items", action="store_true", help="Generate item textures")
    parser.add_argument("--entities", action="store_true", help="Generate entity textures + emissives")
    parser.add_argument("--export-extras", action="store_true", help="Export .gpl/.hex palettes, UV guides, and contact sheet")
    parser.add_argument("--pbr", action="store_true", help="Export LabPBR _n.png normal+POM maps into mod block textures")
    parser.add_argument("--check", action="store_true", help="Validate dimensions, .mcmeta, and emissives")
    args = parser.parse_args()

    block_dir = os.path.join(TEX_ROOT, "block")
    item_dir = os.path.join(TEX_ROOT, "item")
    atlas_dir = os.path.join(TEX_ROOT, "model_atlas")
    if args.all or args.blocks or args.items:
        generate_3d_atlases(atlas_dir, emit_pbr=args.pbr)
        generate_reclaimed_blooms(atlas_dir, emit_pbr=args.pbr)
    if args.all or args.blocks:
        generate_blocks(block_dir, pbr_dir=block_dir if args.pbr else None)
    if args.all or args.items:
        generate_items(item_dir, pbr_dir=item_dir if args.pbr else None)
        generate_mob_effects(os.path.join(TEX_ROOT, "mob_effect"))
        generate_gui_textures(os.path.join(TEX_ROOT, "gui"))
    if args.all or args.entities:
        ent_dir = os.path.join(TEX_ROOT, "entity")
        generate_entities(ent_dir, pbr_dir=ent_dir if args.pbr else None)
        armor_dir = os.path.join(TEX_ROOT, "models", "armor")
        generate_armor_layers(armor_dir)
        generate_armor_variants(armor_dir)
    if args.export_extras:
        export_gpl(os.path.join(EXPORTS_DIR, "mycelial_mind.gpl"))
        export_hex(os.path.join(EXPORTS_DIR, "mycelial_mind.hex"))
        export_uv_guides()
        sheet = build_contact_sheet()
        entity_sheet = build_entity_contact_sheet()
        face_sheet = build_entity_face_contact_sheet()
        icon_sheet = build_item_contact_sheet()
        block_sheet = build_block_contact_sheet()
        print("Exported extras & contact sheets:", sheet, entity_sheet, face_sheet, icon_sheet, block_sheet)
    if args.check:
        run_checks(check_pbr=args.pbr)


if __name__ == "__main__":
    main()