"""Item icon v2 - a coherent pixel-art icon language for all Mycelial Mind items.

Every icon is composed from SDF parts into a single silhouette with flat pixel-art bands,
selective outline, upper-left rim light, lower-right core shadow and per-pixel LabPBR
material zones (metal blades, chitin shells, bioluminescent organs and crystal lenses).
"""

from __future__ import annotations

import os
import numpy as np

from ..compositor import channels_to_labpbr_s, normals_to_labpbr
from ..icon_language import IconCanvas, rot_box, rot_diamond
from ..materials import MATERIALS, Material
from ..palette_oklab import PALETTES
from ..sdf_engine import (
    add_pixel_outline,
    bayer_quantize,
    compute_sdf_ao,
    compute_sdf_lighting_v2,
    compute_sdf_normals,
    make_grid,
    save_flipbook,
    save_rgba,
    sdf_bezier,
    sdf_box,
    sdf_capsule,
    sdf_circle,
    sdf_diamond,
    sdf_onion,
    sdf_polygon,
    sdf_subtract,
    sdf_to_pom_height,
)

ITEM_RES = 16
PULSE_LEVELS = (0.0, 0.45, 1.0, 0.45)


def _p(c: IconCanvas, sdf, ramp: str, material: str = "CHITIN_BONE", tone: float = 0.55, **kw) -> IconCanvas:
    return c.paint(sdf, ramp, material, tone=tone, **kw)


def _emit(name: str, builder, item_dir: str, pbr_dir: str | None, animated: bool = False, frametime: int = 4) -> None:
    first = builder(0.0)
    if animated:
        frames = [builder(p).finish() for p in PULSE_LEVELS]
        save_flipbook(frames, os.path.join(item_dir, f"{name}.png"), frametime=frametime)
    else:
        save_rgba(first.finish(), os.path.join(item_dir, f"{name}.png"))
    if pbr_dir is not None:
        os.makedirs(pbr_dir, exist_ok=True)
        normal, spec = first.pbr_maps()
        save_rgba(normal, os.path.join(pbr_dir, f"{name}_n.png"))
        save_rgba(spec, os.path.join(pbr_dir, f"{name}_s.png"))


# ---------------------------------------------------------------------------
# Tools and weapons
# ---------------------------------------------------------------------------
def _bio_scanner(p: float) -> IconCanvas:
    c = IconCanvas(seed=101)
    xx, yy = c.xx, c.yy
    _p(c, sdf_box(xx, yy, -0.02, 0.10, 0.44, 0.55), "basalt_slate", "BASALT_SLATE", 0.46, height=0.55)
    _p(c, sdf_box(xx, yy, -0.02, 0.50, 0.32, 0.12), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.40)
    _p(c, sdf_box(xx, yy, -0.03, 0.00, 0.29, 0.19), "abyssal_indigo", "OBSIDIAN_CHITIN", 0.24, height=0.30)
    for wy in (-0.075, 0.075):
        line = np.abs(yy - wy) < 0.038
        c.zone(line, "leech_crimson", "BIOLUM_MYCELIUM", tone=0.75 + 0.25 * p)
        c.glow(line, "leech_crimson")
    _p(c, sdf_box(xx, yy, -0.02, 0.20, 0.35, 0.045), "chitin_bone", "CHITIN_BONE", 0.62)
    _p(c, sdf_box(xx, yy, 0.0, 0.39, 0.35, 0.045), "volcanic_ember", "PURIFICATION_GOLD", 0.62)
    _p(c, sdf_capsule(xx, yy, 0.36, -0.32, 0.50, -0.64, 0.07), "volcanic_ember", "PURIFICATION_GOLD", 0.55)
    _p(c, sdf_circle(xx, yy, 0.51, -0.68, 0.09), "leech_crimson", "BIOLUM_MYCELIUM", 0.95, emissive=True)
    c.zone(sdf_box(xx, yy, -0.02, 0.10, 0.44, 0.03), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.20)
    c.zone(sdf_box(xx, yy, -0.40, 0.10, 0.03, 0.55), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.20)
    c.zone(sdf_circle(xx, yy, -0.30, 0.40, 0.05), "spore_chartreuse", "TOXIC_SPORE", 0.85)
    return c


def _enzyme_lance(p: float) -> IconCanvas:
    c = IconCanvas(seed=102)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.70, 0.70, 0.02, -0.02, 0.07), "platinum_tech", "BASALT_SLATE", 0.52)
    for gy in (-0.44, -0.18):
        gp = sdf_capsule(xx, yy, gy - 0.26, gy + 0.34, gy - 0.18, gy + 0.26, 0.10)
        c.zone(gp, "volcanic_ember", "PURIFICATION_GOLD", tone=0.50)
    _p(c, sdf_capsule(xx, yy, 0.04, -0.04, 0.66, -0.66, 0.15), "spore_chartreuse", "TOXIC_SPORE", 0.52, height=0.72)
    c.zone(sdf_capsule(xx, yy, 0.06, -0.06, 0.64, -0.64, 0.05), "venom_emerald", "TOXIC_SPORE", tone=0.26)
    c.zone(sdf_capsule(xx, yy, -0.02, 0.10, 0.58, -0.64, 0.035), "warm_ivory", "CHITIN_BONE", tone=0.92)
    c.zone(sdf_capsule(xx, yy, 0.28, -0.28, 0.58, -0.54, 0.03), "acid_lime", "TOXIC_SPORE", tone=0.80 + 0.2 * p)
    c.glow(sdf_capsule(xx, yy, 0.28, -0.28, 0.58, -0.54, 0.03) <= 0.0, "acid_lime")
    _p(c, sdf_circle(xx, yy, 0.18, -0.18, 0.13), "mutant_amber", "TOXIC_SPORE", 0.70, emissive=True)
    _p(c, sdf_box(xx, yy, -0.68, 0.68, 0.070, 0.070), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.50)
    return c


def _spore_launcher(p: float) -> IconCanvas:
    c = IconCanvas(seed=103)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.62, 0.58, -0.30, 0.26, 0.15), "volcanic_ember", "PURIFICATION_GOLD", 0.48)
    _p(c, sdf_capsule(xx, yy, -0.52, 0.18, 0.58, -0.42, 0.16), "basalt_slate", "BASALT_SLATE", 0.50, height=0.62)
    c.zone(sdf_capsule(xx, yy, -0.48, 0.16, 0.54, -0.40, 0.05), "chitin_bone", "CHITIN_BONE", tone=0.76)
    for ci, t in enumerate((0.18, 0.46, 0.74)):
        cx = -0.46 + t * 1.00
        cy = 0.16 - t * 0.56
        boost = 0.85 if ci == (int(p * 3) % 3) else 0.55
        _p(c, sdf_circle(xx, yy, cx, cy, 0.14), "predator_amethyst", "LIVING_AMETHYST", boost, emissive=True)
    _p(c, sdf_circle(xx, yy, 0.62, -0.46, 0.14 + 0.02 * p), "spore_chartreuse", "TOXIC_SPORE", 0.85, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.62, -0.46, 0.14) <= 0.0, "spore_chartreuse")
    _p(c, sdf_box(xx, yy, -0.28, 0.46, 0.09, 0.20), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.46)
    c.zone(sdf_box(xx, yy, 0.02, 0.10, 0.10, 0.035), "volcanic_ember", "PURIFICATION_GOLD", 0.65)
    _p(c, sdf_box(xx, yy, -0.52, -0.06, 0.13, 0.06), "royal_gold", "PURIFICATION_GOLD", 0.62)
    return c

