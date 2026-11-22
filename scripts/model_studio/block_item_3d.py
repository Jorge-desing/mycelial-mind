"""Masterwork 3D multi-element Minecraft JSON model generator for blocks and items."""

from __future__ import annotations

import json
import os
from typing import Any, Dict, List, Optional, Tuple

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BLOCK_MODEL_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "models", "block")
ITEM_MODEL_DIR = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "models", "item")

Vec3 = Tuple[float, float, float]
TileRef = Tuple[int, int]  # (col, row) in 4x4 64x64 atlas


def _tile_uv(tile: TileRef, face_w: float = 16.0, face_h: float = 16.0) -> List[float]:
    col, row = tile
    du = min(3.70, max(0.25, abs(face_w) * 0.25))
    dv = min(3.70, max(0.25, abs(face_h) * 0.25))
    cu = col * 4.0 + 2.0
    cv = row * 4.0 + 2.0
    return [
        round(cu - du * 0.5, 3),
        round(cv - dv * 0.5, 3),
        round(cu + du * 0.5, 3),
        round(cv + dv * 0.5, 3),
    ]


def _tbox(
    frm: Vec3,
    to: Vec3,
    tile: TileRef | Dict[str, TileRef],
    tex: str = "#atlas",
    rotation: Optional[Dict[str, Any]] = None,
    shade: bool = True,
) -> Dict[str, Any]:
    x1, y1, z1 = frm
    x2, y2, z2 = to
    dx = abs(x2 - x1)
    dy = abs(y2 - y1)
    dz = abs(z2 - z1)
    if isinstance(tile, tuple):
        face_tiles = {f: tile for f in ("north", "south", "east", "west", "up", "down")}
    else:
        default = tile.get("all", next(iter(tile.values())))
        face_tiles = {f: tile.get(f, default) for f in ("north", "south", "east", "west", "up", "down")}

    face_dims = {
        "north": (dx, dy),
        "south": (dx, dy),
        "east": (dz, dy),
        "west": (dz, dy),
        "up": (dx, dz),
        "down": (dx, dz),
    }
    faces = {
        face: {"uv": _tile_uv(face_tiles[face], *face_dims[face]), "texture": tex}
        for face in ("north", "south", "east", "west", "up", "down")
    }
    elem: Dict[str, Any] = {
        "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
        "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
        "faces": faces,
    }
    if rotation is not None:
        elem["rotation"] = rotation
    if not shade:
        elem["shade"] = False
    return elem


