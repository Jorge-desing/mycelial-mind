"""Icon Language v2 - lighting-coherent, multi-material 16x16 icon compositor.

The previous generator painted every icon as a pile of independent SDF parts, each with its
own Blinn-Phong term. That produced "sticker" icons: parts did not share a light source and
the whole item had a single LabPBR material. This module gives the studio a coherent icon
language:

* one global light for the entire silhouette (top-left key light),
* per-pixel material zones (ramp + LabPBR material) composited into a single icon,
* selective 1px outline, upper-left rim light and lower-right core shadow,
* per-pixel LabPBR _n/_s maps derived from the accumulated height and material zones.

Everything is deterministic and pure NumPy so the unit tests can assert exact properties.
"""

from __future__ import annotations

import numpy as np

from .compositor import normals_to_labpbr
from .materials import MATERIALS
from .palette_oklab import PALETTES
from .sdf_engine import (
    BAYER_8X8,
    compute_sdf_ao,
    compute_sdf_curvature,
    compute_sdf_normals,
    make_grid,
    toroidal_value_noise,
)

RAMP_NAMES: tuple[str, ...] = tuple(PALETTES.keys())
MAT_NAMES: tuple[str, ...] = tuple(MATERIALS.keys())
_RAMPS = np.stack([PALETTES[name] for name in RAMP_NAMES]).astype(np.float32)

OUTLINE_RGB: tuple[int, int, int] = (14, 10, 18)

_MATERIAL_SMOOTH = np.array([MATERIALS[name].smoothness for name in MAT_NAMES], dtype=np.float32)
_MATERIAL_F0 = np.array([MATERIALS[name].f0 for name in MAT_NAMES], dtype=np.float32)
_MATERIAL_SSS = np.array([MATERIALS[name].porosity_sss for name in MAT_NAMES], dtype=np.float32)
_MATERIAL_EMIS = np.array([MATERIALS[name].emissive for name in MAT_NAMES], dtype=np.float32)
_MATERIAL_SUB = np.array([MATERIALS[name].is_subsurface for name in MAT_NAMES], dtype=bool)
_MATERIAL_METAL = np.array(
    [MATERIALS[name].metal_id if MATERIALS[name].metal_id is not None else 231 for name in MAT_NAMES],
    dtype=np.float32,
)


def _index(values: tuple[str, ...], value: str, kind: str) -> int:
    try:
        return values.index(value)
    except ValueError as exc:  # pragma: no cover - defensive, surfaces typos in generators
        raise KeyError(f"Unknown {kind} '{value}'. Known: {', '.join(values)}") from exc


