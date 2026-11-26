"""Legendary base anatomy and complete MUTATED/ELITE/phase evolution layers.

Applied as a post-processing pass over the JSON-free ModelDef tree so the original
model definitions stay readable and the upgrade stays diff-friendly.
"""

from __future__ import annotations

from typing import List, Sequence, Tuple

from .uv_packer import CubeSpec, ModelDef, PartNode

TAG_CHITIN = ("chitin_plates",)
TAG_BONE = ("bone_ridges",)
TAG_RUNE = ("runic_band",)
TAG_CRYSTAL = ("crystal_facets",)
TAG_VEINS = ("neural_veins",)
TAG_MAGMA = ("magma_fissures",)
TAG_BROOD = ("brood_egg",)


def _c(name: str, x: float, y: float, z: float, w: int, h: int, d: int,
       role: str = "armor", emissive: bool = False, mirror: bool = False,
       tags: Sequence[str] = (), inflate: float = 0.0) -> CubeSpec:
    return CubeSpec(
        name,
        (float(x), float(y), float(z)),
        (int(w), int(h), int(d)),
        role=role,
        emissive=emissive,
        mirror=mirror,
        detail_tags=tuple(tags),
        inflation=inflate,
    )


def _n(name: str, pivot: Tuple[float, float, float], cubes: Sequence[CubeSpec] = (),
       children: Sequence[PartNode] = (), rotation: Tuple[float, float, float] = (0.0, 0.0, 0.0),
       anim_role: str | None = None, anim_phase: float = 0.0, anim_amp: float = 1.0,
       rank_min: str | None = None, phase_min: int | None = None) -> PartNode:
    return PartNode(
        name,
        pivot=pivot,
        rotation=rotation,
        cubes=tuple(cubes),
        children=tuple(children),
        anim_role=anim_role,
        anim_phase=anim_phase,
        anim_amp=anim_amp,
        rank_min=rank_min,
        phase_min=phase_min,
    )


def _find(parts: Sequence[PartNode], name: str) -> PartNode | None:
    for part in parts:
        if part.name == name:
            return part
        found = _find(part.children, name)
        if found is not None:
            return found
    return None


def _require(model: ModelDef, name: str) -> PartNode:
    part = _find(model.parts, name)
    if part is None:
        raise KeyError(f"{model.registry_id}: missing part '{name}'")
    return part


def _add_cubes(model: ModelDef, part_name: str, cubes: Sequence[CubeSpec]) -> None:
    part = _require(model, part_name)
    part.cubes = tuple(part.cubes) + tuple(cubes)


def _add_children(model: ModelDef, parent_name: str, nodes: Sequence[PartNode]) -> None:
    parent = _require(model, parent_name)
    parent.children = tuple(parent.children) + tuple(nodes)


def _add_roots(model: ModelDef, nodes: Sequence[PartNode]) -> None:
    model.parts = tuple(model.parts) + tuple(nodes)


# ---------------------------------------------------------------------------
# Spore Behemoth (Tier 1 mini-boss)
# ---------------------------------------------------------------------------

