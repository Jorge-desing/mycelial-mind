"""Vectorized SDF primitives, boolean ops, domain warping, Blinn-Phong+AO+SSS lighting, Bayer 4x4/8x8 dithering, and toroidal noise."""

from __future__ import annotations

import json
import math
from typing import Sequence, Tuple
import numpy as np
from PIL import Image

_UNSET = object()
_SKIMAGE_SCHARR: object = _UNSET
_SKIMAGE_FRANGI: object = _UNSET
_SKIMAGE_MEIJERING: object = _UNSET
_SKIMAGE_SATO: object = _UNSET


def _get_skimage_scharr():
    global _SKIMAGE_SCHARR
    if _SKIMAGE_SCHARR is _UNSET:
        try:
            from skimage.filters.edges import scharr_h, scharr_v
            _SKIMAGE_SCHARR = (scharr_h, scharr_v)
        except ImportError:
            _SKIMAGE_SCHARR = None
    return _SKIMAGE_SCHARR


def _get_skimage_frangi():
    global _SKIMAGE_FRANGI
    if _SKIMAGE_FRANGI is _UNSET:
        try:
            from skimage.filters.ridges import frangi
            _SKIMAGE_FRANGI = frangi
        except ImportError:
            _SKIMAGE_FRANGI = None
    return _SKIMAGE_FRANGI


def _get_skimage_meijering():
    global _SKIMAGE_MEIJERING
    if _SKIMAGE_MEIJERING is _UNSET:
        try:
            from skimage.filters.ridges import meijering
            _SKIMAGE_MEIJERING = meijering
        except ImportError:
            _SKIMAGE_MEIJERING = None
    return _SKIMAGE_MEIJERING


def _get_skimage_sato():
    global _SKIMAGE_SATO
    if _SKIMAGE_SATO is _UNSET:
        try:
            from skimage.filters.ridges import sato
            _SKIMAGE_SATO = sato
        except ImportError:
            _SKIMAGE_SATO = None
    return _SKIMAGE_SATO

BAYER_4X4 = (
    np.array(
        [
            [0, 8, 2, 10],
            [12, 4, 14, 6],
            [3, 11, 1, 9],
            [15, 7, 13, 5],
        ],
        dtype=np.float32,
    )
    / 16.0
)

BAYER_8X8 = (
    np.array(
        [
            [0, 32, 8, 40, 2, 34, 10, 42],
            [48, 16, 56, 24, 50, 18, 58, 26],
            [12, 44, 4, 36, 14, 46, 6, 38],
            [60, 28, 52, 20, 62, 30, 54, 22],
            [3, 35, 11, 43, 1, 33, 9, 41],
            [51, 19, 59, 27, 49, 17, 57, 25],
            [15, 47, 7, 39, 13, 45, 5, 37],
            [63, 31, 55, 23, 61, 29, 53, 21],
        ],
        dtype=np.float32,
    )
    / 64.0
)


def make_grid(w: int, h: int):
    x = np.linspace(-1.0, 1.0, w, dtype=np.float32)
    y = np.linspace(-1.0, 1.0, h, dtype=np.float32)
    return np.meshgrid(x, y)


def sdf_circle(xx: np.ndarray, yy: np.ndarray, cx: float, cy: float, r: float) -> np.ndarray:
    return np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) - r


def sdf_diamond(xx: np.ndarray, yy: np.ndarray, cx: float, cy: float, r: float) -> np.ndarray:
    return (np.abs(xx - cx) + np.abs(yy - cy)) - r


def sdf_box(xx: np.ndarray, yy: np.ndarray, cx: float, cy: float, bx: float, by: float) -> np.ndarray:
    dx = np.abs(xx - cx) - bx
    dy = np.abs(yy - cy) - by
    return np.sqrt(np.maximum(dx, 0.0) ** 2 + np.maximum(dy, 0.0) ** 2) + np.minimum(np.maximum(dx, dy), 0.0)


def sdf_capsule(xx: np.ndarray, yy: np.ndarray, ax: float, ay: float, bx: float, by: float, r: float) -> np.ndarray:
    pax, pay = xx - ax, yy - ay
    bax, bay = bx - ax, by - ay
    h = np.clip((pax * bax + pay * bay) / max(1e-6, bax * bax + bay * bay), 0.0, 1.0)
    return np.sqrt((pax - bax * h) ** 2 + (pay - bay * h) ** 2) - r


def sdf_polygon(xx: np.ndarray, yy: np.ndarray, cx: float, cy: float, r: float, sides: int = 6) -> np.ndarray:
    """Exact regular N-sided polygon SDF."""
    px = xx - cx
    py = yy - cy
    ang = np.arctan2(py, px)
    seg = 2.0 * np.pi / max(3, sides)
    a = np.floor(0.5 + ang / seg) * seg - ang
    dist = np.sqrt(px * px + py * py)
    return dist * np.cos(a) - r