def _tile_bayer(height: int, width: int) -> np.ndarray:
    rows = BAYER_8X8.shape[0]
    cols = BAYER_8X8.shape[1]
    return np.tile(BAYER_8X8, (height // rows + 1, width // cols + 1))[:height, :width]


def _grow(mask: np.ndarray) -> np.ndarray:
    padded = np.pad(mask, 1, mode="constant", constant_values=False)
    return (
        padded[:-2, 1:-1]
        | padded[2:, 1:-1]
        | padded[1:-1, :-2]
        | padded[1:-1, 2:]
    )


def _shift(mask: np.ndarray, dy: int, dx: int) -> np.ndarray:
    out = np.zeros_like(mask)
    h, w = mask.shape
    src_y = slice(max(0, -dy), h - max(0, dy))
    dst_y = slice(max(0, dy), h - max(0, -dy))
    src_x = slice(max(0, -dx), w - max(0, dx))
    dst_x = slice(max(0, dx), w - max(0, -dx))
    out[dst_y, dst_x] = mask[src_y, src_x]
    return out


def _region(field: np.ndarray) -> np.ndarray:
    """Accept either an SDF (inside is <= 0) or a boolean mask."""
    if field.dtype == bool:
        return field
    return field <= 0.0


def rot_box(xx, yy, cx: float, cy: float, bx: float, by: float, angle: float):
    """Rotated rounded box SDF (angle in radians) for tilted plates and blades."""
    ca, sa = float(np.cos(angle)), float(np.sin(angle))
    dx, dy = xx - cx, yy - cy
    rx = ca * dx + sa * dy
    ry = -sa * dx + ca * dy
    ax = np.abs(rx) - bx
    ay = np.abs(ry) - by
    return np.sqrt(np.maximum(ax, 0.0) ** 2 + np.maximum(ay, 0.0) ** 2) + np.minimum(np.maximum(ax, ay), 0.0)


def rot_diamond(xx, yy, cx: float, cy: float, r: float, angle: float):
    ca, sa = float(np.cos(angle)), float(np.sin(angle))
    dx, dy = xx - cx, yy - cy
    rx = np.abs(ca * dx + sa * dy)
    ry = np.abs(-sa * dx + ca * dy)
    return (rx + ry) - r


def tapered_blade(xx, yy, ax: float, ay: float, bx: float, by: float, width: float, tip: float = 0.10):
    """Capsule blade whose far end tapers into a point (tip radius shrinks to 0)."""
    pax, pay = xx - ax, yy - ay
    bax, bay = bx - ax, by - ay
    h = np.clip((pax * bax + pay * bay) / max(1e-6, bax * bax + bay * bay), 0.0, 1.0)
    r = width * (1.0 - 0.85 * h) + tip * 0.0
    return np.sqrt((pax - bax * h) ** 2 + (pay - bay * h) ** 2) - r


class IconCanvas:
    """Accumulates SDF parts into a single 16x16 icon with coherent lighting and LabPBR zones."""

    def __init__(self, size: int = 16, seed: int = 0):
        self.size = size
        self.xx, self.yy = make_grid(size, size)
        self.mask = np.zeros((size, size), dtype=bool)
        self.union = np.full((size, size), 1.0, dtype=np.float32)
        self.ramp = np.zeros((size, size), dtype=np.int16)
        self.material = np.zeros((size, size), dtype=np.int16)
        self.tone = np.full((size, size), 0.55, dtype=np.float32)
        self.height = np.zeros((size, size), dtype=np.float32)
        self.emissive = np.zeros((size, size), dtype=bool)
        self.grain = toroidal_value_noise(size, size, 4, 4, seed=seed).astype(np.float32)

    # -- painting -----------------------------------------------------------
    def paint(
        self,
        sdf: np.ndarray,
        ramp: str,
        material: str = "CHITIN_BONE",
        *,
        tone: float = 0.55,
        height: float = 0.55,
        emissive: bool = False,
        bevel: float = 0.10,
        grain: float = 0.12,
        overwrite: bool = True,
    ) -> "IconCanvas":
        region = sdf <= 0.0
        if not overwrite:
            region = region & ~self.mask
        if not np.any(region):
            return self
        taper = bevel * compute_sdf_curvature(sdf) + grain * (self.grain - 0.5)
        self.union = np.where(region, np.minimum(self.union, sdf), self.union)
        self.mask |= region
        self.ramp = np.where(region, _index(RAMP_NAMES, ramp, "ramp"), self.ramp)
        self.material = np.where(region, _index(MAT_NAMES, material, "material"), self.material)
        self.tone = np.where(region, np.clip(tone + taper, 0.02, 1.0), self.tone)
        self.height = np.where(region, height, self.height)
        self.emissive = np.where(region, emissive, self.emissive)
        return self

    def carve(self, sdf: np.ndarray, amount: float = 0.34) -> "IconCanvas":
        """Darken already-painted texels where the carve SDF is inside (grooves, seams, gills)."""
        region = _region(sdf) & self.mask
        self.tone = np.where(region, np.clip(self.tone - amount, 0.02, 1.0), self.tone)
        return self

    def emboss(self, mask: np.ndarray, amount: float = 0.28) -> "IconCanvas":
        """Brighten already-painted texels (ridges, highlights, rivets)."""
        region = _region(mask) & self.mask
        self.tone = np.where(region, np.clip(self.tone + amount, 0.02, 1.0), self.tone)
        return self

    def zone(
        self,
        sdf: np.ndarray,
        ramp: str,
        material: str | None = None,
        tone: float = 0.40,
        bevel: float = 0.0,
    ) -> "IconCanvas":
        """Repaint already-masked texels with a different ramp/material (grooves, facets, plates)."""
        region = _region(sdf) & self.mask
        if not np.any(region):
            return self
        taper = 0.0 if sdf.dtype == bool or bevel == 0.0 else bevel * compute_sdf_curvature(sdf.astype(np.float32))
        self.ramp = np.where(region, _index(RAMP_NAMES, ramp, "ramp"), self.ramp)
        if material is not None:
            self.material = np.where(region, _index(MAT_NAMES, material, "material"), self.material)
        self.tone = np.where(region, np.clip(tone + taper, 0.02, 1.0), self.tone)
        return self

    def glow(self, mask: np.ndarray, ramp: str | None = None) -> "IconCanvas":
        region = _region(mask) & self.mask
        self.emissive = np.where(region, True, self.emissive)
        if ramp is not None:
            self.ramp = np.where(region, _index(RAMP_NAMES, ramp, "ramp"), self.ramp)
        return self

    # -- output -------------------------------------------------------------
    def finish(self, dither: float = 0.0) -> np.ndarray:
        """Composite the icon as flat pixel-art bands: tone -> ramp index, plus edge light/shadow."""
        if not np.any(self.mask):
            return np.zeros((self.size, self.size, 4), dtype=np.uint8)
        index = np.clip(np.round(1.0 + self.tone * 5.0).astype(np.int32), 1, 6)
        rim = self.mask & (~_shift(self.mask, 1, 0) | ~_shift(self.mask, 0, 1))
        core = self.mask & (~_shift(self.mask, -1, 0) | ~_shift(self.mask, 0, -1))
        index = np.where(rim, 7, index)
        index = np.where(core & ~rim, 1, index)
        rgb = _RAMPS[self.ramp, index, :3].astype(np.float32) / 255.0

        if dither > 0.0:
            field = np.clip(index / 7.0 + (_tile_bayer(self.size, self.size) - 0.5) * dither, 0.0, 1.0)
            rgb = _RAMPS[self.ramp, np.clip((field * 8).astype(np.int32), 0, 7), :3].astype(np.float32) / 255.0

        glow_rgb = _RAMPS[self.ramp, 7, :3].astype(np.float32) / 255.0
        rgb = np.where(self.emissive[..., None], rgb * 0.35 + glow_rgb * 0.65, rgb)
        rgb = np.clip(rgb, 0.0, 1.0)

        out = np.zeros((self.size, self.size, 4), dtype=np.uint8)
        out[..., :3] = np.clip(rgb * 255.0, 0, 255).astype(np.uint8)
        out[..., 3] = np.where(self.mask, 255, 0)
        outline = _grow(self.mask) & ~self.mask
        out[outline] = np.array([*OUTLINE_RGB, 255], dtype=np.uint8)
        return out

    def pbr_maps(self) -> tuple[np.ndarray, np.ndarray]:
        nx, ny, nz = compute_sdf_normals(self.union)
        ao = compute_sdf_ao(self.union)
        height = np.clip(0.18 + 0.80 * self.height, 0.16, 1.0).astype(np.float32)
        normal = normals_to_labpbr(nx, ny, nz, ao, height_map=height)

        mat = self.material
        smooth = _MATERIAL_SMOOTH[mat]
        f0 = _MATERIAL_F0[mat]
        sss = _MATERIAL_SSS[mat]
        metal = _MATERIAL_METAL[mat]
        emissive = _MATERIAL_EMIS[mat]

        r = np.clip(smooth * 255.0, 0, 255).astype(np.uint8)
        g = np.where(f0 >= 0.90, metal, np.clip(f0 * 255.0, 0, 229)).astype(np.uint8)
        b_sub = np.where(sss > 0.01, np.clip(65.0 + sss * 190.0, 65, 255), 0.0)
        b_die = np.clip(sss * 64.0, 0, 64)
        b = np.where(_MATERIAL_SUB[mat], b_sub, b_die).astype(np.uint8)
        a_val = np.clip(np.maximum(emissive, 0.70) * 254.0, 1, 254)
        a = np.where(self.emissive, a_val, 255.0).astype(np.uint8)
        spec = np.stack([r, g, b, a], axis=-1)
        return normal, spec


def finalize_icon(canvas: IconCanvas, dither: float = 0.11) -> np.ndarray:
    return canvas.finish(dither=dither)