def _symbiotic_blade(p: float) -> IconCanvas:
    c = IconCanvas(seed=104)
    xx, yy = c.xx, c.yy
    blade = np.minimum(
        sdf_capsule(xx, yy, -0.30, 0.30, 0.62, -0.62, 0.15),
        sdf_capsule(xx, yy, 0.50, -0.50, 0.78, -0.78, 0.075),
    )
    _p(c, blade, "mycelial_violet", "LIVING_AMETHYST", 0.52, height=0.72)
    c.zone(sdf_capsule(xx, yy, -0.34, 0.34, 0.60, -0.60, 0.05), "chitin_bone", "CHITIN_BONE", tone=0.74)
    c.zone(sdf_capsule(xx, yy, -0.24, 0.24, 0.70, -0.70, 0.03), "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.16)
    c.zone(sdf_capsule(xx, yy, -0.18, 0.18, 0.54, -0.54, 0.022), "biolum_cyan", "BIOLUM_CYAN", tone=0.82 + 0.18 * p)
    c.glow(sdf_capsule(xx, yy, -0.18, 0.18, 0.54, -0.54, 0.022) <= 0.0, "biolum_cyan")
    _p(c, sdf_capsule(xx, yy, -0.58, 0.18, -0.18, 0.58, 0.095), "chitin_bone", "CHITIN_BONE", 0.58)
    _p(c, sdf_capsule(xx, yy, -0.72, 0.72, -0.44, 0.44, 0.095), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.34)
    _p(c, sdf_circle(xx, yy, -0.42, 0.42, 0.105), "leech_crimson", "BIOLUM_MYCELIUM", 0.95, emissive=True)
    c.zone(sdf_box(xx, yy, -0.56, 0.56, 0.035, 0.16), "royal_gold", "PURIFICATION_GOLD", 0.62)
    return c


def _spore_antidote(p: float) -> IconCanvas:
    c = IconCanvas(seed=105)
    xx, yy = c.xx, c.yy
    flask = sdf_circle(xx, yy, 0.0, 0.20, 0.46)
    _p(c, flask, "warm_ivory", "LIVING_AMETHYST", 0.62, height=0.62)
    liquid = (flask <= -0.06) & (yy >= 0.14)
    c.zone(liquid, "spore_chartreuse", "TOXIC_SPORE", tone=0.62)
    for bx, by in ((-0.16, 0.28), (0.06, 0.40), (0.18, 0.24)):
        c.zone(sdf_circle(xx, yy, bx, by, 0.05) <= 0.0, "acid_lime", "TOXIC_SPORE", tone=0.95)
        c.glow(sdf_circle(xx, yy, bx, by, 0.05) <= 0.0, "acid_lime")
    c.zone(sdf_capsule(xx, yy, -0.30, 0.02, -0.22, 0.34, 0.03), "warm_ivory", "LIVING_AMETHYST", tone=0.95)
    _p(c, sdf_box(xx, yy, 0.0, -0.36, 0.13, 0.13), "warm_ivory", "LIVING_AMETHYST", 0.62)
    _p(c, sdf_box(xx, yy, 0.0, -0.50, 0.15, 0.09), "volcanic_ember", "PURIFICATION_GOLD", 0.55)
    c.zone(sdf_box(xx, yy, 0.0, -0.50, 0.15, 0.025), "chitin_bone", "CHITIN_BONE", 0.80)
    return c


def _purifier_grenade(p: float) -> IconCanvas:
    c = IconCanvas(seed=106)
    xx, yy = c.xx, c.yy
    body = sdf_polygon(xx, yy, 0.0, 0.10, 0.60, sides=8)
    _p(c, body, "basalt_slate", "BASALT_SLATE", 0.50, height=0.68)
    c.zone(np.abs(yy - 0.10) < 0.05, "chitin_bone", "CHITIN_BONE", tone=0.60)
    c.zone(np.abs(xx) < 0.05, "chitin_bone", "CHITIN_BONE", tone=0.60)
    _p(c, sdf_box(xx, yy, 0.0, -0.50, 0.13, 0.12), "chitin_bone", "CHITIN_BONE", 0.62)
    _p(c, sdf_box(xx, yy, 0.0, -0.64, 0.06, 0.10), "volcanic_ember", "PURIFICATION_GOLD", 0.55)
    _p(c, sdf_diamond(xx, yy, 0.0, 0.10, 0.22 + 0.03 * p), "predator_amethyst", "LIVING_AMETHYST", 0.85, emissive=True)
    c.glow(sdf_diamond(xx, yy, 0.0, 0.10, 0.20) <= 0.0, "predator_amethyst")
    for qx, qy in ((-0.42, -0.22), (0.42, -0.22), (-0.42, 0.42), (0.42, 0.42)):
        c.zone(sdf_circle(xx, yy, qx, qy, 0.055) <= 0.0, "chitin_bone", "CHITIN_BONE", tone=0.82)
    return c


def _bio_extractor(p: float) -> IconCanvas:
    c = IconCanvas(seed=107)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.60, 0.60, 0.16, -0.16, 0.15), "predator_amethyst", "LIVING_AMETHYST", 0.55, height=0.65)
    c.zone(sdf_capsule(xx, yy, -0.56, 0.56, 0.12, -0.12, 0.05), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.30)
    c.zone(sdf_capsule(xx, yy, -0.50, 0.50, 0.06, -0.06, 0.022), "spore_chartreuse", "TOXIC_SPORE", tone=0.85 + 0.15 * p)
    c.glow(sdf_capsule(xx, yy, -0.50, 0.50, 0.06, -0.06, 0.022) <= 0.0, "spore_chartreuse")
    _p(c, sdf_capsule(xx, yy, 0.14, -0.14, 0.66, -0.66, 0.10), "platinum_tech", "BASALT_SLATE", 0.60)
    _p(c, sdf_box(xx, yy, -0.66, 0.66, 0.09, 0.09), "chitin_bone", "CHITIN_BONE", 0.62)
    _p(c, sdf_box(xx, yy, -0.58, 0.58, 0.05, 0.14), "volcanic_ember", "PURIFICATION_GOLD", 0.55)
    return c


# ---------------------------------------------------------------------------
# Chitin armour
# ---------------------------------------------------------------------------
def _chitin_helmet(p: float) -> IconCanvas:
    c = IconCanvas(seed=108)
    xx, yy = c.xx, c.yy
    dome = sdf_circle(xx, yy, 0.0, -0.12, 0.58)
    cheeks = sdf_box(xx, yy, 0.0, 0.28, 0.44, 0.30)
    shell = sdf_subtract(np.minimum(dome, cheeks), sdf_circle(xx, yy, 0.0, 0.22, 0.21))
    _p(c, shell, "chitin_bone", "CHITIN_BONE", 0.60, height=0.62)
    c.zone((np.abs(xx) > 0.30) | (yy > 0.34), "chitin_bone", "CHITIN_BONE", tone=0.42)
    c.zone(np.abs(yy - 0.00) < 0.075, "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.16)
    c.zone(np.abs(yy - 0.00) < 0.035, "leech_crimson", "BIOLUM_MYCELIUM", tone=1.0)
    c.glow(np.abs(yy - 0.00) < 0.035, "leech_crimson")
    for sx in (-1.0, 1.0):
        _p(c, sdf_capsule(xx, yy, sx * 0.46, 0.06, sx * 0.56, -0.44, 0.07), "chitin_bone", "CHITIN_BONE", 0.52)
    c.zone(np.abs(sdf_capsule(xx, yy, 0.0, -0.58, 0.0, -0.22, 0.03)) < 0.03, "warm_ivory", "CHITIN_BONE", tone=0.95)
    c.zone(sdf_box(xx, yy, 0.0, 0.52, 0.34, 0.03), "chitin_bone", "CHITIN_BONE", tone=0.85)
    return c


def _chitin_chestplate(p: float) -> IconCanvas:
    c = IconCanvas(seed=109)
    xx, yy = c.xx, c.yy
    torso = sdf_polygon(xx, yy, 0.0, 0.12, 0.58, sides=4)
    torso = sdf_subtract(torso, sdf_circle(xx, yy, 0.0, -0.34, 0.18))
    _p(c, torso, "chitin_bone", "CHITIN_BONE", 0.58, height=0.66)
    for sx in (-1.0, 1.0):
        _p(c, sdf_circle(xx, yy, sx * 0.44, -0.20, 0.20), "chitin_bone", "CHITIN_BONE", 0.55)
        c.zone(sdf_circle(xx, yy, sx * 0.44, -0.20, 0.10) <= 0.0, "chitin_bone", "CHITIN_BONE", tone=0.78)
    c.zone(np.abs(yy - 0.12) < 0.045, "chitin_bone", "CHITIN_BONE", tone=0.36)
    for ry in (0.26, 0.40):
        c.zone(np.abs(yy - ry) < 0.025, "chitin_bone", "CHITIN_BONE", tone=0.78)
    c.zone(sdf_capsule(xx, yy, -0.28, -0.02, 0.28, -0.02, 0.045), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.75)
    _p(c, sdf_circle(xx, yy, 0.0, 0.05, 0.12), "spore_chartreuse", "TOXIC_SPORE", 0.90, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.05, 0.12) <= 0.0, "spore_chartreuse")
    return c


