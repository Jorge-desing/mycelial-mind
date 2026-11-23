"""Native 16x16 block texture generators using Motor v3 (Gray-Scott, Curvature, Floyd-Steinberg, LabPBR _n + _s)."""

from __future__ import annotations

import os
import numpy as np

from ..compositor import LayerStack, channels_to_labpbr_s, normals_to_labpbr
from ..materials import MATERIALS, Material
from ..palette_oklab import PALETTES
from ..sdf_engine import (
    bayer_quantize,
    cell_plate_field,
    compute_sdf_ao,
    compute_sdf_curvature,
    compute_sdf_lighting_v2,
    compute_sdf_normals,
    domain_warp,
    floyd_steinberg_quantize,
    frangi_vesselness_toroidal,
    gray_scott_toroidal,
    make_grid,
    poisson_disc_toroidal,
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
    toroidal_value_fbm,
    toroidal_value_noise,
    wfc_overlap_toroidal,
)

BLOCK_RES = 16
PULSE_LEVELS = (0.0, 0.45, 1.0, 0.45)


def _save_pbr_pair(
    pbr_dir: str | None,
    name: str,
    sdf: np.ndarray,
    mat: Material,
    height_map: np.ndarray | None = None,
    emissive_mask: np.ndarray | None = None,
    metal_mask: np.ndarray | None = None,
) -> None:
    if pbr_dir is None:
        return
    os.makedirs(pbr_dir, exist_ok=True)
    nx, ny, nz = compute_sdf_normals(sdf)
    ao = compute_sdf_ao(sdf)
    pom = sdf_to_pom_height(sdf) if height_map is None else height_map
    save_rgba(normals_to_labpbr(nx, ny, nz, ao, height_map=pom), os.path.join(pbr_dir, f"{name}_n.png"))

    shape = sdf.shape
    smooth = np.full(shape, mat.smoothness, dtype=np.float32)
    f0 = np.full(shape, mat.f0, dtype=np.float32)
    if metal_mask is not None:
        f0 = np.where(metal_mask, MATERIALS["PURIFICATION_GOLD"].f0, f0)
    sss = np.full(shape, mat.porosity_sss, dtype=np.float32)
    emis = np.full(shape, mat.emissive, dtype=np.float32)
    if emissive_mask is not None:
        emis = np.where(emissive_mask, np.maximum(mat.emissive, 0.65), 0.0).astype(np.float32)
    save_rgba(channels_to_labpbr_s(smooth, f0, sss, emis, is_subsurface=mat.is_subsurface, metal_id=mat.metal_id), os.path.join(pbr_dir, f"{name}_s.png"))


