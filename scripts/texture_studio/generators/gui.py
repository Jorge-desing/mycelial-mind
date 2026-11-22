"""Procedural evolutionary Mycelium, Mushrooms & Hive Mind GUI texture generator (4x 256x256 RGBA atlases).

Generates 4 progressive evolutionary atlases in `src/main/resources/assets/mycelialmind/textures/gui/`:
1. `codex_book.png` (Act I - DORMANT, stage=0): subtle mycelial violet rim, nascent corner mushroom buds,
   light margin hyphae, 5 dim spine ganglia.
2. `codex_book_awakened.png` (Act II - AWAKENED, stage=1): brighter amethyst bevel, medium bioluminescent
   caps with cyan spots in corners, expanded chartreuse/violet hyphae, 7 spine ganglia.
3. `codex_book_adaptive.png` (Act III - ADAPTIVE, stage=2): dual cyan-chartreuse perimeter axons, double
   fungal shelf clusters with chitin gills, dense neural hyphae network, 8 dual-halo spine ganglia.
4. `codex_book_overmind.png` (Act IV - OVERMIND, stage=3): tide-violet / amethyst synaptic overdrive, giant
   luminous corner mushroom crowns with cyan/chartreuse halos, dense neural tendrils, triple-mushroom header
   divider, 9 hyper-luminous spine ganglia.

Shared widget sprite atlas layout (`y = 204..255`) present in all 4 variants (evolving palette per stage):
- (0, 204, 18, 18): Standard recipe cavity
- (20, 204, 22, 22): Output / spotlight chitin-amethyst pedestal
- (44, 204, 22, 16): Synthesis hyphal arrow conduit
- (68, 204, 20, 18) & (90, 204, 20, 18): Chapter fungal cap tabs (inactive / active)
- (112, 204, 16, 16): Act I Medallion (Latent Spore Bud)
- (130, 204, 16, 16): Act II Medallion (Awakened Mushroom)
- (148, 204, 16, 16): Act III Medallion (Adaptive Twin Fungal Cluster)
- (166, 204, 16, 16): Act IV Medallion (Overmind Crown Mushroom + Cyan Halo)
- (184, 204, 36, 16) & (220, 204, 36, 16): Textured CodexButton plates (normal / hover-accent)
- (0, 228, 106, 15): Textured search well frame
- (108, 228, 54, 28): Bio-resonance 3D entity containment chamber
- (164, 228, 6, 24) & (172, 228, 6, 10): Scrollbar track & spore thumb
- (180, 228, 72, 17): Textured entry index card plate
"""

from __future__ import annotations

import os
import numpy as np

from ..palette_oklab import PALETTES
from ..sdf_engine import BAYER_4X4, save_rgba, toroidal_value_noise

GUI_VARIANTS: tuple[tuple[int, str], ...] = (
    (0, "codex_book.png"),
    (1, "codex_book_awakened.png"),
    (2, "codex_book_adaptive.png"),
    (3, "codex_book_overmind.png"),
)


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


def _paint_fungal_corner_cluster(
    canvas: np.ndarray, cx: int, cy: int, flip_x: bool, flip_y: bool, stage: int
) -> None:
    """Grow bioluminescent fungal shelf clusters in corners expanding from stage 0 to 3."""
    cap_ramp = ("mycelial_violet", "mycelial_violet", "predator_amethyst", "tide_violet")[stage]
    cap_hi = PALETTES[cap_ramp][6]
    cap_mid = PALETTES[cap_ramp][4]
    gill_bone = PALETTES["chitin_bone"][5 + min(1, stage)]
    spore_glow = PALETTES["spore_chartreuse"][5 + min(2, stage)]
    cyan_spot = PALETTES["biolum_cyan"][5 + min(2, stage)]
    dark_edge = PALETTES["abyssal_obsidian"][0]

    extent = 8 + stage * 2  # 8 -> 10 -> 12 -> 14 px reach
    max_diag = 7 + stage * 3
    for dy in range(extent):
        for dx in range(extent):
            dist = dx + dy
            in_main = (dx < 3 and dy < extent - 1) or (dy < 3 and dx < extent - 1) or (dist <= max_diag)
            in_extra = stage >= 2 and ((dx < 5 and dy == extent - 2) or (dy < 5 and dx == extent - 2))
            if in_main or in_extra:
                px = cx - dx if flip_x else cx + dx
                py = cy - dy if flip_y else cy + dy
                if 0 <= px < 256 and 0 <= py < 200:
                    if (dx == 2 and dy == 2) or (stage >= 1 and dx == 4 and dy == 1) or (stage >= 3 and dx == 6 and dy == 3):
                        canvas[py, px] = cyan_spot
                    elif (dx == 1 and dy == 4) or (stage >= 2 and dx == 3 and dy == 5):
                        canvas[py, px] = spore_glow
                    elif dx == 0 or dy == 0:
                        canvas[py, px] = gill_bone
                    elif dist == max_diag or dx == extent - 1 or dy == extent - 1:
                        canvas[py, px] = dark_edge
                    elif dx <= 2 or dy <= 2:
                        canvas[py, px] = cap_hi
                    else:
                        canvas[py, px] = cap_mid