def _flow_box(
    frm: Vec3,
    to: Vec3,
    tex: str = "#flow",
    rotation: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    x1, y1, z1 = frm
    x2, y2, z2 = to
    faces = {
        face: {"uv": [0.5, 0.5, 15.5, 15.5], "texture": tex}
        for face in ("north", "south", "east", "west", "up", "down")
    }
    elem: Dict[str, Any] = {
        "from": [round(x1, 3), round(y1, 3), round(z1, 3)],
        "to": [round(x2, 3), round(y2, 3), round(z2, 3)],
        "faces": faces,
        "shade": False,
    }
    if rotation is not None:
        elem["rotation"] = rotation
    return elem


def _rot(origin: Vec3, axis: str, angle: float) -> Dict[str, Any]:
    return {"origin": list(origin), "axis": axis, "angle": angle}


# Tile semantic aliases per atlas (col, row)
# atlas_blocks_hive
H_OBS = (0, 0)
H_VEIN = (1, 0)
H_BONE = (2, 0)
H_AMETHYST = (3, 0)
H_INDIGO_RUNE = (0, 1)
H_CHARTREUSE = (1, 1)
H_VENOM = (2, 1)
H_EMBER = (3, 1)
H_VENT = (0, 2)
H_VIOLET_SCALE = (1, 2)
H_BRASS_COIL = (2, 2)
H_CYAN_CRYSTAL = (3, 2)
H_INDIGO_PLATE = (0, 3)
H_IVORY_SCALE = (1, 3)
H_BRASS_RUNE = (2, 3)
H_GOLD_CRYSTAL = (3, 3)

# atlas_blocks_purification
P_PLATINUM = (0, 0)
P_BRUSHED = (1, 0)
P_AQUA = (2, 0)
P_CYAN_VEIN = (3, 0)
P_BRASS_COIL = (0, 1)
P_DARK_VENT = (1, 1)
P_PLAT_RUNE = (2, 1)
P_AQUA_COIL = (3, 1)
P_INDIGO = (0, 2)
P_BONE = (1, 2)
P_CYAN_GRATE = (2, 2)
P_GOLD = (3, 2)
P_PLAT_COIL = (0, 3)
P_AQUA_RUNE = (1, 3)
P_OBS = (2, 3)
P_IVORY = (3, 3)

# atlas_blocks_altars
A_OBS_RUNE = (0, 0)
A_AMETHYST_RUNE = (1, 0)
A_GOLD = (2, 0)
A_GOLD_RUNE = (3, 0)
A_BRASS_COIL = (0, 1)
A_VIOLET_CRYSTAL = (1, 1)
A_EMBER_CRYSTAL = (2, 1)
A_INDIGO = (3, 1)
A_BONE_SCALE = (0, 2)
A_CYAN_CRYSTAL = (1, 2)
A_OBS = (2, 2)
A_CHARTREUSE_VEIN = (3, 2)
A_PLATINUM = (0, 3)
A_IVORY_CRYSTAL = (1, 3)
A_AMETHYST_CRYSTAL = (2, 3)
A_BRASS = (3, 3)

# atlas_items_weapons
W_OBS = (0, 0)
W_BONE = (1, 0)
W_CYAN = (2, 0)
W_BRUSHED = (3, 0)
W_GOLD_COIL = (0, 1)
W_EMBER = (1, 1)
W_AQUA = (2, 1)
W_INDIGO_COIL = (3, 1)
W_VENT = (0, 2)
W_AMETHYST_VEIN = (1, 2)
W_CHARTREUSE = (2, 2)
W_BRASS = (3, 2)
W_VIOLET = (0, 3)
W_PLAT = (1, 3)
W_IVORY = (2, 3)
W_DARK_VENT = (3, 3)

# atlas_items_biotech
B_PLAT = (0, 0)
B_GLASS_CYAN = (1, 0)
B_VENOM = (2, 0)
B_CHARTREUSE = (3, 0)
B_AQUA = (0, 1)
B_EMBER = (1, 1)
B_BRASS_COIL = (2, 1)
B_OBS_COIL = (3, 1)
B_AMETHYST = (0, 2)
B_IVORY = (1, 2)
B_VENT = (2, 2)
B_VIOLET = (3, 2)
B_INDIGO = (0, 3)
B_GOLD_RUNE = (1, 3)
B_BONE = (2, 3)
B_CYAN_COIL = (3, 3)

# atlas_items_armor_relics
R_OBS_SCALE = (0, 0)
R_INDIGO = (1, 0)
R_BONE_SCALE = (2, 0)
R_GOLD_RUNE = (3, 0)
R_AMETHYST = (0, 1)
R_VIOLET_VEIN = (1, 1)
R_CHARTREUSE = (2, 1)
R_BRASS = (3, 1)
R_CYAN = (0, 2)
R_EMBER = (1, 2)
R_PLAT = (2, 2)
R_IVORY = (3, 2)
R_OBS_RUNE = (0, 3)
R_GOLD_CRYSTAL = (1, 3)
R_VENOM = (2, 3)
R_IVORY_SCALE = (3, 3)


def _atlas_textures(atlas_name: str, particle: str, with_flow: bool = False) -> Dict[str, str]:
    tex = {
        "atlas": f"mycelialmind:model_atlas/{atlas_name}",
        "particle": particle,
    }
    if with_flow:
        tex["flow"] = "mycelialmind:model_atlas/crystal_pulse_flow"
    return tex


# ---------------------------------------------------------------------------
# 17 Masterwork 3D Block Models (+ chitin_pillar_horizontal)
# ---------------------------------------------------------------------------

def build_3d_block_models() -> Dict[str, Dict[str, Any]]:
    models: Dict[str, Dict[str, Any]] = {}

    # 1. mycelial_node: low organic root-mound with asymmetrical synaptic ganglion & 3 flush floor roots
    models["mycelial_node"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/mycelial_node"),
        "elements": [
            _tbox((2, 0, 2), (14, 2.5, 14), _faces(H_OBS, up=H_INDIGO_PLATE)),
            _tbox((4, 2.5, 4), (12, 5.5, 12), _faces(H_INDIGO_PLATE, north=H_INDIGO_RUNE, south=H_INDIGO_RUNE)),
            _tbox((5, 5.5, 5), (11, 10.5, 11), _faces(H_VIOLET_SCALE, up=H_AMETHYST)),
            _tbox((6, 6.5, 4.3), (10, 9.5, 5.0), H_AMETHYST, shade=False),
            _tbox((6, 6.5, 11.0), (10, 9.5, 11.7), H_AMETHYST, shade=False),
            # 3 asymmetrical floor roots flush to base faces
            _tbox((0.5, 0, 6.5), (2.0, 2.5, 9.5), H_VEIN),
            _tbox((14.0, 0, 4.0), (15.5, 2.0, 7.0), H_VEIN),
            _tbox((6.5, 0, 14.0), (9.5, 2.2, 15.5), H_VEIN),
        ],
    }

    # 2. incubator_node: squat horizontal cocoon vat with 3 notched cradle straps & split amber brood sac
    models["incubator_node"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/incubator_node"),
        "elements": [
            _tbox((1, 0, 2), (15, 3, 14), _faces(H_OBS, up=H_INDIGO_PLATE)),
            _tbox((2.2, 3, 3.2), (3.8, 11.5, 12.8), _faces(H_BONE, up=H_BRASS_RUNE)),
            _tbox((3.8, 3, 3.5), (7.2, 10.5, 12.5), H_EMBER, shade=False),
            _tbox((7.2, 3, 3.2), (8.8, 11.5, 12.8), _faces(H_BONE, up=H_BRASS_RUNE)),
            _tbox((8.8, 3, 3.5), (12.2, 10.5, 12.5), H_EMBER, shade=False),
            _tbox((12.2, 3, 3.2), (13.8, 11.5, 12.8), _faces(H_BONE, up=H_BRASS_RUNE)),
            _tbox((3.6, 11.5, 5.5), (12.4, 12.8, 10.5), _faces(H_INDIGO_PLATE, up=H_BRASS_RUNE, north=H_VENT, south=H_VENT)),
        ],
    }

    # 3. sentinel_node: tall slender obsidian watch-monolith with flush cyan slit-eye band
    models["sentinel_node"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/sentinel_node"),
        "elements": [
            _tbox((3, 0, 3), (13, 2.5, 13), _faces(H_OBS, up=H_INDIGO_RUNE)),
            _tbox((5, 2.5, 5), (11, 10.5, 11), _faces(H_INDIGO_PLATE, north=H_INDIGO_RUNE, south=H_INDIGO_RUNE)),
            _tbox((4.8, 10.5, 4.8), (11.2, 12.2, 11.2), H_CYAN_CRYSTAL, shade=False),
            _tbox((4.5, 12.2, 4.5), (11.5, 13.4, 11.5), _faces(H_OBS, up=H_BRASS_RUNE)),
            _tbox((5, 13.4, 5), (11, 14.8, 11), H_INDIGO_PLATE),
            _tbox((5.5, 14.8, 5.5), (10.5, 16, 10.5), _faces(H_OBS, up=H_CYAN_CRYSTAL)),
        ],
    }

    # 4. symbiotic_node: split-arch tuning fork with notched brass clamps & bio-resonator bridges
    models["symbiotic_node"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/symbiotic_node"),
        "elements": [
            _tbox((1.5, 0, 2.5), (14.5, 2.5, 13.5), _faces(H_OBS, up=H_INDIGO_RUNE)),
            _tbox((3.5, 2.5, 4.5), (12.5, 3.8, 11.5), H_INDIGO_RUNE),
            # notched left & right arms split cleanly at brass clamp height (6.5..8.2)
            _tbox((2.5, 3.8, 5.5), (4.8, 6.5, 10.5), H_BONE),
            _tbox((2.0, 6.5, 5.0), (5.2, 8.2, 11.0), _faces(H_BRASS_COIL, east=H_BRASS_RUNE, west=H_BRASS_RUNE)),
            _tbox((2.5, 8.2, 5.5), (4.8, 12.5, 10.5), H_BONE),
            _tbox((2.8, 12.5, 6.0), (4.6, 14.0, 10.0), H_BRASS_RUNE),
            _tbox((11.2, 3.8, 5.5), (13.5, 6.5, 10.5), H_BONE),
            _tbox((10.8, 6.5, 5.0), (14.0, 8.2, 11.0), _faces(H_BRASS_COIL, east=H_BRASS_RUNE, west=H_BRASS_RUNE)),
            _tbox((11.2, 8.2, 5.5), (13.5, 12.5, 10.5), H_BONE),
            _tbox((11.4, 12.5, 6.0), (13.2, 14.0, 10.0), H_BRASS_RUNE),
            # central cradle + resonator bridges + emerald heart
            _tbox((5.2, 3.8, 6.0), (10.8, 5.2, 10.0), _faces(H_BRASS_COIL, up=H_VENOM)),
            _tbox((5.8, 5.2, 5.8), (10.2, 10.5, 10.2), H_VENOM, shade=False),
            _tbox((5.2, 6.8, 7.2), (5.8, 7.8, 8.8), H_BRASS_COIL),
            _tbox((10.2, 6.8, 7.2), (10.8, 7.8, 8.8), H_BRASS_COIL),
        ],
    }

    # 5. purification_pylon: 3-legged tripod, segmented prism between brass collars & foot capacitors
    models["purification_pylon"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_purification", "mycelialmind:block/purification_pylon_side"),
        "elements": [
            _tbox((2.5, 0, 2.5), (13.5, 1.8, 13.5), _faces(P_OBS, up=P_PLATINUM)),
            _tbox((2.8, 1.8, 2.8), (13.2, 2.8, 13.2), _faces(P_PLATINUM, up=P_PLAT_RUNE)),
            _tbox((6.2, 2.8, 6.2), (9.8, 4.0, 9.8), P_AQUA, shade=False),
            _tbox((5.6, 4.0, 5.6), (10.4, 5.0, 10.4), _faces(P_BRASS_COIL, north=P_CYAN_GRATE, south=P_CYAN_GRATE)),
            _tbox((6.2, 5.0, 6.2), (9.8, 8.5, 9.8), P_AQUA, shade=False),
            _tbox((5.6, 8.5, 5.6), (10.4, 9.5, 10.4), _faces(P_BRASS_COIL, north=P_CYAN_GRATE, south=P_CYAN_GRATE)),
            _tbox((6.2, 9.5, 6.2), (9.8, 10.8, 9.8), P_AQUA, shade=False),
            _tbox((4.8, 10.8, 4.8), (11.2, 12.2, 11.2), _faces(P_PLATINUM, up=P_AQUA_RUNE, east=P_DARK_VENT, west=P_DARK_VENT)),
            _tbox((6.4, 12.2, 6.4), (9.6, 13.4, 9.6), P_AQUA, shade=False),
            _tbox((5.2, 13.4, 5.2), (10.8, 14.8, 10.8), _faces(P_GOLD, up=P_AQUA_RUNE)),
            # 3 foot capacitors + angled tripod struts touching hub underside cleanly
            _tbox((1.0, 2.8, 6.8), (3.2, 4.4, 9.2), P_AQUA_COIL, shade=False),
            _tbox((12.8, 2.8, 6.8), (15.0, 4.4, 9.2), P_AQUA_COIL, shade=False),
            _tbox((6.8, 2.8, 12.8), (9.2, 4.4, 15.0), P_AQUA_COIL, shade=False),
            _tbox((1.4, 4.4, 7.3), (2.8, 10.72, 8.7), P_BRUSHED, rotation=_rot((2.8, 4.4, 8.0), "z", -22.5)),
            _tbox((13.2, 4.4, 7.3), (14.6, 10.72, 8.7), P_BRUSHED, rotation=_rot((13.2, 4.4, 8.0), "z", 22.5)),
            _tbox((7.3, 4.4, 13.2), (8.7, 10.72, 14.6), P_BRUSHED, rotation=_rot((8.0, 4.4, 13.2), "x", -22.5)),
        ],
    }

    # 6. purification_turret: armored base, swivel collar, flush side drums, twin barrels & heatsink
    models["purification_turret"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_purification", "mycelialmind:block/purification_turret"),
        "elements": [
            _tbox((2.0, 0, 2.0), (14.0, 2.0, 14.0), _faces(P_OBS, up=P_PLAT_RUNE)),
            _tbox((2.2, 2.0, 2.2), (3.8, 2.8, 3.8), P_GOLD),
            _tbox((12.2, 2.0, 2.2), (13.8, 2.8, 3.8), P_GOLD),
            _tbox((2.2, 2.0, 12.2), (3.8, 2.8, 13.8), P_GOLD),
            _tbox((12.2, 2.0, 12.2), (13.8, 2.8, 13.8), P_GOLD),
            _tbox((4.8, 2.0, 4.8), (11.2, 4.6, 11.2), P_BRASS_COIL),
            _tbox((4.2, 4.6, 4.2), (11.8, 5.6, 11.8), _faces(P_PLATINUM, up=P_BRASS_COIL)),
            # main turret housing + flush side ammo drums + barrel bridge
            _tbox((4.4, 5.6, 4.8), (11.6, 10.6, 12.2), _faces(P_PLATINUM, up=P_PLAT_RUNE, east=P_DARK_VENT, west=P_DARK_VENT)),
            _tbox((2.6, 6.2, 6.0), (4.4, 9.4, 10.2), P_AQUA_COIL, shade=False),
            _tbox((11.6, 6.2, 6.0), (13.4, 9.4, 10.2), P_AQUA_COIL, shade=False),
            _tbox((4.8, 6.5, 3.8), (11.2, 9.8, 4.8), _faces(P_BRUSHED, north=P_CYAN_GRATE)),
            # twin barrels + recoil dampers flush to barrel bridge
            _tbox((5.4, 6.8, 0.2), (7.2, 8.8, 3.8), P_AQUA, shade=False),
            _tbox((8.8, 6.8, 0.2), (10.6, 8.8, 3.8), P_AQUA, shade=False),
            _tbox((5.2, 8.8, 1.5), (7.4, 9.5, 3.8), P_BRUSHED),
            _tbox((8.6, 8.8, 1.5), (10.8, 9.5, 3.8), P_BRUSHED),
            # rear heatsink + top cooling fins
            _tbox((5.4, 6.4, 12.2), (10.6, 9.6, 14.0), P_DARK_VENT),
            _tbox((5.8, 10.6, 10.8), (10.2, 11.6, 13.4), P_BRASS_COIL),
        ],
    }

    # 7. hive_altar: monolithic sacrificial table, split runic piers, notched bevels & 2-tier basin
    models["hive_altar"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_altars", "mycelialmind:block/hive_altar"),
        "elements": [
            _tbox((0.5, 0, 1.5), (15.5, 2.0, 14.5), _faces(A_OBS, up=A_OBS_RUNE)),
            _tbox((1.5, 2.0, 2.5), (14.5, 2.8, 13.5), _faces(A_GOLD, up=A_GOLD_RUNE)),
            # twin piers split cleanly by runic inlay bands (5.0..6.2)
            _tbox((2, 2.8, 3), (6, 5.0, 13), A_OBS_RUNE),
            _tbox((1.8, 5.0, 4.5), (6.2, 6.2, 11.5), A_AMETHYST_RUNE),
            _tbox((2, 6.2, 3), (6, 8.2, 13), A_OBS_RUNE),
            _tbox((10, 2.8, 3), (14, 5.0, 13), A_OBS_RUNE),
            _tbox((9.8, 5.0, 4.5), (14.2, 6.2, 11.5), A_AMETHYST_RUNE),
            _tbox((10, 6.2, 3), (14, 8.2, 13), A_OBS_RUNE),
            # under-slab bevel + altar slab + corner brass rivets + 2-tier basin
            _tbox((1.5, 8.2, 2.0), (14.5, 9.2, 14.0), A_OBS),
            _tbox((0.5, 9.2, 1.0), (15.5, 11.8, 15.0), _faces(A_AMETHYST_RUNE, up=A_OBS_RUNE, down=A_OBS)),
            _tbox((1.0, 11.8, 1.5), (2.2, 12.4, 2.7), A_GOLD_RUNE),
            _tbox((13.8, 11.8, 1.5), (15.0, 12.4, 2.7), A_GOLD_RUNE),
            _tbox((1.0, 11.8, 13.3), (2.2, 12.4, 14.5), A_GOLD_RUNE),
            _tbox((13.8, 11.8, 13.3), (15.0, 12.4, 14.5), A_GOLD_RUNE),
            _tbox((3.8, 11.8, 3.8), (12.2, 12.4, 12.2), _faces(A_OBS_RUNE, up=A_GOLD_RUNE)),
            _tbox((5.0, 12.4, 5.0), (11.0, 13.0, 11.0), A_VIOLET_CRYSTAL, shade=False),
        ],
    }

    # 8. matriarch_altar: royal chitin throne/lectern with zero-overlap filigree & 2-stage chalice
    models["matriarch_altar"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_altars", "mycelialmind:block/matriarch_altar_side"),
        "elements": [
            _tbox((0.5, 0, 0.5), (15.5, 2.0, 15.5), _faces(A_OBS_RUNE, up=A_GOLD_RUNE)),
            _tbox((1.5, 2.0, 1.5), (14.5, 3.0, 14.5), A_GOLD),
            # royal backrest + flanking gold columns + top crown
            _tbox((3.2, 3.0, 11.2), (12.8, 14.0, 14.5), _faces(A_OBS, north=A_OBS_RUNE, up=A_GOLD_RUNE)),
            _tbox((2.0, 3.0, 10.8), (3.2, 14.0, 12.4), A_GOLD_RUNE),
            _tbox((12.8, 3.0, 10.8), (14.0, 14.0, 12.4), A_GOLD_RUNE),
            _tbox((4.5, 14.0, 10.8), (11.5, 16.0, 14.2), A_GOLD_RUNE),
            _tbox((5.2, 6.5, 10.4), (10.8, 13.5, 11.2), A_AMETHYST_RUNE),
            _tbox((6.5, 9.0, 9.8), (9.5, 12.0, 10.4), A_EMBER_CRYSTAL, shade=False),
            # twin flanking armrests
            _tbox((2.2, 3.0, 5.5), (4.0, 8.2, 10.8), A_INDIGO),
            _tbox((12.0, 3.0, 5.5), (13.8, 8.2, 10.8), A_INDIGO),
            _tbox((2.0, 8.2, 5.0), (4.2, 9.2, 10.8), _faces(A_GOLD, up=A_GOLD_RUNE)),
            _tbox((11.8, 8.2, 5.0), (14.0, 9.2, 10.8), _faces(A_GOLD, up=A_GOLD_RUNE)),
            # connecting seat bridge + stepped lectern + 2-stage chalice
            _tbox((4.0, 3.0, 8.8), (12.0, 6.5, 11.2), A_OBS_RUNE),
            _tbox((4.2, 3.0, 2.8), (11.8, 7.2, 8.8), _faces(A_INDIGO, north=A_AMETHYST_RUNE)),
            _tbox((4.8, 7.2, 3.2), (11.2, 8.4, 8.6), _faces(A_GOLD, up=A_GOLD_RUNE)),
            _tbox((6.5, 8.4, 4.5), (9.5, 9.6, 7.5), A_GOLD_RUNE),
            _tbox((7.0, 9.6, 5.0), (9.0, 10.8, 7.0), A_EMBER_CRYSTAL, shade=False),
        ],
    }

    # 9. synaptic_research_bench: cabinet drawers, crossbar, worktop, articulated microscope & specimen jar
    models["synaptic_research_bench"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_purification", "mycelialmind:block/synaptic_research_bench"),
        "elements": [
            _tbox((1, 0, 2), (6.5, 10, 14), _faces(P_OBS, east=P_DARK_VENT, west=P_DARK_VENT)),
            _tbox((1.5, 2.0, 1.6), (6.0, 4.5, 2.0), P_BRUSHED),
            _tbox((1.5, 6.0, 1.6), (6.0, 8.5, 2.0), P_BRUSHED),
            _tbox((3.0, 3.0, 1.3), (4.5, 3.6, 1.6), P_GOLD),
            _tbox((3.0, 7.0, 1.3), (4.5, 7.6, 1.6), P_GOLD),
            _tbox((12.5, 0, 2), (15, 10, 4.5), P_BRUSHED),
            _tbox((12.5, 0, 11.5), (15, 10, 14), P_BRUSHED),
            _tbox((6.5, 2.5, 11.8), (12.5, 3.8, 13.6), P_OBS),
            # worktop + corner brass brackets
            _tbox((0.5, 10, 1), (15.5, 12.5, 15), _faces(P_PLATINUM, up=P_PLAT_RUNE, down=P_OBS)),
            _tbox((0.4, 12.5, 0.9), (2.2, 13.0, 2.5), P_BRASS_COIL),
            _tbox((13.8, 12.5, 0.9), (15.6, 13.0, 2.5), P_BRASS_COIL),
            # connected articulated microscope (base + focus knob + neck + arm + glowing lens)
            _tbox((10.5, 12.5, 9.5), (13.5, 14.5, 13), _faces(P_OBS, north=P_CYAN_GRATE)),
            _tbox((9.7, 13.3, 10.5), (10.5, 14.3, 11.5), P_GOLD),
            _tbox((11.4, 14.5, 10.0), (12.6, 15.6, 12.0), P_BRASS_COIL),
            _tbox((11.3, 14.6, 7.0), (12.7, 15.6, 10.0), P_BRASS_COIL),
            _tbox((11.4, 13.3, 7.2), (12.6, 14.6, 8.4), P_AQUA, shade=False),
            # sample rack + grounded brass tray & glowing specimen jar
            _tbox((2.0, 12.5, 9.5), (5.5, 13.8, 13.5), _faces(P_BRUSHED, up=P_CYAN_GRATE)),
            _tbox((2.2, 12.5, 4.7), (5.3, 13.1, 7.8), P_BRUSHED),
            _tbox((2.5, 13.1, 5.0), (5.0, 15.2, 7.5), P_AQUA_COIL, shade=False),
            _tbox((2.3, 15.2, 4.8), (5.2, 15.7, 7.7), P_BRASS_COIL),
        ],
    }

    # 10. spore_vent: asymmetrical volcanic fumarole mound with flush glowing crater
    models["spore_vent"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/spore_vent_side"),
        "elements": [
            _tbox((1, 0, 1), (15, 3.5, 15), _faces(H_OBS, up=H_INDIGO_PLATE)),
            _tbox((3, 3.5, 2.5), (13.5, 7, 13), _faces(H_INDIGO_PLATE, north=H_VENT, south=H_VENT)),
            _tbox((5, 7, 4), (12, 10.5, 11), _faces(H_VIOLET_SCALE, up=H_OBS)),
            _tbox((6.2, 10.5, 5.2), (10.8, 11.5, 9.8), H_EMBER, shade=False),
        ],
    }

    # 11. bioluminescent_mycelial_cap (flora): curved stalk, glowing gills & stepped cap
    models["bioluminescent_mycelial_cap"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/bioluminescent_mycelial_cap"),
        "elements": [
            _tbox((6.5, 0, 6.5), (9.5, 5.2, 9.5), H_IVORY_SCALE),
            _tbox((6.8, 5.2, 6.8), (9.2, 8.75, 9.2), H_IVORY_SCALE, rotation=_rot((8, 5.2, 6.8), "x", -22.5)),
            _tbox((4, 9.42, 3.5), (12, 10.6, 11.5), H_CYAN_CRYSTAL, shade=False),
            _tbox((3, 10.6, 2.5), (13, 12.6, 12.5), _faces(H_VIOLET_SCALE, down=H_CYAN_CRYSTAL, up=H_AMETHYST)),
            _tbox((4.5, 12.6, 4), (11.5, 14.1, 11), H_AMETHYST, shade=False),
            _tbox((6, 14.1, 5.5), (10, 15.3, 9.5), H_CYAN_CRYSTAL, shade=False),
        ],
    }

    # 12. spore_bulb (flora): calyx + 4 angled petals + glowing chartreuse bulb
    models["spore_bulb"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/spore_bulb"),
        "elements": [
            _tbox((5.0, 0, 5.0), (11.0, 2.5, 11.0), _faces(H_OBS, up=H_CHARTREUSE)),
            _tbox((5.2, 2.5, 5.2), (10.8, 8.5, 10.8), H_CHARTREUSE, shade=False),
            _tbox((6, 8.5, 6), (10, 11, 10), H_GOLD_CRYSTAL, shade=False),
            _tbox((3.6, 2.5, 6.5), (5.0, 9.0, 9.5), H_VIOLET_SCALE, rotation=_rot((5.0, 2.5, 8), "z", 22.5)),
            _tbox((11.0, 2.5, 6.5), (12.4, 9.0, 9.5), H_VIOLET_SCALE, rotation=_rot((11.0, 2.5, 8), "z", -22.5)),
            _tbox((6.5, 2.5, 3.6), (9.5, 9.0, 5.0), H_VIOLET_SCALE, rotation=_rot((8, 2.5, 5.0), "x", -22.5)),
            _tbox((6.5, 2.5, 11.0), (9.5, 9.0, 12.4), H_VIOLET_SCALE, rotation=_rot((8, 2.5, 11.0), "x", 22.5)),
        ],
    }

    # 13. mycelial_tendrils (flora): multi-level 3D organic tendrils rooted cleanly on floor pad
    models["mycelial_tendrils"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/mycelial_tendrils"),
        "elements": [
            _tbox((5.2, 0, 5.2), (10.8, 1.5, 10.8), _faces(H_OBS, up=H_VEIN)),
            _tbox((3.4, 1.5, 7.0), (5.2, 11.5, 9.0), H_VEIN, rotation=_rot((5.2, 1.5, 8.0), "z", 22.5)),
            _tbox((10.8, 1.5, 7.0), (12.6, 10.5, 9.0), H_CYAN_CRYSTAL, rotation=_rot((10.8, 1.5, 8.0), "z", -22.5), shade=False),
            _tbox((7.0, 1.5, 3.4), (9.0, 9.5, 5.2), H_AMETHYST, rotation=_rot((8.0, 1.5, 5.2), "x", -22.5), shade=False),
            _tbox((7.0, 1.5, 10.8), (9.0, 10.0, 12.6), H_VEIN, rotation=_rot((8.0, 1.5, 10.8), "x", 22.5)),
        ],
    }

    # 14. mycelial_rift (Dimensional Portal): threshold, jambs, flush crystal inlays, flow sheet & crown keystone
    models["mycelial_rift"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_altars", "mycelialmind:block/mycelial_rift", with_flow=True),
        "elements": [
            _tbox((1.0, 0, 5.0), (15.0, 2.5, 11.0), _faces(A_OBS_RUNE, up=A_AMETHYST_RUNE)),
            _tbox((1.5, 2.5, 6.0), (3.5, 13.5, 10.0), A_AMETHYST_RUNE),
            _tbox((12.5, 2.5, 6.0), (14.5, 13.5, 10.0), A_AMETHYST_RUNE),
            _tbox((0.8, 7.0, 6.5), (1.5, 9.0, 9.5), A_AMETHYST_CRYSTAL, shade=False),
            _tbox((14.5, 7.0, 6.5), (15.2, 9.0, 9.5), A_AMETHYST_CRYSTAL, shade=False),
            _flow_box((3.5, 2.5, 7.2), (12.5, 13.5, 8.8)),
            _tbox((1.5, 13.5, 5.5), (14.5, 15.6, 10.5), _faces(A_OBS_RUNE, north=A_GOLD_RUNE, south=A_GOLD_RUNE)),
            _tbox((6.5, 13.5, 4.9), (9.5, 15.8, 5.5), A_GOLD_RUNE),
        ],
    }

    # 15. chitin_pillar (+ chitin_pillar_horizontal): fluted column with flush external ribs
    models["chitin_pillar"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/chitin_pillar_side"),
        "elements": [
            _tbox((1, 0, 1), (15, 2.5, 15), _faces(H_BONE, up=H_OBS, down=H_OBS)),
            _tbox((2.5, 2.5, 2.5), (13.5, 13.5, 13.5), _faces(H_IVORY_SCALE, north=H_BONE, south=H_BONE, east=H_BONE, west=H_BONE)),
            _tbox((1, 13.5, 1), (15, 16, 15), _faces(H_BONE, up=H_OBS, down=H_OBS)),
            # 4 fluted vertical ribs flush to shaft exterior
            _tbox((1.5, 2.5, 6), (2.5, 13.5, 10), H_OBS),
            _tbox((13.5, 2.5, 6), (14.5, 13.5, 10), H_OBS),
            _tbox((6, 2.5, 1.5), (10, 13.5, 2.5), H_OBS),
            _tbox((6, 2.5, 13.5), (10, 13.5, 14.5), H_OBS),
        ],
    }
    models["chitin_pillar_horizontal"] = {
        "parent": "mycelialmind:block/chitin_pillar",
    }

    # 16. bio_chitin_lattice: open outer frame, 4 notched corner struts & central amethyst hub
    models["bio_chitin_lattice"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/bio_chitin_lattice"),
        "elements": [
            _tbox((0, 0, 0), (16, 2, 16), _faces(H_OBS, up=H_INDIGO_RUNE, down=H_INDIGO_RUNE)),
            _tbox((0, 14, 0), (16, 16, 16), _faces(H_OBS, up=H_INDIGO_RUNE, down=H_INDIGO_RUNE)),
            _tbox((0, 2, 0), (2, 14, 2), H_OBS),
            _tbox((14, 2, 0), (16, 14, 2), H_OBS),
            _tbox((0, 2, 14), (2, 14, 16), H_OBS),
            _tbox((14, 2, 14), (16, 14, 16), H_OBS),
            # notched cardinal struts + central hub + front/back amethyst faces
            _tbox((7.2, 2.0, 7.2), (8.8, 4.8, 8.8), H_INDIGO_PLATE),
            _tbox((7.2, 11.2, 7.2), (8.8, 14.0, 8.8), H_INDIGO_PLATE),
            _tbox((4.8, 4.8, 4.8), (11.2, 11.2, 11.2), _faces(H_OBS, east=H_VENT, west=H_VENT, up=H_AMETHYST, down=H_AMETHYST)),
            _tbox((5.6, 5.6, 4.2), (10.4, 10.4, 4.8), H_AMETHYST, shade=False),
            _tbox((5.6, 5.6, 11.2), (10.4, 10.4, 11.8), H_AMETHYST, shade=False),
        ],
    }

    # 17. reinforced_bio_barrier: armored bunker bulkhead with notched perimeter frame & 4 flush armor plates
    models["reinforced_bio_barrier"] = {
        "parent": "minecraft:block/block",
        "textures": _atlas_textures("atlas_blocks_hive", "mycelialmind:block/reinforced_bio_barrier"),
        "elements": [
            _tbox((0, 0, 0), (16, 2.5, 16), _faces(H_OBS, up=H_INDIGO_RUNE)),
            _tbox((0, 13.5, 0), (16, 16, 16), _faces(H_OBS, up=H_INDIGO_RUNE)),
            _tbox((0, 2.5, 0), (2.5, 13.5, 2.5), H_OBS),
            _tbox((13.5, 2.5, 0), (16, 13.5, 2.5), H_OBS),
            _tbox((0, 2.5, 13.5), (2.5, 13.5, 16), H_OBS),
            _tbox((13.5, 2.5, 13.5), (16, 13.5, 16), H_OBS),
            _tbox((2.5, 2.5, 2.5), (13.5, 13.5, 13.5), _faces(H_INDIGO_PLATE, up=H_INDIGO_RUNE)),
            # 4 flush cross armor plates on exterior faces
            _tbox((5, 5, 0.5), (11, 11, 2.5), _faces(H_BONE, north=H_IVORY_SCALE)),
            _tbox((5, 5, 13.5), (11, 11, 15.5), _faces(H_BONE, south=H_IVORY_SCALE)),
            _tbox((0.5, 5, 5), (2.5, 11, 11), _faces(H_BONE, west=H_IVORY_SCALE)),
            _tbox((13.5, 5, 5), (15.5, 11, 11), _faces(H_BONE, east=H_IVORY_SCALE)),
        ],
    }

    return models


# ---------------------------------------------------------------------------
# 35 Masterwork 3D Item Models (100% unique architecture per item)
# ---------------------------------------------------------------------------

def _faces(all_tile: TileRef, **overrides: TileRef) -> Dict[str, TileRef]:
    d: Dict[str, TileRef] = {"all": all_tile}
    d.update(overrides)
    return d


_WEAPON_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.5, 0.5], "scale": [0.90, 0.90, 0.90]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.5, 0.5], "scale": [0.90, 0.90, 0.90]},
    "firstperson_righthand": {"rotation": [0, -90, 28], "translation": [1.0, 2.2, 1.0], "scale": [0.75, 0.75, 0.75]},
    "firstperson_lefthand": {"rotation": [0, 90, -28], "translation": [1.0, 2.2, 1.0], "scale": [0.75, 0.75, 0.75]},
    "gui": {"rotation": [25, 45, -35], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
}

