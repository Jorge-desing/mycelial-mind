"""Manifest-driven entity skin + emissive map generator synchronized 1:1 with scripts/model_studio."""

from __future__ import annotations

import json
import math
import os

import numpy as np

from ..compositor import apply_emissive_bloom, channels_to_labpbr_s, normals_to_labpbr
from ..palette_oklab import PALETTES, hex_to_rgb01, oklab_to_rgb, rgb_to_oklab
from ..sdf_engine import (
    bayer_quantize,
    cell_plate_field,
    compute_height_normals,
    compute_sdf_curvature,
    compute_sdf_lighting_v2,
    make_grid,
    sdf_box,
    save_rgba,
    toroidal_fbm,
)
from .entity_skins import (
    ENTITY_SKINS,
    NO_RANK_ENTITIES,
    FaceContext,
    adjust_ramp,
    box_face_rects,
    paint_face,
)

UV_MANIFEST_PATH = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "..", "model_studio", "exports", "uv_manifest.json")
)

RANK_ORDER = ("base", "mutated", "elite")
RANK_SUFFIX = {"base": "", "mutated": "_mutated", "elite": "_elite"}
RANK_SEED = {"base": 0, "mutated": 101, "elite": 211}


def _fill_rect_bayer(canvas: np.ndarray, x: int, y: int, w: int, h: int, ramp: np.ndarray,
                     base_lit: float, seed: int) -> None:
    x, y, w, h = int(x), int(y), int(w), int(h)
    if w <= 0 or h <= 0:
        return
    h_max, w_max = canvas.shape[:2]
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(w_max, x + w), min(h_max, y + h)
    rw, rh = x1 - x0, y1 - y0
    if rw <= 0 or rh <= 0:
        return
    if rw < 2 or rh < 2:
        idx = min(len(ramp) - 1, max(0, int(base_lit * len(ramp))))
        canvas[y0:y1, x0:x1] = ramp[idx]
        return
    xx, yy = make_grid(rw, rh)
    d = sdf_box(xx, yy, 0.0, 0.0, 0.92, 0.92)
    noise = toroidal_fbm(max(rw, 8), max(rh, 8), seed=seed, freq=2.0)[:rh, :rw]
    curv = np.clip(compute_sdf_curvature(d) * 0.14, -0.08, 0.14)
    bevel = np.clip(compute_sdf_lighting_v2(d, specular_strength=0.22, sss_strength=0.10, ao_strength=0.25) + curv, 0.0, 1.0)
    field = np.clip(base_lit * bevel * 0.85 + noise * 0.18, 0.0, 1.0)
    canvas[y0:y1, x0:x1] = bayer_quantize(field, ramp, dither_strength=0.20, matrix=8)


def _oklab_shift_rgba(rgba: np.ndarray, hue_deg: float = 0.0, chroma_scale: float = 1.0,
                      light_delta: float = 0.0, tint_hex: str | None = None, tint_amount: float = 0.0) -> np.ndarray:
    out = rgba.copy()
    mask = out[..., 3] > 0
    if not np.any(mask):
        return out
    rgb = out[mask, :3].astype(np.float32) / 255.0
    lab = rgb_to_oklab(rgb)
    L, a, b = lab[..., 0], lab[..., 1], lab[..., 2]
    if abs(hue_deg) > 1e-3:
        rad = math.radians(hue_deg)
        ca, sa = math.cos(rad), math.sin(rad)
        a, b = a * ca - b * sa, a * sa + b * ca
    a = a * chroma_scale
    b = b * chroma_scale
    L = np.clip(L + light_delta, 0.0, 1.0)
    shifted_lab = np.stack([L, a, b], axis=-1)
    if tint_hex is not None and tint_amount > 0.0:
        t_lab = rgb_to_oklab(hex_to_rgb01(tint_hex))
        shifted_lab = shifted_lab * (1.0 - tint_amount) + t_lab * tint_amount
    new_rgb = np.clip(oklab_to_rgb(shifted_lab) * 255.0, 0, 255).astype(np.uint8)
    out[mask, :3] = new_rgb
    return out