def _paint_mycelial_network(
    canvas: np.ndarray,
    x0: int,
    y0: int,
    w: int,
    h: int,
    noise_field: np.ndarray,
    stage: int,
) -> None:
    """Etch branching hyphae & bioluminescent synaptic spores intensifying by Act stage (0..3)."""
    hypha_col = PALETTES["tide_violet" if stage >= 3 else "mycelial_violet"][4 + min(2, stage)]
    spore_green = PALETTES["spore_chartreuse"][5 + min(1, stage)]
    synapse_cyan = PALETTES["biolum_cyan"][6]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)

    margin_depth = 10.0 + stage * 4.5
    edge_prox = np.clip(
        1.0 - np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)) / margin_depth,
        0.0,
        1.0,
    )
    freq = 0.26 + stage * 0.04
    vein_wave = np.abs(np.sin(xx * freq + noise_field * (3.8 + stage * 0.5)) - np.cos(yy * (freq * 0.9) - noise_field * 3.8))
    thresh = 0.09 + stage * 0.025
    hypha_mask = (vein_wave < thresh) & (edge_prox > (0.36 - stage * 0.06))
    spore_mask = (noise_field > (0.89 - stage * 0.025)) & (edge_prox > 0.35)
    node_mask = (stage >= 1) & (noise_field > (0.94 - stage * 0.015)) & (edge_prox > 0.45)

    sub = canvas[y0 : y0 + h, x0 : x0 + w]
    blend_h = 0.30 + stage * 0.10
    sub[hypha_mask] = (sub[hypha_mask].astype(np.float32) * (1.0 - blend_h) + hypha_col.astype(np.float32) * blend_h).astype(np.uint8)
    sub[spore_mask] = (sub[spore_mask].astype(np.float32) * 0.55 + spore_green.astype(np.float32) * 0.45).astype(np.uint8)
    if np.any(node_mask):
        sub[node_mask] = (sub[node_mask].astype(np.float32) * 0.35 + synapse_cyan.astype(np.float32) * 0.65).astype(np.uint8)