def sdf_bezier(
    xx: np.ndarray,
    yy: np.ndarray,
    p0: Tuple[float, float],
    p1: Tuple[float, float],
    p2: Tuple[float, float],
    r: float,
    samples: int = 16,
) -> np.ndarray:
    """Quadratic Bezier tube SDF evaluated via piecewise capsules for organic tendrils and veins."""
    ts = np.linspace(0.0, 1.0, samples + 1, dtype=np.float32)
    pts_x = (1.0 - ts) ** 2 * p0[0] + 2.0 * (1.0 - ts) * ts * p1[0] + ts ** 2 * p2[0]
    pts_y = (1.0 - ts) ** 2 * p0[1] + 2.0 * (1.0 - ts) * ts * p1[1] + ts ** 2 * p2[1]
    d = np.full_like(xx, 1e9, dtype=np.float32)
    for i in range(samples):
        seg = sdf_capsule(xx, yy, float(pts_x[i]), float(pts_y[i]), float(pts_x[i + 1]), float(pts_y[i + 1]), r)
        d = np.minimum(d, seg)
    return d


def sdf_union(d1: np.ndarray, d2: np.ndarray) -> np.ndarray:
    return np.minimum(d1, d2)


def sdf_subtract(d1: np.ndarray, d2: np.ndarray) -> np.ndarray:
    """Subtract d2 from d1 (d1 \\ d2)."""
    return np.maximum(d1, -d2)


def sdf_intersect(d1: np.ndarray, d2: np.ndarray) -> np.ndarray:
    return np.maximum(d1, d2)


def sdf_onion(d: np.ndarray, thickness: float) -> np.ndarray:
    """Carve concentric shell rings from an SDF."""
    return np.abs(d) - thickness


def sdf_smooth_union(d1: np.ndarray, d2: np.ndarray, k: float = 0.1) -> np.ndarray:
    h = np.clip(0.5 + 0.5 * (d2 - d1) / max(1e-6, k), 0.0, 1.0)
    return d2 * (1.0 - h) + d1 * h - k * h * (1.0 - h)


def domain_warp(
    xx: np.ndarray,
    yy: np.ndarray,
    seed: int = 42,
    strength: float = 0.18,
    freq: float = 2.0,
) -> Tuple[np.ndarray, np.ndarray]:
    """Warp 2D coordinates using periodic toroidal fBm for organic seamless distortion."""
    h, w = xx.shape
    wx = toroidal_fbm(w, h, seed=seed, freq=freq) * 2.0 - 1.0
    wy = toroidal_fbm(w, h, seed=seed + 1000, freq=freq) * 2.0 - 1.0
    return xx + wx * strength, yy + wy * strength


def compute_sdf_normals(sdf: np.ndarray) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Derive normalized 3D surface normals (nx, ny, nz) via toroidal 3x3 Scharr operator (skimage.filters when available)."""
    scharr_pair = _get_skimage_scharr()
    if scharr_pair is not None:
        scharr_h, scharr_v = scharr_pair
        padded = np.pad(sdf.astype(np.float32), 1, mode="wrap")
        gx = scharr_v(padded)[1:-1, 1:-1]
        gy = scharr_h(padded)[1:-1, 1:-1]
    else:
        s_u = np.roll(sdf, 1, axis=0)
        s_d = np.roll(sdf, -1, axis=0)
        gx = (
            3.0 * (np.roll(s_u, -1, axis=1) - np.roll(s_u, 1, axis=1))
            + 10.0 * (np.roll(sdf, -1, axis=1) - np.roll(sdf, 1, axis=1))
            + 3.0 * (np.roll(s_d, -1, axis=1) - np.roll(s_d, 1, axis=1))
        ) / 32.0
        s_l = np.roll(sdf, 1, axis=1)
        s_r = np.roll(sdf, -1, axis=1)
        gy = (
            3.0 * (np.roll(s_l, -1, axis=0) - np.roll(s_l, 1, axis=0))
            + 10.0 * (np.roll(sdf, -1, axis=0) - np.roll(sdf, 1, axis=0))
            + 3.0 * (np.roll(s_r, -1, axis=0) - np.roll(s_r, 1, axis=0))
        ) / 32.0
    norm = np.sqrt(gx * gx + gy * gy + 1e-6)
    nx = gx / norm
    ny = gy / norm
    nz = np.sqrt(np.clip(1.0 - np.clip(sdf * -1.5, 0.0, 0.95) ** 2 * 0.4, 0.15, 1.0))
    n_len = np.sqrt(nx * nx + ny * ny + nz * nz + 1e-6)
    return (nx / n_len).astype(np.float32), (ny / n_len).astype(np.float32), (nz / n_len).astype(np.float32)


def compute_height_normals(height: np.ndarray, strength: float = 2.5) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Derive proportional tangent-space normals (nx, ny, nz) from a height field without flattening-noise blow-up."""
    h = height.astype(np.float32)
    scharr_pair = _get_skimage_scharr()
    if scharr_pair is not None:
        scharr_h, scharr_v = scharr_pair
        padded = np.pad(h, 1, mode="wrap")
        gx = scharr_v(padded)[1:-1, 1:-1] * 0.5
        gy = scharr_h(padded)[1:-1, 1:-1] * 0.5
    else:
        s_u = np.roll(h, 1, axis=0)
        s_d = np.roll(h, -1, axis=0)
        gx = (
            3.0 * (np.roll(s_u, -1, axis=1) - np.roll(s_u, 1, axis=1))
            + 10.0 * (np.roll(h, -1, axis=1) - np.roll(h, 1, axis=1))
            + 3.0 * (np.roll(s_d, -1, axis=1) - np.roll(s_d, 1, axis=1))
        ) / 32.0
        s_l = np.roll(h, 1, axis=1)
        s_r = np.roll(h, -1, axis=1)
        gy = (
            3.0 * (np.roll(s_l, -1, axis=0) - np.roll(s_l, 1, axis=0))
            + 10.0 * (np.roll(h, -1, axis=0) - np.roll(h, 1, axis=0))
            + 3.0 * (np.roll(s_r, -1, axis=0) - np.roll(s_r, 1, axis=0))
        ) / 32.0
    nx_raw = -gx * float(strength)
    ny_raw = -gy * float(strength)
    inv_len = 1.0 / np.sqrt(nx_raw * nx_raw + ny_raw * ny_raw + 1.0)
    return (nx_raw * inv_len).astype(np.float32), (ny_raw * inv_len).astype(np.float32), inv_len.astype(np.float32)


