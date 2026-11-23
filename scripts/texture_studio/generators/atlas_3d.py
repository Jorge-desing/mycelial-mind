"""Procedural 64x64 UV atlas + LabPBR 1.3 + animated .mcmeta generator for Masterwork 3D blocks & items."""

from __future__ import annotations

import os
from typing import Dict, List, Tuple
import numpy as np
from PIL import Image

from ..compositor import channels_to_labpbr_s, normals_to_labpbr
from ..palette_oklab import PALETTES
from ..sdf_engine import (
    bayer_quantize,
    compute_height_normals,
    save_flipbook,
    toroidal_value_fbm,
    toroidal_value_noise,
)

ATLAS_SIZE = 64
TILE_SIZE = 16


def _save_rgba(path: str, arr: np.ndarray) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), mode="RGBA").save(path)


def _coords(n: int = TILE_SIZE) -> Tuple[np.ndarray, np.ndarray]:
    g = (np.arange(n, dtype=np.float32) + 0.5) / n
    return np.meshgrid(g, g)


_IY, _IX = np.mgrid[0:TILE_SIZE, 0:TILE_SIZE]
_TILE_U, _TILE_V = _coords(TILE_SIZE)

# Three crisp 8x8 pixel-aligned geometric rune/meander friezes (repeated twice horizontally across iy=4..11)
_RUNE_GLYPHS: Tuple[np.ndarray, ...] = (
    np.array([
        [0, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 1, 0, 0, 1, 1, 0],
        [0, 1, 0, 1, 1, 0, 1, 0],
        [0, 0, 1, 0, 0, 1, 0, 0],
        [0, 0, 1, 0, 0, 1, 0, 0],
        [0, 1, 0, 1, 1, 0, 1, 0],
        [0, 1, 1, 0, 0, 1, 1, 0],
        [0, 0, 0, 0, 0, 0, 0, 0],
    ], dtype=bool),
    np.array([
        [0, 0, 0, 0, 0, 0, 0, 0],
        [0, 1, 1, 1, 1, 1, 0, 0],
        [0, 1, 0, 0, 0, 1, 0, 0],
        [0, 1, 0, 1, 1, 1, 0, 0],
        [0, 1, 0, 1, 0, 0, 0, 0],
        [0, 1, 0, 1, 1, 1, 1, 0],
        [0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0],
    ], dtype=bool),
    np.array([
        [0, 0, 0, 0, 0, 0, 0, 0],
        [0, 0, 0, 1, 1, 0, 0, 0],
        [0, 0, 1, 0, 0, 1, 0, 0],
        [0, 1, 0, 1, 1, 0, 1, 0],
        [0, 1, 0, 1, 1, 0, 1, 0],
        [0, 0, 1, 0, 0, 1, 0, 0],
        [0, 0, 0, 1, 1, 0, 0, 0],
        [0, 0, 0, 0, 0, 0, 0, 0],
    ], dtype=bool),
)


def _toroidal_polyline_dist(wu: np.ndarray, wv: np.ndarray, pts: List[Tuple[float, float]]) -> np.ndarray:
    """Minimum periodic distance from (wu, wv) in [0, 1)^2 to a 2D polyline across 3x3 wrap offsets."""
    min_d2 = np.full_like(wu, 1e9, dtype=np.float32)
    for idx in range(len(pts) - 1):
        ax, ay = pts[idx]
        bx, by = pts[idx + 1]
        bax = bx - ax
        bay = by - ay
        denom = max(1e-6, bax * bax + bay * bay)
        for ox in (-1.0, 0.0, 1.0):
            for oy in (-1.0, 0.0, 1.0):
                px = wu + ox - ax
                py = wv + oy - ay
                t = np.clip((px * bax + py * bay) / denom, 0.0, 1.0)
                dx = px - bax * t
                dy = py - bay * t
                d2 = dx * dx + dy * dy
                min_d2 = np.minimum(min_d2, d2)
    return np.sqrt(min_d2)


