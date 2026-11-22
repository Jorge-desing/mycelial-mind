"""Procedural Mycelium, Mushrooms & Hive Mind GUI texture generator for the Codex Book (256x256 RGBA).

Generates `src/main/resources/assets/mycelialmind/textures/gui/codex_book.png`:
- (0, 0, 256, 200): Mycelial-fungal & Hive Mind double-spread tome frame:
  * Outer dark chitin carapace rim (`abyssal_obsidian` + `elite_carapace`) with inner mycelial violet bevel
    (`mycelial_violet`), glowing bioluminescent cyan/chartreuse perimeter axons, and 4 fungal shelf clusters
    in the corners (`_paint_fungal_corner_cluster`)
  * Left index membrane panel (`x=9..84, y=9..190`): abyssal indigo fungal vellum laced with branching
    mycelial hyphae & spore flecks in violet, chartreuse and cyan (`_paint_mycelial_network`)
  * Central synaptic spine (`x=85..91, y=7..192`): living amethyst neural cord banded with chitin rings
    and 7 glowing chartreuse/cyan synaptic ganglia
  * Right reading parchment spread (`x=92..246, y=9..190`): abyssal mycelial parchment with organic hyphal
    filigree around the margins and a violet-cyan header axon crowned by a glowing mushroom node
- (0, 204, 18, 18): Standard recipe slot recessed in chitin & violet mycelium
- (20, 204, 22, 22): Output / spotlight pedestal framed in chitin-bone with amethyst core & chartreuse rim
- (44, 204, 22, 16): Synthesis arrow conduit (`mycelial_violet` -> `spore_chartreuse` -> `biolum_cyan`)
- (68, 204, 20, 18): Chapter fungal cap tab (inactive, dark chitin/violet)
- (90, 204, 20, 18): Chapter fungal cap tab (active, predator amethyst with chartreuse edge & cyan glow spots)
- (112, 204, 16, 16): Hive Mind Mushroom Medallion (violet synaptic ring, chartreuse/cyan fungal cap, chitin stipe)
"""

from __future__ import annotations

import os
import numpy as np

from ..palette_oklab import PALETTES
from ..sdf_engine import BAYER_4X4, save_rgba, toroidal_value_noise


def _tile_bayer4(h: int, w: int) -> np.ndarray:
    return np.tile(BAYER_4X4, (h // 4 + 1, w // 4 + 1))[:h, :w].astype(np.float32)


def _sample_ramp(ramp_name: str, t: np.ndarray, dither_strength: float = 0.08) -> np.ndarray:
    ramp = PALETTES[ramp_name].astype(np.float32)
    h, w = t.shape
    bayer = (_tile_bayer4(h, w) - 0.5) * dither_strength
    idx = np.clip(((t + bayer) * 7.999).astype(np.int32), 0, 7)
    return ramp[idx]


def _paint_rect(
    canvas: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    ramp_name: str,
    tone_field: np.ndarray,
    dither: float = 0.07,
) -> None:
    sub = _sample_ramp(ramp_name, np.clip(tone_field, 0.0, 1.0), dither_strength=dither)
    canvas[y0 : y0 + h, x0 : x0 + w, :3] = sub[..., :3].astype(np.uint8)
    canvas[y0 : y0 + h, x0 : x0 + w, 3] = 255


def _draw_border(
    canvas: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    top_left_rgba: tuple[int, int, int, int],
    bottom_right_rgba: tuple[int, int, int, int],
) -> None:
    tl = np.array(top_left_rgba, dtype=np.uint8)
    br = np.array(bottom_right_rgba, dtype=np.uint8)
    canvas[y0, x0 : x0 + w] = tl
    canvas[y0 : y0 + h, x0] = tl
    canvas[y0 + h - 1, x0 : x0 + w] = br
    canvas[y0 : y0 + h, x0 + w - 1] = br


def _paint_fungal_corner_cluster(canvas: np.ndarray, cx: int, cy: int, flip_x: bool, flip_y: bool) -> None:
    """Paint bioluminescent fungal shelf clusters and chitin gills in the tome corners."""
    violet_hi = PALETTES["mycelial_violet"][6]
    violet_mid = PALETTES["mycelial_violet"][4]
    gill_bone = PALETTES["chitin_bone"][5]
    spore_glow = PALETTES["spore_chartreuse"][6]
    cyan_spot = PALETTES["biolum_cyan"][6]
    dark_edge = PALETTES["abyssal_obsidian"][0]
    for dy in range(10):
        for dx in range(10):
            dist = dx + dy
            in_cap = (dx < 3 and dy < 9) or (dy < 3 and dx < 9) or (dx < 7 and dy < 7 and dist <= 9)
            if in_cap:
                px = cx - dx if flip_x else cx + dx
                py = cy - dy if flip_y else cy + dy
                if 0 <= px < 256 and 0 <= py < 256:
                    if (dx == 2 and dy == 2) or (dx == 4 and dy == 1):
                        canvas[py, px] = cyan_spot
                    elif dx == 1 and dy == 4:
                        canvas[py, px] = spore_glow
                    elif dx == 0 or dy == 0:
                        canvas[py, px] = gill_bone
                    elif dist == 9 or dx == 8 or dy == 8:
                        canvas[py, px] = dark_edge
                    elif dx <= 2 or dy <= 2:
                        canvas[py, px] = violet_hi
                    else:
                        canvas[py, px] = violet_mid


def _paint_mycelial_network(
    canvas: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    noise_field: np.ndarray,
) -> None:
    """Etch organic branching hyphae & glowing spores along the parchment margins."""
    hypha_violet = PALETTES["mycelial_violet"][5]
    spore_green = PALETTES["spore_chartreuse"][5]
    synapse_cyan = PALETTES["biolum_cyan"][6]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    edge_prox = np.clip(1.0 - np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)) / 14.0, 0.0, 1.0)
    vein_wave = np.abs(np.sin(xx * 0.30 + noise_field * 4.2) - np.cos(yy * 0.25 - noise_field * 3.8))
    hypha_mask = (vein_wave < 0.13) & (edge_prox > 0.25)
    spore_mask = (noise_field > 0.85) & (edge_prox > 0.40)
    node_mask = (noise_field > 0.91) & (edge_prox > 0.55)

    sub = canvas[y0 : y0 + h, x0 : x0 + w]
    sub[hypha_mask] = (sub[hypha_mask].astype(np.float32) * 0.58 + hypha_violet.astype(np.float32) * 0.42).astype(np.uint8)
    sub[spore_mask] = (sub[spore_mask].astype(np.float32) * 0.62 + spore_green.astype(np.float32) * 0.38).astype(np.uint8)
    sub[node_mask] = (sub[node_mask].astype(np.float32) * 0.45 + synapse_cyan.astype(np.float32) * 0.55).astype(np.uint8)