def generate_blocks(block_dir: str, pbr_dir: str | None = None) -> None:
    os.makedirs(block_dir, exist_ok=True)
    n = BLOCK_RES
    xx, yy = make_grid(n, n)
    ang = np.arctan2(yy, xx)
    rad = np.sqrt(xx * xx + yy * yy)

    # 1) creeping_mycelium: rich organic mycelium mat with fine branching hyphae, spore buds & stratified soil
    wx, wy = domain_warp(xx, yy, seed=101, strength=0.14, freq=2.2)
    gs_pattern = gray_scott_toroidal(n, n, feed=0.055, kill=0.062, steps=160, seed=101)
    soil_fbm = toroidal_value_fbm(n, n, seed=101, base_cells=4, octaves=3)
    top_base_field = np.clip(soil_fbm * 0.55 + gs_pattern * 0.45, 0.0, 1.0)
    vein_b1 = sdf_bezier(wx, wy, (-0.98, -0.42), (-0.08, 0.30), (0.98, -0.22), 0.082)
    vein_b2 = sdf_bezier(wx, wy, (-0.52, 0.98), (0.20, -0.10), (0.62, -0.98), 0.075)
    vein_b3 = sdf_bezier(wx, wy, (-0.85, 0.65), (-0.15, 0.05), (0.80, 0.72), 0.065)
    vein_sdf = np.minimum(np.minimum(vein_b1, vein_b2), vein_b3)
    frangi_raw = frangi_vesselness_toroidal(gs_pattern)
    frangi_veins = frangi_raw > 0.44
    vein_mask = (vein_sdf <= 0.0) | frangi_veins
    vein_halo = (vein_sdf > 0.0) & (vein_sdf <= 0.14)
    _save_pbr_pair(pbr_dir, "creeping_mycelium_top", vein_sdf, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=vein_mask)

    top_frames = []
    for p in PULSE_LEVELS:
        soil_val = np.where(vein_halo, top_base_field * 0.52, top_base_field * 0.78)
        soil = bayer_quantize(soil_val, PALETTES["abyssal_obsidian"], 0.16)
        vein_lit = compute_sdf_lighting_v2(vein_sdf, sss_strength=0.28, ao_strength=0.22) * (0.76 + 0.24 * p)
        vein_lit = np.where(frangi_veins, np.clip(0.52 + frangi_raw * 0.44 * (0.78 + 0.22 * p), 0.0, 1.0), vein_lit)
        vein_col = bayer_quantize(vein_lit, PALETTES["mycelial_violet"], 0.16)
        stack = LayerStack(n, n).add_layer(soil)
        stack.add_layer(np.where(vein_mask[..., None], vein_col, np.zeros_like(vein_col)))
        # Subtle glowing spore nodes nestled on vein junctions
        for nx, ny in [(-0.20, 0.12), (0.36, -0.20), (-0.58, -0.35), (0.48, 0.52)]:
            d = sdf_circle(xx, yy, nx, ny, 0.075 + 0.015 * p)
            node_lit = np.clip(0.62 + 0.38 * p - d * 2.5, 0.0, 1.0)
            node_col = bayer_quantize(node_lit, PALETTES["spore_chartreuse"], 0.12)
            stack.add_layer(np.where((d <= 0.0)[..., None], node_col, np.zeros_like(node_col)))
        top_frames.append(stack.flatten())
    save_flipbook(top_frames, os.path.join(block_dir, "creeping_mycelium_top.png"), frametime=6)

    # Side: stratified dark soil + overhanging purple mycelium mat + cast shadow + fine curved hyphae
    side_fbm = toroidal_value_fbm(n, n, seed=102, base_cells=4, octaves=3)
    side_grit = toroidal_value_fbm(n, n, seed=112, base_cells=8, octaves=1)
    strata = 0.08 * np.sin((yy + 0.25 * side_fbm) * np.pi * 5.0)
    soil_side_val = np.clip(side_fbm * 0.62 + side_grit * 0.20 + strata, 0.08, 0.88)
    cap_depth = -0.60 + 0.14 * np.sin(xx * np.pi * 3.0 + 0.4) + 0.08 * np.cos(xx * np.pi * 6.0 - 0.7)
    cap_mask = yy <= cap_depth
    under_shadow = (~cap_mask) & (yy <= cap_depth + 0.16)
    soil_side = bayer_quantize(np.where(under_shadow, soil_side_val * 0.48, soil_side_val), PALETTES["basalt_slate"], 0.14)
    cap_grad = np.clip(0.92 - (yy + 1.0) * 1.15 + side_grit * 0.18, 0.18, 1.0)
    cap_col = bayer_quantize(cap_grad, PALETTES["mycelial_violet"], 0.16)
    side = np.where(cap_mask[..., None], cap_col, soil_side)

    # Fine curved branching hyphae descending from the mycelium overhang into the soil
    swx, swy = domain_warp(xx, yy, seed=109, strength=0.08, freq=2.4)
    h1 = sdf_bezier(swx, swy, (-0.70, -0.65), (-0.52, -0.15), (-0.74, 0.35), 0.062)
    h2 = sdf_bezier(swx, swy, (-0.12, -0.62), (0.08, -0.05), (-0.10, 0.55), 0.058)
    h3 = sdf_bezier(swx, swy, (0.58, -0.68), (0.42, -0.22), (0.68, 0.25), 0.056)
    hypha_sdf = np.minimum(np.minimum(h1, h2), h3)
    hypha_mask = (hypha_sdf <= 0.0) & (~cap_mask)
    hypha_lit = np.clip(0.85 - (yy + 0.6) * 0.45, 0.25, 0.95)
    hypha_col = bayer_quantize(hypha_lit, PALETTES["mycelial_violet"], 0.14)
    side = np.where(hypha_mask[..., None], hypha_col, side)

    # Micro bioluminescent spore flecks along the overhang and roots
    spore_mask = np.zeros_like(cap_mask)
    for sx, sy in [(-0.56, -0.78), (0.14, -0.74), (0.64, -0.80), (-0.02, 0.10), (-0.64, 0.02)]:
        sm = sdf_circle(xx, yy, sx, sy, 0.065) <= 0.0
        spore_mask |= sm
    spore_col = bayer_quantize(np.full((n, n), 0.78, dtype=np.float32), PALETTES["spore_chartreuse"], 0.10)
    side = np.where(spore_mask[..., None], spore_col, side)

    save_rgba(side, os.path.join(block_dir, "creeping_mycelium_side.png"))
    side_pom_sdf = np.where(cap_mask, -0.35, np.where(hypha_mask, -0.15, 0.18)).astype(np.float32)
    _save_pbr_pair(pbr_dir, "creeping_mycelium_side", side_pom_sdf, MATERIALS["BASALT_SLATE"],
                   height_map=np.where(cap_mask, 0.92, np.where(hypha_mask, 0.80, 0.62)).astype(np.float32),
                   emissive_mask=spore_mask | hypha_mask)

    wfc_exemplar = np.array([[0, 1, 0, 2], [1, 2, 1, 0], [0, 1, 0, 1], [2, 0, 1, 1]], dtype=np.int32)
    # 1b) creeping_mycelium_bottom: root mat under the soil with branching rhizomorphs and glowing nodules
    bot_fbm = toroidal_value_fbm(n, n, seed=103, base_cells=3, octaves=3)
    bot_grit = toroidal_value_noise(n, n, 6, 6, seed=1030)
    bot_bevel, bot_seam = cell_plate_field(n, n, seed=1031, cells=7)
    bot_field = np.clip(0.48 * bot_fbm + 0.24 * bot_grit + 0.14 * (1.0 - bot_seam) + 0.06, 0.0, 1.0)
    bot_base = bayer_quantize(bot_field, PALETTES["abyssal_obsidian"], 0.16, matrix=4)
    bot_warm = bayer_quantize(np.clip(0.30 + 0.22 * bot_fbm, 0.0, 1.0), PALETTES["necrotic_umber"], 0.14, matrix=4)
    bot_base = np.where((bot_bevel > 0.62)[..., None], bot_warm, bot_base)
    bot_base = np.where((bot_seam > 0.84)[..., None], PALETTES["abyssal_obsidian"][0], bot_base)
    bw = domain_warp(xx, yy, seed=1032, strength=0.10, freq=2.2)
    r1 = sdf_bezier(bw[0], bw[1], (-0.96, -0.55), (-0.14, 0.22), (0.92, -0.12), 0.070)
    r2 = sdf_bezier(bw[0], bw[1], (-0.44, 0.96), (0.10, 0.00), (0.56, -0.96), 0.058)
    r3 = sdf_bezier(bw[0], bw[1], (-0.22, -0.96), (0.26, -0.18), (0.96, 0.46), 0.048)
    roots = np.minimum(np.minimum(r1, r2), r3)
    root_col = bayer_quantize(np.clip(0.40 + 0.18 * bot_fbm, 0.0, 1.0), PALETTES["chitin_bone"], 0.12, matrix=4)
    bot = np.where((roots <= 0.0)[..., None], root_col, bot_base)
    bot = np.where((np.abs(roots) < 0.016)[..., None], PALETTES["chitin_bone"][5], bot)
    bot_glow = np.zeros((n, n), dtype=bool)
    for gx, gy in ((-0.60, -0.30), (0.24, 0.36), (0.62, -0.56)):
        d = sdf_circle(xx, yy, gx, gy, 0.085)
        bot_glow |= d <= 0.0
        bot = np.where((d <= 0.0)[..., None], bayer_quantize(np.full((n, n), 0.78), PALETTES["spore_chartreuse"], 0.10), bot)
    save_rgba(bot, os.path.join(block_dir, "creeping_mycelium_bottom.png"))
    _save_pbr_pair(pbr_dir, "creeping_mycelium_bottom", 0.30 - bot_field * 0.35, MATERIALS["BIOLUM_MYCELIUM"],
                   height_map=np.where(roots <= 0.0, 0.86, np.clip(0.42 + 0.36 * bot_field, 0.15, 0.95)),
                   emissive_mask=bot_glow)

    # 2) spore_vent: 6-petal polar SDF + Gray-Scott + Poisson-disc spore pores + SSS + Bayer 8x8
    sv_frames = []
    petal_r = 0.72 + 0.12 * np.cos(6.0 * ang)
    d_outer = rad - petal_r
    _, sv_pores = poisson_disc_toroidal(n, n, min_dist=3.2, seed=104)
    sv_gs = np.clip(gray_scott_toroidal(n, n, feed=0.038, kill=0.061, steps=140, seed=104) * 0.58 + sv_pores * 0.42, 0.0, 1.0)
    sv_pom = np.where(rad < 0.38, 0.25 + 0.45 * (rad / 0.38), sdf_to_pom_height(d_outer, depth=0.55))
    _save_pbr_pair(pbr_dir, "spore_vent_top", d_outer, MATERIALS["TOXIC_SPORE"],
                   height_map=sv_pom, emissive_mask=(rad < 0.40) | (sv_pores < 0.28))
    for p in PULSE_LEVELS:
        d_core = rad - (0.36 + 0.05 * p)
        rim_lit = np.clip(compute_sdf_lighting_v2(d_outer, specular_strength=0.32, sss_strength=0.22, ao_strength=0.35) * 0.65 + sv_gs * 0.35, 0.0, 1.0)
        rim_col = bayer_quantize(rim_lit, PALETTES["leech_crimson"], 0.24, matrix=8)
        pore_spot = (d_outer <= 0.0) & (sv_pores < 0.26)
        rim_col = np.where(pore_spot[..., None], PALETTES["chitin_bone"][-2], rim_col)
        bg_col = bayer_quantize(toroidal_value_fbm(n, n, seed=104, base_cells=4, octaves=3) * 0.65, PALETTES["abyssal_obsidian"], 0.16)
        core_lit = np.clip(1.0 - rad / (0.42 + 0.06 * p), 0.15, 1.0)
        core_col = bayer_quantize(core_lit, PALETTES["spore_chartreuse"], 0.20, matrix=8)
        out = np.where((d_outer <= 0.0)[..., None], rim_col, bg_col)
        out = np.where((d_core <= 0.0)[..., None], core_col, out)
        sv_frames.append(out)
    save_flipbook(sv_frames, os.path.join(block_dir, "spore_vent_top.png"), frametime=5)

    # 2b) spore_vent_side: ribbed organic vent stalk with a glowing fissure and chitin lip
    sv_rib = 0.5 + 0.5 * np.cos(xx * np.pi * 5.0 + 0.7 * np.sin(yy * np.pi * 2.0))
    sv_grit = toroidal_value_fbm(n, n, seed=1041, base_cells=3, octaves=3)
    sv_field = np.clip(0.30 + 0.36 * sv_rib + 0.34 * sv_grit, 0.0, 1.0)
    sv_side = bayer_quantize(sv_field, PALETTES["basalt_slate"], 0.14, matrix=4)
    lip = sdf_box(xx, yy, 0.0, -0.80, 0.85, 0.20)
    lip_col = bayer_quantize(np.clip(0.60 + 0.24 * sv_rib, 0.0, 1.0), PALETTES["chitin_bone"], 0.12, matrix=4)
    sv_side = np.where((lip <= 0.0)[..., None], lip_col, sv_side)
    sv_side = np.where((np.abs(yy + 0.645) < 0.045)[..., None], PALETTES["abyssal_obsidian"][2], sv_side)
    fiss_x = 0.04 + 0.10 * np.sin(yy * np.pi * 2.0)
    fiss = np.abs(xx - fiss_x) < 0.080
    fiss_core = np.abs(xx - fiss_x) < 0.032
    sv_side = np.where((fiss & ~fiss_core)[..., None], PALETTES["abyssal_obsidian"][1], sv_side)
    sv_side = np.where(fiss_core[..., None], bayer_quantize(np.clip(0.55 + 0.32 * sv_grit, 0.0, 1.0), PALETTES["spore_chartreuse"], 0.10), sv_side)
    for cx in (-0.58, 0.58):
        conduit = np.abs(xx - cx) < 0.028
        sv_side = np.where(conduit[..., None], PALETTES["mycelial_violet"][4], sv_side)
    save_rgba(sv_side, os.path.join(block_dir, "spore_vent_side.png"))
    _save_pbr_pair(pbr_dir, "spore_vent_side", np.where(fiss_core, -0.42, 0.28 - sv_field * 0.34), MATERIALS["OBSIDIAN_CHITIN"],
                   height_map=np.where(fiss_core, 0.24, np.where(fiss, 0.40, np.clip(0.42 + 0.40 * sv_rib, 0.18, 1.0))),
                   emissive_mask=fiss_core)

    # 2c) spore_vent_bottom: underside pore field with chitin rims and two glowing ducts
    svb_fbm = toroidal_value_fbm(n, n, seed=1042, base_cells=3, octaves=3)
    svb_field = np.clip(0.36 + 0.36 * svb_fbm, 0.0, 1.0)
    svb = bayer_quantize(svb_field, PALETTES["basalt_slate"], 0.16, matrix=4)
    svb_pores = np.zeros((n, n), dtype=bool)
    for px, py, pr, glow in ((-0.48, -0.42, 0.21, True), (0.42, -0.48, 0.17, False), (0.52, 0.36, 0.19, False), (-0.34, 0.46, 0.18, False)):
        d = sdf_circle(xx, yy, px, py, pr)
        rim = (d > 0.0) & (d < 0.065)
        outer = (d > 0.065) & (d < 0.105)
        svb = np.where(rim[..., None], PALETTES["chitin_bone"][6], svb)
        svb = np.where(outer[..., None], PALETTES["abyssal_obsidian"][1], svb)
        if glow:
            core = sdf_circle(xx, yy, px, py, pr * 0.42)
            svb = np.where((core <= 0.0)[..., None], bayer_quantize(np.full((n, n), 0.78), PALETTES["spore_chartreuse"], 0.10), svb)
        else:
            svb = np.where((d <= 0.0)[..., None], PALETTES["abyssal_obsidian"][1], svb)
        svb_pores |= d <= 0.0
    boss = sdf_circle(xx, yy, 0.02, 0.02, 0.075)
    svb = np.where((sdf_onion(boss, 0.025) <= 0.0)[..., None], PALETTES["chitin_bone"][5], svb)
    save_rgba(svb, os.path.join(block_dir, "spore_vent_bottom.png"))
    _save_pbr_pair(pbr_dir, "spore_vent_bottom", np.where(svb_pores, 0.35, 0.20 - svb_field * 0.30), MATERIALS["OBSIDIAN_CHITIN"],
                   height_map=np.where(svb_pores, 0.20, np.clip(0.45 + 0.34 * svb_field, 0.2, 1.0)))

    # 3) purification_pylon: octagonal/diamond SDF amethyst prism + copper brackets on deepslate (no cyan/diamond)
    slate_bg = bayer_quantize(toroidal_value_fbm(n, n, seed=106, base_cells=4, octaves=3) * 0.75, PALETTES["basalt_slate"], 0.16)
    bracket = (np.abs(xx) > 0.72) & (np.abs(yy) > 0.72)
    copper_col = bayer_quantize(np.clip(1.0 - rad * 0.3, 0.3, 1.0), PALETTES["volcanic_ember"], 0.18)

    d_top = sdf_polygon(xx, yy, 0.0, 0.0, 0.42, sides=8)
    curv_top = np.clip(compute_sdf_curvature(d_top) * 0.22, -0.15, 0.22)
    _save_pbr_pair(pbr_dir, "purification_pylon_top", d_top, MATERIALS["LIVING_AMETHYST"],
                   emissive_mask=(d_top <= 0.0), metal_mask=bracket)
    pp_top_lit = np.clip(compute_sdf_lighting_v2(d_top, specular_strength=0.45) + curv_top, 0.0, 1.0)
    pp_top = np.where(bracket[..., None], copper_col, slate_bg)
    pp_top = np.where((d_top <= 0.0)[..., None], bayer_quantize(pp_top_lit, PALETTES["predator_amethyst"], 0.20), pp_top)
    save_rgba(pp_top, os.path.join(block_dir, "purification_pylon_top.png"))

    pp_frames = []
    d_prism_ref = sdf_polygon(xx * 1.65, yy * 0.82, 0.0, 0.0, 0.62, sides=6)
    d_prism_ref = np.maximum(d_prism_ref, sdf_box(xx, yy, 0.0, 0.0, 0.26, 0.64))
    _save_pbr_pair(pbr_dir, "purification_pylon_side", d_prism_ref, MATERIALS["LIVING_AMETHYST"],
                   emissive_mask=(d_prism_ref <= 0.0), metal_mask=bracket)
    pp_slate = bayer_quantize(np.clip(0.30 + 0.28 * toroidal_value_fbm(n, n, seed=106, base_cells=3, octaves=3), 0.0, 1.0), PALETTES["basalt_slate"], 0.14, matrix=4)
    copper_band = bayer_quantize(np.clip(0.55 + 0.22 * toroidal_value_fbm(n, n, seed=1061, base_cells=5, octaves=2), 0.0, 1.0), PALETTES["volcanic_ember"], 0.12, matrix=4)
    band_mask = (np.abs(yy) > 0.66)
    pp_base = np.where(band_mask[..., None], copper_band, pp_slate)
    pp_base = np.where((np.abs(np.abs(yy) - 0.705) < 0.028)[..., None], PALETTES["volcanic_ember"][2], pp_base)
    for rx in (-0.60, -0.20, 0.20, 0.60):
        for ry in (-0.78, 0.78):
            rivet = sdf_circle(xx, yy, rx, ry, 0.055) <= 0.0
            pp_base = np.where(rivet[..., None], PALETTES["chitin_bone"][5], pp_base)
    for p in PULSE_LEVELS:
        prism = np.maximum(sdf_polygon(xx * 1.65, yy * 0.82, 0.0, 0.0, 0.60 + 0.03 * p, sides=6),
                           sdf_box(xx, yy, 0.0, 0.0, 0.25, 0.62))
        facet = bayer_quantize(compute_sdf_lighting_v2(prism, specular_strength=0.50, sss_strength=0.22), PALETTES["predator_amethyst"], 0.16, matrix=8)
        facet = np.where((np.abs(xx + 0.13) < 0.030)[..., None], PALETTES["predator_amethyst"][6], facet)
        facet = np.where((np.abs(xx - 0.13) < 0.030)[..., None], PALETTES["predator_amethyst"][2], facet)
        out = np.where((prism <= 0.0)[..., None], facet, pp_base)
        core = (np.abs(xx) < 0.045) & (np.abs(yy) < 0.58)
        out = np.where(core[..., None], bayer_quantize(np.full((n, n), 0.72 + 0.26 * p), PALETTES["warm_ivory"], 0.10), out)
        for cy in (-0.44, 0.44):
            bracket_px = (np.abs(np.abs(xx) - 0.32) < 0.075) & (np.abs(yy - cy) < 0.095)
            out = np.where(bracket_px[..., None], copper_col, out)
        pp_frames.append(out)
    save_flipbook(pp_frames, os.path.join(block_dir, "purification_pylon_side.png"), frametime=5)

    # 4) mycelial_node: concentric sdf_onion rings + suspended neural orb (mycelial violet + spore ivory)
    mn_frames = []
    d_orb_ref = sdf_circle(xx, yy, 0.0, 0.0, 0.48)
    _save_pbr_pair(pbr_dir, "mycelial_node", d_orb_ref, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=(d_orb_ref <= 0.0))
    for p in PULSE_LEVELS:
        bg = bayer_quantize(toroidal_value_fbm(n, n, seed=107, base_cells=4, octaves=3) * 0.55, PALETTES["abyssal_obsidian"], 0.16)
        d_orb = sdf_circle(xx, yy, 0.0, 0.0, 0.46 + 0.04 * p)
        d_ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.68), 0.11)
        d_core = sdf_circle(xx, yy, 0.0, 0.0, 0.25 + 0.03 * p)
        orb_col = bayer_quantize(compute_sdf_lighting_v2(d_orb, specular_strength=0.4, sss_strength=0.25), PALETTES["mycelial_violet"], 0.20, matrix=8)
        ring_col = bayer_quantize(compute_sdf_lighting_v2(d_ring) * (0.65 + 0.35 * p), PALETTES["chitin_bone"], 0.16)
        core_col = bayer_quantize(compute_sdf_lighting_v2(d_core, specular_strength=0.5, sss_strength=0.3) * (0.75 + 0.25 * p), PALETTES["warm_ivory"], 0.16)
        cross = ((np.abs(xx) < 0.13) | (np.abs(yy) < 0.13)) & (rad > 0.44)
        out = np.where(cross[..., None], bayer_quantize(np.full_like(xx, 0.6 + 0.3 * p), PALETTES["mycelial_violet"], 0.15), bg)
        out = np.where((d_ring <= 0.0)[..., None], ring_col, out)
        out = np.where(bracket[..., None], bayer_quantize(np.full_like(xx, 0.65), PALETTES["chitin_bone"], 0.18), out)
        out = np.where((d_orb <= 0.0)[..., None], orb_col, out)
        out = np.where((d_core <= 0.0)[..., None], core_col, out)
        mn_frames.append(out)
    save_flipbook(mn_frames, os.path.join(block_dir, "mycelial_node.png"), frametime=4)

    # 5) reinforced_bio_barrier: four beveled chitin armor plates with rivets and a glowing energy seam
    rbb_grain = toroidal_value_fbm(n, n, seed=108, base_cells=4, octaves=3)
    rbb = bayer_quantize(np.clip(0.34 + 0.18 * rbb_grain, 0.0, 1.0), PALETTES["basalt_slate"], 0.12, matrix=4)
    rbb_energy = np.abs(xx) < 0.045
    rbb_energy = rbb_energy | (np.abs(yy) < 0.045)
    rbb = np.where(rbb_energy[..., None], bayer_quantize(np.clip(0.55 + 0.25 * rbb_grain, 0.0, 1.0), PALETTES["mycelial_violet"], 0.10), rbb)
    plate_height = np.zeros((n, n), dtype=np.float32)
    for sx in (-1.0, 1.0):
        for sy in (-1.0, 1.0):
            plate = sdf_box(xx, yy, sx * 0.49, sy * 0.49, 0.395, 0.395)
            plate = sdf_subtract(plate, sdf_circle(xx, yy, sx * 0.49, sy * 0.49, 0.0))
            plate_col = bayer_quantize(np.clip(0.50 + 0.16 * rbb_grain, 0.0, 1.0), PALETTES["chitin_bone"], 0.10, matrix=4)
            rbb = np.where((plate <= 0.0)[..., None], plate_col, rbb)
            top_edge = (plate > -0.055) & (plate <= 0.0) & ((xx * sx < 0.0) | (yy * sy < 0.0))
            rbb = np.where(top_edge[..., None], PALETTES["chitin_bone"][7], rbb)
            low_edge = (plate > -0.055) & (plate <= 0.0) & ((xx * sx > 0.0) | (yy * sy > 0.0))
            rbb = np.where(low_edge[..., None], PALETTES["chitin_bone"][2], rbb)
            for rx in (-0.26, 0.26):
                for ry in (-0.26, 0.26):
                    rbb = np.where((sdf_circle(xx, yy, sx * 0.49 + rx, sy * 0.49 + ry, 0.045) <= 0.0)[..., None], PALETTES["chitin_bone"][6], rbb)
            plate_height = np.maximum(plate_height, np.clip(0.62 - plate * 0.30, 0.0, 1.0))
    save_rgba(rbb, os.path.join(block_dir, "reinforced_bio_barrier.png"))
    _save_pbr_pair(pbr_dir, "reinforced_bio_barrier", np.where(rbb_energy, -0.35, 0.30 - plate_height * 0.5), MATERIALS["CHITIN_BONE"],
                   height_map=np.clip(plate_height + np.where(rbb_energy, 0.10, 0.0), 0.2, 1.0), emissive_mask=rbb_energy)

    # 6) bioluminescent_mycelial_cap (16x16 cutout cross: lavender mushroom cap + Poisson spore spots + ivory stem)
    d_stem = sdf_box(xx, yy, 0.0, 0.35, 0.16, 0.45)
    d_cap = sdf_circle(xx, yy * 1.4, 0.0, -0.25, 0.62)
    cap_mask_m = (d_cap <= 0.0) & (yy <= -0.05)
    stem_mask_m = (d_stem <= 0.0) & (~cap_mask_m)
    _, cap_pores = poisson_disc_toroidal(n, n, min_dist=3.6, seed=106)
    cap_c = bayer_quantize(compute_sdf_lighting_v2(d_cap, sss_strength=0.35), PALETTES["mycelial_violet"], 0.20, matrix=8)
    cap_c = np.where((cap_pores < 0.26)[..., None], PALETTES["warm_ivory"][-1], cap_c)
    stem_c = bayer_quantize(compute_sdf_lighting_v2(d_stem), PALETTES["chitin_bone"], 0.20, matrix=8)
    mush = np.zeros((n, n, 4), dtype=np.uint8)
    mush = np.where(stem_mask_m[..., None], stem_c, mush)
    mush = np.where(cap_mask_m[..., None], cap_c, mush)
    save_rgba(mush, os.path.join(block_dir, "bioluminescent_mycelial_cap.png"))
    _save_pbr_pair(pbr_dir, "bioluminescent_mycelial_cap", np.minimum(d_cap, d_stem), MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=cap_mask_m)

    # 7) mycelial_tendrils: hanging tendril cluster with spore bulbs and dark rim
    t1 = sdf_bezier(xx, yy, (-0.52, -0.98), (-0.30, -0.10), (-0.44, 0.90), 0.115)
    t2 = sdf_bezier(xx, yy, (0.02, -0.98), (0.20, 0.00), (0.02, 0.82), 0.100)
    t3 = sdf_bezier(xx, yy, (0.54, -0.98), (0.36, -0.20), (0.60, 0.62), 0.085)
    branch = np.minimum(
        sdf_bezier(xx, yy, (-0.36, 0.10), (-0.10, -0.12), (0.16, 0.06), 0.055),
        sdf_bezier(xx, yy, (0.30, 0.24), (0.10, 0.10), (-0.08, 0.30), 0.045),
    )
    t_sdf = np.minimum(np.minimum(t1, t2), np.minimum(t3, branch))
    for bx, by, br in ((-0.46, 0.66, 0.11), (0.08, 0.60, 0.09), (0.58, 0.46, 0.08)):
        t_sdf = np.minimum(t_sdf, sdf_circle(xx, yy, bx, by, br))
    t_lit = compute_sdf_lighting_v2(t_sdf, specular_strength=0.32, sss_strength=0.30, ao_strength=0.34)
    t_col = bayer_quantize(t_lit, PALETTES["mycelial_violet"], 0.16, matrix=8)
    t_rim = (t_sdf > -0.06) & (t_sdf <= 0.0)
    t_col = np.where(t_rim[..., None], PALETTES["mycelial_violet"][0], t_col)
    t_glow = np.zeros((n, n), dtype=bool)
    for bx, by, br in ((-0.46, 0.66, 0.07), (0.08, 0.60, 0.055), (0.58, 0.46, 0.05)):
        t_glow |= sdf_circle(xx, yy, bx, by, br) <= 0.0
        t_col = np.where((sdf_circle(xx, yy, bx, by, br) <= 0.0)[..., None], PALETTES["spore_chartreuse"][6], t_col)
    tendrils = np.where((t_sdf <= 0.0)[..., None], t_col, np.zeros((n, n, 4), dtype=np.uint8))
    save_rgba(tendrils, os.path.join(block_dir, "mycelial_tendrils.png"))
    _save_pbr_pair(pbr_dir, "mycelial_tendrils", t_sdf, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=t_glow)

    # 8) spore_bulb (16x16 cutout cross)
    d_bulb = sdf_circle(xx, yy, 0.0, 0.05, 0.44)
    d_stalk = sdf_box(xx, yy, 0.0, 0.65, 0.14, 0.30)
    b_sdf = np.minimum(d_bulb, d_stalk)
    bulb = np.zeros((n, n, 4), dtype=np.uint8)
    bulb = np.where((d_stalk <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(d_stalk), PALETTES["abyssal_obsidian"], 0.2), bulb)
    bulb = np.where((d_bulb <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(d_bulb, sss_strength=0.35), PALETTES["spore_chartreuse"], 0.2, matrix=8), bulb)
    save_rgba(bulb, os.path.join(block_dir, "spore_bulb.png"))
    _save_pbr_pair(pbr_dir, "spore_bulb", b_sdf, MATERIALS["TOXIC_SPORE"], emissive_mask=(d_bulb <= 0.0))

    # 9) hive_altar: obsidian WFC mosaic with a bone frame and a crimson synaptic sigil
    altar_wfc = wfc_overlap_toroidal(np.array([[0, 1, 0, 2], [1, 2, 1, 0], [0, 1, 0, 1], [2, 0, 1, 1]], dtype=np.int32), n, n, n=2, seed=109, symmetry=8).astype(np.float32) * 0.26
    altar_bg = bayer_quantize(np.clip(toroidal_value_fbm(n, n, seed=109, base_cells=3, octaves=3) * 0.40 + altar_wfc + 0.10, 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.14, matrix=4)
    altar_frame = (np.abs(xx) > 0.84) | (np.abs(yy) > 0.84)
    altar = np.where(altar_frame[..., None], PALETTES["chitin_bone"][4], altar_bg)
    altar = np.where(((np.abs(xx) > 0.84) & (np.abs(xx) < 0.90))[..., None], PALETTES["chitin_bone"][6], altar)
    altar = np.where(((np.abs(yy) > 0.84) & (np.abs(yy) < 0.90))[..., None], PALETTES["chitin_bone"][6], altar)
    d_altar_ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.54), 0.055)
    d_altar_sigil = sdf_diamond(xx, yy, 0.0, 0.0, 0.30)
    altar = np.where((d_altar_ring <= 0.0)[..., None], PALETTES["leech_crimson"][5], altar)
    altar = np.where((d_altar_sigil <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(d_altar_sigil, specular_strength=0.50), PALETTES["leech_crimson"], 0.14, matrix=8), altar)
    for ang in (0.0, 90.0, 180.0, 270.0):
        a = np.deg2rad(ang)
        rune = sdf_capsule(xx, yy, np.cos(a) * 0.62, np.sin(a) * 0.62, np.cos(a) * 0.78, np.sin(a) * 0.78, 0.045)
        altar = np.where((rune <= 0.0)[..., None], PALETTES["leech_crimson"][4], altar)
    altar = np.where((sdf_circle(xx, yy, 0.0, 0.0, 0.12) <= 0.0)[..., None], PALETTES["ember_orange"][6], altar)
    save_rgba(altar, os.path.join(block_dir, "hive_altar.png"))
    _save_pbr_pair(pbr_dir, "hive_altar", np.minimum(d_altar_ring, d_altar_sigil), MATERIALS["OBSIDIAN_CHITIN"],
                   emissive_mask=((d_altar_ring <= 0.0) | (d_altar_sigil <= 0.0)))

    # 10) incubator_node: ribbed incubation sac clamped inside a chitin frame
    d_inc_ref = sdf_circle(xx, yy * 1.15, 0.0, 0.0, 0.42)
    inc_bg = bayer_quantize(np.clip(0.28 + 0.30 * gray_scott_toroidal(n, n, feed=0.052, kill=0.062, steps=120, seed=110), 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.18, matrix=4)
    inc_frames = []
    for p in PULSE_LEVELS:
        out = inc_bg.copy()
        sac = sdf_circle(xx, yy * 1.18, 0.0, 0.0, 0.40)
        out = np.where((sac <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(sac, sss_strength=0.35), PALETTES["mycelial_violet"], 0.14, matrix=8), out)
        for rx in (-0.20, 0.0, 0.20):
            rib = sdf_bezier(xx, yy, (rx * 0.6, -0.34), (rx, 0.05), (rx * 1.1, 0.38), 0.030)
            out = np.where((rib <= 0.0)[..., None], PALETTES["mycelial_violet"][2], out)
        ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.62), 0.050)
        out = np.where((ring <= 0.0)[..., None], PALETTES["chitin_bone"][5], out)
        for ang in (0.0, 90.0, 180.0, 270.0):
            a = np.deg2rad(ang)
            clamp_ = sdf_circle(xx, yy, np.cos(a) * 0.62, np.sin(a) * 0.62, 0.09)
            out = np.where((clamp_ <= 0.0)[..., None], PALETTES["chitin_bone"][4], out)
        core = sdf_circle(xx, yy, 0.0, 0.02, 0.12 + 0.03 * p)
        out = np.where((core <= 0.0)[..., None], bayer_quantize(np.full((n, n), 0.70 + 0.30 * p), PALETTES["spore_chartreuse"], 0.10), out)
        inc_frames.append(out)
    save_flipbook(inc_frames, os.path.join(block_dir, "incubator_node.png"), frametime=5)
    _save_pbr_pair(pbr_dir, "incubator_node", d_inc_ref, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=(sdf_circle(xx, yy, 0.0, 0.02, 0.14) <= 0.0))

    # 11) sentinel_node: armored sentry node with a faceted targeting lens and four prongs
    d_sent_ref = sdf_polygon(xx, yy, 0.0, 0.0, 0.34, sides=8)
    sent_bg = bayer_quantize(np.clip(0.34 + 0.30 * toroidal_value_fbm(n, n, seed=111, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["basalt_slate"], 0.14, matrix=4)
    sent_frames = []
    for p in PULSE_LEVELS:
        out = sent_bg.copy()
        for sx, sy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            plate = sdf_box(xx, yy, sx * 0.58, sy * 0.58, 0.24, 0.10)
            plate2 = sdf_box(xx, yy, sx * 0.58, sy * 0.58, 0.10, 0.24)
            plate = np.minimum(plate, plate2)
            out = np.where((plate <= 0.0)[..., None], PALETTES["chitin_bone"][4], out)
            edge = (plate > -0.06) & (plate <= 0.0)
            out = np.where(edge[..., None], PALETTES["chitin_bone"][6], out)
        ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.60), 0.055)
        out = np.where((ring <= 0.0)[..., None], PALETTES["chitin_bone"][5], out)
        for ang in (45.0, 135.0, 225.0, 315.0):
            a = np.deg2rad(ang)
            prong = sdf_capsule(xx, yy, np.cos(a) * 0.42, np.sin(a) * 0.42, np.cos(a) * 0.72, np.sin(a) * 0.72, 0.065)
            out = np.where((prong <= 0.0)[..., None], PALETTES["chitin_bone"][5], out)
        lens = sdf_polygon(xx, yy, 0.0, 0.0, 0.34 + 0.02 * p, sides=8)
        out = np.where((lens <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(lens, specular_strength=0.55), PALETTES["predator_amethyst"], 0.15, matrix=8), out)
        core = sdf_polygon(xx, yy, 0.0, 0.0, 0.14 + 0.02 * p, sides=8)
        out = np.where((core <= 0.0)[..., None], bayer_quantize(np.full((n, n), 0.75 + 0.25 * p), PALETTES["spore_chartreuse"], 0.10), out)
        sent_frames.append(out)
    save_flipbook(sent_frames, os.path.join(block_dir, "sentinel_node.png"), frametime=5)
    _save_pbr_pair(pbr_dir, "sentinel_node", d_sent_ref, MATERIALS["LIVING_AMETHYST"], emissive_mask=(d_sent_ref <= 0.0))

    # 12) mycelial_rift (16x16 animated portal: deep plum swirl + pale lavender core)
    rift_frames = []
    d_rift = sdf_circle(xx, yy, 0.0, 0.0, 0.72)
    for p in PULSE_LEVELS:
        swirl = 0.5 + 0.5 * np.sin(rad * 10.0 - ang * 3.0 + p * 3.14)
        col_r = bayer_quantize(swirl, PALETTES["predator_amethyst"], 0.20, matrix=8)
        col_c = bayer_quantize(np.clip(1.0 - rad * 1.4, 0.0, 1.0), PALETTES["chitin_bone"], 0.18)
        fr = np.where((rad < 0.35)[..., None], col_c, col_r)
        rift_frames.append(fr)
    save_flipbook(rift_frames, os.path.join(block_dir, "mycelial_rift.png"), frametime=4)
    _save_pbr_pair(pbr_dir, "mycelial_rift", d_rift, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=(rad < 0.75))

    # 13) mycelial_stone: layered organic stone with subtle quartz seams, hairline cracks and pores
    ms_fbm = toroidal_value_fbm(n, n, seed=113, base_cells=3, octaves=3)
    ms_grit = toroidal_value_noise(n, n, 6, 6, seed=1130)
    ms_strata = 0.11 * np.sin(yy * np.pi * 4.0 + ms_fbm * 2.6)
    ms_field = np.clip(0.56 * ms_fbm + 0.28 * ms_grit + ms_strata + 0.06, 0.0, 1.0)
    ms_img = bayer_quantize(ms_field, PALETTES["basalt_slate"], 0.15, matrix=4)
    ms_wx, ms_wy = domain_warp(xx, yy, seed=1131, strength=0.10, freq=2.0)
    ms_vein_a = sdf_bezier(ms_wx, ms_wy, (-0.98, -0.30), (-0.10, 0.28), (0.98, -0.02), 0.026)
    ms_vein = ms_vein_a
    ms_vein_col = bayer_quantize(np.clip(0.32 + 0.14 * ms_fbm, 0.0, 1.0), PALETTES["chitin_bone"], 0.10, matrix=4)
    ms_img = np.where((ms_vein <= 0.0)[..., None], ms_vein_col, ms_img)
    ms_crack = sdf_bezier(xx, yy, (-0.95, 0.55), (-0.20, 0.10), (0.55, 0.72), 0.020)
    ms_img = np.where((ms_crack <= 0.0)[..., None], PALETTES["abyssal_obsidian"][1], ms_img)
    _, ms_pores = poisson_disc_toroidal(n, n, min_dist=4.6, seed=1132)
    ms_pore_mask = ms_pores < 0.20
    ms_rim = (ms_pores >= 0.20) & (ms_pores < 0.34)
    ms_img = np.where(ms_rim[..., None], PALETTES["chitin_bone"][3], ms_img)
    ms_img = np.where(ms_pore_mask[..., None], PALETTES["abyssal_obsidian"][0], ms_img)
    save_rgba(ms_img, os.path.join(block_dir, "mycelial_stone.png"))
    ms_height = np.clip(0.42 + 0.34 * ms_field + np.where(ms_vein <= 0.0, 0.12, 0.0) - np.where(ms_pore_mask, 0.28, 0.0), 0.15, 1.0)
    _save_pbr_pair(pbr_dir, "mycelial_stone", 0.28 - ms_field * 0.40, MATERIALS["OBSIDIAN_CHITIN"], height_map=ms_height)

    # 14) purification_turret: armored housing, vented barrel and amethyst targeting lens
    tur_bg = bayer_quantize(np.clip(0.40 + 0.28 * toroidal_value_fbm(n, n, seed=114, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["platinum_tech"], 0.14, matrix=4)
    housing = sdf_box(xx, yy, -0.06, 0.0, 0.68, 0.62)
    tur = np.where((housing <= 0.0)[..., None], tur_bg, bayer_quantize(np.clip(0.20 + 0.18 * toroidal_value_fbm(n, n, seed=1141, base_cells=3, octaves=3), 0.0, 1.0), PALETTES["basalt_slate"], 0.14))
    tur = np.where((sdf_onion(housing, 0.060) <= 0.0)[..., None], PALETTES["platinum_tech"][6], tur)
    tur = np.where((sdf_onion(housing, 0.018) <= 0.0)[..., None], PALETTES["platinum_tech"][2], tur)
    top_plate = (np.abs(yy + 0.50) < 0.10) & (np.abs(xx + 0.06) < 0.56)
    tur = np.where(top_plate[..., None], PALETTES["chitin_bone"][3], tur)
    tur = np.where((np.abs(yy + 0.58) < 0.030)[..., None], PALETTES["chitin_bone"][5], tur)
    barrel = sdf_capsule(xx, yy, 0.18, 0.0, 0.66, 0.0, 0.135)
    tur = np.where((barrel <= 0.0)[..., None], bayer_quantize(np.clip(0.42 + 0.20 * np.sin(xx * np.pi * 7.0), 0.0, 1.0), PALETTES["basalt_slate"], 0.12, matrix=4), tur)
    for bx in (0.34, 0.50):
        tur = np.where((np.abs(xx - bx) < 0.032)[..., None], PALETTES["platinum_tech"][6], tur)
    muzzle = sdf_circle(xx, yy, 0.60, 0.0, 0.135)
    tur = np.where((muzzle <= 0.0)[..., None], PALETTES["abyssal_obsidian"][2], tur)
    tur = np.where((sdf_onion(muzzle, 0.030) <= 0.0)[..., None], PALETTES["platinum_tech"][6], tur)
    tur = np.where((sdf_circle(xx, yy, 0.60, 0.0, 0.055) <= 0.0)[..., None], PALETTES["spore_chartreuse"][3], tur)
    lens = sdf_circle(xx, yy, -0.30, 0.0, 0.245)
    tur = np.where((lens <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(lens, specular_strength=0.55), PALETTES["predator_amethyst"], 0.16, matrix=8), tur)
    tur = np.where((sdf_circle(xx, yy, -0.30, 0.0, 0.10) <= 0.0)[..., None], PALETTES["warm_ivory"][5], tur)
    for vy in (-0.36, -0.20, -0.04):
        vent = (np.abs(xx + 0.52) < 0.08) & (np.abs(yy - vy) < 0.032)
        tur = np.where(vent[..., None], PALETTES["abyssal_obsidian"][1], tur)
    for rx, ry in ((-0.58, 0.46), (0.02, 0.50), (-0.58, -0.42)):
        tur = np.where((sdf_circle(xx, yy, rx, ry, 0.05) <= 0.0)[..., None], PALETTES["chitin_bone"][5], tur)
    save_rgba(tur, os.path.join(block_dir, "purification_turret.png"))
    _save_pbr_pair(pbr_dir, "purification_turret", np.minimum(housing, barrel), MATERIALS["LIVING_AMETHYST"],
                   emissive_mask=(sdf_circle(xx, yy, -0.30, 0.0, 0.19) <= 0.0), metal_mask=bracket)

    # 15) synaptic_research_bench: chitin bench frame, amethyst inlay grid, reagent wells and central lens
    bench_bg = bayer_quantize(np.clip(0.26 + 0.24 * toroidal_value_fbm(n, n, seed=115, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["abyssal_indigo"], 0.14, matrix=4)
    frame = (np.abs(xx) > 0.76) | (np.abs(yy) > 0.76)
    frame_col = bayer_quantize(np.clip(0.48 + 0.18 * toroidal_value_fbm(n, n, seed=1151, base_cells=5, octaves=2), 0.0, 1.0), PALETTES["chitin_bone"], 0.10, matrix=4)
    bench = np.where(frame[..., None], frame_col, bench_bg)
    edge_hi = frame & ((np.abs(xx) < 0.84) & (xx < 0.0) | (np.abs(yy) < 0.84) & (yy < 0.0))
    bench = np.where(edge_hi[..., None], PALETTES["chitin_bone"][7], bench)
    edge_lo = frame & ((np.abs(xx) < 0.84) & (xx > 0.0) | (np.abs(yy) < 0.84) & (yy > 0.0))
    bench = np.where(edge_lo[..., None], PALETTES["chitin_bone"][2], bench)
    for gx in (-0.30, 0.30):
        grid = (np.abs(xx - gx) < 0.065) & (np.abs(yy) < 0.66) & (~frame)
        bench = np.where(grid[..., None], PALETTES["predator_amethyst"][7], bench)
    for gy in (-0.30, 0.30):
        grid = (np.abs(yy - gy) < 0.065) & (np.abs(xx) < 0.66) & (~frame)
        bench = np.where(grid[..., None], PALETTES["predator_amethyst"][7], bench)
    for gx in (-0.30, 0.30):
        for gy in (-0.30, 0.30):
            well = (np.abs(xx - gx) < 0.075) & (np.abs(yy - gy) < 0.075)
            bench = np.where(well[..., None], PALETTES["abyssal_obsidian"][2], bench)
            bench = np.where((sdf_circle(xx, yy, gx, gy, 0.042) <= 0.0)[..., None], PALETTES["spore_chartreuse"][5], bench)
    inner = (np.abs(np.abs(xx) - 0.68) < 0.060) | (np.abs(np.abs(yy) - 0.68) < 0.060)
    bench = np.where((inner & ~frame)[..., None], PALETTES["predator_amethyst"][4], bench)
    lens = sdf_circle(xx, yy, 0.0, 0.0, 0.26)
    bench = np.where((lens <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(lens, specular_strength=0.55, sss_strength=0.25), PALETTES["predator_amethyst"], 0.15, matrix=8), bench)
    bench = np.where((sdf_circle(xx, yy, 0.0, 0.0, 0.11) <= 0.0)[..., None], PALETTES["warm_ivory"][5], bench)
    for rx, ry in ((-0.64, -0.64), (0.64, -0.64), (-0.64, 0.64), (0.64, 0.64)):
        bench = np.where((sdf_circle(xx, yy, rx, ry, 0.06) <= 0.0)[..., None], PALETTES["volcanic_ember"][3], bench)
    save_rgba(bench, os.path.join(block_dir, "synaptic_research_bench.png"))
    _save_pbr_pair(pbr_dir, "synaptic_research_bench", np.where(frame, -0.30, 0.30 - lens * 0.5), MATERIALS["OBSIDIAN_CHITIN"],
                   height_map=np.where(frame, 0.55, np.where(lens <= 0.0, 0.86, 0.70)),
                   emissive_mask=(lens <= 0.0))

    # 16) symbiotic_node (16x64 animated allied node: chartreuse/emerald core + chitin rings)
    sym_frames = []
    d_sym_ref = sdf_circle(xx, yy, 0.0, 0.0, 0.44)
    _save_pbr_pair(pbr_dir, "symbiotic_node", d_sym_ref, MATERIALS["TOXIC_SPORE"], emissive_mask=(d_sym_ref <= 0.0), metal_mask=bracket)
    for p in PULSE_LEVELS:
        bg_sym = bayer_quantize(toroidal_value_fbm(n, n, seed=116, base_cells=4, octaves=3) * 0.55, PALETTES["basalt_slate"], 0.16)
        d_s_ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.68), 0.11)
        d_s_orb = sdf_circle(xx, yy, 0.0, 0.0, 0.42 + 0.04 * p)
        d_s_core = sdf_circle(xx, yy, 0.0, 0.0, 0.22 + 0.03 * p)
        ring_c = bayer_quantize(compute_sdf_lighting_v2(d_s_ring) * (0.7 + 0.3 * p), PALETTES["chitin_bone"], 0.16)
        orb_c = bayer_quantize(compute_sdf_lighting_v2(d_s_orb, specular_strength=0.42, sss_strength=0.25), PALETTES["spore_chartreuse"], 0.18, matrix=8)
        core_c = bayer_quantize(compute_sdf_lighting_v2(d_s_core, specular_strength=0.5) * (0.8 + 0.2 * p), PALETTES["warm_ivory"], 0.16)
        cross_s = ((np.abs(xx) < 0.12) | (np.abs(yy) < 0.12)) & (rad > 0.42)
        out_s = np.where(cross_s[..., None], bayer_quantize(np.full_like(xx, 0.65 + 0.25 * p), PALETTES["spore_chartreuse"], 0.15), bg_sym)
        out_s = np.where((d_s_ring <= 0.0)[..., None], ring_c, out_s)
        out_s = np.where(bracket[..., None], copper_col, out_s)
        out_s = np.where((d_s_orb <= 0.0)[..., None], orb_c, out_s)
        out_s = np.where((d_s_core <= 0.0)[..., None], core_c, out_s)
        sym_frames.append(out_s)
    save_flipbook(sym_frames, os.path.join(block_dir, "symbiotic_node.png"), frametime=4)

    # 17) chitin_pillar_side & chitin_pillar_top: segmented fluted chitin column with rivets
    flute = 0.5 + 0.5 * np.cos(xx * np.pi * 7.0)
    flute_grit = toroidal_value_fbm(n, n, seed=117, base_cells=4, octaves=3)
    pillar_side_l = np.clip(0.44 + 0.30 * flute + 0.20 * flute_grit, 0.0, 1.0)
    pillar_side = bayer_quantize(pillar_side_l, PALETTES["chitin_bone"], 0.12, matrix=4)
    pillar_side = np.where((np.abs(np.abs(xx) - 0.10) < 0.035)[..., None], PALETTES["chitin_bone"][7], pillar_side)
    pillar_side = np.where((np.abs(np.abs(xx) - 0.22) < 0.030)[..., None], PALETTES["chitin_bone"][2], pillar_side)
    for sy in (-0.56, 0.0, 0.56):
        groove = np.abs(yy - sy) < 0.050
        pillar_side = np.where(groove[..., None], PALETTES["basalt_slate"][2], pillar_side)
        lip = (np.abs(yy - sy - 0.075) < 0.028)
        pillar_side = np.where(lip[..., None], PALETTES["chitin_bone"][6], pillar_side)
    for sy in (-0.56, 0.0, 0.56):
        for sx in (-0.52, 0.52):
            pillar_side = np.where((sdf_circle(xx, yy, sx, sy + 0.14, 0.045) <= 0.0)[..., None], PALETTES["mutant_amber"][5], pillar_side)
    save_rgba(pillar_side, os.path.join(block_dir, "chitin_pillar_side.png"))
    _save_pbr_pair(pbr_dir, "chitin_pillar_side", 0.25 - pillar_side_l * 0.34, MATERIALS["CHITIN_BONE"],
                   height_map=np.clip(0.38 + 0.42 * flute, 0.2, 1.0))

    d_ptop = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.62), 0.14)
    d_pcore = sdf_circle(xx, yy, 0.0, 0.0, 0.24)
    ptop_bg = bayer_quantize(toroidal_value_fbm(n, n, seed=118, base_cells=4, octaves=3) * 0.7, PALETTES["basalt_slate"], 0.16)
    ptop_ring = bayer_quantize(compute_sdf_lighting_v2(d_ptop), PALETTES["chitin_bone"], 0.18)
    ptop_core = bayer_quantize(compute_sdf_lighting_v2(d_pcore, sss_strength=0.3), PALETTES["mutant_amber"], 0.18)
    ptop = np.where((d_ptop <= 0.0)[..., None], ptop_ring, ptop_bg)
    ptop = np.where((d_pcore <= 0.0)[..., None], ptop_core, ptop)
    save_rgba(ptop, os.path.join(block_dir, "chitin_pillar_top.png"))
    _save_pbr_pair(pbr_dir, "chitin_pillar_top", np.minimum(d_ptop, d_pcore), MATERIALS["OBSIDIAN_CHITIN"], emissive_mask=(d_pcore <= 0.0))

    # 18) mycelial_arch_brick (16x16 dark carved bricks with glowing violet mortar)
    brick_row = ((yy + 1.0) * 2.0).astype(np.int32)
    x_shift = np.where((brick_row % 2) == 1, 0.5, 0.0)
    ux = ((xx + x_shift + 1.0) % 1.0) - 0.5
    uy = ((yy + 1.0) % 0.5) - 0.25
    mortar_mask = (np.abs(ux) > 0.43) | (np.abs(uy) > 0.19)
    brick_noise = toroidal_value_fbm(n, n, seed=119, base_cells=4, octaves=3) * 0.65 + 0.16
    brick_col = bayer_quantize(np.clip(brick_noise, 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.14)
    mortar_noise = toroidal_value_fbm(n, n, seed=1191, base_cells=4, octaves=2)
    mortar_col = bayer_quantize(np.clip(0.45 + 0.35 * mortar_noise, 0.0, 1.0), PALETTES["mycelial_violet"], 0.14)
    arch_brick = np.where(mortar_mask[..., None], mortar_col, brick_col)
    save_rgba(arch_brick, os.path.join(block_dir, "mycelial_arch_brick.png"))
    _save_pbr_pair(pbr_dir, "mycelial_arch_brick", np.where(mortar_mask, 0.18, -0.25).astype(np.float32), MATERIALS["OBSIDIAN_CHITIN"], emissive_mask=mortar_mask)

    # 19) bio_chitin_lattice (16x16 cutout abyssal obsidian + amethyst-violet filigree)
    border_lat = (np.abs(xx) > 0.80) | (np.abs(yy) > 0.80)
    diag_lat = (np.abs(np.abs(xx) - np.abs(yy)) < 0.13) | (np.abs(xx) < 0.09) | (np.abs(yy) < 0.09)
    lat_solid = border_lat | diag_lat
    lat_glow = diag_lat & (~border_lat) & (rad < 0.56)
    lattice = np.zeros((n, n, 4), dtype=np.uint8)
    lat_obs = bayer_quantize(np.clip(0.45 + 0.30 * toroidal_value_fbm(n, n, seed=120, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.16)
    lat_bone = bayer_quantize(np.clip(0.55 + 0.25 * toroidal_value_fbm(n, n, seed=120, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["chitin_bone"], 0.16)
    lat_amethyst = bayer_quantize(np.clip(0.75 - rad * 0.35, 0.25, 0.92), PALETTES["predator_amethyst"], 0.16)
    lattice = np.where(diag_lat[..., None], lat_bone, lattice)
    lattice = np.where(border_lat[..., None], lat_obs, lattice)
    lattice = np.where(lat_glow[..., None], lat_amethyst, lattice)
    save_rgba(lattice, os.path.join(block_dir, "bio_chitin_lattice.png"))
    _save_pbr_pair(pbr_dir, "bio_chitin_lattice", np.where(lat_solid, -0.25, 0.35).astype(np.float32), MATERIALS["LIVING_AMETHYST"], emissive_mask=lat_glow)

    # 20) luminescent_amber_vein (16x16 animated pulsing amber veins in dark volcanic rock)
    amb_b1 = sdf_bezier(wx, wy, (-0.9, -0.6), (0.0, 0.25), (0.9, -0.35), 0.15)
    amb_b2 = sdf_bezier(wx, wy, (-0.4, 0.9), (0.15, -0.2), (0.55, -0.9), 0.14)
    amb_sdf = np.minimum(amb_b1, amb_b2)
    amb_mask = amb_sdf <= 0.0
    amb_bg = bayer_quantize(toroidal_value_fbm(n, n, seed=121, base_cells=4, octaves=3) * 0.72, PALETTES["abyssal_obsidian"], 0.14)
    amb_frames = []
    for p in PULSE_LEVELS:
        amb_lit = compute_sdf_lighting_v2(amb_sdf, sss_strength=0.40) * (0.72 + 0.28 * p)
        amb_col = bayer_quantize(amb_lit, PALETTES["mutant_amber"], 0.14, matrix=8)
        amb_frames.append(np.where(amb_mask[..., None], amb_col, amb_bg))
    save_flipbook(amb_frames, os.path.join(block_dir, "luminescent_amber_vein.png"), frametime=5)
    _save_pbr_pair(pbr_dir, "luminescent_amber_vein", amb_sdf, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=amb_mask)

    # 21) matriarch_altar_top & matriarch_altar_side (16x16)
    d_mat_ring = sdf_onion(sdf_polygon(xx, yy, 0.0, 0.0, 0.68, sides=8), 0.10)
    d_mat_core = sdf_diamond(xx, yy, 0.0, 0.0, 0.36)
    mat_bg = bayer_quantize(toroidal_value_fbm(n, n, seed=122, base_cells=4, octaves=3) * 0.5, PALETTES["abyssal_obsidian"], 0.16)
    mat_ring = bayer_quantize(compute_sdf_lighting_v2(d_mat_ring, specular_strength=0.5), PALETTES["predator_amethyst"], 0.18)
    mat_core = bayer_quantize(compute_sdf_lighting_v2(d_mat_core, specular_strength=0.55), PALETTES["mutant_amber"], 0.18)
    mat_top = np.where((d_mat_ring <= 0.0)[..., None], mat_ring, mat_bg)
    mat_top = np.where((d_mat_core <= 0.0)[..., None], mat_core, mat_top)
    save_rgba(mat_top, os.path.join(block_dir, "matriarch_altar_top.png"))
    _save_pbr_pair(pbr_dir, "matriarch_altar_top", np.minimum(d_mat_ring, d_mat_core), MATERIALS["LIVING_AMETHYST"], emissive_mask=((d_mat_ring <= 0.0) | (d_mat_core <= 0.0)))

    d_mat_rune = sdf_diamond(xx, yy, 0.0, 0.0, 0.30)
    mat_side_bg = bayer_quantize(np.clip(0.26 + 0.28 * toroidal_value_fbm(n, n, seed=123, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.14, matrix=4)
    mat_side = mat_side_bg.copy()
    trim_top = yy < -0.74
    trim_bot = yy > 0.74
    mat_side = np.where((trim_top | trim_bot)[..., None], PALETTES["chitin_bone"][4], mat_side)
    mat_side = np.where(((np.abs(yy) > 0.74) & (np.abs(yy) < 0.80))[..., None], PALETTES["chitin_bone"][6], mat_side)
    mat_side = np.where((np.abs(yy + 0.86) < 0.025)[..., None], PALETTES["predator_amethyst"][5], mat_side)
    mat_side = np.where((np.abs(yy - 0.86) < 0.025)[..., None], PALETTES["predator_amethyst"][5], mat_side)
    mat_rune_col = bayer_quantize(compute_sdf_lighting_v2(d_mat_rune, sss_strength=0.30, specular_strength=0.45), PALETTES["elite_crimson"], 0.14, matrix=8)
    mat_side = np.where((d_mat_rune <= 0.0)[..., None], mat_rune_col, mat_side)
    mat_side = np.where((sdf_diamond(xx, yy, 0.0, 0.0, 0.11) <= 0.0)[..., None], PALETTES["ember_orange"][6], mat_side)
    for gx in (-0.44, 0.44):
        glyph = sdf_diamond(xx, yy, gx, 0.0, 0.10)
        mat_side = np.where((glyph <= 0.0)[..., None], PALETTES["predator_amethyst"][6], mat_side)
    crack = sdf_bezier(xx, yy, (-0.90, -0.20), (-0.30, 0.10), (-0.52, 0.66), 0.020)
    mat_side = np.where((crack <= 0.0)[..., None], PALETTES["mycelial_violet"][4], mat_side)
    crack2 = sdf_bezier(xx, yy, (0.90, 0.30), (0.34, -0.10), (0.52, -0.60), 0.018)
    mat_side = np.where((crack2 <= 0.0)[..., None], PALETTES["mycelial_violet"][4], mat_side)
    save_rgba(mat_side, os.path.join(block_dir, "matriarch_altar_side.png"))
    _save_pbr_pair(pbr_dir, "matriarch_altar_side", d_mat_rune, MATERIALS["OBSIDIAN_CHITIN"],
                   emissive_mask=(d_mat_rune <= 0.0))

    # 22) antibody_cyst (16x64 animated pulsing organic immune capsule) & antibody_cyst_withered (16x16)
    cyst_gs = gray_scott_toroidal(n, n, feed=0.052, kill=0.061, steps=140, seed=124)
    cyst_v1 = sdf_bezier(wx, wy, (-0.95, -0.55), (0.05, 0.35), (0.95, -0.25), 0.16)
    cyst_v2 = sdf_bezier(wx, wy, (-0.35, 0.95), (0.20, -0.15), (0.60, -0.95), 0.14)
    cyst_core = sdf_circle(xx, yy, 0.0, 0.0, 0.36)
    cyst_veins = np.minimum(cyst_v1, cyst_v2)
    cyst_sdf = np.minimum(cyst_veins, cyst_core)
    cyst_glow_mask = (cyst_veins <= 0.0) | (cyst_core <= -0.08)
    cyst_bg = bayer_quantize(np.clip(cyst_gs * 0.78 + 0.12, 0.0, 1.0), PALETTES["elite_carapace"], 0.22)
    cyst_frames = []
    for p in PULSE_LEVELS:
        vein_lit = compute_sdf_lighting_v2(cyst_veins, sss_strength=0.35) * (0.70 + 0.30 * p)
        core_lit = compute_sdf_lighting_v2(cyst_core, sss_strength=0.45) * (0.68 + 0.32 * p)
        vein_col = bayer_quantize(vein_lit, PALETTES["mutant_amber"], 0.18, matrix=8)
        core_col = bayer_quantize(core_lit, PALETTES["elite_crimson"], 0.18, matrix=8)
        frame = np.where((cyst_veins <= 0.0)[..., None], vein_col, cyst_bg)
        frame = np.where((cyst_core <= 0.0)[..., None], core_col, frame)
        cyst_frames.append(frame)
    save_flipbook(cyst_frames, os.path.join(block_dir, "antibody_cyst.png"), frametime=5)
    _save_pbr_pair(pbr_dir, "antibody_cyst", cyst_sdf, MATERIALS["BIOLUM_MYCELIUM"], emissive_mask=cyst_glow_mask)

    wit_fbm = toroidal_value_fbm(n, n, seed=1241, base_cells=3, octaves=3)
    wit_bevel, wit_seam = cell_plate_field(n, n, seed=1242, cells=5)
    wither_bg = bayer_quantize(np.clip(0.34 + 0.30 * wit_fbm + 0.10 * (1.0 - wit_seam), 0.0, 1.0), PALETTES["basalt_slate"], 0.18, matrix=4)
    wit_umber = bayer_quantize(np.clip(0.26 + 0.20 * wit_fbm, 0.0, 1.0), PALETTES["necrotic_umber"], 0.16, matrix=4)
    wither_bg = np.where((wit_bevel > 0.70)[..., None], wit_umber, wither_bg)
    wit_cyst = np.minimum(sdf_circle(xx, yy, -0.06, 0.02, 0.52), sdf_circle(xx, yy, 0.20, -0.10, 0.36))
    wit_cyst = np.minimum(wit_cyst, np.minimum(sdf_circle(xx, yy, -0.26, 0.18, 0.32), sdf_box(xx, yy, 0.0, 0.10, 0.46, 0.36)))
    withered = np.where((wit_cyst <= 0.0)[..., None], bayer_quantize(np.clip(0.28 + 0.20 * wit_fbm, 0.0, 1.0), PALETTES["elite_carapace"], 0.14, matrix=8), wither_bg)
    withered = np.where((sdf_onion(wit_cyst, 0.050) <= 0.0)[..., None], PALETTES["elite_carapace"][6], withered)
    membrane = (sdf_onion(wit_cyst, 0.050) <= 0.0) & ((xx < -0.02) | (yy < -0.02))
    withered = np.where(membrane[..., None], PALETTES["elite_carapace"][7], withered)
    withered = np.where(((wit_cyst <= 0.0) & (xx > 0.10) & (yy > 0.10))[..., None], PALETTES["elite_carapace"][2], withered)
    withered = np.where((sdf_onion(wit_cyst, 0.015) <= 0.0)[..., None], PALETTES["abyssal_obsidian"][1], withered)
    for wx_, wy_, wcx, wcy in ((-0.32, -0.18, -0.06, 0.34), (-0.10, 0.12, 0.32, 0.26), (0.12, -0.22, 0.28, 0.12), (-0.28, 0.16, -0.10, 0.44)):
        crease = sdf_bezier(xx, yy, (wx_, wy_), (-0.02, 0.04), (wcx, wcy), 0.026)
        withered = np.where((crease <= 0.0)[..., None], PALETTES["abyssal_obsidian"][2], withered)
        ridge = sdf_bezier(xx, yy, (wx_ + 0.05, wy_ - 0.05), (0.02, 0.02), (wcx + 0.04, wcy - 0.04), 0.014)
        withered = np.where(((ridge <= 0.0) & (wit_cyst <= 0.0))[..., None], PALETTES["elite_carapace"][6], withered)
    for nx_, ny_ in ((-0.32, 0.24), (0.26, -0.26), (0.04, 0.38)):
        spot = sdf_circle(xx, yy, nx_, ny_, 0.085)
        withered = np.where((spot <= 0.0)[..., None], PALETTES["necrotic_umber"][2], withered)
        withered = np.where((sdf_onion(spot, 0.022) <= 0.0)[..., None], PALETTES["mutant_amber"][2], withered)
    save_rgba(withered, os.path.join(block_dir, "antibody_cyst_withered.png"))

    # 23) hyphal_vein_{dot,line,climb}: 16x16 cutout grayscale-tintable thread-like hyphal filaments
    vein_noise = toroidal_value_fbm(n, n, seed=125, base_cells=5, octaves=2)

    def _paint_lightning_vein(segs: tuple[tuple[float, float, float, float, float], ...],
                              nodes: tuple[tuple[float, float, float], ...]) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        d_core = np.full((n, n), 10.0, dtype=np.float32)
        for ax, ay, bx, by, rad_s in segs:
            d_core = np.minimum(d_core, sdf_capsule(xx, yy, ax, ay, bx, by, rad_s * 0.62))
        for cx_, cy_, rad_n in nodes:
            d_core = np.minimum(d_core, sdf_circle(xx, yy, cx_, cy_, rad_n * 0.68))
        rim_mask = (d_core > 0.0) & (d_core <= 0.068)
        core_mask = d_core <= 0.0
        spine_mask = d_core <= -0.024
        pore_mask = np.zeros((n, n), dtype=bool)
        for cx_, cy_, _ in nodes:
            pore_mask |= sdf_circle(xx, yy, cx_, cy_, 0.058) <= 0.0

        # Neutral-to-bright grayscale ramp so BlockColors tintindex=0 tints cleanly from faint to intense
        tintable_ramp = np.array(
            [
                [112, 108, 120, 255],
                [142, 138, 152, 255],
                [174, 170, 186, 255],
                [204, 200, 214, 255],
                [228, 224, 238, 255],
                [248, 246, 254, 255],
            ],
            dtype=np.uint8,
        )
        rim_lit = np.clip(0.08 + 0.34 * vein_noise, 0.05, 0.45)
        rim_col = bayer_quantize(rim_lit, tintable_ramp, 0.14, matrix=4)
        body_lit = np.clip(0.52 + 0.36 * vein_noise, 0.45, 0.92)
        body_col = bayer_quantize(body_lit, tintable_ramp, 0.14, matrix=4)
        spine_col = np.array([246, 244, 252, 255], dtype=np.uint8)
        pore_col = np.array([255, 255, 255, 255], dtype=np.uint8)

        canvas = np.zeros((n, n, 4), dtype=np.uint8)
        canvas = np.where(rim_mask[..., None], rim_col, canvas)
        canvas = np.where(core_mask[..., None], body_col, canvas)
        canvas = np.where(spine_mask[..., None], spine_col, canvas)
        canvas = np.where(pore_mask[..., None], pore_col, canvas)
        return canvas, d_core, pore_mask

    dot_img, dot_sdf, dot_glow = _paint_lightning_vein(
        (
            (-0.12, -0.04, -0.52, -0.34, 0.085),
            (-0.04, -0.10, 0.34, -0.42, 0.085),
            (0.08, 0.06, 0.46, 0.26, 0.085),
            (-0.06, 0.08, -0.36, 0.44, 0.085),
        ),
        ((0.0, 0.0, 0.18), (-0.42, -0.28, 0.09), (0.38, 0.20, 0.09)),
    )
    save_rgba(dot_img, os.path.join(block_dir, "hyphal_vein_dot.png"))
    _save_pbr_pair(pbr_dir, "hyphal_vein_dot", dot_sdf, MATERIALS["OBSIDIAN_CHITIN"], emissive_mask=dot_glow)

    line_img, line_sdf, line_glow = _paint_lightning_vein(
        (
            (0.00, 0.06, -0.20, -0.24, 0.095),
            (-0.20, -0.24, 0.16, -0.54, 0.095),
            (0.16, -0.54, -0.12, -0.80, 0.095),
            (-0.12, -0.80, 0.02, -1.00, 0.095),
            (-0.14, -0.34, -0.52, -0.60, 0.075),
            (0.14, -0.56, 0.48, -0.82, 0.075),
        ),
        ((-0.20, -0.24, 0.11), (0.16, -0.54, 0.11)),
    )
    save_rgba(line_img, os.path.join(block_dir, "hyphal_vein_line.png"))
    _save_pbr_pair(pbr_dir, "hyphal_vein_line", line_sdf, MATERIALS["OBSIDIAN_CHITIN"], emissive_mask=line_glow)

    climb_img, climb_sdf, climb_glow = _paint_lightning_vein(
        (
            (0.00, 1.00, -0.18, 0.62, 0.095),
            (-0.18, 0.62, 0.20, 0.18, 0.095),
            (0.20, 0.18, -0.16, -0.26, 0.095),
            (-0.16, -0.26, 0.14, -0.68, 0.095),
            (0.14, -0.68, 0.00, -1.00, 0.095),
            (0.16, 0.22, 0.50, -0.02, 0.075),
            (-0.14, -0.30, -0.48, -0.54, 0.075),
        ),
        ((0.20, 0.18, 0.11), (-0.16, -0.26, 0.11)),
    )
    save_rgba(climb_img, os.path.join(block_dir, "hyphal_vein_climb.png"))
    _save_pbr_pair(pbr_dir, "hyphal_vein_climb", climb_sdf, MATERIALS["OBSIDIAN_CHITIN"], emissive_mask=climb_glow)

    # 24) sovereign_nexus_{top (16x64 animated), side (16x16), bottom (16x16)}
    sn_fbm = toroidal_value_fbm(n, n, seed=131, base_cells=4, octaves=3)
    sn_bg = bayer_quantize(np.clip(0.24 + 0.34 * sn_fbm, 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.16, matrix=4)
    sn_bezel = sdf_onion(sdf_polygon(xx, yy, 0.0, 0.0, 0.72, sides=8), 0.11)
    sn_ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.44), 0.065)
    sn_core_ref = sdf_diamond(xx, yy, 0.0, 0.0, 0.32)
    sn_metal_top = (sn_bezel <= 0.0) | ((np.abs(xx) > 0.74) & (np.abs(yy) > 0.74))
    sn_bezel_col = bayer_quantize(compute_sdf_lighting_v2(sn_bezel, specular_strength=0.60), PALETTES["royal_gold"], 0.14, matrix=4)
    sn_corner_col = bayer_quantize(np.clip(0.55 + 0.25 * sn_fbm, 0.0, 1.0), PALETTES["imperial_brass"], 0.14, matrix=4)
    sn_top_frames = []
    for p in PULSE_LEVELS:
        out = sn_bg.copy()
        out = np.where(((np.abs(xx) > 0.74) & (np.abs(yy) > 0.74))[..., None], sn_corner_col, out)
        out = np.where((sn_bezel <= 0.0)[..., None], sn_bezel_col, out)
        ring_col = bayer_quantize(np.clip(0.62 + 0.35 * p - sn_ring * 0.4, 0.0, 1.0), PALETTES["spore_chartreuse"], 0.14, matrix=8)
        out = np.where((sn_ring <= 0.0)[..., None], ring_col, out)
        d_core = sdf_diamond(xx, yy, 0.0, 0.0, 0.28 + 0.04 * p)
        core_col = bayer_quantize(compute_sdf_lighting_v2(d_core, sss_strength=0.40, specular_strength=0.55) * (0.72 + 0.28 * p), PALETTES["purification_aqua"], 0.16, matrix=8)
        out = np.where((d_core <= 0.0)[..., None], core_col, out)
        out = np.where((sdf_circle(xx, yy, 0.0, 0.0, 0.10) <= 0.0)[..., None], PALETTES["warm_ivory"][6], out)
        sn_top_frames.append(out)
    save_flipbook(sn_top_frames, os.path.join(block_dir, "sovereign_nexus_top.png"), frametime=4)
    _save_pbr_pair(pbr_dir, "sovereign_nexus_top", np.minimum(sn_bezel, sn_core_ref), MATERIALS["PURIFICATION_GOLD"],
                   emissive_mask=((sn_ring <= 0.0) | (sn_core_ref <= 0.0)), metal_mask=sn_metal_top)

    sn_side_bg = bayer_quantize(np.clip(0.28 + 0.30 * toroidal_value_fbm(n, n, seed=132, base_cells=4, octaves=3), 0.0, 1.0), PALETTES["basalt_slate"], 0.14, matrix=4)
    sn_collar = (np.abs(yy) > 0.68)
    sn_side_metal = sn_collar | ((np.abs(xx) > 0.72) & (np.abs(yy) < 0.72))
    sn_side = np.where(sn_collar[..., None], sn_corner_col, sn_side_bg)
    sn_side = np.where(((np.abs(xx) > 0.72) & (np.abs(yy) < 0.72))[..., None], sn_bezel_col, sn_side)
    sn_side = np.where((np.abs(np.abs(yy) - 0.70) < 0.04)[..., None], PALETTES["chitin_bone"][5], sn_side)
    sn_conduit = (np.abs(yy) < 0.08) & (np.abs(xx) < 0.72)
    sn_side = np.where(sn_conduit[..., None], PALETTES["spore_chartreuse"][5], sn_side)
    d_side_core = sdf_polygon(xx, yy, 0.0, 0.0, 0.36, sides=6)
    sn_side = np.where((d_side_core <= 0.0)[..., None], bayer_quantize(compute_sdf_lighting_v2(d_side_core, specular_strength=0.55), PALETTES["purification_aqua"], 0.14, matrix=8), sn_side)
    sn_side = np.where((sdf_diamond(xx, yy, 0.0, 0.0, 0.14) <= 0.0)[..., None], PALETTES["warm_ivory"][6], sn_side)
    save_rgba(sn_side, os.path.join(block_dir, "sovereign_nexus_side.png"))
    _save_pbr_pair(pbr_dir, "sovereign_nexus_side", d_side_core, MATERIALS["OBSIDIAN_CHITIN"],
                   emissive_mask=(d_side_core <= 0.0) | sn_conduit, metal_mask=sn_side_metal)

    sn_bot_fbm = toroidal_value_fbm(n, n, seed=133, base_cells=3, octaves=3)
    sn_bot = bayer_quantize(np.clip(0.24 + 0.32 * sn_bot_fbm, 0.0, 1.0), PALETTES["abyssal_obsidian"], 0.14, matrix=4)
    d_bot_clamp = sdf_onion(sdf_box(xx, yy, 0.0, 0.0, 0.76, 0.76), 0.12)
    sn_bot = np.where((d_bot_clamp <= 0.0)[..., None], sn_corner_col, sn_bot)
    d_bot_ring = sdf_onion(sdf_circle(xx, yy, 0.0, 0.0, 0.42), 0.08)
    sn_bot = np.where((d_bot_ring <= 0.0)[..., None], PALETTES["chitin_bone"][4], sn_bot)
    d_bot_pore = sdf_circle(xx, yy, 0.0, 0.0, 0.18)
    sn_bot = np.where((d_bot_pore <= 0.0)[..., None], PALETTES["purification_aqua"][5], sn_bot)
    save_rgba(sn_bot, os.path.join(block_dir, "sovereign_nexus_bottom.png"))
    _save_pbr_pair(pbr_dir, "sovereign_nexus_bottom", np.minimum(d_bot_clamp, d_bot_pore), MATERIALS["OBSIDIAN_CHITIN"],
                   emissive_mask=(d_bot_pore <= 0.0), metal_mask=(d_bot_clamp <= 0.0))