def _paint_act_medallion(canvas: np.ndarray, x0: int, y0: int, act_idx: int) -> None:
    """Paint one of the 4 distinct 16x16 Act medallions into (x0..x0+16, y0..y0+16)."""
    my, mx = np.mgrid[0:16, 0:16].astype(np.float32)
    mdist = np.sqrt((mx - 7.5) ** 2 + (my - 7.5) ** 2)
    ring_mask = mdist <= 7.2
    inner_mask = mdist <= 5.5
    med = np.zeros((16, 16, 4), dtype=np.uint8)

    ring_ramp = ("abyssal_indigo", "mycelial_violet", "predator_amethyst", "tide_violet")[act_idx]
    med[ring_mask] = PALETTES[ring_ramp][4 + min(2, act_idx)]
    med[inner_mask] = PALETTES["abyssal_obsidian"][1]

    if act_idx == 0:
        # Act I: Dormant spore bud
        bud = mdist <= 3.2
        med[bud] = PALETTES["spore_chartreuse"][5]
        med[mdist <= 1.6] = PALETTES["warm_ivory"][6]
    elif act_idx == 1:
        # Act II: Awakened mushroom cap + chitin stipe
        cap = (mdist <= 5.0) & (my <= 8.5)
        stipe = (np.abs(mx - 7.5) <= 1.5) & (my >= 8.0) & (my <= 13.0)
        med[cap] = PALETTES["spore_chartreuse"][6]
        med[stipe] = PALETTES["chitin_bone"][6]
        med[5, 5] = PALETTES["biolum_cyan"][6]
        med[6, 10] = PALETTES["biolum_cyan"][6]
    elif act_idx == 2:
        # Act III: Dual adaptive mushroom cluster + cyan axon
        cap_l = (np.sqrt((mx - 5.0) ** 2 + (my - 6.0) ** 2) <= 3.2) & (my <= 7.0)
        cap_r = (np.sqrt((mx - 10.5) ** 2 + (my - 7.5) ** 2) <= 2.8) & (my <= 8.5)
        st_l = (np.abs(mx - 5.0) <= 1.1) & (my >= 7.0) & (my <= 12.5)
        st_r = (np.abs(mx - 10.5) <= 1.0) & (my >= 8.5) & (my <= 12.5)
        med[cap_l | cap_r] = PALETTES["biolum_cyan"][6]
        med[st_l | st_r] = PALETTES["chitin_bone"][6]
        med[3:5, 4:7] = PALETTES["spore_chartreuse"][6]
    else:
        # Act IV: Overmind fungal crown + halo ring
        halo = (mdist >= 5.2) & (mdist <= 6.4)
        med[halo] = PALETTES["biolum_cyan"][6]
        cap = (mdist <= 4.8) & (my <= 8.0)
        stipe = (np.abs(mx - 7.5) <= 1.8) & (my >= 8.0) & (my <= 13.5)
        med[cap] = PALETTES["spore_chartreuse"][7]
        med[stipe] = PALETTES["chitin_bone"][7]
        med[4:7, 6:10] = PALETTES["tide_violet"][6]
        med[5, 7:9] = PALETTES["biolum_cyan"][7]

    canvas[y0 : y0 + 16, x0 : x0 + 16] = med


