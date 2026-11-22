"""Unified CLI for Mycelial Mind procedural 3D model studio."""

from __future__ import annotations

import argparse
import json
import os
import sys

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.model_studio.bbmodel_exporter import export_bbmodel_dict, export_bedrock_geo_dict
from scripts.model_studio.block_item_3d import emit_block_and_item_3d_models
from scripts.model_studio.entity_specs import validate_models
from scripts.model_studio.java_emitter import emit_java_model
from scripts.model_studio.metrics import model_metrics
from scripts.model_studio.model_defs import build_all_models
from scripts.model_studio.uv_packer import pack_model_uvs

JAVA_MODEL_DIR = os.path.join(REPO_ROOT, "src", "main", "java", "com", "alex", "mycelialmind", "client", "model")
MANIFEST_PATH = os.path.join(os.path.dirname(__file__), "exports", "uv_manifest.json")
BBMODEL_DIR = os.path.join(os.path.dirname(__file__), "exports", "blockbench")


def _print_metrics(models) -> None:
    print("registry_id          cubes base mut elite p2 p3  height base_width  atlas")
    for model in models:
        metrics = model_metrics(model)
        print(
            f"{model.registry_id:<20} {metrics['cube_count']:>5} {metrics['base_cube_count']:>4} "
            f"{metrics['mutated_cube_count']:>3} {metrics['elite_cube_count']:>5} "
            f"{metrics['phase2_cube_count']:>2} {metrics['phase3_cube_count']:>2} "
            f"{metrics['base_height_px']:>7} {metrics['base_width_px']:>10}  {model.tex_size[0]}x{model.tex_size[1]}"
        )


def build_all(check_only: bool = False) -> None:
    os.makedirs(JAVA_MODEL_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(MANIFEST_PATH), exist_ok=True)
    models = build_all_models()
    report = validate_models(models)
    if not report["ok"]:
        for error in report["errors"][:20]:
            print(f"MODEL CONTRACT ERROR: {error}", file=sys.stderr)
        raise SystemExit(f"Model contract validation failed with {len(report['errors'])} error(s)")
    _print_metrics(models)
    manifest = {}

    for m in models:
        cubes_meta = pack_model_uvs(m)
        java_src = emit_java_model(m)
        manifest[m.registry_id] = {
            "class_name": m.class_name,
            "tex_size": list(m.tex_size),
            "palettes": m.palettes,
            "metrics": model_metrics(m),
            "cubes": cubes_meta,
        }
        if not check_only:
            out_java = os.path.join(JAVA_MODEL_DIR, f"{m.class_name}.java")
            with open(out_java, "w", encoding="utf-8") as f:
                f.write(java_src)
            os.makedirs(BBMODEL_DIR, exist_ok=True)
            with open(os.path.join(BBMODEL_DIR, f"{m.registry_id}.bbmodel"), "w", encoding="utf-8") as f:
                json.dump(export_bbmodel_dict(m), f, indent=2)
            with open(os.path.join(BBMODEL_DIR, f"{m.registry_id}.geo.json"), "w", encoding="utf-8") as f:
                json.dump(export_bedrock_geo_dict(m), f, indent=2)

    num_blocks, num_items = emit_block_and_item_3d_models(check_only=check_only)
    if not check_only:
        with open(MANIFEST_PATH, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2)
        print(f"Emitted {len(models)} EntityModels, {num_blocks} 3D blocks, {num_items} 3D items, and UV manifest: {MANIFEST_PATH}")
    else:
        print(f"Checked {len(models)} EntityModels, {num_blocks} 3D blocks, {num_items} 3D items: 0 UV overlaps, all bounds valid, evolution budgets met.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Mycelial Mind Procedural 3D Model Studio")
    parser.add_argument("--all", action="store_true", help="Emit all 17 Java EntityModels + uv_manifest.json")
    parser.add_argument("--check", action="store_true", help="Validate UV packing, budgets and Java emission")
    args = parser.parse_args()

    if args.all:
        build_all(check_only=False)
    elif args.check:
        build_all(check_only=True)


if __name__ == "__main__":
    main()