def _save_entity_pbr(
    pbr_dir: str | None,
    name: str,
    base: np.ndarray,
    emis: np.ndarray,
    height: np.ndarray,
    metal_mask: np.ndarray,
    sss_mask: np.ndarray,
) -> None:
    if pbr_dir is None:
        return
    os.makedirs(pbr_dir, exist_ok=True)
    alpha_mask = (base[..., 3] > 0).astype(np.float32)
    nx, ny, nz = compute_height_normals(np.clip(height, 0.0, 1.0), strength=2.0)
    ao = np.clip(0.50 + (height - 0.5) * 0.55 + metal_mask * 0.10, 0.25, 1.0).astype(np.float32)
    pom = np.clip(0.22 + height * 0.78, 0.15, 1.0).astype(np.float32)
    save_rgba(normals_to_labpbr(nx, ny, nz, ao=ao, height_map=pom), os.path.join(pbr_dir, f"{name}_n.png"))

    emis_alpha = emis[..., 3].astype(np.float32) / 255.0
    smoothness = np.clip(
        np.where(metal_mask > 0.5, 0.82, np.where(sss_mask > 0.5, 0.58, 0.42))
        + np.where(emis_alpha > 0.1, 0.10, 0.0),
        0.0,
        0.96,
    ).astype(np.float32) * alpha_mask
    f0 = np.where(metal_mask > 0.5, 0.92, 0.05).astype(np.float32)
    porosity_sss = np.where(sss_mask > 0.5, 0.55 + 0.20 * height, 0.10).astype(np.float32) * alpha_mask
    emissive = np.clip(emis_alpha * 0.95, 0.0, 1.0).astype(np.float32)
    save_rgba(
        channels_to_labpbr_s(
            smoothness,
            f0,
            porosity_sss,
            emissive,
            is_subsurface=name.split("_")[0] not in ("purifier",),
            metal_id=231,
        ),
        os.path.join(pbr_dir, f"{name}_s.png"),
    )


def _save_entity(
    entity_dir: str,
    name: str,
    base: np.ndarray,
    emis: np.ndarray,
    height: np.ndarray,
    metal_mask: np.ndarray,
    sss_mask: np.ndarray,
    pbr_dir: str | None,
) -> None:
    save_rgba(base, os.path.join(entity_dir, f"{name}.png"))
    save_rgba(emis, os.path.join(entity_dir, f"{name}_emissive.png"))
    _save_entity_pbr(pbr_dir, name, base, emis, height, metal_mask, sss_mask)


def _render_entity(spec: dict, skin, rank: str, seed: int, reg_id: str = "") -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    tex_w, tex_h = spec["tex_size"]
    base = np.zeros((tex_h, tex_w, 4), dtype=np.uint8)
    emis = np.zeros((tex_h, tex_w, 4), dtype=np.uint8)
    height = np.full((tex_h, tex_w), 0.5, dtype=np.float32)
    metal_mask = np.zeros((tex_h, tex_w), dtype=np.float32)
    sss_mask = np.zeros((tex_h, tex_w), dtype=np.float32)
    pal_map = spec.get("palettes", {})

    for c_idx, cube in enumerate(spec["cubes"]):
        role = cube.get("role", "skin")
        overrides = dict(skin.role_overrides)
        ramp_key = overrides.get(role, pal_map.get(role, "abyssal_obsidian"))
        ramp = adjust_ramp(PALETTES[ramp_key], skin, rank)
        if rank == "mutated":
            accent_key, glow_key = "mutant_amber", "mutant_amber"
        elif rank == "elite":
            accent_key, glow_key = "royal_gold", "royal_gold"
        else:
            accent_key, glow_key = skin.accent, skin.glow
        accent_ramp = adjust_ramp(PALETTES[accent_key], skin, rank)
        glow_ramp = adjust_ramp(PALETTES[glow_key], skin, rank)
        u, v = int(cube["u"]), int(cube["v"])
        w, h, d = int(cube["w"]), int(cube["h"]), int(cube["d"])
        tags = tuple(cube.get("detail_tags", ()))
        name = str(cube.get("name", ""))
        for rect in box_face_rects(u, v, w, h, d):
            ctx = FaceContext(
                base=base,
                emis=emis,
                height=height,
                metal=metal_mask,
                sss=sss_mask,
                rect=rect,
                cube=cube,
                skin=skin,
                ramp=ramp,
                accent_ramp=accent_ramp,
                glow_ramp=glow_ramp,
                seed=seed + c_idx * 17,
                role=role,
                rank=rank,
                tags=tags,
                emissive=bool(cube.get("emissive")),
                name=name,
                ramp_key=ramp_key,
                reg_id=reg_id,
            )
            paint_face(ctx)

    emis = apply_emissive_bloom(emis, radius=2, intensity=0.24)
    island_alpha = base[..., 3]
    halo_alpha = emis[..., 3].astype(np.float32)
    halo_alpha = np.where(halo_alpha > 48.0, halo_alpha, 0.0)
    emis[..., 3] = np.where(island_alpha > 0, halo_alpha, 0).astype(np.uint8)
    return base, emis, height, metal_mask, sss_mask