def _chitin_leggings(p: float) -> IconCanvas:
    c = IconCanvas(seed=110)
    xx, yy = c.xx, c.yy
    _p(c, sdf_box(xx, yy, 0.0, -0.46, 0.38, 0.09), "chitin_bone", "CHITIN_BONE", 0.58, height=0.58)
    _p(c, sdf_box(xx, yy, 0.0, -0.26, 0.40, 0.16), "chitin_bone", "CHITIN_BONE", 0.52, height=0.60)
    for sx in (-1.0, 1.0):
        leg = sdf_box(xx, yy, sx * 0.22, 0.18, 0.14, 0.36)
        knee = sdf_polygon(xx, yy, sx * 0.22, 0.10, 0.19, sides=6)
        _p(c, np.minimum(leg, knee), "chitin_bone", "CHITIN_BONE", 0.52, height=0.62)
        c.zone(np.abs(yy - 0.10) < 0.035, "chitin_bone", "CHITIN_BONE", tone=0.28)
        c.zone(sdf_box(xx, yy, sx * 0.22, 0.46, 0.10, 0.035), "chitin_bone", "CHITIN_BONE", tone=0.82)
    c.zone(sdf_box(xx, yy, 0.0, -0.46, 0.38, 0.025), "chitin_bone", "CHITIN_BONE", tone=0.35)
    _p(c, sdf_diamond(xx, yy, 0.0, -0.26, 0.09), "mycelial_violet", "BIOLUM_MYCELIUM", 0.80, emissive=True)
    c.glow(sdf_diamond(xx, yy, 0.0, -0.26, 0.09) <= 0.0, "mycelial_violet")
    return c


def _chitin_boots(p: float) -> IconCanvas:
    c = IconCanvas(seed=111)
    xx, yy = c.xx, c.yy
    for sx in (-1.0, 1.0):
        ankle = sdf_box(xx, yy, sx * 0.26, 0.02, 0.13, 0.30)
        cuff = sdf_box(xx, yy, sx * 0.26, -0.26, 0.16, 0.07)
        foot = sdf_box(xx, yy, sx * 0.30, 0.40, 0.22, 0.13)
        toe = sdf_circle(xx, yy, sx * 0.48, 0.42, 0.10)
        boot = np.minimum(np.minimum(ankle, cuff), np.minimum(foot, toe))
        _p(c, boot, "chitin_bone", "CHITIN_BONE", 0.56, height=0.64)
        c.zone(sdf_box(xx, yy, sx * 0.30, 0.50, 0.28, 0.035), "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.20)
        c.zone(sdf_box(xx, yy, sx * 0.26, -0.26, 0.16, 0.025), "chitin_bone", "CHITIN_BONE", tone=0.82)
        c.zone(sdf_capsule(xx, yy, sx * 0.26, 0.08, sx * 0.26, 0.30, 0.03), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.68)
        c.zone(sdf_circle(xx, yy, sx * 0.26, 0.02, 0.05) <= 0.0, "chitin_bone", "CHITIN_BONE", tone=0.88)
    return c


def _chitin_leggings(p: float) -> IconCanvas:
    c = IconCanvas(seed=110)
    xx, yy = c.xx, c.yy
    _p(c, sdf_box(xx, yy, 0.0, -0.46, 0.38, 0.09), "chitin_bone", "CHITIN_BONE", 0.58, height=0.58)
    _p(c, sdf_box(xx, yy, 0.0, -0.26, 0.40, 0.16), "chitin_bone", "CHITIN_BONE", 0.52, height=0.60)
    for sx in (-1.0, 1.0):
        leg = sdf_box(xx, yy, sx * 0.22, 0.18, 0.14, 0.36)
        knee = sdf_polygon(xx, yy, sx * 0.22, 0.10, 0.19, sides=6)
        _p(c, np.minimum(leg, knee), "chitin_bone", "CHITIN_BONE", 0.52, height=0.62)
        c.zone(np.abs(yy - 0.10) < 0.035, "chitin_bone", "CHITIN_BONE", tone=0.28)
        c.zone(sdf_box(xx, yy, sx * 0.22, 0.46, 0.10, 0.035), "chitin_bone", "CHITIN_BONE", tone=0.82)
    c.zone(sdf_box(xx, yy, 0.0, -0.46, 0.38, 0.025), "chitin_bone", "CHITIN_BONE", tone=0.35)
    _p(c, sdf_box(xx, yy, 0.0, -0.20, 0.07, 0.10), "mycelial_violet", "BIOLUM_MYCELIUM", 0.70, emissive=True)
    c.glow(sdf_box(xx, yy, 0.0, -0.20, 0.07, 0.10) <= 0.0, "mycelial_violet")
    return c


def _chitin_plate(p: float) -> IconCanvas:
    c = IconCanvas(seed=112)
    xx, yy = c.xx, c.yy
    plate = rot_box(xx, yy, 0.0, 0.02, 0.44, 0.44, 0.34)
    plate = np.minimum(plate, sdf_circle(xx, yy, 0.0, 0.02, 0.62))
    plate = sdf_subtract(plate, sdf_circle(xx, yy, 0.58, -0.34, 0.13))
    plate = sdf_subtract(plate, sdf_circle(xx, yy, -0.56, 0.40, 0.11))
    _p(c, plate, "elite_carapace", "OBSIDIAN_CHITIN", 0.50, height=0.66)
    c.zone(sdf_onion(plate, 0.055), "chitin_bone", "CHITIN_BONE", tone=0.72)
    c.zone(sdf_capsule(xx, yy, -0.34, -0.20, 0.30, 0.16, 0.030), "chitin_bone", "CHITIN_BONE", tone=0.82)
    c.zone(sdf_capsule(xx, yy, -0.38, 0.12, 0.10, 0.38, 0.026), "chitin_bone", "CHITIN_BONE", tone=0.65)
    _p(c, sdf_capsule(xx, yy, -0.42, -0.06, -0.28, 0.22, 0.035), "mycelial_violet", "BIOLUM_MYCELIUM", 0.75, emissive=True)
    for rx, ry in ((-0.32, -0.28), (0.22, -0.10), (0.02, 0.26)):
        _p(c, sdf_circle(xx, yy, rx, ry, 0.06), "mutant_amber", "PURIFICATION_GOLD", 0.82)
        c.zone(sdf_circle(xx, yy, rx - 0.02, ry - 0.02, 0.024) <= 0.0, "warm_ivory", "CHITIN_BONE", tone=0.95)
    return c


# ---------------------------------------------------------------------------
# Vials, serums and injectors
# ---------------------------------------------------------------------------
def _concentrated_antidote(p: float) -> IconCanvas:
    c = IconCanvas(seed=113)
    xx, yy = c.xx, c.yy
    flask = sdf_circle(xx, yy, 0.0, 0.16, 0.44)
    _p(c, flask, "predator_amethyst", "LIVING_AMETHYST", 0.55, height=0.62)
    c.zone((flask <= -0.07) & (yy >= 0.16), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.72)
    c.zone(sdf_capsule(xx, yy, -0.28, 0.02, -0.20, 0.30, 0.03), "warm_ivory", "CHITIN_BONE", tone=0.95)
    _p(c, sdf_box(xx, yy, 0.0, -0.34, 0.12, 0.13), "warm_ivory", "CHITIN_BONE", 0.62)
    _p(c, sdf_box(xx, yy, 0.0, -0.48, 0.15, 0.09), "volcanic_ember", "PURIFICATION_GOLD", 0.55)
    _p(c, sdf_circle(xx, yy, 0.0, 0.20, 0.08), "warm_ivory", "BIOLUM_MYCELIUM", 0.95 + 0.05 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.20, 0.08) <= 0.0, "warm_ivory")
    return c