def frangi_vesselness_toroidal(field: np.ndarray, beta: float = 0.5, c: float = 0.25) -> np.ndarray:
    """Extract branching tubular hyphal filaments in [0, 1] via skimage.filters.frangi (with toroidal wrap)."""
    f = field.astype(np.float32)
    frangi_fn = _get_skimage_frangi()
    if frangi_fn is not None:
        pad = 4
        padded = np.pad(f, pad, mode="wrap")
        vessel = frangi_fn(padded, sigmas=(1.0, 2.0), beta=beta, black_ridges=False)[pad:-pad, pad:-pad].astype(np.float32)
        vmax = float(vessel.max())
        return (vessel / vmax).astype(np.float32) if vmax > 1e-6 else np.zeros_like(f)
    else:
        hxx = np.roll(f, -1, axis=1) - 2.0 * f + np.roll(f, 1, axis=1)
        hyy = np.roll(f, -1, axis=0) - 2.0 * f + np.roll(f, 1, axis=0)
        hxy = 0.25 * (
            np.roll(np.roll(f, -1, axis=0), -1, axis=1)
            - np.roll(np.roll(f, -1, axis=0), 1, axis=1)
            - np.roll(np.roll(f, 1, axis=0), -1, axis=1)
            + np.roll(np.roll(f, 1, axis=0), 1, axis=1)
        )
        trace = hxx + hyy
        disc = np.sqrt(np.maximum(0.0, (hxx - hyy) ** 2 + 4.0 * hxy * hxy))
        eig_a = 0.5 * (trace + disc)
        eig_b = 0.5 * (trace - disc)
        swap = np.abs(eig_a) > np.abs(eig_b)
        lam1 = np.where(swap, eig_b, eig_a)
        lam2 = np.where(swap, eig_a, eig_b)

        rb2 = (lam1 / np.where(np.abs(lam2) > 1e-5, lam2, 1e-5)) ** 2
        s2 = lam1 * lam1 + lam2 * lam2
        c_val = max(1e-4, float(np.percentile(np.sqrt(s2), 90)) * c)
        vessel = np.exp(-rb2 / (2.0 * beta * beta)) * (1.0 - np.exp(-s2 / (2.0 * c_val * c_val)))
        vessel = np.where(lam2 < 0.0, vessel, 0.0)
        vmax = float(vessel.max())
        return (vessel / vmax).astype(np.float32) if vmax > 1e-6 else np.zeros_like(f)