def generate_entities(entity_dir: str, pbr_dir: str | None = None, names: set[str] | None = None) -> None:
    os.makedirs(entity_dir, exist_ok=True)
    with open(UV_MANIFEST_PATH, encoding="utf-8") as f:
        manifest = json.load(f)

    for idx, (reg_id, spec) in enumerate(manifest.items()):
        if names is not None and reg_id not in names:
            continue
        if reg_id not in ENTITY_SKINS:
            raise KeyError(f"Missing EntitySkin profile for {reg_id}")
        skin = ENTITY_SKINS[reg_id]
        ranks = ("base",) if reg_id in NO_RANK_ENTITIES else RANK_ORDER
        for rank in ranks:
            seed = 200 + idx * 30 + RANK_SEED[rank]
            base, emis, height, metal_mask, sss_mask = _render_entity(spec, skin, rank, seed, reg_id)
            name = f"{reg_id}{RANK_SUFFIX[rank]}"
            _save_entity(entity_dir, name, base, emis, height, metal_mask, sss_mask, pbr_dir)

    if names is None or "acid_spit" in names:
        xx32, yy32 = make_grid(32, 32)
        d_spit = np.sqrt(xx32 * xx32 + yy32 * yy32) - 0.65
        spit_lit = compute_sdf_lighting_v2(d_spit, specular_strength=0.45, sss_strength=0.35)
        spit_rgba = np.where(
            (d_spit <= 0.0)[..., None],
            bayer_quantize(spit_lit, PALETTES["spore_chartreuse"], 0.18, matrix=8),
            np.zeros((32, 32, 4), dtype=np.uint8),
        )
        save_rgba(spit_rgba, os.path.join(entity_dir, "acid_spit.png"))


def generate_armor_layers(armor_dir: str) -> None:
    os.makedirs(armor_dir, exist_ok=True)
    for layer_idx in (1, 2):
        al = np.zeros((32, 64, 4), dtype=np.uint8)
        _fill_rect_bayer(al, 0, 0, 32, 16, PALETTES["chitin_bone"], 0.85, 750 + layer_idx)
        _fill_rect_bayer(al, 16, 16, 24, 16, PALETTES["chitin_bone"], 0.82, 760 + layer_idx)
        _fill_rect_bayer(al, 40, 16, 16, 16, PALETTES["chitin_bone"], 0.82, 770 + layer_idx)
        _fill_rect_bayer(al, 0, 16, 16, 16, PALETTES["abyssal_indigo"], 0.78, 780 + layer_idx)
        bevel, seam = cell_plate_field(64, 32, seed=790 + layer_idx, cells=9)
        a_mask = al[..., 3] > 0
        al[a_mask & (seam > 0.72)] = PALETTES["abyssal_obsidian"][0]
        al[8:10, 8:16] = PALETTES["mycelial_violet"][-2]
        save_rgba(al, os.path.join(armor_dir, f"chitin_layer_{layer_idx}.png"))


def generate_armor_variants(armor_dir: str) -> None:
    """Generate mutated and elite Oklab-shifted chitin armor layer variants."""
    from PIL import Image

    os.makedirs(armor_dir, exist_ok=True)
    for layer_idx in (1, 2):
        base_path = os.path.join(armor_dir, f"chitin_layer_{layer_idx}.png")
        if not os.path.exists(base_path):
            continue
        base_arr = np.array(Image.open(base_path).convert("RGBA"), dtype=np.uint8)
        mut = _oklab_shift_rgba(base_arr, hue_deg=26.0, chroma_scale=1.18, light_delta=0.05, tint_hex="#f59e0b", tint_amount=0.22)
        eli = _oklab_shift_rgba(base_arr, hue_deg=-18.0, chroma_scale=1.22, light_delta=-0.04, tint_hex="#9e1b32", tint_amount=0.34)
        save_rgba(mut, os.path.join(armor_dir, f"chitin_layer_{layer_idx}_mutated.png"))
        save_rgba(eli, os.path.join(armor_dir, f"chitin_layer_{layer_idx}_elite.png"))