def _counter_serum(p: float) -> IconCanvas:
    c = IconCanvas(seed=114)
    xx, yy = c.xx, c.yy
    amp = sdf_capsule(xx, yy, 0.0, -0.26, 0.0, 0.28, 0.20)
    top = sdf_diamond(xx, yy, 0.0, -0.38, 0.19)
    bottom = sdf_diamond(xx, yy, 0.0, 0.40, 0.19)
    _p(c, np.minimum(np.minimum(amp, top), bottom), "warm_ivory", "LIVING_AMETHYST", 0.62, height=0.60)
    c.zone(sdf_box(xx, yy, 0.0, 0.10, 0.20, 0.22), "spore_chartreuse", "TOXIC_SPORE", tone=0.62)
    c.zone(sdf_box(xx, yy, 0.0, 0.10, 0.20, 0.025), "venom_emerald", "TOXIC_SPORE", tone=0.30)
    c.zone(sdf_box(xx, yy, 0.0, -0.20, 0.20, 0.025), "chitin_bone", "CHITIN_BONE", tone=0.88)
    c.zone(sdf_box(xx, yy, 0.0, -0.02, 0.20, 0.012), "acid_lime", "TOXIC_SPORE", tone=0.92 + 0.08 * p)
    c.glow(sdf_box(xx, yy, 0.0, 0.10, 0.20, 0.012) <= 0.0, "acid_lime")
    return c


def _genetic_sample(p: float) -> IconCanvas:
    c = IconCanvas(seed=115)
    xx, yy = c.xx, c.yy
    tube = sdf_box(xx, yy, 0.0, 0.02, 0.22, 0.36)
    _p(c, tube, "warm_ivory", "LIVING_AMETHYST", 0.62, height=0.58)
    c.zone(sdf_box(xx, yy, 0.0, 0.18, 0.22, 0.16), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.62)
    c.zone(sdf_box(xx, yy, 0.0, -0.32, 0.22, 0.05), "chitin_bone", "CHITIN_BONE", tone=0.88)
    c.zone(sdf_box(xx, yy, 0.0, 0.36, 0.22, 0.035), "chitin_bone", "CHITIN_BONE", tone=0.34)
    swab = sdf_capsule(xx, yy, 0.0, -0.50, 0.0, -0.22, 0.05)
    _p(c, swab, "warm_ivory", "CHITIN_BONE", 0.80)
    c.zone(sdf_circle(xx, yy, 0.0, -0.52, 0.085) <= 0.0, "spore_chartreuse", "TOXIC_SPORE", tone=0.88)
    c.zone(sdf_box(xx, yy, 0.0, -0.18, 0.18, 0.035), "volcanic_ember", "PURIFICATION_GOLD", tone=0.62)
    c.zone(sdf_capsule(xx, yy, -0.13, -0.10, -0.13, 0.34, 0.025), "warm_ivory", "LIVING_AMETHYST", tone=0.95)
    return c


def _symbiotic_mutagen(p: float) -> IconCanvas:
    c = IconCanvas(seed=116)
    xx, yy = c.xx, c.yy
    body = sdf_capsule(xx, yy, 0.0, -0.20, 0.0, 0.20, 0.22)
    _p(c, body, "elite_carapace", "OBSIDIAN_CHITIN", 0.54, height=0.62)
    c.zone(sdf_box(xx, yy, 0.0, 0.02, 0.15, 0.16), "leech_crimson", "BIOLUM_MYCELIUM", tone=0.66)
    c.zone(sdf_box(xx, yy, 0.0, 0.02, 0.15, 0.03), "elite_crimson", "BIOLUM_MYCELIUM", tone=0.95)
    c.zone(sdf_box(xx, yy, 0.0, -0.16, 0.22, 0.035), "chitin_bone", "CHITIN_BONE", tone=0.85)
    c.zone(sdf_box(xx, yy, 0.0, 0.20, 0.22, 0.035), "chitin_bone", "CHITIN_BONE", tone=0.85)
    _p(c, sdf_box(xx, yy, 0.0, -0.30, 0.10, 0.10), "chitin_bone", "CHITIN_BONE", 0.62)
    _p(c, sdf_box(xx, yy, 0.0, -0.44, 0.18, 0.06), "chitin_bone", "CHITIN_BONE", 0.58)
    _p(c, sdf_capsule(xx, yy, 0.0, 0.20, 0.0, 0.56, 0.035), "platinum_tech", "BASALT_SLATE", 0.62)
    c.zone(sdf_box(xx, yy, -0.11, 0.06, 0.03, 0.16), "warm_ivory", "CHITIN_BONE", tone=0.95)
    return c

def _schism_injector(p: float) -> IconCanvas:
    c = IconCanvas(seed=117)
    xx, yy = c.xx, c.yy
    _p(c, sdf_box(xx, yy, 0.0, 0.04, 0.24, 0.44), "chitin_bone", "CHITIN_BONE", 0.60, height=0.62)
    c.zone(sdf_box(xx, yy, 0.0, 0.04, 0.24, 0.03), "chitin_bone", "CHITIN_BONE", tone=0.36)
    _p(c, sdf_circle(xx, yy, -0.09, 0.10, 0.13), "mutant_amber", "PURIFICATION_GOLD", 0.70, emissive=True)
    _p(c, sdf_circle(xx, yy, 0.09, 0.10, 0.13), "elite_crimson", "BIOLUM_MYCELIUM", 0.70, emissive=True)
    c.glow((sdf_circle(xx, yy, -0.09, 0.10, 0.10) <= 0.0) | (sdf_circle(xx, yy, 0.09, 0.10, 0.10) <= 0.0), "mutant_amber")
    _p(c, sdf_box(xx, yy, 0.0, -0.46, 0.10, 0.10), "platinum_tech", "BASALT_SLATE", 0.60)
    _p(c, sdf_capsule(xx, yy, 0.0, -0.50, 0.0, -0.74, 0.03), "platinum_tech", "BASALT_SLATE", 0.65)
    _p(c, sdf_box(xx, yy, 0.0, 0.52, 0.16, 0.05), "basalt_slate", "BASALT_SLATE", 0.50)
    return c


def _false_engram_injector(p: float) -> IconCanvas:
    c = IconCanvas(seed=118)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.44, 0.44, 0.14, -0.14, 0.16), "volcanic_ember", "PURIFICATION_GOLD", 0.50, height=0.62)
    c.zone(sdf_capsule(xx, yy, -0.40, 0.40, 0.10, -0.10, 0.05), "mutant_amber", "PURIFICATION_GOLD", tone=0.70)
    _p(c, sdf_capsule(xx, yy, 0.12, -0.12, 0.78, -0.78, 0.065), "predator_amethyst", "LIVING_AMETHYST", 0.72, emissive=True)
    c.glow(sdf_capsule(xx, yy, 0.50, -0.50, 0.78, -0.78, 0.045) <= 0.0, "predator_amethyst")
    _p(c, sdf_box(xx, yy, -0.56, 0.56, 0.16, 0.05), "chitin_bone", "CHITIN_BONE", 0.62)
    _p(c, sdf_capsule(xx, yy, -0.62, 0.62, -0.44, 0.44, 0.05), "chitin_bone", "CHITIN_BONE", 0.58)
    c.zone(sdf_capsule(xx, yy, -0.30, 0.30, -0.12, 0.12, 0.035), "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.30)
    return c

def _synaptic_graft(p: float) -> IconCanvas:
    c = IconCanvas(seed=119)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.52, 0.52, 0.16, -0.16, 0.17), "volcanic_ember", "PURIFICATION_GOLD", 0.50, height=0.62)
    _p(c, sdf_circle(xx, yy, -0.44, 0.44, 0.15), "mutant_amber", "PURIFICATION_GOLD", 0.72, emissive=True)
    c.zone(sdf_capsule(xx, yy, -0.50, 0.50, 0.12, -0.12, 0.04), "mutant_amber", "PURIFICATION_GOLD", tone=0.62)
    _p(c, sdf_capsule(xx, yy, 0.16, -0.16, 0.62, -0.62, 0.07), "spore_chartreuse", "TOXIC_SPORE", 0.72, emissive=True)
    c.glow(sdf_capsule(xx, yy, 0.36, -0.36, 0.62, -0.62, 0.05) <= 0.0, "spore_chartreuse")
    _p(c, sdf_capsule(xx, yy, 0.62, -0.62, 0.74, -0.30, 0.045), "spore_chartreuse", "TOXIC_SPORE", 0.70)
    _p(c, sdf_box(xx, yy, -0.62, 0.62, 0.06, 0.06), "chitin_bone", "CHITIN_BONE", 0.58)
    return c

