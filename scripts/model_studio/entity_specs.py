"""Single source of truth for model presence targets, evolution budgets and runtime contracts."""

from __future__ import annotations

from typing import Any, Dict, Iterable, List, Sequence, Tuple

from .metrics import model_metrics
from .uv_packer import ModelDef

ATLAS_BY_MODEL: Dict[str, Tuple[int, int]] = {
    "sporeling": (256, 256),
    "leech_parasite": (256, 256),
    "acid_spitter": (256, 256),
    "burrower": (256, 256),
    "pyro_shroom": (256, 256),
    "chitin_brute": (256, 256),
    "mycelial_stalker": (256, 256),
    "synapse_weaver": (256, 256),
    "goliath_carrier": (256, 256),
    "colony_warden": (256, 256),
    "spore_architect": (256, 256),
    "purifier_drone": (128, 64),
    "spore_behemoth": (512, 512),
    "hive_core": (512, 512),
    "apex_matriarch": (512, 512),
    "synaptic_mimic": (256, 256),
    "mycelial_chimera": (256, 256),
    "colony_symbiont": (256, 256),
    "spore_glider": (256, 256),
    "root_keeper": (256, 256),
}

# Runtime contract mirrored in ClientSetup.java (shadow, renderScale) and ModEntities.java (hitbox).
RENDER_SPECS: Dict[str, Dict[str, Any]] = {
    "spore_behemoth": {"render_scale": 1.45, "shadow": 1.45, "hitbox": (2.2, 4.0), "aoe_inflate": 2.2},
    "hive_core": {"render_scale": 1.70, "shadow": 1.85, "hitbox": (3.0, 4.5), "aoe_inflate": 7.2},
    "apex_matriarch": {"render_scale": 1.90, "shadow": 2.35, "hitbox": (3.6, 5.6), "aoe_inflate": 7.6},
    "goliath_carrier": {"render_scale": 1.10, "shadow": 1.05, "hitbox": (1.6, 2.8), "aoe_inflate": 0.0},
    "colony_warden": {"render_scale": 1.10, "shadow": 1.05, "hitbox": (1.5, 3.1), "aoe_inflate": 0.0},
}

RANK_MODELS: Tuple[str, ...] = (
    "sporeling",
    "leech_parasite",
    "acid_spitter",
    "burrower",
    "pyro_shroom",
    "chitin_brute",
    "mycelial_stalker",
    "synapse_weaver",
    "spore_architect",
    "synaptic_mimic",
    "mycelial_chimera",
    "goliath_carrier",
    "colony_warden",
    "spore_behemoth",
    "root_keeper",
)

NO_RANK_MODELS: Tuple[str, ...] = ("purifier_drone", "hive_core", "apex_matriarch", "colony_symbiont", "spore_glider")

# Minimum cube budgets. Base budgets only apply to the units whose silhouette grows.
BUDGETS: Dict[str, Dict[str, float]] = {
    "spore_behemoth": {
        "total_cubes": 100, "base_cubes": 80, "base_height_px": 62.0, "base_width_px": 46.0,
        "emissive_min": 20, "mutated_min": 10, "elite_min": 14,
    },
    "hive_core": {
        "total_cubes": 115, "base_cubes": 100, "base_height_px": 52.0, "base_width_px": 46.0,
        "emissive_min": 45,
    },
    "apex_matriarch": {
        "total_cubes": 130, "base_cubes": 115, "base_height_px": 68.0, "base_width_px": 64.0,
        "emissive_min": 50,
    },
    "goliath_carrier": {
        "total_cubes": 46, "base_cubes": 40, "base_height_px": 36.0, "base_width_px": 26.0,
        "emissive_min": 12, "mutated_min": 6, "elite_min": 10,
    },
    "colony_warden": {
        "total_cubes": 44, "base_cubes": 38, "base_height_px": 42.0, "base_width_px": 26.0,
        "emissive_min": 14, "mutated_min": 6, "elite_min": 10,
    },
}

STANDARD_RANK_BUDGET = {"mutated_min": 5, "elite_min": 8}

PHASE_BUDGETS: Dict[str, Dict[str, int]] = {
    "hive_core": {"phase2": 17},
    "apex_matriarch": {"phase2": 14, "phase3": 15},
}

