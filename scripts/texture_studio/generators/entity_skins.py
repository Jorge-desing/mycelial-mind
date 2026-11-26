"""Per-species entity skin profiles and face-aware procedural painting.

The entity pipeline paints every one of the six box-UV faces of every cube, instead of
filling a generic cushion and detailing only the front face.  All motifs are built from the
existing SDF / noise / palette primitives in sdf_engine.py and palette_oklab.py.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, Tuple

import numpy as np

from ..palette_oklab import PALETTES, oklab_to_rgb, rgb_to_oklab
from ..sdf_engine import (
    cell_plate_field,
    compute_height_normals,
    fast_cellular_warped,
    make_grid,
    meijering_neuriteness_toroidal,
    ridged_fbm,
    sdf_bezier,
    sdf_circle,
    sdf_diamond,
    sdf_segment,
    toroidal_value_fbm,
    toroidal_value_noise,
)

FACE_KINDS: Tuple[str, ...] = ("top", "bottom", "right", "front", "left", "back")

FACE_BASE_LIGHT: Dict[str, float] = {
    "top": 0.96,
    "bottom": 0.40,
    "front": 0.86,
    "back": 0.62,
    "left": 0.76,
    "right": 0.70,
}

FACE_LIGHT_DIR: Dict[str, Tuple[float, float, float]] = {
    "top": (0.10, -0.55, 0.83),
    "bottom": (0.05, 0.55, 0.83),
    "front": (0.00, -0.35, 0.94),
    "back": (0.00, 0.35, 0.94),
    "left": (-0.55, -0.20, 0.81),
    "right": (0.55, -0.20, 0.81),
}


@dataclass(frozen=True)
class FaceRect:
    kind: str
    x: int
    y: int
    w: int
    h: int


def box_face_rects(u: int, v: int, w: int, h: int, d: int) -> Tuple[FaceRect, ...]:
    """Return the six Minecraft box-UV face rectangles for a cube at (u, v)."""
    return (
        FaceRect("top", u + d, v, w, d),
        FaceRect("bottom", u + d + w, v, w, d),
        FaceRect("right", u, v + d, d, h),
        FaceRect("front", u + d, v + d, w, h),
        FaceRect("left", u + d + w, v + d, d, h),
        FaceRect("back", u + d + w + d, v + d, w, h),
    )


@dataclass(frozen=True)
class EntitySkin:
    motif: str
    eye: str
    accent: str
    glow: str
    rank_theme: str
    motif_variant: str = "standard"
    lift: float = 0.08
    contrast: float = 1.10
    chroma: float = 1.06
    edge: str = "soft"
    metal_roles: Tuple[str, ...] = ("metal",)
    sss_roles: Tuple[str, ...] = ("skin", "spore_sac")
    accent_roles: Tuple[str, ...] = ("armor", "bone", "glow_core", "glow_eye")
    hyphae: bool = True
    role_overrides: Tuple[Tuple[str, str], ...] = ()
    moss_strength: float = 0.55


ENTITY_SKINS: Dict[str, EntitySkin] = {
    "sporeling": EntitySkin("fungal_cap", "beady", "spore_chartreuse", "tide_violet", "fungal_amber", lift=0.09, moss_strength=0.65,
                            role_overrides=(("skin", "mycelial_violet"), ("armor", "venom_emerald"), ("bone", "warm_ivory"), ("spore_sac", "chitin_bone"))),
    "leech_parasite": EntitySkin("segmented_hide", "cluster_4", "histamine_rose", "leech_crimson", "carapace_amber", motif_variant="sucker", lift=0.08, edge="hard", moss_strength=0.10,
                                 role_overrides=(("skin", "leech_crimson"), ("armor", "abyssal_obsidian"), ("bone", "leech_crimson"))),
    "acid_spitter": EntitySkin("mossy_carapace", "compound_cluster", "acid_lime", "spore_chartreuse", "fungal_amber", motif_variant="acid", lift=0.08, edge="hard", moss_strength=0.35,
                               role_overrides=(("skin", "acid_lime"), ("armor", "basalt_slate"), ("bone", "basalt_slate"), ("spore_sac", "spore_chartreuse"))),
    "burrower": EntitySkin("segmented_hide", "lens", "ember_orange", "spore_chartreuse", "carapace_amber", motif_variant="drill", lift=0.08, edge="hard", moss_strength=0.45,
                           role_overrides=(("skin", "basalt_slate"), ("armor", "basalt_slate"), ("bone", "basalt_slate"), ("glow_core", "ember_orange"))),
    "pyro_shroom": EntitySkin("ember_crust", "ember_slit", "ember_orange", "pyro_magma", "ember_amber", motif_variant="gills", lift=0.09, edge="hard", moss_strength=0.20,
                              role_overrides=(("skin", "volcanic_ember"), ("armor", "basalt_slate"), ("bone", "chitin_bone"), ("magma", "pyro_magma"))),
    "chitin_brute": EntitySkin("bark_slabs", "predator_slit", "spore_chartreuse", "mycelial_violet", "carapace_amber", motif_variant="brute", lift=0.08, contrast=1.14, edge="hard", moss_strength=0.85,
                               role_overrides=(("skin", "venom_emerald"), ("armor", "basalt_slate"), ("bone", "chitin_bone"))),
    "mycelial_stalker": EntitySkin("neural_weave", "four_eyes", "tide_violet", "spore_chartreuse", "neural_amber", motif_variant="sinew", lift=0.07, moss_strength=0.40,
                                   role_overrides=(("skin", "warm_ivory"), ("armor", "predator_amethyst"), ("bone", "spectral_bone"))),
    "synapse_weaver": EntitySkin("neural_weave", "three_eyes", "purification_aqua", "tide_violet", "neural_amber", motif_variant="robe", lift=0.08, moss_strength=0.20,
                                 role_overrides=(("skin", "tide_violet"), ("armor", "abyssal_obsidian"), ("bone", "spectral_bone"))),
    "goliath_carrier": EntitySkin("hide_ribs", "t_visor", "chitin_bone", "tide_violet", "carapace_amber", motif_variant="brood", lift=0.07, edge="hard", moss_strength=0.05,
                                  role_overrides=(("skin", "volcanic_ember"), ("armor", "basalt_slate"), ("bone", "warm_ivory"), ("spore_sac", "chitin_bone"))),
    "colony_warden": EntitySkin("royal_filigree", "visor_slit", "royal_gold", "spore_chartreuse", "royal_crimson", motif_variant="mantle", lift=0.08, edge="hard", moss_strength=0.35,
                                role_overrides=(("skin", "warm_ivory"), ("armor", "abyssal_indigo"), ("bone", "chitin_bone"))),
    "spore_architect": EntitySkin("tech_plating", "lens", "purifier_glow", "spore_chartreuse", "carapace_amber", motif_variant="tank", lift=0.08, moss_strength=0.40,
                                  role_overrides=(("skin", "venom_emerald"), ("armor", "platinum_tech"), ("bone", "chitin_bone"), ("spore_sac", "mycelial_violet"))),
    "purifier_drone": EntitySkin("tech_plating", "lens", "royal_gold", "purifier_glow", "none", motif_variant="drone", lift=0.07, sss_roles=("spore_sac",), metal_roles=("metal", "armor"), hyphae=False, moss_strength=0.0,
                                 role_overrides=(("skin", "chitin_bone"), ("armor", "platinum_tech"), ("bone", "chitin_bone"), ("metal", "platinum_tech"))),
    "spore_behemoth": EntitySkin("mossy_carapace", "four_eyes", "spore_chartreuse", "tide_violet", "carapace_amber", motif_variant="kaiju", lift=0.08, edge="hard", moss_strength=0.75,
                                 role_overrides=(("skin", "venom_emerald"), ("armor", "basalt_slate"), ("bone", "chitin_bone"), ("spore_sac", "warm_ivory"))),
    "hive_core": EntitySkin("brass_rune", "core_gem", "royal_gold", "tide_violet", "none", motif_variant="core", lift=0.08, metal_roles=("metal", "bone", "armor"), moss_strength=0.25,
                            role_overrides=(("skin", "imperial_brass"), ("armor", "basalt_slate"), ("bone", "imperial_brass"), ("spore_sac", "tide_violet"))),
    "apex_matriarch": EntitySkin("royal_filigree", "crown_gem", "royal_gold", "spore_chartreuse", "none", motif_variant="matriarch", lift=0.08, metal_roles=("metal", "armor"), moss_strength=0.30,
                                 role_overrides=(("skin", "predator_amethyst"), ("armor", "royal_gold"), ("bone", "warm_ivory"), ("spore_sac", "tide_violet"))),
    "synaptic_mimic": EntitySkin("patchwork", "split_mask", "purification_aqua", "tide_violet", "neural_amber", motif_variant="echo", lift=0.08, moss_strength=0.30,
                                 role_overrides=(("skin", "abyssal_indigo"), ("armor", "abyssal_obsidian"), ("bone", "spectral_bone"))),
    "mycelial_chimera": EntitySkin("patchwork", "compound_cluster", "spore_chartreuse", "mycelial_violet", "carapace_amber", motif_variant="suture", lift=0.07, moss_strength=0.50,
                                   role_overrides=(("skin", "volcanic_ember"), ("armor", "basalt_slate"), ("bone", "chitin_bone"), ("spore_sac", "venom_emerald"))),
    "colony_symbiont": EntitySkin("royal_filigree", "visor_slit", "purification_aqua", "spore_chartreuse", "none", motif_variant="mantle", lift=0.08, metal_roles=("metal",), moss_strength=0.25,
                                  role_overrides=(("skin", "mycelial_violet"), ("armor", "royal_gold"), ("bone", "chitin_bone"), ("spore_sac", "purification_aqua"), ("metal", "imperial_brass"))),
    "spore_glider": EntitySkin("neural_weave", "beady", "biolum_cyan", "purification_aqua", "none", motif_variant="robe", lift=0.09, moss_strength=0.20,
                               role_overrides=(("skin", "biolum_cyan"), ("armor", "abyssal_indigo"), ("bone", "chitin_bone"), ("spore_sac", "purification_aqua"))),
    "root_keeper": EntitySkin("bark_slabs", "three_eyes", "mutant_amber", "royal_gold", "carapace_amber", motif_variant="brute", lift=0.08, contrast=1.14, edge="hard", moss_strength=0.70,
                              role_overrides=(("skin", "predator_amethyst"), ("armor", "abyssal_obsidian"), ("bone", "chitin_bone"), ("spore_sac", "mutant_amber"))),
}

NO_RANK_ENTITIES = frozenset({"purifier_drone", "hive_core", "apex_matriarch", "colony_symbiont", "spore_glider"})

COLONY_SYMBIONT_CASTE_SKINS: Dict[str, EntitySkin] = {
    "harvester": EntitySkin("mossy_carapace", "compound_cluster", "spore_chartreuse", "acid_lime", "none", motif_variant="acid", lift=0.08, moss_strength=0.65,
                            role_overrides=(("skin", "venom_emerald"), ("armor", "chitin_bone"), ("bone", "warm_ivory"), ("spore_sac", "spore_chartreuse"), ("metal", "imperial_brass"))),
    "excavator": EntitySkin("segmented_hide", "lens", "ember_orange", "mutant_amber", "none", motif_variant="drill", lift=0.08, edge="hard", metal_roles=("metal", "armor"), moss_strength=0.20,
                            role_overrides=(("skin", "volcanic_ember"), ("armor", "basalt_slate"), ("bone", "imperial_brass"), ("spore_sac", "ember_orange"), ("metal", "imperial_brass"))),
    "courier": EntitySkin("tech_plating", "visor_slit", "purification_aqua", "purifier_glow", "none", motif_variant="drone", lift=0.08, metal_roles=("metal",), hyphae=False, moss_strength=0.10,
                          role_overrides=(("skin", "warm_ivory"), ("armor", "platinum_tech"), ("bone", "spectral_bone"), ("spore_sac", "purification_aqua"), ("metal", "platinum_tech"))),
    "architect": EntitySkin("neural_weave", "three_eyes", "tide_violet", "mycelial_violet", "none", motif_variant="robe", lift=0.08, metal_roles=("metal",), moss_strength=0.30,
                            role_overrides=(("skin", "mycelial_violet"), ("armor", "predator_amethyst"), ("bone", "royal_gold"), ("spore_sac", "tide_violet"), ("metal", "royal_gold"))),
    "sentinel": EntitySkin("royal_filigree", "predator_slit", "elite_crimson", "histamine_rose", "none", motif_variant="mantle", lift=0.08, contrast=1.14, edge="hard", metal_roles=("metal", "armor"), moss_strength=0.15,
                           role_overrides=(("skin", "abyssal_obsidian"), ("armor", "elite_crimson"), ("bone", "royal_gold"), ("spore_sac", "histamine_rose"), ("metal", "royal_gold"))),
}

METAL_PALETTES = frozenset({"royal_gold", "imperial_brass", "platinum_tech"})

RANK_TINT = {
    "base": None,
    "mutated": "mutant_amber",
    "elite": "elite_crimson",
}


@dataclass
class MotifLayers:
    height: np.ndarray
    accent: np.ndarray
    seam: np.ndarray
    glow: np.ndarray
    rim: np.ndarray
    moss: np.ndarray | None = None


def _zeros(w: int, h: int) -> np.ndarray:
    return np.zeros((h, w), dtype=np.float32)


def _clip01(arr: np.ndarray) -> np.ndarray:
    return np.clip(arr, 0.0, 1.0).astype(np.float32)


def _smoothstep(edge0: float, edge1: float, x: np.ndarray) -> np.ndarray:
    t = np.clip((x - edge0) / max(1e-6, edge1 - edge0), 0.0, 1.0)
    return (t * t * (3.0 - 2.0 * t)).astype(np.float32)


def _rim_band(field: np.ndarray, center: float, width: float) -> np.ndarray:
    return np.clip(1.0 - np.abs(field - center) / max(1e-6, width), 0.0, 1.0).astype(np.float32)


def _edge_ao(w: int, h: int, strength: float = 0.20) -> np.ndarray:
    ao = _zeros(w, h)
    if w < 2 or h < 2:
        return ao
    ao[:, 0] = ao[:, -1] = strength
    ao[0, :] = ao[-1, :] = strength
    if w >= 5 and h >= 5:
        inner = strength * 0.45
        ao[1, 1:-1] = np.maximum(ao[1, 1:-1], inner)
        ao[-2, 1:-1] = np.maximum(ao[-2, 1:-1], inner)
        ao[1:-1, 1] = np.maximum(ao[1:-1, 1], inner)
        ao[1:-1, -2] = np.maximum(ao[1:-1, -2], inner)
    return ao


def _rivet_mask(w: int, h: int, seed: int, count: int = 4) -> np.ndarray:
    if w < 4 or h < 4:
        return _zeros(w, h)
    rng = np.random.default_rng(seed + 17)
    xx, yy = make_grid(w, h)
    mask = _zeros(w, h)
    for _ in range(max(1, count)):
        cx = float(rng.uniform(-0.75, 0.75))
        cy = float(rng.uniform(-0.75, 0.75))
        radius = float(rng.uniform(0.10, 0.18))
        d = sdf_circle(xx, yy, cx, cy, radius)
        mask = np.maximum(mask, _clip01(1.0 - d / max(0.02, radius * 0.9)))
    return _clip01(mask)


def _pustule_mask(w: int, h: int, seed: int, count: int = 6) -> np.ndarray:
    if w < 3 or h < 3:
        return _zeros(w, h)
    rng = np.random.default_rng(seed + 91)
    xx, yy = make_grid(w, h)
    mask = _zeros(w, h)
    for _ in range(max(1, count)):
        cx = float(rng.uniform(-0.8, 0.8))
        cy = float(rng.uniform(-0.8, 0.8))
        radius = float(rng.uniform(0.12, 0.28))
        d = sdf_circle(xx, yy, cx, cy, radius)
        dome = _clip01(1.0 - d / max(0.02, radius))
        mask = np.maximum(mask, dome)
    return _clip01(mask)


def _ring_field(w: int, h: int, count: int, face: str, phase: float = 0.0) -> Tuple[np.ndarray, np.ndarray]:
    if face in ("top", "bottom"):
        xx, yy = make_grid(w, h)
        r = np.sqrt(xx * xx + yy * yy) / 1.414
        value = 0.5 + 0.5 * np.cos((r * max(2, count) + phase) * 2.0 * np.pi)
        ridge = np.clip(1.0 - np.abs(2.0 * value - 1.0), 0.0, 1.0)
        seam = _clip01(1.0 - ridge / 0.16)
        return ridge.astype(np.float32), seam
    y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
    value = 0.5 + 0.5 * np.cos((y * max(2, count) + phase) * 2.0 * np.pi)
    ridge = np.clip(1.0 - np.abs(2.0 * value - 1.0), 0.0, 1.0) * np.ones((1, w), dtype=np.float32)
    seam = _clip01(1.0 - ridge / 0.16)
    return ridge.astype(np.float32), seam


def _plate_layers(w: int, h: int, seed: int, cells: int, strength: float = 1.0) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    cells = int(max(2, min(7, cells)))
    bevel, _ = cell_plate_field(w, h, seed=seed, cells=cells)
    flat = _smoothstep(0.14, 0.52, bevel)
    seam = np.clip(1.0 - _smoothstep(0.02, 0.18, bevel), 0.0, 1.0).astype(np.float32)
    rim = _rim_band(bevel, 0.24, 0.10) * (1.0 - seam * 0.45)
    height = 0.27 + flat * (0.38 * strength)
    return _clip01(height), _clip01(seam), _clip01(rim)

def _grid_plate_layers(w: int, h: int, seed: int, rows: int, cols: int) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Crisp brick-like plate rows with 1px dark seams and 1px bright top rims."""
    rng = np.random.default_rng(seed + 29)
    height = np.full((h, w), 0.34, dtype=np.float32)
    seam = _zeros(w, h)
    rim = _zeros(w, h)
    rows = int(max(1, min(4, rows)))
    cols = int(max(1, min(6, cols)))
    if h < 4:
        rows = 1
    if w < 4:
        cols = 1
    row_edges = [0]
    for _ in range(rows - 1):
        step = max(2, h // rows + int(rng.integers(-1, 2)))
        row_edges.append(min(h - 1, row_edges[-1] + step))
    row_edges.append(h)
    row_edges = sorted(set(row_edges))
    for ri in range(len(row_edges) - 1):
        y0, y1 = row_edges[ri], row_edges[ri + 1]
        if y1 - y0 < 1:
            continue
        col_edges = [0]
        offset = int(rng.integers(0, max(2, w // cols))) if ri % 2 else 0
        x = -offset
        while x < w - 1:
            step = max(2, w // cols + int(rng.integers(-1, 2)))
            col_edges.append(min(w, x + step))
            x += step
        col_edges.append(w)
        col_edges = sorted(set([0, w] + [max(0, min(w, c)) for c in col_edges]))
        for ci in range(len(col_edges) - 1):
            x0, x1 = col_edges[ci], col_edges[ci + 1]
            if x1 - x0 < 1:
                continue
            plate_h = float(rng.uniform(0.48, 0.62))
            height[y0:y1, x0:x1] = plate_h
            rim[y0, x0:x1] = 1.0
            seam[y1 - 1, x0:x1] = 1.0
            seam[y0:y1, x1 - 1] = 1.0
            if x1 - x0 >= 4 and y1 - y0 >= 4:
                cy = y0 + (y1 - y0) // 2
                cx = x0 + (x1 - x0) // 2
                height[cy, cx] = min(1.0, plate_h + 0.18)
                rim[cy, cx] = 1.0
    return _clip01(height), _clip01(seam), _clip01(rim)


def _merge_layers(a: MotifLayers, b: MotifLayers) -> MotifLayers:
    moss = None
    if a.moss is not None or b.moss is not None:
        shape = a.height.shape
        ma = a.moss if a.moss is not None else np.zeros(shape, dtype=np.float32)
        mb = b.moss if b.moss is not None else np.zeros(shape, dtype=np.float32)
        moss = np.maximum(ma, mb).astype(np.float32)
    return MotifLayers(
        np.maximum(a.height, b.height).astype(np.float32),
        np.maximum(a.accent, b.accent).astype(np.float32),
        np.maximum(a.seam, b.seam).astype(np.float32),
        np.maximum(a.glow, b.glow).astype(np.float32),
        np.maximum(a.rim, b.rim).astype(np.float32),
        moss,
    )


def _scale_layers(layers: MotifLayers, factor: float) -> MotifLayers:
    moss = None if layers.moss is None else (layers.moss * factor).astype(np.float32)
    return MotifLayers(
        np.maximum(layers.height, 0.30 + (layers.height - 0.30) * factor).astype(np.float32),
        (layers.accent * factor).astype(np.float32),
        (layers.seam * factor).astype(np.float32),
        (layers.glow * factor).astype(np.float32),
        (layers.rim * factor).astype(np.float32),
        moss,
    )


def _core_layers(w: int, h: int, seed: int) -> MotifLayers:
    xx, yy = make_grid(w, h)
    r = np.sqrt(xx * xx + yy * yy)
    theta = np.arctan2(yy, xx)
    core = _clip01(1.0 - sdf_diamond(xx, yy, 0.0, 0.0, 0.72) / 0.72)
    rim = _rim_band(core, 0.55, 0.11)
    facets = 0.5 + 0.5 * np.cos(theta * 6.0 + r * 5.0)
    height = 0.30 + core * 0.44 + facets * 0.06
    accent = _clip01((core - 0.42) / 0.58) * (0.55 + 0.35 * facets)
    seam = np.maximum(rim * 0.85, _clip01((1.0 - core) * 0.25))
    glow = _clip01((core - 0.28) * 1.35) * (0.55 + 0.45 * facets) * 0.80
    rim_hl = rim * 0.65
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim_hl))


def _lens_layers(w: int, h: int, seed: int) -> MotifLayers:
    xx, yy = make_grid(w, h)
    r = np.sqrt(xx * xx + yy * yy)
    rings = 0.5 + 0.5 * np.cos(r * 8.0 * np.pi)
    core = _clip01(1.0 - r / 0.55)
    height = 0.32 + core * 0.30 + rings * 0.05
    accent = _clip01((core - 0.45) / 0.55) * 0.55
    seam = _rim_band(r, 0.52, 0.10) * 0.8
    glow = _clip01((core - 0.25) * 1.4) * (0.55 + 0.45 * rings) * 0.75
    rim_hl = _rim_band(r, 0.40, 0.08)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim_hl))


def _sac_layers(w: int, h: int, seed: int, variant: str, name: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    r = np.sqrt(xx * xx + yy * yy)
    bulb = _clip01(1.0 - r / 0.86)
    rim = _rim_band(bulb, 0.48, 0.12)
    height = 0.28 + bulb * 0.42
    accent = _clip01((bulb - 0.40) / 0.60) * 0.82
    seam = rim * 0.80
    glow = _clip01((bulb - 0.30) * 1.25) * 0.60
    if "egg" in name:
        spot = _pustule_mask(w, h, seed + 3, 3)
        core = _clip01((spot - 0.55) / 0.45)
        height = np.maximum(height, 0.32 + spot * 0.36)
        accent = np.maximum(accent, core * 0.75)
        seam = np.maximum(seam, _rim_band(spot, 0.50, 0.12) * 0.55)
        glow = np.maximum(glow, core * 0.65)
    if min(w, h) >= 6:
        pattern = toroidal_value_fbm(w, h, seed=seed + 31, base_cells=max(2, min(5, w // 3)), octaves=2)
        height = np.clip(height * 0.82 + pattern * 0.16 + 0.06, 0.0, 1.0)
        accent = np.maximum(accent, _clip01((pattern - 0.62) * 1.6) * 0.45)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _hyphae_layers(w: int, h: int, seed: int, strength: float = 1.0) -> MotifLayers:
    field = ridged_fbm(w, h, seed=seed + 71, freq=max(2.0, min(6.0, w / 3.0)))
    threads = _clip01(field * 0.25 + (meijering_neuriteness_toroidal(field) * 0.75 if min(w, h) >= 6 else field))
    core = _clip01((threads - 0.42) / 0.58)
    height = 0.34 + core * 0.16 * strength
    accent = core * 0.95 * strength
    seam = _rim_band(threads, 0.30, 0.16) * 0.35
    glow = core * 0.16 * strength
    rim = core * 0.58 * strength
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _mold_layers(w: int, h: int, seed: int, strength: float = 1.0) -> MotifLayers:
    field = toroidal_value_fbm(w, h, seed=seed + 107, base_cells=max(2, min(5, w // 3)), octaves=3)
    stain = _clip01((field - 0.55) / 0.45)
    height = 0.33 + stain * 0.07 * strength
    accent = stain * 0.38 * strength
    seam = _rim_band(field, 0.55, 0.12) * 0.16
    glow = stain * 0.08 * strength
    rim = stain * 0.18 * strength
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _moss_layers(w: int, h: int, seed: int, face: str, strength: float = 1.0) -> MotifLayers:
    clump = toroidal_value_fbm(w, h, seed=seed + 127, base_cells=max(2, min(6, w // 3)), octaves=3)
    fuzz = toroidal_value_noise(w, h, max(4, w), max(4, h), seed=seed + 131)
    mask = _clip01((clump - 0.42) / 0.58) * _clip01((fuzz - 0.18) / 0.82)
    if face == "top":
        mask = mask * 1.18
    elif face == "bottom":
        mask = mask * 0.82
    mask = _clip01(mask * 0.82 + _edge_ao(w, h, 0.28) * 0.45)
    height = 0.34 + mask * 0.08
    zeros = _zeros(w, h)
    return MotifLayers(_clip01(height), zeros, zeros, zeros, zeros, _clip01(mask * strength))


def _shelf_layers(w: int, h: int, seed: int, strength: float = 1.0) -> MotifLayers:
    if w * h < 24:
        return MotifLayers(_zeros(w, h), _zeros(w, h), _zeros(w, h), _zeros(w, h), _zeros(w, h))
    rng = np.random.default_rng(seed + 113)
    xx, yy = make_grid(w, h)
    height = _zeros(w, h)
    accent = _zeros(w, h)
    seam = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    for _ in range(int(rng.integers(1, 3))):
        cx = float(rng.uniform(-0.7, 0.7))
        cy = float(rng.uniform(-0.7, 0.7))
        radius = float(rng.uniform(0.18, 0.34))
        d = sdf_circle(xx, yy, cx, cy, radius)
        core = _clip01(1.0 - d / radius)
        edge = _rim_band(core, 0.52, 0.14)
        height = np.maximum(height, 0.33 + core * 0.30 * strength)
        accent = np.maximum(accent, core * 0.62 * strength)
        seam = np.maximum(seam, edge * 0.65)
        glow = np.maximum(glow, core * 0.20 * strength)
        rim = np.maximum(rim, core * 0.70 * strength)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _spore_cluster_layers(w: int, h: int, seed: int, strength: float = 0.8) -> MotifLayers:
    spots = _pustule_mask(w, h, seed + 83, 4 if w * h >= 48 else 3)
    core = _clip01((spots - 0.58) / 0.42)
    rim = _rim_band(spots, 0.46, 0.12)
    height = 0.30 + spots * 0.26
    accent = core * 0.95 * strength
    seam = rim * 0.50
    glow = core * 0.28 * strength
    rim_hl = core * 0.75
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim_hl))


def _cap_growth_layers(w: int, h: int, seed: int, strength: float = 0.7) -> MotifLayers:
    xx, yy = make_grid(w, h)
    r = np.sqrt(xx * xx + yy * yy)
    theta = np.arctan2(yy, xx)
    cap = _clip01(1.0 - r / 0.82)
    rim = _rim_band(cap, 0.52, 0.13)
    gills = 0.5 + 0.5 * np.cos(theta * 9.0 + r * 5.0)
    spots = _pustule_mask(w, h, seed + 173, 2)
    spot_core = _clip01((spots - 0.58) / 0.42)
    height = 0.30 + cap * 0.34 + gills * 0.05 + spots * 0.18
    accent = _clip01((cap - 0.48) / 0.52) * 0.55 + spot_core * 0.60
    seam = np.maximum(rim * 0.70, _rim_band(spots, 0.48, 0.12) * 0.40)
    glow = spot_core * 0.25
    rim_hl = cap * 0.55
    layers = MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim_hl))
    if strength < 1.0:
        layers = _scale_layers(layers, strength)
    return layers


def _spike_layers(w: int, h: int, seed: int, face: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    along = np.clip((1.0 - (yy + 1.0) * 0.5) if face not in ("top", "bottom") else (1.0 - np.sqrt(xx * xx + yy * yy) / 1.414), 0.0, 1.0)
    ridge = 0.5 + 0.5 * np.cos(xx * 5.0 * np.pi + along * 3.0)
    height = 0.30 + along * 0.42 + ridge * 0.05
    accent = _clip01((along - 0.52) / 0.48) * 0.85
    seam = _rim_band(along, 0.34, 0.10) * 0.55
    glow = _clip01((along - 0.62) / 0.38) * (0.50 + 0.50 * ridge) * 0.55
    rim = _clip01((along - 0.72) / 0.28) * 0.72
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _name_has(name: str, *keys: str) -> bool:
    lower = name.lower()
    return any(k in lower for k in keys)


def _organic_skin_layers(w: int, h: int, seed: int, variant: str) -> MotifLayers:
    grain = toroidal_value_fbm(w, h, seed=seed + 7, base_cells=max(2, min(6, w // 2)), octaves=3)
    pores = _rivet_mask(w, h, seed + 19, 2 if w * h < 40 else 4)
    height = 0.38 + grain * 0.10 - pores * 0.05
    accent = _zeros(w, h)
    seam = _clip01(pores * 0.55)
    glow = _zeros(w, h)
    rim = _clip01((grain - 0.72) * 1.8) * 0.35
    if variant == "sinew":
        field = ridged_fbm(w, h, seed=seed + 41, freq=max(2.0, min(5.0, w / 4.0)))
        veins = _clip01(field * 0.25 + (meijering_neuriteness_toroidal(field) * 0.75 if min(w, h) >= 6 else field))
        height = np.maximum(height, 0.34 + veins * 0.22)
        accent = np.maximum(accent, _clip01((veins - 0.58) / 0.42) * 0.75)
        glow = np.maximum(glow, _clip01((veins - 0.72) / 0.28) * 0.55)
        seam = np.maximum(seam, _rim_band(veins, 0.45, 0.12) * 0.35)
    if variant == "fungal":
        spots = _pustule_mask(w, h, seed + 23, 3)
        core = _clip01((spots - 0.55) / 0.45)
        height = np.maximum(height, 0.34 + spots * 0.26)
        accent = np.maximum(accent, core * 0.70)
        seam = np.maximum(seam, _rim_band(spots, 0.45, 0.12) * 0.45)
        glow = np.maximum(glow, core * 0.18)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _face_skin_layers(w: int, h: int, seed: int, eye_pattern: str, motif: str = "standard") -> MotifLayers:
    grain = toroidal_value_fbm(w, h, seed=seed + 7, base_cells=max(2, min(5, w // 2)), octaves=3)
    height = 0.40 + grain * 0.08
    accent = _zeros(w, h)
    seam = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    brow_y = min(h - 1, max(0, h // 3 - 1))
    seam[brow_y, :] = 0.85
    if h >= 5:
        seam[-2, :] = np.maximum(seam[-2, :], 0.45)
    if w >= 5 and h >= 5:
        seam[0, :] = np.maximum(seam[0, :], 0.35)
        seam[:, 0] = np.maximum(seam[:, 0], 0.30)
        seam[:, -1] = np.maximum(seam[:, -1], 0.30)
    if eye_pattern in ("t_visor", "visor_slit"):
        vy = min(h - 1, max(0, h // 2 - 2))
        seam[vy, :] = 0.55
    if w >= 3 and h >= 3:
        cx = w // 2
        if motif == "fungal_cap":
            accent[1, max(0, cx - 1) : min(w, cx + 1)] = 1.0
            glow[1, max(0, cx - 1) : min(w, cx + 1)] = 0.6
        elif motif == "royal_filigree":
            accent[1 : max(2, h // 2 - 1), cx] = 1.0
            rim[1 : max(2, h // 2 - 1), cx] = 0.8
        elif motif == "neural_weave":
            glow[1, cx] = 1.0
            accent[max(1, h // 2 - 1) : h // 2 + 1, cx] = 0.8
        elif motif == "patchwork":
            for i in range(min(w, h)):
                seam[i, i] = 0.9
        elif motif == "segmented_hide":
            seam[1 : h - 1, cx] = 0.75
        elif motif == "ember_crust":
            seam[1 : max(2, h // 2), cx] = 0.85
            glow[1 : max(2, h // 2), cx] = 0.55
        elif motif == "tech_plating":
            seam[min(h - 1, h // 2), 1 : w - 1] = 0.70
        elif motif == "brass_rune":
            accent[min(h - 1, h // 2), 1 : w - 1] = 0.85
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _tag_layers(w: int, h: int, seed: int, tags: Tuple[str, ...], role: str) -> MotifLayers:
    height = _zeros(w, h)
    accent = _zeros(w, h)
    seam = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    xx, yy = make_grid(w, h)
    if "neural_veins" in tags:
        field = ridged_fbm(w, h, seed=seed + 43, freq=max(2.0, min(5.0, w / 4.0)))
        veins = _clip01(field * 0.3 + (meijering_neuriteness_toroidal(field) * 0.7 if min(w, h) >= 6 else field))
        height = np.maximum(height, 0.34 + veins * 0.20)
        accent = np.maximum(accent, _clip01((veins - 0.58) / 0.42))
        glow = np.maximum(glow, _clip01((veins - 0.70) / 0.30) * 0.30)
    if "magma_fissures" in tags or role == "magma":
        fiss = ridged_fbm(w, h, seed=seed + 47, freq=3.2)
        hot = _clip01((fiss - 0.52) / 0.48)
        height = np.maximum(height, 0.30 + fiss * 0.28)
        accent = np.maximum(accent, hot)
        seam = np.maximum(seam, hot * 0.8)
        glow = np.maximum(glow, _clip01((fiss - 0.58) / 0.42))
    if "crystal_facets" in tags:
        facets = 0.5 + 0.5 * np.cos((xx * 2.7 + yy * 1.9) * np.pi)
        height = np.maximum(height, 0.32 + facets * 0.22)
        accent = np.maximum(accent, _clip01((facets - 0.62) / 0.38) * 0.8)
        rim = np.maximum(rim, _clip01((facets - 0.78) / 0.22))
    if "runic_band" in tags and w >= 2 and h >= 1:
        ry = min(h - 1, max(0, h // 2))
        for gx in range(1, w - 1, 3):
            seam[ry, gx : min(w, gx + 2)] = 0.75
            accent[ry, gx] = 1.0
    if "bone_ridges" in tags and h >= 4:
        for yy0 in range(1, h - 1, max(2, h // 3)):
            seam[yy0, :] = np.maximum(seam[yy0, :], 0.65)
            if yy0 + 1 < h:
                rim[yy0 + 1, :] = np.maximum(rim[yy0 + 1, :], 0.55)
    if "brushed_metal" in tags:
        streak = toroidal_value_noise(w, h, max(2, w // 2), max(2, h // 8 + 2), seed=seed + 59)
        accent = np.maximum(accent, _clip01((streak - 0.60) / 0.40) * 0.65)
        seam = np.maximum(seam, _clip01((0.30 - streak) / 0.30) * 0.55)
    if "brood_egg" in tags:
        eggs = _pustule_mask(w, h, seed + 61, 3)
        core = _clip01((eggs - 0.55) / 0.45)
        height = np.maximum(height, 0.32 + eggs * 0.34)
        accent = np.maximum(accent, core * 0.75)
        glow = np.maximum(glow, core * 0.30)
        seam = np.maximum(seam, _rim_band(eggs, 0.48, 0.12) * 0.55)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_plates(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    rows = max(1, min(4, h // 3))
    cols = max(1, min(5, w // 4))
    if variant == "kaiju":
        rows = max(1, min(3, rows))
        cols = max(1, min(3, cols))
    height, seam, rim = _grid_plate_layers(w, h, seed, rows, cols)
    rivets = _rivet_mask(w, h, seed, 2 if w * h < 60 else 3)
    accent = _clip01(rim * 0.55 + rivets * 0.85)
    glow = _zeros(w, h)
    xx, yy = make_grid(w, h)
    if variant == "acid" and role in ("spore_sac", "glow_core", "skin"):
        sac = _clip01(1.0 - sdf_circle(xx, yy, 0.0, 0.0, 0.52) / 0.52)
        rim_sac = _rim_band(sac, 0.55, 0.16)
        accent = np.maximum(accent, _clip01((sac - 0.62) / 0.38))
        seam = np.maximum(seam, rim_sac * 0.55)
        glow = np.maximum(glow, _clip01((sac - 0.58) * 1.5))
        height = np.maximum(height, 0.30 + sac * 0.34)
    if variant == "brood" and ("egg" in name or role == "spore_sac"):
        egg = _clip01(1.0 - sdf_circle(xx, yy, 0.0, 0.0, 0.62) / 0.62)
        rim_egg = _rim_band(egg, 0.55, 0.16)
        height = np.maximum(height, 0.30 + egg * 0.42)
        accent = np.maximum(accent, _clip01((egg - 0.66) / 0.34))
        seam = np.maximum(seam, rim_egg * 0.65)
        glow = np.maximum(glow, _clip01((egg - 0.62) * 1.6))
    if role == "glow_core":
        core = _clip01(1.0 - sdf_diamond(xx, yy, 0.0, 0.0, 0.58) / 0.58)
        rim_core = _rim_band(core, 0.55, 0.12)
        height = np.maximum(height * 0.55, 0.30 + core * 0.46)
        accent = np.maximum(accent, _clip01((core - 0.55) / 0.45))
        seam = np.maximum(seam, rim_core * 0.75)
        glow = np.maximum(glow, _clip01((core - 0.48) * 1.6))
    if role == "bone" or variant == "kaiju":
        ridge = ridged_fbm(w, h, seed=seed + 13, freq=3.0)
        accent = np.maximum(accent, _clip01((ridge - 0.68) * 2.5) * 0.65)
        seam = np.maximum(seam, _rim_band(ridge, 0.50, 0.10) * 0.55)
        height = np.maximum(height, 0.28 + ridge * 0.30)
    if "tusk" in name or "horn" in name or "spike" in name or "claw" in name:
        accent = np.maximum(accent, _clip01(0.55 + 0.45 * np.sin(np.linspace(0, 3.0, h))[:, None]))
        rim = np.maximum(rim, 0.55)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_segmented(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    count = max(3, min(8, h // 3)) if face not in ("top", "bottom") else max(3, min(7, (w + h) // 8))
    ridge, seam = _ring_field(w, h, count, face, phase=0.15)
    grain = toroidal_value_fbm(w, h, seed=seed + 7, base_cells=max(2, min(6, w // 3)), octaves=3)
    height = 0.30 + ridge * 0.38 + grain * 0.10
    accent = _clip01(ridge * 0.85)
    glow = _zeros(w, h)
    rim = _clip01(ridge * 0.75)
    if variant == "drill":
        xx, yy = make_grid(w, h)
        spiral = 0.5 + 0.5 * np.cos((xx * 3.0 + yy * 3.0) * np.pi + seed * 0.01)
        accent = np.maximum(accent, _clip01(spiral * 0.55))
        height = np.maximum(height, 0.30 + spiral * 0.30)
    if "proboscis" in name or "drill_tip" in name or "stinger" in name or "tail_tip" in name:
        xx, yy = make_grid(w, h)
        tip = _clip01(1.0 - sdf_circle(xx, yy, 0.0, 0.0, 0.45) / 0.45)
        accent = np.maximum(accent, tip * 0.9)
        glow = np.maximum(glow, _clip01((tip - 0.55) * 1.8))
        height = np.maximum(height, 0.35 + tip * 0.45)
    if role == "glow_core":
        glow = np.maximum(glow, _clip01(ridge * 0.85))
    if "spine" in name or "crest" in name or "fin" in name:
        accent = np.maximum(accent, _clip01(ridge * 0.85))
        rim = np.maximum(rim, 0.65)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_mossy_carapace(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    cells = max(2, min(3, (w + h) // 12)) if variant == "kaiju" else max(2, min(5, (w + h) // 8))
    f1, edge = fast_cellular_warped(w, h, seed=seed + 137, cells=cells, warp=0.18 if variant != "kaiju" else 0.26)
    lumps = _clip01(f1)
    seams = _clip01(edge * 1.25)
    holes = _pustule_mask(w, h, seed + 139, 3)
    hole_core = _clip01((holes - 0.58) / 0.42)
    hole_rim = _rim_band(holes, 0.46, 0.12)
    height = 0.30 + lumps * 0.30 - seams * 0.16 + holes * 0.18
    accent = _clip01((lumps - 0.55) / 0.45) * 0.60 + hole_core * 0.80
    seam = np.maximum(seams * 0.75, hole_rim * 0.60)
    glow = hole_core * 0.45
    rim = _clip01((lumps - 0.75) / 0.25) * 0.50
    if variant == "kaiju":
        vents = _pustule_mask(w, h, seed + 141, 2)
        vent_core = _clip01((vents - 0.56) / 0.44)
        height = np.maximum(height, 0.30 + vents * 0.34)
        accent = np.maximum(accent, vent_core * 0.85)
        seam = np.maximum(seam, _rim_band(vents, 0.48, 0.12) * 0.55)
        glow = np.maximum(glow, vent_core * 0.65)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_bark_slabs(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    bark = toroidal_value_noise(w, h, max(2, w // 2), max(2, h // 5 + 2), seed=seed + 151)
    grain = toroidal_value_fbm(w, h, seed=seed + 153, base_cells=max(2, min(6, w // 3)), octaves=3)
    xx, yy = make_grid(w, h)
    seam = _zeros(w, h)
    rng = np.random.default_rng(seed + 157)
    for _ in range(max(1, min(3, w // 5))):
        cx = float(rng.uniform(-0.8, 0.8))
        d = sdf_segment(xx, yy, cx, -0.95, cx + float(rng.uniform(-0.2, 0.2)), 0.95)
        crack = _clip01(1.0 - d / 0.10)
        seam = np.maximum(seam, crack)
    height = 0.32 + bark * 0.26 + grain * 0.08 - seam * 0.18
    accent = _clip01((bark - 0.58) / 0.42) * 0.62
    glow = _zeros(w, h)
    rim = _clip01((bark - 0.76) / 0.24) * 0.55
    if variant == "kaiju":
        vent = _pustule_mask(w, h, seed + 159, 2)
        vent_core = _clip01((vent - 0.58) / 0.42)
        glow = np.maximum(glow, vent_core * 0.55)
        accent = np.maximum(accent, vent_core * 0.70)
        seam = np.maximum(seam, _rim_band(vent, 0.48, 0.12) * 0.55)
        height = np.maximum(height, 0.32 + vent * 0.24)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_hide_ribs(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    ridge, seam = _ring_field(w, h, max(2, min(4, h // 3)), face, phase=0.2)
    grain = toroidal_value_fbm(w, h, seed=seed + 163, base_cells=max(2, min(6, w // 3)), octaves=3)
    pores = _rivet_mask(w, h, seed + 167, 2 if w * h < 50 else 4)
    height = 0.30 + ridge * 0.20 + grain * 0.11 - pores * 0.04
    accent = _clip01((ridge - 0.60) / 0.40) * 0.45
    seam = np.maximum(seam * 0.60, _rim_band(ridge, 0.28, 0.10) * 0.45)
    glow = _zeros(w, h)
    rim = ridge * 0.42
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_ember(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    crust = toroidal_value_fbm(w, h, seed=seed + 21, base_cells=max(2, min(6, w // 3)), octaves=3)
    fiss = ridged_fbm(w, h, seed=seed + 33, freq=3.2)
    hot = _clip01((fiss - 0.56) / 0.44)
    height = 0.30 + crust * 0.30 - hot * 0.16
    accent = _clip01(hot * 0.75)
    seam = _clip01(hot * 0.9)
    glow = _clip01((fiss - 0.62) / 0.38)
    rim = _clip01((fiss - 0.70) / 0.30)
    if variant == "gills" and face in ("front", "back", "left", "right"):
        y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
        gills = 0.5 + 0.5 * np.cos(y * max(4, h // 2) * 2.0 * np.pi)
        accent = np.maximum(accent, gills * 0.55)
        height = np.maximum(height, 0.32 + gills * 0.22)
        glow = np.maximum(glow, _clip01((gills - 0.70) * 0.8))
    if role == "magma" or "magma" in name or "ember" in name:
        glow = np.maximum(glow, _clip01((fiss - 0.50) / 0.50))
        seam = np.maximum(seam, glow * 0.8)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_veins(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    field = ridged_fbm(w, h, seed=seed + 41, freq=max(2.0, min(5.0, w / 4.0)))
    if min(w, h) >= 6:
        veins = _clip01(field * 0.25 + meijering_neuriteness_toroidal(field) * 0.75)
    else:
        veins = field
    height = 0.32 + veins * 0.26
    accent = _clip01(veins * 0.85)
    glow = _clip01((veins - 0.58) / 0.42) * 0.38
    rim = _clip01((veins - 0.70) / 0.30)
    seam = _zeros(w, h)
    if variant == "robe" and ("robe" in name or role == "skin"):
        y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
        folds = 0.5 + 0.5 * np.cos(y * max(4, h // 2) * 2.0 * np.pi)
        height = 0.32 + folds * 0.26
        accent = np.maximum(accent, folds * 0.40)
        seam = _clip01((1.0 - folds) * 0.55)
        glow = np.maximum(glow, _clip01((folds - 0.72) * 0.8))
    if variant == "sinew" and ("antler" in name or "scythe" in name or "blade" in name):
        accent = np.maximum(accent, _clip01(veins * 1.2))
        rim = np.maximum(rim, 0.7)
    if "halo" in name or "crown" in name:
        xx, yy = make_grid(w, h)
        r = np.sqrt(xx * xx + yy * yy)
        halo = _clip01(1.0 - np.abs(r - 0.62) / 0.16)
        accent = np.maximum(accent, halo * 0.9)
        glow = np.maximum(glow, halo)
        height = np.maximum(height, 0.35 + halo * 0.35)
    if role == "glow_core":
        glow = np.maximum(glow, _clip01(veins * 1.1))
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_filigree(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    height = _zeros(w, h)
    accent = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    seam = _zeros(w, h)
    rng = np.random.default_rng(seed + 53)
    for _ in range(max(2, min(5, (w + h) // 10))):
        cx = float(rng.uniform(-0.35, 0.35))
        cy = float(rng.uniform(-0.35, 0.35))
        ax = cx - 0.55
        ay = cy + float(rng.uniform(-0.25, 0.25))
        bx = cx + 0.55
        by = cy + float(rng.uniform(-0.25, 0.25))
        d = sdf_bezier(xx, yy, (ax, ay), (cx, cy + 0.35), (bx, by), 0.09)
        stroke = _clip01(1.0 - d / 0.16)
        accent = np.maximum(accent, stroke)
        height = np.maximum(height, 0.30 + stroke * 0.35)
        rim = np.maximum(rim, stroke * 0.8)
        dash = _clip01(np.sin((xx * 4.0 + yy * 3.0 + seed) * np.pi) * 0.5 + 0.5) * stroke
        glow = np.maximum(glow, _clip01((dash - 0.65) * 1.6))
    if "gem" in name or "crown" in name or role == "glow_core":
        gem = _clip01(1.0 - sdf_diamond(xx, yy, 0.0, 0.0, 0.48) / 0.48)
        accent = np.maximum(accent, gem * 0.9)
        glow = np.maximum(glow, _clip01((gem - 0.35) * 1.7))
        height = np.maximum(height, 0.35 + gem * 0.45)
    if variant == "warden" and ("shield" in name or "pauldron" in name):
        bevel, pl_seam, pl_rim = _plate_layers(w, h, seed + 3, 3, 0.8)
        height = np.maximum(height, bevel)
        seam = np.maximum(seam, pl_seam)
        rim = np.maximum(rim, pl_rim)
    if "halo" in name:
        r = np.sqrt(xx * xx + yy * yy)
        halo = _clip01(1.0 - np.abs(r - 0.60) / 0.18)
        accent = np.maximum(accent, halo)
        glow = np.maximum(glow, halo)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_runes(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    height = _zeros(w, h)
    accent = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    seam = _zeros(w, h)
    rng = np.random.default_rng(seed + 67)
    for col in range(max(2, min(5, w // 4))):
        cy = -0.7 + col * 1.4 / max(1, min(4, w // 4) - 1)
        length = min(h / max(1.0, w) * 0.8, 0.8)
        d = sdf_segment(xx, yy, cy, -length, cy, length)
        rib = _clip01(1.0 - d / 0.14)
        accent = np.maximum(accent, rib * 0.8)
        height = np.maximum(height, 0.30 + rib * 0.32)
        rim = np.maximum(rim, rib * 0.7)
    for _ in range(max(2, min(7, (w * h) // 30))):
        cx = float(rng.uniform(-0.65, 0.65))
        cy = float(rng.uniform(-0.65, 0.65))
        rune = _clip01(1.0 - sdf_circle(xx, yy, cx, cy, 0.10) / 0.10)
        ring = _clip01(1.0 - np.abs(sdf_circle(xx, yy, cx, cy, 0.20)) / 0.06)
        accent = np.maximum(accent, rune * 0.7)
        glow = np.maximum(glow, ring * 0.8)
        height = np.maximum(height, 0.30 + rune * 0.24)
        seam = np.maximum(seam, (1.0 - rune) * 0.25)
    if role == "glow_core" or "crystal" in name or "gem" in name:
        core = _clip01(1.0 - sdf_diamond(xx, yy, 0.0, 0.0, 0.58) / 0.58)
        accent = np.maximum(accent, core * 0.9)
        glow = np.maximum(glow, _clip01((core - 0.28) * 1.8))
        height = np.maximum(height, 0.35 + core * 0.50)
        seam = np.maximum(seam, (1.0 - core) * 0.35)
    if "rib" in name or "trim" in name:
        bevel, pl_seam, pl_rim = _plate_layers(w, h, seed + 5, 3, 0.7)
        height = np.maximum(height, bevel)
        seam = np.maximum(seam, pl_seam)
        rim = np.maximum(rim, pl_rim)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_tech(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    streak = toroidal_value_noise(w, h, max(2, w // 2), max(2, h // 8 + 2), seed=seed + 71)
    height = 0.32 + streak * 0.22
    accent = _clip01((streak - 0.52) * 1.8)
    seam = _clip01((0.35 - streak) * 2.0)
    glow = _zeros(w, h)
    rim = _clip01((streak - 0.72) * 2.0)
    xx, yy = make_grid(w, h)
    if "vent" in name or "grill" in name or variant == "tank":
        for x in np.linspace(-0.55, 0.55, max(2, min(4, w // 3))):
            slat = _clip01(1.0 - np.abs(sdf_segment(xx, yy, x, -0.55, x, 0.55)) / 0.10)
            seam = np.maximum(seam, slat * 0.8)
            height = np.maximum(height, 0.30 + slat * 0.25)
    if "blade" in name or variant == "drone":
        for side in (-1.0, 1.0):
            d = sdf_segment(xx, yy, -0.7 * side, -0.5, 0.7 * side, 0.5)
            blade = _clip01(1.0 - d / 0.16)
            accent = np.maximum(accent, blade * 0.8)
            height = np.maximum(height, 0.32 + blade * 0.30)
    rivets = _rivet_mask(w, h, seed + 9, 4)
    accent = np.maximum(accent, rivets * 0.7)
    glow = np.maximum(glow, rivets * 0.18)
    if "core" in name or role == "glow_core":
        glow = np.maximum(glow, _clip01(1.0 - np.sqrt(xx * xx + yy * yy) / 0.55))
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_patch(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    height = toroidal_value_fbm(w, h, seed=seed + 81, base_cells=max(2, min(5, w // 3)), octaves=3) * 0.22 + 0.30
    accent = _zeros(w, h)
    seam = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    rng = np.random.default_rng(seed + 83)
    for _ in range(max(2, min(5, (w * h) // 40))):
        x0 = float(rng.uniform(-0.8, 0.4))
        y0 = float(rng.uniform(-0.8, 0.4))
        ww = float(rng.uniform(0.25, 0.65))
        hh = float(rng.uniform(0.25, 0.65))
        panel = _clip01(np.minimum((xx - x0) / 0.05, (x0 + ww - xx) / 0.05))
        panel = np.minimum(panel, _clip01(np.minimum((yy - y0) / 0.05, (y0 + hh - yy) / 0.05)))
        panel = _clip01(panel)
        accent = np.maximum(accent, panel * 0.72)
        height = np.maximum(height, 0.30 + panel * 0.25)
        seam = np.maximum(seam, _clip01(1.0 - panel / 0.18))
    stitches = _rivet_mask(w, h, seed + 11, 7)
    accent = np.maximum(accent, stitches * 0.55)
    if variant == "echo" and "mask" in name:
        split = _clip01(np.abs(xx + yy * 0.25) / 0.10)
        seam = np.maximum(seam, split)
        glow = np.maximum(glow, _clip01(1.0 - split / 0.25) * 0.28)
    if "saddle" in name or "strap" in name:
        strap = _clip01(1.0 - np.abs(np.abs(xx) - 0.45) / 0.10)
        accent = np.maximum(accent, strap * 0.9)
        height = np.maximum(height, 0.32 + strap * 0.28)
    if "pylon" in name or "echo" in name:
        glow = np.maximum(glow, _clip01(1.0 - np.sqrt(xx * xx + yy * yy) / 0.35) * 0.45)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


def _motif_fungal_cap(w: int, h: int, seed: int, face: str, role: str, variant: str, name: str) -> MotifLayers:
    xx, yy = make_grid(w, h)
    height = _zeros(w, h)
    accent = _zeros(w, h)
    seam = _zeros(w, h)
    glow = _zeros(w, h)
    rim = _zeros(w, h)
    if face in ("top", "bottom"):
        r = np.sqrt(xx * xx + yy * yy)
        theta = np.arctan2(yy, xx)
        gills = 0.5 + 0.5 * np.cos(theta * max(8, min(16, w)))
        gills = gills * _clip01(r * 1.8)
        height = 0.30 + gills * 0.26
        accent = _clip01((gills - 0.58) / 0.42) * 0.75
        seam = _rim_band(gills, 0.36, 0.12) * 0.70
    else:
        y = np.linspace(0.0, 1.0, h, dtype=np.float32)[:, None]
        gill_count = max(2, min(6, h // 2 + 2))
        gills = 0.5 + 0.5 * np.cos(y * gill_count * 2.0 * np.pi)
        height = 0.30 + gills * 0.26
        accent = _clip01((gills - 0.58) / 0.42) * 0.75
        seam = _rim_band(gills, 0.36, 0.12) * 0.70
    spots = _pustule_mask(w, h, seed + 19, 5)
    spot_core = _clip01((spots - 0.62) / 0.38)
    spot_edge = _rim_band(spots, 0.48, 0.12)
    accent = np.maximum(accent, spot_core * 0.85)
    seam = np.maximum(seam, spot_edge * 0.60)
    glow = np.maximum(glow, spot_core * 0.18)
    height = np.maximum(height, 0.30 + spots * 0.32)
    rim = np.maximum(rim, spot_core * 0.55)
    if "cap_rim" in name or "crown" in name:
        r = np.sqrt(xx * xx + yy * yy)
        edge = _rim_band(r, 0.78, 0.10)
        accent = np.maximum(accent, edge * 0.85)
    return MotifLayers(_clip01(height), _clip01(accent), _clip01(seam), _clip01(glow), _clip01(rim))


MOTIFS = {
    "fungal_cap": _motif_fungal_cap,
    "segmented_hide": _motif_segmented,
    "chitin_plates": _motif_plates,
    "mossy_carapace": _motif_mossy_carapace,
    "bark_slabs": _motif_bark_slabs,
    "hide_ribs": _motif_hide_ribs,
    "ember_crust": _motif_ember,
    "neural_weave": _motif_veins,
    "royal_filigree": _motif_filigree,
    "brass_rune": _motif_runes,
    "tech_plating": _motif_tech,
    "patchwork": _motif_patch,
}

EYE_STYLES = (
    "predator_slit",
    "compound_cluster",
    "beady",
    "visor_slit",
    "lens",
    "core_gem",
    "split_mask",
    "ember_slit",
    "cluster_4",
    "four_eyes",
    "three_eyes",
    "t_visor",
    "crown_gem",
)

RANK_THEMES = ("fungal_amber", "carapace_amber", "neural_amber", "ember_amber", "royal_crimson", "none")


@dataclass
class FaceContext:
    base: np.ndarray
    emis: np.ndarray
    height: np.ndarray
    metal: np.ndarray
    sss: np.ndarray
    rect: FaceRect
    cube: dict
    skin: EntitySkin
    ramp: np.ndarray
    accent_ramp: np.ndarray
    glow_ramp: np.ndarray
    seed: int
    role: str
    rank: str
    tags: Tuple[str, ...]
    emissive: bool
    name: str
    ramp_key: str = "abyssal_obsidian"
    reg_id: str = ""


def adjust_ramp(ramp: np.ndarray, skin: EntitySkin, rank: str) -> np.ndarray:
    rgb = ramp[:, :3].astype(np.float32) / 255.0
    lab = rgb_to_oklab(rgb)
    L, a, b = lab[..., 0], lab[..., 1], lab[..., 2]
    L = np.clip(0.5 + (L - 0.5) * skin.contrast + skin.lift * 0.62 - 0.02, 0.02, 0.98)
    a = a * skin.chroma
    b = b * skin.chroma
    tint_key = RANK_TINT.get(rank)
    if tint_key is not None:
        tint = rgb_to_oklab(PALETTES[tint_key][-2, :3].astype(np.float32) / 255.0)
        amount = 0.30 if rank == "mutated" else 0.38
        a = a * (1.0 - amount) + tint[1] * amount
        b = b * (1.0 - amount) + tint[2] * amount
        L = np.clip(L + (0.03 if rank == "mutated" else -0.02), 0.02, 0.98)
    new_rgb = np.clip(oklab_to_rgb(np.stack([L, a, b], axis=-1)) * 255.0, 0, 255).astype(np.uint8)
    out = ramp.copy()
    out[:, :3] = new_rgb
    return out


def _apply_rank_theme(ctx: FaceContext, layers: MotifLayers) -> MotifLayers:
    if ctx.rank == "base" or ctx.skin.rank_theme == "none":
        return layers
    h, w = layers.height.shape
    theme = ctx.skin.rank_theme
    theme_seed = ctx.seed + (173 if ctx.rank == "mutated" else 317)
    if ctx.rank == "mutated":
        small = w * h < 40
        count = 3 if small else (6 if theme == "neural_amber" else 8)
        pust = _pustule_mask(w, h, theme_seed, count)
        pust_strength = 0.45 if small else 0.65
        pust_core = _clip01((pust - 0.66) / 0.34)
        pust_rim = _rim_band(pust, 0.52, 0.12)
        layers.height = np.maximum(layers.height, 0.30 + pust * (0.18 if small else 0.28))
        layers.accent = np.maximum(layers.accent, pust_core * (0.90 if small else 1.00))
        layers.seam = np.maximum(layers.seam, pust_rim * (0.55 if small else 0.75))
        layers.glow = np.maximum(layers.glow, pust_core * (0.18 if small else 0.30))
        if theme == "neural_amber":
            layers.glow = np.maximum(layers.glow, _clip01((layers.accent - 0.65) * 1.5) * 0.6)
    else:
        if ctx.role in ("armor", "bone", "spore_sac"):
            filigree = _motif_filigree(w, h, theme_seed + 5, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
            layers = _merge_layers(layers, _scale_layers(filigree, 0.75))
            layers.rim = np.maximum(layers.rim, filigree.rim * 0.85)
        else:
            layers = _merge_layers(layers, _scale_layers(_spore_cluster_layers(w, h, theme_seed + 7, 0.9), 0.85))
        if ctx.role in ("skin", "armor", "bone", "spore_sac") and w * h >= 9:
            layers = _merge_layers(layers, _moss_layers(w, h, theme_seed + 9, ctx.rect.kind, 0.55))
        if theme == "royal_crimson":
            xx, yy = make_grid(w, h)
            gem = _clip01(1.0 - sdf_diamond(xx, yy, 0.0, 0.0, 0.38) / 0.38)
            layers.glow = np.maximum(layers.glow, _clip01((gem - 0.40) * 1.6) * 0.55)
        else:
            layers.glow = np.maximum(layers.glow, _clip01((layers.accent - 0.72) * 1.6) * 0.25)
    return layers


def paint_face(ctx: FaceContext) -> None:
    w, h = ctx.rect.w, ctx.rect.h
    if w <= 0 or h <= 0:
        return
    x0, y0 = ctx.rect.x, ctx.rect.y
    if w == 1 and h == 1:
        col = ctx.ramp[3 if ctx.rect.kind == "top" else 2].copy()
        ctx.base[y0, x0] = col
        ctx.height[y0, x0] = 0.5
        ctx.metal[y0, x0] = 1.0 if ctx.role in ctx.skin.metal_roles else 0.0
        ctx.sss[y0, x0] = 1.0 if ctx.role in ctx.skin.sss_roles else 0.0
        return
    pattern = ctx.cube.get("eye_pattern")
    if ctx.role == "glow_core":
        if _name_has(ctx.name, "cap_gills", "gill"):
            layers = _motif_fungal_cap(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
        elif _name_has(ctx.name, "antenna", "horn", "spike", "claw", "stinger", "scythe", "blade", "mandible", "tail_tip", "quill", "tendril", "shard"):
            layers = _spike_layers(w, h, ctx.seed, ctx.rect.kind)
        elif _name_has(ctx.name, "vent", "ember", "magma"):
            layers = _motif_ember(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
        elif _name_has(ctx.name, "sac", "lump", "egg", "bulb"):
            layers = _sac_layers(w, h, ctx.seed, ctx.skin.motif_variant, ctx.name)
        else:
            layers = _core_layers(w, h, ctx.seed)
    elif ctx.role == "glow_eye":
        if pattern and ctx.rect.kind == "front":
            layers = _face_skin_layers(w, h, ctx.seed, pattern, ctx.skin.motif)
        elif _name_has(ctx.name, "claw", "wing", "petal", "crown", "shard", "mandible"):
            layers = _spike_layers(w, h, ctx.seed, ctx.rect.kind)
        else:
            layers = _lens_layers(w, h, ctx.seed)
    elif ctx.role == "spore_sac":
        if _name_has(ctx.name, "cap"):
            layers = _motif_fungal_cap(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
        else:
            layers = _sac_layers(w, h, ctx.seed, ctx.skin.motif_variant, ctx.name)
    elif ctx.role == "skin":
        if pattern and ctx.rect.kind == "front":
            layers = _face_skin_layers(w, h, ctx.seed, pattern, ctx.skin.motif)
        else:
            organic = "sinew" if ctx.skin.motif == "neural_weave" else ("fungal" if ctx.skin.motif == "fungal_cap" else "standard")
            species_layers = MOTIFS[ctx.skin.motif](w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
            organic_layers = _scale_layers(_organic_skin_layers(w, h, ctx.seed, organic), 0.55)
            layers = _merge_layers(organic_layers, species_layers)
    elif ctx.role == "magma":
        layers = _motif_ember(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
    elif ctx.role == "metal":
        layers = _motif_tech(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
    elif ctx.role in ("bone", "armor") and _name_has(ctx.name, "shell", "hunch", "pauldron", "plate", "crest", "shield", "bastion", "halo"):
        layers = _motif_fungal_cap(w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
    else:
        layers = MOTIFS[ctx.skin.motif](w, h, ctx.seed, ctx.rect.kind, ctx.role, ctx.skin.motif_variant, ctx.name)
    layers = _merge_layers(layers, _tag_layers(w, h, ctx.seed, ctx.tags, ctx.role))
    area = w * h
    if ctx.skin.hyphae and ctx.role in ("skin", "armor", "bone", "spore_sac", "glow_core") and area >= 16:
        layers = _merge_layers(layers, _hyphae_layers(w, h, ctx.seed, 0.85 if ctx.role == "skin" else 1.15))
        if ctx.role in ("skin", "armor", "bone", "spore_sac") and area >= 36:
            layers = _merge_layers(layers, _mold_layers(w, h, ctx.seed, 0.9))
            layers = _merge_layers(layers, _shelf_layers(w, h, ctx.seed, 0.9))
        if ctx.role in ("skin", "armor", "bone", "spore_sac") and area >= 20:
            layers = _merge_layers(layers, _moss_layers(w, h, ctx.seed, ctx.rect.kind, 0.95))
        if ctx.rect.kind == "top" and ctx.role in ("skin", "armor", "bone", "spore_sac") and area >= 24:
            layers = _merge_layers(layers, _cap_growth_layers(w, h, ctx.seed + 3, 0.75))
        if ctx.role in ("skin", "armor", "spore_sac") and area >= 36:
            layers = _merge_layers(layers, _spore_cluster_layers(w, h, ctx.seed, 0.85))
    layers = _apply_rank_theme(ctx, layers)
    grain = toroidal_value_fbm(w, h, seed=ctx.seed + 5, base_cells=max(2, min(7, w // 2)), octaves=3)
    moss_height = layers.moss if layers.moss is not None else 0.0
    height = _clip01(0.40 + layers.height * 0.42 + grain * 0.06 - layers.seam * 0.16 + moss_height * 0.06 * ctx.skin.moss_strength)
    nx, ny, nz = compute_height_normals(height, strength=1.75)
    lx, ly, lz = FACE_LIGHT_DIR[ctx.rect.kind]
    lambert = np.clip(nx * lx + ny * ly + nz * lz, 0.0, 1.0)
    cavity = np.clip(layers.seam * 0.85 + _edge_ao(w, h) * 0.55, 0.0, 1.0)
    lit = np.clip(
        FACE_BASE_LIGHT[ctx.rect.kind] * 0.26
        + lambert * 0.44
        + 0.16
        - cavity * 0.20,
        0.0,
        1.0,
    ).astype(np.float32)
    lit = np.clip((lit - 0.5) * 1.08 + 0.5, 0.0, 1.0).astype(np.float32)
    # Posterized 4-5 tone pixel art: keeps every sculpted layer readable at 5-16 px faces
    # instead of dithering the whole surface into camouflage noise.
    band_count = 5 if w * h >= 100 else 4
    band_index = np.clip((lit * band_count).astype(np.int32), 0, band_count - 1)
    levels = np.rint(np.linspace(1.0, 7.0, band_count)).astype(np.int32)
    rgba = ctx.ramp[levels[band_index]]
    seam_mask = layers.seam > 0.58
    rim_mask = (layers.rim > 0.62) & ~seam_mask
    accent_mask = (layers.accent > 0.62) & ~rim_mask & ~seam_mask
    if np.any(accent_mask):
        f = rgba.astype(np.float32)
        f[accent_mask, :3] = f[accent_mask, :3] * 0.55 + ctx.accent_ramp[-2, :3].astype(np.float32) * 0.45
        rgba = np.clip(f, 0, 255).astype(np.uint8)
    if np.any(rim_mask):
        rgba[rim_mask, :3] = np.clip(
            rgba[rim_mask, :3].astype(np.float32) * 0.45 + ctx.accent_ramp[-2, :3].astype(np.float32) * 0.55, 0, 255
        ).astype(np.uint8)
    if np.any(seam_mask):
        rgba[seam_mask, :3] = np.clip(
            rgba[seam_mask, :3].astype(np.float32) * 0.25 + ctx.ramp[0, :3].astype(np.float32) * 0.75, 0, 255
        ).astype(np.uint8)
    if layers.moss is not None and ctx.skin.moss_strength > 0.01:
        moss = _clip01(layers.moss)
        moss_mask = moss > 0.50
        if np.any(moss_mask):
            green = PALETTES["venom_emerald"][-2, :3].astype(np.float32)
            bright = PALETTES["spore_chartreuse"][-2, :3].astype(np.float32)
            blend = 0.66 * ctx.skin.moss_strength
            mf = rgba.astype(np.float32)
            mf[moss_mask, :3] = mf[moss_mask, :3] * (1.0 - blend) + green * blend
            highlight = moss > 0.78
            if np.any(highlight):
                hi_blend = 0.62 * ctx.skin.moss_strength
                mf[highlight, :3] = mf[highlight, :3] * (1.0 - hi_blend) + bright * hi_blend
            rgba = np.clip(mf, 0, 255).astype(np.uint8)
    rgba = _apply_signature(ctx, rgba)
    _apply_edge(rgba, w, h, ctx.skin.edge, ctx.rect.kind)
    _add_wear(rgba, w, h, ctx.seed)
    ctx.base[y0 : y0 + h, x0 : x0 + w] = rgba
    glow = _clip01(layers.glow)
    glow = np.where(glow > 0.42, (glow - 0.42) / 0.58, 0.0).astype(np.float32)
    if glow.shape == (h, w) and float(glow.max()) > 0.03:
        em = np.zeros((h, w, 4), dtype=np.uint8)
        em[..., :3] = ctx.glow_ramp[-1, :3]
        em[..., 3] = (glow * 255.0).astype(np.uint8)
        ctx.emis[y0 : y0 + h, x0 : x0 + w] = em
    ctx.height[y0 : y0 + h, x0 : x0 + w] = height
    metal = 1.0 if (ctx.role in ctx.skin.metal_roles or ctx.ramp_key in METAL_PALETTES) else 0.0
    ctx.metal[y0 : y0 + h, x0 : x0 + w] = metal
    ctx.sss[y0 : y0 + h, x0 : x0 + w] = 1.0 if ctx.role in ctx.skin.sss_roles else 0.0
    pattern = ctx.cube.get("eye_pattern")
    if pattern and ctx.rect.kind == "front":
        paint_eyes(ctx, layers, pattern)


def paint_eyes(ctx: FaceContext, layers: MotifLayers, pattern: str) -> None:
    w, h = ctx.rect.w, ctx.rect.h
    if w < 3 or h < 3:
        return
    x0, y0 = ctx.rect.x, ctx.rect.y
    face = ctx.base[y0 : y0 + h, x0 : x0 + w]
    emis = ctx.emis[y0 : y0 + h, x0 : x0 + w]
    eye_ramp = ctx.accent_ramp
    pupil_col = ctx.ramp[0, :3]
    glow_col = ctx.glow_ramp[-1, :3]
    style = ctx.skin.eye
    if style in ("visor_slit", "t_visor"):
        bar_y = max(1, h // 2 - 1)
        bar_h = 2 if h >= 6 else 1
        bar_w = max(2, int(w * 0.62))
        bx = max(0, (w - bar_w) // 2)
        face[bar_y : bar_y + bar_h, bx : bx + bar_w, :3] = eye_ramp[-1, :3]
        emis[bar_y : bar_y + bar_h, bx : bx + bar_w, :3] = glow_col
        emis[bar_y : bar_y + bar_h, bx : bx + bar_w, 3] = 220
        for notch in range(bx + 2, bx + bar_w, 3):
            face[bar_y : bar_y + bar_h, notch, :3] = pupil_col
            emis[bar_y : bar_y + bar_h, notch, 3] = 255
        return
    if style == "lens":
        cx = (w - 1) / 2.0
        cy = (h - 1) / 2.0
        r = max(1.0, min(w, h) * 0.32)
        xx, yy = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
        ring = np.abs(d - r) < 1.0
        socket = (d > r) & (d <= r + 1.05)
        core = d <= max(1.0, r * 0.45)
        face[socket, :3] = pupil_col
        face[ring, :3] = eye_ramp[-2, :3]
        face[core, :3] = eye_ramp[-1, :3]
        emis[core, :3] = glow_col
        emis[core, 3] = 235
        emis[ring, :3] = eye_ramp[-1, :3]
        emis[ring, 3] = 160
        sx = int(round(cx - r * 0.35))
        sy = int(round(cy - r * 0.35))
        if 0 <= sx < w and 0 <= sy < h:
            face[sy, sx, :3] = np.array([255, 255, 255], dtype=np.uint8)
            emis[sy, sx, 3] = 255
        return
    if style in ("core_gem", "crown_gem"):
        cx = (w - 1) / 2.0
        cy = (h - 1) / 2.0
        xx, yy = np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))
        d = np.abs(xx - cx) + np.abs(yy - cy)
        r = max(1.0, min(w, h) * 0.35)
        gem = d <= r
        socket = (d > r) & (d <= r + 1.05)
        face[socket, :3] = pupil_col
        face[gem, :3] = eye_ramp[-2, :3]
        emis[gem, :3] = glow_col
        emis[gem, 3] = 235
        face[gem, :3] = np.clip(face[gem, :3].astype(np.float32) * 0.55 + glow_col.astype(np.float32) * 0.45, 0, 255).astype(np.uint8)
        return
    eyes = _eye_positions(w, h, pattern)
    for (ex, ey, ew, eh) in eyes:
        if ew < 1 or eh < 1:
            continue
        sx = max(0, ex - 1)
        sy = max(0, ey - 1)
        sw = min(w - sx, ew + 2)
        sh = min(h - sy, eh + 2)
        face[sy : sy + sh, sx : sx + sw, :3] = (face[sy : sy + sh, sx : sx + sw, :3].astype(np.float32) * 0.45).astype(np.uint8)
        face[ey : ey + eh, ex : ex + ew, :3] = eye_ramp[-2, :3]
        emis[ey : ey + eh, ex : ex + ew, :3] = glow_col
        emis[ey : ey + eh, ex : ex + ew, 3] = 220
        if style in ("predator_slit", "ember_slit"):
            pupil_w = max(1, ew // 2)
            px = ex + (ew - pupil_w) // 2
            face[ey : ey + eh, px : px + pupil_w, :3] = pupil_col
            emis[ey : ey + eh, px : px + pupil_w, :3] = glow_col
            emis[ey : ey + eh, px : px + pupil_w, 3] = 255
        elif style == "beady":
            face[ey : ey + eh, ex : ex + ew, :3] = eye_ramp[-1, :3]
        elif style in ("compound_cluster", "cluster_4"):
            for oy in range(ey, ey + eh, 2):
                for ox in range(ex, ex + ew, 2):
                    face[oy, ox, :3] = eye_ramp[-1, :3]
                    emis[oy, ox, 3] = 200
        elif style == "split_mask":
            for ox in range(ex, ex + ew):
                if (ox + ey) % 2 == 0:
                    face[ey : ey + eh, ox, :3] = eye_ramp[-2, :3]
        if ew >= 2 and eh >= 2:
            face[ey, ex, :3] = np.clip(eye_ramp[-1, :3].astype(np.float32) * 0.65 + 255.0 * 0.35, 0, 255).astype(np.uint8)
            emis[ey, ex, 3] = 255
    if ctx.rank == "mutated" and w >= 6 and h >= 6:
        ex = max(0, w // 2 - 1)
        ey = max(1, h // 5)
        face[ey : ey + 2, ex : ex + 2, :3] = eye_ramp[-1, :3]
        emis[ey : ey + 2, ex : ex + 2, :3] = glow_col
        emis[ey : ey + 2, ex : ex + 2, 3] = 255


def _eye_positions(w: int, h: int, pattern: str) -> list:
    out = []
    if pattern == "two_eyes":
        ew = max(2, w // 4)
        eh = max(2, h // 4)
        ey = max(1, h // 3)
        out.append((max(0, w // 6), ey, ew, eh))
        out.append((max(0, w - w // 6 - ew), ey, ew, eh))
    elif pattern == "three_eyes":
        ew = max(2, w // 5)
        eh = max(2, h // 6)
        ey = max(1, h // 2 - 1)
        out.append((max(0, w // 6), ey, ew, eh))
        out.append((max(0, w - w // 6 - ew), ey, ew, eh))
        out.append((max(0, w // 2 - ew // 2), max(0, ey - max(2, h // 3)), ew, eh))
    elif pattern == "four_eyes":
        ew = max(2, w // 5)
        eh = max(2, h // 6)
        out.append((max(0, w // 6), max(1, h // 4), ew, eh))
        out.append((max(0, w - w // 6 - ew), max(1, h // 4), ew, eh))
        out.append((max(0, w // 5), max(1, h // 2 + 1), ew, eh))
        out.append((max(0, w - w // 5 - ew), max(1, h // 2 + 1), ew, eh))
    elif pattern == "t_visor":
        out.append((max(0, w // 5), max(1, h // 2 - 1), max(3, w * 3 // 5), 2 if h >= 6 else 1))
        out.append((max(0, w // 2), max(1, h // 2 + 1), max(1, w // 8), max(1, h // 4)))
    elif pattern == "lens":
        out.append((max(0, w // 2 - 1), max(0, h // 2 - 1), min(2, w), min(2, h)))
    elif pattern == "core_heart":
        out.append((max(1, w // 3), max(1, h // 3), max(2, w // 3), max(2, h // 3)))
    return out


def _apply_edge(rgba: np.ndarray, w: int, h: int, edge: str, face: str) -> None:
    """Directional 1px bevel: lit top/left rim, dark bottom/right rim and inner AO."""
    if w < 3 or h < 3:
        return
    hard = 1.0 if edge == "hard" else 0.72
    f = rgba[:, :, :3].astype(np.float32)
    f[0, :, :] = np.clip(f[0, :, :] * (1.0 + 0.18 * hard) + 9.0 * hard, 0, 255)
    f[:, 0, :] = np.clip(f[:, 0, :] * (1.0 + 0.12 * hard) + 5.0 * hard, 0, 255)
    f[-1, :, :] = f[-1, :, :] * (1.0 - 0.30 * hard)
    f[:, -1, :] = f[:, -1, :] * (1.0 - 0.26 * hard)
    if w >= 6 and h >= 6:
        f[-2, :, :] *= 0.90
        f[:, -2, :] *= 0.92
    if face == "top":
        f[0, :, :] = np.clip(f[0, :, :] * 1.06 + 4.0, 0, 255)
    rgba[:, :, :3] = np.clip(f, 0, 255).astype(np.uint8)


def _add_wear(rgba: np.ndarray, w: int, h: int, seed: int) -> None:
    if min(w, h) < 8:
        return
    rng = np.random.default_rng(seed + 997)
    amount = max(1, (w * h) // 56)
    for _ in range(amount):
        x = int(rng.integers(0, w))
        y = int(rng.integers(0, h))
        length = int(rng.integers(1, 3))
        shade = float(rng.uniform(0.82, 0.94))
        for k in range(length):
            xx = x + k
            if xx >= w:
                break
            rgba[y, xx, :3] = np.clip(rgba[y, xx, :3].astype(np.float32) * shade, 0, 255).astype(np.uint8)



# ---------------------------------------------------------------------------
# Species signature pass (v2): bold 1px marks that survive 5-16 px cube faces.
# Applied after posterization and before the bevel/eyes so every species keeps a
# readable identity in-game without adding camouflage noise.
# ---------------------------------------------------------------------------
def _sig_dot(rgba: np.ndarray, x: int, y: int, color: np.ndarray, blend: float = 0.75) -> None:
    h, w = rgba.shape[:2]
    if 0 <= x < w and 0 <= y < h:
        rgba[y, x, :3] = np.clip(
            rgba[y, x, :3].astype(np.float32) * (1.0 - blend) + color.astype(np.float32) * blend, 0, 255
        ).astype(np.uint8)


def _sig_hline(rgba: np.ndarray, y: int, x0: int, x1: int, color: np.ndarray, blend: float = 0.65) -> None:
    h, w = rgba.shape[:2]
    if not 0 <= y < h:
        return
    x0, x1 = sorted((max(0, min(w - 1, x0)), max(0, min(w - 1, x1))))
    rgba[y, x0 : x1 + 1, :3] = np.clip(
        rgba[y, x0 : x1 + 1, :3].astype(np.float32) * (1.0 - blend) + color.astype(np.float32) * blend, 0, 255
    ).astype(np.uint8)


def _sig_vline(rgba: np.ndarray, x: int, y0: int, y1: int, color: np.ndarray, blend: float = 0.65) -> None:
    h, w = rgba.shape[:2]
    if not 0 <= x < w:
        return
    y0, y1 = sorted((max(0, min(h - 1, y0)), max(0, min(h - 1, y1))))
    rgba[y0 : y1 + 1, x, :3] = np.clip(
        rgba[y0 : y1 + 1, x, :3].astype(np.float32) * (1.0 - blend) + color.astype(np.float32) * blend, 0, 255
    ).astype(np.uint8)


def _sig_sporeling(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_hline(rgba, max(1, h // 2), 0, w - 1, dark, 0.55)
    _sig_hline(rgba, max(1, h // 2) + 1, 0, w - 1, light, 0.30)
    for x in range(1, w, max(2, w // 3)):
        _sig_dot(rgba, x, max(1, h // 4), accent, 0.80)


def _sig_leech(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.34, 0.68):
        y = max(1, int(h * frac))
        _sig_hline(rgba, y, 0, w - 1, dark, 0.70)
        _sig_hline(rgba, y - 1, 0, w - 1, light, 0.30)


def _sig_acid(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for x, y in ((w // 3, h // 3), (2 * w // 3, h // 2), (w // 2, 2 * h // 3)):
        _sig_dot(rgba, x, y, dark, 0.75)
        _sig_dot(rgba, x + 1, y, accent, 0.70)


def _sig_burrower(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.25, 0.5, 0.75):
        x = max(1, int(w * frac))
        _sig_vline(rgba, x, 0, h - 1, dark, 0.60)
        _sig_vline(rgba, x + 1, 0, h - 1, light, 0.25)


def _sig_pyro(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for x in range(1, w - 1, max(2, w // 3)):
        _sig_dot(rgba, x, h // 2, accent, 0.85)
        _sig_dot(rgba, x + 1, h // 2 - 1, dark, 0.60)
        _sig_dot(rgba, x + 1, h // 2 + 1, dark, 0.60)


def _sig_brute(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.3, 0.7):
        x = max(1, int(w * frac))
        _sig_vline(rgba, x, 1, h - 2, dark, 0.60)
        _sig_vline(rgba, x + 1, 1, h - 2, light, 0.28)
    _sig_dot(rgba, w // 2, h // 3, accent, 0.75)
    _sig_dot(rgba, w // 2 + 1, h // 3, accent, 0.75)


def _sig_stalker(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for x in range(1, w - 1, max(2, w // 2)):
        _sig_dot(rgba, x, h // 3, light, 0.55)
        _sig_dot(rgba, x + 1, h // 3 + 1, light, 0.55)
        _sig_dot(rgba, x + 1, h // 3 + 2, dark, 0.50)


def _sig_weaver(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    cx = w // 2
    _sig_vline(rgba, cx, 1, h - 2, light, 0.55)
    _sig_hline(rgba, h // 3, max(0, cx - 2), min(w - 1, cx + 2), dark, 0.60)
    _sig_hline(rgba, 2 * h // 3, max(0, cx - 2), min(w - 1, cx + 2), dark, 0.60)


def _sig_carrier(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.3, 0.6):
        y = max(1, int(h * frac))
        _sig_hline(rgba, y, 0, w - 1, dark, 0.65)
        _sig_hline(rgba, y - 1, 0, w - 1, light, 0.30)


def _sig_warden(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    y = h // 2
    x = w // 2
    for k in range(min(3, w // 2)):
        _sig_dot(rgba, x - k, y + k, accent, 0.85)
        _sig_dot(rgba, x + k, y + k, accent, 0.85)


def _sig_architect(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.3, 0.5, 0.7):
        y = max(1, int(h * frac))
        _sig_hline(rgba, y, 1, w - 2, dark, 0.75)
        _sig_hline(rgba, y + 1, 1, w - 2, light, 0.28)


def _sig_behemoth(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for frac in (0.25, 0.5, 0.75):
        y = max(1, int(h * frac))
        _sig_hline(rgba, y, 0, w - 1, dark, 0.70)
        _sig_hline(rgba, y + 1, 0, w - 1, light, 0.32)


def _sig_hive(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_vline(rgba, w // 2, 1, h - 2, dark, 0.60)
    _sig_hline(rgba, h // 2, 1, w - 2, dark, 0.60)
    for x in (1, w - 2):
        for y in (1, h - 2):
            _sig_dot(rgba, x, y, light, 0.80)


def _sig_matriarch(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for k in range(min(4, w // 2, h // 2)):
        _sig_dot(rgba, 1 + k, h // 2 - k, accent, 0.80)
        _sig_dot(rgba, w - 2 - k, h // 2 + k, accent, 0.80)


def _sig_mimic(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_vline(rgba, w // 2, 0, h - 1, dark, 0.65)
    _sig_hline(rgba, h // 3, w // 2, w - 2, light, 0.45)
    _sig_dot(rgba, w // 2 - 1, h // 3 + 1, accent, 0.70)


def _sig_chimera(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    for x, y in ((w // 3, h // 3), (2 * w // 3, 2 * h // 3)):
        _sig_dot(rgba, x, y, dark, 0.70)
        _sig_dot(rgba, x + 1, y + 1, dark, 0.70)
        _sig_dot(rgba, x + 1, y, light, 0.45)
        _sig_dot(rgba, x, y + 1, light, 0.45)


def _sig_drone(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_hline(rgba, h // 3, 1, w - 2, dark, 0.65)
    _sig_hline(rgba, 2 * h // 3, 1, w - 2, dark, 0.65)
    _sig_dot(rgba, w // 2, h // 2, accent, 0.85)


def _sig_colony_symbiont(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_hline(rgba, h // 4, 1, w - 2, dark, 0.60)
    _sig_vline(rgba, w // 2, h // 4, h - 2, light, 0.45)
    _sig_dot(rgba, w // 2, h // 2, accent, 0.85)


def _sig_spore_glider(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_hline(rgba, h // 2, 1, w - 2, light, 0.50)
    _sig_dot(rgba, w // 2, h // 3, accent, 0.80)


def _sig_root_keeper(ctx, rgba, dark, light, accent):
    w, h = ctx.rect.w, ctx.rect.h
    _sig_vline(rgba, w // 3, 1, h - 2, dark, 0.65)
    _sig_vline(rgba, (2 * w) // 3, 1, h - 2, dark, 0.65)
    _sig_dot(rgba, w // 2, h // 2, accent, 0.85)


SIGNATURES = {
    "sporeling": _sig_sporeling,
    "leech_parasite": _sig_leech,
    "acid_spitter": _sig_acid,
    "burrower": _sig_burrower,
    "pyro_shroom": _sig_pyro,
    "chitin_brute": _sig_brute,
    "mycelial_stalker": _sig_stalker,
    "synapse_weaver": _sig_weaver,
    "goliath_carrier": _sig_carrier,
    "colony_warden": _sig_warden,
    "spore_architect": _sig_architect,
    "spore_behemoth": _sig_behemoth,
    "hive_core": _sig_hive,
    "apex_matriarch": _sig_matriarch,
    "synaptic_mimic": _sig_mimic,
    "mycelial_chimera": _sig_chimera,
    "purifier_drone": _sig_drone,
    "colony_symbiont": _sig_colony_symbiont,
    "spore_glider": _sig_spore_glider,
    "root_keeper": _sig_root_keeper,
}


def _apply_signature(ctx: FaceContext, rgba: np.ndarray) -> np.ndarray:
    w, h = ctx.rect.w, ctx.rect.h
    if w * h < 12 or ctx.role not in ("skin", "armor", "bone"):
        return rgba
    if ctx.rect.kind in ("top", "bottom") and w * h < 36:
        return rgba
    fn = SIGNATURES.get(ctx.reg_id)
    if fn is None:
        return rgba
    dark = ctx.ramp[0, :3].astype(np.float32)
    light = ctx.ramp[-1, :3].astype(np.float32)
    accent = ctx.accent_ramp[-2, :3].astype(np.float32)
    fn(ctx, rgba, dark, light, accent)
    if ctx.rank == "mutated" and w >= 6 and h >= 6:
        for k in range(min(w - 2, 4)):
            _sig_dot(rgba, 1 + k, h - 2 - k, accent, 0.55)
    elif ctx.rank == "elite" and w >= 6 and h >= 6:
        _sig_hline(rgba, 1, 1, w - 2, accent, 0.45)
    return rgba

