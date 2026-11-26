"""Procedural sprite painters for the Mycelial Mind particle families.

The painters build silhouettes from signed distance fields, shade them with the
shared texture-studio lighting model, quantize onto hue-shifted Oklab ramps with
Bayer dithering and finally composite a bright emissive core so every particle
stays readable in grayscale. Frames are plain RGBA arrays; the studio writes one
PNG per frame and a ``particles/<id>.json`` list in frame order.
"""

from __future__ import annotations

from typing import List

import numpy as np

from scripts.texture_studio.palette_oklab import PALETTES
from scripts.texture_studio.sdf_engine import (
    bayer_quantize,
    compute_sdf_lighting_v2,
    make_grid,
    sdf_bezier,
    sdf_capsule,
    sdf_circle,
    sdf_diamond,
    sdf_polygon,
    sdf_subtract,
    sdf_union,
    toroidal_value_fbm,
)

from .catalog import ParticleSpec

_TWO_PI = 2.0 * np.pi


def _rot(xx: np.ndarray, yy: np.ndarray, angle: float):
    c, s = float(np.cos(angle)), float(np.sin(angle))
    return xx * c + yy * s, -xx * s + yy * c


def _blank(size: int) -> np.ndarray:
    return np.zeros((size, size, 4), dtype=np.uint8)


def _halo(sdf: np.ndarray, thickness: float, samples: int = 3) -> np.ndarray:
    d = np.abs(sdf) - thickness
    for i in range(1, samples):
        d = np.minimum(d, np.abs(sdf) - thickness * (1.0 + i * 1.35))
    return d


def _over(
    rgba: np.ndarray,
    sdf: np.ndarray,
    palette: str,
    size: int,
    boost: float = 1.0,
    alpha: float = 1.0,
    dither: float = 0.16,
    aa: float = 0.62,
) -> np.ndarray:
    """Alpha-composite one shaded SDF layer over the current sprite."""
    lit = compute_sdf_lighting_v2(sdf, specular_strength=0.55, sss_strength=0.25, ao_strength=0.30)
    lit = np.clip(lit * boost * 1.22 + 0.10, 0.0, 1.0)
    col = bayer_quantize(lit, PALETTES[palette], dither, matrix=8).astype(np.float32)[..., :3] / 255.0
    cov = np.clip(0.5 - sdf * (size * aa * 0.5), 0.0, 1.0)
    a = np.clip(cov * alpha, 0.0, 1.0)
    a = np.round(a * 8.0) / 8.0
    dst_a = rgba[..., 3].astype(np.float32) / 255.0
    out_a = a + dst_a * (1.0 - a)
    safe = np.where(out_a > 1e-5, out_a, 1.0)
    dst_rgb = rgba[..., :3].astype(np.float32) / 255.0
    out_rgb = (col * a[..., None] + dst_rgb * dst_a[..., None] * (1.0 - a[..., None])) / safe[..., None]
    rgba[..., :3] = np.clip(out_rgb * 255.0, 0.0, 255.0).astype(np.uint8)
    rgba[..., 3] = np.clip(out_a * 255.0, 0.0, 255.0).astype(np.uint8)
    return rgba


def _paint_field(
    rgba: np.ndarray,
    mask: np.ndarray,
    field: np.ndarray,
    palette: str,
    size: int,
    alpha: float = 1.0,
    dither: float = 0.20,
) -> np.ndarray:
    lit = np.clip(field * 0.55 + 0.42, 0.0, 1.0)
    col = bayer_quantize(lit, PALETTES[palette], dither, matrix=8).astype(np.float32)[..., :3] / 255.0
    cov = np.clip(0.5 - mask * (size * 0.30), 0.0, 1.0)
    a = np.clip(cov * field * alpha, 0.0, 1.0)
    a = np.round(a * 8.0) / 8.0
    dst_a = rgba[..., 3].astype(np.float32) / 255.0
    out_a = a + dst_a * (1.0 - a)
    safe = np.where(out_a > 1e-5, out_a, 1.0)
    dst_rgb = rgba[..., :3].astype(np.float32) / 255.0
    out_rgb = (col * a[..., None] + dst_rgb * dst_a[..., None] * (1.0 - a[..., None])) / safe[..., None]
    rgba[..., :3] = np.clip(out_rgb * 255.0, 0.0, 255.0).astype(np.uint8)
    rgba[..., 3] = np.clip(out_a * 255.0, 0.0, 255.0).astype(np.uint8)
    return rgba


def _seed(spec: ParticleSpec) -> float:
    return float(spec.params.get("seed", 7))


