"""Procedural bio-horror GUI texture generator for the Mycelial Codex Book (256x256 RGBA).

Generates `src/main/resources/assets/mycelialmind/textures/gui/codex_book.png`:
- (0, 0, 256, 200): Eldritch mycelial-chitin double-spread grimoire frame:
  * Outer necrotic carapace rim (`abyssal_obsidian` + `necrotic_umber`) with arterial crimson-violet veins
    and jagged chitin-bone corner claws cradling glowing spore pustules
  * Left index hide panel (`x=9..84, y=9..190`): spore-blighted dark membrane with creeping hyphal tendrils
  * Central vertebral spine (`x=85..91, y=7..192`): bone-and-sinew cord studded with unblinking Hive Eyes
  * Right reading parchment spread (`x=92..246, y=9..190`): dark fungal parchment stained with necrotic mold,
    corner mycelium creeping inward, and an organic header rule crowned by a crimson-chartreuse eye
- (0, 204, 18, 18): Standard recipe slot cavity (chitin bevel + crimson tendinous rim)
- (20, 204, 22, 22): Output / spotlight pedestal frame (bone-chitin fangs + toxic spore inner glow)
- (44, 204, 22, 16): Synthesis arrow (pulsing chartreuse-crimson neural tendril conduit)
- (68, 204, 20, 18): Chapter fungal cap tab (inactive, necrotic cap)
- (90, 204, 20, 18): Chapter fungal cap tab (active, awakened crimson-violet tissue + chartreuse rim)
- (112, 204, 16, 16): Hive Mind Eye medallion (crimson sclera, chartreuse iris, vertical slit pupil)
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


def _paint_horror_corner_claw(canvas: np.ndarray, cx: int, cy: int, flip_x: bool, flip_y: bool) -> None:
    """Paint jagged bone-chitin corner claws clutching a bioluminescent spore eye pustule."""
    bone_hi = PALETTES["chitin_bone"][6]
    bone_mid = PALETTES["chitin_bone"][4]
    bone_dark = PALETTES["chitin_bone"][1]
    crimson = PALETTES["leech_crimson"][5]
    pustule = PALETTES["spore_chartreuse"][6]
    for dy in range(10):
        for dx in range(10):
            # Sharp L-claw + diagonal talon prong
            is_claw = (
                (dx < 3 and dy < 10)
                or (dy < 3 and dx < 10)
                or (dx < 6 and dy < 6 and (dx + dy <= 8))
            )
            if is_claw:
                px = cx - dx if flip_x else cx + dx
                py = cy - dy if flip_y else cy + dy
                if 0 <= px < 256 and 0 <= py < 256:
                    if dx == 2 and dy == 2:
                        canvas[py, px] = pustule
                    elif (dx == 3 and dy == 2) or (dx == 2 and dy == 3):
                        canvas[py, px] = crimson
                    elif dx == 0 or dy == 0:
                        canvas[py, px] = bone_hi
                    elif dx == 9 or dy == 9 or dx + dy == 8:
                        canvas[py, px] = bone_dark
                    else:
                        canvas[py, px] = bone_mid


def _paint_hyphal_tendrils(
    canvas: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    noise_field: np.ndarray,
) -> None:
    """Etch creepy branching mycelial/fungal veins along the dark edges of a parchment panel."""
    hypha_col = PALETTES["leech_crimson"][3]
    spore_speck = PALETTES["spore_chartreuse"][5]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    edge_prox = np.clip(1.0 - np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)) / 14.0, 0.0, 1.0)
    vein_wave = np.abs(np.sin((xx * 0.28 + noise_field * 4.5)) - np.cos((yy * 0.24 - noise_field * 4.0)))
    vein_mask = (vein_wave < 0.14) & (edge_prox > 0.28)
    speck_mask = (noise_field > 0.84) & (edge_prox > 0.45)

    sub = canvas[y0 : y0 + h, x0 : x0 + w]
    sub[vein_mask] = (sub[vein_mask].astype(np.float32) * 0.55 + hypha_col.astype(np.float32) * 0.45).astype(np.uint8)
    sub[speck_mask] = (sub[speck_mask].astype(np.float32) * 0.65 + spore_speck.astype(np.float32) * 0.35).astype(np.uint8)


def generate_gui_textures(gui_dir: str) -> str:
    """Generate the 256x256 procedural bio-horror Codex GUI texture atlas."""
    os.makedirs(gui_dir, exist_ok=True)
    canvas = np.zeros((256, 256, 4), dtype=np.uint8)

    noise_256 = toroidal_value_noise(256, 256, 16, 16, seed=404).astype(np.float32)
    fine_256 = toroidal_value_noise(256, 256, 32, 32, seed=405).astype(np.float32)

    # 1. Outer Necrotic Carapace & Fungal Hide Tome Frame (0, 0, 256, 200)
    book_w, book_h = 256, 200
    yy, xx = np.mgrid[0:book_h, 0:book_w].astype(np.float32)
    edge_dist = np.minimum(np.minimum(xx, book_w - 1 - xx), np.minimum(yy, book_h - 1 - yy))
    frame_tone = 0.18 + 0.16 * noise_256[:book_h, :book_w] + 0.08 * np.clip(edge_dist / 8.0, 0.0, 1.0)
    _paint_rect(canvas, 0, 0, book_w, book_h, "abyssal_obsidian", frame_tone, dither=0.06)

    # Inner necrotic umber flesh-leather bevel (3px inset)
    rim_tone = 0.28 + 0.18 * noise_256[3 : book_h - 3, 3 : book_w - 3]
    _paint_rect(canvas, 3, 3, book_w - 6, book_h - 6, "necrotic_umber", rim_tone, dither=0.07)
    _draw_border(canvas, 0, 0, book_w, book_h, (98, 52, 64, 255), (12, 8, 16, 255))
    _draw_border(canvas, 3, 3, book_w - 6, book_h - 6, (164, 152, 138, 255), (34, 14, 20, 255))

    # Pulsing arterial crimson vein perimeter (inset 6px)
    vein_col = PALETTES["leech_crimson"][5]
    canvas[6, 10 : book_w - 10] = vein_col
    canvas[book_h - 7, 10 : book_w - 10] = vein_col
    canvas[10 : book_h - 10, 6] = vein_col
    canvas[10 : book_h - 10, book_w - 7] = vein_col

    # 2. Left Index Membrane Panel (x=9..84, y=9..190 -> w=76, h=182)
    lx, ly, lw, lh = 9, 9, 76, 182
    lyy, lxx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    l_vignette = np.minimum(
        np.minimum(lxx, lw - 1 - lxx) / 10.0,
        np.minimum(lyy, lh - 1 - lyy) / 12.0,
    )
    l_tone = (
        0.14
        + 0.16 * np.clip(l_vignette, 0.0, 1.0)
        + 0.10 * noise_256[ly : ly + lh, lx : lx + lw]
        + 0.06 * fine_256[ly : ly + lh, lx : lx + lw]
    )
    _paint_rect(canvas, lx, ly, lw, lh, "abyssal_obsidian", l_tone, dither=0.07)
    _paint_hyphal_tendrils(canvas, lx, ly, lw, lh, noise_256[ly : ly + lh, lx : lx + lw])
    _draw_border(canvas, lx, ly, lw, lh, (36, 16, 24, 255), (128, 48, 62, 255))

    # 3. Central Vertebral Sinew & Watcher-Eyes Spine Seam (x=85..91, y=7..192 -> w=7, h=186)
    sx, sy, sw, sh = 85, 7, 7, 186
    syy, sxx = np.mgrid[0:sh, 0:sw].astype(np.float32)
    spine_profile = 1.0 - np.abs((sxx - 3.0) / 3.5)
    vertebrae = 0.5 + 0.5 * np.sin(syy * 0.52)
    s_tone = 0.18 + 0.34 * spine_profile + 0.16 * vertebrae
    _paint_rect(canvas, sx, sy, sw, sh, "leech_crimson", s_tone, dither=0.05)
    # Bone vertebrae ridges & unblinking glowing eyes embedded in the spine
    for node_y in range(18, sh - 14, 24):
        canvas[sy + node_y - 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][5]
        canvas[sy + node_y + 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][4]
        canvas[sy + node_y - 1 : sy + node_y + 2, sx + 1 : sx + 6] = PALETTES["spore_chartreuse"][6]
        canvas[sy + node_y - 1 : sy + node_y + 2, sx + 3] = PALETTES["abyssal_obsidian"][0]

    # 4. Right Spore-Stained Necrotic Parchment Spread (x=92..246, y=9..190 -> w=155, h=182)
    rx, ry, rw, rh = 92, 9, 155, 182
    ryy, rxx = np.mgrid[0:rh, 0:rw].astype(np.float32)
    r_vignette = np.minimum(
        np.minimum(rxx, rw - 1 - rxx) / 14.0,
        np.minimum(ryy, rh - 1 - ryy) / 14.0,
    )
    spine_shadow = np.clip(rxx / 10.0, 0.0, 1.0)
    r_tone = (
        0.13
        + 0.18 * np.clip(r_vignette, 0.0, 1.0) * spine_shadow
        + 0.11 * noise_256[ry : ry + rh, rx : rx + rw]
        + 0.05 * fine_256[ry : ry + rh, rx : rx + rw]
    )
    _paint_rect(canvas, rx, ry, rw, rh, "necrotic_umber", r_tone * 0.65, dither=0.07)
    _paint_hyphal_tendrils(canvas, rx, ry, rw, rh, noise_256[ry : ry + rh, rx : rx + rw])
    _draw_border(canvas, rx, ry, rw, rh, (36, 16, 26, 255), (144, 58, 72, 255))

    # Organic header vein divider with glowing fungal spore eye
    canvas[ry + 22, rx + 8 : rx + rw - 8] = PALETTES["leech_crimson"][5]
    canvas[ry + 23, rx + 8 : rx + rw - 8] = PALETTES["abyssal_obsidian"][0]
    canvas[ry + 22, rx + rw // 2 - 3 : rx + rw // 2 + 4] = PALETTES["spore_chartreuse"][6]
    canvas[ry + 22, rx + rw // 2] = PALETTES["abyssal_obsidian"][0]

    # 5. Four Chitin Bone & Spore-Eye Corner Claws
    _paint_horror_corner_claw(canvas, 2, 2, flip_x=False, flip_y=False)
    _paint_horror_corner_claw(canvas, book_w - 3, 2, flip_x=True, flip_y=False)
    _paint_horror_corner_claw(canvas, 2, book_h - 3, flip_x=False, flip_y=True)
    _paint_horror_corner_claw(canvas, book_w - 3, book_h - 3, flip_x=True, flip_y=True)

    # 6. UI Widgets Row (y=204..255)
    # 6a. Standard 18x18 recipe cavity slot at (0, 204)
    slot_tone = 0.16 + 0.14 * fine_256[204:222, 0:18]
    _paint_rect(canvas, 0, 204, 18, 18, "abyssal_obsidian", slot_tone, dither=0.05)
    _draw_border(canvas, 0, 204, 18, 18, (148, 56, 70, 255), (36, 16, 24, 255))
    _draw_border(canvas, 1, 205, 16, 16, (18, 10, 18, 255), (96, 78, 114, 255))

    # 6b. Output / Spotlight 22x22 chitin-bone & toxic spore pedestal at (20, 204)
    out_tone = 0.26 + 0.16 * fine_256[204:226, 20:42]
    _paint_rect(canvas, 20, 204, 22, 22, "leech_crimson", out_tone, dither=0.05)
    _draw_border(
        canvas,
        20,
        204,
        22,
        22,
        tuple(int(c) for c in PALETTES["chitin_bone"][6]),
        tuple(int(c) for c in PALETTES["chitin_bone"][1]),
    )
    _draw_border(
        canvas,
        22,
        206,
        18,
        18,
        (24, 12, 22, 255),
        tuple(int(c) for c in PALETTES["spore_chartreuse"][6]),
    )

    # 6c. Synthesis Tendril Arrow 22x16 at (44, 204)
    ax0, ay0 = 44, 204
    shaft_col = PALETTES["leech_crimson"][5]
    glow_col = PALETTES["spore_chartreuse"][6]
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

    # 6d. Chapter Fungal Ribbon Tab Inactive (20x18 at 68, 204) & Active (20x18 at 90, 204)
    tab_in_tone = 0.24 + 0.14 * fine_256[204:222, 68:88]
    _paint_rect(canvas, 68, 204, 20, 18, "necrotic_umber", tab_in_tone, dither=0.06)
    _draw_border(canvas, 68, 204, 20, 18, (112, 52, 64, 255), (22, 12, 18, 255))

    tab_ac_tone = 0.52 + 0.16 * fine_256[204:222, 90:110]
    _paint_rect(canvas, 90, 204, 20, 18, "leech_crimson", tab_ac_tone, dither=0.05)
    _draw_border(
        canvas,
        90,
        204,
        20,
        18,
        tuple(int(c) for c in PALETTES["spore_chartreuse"][6]),
        tuple(int(c) for c in PALETTES["chitin_bone"][5]),
    )

    # 6e. Hive Mind Eye Medallion 16x16 at (112, 204)
    my, mx = np.mgrid[0:16, 0:16].astype(np.float32)
    mdist = np.sqrt((mx - 7.5) ** 2 + (my - 7.5) ** 2)
    socket_mask = mdist <= 7.2
    iris_mask = mdist <= 4.4
    pupil_mask = (np.abs(mx - 7.5) <= 1.2) & (np.abs(my - 7.5) <= 3.6)
    med = np.zeros((16, 16, 4), dtype=np.uint8)
    med[socket_mask] = PALETTES["leech_crimson"][5]
    med[iris_mask] = PALETTES["spore_chartreuse"][6]
    med[pupil_mask] = PALETTES["abyssal_obsidian"][0]
    med[5, 5] = PALETTES["warm_ivory"][7]
    canvas[204:220, 112:128] = med

    out_path = os.path.join(gui_dir, "codex_book.png")
    save_rgba(canvas, out_path)
    return out_path
