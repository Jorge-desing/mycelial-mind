"""Blockbench (.bbmodel) and Bedrock/AzureLib/GeckoLib (.geo.json) exporters for Model Studio."""

from __future__ import annotations

import math
import uuid
from typing import Any, Dict, List, Tuple

from .uv_packer import ModelDef, PartNode


def _rad_to_deg(rad: float) -> float:
    return round(math.degrees(rad), 3)


def _part_uuid(model_id: str, name: str) -> str:
    return str(uuid.uuid5(uuid.NAMESPACE_DNS, f"mycelialmind.{model_id}.{name}"))


def export_bbmodel_dict(model: ModelDef) -> Dict[str, Any]:
    """Convert a ModelDef into a valid Blockbench .bbmodel JSON structure."""
    tex_w, tex_h = model.tex_size
    elements: List[Dict[str, Any]] = []
    outliner: List[Dict[str, Any]] = []

    def walk(parts: Tuple[PartNode, ...], parent_pivot: Tuple[float, float, float]) -> List[Dict[str, Any]]:
        nodes: List[Dict[str, Any]] = []
        for p in parts:
            abs_pivot = (
                parent_pivot[0] + p.pivot[0],
                24.0 - (parent_pivot[1] + p.pivot[1]),
                parent_pivot[2] + p.pivot[2],
            )
            children_entries: List[Any] = []
            for c in p.cubes:
                elem_id = _part_uuid(model.registry_id, f"{p.name}.{c.name}")
                ox, oy, oz = c.origin
                w, h, d = c.size
                u, v = c.uv
                x0 = abs_pivot[0] + ox
                y0 = abs_pivot[1] - oy - h
                z0 = abs_pivot[2] + oz
                elements.append({
                    "name": c.name,
                    "box_uv": True,
                    "rescale": False,
                    "locked": False,
                    "from": [round(x0, 3), round(y0, 3), round(z0, 3)],
                    "to": [round(x0 + w, 3), round(y0 + h, 3), round(z0 + d, 3)],
                    "inflate": c.inflation,
                    "mirror_uv": bool(c.mirror),
                    "uv_offset": [int(u), int(v)],
                    "origin": [round(abs_pivot[0], 3), round(abs_pivot[1], 3), round(abs_pivot[2], 3)],
                    "uuid": elem_id,
                })
                children_entries.append(elem_id)

            sub_groups = walk(p.children, (parent_pivot[0] + p.pivot[0], parent_pivot[1] + p.pivot[1], parent_pivot[2] + p.pivot[2]))
            children_entries.extend(sub_groups)
            nodes.append({
                "name": p.name,
                "origin": [round(abs_pivot[0], 3), round(abs_pivot[1], 3), round(abs_pivot[2], 3)],
                "rotation": [_rad_to_deg(-p.rotation[0]), _rad_to_deg(-p.rotation[1]), _rad_to_deg(p.rotation[2])],
                "uuid": _part_uuid(model.registry_id, p.name),
                "export": True,
                "isOpen": True,
                "visibility": True,
                "children": children_entries,
            })
        return nodes

    outliner = walk(model.parts, (0.0, 0.0, 0.0))

    # Build Blockbench keyframe preview animations from PartNode anim_roles
    animators: Dict[str, Any] = {}

    def collect_animators(parts: Tuple[PartNode, ...]) -> None:
        for p in parts:
            uid = _part_uuid(model.registry_id, p.name)
            if p.anim_role in ("breath_chest", "sac_pulse", "float_bob", "tendril_sway", "orbit_orb"):
                animators[uid] = {
                    "name": p.name,
                    "type": "bone",
                    "keyframes": [
                        {"channel": "rotation", "data_points": [{"x": "0", "y": "0", "z": "0"}], "time": 0.0, "interpolation": "smooth", "uuid": _part_uuid(model.registry_id, f"{p.name}.kf0")},
                        {"channel": "rotation", "data_points": [{"x": "4", "y": "0", "z": "2"}], "time": 1.0, "interpolation": "smooth", "uuid": _part_uuid(model.registry_id, f"{p.name}.kf1")},
                        {"channel": "rotation", "data_points": [{"x": "0", "y": "0", "z": "0"}], "time": 2.0, "interpolation": "smooth", "uuid": _part_uuid(model.registry_id, f"{p.name}.kf2")},
                    ],
                }
            collect_animators(p.children)

    collect_animators(model.parts)

    return {
        "meta": {
            "format_version": "4.10",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": model.registry_id,
        "model_identifier": f"mycelialmind:{model.registry_id}",
        "resolution": {"width": int(tex_w), "height": int(tex_h)},
        "elements": elements,
        "outliner": outliner,
        "textures": [
            {
                "name": f"{model.registry_id}.png",
                "folder": "entity",
                "namespace": "mycelialmind",
                "id": "0",
                "particle": True,
                "visible": True,
                "uv_width": int(tex_w),
                "uv_height": int(tex_h),
                "relative_path": f"../../../../src/main/resources/assets/mycelialmind/textures/entity/{model.registry_id}.png",
            }
        ],
        "animations": [
            {
                "uuid": _part_uuid(model.registry_id, "anim.idle"),
                "name": "idle",
                "loop": "loop",
                "length": 2.0,
                "snapping": 24,
                "animators": animators,
            }
        ],
    }


def export_bedrock_geo_dict(model: ModelDef) -> Dict[str, Any]:
    """Convert a ModelDef into a Bedrock/AzureLib/GeckoLib .geo.json structure."""
    tex_w, tex_h = model.tex_size
    bones: List[Dict[str, Any]] = []

    def walk(parts: Tuple[PartNode, ...], parent_name: str | None, parent_pivot: Tuple[float, float, float]) -> None:
        for p in parts:
            abs_pivot = (
                parent_pivot[0] + p.pivot[0],
                24.0 - (parent_pivot[1] + p.pivot[1]),
                parent_pivot[2] + p.pivot[2],
            )
            cubes: List[Dict[str, Any]] = []
            for c in p.cubes:
                ox, oy, oz = c.origin
                w, h, d = c.size
                u, v = c.uv
                cubes.append({
                    "origin": [round(abs_pivot[0] + ox, 3), round(abs_pivot[1] - oy - h, 3), round(abs_pivot[2] + oz, 3)],
                    "size": [int(w), int(h), int(d)],
                    "uv": [int(u), int(v)],
                    "inflate": c.inflation,
                    "mirror": bool(c.mirror),
                })
            bone: Dict[str, Any] = {
                "name": p.name,
                "pivot": [round(abs_pivot[0], 3), round(abs_pivot[1], 3), round(abs_pivot[2], 3)],
                "rotation": [_rad_to_deg(-p.rotation[0]), _rad_to_deg(-p.rotation[1]), _rad_to_deg(p.rotation[2])],
                "cubes": cubes,
            }
            if parent_name is not None:
                bone["parent"] = parent_name
            bones.append(bone)
            walk(p.children, p.name, (parent_pivot[0] + p.pivot[0], parent_pivot[1] + p.pivot[1], parent_pivot[2] + p.pivot[2]))

    walk(model.parts, None, (0.0, 0.0, 0.0))
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.mycelialmind.{model.registry_id}",
                    "texture_width": int(tex_w),
                    "texture_height": int(tex_h),
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 3,
                    "visible_bounds_offset": [0, 1.25, 0],
                },
                "bones": bones,
            }
        ],
    }