def poisson_disc_toroidal(
    w: int,
    h: int,
    min_dist: float,
    seed: int = 42,
    k: int = 20,
) -> Tuple[np.ndarray, np.ndarray]:
    """Bridson Poisson-disc sampling on a toroidal domain, returning (points Nx2, normalized distance field)."""
    rng = np.random.default_rng(seed)
    r = max(1.5, float(min_dist))
    points: list[tuple[float, float]] = [(float(rng.uniform(0, w)), float(rng.uniform(0, h)))]
    active = [0]

    def toroidal_dist2(ax: float, ay: float, bx: float, by: float) -> float:
        dx = abs(ax - bx)
        dx = min(dx, w - dx)
        dy = abs(ay - by)
        dy = min(dy, h - dy)
        return dx * dx + dy * dy

    r2 = r * r
    while active:
        idx = int(rng.integers(0, len(active)))
        px, py = points[active[idx]]
        found = False
        for _ in range(k):
            ang = float(rng.uniform(0.0, 2.0 * np.pi))
            rad = float(rng.uniform(r, 2.0 * r))
            nx = (px + np.cos(ang) * rad) % w
            ny = (py + np.sin(ang) * rad) % h
            if all(toroidal_dist2(nx, ny, qx, qy) >= r2 for qx, qy in points):
                points.append((float(nx), float(ny)))
                active.append(len(points) - 1)
                found = True
                break
        if not found:
            active.pop(idx)

    pts_arr = np.array(points, dtype=np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d_min = np.full((h, w), 1e9, dtype=np.float32)
    for qx, qy in points:
        dx = np.abs(xx - qx)
        dx = np.minimum(dx, w - dx)
        dy = np.abs(yy - qy)
        dy = np.minimum(dy, h - dy)
        d_min = np.minimum(d_min, np.sqrt(dx * dx + dy * dy))
    return pts_arr, np.clip(d_min / max(1e-4, r), 0.0, 1.0).astype(np.float32)


def meijering_neuriteness_toroidal(field: np.ndarray, sigmas: Tuple[float, ...] = (1.0, 2.0)) -> np.ndarray:
    """Extract fine synaptic/neuritic filaments in [0, 1] via skimage.filters.meijering (with toroidal wrap)."""
    f = field.astype(np.float32)
    meijering_fn = _get_skimage_meijering()
    if meijering_fn is not None:
        pad = 4
        padded = np.pad(f, pad, mode="wrap")
        neur = meijering_fn(padded, sigmas=sigmas, black_ridges=False)[pad:-pad, pad:-pad].astype(np.float32)
        vmax = float(neur.max())
        return (neur / vmax).astype(np.float32) if vmax > 1e-6 else np.zeros_like(f)
    return frangi_vesselness_toroidal(f)


def sato_ridge_toroidal(field: np.ndarray, sigmas: Tuple[float, ...] = (1.0, 2.0)) -> np.ndarray:
    """Extract continuous carapace ridges in [0, 1] via skimage.filters.sato (with toroidal wrap)."""
    f = field.astype(np.float32)
    sato_fn = _get_skimage_sato()
    if sato_fn is not None:
        pad = 4
        padded = np.pad(f, pad, mode="wrap")
        ridge = sato_fn(padded, sigmas=sigmas, black_ridges=False)[pad:-pad, pad:-pad].astype(np.float32)
        vmax = float(ridge.max())
        return (ridge / vmax).astype(np.float32) if vmax > 1e-6 else np.zeros_like(f)
    return frangi_vesselness_toroidal(f)


def wfc_overlap_toroidal(
    exemplar: np.ndarray,
    out_w: int,
    out_h: int,
    n: int = 2,
    seed: int = 42,
    symmetry: int = 1,
) -> np.ndarray:
    """Synthesize a seamless 2D pattern from an integer exemplar via D4-symmetric Shannon-entropy WFC."""
    rng = np.random.default_rng(seed)
    sym_count = max(1, min(8, int(symmetry)))
    variants = []
    cur = exemplar
    for i in range(4):
        variants.append(cur)
        variants.append(np.fliplr(cur))
        cur = np.rot90(cur)
    variants = variants[:sym_count]

    patterns: list[tuple[int, ...]] = []
    weights: list[int] = []
    pat_map: dict[tuple[int, ...], int] = {}

    for var in variants:
        vh, vw = var.shape[:2]
        for y in range(vh):
            for x in range(vw):
                patch = tuple(
                    int(var[(y + dy) % vh, (x + dx) % vw])
                    for dy in range(n)
                    for dx in range(n)
                )
                if patch not in pat_map:
                    pat_map[patch] = len(patterns)
                    patterns.append(patch)
                    weights.append(1)
                else:
                    weights[pat_map[patch]] += 1

    num_p = len(patterns)
    w_list = [float(w) for w in weights]
    w_log_w_list = [w * math.log(max(w, 1e-12)) for w in w_list]

    compat_r = [set() for _ in range(num_p)]
    compat_d = [set() for _ in range(num_p)]
    for i, pi in enumerate(patterns):
        for j, pj in enumerate(patterns):
            if all(pi[dy * n + dx + 1] == pj[dy * n + dx] for dy in range(n) for dx in range(n - 1)):
                compat_r[i].add(j)
            if all(pi[(dy + 1) * n + dx] == pj[dy * n + dx] for dy in range(n - 1) for dx in range(n)):
                compat_d[i].add(j)

    compat_l = [set() for _ in range(num_p)]
    compat_u = [set() for _ in range(num_p)]
    for i in range(num_p):
        for j in compat_r[i]:
            compat_l[j].add(i)
        for j in compat_d[i]:
            compat_u[j].add(i)

    def shannon_entropy(opts: set[int]) -> float:
        sw = 0.0
        swl = 0.0
        for p in opts:
            sw += w_list[p]
            swl += w_log_w_list[p]
        if sw <= 0.0:
            return 0.0
        return math.log(sw) - swl / sw + float(rng.uniform(0.0, 1e-5))

    eh, ew = exemplar.shape[:2]
    all_p = set(range(num_p))
    for _attempt in range(12):
        wave = [[set(all_p) for _ in range(out_w)] for _ in range(out_h)]
        failed = False
        while True:
            best = None
            best_ent = 1e18
            for y in range(out_h):
                for x in range(out_w):
                    c = len(wave[y][x])
                    if c == 0:
                        failed = True
                        break
                    if c > 1:
                        ent = shannon_entropy(wave[y][x])
                        if ent < best_ent:
                            best_ent = ent
                            best = (y, x)
                if failed:
                    break
            if failed or best is None:
                break

            by, bx = best
            opts = list(wave[by][bx])
            w_arr = np.array([weights[p] for p in opts], dtype=np.float64)
            w_arr /= w_arr.sum()
            chosen = int(rng.choice(opts, p=w_arr))
            wave[by][bx] = {chosen}

            stack = [(by, bx)]
            while stack and not failed:
                cy, cx = stack.pop()
                cur_set = wave[cy][cx]
                neighbors = (
                    (cy, (cx + 1) % out_w, compat_r),
                    (cy, (cx - 1) % out_w, compat_l),
                    ((cy + 1) % out_h, cx, compat_d),
                    ((cy - 1) % out_h, cx, compat_u),
                )
                for ny, nx, compat_table in neighbors:
                    allowed = set().union(*(compat_table[p] for p in cur_set))
                    before = len(wave[ny][nx])
                    wave[ny][nx] &= allowed
                    if not wave[ny][nx]:
                        failed = True
                        break
                    if len(wave[ny][nx]) < before:
                        stack.append((ny, nx))
        if not failed:
            out = np.zeros((out_h, out_w), dtype=exemplar.dtype)
            for y in range(out_h):
                for x in range(out_w):
                    pid = next(iter(wave[y][x]))
                    out[y, x] = patterns[pid][0]
            return out

    return np.tile(exemplar, (out_h // eh + 1, out_w // ew + 1))[:out_h, :out_w]


def wfc_simple_tiled(
    tiles: Sequence[str],
    sockets: dict[str, tuple[str, str, str, str]],
    out_w: int,
    out_h: int,
    seed: int = 42,
) -> list[list[str]]:
    """SimpleTiledModel WFC: place named tiles with (N, E, S, W) socket matching."""
    rng = np.random.default_rng(seed)
    n_tiles = len(tiles)
    all_t = set(range(n_tiles))
    compat_e = [{j for j in range(n_tiles) if sockets[tiles[i]][1] == sockets[tiles[j]][3]} for i in range(n_tiles)]
    compat_w = [{j for j in range(n_tiles) if sockets[tiles[i]][3] == sockets[tiles[j]][1]} for i in range(n_tiles)]
    compat_s = [{j for j in range(n_tiles) if sockets[tiles[i]][2] == sockets[tiles[j]][0]} for i in range(n_tiles)]
    compat_n = [{j for j in range(n_tiles) if sockets[tiles[i]][0] == sockets[tiles[j]][2]} for i in range(n_tiles)]

    for _attempt in range(12):
        wave = [[set(all_t) for _ in range(out_w)] for _ in range(out_h)]
        failed = False
        while True:
            best = None
            best_len = n_tiles + 1
            for y in range(out_h):
                for x in range(out_w):
                    c = len(wave[y][x])
                    if c == 0:
                        failed = True
                        break
                    if 1 < c < best_len:
                        best_len = c
                        best = (y, x)
                if failed:
                    break
            if failed or best is None:
                break
            by, bx = best
            chosen = int(rng.choice(list(wave[by][bx])))
            wave[by][bx] = {chosen}
            stack = [(by, bx)]
            while stack and not failed:
                cy, cx = stack.pop()
                cur_set = wave[cy][cx]
                dirs = [
                    (cy, cx + 1, compat_e),
                    (cy, cx - 1, compat_w),
                    (cy + 1, cx, compat_s),
                    (cy - 1, cx, compat_n),
                ]
                for ny, nx, table in dirs:
                    if 0 <= ny < out_h and 0 <= nx < out_w:
                        allowed = set().union(*(table[t] for t in cur_set))
                        before = len(wave[ny][nx])
                        wave[ny][nx] &= allowed
                        if not wave[ny][nx]:
                            failed = True
                            break
                        if len(wave[ny][nx]) < before:
                            stack.append((ny, nx))
        if not failed:
            return [[tiles[next(iter(wave[y][x]))] for x in range(out_w)] for y in range(out_h)]
    return [[tiles[0] for _ in range(out_w)] for _ in range(out_h)]


def fast_cellular_warped(
    w: int,
    h: int,
    seed: int = 42,
    cells: int = 8,
    warp: float = 0.15,
) -> Tuple[np.ndarray, np.ndarray]:
    """Toroidal FastNoiseLite-style domain-warped Cellular (F1, F2-F1 edge) fields in [0, 1]."""
    bevel, seam = cell_plate_field(w, h, seed=seed, cells=cells)
    wx = toroidal_fbm(w, h, seed=seed + 11, freq=2.0) * 2.0 - 1.0
    wy = toroidal_fbm(w, h, seed=seed + 29, freq=2.0) * 2.0 - 1.0
    shift_x = np.round(wx * warp * w).astype(np.int32)
    shift_y = np.round(wy * warp * h).astype(np.int32)
    yy, xx = np.mgrid[0:h, 0:w]
    wy_idx = (yy + shift_y) % h
    wx_idx = (xx + shift_x) % w
    return bevel[wy_idx, wx_idx], seam[wy_idx, wx_idx]


def compute_sdf_curvature(sdf: np.ndarray) -> np.ndarray:
    """Estimate normalized surface curvature in [-1, 1] via SDF Laplacian (convex ridges > 0, crevices < 0)."""
    lap = (
        np.roll(sdf, 1, axis=0)
        + np.roll(sdf, -1, axis=0)
        + np.roll(sdf, 1, axis=1)
        + np.roll(sdf, -1, axis=1)
        - 4.0 * sdf
    )
    scale = max(1e-5, float(np.percentile(np.abs(lap), 95)))
    return np.clip(lap / scale, -1.0, 1.0).astype(np.float32)


def compute_sdf_ao(sdf: np.ndarray, radius: float = 0.18) -> np.ndarray:
    """Approximate Ambient Occlusion in [0, 1] from SDF concavity/edge proximity."""
    edge_occl = np.clip(np.abs(sdf) / max(1e-5, radius), 0.25, 1.0)
    return np.where(sdf <= 0.0, np.clip(0.55 + 0.45 * edge_occl, 0.0, 1.0), 1.0)


def sdf_to_pom_height(sdf: np.ndarray, depth: float = 0.65, scale: float = 2.2, invert: bool = False) -> np.ndarray:
    """Convert an SDF into a normalized [1-depth, 1] Parallax Occlusion Mapping (POM) height field."""
    elev = np.clip(-sdf * scale, 0.0, 1.0)
    if invert:
        elev = 1.0 - elev
    return np.clip(1.0 - depth * (1.0 - elev), 0.06, 1.0).astype(np.float32)


def compute_sdf_lighting(sdf: np.ndarray, light_dir=(0.45, -0.55, 0.70), rim_strength: float = 0.25) -> np.ndarray:
    """Derive surface normals from SDF gradient and compute Lambertian + rim lighting in [0, 1]."""
    nx, ny, nz = compute_sdf_normals(sdf)
    ld = np.array(light_dir, dtype=np.float32)
    ld = ld / np.linalg.norm(ld)
    lambert = np.clip(nx * ld[0] + ny * ld[1] + nz * ld[2], 0.0, 1.0)
    rim = np.clip(1.0 - np.abs(sdf) * 2.5, 0.0, 1.0) * rim_strength
    return np.clip(lambert * 0.85 + rim + 0.1, 0.0, 1.0)


def compute_sdf_lighting_v2(
    sdf: np.ndarray,
    light_dir=(0.45, -0.55, 0.70),
    specular_power: float = 18.0,
    specular_strength: float = 0.35,
    sss_strength: float = 0.18,
    ao_strength: float = 0.30,
    rim_strength: float = 0.22,
) -> np.ndarray:
    """Blinn-Phong diffuse + specular + Ambient Occlusion + Subsurface Scattering in [0, 1]."""
    nx, ny, nz = compute_sdf_normals(sdf)
    ld = np.array(light_dir, dtype=np.float32)
    ld = ld / np.linalg.norm(ld)
    lambert = np.clip(nx * ld[0] + ny * ld[1] + nz * ld[2], 0.0, 1.0)

    # Halfway vector between light_dir and view_dir=(0, 0, 1)
    hv = ld + np.array([0.0, 0.0, 1.0], dtype=np.float32)
    hv = hv / np.linalg.norm(hv)
    ndoth = np.clip(nx * hv[0] + ny * hv[1] + nz * hv[2], 0.0, 1.0)
    specular = (ndoth ** specular_power) * specular_strength

    # Subsurface scattering peaks near thin/translucent borders
    sss = np.clip(1.0 - np.abs(sdf) * 3.0, 0.0, 1.0) * sss_strength
    ao = compute_sdf_ao(sdf)
    ao_factor = 1.0 - ao_strength * (1.0 - ao)
    rim = np.clip(1.0 - np.abs(sdf) * 2.5, 0.0, 1.0) * rim_strength

    lit = (lambert * 0.75 + sss + rim + 0.08) * ao_factor + specular
    return np.clip(lit, 0.0, 1.0)


def bayer_quantize(field: np.ndarray, ramp: np.ndarray, dither_strength: float = 0.24, matrix: int = 4) -> np.ndarray:
    """Quantize a [0, 1] scalar field into discrete RGBA ramp indices using Bayer 4x4 or 8x8 dithering."""
    h, w = field.shape
    bayer = BAYER_8X8 if matrix == 8 else BAYER_4X4
    m = bayer.shape[0]
    tiled = np.tile(bayer, (h // m + 1, w // m + 1))[:h, :w]
    dithered = np.clip(field + (tiled - 0.5) * dither_strength, 0.0, 0.9999)
    idx = (dithered * len(ramp)).astype(np.int32)
    return ramp[idx]


def floyd_steinberg_quantize(field: np.ndarray, ramp: np.ndarray) -> np.ndarray:
    """Quantize a [0, 1] scalar field onto a discrete RGBA ramp via Floyd-Steinberg error diffusion."""
    h, w = field.shape
    buf = np.clip(field.astype(np.float32), 0.0, 1.0).copy()
    n = len(ramp)
    idx_out = np.zeros((h, w), dtype=np.int32)
    for y in range(h):
        for x in range(w):
            old = float(np.clip(buf[y, x], 0.0, 0.9999))
            q_idx = int(old * n)
            new_val = (q_idx + 0.5) / n
            idx_out[y, x] = q_idx
            err = old - new_val
            if x + 1 < w:
                buf[y, x + 1] += err * (7.0 / 16.0)
            if y + 1 < h:
                if x > 0:
                    buf[y + 1, x - 1] += err * (3.0 / 16.0)
                buf[y + 1, x] += err * (5.0 / 16.0)
                if x + 1 < w:
                    buf[y + 1, x + 1] += err * (1.0 / 16.0)
    return ramp[idx_out]


def gray_scott_toroidal(
    w: int,
    h: int,
    feed: float = 0.055,
    kill: float = 0.062,
    steps: int = 180,
    seed: int = 42,
) -> np.ndarray:
    """Simulate seamless toroidal Gray-Scott reaction-diffusion, returning normalized V concentration in [0, 1]."""
    rng = np.random.default_rng(seed)
    u = np.ones((h, w), dtype=np.float32)
    v = np.zeros((h, w), dtype=np.float32)
    for _ in range(max(3, (w * h) // 160)):
        cx = int(rng.integers(0, w))
        cy = int(rng.integers(0, h))
        r = int(rng.integers(2, max(3, min(w, h) // 6)))
        yy, xx = np.mgrid[0:h, 0:w]
        dx = np.minimum(np.abs(xx - cx), w - np.abs(xx - cx))
        dy = np.minimum(np.abs(yy - cy), h - np.abs(yy - cy))
        mask = (dx * dx + dy * dy) <= r * r
        u[mask] = 0.50
        v[mask] = 0.25 + rng.uniform(0.0, 0.05)

    du, dv, dt = 0.16, 0.08, 1.0
    for _ in range(steps):
        lu = (
            0.20 * (np.roll(u, 1, 0) + np.roll(u, -1, 0) + np.roll(u, 1, 1) + np.roll(u, -1, 1))
            + 0.05 * (np.roll(np.roll(u, 1, 0), 1, 1) + np.roll(np.roll(u, 1, 0), -1, 1)
                      + np.roll(np.roll(u, -1, 0), 1, 1) + np.roll(np.roll(u, -1, 0), -1, 1))
            - u
        )
        lv = (
            0.20 * (np.roll(v, 1, 0) + np.roll(v, -1, 0) + np.roll(v, 1, 1) + np.roll(v, -1, 1))
            + 0.05 * (np.roll(np.roll(v, 1, 0), 1, 1) + np.roll(np.roll(v, 1, 0), -1, 1)
                      + np.roll(np.roll(v, -1, 0), 1, 1) + np.roll(np.roll(v, -1, 0), -1, 1))
            - v
        )
        uvv = u * v * v
        u = np.clip(u + (du * lu - uvv + feed * (1.0 - u)) * dt, 0.0, 1.0)
        v = np.clip(v + (dv * lv + uvv - (feed + kill) * v) * dt, 0.0, 1.0)

    v_min, v_max = float(v.min()), float(v.max())
    if v_max - v_min < 1e-6:
        return np.zeros((h, w), dtype=np.float32)
    return np.clip((v - v_min) / (v_max - v_min), 0.0, 1.0).astype(np.float32)


def toroidal_fbm(w: int, h: int, seed: int = 42, freq: float = 2.0) -> np.ndarray:
    """Generate seamless periodic 2D fBm noise in [0, 1] via trigonometric toroidal projection."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xx / w * 2.0 * np.pi
    v = yy / h * 2.0 * np.pi
    acc = np.zeros((h, w), dtype=np.float32)
    amp = 1.0
    total_amp = 0.0
    for oct_idx in range(3):
        f = freq * (2 ** oct_idx)
        p1, p2, p3, p4 = rng.uniform(0, 2 * np.pi, size=4)
        wave = (
            np.sin(f * u + p1) * np.cos(f * v + p2)
            + np.cos(f * (u + v) + p3) * 0.5
            + np.sin(f * (u - v) + p4) * 0.5
        )
        acc += wave * amp
        total_amp += amp
        amp *= 0.5
    acc = acc / total_amp
    return np.clip((acc - acc.min()) / max(1e-6, acc.max() - acc.min()), 0.0, 1.0)


def toroidal_value_noise(w: int, h: int, cells_x: int, cells_y: int, seed: int = 42) -> np.ndarray:
    """Generate seamless periodic 2D Hermite-interpolated value noise in [0, 1] without trig grid aliasing."""
    rng = np.random.default_rng(seed)
    cx = max(2, min(w, int(cells_x)))
    cy = max(2, min(h, int(cells_y)))
    grid = rng.uniform(0.0, 1.0, size=(cy, cx)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx = (xx / float(max(1, w))) * float(cx)
    fy = (yy / float(max(1, h))) * float(cy)
    ix = np.floor(fx).astype(np.int32)
    iy = np.floor(fy).astype(np.int32)
    tx = fx - ix
    ty = fy - iy
    x0 = ix % cx
    y0 = iy % cy
    x1 = (x0 + 1) % cx
    y1 = (y0 + 1) % cy
    sx = tx * tx * (3.0 - 2.0 * tx)
    sy = ty * ty * (3.0 - 2.0 * ty)
    v00 = grid[y0, x0]
    v10 = grid[y0, x1]
    v01 = grid[y1, x0]
    v11 = grid[y1, x1]
    nx0 = v00 * (1.0 - sx) + v10 * sx
    nx1 = v01 * (1.0 - sx) + v11 * sx
    return (nx0 * (1.0 - sy) + nx1 * sy).astype(np.float32)


def toroidal_value_fbm(
    w: int,
    h: int,
    seed: int = 42,
    base_cells: int = 4,
    octaves: int = 3,
    persistence: float = 0.5,
) -> np.ndarray:
    """Multi-octave seamless organic periodic value fBm normalized to [0, 1]."""
    acc = np.zeros((h, w), dtype=np.float32)
    amp = 1.0
    for i in range(max(1, int(octaves))):
        scale = 2 ** i
        acc += toroidal_value_noise(w, h, base_cells * scale, base_cells * scale, seed=seed + i * 97) * amp
        amp *= persistence
    lo = float(acc.min())
    hi = float(acc.max())
    if hi - lo < 1e-6:
        return np.full((h, w), 0.5, dtype=np.float32)
    return np.clip((acc - lo) / (hi - lo), 0.0, 1.0).astype(np.float32)


def ridged_fbm(w: int, h: int, seed: int = 42, freq: float = 4.0) -> np.ndarray:
    """Generate seamless ridged fBm in [0, 1] suitable for organic veins and cracks."""
    base = toroidal_fbm(w, h, seed=seed, freq=freq)
    ridged = 1.0 - np.abs(2.0 * base - 1.0)
    return np.clip(ridged ** 2.2, 0.0, 1.0)


def cell_plate_field(w: int, h: int, seed: int = 42, cells: int = 6) -> tuple[np.ndarray, np.ndarray]:
    """Generate toroidal Voronoi-like armored plate bevel field and seam mask in [0, 1]."""
    rng = np.random.default_rng(seed)
    pts_x = rng.uniform(0.0, float(w), size=cells)
    pts_y = rng.uniform(0.0, float(h), size=cells)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)

    d1 = np.full((h, w), 1e9, dtype=np.float32)
    d2 = np.full((h, w), 1e9, dtype=np.float32)
    for px, py in zip(pts_x, pts_y):
        dx = np.abs(xx - px)
        dx = np.minimum(dx, w - dx)
        dy = np.abs(yy - py)
        dy = np.minimum(dy, h - dy)
        dist = np.sqrt(dx * dx + dy * dy)
        closer = dist < d1
        d2 = np.where(closer, d1, np.minimum(d2, dist))
        d1 = np.where(closer, dist, d1)

    gap = d2 - d1
    max_gap = max(1.0, float(np.percentile(gap, 95)))
    bevel = np.clip(gap / max_gap, 0.0, 1.0)
    seam = np.clip(1.0 - gap / 2.2, 0.0, 1.0)
    return bevel, seam


def sdf_segment(xx: np.ndarray, yy: np.ndarray, ax: float, ay: float, bx: float, by: float) -> np.ndarray:
    """Unsigned distance to a 2D line segment."""
    pax, pay = xx - ax, yy - ay
    bax, bay = bx - ax, by - ay
    h = np.clip((pax * bax + pay * bay) / max(1e-6, bax * bax + bay * bay), 0.0, 1.0)
    return np.sqrt((pax - bax * h) ** 2 + (pay - bay * h) ** 2)


def add_pixel_outline(rgba: np.ndarray, outline_col=(18, 12, 28, 255)) -> np.ndarray:
    """Add a crisp 1px dark outline around non-transparent pixels."""
    alpha = rgba[..., 3] > 0
    padded = np.pad(alpha, 1, mode="constant", constant_values=False)
    neighbor = (
        padded[:-2, 1:-1] | padded[2:, 1:-1] | padded[1:-1, :-2] | padded[1:-1, 2:]
    )
    outline_mask = neighbor & (~alpha)
    out = rgba.copy()
    out[outline_mask] = np.array(outline_col, dtype=np.uint8)
    return out


def save_rgba(rgba: np.ndarray, path: str) -> None:
    Image.fromarray(rgba, mode="RGBA").save(path)


def save_flipbook(frames: Sequence[np.ndarray], path: str, frametime: int = 5, interpolate: bool = True) -> None:
    stacked = np.vstack(frames)
    Image.fromarray(stacked, mode="RGBA").save(path)
    meta = {
        "animation": {
            "frametime": frametime,
            "interpolate": interpolate,
            "frames": [0, 1, 2, 3, 2, 1],
        }
    }
    with open(path + ".mcmeta", "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)