def _neural_echo_decoy(p: float) -> IconCanvas:
    c = IconCanvas(seed=120)
    xx, yy = c.xx, c.yy
    ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.52), 0.09)
    _p(c, ring, "volcanic_ember", "PURIFICATION_GOLD", 0.52, height=0.55)
    for ang in (0.0, 90.0, 180.0, 270.0):
        a = np.deg2rad(ang)
        _p(c, sdf_capsule(xx, yy, np.cos(a) * 0.46, np.sin(a) * 0.46, np.cos(a) * 0.74, np.sin(a) * 0.74, 0.055),
           "mutant_amber", "PURIFICATION_GOLD", 0.62)
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.24), "predator_amethyst", "LIVING_AMETHYST", 0.70, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.0, 0.16) <= 0.0, "predator_amethyst")
    c.zone(sdf_circle(xx, yy, 0.0, 0.0, 0.08) <= 0.0, "warm_ivory", "BIOLUM_MYCELIUM", tone=0.95)
    return c

def _latent_spore(p: float) -> IconCanvas:
    c = IconCanvas(seed=121)
    xx, yy = c.xx, c.yy
    sac = sdf_circle(xx, yy * 1.3, 0.0, 0.22, 0.42)
    _p(c, sac, "spore_chartreuse", "TOXIC_SPORE", 0.62, height=0.65)
    c.zone(xx > 0.10, "spore_chartreuse", "TOXIC_SPORE", tone=0.42)
    cap = sdf_subtract(sdf_circle(xx, yy * 1.15, 0.0, -0.12, 0.52), sdf_box(xx, yy, 0.0, 0.22, 0.9, 0.10))
    _p(c, cap, "mycelial_violet", "BIOLUM_MYCELIUM", 0.60, height=0.75)
    c.zone(sdf_box(xx, yy, 0.0, -0.40, 0.34, 0.04), "warm_ivory", "CHITIN_BONE", tone=0.90)
    for gx in (-0.24, -0.08, 0.08, 0.24):
        c.zone(np.abs(sdf_capsule(xx, yy, gx, 0.02, gx * 1.15, 0.16, 0.015)) < 0.03, "venom_emerald", "TOXIC_SPORE", tone=0.18)
    _p(c, sdf_box(xx, yy, 0.0, 0.56, 0.08, 0.14), "chitin_bone", "CHITIN_BONE", 0.55)
    _p(c, sdf_circle(xx, yy, 0.0, 0.20, 0.10), "warm_ivory", "BIOLUM_MYCELIUM", 0.95 + 0.05 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.20, 0.10) <= 0.0, "warm_ivory")
    return c


def _mycelial_biomass(p: float) -> IconCanvas:
    c = IconCanvas(seed=122)
    xx, yy = c.xx, c.yy
    strands = np.minimum(
        sdf_bezier(xx, yy, (-0.52, 0.44), (-0.16, -0.10), (-0.26, -0.58), 0.15),
        sdf_bezier(xx, yy, (-0.06, 0.56), (0.30, 0.12), (0.18, -0.46), 0.13),
    )
    clump = sdf_circle(xx, yy, 0.10, 0.20, 0.24)
    _p(c, np.minimum(strands, clump), "mycelial_violet", "BIOLUM_MYCELIUM", 0.55, height=0.58)
    c.zone(np.abs(strands) < 0.025, "warm_ivory", "BIOLUM_MYCELIUM", tone=0.85)
    c.zone(sdf_circle(xx, yy, 0.14, 0.24, 0.10), "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.32)
    for nx, ny, rr in ((-0.46, 0.40, 0.11), (-0.22, -0.50, 0.12), (0.30, -0.34, 0.10)):
        _p(c, sdf_circle(xx, yy, nx, ny, rr), "spore_chartreuse", "TOXIC_SPORE", 0.88, emissive=True)
        c.glow(sdf_circle(xx, yy, nx, ny, rr) <= 0.0, "spore_chartreuse")
    _p(c, sdf_circle(xx, yy, 0.36, 0.34, 0.11), "chitin_bone", "CHITIN_BONE", 0.52)
    c.zone(sdf_circle(xx, yy, 0.36, 0.34, 0.045) <= 0.0, "chitin_bone", "CHITIN_BONE", tone=0.82)
    return c


def _overmind_essence(p: float) -> IconCanvas:
    c = IconCanvas(seed=123)
    xx, yy = c.xx, c.yy
    shard = sdf_polygon(xx * 0.82, yy * 1.10, 0.0, 0.02, 0.58, sides=6)
    _p(c, shard, "predator_amethyst", "LIVING_AMETHYST", 0.58, height=0.85)
    c.zone(np.abs(xx + 0.08) < 0.05, "mycelial_violet", "LIVING_AMETHYST", tone=0.30)
    c.zone(np.abs(xx - 0.16) < 0.05, "mycelial_violet", "LIVING_AMETHYST", tone=0.78)
    c.zone(np.abs(yy - 0.02) < 0.035, "mycelial_violet", "LIVING_AMETHYST", tone=0.22)
    _p(c, sdf_diamond(xx, yy, 0.0, 0.0, 0.20), "spore_chartreuse", "TOXIC_SPORE", 0.95 + 0.05 * p, emissive=True, height=0.95)
    c.glow(sdf_diamond(xx, yy, 0.0, 0.0, 0.20) <= 0.0, "spore_chartreuse")
    return c

def _rift_catalyst(p: float) -> IconCanvas:
    c = IconCanvas(seed=124)
    xx, yy = c.xx, c.yy
    left = np.minimum(sdf_polygon(xx, yy, -0.22, 0.04, 0.34, sides=3), sdf_box(xx, yy, -0.20, 0.10, 0.12, 0.44))
    right = np.minimum(sdf_polygon(xx, yy, 0.22, 0.04, 0.34, sides=3), sdf_box(xx, yy, 0.20, 0.10, 0.12, 0.44))
    _p(c, left, "predator_amethyst", "LIVING_AMETHYST", 0.55, height=0.75)
    _p(c, right, "predator_amethyst", "LIVING_AMETHYST", 0.68, height=0.75)
    c.zone(np.abs(xx) < 0.045, "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.90 + 0.10 * p)
    c.glow(np.abs(xx) < 0.045, "mycelial_violet")
    c.zone((np.abs(xx) > 0.05) & (np.abs(xx) < 0.10), "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.15)
    c.zone(sdf_capsule(xx, yy, -0.30, -0.30, -0.14, 0.28, 0.025), "mycelial_violet", "LIVING_AMETHYST", tone=0.85)
    return c


def _synaptic_core(p: float) -> IconCanvas:
    c = IconCanvas(seed=125)
    xx, yy = c.xx, c.yy
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.34), "mycelial_violet", "BIOLUM_MYCELIUM", 0.58, height=0.80)
    for ang in range(6):
        a = ang * np.pi / 3.0
        ax, ay = np.cos(a), np.sin(a)
        _p(c, sdf_capsule(xx, yy, ax * 0.18, ay * 0.18, ax * 0.66, ay * 0.66, 0.075), "chitin_bone", "CHITIN_BONE", 0.55)
        _p(c, sdf_circle(xx, yy, ax * 0.70, ay * 0.70, 0.075), "spore_chartreuse", "TOXIC_SPORE", 0.90, emissive=True)
        c.glow(sdf_circle(xx, yy, ax * 0.70, ay * 0.70, 0.075) <= 0.0, "spore_chartreuse")
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.16), "warm_ivory", "BIOLUM_MYCELIUM", 0.95 + 0.05 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.0, 0.16) <= 0.0, "warm_ivory")
    return c


def _neural_echo_core(p: float) -> IconCanvas:
    c = IconCanvas(seed=126)
    xx, yy = c.xx, c.yy
    _p(c, sdf_polygon(xx, yy, 0.0, 0.0, 0.50, sides=6), "spore_chartreuse", "TOXIC_SPORE", 0.60, height=0.72)
    c.zone(sdf_onion(sdf_polygon(xx, yy, 0.0, 0.0, 0.44, sides=6), 0.04), "venom_emerald", "TOXIC_SPORE", tone=0.30)
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.24), "warm_ivory", "BIOLUM_MYCELIUM", 0.95, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.0, 0.24) <= 0.0, "warm_ivory")
    for ex, ey in ((-0.46, 0.10), (0.46, 0.10), (0.0, 0.50)):
        c.zone(sdf_capsule(xx, yy, ex, ey, ex * 1.35, ey * 1.25, 0.05), "venom_emerald", "TOXIC_SPORE", tone=0.55)
    return c