# ----------------------------------------------------------------------
# Shape recipes
# ----------------------------------------------------------------------
def _shape_spore(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    squash = float(p.get("squash", 1.0))
    lobes = max(1, int(p.get("lobes", 2)))
    lobe_sdf = None
    for i in range(lobes):
        a = i * (_TWO_PI / lobes) + _seed(s) * 0.013 + t * 0.35
        cx, cy = float(np.cos(a)) * 0.20, float(np.sin(a)) * 0.16
        lob = sdf_circle(xx, yy / squash, cx, cy, 0.30 - i * 0.028)
        lobe_sdf = lob if lobe_sdf is None else sdf_union(lobe_sdf, lob)
    body = sdf_union(lobe_sdf, sdf_circle(xx, yy, 0.0, 0.04, 0.33))
    for i in range(int(p.get("cilia", 0))):
        a = i * (_TWO_PI / max(1, int(p.get("cilia", 1)))) + t * 0.55
        x0, y0 = float(np.cos(a)) * 0.28, float(np.sin(a)) * 0.24
        body = sdf_union(body, sdf_bezier(
            xx, yy,
            (x0, y0),
            (float(np.cos(a + 0.45)) * 0.46, float(np.sin(a + 0.45)) * 0.42),
            (float(np.cos(a + 0.85)) * 0.62, float(np.sin(a + 0.85)) * 0.56),
            0.030,
        ))
    if p.get("fin"):
        body = sdf_union(body, sdf_polygon(xx, yy, 0.0, -0.44, 0.20, 3))
    if p.get("wake"):
        body = sdf_union(body, sdf_capsule(xx, yy, 0.0, -0.46, 0.0, -0.78 - 0.08 * t, 0.026))
    _over(rgba, _halo(body, 0.10, 2), s.accent, s.size, boost=0.55, alpha=0.24)
    _over(rgba, body, s.palette, s.size, boost=1.02, alpha=0.96)
    core = sdf_circle(xx, yy, 0.03, -0.02, float(p.get("nucleus", 0.22)) * (1.0 + 0.20 * float(np.sin(t * _TWO_PI))))
    _over(rgba, core, "warm_ivory", s.size, boost=1.55, alpha=1.0)
    _over(rgba, sdf_circle(xx, yy, 0.03, -0.02, 0.075), s.accent, s.size, boost=1.6, alpha=0.95)
    if p.get("star"):
        for a in (0.0, np.pi / 2):
            _over(rgba, sdf_capsule(
                xx, yy, -0.52 * float(np.cos(a)), -0.52 * float(np.sin(a)),
                0.52 * float(np.cos(a)), 0.52 * float(np.sin(a)), 0.030,
            ), "warm_ivory", s.size, boost=1.45, alpha=0.85)
    if p.get("halo"):
        spin = t * _TWO_PI
        _over(rgba, sdf_capsule(
            xx, yy,
            float(np.cos(spin)) * 0.46, float(np.sin(spin)) * 0.46,
            float(np.cos(spin + np.pi)) * 0.46, float(np.sin(spin + np.pi)) * 0.46, 0.024,
        ), s.accent, s.size, boost=1.2, alpha=0.55)
        for k in range(2):
            a = spin + k * np.pi + 0.7
            _over(rgba, sdf_circle(xx, yy, float(np.cos(a)) * 0.53, float(np.sin(a)) * 0.53, 0.055),
                  "warm_ivory", s.size, boost=1.4, alpha=0.7)


def _shape_teardrop(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    sway = 0.025 * float(np.sin(t * _TWO_PI))
    tail = float(p.get("tail", 0.66))
    drop = sdf_union(
        sdf_circle(xx, yy, sway, 0.24, 0.42),
        sdf_capsule(xx, yy, 0.0, -tail - 0.14 * t, sway, 0.14, 0.14),
    )
    beads = int(p.get("beads", 0))
    if beads:
        drop = sdf_union(drop, sdf_circle(xx, yy, 0.24, -0.30, 0.085))
        drop = sdf_union(drop, sdf_circle(xx, yy, -0.20, -0.44 - 0.05 * t, 0.062))
    _over(rgba, _halo(drop, 0.09, 2), s.accent, s.size, boost=0.55, alpha=0.22)
    _over(rgba, drop, s.palette, s.size, boost=1.0, alpha=0.95)
    for i in range(int(p.get("fissures", 0))):
        x0 = -0.20 + 0.14 * i
        _over(rgba, sdf_capsule(xx, yy, x0, 0.34, x0 + 0.10 * (i - 1), -0.18, 0.030),
              s.accent, s.size, boost=1.35, alpha=0.85)
    _over(rgba, sdf_circle(xx, yy, -0.13, 0.32, 0.13), "warm_ivory", s.size, boost=1.6, alpha=0.9)
    _over(rgba, sdf_circle(xx, yy, 0.16, 0.10, 0.055), s.accent, s.size, boost=1.5, alpha=0.8)


def _shape_crystal(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    ang = 0.18 + t * 0.22
    xr, yr = _rot(xx, yy, ang)
    sides = max(3, int(p.get("points", 4)))
    body = sdf_polygon(xr, yr, 0.0, 0.0, 0.72, sides)
    inner = sdf_polygon(xr, yr, 0.0, 0.0, 0.36, sides)
    _over(rgba, _halo(body, 0.09, 2), s.accent, s.size, boost=0.55, alpha=0.24)
    _over(rgba, body, s.palette, s.size, boost=1.0, alpha=0.95)
    _over(rgba, inner, s.accent, s.size, boost=1.25, alpha=0.55)
    if p.get("rune"):
        _over(rgba, sdf_capsule(xr, yr, -0.28, -0.10, 0.28, 0.12, 0.035), "warm_ivory", s.size, boost=1.4, alpha=0.9)
        _over(rgba, sdf_capsule(xr, yr, -0.20, 0.26, 0.16, -0.24, 0.028), s.accent, s.size, boost=1.2, alpha=0.8)
    if p.get("fracture"):
        for i in range(3):
            a = 0.5 + i * 0.9
            _over(rgba, sdf_capsule(
                xr, yr, 0.0, 0.0,
                float(np.cos(a)) * 0.55, float(np.sin(a)) * 0.55, 0.026,
            ), "basalt_slate", s.size, boost=0.7, alpha=0.85)
    _over(rgba, sdf_circle(xr, yr, -0.22, -0.26, 0.075), "warm_ivory", s.size, boost=1.7, alpha=0.95)


def _shape_reticle(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    ring = np.abs(np.sqrt(xx * xx + yy * yy) - 0.64) - 0.045
    _over(rgba, _halo(ring, 0.035, 2), s.palette, s.size, boost=1.0, alpha=0.35)
    _over(rgba, ring, s.palette, s.size, boost=1.35, alpha=0.95)
    ticks = int(p.get("ticks", 4))
    for i in range(ticks):
        a = i * (_TWO_PI / ticks) + t * 0.45
        _over(rgba, sdf_capsule(
            xx, yy, float(np.cos(a)) * 0.54, float(np.sin(a)) * 0.54,
            float(np.cos(a)) * 0.76, float(np.sin(a)) * 0.76, 0.038,
        ), s.accent, s.size, boost=1.4, alpha=0.9)
    if p.get("cross"):
        _over(rgba, sdf_capsule(xx, yy, -0.34, 0.0, 0.34, 0.0, 0.030), "warm_ivory", s.size, boost=1.5, alpha=0.85)
        _over(rgba, sdf_capsule(xx, yy, 0.0, -0.34, 0.0, 0.34, 0.030), "warm_ivory", s.size, boost=1.5, alpha=0.85)
    _over(rgba, np.abs(np.sqrt(xx * xx + yy * yy) - 0.30) - 0.026, s.accent, s.size, boost=1.2, alpha=0.65)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.15), "warm_ivory", s.size, boost=1.6, alpha=0.95)
    _over(rgba, sdf_diamond(xx, yy, 0.0, 0.0, 0.075), s.accent, s.size, boost=1.7, alpha=1.0)


def _shape_arc(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    crescent = sdf_subtract(sdf_circle(xx, yy, 0.0, 0.0, 0.60), sdf_circle(xx, yy, 0.26, 0.10, 0.54))
    _over(rgba, _halo(crescent, 0.07, 2), s.palette, s.size, boost=0.8, alpha=0.30)
    _over(rgba, crescent, s.palette, s.size, boost=1.05, alpha=0.95)
    _over(rgba, _halo(crescent, 0.022, 1), "warm_ivory", s.size, boost=1.6, alpha=0.75)
    for k in range(int(s.params.get("satellites", 0))):
        a = 0.9 + k * 0.9 + t * 0.8
        _over(rgba, sdf_circle(xx, yy, float(np.cos(a)) * 0.55, float(np.sin(a)) * 0.55, 0.07),
              s.accent, s.size, boost=1.5, alpha=0.9)


def _shape_cross(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    vertical = sdf_capsule(xx, yy, 0.0, -0.54, 0.0, 0.54, 0.105)
    horizontal = sdf_capsule(xx, yy, -0.54, 0.0, 0.54, 0.0, 0.105)
    body = sdf_union(vertical, horizontal)
    if p.get("hexagon"):
        body = sdf_union(body, np.abs(sdf_polygon(xx, yy, 0.0, 0.0, 0.34, 6)) - 0.030)
    _over(rgba, _halo(body, 0.075, 2), s.accent, s.size, boost=0.7, alpha=0.28)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.95)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.16), s.accent, s.size, boost=1.6, alpha=0.95)
    _over(rgba, sdf_diamond(xx, yy, 0.0, 0.0, 0.075), "warm_ivory", s.size, boost=1.7, alpha=1.0)
    for a in (0.0, np.pi / 2, np.pi, 3 * np.pi / 2):
        _over(rgba, sdf_circle(
            xx, yy, float(np.cos(a + t * 0.6)) * 0.62, float(np.sin(a + t * 0.6)) * 0.62, 0.045,
        ), "warm_ivory", s.size, boost=1.4, alpha=0.7)


def _shape_chevron(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    tip = -0.36 - 0.10 * t
    left = sdf_capsule(xx, yy, -0.58, 0.26 + 0.08 * t, 0.0, tip, 0.085)
    right = sdf_capsule(xx, yy, 0.58, 0.26 + 0.08 * t, 0.0, tip, 0.085)
    body = sdf_union(left, right)
    _over(rgba, _halo(body, 0.06, 2), s.accent, s.size, boost=0.8, alpha=0.30)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.95)
    for i in range(int(s.params.get("tails", 0))):
        off = 0.14 + i * 0.12 + 0.16 * t
        _over(rgba, sdf_capsule(xx, yy, -0.10 - off * 0.3, 0.42 + off, 0.12 + off * 0.3, 0.10 + off, 0.030),
              s.accent, s.size, boost=1.2, alpha=0.55)
    _over(rgba, sdf_capsule(xx, yy, -0.06, tip + 0.06, 0.06, tip + 0.06, 0.045),
          "warm_ivory", s.size, boost=1.6, alpha=0.9)
    for i in range(3):
        _over(rgba, sdf_circle(xx, yy, 0.0, 0.34 + i * 0.16 + 0.12 * t, 0.035 - i * 0.006),
              "warm_ivory", s.size, boost=1.3, alpha=0.55 - i * 0.12)


def _shape_slash(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    ang = -0.7 + t * 0.34
    xr, yr = _rot(xx, yy, ang)
    body = sdf_capsule(xr, yr, -0.62, 0.10, 0.62, -0.10, 0.075)
    edge = sdf_capsule(xr, yr, -0.52, 0.24, 0.56, 0.02, 0.030)
    _over(rgba, _halo(body, 0.06, 2), s.palette, s.size, boost=0.7, alpha=0.30)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.95)
    _over(rgba, edge, "warm_ivory", s.size, boost=1.6, alpha=0.85)
    for i in range(3):
        _over(rgba, sdf_circle(xr, yr, 0.42 + i * 0.09, -0.30 - i * 0.08, 0.040),
              "warm_ivory", s.size, boost=1.4, alpha=0.7)


def _shape_dart(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    ang = 0.1 + t * 0.15
    xr, yr = _rot(xx, yy, ang)
    body = sdf_union(
        sdf_polygon(xr, yr, 0.0, 0.10, 0.46, 4),
        sdf_capsule(xr, yr, 0.0, 0.20, 0.0, -0.52, 0.115),
    )
    _over(rgba, _halo(body, 0.07, 2), s.palette, s.size, boost=0.7, alpha=0.30)
    _over(rgba, body, s.palette, s.size, boost=1.08, alpha=0.95)
    fin_l = sdf_capsule(xr, yr, -0.30, 0.30, -0.52, 0.54, 0.028)
    fin_r = sdf_capsule(xr, yr, 0.30, 0.30, 0.52, 0.54, 0.028)
    _over(rgba, sdf_union(fin_l, fin_r), s.accent, s.size, boost=1.35, alpha=0.9)
    _over(rgba, sdf_circle(xr, yr, 0.0, -0.44, 0.10), "warm_ivory", s.size, boost=1.7, alpha=1.0)


def _shape_ember(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    seed = _seed(s)
    ox = 0.06 * float(np.sin(seed * 0.71))
    oy = 0.05 * float(np.cos(seed * 0.93))
    crust_x = -0.10 + 0.16 * float(np.sin(seed * 1.31))
    body = sdf_union(
        sdf_circle(xx, yy, ox, 0.08 + oy, 0.34),
        sdf_union(sdf_circle(xx, yy, ox + 0.20, -0.16 + oy, 0.21),
                  sdf_circle(xx, yy, ox - 0.18, -0.12 + oy, 0.19)),
    )
    body = sdf_subtract(body, sdf_circle(xx, yy, crust_x, 0.20, 0.10))
    _over(rgba, _halo(body, 0.08, 2), s.palette, s.size, boost=0.7, alpha=0.26)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.95)
    _over(rgba, sdf_circle(xx, yy, ox + 0.02, 0.02 + oy, 0.175), "warm_ivory", s.size, boost=1.65, alpha=0.95)
    # Fungal smoke wisp rising with the frame.
    sway = 0.10 * float(np.sin(t * _TWO_PI + 0.6))
    _over(rgba, sdf_bezier(xx, yy, (ox + 0.02, 0.28), (ox + 0.22 + sway, 0.48), (ox - 0.10 + sway, 0.72), 0.030),
          s.accent, s.size, boost=0.9, alpha=0.55)
    _over(rgba, sdf_circle(xx, yy, ox + 0.30, -0.30 + oy, 0.035), s.accent, s.size, boost=1.5, alpha=0.85)


def _shape_wave_crystal(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    xr, yr = _rot(xx, yy, 0.4 + t * 0.3)
    body = sdf_polygon(xr, yr, 0.0, 0.0, 0.62, 4)
    _over(rgba, _halo(body, 0.08, 2), "spectral_bone", s.size, boost=0.7, alpha=0.26)
    _over(rgba, body, s.palette, s.size, boost=1.05, alpha=0.92)
    for i in range(4):
        a = 0.78 * i + 0.2
        _over(rgba, sdf_capsule(xr, yr, 0.0, 0.0, float(np.cos(a)) * 0.56, float(np.sin(a)) * 0.56, 0.026),
              "spectral_bone", s.size, boost=1.3, alpha=0.75)
    _over(rgba, np.abs(np.sqrt(xr * xr + yr * yr) - 0.30) - 0.025, "warm_ivory", s.size, boost=1.4, alpha=0.8)
    _over(rgba, sdf_circle(xr, yr, 0.0, 0.0, 0.12), "warm_ivory", s.size, boost=1.6, alpha=0.95)


def _shape_rune(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    ang = t * 0.8
    xr, yr = _rot(xx, yy, ang)
    sides = max(3, int(p.get("points", 5)))
    ring = np.abs(sdf_polygon(xr, yr, 0.0, 0.0, 0.60, sides)) - 0.032
    _over(rgba, _halo(ring, 0.04, 2), s.palette, s.size, boost=0.9, alpha=0.30)
    _over(rgba, ring, s.palette, s.size, boost=1.35, alpha=0.92)
    _over(rgba, sdf_capsule(xr, yr, -0.26, 0.0, 0.26, 0.0, 0.030), "warm_ivory", s.size, boost=1.5, alpha=0.85)
    _over(rgba, sdf_capsule(xr, yr, 0.0, -0.28, 0.0, 0.28, 0.030), s.accent, s.size, boost=1.35, alpha=0.85)
    _over(rgba, sdf_circle(xr, yr, 0.0, 0.0, 0.11), "warm_ivory", s.size, boost=1.65, alpha=0.95)
    orbit = t * _TWO_PI + 0.7
    _over(rgba, sdf_circle(xx, yy, float(np.cos(orbit)) * 0.66, float(np.sin(orbit)) * 0.66, 0.055),
          "warm_ivory", s.size, boost=1.6, alpha=0.85)
    if p.get("glitch"):
        for i in range(3):
            off = (i - 1) * 0.16
            _over(rgba, sdf_capsule(xx + off, yy, -0.34, 0.34 + off * 0.4, 0.34, 0.24 + off * 0.4, 0.022),
                  s.accent if i % 2 == 0 else "biolum_cyan", s.size, boost=1.4, alpha=0.6)


def _shape_star(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    spokes = int(s.params.get("points", 8))
    body = sdf_circle(xx, yy, 0.0, 0.0, 0.14)
    length = 0.46 + 0.22 * t
    for i in range(spokes // 2):
        a = i * (np.pi / max(1, spokes // 2))
        dx, dy = float(np.cos(a)) * length, float(np.sin(a)) * length
        body = sdf_union(body, sdf_capsule(xx, yy, -dx, -dy, dx, dy, 0.042))
    _over(rgba, _halo(body, 0.09, 2), s.palette, s.size, boost=0.7, alpha=0.28)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.92)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.155), "warm_ivory", s.size, boost=1.6, alpha=0.95)
    _over(rgba, sdf_diamond(xx, yy, 0.0, 0.0, 0.085), s.accent, s.size, boost=1.7, alpha=1.0)


def _shape_hex_glyph(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    ang = t * 0.55
    xr, yr = _rot(xx, yy, ang)
    ring = np.abs(sdf_polygon(xr, yr, 0.0, 0.0, 0.60, 6)) - 0.035
    _over(rgba, _halo(ring, 0.05, 2), s.palette, s.size, boost=0.9, alpha=0.30)
    _over(rgba, ring, s.palette, s.size, boost=1.4, alpha=0.95)
    _over(rgba, sdf_polygon(xr, yr, 0.0, 0.0, 0.42, 6), s.accent, s.size, boost=1.15, alpha=0.45)
    # Inner circuit traces.
    _over(rgba, sdf_capsule(xr, yr, -0.28, -0.16, 0.28, -0.16, 0.026), "warm_ivory", s.size, boost=1.5, alpha=0.85)
    _over(rgba, sdf_capsule(xr, yr, -0.10, -0.16, -0.10, 0.18, 0.026), s.accent, s.size, boost=1.4, alpha=0.85)
    _over(rgba, sdf_capsule(xr, yr, 0.18, -0.16, 0.18, 0.10, 0.026), s.accent, s.size, boost=1.4, alpha=0.85)
    _over(rgba, sdf_circle(xr, yr, -0.10, 0.18, 0.055), "warm_ivory", s.size, boost=1.6, alpha=0.95)
    pins = int(s.params.get("pins", 0))
    for i in range(pins):
        a = i * (_TWO_PI / max(1, pins)) + 0.5
        _over(rgba, sdf_capsule(
            xr, yr, float(np.cos(a)) * 0.60, float(np.sin(a)) * 0.60,
            float(np.cos(a)) * 0.78, float(np.sin(a)) * 0.78, 0.030,
        ), "warm_ivory", s.size, boost=1.5, alpha=0.85)


def _shape_ring(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    radius = 0.26 + 0.60 * t
    wobble = 0.0
    if p.get("wobble"):
        wobble = float(p.get("wobble_amp", 0.028)) * np.sin(6.0 * np.arctan2(yy, xx) + t * _TWO_PI)
    rings = max(1, int(p.get("rings", 1)))
    spacing = float(p.get("spacing", 0.34))
    for k in range(rings):
        rr = radius * (1.0 - k * spacing)
        ring_k = np.abs(np.sqrt(xx * xx + yy * yy) - (rr + wobble * (1.0 - k * 0.25))) - 0.040
        _over(rgba, _halo(ring_k, 0.05, 2), s.palette, s.size, boost=0.9 - k * 0.15, alpha=0.30 - k * 0.06)
        _over(rgba, ring_k, s.palette, s.size, boost=1.4 - k * 0.2, alpha=0.95 - k * 0.15)
        _over(rgba, np.abs(ring_k) - 0.012, "warm_ivory", s.size, boost=1.5, alpha=0.55 - k * 0.10)
    notches = int(p.get("notches", 0))
    for i in range(notches):
        a = i * (_TWO_PI / max(1, notches)) + t * 0.3
        _over(rgba, sdf_capsule(
            xx, yy, float(np.cos(a)) * (radius - 0.05), float(np.sin(a)) * (radius - 0.05),
            float(np.cos(a)) * (radius + 0.05), float(np.sin(a)) * (radius + 0.05), 0.032,
        ), s.accent, s.size, boost=1.45, alpha=0.9)
    for i in range(int(p.get("glyphs", 0))):
        a = 0.8 * i + t * 0.5
        cx, cy = float(np.cos(a)) * (radius * 0.62), float(np.sin(a)) * (radius * 0.62)
        _over(rgba, sdf_capsule(xx, yy, cx - 0.10, cy, cx + 0.10, cy, 0.024), "warm_ivory", s.size, boost=1.35, alpha=0.75)
    if t < 0.92:
        _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.075 + 0.04 * t), "warm_ivory", s.size, boost=1.6, alpha=1.0 - t)


def _shape_mist(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    p = s.params
    size = s.size
    field = toroidal_value_fbm(size, size, seed=int(_seed(s)) + int(t * 4), base_cells=4, octaves=3)
    field = np.roll(field, int(t * size * 0.22), axis=1)
    sx = float(p.get("sx", 1.0))
    sy = float(p.get("sy", 1.0))
    mask = sdf_circle(xx * sx, yy * sy, 0.0, 0.02, 0.72 + 0.06 * t)
    _paint_field(rgba, mask, field * float(p.get("density", 0.55)) + 0.30, s.palette, size, alpha=0.90)
    for i in range(int(p.get("strands", 0))):
        a = i * (_TWO_PI / max(1, int(p.get("strands", 1)))) + 0.4 + t * 0.4
        _over(rgba, sdf_bezier(
            xx, yy,
            (float(np.cos(a)) * 0.16, float(np.sin(a)) * 0.16),
            (float(np.cos(a + 0.4)) * 0.48, float(np.sin(a + 0.4)) * 0.44),
            (float(np.cos(a + 0.8)) * 0.66, float(np.sin(a + 0.8)) * 0.58),
            0.026,
        ), s.accent, size, boost=1.0, alpha=0.42)
    core = sdf_circle(xx, yy, 0.0, 0.0, 0.20 * (1.0 - 0.35 * t))
    _over(rgba, core, "warm_ivory", size, boost=1.3, alpha=0.60 * (1.0 - t))
    # Bioluminescent motes keep the cloud readable in grayscale.
    for i in range(4):
        a = i * (_TWO_PI / 4.0) + t * 0.9 + _seed(s) * 0.01
        dist = 0.24 + 0.18 * ((i + 1) % 3) / 2.0
        _over(rgba, sdf_circle(xx, yy, float(np.cos(a)) * dist, float(np.sin(a)) * dist, 0.045),
              s.accent, size, boost=1.5, alpha=0.75 - 0.12 * i)


def _shape_splash(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    drops = int(s.params.get("drops", 6))
    body = sdf_circle(xx, yy, 0.0, 0.0, 0.16 * (1.0 - 0.4 * t))
    for i in range(drops):
        a = i * (_TWO_PI / drops) + 0.2
        dist = 0.40 + 0.42 * t + 0.05 * (i % 2)
        r = 0.075 - 0.025 * t + 0.02 * (i % 3)
        body = sdf_union(body, sdf_circle(xx, yy, float(np.cos(a)) * dist, float(np.sin(a)) * dist, max(0.028, r)))
    _over(rgba, _halo(body, 0.05, 2), "purifier_glow", s.size, boost=0.7, alpha=0.26)
    _over(rgba, body, s.palette, s.size, boost=1.1, alpha=0.94)
    vapor = np.abs(sdf_circle(xx, yy, 0.0, 0.0, 0.34 + 0.30 * t)) - 0.030
    _over(rgba, vapor, "purifier_glow", s.size, boost=1.0, alpha=0.40)
    _over(rgba, sdf_circle(xx, yy, -0.06, 0.08, 0.06), "warm_ivory", s.size, boost=1.6, alpha=0.9)


def _shape_spark(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    body = sdf_circle(xx, yy, 0.0, 0.0, 0.12)
    for a in (0.0, np.pi / 2, np.pi / 4, 3 * np.pi / 4):
        length = 0.62 if a in (0.0, np.pi / 2) else 0.42
        dx, dy = float(np.cos(a)) * length, float(np.sin(a)) * length
        body = sdf_union(body, sdf_capsule(xx, yy, -dx, -dy, dx, dy, 0.030))
    _over(rgba, _halo(body, 0.08, 2), s.palette, s.size, boost=0.8, alpha=0.26)
    _over(rgba, body, s.palette, s.size, boost=1.15, alpha=0.94)
    if s.params.get("fork"):
        sway = 0.10 * float(np.sin(t * _TWO_PI))
        _over(rgba, sdf_bezier(xx, yy, (0.12, -0.10), (0.40 + sway, -0.30), (0.58 + sway, -0.02), 0.024),
              s.accent, s.size, boost=1.3, alpha=0.80)
        _over(rgba, sdf_circle(xx, yy, 0.58 + sway, -0.02, 0.045), "warm_ivory", s.size, boost=1.6, alpha=0.9)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.10), "warm_ivory", s.size, boost=1.65, alpha=1.0)


def _shape_spike_pulse(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    radius = 0.30 + 0.44 * t
    ring = np.abs(np.sqrt(xx * xx + yy * yy) - radius) - 0.034
    _over(rgba, _halo(ring, 0.04, 2), s.palette, s.size, boost=0.8, alpha=0.28)
    _over(rgba, ring, s.palette, s.size, boost=1.3, alpha=0.9)
    spikes = int(s.params.get("spikes", 8))
    for i in range(spikes):
        a = i * (_TWO_PI / spikes) + t * 0.25
        _over(rgba, sdf_capsule(
            xx, yy, float(np.cos(a)) * (radius - 0.02), float(np.sin(a)) * (radius - 0.02),
            float(np.cos(a)) * (radius + 0.16), float(np.sin(a)) * (radius + 0.16), 0.026,
        ), s.accent, s.size, boost=1.35, alpha=0.85)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.10), "warm_ivory", s.size, boost=1.5, alpha=0.9)


def _shape_dust(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    rng = np.random.default_rng(int(_seed(s)))
    body = sdf_circle(xx, yy, 0.0, 0.0, 0.10)
    for i in range(5):
        a = rng.uniform(0.0, _TWO_PI) + t * 0.5
        dist = 0.22 + rng.uniform(0.0, 0.24)
        body = sdf_union(body, sdf_circle(xx, yy, float(np.cos(a)) * dist, float(np.sin(a)) * dist,
                                          float(rng.uniform(0.035, 0.065))))
    _over(rgba, _halo(body, 0.06, 2), s.accent, s.size, boost=0.8, alpha=0.24)
    _over(rgba, body, s.palette, s.size, boost=1.0, alpha=0.9)
    _over(rgba, sdf_circle(xx, yy, 0.0, 0.0, 0.095), "warm_ivory", s.size, boost=1.55, alpha=0.95)


def _shape_wisp(xx, yy, s: ParticleSpec, t: float, rgba) -> None:
    swing = 0.12 * float(np.sin(t * np.pi + 0.30))
    tail = sdf_bezier(xx, yy, (-0.34, 0.34), (0.05 + swing, 0.10), (0.30 + swing, -0.26), 0.045)
    head = sdf_circle(xx, yy, 0.30 + swing, -0.26, 0.16)
    body = sdf_union(tail, head)
    _over(rgba, _halo(body, 0.06, 2), s.palette, s.size, boost=0.8, alpha=0.24)
    _over(rgba, body, s.palette, s.size, boost=1.0, alpha=0.92)
    _over(rgba, sdf_circle(xx, yy, 0.30 + swing, -0.26, 0.075), "warm_ivory", s.size, boost=1.6, alpha=0.95)
    for i in range(4):
        _over(rgba, sdf_circle(xx, yy, -0.36 - i * 0.10, 0.38 + i * 0.065, 0.030 - i * 0.004),
              s.accent, s.size, boost=1.3, alpha=0.65 - i * 0.10)


_SHAPES = {
    "spore": _shape_spore,
    "teardrop": _shape_teardrop,
    "crystal": _shape_crystal,
    "reticle": _shape_reticle,
    "arc": _shape_arc,
    "cross": _shape_cross,
    "chevron": _shape_chevron,
    "slash": _shape_slash,
    "dart": _shape_dart,
    "ember": _shape_ember,
    "wave_crystal": _shape_wave_crystal,
    "rune": _shape_rune,
    "star": _shape_star,
    "hex_glyph": _shape_hex_glyph,
    "ring": _shape_ring,
    "mist": _shape_mist,
    "splash": _shape_splash,
    "spark": _shape_spark,
    "spike_pulse": _shape_spike_pulse,
    "dust": _shape_dust,
    "wisp": _shape_wisp,
}


def render_frame(spec: ParticleSpec, frame: int) -> np.ndarray:
    """Render one RGBA frame of the spec (0-based frame index)."""
    if spec.shape not in _SHAPES:
        raise KeyError(f"Unknown particle shape '{spec.shape}' for '{spec.id}'")
    xx, yy = make_grid(spec.size, spec.size)
    rgba = _blank(spec.size)
    t = 0.0 if spec.frames <= 1 else frame / float(spec.frames - 1)
    _SHAPES[spec.shape](xx, yy, spec, t, rgba)
    # Guarantee transparent corners so the sprite never reads as a square blob.
    if rgba[0, 0, 3] > 0 or rgba[0, -1, 3] > 0 or rgba[-1, 0, 3] > 0 or rgba[-1, -1, 3] > 0:
        for corner in ((0, 0), (0, -1), (-1, 0), (-1, -1)):
            rgba[corner[0], corner[1], 3] = 0
    return rgba


def render_spec(spec: ParticleSpec) -> List[np.ndarray]:
    return [render_frame(spec, i) for i in range(spec.frames)]


__all__ = ["render_frame", "render_spec"]