# Signature parts that must exist after the legendary upgrade (guards the design contract).
SIGNATURE_PARTS: Dict[str, Tuple[str, ...]] = {
    "sporeling": ("cap_rib_f", "mutated_cap_growth", "elite_halo"),
    "leech_parasite": ("dorsal_frill_1", "mutated_ridge_1", "elite_crown_frill"),
    "acid_spitter": ("spinneret", "mutated_venom_bulb", "elite_gem_orbs"),
    "burrower": ("helix_fin_r", "mutated_tail_spikes", "elite_drill_crown"),
    "pyro_shroom": ("ember_grill_1", "mutated_ember_horns", "elite_flame_halo"),
    "chitin_brute": ("elbow_spur_r", "mutated_chest_plate", "elite_shoulder_blade_r"),
    "mycelial_stalker": ("hood_lappet_r", "mutated_neck_frill", "elite_hood_crown"),
    "synapse_weaver": ("veil_1", "mutated_robe_tendril_1", "elite_halo_tier2"),
    "spore_architect": ("blueprint_prism", "mutated_blueprint_prism", "elite_crown"),
    "synaptic_mimic": ("mimic_visor_shard", "mutated_mask_spikes", "elite_blade_fan"),
    "mycelial_chimera": ("saddle_stirrup_r", "mutated_tail_barbs", "elite_gene_sockets"),
    "goliath_carrier": ("howdah_deck", "mutated_howdah", "elite_brood_beacon"),
    "colony_warden": ("colony_standard", "mutated_banner_strips", "elite_shield_gem"),
    "spore_behemoth": ("sternum_maw", "mutated_mortar_pods", "elite_siege_crown"),
    "hive_core": ("crown_chandelier", "phase2_crystal_wing_r", "phase2_bastion_ring"),
    "apex_matriarch": ("sternum_reliquary", "p2_bastion_ring", "p3_halo"),
    "purifier_drone": ("sensor_mast", "landing_skid_r"),
    "colony_symbiont": ("caste_courier_pack", "caste_sentinel_shield", "antenna_crest"),
    "spore_glider": ("glider_crest", "fore_membrane_r", "pollen_tail_r"),
    "root_keeper": ("root_heart_core", "mutated_root_spines", "elite_root_halo"),
}


def _missing_parts(model: ModelDef) -> List[str]:
    from .metrics import iter_cubes, iter_parts

    names = {part.name for part in iter_parts(model.parts)}
    names.update(cube.name for cube in iter_cubes(model.parts))
    return [name for name in SIGNATURE_PARTS.get(model.registry_id, ()) if name not in names]


def validate_models(models: Sequence[ModelDef]) -> Dict[str, Any]:
    rows: List[Dict[str, Any]] = []
    errors: List[str] = []
    for model in models:
        metrics = model_metrics(model)
        row: Dict[str, Any] = {"registry_id": model.registry_id, **metrics}
        rows.append(row)
        expected_atlas = ATLAS_BY_MODEL.get(model.registry_id)
        if expected_atlas is None:
            errors.append(f"{model.registry_id}: no ATLAS_BY_MODEL entry")
        elif tuple(model.tex_size) != expected_atlas:
            errors.append(f"{model.registry_id}: atlas {model.tex_size} != {expected_atlas}")
        budget = BUDGETS.get(model.registry_id, {})
        if "total_cubes" in budget and metrics["cube_count"] < budget["total_cubes"]:
            errors.append(f"{model.registry_id}: cubes {metrics['cube_count']} < {budget['total_cubes']}")
        if "base_cubes" in budget and metrics["base_cube_count"] < budget["base_cubes"]:
            errors.append(f"{model.registry_id}: base cubes {metrics['base_cube_count']} < {budget['base_cubes']}")
        if "base_height_px" in budget and metrics["base_height_px"] < budget["base_height_px"]:
            errors.append(f"{model.registry_id}: base height {metrics['base_height_px']} < {budget['base_height_px']}")
        if "base_width_px" in budget and metrics["base_width_px"] < budget["base_width_px"]:
            errors.append(f"{model.registry_id}: base width {metrics['base_width_px']} < {budget['base_width_px']}")
        if "emissive_min" in budget and metrics["emissive_count"] < budget["emissive_min"]:
            errors.append(f"{model.registry_id}: emissive {metrics['emissive_count']} < {budget['emissive_min']}")
        if model.registry_id in RANK_MODELS:
            rank_budget = {**STANDARD_RANK_BUDGET, **budget}
            if metrics["mutated_cube_count"] < rank_budget["mutated_min"]:
                errors.append(
                    f"{model.registry_id}: mutated layer {metrics['mutated_cube_count']} < {rank_budget['mutated_min']}"
                )
            if metrics["elite_cube_count"] < rank_budget["elite_min"]:
                errors.append(
                    f"{model.registry_id}: elite layer {metrics['elite_cube_count']} < {rank_budget['elite_min']}"
                )
            if metrics["elite_cube_count"] <= metrics["mutated_cube_count"] and model.registry_id in (
                "spore_behemoth", "goliath_carrier", "colony_warden"
            ):
                errors.append(
                    f"{model.registry_id}: elite layer must exceed mutated "
                    f"({metrics['elite_cube_count']} <= {metrics['mutated_cube_count']})"
                )
        phase_budget = PHASE_BUDGETS.get(model.registry_id, {})
        for phase_key, minimum in phase_budget.items():
            key = f"{phase_key}_cube_count"
            if metrics[key] < minimum:
                errors.append(f"{model.registry_id}: {phase_key} layer {metrics[key]} < {minimum}")
        usage = metrics["atlas_usage"]
        if usage["height_fraction"] > 0.90:
            errors.append(f"{model.registry_id}: atlas height usage {usage['height_fraction']:.2%} > 90%")
        if usage["area_fraction"] > 0.85:
            errors.append(f"{model.registry_id}: atlas area usage {usage['area_fraction']:.2%} > 85%")
        missing = _missing_parts(model)
        if missing:
            errors.append(f"{model.registry_id}: missing signature parts {missing}")
    return {"ok": not errors, "errors": errors, "rows": rows}