def _overmind_crown(p: float) -> IconCanvas:
    c = IconCanvas(seed=127)
    xx, yy = c.xx, c.yy
    band = sdf_box(xx, yy, 0.0, 0.34, 0.56, 0.18)
    _p(c, band, "predator_amethyst", "LIVING_AMETHYST", 0.55, height=0.62)
    c.zone(np.abs(yy - 0.44) < 0.035, "chitin_bone", "CHITIN_BONE", tone=0.72)
    for px, ph in ((-0.42, 0.40), (0.0, 0.52), (0.42, 0.40)):
        spike = sdf_polygon(xx, yy, px, -0.06 - (0.30 - ph * 0.2), 0.22, sides=3)
        _p(c, spike, "predator_amethyst", "LIVING_AMETHYST", 0.62, height=0.72)
        c.zone(sdf_capsule(xx, yy, px, 0.10, px, -0.40, 0.025), "mycelial_violet", "LIVING_AMETHYST", tone=0.32)
    _p(c, sdf_diamond(xx, yy, 0.0, 0.30, 0.14), "spore_chartreuse", "TOXIC_SPORE", 0.92 + 0.08 * p, emissive=True)
    c.glow(sdf_diamond(xx, yy, 0.0, 0.30, 0.12) <= 0.0, "spore_chartreuse")
    for gx in (-0.34, 0.34):
        _p(c, sdf_diamond(xx, yy, gx, 0.20, 0.07), "warm_ivory", "BIOLUM_MYCELIUM", 0.80)
    return c


def _purifier_drone_core(p: float) -> IconCanvas:
    c = IconCanvas(seed=128)
    xx, yy = c.xx, c.yy
    shell = sdf_polygon(xx, yy, 0.0, 0.0, 0.62, sides=8)
    _p(c, shell, "chitin_bone", "CHITIN_BONE", 0.56, height=0.70)
    c.zone(sdf_onion(sdf_polygon(xx, yy, 0.0, 0.0, 0.50, sides=8), 0.05), "royal_gold", "PURIFICATION_GOLD", tone=0.62)
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.30), "predator_amethyst", "LIVING_AMETHYST", 0.70, height=0.80)
    _p(c, sdf_circle(xx, yy, 0.0, 0.0, 0.14), "warm_ivory", "BIOLUM_MYCELIUM", 0.95 + 0.05 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.0, 0.14) <= 0.0, "warm_ivory")
    for ang in (45.0, 135.0, 225.0, 315.0):
        a = np.deg2rad(ang)
        _p(c, sdf_circle(xx, yy, np.cos(a) * 0.44, np.sin(a) * 0.44, 0.07), "chitin_bone", "CHITIN_BONE", 0.72)
    return c


def _synaptic_suppressor(p: float) -> IconCanvas:
    c = IconCanvas(seed=129)
    xx, yy = c.xx, c.yy
    _p(c, sdf_capsule(xx, yy, -0.72, 0.72, 0.22, -0.22, 0.085), "abyssal_obsidian", "OBSIDIAN_CHITIN", 0.44, height=0.58)
    for t in (0.25, 0.45):
        gx, gy = -0.72 + t * 0.90, 0.72 - t * 0.90
        c.zone(sdf_capsule(xx, yy, gx - 0.10, gy + 0.08, gx + 0.04, gy - 0.06, 0.03), "volcanic_ember", "PURIFICATION_GOLD", tone=0.65)
    head = sdf_onion(sdf_circle(xx, yy, 0.36, -0.36, 0.30), 0.065)
    _p(c, head, "chitin_bone", "CHITIN_BONE", 0.58, height=0.62)
    _p(c, sdf_circle(xx, yy, 0.36, -0.36, 0.17), "biolum_cyan", "BIOLUM_CYAN", 0.80 + 0.20 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.36, -0.36, 0.17) <= 0.0, "biolum_cyan")
    for ang in (0.0, 120.0, 240.0):
        a = np.deg2rad(ang)
        _p(c, sdf_capsule(xx, yy, 0.36 + np.cos(a) * 0.24, -0.36 + np.sin(a) * 0.24,
                          0.36 + np.cos(a) * 0.38, -0.36 + np.sin(a) * 0.38, 0.035), "chitin_bone", "CHITIN_BONE", 0.70)
    return c


def _tide_beacon(p: float) -> IconCanvas:
    c = IconCanvas(seed=130)
    xx, yy = c.xx, c.yy
    _p(c, sdf_box(xx, yy, 0.0, 0.52, 0.46, 0.13), "elite_carapace", "OBSIDIAN_CHITIN", 0.50, height=0.50)
    c.zone(sdf_box(xx, yy, 0.0, 0.52, 0.46, 0.03), "volcanic_ember", "PURIFICATION_GOLD", tone=0.72)
    _p(c, sdf_box(xx, yy, 0.0, 0.18, 0.16, 0.34), "chitin_bone", "CHITIN_BONE", 0.60, height=0.55)
    for px in (-0.30, 0.0, 0.30):
        _p(c, sdf_capsule(xx, yy, px * 0.5, 0.30, px, -0.22, 0.065), "chitin_bone", "CHITIN_BONE", 0.52)
    _p(c, sdf_diamond(xx, yy, 0.0, -0.36, 0.30), "predator_amethyst", "LIVING_AMETHYST", 0.75, height=0.80)
    c.zone(sdf_diamond(xx, yy, 0.0, -0.36, 0.12), "spore_chartreuse", "TOXIC_SPORE", tone=0.95 + 0.05 * p)
    c.glow(sdf_diamond(xx, yy, 0.0, -0.36, 0.12) <= 0.0, "spore_chartreuse")
    c.zone(np.abs(xx) < 0.03, "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.30)
    return c


def _hive_heart(p: float) -> IconCanvas:
    c = IconCanvas(seed=131)
    xx, yy = c.xx, c.yy
    lobes = np.minimum(sdf_circle(xx, yy, -0.20, -0.10, 0.32), sdf_circle(xx, yy, 0.20, -0.10, 0.32))
    body = sdf_polygon(xx, yy, 0.0, 0.18, 0.42, sides=4)
    heart = np.minimum(lobes, body)
    _p(c, heart, "leech_crimson", "BIOLUM_MYCELIUM", 0.58, height=0.80)
    c.zone(xx > 0.12, "elite_crimson", "BIOLUM_MYCELIUM", tone=0.38)
    _p(c, sdf_capsule(xx, yy, 0.06, -0.34, 0.16, -0.60, 0.075), "leech_crimson", "BIOLUM_MYCELIUM", 0.50)
    _p(c, sdf_capsule(xx, yy, -0.10, -0.34, -0.22, -0.56, 0.065), "leech_crimson", "BIOLUM_MYCELIUM", 0.50)
    for vx1, vy1, vx2, vy2 in ((-0.30, -0.20, -0.05, 0.30), (0.22, -0.24, 0.05, 0.34)):
        c.zone(np.abs(sdf_capsule(xx, yy, vx1, vy1, vx2, vy2, 0.02)) < 0.03, "mutant_amber", "BIOLUM_MYCELIUM", tone=0.85)
    _p(c, sdf_circle(xx, yy, 0.0, 0.02, 0.12), "warm_ivory", "BIOLUM_MYCELIUM", 0.95 + 0.05 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.0, 0.02, 0.12) <= 0.0, "warm_ivory")
    return c