_LAUNCHER_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, -2.0], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 1.5, -2.0], "scale": [0.9, 0.9, 0.9]},
    "firstperson_righthand": {"rotation": [0, -10, 5], "translation": [0, 3.0, -1.0], "scale": [0.75, 0.75, 0.75]},
    "firstperson_lefthand": {"rotation": [0, 10, -5], "translation": [0, 3.0, -1.0], "scale": [0.75, 0.75, 0.75]},
    "gui": {"rotation": [25, 135, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
}

_TOOL_INJECTOR_DISPLAY = {
    "thirdperson_righthand": {"rotation": [10, -90, 25], "translation": [0, 3.0, 1.0], "scale": [0.68, 0.68, 0.68]},
    "thirdperson_lefthand": {"rotation": [10, 90, -25], "translation": [0, 3.0, 1.0], "scale": [0.68, 0.68, 0.68]},
    "firstperson_righthand": {"rotation": [0, -75, 18], "translation": [0.8, 2.5, 0.5], "scale": [0.62, 0.62, 0.62]},
    "firstperson_lefthand": {"rotation": [0, 75, -18], "translation": [0.8, 2.5, 0.5], "scale": [0.62, 0.62, 0.62]},
    "gui": {"rotation": [28, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.90, 0.90, 0.90]},
}

_RELIC_DISPLAY = {
    "thirdperson_righthand": {"rotation": [15, 45, 0], "translation": [0, 2.5, 1.0], "scale": [0.65, 0.65, 0.65]},
    "thirdperson_lefthand": {"rotation": [15, -45, 0], "translation": [0, 2.5, 1.0], "scale": [0.65, 0.65, 0.65]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, -45, 0], "translation": [0, 2.5, 0], "scale": [0.6, 0.6, 0.6]},
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
}

