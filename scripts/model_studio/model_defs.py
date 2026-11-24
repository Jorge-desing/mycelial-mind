"""Definitions for all 17 evolved legendary multi-articulated 3D EntityModels."""

from __future__ import annotations

from typing import List
from .uv_packer import CubeSpec, ModelDef, PartNode


def _biped_legs(
    y_hip: float = 12.0,
    x_off: float = 2.5,
    w: int = 4,
    h: int = 12,
    d: int = 4,
    role: str = "armor",
    knee_guard: bool = False,
) -> tuple[PartNode, PartNode]:
    r_cubes = [CubeSpec("right_leg", (-w / 2, 0, -d / 2), (w, h, d), role=role)]
    l_cubes = [CubeSpec("left_leg", (-w / 2, 0, -d / 2), (w, h, d), role=role, mirror=True)]
    if knee_guard:
        r_cubes.append(CubeSpec("right_knee", (-w / 2 - 0.5, h * 0.35, -d / 2 - 0.5), (w + 1, 3, d + 1), role="bone", inflation=0.1))
        l_cubes.append(CubeSpec("left_knee", (-w / 2 - 0.5, h * 0.35, -d / 2 - 0.5), (w + 1, 3, d + 1), role="bone", inflation=0.1, mirror=True))
    return (
        PartNode("right_leg", pivot=(-x_off, y_hip, 0.0), cubes=tuple(r_cubes), anim_role="biped_leg_r"),
        PartNode("left_leg", pivot=(x_off, y_hip, 0.0), cubes=tuple(l_cubes), anim_role="biped_leg_l"),
    )


def _arachnid_leg(
    name: str,
    pivot: tuple[float, float, float],
    yaw: float,
    side: str,
    phase: float,
    coxa_len: int = 5,
    tibia_len: int = 6,
    role: str = "armor",
    thick: int = 2,
) -> PartNode:
    sign = -1.0 if side == "r" else 1.0
    coxa_ox = -coxa_len if side == "r" else 0
    tibia_px = sign * coxa_len
    tibia_ox = -tibia_len if side == "r" else 0
    mir = side == "l"
    return PartNode(
        name,
        pivot=pivot,
        rotation=(0.0, yaw, sign * -0.22),
        anim_role=f"arachnid_coxa_{side}",
        anim_phase=phase,
        cubes=(
            CubeSpec(f"{name}_coxa", (coxa_ox, -thick / 2, -thick / 2), (coxa_len, thick, thick), role=role, mirror=mir),
        ),
        children=(
            PartNode(
                f"{name}_tibia",
                pivot=(tibia_px, 0.0, 0.0),
                rotation=(0.0, 0.0, sign * 0.72),
                anim_role="arachnid_tibia",
                anim_phase=phase,
                cubes=(
                    CubeSpec(f"{name}_tibia", (tibia_ox, -1, -1), (tibia_len, 2, 2), role="bone", mirror=mir),
                    CubeSpec(f"{name}_claw", (tibia_ox + (0 if side == "r" else tibia_len - 2), 0.5, -0.5), (2, 2, 1), role="glow_eye", emissive=True, mirror=mir),
                ),
            ),
        ),
    )