def _make_tile(
    palette_key: str,
    pattern: str,
    seed: int,
    smooth: float = 0.45,
    f0: float = 0.08,
    porosity_sss: float = 0.20,
    emissive: float = 0.0,
    is_subsurface: bool = False,
    metal_id: int | None = None,
) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    """Synthesize a 16x16 material tile returning (albedo, height, nx, ny, nz, s_img)."""
    ramp = PALETTES[palette_key]
    iy, ix = _IY, _IX
    u, v = _TILE_U, _TILE_V
    em_arr: np.ndarray | None = None

    if pattern == "beveled_plate":
        # Forged interlocking armor/stone panels with crisp 1px directional seams & corner bolts
        grain = toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed, base_cells=4, octaves=3)
        h = 0.54 + 0.14 * (grain - 0.5)
        h_seam = (iy == 0) | (iy == 8)
        h_hi = (iy == 1) | (iy == 9)
        h_lo = (iy == 7) | (iy == 15)
        v_seam = ((ix == 0) & (iy < 8)) | ((ix == 8) & (iy >= 8))
        v_hi = ((ix == 1) & (iy < 8)) | ((ix == 9) & (iy >= 8))
        seam_all = h_seam | v_seam
        h = np.where(h_lo & (~seam_all), h - 0.06, h)
        h = np.where(h_hi & (~seam_all), h + 0.13, h)
        h = np.where(v_hi & (~seam_all) & (~h_hi), h + 0.09, h)
        h = np.where(h_seam, h - 0.24, h)
        h = np.where(v_seam & (~h_seam), h - 0.18, h)
        for bx, by in ((2, 2), (14, 2), (10, 10), (6, 10)):
            h[by, bx] += 0.10
        h = np.clip(h, 0.08, 0.96)
    elif pattern == "brushed_metal":
        # Anisotropic horizontally brushed steel/titanium without cylindrical banding
        streak = (
            0.65 * toroidal_value_noise(TILE_SIZE, TILE_SIZE, 2, 16, seed=seed + 7)
            + 0.35 * toroidal_value_noise(TILE_SIZE, TILE_SIZE, 4, 8, seed=seed + 13)
        )
        sheen = 0.07 * np.cos(((ix * 0.6 + iy * 0.4) / float(TILE_SIZE)) * 2.0 * np.pi)
        h = 0.54 + 0.14 * (streak - 0.5) + sheen
        h = np.where(iy == 1, h + 0.14, h)
        h = np.where(iy == 15, h - 0.06, h)
        h = np.where(iy == 0, h - 0.20, h)
        h = np.clip(h, 0.08, 0.96)
    elif pattern == "crystal_facet":
        # Flat-planed toroidal Voronoi mineral facets with crisp directional edge glints
        rng = np.random.default_rng(seed + 31)
        sx = np.empty(6, dtype=np.float32)
        sy = np.empty(6, dtype=np.float32)
        sx[0], sy[0] = rng.uniform(4.2, 7.6), rng.uniform(4.2, 7.6)
        sx[1], sy[1] = rng.uniform(8.4, 11.8), rng.uniform(8.2, 11.8)
        sx[2], sy[2] = rng.uniform(0.5, 4.0), rng.uniform(8.5, 14.5)
        sx[3], sy[3] = rng.uniform(11.5, 15.5), rng.uniform(1.0, 5.5)
        sx[4], sy[4] = rng.uniform(1.0, 6.0), rng.uniform(0.5, 3.8)
        sx[5], sy[5] = rng.uniform(9.5, 15.0), rng.uniform(11.5, 15.5)

        fx = ix.astype(np.float32) + 0.5
        fy = iy.astype(np.float32) + 0.5
        d1 = np.full((TILE_SIZE, TILE_SIZE), 1e9, dtype=np.float32)
        d2 = np.full((TILE_SIZE, TILE_SIZE), 1e9, dtype=np.float32)
        c1 = np.zeros((TILE_SIZE, TILE_SIZE), dtype=np.int32)
        c2 = np.zeros((TILE_SIZE, TILE_SIZE), dtype=np.int32)
        dx1 = np.zeros((TILE_SIZE, TILE_SIZE), dtype=np.float32)
        dy1 = np.zeros((TILE_SIZE, TILE_SIZE), dtype=np.float32)

        for cid in range(6):
            dx = fx - sx[cid]
            dx = np.where(dx > TILE_SIZE * 0.5, dx - TILE_SIZE, dx)
            dx = np.where(dx < -TILE_SIZE * 0.5, dx + TILE_SIZE, dx)
            dy = fy - sy[cid]
            dy = np.where(dy > TILE_SIZE * 0.5, dy - TILE_SIZE, dy)
            dy = np.where(dy < -TILE_SIZE * 0.5, dy + TILE_SIZE, dy)
            dist = np.sqrt(dx * dx + dy * dy)
            closer1 = dist < d1
            closer2 = (~closer1) & (dist < d2)
            d2 = np.where(closer1, d1, np.where(closer2, dist, d2))
            c2 = np.where(closer1, c1, np.where(closer2, cid, c2))
            d1 = np.where(closer1, dist, d1)
            c1 = np.where(closer1, cid, c1)
            dx1 = np.where(closer1, dx, dx1)
            dy1 = np.where(closer1, dy, dy1)

        lums = np.array([0.28, 0.82, 0.42, 0.70, 0.92, 0.56], dtype=np.float32)
        shift = int(seed % 3)
        lums_rot = np.roll(lums, shift)
        # Guarantee the two central cells keep strong value separation
        if abs(float(lums_rot[0] - lums_rot[1])) < 0.35:
            lums_rot[1] = 0.90 if lums_rot[0] < 0.55 else 0.30
        slopes = rng.uniform(-1.0, 1.0, size=(6, 2)).astype(np.float32)
        plane = lums_rot[c1] + 0.012 * (dx1 * slopes[c1, 0] + dy1 * slopes[c1, 1])

        vx = sx[c2] - sx[c1]
        vx = np.where(vx > TILE_SIZE * 0.5, vx - TILE_SIZE, vx)
        vx = np.where(vx < -TILE_SIZE * 0.5, vx + TILE_SIZE, vx)
        vy = sy[c2] - sy[c1]
        vy = np.where(vy > TILE_SIZE * 0.5, vy - TILE_SIZE, vy)
        vy = np.where(vy < -TILE_SIZE * 0.5, vy + TILE_SIZE, vy)
        edge_mask = (d2 - d1) < 1.15
        lit_edge = (vx + vy) > 0.0
        h = np.where(edge_mask & lit_edge, plane + 0.13, np.where(edge_mask, plane - 0.08, plane))
        # Two crisp 1px internal white-hot crystal glints
        gx0 = int(np.clip(round(float(sx[1]) - 0.5), 1, TILE_SIZE - 2))
        gy0 = int(np.clip(round(float(sy[1]) - 0.5), 1, TILE_SIZE - 2))
        gx1 = int(np.clip(round(float(sx[0]) + 0.5), 1, TILE_SIZE - 2))
        gy1 = int(np.clip(round(float(sy[0]) - 0.5), 1, TILE_SIZE - 2))
        h[gy0, gx0] += 0.16
        h[gy1, gx1] += 0.16
        h = np.clip(h, 0.10, 0.98)
    elif pattern == "neural_vein":
        # Deep bioluminescent tissue with organic irregular branching filaments
        sub = 0.24 + 0.14 * toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed + 5, base_cells=4, octaves=2)
        wx = (toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed + 41, base_cells=4, octaves=2) - 0.5) * 0.20
        wy = (toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed + 43, base_cells=4, octaves=2) - 0.5) * 0.20
        wu = (u + wx) % 1.0
        wv = (v + wy) % 1.0

        rng = np.random.default_rng(seed + 47)
        theta = float(rng.uniform(0.0, np.pi))
        ct, st = float(np.cos(theta)), float(np.sin(theta))
        nx_p, ny_p = -st, ct
        cx0 = 0.50 + float(rng.uniform(-0.06, 0.06))
        cy0 = 0.50 + float(rng.uniform(-0.06, 0.06))
        trunk = [
            (cx0 - 0.42 * ct + 0.08 * nx_p, cy0 - 0.42 * st + 0.08 * ny_p),
            (cx0 - 0.16 * ct - 0.07 * nx_p, cy0 - 0.16 * st - 0.07 * ny_p),
            (cx0, cy0),
            (cx0 + 0.18 * ct + 0.09 * nx_p, cy0 + 0.18 * st + 0.09 * ny_p),
            (cx0 + 0.44 * ct - 0.05 * nx_p, cy0 + 0.44 * st - 0.05 * ny_p),
        ]
        branch_a = [
            trunk[1],
            (trunk[1][0] + 0.18 * nx_p - 0.06 * ct, trunk[1][1] + 0.18 * ny_p - 0.06 * st),
            (trunk[1][0] + 0.33 * nx_p + 0.04 * ct, trunk[1][1] + 0.33 * ny_p + 0.04 * st),
        ]
        branch_b = [
            trunk[3],
            (trunk[3][0] - 0.19 * nx_p + 0.06 * ct, trunk[3][1] - 0.19 * ny_p + 0.06 * st),
            (trunk[3][0] - 0.34 * nx_p - 0.05 * ct, trunk[3][1] - 0.34 * ny_p - 0.05 * st),
        ]
        d_tr = _toroidal_polyline_dist(wu, wv, trunk)
        d_ba = _toroidal_polyline_dist(wu, wv, branch_a)
        d_bb = _toroidal_polyline_dist(wu, wv, branch_b)
        min_dist = np.minimum(d_tr, np.minimum(d_ba, d_bb))
        vein = np.clip(1.0 - min_dist / 0.095, 0.0, 1.0) ** 1.4
        droplets = np.zeros_like(sub)
        for px, py in (trunk[2], branch_a[1], branch_b[1]):
            dx_i = int(np.clip(round(px * TILE_SIZE - 0.5), 0, TILE_SIZE - 1)) % TILE_SIZE
            dy_i = int(np.clip(round(py * TILE_SIZE - 0.5), 0, TILE_SIZE - 1)) % TILE_SIZE
            droplets[dy_i, dx_i] = 0.22
        h = np.clip(sub + 0.68 * vein + droplets, 0.08, 0.98)
        em_arr = np.clip(emissive * np.clip(0.18 + 0.82 * (vein + droplets), 0.0, 1.0), 0.0, 1.0).astype(np.float32)
    elif pattern == "runic_carving":
        # Chiseled masonry with crisp horizontal channels and 8x8 angular rune frieze
        grain = toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed + 19, base_cells=4, octaves=3)
        h = 0.44 + 0.12 * (grain - 0.5)
        trim_groove = (iy == 2) | (iy == 13)
        trim_hi = (iy == 3) | (iy == 14)
        h = np.where(trim_hi, h + 0.12, h)
        h = np.where(trim_groove, h - 0.22, h)

        glyph = _RUNE_GLYPHS[seed % len(_RUNE_GLYPHS)]
        rune_full = np.zeros((TILE_SIZE, TILE_SIZE), dtype=bool)
        rune_band = np.tile(glyph, (1, 2))
        rune_full[4:12, :] = rune_band

        if emissive > 0.10:
            above = np.roll(rune_full, -1, axis=0) & (~rune_full) & (~trim_groove)
            h = np.where(above, h - 0.14, h)
            h = np.where(rune_full, 0.90, h)
            bleed = (
                np.roll(rune_full, 1, axis=0)
                | np.roll(rune_full, -1, axis=0)
                | np.roll(rune_full, 1, axis=1)
                | np.roll(rune_full, -1, axis=1)
            ) & (~rune_full)
            em_arr = np.where(rune_full, emissive, np.where(bleed, emissive * 0.25, 0.0)).astype(np.float32)
        else:
            below_right = np.roll(np.roll(rune_full, 1, axis=0), 1, axis=1) & (~rune_full) & (~trim_groove)
            h = np.where(below_right, h + 0.12, h)
            h = np.where(rune_full, h - 0.22, h)
        h = np.clip(h, 0.08, 0.96)
    elif pattern == "ribbed_coil":
        # Crisp period-4px horizontal machined collar rings & wrapped grips
        grain = toroidal_value_noise(TILE_SIZE, TILE_SIZE, 2, 16, seed=seed + 29)
        cyl = 0.07 * np.cos((u - 0.35) * 2.0 * np.pi)
        mod4 = iy % 4
        h = np.where(
            mod4 == 3,
            0.18,
            np.where(
                mod4 == 0,
                0.80 + 0.05 * cyl,
                np.where(mod4 == 1, 0.56, 0.50) + 0.06 * (grain - 0.5) + cyl,
            ),
        )
        h = np.clip(h, 0.10, 0.94)
    elif pattern == "chitin_scale":
        # Overlapping fibrous bone/chitin carapace plates (2 staggered rows across 16px)
        fiber = (
            0.65 * toroidal_value_noise(TILE_SIZE, TILE_SIZE, 16, 3, seed=seed + 17)
            + 0.35 * toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed + 23, base_cells=4, octaves=2)
        )
        u_row = (ix[0].astype(np.float32) + 0.5) / float(TILE_SIZE)
        s0 = (7 + np.round(0.85 * np.sin(u_row * 2.0 * np.pi + float(seed % 5))).astype(np.int32)) % TILE_SIZE
        s1 = (15 + np.round(0.85 * np.cos(u_row * 2.0 * np.pi + float(seed % 7))).astype(np.int32)) % TILE_SIZE
        d0 = (iy - s0[None, :]) % TILE_SIZE
        d1 = (iy - s1[None, :]) % TILE_SIZE
        dist_below = np.minimum(d0, d1)
        dist_above = np.minimum((s0[None, :] - iy) % TILE_SIZE, (s1[None, :] - iy) % TILE_SIZE)
        slope = 0.46 + 0.18 * np.clip(dist_below.astype(np.float32) / 7.0, 0.0, 1.0)
        h = slope + 0.14 * (fiber - 0.5)
        shadow_seam = dist_below == 0
        lip_hi = (dist_above == 1) & (~shadow_seam)
        suture_x0 = (4 + seed % 3) % TILE_SIZE
        suture_x1 = (12 + seed % 3) % TILE_SIZE
        suture = ((d0 < d1) & (ix == suture_x0)) | ((d1 < d0) & (ix == suture_x1))
        h = np.where(lip_hi, h + 0.14, h)
        h = np.where(suture & (~shadow_seam), h - 0.14, h)
        h = np.where(shadow_seam, h - 0.26, h)
        h = np.clip(h, 0.08, 0.94)
    elif pattern == "vent_grate":
        # Crisp industrial louvered intake slits with vertical frame dividers (zero slit noise)
        grain = toroidal_value_noise(TILE_SIZE, TILE_SIZE, 2, 16, seed=seed + 37)
        mod4 = iy % 4
        rib = (ix == 0) | (ix == 8)
        h = np.where(
            rib,
            0.62,
            np.where(
                mod4 == 3,
                0.12,
                np.where(mod4 == 0, 0.78, 0.54 + 0.08 * (grain - 0.5)),
            ),
        )
        h = np.clip(h, 0.08, 0.92)
    else:
        grain = toroidal_value_fbm(TILE_SIZE, TILE_SIZE, seed=seed, base_cells=4, octaves=3)
        h = np.clip(0.45 + 0.25 * (grain - 0.5), 0.10, 0.94)

    # Low-dither Oklab quantization + subtle proportional top-left key bevel
    albedo = bayer_quantize(h, ramp, dither_strength=0.11)
    nx_lit, ny_lit, _ = compute_height_normals(h, strength=1.6)
    directional = np.clip(-nx_lit * 0.12 - ny_lit * 0.14, -0.10, 0.12)
    rgb = np.clip(albedo[..., :3].astype(np.float32) * (1.0 + directional[..., None]), 0, 255).astype(np.uint8)
    albedo = np.concatenate([rgb, albedo[..., 3:4]], axis=-1)

    nx_pbr, ny_pbr, nz_pbr = compute_height_normals(h, strength=2.2)
    sm_arr = np.full((TILE_SIZE, TILE_SIZE), smooth, dtype=np.float32)
    f0_arr = np.full((TILE_SIZE, TILE_SIZE), f0, dtype=np.float32)
    ps_arr = np.full((TILE_SIZE, TILE_SIZE), porosity_sss, dtype=np.float32)
    if em_arr is None:
        em_arr = np.clip(emissive * (0.45 + 0.55 * h), 0.0, 1.0).astype(np.float32)

    s_img = channels_to_labpbr_s(sm_arr, f0_arr, ps_arr, em_arr, is_subsurface=is_subsurface, metal_id=metal_id)
    if metal_id is not None:
        s_img[..., 1] = np.uint8(int(np.clip(metal_id, 230, 254)))
    return albedo, h.astype(np.float32), nx_pbr, ny_pbr, nz_pbr, s_img


