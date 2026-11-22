"""Procedural GUI texture generator for the Mycelial Codex Book (256x256 RGBA atlas).

Generates `src/main/resources/assets/mycelialmind/textures/gui/codex_book.png`:
- (0, 0, 256, 200): Double-spread chitin & bioluminescent tome frame:
  * Outer chitin carapace bevelled rim with abyssal obsidian shadow and imperial brass corner brackets
  * Left index vellum panel (x=10..85, y=12..188): dark spore-stained mycelium parchment with subtle vein filigree
  * Central synaptic spine seam (x=86..91, y=8..192): ribbed neural cord with glowing violet-chartreuse nodes
  * Right reading parchment spread (x=92..246, y=12..188): warm organic spore-vellum with aged margin vignette and header rule
- (0, 204, 18, 18): Standard recipe slot frame (chitin bevel + dark recessed well)
- (20, 204, 22, 22): Output / spotlight pedestal frame (imperial brass + bioluminescent amethyst corner accents)
- (44, 204, 22, 16): Synthesis / crafting arrow sprite (glowing chartreuse-cyan neural conduit arrow)
- (68, 204, 20, 18): Chapter ribbon tab (inactive, dark chitin)
- (90, 204, 20, 18): Chapter ribbon tab (active, glowing amethyst-gold)
- (112, 204, 16, 16): Entry status badge / synaptic seal medallion
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


def _paint_corner_clasp(canvas: np.ndarray, cx: int, cy: int, flip_x: bool, flip_y: bool) -> None:
    brass_hi = PALETTES["imperial_brass"][6]
    brass_mid = PALETTES["imperial_brass"][4]
    brass_lo = PALETTES["imperial_brass"][1]
    gem = PALETTES["spore_chartreuse"][6]
    for dy in range(10):
        for dx in range(10):
            if (dx < 3 and dy < 10) or (dy < 3 and dx < 10) or (dx < 5 and dy < 5):
                px = cx - dx if flip_x else cx + dx
                py = cy - dy if flip_y else cy + dy
                if 0 <= px < 256 and 0 <= py < 256:
                    if dx == 2 and dy == 2:
                        canvas[py, px] = gem
                    elif dx == 0 or dy == 0:
                        canvas[py, px] = brass_hi
                    elif dx == 9 or dy == 9 or (dx == 4 and dy == 4):
                        canvas[py, px] = brass_lo
                    else:
                        canvas[py, px] = brass_mid


def generate_gui_textures(gui_dir: str) -> str:
    """Generate the 256x256 procedural Codex GUI texture atlas."""
    os.makedirs(gui_dir, exist_ok=True)
    canvas = np.zeros((256, 256, 4), dtype=np.uint8)

    noise_256 = toroidal_value_noise(256, 256, 16, 16, seed=404).astype(np.float32)
    fine_256 = toroidal_value_noise(256, 256, 32, 32, seed=405).astype(np.float32)

    # 1. Outer chitin tome frame (0, 0, 256, 200)
    book_w, book_h = 256, 200
    yy, xx = np.mgrid[0:book_h, 0:book_w].astype(np.float32)
    edge_dist = np.minimum(np.minimum(xx, book_w - 1 - xx), np.minimum(yy, book_h - 1 - yy))
    frame_tone = 0.22 + 0.14 * noise_256[:book_h, :book_w] + 0.08 * np.clip(edge_dist / 8.0, 0.0, 1.0)
    _paint_rect(canvas, 0, 0, book_w, book_h, "abyssal_obsidian", frame_tone, dither=0.06)

    # Chitin inner rim (3px inset)
    rim_tone = 0.34 + 0.16 * noise_256[3 : book_h - 3, 3 : book_w - 3]
    _paint_rect(canvas, 3, 3, book_w - 6, book_h - 6, "elite_carapace", rim_tone, dither=0.06)
    _draw_border(canvas, 0, 0, book_w, book_h, (92, 74, 122, 255), (14, 10, 22, 255))
    _draw_border(canvas, 3, 3, book_w - 6, book_h - 6, (148, 115, 188, 255), (28, 18, 42, 255))

    # Glowing synaptic circuit trace along the frame perimeter (inset 6px)
    circuit_col = PALETTES["mycelial_violet"][5]
    canvas[6, 10 : book_w - 10] = circuit_col
    canvas[book_h - 7, 10 : book_w - 10] = circuit_col
    canvas[10 : book_h - 10, 6] = circuit_col
    canvas[10 : book_h - 10, book_w - 7] = circuit_col

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
        + 0.09 * noise_256[ly : ly + lh, lx : lx + lw]
        + 0.05 * fine_256[ly : ly + lh, lx : lx + lw]
    )
    _paint_rect(canvas, lx, ly, lw, lh, "abyssal_indigo", l_tone, dither=0.06)
    _draw_border(canvas, lx, ly, lw, lh, (30, 20, 48, 255), (112, 78, 160, 255))

    # 3. Central Synaptic Spine Seam (x=85..91, y=7..192 -> w=7, h=186)
    sx, sy, sw, sh = 85, 7, 7, 186
    syy, sxx = np.mgrid[0:sh, 0:sw].astype(np.float32)
    spine_profile = 1.0 - np.abs((sxx - 3.0) / 3.5)
    spine_ribs = 0.5 + 0.5 * np.sin(syy * 0.55)
    s_tone = 0.20 + 0.35 * spine_profile + 0.14 * spine_ribs
    _paint_rect(canvas, sx, sy, sw, sh, "predator_amethyst", s_tone, dither=0.05)
    for node_y in range(18, sh - 14, 24):
        canvas[sy + node_y - 1 : sy + node_y + 2, sx + 2 : sx + 5] = PALETTES["spore_chartreuse"][6]
        canvas[sy + node_y, sx + 3] = PALETTES["warm_ivory"][7]

    # 4. Right Reading Parchment Spread (x=92..246, y=9..190 -> w=155, h=182)
    rx, ry, rw, rh = 92, 9, 155, 182
    ryy, rxx = np.mgrid[0:rh, 0:rw].astype(np.float32)
    r_vignette = np.minimum(
        np.minimum(rxx, rw - 1 - rxx) / 14.0,
        np.minimum(ryy, rh - 1 - ryy) / 14.0,
    )
    # Spine shadow on left edge of right page
    spine_shadow = np.clip(rxx / 9.0, 0.0, 1.0)
    r_tone = (
        0.14
        + 0.18 * np.clip(r_vignette, 0.0, 1.0) * spine_shadow
        + 0.10 * noise_256[ry : ry + rh, rx : rx + rw]
        + 0.05 * fine_256[ry : ry + rh, rx : rx + rw]
    )
    _paint_rect(canvas, rx, ry, rw, rh, "abyssal_obsidian", r_tone, dither=0.06)
    _draw_border(canvas, rx, ry, rw, rh, (32, 22, 50, 255), (132, 94, 186, 255))

    # Decorative header rule and subtle corner filigree inside reading page
    canvas[ry + 22, rx + 8 : rx + rw - 8] = PALETTES["mycelial_violet"][4]
    canvas[ry + 23, rx + 8 : rx + rw - 8] = PALETTES["abyssal_obsidian"][1]
    canvas[ry + 22, rx + rw // 2 - 2 : rx + rw // 2 + 3] = PALETTES["spore_chartreuse"][6]

    # 5. Four Brass & Bioluminescent Corner Clasps on main tome frame
    _paint_corner_clasp(canvas, 2, 2, flip_x=False, flip_y=False)
    _paint_corner_clasp(canvas, book_w - 3, 2, flip_x=True, flip_y=False)
    _paint_corner_clasp(canvas, 2, book_h - 3, flip_x=False, flip_y=True)
    _paint_corner_clasp(canvas, book_w - 3, book_h - 3, flip_x=True, flip_y=True)

    # 6. UI Widgets Row (y=204..255)
    # 6a. Standard 18x18 recipe slot frame at (0, 204)
    slot_tone = 0.18 + 0.12 * fine_256[204:222, 0:18]
    _paint_rect(canvas, 0, 204, 18, 18, "abyssal_obsidian", slot_tone, dither=0.04)
    _draw_border(canvas, 0, 204, 18, 18, (130, 96, 178, 255), (42, 28, 66, 255))
    _draw_border(canvas, 1, 205, 16, 16, (20, 14, 32, 255), (86, 62, 122, 255))

    # 6b. Output / Spotlight 22x22 pedestal frame at (20, 204)
    out_tone = 0.24 + 0.15 * fine_256[204:226, 20:42]
    _paint_rect(canvas, 20, 204, 22, 22, "predator_amethyst", out_tone, dither=0.05)
    _draw_border(canvas, 20, 204, 22, 22, tuple(int(c) for c in PALETTES["imperial_brass"][6]), tuple(int(c) for c in PALETTES["imperial_brass"][2]))
    _draw_border(canvas, 22, 206, 18, 18, (24, 16, 38, 255), tuple(int(c) for c in PALETTES["spore_chartreuse"][5]))

    # 6c. Synthesis / Crafting Arrow 22x16 at (44, 204)
    ax0, ay0 = 44, 204
    shaft_col = PALETTES["spore_chartreuse"][5]
    glow_col = PALETTES["biolum_cyan"][6]
    edge_col = PALETTES["abyssal_obsidian"][1]
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

    # 6d. Chapter Ribbon Tab Inactive (20x18 at 68, 204) & Active (20x18 at 90, 204)
    tab_in_tone = 0.28 + 0.12 * fine_256[204:222, 68:88]
    _paint_rect(canvas, 68, 204, 20, 18, "elite_carapace", tab_in_tone, dither=0.05)
    _draw_border(canvas, 68, 204, 20, 18, (112, 92, 142, 255), (26, 18, 38, 255))

    tab_ac_tone = 0.48 + 0.16 * fine_256[204:222, 90:110]
    _paint_rect(canvas, 90, 204, 20, 18, "mycelial_violet", tab_ac_tone, dither=0.05)
    _draw_border(canvas, 90, 204, 20, 18, tuple(int(c) for c in PALETTES["spore_chartreuse"][6]), tuple(int(c) for c in PALETTES["imperial_brass"][4]))

    # 6e. Synaptic Seal Medallion 16x16 at (112, 204)
    my, mx = np.mgrid[0:16, 0:16].astype(np.float32)
    mdist = np.sqrt((mx - 7.5) ** 2 + (my - 7.5) ** 2)
    ring_mask = mdist <= 7.0
    core_mask = mdist <= 3.5
    med = np.zeros((16, 16, 4), dtype=np.uint8)
    med[ring_mask] = PALETTES["imperial_brass"][5]
    med[core_mask] = PALETTES["spore_chartreuse"][6]
    canvas[204:220, 112:128] = med

    out_path = os.path.join(gui_dir, "codex_book.png")
    save_rgba(canvas, out_path)
    return out_path
