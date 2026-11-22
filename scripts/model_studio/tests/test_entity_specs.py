"""Contract tests: atlas sizes, evolution budgets, silhouettes and Java runtime sync."""

from __future__ import annotations

import os
import re
import sys
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.model_studio.entity_specs import (
    ATLAS_BY_MODEL,
    BUDGETS,
    PHASE_BUDGETS,
    RANK_MODELS,
    RENDER_SPECS,
    validate_models,
)
from scripts.model_studio.java_emitter import ANIM_ROLES, emit_java_model
from scripts.model_studio.metrics import (
    atlas_usage,
    iter_parts,
    layer_part_count,
    model_metrics,
    rest_pose_bounds,
    silhouette_fingerprint,
)
from scripts.model_studio.model_defs import build_all_models
from scripts.model_studio.uv_packer import pack_model_uvs


def _read(rel_path: str) -> str:
    with open(os.path.join(REPO_ROOT, rel_path), encoding="utf-8") as handle:
        return handle.read()


def _client_renderers() -> dict[str, dict[str, float]]:
    text = _read("src/main/java/com/alex/mycelialmind/client/ClientSetup.java")
    pattern = re.compile(
        r'buildMobRenderer\(ctx,\s*new (\w+)Model<>\(ctx\.bakeLayer\(\1Model\.LAYER_LOCATION\)\),'
        r'\s*([\d.]+)F,\s*"([a-z_]+)"(?:,\s*(?:true|false),\s*([\d.]+)F)?\)',
        re.DOTALL,
    )
    out: dict[str, dict[str, float]] = {}
    for match in pattern.finditer(text):
        registry_id = match.group(3)
        shadow = float(match.group(2))
        render_scale = float(match.group(4)) if match.group(4) else 1.0
        out[registry_id] = {"shadow": shadow, "render_scale": render_scale}
    return out


def _entity_hitboxes() -> dict[str, tuple[float, float]]:
    text = _read("src/main/java/com/alex/mycelialmind/registry/ModEntities.java")
    pattern = re.compile(
        r'ENTITY_TYPES\.register\("([a-z_]+)",[\s\S]*?\.sized\(([\d.]+)F,\s*([\d.]+)F\)'
    )
    return {m.group(1): (float(m.group(2)), float(m.group(3))) for m in pattern.finditer(text)}


class TestEntitySpecs(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.models = build_all_models()
        cls.by_id = {m.registry_id: m for m in cls.models}

    def test_atlas_contract_matches_manifest(self) -> None:
        self.assertEqual(set(ATLAS_BY_MODEL), set(self.by_id))
        for model in self.models:
            self.assertEqual(tuple(model.tex_size), ATLAS_BY_MODEL[model.registry_id], model.registry_id)

    def test_geometry_budgets_and_complete_evolution(self) -> None:
        report = validate_models(self.models)
        self.assertTrue(report["ok"], "\n".join(report["errors"]))
        for registry_id in RANK_MODELS:
            model = self.by_id[registry_id]
            metrics = model_metrics(model)
            self.assertGreaterEqual(metrics["mutated_cube_count"], 5, registry_id)
            self.assertGreaterEqual(metrics["elite_cube_count"], 8, registry_id)
            self.assertGreaterEqual(layer_part_count(model, rank="MUTATED"), 2, registry_id)
            self.assertGreaterEqual(layer_part_count(model, rank="ELITE"), 2, registry_id)
            for rank in ("MUTATED", "ELITE"):
                emissive = [
                    cube
                    for part in iter_parts(model.parts)
                    if part.rank_min == rank
                    for cube in part.cubes
                    if cube.emissive
                ]
                self.assertTrue(emissive, f"{registry_id} {rank} layer has no emissive accent")
        for registry_id, budget in BUDGETS.items():
            if "mutated_min" in budget:
                self.assertGreaterEqual(
                    model_metrics(self.by_id[registry_id])["mutated_cube_count"], budget["mutated_min"]
                )
            if "elite_min" in budget:
                self.assertGreaterEqual(
                    model_metrics(self.by_id[registry_id])["elite_cube_count"], budget["elite_min"]
                )
        for registry_id, phases in PHASE_BUDGETS.items():
            metrics = model_metrics(self.by_id[registry_id])
            for phase_key, minimum in phases.items():
                self.assertGreaterEqual(metrics[f"{phase_key}_cube_count"], minimum, registry_id)

    def test_evolution_layers_stay_out_of_base_silhouette(self) -> None:
        for registry_id in RANK_MODELS:
            model = self.by_id[registry_id]
            base = rest_pose_bounds(model, include_conditional=False)
            full = rest_pose_bounds(model, include_conditional=True)
            self.assertLessEqual(base.height, full.height + 0.01, registry_id)
            self.assertNotEqual(model_metrics(model)["base_cube_count"], model_metrics(model)["cube_count"], registry_id)

    def test_silhouettes_are_unique(self) -> None:
        fingerprints: dict[tuple, str] = {}
        for model in self.models:
            fingerprint = silhouette_fingerprint(model)
            self.assertNotIn(fingerprint, fingerprints, f"{model.registry_id} clones {fingerprints.get(fingerprint)}")
            fingerprints[fingerprint] = model.registry_id

    def test_runtime_render_scale_shadow_and_hitbox_sync(self) -> None:
        renderers = _client_renderers()
        hitboxes = _entity_hitboxes()
        for registry_id, spec in RENDER_SPECS.items():
            self.assertIn(registry_id, renderers, registry_id)
            self.assertAlmostEqual(renderers[registry_id]["shadow"], spec["shadow"], places=3, msg=registry_id)
            self.assertAlmostEqual(renderers[registry_id]["render_scale"], spec["render_scale"], places=3, msg=registry_id)
            self.assertIn(registry_id, hitboxes, registry_id)
            self.assertAlmostEqual(hitboxes[registry_id][0], spec["hitbox"][0], places=3, msg=registry_id)
            self.assertAlmostEqual(hitboxes[registry_id][1], spec["hitbox"][1], places=3, msg=registry_id)

    def test_boss_aoe_inflate_preserved(self) -> None:
        classes = {
            "spore_behemoth": "src/main/java/com/alex/mycelialmind/entity/SporeBehemothEntity.java",
            "hive_core": "src/main/java/com/alex/mycelialmind/entity/HiveCoreBossEntity.java",
            "apex_matriarch": "src/main/java/com/alex/mycelialmind/entity/ApexMatriarchEntity.java",
        }
        for registry_id, path in classes.items():
            values = [float(v) for v in re.findall(r"inflate\(([\d.]+)D\)", _read(path))]
            self.assertTrue(values, registry_id)
            self.assertIn(RENDER_SPECS[registry_id]["aoe_inflate"], values, registry_id)

    def test_anim_roles_are_known_and_piston_pump_emits(self) -> None:
        for model in self.models:
            for part in iter_parts(model.parts):
                if part.anim_role is not None:
                    self.assertIn(part.anim_role, ANIM_ROLES, f"{model.registry_id}.{part.name}")
        behemoth = self.by_id["spore_behemoth"]
        pack_model_uvs(behemoth)
        java = emit_java_model(behemoth)
        self.assertIn("attackSwing * 0.22F", java)
        self.assertIn("this.pressurePistons", java)

    def test_atlas_headroom(self) -> None:
        for model in self.models:
            usage = atlas_usage(model)
            self.assertLessEqual(usage["height_fraction"], 0.90, model.registry_id)
            self.assertLessEqual(usage["area_fraction"], 0.85, model.registry_id)


if __name__ == "__main__":
    unittest.main()