# Each atlas is a 4x4 grid of 16x16 tiles (total 64x64).
# Tile index (col, row) maps to Minecraft UV [col*4, row*4, (col+1)*4, (row+1)*4].
ATLAS_SPECS: Dict[str, List[Tuple[str, str, int, Dict[str, object]]]] = {
    "atlas_blocks_hive": [
        ("abyssal_obsidian", "beveled_plate", 101, {"smooth": 0.35, "porosity_sss": 0.25}),
        ("mycelial_violet", "neural_vein", 102, {"smooth": 0.48, "porosity_sss": 0.65, "is_subsurface": True, "emissive": 0.25}),
        ("chitin_bone", "chitin_scale", 103, {"smooth": 0.42, "porosity_sss": 0.20}),
        ("predator_amethyst", "crystal_facet", 104, {"smooth": 0.88, "f0": 0.14, "emissive": 0.85, "is_subsurface": True}),
        ("abyssal_indigo", "runic_carving", 105, {"smooth": 0.40, "porosity_sss": 0.35}),
        ("spore_chartreuse", "crystal_facet", 106, {"smooth": 0.82, "emissive": 0.90, "is_subsurface": True}),
        ("venom_emerald", "neural_vein", 107, {"smooth": 0.55, "emissive": 0.72, "is_subsurface": True}),
        ("volcanic_ember", "crystal_facet", 108, {"smooth": 0.75, "emissive": 0.92, "is_subsurface": True}),
        ("abyssal_obsidian", "vent_grate", 109, {"smooth": 0.50, "f0": 0.10}),
        ("mycelial_violet", "chitin_scale", 110, {"smooth": 0.45, "porosity_sss": 0.55, "is_subsurface": True}),
        ("imperial_brass", "ribbed_coil", 111, {"smooth": 0.76, "metal_id": 231}),
        ("biolum_cyan", "crystal_facet", 112, {"smooth": 0.90, "emissive": 0.88, "is_subsurface": True}),
        ("abyssal_indigo", "beveled_plate", 113, {"smooth": 0.38, "porosity_sss": 0.28}),
        ("warm_ivory", "chitin_scale", 114, {"smooth": 0.34, "porosity_sss": 0.50, "is_subsurface": True}),
        ("imperial_brass", "runic_carving", 115, {"smooth": 0.78, "metal_id": 231, "emissive": 0.25}),
        ("royal_gold", "crystal_facet", 116, {"smooth": 0.92, "emissive": 0.95, "metal_id": 231}),
    ],
    "atlas_blocks_purification": [
        ("platinum_tech", "beveled_plate", 201, {"smooth": 0.78, "metal_id": 236}),
        ("platinum_tech", "brushed_metal", 202, {"smooth": 0.82, "metal_id": 236}),
        ("purification_aqua", "crystal_facet", 203, {"smooth": 0.92, "emissive": 0.90, "is_subsurface": True}),
        ("biolum_cyan", "neural_vein", 204, {"smooth": 0.84, "emissive": 0.85, "is_subsurface": True}),
        ("imperial_brass", "ribbed_coil", 205, {"smooth": 0.78, "metal_id": 231}),
        ("abyssal_obsidian", "vent_grate", 206, {"smooth": 0.52, "f0": 0.12}),
        ("platinum_tech", "runic_carving", 207, {"smooth": 0.74, "metal_id": 236}),
        ("purification_aqua", "ribbed_coil", 208, {"smooth": 0.85, "emissive": 0.75}),
        ("abyssal_indigo", "beveled_plate", 209, {"smooth": 0.42, "porosity_sss": 0.22}),
        ("chitin_bone", "beveled_plate", 210, {"smooth": 0.45, "porosity_sss": 0.18}),
        ("biolum_cyan", "vent_grate", 211, {"smooth": 0.80, "emissive": 0.70}),
        ("royal_gold", "beveled_plate", 212, {"smooth": 0.86, "metal_id": 231}),
        ("platinum_tech", "ribbed_coil", 213, {"smooth": 0.75, "metal_id": 236}),
        ("purification_aqua", "runic_carving", 214, {"smooth": 0.88, "emissive": 0.82}),
        ("abyssal_obsidian", "beveled_plate", 215, {"smooth": 0.40, "porosity_sss": 0.25}),
        ("warm_ivory", "crystal_facet", 216, {"smooth": 0.90, "emissive": 0.85}),
    ],
    "atlas_blocks_altars": [
        ("abyssal_obsidian", "runic_carving", 301, {"smooth": 0.45, "porosity_sss": 0.25}),
        ("predator_amethyst", "runic_carving", 302, {"smooth": 0.65, "emissive": 0.65, "is_subsurface": True}),
        ("royal_gold", "beveled_plate", 303, {"smooth": 0.88, "metal_id": 231}),
        ("royal_gold", "runic_carving", 304, {"smooth": 0.90, "metal_id": 231, "emissive": 0.45}),
        ("imperial_brass", "ribbed_coil", 305, {"smooth": 0.78, "metal_id": 231}),
        ("mycelial_violet", "crystal_facet", 306, {"smooth": 0.88, "emissive": 0.88, "is_subsurface": True}),
        ("volcanic_ember", "crystal_facet", 307, {"smooth": 0.86, "emissive": 0.95, "is_subsurface": True}),
        ("abyssal_indigo", "beveled_plate", 308, {"smooth": 0.42, "porosity_sss": 0.30}),
        ("chitin_bone", "chitin_scale", 309, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("biolum_cyan", "crystal_facet", 310, {"smooth": 0.90, "emissive": 0.85, "is_subsurface": True}),
        ("abyssal_obsidian", "beveled_plate", 311, {"smooth": 0.38, "porosity_sss": 0.28}),
        ("spore_chartreuse", "neural_vein", 312, {"smooth": 0.70, "emissive": 0.78, "is_subsurface": True}),
        ("platinum_tech", "beveled_plate", 313, {"smooth": 0.78, "metal_id": 236}),
        ("warm_ivory", "crystal_facet", 314, {"smooth": 0.90, "emissive": 0.80}),
        ("predator_amethyst", "crystal_facet", 315, {"smooth": 0.92, "emissive": 0.92, "is_subsurface": True}),
        ("imperial_brass", "beveled_plate", 316, {"smooth": 0.80, "metal_id": 231}),
    ],
    "atlas_items_weapons": [
        ("abyssal_obsidian", "beveled_plate", 401, {"smooth": 0.48, "f0": 0.12}),
        ("chitin_bone", "chitin_scale", 402, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("biolum_cyan", "crystal_facet", 403, {"smooth": 0.92, "emissive": 0.92, "is_subsurface": True}),
        ("platinum_tech", "brushed_metal", 404, {"smooth": 0.86, "metal_id": 236}),
        ("royal_gold", "ribbed_coil", 405, {"smooth": 0.86, "metal_id": 231}),
        ("volcanic_ember", "crystal_facet", 406, {"smooth": 0.88, "emissive": 0.92, "is_subsurface": True}),
        ("purification_aqua", "crystal_facet", 407, {"smooth": 0.90, "emissive": 0.88, "is_subsurface": True}),
        ("abyssal_indigo", "ribbed_coil", 408, {"smooth": 0.44, "porosity_sss": 0.28}),
        ("platinum_tech", "vent_grate", 409, {"smooth": 0.78, "metal_id": 236}),
        ("predator_amethyst", "neural_vein", 410, {"smooth": 0.75, "emissive": 0.80, "is_subsurface": True}),
        ("spore_chartreuse", "crystal_facet", 411, {"smooth": 0.85, "emissive": 0.88, "is_subsurface": True}),
        ("imperial_brass", "beveled_plate", 412, {"smooth": 0.82, "metal_id": 231}),
        ("mycelial_violet", "beveled_plate", 413, {"smooth": 0.50, "porosity_sss": 0.55, "is_subsurface": True}),
        ("platinum_tech", "beveled_plate", 414, {"smooth": 0.82, "metal_id": 236}),
        ("warm_ivory", "crystal_facet", 415, {"smooth": 0.90, "emissive": 0.85}),
        ("abyssal_obsidian", "vent_grate", 416, {"smooth": 0.52, "f0": 0.14}),
    ],
    "atlas_items_biotech": [
        ("platinum_tech", "beveled_plate", 501, {"smooth": 0.82, "metal_id": 236}),
        ("biolum_cyan", "crystal_facet", 502, {"smooth": 0.94, "emissive": 0.88, "is_subsurface": True}),
        ("venom_emerald", "neural_vein", 503, {"smooth": 0.82, "emissive": 0.86, "is_subsurface": True}),
        ("spore_chartreuse", "neural_vein", 504, {"smooth": 0.82, "emissive": 0.86, "is_subsurface": True}),
        ("purification_aqua", "neural_vein", 505, {"smooth": 0.88, "emissive": 0.88, "is_subsurface": True}),
        ("volcanic_ember", "neural_vein", 506, {"smooth": 0.84, "emissive": 0.90, "is_subsurface": True}),
        ("imperial_brass", "ribbed_coil", 507, {"smooth": 0.80, "metal_id": 231}),
        ("abyssal_obsidian", "ribbed_coil", 508, {"smooth": 0.45, "porosity_sss": 0.25}),
        ("predator_amethyst", "crystal_facet", 509, {"smooth": 0.88, "emissive": 0.85, "is_subsurface": True}),
        ("warm_ivory", "beveled_plate", 510, {"smooth": 0.72, "porosity_sss": 0.15}),
        ("platinum_tech", "vent_grate", 511, {"smooth": 0.76, "metal_id": 236}),
        ("mycelial_violet", "crystal_facet", 512, {"smooth": 0.86, "emissive": 0.80, "is_subsurface": True}),
        ("abyssal_indigo", "beveled_plate", 513, {"smooth": 0.45, "porosity_sss": 0.30}),
        ("royal_gold", "runic_carving", 514, {"smooth": 0.88, "metal_id": 231, "emissive": 0.40}),
        ("chitin_bone", "beveled_plate", 515, {"smooth": 0.48, "porosity_sss": 0.20}),
        ("biolum_cyan", "ribbed_coil", 516, {"smooth": 0.85, "emissive": 0.78}),
    ],
    "atlas_items_armor_relics": [
        ("abyssal_obsidian", "chitin_scale", 601, {"smooth": 0.52, "porosity_sss": 0.22}),
        ("abyssal_indigo", "beveled_plate", 602, {"smooth": 0.46, "porosity_sss": 0.25}),
        ("chitin_bone", "chitin_scale", 603, {"smooth": 0.48, "porosity_sss": 0.18}),
        ("royal_gold", "runic_carving", 604, {"smooth": 0.90, "metal_id": 231, "emissive": 0.50}),
        ("predator_amethyst", "crystal_facet", 605, {"smooth": 0.92, "emissive": 0.92, "is_subsurface": True}),
        ("mycelial_violet", "neural_vein", 606, {"smooth": 0.65, "emissive": 0.75, "is_subsurface": True}),
        ("spore_chartreuse", "chitin_scale", 607, {"smooth": 0.68, "emissive": 0.80, "is_subsurface": True}),
        ("imperial_brass", "beveled_plate", 608, {"smooth": 0.84, "metal_id": 231}),
        ("biolum_cyan", "crystal_facet", 609, {"smooth": 0.92, "emissive": 0.90, "is_subsurface": True}),
        ("volcanic_ember", "neural_vein", 610, {"smooth": 0.80, "emissive": 0.90, "is_subsurface": True}),
        ("platinum_tech", "beveled_plate", 611, {"smooth": 0.82, "metal_id": 236}),
        ("warm_ivory", "crystal_facet", 612, {"smooth": 0.90, "emissive": 0.85}),
        ("abyssal_obsidian", "runic_carving", 613, {"smooth": 0.48, "porosity_sss": 0.22}),
        ("royal_gold", "crystal_facet", 614, {"smooth": 0.94, "emissive": 0.95, "metal_id": 231}),
        ("venom_emerald", "crystal_facet", 615, {"smooth": 0.88, "emissive": 0.88, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 616, {"smooth": 0.38, "porosity_sss": 0.45, "is_subsurface": True}),
    ],
    "atlas_relic_synaptic_suppressor": [
        ("abyssal_obsidian", "chitin_scale", 701, {"smooth": 0.40, "porosity_sss": 0.18}),
        ("platinum_tech", "brushed_metal", 702, {"smooth": 0.88, "metal_id": 236}),
        ("purification_aqua", "crystal_facet", 703, {"smooth": 0.94, "emissive": 0.96, "is_subsurface": True}),
        ("royal_gold", "runic_carving", 704, {"smooth": 0.86, "metal_id": 231, "emissive": 0.35}),
        ("abyssal_indigo", "beveled_plate", 705, {"smooth": 0.42, "porosity_sss": 0.16}),
        ("biolum_cyan", "neural_vein", 706, {"smooth": 0.82, "emissive": 0.94, "is_subsurface": True}),
        ("platinum_tech", "vent_grate", 707, {"smooth": 0.76, "metal_id": 236}),
        ("imperial_brass", "ribbed_coil", 708, {"smooth": 0.82, "metal_id": 231}),
        ("mycelial_violet", "chitin_scale", 709, {"smooth": 0.44, "porosity_sss": 0.32, "is_subsurface": True}),
        ("purification_aqua", "crystal_facet", 710, {"smooth": 0.90, "emissive": 0.84, "is_subsurface": True}),
        ("imperial_brass", "beveled_plate", 711, {"smooth": 0.84, "metal_id": 231}),
        ("predator_amethyst", "neural_vein", 712, {"smooth": 0.78, "emissive": 0.76, "is_subsurface": True}),
        ("abyssal_obsidian", "runic_carving", 713, {"smooth": 0.48, "porosity_sss": 0.18}),
        ("biolum_cyan", "ribbed_coil", 714, {"smooth": 0.84, "emissive": 0.82, "is_subsurface": True}),
        ("warm_ivory", "crystal_facet", 715, {"smooth": 0.90, "emissive": 0.72}),
        ("platinum_tech", "runic_carving", 716, {"smooth": 0.72, "metal_id": 236}),
    ],
    "atlas_relic_tide_beacon": [
        ("tide_violet", "chitin_scale", 721, {"smooth": 0.50, "porosity_sss": 0.42, "is_subsurface": True}),
        ("abyssal_obsidian", "beveled_plate", 722, {"smooth": 0.42, "porosity_sss": 0.20}),
        ("imperial_brass", "ribbed_coil", 723, {"smooth": 0.80, "metal_id": 231}),
        ("royal_gold", "runic_carving", 724, {"smooth": 0.92, "metal_id": 231, "emissive": 0.34}),
        ("abyssal_indigo", "beveled_plate", 725, {"smooth": 0.40, "porosity_sss": 0.24}),
        ("mycelial_violet", "neural_vein", 726, {"smooth": 0.68, "emissive": 0.78, "is_subsurface": True}),
        ("biolum_cyan", "crystal_facet", 727, {"smooth": 0.92, "emissive": 0.88, "is_subsurface": True}),
        ("abyssal_indigo", "runic_carving", 728, {"smooth": 0.44, "porosity_sss": 0.25}),
        ("predator_amethyst", "crystal_facet", 729, {"smooth": 0.90, "emissive": 0.82, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 730, {"smooth": 0.38, "porosity_sss": 0.38, "is_subsurface": True}),
        ("platinum_tech", "vent_grate", 731, {"smooth": 0.72, "metal_id": 236}),
        ("tide_violet", "crystal_facet", 732, {"smooth": 0.94, "emissive": 0.94, "is_subsurface": True}),
        ("abyssal_obsidian", "runic_carving", 733, {"smooth": 0.46, "porosity_sss": 0.22}),
        ("imperial_brass", "beveled_plate", 734, {"smooth": 0.84, "metal_id": 231}),
        ("spore_chartreuse", "crystal_facet", 735, {"smooth": 0.86, "emissive": 0.86, "is_subsurface": True}),
        ("warm_ivory", "crystal_facet", 736, {"smooth": 0.90, "emissive": 0.78}),
    ],
    "atlas_relic_overmind_crown": [
        ("abyssal_obsidian", "chitin_scale", 741, {"smooth": 0.48, "porosity_sss": 0.22}),
        ("royal_gold", "beveled_plate", 742, {"smooth": 0.88, "metal_id": 231}),
        ("predator_amethyst", "crystal_facet", 743, {"smooth": 0.92, "emissive": 0.92, "is_subsurface": True}),
        ("mycelial_violet", "neural_vein", 744, {"smooth": 0.72, "emissive": 0.72, "is_subsurface": True}),
        ("abyssal_indigo", "runic_carving", 745, {"smooth": 0.42, "porosity_sss": 0.22}),
        ("imperial_brass", "ribbed_coil", 746, {"smooth": 0.82, "metal_id": 231}),
        ("royal_gold", "runic_carving", 747, {"smooth": 0.90, "metal_id": 231, "emissive": 0.40}),
        ("predator_amethyst", "crystal_facet", 748, {"smooth": 0.94, "emissive": 0.96, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 749, {"smooth": 0.42, "porosity_sss": 0.32, "is_subsurface": True}),
        ("chitin_bone", "beveled_plate", 750, {"smooth": 0.52, "porosity_sss": 0.16}),
        ("platinum_tech", "beveled_plate", 751, {"smooth": 0.84, "metal_id": 236}),
        ("spore_chartreuse", "neural_vein", 752, {"smooth": 0.74, "emissive": 0.84, "is_subsurface": True}),
        ("abyssal_obsidian", "runic_carving", 753, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("tide_violet", "crystal_facet", 754, {"smooth": 0.88, "emissive": 0.84, "is_subsurface": True}),
        ("royal_gold", "crystal_facet", 755, {"smooth": 0.92, "metal_id": 231, "emissive": 0.82}),
        ("warm_ivory", "crystal_facet", 756, {"smooth": 0.90, "emissive": 0.80}),
    ],
    "atlas_relic_quorum_seal": [
        ("venom_emerald", "chitin_scale", 761, {"smooth": 0.58, "porosity_sss": 0.42, "is_subsurface": True}),
        ("abyssal_obsidian", "beveled_plate", 762, {"smooth": 0.44, "porosity_sss": 0.22}),
        ("imperial_brass", "beveled_plate", 763, {"smooth": 0.84, "metal_id": 231}),
        ("imperial_brass", "runic_carving", 764, {"smooth": 0.88, "metal_id": 231, "emissive": 0.24}),
        ("venom_emerald", "neural_vein", 765, {"smooth": 0.78, "emissive": 0.78, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 766, {"smooth": 0.44, "porosity_sss": 0.32, "is_subsurface": True}),
        ("royal_gold", "beveled_plate", 767, {"smooth": 0.88, "metal_id": 231}),
        ("spore_chartreuse", "crystal_facet", 768, {"smooth": 0.92, "emissive": 0.90, "is_subsurface": True}),
        ("spore_chartreuse", "chitin_scale", 769, {"smooth": 0.52, "porosity_sss": 0.40, "is_subsurface": True}),
        ("platinum_tech", "beveled_plate", 770, {"smooth": 0.80, "metal_id": 236}),
        ("venom_emerald", "neural_vein", 771, {"smooth": 0.72, "emissive": 0.80, "is_subsurface": True}),
        ("abyssal_indigo", "chitin_scale", 772, {"smooth": 0.46, "porosity_sss": 0.25}),
        ("abyssal_obsidian", "runic_carving", 773, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("warm_ivory", "crystal_facet", 774, {"smooth": 0.90, "emissive": 0.72}),
        ("imperial_brass", "ribbed_coil", 775, {"smooth": 0.82, "metal_id": 231}),
        ("venom_emerald", "crystal_facet", 776, {"smooth": 0.90, "emissive": 0.92, "is_subsurface": True}),
    ],
    "atlas_relic_rift_catalyst": [
        ("abyssal_obsidian", "runic_carving", 781, {"smooth": 0.46, "porosity_sss": 0.24}),
        ("abyssal_indigo", "beveled_plate", 782, {"smooth": 0.40, "porosity_sss": 0.28}),
        ("tide_violet", "crystal_facet", 783, {"smooth": 0.92, "emissive": 0.86, "is_subsurface": True}),
        ("ember_orange", "crystal_facet", 784, {"smooth": 0.92, "emissive": 0.96, "is_subsurface": True}),
        ("mycelial_violet", "runic_carving", 785, {"smooth": 0.60, "emissive": 0.55, "is_subsurface": True}),
        ("mycelial_violet", "neural_vein", 786, {"smooth": 0.74, "emissive": 0.76, "is_subsurface": True}),
        ("predator_amethyst", "crystal_facet", 787, {"smooth": 0.94, "emissive": 0.92, "is_subsurface": True}),
        ("warm_ivory", "crystal_facet", 788, {"smooth": 0.90, "emissive": 0.82}),
        ("platinum_tech", "beveled_plate", 789, {"smooth": 0.76, "metal_id": 236}),
        ("abyssal_obsidian", "vent_grate", 790, {"smooth": 0.48, "porosity_sss": 0.18}),
        ("royal_gold", "beveled_plate", 791, {"smooth": 0.84, "metal_id": 231}),
        ("biolum_cyan", "neural_vein", 792, {"smooth": 0.80, "emissive": 0.80, "is_subsurface": True}),
        ("abyssal_indigo", "chitin_scale", 793, {"smooth": 0.44, "porosity_sss": 0.28}),
        ("imperial_brass", "ribbed_coil", 794, {"smooth": 0.80, "metal_id": 231}),
        ("ember_orange", "neural_vein", 795, {"smooth": 0.82, "emissive": 0.92, "is_subsurface": True}),
        ("warm_ivory", "runic_carving", 796, {"smooth": 0.84, "emissive": 0.64}),
    ],
    "atlas_relic_hive_heart": [
        ("leech_crimson", "chitin_scale", 801, {"smooth": 0.48, "porosity_sss": 0.58, "is_subsurface": True}),
        ("histamine_rose", "neural_vein", 802, {"smooth": 0.74, "emissive": 0.60, "is_subsurface": True}),
        ("necrotic_umber", "neural_vein", 803, {"smooth": 0.42, "porosity_sss": 0.48, "is_subsurface": True}),
        ("abyssal_obsidian", "chitin_scale", 804, {"smooth": 0.44, "porosity_sss": 0.24}),
        ("leech_crimson", "ribbed_coil", 805, {"smooth": 0.60, "porosity_sss": 0.50, "is_subsurface": True}),
        ("imperial_brass", "beveled_plate", 806, {"smooth": 0.80, "metal_id": 231}),
        ("volcanic_ember", "crystal_facet", 807, {"smooth": 0.90, "emissive": 0.88, "is_subsurface": True}),
        ("histamine_rose", "crystal_facet", 808, {"smooth": 0.94, "emissive": 0.90, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 809, {"smooth": 0.40, "porosity_sss": 0.45, "is_subsurface": True}),
        ("leech_crimson", "chitin_scale", 810, {"smooth": 0.54, "porosity_sss": 0.55, "is_subsurface": True}),
        ("histamine_rose", "neural_vein", 811, {"smooth": 0.70, "emissive": 0.68, "is_subsurface": True}),
        ("abyssal_indigo", "beveled_plate", 812, {"smooth": 0.42, "porosity_sss": 0.24}),
        ("abyssal_obsidian", "runic_carving", 813, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("pyro_magma", "ribbed_coil", 814, {"smooth": 0.70, "emissive": 0.68, "is_subsurface": True}),
        ("mutant_amber", "crystal_facet", 815, {"smooth": 0.88, "emissive": 0.82, "is_subsurface": True}),
        ("warm_ivory", "crystal_facet", 816, {"smooth": 0.88, "emissive": 0.70}),
    ],
    "atlas_relic_overmind_essence": [
        ("warm_ivory", "chitin_scale", 821, {"smooth": 0.44, "porosity_sss": 0.42, "is_subsurface": True}),
        ("royal_gold", "beveled_plate", 822, {"smooth": 0.90, "metal_id": 231}),
        ("chitin_bone", "crystal_facet", 823, {"smooth": 0.90, "emissive": 0.76, "is_subsurface": True}),
        ("abyssal_obsidian", "runic_carving", 824, {"smooth": 0.46, "porosity_sss": 0.20}),
        ("warm_ivory", "crystal_facet", 825, {"smooth": 0.94, "emissive": 0.92, "is_subsurface": True}),
        ("mutant_amber", "crystal_facet", 826, {"smooth": 0.90, "emissive": 0.86, "is_subsurface": True}),
        ("royal_gold", "runic_carving", 827, {"smooth": 0.88, "metal_id": 231, "emissive": 0.34}),
        ("abyssal_indigo", "beveled_plate", 828, {"smooth": 0.42, "porosity_sss": 0.24}),
        ("platinum_tech", "beveled_plate", 829, {"smooth": 0.82, "metal_id": 236}),
        ("imperial_brass", "ribbed_coil", 830, {"smooth": 0.80, "metal_id": 231}),
        ("abyssal_obsidian", "vent_grate", 831, {"smooth": 0.50, "porosity_sss": 0.20}),
        ("biolum_cyan", "neural_vein", 832, {"smooth": 0.72, "emissive": 0.68, "is_subsurface": True}),
        ("warm_ivory", "chitin_scale", 833, {"smooth": 0.40, "porosity_sss": 0.44, "is_subsurface": True}),
        ("spectral_bone", "neural_vein", 834, {"smooth": 0.78, "emissive": 0.76, "is_subsurface": True}),
        ("tide_violet", "crystal_facet", 835, {"smooth": 0.88, "emissive": 0.80, "is_subsurface": True}),
        ("royal_gold", "crystal_facet", 836, {"smooth": 0.92, "metal_id": 231, "emissive": 0.84}),
    ],
}


def generate_3d_atlases(out_dir: str, emit_pbr: bool = True) -> List[str]:
    os.makedirs(out_dir, exist_ok=True)
    generated: List[str] = []

    for atlas_name, tiles in ATLAS_SPECS.items():
        assert len(tiles) == 16, f"{atlas_name} must define exactly 16 tiles (4x4)"
        albedo = np.zeros((ATLAS_SIZE, ATLAS_SIZE, 4), dtype=np.uint8)
        height = np.zeros((ATLAS_SIZE, ATLAS_SIZE), dtype=np.float32)
        nx_full = np.zeros((ATLAS_SIZE, ATLAS_SIZE), dtype=np.float32)
        ny_full = np.zeros((ATLAS_SIZE, ATLAS_SIZE), dtype=np.float32)
        nz_full = np.ones((ATLAS_SIZE, ATLAS_SIZE), dtype=np.float32)
        s_img = np.zeros((ATLAS_SIZE, ATLAS_SIZE, 4), dtype=np.uint8)

        for idx, (pal, pat, seed, opts) in enumerate(tiles):
            col = idx % 4
            row = idx // 4
            x0, y0 = col * TILE_SIZE, row * TILE_SIZE
            t_alb, t_h, t_nx, t_ny, t_nz, t_s = _make_tile(
                pal,
                pat,
                seed,
                smooth=float(opts.get("smooth", 0.45)),
                f0=float(opts.get("f0", 0.08)),
                porosity_sss=float(opts.get("porosity_sss", 0.20)),
                emissive=float(opts.get("emissive", 0.0)),
                is_subsurface=bool(opts.get("is_subsurface", False)),
                metal_id=opts.get("metal_id"),  # type: ignore[arg-type]
            )
            albedo[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_alb
            height[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_h
            nx_full[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_nx
            ny_full[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_ny
            nz_full[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_nz
            s_img[y0 : y0 + TILE_SIZE, x0 : x0 + TILE_SIZE] = t_s

        base_path = os.path.join(out_dir, f"{atlas_name}.png")
        _save_rgba(base_path, albedo)
        generated.append(base_path)

        if emit_pbr:
            ao = np.clip(0.55 + 0.45 * height, 0.0, 1.0)
            norm_img = normals_to_labpbr(nx_full, ny_full, nz_full, ao=ao, height_map=height)
            _save_rgba(os.path.join(out_dir, f"{atlas_name}_n.png"), norm_img)
            _save_rgba(os.path.join(out_dir, f"{atlas_name}_s.png"), s_img)

    # Animated 4-frame emissive crystal & plasma energy flow strip (64x256 + .mcmeta)
    frames = []
    u, v = _coords(ATLAS_SIZE)
    ramp_c = PALETTES["predator_amethyst"]
    ramp_b = PALETTES["biolum_cyan"]
    plasma_a = toroidal_value_fbm(ATLAS_SIZE, ATLAS_SIZE, seed=901, base_cells=6, octaves=3)
    plasma_b = toroidal_value_fbm(ATLAS_SIZE, ATLAS_SIZE, seed=902, base_cells=6, octaves=3)
    warp_x = (toroidal_value_fbm(ATLAS_SIZE, ATLAS_SIZE, seed=903, base_cells=4, octaves=2) - 0.5) * 0.10
    warp_y = (toroidal_value_fbm(ATLAS_SIZE, ATLAS_SIZE, seed=904, base_cells=4, octaves=2) - 0.5) * 0.10
    dist_c = np.hypot(u - 0.5 + warp_x, v - 0.5 + warp_y)
    for f_idx in range(4):
        phase = f_idx * (np.pi / 2.0)
        flow = np.clip(
            0.5 + 0.35 * (np.cos(phase) * (plasma_a - 0.5) + np.sin(phase) * (plasma_b - 0.5)) * 2.0,
            0.0,
            1.0,
        )
        r_target = 0.24 + 0.03 * np.sin(phase)
        ring = np.clip(1.0 - np.abs(dist_c - r_target) / 0.11, 0.0, 1.0)
        ring_stepped = np.floor(ring * 5.0 + 0.2) / 5.0
        h = np.clip(0.18 + 0.45 * flow + 0.48 * ring_stepped, 0.0, 1.0)
        c1 = bayer_quantize(h, ramp_c, dither_strength=0.14).astype(np.float32)
        c2 = bayer_quantize(h, ramp_b, dither_strength=0.14).astype(np.float32)
        blend = np.clip(ring_stepped[..., None], 0.0, 1.0)
        frame = np.clip(c1 * (1.0 - blend) + c2 * blend, 0, 255).astype(np.uint8)
        frames.append(frame)

    flow_path = os.path.join(out_dir, "crystal_pulse_flow.png")
    save_flipbook(frames, flow_path, frametime=4, interpolate=True)
    generated.append(flow_path)

    return generated