_MATERIAL_COMPACT_DISPLAY = {
    "thirdperson_righthand": {"rotation": [20, 45, 0], "translation": [0, 2.0, 1.0], "scale": [0.50, 0.50, 0.50]},
    "thirdperson_lefthand": {"rotation": [20, -45, 0], "translation": [0, 2.0, 1.0], "scale": [0.50, 0.50, 0.50]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2.5, 0], "scale": [0.52, 0.52, 0.52]},
    "firstperson_lefthand": {"rotation": [0, -45, 0], "translation": [0, 2.5, 0], "scale": [0.52, 0.52, 0.52]},
    "gui": {"rotation": [32, 225, 0], "translation": [0, 0.5, 0], "scale": [1.05, 1.05, 1.05]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2.5, 0], "scale": [0.48, 0.48, 0.48]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
}


def build_3d_item_models() -> Dict[str, Dict[str, Any]]:
    models: Dict[str, Dict[str, Any]] = {}

    # === 11 Masterwork Weapons, Tech Tools & Relics ===
    # 1. symbiotic_blade: functional sword hierarchy (pommel -> slim waisted grip -> ricasso -> split quillons -> fuller + tapered 3-step blade)
    models["symbiotic_blade"] = {
        "textures": _atlas_textures("atlas_items_weapons", "mycelialmind:item/symbiotic_blade"),
        "elements": [
            # Pommel + pommel collar + slim ergonomic grip + ricasso
            _tbox((7.0, 0.0, 7.0), (9.0, 1.2, 9.0), W_GOLD_COIL),
            _tbox((7.2, 1.2, 7.2), (8.8, 1.7, 8.8), W_OBS),
            _tbox((7.4, 1.7, 7.4), (8.6, 4.6, 8.6), W_INDIGO_COIL),
            _tbox((7.2, 4.6, 7.0), (8.8, 5.4, 9.0), W_OBS),
            # Split crossguard with central cyan tang socket + tapered swept quillons
            _tbox((5.2, 5.4, 6.8), (7.0, 6.2, 9.2), W_OBS),
            _tbox((9.0, 5.4, 6.8), (10.8, 6.2, 9.2), W_OBS),
            _tbox((7.0, 5.4, 6.7), (9.0, 6.2, 9.3), W_CYAN, shade=False),
            _tbox((3.8, 5.5, 7.0), (5.2, 6.2, 9.0), W_BONE, rotation=_rot((5.2, 6.2, 8.0), "z", -22.5)),
            _tbox((10.8, 5.5, 7.0), (12.2, 6.2, 9.0), W_BONE, rotation=_rot((10.8, 6.2, 8.0), "z", 22.5)),
            # Slim tapered blade: central fuller spine + razor plasma edge + 3 micro dorsal serrations + 2-step spear point
            _tbox((7.55, 6.2, 7.5), (8.45, 13.6, 8.6), W_BRUSHED),
            _tbox((7.7, 6.2, 6.5), (8.3, 13.6, 7.5), W_CYAN, shade=False),
            _tbox((7.65, 7.2, 8.6), (8.35, 8.1, 9.0), W_BONE),
            _tbox((7.65, 9.0, 8.6), (8.35, 9.9, 9.0), W_BONE),
            _tbox((7.65, 10.8, 8.6), (8.35, 11.6, 8.95), W_BONE),
            _tbox((7.65, 13.6, 6.9), (8.35, 14.9, 8.3), W_CYAN, shade=False),
            _tbox((7.8, 14.9, 7.2), (8.2, 16.0, 8.0), W_CYAN, shade=False),
        ],
        "display": _WEAPON_DISPLAY,
    }

    # 2. enzyme_lance: functional polearm hierarchy (butt cap -> slim fluted shaft -> guard cup -> capillary enzyme chamber -> 3-stage needle)
    models["enzyme_lance"] = {
        "textures": _atlas_textures("atlas_items_weapons", "mycelialmind:item/enzyme_lance"),
        "elements": [
            _tbox((7.2, 0.0, 7.2), (8.8, 1.2, 8.8), W_OBS),
            _tbox((7.5, 1.2, 7.5), (8.5, 5.2, 8.5), W_BONE),
            _tbox((7.1, 5.2, 7.1), (8.9, 5.9, 8.9), W_GOLD_COIL),
            _tbox((7.5, 5.9, 7.5), (8.5, 8.6, 8.5), W_BONE),
            _tbox((6.0, 8.6, 6.0), (10.0, 9.6, 10.0), W_OBS),
            _tbox((6.9, 9.6, 6.9), (9.1, 12.0, 9.1), W_EMBER, shade=False),
            # Exterior capillary feed-tubes
            _tbox((6.3, 9.6, 7.5), (6.9, 12.0, 8.5), W_BRASS),
            _tbox((9.1, 9.6, 7.5), (9.7, 12.0, 8.5), W_BRASS),
            _tbox((6.6, 12.0, 6.6), (9.4, 12.8, 9.4), W_BRUSHED),
            _tbox((7.1, 12.8, 7.1), (8.9, 14.1, 8.9), W_BRUSHED),
            _tbox((7.4, 14.1, 7.4), (8.6, 15.1, 8.6), W_BRASS),
            _tbox((7.7, 15.1, 7.7), (8.3, 16.0, 8.3), W_EMBER, shade=False),
        ],
        "display": _WEAPON_DISPLAY,
    }

    # 3. neutralizing_spore_launcher: stock + grip + trigger guard + picatinny rail + side drum + shrouded barrel & muzzle brake
    models["neutralizing_spore_launcher"] = {
        "textures": _atlas_textures("atlas_items_weapons", "mycelialmind:item/neutralizing_spore_launcher"),
        "elements": [
            _tbox((6.9, 6.6, 12.0), (9.1, 10.0, 15.5), W_OBS),
            _tbox((7.1, 2.5, 9.2), (8.9, 6.5, 11.2), W_INDIGO_COIL, rotation=_rot((8.0, 6.5, 11.2), "x", -22.5)),
            _tbox((7.5, 5.2, 7.0), (8.5, 6.5, 9.0), W_OBS),
            _tbox((6.3, 6.5, 4.8), (9.7, 10.5, 12.0), _faces(W_PLAT, east=W_VENT, west=W_VENT, up=W_BRUSHED)),
            _tbox((9.7, 6.6, 6.4), (11.9, 9.6, 10.2), _faces(W_AQUA, up=W_BRASS, down=W_OBS), shade=False),
            # Top sight rail + holo optic
            _tbox((7.4, 10.5, 3.2), (8.6, 11.1, 10.5), W_BRASS),
            _tbox((7.2, 11.1, 6.4), (8.8, 12.5, 8.8), _faces(W_CYAN, east=W_OBS, west=W_OBS), shade=False),
            # Shrouded barrel + stepped muzzle brake
            _tbox((6.8, 7.3, 1.4), (9.2, 9.7, 4.8), _faces(W_VENT, up=W_BRUSHED, down=W_OBS)),
            _tbox((6.5, 7.0, 0.0), (9.5, 10.0, 1.4), _faces(W_AQUA, north=W_DARK_VENT), shade=False),
        ],
        "display": _LAUNCHER_DISPLAY,
    }

    # 4. bio_scanner
    models["bio_scanner"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/bio_scanner"),
        "elements": [
            # Ergonomic lower handle grip + brass collar + slimmer upper reader head
            _tbox((6.5, 1.2, 7.0), (9.5, 4.8, 9.0), _faces(B_OBS_COIL, south=B_VENT)),
            _tbox((6.1, 4.8, 6.7), (9.9, 5.6, 9.3), B_BRASS_COIL),
            _tbox((4.8, 5.6, 6.6), (11.2, 11.2, 9.4), _faces(B_OBS_COIL, up=B_PLAT, south=B_VENT)),
            _tbox((5.4, 6.6, 6.1), (10.6, 10.5, 6.6), B_GLASS_CYAN, shade=False),
            _tbox((5.4, 5.7, 6.1), (10.6, 6.4, 6.6), _faces(B_VENT, north=B_BRASS_COIL)),
            _tbox((11.2, 6.2, 7.1), (12.3, 10.2, 8.9), _faces(B_BRASS_COIL, east=B_GOLD_RUNE)),
            _tbox((5.3, 11.2, 7.4), (6.4, 15.2, 8.6), B_PLAT, rotation=_rot((5.3, 11.2, 8.0), "z", 22.5)),
            _tbox((9.6, 11.2, 7.4), (10.7, 14.4, 8.6), B_AQUA, rotation=_rot((10.7, 11.2, 8.0), "z", -22.5), shade=False),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    # 5. bio_extractor (Surgical Biopsy Extractor: angled pistol grip, vacuum plunger, venom chamber & twin biopsy jaws)
    models["bio_extractor"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/bio_extractor"),
        "elements": [
            _tbox((7.0, 1.2, 7.2), (9.0, 5.4, 9.2), _faces(B_OBS_COIL, south=B_BRASS_COIL), rotation=_rot((8.0, 5.4, 9.2), "x", -22.5)),
            _tbox((5.8, 5.4, 5.2), (10.2, 8.4, 10.8), _faces(B_PLAT, north=B_VENT, south=B_OBS_COIL)),
            _tbox((6.8, 6.0, 10.8), (9.2, 7.8, 13.4), B_BRASS_COIL),
            _tbox((6.2, 5.6, 13.4), (9.8, 8.2, 14.4), B_OBS_COIL),
            _tbox((6.5, 8.4, 5.6), (9.5, 10.4, 10.0), B_VENOM, shade=False),
            _tbox((6.2, 10.4, 5.6), (9.8, 11.2, 10.0), _faces(B_PLAT, up=B_GOLD_RUNE)),
            _tbox((5.6, 5.6, 4.0), (10.4, 8.2, 5.2), _faces(B_BRASS_COIL, north=B_VENT)),
            _tbox((7.6, 6.5, 0.8), (8.4, 7.3, 4.0), B_VENOM, shade=False),
            _tbox((5.6, 6.4, 1.6), (6.5, 7.4, 4.0), B_PLAT, rotation=_rot((5.6, 6.9, 4.0), "y", 22.5)),
            _tbox((9.5, 6.4, 1.6), (10.4, 7.4, 4.0), B_PLAT, rotation=_rot((10.4, 6.9, 4.0), "y", -22.5)),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    # 6. synaptic_suppressor: zero-overlap emitter pins bridging the 3 containment ribs to the core
    models["synaptic_suppressor"] = {
        "textures": _atlas_textures("atlas_items_weapons", "mycelialmind:item/synaptic_suppressor"),
        "elements": [
            _tbox((7.0, 0.5, 7.0), (9.0, 6.8, 9.0), W_OBS),
            _tbox((5.4, 6.8, 5.4), (10.6, 8.4, 10.6), W_GOLD_COIL),
            _tbox((6.5, 9.4, 6.5), (9.5, 13.0, 9.5), W_AMETHYST_VEIN, shade=False),
            _tbox((5.9, 10.8, 7.4), (6.5, 11.6, 8.6), W_GOLD_COIL),
            _tbox((9.5, 10.8, 7.4), (10.1, 11.6, 8.6), W_GOLD_COIL),
            _tbox((7.4, 10.8, 5.9), (8.6, 11.6, 6.5), W_GOLD_COIL),
            _tbox((4.8, 8.4, 7.4), (5.9, 14.2, 8.6), W_BRUSHED),
            _tbox((10.1, 8.4, 7.4), (11.2, 14.2, 8.6), W_BRUSHED),
            _tbox((7.4, 8.4, 4.8), (8.6, 14.2, 5.9), W_BRUSHED),
            _tbox((4.8, 14.2, 4.8), (11.2, 15.8, 11.2), W_GOLD_COIL),
        ],
        "display": _WEAPON_DISPLAY,
    }

    # 7. tide_beacon: radial struts connecting the 3 fins cleanly to the prism & tripod base
    models["tide_beacon"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/tide_beacon"),
        "elements": [
            _tbox((4.0, 1.0, 4.0), (12.0, 2.8, 12.0), R_OBS_RUNE),
            _tbox((6.0, 2.8, 6.0), (10.0, 12.0, 10.0), R_CYAN, shade=False),
            _tbox((6.5, 12.0, 6.5), (9.5, 14.8, 9.5), R_GOLD_CRYSTAL, shade=False),
            _tbox((4.6, 5.2, 7.4), (6.0, 6.2, 8.6), R_BRASS),
            _tbox((10.0, 5.2, 7.4), (11.4, 6.2, 8.6), R_BRASS),
            _tbox((7.4, 5.2, 4.6), (8.6, 6.2, 6.0), R_BRASS),
            _tbox((2.8, 4.5, 7.2), (4.6, 11.5, 8.8), R_PLAT),
            _tbox((11.4, 4.5, 7.2), (13.2, 11.5, 8.8), R_PLAT),
            _tbox((7.2, 4.5, 2.8), (8.8, 11.5, 4.6), R_PLAT),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 8. neural_echo_decoy: central emitter stem & flush base collar connecting orb & antennae
    models["neural_echo_decoy"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/neural_echo_decoy"),
        "elements": [
            _tbox((4.5, 1.0, 4.5), (11.5, 3.2, 11.5), R_OBS_RUNE),
            _tbox((6.0, 3.2, 6.0), (10.0, 8.0, 10.0), R_INDIGO),
            _tbox((7.2, 8.0, 7.2), (8.8, 9.5, 8.8), R_BRASS),
            _tbox((6.5, 9.5, 6.5), (9.5, 12.8, 9.5), R_AMETHYST, shade=False),
            _tbox((4.5, 3.2, 7.3), (5.7, 11.8, 8.7), R_PLAT, rotation=_rot((4.5, 3.2, 8.0), "z", 22.5)),
            _tbox((10.3, 3.2, 7.3), (11.5, 11.8, 8.7), R_PLAT, rotation=_rot((11.5, 3.2, 8.0), "z", -22.5)),
            _tbox((7.3, 3.2, 10.3), (8.7, 11.8, 11.5), R_PLAT, rotation=_rot((8.0, 3.2, 11.5), "x", 22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 9. overmind_crown: slimmer 1.2-thick circlet + 3-step tapered central spire & angled side spires
    models["overmind_crown"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/overmind_crown"),
        "elements": [
            _tbox((3.0, 4.0, 3.0), (13.0, 6.2, 4.2), R_OBS_RUNE),
            _tbox((3.0, 4.0, 11.8), (13.0, 6.2, 13.0), R_OBS_RUNE),
            _tbox((3.0, 4.0, 4.2), (4.2, 6.2, 11.8), R_OBS_RUNE),
            _tbox((11.8, 4.0, 4.2), (13.0, 6.2, 11.8), R_OBS_RUNE),
            _tbox((6.8, 6.2, 3.0), (9.2, 8.6, 4.2), R_GOLD_RUNE),
            _tbox((7.1, 8.6, 3.1), (8.9, 11.4, 4.1), R_GOLD_RUNE),
            _tbox((7.5, 11.4, 3.3), (8.5, 14.0, 3.9), R_GOLD_RUNE),
            _tbox((7.3, 6.8, 2.3), (8.7, 9.6, 3.0), R_GOLD_CRYSTAL, shade=False),
            _tbox((4.2, 6.2, 3.1), (5.5, 10.8, 4.1), R_GOLD_RUNE, rotation=_rot((4.2, 6.2, 3.6), "z", 22.5)),
            _tbox((10.5, 6.2, 3.1), (11.8, 10.8, 4.1), R_GOLD_RUNE, rotation=_rot((11.8, 6.2, 3.6), "z", -22.5)),
            _tbox((3.1, 6.2, 7.2), (4.2, 9.6, 8.8), R_BRASS),
            _tbox((11.8, 6.2, 7.2), (12.9, 9.6, 8.8), R_BRASS),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 10. purifier_drone_core: chassis with non-overlapping rotor arms connecting all 4 micro-rotors
    models["purifier_drone_core"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/purifier_drone_core"),
        "elements": [
            _tbox((5.0, 5.0, 5.0), (11.0, 11.0, 11.0), R_PLAT),
            _tbox((6.2, 6.2, 4.4), (9.8, 9.8, 5.0), R_CYAN, shade=False),
            _tbox((4.0, 7.2, 4.0), (6.2, 8.8, 5.0), R_BRASS),
            _tbox((9.8, 7.2, 4.0), (12.0, 8.8, 5.0), R_BRASS),
            _tbox((4.0, 7.2, 11.0), (12.0, 8.8, 12.2), R_BRASS),
            # 4 individual outward rotor arms flush to upper chassis corners + rotors on top
            _tbox((3.2, 9.2, 3.2), (5.0, 10.2, 5.0), R_PLAT),
            _tbox((11.0, 9.2, 3.2), (12.8, 10.2, 5.0), R_PLAT),
            _tbox((3.2, 9.2, 11.0), (5.0, 10.2, 12.8), R_PLAT),
            _tbox((11.0, 9.2, 11.0), (12.8, 10.2, 12.8), R_PLAT),
            _tbox((2.0, 10.2, 2.0), (4.5, 11.2, 4.5), R_CYAN, shade=False),
            _tbox((11.5, 10.2, 2.0), (14.0, 11.2, 4.5), R_CYAN, shade=False),
            _tbox((2.0, 10.2, 11.5), (4.5, 11.2, 14.0), R_CYAN, shade=False),
            _tbox((11.5, 10.2, 11.5), (14.0, 11.2, 14.0), R_CYAN, shade=False),
            _tbox((5.8, 11.0, 5.8), (10.2, 12.2, 10.2), R_OBS_RUNE),
            _tbox((4.8, 3.6, 5.4), (5.8, 5.0, 10.6), R_BRASS),
            _tbox((10.2, 3.6, 5.4), (11.2, 5.0, 10.6), R_BRASS),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 11. chimera_ovum
    models["chimera_ovum"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chimera_ovum"),
        "elements": [
            _tbox((5.0, 2.0, 5.0), (11.0, 4.0, 11.0), R_VIOLET_VEIN),
            _tbox((4.2, 4.0, 4.2), (11.8, 10.5, 11.8), R_OBS_SCALE),
            _tbox((6.0, 6.0, 3.6), (10.0, 9.5, 4.2), R_CHARTREUSE, shade=False),
            _tbox((6.0, 6.0, 11.8), (10.0, 9.5, 12.4), R_CHARTREUSE, shade=False),
            _tbox((5.0, 10.5, 5.0), (11.0, 12.5, 11.0), R_VIOLET_VEIN),
            _tbox((5.5, 12.5, 5.5), (10.5, 14.2, 10.5), R_BONE_SCALE),
            _tbox((2.8, 4.2, 6.8), (4.2, 9.2, 9.2), R_BONE_SCALE, rotation=_rot((4.2, 4.2, 8.0), "z", 22.5)),
            _tbox((11.8, 4.2, 6.8), (13.2, 9.2, 9.2), R_BONE_SCALE, rotation=_rot((11.8, 4.2, 8.0), "z", -22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    # === 4 Chitin Armor 3D Gear Models (multi-face interior/exterior) ===
    models["chitin_helmet"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chitin_helmet"),
        "elements": [
            _tbox((3.6, 4.5, 3.6), (12.4, 10.5, 12.4), _faces(R_OBS_SCALE, north=R_OBS_RUNE, down=R_INDIGO)),
            _tbox((4.2, 5.4, 2.9), (11.8, 7.2, 3.6), R_AMETHYST, shade=False),
            _tbox((3.6, 3.2, 3.6), (5.6, 4.5, 6.2), _faces(R_BONE_SCALE, east=R_INDIGO)),
            _tbox((10.4, 3.2, 3.6), (12.4, 4.5, 6.2), _faces(R_BONE_SCALE, west=R_INDIGO)),
            _tbox((3.2, 10.5, 4.2), (12.8, 12.3, 11.8), _faces(R_BONE_SCALE, up=R_OBS_RUNE)),
            _tbox((7.3, 12.3, 4.8), (8.7, 13.6, 11.2), R_OBS_RUNE),
            _tbox((2.4, 12.3, 6.6), (4.0, 15.2, 8.4), R_BONE_SCALE, rotation=_rot((2.4, 12.3, 7.5), "z", 22.5)),
            _tbox((12.0, 12.3, 6.6), (13.6, 15.2, 8.4), R_BONE_SCALE, rotation=_rot((13.6, 12.3, 7.5), "z", -22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["chitin_chestplate"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chitin_chestplate"),
        "elements": [
            _tbox((4.8, 1.8, 5.0), (11.2, 3.6, 11.0), _faces(R_INDIGO, north=R_OBS_RUNE)),
            _tbox((6.8, 2.0, 4.4), (9.2, 3.4, 5.0), R_BONE_SCALE),
            _tbox((4.0, 3.6, 5.0), (12.0, 10.8, 11.0), _faces(R_OBS_SCALE, north=R_OBS_RUNE, south=R_INDIGO)),
            _tbox((5.8, 6.2, 4.3), (10.2, 9.8, 5.0), R_AMETHYST, shade=False),
            _tbox((7.2, 4.4, 11.0), (8.8, 10.4, 11.8), R_BONE_SCALE),
            _tbox((3.8, 10.8, 5.0), (12.2, 12.6, 11.0), _faces(R_INDIGO, up=R_OBS_RUNE)),
            _tbox((1.4, 9.6, 4.6), (3.8, 13.0, 11.4), _faces(R_BONE_SCALE, down=R_INDIGO), rotation=_rot((3.8, 13.0, 8.0), "z", -22.5)),
            _tbox((12.2, 9.6, 4.6), (14.6, 13.0, 11.4), _faces(R_BONE_SCALE, down=R_INDIGO), rotation=_rot((12.2, 13.0, 8.0), "z", 22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["chitin_leggings"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chitin_leggings"),
        "elements": [
            _tbox((4.0, 10.6, 5.2), (12.0, 13.2, 10.8), _faces(R_OBS_RUNE, up=R_INDIGO)),
            _tbox((6.8, 11.0, 4.6), (9.2, 12.8, 5.2), R_BONE_SCALE),
            _tbox((4.3, 2.0, 5.5), (7.3, 10.6, 10.5), _faces(R_OBS_SCALE, east=R_INDIGO)),
            _tbox((8.7, 2.0, 5.5), (11.7, 10.6, 10.5), _faces(R_OBS_SCALE, west=R_INDIGO)),
            _tbox((4.3, 5.4, 4.7), (7.3, 8.0, 5.5), R_BONE_SCALE),
            _tbox((8.7, 5.4, 4.7), (11.7, 8.0, 5.5), R_BONE_SCALE),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["chitin_boots"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chitin_boots"),
        "elements": [
            _tbox((3.3, 2.0, 4.2), (7.1, 7.6, 11.4), _faces(R_OBS_SCALE, down=R_INDIGO)),
            _tbox((8.9, 2.0, 4.2), (12.7, 7.6, 11.4), _faces(R_OBS_SCALE, down=R_INDIGO)),
            # heel strap bridging left & right boots for a connected pair display
            _tbox((7.1, 6.2, 7.2), (8.9, 7.2, 8.8), R_BONE_SCALE),
            _tbox((3.1, 2.0, 2.6), (7.3, 4.4, 4.2), R_BONE_SCALE),
            _tbox((8.7, 2.0, 2.6), (12.9, 4.4, 4.2), R_BONE_SCALE),
            _tbox((3.5, 7.6, 4.4), (6.9, 8.6, 11.2), _faces(R_OBS_RUNE, up=R_INDIGO)),
            _tbox((9.1, 7.6, 4.4), (12.5, 8.6, 11.2), _faces(R_OBS_RUNE, up=R_INDIGO)),
            _tbox((4.5, 4.5, 11.4), (5.9, 7.4, 12.8), R_AMETHYST, shade=False),
            _tbox((10.1, 4.5, 11.4), (11.5, 7.4, 12.8), R_AMETHYST, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    # === 17 Tactical, Biotech & Endgame Relics (100% unique silhouettes) ===
    models["purifier_grenade"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/purifier_grenade"),
        "elements": [
            _tbox((5.2, 2.0, 5.2), (10.8, 4.2, 10.8), _faces(B_PLAT, down=B_OBS_COIL)),
            _tbox((4.8, 4.2, 4.8), (11.2, 8.6, 11.2), B_AQUA, shade=False),
            _tbox((4.2, 4.4, 7.2), (4.8, 8.4, 8.8), B_OBS_COIL),
            _tbox((11.2, 4.4, 7.2), (11.8, 8.4, 8.8), B_OBS_COIL),
            _tbox((7.2, 4.4, 4.2), (8.8, 8.4, 4.8), B_OBS_COIL),
            _tbox((7.2, 4.4, 11.2), (8.8, 8.4, 11.8), B_OBS_COIL),
            _tbox((5.2, 8.6, 5.2), (10.8, 10.6, 10.8), _faces(B_PLAT, up=B_GOLD_RUNE)),
            _tbox((6.6, 10.6, 6.6), (9.4, 12.6, 9.4), B_BRASS_COIL),
            _tbox((5.8, 12.6, 7.1), (10.2, 14.3, 8.9), B_VENT),
            _tbox((4.2, 11.4, 7.6), (6.6, 12.2, 8.4), B_GOLD_RUNE),
            _tbox((10.2, 8.4, 7.4), (10.8, 13.6, 8.6), B_PLAT, rotation=_rot((10.2, 13.6, 8.0), "z", 22.5)),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    # 5 Unique Vials (no shared template)
    # 1) spore_antidote: compact medical ampoule with horizontal platinum band & pressure ring cap
    models["spore_antidote"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/spore_antidote"),
        "elements": [
            _tbox((5.2, 1.0, 5.2), (10.8, 2.4, 10.8), _faces(B_OBS_COIL, up=B_PLAT)),
            _tbox((5.6, 2.4, 5.6), (10.4, 4.4, 10.4), B_VENOM, shade=False),
            _tbox((5.2, 4.4, 5.2), (10.8, 5.6, 10.8), _faces(B_PLAT, north=B_IVORY, south=B_IVORY)),
            _tbox((6.6, 4.6, 4.8), (9.4, 5.4, 5.2), B_IVORY, shade=False),
            _tbox((5.6, 5.6, 5.6), (10.4, 8.2, 10.4), B_VENOM, shade=False),
            _tbox((6.6, 8.2, 6.6), (9.4, 10.0, 9.4), B_BRASS_COIL),
            _tbox((6.0, 10.0, 6.0), (10.0, 12.0, 10.0), B_OBS_COIL),
            _tbox((7.2, 12.0, 7.4), (8.8, 13.4, 8.6), B_PLAT),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 2) concentrated_antidote: twin-chamber pressurized flask with mixer header & purge valve
    models["concentrated_antidote"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/concentrated_antidote"),
        "elements": [
            _tbox((4.0, 1.2, 5.6), (12.0, 2.6, 10.4), _faces(B_OBS_COIL, up=B_GOLD_RUNE)),
            _tbox((4.6, 2.6, 6.2), (7.2, 9.4, 9.8), B_AQUA, shade=False),
            _tbox((8.8, 2.6, 6.2), (11.4, 9.4, 9.8), B_AQUA, shade=False),
            _tbox((7.2, 5.2, 7.2), (8.8, 6.8, 8.8), B_PLAT),
            _tbox((4.2, 9.4, 5.8), (11.8, 11.2, 10.2), _faces(B_GOLD_RUNE, north=B_GLASS_CYAN, south=B_GLASS_CYAN)),
            _tbox((6.4, 11.2, 6.4), (9.6, 12.8, 9.6), B_PLAT),
            _tbox((7.3, 12.8, 7.3), (8.7, 14.4, 8.7), B_BRASS_COIL),
            _tbox((6.2, 13.4, 7.6), (7.3, 14.2, 8.4), B_VENT),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 3) counter_serum: reactive centrifuge capsule suspended inside an octagonal gyro ring
    models["counter_serum"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/counter_serum"),
        "elements": [
            _tbox((5.0, 1.0, 5.0), (11.0, 2.4, 11.0), _faces(B_OBS_COIL, up=B_PLAT)),
            _tbox((6.8, 2.4, 6.8), (9.2, 4.2, 9.2), B_BRASS_COIL),
            _tbox((5.8, 4.2, 5.8), (10.2, 9.8, 10.2), B_CHARTREUSE, shade=False),
            _tbox((3.6, 6.2, 7.0), (5.8, 7.8, 9.0), B_PLAT),
            _tbox((10.2, 6.2, 7.0), (12.4, 7.8, 9.0), B_PLAT),
            _tbox((3.8, 7.8, 7.4), (5.0, 11.2, 8.6), B_BRASS_COIL, rotation=_rot((3.8, 7.8, 8.0), "z", 22.5)),
            _tbox((11.0, 7.8, 7.4), (12.2, 11.2, 8.6), B_BRASS_COIL, rotation=_rot((12.2, 7.8, 8.0), "z", -22.5)),
            _tbox((5.6, 9.8, 5.6), (10.4, 11.4, 10.4), _faces(B_PLAT, north=B_CHARTREUSE, up=B_GOLD_RUNE)),
            _tbox((10.4, 9.9, 7.3), (11.6, 11.2, 8.7), B_BRASS_COIL),
            _tbox((6.8, 11.4, 6.8), (9.2, 13.4, 9.2), B_BONE),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 4) genetic_sample: cryo-biopsy tube with 4-step alternating double helix core & reader cap
    models["genetic_sample"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/genetic_sample"),
        "elements": [
            _tbox((5.2, 0.8, 5.2), (10.8, 2.2, 10.8), _faces(B_OBS_COIL, up=B_CYAN_COIL)),
            _tbox((4.6, 2.2, 7.3), (5.4, 11.8, 8.7), B_PLAT),
            _tbox((10.6, 2.2, 7.3), (11.4, 11.8, 8.7), B_PLAT),
            _tbox((6.0, 2.2, 6.6), (8.6, 4.6, 9.4), B_AMETHYST, shade=False),
            _tbox((7.4, 4.6, 6.6), (10.0, 7.0, 9.4), B_CYAN_COIL, shade=False),
            _tbox((6.0, 7.0, 6.6), (8.6, 9.4, 9.4), B_AMETHYST, shade=False),
            _tbox((7.4, 9.4, 6.6), (10.0, 11.8, 9.4), B_CYAN_COIL, shade=False),
            _tbox((4.6, 11.8, 5.8), (11.4, 13.4, 10.2), _faces(B_PLAT, north=B_GLASS_CYAN, up=B_OBS_COIL)),
            _tbox((6.8, 13.4, 6.8), (9.2, 15.0, 9.2), B_BRASS_COIL),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 5) symbiotic_mutagen: embryonic mutagenic reactor clasped by chitinous claws & inoculation needle
    models["symbiotic_mutagen"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/symbiotic_mutagen"),
        "elements": [
            _tbox((5.4, 1.0, 5.4), (10.6, 2.4, 10.6), B_OBS_COIL),
            _tbox((5.0, 2.4, 5.0), (11.0, 3.6, 11.0), _faces(B_GOLD_RUNE, up=B_EMBER)),
            _tbox((5.4, 3.6, 5.4), (10.6, 9.6, 10.6), B_EMBER, shade=False),
            _tbox((10.6, 4.8, 6.2), (11.8, 8.2, 9.8), B_CHARTREUSE, shade=False),
            _tbox((3.8, 3.6, 7.3), (5.0, 10.4, 8.7), B_BONE, rotation=_rot((5.0, 3.6, 8.0), "z", 22.5)),
            _tbox((7.3, 3.6, 11.0), (8.7, 10.4, 12.2), B_BONE, rotation=_rot((8.0, 3.6, 11.0), "x", 22.5)),
            _tbox((5.6, 9.6, 5.6), (10.4, 11.2, 10.4), _faces(B_GOLD_RUNE, up=B_EMBER)),
            _tbox((6.8, 11.2, 6.8), (9.2, 13.0, 9.2), B_BONE),
            _tbox((7.5, 13.0, 7.5), (8.5, 15.2, 8.5), B_EMBER, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 4 Unique Cores (no shared template)
    # 1) synaptic_core: living synaptic ganglion on bone cradle with curved ribs & 4 upper axons
    models["synaptic_core"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/synaptic_core"),
        "elements": [
            _tbox((5.0, 2.0, 4.8), (11.0, 3.8, 11.2), _faces(R_BONE_SCALE, up=R_VIOLET_VEIN)),
            _tbox((5.4, 3.8, 5.4), (10.6, 9.8, 10.6), R_AMETHYST, shade=False),
            _tbox((3.8, 3.8, 6.8), (5.0, 10.6, 9.2), R_BONE_SCALE, rotation=_rot((5.0, 3.8, 8.0), "z", 22.5)),
            _tbox((11.0, 3.8, 6.8), (12.2, 10.6, 9.2), R_BONE_SCALE, rotation=_rot((11.0, 3.8, 8.0), "z", -22.5)),
            _tbox((5.6, 9.8, 5.6), (7.0, 13.8, 7.0), R_GOLD_CRYSTAL, shade=False),
            _tbox((9.0, 9.8, 5.6), (10.4, 13.8, 7.0), R_VIOLET_VEIN, shade=False),
            _tbox((5.6, 9.8, 9.0), (7.0, 13.8, 10.4), R_VIOLET_VEIN, shade=False),
            _tbox((9.0, 9.8, 9.0), (10.4, 13.8, 10.4), R_GOLD_CRYSTAL, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["hive_heart"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/hive_heart"),
        "elements": [
            # Organic asymmetrical biomechanical heart: root -> main chamber -> lateral ventricle -> chitin ribs -> angled aortic valves
            _tbox((6.8, 1.0, 6.8), (9.2, 2.2, 9.2), R_OBS_RUNE),
            _tbox((6.2, 2.2, 6.2), (9.8, 3.5, 9.8), R_GOLD_CRYSTAL, shade=False),
            _tbox((5.0, 3.5, 5.0), (11.0, 11.0, 11.0), R_EMBER, shade=False),
            _tbox((11.0, 4.5, 5.8), (12.5, 9.5, 10.2), R_CHARTREUSE, shade=False),
            _tbox((4.1, 4.6, 5.4), (5.0, 9.8, 10.6), R_VIOLET_VEIN),
            _tbox((5.6, 5.2, 4.2), (10.4, 9.2, 5.0), _faces(R_OBS_SCALE, north=R_OBS_RUNE)),
            _tbox((5.6, 5.2, 11.0), (10.4, 9.2, 11.8), _faces(R_OBS_SCALE, south=R_OBS_RUNE)),
            _tbox((5.6, 11.0, 6.4), (7.6, 14.4, 8.8), R_OBS_RUNE, rotation=_rot((5.6, 11.0, 7.6), "z", 22.5)),
            _tbox((8.8, 11.0, 7.2), (10.6, 13.8, 9.4), R_GOLD_RUNE, rotation=_rot((10.6, 11.0, 8.3), "z", -22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 2) neural_echo_core: perpendicular dual-ring gyroscope around a cyan echo sphere
    models["neural_echo_core"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/neural_echo_core"),
        "elements": [
            _tbox((6.0, 6.0, 6.0), (10.0, 10.0, 10.0), R_CYAN, shade=False),
            _tbox((4.2, 7.4, 7.4), (6.0, 8.6, 8.6), R_PLAT),
            _tbox((10.0, 7.4, 7.4), (11.8, 8.6, 8.6), R_PLAT),
            _tbox((7.4, 4.4, 7.4), (8.6, 6.0, 8.6), R_BRASS),
            _tbox((7.4, 10.0, 7.4), (8.6, 11.6, 8.6), R_BRASS),
            # Horizontal equatorial platinum ring
            _tbox((3.2, 7.2, 4.2), (4.2, 8.8, 11.8), R_PLAT),
            _tbox((11.8, 7.2, 4.2), (12.8, 8.8, 11.8), R_PLAT),
            _tbox((3.2, 7.2, 3.2), (12.8, 8.8, 4.2), R_PLAT),
            _tbox((3.2, 7.2, 11.8), (12.8, 8.8, 12.8), R_PLAT),
            # Vertical meridian brass ring outside equatorial Z-bounds + polar plates
            _tbox((7.2, 4.4, 2.2), (8.8, 11.6, 3.2), R_BRASS),
            _tbox((7.2, 4.4, 12.8), (8.8, 11.6, 13.8), R_BRASS),
            _tbox((7.2, 3.2, 2.2), (8.8, 4.4, 13.8), _faces(R_BRASS, down=R_PLAT)),
            _tbox((7.2, 11.6, 2.2), (8.8, 12.8, 13.8), _faces(R_BRASS, up=R_PLAT)),
            _tbox((7.5, 12.8, 7.5), (8.5, 14.8, 8.5), R_CYAN, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 3) overmind_essence: imperial chalice reliquary with 3-tier golden diamond & winged volutes
    models["overmind_essence"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/overmind_essence"),
        "elements": [
            _tbox((5.2, 0.8, 5.2), (10.8, 2.2, 10.8), _faces(R_OBS_RUNE, up=R_GOLD_RUNE)),
            _tbox((7.0, 2.2, 7.0), (9.0, 4.6, 9.0), R_GOLD_RUNE),
            _tbox((5.4, 4.6, 5.4), (10.6, 6.0, 10.6), _faces(R_OBS_RUNE, up=R_GOLD_RUNE)),
            _tbox((5.8, 6.0, 5.8), (10.2, 8.2, 10.2), R_GOLD_CRYSTAL, shade=False),
            _tbox((6.4, 8.2, 6.4), (9.6, 10.4, 9.6), R_GOLD_CRYSTAL, shade=False),
            _tbox((7.2, 10.4, 7.2), (8.8, 12.2, 8.8), R_GOLD_CRYSTAL, shade=False),
            _tbox((3.8, 6.0, 7.2), (5.4, 11.4, 8.8), _faces(R_GOLD_RUNE, north=R_OBS_RUNE, south=R_OBS_RUNE), rotation=_rot((5.4, 6.0, 8.0), "z", 22.5)),
            _tbox((10.6, 6.0, 7.2), (12.2, 11.4, 8.8), _faces(R_GOLD_RUNE, north=R_OBS_RUNE, south=R_OBS_RUNE), rotation=_rot((10.6, 6.0, 8.0), "z", -22.5)),
            _tbox((6.4, 12.2, 6.4), (9.6, 13.6, 9.6), _faces(R_OBS_RUNE, up=R_GOLD_RUNE)),
            _tbox((7.5, 13.6, 7.5), (8.5, 15.2, 8.5), R_GOLD_CRYSTAL, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 4) rift_catalyst: abyssal monolith gate framing an incandescent dimensional tear
    models["rift_catalyst"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/rift_catalyst"),
        "elements": [
            _tbox((3.8, 0.8, 4.8), (12.2, 2.4, 11.2), _faces(R_OBS_RUNE, up=R_VIOLET_VEIN)),
            _tbox((4.0, 2.4, 6.4), (6.0, 12.4, 9.6), _faces(R_OBS_RUNE, north=R_VIOLET_VEIN, south=R_VIOLET_VEIN)),
            _tbox((10.0, 2.4, 6.4), (12.0, 12.4, 9.6), _faces(R_OBS_RUNE, north=R_VIOLET_VEIN, south=R_VIOLET_VEIN)),
            _tbox((6.8, 2.4, 7.2), (9.2, 12.4, 8.8), R_EMBER, shade=False),
            _tbox((7.1, 3.6, 8.8), (8.9, 11.2, 9.5), R_VIOLET_VEIN, shade=False),
            _tbox((3.8, 12.4, 6.0), (12.2, 14.0, 10.0), _faces(R_OBS_RUNE, up=R_EMBER)),
            _tbox((2.6, 14.0, 7.2), (3.8, 15.8, 8.8), R_EMBER, rotation=_rot((3.8, 14.0, 8.0), "z", 22.5), shade=False),
            _tbox((12.2, 14.0, 7.2), (13.4, 15.8, 8.8), R_EMBER, rotation=_rot((12.2, 14.0, 8.0), "z", -22.5), shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["synaptic_graft"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/synaptic_graft"),
        "elements": [
            _tbox((5.4, 1.2, 5.4), (10.6, 2.8, 10.6), _faces(B_PLAT, up=B_BRASS_COIL)),
            _tbox((6.4, 2.8, 6.4), (9.6, 5.2, 9.6), B_OBS_COIL),
            _tbox((5.2, 5.2, 5.2), (10.8, 10.0, 10.8), _faces(B_PLAT, east=B_VENT, west=B_VENT)),
            _tbox((6.2, 6.2, 4.6), (9.8, 9.0, 5.2), B_AQUA, shade=False),
            _tbox((6.0, 10.0, 6.0), (10.0, 11.0, 10.0), B_BRASS_COIL),
            _tbox((5.2, 11.0, 7.3), (6.4, 14.8, 8.7), B_VENOM, rotation=_rot((5.2, 11.0, 8.0), "z", 22.5), shade=False),
            _tbox((9.6, 11.0, 7.3), (10.8, 14.8, 8.7), B_VENOM, rotation=_rot((10.8, 11.0, 8.0), "z", -22.5), shade=False),
            _tbox((7.3, 11.0, 5.2), (8.7, 14.4, 6.4), B_AQUA, shade=False),
            _tbox((7.3, 11.0, 9.6), (8.7, 14.4, 10.8), B_VENOM, shade=False),
        ],
        "display": _RELIC_DISPLAY,
    }

    # 2) schism_injector: heavy dual-chamber syringe with twin needles to induce faction schism
    models["schism_injector"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/schism_injector"),
        "elements": [
            _tbox((6.0, 0.5, 6.8), (10.0, 1.8, 9.2), _faces(B_OBS_COIL, up=B_INDIGO)),
            _tbox((7.1, 1.8, 7.1), (8.9, 4.4, 8.9), B_INDIGO),
            _tbox((4.6, 4.4, 6.0), (11.4, 5.6, 10.0), _faces(B_OBS_COIL, up=B_GOLD_RUNE)),
            _tbox((5.0, 5.6, 6.4), (7.2, 10.2, 9.6), B_EMBER, shade=False),
            _tbox((8.8, 5.6, 6.4), (11.0, 10.2, 9.6), B_EMBER, shade=False),
            _tbox((7.2, 6.6, 7.0), (8.8, 9.2, 9.0), B_BRASS_COIL),
            _tbox((4.6, 10.2, 6.0), (11.4, 11.4, 10.0), B_GOLD_RUNE),
            _tbox((5.4, 11.4, 7.2), (6.8, 12.8, 8.8), B_VENT),
            _tbox((5.8, 12.8, 7.6), (6.4, 15.8, 8.4), B_EMBER, shade=False),
            _tbox((9.2, 11.4, 7.2), (10.6, 12.8, 8.8), B_VENT),
            _tbox((9.6, 12.8, 7.6), (10.2, 15.8, 8.4), B_EMBER, shade=False),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    # 3) false_engram_injector: slender psionic lancet with 3 stepped resonator coils & emitter tip
    models["false_engram_injector"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/false_engram_injector"),
        "elements": [
            _tbox((6.4, 0.5, 6.4), (9.6, 1.8, 9.6), _faces(B_GOLD_RUNE, up=B_INDIGO)),
            _tbox((7.3, 1.8, 7.3), (8.7, 5.0, 8.7), B_INDIGO),
            _tbox((6.4, 5.0, 6.4), (9.6, 10.2, 9.6), B_AMETHYST, shade=False),
            _tbox((4.8, 5.4, 7.2), (6.4, 6.4, 8.8), B_GOLD_RUNE),
            _tbox((9.6, 5.4, 7.2), (11.2, 6.4, 8.8), B_GOLD_RUNE),
            _tbox((4.8, 7.1, 7.2), (6.4, 8.1, 8.8), B_INDIGO),
            _tbox((9.6, 7.1, 7.2), (11.2, 8.1, 8.8), B_INDIGO),
            _tbox((4.8, 8.8, 7.2), (6.4, 9.8, 8.8), B_GOLD_RUNE),
            _tbox((9.6, 8.8, 7.2), (11.2, 9.8, 8.8), B_GOLD_RUNE),
            _tbox((6.8, 10.2, 6.8), (9.2, 11.6, 9.2), B_INDIGO),
            _tbox((7.6, 11.6, 7.6), (8.4, 15.0, 8.4), B_PLAT),
            _tbox((7.2, 15.0, 7.2), (8.8, 16.0, 8.8), B_AMETHYST, shade=False),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    models["truce_pheromone"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/truce_pheromone"),
        "elements": [
            _tbox((4.7, 1.2, 4.7), (11.3, 2.4, 11.3), _faces(B_PLAT, down=B_OBS_COIL)),
            _tbox((5.2, 2.4, 5.2), (10.8, 4.4, 10.8), B_VENOM, shade=False),
            _tbox((4.7, 4.4, 4.7), (11.3, 5.8, 11.3), _faces(B_GOLD_RUNE, north=B_GLASS_CYAN, south=B_GLASS_CYAN)),
            _tbox((5.2, 5.8, 5.2), (10.8, 8.4, 10.8), B_VENOM, shade=False),
            _tbox((4.1, 2.4, 7.2), (4.7, 8.4, 8.8), B_PLAT),
            _tbox((11.3, 2.4, 7.2), (11.9, 8.4, 8.8), B_PLAT),
            _tbox((6.4, 8.4, 6.4), (9.6, 10.8, 9.6), B_PLAT),
            _tbox((5.6, 10.8, 7.0), (10.4, 12.8, 9.0), B_BRASS_COIL),
            _tbox((10.4, 11.2, 7.3), (12.8, 12.6, 8.7), B_VENT),
            _tbox((4.2, 10.2, 7.4), (5.6, 12.0, 8.6), B_OBS_COIL),
        ],
        "display": _TOOL_INJECTOR_DISPLAY,
    }

    models["quorum_seal"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/quorum_seal"),
        "elements": [
            _tbox((4.4, 4.6, 6.8), (11.6, 11.4, 9.2), _faces(R_GOLD_RUNE, east=R_BRASS, west=R_BRASS)),
            _tbox((5.4, 11.4, 7.0), (10.6, 12.6, 9.0), R_GOLD_RUNE),
            _tbox((7.0, 12.6, 7.4), (9.0, 14.2, 8.6), R_GOLD_CRYSTAL, shade=False),
            _tbox((3.4, 5.8, 7.1), (4.4, 10.2, 8.9), _faces(R_OBS_RUNE, north=R_PLAT, south=R_PLAT)),
            _tbox((11.6, 5.8, 7.1), (12.6, 10.2, 8.9), _faces(R_OBS_RUNE, north=R_PLAT, south=R_PLAT)),
            _tbox((6.1, 6.1, 5.9), (9.9, 9.9, 6.8), R_AMETHYST, shade=False),
            _tbox((6.1, 6.1, 9.2), (9.9, 9.9, 10.1), R_AMETHYST, shade=False),
            _tbox((5.6, 1.0, 7.3), (7.2, 4.6, 8.7), R_VIOLET_VEIN, rotation=_rot((5.6, 4.6, 8.0), "z", -22.5)),
            _tbox((8.8, 1.0, 7.3), (10.4, 4.6, 8.7), R_VIOLET_VEIN, rotation=_rot((10.4, 4.6, 8.0), "z", 22.5)),
        ],
        "display": _RELIC_DISPLAY,
    }

    models["volatile_engram"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/volatile_engram"),
        "elements": [
            _tbox((5.6, 5.6, 5.6), (10.4, 10.4, 10.4), R_CYAN, shade=False),
            _tbox((6.4, 4.0, 6.4), (9.6, 5.6, 9.6), _faces(R_OBS_RUNE, north=R_CYAN, south=R_CYAN)),
            _tbox((7.3, 2.0, 7.3), (8.7, 4.0, 8.7), R_GOLD_CRYSTAL, shade=False),
            _tbox((6.4, 10.4, 6.4), (9.6, 12.0, 9.6), _faces(R_OBS_RUNE, north=R_CYAN, south=R_CYAN)),
            _tbox((7.3, 12.0, 7.3), (8.7, 14.0, 8.7), R_GOLD_CRYSTAL, shade=False),
            _tbox((4.2, 6.4, 6.8), (5.6, 9.6, 9.2), R_AMETHYST, shade=False),
            _tbox((3.4, 7.2, 7.4), (4.2, 8.8, 8.6), R_GOLD_RUNE),
            _tbox((10.4, 6.4, 6.8), (11.8, 9.6, 9.2), R_AMETHYST, shade=False),
            _tbox((11.8, 7.2, 7.4), (12.6, 8.8, 8.6), R_GOLD_RUNE),
            _tbox((6.8, 6.4, 4.2), (9.2, 9.6, 5.6), R_GOLD_CRYSTAL, shade=False),
            _tbox((7.4, 7.2, 3.4), (8.6, 8.8, 4.2), R_OBS_RUNE),
            _tbox((6.8, 6.4, 10.4), (9.2, 9.6, 11.8), R_GOLD_CRYSTAL, shade=False),
            _tbox((7.4, 7.2, 11.8), (8.6, 8.8, 12.6), R_OBS_RUNE),
        ],
        "display": _RELIC_DISPLAY,
    }

    # === 3 Sculpted 3D Biological Materials (35 total 3D items) ===
    models["mycelial_biomass"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/mycelial_biomass"),
        "elements": [
            _tbox((4.8, 1.8, 4.8), (11.2, 3.6, 11.2), _faces(R_INDIGO, up=R_VIOLET_VEIN)),
            _tbox((3.4, 2.0, 6.2), (4.8, 3.4, 9.8), R_VIOLET_VEIN),
            _tbox((4.8, 3.6, 4.8), (9.2, 9.0, 9.6), _faces(R_VIOLET_VEIN, up=R_CHARTREUSE)),
            _tbox((9.2, 3.6, 6.0), (12.4, 7.6, 10.4), R_AMETHYST, shade=False),
            _tbox((5.6, 3.6, 9.6), (9.2, 7.0, 12.2), R_VIOLET_VEIN),
            _tbox((5.6, 9.0, 5.8), (8.0, 10.8, 8.2), R_CHARTREUSE, shade=False),
            _tbox((10.0, 7.6, 7.2), (11.6, 9.2, 9.0), R_CHARTREUSE, shade=False),
            _tbox((2.8, 3.4, 6.8), (4.4, 5.4, 8.8), R_CHARTREUSE, shade=False),
        ],
        "display": _MATERIAL_COMPACT_DISPLAY,
    }

    models["chitin_plate"] = {
        "textures": _atlas_textures("atlas_items_armor_relics", "mycelialmind:item/chitin_plate"),
        "elements": [
            _tbox((3.0, 2.2, 6.2), (13.0, 5.0, 9.8), _faces(R_OBS_SCALE, north=R_BONE_SCALE, south=R_INDIGO)),
            _tbox((3.8, 5.0, 6.5), (12.2, 8.2, 9.5), _faces(R_OBS_SCALE, north=R_OBS_RUNE)),
            _tbox((4.8, 8.2, 6.8), (11.2, 11.0, 9.2), _faces(R_OBS_SCALE, up=R_BONE_SCALE)),
            _tbox((7.0, 3.0, 5.4), (9.0, 12.2, 6.2), R_BONE_SCALE),
            _tbox((2.2, 6.2, 7.3), (3.8, 10.2, 8.7), R_BONE_SCALE, rotation=_rot((3.8, 6.2, 8.0), "z", 22.5)),
            _tbox((12.2, 6.2, 7.3), (13.8, 10.2, 8.7), R_BONE_SCALE, rotation=_rot((12.2, 6.2, 8.0), "z", -22.5)),
        ],
        "display": _MATERIAL_COMPACT_DISPLAY,
    }

    models["latent_spore"] = {
        "textures": _atlas_textures("atlas_items_biotech", "mycelialmind:item/latent_spore"),
        "elements": [
            _tbox((4.6, 1.4, 4.6), (11.4, 3.6, 11.4), _faces(B_BONE, up=B_VIOLET)),
            _tbox((5.0, 3.6, 5.0), (11.0, 9.6, 11.0), B_AMETHYST, shade=False),
            _tbox((6.2, 9.6, 6.2), (9.8, 11.8, 9.8), B_CHARTREUSE, shade=False),
            _tbox((7.3, 11.8, 7.3), (8.7, 13.6, 8.7), B_AMETHYST, shade=False),
            _tbox((3.4, 3.6, 7.4), (4.6, 10.0, 8.6), B_CHARTREUSE, rotation=_rot((4.6, 3.6, 8.0), "z", 22.5), shade=False),
            _tbox((11.4, 3.6, 7.4), (12.6, 10.0, 8.6), B_CHARTREUSE, rotation=_rot((11.4, 3.6, 8.0), "z", -22.5), shade=False),
            _tbox((7.4, 3.6, 11.4), (8.6, 10.0, 12.6), B_AMETHYST, rotation=_rot((8.0, 3.6, 11.4), "x", 22.5), shade=False),
        ],
        "display": _MATERIAL_COMPACT_DISPLAY,
    }

    return models


def emit_block_and_item_3d_models(check_only: bool = False) -> Tuple[int, int]:
    block_models = build_3d_block_models()
    item_models = build_3d_item_models()

    if not check_only:
        os.makedirs(BLOCK_MODEL_DIR, exist_ok=True)
        os.makedirs(ITEM_MODEL_DIR, exist_ok=True)
        for name, payload in block_models.items():
            out_path = os.path.join(BLOCK_MODEL_DIR, f"{name}.json")
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(payload, f, indent=2)
        for name, payload in item_models.items():
            out_path = os.path.join(ITEM_MODEL_DIR, f"{name}.json")
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(payload, f, indent=2)

    return len(block_models), len(item_models)