def generate_gui_textures(gui_dir: str) -> str:
    """Generate the 256x256 procedural Mycelium, Mushrooms & Hive Mind GUI texture atlas."""
    os.makedirs(gui_dir, exist_ok=True)
    canvas = np.zeros((256, 256, 4), dtype=np.uint8)

    noise_256 = toroidal_value_noise(256, 256, 16, 16, seed=404).astype(np.float32)
    fine_256 = toroidal_value_noise(256, 256, 32, 32, seed=405).astype(np.float32)

    # 1. Outer Chitin Carapace & Mycelial Violet Tome Frame (0, 0, 256, 200)
    book_w, book_h = 256, 200
    yy, xx = np.mgrid[0:book_h, 0:book_w].astype(np.float32)
    edge_dist = np.minimum(np.minimum(xx, book_w - 1 - xx), np.minimum(yy, book_h - 1 - yy))
    frame_tone = 0.20 + 0.15 * noise_256[:book_h, :book_w] + 0.08 * np.clip(edge_dist / 8.0, 0.0, 1.0)
    _paint_rect(canvas, 0, 0, book_w, book_h, "abyssal_obsidian", frame_tone, dither=0.06)

    # Inner mycelial violet fungal cap rim (3px inset)
    rim_tone = 0.32 + 0.16 * noise_256[3 : book_h - 3, 3 : book_w - 3]
    _paint_rect(canvas, 3, 3, book_w - 6, book_h - 6, "mycelial_violet", rim_tone, dither=0.06)
    _draw_border(canvas, 0, 0, book_w, book_h, (94, 78, 126, 255), (14, 10, 22, 255))
    _draw_border(canvas, 3, 3, book_w - 6, book_h - 6, (164, 142, 180, 255), (34, 22, 48, 255))

    # Glowing bioluminescent cyan-chartreuse synaptic axon perimeter (inset 6px)
    axon_col = PALETTES["biolum_cyan"][5]
    canvas[6, 10 : book_w - 10] = axon_col
    canvas[book_h - 7, 10 : book_w - 10] = axon_col
    canvas[10 : book_h - 10, 6] = axon_col
    canvas[10 : book_h - 10, book_w - 7] = axon_col
    for gx in range(28, book_w - 24, 32):
        canvas[6, gx : gx + 2] = PALETTES["spore_chartreuse"][6]
        canvas[book_h - 7, gx : gx + 2] = PALETTES["spore_chartreuse"][6]

    # 2. Left Index Vellum Panel (x=9..84, y=9..190 -> w=76, h=182)
    lx, ly, lw, lh = 9, 9, 76, 182
    lyy, lxx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    l_vignette = np.minimum(
        np.minimum(lxx, lw - 1 - lxx) / 10.0,
        np.minimum(lyy, lh - 1 - lyy) / 12.0,
    )
    l_tone = (
        0.16
        + 0.16 * np.clip(l_vignette, 0.0, 1.0)
        + 0.10 * noise_256[ly : ly + lh, lx : lx + lw]
        + 0.05 * fine_256[ly : ly + lh, lx : lx + lw]
    )
    _paint_rect(canvas, lx, ly, lw, lh, "abyssal_indigo", l_tone, dither=0.06)
    _paint_mycelial_network(canvas, lx, ly, lw, lh, noise_256[ly : ly + lh, lx : lx + lw])
    _draw_border(canvas, lx, ly, lw, lh, (32, 22, 52, 255), (124, 92, 168, 255))

    # 3. Central Hive Mind Synaptic Spine Seam (x=85..91, y=7..192 -> w=7, h=186)
    sx, sy, sw, sh = 85, 7, 7, 186
    syy, sxx = np.mgrid[0:sh, 0:sw].astype(np.float32)
    spine_profile = 1.0 - np.abs((sxx - 3.0) / 3.5)
    rib_rings = 0.5 + 0.5 * np.sin(syy * 0.52)
    s_tone = 0.22 + 0.34 * spine_profile + 0.14 * rib_rings
    _paint_rect(canvas, sx, sy, sw, sh, "predator_amethyst", s_tone, dither=0.05)
    for node_y in range(18, sh - 14, 24):
        canvas[sy + node_y - 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][5]
        canvas[sy + node_y + 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][4]
        canvas[sy + node_y - 1 : sy + node_y + 2, sx + 1 : sx + 6] = PALETTES["spore_chartreuse"][6]
        canvas[sy + node_y, sx + 3] = PALETTES["biolum_cyan"][7]

    # 4. Right Mycelial Parchment Reading Spread (x=92..246, y=9..190 -> w=155, h=182)
    rx, ry, rw, rh = 92, 9, 155, 182
    ryy, rxx = np.mgrid[0:rh, 0:rw].astype(np.float32)
    r_vignette = np.minimum(
        np.minimum(rxx, rw - 1 - rxx) / 14.0,
        np.minimum(ryy, rh - 1 - ryy) / 14.0,
    )
    spine_shadow = np.clip(rxx / 10.0, 0.0, 1.0)
    r_tone = (
        0.15
        + 0.18 * np.clip(r_vignette, 0.0, 1.0) * spine_shadow
        + 0.10 * noise_256[ry : ry + rh, rx : rx + rw]
        + 0.05 * fine_256[ry : ry + rh, rx : rx + rw]
    )
    _paint_rect(canvas, rx, ry, rw, rh, "abyssal_obsidian", r_tone, dither=0.06)
    _paint_mycelial_network(canvas, rx, ry, rw, rh, noise_256[ry : ry + rh, rx : rx + rw])
    _draw_border(canvas, rx, ry, rw, rh, (34, 24, 54, 255), (140, 106, 188, 255))

    # Organic header axon divider crowned by a glowing fungal mushroom node
    canvas[ry + 22, rx + 8 : rx + rw - 8] = PALETTES["mycelial_violet"][5]
    canvas[ry + 23, rx + 8 : rx + rw - 8] = PALETTES["abyssal_obsidian"][1]
    canvas[ry + 21, rx + rw // 2 - 2 : rx + rw // 2 + 3] = PALETTES["spore_chartreuse"][6]
    canvas[ry + 22, rx + rw // 2 - 3 : rx + rw // 2 + 4] = PALETTES["biolum_cyan"][6]
    canvas[ry + 23, rx + rw // 2] = PALETTES["chitin_bone"][6]

    # 5. Four Fungal Shelf Clusters in the Corners
    _paint_fungal_corner_cluster(canvas, 2, 2, flip_x=False, flip_y=False)
    _paint_fungal_corner_cluster(canvas, book_w - 3, 2, flip_x=True, flip_y=False)
    _paint_fungal_corner_cluster(canvas, 2, book_h - 3, flip_x=False, flip_y=True)
    _paint_fungal_corner_cluster(canvas, book_w - 3, book_h - 3, flip_x=True, flip_y=True)

    # 6. UI Widgets Row (y=204..255)
    # 6a. Standard 18x18 recipe cavity framed in chitin & violet mycelium at (0, 204)
    slot_tone = 0.18 + 0.12 * fine_256[204:222, 0:18]
    _paint_rect(canvas, 0, 204, 18, 18, "abyssal_obsidian", slot_tone, dither=0.04)
    _draw_border(canvas, 0, 204, 18, 18, (140, 108, 184, 255), (38, 24, 58, 255))
    _draw_border(canvas, 1, 205, 16, 16, (20, 14, 34, 255), (88, 68, 128, 255))

    # 6b. Output / Spotlight 22x22 chitin-bone & living amethyst pedestal at (20, 204)
    out_tone = 0.28 + 0.15 * fine_256[204:226, 20:42]
    _paint_rect(canvas, 20, 204, 22, 22, "predator_amethyst", out_tone, dither=0.05)
    _draw_border(
        canvas,
        20,
        204,
        22,
        22,
        tuple(int(c) for c in PALETTES["chitin_bone"][6]),
        tuple(int(c) for c in PALETTES["chitin_bone"][2]),
    )
    _draw_border(
        canvas,
        22,
        206,
        18,
        18,
        (24, 16, 40, 255),
        tuple(int(c) for c in PALETTES["biolum_cyan"][6]),
    )

    # 6c. Synthesis Hyphal Arrow 22x16 at (44, 204)
    ax0, ay0 = 44, 204
    shaft_col = PALETTES["spore_chartreuse"][5]
    glow_col = PALETTES["biolum_cyan"][6]
    edge_col = PALETTES["abyssal_obsidian"][0]
    canvas[ay0 + 6 : ay0 + 10, ax0 + 2 : ax0 + 15] = shaft_col
    canvas[ay0 + 7 : ay0 + 9, ax0 + 3 : ax0 + 15] = glow_col
    canvas[ay0 + 5, ax0 + 2 : ax0 + 14] = edge_col
    canvas[ay0 + 10, ax0 + 2 : ax0 + 14] = edge_col
    for step in range(6):
        top_y = ay0 + 3 + step
        bot_y = ay0 + 12 - step
        px = ax0 + 13 + step
        if top_y <= bot_y and px < ax0 + 20:
            canvas[top_y : bot_y + 1, px] = shaft_col
            if top_y + 1 <= bot_y - 1:
                canvas[top_y + 1 : bot_y, px] = glow_col

    # 6d. Chapter Fungal Cap Ribbon Tab Inactive (20x18 at 68, 204) & Active (20x18 at 90, 204)
    tab_in_tone = 0.24 + 0.14 * fine_256[204:222, 68:88]
    _paint_rect(canvas, 68, 204, 20, 18, "elite_carapace", tab_in_tone, dither=0.05)
    _draw_border(canvas, 68, 204, 20, 18, (118, 98, 152, 255), (26, 18, 40, 255))

    tab_ac_tone = 0.50 + 0.16 * fine_256[204:222, 90:110]
    _paint_rect(canvas, 90, 204, 20, 18, "mycelial_violet", tab_ac_tone, dither=0.05)
    _draw_border(
        canvas,
        90,
        204,
        20,
        18,
        tuple(int(c) for c in PALETTES["spore_chartreuse"][6]),
        tuple(int(c) for c in PALETTES["biolum_cyan"][5]),
    )
    canvas[207, 94] = PALETTES["biolum_cyan"][6]
    canvas[208, 104] = PALETTES["spore_chartreuse"][6]

    # 6e. Hive Mind Mushroom Medallion 16x16 at (112, 204)
    my, mx = np.mgrid[0:16, 0:16].astype(np.float32)
    mdist = np.sqrt((mx - 7.5) ** 2 + (my - 7.5) ** 2)
    ring_mask = mdist <= 7.2
    cap_mask = (mdist <= 5.2) & (my <= 8.5)
    stipe_mask = (np.abs(mx - 7.5) <= 1.6) & (my >= 8.0) & (my <= 13.0)
    med = np.zeros((16, 16, 4), dtype=np.uint8)
    med[ring_mask] = PALETTES["mycelial_violet"][4]
    med[cap_mask] = PALETTES["spore_chartreuse"][6]
    med[stipe_mask] = PALETTES["chitin_bone"][6]
    med[5, 5] = PALETTES["biolum_cyan"][7]
    med[6, 10] = PALETTES["biolum_cyan"][7]
    canvas[204:220, 112:128] = med

    out_path = os.path.join(gui_dir, "codex_book.png")
    save_rgba(canvas, out_path)
    return out_path