def _upgrade_spore_behemoth(model: ModelDef) -> None:
    _add_cubes(model, "torso", (
        _c("behemoth_spine_plate_a", -8, -19, 6.5, 16, 3, 4, role="bone", tags=TAG_CHITIN),
        _c("behemoth_spine_plate_b", -8, -11, 6.5, 16, 3, 4, role="bone", tags=TAG_CHITIN),
        _c("behemoth_spine_plate_c", -8, -4, 6, 16, 3, 4, role="bone", tags=TAG_CHITIN),
        _c("rib_hoop_r", -9.5, -9, 6.5, 2, 2, 6, role="bone"),
        _c("rib_hoop_l", 7.5, -9, 6.5, 2, 2, 6, role="bone", mirror=True),
        _c("sternum_maw", -3, -6, -8.2, 6, 5, 2, role="glow_core", emissive=True, tags=TAG_VEINS),
        _c("maw_fang_r", -4, -5, -9.2, 2, 3, 2, role="bone"),
        _c("maw_fang_l", 2, -5, -9.2, 2, 3, 2, role="bone", mirror=True),
        _c("pyroclast_duct_r", -9.5, -2, -6, 2, 5, 3, role="glow_core", emissive=True),
        _c("pyroclast_duct_l", 7.5, -2, -6, 2, 5, 3, role="glow_core", emissive=True, mirror=True),
        _c("behemoth_pelvic_band", -6, 5, -4, 12, 2, 8, role="armor"),
    ))
    _add_cubes(model, "head", (
        _c("cheek_plate_r", -6, -4, -7.5, 2, 4, 5, role="armor"),
        _c("cheek_plate_l", 4, -4, -7.5, 2, 4, 5, role="armor", mirror=True),
        _c("brow_spike_c", -1.5, -8, -7.8, 3, 3, 3, role="bone"),
        _c("spore_throat", 0, 2, -6.5, 4, 2, 2, role="glow_core", emissive=True),
        _c("horn_ram_r", -7.5, -13, -4, 3, 7, 3, role="bone"),
        _c("horn_ram_tip_r", -8, -17, -5, 2, 4, 2, role="glow_core", emissive=True),
        _c("horn_stub_l", 5.5, -10, -4, 2, 4, 3, role="bone", mirror=True),
        _c("ear_fan_r", -8, -3, -3, 2, 5, 4, role="armor"),
        _c("ear_fan_l", 6, -3, -3, 2, 5, 4, role="armor", mirror=True),
    ))
    _add_cubes(model, "jaw_node", (
        _c("jaw_grinder_r", -4, 2.8, -5.2, 2, 2, 1, role="bone"),
        _c("jaw_grinder_l", 2, 2.8, -5.2, 2, 2, 1, role="bone", mirror=True),
    ))
    _add_cubes(model, "hunch_sac", (
        _c("furnace_shell", -5, -4, -6.5, 10, 4, 1, role="armor"),
        _c("furnace_core", -3, -3.5, -7, 6, 3, 1, role="glow_core", emissive=True),
        _c("furnace_gauge_r", -5.5, -2, -6.2, 1, 2, 1, role="glow_eye", emissive=True),
        _c("furnace_gauge_l", 4.5, -2, -6.2, 1, 2, 1, role="glow_eye", emissive=True, mirror=True),
        _c("chimney_cap_r", -5.5, -11, -2, 3, 1, 3, role="armor"),
        _c("chimney_cap_c", -1.5, -12, -1.5, 3, 1, 3, role="armor"),
        _c("chimney_cap_l", 2.5, -10, 1, 3, 1, 3, role="armor", mirror=True),
        _c("furnace_stack_a", -2, -23, -1.5, 4, 11, 4, role="armor", tags=TAG_CHITIN),
        _c("furnace_stack_b", -1, -27, -1, 2, 5, 2, role="glow_core", emissive=True),
    ))
    _add_children(model, "hunch_sac", (
        _n("pressure_pistons", (0, -3, 2.5), (
            _c("pressure_piston_r", -6.5, 0, -1, 2, 3, 2, role="armor"),
            _c("pressure_piston_l", 4.5, 0, -1, 2, 3, 2, role="armor", mirror=True),
        ), anim_role="piston_pump"),
    ))
    _add_cubes(model, "arm_r", (
        _c("pauldron_horn_r", -8.5, -5, -2, 3, 3, 8, role="armor"),
        _c("pauldron_horn_tip_r", -11, -3, -1, 3, 3, 4, role="bone"),
    ))
    _add_cubes(model, "arm_l", (
        _c("pauldron_horn_l", 5.5, -5, -2, 3, 3, 8, role="armor", mirror=True),
        _c("pauldron_horn_tip_l", 8, -3, -1, 3, 3, 4, role="bone", mirror=True),
    ))
    _add_cubes(model, "mace_forearm", (
        _c("ram_piston_a_r", -2, 2, -6, 4, 3, 3, role="armor"),
        _c("ram_piston_b_r", -2, 5, -7, 4, 3, 4, role="armor"),
        _c("ram_bash_plate", -6.5, 9, -9, 13, 4, 2, role="armor", tags=TAG_CHITIN),
    ))
    _add_children(model, "mace_forearm", (
        _n("ram_pistons", (0, 4, -7), (
            _c("ram_piston_core", -2, 0, -1, 4, 2, 2, role="armor"),
            _c("ram_piston_pipe", 2, 0, 1, 1, 3, 1, role="glow_core", emissive=True),
        ), anim_role="piston_pump", anim_phase=0.6),
    ))
    _add_cubes(model, "claw_forearm", (
        _c("claw_palm_plate", -4.5, 6, -5.5, 9, 3, 6, role="armor"),
        _c("claw_finger_1", -4, 10, -6, 2, 4, 4, role="bone"),
        _c("claw_finger_2", -1, 10, -6, 2, 4, 4, role="bone"),
        _c("claw_finger_3", 2, 10, -6, 2, 4, 4, role="bone"),
        _c("claw_thumb_2", 3.5, 9, -3, 2, 4, 3, role="bone", mirror=True),
    ))
    for side, sign in (("r", -1), ("l", 1)):
        leg_part = "right_leg" if side == "r" else "left_leg"
        mirror = side == "l"
        base_x = -4.5 if side == "r" else 2.5
        mirror_x = -2.5 if side == "r" else 1.5
        _add_cubes(model, leg_part, (
            _c(f"hip_actuator_{side}", base_x, -1, -4.5, 2, 4, 2, role="armor", mirror=mirror),
            _c(f"knee_piston_{side}", base_x, 3.5, 3.5, 2, 6, 2, role="armor", mirror=mirror),
            _c(f"hoof_plate_{side}", -4, 11, -4.5, 8, 3, 9, role="armor", mirror=mirror),
            _c(f"hoof_toe_{side}", -3.5, 13.5, -6.5, 7, 2, 3, role="bone", mirror=mirror),
        ))
    # MUTATED and ELITE layers
    _add_cubes(model, "mutated_behemoth_spikes", (
        _c("b_spike_2_r", -7.5, -7, 3, 3, 7, 3, role="glow_core", emissive=True),
        _c("b_spike_2_l", 4.5, -7, 3, 3, 7, 3, role="glow_core", emissive=True, mirror=True),
        _c("b_spike_3_r", -6.5, -4, 1, 2, 4, 2, role="bone"),
        _c("b_spike_3_l", 4.5, -4, 1, 2, 4, 2, role="bone", mirror=True),
    ))
    _add_children(model, "hunch_sac", (
        _n("mutated_mortar_pods", (0, -11, 1), (
            _c("mortar_pod_r", -6, -2, -2, 4, 4, 4, role="armor"),
            _c("mortar_pod_l", 2, -2, -2, 4, 4, 4, role="armor", mirror=True),
            _c("mortar_barrel_r", -5.5, -5, -1, 3, 3, 3, role="bone"),
            _c("mortar_barrel_l", 2.5, -5, -1, 3, 3, 3, role="bone", mirror=True),
            _c("mortar_vent_r", -5, -6, -0.5, 2, 2, 2, role="glow_core", emissive=True),
            _c("mortar_vent_l", 3, -6, -0.5, 2, 2, 2, role="glow_core", emissive=True, mirror=True),
        ), rank_min="MUTATED", anim_role="sac_pulse"),
    ))
    _add_cubes(model, "elite_behemoth_crown", (
        _c("crown_horn_3_r", -7.5, -7, 1, 2, 5, 2, role="bone"),
        _c("crown_horn_3_l", 5.5, -7, 1, 2, 5, 2, role="bone", mirror=True),
        _c("crown_horn_4_r", -5.5, -9, 4, 2, 4, 2, role="bone"),
        _c("crown_horn_4_l", 3.5, -9, 4, 2, 4, 2, role="bone", mirror=True),
    ))
    _add_children(model, "torso", (
        _n("elite_siege_crown", (0, -18, 3.5), (
            _c("siege_horn_1", -3, -6, -2, 2, 5, 2, role="bone"),
            _c("siege_horn_2", -6, -5, 0, 2, 4, 2, role="bone"),
            _c("siege_horn_3", 4, -5, 0, 2, 4, 2, role="bone", mirror=True),
            _c("siege_reactor_core", -2.5, -4, -2.5, 5, 4, 5, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            _c("siege_reactor_ring", -3, -1, -3, 6, 1, 6, role="armor"),
            _c("siege_reactor_vent", 0, -3, -3, 2, 2, 1, role="glow_eye", emissive=True),
        ), rank_min="ELITE", anim_role="orbit_ring_b", anim_amp=-1.0),
    ))


# ---------------------------------------------------------------------------
# Hive Core (Tier 2 boss)
# ---------------------------------------------------------------------------

def _upgrade_hive_core(model: ModelDef) -> None:
    _add_cubes(model, "core_body", (
        _c("core_shell_rib_nw", -7.5, -9, -7.5, 1, 18, 1, role="armor", tags=TAG_CHITIN),
        _c("core_shell_rib_ne", 6.5, -9, -7.5, 1, 18, 1, role="armor", mirror=True, tags=TAG_CHITIN),
        _c("core_shell_rib_sw", -7.5, -9, 6.5, 1, 18, 1, role="armor", tags=TAG_CHITIN),
        _c("core_shell_rib_se", 6.5, -9, 6.5, 1, 18, 1, role="armor", mirror=True, tags=TAG_CHITIN),
        _c("core_eye", -2, -3, -7, 4, 6, 1, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        _c("core_vein_n", -1, -10, -7.2, 2, 10, 1, role="glow_core", emissive=True),
        _c("core_vein_s", -1, -1, -7.2, 2, 10, 1, role="glow_core", emissive=True),
        _c("core_vein_e", 6.2, -10, -1, 1, 10, 2, role="glow_core", emissive=True),
        _c("core_vein_w", -7.2, -10, -1, 1, 10, 2, role="glow_core", emissive=True, mirror=True),
        _c("crown_spire_n", -1.5, -18, -5.5, 3, 5, 2, role="bone"),
        _c("crown_spire_s", -1.5, -18, 3.5, 3, 5, 2, role="bone"),
        _c("crown_spire_e", 3.5, -18, -1.5, 2, 5, 3, role="bone", mirror=True),
        _c("crown_spire_w", -5.5, -18, -1.5, 2, 5, 3, role="bone"),
        _c("crown_spire_ne", 2.5, -16, -4, 2, 4, 2, role="bone", mirror=True),
        _c("crown_spire_nw", -4.5, -16, -4, 2, 4, 2, role="bone"),
        _c("crown_spire_se", 2.5, -16, 2, 2, 4, 2, role="bone", mirror=True),
        _c("crown_spire_sw", -4.5, -16, 2, 2, 4, 2, role="bone"),
        _c("nether_spire_tip", -1.5, 17, -1.5, 3, 4, 3, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
        _c("root_crown_n", -1, 14, -5, 2, 5, 2, role="bone"),
        _c("root_crown_s", -1, 14, 3, 2, 5, 2, role="bone"),
        _c("root_crown_e", 3, 14, -1, 2, 5, 2, role="bone", mirror=True),
        _c("root_crown_w", -5, 14, -1, 2, 5, 2, role="bone"),
        _c("conduit_bridge_ne", 3, -8, -7, 2, 2, 2, role="armor"),
        _c("conduit_bridge_nw", -5, -8, -7, 2, 2, 2, role="armor", mirror=True),
        _c("conduit_bridge_se", 3, -8, 5, 2, 2, 2, role="armor"),
        _c("conduit_bridge_sw", -5, -8, 5, 2, 2, 2, role="armor", mirror=True),
        _c("crown_spire_crown", -2.5, -34, -2.5, 5, 21, 5, role="bone", tags=TAG_RUNE),
        _c("crown_spire_crown_gem", -1, -31, -1, 2, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        _c("crown_spire_crown_collar", -3.5, -30, -3.5, 7, 1, 7, role="armor", tags=TAG_RUNE),
        _c("outer_rib_w", -23, -6, -2, 16, 12, 4, role="armor", tags=TAG_CHITIN),
        _c("outer_rib_e", 7, -6, -2, 16, 12, 4, role="armor", mirror=True, tags=TAG_CHITIN),
    ))
    _add_children(model, "core_body", (
        _n("crown_chandelier", (0, -13, 0), (
            _c("chandelier_n", -6, -1, -7, 12, 2, 2, role="bone", tags=TAG_RUNE),
            _c("chandelier_s", -6, -1, 5, 12, 2, 2, role="bone", tags=TAG_RUNE),
            _c("chandelier_w", -7, -1, -5, 2, 2, 10, role="glow_core", emissive=True),
            _c("chandelier_e", 5, -1, -5, 2, 2, 10, role="glow_core", emissive=True),
            _c("chandelier_gem_n", -1, -2, -8, 2, 2, 1, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("chandelier_gem_s", -1, -2, 7, 2, 2, 1, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        ), anim_role="orbit_ring_a", anim_phase=0.8),
    ))
    for name, cube in (
        ("calyx_bloom_n", _c("calyx_petal_n", -3, 0, -6, 6, 8, 1, role="armor", tags=TAG_CHITIN)),
        ("calyx_bloom_s", _c("calyx_petal_s", -3, 0, 5, 6, 8, 1, role="armor", tags=TAG_CHITIN)),
        ("calyx_bloom_e", _c("calyx_petal_e", 4, 0, -3, 1, 8, 6, role="armor", mirror=True, tags=TAG_CHITIN)),
        ("calyx_bloom_w", _c("calyx_petal_w", -5, 0, -3, 1, 8, 6, role="armor", tags=TAG_CHITIN)),
    ):
        _add_children(model, "core_body", (_n(name, (0, 9, 0), (cube,), anim_role="petal_flare"),))
    for side, gem_name in (("n", "rib_n_gem"), ("s", "rib_s_gem"), ("e", "rib_e_eye"), ("w", "rib_w_eye")):
        rib = _require(model, f"rib_{side}")
        rib.cubes = tuple(rib.cubes) + (
            _c(gem_name, -1, 5, -2, 1, 3, 1, role="glow_eye", emissive=True),
            _c(f"rib_collar_{side}", -2.5, 0, -2, 5, 1, 4, role="armor"),
        )
    for idx, (px, pz) in enumerate(((11, 11), (-11, 11), (11, -11), (-11, -11)), start=9):
        _add_roots(model, (
            _n(f"shard_{idx}", (px, -4, pz), (
                _c(f"shard_{idx}", -1.5, -4, -1.5, 3, 8, 3, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
                _c(f"shard_{idx}_cap", -1, -6, -1, 2, 2, 2, role="bone"),
            ), anim_role="orbit_orb", anim_phase=idx * 0.9),
        ))
    for idx, (px, pz, phase) in enumerate(((-2, -7, 0.3), (2, -7, 1.2), (-2, 7, 2.1), (2, 7, 3.0)), start=7):
        _add_children(model, "core_body", (
            _n(f"tendril_{idx}", (px, 11, pz), (
                _c(f"tendril_{idx}", -0.5, 0, -0.5, 1, 12, 1, role="skin", tags=TAG_VEINS),
            ), anim_role="tendril_sway", anim_phase=phase, children=(
                _n(f"tendril_{idx}_tip", (0, 11.5, 0), (
                    _c(f"tendril_{idx}_tip", -0.5, 0, -0.5, 1, 7, 1, role="glow_core", emissive=True),
                ), anim_role="tendril_sway", anim_phase=phase + 0.9),
            )),
        ))
    # Phase 2 "Armored Nexus"
    _add_children(model, "core_body", (
        _n("phase2_armor_shell", (0, -2, 0), (
            _c("p2_shell_n", -8, -6, -8, 16, 12, 2, role="armor", tags=TAG_CHITIN),
            _c("p2_shell_s", -8, -6, 6, 16, 12, 2, role="armor", tags=TAG_CHITIN),
            _c("p2_shell_e", 6, -6, -6, 2, 12, 12, role="armor", mirror=True, tags=TAG_CHITIN),
            _c("p2_shell_w", -8, -6, -6, 2, 12, 12, role="armor", tags=TAG_CHITIN),
        ), phase_min=2),
        _n("phase2_crystal_wing_r", (7, -4, 4), (
            _c("p2_wing_r", 0, -8, 0, 1, 12, 6, role="armor", tags=TAG_CHITIN),
            _c("p2_wing_r_vein", 1, -6, 1, 1, 8, 3, role="glow_core", emissive=True),
        ), phase_min=2, anim_role="petal_flare", anim_phase=0.4),
        _n("phase2_crystal_wing_l", (-7, -4, 4), (
            _c("p2_wing_l", -1, -8, 0, 1, 12, 6, role="armor", mirror=True, tags=TAG_CHITIN),
            _c("p2_wing_l_vein", -2, -6, 1, 1, 8, 3, role="glow_core", emissive=True, mirror=True),
        ), phase_min=2, anim_role="petal_flare", anim_phase=1.2),
        _n("phase2_bastion_ring", (0, 0, 0), (
            _c("p2_bastion_n", -9, -1, -10, 18, 2, 2, role="armor"),
            _c("p2_bastion_s", -9, -1, 8, 18, 2, 2, role="armor"),
            _c("p2_bastion_w", -10, -1, -8, 2, 2, 16, role="glow_core", emissive=True),
            _c("p2_bastion_e", 8, -1, -8, 2, 2, 16, role="glow_core", emissive=True),
            _c("p2_bastion_gem_n", -1, -2, -10, 2, 2, 1, role="glow_eye", emissive=True),
            _c("p2_bastion_gem_s", -1, -2, 9, 2, 2, 1, role="glow_eye", emissive=True),
        ), phase_min=2, anim_role="orbit_ring_a", anim_amp=1.15),
    ))
    for idx, (px, pz) in enumerate(((-5, -6), (5, -6), (0, -8)), start=1):
        _add_children(model, "core_body", (
            _n(f"phase2_eye_{idx}", (px, -2, pz), (
                _c(f"phase2_eye_{idx}", -1, -1, -1, 2, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            ), phase_min=2, anim_role="orbit_orb", anim_phase=idx * 1.1),
        ))
    _add_children(model, "core_body", (
        _n("phase2_core_lance", (0, 4, -8), (
            _c("p2_lance_shaft", -1, 0, -3, 2, 2, 6, role="bone"),
            _c("p2_lance_tip", -1.5, -1, -6, 3, 3, 3, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        ), phase_min=2),
    ))


# ---------------------------------------------------------------------------
# Apex Matriarch (Tier 3 final boss)
# ---------------------------------------------------------------------------

def _upgrade_apex_matriarch(model: ModelDef) -> None:
    _add_cubes(model, "body", (
        _c("royal_gorget", -7.5, -16, -6, 15, 3, 12, role="bone", tags=TAG_CHITIN),
        _c("sternum_reliquary", -4, -13, -7, 8, 7, 1, role="bone", tags=TAG_RUNE),
        _c("sternum_reliquary_gem", -2.5, -11, -7.5, 5, 5, 1, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
        _c("rib_filigree_r", -8, -12, -5, 1, 8, 10, role="armor", tags=TAG_RUNE),
        _c("rib_filigree_l", 7, -12, -5, 1, 8, 10, role="armor", mirror=True, tags=TAG_RUNE),
        _c("pelvis_plate_r", -7.5, 0, -5.5, 3, 4, 11, role="armor"),
        _c("pelvis_plate_l", 4.5, 0, -5.5, 3, 4, 11, role="armor", mirror=True),
        _c("hip_rail_2_r", -8.5, 3, -5, 3, 2, 10, role="bone"),
        _c("hip_rail_2_l", 5.5, 3, -5, 3, 2, 10, role="bone", mirror=True),
    ))
    _add_children(model, "body", (
        _n("royal_cloak_r", (-6, -10, 5), (
            _c("royal_cloak_r", -1, 0, 0, 1, 14, 6, role="skin", tags=TAG_VEINS),
        ), anim_role="tendril_sway", anim_phase=0.4),
        _n("royal_cloak_l", (6, -10, 5), (
            _c("royal_cloak_l", -1, 0, 0, 1, 14, 6, role="skin", mirror=True, tags=TAG_VEINS),
        ), anim_role="tendril_sway", anim_phase=1.4),
    ))
    _add_cubes(model, "head", (
        _c("horn_ir", -3.5, -21, -1, 2, 5, 2, role="bone"),
        _c("horn_il", 1.5, -21, -1, 2, 5, 2, role="bone", mirror=True),
        _c("horn_crown_rear", -1, -17, 3, 2, 5, 2, role="bone"),
        _c("crown_wing_tier2_r", -9.5, -11, 3, 2, 5, 3, role="armor", tags=TAG_CRYSTAL),
        _c("crown_wing_tier2_l", 7.5, -11, 3, 2, 5, 3, role="armor", mirror=True, tags=TAG_CRYSTAL),
        _c("crown_veil", -3, -9, 4, 6, 6, 1, role="skin"),
        _c("crown_apex_spire", -2.5, -37, -2.5, 5, 17, 5, role="bone", tags=TAG_RUNE),
        _c("crown_apex_gem", -1, -33, -1, 2, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        _c("crown_apex_collar", -4, -31, -4, 8, 1, 8, role="armor", tags=TAG_RUNE),
    ))
    _add_children(model, "u_scythe_r", (
        _n("scythe_fan_r", (0, 1, -8), (
            _c("scythe_fan_blade_1", -1, 0, -2, 1, 3, 5, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            _c("scythe_fan_blade_2", -1, 3, -3, 1, 2, 4, role="bone"),
            _c("scythe_fan_blade_3", -1, -1, -3, 1, 2, 4, role="bone"),
        ), anim_role="forearm", anim_phase=0.3),
    ))
    _add_children(model, "u_scythe_l", (
        _n("scythe_fan_l", (0, 1, -8), (
            _c("scythe_fan_blade_1_l", -1, 0, -2, 1, 3, 5, role="glow_core", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            _c("scythe_fan_blade_2_l", -1, 3, -3, 1, 2, 4, role="bone", mirror=True),
            _c("scythe_fan_blade_3_l", -1, -1, -3, 1, 2, 4, role="bone", mirror=True),
        ), anim_role="forearm", anim_phase=1.0),
    ))
    _add_cubes(model, "lower_arm_r", (
        _c("lower_claw_finger_r", -1, 10, -7, 2, 2, 4, role="glow_eye", emissive=True),
        _c("lower_arm_joint_r", -2.5, -1, -2, 5, 2, 4, role="armor"),
    ))
    _add_cubes(model, "lower_arm_l", (
        _c("lower_claw_finger_l", -1, 10, -7, 2, 2, 4, role="glow_eye", emissive=True, mirror=True),
        _c("lower_arm_joint_l", -2.5, -1, -2, 5, 2, 4, role="armor", mirror=True),
    ))
    from .model_defs import _arachnid_leg

    _add_roots(model, (
        _arachnid_leg("m_leg_r4", (-7.0, 15.0, 11.0), 0.48, "r", 0.0, coxa_len=10, tibia_len=11, thick=3),
        _arachnid_leg("m_leg_l4", (7.0, 15.0, 11.0), -0.48, "l", 1.5, coxa_len=10, tibia_len=11, thick=3),
    ))
    abdomen = _require(model, "abdomen")
    abdomen.cubes = tuple(abdomen.cubes) + (
        _c("brood_chamber", -6, -4, 4, 12, 9, 8, role="armor", tags=TAG_CHITIN),
        _c("egg_sac_1", -5, -5, 5, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("egg_sac_2", -1, -6, 6, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("egg_sac_3", 3, -5, 5, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("egg_sac_4", -3, -4, 9, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("dorsal_egg_sac_2_r", -7, -8, 10, 3, 3, 5, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("dorsal_egg_sac_2_l", 4, -8, 10, 3, 3, 5, role="glow_core", emissive=True, mirror=True, tags=TAG_BROOD),
        _c("royal_outrider_rail_r", -32, -5, 10, 12, 1, 1, role="bone"),
        _c("royal_outrider_rail_r2", -20, -5, 10, 11, 1, 1, role="bone"),
        _c("royal_outrider_r", -32, -6, 8, 3, 4, 4, role="armor", tags=TAG_CHITIN),
        _c("royal_outrider_rail_l", 20, -5, 10, 12, 1, 1, role="bone", mirror=True),
        _c("royal_outrider_rail_l2", 9, -5, 10, 11, 1, 1, role="bone", mirror=True),
        _c("royal_outrider_l", 29, -6, 8, 3, 4, 4, role="armor", mirror=True, tags=TAG_CHITIN),
    )
    _add_children(model, "abdomen", (
        _n("ovipositor", (0, -2, 17), (
            _c("ovipositor_shaft", -1, 0, 0, 2, 2, 8, role="armor"),
            _c("ovipositor_tip", -0.5, 0, 7, 1, 1, 3, role="glow_eye", emissive=True),
        ), anim_role="tail_chain", anim_phase=0.6),
        _n("petal_upper_r", (-8, -8, 6), (
            _c("petal_upper_r", -1, -10, -4, 2, 12, 8, role="armor", tags=TAG_CHITIN),
            _c("petal_upper_r_vein", 0, -8, -2, 1, 8, 4, role="glow_core", emissive=True),
        ), anim_role="mantle_wing_r", anim_phase=0.2),
        _n("petal_upper_l", (8, -8, 6), (
            _c("petal_upper_l", -1, -10, -4, 2, 12, 8, role="armor", mirror=True, tags=TAG_CHITIN),
            _c("petal_upper_l_vein", -1, -8, -2, 1, 8, 4, role="glow_core", emissive=True, mirror=True),
        ), anim_role="mantle_wing_l", anim_phase=0.2),
        _n("mantle_membrane_r", (0, -4, 8), (
            _c("mantle_membrane_r", -1, -8, -2, 1, 10, 2, role="skin"),
        )),
        _n("mantle_membrane_l", (1, -4, 8), (
            _c("mantle_membrane_l", 0, -8, -2, 1, 10, 2, role="skin", mirror=True),
        )),
    ))
    # Phase 2 and Phase 3 full forms
    _add_cubes(model, "abdomen", (
        _c("p2_carapace_a", -7, -6, 1, 14, 3, 10, role="armor", tags=TAG_CHITIN),
        _c("p2_carapace_b", -6, -5, 8, 12, 2, 6, role="armor", tags=TAG_CHITIN),
    ))
    _add_children(model, "abdomen", (
        _n("phase2_wing_upper_r", (-8, -7, 8), (
            _c("p2_wing_upper_r", -1, -9, -3, 2, 11, 6, role="armor", tags=TAG_CHITIN),
            _c("p2_wing_upper_r_vein", 0, -7, -1, 1, 7, 3, role="glow_core", emissive=True),
        ), phase_min=2, anim_role="mantle_wing_r", anim_phase=0.6),
        _n("phase2_wing_upper_l", (8, -7, 8), (
            _c("p2_wing_upper_l", -1, -9, -3, 2, 11, 6, role="armor", mirror=True, tags=TAG_CHITIN),
            _c("p2_wing_upper_l_vein", -1, -7, -1, 1, 7, 3, role="glow_core", emissive=True, mirror=True),
        ), phase_min=2, anim_role="mantle_wing_l", anim_phase=0.6),
        _n("p2_bastion_ring", (0, -8, 8), (
            _c("p2_bastion_n", -10, -1, -11, 20, 2, 2, role="armor", tags=TAG_RUNE),
            _c("p2_bastion_s", -10, -1, 9, 20, 2, 2, role="armor", tags=TAG_RUNE),
            _c("p2_bastion_w", -11, -1, -9, 2, 2, 18, role="glow_core", emissive=True),
            _c("p2_bastion_e", 9, -1, -9, 2, 2, 18, role="glow_core", emissive=True),
            _c("p2_bastion_gem_n", -1, -2, -11, 2, 2, 1, role="glow_eye", emissive=True),
            _c("p2_bastion_gem_s", -1, -2, 10, 2, 2, 1, role="glow_eye", emissive=True),
        ), phase_min=2, anim_role="orbit_ring_a", anim_amp=1.2),
    ))
    for idx, (px, py) in enumerate(((-6, -12), (6, -12), (0, -16)), start=1):
        _add_children(model, "abdomen", (
            _n(f"p2_gem_orb_{idx}", (px, py, 8), (
                _c(f"p2_gem_orb_{idx}", -1, -1, -1, 2, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            ), phase_min=2, anim_role="orbit_orb", anim_phase=idx * 1.2),
        ))
    _add_children(model, "abdomen", (
        _n("p3_ascension_wings_r", (-9, -10, 10), (
            _c("p3_wing_r_a", -1, -8, -4, 2, 12, 5, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            _c("p3_wing_r_b", 1, -6, -2, 1, 9, 3, role="glow_eye", emissive=True),
        ), phase_min=3, anim_role="mantle_wing_r", anim_phase=0.3),
        _n("p3_ascension_wings_l", (9, -10, 10), (
            _c("p3_wing_l_a", -1, -8, -4, 2, 12, 5, role="glow_core", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            _c("p3_wing_l_b", -2, -6, -2, 1, 9, 3, role="glow_eye", emissive=True, mirror=True),
        ), phase_min=3, anim_role="mantle_wing_l", anim_phase=0.3),
        _n("p3_halo", (0, -12, 8), (
            _c("p3_halo_n", -8, -1, -9, 16, 2, 2, role="glow_core", emissive=True, tags=TAG_RUNE),
            _c("p3_halo_s", -8, -1, 8, 16, 2, 2, role="glow_core", emissive=True, tags=TAG_RUNE),
            _c("p3_halo_w", -9, -1, -7, 2, 2, 15, role="glow_eye", emissive=True),
            _c("p3_halo_e", 7, -1, -7, 2, 2, 15, role="glow_eye", emissive=True),
            _c("p3_halo_gem_n", -1, -2, -9, 2, 2, 1, role="glow_core", emissive=True),
            _c("p3_halo_gem_s", -1, -2, 9, 2, 2, 1, role="glow_core", emissive=True),
        ), phase_min=3, anim_role="orbit_ring_b", anim_amp=-1.2),
        _n("p3_crown_rear", (0, -6, 14), (
            _c("p3_crown_rear", -1, -6, -1, 2, 7, 2, role="bone"),
            _c("p3_crown_rear_gem", -1.5, -3, -2, 3, 2, 1, role="glow_eye", emissive=True),
        ), phase_min=3),
        _n("p3_core", (0, -6, 12), (
            _c("p3_core", -2.5, -2, -2.5, 5, 5, 5, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            _c("p3_core_shell", -3, -1, -3, 6, 3, 6, role="armor"),
        ), phase_min=3, anim_role="sac_pulse"),
    ))
    for idx, (px, py) in enumerate(((-7, -14), (7, -14), (0, -18)), start=1):
        _add_children(model, "abdomen", (
            _n(f"p3_choir_{idx}", (px, py, 10), (
                _c(f"p3_choir_{idx}", -1, -1, -1, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            ), phase_min=3, anim_role="orbit_orb", anim_phase=idx * 1.4),
        ))


# ---------------------------------------------------------------------------
# Greater mobs
# ---------------------------------------------------------------------------

def _upgrade_goliath_carrier(model: ModelDef) -> None:
    _add_cubes(model, "body", (
        _c("howdah_deck", -7, -13, -6, 14, 2, 12, role="bone", tags=TAG_CHITIN),
        _c("howdah_rail_r", -7, -15, -6, 2, 3, 12, role="armor"),
        _c("howdah_rail_l", 5, -15, -6, 2, 3, 12, role="armor", mirror=True),
        _c("howdah_canopy", -5, -18, -4, 10, 4, 8, role="spore_sac", emissive=True, tags=TAG_BROOD),
        _c("brood_pod_1", -5, -9, -5, 4, 5, 4, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("brood_pod_2", 1, -8, 0, 4, 5, 4, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("brood_pod_3", -2, -8, 4, 4, 5, 4, role="glow_core", emissive=True, tags=TAG_BROOD),
        _c("carrier_tusk_r", -6, -4, -11, 2, 4, 3, role="bone"),
        _c("carrier_tusk_l", 4, -4, -11, 2, 4, 3, role="bone", mirror=True),
        _c("tail_ovipositor", -1.5, -2, 9, 3, 3, 4, role="bone"),
        _c("howdah_mast", -1, -24, -1, 2, 10, 2, role="bone", tags=TAG_RUNE),
        _c("howdah_rail_out_r", -12, -15, -6, 2, 3, 12, role="armor"),
        _c("howdah_spur_r", -13, -13, -2, 2, 2, 4, role="bone"),
        _c("howdah_rail_out_l", 10, -15, -6, 2, 3, 12, role="armor", mirror=True),
        _c("howdah_spur_l", 12, -13, -2, 2, 2, 4, role="bone", mirror=True),
    ))
    _add_children(model, "body", (
        _n("carrier_banner_r", (-8, -2, -8), (
            _c("carrier_banner_r", -1, 0, 0, 1, 8, 4, role="glow_eye", emissive=True, tags=TAG_RUNE),
        ), anim_role="tendril_sway", anim_phase=0.5),
        _n("carrier_banner_l", (8, -2, -8), (
            _c("carrier_banner_l", -1, 0, 0, 1, 8, 4, role="glow_eye", emissive=True, mirror=True, tags=TAG_RUNE),
        ), anim_role="tendril_sway", anim_phase=1.5),
    ))
    for idx, (px, pz) in enumerate(((-5, -8), (5, -8), (-5, 8), (5, 8)), start=1):
        _add_children(model, "body", (
            _n(f"incubator_tendril_{idx}", (px, 4, pz), (
                _c(f"incubator_tendril_{idx}", -0.5, 0, -0.5, 1, 5, 1, role="spore_sac", emissive=True, tags=TAG_BROOD),
            ), anim_role="tendril_sway", anim_phase=idx * 0.8),
        ))
    _add_cubes(model, "mutated_side_sacs", (
        _c("side_sac_r_2", -13, -5, 2, 3, 4, 4, role="glow_core", emissive=True),
        _c("side_sac_l_2", 10, -5, 2, 3, 4, 4, role="glow_core", emissive=True, mirror=True),
    ))
    _add_children(model, "body", (
        _n("mutated_howdah", (0, -13, 0), (
            _c("mut_howdah_spike_r", -6, -3, -5, 2, 3, 10, role="bone"),
            _c("mut_howdah_spike_l", 4, -3, -5, 2, 3, 10, role="bone", mirror=True),
        ), rank_min="MUTATED"),
        _n("elite_crown", (0, -15, 0), (
            _c("carrier_crown_1", -1, -6, -4, 2, 5, 2, role="bone"),
            _c("carrier_crown_2", -4, -5, 0, 2, 4, 2, role="bone"),
            _c("carrier_crown_3", 2, -5, 0, 2, 4, 2, role="bone", mirror=True),
        ), rank_min="ELITE"),
        _n("elite_brood_beacon", (0, -11, 0), (
            _c("elite_brood_beacon", -2, -2, -6, 4, 4, 1, role="glow_eye", emissive=True, tags=TAG_BROOD),
        ), rank_min="ELITE", anim_role="sac_pulse"),
        _n("elite_halo", (0, -14, 0), (
            _c("carrier_halo_n", -7, -1, -8, 14, 2, 2, role="glow_core", emissive=True, tags=TAG_RUNE),
            _c("carrier_halo_s", -7, -1, 6, 14, 2, 2, role="glow_core", emissive=True, tags=TAG_RUNE),
            _c("carrier_halo_gem", -1, -2, -8, 2, 2, 1, role="glow_eye", emissive=True),
        ), rank_min="ELITE", anim_role="orbit_ring_a"),
    ))
    for side in ("r", "l"):
        leg = "leg_fl" if side == "r" else "leg_fr"
        _add_children(model, leg, (
            _n(f"mutated_leg_plate_{side}", (0, 0, 0), (
                _c(f"mutated_leg_plate_{side}", -3, 1, 1, 6, 3, 3, role="bone", mirror=(side == "l"), tags=TAG_CHITIN),
            ), rank_min="MUTATED"),
        ))
    _add_cubes(model, "elite_dorsal_crest", (
        _c("crest_c", -2, -6, -2, 4, 6, 4, role="bone", tags=TAG_CHITIN),
    ))


def _upgrade_colony_warden(model: ModelDef) -> None:
    _add_cubes(model, "torso", (
        _c("banner_pole", -0.5, -18, 3, 1, 10, 1, role="bone"),
        _c("colony_standard", -5, -26, 3.2, 4, 8, 1, role="skin", tags=TAG_RUNE),
        _c("colony_standard_2", 1, -26, 3.2, 4, 8, 1, role="skin", mirror=True, tags=TAG_RUNE),
        _c("banner_top_spike", -1, -19, 3, 2, 2, 2, role="glow_eye", emissive=True),
        _c("tasset_skirt_f", -5, -1, -3.5, 10, 3, 1, role="armor", tags=TAG_CHITIN),
        _c("tasset_skirt_b", -5, -1, 2.5, 10, 3, 1, role="armor", tags=TAG_CHITIN),
        _c("banner_pole_tall", -0.5, -29, 3, 1, 16, 1, role="bone", tags=TAG_RUNE),
        _c("banner_crossbar", -7, -27, 3, 14, 1, 1, role="bone", tags=TAG_RUNE),
        _c("banner_wing_r", 8, -26, 3, 5, 2, 1, role="bone", tags=TAG_RUNE),
        _c("banner_wing_l", -13, -26, 3, 5, 2, 1, role="bone", mirror=True, tags=TAG_RUNE),
    ))
    _add_cubes(model, "head", (
        _c("helm_cheek_r", -4.5, -7.5, -4.5, 1, 4, 4, role="armor"),
        _c("helm_cheek_l", 3.5, -7.5, -4.5, 1, 4, 4, role="armor", mirror=True),
        _c("visor_rune", -2.5, -3, -4.6, 5, 1, 1, role="glow_eye", emissive=True, tags=TAG_RUNE),
    ))
    _add_cubes(model, "arm_r", (
        _c("pauldron_crest_r", -3.5, -4, -3, 1, 4, 4, role="bone"),
    ))
    _add_cubes(model, "arm_l", (
        _c("pauldron_crest_l", 2.5, -4, -3, 1, 4, 4, role="bone", mirror=True),
        _c("shield_lens", 3.5, 5, -5.5, 1, 4, 4, role="glow_core", emissive=True),
    ))
    for side, leg in (("r", "right_leg"), ("l", "left_leg")):
        _add_cubes(model, leg, (
            _c(f"warden_spur_heel_{side}", -2, 10, 2, 4, 3, 3, role="bone", mirror=(side == "l")),
        ))
    _add_children(model, "cloak", (
        _n("mutated_banner_strips", (0, 0, 0), (
            _c("mutated_banner_strip_1", -4.5, 4, 1.2, 2, 10, 1, role="glow_core", emissive=True),
            _c("mutated_banner_strip_2", 2.5, 4, 1.2, 2, 10, 1, role="glow_core", emissive=True, mirror=True),
        ), rank_min="MUTATED", anim_role="tendril_sway"),
    ))
    _add_children(model, "head", (
        _n("mutated_helm_spikes", (0, 0, 0), (
            _c("mutated_helm_spike_r", -4, -11, -1, 1, 3, 2, role="bone"),
            _c("mutated_helm_spike_l", 3, -11, -1, 1, 3, 2, role="bone", mirror=True),
        ), rank_min="MUTATED"),
    ))
    _add_cubes(model, "elite_halo", (
        _c("halo_gem_l", -2, 1, -0.5, 1, 1, 1, role="glow_eye", emissive=True),
        _c("halo_gem_r", 1, 1, -0.5, 1, 1, 1, role="glow_eye", emissive=True, mirror=True),
    ))
    _add_children(model, "head", (
        _n("elite_crown", (0, -8, 0), (
            _c("warden_crown_1", -1, -5, -3, 2, 5, 2, role="bone"),
            _c("warden_crown_2", -4, -4, 0, 2, 4, 2, role="bone"),
            _c("warden_crown_3", 2, -4, 0, 2, 4, 2, role="bone", mirror=True),
        ), rank_min="ELITE"),
    ))
    _add_children(model, "arm_l", (
        _n("elite_shield_gem", (0, 0, 0), (
            _c("elite_shield_gem", 4, 6, -3, 1, 1, 1, role="glow_core", emissive=True),
        ), rank_min="ELITE"),
    ))
    _add_children(model, "cloak", (
        _n("elite_cloak_runes", (0, 0, 0), (
            _c("elite_cloak_rune_1", -4, 6.5, 1.2, 1, 1, 1, role="glow_eye", emissive=True),
            _c("elite_cloak_rune_2", 3, 6.5, 1.2, 1, 1, 1, role="glow_eye", emissive=True, mirror=True),
        ), rank_min="ELITE"),
    ))


# ---------------------------------------------------------------------------
# Standard roster signature modules
# ---------------------------------------------------------------------------

def _upgrade_standard_mobs(model: ModelDef) -> None:
    reg = model.registry_id
    if reg == "sporeling":
        _add_cubes(model, "cap", (
            _c("cap_rib_f", -1, -2, -5, 2, 2, 1, role="spore_sac"),
            _c("cap_rib_b", -1, -2, 4, 2, 2, 1, role="spore_sac"),
            _c("cap_rib_l", -5, -2, -1, 1, 2, 2, role="spore_sac"),
            _c("cap_rib_r", 4, -2, -1, 1, 2, 2, role="spore_sac", mirror=True),
        ))
        _add_cubes(model, "body", (
            _c("mandible_nib_r", -2.5, -2, -5, 1, 1, 2, role="bone"),
            _c("mandible_nib_l", 1.5, -2, -5, 1, 1, 2, role="bone", mirror=True),
            _c("spore_pouch_r", -4, -4, -1, 2, 2, 2, role="spore_sac"),
            _c("spore_pouch_l", 2, -4, -1, 2, 2, 2, role="spore_sac", mirror=True),
        ))
    elif reg == "leech_parasite":
        _add_cubes(model, "mid", (
            _c("collar_plate", -3, -2.5, -4.5, 6, 1, 2, role="armor"),
            _c("dorsal_frill_1", -2.5, -3.2, -2, 5, 1, 2, role="armor"),
            _c("dorsal_frill_2", -2.5, -3.2, 0, 5, 1, 2, role="armor"),
            _c("dorsal_frill_3", -2.5, -3.2, 2, 5, 1, 2, role="armor"),
        ))
        _add_cubes(model, "rear", (
            _c("anchor_sucker", -2, -3, -2, 4, 1, 4, role="bone"),
        ))
        _add_cubes(model, "head", (
            _c("eye_cluster", -2.5, -3.5, -3.2, 1, 1, 1, role="glow_eye", emissive=True),
            _c("eye_cluster_l", 1.5, -3.5, -3.2, 1, 1, 1, role="glow_eye", emissive=True, mirror=True),
        ))
    elif reg == "acid_spitter":
        _add_cubes(model, "abdomen", (
            _c("spinneret", -1.5, -2, 8.5, 3, 2, 3, role="armor"),
            _c("spinneret_ring", -2, -1, 10, 4, 1, 1, role="glow_core", emissive=True),
            _c("abdomen_ridge_1", -4.5, -4.5, 2, 9, 1, 2, role="bone"),
            _c("abdomen_ridge_2", -4.5, -4.5, 5, 9, 1, 2, role="bone"),
        ))
        _add_cubes(model, "head", (
            _c("venom_tooth_r", -2, 1.5, -5.5, 1, 2, 1, role="bone"),
            _c("venom_tooth_l", 1, 1.5, -5.5, 1, 2, 1, role="bone", mirror=True),
        ))
        _add_cubes(model, "thorax", (
            _c("leg_spur_r", -4, -1, -3, 2, 1, 2, role="armor"),
            _c("leg_spur_l", 2, -1, -3, 2, 1, 2, role="armor", mirror=True),
        ))
    elif reg == "burrower":
        _add_cubes(model, "drill_head", (
            _c("helix_fin_r", -3.5, -1, -4, 1, 3, 5, role="bone"),
            _c("helix_fin_l", 2.5, -1, -4, 1, 3, 5, role="bone", mirror=True),
        ))
        _add_cubes(model, "body", (
            _c("mud_skirt_r", -4.5, -3, -1, 1, 2, 8, role="armor"),
            _c("mud_skirt_l", 3.5, -3, -1, 1, 2, 8, role="armor", mirror=True),
            _c("sensor_whisker_1", -1, -3.5, -5.5, 1, 1, 3, role="bone"),
            _c("sensor_whisker_2", 0, -3.5, -5.5, 1, 1, 3, role="bone", mirror=True),
        ))
    elif reg == "pyro_shroom":
        _add_cubes(model, "mid_seg", (
            _c("ember_grill_1", -3, -4.5, -2, 6, 1, 1, role="magma"),
            _c("ember_grill_2", -3, -4.5, -0.5, 6, 1, 1, role="magma"),
            _c("ember_grill_3", -3, -4.5, 1, 6, 1, 1, role="magma"),
            _c("ember_grill_4", -3, -4.5, 2.5, 6, 1, 1, role="magma"),
        ))
        _add_cubes(model, "tail_seg", (
            _c("ash_stack", -1, -4, 0, 2, 3, 2, role="armor"),
        ))
        _add_cubes(model, "head_seg", (
            _c("horn_tip_r", -3, -6, -3.5, 1, 2, 1, role="glow_core", emissive=True),
            _c("horn_tip_l", 2, -6, -3.5, 1, 2, 1, role="glow_core", emissive=True, mirror=True),
        ))
    elif reg == "chitin_brute":
        _add_cubes(model, "forearm_r", (
            _c("elbow_spur_r", -4, 3, 1, 2, 3, 3, role="bone"),
        ))
        _add_cubes(model, "forearm_l", (
            _c("elbow_spur_l", 2, 3, 1, 2, 3, 3, role="bone", mirror=True),
        ))
        _add_cubes(model, "torso", (
            _c("gut_plate", -4, 1, -5, 8, 3, 1, role="armor", tags=TAG_CHITIN),
            _c("back_harness", -5, -10, 4, 10, 2, 3, role="armor"),
        ))
        _add_cubes(model, "head", (
            _c("berzerker_horn", -0.5, -8, -1, 2, 4, 2, role="bone"),
        ))
    elif reg == "mycelial_stalker":
        _add_cubes(model, "head", (
            _c("hood_lappet_r", -4, -7, -2, 1, 6, 4, role="armor"),
            _c("hood_lappet_l", 3, -7, -2, 1, 6, 4, role="armor", mirror=True),
        ))
        _add_cubes(model, "torso", (
            _c("neck_vertebra_1", -2, -12.5, -2, 4, 1, 3, role="bone"),
            _c("neck_vertebra_2", -2, -11.5, -2, 4, 1, 3, role="bone"),
        ))
        _add_cubes(model, "r_shin", (_c("talon_spur_1", 1, 5, -1, 1, 2, 3, role="bone"),))
        _add_cubes(model, "l_shin", (_c("talon_spur_2", -2, 5, -1, 1, 2, 3, role="bone", mirror=True),))
    elif reg == "synapse_weaver":
        _add_cubes(model, "body", (
            _c("veil_1", -3, -1, 2.5, 6, 8, 1, role="skin"),
            _c("veil_2", -4, -1, -2, 1, 7, 5, role="skin"),
            _c("veil_3", 3, -1, -2, 1, 7, 5, role="skin", mirror=True),
        ))
        _add_cubes(model, "head", (
            _c("mind_lens", -1, -4, -4, 2, 2, 1, role="glow_eye", emissive=True),
            _c("crown_rune_m", -2.5, -9, 1, 5, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
        ))
        _add_children(model, "body", (
            _n("focus_ring", (0, -4, 0), (
                _c("focus_ring_n", -6, -1, -6, 12, 1, 1, role="armor", tags=TAG_RUNE),
                _c("focus_ring_s", -6, -1, 5, 12, 1, 1, role="armor", tags=TAG_RUNE),
                _c("focus_ring_gem", -1, -2, -6, 2, 1, 1, role="glow_eye", emissive=True),
            ), anim_role="orbit_ring_b"),
        ))
    elif reg == "spore_architect":
        _add_cubes(model, "torso", (
            _c("tank_valve", -3.5, -11, 2, 1, 2, 2, role="armor"),
        ))
        _add_cubes(model, "torso", (
            _c("blueprint_prism", 1, -10, 3, 2, 2, 2, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
            _c("tool_holster", -4, -6, 1, 1, 4, 2, role="bone"),
        ))
        _add_cubes(model, "right_leg", (_c("boot_plate_r", -2, 10, -2, 4, 2, 4, role="armor"),))
        _add_cubes(model, "left_leg", (_c("boot_plate_l", -2, 10, -2, 4, 2, 4, role="armor", mirror=True),))
    elif reg == "synaptic_mimic":
        _add_cubes(model, "head", (
            _c("mimic_visor_shard", -3, -9, -4.5, 2, 2, 1, role="glow_eye", emissive=True),
        ))
        _add_cubes(model, "arm_r", (
            _c("blade_back_ridge", 0, 2, -4, 1, 3, 4, role="armor"),
        ))
        _add_cubes(model, "arm_l", (
            _c("gauntlet_vent", 2.5, 5, -2, 1, 3, 2, role="glow_core", emissive=True),
        ))
        _add_children(model, "body", (
            _n("echo_tendril_r", (-4, -10, 2), (_c("echo_tendril_r", -1, 0, 0, 1, 6, 2, role="skin"),),
              anim_role="tendril_sway", anim_phase=0.4),
            _n("echo_tendril_l", (4, -10, 2), (_c("echo_tendril_l", -1, 0, 0, 1, 6, 2, role="skin", mirror=True),),
              anim_role="tendril_sway", anim_phase=1.4),
        ))
    elif reg == "mycelial_chimera":
        _add_cubes(model, "body", (
            _c("saddle_stirrup_r", -6, -3, -2, 1, 3, 2, role="armor"),
            _c("saddle_stirrup_l", 5, -3, -2, 1, 3, 2, role="armor", mirror=True),
            _c("tail_fan", -1.5, -6, 8, 3, 2, 4, role="spore_sac"),
            _c("mane_ring", -4.5, -8, -3, 9, 1, 2, role="bone"),
            _c("saddle_horn", -1, -9, -4, 2, 3, 2, role="bone"),
        ))
    elif reg == "purifier_drone":
        _add_cubes(model, "chassis", (
            _c("sensor_mast", -0.5, -8, -1, 1, 4, 1, role="bone"),
            _c("vent_baffle_r", -4.5, -2.5, 2, 2, 1, 3, role="armor"),
            _c("vent_baffle_l", 2.5, -2.5, 2, 2, 1, 3, role="armor", mirror=True),
            _c("landing_skid_r", -4, 2.5, 2, 2, 1, 5, role="bone"),
            _c("landing_skid_l", 2, 2.5, 2, 2, 1, 5, role="bone", mirror=True),
        ))


# ---------------------------------------------------------------------------
# Complete evolution layers
# ---------------------------------------------------------------------------

def _rank_layers(model: ModelDef) -> None:
    reg = model.registry_id
    if reg == "sporeling":
        _add_children(model, "cap", (
            _n("mutated_cap_growth", (0, -4, 0), (
                _c("mut_cap_lobe_r", -6, -1, -1, 2, 3, 3, role="spore_sac", tags=TAG_CRYSTAL),
                _c("mut_cap_lobe_l", 4, -1, -1, 2, 3, 3, role="spore_sac", mirror=True, tags=TAG_CRYSTAL),
                _c("mut_spore_vent", 0, -2, 4, 2, 2, 1, role="glow_core", emissive=True),
            ), rank_min="MUTATED", anim_role="sac_pulse"),
            _n("elite_halo", (0, -4, 0), (
                _c("elite_halo_n", -5, -1, -5, 10, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("elite_halo_s", -5, -1, 4, 10, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("elite_halo_gem", -1, -2, -5, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_a"),
        ))
        _add_cubes(model, "elite_crown", (
            _c("crown_shard_r", 2, -4, 0, 1, 3, 1, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("crown_shard_l", -3, -4, 0, 1, 3, 1, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            _c("crown_shard_c", -1, -5, -1, 2, 4, 1, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        ))
    elif reg == "leech_parasite":
        _add_children(model, "mid", (
            _n("mutated_ridge", (0, 0, 0), (
                _c("mutated_ridge_1", -1.5, -3, -2, 3, 1, 2, role="bone"),
                _c("mutated_ridge_2", -1.5, -3, 0, 3, 1, 2, role="bone"),
                _c("mutated_ridge_3", -1.5, -3, 2, 3, 1, 2, role="bone"),
                _c("mutated_fin_r", -4.5, 0, -2, 2, 1, 4, role="armor"),
                _c("mutated_fin_l", 2.5, 0, -2, 2, 1, 4, role="armor", mirror=True),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "rear", (
            _n("mutated_sucker", (0, 0, 0), (
                _c("mutated_sucker", -1.5, -3, -1, 3, 1, 3, role="glow_core", emissive=True),
            ), rank_min="MUTATED", anim_role="sac_pulse"),
        ))
        _add_cubes(model, "elite_fins", (
            _c("elite_fin_top_r", -4, -3, -1, 2, 4, 2, role="glow_core", emissive=True),
            _c("elite_fin_top_l", 2, -3, -1, 2, 4, 2, role="glow_core", emissive=True, mirror=True),
        ))
        _add_children(model, "rear", (
            _n("elite_crown_frill", (0, -2, 0), (
                _c("crown_frill_1", -0.5, -5, -3, 1, 4, 2, role="bone"),
                _c("crown_frill_2", -0.5, -6, 0, 1, 5, 2, role="bone"),
                _c("crown_frill_3", -0.5, -5, 2, 1, 4, 2, role="bone"),
            ), rank_min="ELITE", anim_role="tendril_sway"),
        ))
        _add_children(model, "mid", (
            _n("elite_dorsal_blades", (0, -2.5, 0), (
                _c("leech_dorsal_blade_r", -1, -4, -2, 1, 4, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
                _c("leech_dorsal_blade_l", 0, -4, -2, 1, 4, 2, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            ), rank_min="ELITE"),
        ))
    elif reg == "acid_spitter":
        _add_cubes(model, "mutated_stinger", (
            _c("stinger_mid", -1.5, -5, -1.5, 3, 3, 3, role="bone"),
        ))
        _add_children(model, "abdomen", (
            _n("mutated_venom_bulb", (0, -6, 5), (
                _c("mutated_venom_bulb", -2, -2, -2, 4, 4, 4, role="glow_core", emissive=True),
            ), rank_min="MUTATED", anim_role="sac_pulse"),
        ))
        _add_children(model, "abdomen", (
            _n("mutated_leg_spurs", (0, 0, 0), (
                _c("mutated_leg_spur_r", -4.5, -1, 3, 2, 1, 2, role="armor"),
                _c("mutated_leg_spur_l", 2.5, -1, 3, 2, 1, 2, role="armor", mirror=True),
            ), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_crest", (
            _c("elite_crest_2", -3, -3, -1, 1, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("elite_crest_3", 2, -3, -1, 1, 2, 2, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            _c("elite_crest_4", -1, -4, -1, 2, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        ))
        _add_children(model, "abdomen", (
            _n("elite_abdomen_plates", (0, -4.5, 4), (
                _c("abdomen_plate_1", -4.5, -1, 0, 9, 1, 3, role="bone", tags=TAG_CHITIN),
                _c("abdomen_plate_2", -4.5, -1, 3, 9, 1, 3, role="bone", tags=TAG_CHITIN),
                _c("abdomen_plate_3", -4.5, -3, 1.5, 9, 2, 2, role="bone", tags=TAG_CHITIN),
            ), rank_min="ELITE"),
            _n("elite_gem_orbs", (0, -8, 3), (
                _c("elite_gem_orb_r", -4, -1, -1, 1, 1, 1, role="glow_eye", emissive=True),
                _c("elite_gem_orb_l", 3, -1, -1, 1, 1, 1, role="glow_eye", emissive=True, mirror=True),
            ), rank_min="ELITE", anim_role="orbit_orb"),
        ))
    elif reg == "burrower":
        _add_cubes(model, "mutated_side_drills", (
            _c("side_drill_r_2", -6.5, -2.5, -4, 2, 2, 4, role="bone"),
            _c("side_drill_l_2", 4.5, -2.5, -4, 2, 2, 4, role="bone", mirror=True),
        ))
        _add_children(model, "abdomen", (
            _n("mutated_tail_spikes", (0, -2, 0), (
                _c("tail_spike_1", -1, 0, 8, 2, 2, 2, role="bone"),
                _c("tail_spike_2", -0.5, -2, 6, 1, 2, 2, role="bone"),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "body", (
            _n("mutated_carapace", (0, -3, 0), (
                _c("mut_carapace_1", -4.5, -1, -4, 9, 1, 4, role="bone", tags=TAG_CHITIN),
                _c("mut_carapace_2", -4.5, -1, 1, 9, 1, 4, role="bone", tags=TAG_CHITIN),
                _c("mut_carapace_vent", -2, -2, 2, 2, 1, 2, role="glow_core", emissive=True),
            ), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_shell", (
            _c("elite_carapace_2", -2, -2, 3, 4, 2, 4, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
        ))
        _add_children(model, "drill_head", (
            _n("elite_drill_crown", (0, -3, -3), (
                _c("drill_crown_1", -1, -4, -2, 2, 4, 2, role="bone"),
                _c("drill_crown_2", -3, -3, 0, 1, 3, 2, role="bone"),
                _c("drill_crown_3", 2, -3, 0, 1, 3, 2, role="bone", mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "body", (
            _n("elite_aura", (0, 0, 0), (
                _c("burrower_aura_n", -6, -1, -7, 12, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("burrower_aura_s", -6, -1, 6, 12, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("burrower_aura_gem", -1, -2, -7, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_b"),
        ))
    elif reg == "pyro_shroom":
        _add_cubes(model, "mutated_vents", (
            _c("mut_vent_ember", -1, -4, -1, 2, 2, 2, role="glow_core", emissive=True),
        ))
        _add_children(model, "head_seg", (
            _n("mutated_ember_horns", (0, -2.5, -2), (
                _c("mut_horn_r", -3.5, -3, -3, 1, 3, 2, role="glow_core", emissive=True),
                _c("mut_horn_l", 2.5, -3, -3, 1, 3, 2, role="glow_core", emissive=True, mirror=True),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "tail_seg", (
            _n("mutated_ash_stack", (0, -2, 0), (
                _c("ash_stack_mut", -1.5, -2, -1, 3, 3, 3, role="armor"),
            ), rank_min="MUTATED", anim_role="sac_pulse"),
        ))
        _add_cubes(model, "elite_plates", (
            _c("elite_plate_top_r", -2, -4, -1, 2, 2, 3, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("elite_plate_top_l", 0, -4, -1, 2, 2, 3, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
        ))
        _add_children(model, "head_seg", (
            _n("elite_crown", (0, -2.5, -2), (
                _c("pyro_crown_1", -1, -3, -3, 2, 3, 2, role="bone"),
                _c("pyro_crown_2", -4, -2, -1, 1, 2, 2, role="bone"),
                _c("pyro_crown_3", 3, -2, -1, 1, 2, 2, role="bone", mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "mid_seg", (
            _n("elite_flame_halo", (0, -5, 0), (
                _c("flame_halo_n", -5, -1, -5, 10, 1, 1, role="glow_core", emissive=True),
                _c("flame_halo_s", -5, -1, 4, 10, 1, 1, role="glow_core", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_a"),
        ))
    elif reg == "chitin_brute":
        _add_cubes(model, "mutated_horns", (
            _c("brute_horn_r_2", -6, -6, -1, 1, 4, 2, role="bone"),
            _c("brute_horn_l_2", 5, -6, -1, 1, 4, 2, role="bone", mirror=True),
        ))
        _add_children(model, "torso", (
            _n("mutated_chest_plate", (0, 0, 0), (
                _c("mutated_chest_plate_a", -4.5, -6, -6, 9, 3, 1, role="bone", tags=TAG_CHITIN),
                _c("mutated_chest_plate_b", -4.5, -2, -6, 9, 2, 1, role="bone", tags=TAG_CHITIN),
                _c("mutated_chest_vent", -1, -5, -6.5, 2, 2, 1, role="glow_core", emissive=True),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "forearm_r", (
            _n("mutated_knuckle_r", (0, 0, 0), (_c("mutated_knuckle_r", -3, 8, -4, 2, 2, 2, role="bone"),), rank_min="MUTATED"),
        ))
        _add_children(model, "forearm_l", (
            _n("mutated_knuckle_l", (0, 0, 0), (_c("mutated_knuckle_l", 1, 8, -4, 2, 2, 2, role="bone", mirror=True),), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_spines", (
            _c("spine_4", -1, -5, 6, 2, 5, 2, role="glow_core", emissive=True),
            _c("spine_5", -1, -4, -4, 2, 4, 2, role="glow_core", emissive=True),
        ))
        _add_children(model, "head", (
            _n("elite_crown", (0, -5, 0), (
                _c("brute_crown_1", -1, -3, -3, 2, 3, 2, role="bone"),
                _c("brute_crown_2", -4, -2, 0, 2, 2, 2, role="bone"),
                _c("brute_crown_3", 2, -2, 0, 2, 2, 2, role="bone", mirror=True),
                _c("brute_crown_gem", -1, -3, -2, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "arm_r", (
            _n("elite_shoulder_blade_r", (0, 0, 0), (
                _c("elite_shoulder_blade_r", -4, -6, -2, 1, 5, 4, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "arm_l", (
            _n("elite_shoulder_blade_l", (0, 0, 0), (
                _c("elite_shoulder_blade_l", 3, -6, -2, 1, 5, 4, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "forearm_r", (
            _n("elite_gauntlet_gem", (0, 0, 0), (_c("elite_gauntlet_gem", -2, 7, -4.5, 1, 1, 1, role="glow_core", emissive=True),), rank_min="ELITE"),
        ))
    elif reg == "mycelial_stalker":
        _add_cubes(model, "mutated_antlers", (
            _c("antler_r_2", -5, -7, 1, 1, 4, 1, role="bone"),
            _c("antler_l_2", 4, -7, 1, 1, 4, 1, role="bone", mirror=True),
        ))
        _add_children(model, "torso", (
            _n("mutated_neck_frill", (0, 0, 0), (
                _c("mutated_neck_frill_1", -3, -12, -3, 6, 1, 2, role="armor"),
                _c("mutated_neck_frill_2", -2.5, -11, -3, 5, 1, 2, role="armor"),
                _c("mutated_neck_glow", -1, -12.5, -3.2, 2, 1, 1, role="glow_core", emissive=True),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "arm_r", (
            _n("mutated_claw_spur_r", (0, 0, 0), (_c("mutated_claw_spur_r", -2.5, 8, -2, 2, 2, 3, role="bone"),), rank_min="MUTATED"),
        ))
        _add_children(model, "arm_l", (
            _n("mutated_claw_spur_l", (0, 0, 0), (_c("mutated_claw_spur_l", 0.5, 8, -2, 2, 2, 3, role="bone", mirror=True),), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_dorsal_blades", (
            _c("d_blade_c_r", -2, -5, 3, 1, 5, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("d_blade_c_l", 1, -5, 3, 1, 5, 2, role="glow_eye", emissive=True, mirror=True, tags=TAG_CRYSTAL),
        ))
        _add_children(model, "head", (
            _n("elite_hood_crown", (0, -7, 0), (
                _c("hood_crown_1", -1, -5, -3, 2, 4, 2, role="bone"),
                _c("hood_crown_2", -4, -4, -1, 1, 3, 2, role="bone"),
                _c("hood_crown_3", 3, -4, -1, 1, 3, 2, role="bone", mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "tail_tip_s", (
            _n("elite_tail_fan", (0, 0, 3), (
                _c("tail_fan_r", -2, -1, 0, 1, 2, 3, role="glow_eye", emissive=True),
                _c("tail_fan_l", 1, -1, 0, 1, 2, 3, role="glow_eye", emissive=True, mirror=True),
            ), rank_min="ELITE", anim_role="tail_chain"),
        ))
        _add_children(model, "torso", (
            _n("elite_aura", (0, -4, 0), (
                _c("stalker_aura_n", -7, -1, -7, 14, 1, 1, role="glow_core", emissive=True),
                _c("stalker_aura_s", -7, -1, 6, 14, 1, 1, role="glow_core", emissive=True),
                _c("stalker_aura_gem", -1, -2, -7, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_b"),
        ))
    elif reg == "synapse_weaver":
        _add_cubes(model, "mutated_ring", (
            _c("mut_ring_c", -8, -0.5, 0, 16, 1, 1, role="glow_core", emissive=True),
        ))
        _add_children(model, "body", (
            _n("mutated_orb", (7, -4, 4), (
                _c("mut_orb", -1, -1, -1, 2, 2, 2, role="glow_core", emissive=True),
            ), rank_min="MUTATED", anim_role="orbit_orb", anim_phase=0.6),
        ))
        _add_children(model, "robe_f", (
            _n("mutated_robe_tendril_1", (0, 0, 0), (_c("mut_robe_tendril_1", -2.5, 7, -0.5, 2, 3, 1, role="spore_sac", emissive=True),), rank_min="MUTATED", anim_role="tendril_sway"),
        ))
        _add_children(model, "robe_b", (
            _n("mutated_robe_tendril_2", (0, 0, 0), (_c("mut_robe_tendril_2", 0.5, 7, -0.5, 2, 3, 1, role="spore_sac", emissive=True),), rank_min="MUTATED", anim_role="tendril_sway"),
        ))
        _add_cubes(model, "elite_crown", (
            _c("weaver_crown_back", -1.5, -3, 2, 3, 2, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
        ))
        _add_children(model, "head", (
            _n("elite_halo_tier2", (0, -9, 0), (
                _c("halo_t2_n", -7, -1, -7, 14, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("halo_t2_s", -7, -1, 6, 14, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),
                _c("halo_t2_w", -7, -1, -6, 1, 1, 12, role="glow_eye", emissive=True),
                _c("halo_t2_gem", -1, -2, -7, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_a"),
        ))
        _add_children(model, "body", (
            _n("elite_orbs", (0, -3, 0), (
                _c("elite_orb_1", -1, -1, -1, 2, 2, 2, role="glow_eye", emissive=True),
                _c("elite_orb_2", -1, -1, -1, 2, 2, 2, role="glow_eye", emissive=True, mirror=True),
            ), rank_min="ELITE", anim_role="orbit_orb", anim_phase=2.0),
        ))
        _add_children(model, "body", (
            _n("elite_shoulder_runes", (0, 0, 0), (_c("elite_shoulder_rune", -2, -7, -3, 4, 1, 1, role="glow_core", emissive=True, tags=TAG_RUNE),), rank_min="ELITE"),
        ))
    elif reg == "spore_architect":
        _add_cubes(model, "mutated_servo", (
            _c("servo_elbow", -2, -2, 1, 1, 2, 2, role="armor"),
        ))
        _add_children(model, "torso", (
            _n("mutated_tank_pod", (0, 0, 0), (_c("mutated_tank_pod", -3, -3, 4, 6, 3, 2, role="spore_sac", emissive=True),), rank_min="MUTATED", anim_role="sac_pulse"),
        ))
        _add_children(model, "torso", (
            _n("mutated_blueprint_prism", (0, 0, 0), (_c("mutated_blueprint_prism", -1, -10, 3, 2, 2, 1, role="glow_core", emissive=True, tags=TAG_CRYSTAL),), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_satellite", (_c("elite_satellite_dish", -2, -1, -2, 4, 1, 4, role="glow_eye", emissive=True),))
        _add_children(model, "head", (
            _n("elite_crown", (0, -7, 0), (
                _c("architect_crown_1", -1, -4, -2, 2, 3, 2, role="bone"),
                _c("architect_crown_2", -3.5, -3, 0, 1, 2, 2, role="bone"),
                _c("architect_crown_3", 2.5, -3, 0, 1, 2, 2, role="bone", mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "torso", (
            _n("elite_drone_orb", (0, -6, -5), (
                _c("elite_drone_orb", -1, -1, -1, 2, 2, 2, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_orb"),
        ))
        _add_children(model, "arm_r", (
            _n("elite_arm_plate_r", (0, 0, 0), (_c("elite_arm_plate_r", -2, 0, -2.5, 4, 3, 1, role="armor"),), rank_min="ELITE"),
        ))
        _add_children(model, "arm_l", (
            _n("elite_arm_plate_l", (0, 0, 0), (_c("elite_arm_plate_l", -2, 0, -2.5, 4, 3, 1, role="armor", mirror=True),), rank_min="ELITE"),
        ))
    elif reg == "synaptic_mimic":
        _add_cubes(model, "mutated_echo_fins", (_c("m_fin_c", -2, -4, 0, 4, 1, 3, role="glow_core", emissive=True),))
        _add_children(model, "head", (
            _n("mutated_mask_spikes", (0, -7, 0), (
                _c("mask_spike_r", -4.5, -4, -1, 1, 3, 2, role="bone"),
                _c("mask_spike_l", 3.5, -4, -1, 1, 3, 2, role="bone", mirror=True),
            ), rank_min="MUTATED"),
        ))
        _add_children(model, "arm_l", (
            _n("mutated_gauntlet_spike", (0, 0, 0), (_c("mutated_gauntlet_spike", 2, 4, -3, 1, 3, 2, role="bone"),), rank_min="MUTATED"),
        ))
        _add_cubes(model, "elite_echo_halo", (
            _c("echo_halo_2", -3, -2, -3, 6, 1, 6, role="glow_eye", emissive=True),
            _c("echo_halo_3", -2, -3, -2, 4, 1, 4, role="glow_eye", emissive=True, tags=TAG_RUNE),
        ))
        _add_children(model, "head", (
            _n("elite_mask_crown", (0, -8, 0), (
                _c("mask_crown_1", -1, -4, -2, 2, 3, 2, role="bone"),
                _c("mask_crown_2", -4, -4, 0, 1, 3, 2, role="bone"),
                _c("mask_crown_3", 3, -4, 0, 1, 3, 2, role="bone", mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "arm_r", (
            _n("elite_blade_fan", (0, 0, 0), (
                _c("elite_blade_fan_1", -1.5, 5, -8, 1, 3, 3, role="glow_core", emissive=True, tags=TAG_CRYSTAL),
                _c("elite_blade_fan_2", -1.5, 5, -5, 1, 3, 3, role="bone"),
                _c("elite_blade_fan_3", -1.5, 8, -6, 1, 2, 3, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="forearm"),
        ))
    elif reg == "mycelial_chimera":
        _add_cubes(model, "mutated_chimera_horns", (
            _c("c_horn_r_2", -5, -4, 1, 1, 3, 2, role="glow_core", emissive=True),
            _c("c_horn_l_2", 4, -4, 1, 1, 3, 2, role="glow_core", emissive=True, mirror=True),
        ))
        _add_children(model, "body", (
            _n("mutated_saddle_spikes", (0, 0, 0), (
                _c("mutated_saddle_spike_r", -5, -9, -1, 1, 2, 3, role="bone"),
                _c("mutated_saddle_spike_l", 4, -9, -1, 1, 2, 3, role="bone", mirror=True),
            ), rank_min="MUTATED"),
            _n("mutated_tail_barbs", (0, 0, 0), (
                _c("mutated_tail_barb_1", -1, -5, 9, 2, 2, 3, role="armor"),
                _c("mutated_tail_barb_2", -1, -4, 12, 2, 2, 2, role="armor", mirror=True),
            ), rank_min="MUTATED", anim_role="tail_chain"),
        ))
        _add_cubes(model, "elite_chimera_crown", (
            _c("c_crown_spike_1", -1, -4, -2, 2, 3, 2, role="glow_eye", emissive=True, tags=TAG_CRYSTAL),
            _c("c_crown_spike_2", -3.5, -3, 0, 1, 2, 2, role="bone"),
        ))
        _add_children(model, "body", (
            _n("elite_gene_sockets", (0, -9, 0), (
                _c("gene_socket_r1", -5.5, -1, -4, 1, 2, 1, role="glow_core", emissive=True),
                _c("gene_socket_r2", -5.5, -1, 0, 1, 2, 1, role="glow_core", emissive=True),
                _c("gene_socket_l1", 4.5, -1, -4, 1, 2, 1, role="glow_core", emissive=True, mirror=True),
                _c("gene_socket_l2", 4.5, -1, 0, 1, 2, 1, role="glow_core", emissive=True, mirror=True),
            ), rank_min="ELITE"),
        ))
        _add_children(model, "body", (
            _n("elite_halo", (0, -10, 0), (
                _c("chimera_halo_n", -6, -1, -7, 12, 1, 1, role="glow_core", emissive=True),
                _c("chimera_halo_s", -6, -1, 6, 12, 1, 1, role="glow_core", emissive=True),
                _c("chimera_halo_gem", -1, -2, -7, 2, 1, 1, role="glow_eye", emissive=True),
            ), rank_min="ELITE", anim_role="orbit_ring_a"),
        ))
    elif reg == "goliath_carrier":
        _add_cubes(model, "elite_dorsal_crest", (_c("crest_top", -2, -8, -1, 4, 2, 3, role="bone", tags=TAG_CHITIN),))


def apply_legendary_upgrades(models: List[ModelDef]) -> None:
    from .entity_specs import ATLAS_BY_MODEL

    for model in models:
        model.tex_size = ATLAS_BY_MODEL[model.registry_id]
    by_id = {model.registry_id: model for model in models}
    for registry_id, upgrader in (
        ("spore_behemoth", _upgrade_spore_behemoth),
        ("hive_core", _upgrade_hive_core),
        ("apex_matriarch", _upgrade_apex_matriarch),
        ("goliath_carrier", _upgrade_goliath_carrier),
        ("colony_warden", _upgrade_colony_warden),
    ):
        upgrader(by_id[registry_id])
    for model in models:
        _upgrade_standard_mobs(model)
        if model.registry_id in {
            "sporeling", "leech_parasite", "acid_spitter", "burrower", "pyro_shroom",
            "chitin_brute", "mycelial_stalker", "synapse_weaver", "spore_architect",
            "synaptic_mimic", "mycelial_chimera", "goliath_carrier", "colony_warden",
            "spore_behemoth",
        }:
            _rank_layers(model)