def _chimera_ovum(p: float) -> IconCanvas:
    c = IconCanvas(seed=132)
    xx, yy = c.xx, c.yy
    ex = xx / 0.46
    ey = (yy - 0.06) / 0.60
    egg = np.sqrt(ex * ex + ey * ey) - 0.90
    _p(c, egg, "mycelial_violet", "BIOLUM_MYCELIUM", 0.52, height=0.74)
    c.zone(sdf_onion(egg, 0.055), "predator_amethyst", "LIVING_AMETHYST", tone=0.30)
    c.zone(xx > 0.12, "mycelial_violet", "BIOLUM_MYCELIUM", tone=0.34)
    c.zone(np.abs(sdf_capsule(xx, yy, -0.32, -0.12, -0.36, 0.22, 0.035)) < 0.030, "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.18)
    c.zone(np.abs(sdf_capsule(xx, yy, 0.22, 0.30, 0.30, 0.04, 0.030)) < 0.028, "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.22)
    spiral = sdf_bezier(xx, yy, (-0.16, 0.28), (0.26, 0.02), (-0.12, -0.26), 0.045)
    c.zone(spiral, "spore_chartreuse", "TOXIC_SPORE", tone=0.92)
    c.zone(np.abs(sdf_capsule(xx, yy, 0.16, 0.34, 0.02, 0.46, 0.018)) < 0.024, "venom_emerald", "TOXIC_SPORE", tone=0.30)
    _p(c, sdf_circle(xx, yy, 0.02, 0.10, 0.11), "warm_ivory", "BIOLUM_MYCELIUM", 0.92 + 0.08 * p, emissive=True)
    c.glow(sdf_circle(xx, yy, 0.02, 0.10, 0.11) <= 0.0, "warm_ivory")
    return c


def _volatile_engram(p: float) -> IconCanvas:
    c = IconCanvas(seed=133)
    xx, yy = c.xx, c.yy
    shard = sdf_diamond(xx * 1.35, yy * 0.78, 0.0, 0.02, 0.56)
    shard = sdf_subtract(shard, sdf_circle(xx, yy, 0.34, -0.56, 0.16))
    _p(c, shard, "predator_amethyst", "LIVING_AMETHYST", 0.58, height=0.82)
    c.zone(np.abs(xx + 0.12) < 0.05, "mycelial_violet", "LIVING_AMETHYST", tone=0.30)
    c.zone(np.abs(xx - 0.14) < 0.05, "mycelial_violet", "LIVING_AMETHYST", tone=0.78)
    rune = sdf_capsule(xx, yy, -0.16, 0.16, 0.16, -0.16, 0.03)
    c.zone(rune, "biolum_cyan", "BIOLUM_CYAN", tone=0.85 + 0.15 * p)
    c.glow(rune, "biolum_cyan")
    _p(c, sdf_diamond(xx, yy, 0.0, 0.0, 0.12), "spore_chartreuse", "TOXIC_SPORE", 0.95, emissive=True)
    c.glow(sdf_diamond(xx, yy, 0.0, 0.0, 0.12) <= 0.0, "spore_chartreuse")
    return c

def _truce_pheromone(p: float) -> IconCanvas:
    c = IconCanvas(seed=134)
    xx, yy = c.xx, c.yy
    bottle = sdf_polygon(xx, yy, 0.0, 0.16, 0.42, sides=6)
    _p(c, bottle, "warm_ivory", "LIVING_AMETHYST", 0.60, height=0.62)
    c.zone(sdf_polygon(xx, yy, 0.0, 0.20, 0.30, sides=6), "biolum_cyan", "BIOLUM_CYAN", tone=0.62)
    swirl = sdf_bezier(xx, yy, (-0.20, 0.34), (0.06, 0.12), (-0.04, 0.24), 0.035)
    c.zone(swirl, "warm_ivory", "BIOLUM_CYAN", tone=0.90 + 0.10 * p)
    c.glow(swirl, "warm_ivory")
    c.zone(sdf_box(xx, yy, 0.0, -0.22, 0.16, 0.10), "chitin_bone", "CHITIN_BONE", tone=0.62)
    _p(c, sdf_box(xx, yy, 0.0, -0.36, 0.20, 0.07), "volcanic_ember", "PURIFICATION_GOLD", 0.58)
    c.zone(sdf_capsule(xx, yy, -0.30, 0.06, -0.22, 0.30, 0.03), "warm_ivory", "LIVING_AMETHYST", tone=0.95)
    return c

def _quorum_seal(p: float) -> IconCanvas:
    c = IconCanvas(seed=135)
    xx, yy = c.xx, c.yy
    ca, sa = np.cos(np.pi / 6.0), np.sin(np.pi / 6.0)
    rx = ca * xx + sa * yy
    ry = -sa * xx + ca * yy
    seal = sdf_polygon(rx, ry, 0.0, 0.0, 0.62, sides=6)
    _p(c, seal, "imperial_brass", "PURIFICATION_GOLD", 0.55, height=0.62)
    c.zone(sdf_onion(seal, 0.055), "chitin_bone", "CHITIN_BONE", tone=0.72)
    c.zone(sdf_polygon(rx, ry, 0.0, 0.0, 0.34, sides=6), "chitin_bone", "CHITIN_BONE", tone=0.48)
    c.zone(sdf_diamond(xx, yy, 0.0, 0.0, 0.22), "spore_chartreuse", "TOXIC_SPORE", tone=0.85)
    c.glow(sdf_diamond(xx, yy, 0.0, 0.0, 0.14) <= 0.0, "spore_chartreuse")
    for ang in (0.0, 120.0, 240.0):
        a = np.deg2rad(ang)
        c.zone(sdf_capsule(xx, yy, np.cos(a) * 0.36, np.sin(a) * 0.36, np.cos(a) * 0.54, np.sin(a) * 0.54, 0.022),
               "imperial_brass", "PURIFICATION_GOLD", tone=0.82)
    return c

def _mycelial_codex(p: float) -> IconCanvas:
    c = IconCanvas(seed=136)
    xx, yy = c.xx, c.yy
    # Deep fungal cap & mycelial violet tome cover
    cover = sdf_box(xx, yy, 0.06, 0.04, 0.50, 0.62)
    _p(c, cover, "mycelial_violet", "OBSIDIAN_CHITIN", 0.44, height=0.64)
    # Recessed gill-textured inner membrane
    c.zone(sdf_box(xx, yy, 0.10, 0.04, 0.34, 0.46), "abyssal_indigo", "OBSIDIAN_CHITIN", tone=0.28)
    c.zone(sdf_onion(sdf_box(xx, yy, 0.10, 0.04, 0.36, 0.48), 0.03), "predator_amethyst", "LIVING_AMETHYST", tone=0.52)
    # Segmented fungal stipe & chitin-bone spine with chartreuse spore rings
    spine = sdf_box(xx, yy, -0.40, 0.04, 0.14, 0.64)
    _p(c, spine, "chitin_bone", "CHITIN_BONE", 0.60, height=0.72)
    for ry in (-0.42, -0.14, 0.14, 0.42):
        c.zone(sdf_box(xx, yy, -0.40, ry, 0.15, 0.04), "mycelial_violet", "LIVING_AMETHYST", tone=0.55)
    c.zone(sdf_box(xx, yy, -0.40, 0.04, 0.15, 0.05), "spore_chartreuse", "TOXIC_SPORE", tone=0.82)
    # Hyphal ivory parchment fore-edge with spore chartreuse striations
    page_edge = sdf_box(xx, yy, 0.50, 0.04, 0.08, 0.54)
    c.zone(page_edge, "warm_ivory", "CHITIN_BONE", tone=0.88)
    for py in (-0.28, -0.06, 0.16, 0.36):
        c.zone(sdf_box(xx, yy, 0.50, py, 0.07, 0.025), "spore_chartreuse", "TOXIC_SPORE", tone=0.72)
    # Chitin-bone corner mounts & bioluminescent cyan synaptic clasp
    for cy in (-0.52, 0.60):
        c.zone(sdf_box(xx, yy, 0.44, cy, 0.12, 0.06), "chitin_bone", "CHITIN_BONE", tone=0.76)
    _p(c, sdf_box(xx, yy, 0.58, 0.04, 0.06, 0.22), "chitin_bone", "CHITIN_BONE", 0.78, height=0.74)
    c.zone(sdf_box(xx, yy, 0.58, 0.04, 0.05, 0.06), "biolum_cyan", "BIOLUM_CYAN", tone=0.92)
    # Radial mycelial axons & corner synaptic nodes
    for ang in (40.0, 140.0, 220.0, 320.0):
        a = np.deg2rad(ang)
        tx = 0.08 + np.cos(a) * 0.32
        ty = 0.04 + np.sin(a) * 0.34
        c.zone(sdf_capsule(xx, yy, 0.08, 0.04, tx, ty, 0.028), "biolum_cyan", "BIOLUM_CYAN", tone=0.78 + 0.16 * p)
        c.zone(sdf_circle(xx, yy, tx, ty, 0.045) <= 0.0, "spore_chartreuse", "TOXIC_SPORE", tone=0.88)
    # Central Hive Mind Bioluminescent Mushroom Emblem
    stem = sdf_box(xx, yy, 0.08, 0.16, 0.065, 0.12)
    _p(c, stem, "warm_ivory", "CHITIN_BONE", 0.88, height=0.76)
    cap = sdf_subtract(sdf_circle(xx, yy * 1.25, 0.08, -0.02, 0.21), sdf_box(xx, yy, 0.08, 0.18, 0.30, 0.10))
    _p(c, cap, "spore_chartreuse", "TOXIC_SPORE", 0.85 + 0.12 * p, emissive=True, height=0.84)
    c.glow(cap <= 0.0, "spore_chartreuse")
    c.zone(sdf_circle(xx, yy, 0.03, -0.08, 0.045) <= 0.0, "biolum_cyan", "BIOLUM_CYAN", tone=0.95)
    c.zone(sdf_circle(xx, yy, 0.14, -0.06, 0.04) <= 0.0, "biolum_cyan", "BIOLUM_CYAN", tone=0.92)
    return c


_ICONS = {
    "bio_scanner": (_bio_scanner, True, 4),
    "mycelial_codex": (_mycelial_codex, False, 0),
    "enzyme_lance": (_enzyme_lance, True, 5),
    "neutralizing_spore_launcher": (_spore_launcher, True, 4),
    "symbiotic_blade": (_symbiotic_blade, True, 4),
    "spore_antidote": (_spore_antidote, True, 5),
    "purifier_grenade": (_purifier_grenade, True, 4),
    "tide_beacon": (_tide_beacon, False, 0),
    "bio_extractor": (_bio_extractor, False, 0),
    "chitin_helmet": (_chitin_helmet, False, 0),
    "chitin_chestplate": (_chitin_chestplate, False, 0),
    "chitin_leggings": (_chitin_leggings, False, 0),
    "chitin_boots": (_chitin_boots, False, 0),
    "chitin_plate": (_chitin_plate, False, 0),
    "concentrated_antidote": (_concentrated_antidote, False, 0),
    "counter_serum": (_counter_serum, False, 0),
    "genetic_sample": (_genetic_sample, False, 0),
    "symbiotic_mutagen": (_symbiotic_mutagen, False, 0),
    "hive_heart": (_hive_heart, True, 4),
    "latent_spore": (_latent_spore, False, 0),
    "mycelial_biomass": (_mycelial_biomass, False, 0),
    "overmind_essence": (_overmind_essence, False, 0),
    "rift_catalyst": (_rift_catalyst, False, 0),
    "synaptic_core": (_synaptic_core, False, 0),
    "overmind_crown": (_overmind_crown, False, 0),
    "purifier_drone_core": (_purifier_drone_core, False, 0),
    "synaptic_suppressor": (_synaptic_suppressor, True, 4),
    "neural_echo_core": (_neural_echo_core, False, 0),
    "synaptic_graft": (_synaptic_graft, False, 0),
    "neural_echo_decoy": (_neural_echo_decoy, False, 0),
    "schism_injector": (_schism_injector, False, 0),
    "truce_pheromone": (_truce_pheromone, False, 0),
    "quorum_seal": (_quorum_seal, False, 0),
    "chimera_ovum": (_chimera_ovum, False, 0),
    "volatile_engram": (_volatile_engram, False, 0),
    "false_engram_injector": (_false_engram_injector, False, 0),
}


def generate_items(item_dir: str, pbr_dir: str | None = None) -> None:
    os.makedirs(item_dir, exist_ok=True)
    for name, (builder, animated, frametime) in _ICONS.items():
        _emit(name, builder, item_dir, pbr_dir, animated=animated, frametime=frametime or 4)


# ---------------------------------------------------------------------------
# Mob effect HUD icons (18x18) keep the original painter for crisp status glyphs
# ---------------------------------------------------------------------------
def _blank_rgba(w=18, h=18) -> np.ndarray:
    return np.zeros((h, w, 4), dtype=np.uint8)


def _paint_sdf(canvas: np.ndarray, sdf: np.ndarray, ramp: np.ndarray, boost: float = 1.0) -> np.ndarray:
    lit = np.clip(compute_sdf_lighting_v2(sdf, specular_strength=0.40, sss_strength=0.20, ao_strength=0.28) * boost, 0.0, 1.0)
    col = bayer_quantize(lit, ramp, 0.22, matrix=8)
    return np.where((sdf <= 0.0)[..., None], col, canvas)


def generate_mob_effects(mob_effect_dir: str) -> None:
    """Generate dedicated 18x18 pixel-art HUD icons for all 4 Mycelial Mind MobEffects."""
    os.makedirs(mob_effect_dir, exist_ok=True)
    n = 18
    ex, ey = make_grid(n, n)

    inf_ring = sdf_circle(ex, ey, 0.0, 0.0, 0.62)
    inf_node_a = sdf_circle(ex, ey, -0.35, -0.25, 0.25)
    inf_node_b = sdf_circle(ex, ey, 0.35, 0.28, 0.22)
    inf_core = sdf_circle(ex, ey, 0.0, 0.0, 0.26)
    inf_img = _paint_sdf(_blank_rgba(), np.minimum(inf_ring, np.minimum(inf_node_a, inf_node_b)), PALETTES["mycelial_violet"], 0.95)
    inf_img = _paint_sdf(inf_img, inf_core, PALETTES["elite_crimson"], 1.1)
    save_rgba(add_pixel_outline(inf_img), os.path.join(mob_effect_dir, "mycelial_infection.png"))

    rot_drop = sdf_capsule(ex, ey, 0.0, -0.52, 0.0, 0.28, 0.36)
    rot_lobe = sdf_circle(ex, ey, 0.0, 0.24, 0.50)
    rot_core = sdf_diamond(ex, ey, 0.0, 0.16, 0.24)
    rot_img = _paint_sdf(_blank_rgba(), np.minimum(rot_drop, rot_lobe), PALETTES["spore_chartreuse"], 0.95)
    rot_img = _paint_sdf(rot_img, rot_core, PALETTES["leech_crimson"], 1.05)
    save_rgba(add_pixel_outline(rot_img), os.path.join(mob_effect_dir, "spore_rot.png"))

    ins_outer = sdf_diamond(ex, ey, 0.0, 0.0, 0.68)
    ins_mid = sdf_circle(ex, ey, 0.0, 0.0, 0.36)
    ins_pupil = sdf_box(ex, ey, 0.0, 0.0, 0.10, 0.26)
    ins_img = _paint_sdf(_blank_rgba(), ins_outer, PALETTES["biolum_cyan"], 1.05)
    ins_img = _paint_sdf(ins_img, ins_mid, PALETTES["predator_amethyst"], 1.0)
    ins_img = _paint_sdf(ins_img, ins_pupil, PALETTES["royal_gold"], 1.1)
    save_rgba(add_pixel_outline(ins_img), os.path.join(mob_effect_dir, "synaptic_insight.png"))

    vis_lens = sdf_circle(ex, ey, 0.0, 0.0, 0.64)
    vis_bar_h = sdf_box(ex, ey, 0.0, 0.0, 0.72, 0.09)
    vis_bar_v = sdf_box(ex, ey, 0.0, 0.0, 0.09, 0.72)
    vis_core = sdf_circle(ex, ey, 0.0, 0.0, 0.24)
    vis_img = _paint_sdf(_blank_rgba(), vis_lens, PALETTES["purification_aqua"], 0.98)
    vis_img = _paint_sdf(vis_img, np.minimum(vis_bar_h, vis_bar_v), PALETTES["chitin_bone"], 1.0)
    vis_img = _paint_sdf(vis_img, vis_core, PALETTES["mutant_amber"], 1.1)
    save_rgba(add_pixel_outline(vis_img), os.path.join(mob_effect_dir, "synaptic_vision.png"))