def build_all_models() -> List[ModelDef]:
    models: List[ModelDef] = []

    # 1) SporelingModel (128x128): Common fungal mite — pure Mycelium Mauve + Mushroom Ivory (no rare metals)
    models.append(ModelDef(
        class_name="SporelingModel",
        registry_id="sporeling",
        description="Legendary fungal mite with 3-tier glowing cap, under-gills, sensory antennae, and rank crystals",
        tex_size=(128, 128),
        palettes={"skin": "abyssal_indigo", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "chitin_bone"},
        parts=(
            PartNode("body", pivot=(0.0, 20.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("body", (-3, -5, -4), (6, 5, 8), role="skin", eye_pattern="two_eyes", detail_tags=("neural_veins",)),
                CubeSpec("belly_core", (-2, -3, -3), (4, 3, 6), role="glow_core", emissive=True, inflation=-0.2),
                CubeSpec("cap_stalk", (-2, -6, -2), (4, 2, 4), role="bone"),
                CubeSpec("hip_fl", (-3.5, -1, -3.5), (2, 2, 2), role="armor"),
                CubeSpec("hip_fr", (1.5, -1, -3.5), (2, 2, 2), role="armor", mirror=True),
                CubeSpec("hip_bl", (-3.5, -1, 1.5), (2, 2, 2), role="armor"),
                CubeSpec("hip_br", (1.5, -1, 1.5), (2, 2, 2), role="armor", mirror=True),
            ), children=(
                PartNode("cap", pivot=(0.0, -5.0, 0.0), anim_role="sac_pulse", cubes=(
                    CubeSpec("cap_gills", (-4, -1, -4), (8, 1, 8), role="glow_core", emissive=True),
                    CubeSpec("cap_rim", (-5, -4, -5), (10, 3, 10), role="spore_sac"),
                    CubeSpec("cap_dome", (-3, -7, -3), (6, 3, 6), role="spore_sac", emissive=True),
                    CubeSpec("cap_spire", (-1, -9, -1), (2, 2, 2), role="bone", detail_tags=("crystal_facets",)),
                ), children=(
                    PartNode("mutated_spores", pivot=(0.0, -4.0, 0.0), rank_min="MUTATED", anim_role="sac_pulse", anim_phase=1.2, cubes=(
                        CubeSpec("spore_lump_r", (-6, -1, -1), (2, 2, 2), role="glow_core", emissive=True),
                        CubeSpec("spore_lump_l", (4, -1, 1), (2, 2, 2), role="glow_core", emissive=True, mirror=True),
                    )),
                    PartNode("elite_crown", pivot=(0.0, -9.0, 0.0), rank_min="ELITE", anim_role="orbit_ring_a", cubes=(
                        CubeSpec("crown_shard_f", (-1, -3, -3), (2, 3, 1), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                        CubeSpec("crown_shard_b", (-1, -3, 2), (2, 3, 1), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                    )),
                )),
                PartNode("antenna_r", pivot=(-2.0, -4.0, -4.0), rotation=(-0.45, 0.25, 0.0), anim_role="tendril_sway", anim_phase=0.0, cubes=(
                    CubeSpec("antenna_r", (-0.5, -5, -0.5), (1, 5, 1), role="glow_core", emissive=True),
                )),
                PartNode("antenna_l", pivot=(2.0, -4.0, -4.0), rotation=(-0.45, -0.25, 0.0), anim_role="tendril_sway", anim_phase=1.5, cubes=(
                    CubeSpec("antenna_l", (-0.5, -5, -0.5), (1, 5, 1), role="glow_core", emissive=True, mirror=True),
                )),
            )),
            PartNode("leg_fl", pivot=(-2.5, 20.0, -2.5), anim_role="quad_leg_a", anim_phase=0.0, cubes=(CubeSpec("leg_fl", (-1, 0, -1), (2, 4, 2), role="armor"),)),
            PartNode("leg_fr", pivot=(2.5, 20.0, -2.5), anim_role="quad_leg_b", anim_phase=0.0, cubes=(CubeSpec("leg_fr", (-1, 0, -1), (2, 4, 2), role="armor", mirror=True),)),
            PartNode("leg_bl", pivot=(-2.5, 20.0, 2.5), anim_role="quad_leg_b", anim_phase=0.4, cubes=(CubeSpec("leg_bl", (-1, 0, -1), (2, 4, 2), role="armor"),)),
            PartNode("leg_br", pivot=(2.5, 20.0, 2.5), anim_role="quad_leg_a", anim_phase=0.4, cubes=(CubeSpec("leg_br", (-1, 0, -1), (2, 4, 2), role="armor", mirror=True),)),
        ),
    ))

    # 2) LeechParasiteModel (128x128): raised arched head, sleek dorsal ridge, unified Leech Crimson + Obsidian
    models.append(ModelDef(
        class_name="LeechParasiteModel",
        registry_id="leech_parasite",
        description="Armored blood-leech with raised snapping mandibles, 4 undulating segments, lateral fins, and dorsal ridge",
        tex_size=(128, 128),
        palettes={"skin": "leech_crimson", "armor": "abyssal_obsidian", "bone": "abyssal_obsidian", "spore_sac": "leech_crimson", "glow_eye": "leech_crimson", "glow_core": "leech_crimson"},
        parts=(
            PartNode("head", pivot=(0.0, 20.0, -5.0), rotation=(-0.12, 0.0, 0.0), anim_role="leech_wave", anim_phase=0.0, cubes=(
                CubeSpec("head", (-2.5, -2.5, -3), (5, 4, 4), role="skin", eye_pattern="two_eyes"),
                CubeSpec("head_crest", (-1.5, -3.5, -2), (3, 1, 3), role="armor"),
            ), children=(
                PartNode("fang_r", pivot=(-1.8, 0.5, -3.0), rotation=(0.25, 0.25, 0.0), anim_role="mandible_r", cubes=(CubeSpec("fang_r", (-0.5, -0.5, -2.5), (1, 1, 3), role="armor"),)),
                PartNode("fang_l", pivot=(1.8, 0.5, -3.0), rotation=(0.25, -0.25, 0.0), anim_role="mandible_l", anim_phase=0.6, cubes=(CubeSpec("fang_l", (-0.5, -0.5, -2.5), (1, 1, 3), role="armor", mirror=True),)),
                PartNode("proboscis", pivot=(0.0, 1.0, -3.0), anim_role="jaw", anim_phase=0.3, cubes=(CubeSpec("proboscis", (-0.5, -0.5, -2), (1, 1, 2), role="glow_core", emissive=True),)),
            )),
            PartNode("mid", pivot=(0.0, 21.5, -0.5), anim_role="leech_wave", anim_phase=0.7, cubes=(
                CubeSpec("mid", (-3, -2, -3), (6, 4, 6), role="skin"),
                CubeSpec("neck_ring", (-2, -1.5, -4.5), (4, 3, 2), role="armor"),
                CubeSpec("fin_r", (-4.5, 0.5, -2), (2, 1, 4), role="armor"),
                CubeSpec("fin_l", (2.5, 0.5, -2), (2, 1, 4), role="armor", mirror=True),
            ), children=(
                PartNode("sac", pivot=(0.0, -2.0, 0.0), anim_role="sac_pulse", cubes=(
                    CubeSpec("sac", (-1.5, -1.5, -2), (3, 2, 4), role="glow_core", emissive=True),
                )),
                PartNode("mutated_spines", pivot=(0.0, -2.5, 0.0), rank_min="MUTATED", cubes=(
                    CubeSpec("spine_m1", (-0.5, -2.5, -2), (1, 2, 1), role="armor"),
                    CubeSpec("spine_m2", (-0.5, -2.5, 1), (1, 2, 1), role="armor"),
                )),
            )),
            PartNode("rear", pivot=(0.0, 22.0, 4.5), anim_role="leech_wave", anim_phase=1.4, cubes=(
                CubeSpec("rear", (-2, -2, -1), (4, 4, 4), role="armor"),
                CubeSpec("waist_ring", (-1.5, -1.5, -2.5), (3, 3, 2), role="skin"),
            ), children=(
                PartNode("tail_tip", pivot=(0.0, 0.0, 3.0), anim_role="tail_chain", anim_phase=2.1, cubes=(
                    CubeSpec("tail_tip", (-1, -1, 0), (2, 2, 4), role="skin"),
                    CubeSpec("tail_stinger", (-0.5, -0.5, 4), (1, 1, 2), role="glow_core", emissive=True),
                )),
                PartNode("elite_fins", pivot=(0.0, -2.0, 1.0), rank_min="ELITE", cubes=(
                    CubeSpec("elite_fin_r", (-3, -2, 0), (2, 3, 2), role="glow_core", emissive=True),
                    CubeSpec("elite_fin_l", (1, -2, 0), (2, 3, 2), role="glow_core", emissive=True, mirror=True),
                )),
            )),
        ),
    ))

    # 3) AcidSpitterModel (128x128): Venom Emerald + Spore Chartreuse
    models.append(ModelDef(
        class_name="AcidSpitterModel",
        registry_id="acid_spitter",
        description="Venomous arachnid spitter with articulated chelicerae, 6 IK legs, dual-chamber pulsing sac, and rank stinger",
        tex_size=(128, 128),
        palettes={"skin": "venom_emerald", "armor": "venom_emerald", "bone": "venom_emerald", "spore_sac": "spore_chartreuse", "glow_eye": "spore_chartreuse", "glow_core": "spore_chartreuse"},
        parts=(
            PartNode("thorax", pivot=(0.0, 17.5, -1.0), anim_role="breath_chest", cubes=(
                CubeSpec("thorax", (-3.5, -2.5, -4), (7, 5, 8), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("thorax_plates", (-4, -3, -2), (8, 2, 5), role="bone", inflation=0.1, detail_tags=("chitin_plates",)),
                CubeSpec("venom_duct_r", (-3.8, -1.5, -4), (1, 2, 9), role="glow_core", emissive=True),
                CubeSpec("venom_duct_l", (2.8, -1.5, -4), (1, 2, 9), role="glow_core", emissive=True, mirror=True),
            ), children=(
                PartNode("head", pivot=(0.0, -0.5, -4.0), anim_role="head", cubes=(
                    CubeSpec("head", (-3, -2.5, -5), (6, 5, 5), role="skin", eye_pattern="four_eyes"),
                    CubeSpec("brow_ridge", (-3.5, -3, -5.2), (7, 1, 3), role="armor"),
                ), children=(
                    PartNode("chelicera_r", pivot=(-2.0, 1.5, -5.0), anim_role="mandible_r", cubes=(CubeSpec("chelicera_r", (-1, -1, -3), (2, 2, 3), role="bone"),)),
                    PartNode("chelicera_l", pivot=(2.0, 1.5, -5.0), anim_role="mandible_l", anim_phase=0.5, cubes=(CubeSpec("chelicera_l", (-1, -1, -3), (2, 2, 3), role="bone", mirror=True),)),
                    PartNode("elite_crest", pivot=(0.0, -3.0, -2.0), rank_min="ELITE", cubes=(
                        CubeSpec("elite_crest", (-2, -2, -2), (4, 2, 4), role="glow_eye", emissive=True),
                    )),
                )),
                PartNode("abdomen", pivot=(0.0, -1.5, 4.0), rotation=(0.18, 0.0, 0.0), anim_role="sac_pulse", cubes=(
                    CubeSpec("abdomen_shell", (-4.5, -4, 0), (9, 7, 9), role="armor"),
                    CubeSpec("venom_sac_inner", (-3.5, -5, 1), (7, 6, 7), role="spore_sac", emissive=True, inflation=0.15),
                    CubeSpec("venom_sac_bulb", (-2.5, -6.5, 3), (5, 3, 5), role="glow_core", emissive=True),
                ), children=(
                    PartNode("mutated_stinger", pivot=(0.0, -4.0, 8.0), rank_min="MUTATED", rotation=(0.45, 0.0, 0.0), anim_role="tail_chain", cubes=(
                        CubeSpec("stinger_base", (-1.5, -4, -1.5), (3, 4, 3), role="bone"),
                        CubeSpec("stinger_tip", (-0.5, -7, -0.5), (1, 3, 1), role="glow_eye", emissive=True),
                    )),
                )),
            )),
            _arachnid_leg("leg_r1", (-3.5, 18.0, -3.0), 0.45, "r", 0.0),
            _arachnid_leg("leg_r2", (-3.5, 18.0, 0.0), 0.0, "r", 1.5),
            _arachnid_leg("leg_r3", (-3.5, 18.0, 3.0), -0.45, "r", 3.0),
            _arachnid_leg("leg_l1", (3.5, 18.0, -3.0), -0.45, "l", 1.5),
            _arachnid_leg("leg_l2", (3.5, 18.0, 0.0), 0.0, "l", 3.0),
            _arachnid_leg("leg_l3", (3.5, 18.0, 3.0), 0.45, "l", 0.0),
        ),
    ))

    # 4) BurrowerModel (128x128): 3-stage conical spinning drill + excavating claws + 6 legs + segmented carapace
    models.append(ModelDef(
        class_name="BurrowerModel",
        registry_id="burrower",
        description="Armored subterranean burrower with 3-tier spinning drill head, digging claws, and segmented carapace",
        tex_size=(128, 128),
        palettes={"skin": "mycelial_violet", "armor": "abyssal_obsidian", "bone": "mycelial_violet", "spore_sac": "predator_amethyst", "glow_eye": "warm_ivory", "glow_core": "mycelial_violet"},
        parts=(
            PartNode("body", pivot=(0.0, 19.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("body", (-4, -3, -5), (8, 6, 10), role="armor", eye_pattern="two_eyes"),
                CubeSpec("dorsal_rib_1", (-4.5, -3.5, -3), (9, 2, 2), role="bone"),
                CubeSpec("dorsal_rib_2", (-4.5, -3.5, 1), (9, 2, 2), role="bone"),
                CubeSpec("drill_collar", (-3.5, -3, -5.5), (7, 6, 2), role="bone"),
                CubeSpec("tail_hinge", (-3, -2, 4.5), (6, 4, 2), role="bone"),
            ), children=(
                PartNode("drill_head", pivot=(0.0, 0.0, -5.0), anim_role="drill_spin", cubes=(
                    CubeSpec("drill_base", (-3, -3, -3), (6, 6, 3), role="bone"),
                    CubeSpec("drill_mid", (-2, -2, -6), (4, 4, 3), role="armor"),
                    CubeSpec("drill_tip", (-1, -1, -9), (2, 2, 3), role="glow_core", emissive=True),
                )),
                PartNode("mutated_side_drills", pivot=(0.0, 0.5, -4.0), rank_min="MUTATED", cubes=(
                    CubeSpec("drill_mount_r", (-4.5, -1.5, -2), (2, 3, 3), role="armor"),
                    CubeSpec("drill_mount_l", (2.5, -1.5, -2), (2, 3, 3), role="armor", mirror=True),
                    CubeSpec("side_drill_r", (-6, -1, -4), (2, 2, 4), role="bone"),
                    CubeSpec("side_drill_l", (4, -1, -4), (2, 2, 4), role="bone", mirror=True),
                )),
                PartNode("abdomen", pivot=(0.0, 0.5, 5.0), anim_role="tail_chain", anim_phase=0.8, cubes=(
                    CubeSpec("abdomen", (-3, -2.5, 0), (6, 5, 6), role="skin"),
                    CubeSpec("tail_glow", (-2, -1.5, 6), (4, 3, 2), role="glow_eye", emissive=True),
                )),
                PartNode("elite_shell", pivot=(0.0, -3.5, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("elite_carapace", (-3, -2, -4), (6, 2, 8), role="glow_core", emissive=True),
                )),
            )),
            PartNode("leg_r1", pivot=(-4.0, 20.0, -3.0), anim_role="quad_leg_a", anim_phase=0.0, cubes=(CubeSpec("leg_r1", (-2, 0, -1), (2, 4, 2), role="bone"),)),
            PartNode("leg_r2", pivot=(-4.0, 20.0, 0.0), anim_role="quad_leg_b", anim_phase=0.6, cubes=(CubeSpec("leg_r2", (-2, 0, -1), (2, 4, 2), role="bone"),)),
            PartNode("leg_r3", pivot=(-4.0, 20.0, 3.0), anim_role="quad_leg_a", anim_phase=1.2, cubes=(CubeSpec("leg_r3", (-2, 0, -1), (2, 4, 2), role="bone"),)),
            PartNode("leg_l1", pivot=(4.0, 20.0, -3.0), anim_role="quad_leg_b", anim_phase=0.0, cubes=(CubeSpec("leg_l1", (0, 0, -1), (2, 4, 2), role="bone", mirror=True),)),
            PartNode("leg_l2", pivot=(4.0, 20.0, 0.0), anim_role="quad_leg_a", anim_phase=0.6, cubes=(CubeSpec("leg_l2", (0, 0, -1), (2, 4, 2), role="bone", mirror=True),)),
            PartNode("leg_l3", pivot=(4.0, 20.0, 3.0), anim_role="quad_leg_b", anim_phase=1.2, cubes=(CubeSpec("leg_l3", (0, 0, -1), (2, 4, 2), role="bone", mirror=True),)),
        ),
    ))

    # 5) PyroShroomModel (128x128): Volcanic Ember + Pyro Magma
    models.append(ModelDef(
        class_name="PyroShroomModel",
        registry_id="pyro_shroom",
        description="Volcanic fungal beetle with 4 armored legs, arched magma dome, burning horns, and obsidian carapace",
        tex_size=(128, 128),
        palettes={"skin": "volcanic_ember", "armor": "abyssal_obsidian", "bone": "volcanic_ember", "magma": "pyro_magma", "spore_sac": "pyro_magma", "glow_eye": "pyro_magma", "glow_core": "pyro_magma"},
        parts=(
            PartNode("head_seg", pivot=(0.0, 18.5, -5.0), anim_role="head", cubes=(
                CubeSpec("head_seg", (-3, -2.5, -3), (6, 5, 4), role="armor", eye_pattern="two_eyes", detail_tags=("magma_fissures",)),
                CubeSpec("horn_r", (-3, -4, -3.5), (1, 2, 2), role="glow_core", emissive=True),
                CubeSpec("horn_l", (2, -4, -3.5), (1, 2, 2), role="glow_core", emissive=True, mirror=True),
            )),
            PartNode("mid_seg", pivot=(0.0, 18.0, 0.0), anim_role="sac_pulse", cubes=(
                CubeSpec("mid_seg", (-4, -3, -3), (8, 5, 7), role="armor", detail_tags=("magma_fissures",)),
                CubeSpec("thorax_joint", (-2.5, -1.5, -4.5), (5, 3, 2), role="bone"),
                CubeSpec("tail_joint", (-2.5, -1.5, 3.5), (5, 3, 2), role="bone"),
                CubeSpec("magma_core", (-3, -4, -2), (6, 4, 5), role="magma", emissive=True, inflation=0.1, detail_tags=("magma_fissures",)),
                CubeSpec("dome_ridge", (-2, -5, -1), (4, 2, 4), role="glow_core", emissive=True),
            ), children=(
                PartNode("mutated_vents", pivot=(0.0, -4.5, 1.0), rank_min="MUTATED", cubes=(
                    CubeSpec("mut_vent_c", (-1.5, -3, -1.5), (3, 3, 3), role="glow_core", emissive=True),
                )),
            )),
            PartNode("tail_seg", pivot=(0.0, 19.0, 5.5), anim_role="leech_wave", anim_phase=1.2, cubes=(
                CubeSpec("tail_seg", (-2.5, -2, -1), (5, 4, 5), role="armor"),
                CubeSpec("tail_ember", (-1.5, -2.5, 1), (3, 2, 3), role="glow_core", emissive=True),
            ), children=(
                PartNode("elite_plates", pivot=(0.0, -2.5, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("elite_plate_r", (-3.5, -2, -1), (2, 3, 4), role="skin"),
                    CubeSpec("elite_plate_l", (1.5, -2, -1), (2, 3, 4), role="skin", mirror=True),
                )),
            )),
            PartNode("p_leg_fl", pivot=(-3.5, 20.0, -2.5), anim_role="quad_leg_a", anim_phase=0.0, cubes=(CubeSpec("p_leg_fl", (-1, 0, -1), (2, 4, 2), role="armor"),)),
            PartNode("p_leg_fr", pivot=(3.5, 20.0, -2.5), anim_role="quad_leg_b", anim_phase=0.0, cubes=(CubeSpec("p_leg_fr", (-1, 0, -1), (2, 4, 2), role="armor", mirror=True),)),
            PartNode("p_leg_bl", pivot=(-3.5, 20.0, 2.5), anim_role="quad_leg_b", anim_phase=0.5, cubes=(CubeSpec("p_leg_bl", (-1, 0, -1), (2, 4, 2), role="armor"),)),
            PartNode("p_leg_br", pivot=(3.5, 20.0, 2.5), anim_role="quad_leg_a", anim_phase=0.5, cubes=(CubeSpec("p_leg_br", (-1, 0, -1), (2, 4, 2), role="armor", mirror=True),)),
        ),
    ))

    # 6) ChitinBruteModel (128x128): Common brute — Obsidian + Mushroom Bone + Mycelial Violet (no rare metals)
    models.append(ModelDef(
        class_name="ChitinBruteModel",
        registry_id="chitin_brute",
        description="Hunched chitin juggernaut with layered shoulder plates, bone tusks, heavy smash gauntlets, and rank dorsal spines",
        tex_size=(128, 128),
        palettes={"skin": "abyssal_indigo", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "mycelial_violet"},
        parts=(
            PartNode("torso", pivot=(0.0, 9.5, 0.0), rotation=(0.14, 0.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-6, -7, -4.5), (12, 13, 9), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("chest_core", (-3, -4, -5), (6, 6, 2), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                CubeSpec("hunch_shell", (-5, -8, 1), (10, 8, 5), role="bone", detail_tags=("bone_ridges",)),
                CubeSpec("dorsal_ridge", (-2, -11, 2), (4, 6, 4), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("brute_neck_collar", (-4, -7, -5), (8, 3, 4), role="armor"),
                CubeSpec("brute_pelvis", (-5, 5, -3.5), (10, 3, 7), role="armor"),
                CubeSpec("brute_tasset_r", (-6.5, 3, -3.5), (3, 5, 7), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("brute_tasset_l", (3.5, 3, -3.5), (3, 5, 7), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("brute_shoulder_joint_r", (-8.1, -5.5, -1.5), (3, 3, 3), role="armor"),
                CubeSpec("brute_shoulder_joint_l", (5.1, -5.5, -1.5), (3, 3, 3), role="armor", mirror=True),
            ), children=(
                PartNode("head", pivot=(0.0, -5.0, -4.5), anim_role="head", cubes=(
                    CubeSpec("head", (-4, -5, -5), (8, 6, 6), role="skin", eye_pattern="two_eyes"),
                    CubeSpec("brow_plate", (-4.5, -6, -5.5), (9, 2, 4), role="armor"),
                    CubeSpec("tusk_r", (-4.5, -1, -7), (2, 4, 2), role="bone"),
                    CubeSpec("tusk_l", (2.5, -1, -7), (2, 4, 2), role="bone", mirror=True),
                ), children=(
                    PartNode("brute_jaw", pivot=(0.0, 1.0, -2.0), anim_role="jaw", cubes=(
                        CubeSpec("brute_jaw", (-3.5, 0, -4), (7, 3, 5), role="bone"),
                    )),
                    PartNode("mutated_horns", pivot=(0.0, -5.5, -2.0), rank_min="MUTATED", cubes=(
                        CubeSpec("brute_horn_r", (-5, -5, -1), (2, 5, 2), role="bone"),
                        CubeSpec("brute_horn_l", (3, -5, -1), (2, 5, 2), role="bone", mirror=True),
                    )),
                )),
                PartNode("elite_spines", pivot=(0.0, -8.0, 2.0), rank_min="ELITE", cubes=(
                    CubeSpec("spine_1", (-1, -5, -2), (2, 5, 2), role="glow_core", emissive=True),
                    CubeSpec("spine_2", (-1, -6, 1), (2, 6, 2), role="glow_core", emissive=True),
                    CubeSpec("spine_3", (-1, -4, 4), (2, 4, 2), role="glow_core", emissive=True),
                )),
                PartNode("arm_r", pivot=(-10.1, -4.5, 0.0), anim_role="smash_arm", cubes=(
                    CubeSpec("pauldron_r", (-3.5, -3, -3.5), (6, 6, 7), role="bone"),
                    CubeSpec("upper_arm_r", (-2.5, 2, -2.5), (5, 7, 5), role="armor"),
                ), children=(
                    PartNode("forearm_r", pivot=(0.0, 8.5, 0.0), anim_role="forearm", cubes=(
                        CubeSpec("forearm_r", (-3, 0, -3), (6, 8, 6), role="bone"),
                        CubeSpec("knuckle_r", (-3.5, 6, -3.5), (7, 3, 7), role="glow_core", emissive=True),
                    )),
                )),
                PartNode("arm_l", pivot=(10.1, -4.5, 0.0), anim_role="biped_arm_l", cubes=(
                    CubeSpec("pauldron_l", (-2.5, -3, -3.5), (6, 6, 7), role="bone", mirror=True),
                    CubeSpec("upper_arm_l", (-2.5, 2, -2.5), (5, 7, 5), role="armor", mirror=True),
                ), children=(
                    PartNode("forearm_l", pivot=(0.0, 8.5, 0.0), anim_role="forearm", anim_phase=1.0, cubes=(
                        CubeSpec("forearm_l", (-3, 0, -3), (6, 8, 6), role="bone", mirror=True),
                        CubeSpec("left_knuckle_guard", (-3.5, 6, -3.5), (7, 3, 7), role="armor", mirror=True),
                    )),
                )),
            )),
            *_biped_legs(y_hip=14.0, x_off=3.5, w=5, h=10, d=5, role="armor", knee_guard=True),
        ),
    ))

    # 7) MycelialStalkerModel (128x128): Common stalker — Predator Amethyst + Bone Scythes (no rare metals)
    models.append(ModelDef(
        class_name="MycelialStalkerModel",
        registry_id="mycelial_stalker",
        description="Tall abyssal stalker with predator hunch, digitigrade legs, hooded 3-eye skull, twin scythes, and 3-segment tail",
        tex_size=(128, 128),
        palettes={"skin": "predator_amethyst", "armor": "abyssal_indigo", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "chitin_bone"},
        parts=(
            PartNode("torso", pivot=(0.0, 11.0, 0.0), rotation=(0.24, 0.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-3.5, -10, -2), (7, 11, 4), role="skin", detail_tags=("neural_veins",)),
                CubeSpec("stalker_neck", (-2, -11.5, -3), (4, 3, 4), role="armor"),
                CubeSpec("stalker_pelvis", (-3, 0, -2), (6, 2, 4), role="armor"),
                CubeSpec("rib_core", (-2.5, -8, -1.5), (5, 7, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                CubeSpec("rib_cage", (-4, -9, -2.5), (8, 7, 5), role="bone", inflation=0.1, detail_tags=("bone_ridges",)),
                CubeSpec("rib_spike_r", (-5, -8, -1), (1, 5, 2), role="bone"),
                CubeSpec("rib_spike_l", (4, -8, -1), (1, 5, 2), role="bone", mirror=True),
            ), children=(
                PartNode("head", pivot=(0.0, -10.0, -1.5), rotation=(-0.18, 0.0, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-3, -7, -3), (6, 7, 6), role="skin", eye_pattern="three_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("cowl", (-3.5, -8, -3.5), (7, 8, 7), role="armor", inflation=0.25, detail_tags=("chitin_plates",)),
                    CubeSpec("cowl_tip", (-1.5, -10, 1), (3, 3, 4), role="armor"),
                ), children=(
                    PartNode("stalker_jaw", pivot=(0.0, 0.0, -1.5), anim_role="jaw", cubes=(
                        CubeSpec("stalker_jaw", (-2.5, -1, -2.5), (5, 2, 4), role="bone"),
                    )),
                    PartNode("mutated_antlers", pivot=(0.0, -7.5, 0.0), rank_min="MUTATED", cubes=(
                        CubeSpec("antler_r", (-4.5, -5, -1), (1, 5, 2), role="bone"),
                        CubeSpec("antler_l", (3.5, -5, -1), (1, 5, 2), role="bone", mirror=True),
                    )),
                )),
                PartNode("elite_dorsal_blades", pivot=(0.0, -7.0, 2.5), rank_min="ELITE", cubes=(
                    CubeSpec("d_blade_r", (-4, -6, 0), (1, 7, 3), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("d_blade_l", (3, -6, 0), (1, 7, 3), role="glow_eye", emissive=True, mirror=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("tail_base", pivot=(0.0, -1.0, 2.0), anim_role="tail_chain", anim_phase=0.0, cubes=(
                    CubeSpec("tail_1", (-1, -1, 0), (2, 2, 5), role="armor"),
                ), children=(
                    PartNode("tail_mid", pivot=(0.0, 0.0, 5.0), anim_role="tail_chain", anim_phase=0.8, cubes=(
                        CubeSpec("tail_2", (-1, -1, 0), (2, 2, 4), role="skin"),
                    ), children=(
                        PartNode("tail_tip_s", pivot=(0.0, 0.0, 4.0), anim_role="tail_chain", anim_phase=1.6, cubes=(
                            CubeSpec("tail_3", (-0.5, -0.5, 0), (1, 1, 4), role="glow_eye", emissive=True),
                            CubeSpec("tail_barb_r", (-1.5, -0.5, 2.5), (1, 1, 3), role="bone"),
                            CubeSpec("tail_barb_l", (0.5, -0.5, 2.5), (1, 1, 3), role="bone", mirror=True),
                        )),
                    )),
                )),
                PartNode("arm_r", pivot=(-4.5, -9.0, 0.0), anim_role="mantis_arm_r", cubes=(
                    CubeSpec("arm_r", (-1.5, 0, -1.5), (3, 10, 3), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("elbow_spur_r", (-2, 4, 1), (2, 4, 3), role="bone"),
                    CubeSpec("scythe_r", (-1, 10.05, -7), (1, 3, 8), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("arm_l", pivot=(4.5, -9.0, 0.0), anim_role="mantis_arm_l", anim_phase=0.8, cubes=(
                    CubeSpec("arm_l", (-1.5, 0, -1.5), (3, 10, 3), role="armor", mirror=True, detail_tags=("chitin_plates",)),
                    CubeSpec("elbow_spur_l", (0, 4, 1), (2, 4, 3), role="bone", mirror=True),
                    CubeSpec("scythe_l", (0, 10.05, -7), (1, 3, 8), role="glow_eye", emissive=True, mirror=True, detail_tags=("crystal_facets",)),
                )),
            )),
            PartNode("right_leg", pivot=(-2.2, 11.0, 0.5), rotation=(-0.22, 0.0, 0.0), anim_role="biped_leg_r", cubes=(
                CubeSpec("r_thigh", (-1.5, 0, -1.5), (3, 7, 3), role="armor", detail_tags=("chitin_plates",)),
            ), children=(
                PartNode("r_shin", pivot=(0.0, 6.5, 0.5), rotation=(0.30, 0.0, 0.0), anim_role="biped_shin", cubes=(
                    CubeSpec("r_shin", (-1, 0, -1), (2, 7, 2), role="skin"),
                )),
            )),
            PartNode("left_leg", pivot=(2.2, 11.0, 0.5), rotation=(-0.22, 0.0, 0.0), anim_role="biped_leg_l", cubes=(
                CubeSpec("l_thigh", (-1.5, 0, -1.5), (3, 7, 3), role="armor", mirror=True, detail_tags=("chitin_plates",)),
            ), children=(
                PartNode("l_shin", pivot=(0.0, 6.5, 0.5), rotation=(0.30, 0.0, 0.0), anim_role="biped_shin", anim_phase=3.141593, cubes=(
                    CubeSpec("l_shin", (-1, 0, -1), (2, 7, 2), role="skin", mirror=True),
                )),
            )),
        ),
    ))

    # 8) SynapseWeaverModel (128x128): Special psychic weaver — Predator Amethyst + Biolum Cyan Runic Halo
    models.append(ModelDef(
        class_name="SynapseWeaverModel",
        registry_id="synapse_weaver",
        description="Floating psychic weaver with split tendril skirt, runic halo, 3 orbiting neural crystals, and rank crown",
        tex_size=(128, 128),
        palettes={"skin": "predator_amethyst", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "predator_amethyst"},
        parts=(
            PartNode("body", pivot=(0.0, 9.0, 0.0), anim_role="float_bob", cubes=(
                CubeSpec("body", (-4, -8, -2.5), (8, 11, 5), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("weaver_neck", (-2, -9, -1.5), (4, 2, 3), role="bone"),
                CubeSpec("weaver_pelvis", (-3.5, 2, -2), (7, 3, 4), role="armor"),
                CubeSpec("weaver_shoulder_r", (-6, -8, -2), (3, 3, 4), role="bone"),
                CubeSpec("weaver_shoulder_l", (3, -8, -2), (3, 3, 4), role="bone", mirror=True),
                CubeSpec("neural_heart", (-2, -5, -3), (4, 4, 2), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
            ), children=(
                PartNode("head", pivot=(0.0, -8.5, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-3.5, -7, -3.5), (7, 7, 7), role="skin", eye_pattern="three_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("halo", (-5.5, -9, 1.5), (11, 11, 1), role="glow_core", emissive=True, detail_tags=("runic_band",)),
                ), children=(
                    PartNode("elite_crown", pivot=(0.0, -8.0, 0.0), rank_min="ELITE", cubes=(
                        CubeSpec("crown_f", (-2, -4, -3), (4, 4, 1), role="glow_eye", emissive=True),
                        CubeSpec("crown_r", (-4, -3, -1), (1, 3, 2), role="glow_eye", emissive=True),
                        CubeSpec("crown_l", (3, -3, -1), (1, 3, 2), role="glow_eye", emissive=True, mirror=True),
                    )),
                )),
                PartNode("robe_f", pivot=(0.0, 3.0, -2.0), anim_role="tendril_sway", anim_phase=0.0, cubes=(CubeSpec("robe_f", (-3, 0, -0.5), (6, 8, 1), role="skin"),)),
                PartNode("robe_b", pivot=(0.0, 3.0, 2.0), anim_role="tendril_sway", anim_phase=1.6, cubes=(CubeSpec("robe_b", (-3, 0, -0.5), (6, 8, 1), role="skin"),)),
                PartNode("robe_r", pivot=(-3.5, 3.0, 0.0), anim_role="tendril_sway", anim_phase=0.8, cubes=(CubeSpec("robe_r", (-0.5, 0, -2), (1, 8, 4), role="bone"),)),
                PartNode("robe_l", pivot=(3.5, 3.0, 0.0), anim_role="tendril_sway", anim_phase=2.4, cubes=(CubeSpec("robe_l", (-0.5, 0, -2), (1, 8, 4), role="bone", mirror=True),)),
                PartNode("arm_r", pivot=(-5.0, -6.5, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("arm_r", (-1.5, 0, -1.5), (3, 9, 3), role="bone"),
                    CubeSpec("staff_orb_r", (-1.5, 9, -2.5), (3, 3, 3), role="glow_eye", emissive=True),
                )),
                PartNode("arm_l", pivot=(5.0, -6.5, 0.0), anim_role="combat_arm_l", anim_phase=1.0, cubes=(
                    CubeSpec("arm_l", (-1.5, 0, -1.5), (3, 9, 3), role="bone", mirror=True),
                )),
                PartNode("mutated_ring", pivot=(0.0, -3.0, 0.0), rank_min="MUTATED", anim_role="orbit_ring_a", cubes=(
                    CubeSpec("mut_ring_a", (-8, -0.5, -8), (16, 1, 1), role="glow_core", emissive=True),
                    CubeSpec("mut_ring_b", (-8, -0.5, 7), (16, 1, 1), role="glow_core", emissive=True),
                )),
                PartNode("orb_1", pivot=(-7.0, -4.0, -4.0), anim_role="orbit_orb", anim_phase=0.0, cubes=(CubeSpec("orb_1", (-1, -1, -1), (2, 3, 2), role="glow_eye", emissive=True),)),
                PartNode("orb_2", pivot=(7.0, -4.0, -4.0), anim_role="orbit_orb", anim_phase=2.1, cubes=(CubeSpec("orb_2", (-1, -1, -1), (2, 3, 2), role="glow_core", emissive=True),)),
                PartNode("orb_3", pivot=(0.0, -4.0, 7.0), anim_role="orbit_orb", anim_phase=4.2, cubes=(CubeSpec("orb_3", (-1, -1, -1), (2, 3, 2), role="glow_eye", emissive=True),)),
            )),
        ),
    ))

    # 9) GoliathCarrierModel (256x256): Common carrier — Obsidian + Chitin Bone + Mycelial Violet/Chartreuse Brood Eggs (no rare metals)
    models.append(ModelDef(
        class_name="GoliathCarrierModel",
        registry_id="goliath_carrier",
        description="Massive brood-carrier with open ribcage incubator chamber, visible glowing brood eggs, 4 heavy legs, and rank crests",
        tex_size=(256, 256),
        palettes={"skin": "abyssal_indigo", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "spore_chartreuse"},
        parts=(
            PartNode("body", pivot=(0.0, 12.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("lower_hull", (-8, -2, -9), (16, 7, 18), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("brood_core", (-5, -10, -6), (10, 9, 12), role="spore_sac", emissive=True),
                CubeSpec("brood_egg_1", (-4, -9, -4), (3, 4, 3), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                CubeSpec("brood_egg_2", (1, -10, -2), (3, 4, 3), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                CubeSpec("brood_egg_3", (-2, -9, 2), (4, 4, 4), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                CubeSpec("brood_egg_4", (-4.5, -9, 1.5), (3, 4, 3), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                CubeSpec("brood_egg_5", (1.5, -9, -4.5), (3, 4, 3), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                CubeSpec("neck_shield", (-5.5, -3, -10.5), (11, 4, 3), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("goliath_neck_collar", (-4, -2, -10), (8, 5, 3), role="bone"),
                CubeSpec("incubator_vent_r", (-7, -14, -1), (2, 3, 3), role="armor"),
                CubeSpec("incubator_vent_l", (5, -14, -1), (2, 3, 3), role="armor", mirror=True),
                CubeSpec("spine_arch_1", (-9, -11, -5), (18, 2, 3), role="bone", detail_tags=("bone_ridges",)),
                CubeSpec("spine_arch_2", (-9, -12, 0), (18, 2, 3), role="bone", detail_tags=("bone_ridges",)),
                CubeSpec("spine_arch_3", (-9, -11, 5), (18, 2, 3), role="bone", detail_tags=("bone_ridges",)),
                CubeSpec("side_rib_r", (-9, -10, -7), (2, 9, 15), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("side_rib_l", (7, -10, -7), (2, 9, 15), role="armor", mirror=True, detail_tags=("chitin_plates",)),
            ), children=(
                PartNode("head", pivot=(0.0, 1.0, -9.0), anim_role="head", cubes=(
                    CubeSpec("head", (-4.5, -4, -5), (9, 7, 6), role="skin", eye_pattern="four_eyes"),
                    CubeSpec("jaw_plate", (-4, 2, -5.5), (8, 2, 5), role="bone"),
                )),
                PartNode("sac_top", pivot=(0.0, -12.0, 0.0), anim_role="sac_pulse", cubes=(
                    CubeSpec("sac_top", (-4, -4, -4), (8, 4, 8), role="glow_core", emissive=True),
                )),
                PartNode("mutated_side_sacs", pivot=(0.0, -5.0, 2.0), rank_min="MUTATED", anim_role="sac_pulse", anim_phase=1.3, cubes=(
                    CubeSpec("side_sac_r", (-12, -3, -3), (3, 6, 6), role="glow_core", emissive=True),
                    CubeSpec("side_sac_l", (9, -3, -3), (3, 6, 6), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("elite_dorsal_crest", pivot=(0.0, -14.0, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("crest_f", (-2, -5, -4), (4, 5, 2), role="bone"),
                    CubeSpec("crest_b", (-2, -6, 2), (4, 6, 2), role="bone"),
                )),
            )),
            PartNode("leg_fl", pivot=(-7.0, 16.0, -6.5), anim_role="quad_leg_a", anim_phase=0.0, cubes=(
                CubeSpec("leg_fl", (-2.5, 0, -2.5), (5, 8, 5), role="armor"),
                CubeSpec("greave_fl", (-3, 2, -3.5), (6, 4, 6), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("hoof_fl", (-3, 6, -3), (6, 2, 6), role="bone"),
            )),
            PartNode("leg_fr", pivot=(7.0, 16.0, -6.5), anim_role="quad_leg_b", anim_phase=0.0, cubes=(
                CubeSpec("leg_fr", (-2.5, 0, -2.5), (5, 8, 5), role="armor", mirror=True),
                CubeSpec("greave_fr", (-3, 2, -3.5), (6, 4, 6), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("hoof_fr", (-3, 6, -3), (6, 2, 6), role="bone", mirror=True),
            )),
            PartNode("leg_bl", pivot=(-7.0, 16.0, 6.5), anim_role="quad_leg_b", anim_phase=0.5, cubes=(
                CubeSpec("leg_bl", (-2.5, 0, -2.5), (5, 8, 5), role="armor"),
                CubeSpec("greave_bl", (-3, 2, -3.5), (6, 4, 6), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("hoof_bl", (-3, 6, -3), (6, 2, 6), role="bone"),
            )),
            PartNode("leg_br", pivot=(7.0, 16.0, 6.5), anim_role="quad_leg_a", anim_phase=0.5, cubes=(
                CubeSpec("leg_br", (-2.5, 0, -2.5), (5, 8, 5), role="armor", mirror=True),
                CubeSpec("greave_br", (-3, 2, -3.5), (6, 4, 6), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("hoof_br", (-3, 6, -3), (6, 2, 6), role="bone", mirror=True),
            )),
        ),
    ))

    # 10) ColonyWardenModel (256x256): Obsidian + Chitin Bone + Imperial Brass + Tassets + Winged Helm
    models.append(ModelDef(
        class_name="ColonyWardenModel",
        registry_id="colony_warden",
        description="Armored colony knight-warden with winged T-visor helm, tassets, chitin halberd, runic bio-shield, and cloak",
        tex_size=(256, 256),
        palettes={"skin": "warm_ivory", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "predator_amethyst"},
        parts=(
            PartNode("torso", pivot=(0.0, 11.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-4.5, -12, -2.5), (9, 12, 5), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("warden_gorget", (-5, -13, -3.5), (10, 2, 7), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("warden_belt", (-5.0, -1.5, -3.1), (10, 2, 6), role="armor"),
                CubeSpec("chest_crest", (-3, -10, -3), (6, 5, 1), role="glow_core", emissive=True, detail_tags=("runic_band",)),
                CubeSpec("tasset_r", (-5.5, -1, -3), (3, 5, 6), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("tasset_l", (2.5, -1, -3), (3, 5, 6), role="bone", mirror=True, detail_tags=("chitin_plates",)),
            ), children=(
                PartNode("head", pivot=(0.0, -12.0, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-4, -8, -4), (8, 8, 8), role="armor", eye_pattern="t_visor", detail_tags=("chitin_plates",)),
                    CubeSpec("helm_crest", (-1, -12, -3), (2, 4, 8), role="glow_core", emissive=True, detail_tags=("runic_band",)),
                    CubeSpec("wing_crest_r", (-5, -11, -1), (1, 5, 5), role="bone", detail_tags=("crystal_facets",)),
                    CubeSpec("wing_crest_l", (4, -11, -1), (1, 5, 5), role="bone", mirror=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("cloak", pivot=(0.0, -11.5, 2.5), rotation=(0.12, 0.0, 0.0), anim_role="tendril_sway", anim_phase=0.5, anim_amp=0.5, cubes=(
                    CubeSpec("cloak", (-5, 0, 0), (10, 16, 1), role="skin"),
                    CubeSpec("mantle_banner_r", (-5.5, 2, 0.5), (2, 14, 1), role="glow_core", emissive=True),
                    CubeSpec("mantle_banner_l", (3.5, 2, 0.5), (2, 14, 1), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("elite_halo", pivot=(0.0, -15.0, 3.0), rank_min="ELITE", anim_role="orbit_ring_a", cubes=(
                    CubeSpec("halo_top", (-6, -2, -0.5), (12, 2, 1), role="glow_core", emissive=True),
                    CubeSpec("halo_l", (-6, 0, -0.5), (2, 8, 1), role="glow_core", emissive=True),
                    CubeSpec("halo_r", (4, 0, -0.5), (2, 8, 1), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("arm_r", pivot=(-6.5, -10.0, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("pauldron_r", (-3.55, -2, -3), (5, 5, 6), role="bone"),
                    CubeSpec("arm_r", (-2.05, 2, -2), (4, 10, 4), role="armor"),
                    CubeSpec("halberd_shaft", (-1, 12.05, -10), (2, 2, 18), role="bone"),
                    CubeSpec("halberd_blade", (-1.5, 6, -13), (3, 6, 5), role="glow_core", emissive=True),
                    CubeSpec("halberd_spear_tip", (-1, 8, -16), (2, 3, 4), role="glow_eye", emissive=True),
                    CubeSpec("halberd_back_spike", (-1, 9, 6), (2, 3, 3), role="bone"),
                ), children=(
                    PartNode("mutated_spike_r", pivot=(-3.0, -2.0, 0.0), rank_min="MUTATED", cubes=(
                        CubeSpec("p_spike_r", (-2, -4, -1), (2, 4, 2), role="glow_core", emissive=True),
                    )),
                )),
                PartNode("arm_l", pivot=(6.5, -10.0, 0.0), anim_role="combat_arm_l", cubes=(
                    CubeSpec("pauldron_l", (-1.45, -2, -3), (5, 5, 6), role="bone", mirror=True),
                    CubeSpec("arm_l", (-1.95, 2, -2), (4, 10, 4), role="armor", mirror=True),
                    CubeSpec("shield_plate", (2, 2, -5), (2, 11, 9), role="armor"),
                    CubeSpec("shield_trim", (3.5, 4, -3), (1, 7, 5), role="glow_core", emissive=True),
                    CubeSpec("shield_boss_spike", (4.5, 6.5, -1.5), (2, 2, 2), role="glow_eye", emissive=True),
                ), children=(
                    PartNode("mutated_spike_l", pivot=(3.0, -2.0, 0.0), rank_min="MUTATED", cubes=(
                        CubeSpec("p_spike_l", (0, -4, -1), (2, 4, 2), role="glow_core", emissive=True, mirror=True),
                    )),
                )),
            )),
            *_biped_legs(y_hip=11.0, x_off=2.5, w=4, h=13, d=4, role="armor", knee_guard=True),
        ),
    ))

    # 11) SporeArchitectModel (128x128): Common builder — Abyssal Indigo + Chitin Bone + Mycelial Violet (no rare metals)
    models.append(ModelDef(
        class_name="SporeArchitectModel",
        registry_id="spore_architect",
        description="Colony architect with spore-tank backpack, injector claw arm, monocular lens, and rank satellite",
        tex_size=(128, 128),
        palettes={"skin": "abyssal_indigo", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "mycelial_violet", "glow_eye": "warm_ivory", "glow_core": "mycelial_violet"},
        parts=(
            PartNode("torso", pivot=(0.0, 12.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-4, -11, -2.5), (8, 11, 5), role="armor"),
                CubeSpec("tank_pack", (-3.5, -10, 2.5), (7, 9, 4), role="spore_sac", emissive=True),
                CubeSpec("tank_cap", (-4, -11, 2), (8, 2, 5), role="bone"),
                CubeSpec("tank_hose", (-5.5, -9.6, 0), (2, 2, 4), role="glow_core", emissive=True),
            ), children=(
                PartNode("head", pivot=(0.0, -11.0, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-3.5, -7, -3.5), (7, 7, 7), role="skin", eye_pattern="lens"),
                    CubeSpec("lens_rim", (-2.5, -5.5, -4.2), (5, 4, 1), role="bone"),
                    CubeSpec("antenna_tech", (2.5, -11, 0), (1, 4, 1), role="glow_core", emissive=True),
                )),
                PartNode("elite_satellite", pivot=(0.0, -13.0, 4.5), rank_min="ELITE", anim_role="orbit_ring_a", cubes=(
                    CubeSpec("sat_dish", (-3, -2, -1), (6, 4, 2), role="glow_core", emissive=True),
                )),
                PartNode("arm_r", pivot=(-5.6, -9.5, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("arm_r", (-1.5, 0, -1.5), (3, 9, 3), role="armor"),
                    CubeSpec("injector_claw", (-1.5, 9.05, -4.5), (3, 2, 5), role="glow_core", emissive=True),
                )),
                PartNode("arm_l", pivot=(5.6, -9.5, 0.0), anim_role="biped_arm_l", cubes=(
                    CubeSpec("arm_l", (-1.5, 0, -1.5), (3, 10, 3), role="armor", mirror=True),
                )),
                PartNode("mutated_servo", pivot=(-4.5, -6.0, 3.0), rank_min="MUTATED", anim_role="tendril_sway", cubes=(
                    CubeSpec("servo_boom", (-1.5, -1, -1), (3, 2, 2), role="armor"),
                    CubeSpec("servo_arm", (-3, -4, 0), (2, 5, 2), role="bone"),
                    CubeSpec("servo_tip", (-3.5, -6, -1), (3, 2, 3), role="glow_eye", emissive=True),
                )),
            )),
            *_biped_legs(y_hip=12.0, x_off=2.2, w=3, h=12, d=3, role="armor"),
        ),
    ))

    # 12) PurifierDroneModel (128x64): Crafted special ally — Chitin + Gold Ring + Amethyst Core (matches purifier_drone_core)
    models.append(ModelDef(
        class_name="PurifierDroneModel",
        registry_id="purifier_drone",
        description="Allied floating purifier drone with chitin-gold chassis, amethyst lens, twin counter-rotating rotors, and stabilizer fins",
        tex_size=(128, 64),
        palettes={"skin": "chitin_bone", "armor": "abyssal_obsidian", "bone": "chitin_bone", "metal": "royal_gold", "spore_sac": "mycelial_violet", "glow_eye": "predator_amethyst", "glow_core": "mycelial_violet"},
        parts=(
            PartNode("chassis", pivot=(0.0, 14.0, 0.0), anim_role="float_bob", cubes=(
                CubeSpec("chassis", (-4, -3, -4), (8, 5, 8), role="metal", eye_pattern="lens", detail_tags=("brushed_metal",)),
                CubeSpec("top_dome", (-3, -4.5, -3), (6, 2, 6), role="armor", detail_tags=("brushed_metal",)),
                CubeSpec("purifier_core", (-2.5, 2, -2.5), (5, 2, 5), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                CubeSpec("tail_fin", (-0.5, -3, 4), (1, 4, 3), role="bone", detail_tags=("brushed_metal",)),
                CubeSpec("chassis_boom_r", (-6, -2.5, -1), (3, 1, 2), role="armor"),
                CubeSpec("chassis_boom_l", (3, -2.5, -1), (3, 1, 2), role="armor", mirror=True),
            ), children=(
                PartNode("rotor_r", pivot=(-5.5, -3.5, 0.0), anim_role="rotor_spin", anim_phase=0.0, cubes=(
                    CubeSpec("arm_strut_r", (-0.5, 0, -0.5), (1, 2, 1), role="armor"),
                    CubeSpec("blade_rx", (-4, -0.5, -0.5), (8, 1, 1), role="glow_eye", emissive=True),
                    CubeSpec("blade_rz", (-0.5, -0.5, -4), (1, 1, 8), role="glow_eye", emissive=True),
                )),
                PartNode("rotor_l", pivot=(5.5, -3.5, 0.0), anim_role="rotor_spin", anim_phase=1.57, anim_amp=-1.0, cubes=(
                    CubeSpec("arm_strut_l", (-0.5, 0, -0.5), (1, 2, 1), role="armor", mirror=True),
                    CubeSpec("blade_lx", (-4, -0.5, -0.5), (8, 1, 1), role="glow_eye", emissive=True, mirror=True),
                    CubeSpec("blade_lz", (-0.5, -0.5, -4), (1, 1, 8), role="glow_eye", emissive=True, mirror=True),
                )),
            )),
        ),
    ))

    # 13) SporeBehemothModel (256x256): Venom Emerald + Spore Chartreuse + 3-piece Siege Ram Fist
    # 13) SporeBehemothModel (256x256): Tier 1 Mini-Boss — Colossal Fungal Siege Juggernaut
    models.append(ModelDef(
        class_name="SporeBehemothModel",
        registry_id="spore_behemoth",
        description="Tier 1 Boss: Colossal fungal siege juggernaut with dual spore chimneys, siege tusks, 3-stage crusher ram, and armored bastion plates",
        tex_size=(256, 256),
        palettes={"skin": "venom_emerald", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "spore_chartreuse", "glow_eye": "spore_chartreuse", "glow_core": "spore_chartreuse"},
        parts=(
            PartNode("torso", pivot=(0.0, 3.5, 0.0), rotation=(0.14, 0.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-9, -13, -7), (18, 20, 14), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("behemoth_neck_collar", (-6, -13, -8.5), (12, 5, 4), role="bone"),
                CubeSpec("clavicle_r", (-12, -11, -2.5), (4, 5, 5), role="armor"),
                CubeSpec("clavicle_l", (8, -11, -2.5), (4, 5, 5), role="armor", mirror=True),
                CubeSpec("chest_bastion_r", (-8.5, -11, -8), (7, 4, 2), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("chest_bastion_r_low", (-8.5, -5, -8), (7, 4, 2), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("chest_bastion_l", (1.5, -11, -8), (7, 4, 2), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("chest_bastion_l_low", (1.5, -5, -8), (7, 4, 2), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("sternum_glow", (-4, -8, -7.8), (8, 9, 2), role="glow_core", emissive=True, detail_tags=("neural_veins",)),
                CubeSpec("behemoth_hunch", (-7, -18, -2), (14, 8, 11), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("dorsal_ridge_1", (-2, -16, 7), (4, 6, 3), role="armor"),
                CubeSpec("dorsal_ridge_2", (-2, -8, 7), (4, 6, 3), role="armor"),
                CubeSpec("behemoth_tasset_r", (-10.5, 1, -5.5), (3, 7, 11), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("behemoth_tasset_l", (7.5, 1, -5.5), (3, 7, 11), role="bone", detail_tags=("chitin_plates",), mirror=True),
            ), children=(
                PartNode("head", pivot=(0.0, -10.0, -7.5), anim_role="head", cubes=(
                    CubeSpec("head", (-5.5, -6, -7), (11, 10, 8), role="skin", eye_pattern="four_eyes", detail_tags=("chitin_plates",)),
                    CubeSpec("brow_crest", (-6, -7, -7.5), (12, 3, 4), role="armor"),
                    CubeSpec("jaw_behemoth", (-5, 2, -7.5), (10, 4, 7), role="bone"),
                    CubeSpec("tusk_siege_r", (-6, -1, -10.5), (2, 5, 4), role="bone"),
                    CubeSpec("tusk_siege_l", (4, -1, -10.5), (2, 5, 4), role="bone", mirror=True),
                    CubeSpec("horn_big_r", (-7.5, -12, -4), (3, 8, 4), role="bone"),
                    CubeSpec("horn_tip_r", (-8.0, -16, -5), (2, 5, 3), role="glow_core", emissive=True),
                    CubeSpec("horn_broken_l", (4.5, -10, -4), (3, 5, 3), role="bone", mirror=True),
                ), children=(
                    PartNode("jaw_node", pivot=(0.0, 2.5, -3.5), anim_role="jaw", cubes=(
                        CubeSpec("jaw_behemoth_lower", (-4.5, 0, -5), (9, 3, 6), role="bone"),
                        CubeSpec("jaw_fang_r", (-4, -2, -5.5), (2, 2, 2), role="glow_eye", emissive=True),
                        CubeSpec("jaw_fang_l", (2.0, -2, -5.5), (2, 2, 2), role="glow_eye", emissive=True, mirror=True),
                    )),
                )),
                PartNode("hunch_sac", pivot=(0.0, -18.5, 3.5), anim_role="sac_pulse", cubes=(
                    CubeSpec("hunch_sac", (-6, -6, -4.5), (12, 6, 9), role="spore_sac", emissive=True),
                    CubeSpec("chimney_r", (-5.5, -10, -2), (3, 5, 3), role="armor"),
                    CubeSpec("chimney_vent_r", (-5, -11, -1.5), (2, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("chimney_c", (-1.5, -11, -1.5), (3, 5, 3), role="armor"),
                    CubeSpec("chimney_vent_c", (-1, -12, -1), (2, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("chimney_l", (2.5, -9, 1), (3, 4, 3), role="armor", mirror=True),
                    CubeSpec("chimney_vent_l", (3, -10, 1.5), (2, 2, 2), role="glow_core", emissive=True, mirror=True),
                    CubeSpec("exhaust_pipe_r", (-7.5, -6, 0), (2, 5, 3), role="armor"),
                    CubeSpec("exhaust_pipe_l", (5.5, -6, 0), (2, 5, 3), role="armor", mirror=True),
                )),
                PartNode("mutated_behemoth_spikes", pivot=(0.0, -16.0, -2.0), rank_min="MUTATED", cubes=(
                    CubeSpec("b_spike_r", (-7.5, -7, 0), (3, 7, 3), role="glow_core", emissive=True),
                    CubeSpec("b_spike_l", (4.5, -7, 0), (3, 7, 3), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("elite_behemoth_crown", pivot=(0.0, -19.0, 3.5), rank_min="ELITE", cubes=(
                    CubeSpec("b_elite_fin", (-3, -8, -2.5), (6, 8, 5), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("crown_horn_c", (-1.5, -11, -1.5), (3, 4, 3), role="glow_core", emissive=True),
                    CubeSpec("crown_horn_r", (-5.5, -9, -1), (2, 5, 2), role="bone"),
                    CubeSpec("crown_horn_l", (3.5, -9, -1), (2, 5, 2), role="bone", mirror=True),
                )),
                PartNode("arm_r", pivot=(-12.0, -9.5, 0.0), anim_role="smash_arm", cubes=(
                    CubeSpec("b_pauldron_r", (-5.5, -5, -5.5), (10, 10, 11), role="bone", detail_tags=("chitin_plates",)),
                    CubeSpec("pauldron_horn_r", (-6.5, -9, -2), (3, 5, 4), role="armor"),
                    CubeSpec("b_upper_r", (-4.0, 4, -4.0), (8, 10, 8), role="armor"),
                ), children=(
                    PartNode("mace_forearm", pivot=(0.0, 12.5, 0.0), anim_role="forearm", cubes=(
                        CubeSpec("mace_shaft", (-4.5, 0, -4.5), (9, 9, 9), role="bone", detail_tags=("chitin_plates",)),
                        CubeSpec("crusher_elbow_guard", (-5, -1, 1), (10, 4, 4), role="armor", detail_tags=("chitin_plates",)),
                        CubeSpec("mace_head", (-6, 7, -6), (12, 9, 12), role="glow_core", emissive=True, detail_tags=("magma_fissures",)),
                        CubeSpec("ram_spike_f", (-2.5, 9, -8.5), (5, 5, 3), role="armor", detail_tags=("chitin_plates",)),
                        CubeSpec("ram_spike_s", (-8, 9, -2.5), (2, 5, 5), role="bone"),
                    )),
                )),
                PartNode("arm_l", pivot=(12.0, -9.5, 0.0), anim_role="biped_arm_l", cubes=(
                    CubeSpec("b_pauldron_l", (-4.5, -5, -5.5), (10, 10, 11), role="bone", detail_tags=("chitin_plates",), mirror=True),
                    CubeSpec("b_upper_l", (-4.0, 4, -4.0), (8, 10, 8), role="armor", mirror=True),
                ), children=(
                    PartNode("claw_forearm", pivot=(0.0, 12.5, 0.0), anim_role="forearm", anim_phase=1.2, cubes=(
                        CubeSpec("claw_arm", (-4.0, 0, -4.0), (8, 9, 8), role="bone", mirror=True),
                        CubeSpec("claw_fingers", (-3.5, 8, -6), (7, 5, 5), role="glow_eye", emissive=True),
                        CubeSpec("claw_thumb", (-5.0, 7, -3), (2, 5, 3), role="bone"),
                    )),
                )),
            )),
            *_biped_legs(y_hip=9.5, x_off=5.0, w=7, h=14, d=7, role="armor", knee_guard=True),
        ),
    ))

    # 14) HiveCoreModel (256x256): Tier 2 Sanctuary Boss — Towering Biomechanical Archon Monolith
    models.append(ModelDef(
        class_name="HiveCoreModel",
        registry_id="hive_core",
        description="Tier 2 Boss: Towering biomechanical Hive Core archon with cathedral crown, 8 shield ribs, dual runic gyroscopes, 6 crystal obelisks, and 6 abyssal tendrils",
        tex_size=(256, 256),
        palettes={"skin": "mycelial_violet", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "imperial_brass", "glow_eye": "imperial_brass", "glow_core": "imperial_brass"},
        parts=(
            PartNode("core_body", pivot=(0.0, 6.0, 0.0), anim_role="float_bob", cubes=(
                CubeSpec("core_crystal", (-6, -8, -6), (12, 16, 12), role="glow_core", emissive=True, eye_pattern="core_heart", detail_tags=("crystal_facets",)),
                CubeSpec("inner_cage_n", (-6.5, -5, -6.5), (13, 10, 2), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("inner_cage_s", (-6.5, -5, 4.5), (13, 10, 2), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("core_crown_neck", (-4, -9, -4), (8, 2, 8), role="bone"),
                CubeSpec("upper_crown", (-5, -13, -5), (10, 5, 10), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("crown_spire_c", (-2.5, -21, -2.5), (5, 8, 5), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                CubeSpec("crown_prong_n", (-1.5, -18, -5.5), (3, 5, 2), role="bone"),
                CubeSpec("crown_prong_s", (-1.5, -18, 3.5), (3, 5, 2), role="bone"),
                CubeSpec("crown_prong_e", (3.5, -18, -1.5), (2, 5, 3), role="bone"),
                CubeSpec("crown_prong_w", (-5.5, -18, -1.5), (2, 5, 3), role="bone", mirror=True),
                CubeSpec("lower_calyx", (-5, 8, -5), (10, 4, 10), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("tendril_collar", (-4.5, 10.5, -5.5), (9, 2, 11), role="armor"),
                CubeSpec("rib_strut_n", (-2, -5, -8), (4, 2, 3), role="bone"),
                CubeSpec("rib_strut_s", (-2, -5, 5), (4, 2, 3), role="bone"),
                CubeSpec("rib_strut_e", (5, -5, -2), (3, 2, 4), role="bone"),
                CubeSpec("rib_strut_w", (-8, -5, -2), (3, 2, 4), role="bone", mirror=True),
                CubeSpec("nether_spire", (-2, 12, -2), (4, 6, 4), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                CubeSpec("conduit_ne", (5, -6, -7), (2, 12, 2), role="glow_core", emissive=True),
                CubeSpec("conduit_nw", (-7, -6, -7), (2, 12, 2), role="glow_core", emissive=True),
                CubeSpec("conduit_se", (5, -6, 5), (2, 12, 2), role="glow_core", emissive=True),
                CubeSpec("conduit_sw", (-7, -6, 5), (2, 12, 2), role="glow_core", emissive=True),
            ), children=(
                PartNode("rib_n", pivot=(0.0, -7.0, -8.0), anim_role="petal_flare", anim_phase=0.0, cubes=(
                    CubeSpec("rib_n", (-3.5, 0, -1.5), (7, 15, 3), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("rib_n_trim", (-1.5, 2, -2.0), (3, 11, 1), role="glow_core", emissive=True),
                )),
                PartNode("rib_s", pivot=(0.0, -7.0, 8.0), rotation=(0.0, 3.1416, 0.0), anim_role="petal_flare", anim_phase=1.5, cubes=(
                    CubeSpec("rib_s", (-3.5, 0, -1.5), (7, 15, 3), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("rib_s_trim", (-1.5, 2, -2.0), (3, 11, 1), role="glow_core", emissive=True),
                )),
                PartNode("rib_e", pivot=(8.0, -7.0, 0.0), anim_role="shield_plate", anim_phase=0.8, anim_amp=1.0, cubes=(
                    CubeSpec("rib_e", (-1.5, 0, -3.5), (3, 15, 7), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("rib_e_trim", (1.0, 2, -1.5), (1, 11, 3), role="glow_core", emissive=True),
                )),
                PartNode("rib_w", pivot=(-8.0, -7.0, 0.0), anim_role="shield_plate", anim_phase=2.4, anim_amp=-1.0, cubes=(
                    CubeSpec("rib_w", (-1.5, 0, -3.5), (3, 15, 7), role="armor", detail_tags=("chitin_plates",), mirror=True),
                    CubeSpec("rib_w_trim", (-2.0, 2, -1.5), (1, 11, 3), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("rib_ne", pivot=(6.5, -6.0, -6.5), rotation=(0.0, 0.785, 0.0), anim_role="petal_flare", anim_phase=0.4, cubes=(
                    CubeSpec("rib_ne", (-2, 0, -1), (4, 13, 2), role="bone"),
                    CubeSpec("rib_ne_gem", (-1, 3, -1.5), (2, 7, 1), role="glow_eye", emissive=True),
                )),
                PartNode("rib_nw", pivot=(-6.5, -6.0, -6.5), rotation=(0.0, -0.785, 0.0), anim_role="petal_flare", anim_phase=1.2, cubes=(
                    CubeSpec("rib_nw", (-2, 0, -1), (4, 13, 2), role="bone", mirror=True),
                    CubeSpec("rib_nw_gem", (-1, 3, -1.5), (2, 7, 1), role="glow_eye", emissive=True, mirror=True),
                )),
                PartNode("rib_se", pivot=(6.5, -6.0, 6.5), rotation=(0.0, 2.356, 0.0), anim_role="petal_flare", anim_phase=2.0, cubes=(
                    CubeSpec("rib_se", (-2, 0, -1), (4, 13, 2), role="bone"),
                )),
                PartNode("rib_sw", pivot=(-6.5, -6.0, 6.5), rotation=(0.0, -2.356, 0.0), anim_role="petal_flare", anim_phase=2.8, cubes=(
                    CubeSpec("rib_sw", (-2, 0, -1), (4, 13, 2), role="bone", mirror=True),
                )),
                PartNode("ring_a", pivot=(0.0, 0.0, 0.0), anim_role="orbit_ring_a", cubes=(
                    CubeSpec("ring_a_n", (-11, -1.5, -12), (22, 3, 2), role="bone", detail_tags=("runic_band",)),
                    CubeSpec("ring_a_s", (-11, -1.5, 10), (22, 3, 2), role="bone", detail_tags=("runic_band",)),
                    CubeSpec("ring_a_w", (-12, -1.5, -10), (2, 3, 20), role="glow_eye", emissive=True, detail_tags=("runic_band",)),
                    CubeSpec("ring_a_e", (10, -1.5, -10), (2, 3, 20), role="glow_eye", emissive=True, detail_tags=("runic_band",)),
                )),
                PartNode("shard_1", pivot=(-15.0, -4.0, -15.0), anim_role="orbit_orb", anim_phase=0.0, cubes=(CubeSpec("shard_1", (-2, -5, -2), (4, 10, 4), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_2", pivot=(15.0, -4.0, -15.0), anim_role="orbit_orb", anim_phase=1.1, cubes=(CubeSpec("shard_2", (-2, -5, -2), (4, 10, 4), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_3", pivot=(-15.0, -4.0, 15.0), anim_role="orbit_orb", anim_phase=2.2, cubes=(CubeSpec("shard_3", (-2, -5, -2), (4, 10, 4), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_4", pivot=(15.0, -4.0, 15.0), anim_role="orbit_orb", anim_phase=3.3, cubes=(CubeSpec("shard_4", (-2, -5, -2), (4, 10, 4), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_5", pivot=(-17.0, 0.0, 0.0), anim_role="orbit_orb", anim_phase=4.4, cubes=(CubeSpec("shard_5", (-1.5, -4, -1.5), (3, 8, 3), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_6", pivot=(17.0, 0.0, 0.0), anim_role="orbit_orb", anim_phase=5.5, cubes=(CubeSpec("shard_6", (-1.5, -4, -1.5), (3, 8, 3), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),)),
                PartNode("shard_7", pivot=(0.0, -4.0, -17.0), anim_role="orbit_orb", anim_phase=1.8, cubes=(
                    CubeSpec("shard_7", (-1.5, -4, -1.5), (3, 8, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("shard_7_cap", (-1, -6, -1), (2, 2, 2), role="bone"),
                )),
                PartNode("shard_8", pivot=(0.0, -4.0, 17.0), anim_role="orbit_orb", anim_phase=4.9, cubes=(
                    CubeSpec("shard_8", (-1.5, -4, -1.5), (3, 8, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("shard_8_cap", (-1, -6, -1), (2, 2, 2), role="bone"),
                )),
                PartNode("upper_halo_ring", pivot=(0.0, -14.0, 0.0), anim_role="orbit_ring_a", cubes=(
                    CubeSpec("u_halo_n", (-8, -1, -9), (16, 2, 2), role="glow_eye", emissive=True, detail_tags=("runic_band",)),
                    CubeSpec("u_halo_s", (-8, -1, 7.5), (16, 2, 2), role="glow_eye", emissive=True, detail_tags=("runic_band",)),
                    CubeSpec("u_halo_w", (-9, -1, -7.5), (2, 2, 15), role="bone"),
                    CubeSpec("u_halo_e", (7.5, -1, -7.5), (2, 2, 15), role="bone"),
                )),
                PartNode("lower_halo_ring", pivot=(0.0, 10.0, 0.0), anim_role="orbit_ring_b", cubes=(
                    CubeSpec("l_halo_n", (-7, -1, -8), (14, 2, 2), role="bone", detail_tags=("runic_band",)),
                    CubeSpec("l_halo_s", (-7, -1, 6.5), (14, 2, 2), role="bone", detail_tags=("runic_band",)),
                    CubeSpec("l_halo_w", (-8, -1, -6.5), (2, 2, 13), role="glow_core", emissive=True),
                    CubeSpec("l_halo_e", (6.5, -1, -6.5), (2, 2, 13), role="glow_core", emissive=True),
                )),
                PartNode("ring_b", pivot=(0.0, 0.0, 0.0), anim_role="orbit_ring_b", cubes=(
                    CubeSpec("ring_b_n", (-13, -1, -14), (26, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("ring_b_s", (-13, -1, 12), (26, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("ring_b_w", (-14, -1, -12), (2, 2, 24), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("ring_b_e", (12, -1, -12), (2, 2, 24), role="armor", detail_tags=("chitin_plates",)),
                )),
                PartNode("phase2_aegis_halo", pivot=(0.0, -14.0, 0.0), phase_min=2, anim_role="orbit_ring_a", cubes=(
                    CubeSpec("p2_halo_n", (-9, -1, -9), (18, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("p2_halo_s", (-9, -1, 7), (18, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("p2_halo_star", (-3, -5, -3), (6, 4, 6), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("tendril_1", pivot=(-4.0, 11.0, -4.0), anim_role="tendril_sway", anim_phase=0.0, cubes=(CubeSpec("tendril_1", (-1, 0, -1), (2, 12, 2), role="skin", detail_tags=("neural_veins",)),), children=(
                    PartNode("tendril_1_tip", pivot=(0.0, 11.5, 0.0), anim_role="tendril_sway", anim_phase=0.9, cubes=(CubeSpec("tendril_1_tip", (-0.5, 0, -0.5), (1, 7, 1), role="glow_core", emissive=True),)),
                )),
                PartNode("tendril_2", pivot=(4.0, 11.0, -4.0), anim_role="tendril_sway", anim_phase=1.2, cubes=(CubeSpec("tendril_2", (-1, 0, -1), (2, 12, 2), role="skin", detail_tags=("neural_veins",)),), children=(
                    PartNode("tendril_2_tip", pivot=(0.0, 11.5, 0.0), anim_role="tendril_sway", anim_phase=2.1, cubes=(CubeSpec("tendril_2_tip", (-0.5, 0, -0.5), (1, 7, 1), role="glow_core", emissive=True),)),
                )),
                PartNode("tendril_3", pivot=(-4.0, 11.0, 4.0), anim_role="tendril_sway", anim_phase=2.4, cubes=(CubeSpec("tendril_3", (-1, 0, -1), (2, 12, 2), role="glow_eye", emissive=True),), children=(
                    PartNode("tendril_3_tip", pivot=(0.0, 11.5, 0.0), anim_role="tendril_sway", anim_phase=3.3, cubes=(CubeSpec("tendril_3_tip", (-0.5, 0, -0.5), (1, 7, 1), role="bone"),)),
                )),
                PartNode("tendril_4", pivot=(4.0, 11.0, 4.0), anim_role="tendril_sway", anim_phase=3.6, cubes=(CubeSpec("tendril_4", (-1, 0, -1), (2, 12, 2), role="glow_eye", emissive=True),), children=(
                    PartNode("tendril_4_tip", pivot=(0.0, 11.5, 0.0), anim_role="tendril_sway", anim_phase=4.5, cubes=(CubeSpec("tendril_4_tip", (-0.5, 0, -0.5), (1, 7, 1), role="bone"),)),
                )),
                PartNode("tendril_5", pivot=(0.0, 11.0, -5.5), anim_role="tail_chain", anim_phase=4.5, cubes=(CubeSpec("tendril_5", (-1, 0, -1), (2, 11, 2), role="bone"),), children=(
                    PartNode("tendril_5_tip", pivot=(0.0, 10.5, 0.0), anim_role="tail_chain", anim_phase=5.4, cubes=(CubeSpec("tendril_5_tip", (-0.5, 0, -0.5), (1, 6, 1), role="glow_eye", emissive=True),)),
                )),
                PartNode("tendril_6", pivot=(0.0, 11.0, 5.5), anim_role="tail_chain", anim_phase=5.2, cubes=(CubeSpec("tendril_6", (-1, 0, -1), (2, 11, 2), role="bone"),), children=(
                    PartNode("tendril_6_tip", pivot=(0.0, 10.5, 0.0), anim_role="tail_chain", anim_phase=6.1, cubes=(CubeSpec("tendril_6_tip", (-0.5, 0, -0.5), (1, 6, 1), role="glow_eye", emissive=True),)),
                )),
            )),
        ),
    ))

    # 15) ApexMatriarchModel (256x256): Tier 3 Final Dimensional Boss — Sovereign Eldritch Queen
    models.append(ModelDef(
        class_name="ApexMatriarchModel",
        registry_id="apex_matriarch",
        description="Tier 3 Final Boss: Sovereign Dimensional Apex Matriarch with 7-spired imperial crown, 4 mantis scythe arms, 6 long IK legs, 6 queen mantle wings, and Phase 2/3 ascension halos",
        tex_size=(256, 256),
        palettes={"skin": "predator_amethyst", "armor": "abyssal_obsidian", "bone": "chitin_bone", "spore_sac": "royal_gold", "glow_eye": "royal_gold", "glow_core": "royal_gold"},
        parts=(
            PartNode("body", pivot=(0.0, 8.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("thorax_core", (-7, -15, -6), (14, 16, 12), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("pelvis_core", (-6.5, 0, -5), (13, 8, 11), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("hip_rail_r", (-8, 5, -5), (3, 3, 12), role="bone"),
                CubeSpec("hip_rail_l", (5, 5, -5), (3, 3, 12), role="bone", mirror=True),
                CubeSpec("shoulder_socket_r", (-9.5, -14.5, -3), (3, 4, 4), role="armor"),
                CubeSpec("shoulder_socket_l", (6.5, -14.5, -3), (3, 4, 4), role="armor", mirror=True),
                CubeSpec("matriarch_petiole", (-4, -3, 4), (8, 6, 4), role="bone"),
                CubeSpec("royal_collar_r", (-9, -16, -5), (4, 6, 10), role="bone", detail_tags=("chitin_plates",)),
                CubeSpec("royal_collar_l", (5, -16, -5), (4, 6, 10), role="bone", detail_tags=("chitin_plates",), mirror=True),
                CubeSpec("royal_plastron", (-5, -13, -6.5), (10, 8, 2), role="bone", detail_tags=("runic_band",)),
                CubeSpec("sternum_gem", (-3.5, -12, -6.8), (7, 9, 2), role="glow_core", emissive=True, eye_pattern="core_heart", detail_tags=("crystal_facets",)),
                CubeSpec("dorsal_crest_1", (-2, -14, 5.5), (4, 6, 3), role="glow_eye", emissive=True),
                CubeSpec("dorsal_crest_2", (-2, -7, 5.5), (4, 5, 3), role="bone"),
            ), children=(
                PartNode("head", pivot=(0.0, -15.5, -2.5), anim_role="head", cubes=(
                    CubeSpec("head", (-5, -9, -5), (10, 9, 10), role="skin", eye_pattern="three_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("crown_base", (-5.5, -11, -4.5), (11, 3, 9), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("horn_c", (-1.5, -20, -2.5), (3, 10, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("horn_mr", (-4.5, -17, -1.5), (2, 7, 2), role="bone"),
                    CubeSpec("horn_ml", (2.5, -17, -1.5), (2, 7, 2), role="bone", mirror=True),
                    CubeSpec("horn_or", (-7.5, -15, 0.5), (2, 7, 3), role="armor"),
                    CubeSpec("horn_ol", (5.5, -15, 0.5), (2, 7, 3), role="armor", mirror=True),
                    CubeSpec("crown_wing_r", (-9.5, -13, 2.0), (2, 6, 4), role="glow_eye", emissive=True),
                    CubeSpec("crown_wing_l", (7.5, -13, 2.0), (2, 6, 4), role="glow_eye", emissive=True, mirror=True),
                    CubeSpec("crown_gem_c", (-1, -21, -2), (2, 2, 2), role="glow_core", emissive=True),
                    CubeSpec("crown_gem_r", (-4.5, -18, -1), (2, 2, 2), role="glow_eye", emissive=True),
                    CubeSpec("crown_gem_l", (2.5, -18, -1), (2, 2, 2), role="glow_eye", emissive=True, mirror=True),
                ), children=(
                    PartNode("m_mandible_r", pivot=(-3.0, -1.0, -5.0), anim_role="mandible_r", cubes=(
                        CubeSpec("m_mand_r", (-1.5, -1, -4), (3, 3, 4), role="glow_core", emissive=True),
                    )),
                    PartNode("m_mand_l", pivot=(3.0, -1.0, -5.0), anim_role="mandible_l", anim_phase=0.5, cubes=(
                        CubeSpec("m_mand_l", (-1.5, -1, -4), (3, 3, 4), role="glow_core", emissive=True, mirror=True),
                    )),
                )),
                # 4 Articulated Mantis Scythe Arms
                PartNode("upper_arm_r", pivot=(-9.0, -13.5, -1.5), rotation=(-0.28, 0.0, 0.18), anim_role="mantis_arm_r", anim_phase=0.0, cubes=(
                    CubeSpec("u_pauldron_r", (-3, -3, -3), (6, 6, 6), role="bone", detail_tags=("chitin_plates",)),
                    CubeSpec("u_arm_r", (-2.5, 2, -2.5), (5, 12, 5), role="armor", detail_tags=("chitin_plates",)),
                ), children=(
                    PartNode("u_scythe_r", pivot=(0.0, 13.0, 0.0), anim_role="forearm", cubes=(
                        CubeSpec("u_forearm_r", (-2, 0, -3), (4, 6, 5), role="bone"),
                        CubeSpec("scythe_guard_r", (-2.5, 0, -4), (5, 7, 2), role="armor"),
                        CubeSpec("u_scythe_r", (-1, 1, -12), (2, 5, 13), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                    )),
                )),
                PartNode("upper_arm_l", pivot=(9.0, -13.5, -1.5), rotation=(-0.28, 0.0, -0.18), anim_role="mantis_arm_l", anim_phase=0.7, cubes=(
                    CubeSpec("u_pauldron_l", (-3, -3, -3), (6, 6, 6), role="bone", detail_tags=("chitin_plates",), mirror=True),
                    CubeSpec("u_arm_l", (-2.5, 2, -2.5), (5, 12, 5), role="armor", detail_tags=("chitin_plates",), mirror=True),
                ), children=(
                    PartNode("u_scythe_l", pivot=(0.0, 13.0, 0.0), anim_role="forearm", anim_phase=0.7, cubes=(
                        CubeSpec("u_forearm_l", (-2, 0, -3), (4, 6, 5), role="bone", mirror=True),
                        CubeSpec("scythe_guard_l", (-2.5, 0, -4), (5, 7, 2), role="armor", mirror=True),
                        CubeSpec("u_scythe_l", (-1, 1, -12), (2, 5, 13), role="glow_core", emissive=True, detail_tags=("crystal_facets",), mirror=True),
                    )),
                )),
                PartNode("lower_arm_r", pivot=(-7.5, -6.5, -2.0), rotation=(-0.18, 0.0, 0.12), anim_role="combat_arm_r", cubes=(
                    CubeSpec("l_arm_r", (-2, 0, -2), (4, 11, 4), role="bone", detail_tags=("chitin_plates",)),
                    CubeSpec("l_claw_r", (-1.5, 9, -6), (3, 3, 6), role="glow_eye", emissive=True),
                )),
                PartNode("lower_arm_l", pivot=(7.5, -6.5, -2.0), rotation=(-0.18, 0.0, -0.12), anim_role="combat_arm_l", cubes=(
                    CubeSpec("l_arm_l", (-2, 0, -2), (4, 11, 4), role="bone", detail_tags=("chitin_plates",), mirror=True),
                    CubeSpec("l_claw_l", (-1.5, 9, -6), (3, 3, 6), role="glow_eye", emissive=True, mirror=True),
                )),
                # Sovereign Abdomen + 6-Wing Mantle + Phase 2 & 3 Ascension Rings
                PartNode("abdomen", pivot=(0.0, -1.0, 5.5), rotation=(0.16, 0.0, 0.0), anim_role="sac_pulse", cubes=(
                    CubeSpec("abdomen", (-8, -6, 0), (16, 12, 18), role="armor", detail_tags=("chitin_plates",)),
                    CubeSpec("abdomen_glow", (-6, -7, 2), (12, 10, 14), role="spore_sac", emissive=True, inflation=0.15),
                    CubeSpec("dorsal_egg_sac_r", (-6.5, -9, 4), (4, 4, 8), role="glow_core", emissive=True, detail_tags=("brood_egg",)),
                    CubeSpec("dorsal_egg_sac_l", (2.5, -9, 4), (4, 4, 8), role="glow_core", emissive=True, detail_tags=("brood_egg",), mirror=True),
                ), children=(
                    PartNode("stinger_base", pivot=(0.0, -2.0, 17.5), rotation=(0.45, 0.0, 0.0), anim_role="tail_chain", anim_phase=0.2, cubes=(
                        CubeSpec("stinger_base", (-2, -2, 0), (4, 4, 7), role="armor", detail_tags=("chitin_plates",)),
                    ), children=(
                        PartNode("stinger_tip", pivot=(0.0, 0.0, 6.5), rotation=(0.55, 0.0, 0.0), anim_role="tail_chain", anim_phase=1.0, cubes=(
                            CubeSpec("stinger_tip", (-1.5, -1.5, 0), (3, 3, 7), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                        )),
                    )),
                    PartNode("petal_top", pivot=(0.0, -6.5, 5.0), rotation=(0.38, 0.0, 0.0), anim_role="petal_flare", anim_phase=0.0, cubes=(
                        CubeSpec("petal_top", (-6, -14, -1), (12, 14, 2), role="glow_eye", emissive=True, detail_tags=("neural_veins",)),
                    )),
                    PartNode("petal_r", pivot=(-8.0, -4.0, 5.0), rotation=(0.0, 0.0, -0.48), anim_role="mantle_wing_r", anim_phase=0.5, cubes=(
                        CubeSpec("petal_r", (-1, -13, -5), (2, 14, 10), role="armor", detail_tags=("chitin_plates",)),
                        CubeSpec("petal_r_vein", (-1.5, -11, -3), (1, 10, 6), role="glow_core", emissive=True),
                    )),
                    PartNode("petal_l", pivot=(8.0, -4.0, 5.0), rotation=(0.0, 0.0, 0.48), anim_role="mantle_wing_l", anim_phase=0.5, cubes=(
                        CubeSpec("petal_l", (-1, -13, -5), (2, 14, 10), role="armor", detail_tags=("chitin_plates",), mirror=True),
                        CubeSpec("petal_l_vein", (0.5, -11, -3), (1, 10, 6), role="glow_core", emissive=True, mirror=True),
                    )),
                    PartNode("petal_low_r", pivot=(-7.5, -1.0, 11.0), rotation=(0.15, 0.0, -0.72), anim_role="mantle_wing_r", anim_phase=1.2, cubes=(
                        CubeSpec("petal_low_r", (-1, -10, -3), (2, 11, 7), role="bone"),
                    )),
                    PartNode("petal_low_l", pivot=(7.5, -1.0, 11.0), rotation=(0.15, 0.0, 0.72), anim_role="mantle_wing_l", anim_phase=1.2, cubes=(
                        CubeSpec("petal_low_l", (-1, -10, -3), (2, 11, 7), role="bone", mirror=True),
                    )),
                    PartNode("phase2_aura", pivot=(0.0, -8.0, 8.0), phase_min=2, anim_role="orbit_ring_a", cubes=(
                        CubeSpec("p2_ring_a", (-12, -1.5, -12), (24, 3, 2), role="glow_core", emissive=True, detail_tags=("runic_band",)),
                        CubeSpec("p2_ring_b", (-12, -1.5, 10), (24, 3, 2), role="glow_core", emissive=True, detail_tags=("runic_band",)),
                        CubeSpec("p2_ring_w", (-12, -1.5, -10), (2, 3, 20), role="glow_eye", emissive=True),
                        CubeSpec("p2_ring_e", (10, -1.5, -10), (2, 3, 20), role="glow_eye", emissive=True),
                    )),
                    PartNode("phase3_spire", pivot=(0.0, -12.0, 9.0), phase_min=3, anim_role="orbit_ring_b", cubes=(
                        CubeSpec("p3_crown", (-5, -11, -5), (10, 11, 10), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                        CubeSpec("p3_wing_r", (-14, -9, -1), (9, 7, 2), role="glow_core", emissive=True),
                        CubeSpec("p3_wing_l", (5, -9, -1), (9, 7, 2), role="glow_core", emissive=True, mirror=True),
                    )),
                )),
            )),
            _arachnid_leg("m_leg_r1", (-7.0, 15.0, -4.0), 0.48, "r", 0.0, coxa_len=9, tibia_len=10, thick=3),
            _arachnid_leg("m_leg_r2", (-7.0, 15.0, 1.0), 0.0, "r", 1.5, coxa_len=9, tibia_len=10, thick=3),
            _arachnid_leg("m_leg_r3", (-7.0, 15.0, 6.0), -0.48, "r", 3.0, coxa_len=9, tibia_len=10, thick=3),
            _arachnid_leg("m_leg_l1", (7.0, 15.0, -4.0), -0.48, "l", 1.5, coxa_len=9, tibia_len=10, thick=3),
            _arachnid_leg("m_leg_l2", (7.0, 15.0, 1.0), 0.0, "l", 3.0, coxa_len=9, tibia_len=10, thick=3),
            _arachnid_leg("m_leg_l3", (7.0, 15.0, 6.0), 0.48, "l", 0.0, coxa_len=9, tibia_len=10, thick=3),
        ),
    ))

    # 16) SynapticMimicModel (128x128): Adaptive fungal doppelgänger with split neural mask, blade-arm, and spore/shield gauntlet
    models.append(ModelDef(
        class_name="SynapticMimicModel",
        registry_id="synaptic_mimic",
        description="Adaptive fungal doppelgänger with split neural mask, mimicking antennae, chitin blade arm, and shield/spore gauntlet",
        tex_size=(128, 128),
        palettes={
            "skin": "abyssal_indigo",
            "armor": "abyssal_obsidian",
            "bone": "chitin_bone",
            "spore_sac": "mycelial_violet",
            "glow_eye": "warm_ivory",
            "glow_core": "spore_chartreuse",
        },
        parts=(
            PartNode("body", pivot=(0.0, 12.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-4, -12, -2), (8, 12, 4), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("mimic_neck", (-2, -13, -2), (4, 2, 4), role="bone"),
                CubeSpec("mimic_pelvis", (-3, -1, -2), (6, 2, 4), role="armor"),
                CubeSpec("echo_sternum", (-2, -9, -2.5), (4, 5, 1), role="glow_core", emissive=True, detail_tags=("neural_veins",)),
                CubeSpec("dorsal_spine", (-1, -11, 2), (2, 9, 2), role="bone"),
            ), children=(
                PartNode("head", pivot=(0.0, -12.0, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-4, -8, -4), (8, 8, 8), role="skin", eye_pattern="t_visor", detail_tags=("neural_veins",)),
                    CubeSpec("split_mask_r", (-4.5, -7, -4.5), (4, 6, 2), role="bone"),
                    CubeSpec("split_mask_l", (0.5, -7, -4.5), (4, 6, 2), role="spore_sac", emissive=True),
                    CubeSpec("mimic_crest_r", (-3, -12, -1), (1, 4, 2), role="glow_core", emissive=True),
                    CubeSpec("mimic_crest_l", (2, -12, -1), (1, 4, 2), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("arm_r", pivot=(-5.5, -10.0, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("arm_r_upper", (-2, -2, -2), (4, 10, 4), role="armor"),
                    CubeSpec("mimic_blade", (-1.5, 6, -5), (1, 3, 7), role="glow_core", emissive=True),
                )),
                PartNode("arm_l", pivot=(5.5, -10.0, 0.0), anim_role="combat_arm_l", cubes=(
                    CubeSpec("arm_l_upper", (-2, -2, -2), (4, 10, 4), role="armor", mirror=True),
                    CubeSpec("mimic_gauntlet", (1.5, 4, -2.5), (2, 5, 5), role="bone", mirror=True),
                )),
                PartNode("mutated_echo_fins", pivot=(0.0, -9.0, 3.0), rank_min="MUTATED", cubes=(
                    CubeSpec("m_fin_r", (-4, -3, 0), (1, 6, 3), role="glow_core", emissive=True),
                    CubeSpec("m_fin_l", (3, -3, 0), (1, 6, 3), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("elite_echo_halo", pivot=(0.0, -21.0, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("echo_pylon_r", (-3, 0, -0.5), (1, 2, 1), role="bone"),
                    CubeSpec("echo_pylon_l", (2, 0, -0.5), (1, 2, 1), role="bone", mirror=True),
                    CubeSpec("e_halo_top", (-3, -1, -3), (6, 1, 6), role="glow_eye", emissive=True),
                )),
            )),
            *_biped_legs(y_hip=12.0, x_off=2.2, w=4, h=12, d=4, role="armor", knee_guard=True),
        ),
    ))

    # 17) MycelialChimeraModel (128x128): Modular combat mount & guardian with 5 gene-reactive dorsal modules and rank evolutions
    models.append(ModelDef(
        class_name="MycelialChimeraModel",
        registry_id="mycelial_chimera",
        description="Modular bio-forged Chimera mount & guardian with symbiotic saddle, 4 sturdy legs, and 5 gene-reactive dorsal modules",
        tex_size=(128, 128),
        palettes={
            "skin": "abyssal_indigo",
            "armor": "abyssal_obsidian",
            "bone": "chitin_bone",
            "spore_sac": "mycelial_violet",
            "glow_eye": "warm_ivory",
            "glow_core": "spore_chartreuse",
        },
        parts=(
            PartNode("body", pivot=(0.0, 12.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("chimera_torso", (-5, -7, -8), (10, 8, 16), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("chimera_neck", (-3.5, -6, -9.5), (7, 5, 3), role="armor"),
                CubeSpec("symbiotic_saddle", (-4, -8, -3), (8, 1, 7), role="bone", inflation=0.1),
                CubeSpec("chest_core", (-2.5, -5, -8.5), (5, 4, 1), role="glow_core", emissive=True, detail_tags=("neural_veins",)),
            ), children=(
                PartNode("head", pivot=(0.0, -5.0, -8.5), anim_role="head", cubes=(
                    CubeSpec("chimera_head", (-4, -4, -6), (8, 7, 6), role="skin", eye_pattern="four_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("jaw_plate", (-3, 2, -5.5), (6, 2, 4), role="bone"),
                ), children=(
                    PartNode("c_mandible_r", pivot=(-3.0, 1.0, -5.0), anim_role="mandible_r", cubes=(
                        CubeSpec("c_mand_r", (-1, -1, -3), (2, 2, 3), role="glow_core", emissive=True),
                    )),
                    PartNode("c_mandible_l", pivot=(3.0, 1.0, -5.0), anim_role="mandible_l", anim_phase=0.5, cubes=(
                        CubeSpec("c_mand_l", (-1, -1, -3), (2, 2, 3), role="glow_core", emissive=True, mirror=True),
                    )),
                )),
                # 5 Gene-reactive modules (visible when chassisGene or mutationGene matches)
                PartNode("gene_stinger", pivot=(0.0, -7.0, 8.0), gene_match="MELEE_THORNS", anim_role="tail_chain", cubes=(
                    CubeSpec("stinger_arc", (-1.5, -6, 0), (3, 6, 4), role="armor"),
                    CubeSpec("stinger_tip", (-1, -10, -2), (2, 4, 3), role="glow_core", emissive=True),
                )),
                PartNode("gene_fins", pivot=(0.0, -6.0, 0.0), gene_match="PROJECTILE_DEFLECT", cubes=(
                    CubeSpec("fin_r", (-7, -4, -3), (2, 5, 8), role="spore_sac", emissive=True),
                    CubeSpec("fin_l", (5, -4, -3), (2, 5, 8), role="spore_sac", emissive=True, mirror=True),
                )),
                PartNode("gene_vents", pivot=(0.0, -7.5, 2.0), gene_match="FIRE_RESISTANT", anim_role="sac_pulse", cubes=(
                    CubeSpec("vent_r", (-4.5, -4, 0), (2, 4, 2), role="glow_eye", emissive=True),
                    CubeSpec("vent_l", (2.5, -4, 0), (2, 4, 2), role="glow_eye", emissive=True, mirror=True),
                )),
                PartNode("gene_carapace", pivot=(0.0, -8.0, 0.0), gene_match="BLAST_SHIELDED", cubes=(
                    CubeSpec("heavy_shell", (-6, -2, -6), (12, 3, 12), role="bone", detail_tags=("chitin_plates",)),
                )),
                PartNode("gene_crest", pivot=(0.0, -9.0, -5.0), gene_match="MAGIC_CARAPACE", cubes=(
                    CubeSpec("arcane_crest", (-2, -4, -1), (4, 4, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                )),
                # Rank parts
                PartNode("mutated_chimera_horns", pivot=(0.0, -8.5, -6.0), rank_min="MUTATED", cubes=(
                    CubeSpec("c_horn_r", (-4, -3, 0), (1, 3, 2), role="glow_core", emissive=True),
                    CubeSpec("c_horn_l", (3, -3, 0), (1, 3, 2), role="glow_core", emissive=True, mirror=True),
                )),
                PartNode("elite_chimera_crown", pivot=(0.0, -8.0, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("c_crown_strut_r", (-3, -1, -1), (1, 2, 2), role="bone"),
                    CubeSpec("c_crown_strut_l", (2, -1, -1), (1, 2, 2), role="bone", mirror=True),
                    CubeSpec("c_elite_halo", (-3, -2, -3), (6, 1, 6), role="glow_eye", emissive=True),
                )),
            )),
            _arachnid_leg("c_leg_fr", (-5.0, 14.0, -5.0), 0.35, "r", 0.0, coxa_len=4, tibia_len=6, thick=3),
            _arachnid_leg("c_leg_fl", (5.0, 14.0, -5.0), -0.35, "l", 1.5, coxa_len=4, tibia_len=6, thick=3),
            _arachnid_leg("c_leg_br", (-5.0, 14.0, 5.0), -0.35, "r", 1.5, coxa_len=4, tibia_len=6, thick=3),
            _arachnid_leg("c_leg_bl", (5.0, 14.0, 5.0), 0.35, "l", 0.0, coxa_len=4, tibia_len=6, thick=3),
        ),
    ))

    # 18) ColonySymbiontModel (256x256): Polymorphic player-aligned colony worker with 5 caste modules
    models.append(ModelDef(
        class_name="ColonySymbiontModel",
        registry_id="colony_symbiont",
        description="Polymorphic cooperative Sovereign Colony helper with sensory crest, courier pack, and caste-reactive tools",
        tex_size=(256, 256),
        palettes={
            "skin": "abyssal_indigo",
            "armor": "venom_emerald",
            "bone": "chitin_bone",
            "spore_sac": "mycelial_violet",
            "glow_eye": "warm_ivory",
            "glow_core": "spore_chartreuse",
        },
        parts=(
            PartNode("body", pivot=(0.0, 12.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-3.5, -10, -2), (7, 10, 4), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("symbiont_sternum", (-2, -8, -2.5), (4, 4, 1), role="glow_core", emissive=True, detail_tags=("neural_veins",)),
                CubeSpec("symbiont_collar", (-2.5, -11, -2), (5, 2, 4), role="bone"),
            ), children=(
                PartNode("head", pivot=(0.0, -10.5, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-3, -6, -3), (6, 6, 6), role="skin", eye_pattern="three_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("visor_brow", (-3, -5, -3.5), (6, 2, 1), role="glow_eye", emissive=True),
                    CubeSpec("antenna_crest", (-1, -9, -1), (2, 3, 2), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("caste_courier_pack", pivot=(0.0, -5.5, 2.0), cubes=(
                    CubeSpec("pack_frame", (-3, -4, 0), (6, 7, 3), role="bone", detail_tags=("chitin_plates",)),
                    CubeSpec("pack_core", (-2, -3, 2.5), (4, 4, 1), role="glow_core", emissive=True),
                )),
                PartNode("arm_r", pivot=(-4.5, -8.5, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("arm_r", (-1.5, -1, -1.5), (3, 9, 3), role="armor"),
                ), children=(
                    PartNode("caste_harvester_scythe", pivot=(0.0, 7.0, 0.0), cubes=(
                        CubeSpec("scythe_haft", (-0.5, 0, -4), (1, 1, 5), role="bone"),
                        CubeSpec("scythe_blade", (-0.5, 0, -6), (1, 3, 2), role="glow_core", emissive=True),
                    )),
                    PartNode("caste_excavator_pick", pivot=(0.0, 7.0, 0.0), cubes=(
                        CubeSpec("pick_haft", (-0.5, 0, -4), (1, 1, 5), role="bone"),
                        CubeSpec("pick_head", (-0.5, -2, -5), (1, 5, 2), role="glow_eye", emissive=True),
                    )),
                    PartNode("caste_architect_hammer", pivot=(0.0, 7.0, 0.0), cubes=(
                        CubeSpec("hammer_haft", (-0.5, 0, -3.5), (1, 1, 4), role="bone"),
                        CubeSpec("hammer_head", (-1.5, -1.5, -5), (3, 3, 2), role="glow_core", emissive=True),
                    )),
                )),
                PartNode("arm_l", pivot=(4.5, -8.5, 0.0), anim_role="combat_arm_l", anim_phase=0.8, cubes=(
                    CubeSpec("arm_l", (-1.5, -1, -1.5), (3, 9, 3), role="armor", mirror=True),
                ), children=(
                    PartNode("caste_sentinel_shield", pivot=(1.5, 4.0, 0.0), cubes=(
                        CubeSpec("shield_plate_s", (-0.5, -3, -2.5), (1, 6, 5), role="bone", mirror=True),
                        CubeSpec("shield_boss_s", (0, -1, -1.5), (1, 2, 3), role="glow_eye", emissive=True, mirror=True),
                    )),
                )),
            )),
            *_biped_legs(y_hip=12.0, x_off=1.8, w=3, h=12, d=3, role="armor", knee_guard=True),
        ),
    ))

    # 19) SporeGliderModel (256x256): Peaceful sinkhole gliding pollinator
    models.append(ModelDef(
        class_name="SporeGliderModel",
        registry_id="spore_glider",
        description="Peaceful gliding sinkhole pollinator with twin bioluminescent wing membranes and spore-tail tendrils",
        tex_size=(256, 256),
        palettes={
            "skin": "biolum_cyan",
            "armor": "abyssal_indigo",
            "bone": "chitin_bone",
            "spore_sac": "purification_aqua",
            "glow_eye": "warm_ivory",
            "glow_core": "spore_chartreuse",
        },
        parts=(
            PartNode("body", pivot=(0.0, 15.0, 0.0), anim_role="float_bob", cubes=(
                CubeSpec("body", (-3, -2, -5), (6, 4, 10), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("belly_sac", (-2, 1, -3), (4, 2, 6), role="spore_sac"),
                CubeSpec("dorsal_spine", (-1, -3, -4), (2, 1, 8), role="bone"),
            ), children=(
                PartNode("head", pivot=(0.0, -0.5, -5.0), anim_role="head", cubes=(
                    CubeSpec("head", (-2.5, -2.5, -4), (5, 4, 4), role="skin", eye_pattern="two_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("glider_crest", (-1.5, -4, -3), (3, 2, 3), role="glow_core", emissive=True, detail_tags=("crystal_facets",)),
                )),
                PartNode("wing_fore_r", pivot=(-3.0, -1.0, -2.0), anim_role="mantle_wing_r", cubes=(
                    CubeSpec("fore_membrane_r", (-9, -0.5, -3), (9, 1, 6), role="spore_sac"),
                    CubeSpec("fore_rib_r", (-9, -1.0, -3), (9, 1, 1), role="bone"),
                )),
                PartNode("wing_fore_l", pivot=(3.0, -1.0, -2.0), anim_role="mantle_wing_l", cubes=(
                    CubeSpec("fore_membrane_l", (0, -0.5, -3), (9, 1, 6), role="spore_sac", mirror=True),
                    CubeSpec("fore_rib_l", (0, -1.0, -3), (9, 1, 1), role="bone", mirror=True),
                )),
                PartNode("wing_aft_r", pivot=(-2.5, 0.0, 3.0), anim_role="mantle_wing_r", anim_phase=0.8, cubes=(
                    CubeSpec("aft_membrane_r", (-6, -0.5, -1), (6, 1, 4), role="spore_sac"),
                )),
                PartNode("wing_aft_l", pivot=(2.5, 0.0, 3.0), anim_role="mantle_wing_l", anim_phase=0.8, cubes=(
                    CubeSpec("aft_membrane_l", (0, -0.5, -1), (6, 1, 4), role="spore_sac", mirror=True),
                )),
                PartNode("pollen_tail_r", pivot=(-1.5, 0.5, 5.0), anim_role="tendril_sway", anim_phase=0.0, cubes=(
                    CubeSpec("pollen_tail_r", (-0.5, -0.5, 0), (1, 1, 6), role="bone"),
                    CubeSpec("pollen_pod_r", (-1, -1, 5), (2, 2, 2), role="glow_eye", emissive=True),
                )),
                PartNode("pollen_tail_l", pivot=(1.5, 0.5, 5.0), anim_role="tendril_sway", anim_phase=1.4, cubes=(
                    CubeSpec("pollen_tail_l", (-0.5, -0.5, 0), (1, 1, 6), role="bone", mirror=True),
                    CubeSpec("pollen_pod_l", (-1, -1, 5), (2, 2, 2), role="glow_eye", emissive=True, mirror=True),
                )),
            )),
        ),
    ))

    # 20) RootKeeperModel (256x256): Territorial Root Cavern guardian with MUTATED & ELITE layers
    models.append(ModelDef(
        class_name="RootKeeperModel",
        registry_id="root_keeper",
        description="Territorial Root Cavern guardian armored in gnarled chitin roots, amber crest, and heavy root mauls",
        tex_size=(256, 256),
        palettes={
            "skin": "predator_amethyst",
            "armor": "abyssal_obsidian",
            "bone": "chitin_bone",
            "spore_sac": "mutant_amber",
            "glow_eye": "royal_gold",
            "glow_core": "mutant_amber",
        },
        parts=(
            PartNode("torso", pivot=(0.0, 10.0, 0.0), anim_role="breath_chest", cubes=(
                CubeSpec("torso", (-5, -12, -3.5), (10, 12, 7), role="armor", detail_tags=("chitin_plates",)),
                CubeSpec("root_heart_core", (-2.5, -9, -4.2), (5, 5, 1), role="glow_core", emissive=True, detail_tags=("neural_veins",)),
                CubeSpec("root_mantle", (-6, -13, -3), (12, 3, 6), role="bone"),
                CubeSpec("root_skirt", (-4.5, -1, -3), (9, 2, 6), role="armor"),
            ), children=(
                PartNode("head", pivot=(0.0, -12.0, 0.0), anim_role="head", cubes=(
                    CubeSpec("head", (-4, -7, -4), (8, 7, 8), role="skin", eye_pattern="three_eyes", detail_tags=("neural_veins",)),
                    CubeSpec("amber_crown", (-2.5, -10, -2.5), (5, 3, 5), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                ), children=(
                    PartNode("elite_antler_crown", pivot=(0.0, -9.0, 0.0), rank_min="ELITE", cubes=(
                        CubeSpec("antler_spike_r", (-5, -4, -1), (2, 5, 2), role="bone"),
                        CubeSpec("antler_spike_l", (3, -4, -1), (2, 5, 2), role="bone", mirror=True),
                        CubeSpec("antler_tip_r", (-5.5, -6, -1), (2, 2, 2), role="glow_eye", emissive=True),
                        CubeSpec("antler_tip_l", (3.5, -6, -1), (2, 2, 2), role="glow_eye", emissive=True, mirror=True),
                    )),
                )),
                PartNode("arm_r", pivot=(-7.0, -10.0, 0.0), anim_role="combat_arm_r", cubes=(
                    CubeSpec("arm_r", (-2, 0, -2), (4, 12, 4), role="armor"),
                    CubeSpec("root_maul_r", (-2.5, 10, -4), (5, 4, 6), role="bone", detail_tags=("chitin_plates",)),
                ), children=(
                    PartNode("mutated_gauntlet_r", pivot=(0.0, 0.0, 0.0), rank_min="MUTATED", cubes=(
                        CubeSpec("mut_pauldron_r", (-3, -2, -2.5), (5, 4, 5), role="bone"),
                        CubeSpec("mut_maul_glow_r", (-1.5, 11, -4.5), (3, 2, 1), role="glow_core", emissive=True),
                    )),
                )),
                PartNode("arm_l", pivot=(7.0, -10.0, 0.0), anim_role="combat_arm_l", cubes=(
                    CubeSpec("arm_l", (-2, 0, -2), (4, 12, 4), role="armor", mirror=True),
                    CubeSpec("root_maul_l", (-2.5, 10, -4), (5, 4, 6), role="bone", mirror=True, detail_tags=("chitin_plates",)),
                ), children=(
                    PartNode("mutated_gauntlet_l", pivot=(0.0, 0.0, 0.0), rank_min="MUTATED", cubes=(
                        CubeSpec("mut_pauldron_l", (-2, -2, -2.5), (5, 4, 5), role="bone", mirror=True),
                        CubeSpec("mut_maul_glow_l", (-1.5, 11, -4.5), (3, 2, 1), role="glow_core", emissive=True, mirror=True),
                    )),
                )),
                PartNode("mutated_root_spines", pivot=(0.0, -7.0, 3.5), rank_min="MUTATED", cubes=(
                    CubeSpec("mut_spine_r", (-4, -3, 0), (2, 6, 2), role="bone"),
                    CubeSpec("mut_spine_l", (2, -3, 0), (2, 6, 2), role="bone", mirror=True),
                    CubeSpec("mut_amber_node", (-1.5, -2, 1), (3, 4, 2), role="glow_core", emissive=True),
                )),
                PartNode("elite_root_halo", pivot=(0.0, -14.0, 3.5), rank_min="ELITE", anim_role="orbit_ring_a", cubes=(
                    CubeSpec("keeper_halo_n", (-6, -1, -6), (12, 1, 1), role="glow_core", emissive=True),
                    CubeSpec("keeper_halo_s", (-6, -1, 5), (12, 1, 1), role="glow_core", emissive=True),
                    CubeSpec("keeper_halo_e", (5, -1, -5), (1, 1, 10), role="glow_eye", emissive=True),
                    CubeSpec("keeper_halo_w", (-6, -1, -5), (1, 1, 10), role="glow_eye", emissive=True, mirror=True),
                )),
                PartNode("elite_shoulder_crests", pivot=(0.0, -13.0, 0.0), rank_min="ELITE", cubes=(
                    CubeSpec("crest_crystal_r", (-7.5, -3, -1), (2, 4, 2), role="glow_eye", emissive=True, detail_tags=("crystal_facets",)),
                    CubeSpec("crest_crystal_l", (5.5, -3, -1), (2, 4, 2), role="glow_eye", emissive=True, mirror=True, detail_tags=("crystal_facets",)),
                )),
            )),
            *_biped_legs(y_hip=10.0, x_off=3.0, w=4, h=14, d=4, role="armor", knee_guard=True),
        ),
    ))

    from .legendary_upgrades import apply_legendary_upgrades

    apply_legendary_upgrades(models)
    return models