def _build_atlas_for_stage(stage: int) -> np.ndarray:
    """Build one complete 256x256 evolutionary GUI atlas for HiveProgressionTier stage (0..3)."""
    canvas = np.zeros((256, 256, 4), dtype=np.uint8)
    noise_256 = toroidal_value_noise(256, 256, 16, 16, seed=404 + stage * 17).astype(np.float32)
    fine_256 = toroidal_value_noise(256, 256, 32, 32, seed=405 + stage * 23).astype(np.float32)

    # 1. Outer Chitin Carapace & Evolving Mycelial Frame (0, 0, 256, 200)
    book_w, book_h = 256, 200
    yy, xx = np.mgrid[0:book_h, 0:book_w].astype(np.float32)
    edge_dist = np.minimum(np.minimum(xx, book_w - 1 - xx), np.minimum(yy, book_h - 1 - yy))
    frame_tone = 0.18 + 0.04 * stage + 0.14 * noise_256[:book_h, :book_w] + 0.08 * np.clip(edge_dist / 8.0, 0.0, 1.0)
    _paint_rect(canvas, 0, 0, book_w, book_h, "abyssal_obsidian", frame_tone, dither=0.06)

    rim_ramp = ("abyssal_indigo", "mycelial_violet", "predator_amethyst", "tide_violet")[stage]
    rim_tone = 0.28 + 0.07 * stage + 0.16 * noise_256[3 : book_h - 3, 3 : book_w - 3]
    _paint_rect(canvas, 3, 3, book_w - 6, book_h - 6, rim_ramp, rim_tone, dither=0.06)
    _draw_border(canvas, 0, 0, book_w, book_h, (94, 78, 126, 255), (14, 10, 22, 255))
    rim_hi = tuple(int(c) for c in PALETTES[rim_ramp][6])
    _draw_border(canvas, 3, 3, book_w - 6, book_h - 6, rim_hi, (34, 22, 48, 255))

    # Glowing perimeter axons & synaptic nodes (expanding with stage)
    axon_col = PALETTES["biolum_cyan"][4 + min(2, stage)]
    canvas[6, 10 : book_w - 10] = axon_col
    canvas[book_h - 7, 10 : book_w - 10] = axon_col
    canvas[10 : book_h - 10, 6] = axon_col
    canvas[10 : book_h - 10, book_w - 7] = axon_col
    if stage >= 2:
        canvas[5, 16 : book_w - 16] = PALETTES["tide_violet"][5]
        canvas[book_h - 6, 16 : book_w - 16] = PALETTES["tide_violet"][5]
    step_nodes = max(14, 36 - stage * 7)
    for gx in range(22, book_w - 20, step_nodes):
        canvas[6, gx : gx + 2] = PALETTES["spore_chartreuse"][5 + min(2, stage)]
        canvas[book_h - 7, gx : gx + 2] = PALETTES["spore_chartreuse"][5 + min(2, stage)]

    # 2. Left Index Vellum Panel (x=9..84, y=9..190 -> w=76, h=182)
    lx, ly, lw, lh = 9, 9, 76, 182
    lyy, lxx = np.mgrid[0:lh, 0:lw].astype(np.float32)
    l_vig = np.minimum(np.minimum(lxx, lw - 1 - lxx) / 10.0, np.minimum(lyy, lh - 1 - lyy) / 12.0)
    l_tone = 0.14 + 0.03 * stage + 0.16 * np.clip(l_vig, 0.0, 1.0) + 0.10 * noise_256[ly : ly + lh, lx : lx + lw]
    _paint_rect(canvas, lx, ly, lw, lh, "abyssal_indigo", l_tone, dither=0.06)
    _paint_mycelial_network(canvas, lx, ly, lw, lh, noise_256[ly : ly + lh, lx : lx + lw], stage)
    _draw_border(canvas, lx, ly, lw, lh, (32, 22, 52, 255), tuple(int(c) for c in PALETTES[rim_ramp][5]))

    # 3. Central Hive Mind Neural Spine Seam (x=85..91, y=7..192 -> w=7, h=186)
    sx, sy, sw, sh = 85, 7, 7, 186
    syy, sxx = np.mgrid[0:sh, 0:sw].astype(np.float32)
    spine_profile = 1.0 - np.abs((sxx - 3.0) / 3.5)
    rib_rings = 0.5 + 0.5 * np.sin(syy * (0.45 + stage * 0.05))
    s_tone = 0.20 + 0.06 * stage + 0.34 * spine_profile + 0.14 * rib_rings
    _paint_rect(canvas, sx, sy, sw, sh, "tide_violet" if stage >= 3 else "predator_amethyst", s_tone, dither=0.05)

    ganglia_step = (32, 24, 20, 18)[stage]
    for node_y in range(16, sh - 12, ganglia_step):
        canvas[sy + node_y - 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][5 + min(1, stage)]
        canvas[sy + node_y + 3, sx + 1 : sx + sw - 1] = PALETTES["chitin_bone"][4 + min(2, stage)]
        canvas[sy + node_y - 1 : sy + node_y + 2, sx + 1 : sx + 6] = PALETTES["spore_chartreuse"][5 + min(2, stage)]
        canvas[sy + node_y, sx + 3] = PALETTES["biolum_cyan"][6 + min(1, stage)]
        if stage >= 2:
            canvas[sy + node_y - 1, sx + 2] = PALETTES["biolum_cyan"][6]
            canvas[sy + node_y + 1, sx + 4] = PALETTES["biolum_cyan"][6]

    # 4. Right Mycelial Parchment Reading Spread (x=92..246, y=9..190 -> w=155, h=182)
    rx, ry, rw, rh = 92, 9, 155, 182
    ryy, rxx = np.mgrid[0:rh, 0:rw].astype(np.float32)
    r_vig = np.minimum(np.minimum(rxx, rw - 1 - rxx) / 14.0, np.minimum(ryy, rh - 1 - ryy) / 14.0)
    spine_shadow = np.clip(rxx / 10.0, 0.0, 1.0)
    r_tone = 0.14 + 0.025 * stage + 0.18 * np.clip(r_vig, 0.0, 1.0) * spine_shadow + 0.10 * noise_256[ry : ry + rh, rx : rx + rw]
    _paint_rect(canvas, rx, ry, rw, rh, "abyssal_obsidian", r_tone, dither=0.06)
    _paint_mycelial_network(canvas, rx, ry, rw, rh, noise_256[ry : ry + rh, rx : rx + rw], stage)
    _draw_border(canvas, rx, ry, rw, rh, (34, 24, 54, 255), tuple(int(c) for c in PALETTES[rim_ramp][6]))

    # Header axon divider crowned by 1 (Dormant/Awakened) or 3 (Adaptive/Overmind) mushroom nodes
    canvas[ry + 22, rx + 8 : rx + rw - 8] = PALETTES[rim_ramp][5]
    canvas[ry + 23, rx + 8 : rx + rw - 8] = PALETTES["abyssal_obsidian"][1]
    centers = (rx + rw // 2,) if stage <= 1 else (rx + rw // 2 - 20, rx + rw // 2, rx + rw // 2 + 20)
    for cx_m in centers:
        canvas[ry + 21, cx_m - 2 : cx_m + 3] = PALETTES["spore_chartreuse"][6]
        canvas[ry + 22, cx_m - 3 : cx_m + 4] = PALETTES["biolum_cyan"][6]
        canvas[ry + 23, cx_m] = PALETTES["chitin_bone"][6]

    # 5. Four Corner Bioluminescent Fungal Shelf Clusters (evolving with stage)
    _paint_fungal_corner_cluster(canvas, 2, 2, flip_x=False, flip_y=False, stage=stage)
    _paint_fungal_corner_cluster(canvas, book_w - 3, 2, flip_x=True, flip_y=False, stage=stage)
    _paint_fungal_corner_cluster(canvas, 2, book_h - 3, flip_x=False, flip_y=True, stage=stage)
    _paint_fungal_corner_cluster(canvas, book_w - 3, book_h - 3, flip_x=True, flip_y=True, stage=stage)

    # 6. Complete UI Widget Sprite Atlas (y = 204..255)
    # 6a. (0, 204, 18, 18): Recipe cavity
    _paint_rect(canvas, 0, 204, 18, 18, "abyssal_obsidian", 0.18 + 0.12 * fine_256[204:222, 0:18], dither=0.04)
    _draw_border(canvas, 0, 204, 18, 18, tuple(int(c) for c in PALETTES[rim_ramp][6]), (38, 24, 58, 255))
    _draw_border(canvas, 1, 205, 16, 16, (20, 14, 34, 255), (88, 68, 128, 255))

    # 6b. (20, 204, 22, 22): Output / spotlight pedestal
    _paint_rect(canvas, 20, 204, 22, 22, "tide_violet" if stage >= 3 else "predator_amethyst", 0.30 + 0.16 * fine_256[204:226, 20:42], dither=0.05)
    _draw_border(canvas, 20, 204, 22, 22, tuple(int(c) for c in PALETTES["chitin_bone"][6]), tuple(int(c) for c in PALETTES["chitin_bone"][2]))
    _draw_border(canvas, 22, 206, 18, 18, (24, 16, 40, 255), tuple(int(c) for c in PALETTES["biolum_cyan"][6]))

    # 6c. (44, 204, 22, 16): Synthesis hyphal arrow
    ax0, ay0 = 44, 204
    shaft_col = PALETTES["spore_chartreuse"][5 + min(1, stage)]
    glow_col = PALETTES["biolum_cyan"][6]
    edge_col = PALETTES["abyssal_obsidian"][0]
    canvas[ay0 + 6 : ay0 + 10, ax0 + 2 : ax0 + 15] = shaft_col
    canvas[ay0 + 7 : ay0 + 9, ax0 + 3 : ax0 + 15] = glow_col
    canvas[ay0 + 5, ax0 + 2 : ax0 + 14] = edge_col
    canvas[ay0 + 10, ax0 + 2 : ax0 + 14] = edge_col
    for step in range(6):
        top_y, bot_y, px = ay0 + 3 + step, ay0 + 12 - step, ax0 + 13 + step
        if top_y <= bot_y and px < ax0 + 20:
            canvas[top_y : bot_y + 1, px] = shaft_col
            if top_y + 1 <= bot_y - 1:
                canvas[top_y + 1 : bot_y, px] = glow_col

    # 6d. (68, 204, 20, 18) & (90, 204, 20, 18): Chapter fungal cap tabs
    _paint_rect(canvas, 68, 204, 20, 18, "elite_carapace", 0.24 + 0.14 * fine_256[204:222, 68:88], dither=0.05)
    _draw_border(canvas, 68, 204, 20, 18, (118, 98, 152, 255), (26, 18, 40, 255))
    _paint_rect(canvas, 90, 204, 20, 18, rim_ramp, 0.50 + 0.16 * fine_256[204:222, 90:110], dither=0.05)
    _draw_border(canvas, 90, 204, 20, 18, tuple(int(c) for c in PALETTES["spore_chartreuse"][6]), tuple(int(c) for c in PALETTES["biolum_cyan"][5]))
    canvas[207, 94] = PALETTES["biolum_cyan"][6]
    canvas[208, 104] = PALETTES["spore_chartreuse"][6]

    # 6e. 4 Evolutionary Act Medallions (112, 130, 148, 166 at y=204, 16x16 each)
    for act_i in range(4):
        _paint_act_medallion(canvas, 112 + act_i * 18, 204, act_i)

    # 6f. CodexButton textured plates: Normal (184, 204, 36x16) & Hover/Accent (220, 204, 36x16)
    _paint_rect(canvas, 184, 204, 36, 16, "abyssal_indigo", 0.22 + 0.14 * fine_256[204:220, 184:220], dither=0.05)
    _draw_border(canvas, 184, 204, 36, 16, tuple(int(c) for c in PALETTES[rim_ramp][5]), (24, 14, 38, 255))
    canvas[205, 185:219] = PALETTES[rim_ramp][6]

    _paint_rect(canvas, 220, 204, 36, 16, rim_ramp, 0.44 + 0.18 * fine_256[204:220, 220:256], dither=0.05)
    _draw_border(canvas, 220, 204, 36, 16, tuple(int(c) for c in PALETTES["spore_chartreuse"][6]), tuple(int(c) for c in PALETTES["biolum_cyan"][5]))
    canvas[205, 221:255] = PALETTES["biolum_cyan"][6]

    # 6g. Textured Search Well Frame (0, 228, 106, 15)
    _paint_rect(canvas, 0, 228, 106, 15, "abyssal_obsidian", 0.12 + 0.10 * fine_256[228:243, 0:106], dither=0.04)
    _draw_border(canvas, 0, 228, 106, 15, tuple(int(c) for c in PALETTES[rim_ramp][5]), (18, 12, 30, 255))
    canvas[229, 2:12] = PALETTES["spore_chartreuse"][5]

    # 6h. Bio-Resonance Entity Containment Chamber (108, 228, 54, 28)
    _paint_rect(canvas, 108, 228, 54, 28, "abyssal_indigo", 0.16 + 0.14 * noise_256[228:256, 108:162], dither=0.05)
    _paint_mycelial_network(canvas, 108, 228, 54, 28, noise_256[228:256, 108:162], max(1, stage))
    _draw_border(canvas, 108, 228, 54, 28, tuple(int(c) for c in PALETTES["biolum_cyan"][6]), tuple(int(c) for c in PALETTES[rim_ramp][5]))
    canvas[250:254, 111:159] = PALETTES[rim_ramp][3]

    # 6i. Scrollbar Track (164, 228, 6, 24) & Spore Thumb (172, 228, 6, 10)
    _paint_rect(canvas, 164, 228, 6, 24, "abyssal_obsidian", 0.20 + 0.08 * fine_256[228:252, 164:170], dither=0.03)
    _draw_border(canvas, 164, 228, 6, 24, (54, 38, 82, 255), (20, 14, 32, 255))
    _paint_rect(canvas, 172, 228, 6, 10, "spore_chartreuse", 0.68 + 0.15 * fine_256[228:238, 172:178], dither=0.04)
    _draw_border(canvas, 172, 228, 6, 10, tuple(int(c) for c in PALETTES["biolum_cyan"][6]), (48, 72, 28, 255))

    # 6j. Entry Index Card Textured Plate (180, 228, 72, 17)
    _paint_rect(canvas, 180, 228, 72, 17, "abyssal_indigo", 0.24 + 0.12 * noise_256[228:245, 180:252], dither=0.05)
    _draw_border(canvas, 180, 228, 72, 17, tuple(int(c) for c in PALETTES[rim_ramp][5]), (26, 16, 42, 255))

    return canvas


def generate_gui_textures(gui_dir: str) -> list[str]:
    """Generate all 4 evolutionary 256x256 Codex GUI texture atlases."""
    os.makedirs(gui_dir, exist_ok=True)
    paths: list[str] = []
    for stage, filename in GUI_VARIANTS:
        out_path = os.path.join(gui_dir, filename)
        atlas = _build_atlas_for_stage(stage)
        save_rgba(atlas, out_path)
        paths.append(out_path)
    return paths
