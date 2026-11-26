"""Deterministic NBT structure generator for Mycelial Mind's Gothic-Organic 10-piece hive_sanctum Jigsaw complex."""

from __future__ import annotations

import gzip
import math
import os
import struct
from typing import Any

DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11


class NbtWriter:
    def __init__(self) -> None:
        self.buf = bytearray()

    def _str(self, s: str) -> None:
        b = s.encode("utf-8")
        self.buf.extend(struct.pack(">H", len(b)))
        self.buf.extend(b)

    def write_tag(self, tag_id: int, name: str | None, value: Any) -> None:
        self.buf.append(tag_id)
        if name is not None:
            self._str(name)
        self.write_payload(tag_id, value)

    def write_payload(self, tag_id: int, value: Any) -> None:
        if tag_id == TAG_BYTE:
            self.buf.extend(struct.pack(">b", value))
        elif tag_id == TAG_SHORT:
            self.buf.extend(struct.pack(">h", value))
        elif tag_id == TAG_INT:
            self.buf.extend(struct.pack(">i", value))
        elif tag_id == TAG_LONG:
            self.buf.extend(struct.pack(">q", value))
        elif tag_id == TAG_FLOAT:
            self.buf.extend(struct.pack(">f", value))
        elif tag_id == TAG_DOUBLE:
            self.buf.extend(struct.pack(">d", value))
        elif tag_id == TAG_STRING:
            self._str(value)
        elif tag_id == TAG_LIST:
            elem_type, items = value
            self.buf.append(elem_type)
            self.buf.extend(struct.pack(">i", len(items)))
            for item in items:
                self.write_payload(elem_type, item)
        elif tag_id == TAG_COMPOUND:
            for k, (tid, val) in value.items():
                self.write_tag(tid, k, val)
            self.buf.append(TAG_END)
        elif tag_id == TAG_INT_ARRAY:
            self.buf.extend(struct.pack(">i", len(value)))
            for v in value:
                self.buf.extend(struct.pack(">i", v))
        else:
            raise ValueError(f"Unsupported NBT tag: {tag_id}")


class StructureBuilder:
    def __init__(self, sx: int, sy: int, sz: int) -> None:
        self.sx = sx
        self.sy = sy
        self.sz = sz
        self.grid: dict[tuple[int, int, int], tuple[str, dict[str, str], dict[str, Any] | None]] = {}
        self.fill(0, 0, 0, sx - 1, sy - 1, sz - 1, "minecraft:air")

    def set(
        self,
        x: int,
        y: int,
        z: int,
        block: str,
        props: dict[str, str] | None = None,
        nbt: dict[str, Any] | None = None,
    ) -> None:
        if 0 <= x < self.sx and 0 <= y < self.sy and 0 <= z < self.sz:
            self.grid[(x, y, z)] = (block, props or {}, nbt)

    def fill(
        self,
        x1: int,
        y1: int,
        z1: int,
        x2: int,
        y2: int,
        z2: int,
        block: str,
        props: dict[str, str] | None = None,
    ) -> None:
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, block, props)

    def stair(self, x: int, y: int, z: int, facing: str, half: str = "bottom") -> None:
        self.set(
            x, y, z,
            "minecraft:deepslate_brick_stairs",
            {"facing": facing, "half": half, "shape": "straight", "waterlogged": "false"},
        )

    def jigsaw(self, x: int, y: int, z: int, facing: str, pool: str, name: str = "mycelialmind:corridor_socket") -> None:
        nbt = {
            "id": (TAG_STRING, "minecraft:jigsaw"),
            "name": (TAG_STRING, name),
            "target": (TAG_STRING, name),
            "pool": (TAG_STRING, pool),
            "final_state": (TAG_STRING, "minecraft:air"),
            "joint": (TAG_STRING, "aligned"),
            "selection_priority": (TAG_INT, 0),
            "placement_priority": (TAG_INT, 0),
        }
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": f"{facing}_up"}, nbt)

    def chest(
        self,
        x: int,
        y: int,
        z: int,
        facing: str,
        items: list[tuple[int, str, int]],
        loot_table: str | None = None,
    ) -> None:
        if loot_table:
            nbt: dict[str, Any] = {
                "id": (TAG_STRING, "minecraft:chest"),
                "LootTable": (TAG_STRING, loot_table),
            }
        else:
            item_compounds = [
                {
                    "Slot": (TAG_BYTE, slot),
                    "id": (TAG_STRING, item_id),
                    "count": (TAG_INT, count),
                }
                for slot, item_id, count in items
            ]
            nbt = {
                "id": (TAG_STRING, "minecraft:chest"),
                "Items": (TAG_LIST, (TAG_COMPOUND, item_compounds)),
            }
        self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"}, nbt)

    def to_nbt_bytes(self) -> bytes:
        palette_index: dict[tuple[str, tuple[tuple[str, str], ...]], int] = {}
        palette: list[dict[str, Any]] = []
        blocks: list[dict[str, Any]] = []

        for (x, y, z), (name, props, nbt) in sorted(self.grid.items()):
            key = (name, tuple(sorted(props.items())))
            if key not in palette_index:
                palette_index[key] = len(palette)
                entry: dict[str, Any] = {"Name": (TAG_STRING, name)}
                if props:
                    entry["Properties"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in props.items()})
                palette.append(entry)
            b_entry: dict[str, Any] = {
                "pos": (TAG_LIST, (TAG_INT, [x, y, z])),
                "state": (TAG_INT, palette_index[key]),
            }
            if nbt is not None:
                b_entry["nbt"] = (TAG_COMPOUND, nbt)
            blocks.append(b_entry)

        root = {
            "DataVersion": (TAG_INT, DATA_VERSION),
            "size": (TAG_LIST, (TAG_INT, [self.sx, self.sy, self.sz])),
            "palette": (TAG_LIST, (TAG_COMPOUND, palette)),
            "blocks": (TAG_LIST, (TAG_COMPOUND, blocks)),
            "entities": (TAG_LIST, (TAG_COMPOUND, [])),
        }
        writer = NbtWriter()
        writer.write_tag(TAG_COMPOUND, "", root)
        return gzip.compress(bytes(writer.buf))


def build_sanctum_atrium() -> StructureBuilder:
    s = StructureBuilder(23, 21, 23)
    cx, cz = 11, 11
    for y in range(21):
        for x in range(23):
            for z in range(23):
                dx, dz = x - cx, z - cz
                dist = (dx * dx + dz * dz) ** 0.5
                # Lobed octagonal-fungal footprint (eliminates boxy square corners)
                in_lobe = (abs(dx) <= 3 and abs(dz) <= 11) or (abs(dz) <= 3 and abs(dx) <= 11)
                effective_r = 11.4 if in_lobe else 9.8
                if y == 0:
                    if dist <= effective_r:
                        s.set(x, y, z, "mycelialmind:creeping_mycelium" if (abs(dx) + abs(dz)) % 4 == 0 else "minecraft:deepslate_tiles")
                elif y <= 11:
                    if effective_r - 1.25 <= dist <= effective_r + 0.55:
                        if in_lobe and y in (5, 6, 7, 8) and (abs(dx) == 0 or abs(dz) == 0):
                            s.set(x, y, z, "minecraft:purple_stained_glass")
                        elif y in (6, 7) and (abs(dx) == 5 or abs(dz) == 5):
                            s.set(x, y, z, "mycelialmind:bio_chitin_lattice")
                        elif y == 10 and (abs(dx) % 2 == 0 or abs(dz) % 2 == 0):
                            s.set(x, y, z, "mycelialmind:luminescent_amber_vein")
                        elif abs(abs(dx) - abs(dz)) <= 1:
                            s.set(x, y, z, "mycelialmind:chitin_pillar", {"axis": "y"})
                        else:
                            s.set(x, y, z, "mycelialmind:mycelial_arch_brick")
                else:
                    # Stepped overhanging fungal cap dome (y = 12..20)
                    t = (y - 12) / 8.0
                    r_limit = max(1.6, 11.2 * (1.0 - t * 0.85))
                    if r_limit - 1.4 <= dist <= r_limit + 0.7:
                        if y >= 19:
                            s.set(x, y, z, "mycelialmind:luminescent_amber_vein" if dist <= 1.8 else "minecraft:amethyst_block")
                        elif y == 12 and dist >= 9.4:
                            s.set(x, y, z, "minecraft:amethyst_block" if (abs(dx) + abs(dz)) % 3 == 0 else "minecraft:crying_obsidian")
                        elif abs(dx) <= 1 or abs(dz) <= 1:
                            s.set(x, y, z, "mycelialmind:mycelial_arch_brick")
                        else:
                            s.set(x, y, z, "minecraft:deepslate_tiles" if (x + z) % 2 == 0 else "mycelialmind:mycelial_stone")

    # 4 monumental chitin columns + 6-connected coiling organic tentacles
    for px, sx in ((6, -1), (16, 1)):
        for pz, sz in ((6, -1), (16, 1)):
            for y in range(1, 14):
                s.set(px, y, pz, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(px, 14, pz, "mycelialmind:luminescent_amber_vein")
            s.set(px + sx, 4, pz, "mycelialmind:mycelial_arch_brick")
            s.set(px + sx, 5, pz, "mycelialmind:mycelial_arch_brick")
            s.set(px + sx, 5, pz + sz, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(px, 5, pz + sz, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(px, 6, pz + sz, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(px, 7, pz + sz, "minecraft:amethyst_block")
            for ox, oz, f in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
                s.stair(px + ox, 13, pz + oz, f, "top")

    # 4 hanging chain chandeliers welded to dome at y = 16
    for lx, lz in ((7, 7), (15, 7), (7, 15), (15, 15)):
        s.set(lx, 16, lz, "mycelialmind:mycelial_arch_brick")
        s.set(lx, 15, lz, "minecraft:chain")
        s.set(lx, 14, lz, "minecraft:chain")
        s.set(lx, 13, lz, "mycelialmind:luminescent_amber_vein")

    # 2 anatomical Chitin/Bone guardian effigies flanking the altar
    for ex, ez in ((cx - 4, cz), (cx + 4, cz)):
        s.set(ex, 1, ez, "minecraft:polished_basalt")
        s.set(ex, 2, ez, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.set(ex, 3, ez, "minecraft:bone_block", {"axis": "y"})
        s.set(ex, 4, ez, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.set(ex, 5, ez, "mycelialmind:luminescent_amber_vein")
        s.set(ex, 4, ez - 1, "minecraft:deepslate_brick_wall")
        s.set(ex, 4, ez + 1, "minecraft:deepslate_brick_wall")

    # Central raised dais & Hive Altar
    s.fill(9, 1, 9, 13, 1, 13, "mycelialmind:mycelial_arch_brick")
    s.fill(10, 2, 10, 12, 2, 12, "minecraft:bone_block", {"axis": "y"})
    s.set(cx, 3, cz, "mycelialmind:hive_altar")
    for ox, oz in ((9, 9), (13, 9), (9, 13), (13, 13)):
        s.set(ox, 2, oz, "mycelialmind:spore_vent")

    # Synaptic floor conduits from the 4 monumental columns to the central dais
    for px in (6, 16):
        for pz in (6, 16):
            cur_x, cur_z = px, pz
            while cur_x != cx:
                s.set(cur_x, 0, cur_z, "mycelialmind:luminescent_amber_vein")
                cur_x += 1 if cx > cur_x else -1
            while cur_z != cz:
                s.set(cur_x, 0, cur_z, "mycelialmind:luminescent_amber_vein")
                cur_z += 1 if cz > cur_z else -1

    # Continuous y = 6 triforium walkway ring connecting all 3 Sentinel balconies
    walkway_cells = set()
    for x in range(2, 21):
        for z in range(2, 21):
            dist = math.hypot(x - cx, z - cz)
            if 7.3 <= dist <= 9.3:
                s.set(x, 6, z, "mycelialmind:mycelial_arch_brick")
                walkway_cells.add((x, z))
    for x, z in walkway_cells:
        dist = math.hypot(x - cx, z - cz)
        if dist < 7.9:
            s.set(x, 7, z, "minecraft:deepslate_brick_wall")

    # 2 real staircases (y = 1..6) from arena floor up to the South-West and South-East triforium
    for step, sx_w in enumerate(range(10, 5, -1)):
        sy = step + 1
        for fill_y in range(1, sy):
            s.set(sx_w, fill_y, 18, "mycelialmind:mycelial_arch_brick")
        s.stair(sx_w, sy, 18, "west", "bottom")
        for clear_y in range(sy + 1, min(sy + 5, 11)):
            s.set(sx_w, clear_y, 18, "minecraft:air")
    for clear_y in range(7, 11):
        s.set(5, clear_y, 18, "minecraft:air")
    s.set(5, 7, 17, "minecraft:air")

    for step, sx_e in enumerate(range(12, 17)):
        sy = step + 1
        for fill_y in range(1, sy):
            s.set(sx_e, fill_y, 18, "mycelialmind:mycelial_arch_brick")
        s.stair(sx_e, sy, 18, "east", "bottom")
        for clear_y in range(sy + 1, min(sy + 5, 11)):
            s.set(sx_e, clear_y, 18, "minecraft:air")
    for clear_y in range(7, 11):
        s.set(17, clear_y, 18, "minecraft:air")
    s.set(17, 7, 17, "minecraft:air")

    # 3 Sentinel Nodes on elevated triforium alcoves connected to the walkway
    for sx, sy, sz in ((11, 7, 3), (3, 7, 11), (19, 7, 11)):
        s.set(sx, sy - 1, sz, "mycelialmind:mycelial_arch_brick")
        s.set(sx, sy, sz, "mycelialmind:sentinel_node")

    # 4 cardinal corridor doorways + Jigsaw sockets
    doorways = [
        (11, 0, "north", True),
        (11, 22, "south", True),
        (0, 11, "west", False),
        (22, 11, "east", False),
    ]
    for dx, dz, facing, z_edge in doorways:
        for w in range(-1, 2):
            for h in range(1, 5):
                if z_edge:
                    s.set(dx + w, h, dz, "minecraft:air")
                else:
                    s.set(dx, h, dz + w, "minecraft:air")
        s.jigsaw(dx, 1, dz, facing, "mycelialmind:hive_sanctum/corridors")
    return s


def _chamfer_room_corners(s: StructureBuilder, depth: int = 2) -> None:
    """Rounds off extreme exterior corners so rooms have an organic silhouette from outside."""
    for x in range(s.sx):
        for z in range(s.sz):
            dx = min(x, s.sx - 1 - x)
            dz = min(z, s.sz - 1 - z)
            if dx + dz < depth:
                for y in range(s.sy):
                    s.set(x, y, z, "minecraft:air")


def _carve_corridor_segment(s: StructureBuilder, x1: int, z1: int, x2: int, z2: int) -> None:
    s.fill(x1, 0, z1, x2, 0, z2, "minecraft:deepslate_bricks")
    s.fill(x1, 6, z1, x2, 6, z2, "mycelialmind:mycelial_arch_brick")
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            is_wall = x in (x1, x2) or z in (z1, z2)
            for y in range(1, 6):
                s.set(x, y, z, "mycelialmind:mycelial_arch_brick" if is_wall else "minecraft:air")


def build_corridor_straight() -> StructureBuilder:
    s = StructureBuilder(7, 7, 9)
    _carve_corridor_segment(s, 0, 0, 6, 8)
    s.fill(2, 1, 0, 4, 4, 8, "minecraft:air")
    for z in (2, 6):
        for y in range(1, 5):
            s.set(1, y, z, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(5, y, z, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.stair(2, 4, z, "west", "top")
        s.stair(4, 4, z, "east", "top")
        s.set(3, 5, z, "mycelialmind:luminescent_amber_vein")
    s.set(3, 0, 4, "mycelialmind:creeping_mycelium")
    s.jigsaw(3, 1, 8, "south", "mycelialmind:hive_sanctum/caps")
    s.jigsaw(3, 1, 0, "north", "mycelialmind:hive_sanctum/rooms")
    return s


def build_corridor_turn() -> StructureBuilder:
    s = StructureBuilder(9, 7, 9)
    _carve_corridor_segment(s, 0, 0, 8, 8)
    s.fill(3, 1, 3, 5, 4, 8, "minecraft:air")
    s.fill(3, 1, 3, 8, 4, 5, "minecraft:air")
    s.set(4, 5, 4, "minecraft:chain")
    s.set(4, 4, 4, "mycelialmind:luminescent_amber_vein")
    s.set(2, 1, 2, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(2, 2, 2, "mycelialmind:spore_bulb")
    s.jigsaw(4, 1, 8, "south", "mycelialmind:hive_sanctum/caps")
    s.jigsaw(8, 1, 4, "east", "mycelialmind:hive_sanctum/rooms")
    return s


def build_corridor_cross() -> StructureBuilder:
    s = StructureBuilder(9, 7, 9)
    _carve_corridor_segment(s, 0, 0, 8, 8)
    s.fill(3, 1, 0, 5, 4, 8, "minecraft:air")
    s.fill(0, 1, 3, 8, 4, 5, "minecraft:air")
    for cx, cz in ((2, 2), (6, 2), (2, 6), (6, 6)):
        for y in range(1, 6):
            s.set(cx, y, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(4, 5, 4, "minecraft:chain")
    s.set(4, 4, 4, "mycelialmind:luminescent_amber_vein")
    s.jigsaw(4, 1, 8, "south", "mycelialmind:hive_sanctum/caps")
    s.jigsaw(4, 1, 0, "north", "mycelialmind:hive_sanctum/rooms")
    s.jigsaw(0, 1, 4, "west", "mycelialmind:hive_sanctum/rooms")
    s.jigsaw(8, 1, 4, "east", "mycelialmind:hive_sanctum/rooms")
    return s


def build_ribbed_nave() -> StructureBuilder:
    s = StructureBuilder(11, 11, 13)
    _carve_corridor_segment(s, 0, 0, 10, 12)
    s.fill(0, 10, 0, 10, 10, 12, "mycelialmind:mycelial_arch_brick")
    s.fill(2, 1, 1, 8, 9, 11, "minecraft:air")
    # South/North doorways
    s.fill(4, 1, 11, 6, 4, 12, "minecraft:air")
    s.fill(4, 1, 0, 6, 4, 1, "minecraft:air")
    # 3 giant diaphragm arches of bone_block + chitin_pillar + inverted stairs
    for z in (3, 6, 9):
        for y in range(1, 8):
            s.set(2, y, z, "mycelialmind:chitin_pillar", {"axis": "y"})
            s.set(8, y, z, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.stair(3, 7, z, "west", "top")
        s.stair(7, 7, z, "east", "top")
        s.fill(3, 8, z, 7, 8, z, "minecraft:bone_block", {"axis": "x"})
        s.set(5, 8, z, "mycelialmind:luminescent_amber_vein")
        s.set(5, 7, z, "minecraft:chain")
        s.set(5, 6, z, "mycelialmind:bio_chitin_lattice")
    s.jigsaw(5, 1, 12, "south", "mycelialmind:hive_sanctum/caps")
    s.jigsaw(5, 1, 0, "north", "mycelialmind:hive_sanctum/rooms")
    for cx in (1, 9):
        for cz in (1, 11):
            s.fill(cx, 0, cz, cx, 10, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    _chamfer_room_corners(s, 2)
    return s


def build_forbidden_lab() -> StructureBuilder:
    s = StructureBuilder(13, 10, 13)
    _carve_corridor_segment(s, 0, 0, 12, 12)
    s.fill(0, 9, 0, 12, 9, 12, "mycelialmind:mycelial_arch_brick")
    s.fill(2, 1, 2, 10, 8, 10, "minecraft:air")
    s.fill(5, 1, 10, 7, 4, 12, "minecraft:air")
    # Central research bench dais
    s.fill(5, 1, 5, 7, 1, 7, "minecraft:polished_basalt")
    s.set(6, 2, 6, "mycelialmind:synaptic_research_bench")
    # 2 Bio-containment capsules
    for cx in (3, 9):
        s.set(cx, 1, 4, "mycelialmind:mycelial_arch_brick")
        s.set(cx, 2, 4, "mycelialmind:bio_chitin_lattice")
        s.set(cx, 3, 4, "minecraft:shroomlight")
        s.set(cx, 4, 4, "mycelialmind:bio_chitin_lattice")
        s.set(cx, 5, 4, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(6, 8, 6, "minecraft:chain")
    s.set(6, 7, 6, "mycelialmind:luminescent_amber_vein")
    s.chest(6, 1, 2, "south", [
        (0, "mycelialmind:truce_pheromone", 2),
        (1, "mycelialmind:neural_echo_core", 1),
        (2, "mycelialmind:synaptic_core", 1),
        (3, "mycelialmind:mycelial_biomass", 8),
    ], loot_table="mycelialmind:chests/sanctum_forbidden_lab")
    s.jigsaw(6, 1, 12, "south", "mycelialmind:hive_sanctum/caps")
    for cx in (1, 11):
        for cz in (1, 11):
            s.fill(cx, 0, cz, cx, 9, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    _chamfer_room_corners(s, 2)
    return s


def build_ossuary_crypt() -> StructureBuilder:
    s = StructureBuilder(13, 10, 13)
    _carve_corridor_segment(s, 0, 0, 12, 12)
    s.fill(0, 9, 0, 12, 9, 12, "mycelialmind:mycelial_stone")
    s.fill(2, 1, 2, 10, 8, 10, "minecraft:air")
    s.fill(5, 1, 10, 7, 4, 12, "minecraft:air")
    # Sculpted 3D Leviathan ribcage & skull (6-connected arches + spine-mounted skull)
    for z in (4, 6, 8):
        for y in range(1, 6):
            s.set(3, y, z, "minecraft:bone_block", {"axis": "y"})
            s.set(9, y, z, "minecraft:bone_block", {"axis": "y"})
        s.set(4, 5, z, "minecraft:bone_block", {"axis": "x"})
        s.set(8, 5, z, "minecraft:bone_block", {"axis": "x"})
        s.fill(4, 6, z, 8, 6, z, "minecraft:bone_block", {"axis": "x"})
    # Leviathan skull mounted on north wall via bone spine
    s.fill(5, 2, 3, 7, 4, 3, "minecraft:bone_block", {"axis": "y"})
    s.set(6, 3, 2, "minecraft:bone_block", {"axis": "z"})
    s.set(6, 1, 3, "minecraft:polished_basalt")
    s.set(5, 3, 3, "mycelialmind:luminescent_amber_vein")
    s.set(7, 3, 3, "mycelialmind:luminescent_amber_vein")
    s.chest(6, 1, 4, "south", [
        (0, "mycelialmind:schism_injector", 1),
        (1, "mycelialmind:synaptic_core", 1),
        (2, "mycelialmind:chitin_plate", 5),
        (3, "mycelialmind:concentrated_antidote", 1),
    ], loot_table="mycelialmind:chests/sanctum_ossuary_crypt")
    s.jigsaw(6, 1, 12, "south", "mycelialmind:hive_sanctum/caps")
    for cx in (1, 11):
        for cz in (1, 11):
            s.fill(cx, 0, cz, cx, 9, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    _chamfer_room_corners(s, 2)
    return s


def build_spore_vault() -> StructureBuilder:
    s = StructureBuilder(11, 9, 11)
    _carve_corridor_segment(s, 0, 0, 10, 10)
    s.fill(0, 8, 0, 10, 8, 10, "mycelialmind:mycelial_arch_brick")
    s.fill(2, 1, 2, 8, 7, 8, "minecraft:air")
    s.fill(4, 1, 8, 6, 4, 10, "minecraft:air")
    for cx, cz in ((2, 2), (8, 2), (2, 8), (8, 8)):
        for y in range(1, 7):
            s.set(cx, y, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(5, 7, 5, "minecraft:chain")
    s.set(5, 6, 5, "mycelialmind:luminescent_amber_vein")
    s.set(5, 1, 4, "mycelialmind:reinforced_bio_barrier")
    s.set(3, 1, 5, "mycelialmind:spore_vent")
    s.set(7, 1, 5, "mycelialmind:spore_vent")
    s.chest(4, 1, 2, "south", [
        (0, "mycelialmind:synaptic_core", 1),
        (1, "mycelialmind:chitin_plate", 4),
        (2, "mycelialmind:concentrated_antidote", 1),
        (3, "mycelialmind:mycelial_biomass", 8),
    ], loot_table="mycelialmind:chests/sanctum_spore_vault")
    s.chest(6, 1, 2, "south", [
        (0, "mycelialmind:purifier_grenade", 2),
        (1, "mycelialmind:latent_spore", 4),
        (2, "mycelialmind:truce_pheromone", 2),
    ], loot_table="mycelialmind:chests/sanctum_spore_vault")
    s.jigsaw(5, 1, 10, "south", "mycelialmind:hive_sanctum/caps")
    for cx in (1, 9):
        for cz in (1, 9):
            s.fill(cx, 0, cz, cx, 8, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    _chamfer_room_corners(s, 2)
    return s


def build_incubation_wing() -> StructureBuilder:
    s = StructureBuilder(11, 9, 11)
    _carve_corridor_segment(s, 0, 0, 10, 10)
    s.fill(0, 8, 0, 10, 8, 10, "mycelialmind:mycelial_stone")
    s.fill(2, 1, 2, 8, 7, 8, "minecraft:air")
    s.fill(4, 1, 8, 6, 4, 10, "minecraft:air")
    s.fill(3, 0, 3, 7, 0, 7, "mycelialmind:creeping_mycelium")
    s.set(5, 1, 5, "mycelialmind:mycelial_arch_brick")
    s.set(5, 2, 5, "mycelialmind:incubator_node")
    s.set(5, 7, 5, "mycelialmind:bio_chitin_lattice")
    for fx, fz in ((3, 3), (7, 3), (3, 7), (7, 7)):
        s.set(fx, 1, fz, "mycelialmind:bioluminescent_mycelial_cap")
        s.set(fx, 7, fz, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.set(fx, 6, fz, "minecraft:deepslate_brick_wall")
    s.set(1, 4, 5, "mycelialmind:chitin_pillar", {"axis": "x"})
    s.set(2, 4, 5, "minecraft:shroomlight")
    s.set(9, 4, 5, "mycelialmind:chitin_pillar", {"axis": "x"})
    s.set(8, 4, 5, "minecraft:shroomlight")
    s.jigsaw(5, 1, 10, "south", "mycelialmind:hive_sanctum/caps")
    for cx in (1, 9):
        for cz in (1, 9):
            s.fill(cx, 0, cz, cx, 8, cz, "mycelialmind:chitin_pillar", {"axis": "y"})
    _chamfer_room_corners(s, 2)
    return s


def build_cap_end() -> StructureBuilder:
    s = StructureBuilder(7, 7, 3)
    _carve_corridor_segment(s, 0, 0, 6, 2)
    s.fill(2, 1, 1, 4, 4, 2, "minecraft:air")
    s.set(3, 3, 0, "mycelialmind:luminescent_amber_vein")
    s.stair(2, 4, 1, "west", "top")
    s.stair(4, 4, 1, "east", "top")
    s.set(1, 1, 1, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(5, 1, 1, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.jigsaw(3, 1, 2, "south", "mycelialmind:hive_sanctum/caps")
    return s


def verify_6_connected(s: StructureBuilder, name: str) -> None:
    """Aborts generation if any piece contains disconnected or corner-only blocks."""
    non_air = {pos for pos, entry in s.grid.items() if entry[0] != "minecraft:air"}
    if not non_air:
        return
    seed = next(iter(non_air))
    visited = {seed}
    queue = [seed]
    while queue:
        x, y, z = queue.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            nxt = (x + dx, y + dy, z + dz)
            if nxt in non_air and nxt not in visited:
                visited.add(nxt)
                queue.append(nxt)
    if len(visited) != len(non_air):
        unvisited = sorted(non_air - visited)
        raise RuntimeError(
            f"{name}: {len(unvisited)} disconnected blocks found (not 6-connected); sample={unvisited[:5]}"
        )


def build_twisted_spire_tower() -> StructureBuilder:
    s = StructureBuilder(9, 15, 9)
    # y=0: foundation pad (7x7 tower base + 9x9 fungal plinth corners)
    s.fill(1, 0, 1, 7, 0, 7, "minecraft:deepslate_bricks")
    for x in (2, 4, 6):
        for z in (2, 4, 6):
            if (x + z) % 4 == 0:
                s.set(x, 0, z, "mycelialmind:creeping_mycelium")
    for cx in (0, 8):
        for cz in (1, 7):
            s.set(cx, 0, cz, "mycelialmind:mycelial_arch_brick")
    for cz in (0, 8):
        for cx in (1, 7):
            s.set(cx, 0, cz, "mycelialmind:mycelial_arch_brick")

    # y=1..10: slender spire walls + corner pillars + South arch
    for y in range(1, 11):
        for x in range(1, 8):
            for z in range(1, 8):
                on_wall = x in (1, 7) or z in (1, 7)
                if not on_wall:
                    continue
                is_corner = x in (1, 7) and z in (1, 7)
                if z == 7 and x in (3, 4, 5) and y <= 3:
                    continue
                if is_corner:
                    s.set(x, y, z, "mycelialmind:chitin_pillar" if y % 4 == 0 else "minecraft:polished_basalt", {"axis": "y"})
                elif y in (5, 6) and (x == 4 or z == 4):
                    s.set(x, y, z, "mycelialmind:bio_chitin_lattice")
                elif y == 10 and (x == 4 or z == 4):
                    s.set(x, y, z, "minecraft:shroomlight")
                else:
                    s.set(x, y, z, "mycelialmind:mycelial_arch_brick" if (x + y + z) % 5 == 0 else "minecraft:deepslate_bricks")

    # Interior spiral/rib ledges and hanging tendrils
    s.set(2, 4, 2, "mycelialmind:mycelial_arch_brick")
    s.set(6, 7, 2, "mycelialmind:mycelial_arch_brick")

    # y=11: overhanging rim & battlements
    for x in range(0, 9):
        for z in range(0, 9):
            dist = max(abs(x - 4), abs(z - 4))
            if dist >= 2:
                is_outer = dist == 4
                s.set(x, 11, z, "minecraft:deepslate_brick_wall" if (is_outer and (x + z) % 2 == 0)
                      else ("minecraft:deepslate_brick_slab" if is_outer else "minecraft:deepslate_bricks"),
                      {} if not is_outer or (x + z) % 2 == 0 else {"type": "bottom", "waterlogged": "false"})
            elif x == 4 and z == 4:
                s.set(x, 11, z, "minecraft:shroomlight")
            else:
                s.set(x, 11, z, "mycelialmind:mycelial_arch_brick")
    for tx, tz in ((0, 4), (8, 4), (4, 0), (4, 8)):
        s.set(tx, 10, tz, "mycelialmind:mycelial_tendrils", {"mature": "true"})

    # y=12..14: fungal-infested asymmetric crown spire
    s.fill(2, 12, 2, 6, 12, 6, "mycelialmind:mycelial_stone")
    s.set(4, 12, 4, "mycelialmind:mycelial_arch_brick")
    s.fill(3, 13, 3, 5, 13, 5, "mycelialmind:mycelial_arch_brick")
    s.set(4, 13, 4, "minecraft:shroomlight")
    s.set(4, 14, 4, "mycelialmind:luminescent_amber_vein")

    # 6-connected exterior mycelial spine climbing from ground (8,0,4) -> (7,10,4)
    s.set(8, 0, 4, "mycelialmind:creeping_mycelium")
    for y in range(0, 6):
        s.set(8, y, 4, "mycelialmind:chitin_pillar" if y % 2 == 0 else "mycelialmind:mycelial_arch_brick",
              {"axis": "y"} if y % 2 == 0 else {})
    s.set(8, 6, 4, "mycelialmind:luminescent_amber_vein")
    s.set(8, 11, 4, "mycelialmind:mycelial_arch_brick")
    return s


def build_twisted_gallery_hall() -> StructureBuilder:
    s = StructureBuilder(13, 10, 9)
    # y=0: foundation floor
    s.fill(0, 0, 0, 12, 0, 8, "minecraft:deepslate_bricks")
    for x in range(2, 11):
        s.set(x, 0, 4, "mycelialmind:creeping_mycelium")

    # y=1..6: wide hall with East/West portals & buttressed ribs
    for y in range(1, 7):
        for x in range(1, 12):
            for z in range(1, 8):
                on_wall = x in (1, 11) or z in (1, 7)
                if not on_wall:
                    continue
                if x in (1, 11) and z in (3, 4, 5) and y <= 3:
                    continue
                is_rib = x in (1, 4, 8, 11) and z in (1, 7)
                if is_rib:
                    s.set(x, y, z, "mycelialmind:chitin_pillar" if y % 4 == 0 else "minecraft:polished_basalt", {"axis": "y"})
                elif y in (3, 4) and x in (6, 2, 10):
                    s.set(x, y, z, "mycelialmind:bio_chitin_lattice")
                elif y == 6 and x in (4, 8):
                    s.set(x, y, z, "minecraft:shroomlight")
                else:
                    s.set(x, y, z, "mycelialmind:mycelial_arch_brick" if (x + y + z) % 6 == 0 else "minecraft:deepslate_bricks")

    # Exterior buttresses attached cleanly at y=0..4
    for bx in (4, 8):
        for bz in (0, 8):
            s.fill(bx, 1, bz, bx, 3, bz, "minecraft:polished_basalt", {"axis": "y"})
            s.stair(bx, 4, bz, "south" if bz == 0 else "north")

    # y=7..9: vaulted fungal roof
    s.fill(1, 7, 1, 11, 7, 7, "minecraft:deepslate_bricks")
    for x in range(2, 11):
        s.set(x, 7, 4, "mycelialmind:mycelial_arch_brick")
    s.set(4, 7, 4, "minecraft:shroomlight")
    s.set(8, 7, 4, "minecraft:shroomlight")
    s.fill(2, 8, 2, 10, 8, 6, "mycelialmind:mycelial_stone")
    s.fill(3, 8, 3, 9, 8, 5, "mycelialmind:mycelial_arch_brick")
    for x in range(4, 9):
        s.set(x, 9, 4, "mycelialmind:luminescent_amber_vein" if x in (4, 8) else "minecraft:deepslate_brick_slab",
              {} if x in (4, 8) else {"type": "bottom", "waterlogged": "false"})
    return s


def build_twisted_vault_bunker() -> StructureBuilder:
    s = StructureBuilder(11, 7, 11)
    # y=0: heavy grounded foundation
    s.fill(0, 0, 0, 10, 0, 10, "minecraft:deepslate_bricks")
    for p in range(2, 9):
        s.set(p, 0, p, "mycelialmind:creeping_mycelium")
        s.set(10 - p, 0, p, "mycelialmind:creeping_mycelium")

    # y=1..4: squat fortified vault walls
    for y in range(1, 5):
        for x in range(1, 10):
            for z in range(1, 10):
                on_wall = x in (1, 9) or z in (1, 9)
                if not on_wall:
                    continue
                if z == 9 and x in (4, 5, 6) and y <= 2:
                    continue
                is_corner = (x in (1, 2, 8, 9)) and (z in (1, 2, 8, 9))
                if is_corner:
                    s.set(x, y, z, "minecraft:polished_basalt", {"axis": "y"})
                elif y == 2 and (x == 5 or z == 5):
                    s.set(x, y, z, "mycelialmind:spore_vent")
                else:
                    s.set(x, y, z, "minecraft:deepslate_bricks")

    # Outer bastion wall crenellations
    for cx in (0, 10):
        for cz in (2, 5, 8):
            s.set(cx, 1, cz, "minecraft:deepslate_brick_wall")
            s.set(cz, 1, cx, "minecraft:deepslate_brick_wall")

    # y=5..6: armored fungal bunker roof
    s.fill(1, 5, 1, 9, 5, 9, "minecraft:deepslate_bricks")
    s.fill(2, 5, 2, 8, 5, 8, "mycelialmind:mycelial_arch_brick")
    s.set(5, 5, 5, "minecraft:shroomlight")
    s.fill(3, 6, 3, 7, 6, 7, "mycelialmind:mycelial_stone")
    s.set(5, 6, 5, "mycelialmind:luminescent_amber_vein")
    for rx in (2, 8):
        for rz in (2, 8):
            s.set(rx, 6, rz, "minecraft:deepslate_brick_slab", {"type": "bottom", "waterlogged": "false"})
    return s


def build_vertical_shaft() -> StructureBuilder:
    """
    6-connected stepped vertical shaft connecting two horizontal hive_sanctum levels (deltaY = +8).
    South entrance (3, 1, 10) -> Stepped Deepslate/Arch staircase -> North upper exit (3, 9, 0).
    """
    s = StructureBuilder(7, 15, 11)
    # Build solid stepped prism shell: floor at y=0, sloped roof y_ceil(z)
    for z in range(s.sz):
        if z >= 8:
            y_floor = 0
            y_ceil = 6
        elif z >= 2:
            step = 8 - z  # 1..6
            y_floor = step
            y_ceil = min(14, step + 7)
        else:
            y_floor = 8
            y_ceil = 14

        for x in range(s.sx):
            # Solid base foundation under sloped corridor so exterior never leaves floating gaps
            for y in range(0, y_floor + 1):
                s.set(x, y, z, "minecraft:deepslate_bricks")
            is_wall_x = x in (0, 6)
            is_end_wall = (z == 0) or (z == 10)
            for y in range(y_floor + 1, y_ceil):
                if is_wall_x or is_end_wall:
                    s.set(x, y, z, "mycelialmind:mycelial_arch_brick")
                else:
                    s.set(x, y, z, "minecraft:air")
            s.set(x, y_ceil, z, "mycelialmind:mycelial_arch_brick")

    # Carve North (upper) and South (lower) 3x4 doorways
    s.fill(2, 1, 10, 4, 4, 10, "minecraft:air")
    s.fill(2, 9, 0, 4, 12, 0, "minecraft:air")

    # Real climbable staircase steps ascending North across z = 8..2 (y = 1..7, landing onto y = 8 at z = 1)
    for step in range(7):
        z = 8 - step
        stair_y = step + 1
        for x in range(2, 5):
            s.stair(x, stair_y, z, "north", "bottom")
        # Raised stringer trim on side walkways (x = 1, 5)
        s.set(1, stair_y, z, "mycelialmind:chitin_pillar" if step % 3 == 0 else "mycelialmind:mycelial_arch_brick", {"axis": "y"} if step % 3 == 0 else {})
        s.set(5, stair_y, z, "mycelialmind:chitin_pillar" if step % 3 == 0 else "mycelialmind:mycelial_arch_brick", {"axis": "y"} if step % 3 == 0 else {})

    # Structural chitin ribs & amber lighting at lower and upper landings
    for y in range(1, 6):
        s.set(1, y, 9, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.set(5, y, 9, "mycelialmind:chitin_pillar", {"axis": "y"})
    for y in range(9, 14):
        s.set(1, y, 1, "mycelialmind:chitin_pillar", {"axis": "y"})
        s.set(5, y, 1, "mycelialmind:chitin_pillar", {"axis": "y"})
    s.set(1, 4, 8, "mycelialmind:luminescent_amber_vein")
    s.set(5, 4, 8, "mycelialmind:luminescent_amber_vein")
    s.set(1, 12, 2, "mycelialmind:luminescent_amber_vein")
    s.set(5, 12, 2, "mycelialmind:luminescent_amber_vein")
    s.set(3, 0, 9, "mycelialmind:creeping_mycelium")
    s.set(3, 8, 1, "mycelialmind:creeping_mycelium")

    s.jigsaw(3, 1, 10, "south", "mycelialmind:hive_sanctum/corridors")
    s.jigsaw(3, 9, 0, "north", "mycelialmind:hive_sanctum/rooms")
    return s


def main() -> None:
    out_dir = os.path.join("src", "main", "resources", "data", "mycelialmind", "structure", "hive_sanctum")
    os.makedirs(out_dir, exist_ok=True)
    pieces = {
        "sanctum_atrium.nbt": build_sanctum_atrium(),
        "corridor_straight.nbt": build_corridor_straight(),
        "corridor_turn.nbt": build_corridor_turn(),
        "corridor_cross.nbt": build_corridor_cross(),
        "vertical_shaft.nbt": build_vertical_shaft(),
        "ribbed_nave.nbt": build_ribbed_nave(),
        "forbidden_lab.nbt": build_forbidden_lab(),
        "ossuary_crypt.nbt": build_ossuary_crypt(),
        "spore_vault.nbt": build_spore_vault(),
        "incubation_wing.nbt": build_incubation_wing(),
        "cap_end.nbt": build_cap_end(),
    }
    for fname, builder in pieces.items():
        verify_6_connected(builder, fname)
        path = os.path.join(out_dir, fname)
        with open(path, "wb") as f:
            f.write(builder.to_nbt_bytes())
        print(f"Generated {path} ({builder.sx}x{builder.sy}x{builder.sz}, {len(builder.grid)} blocks)")

    refl_dir = os.path.join("src", "main", "resources", "data", "mycelialmind", "structure", "twisted_reflection")
    os.makedirs(refl_dir, exist_ok=True)
    refl_pieces = {
        "spire_tower.nbt": build_twisted_spire_tower(),
        "gallery_hall.nbt": build_twisted_gallery_hall(),
        "vault_bunker.nbt": build_twisted_vault_bunker(),
    }
    for fname, builder in refl_pieces.items():
        verify_6_connected(builder, fname)
        path = os.path.join(refl_dir, fname)
        with open(path, "wb") as f:
            f.write(builder.to_nbt_bytes())
        print(f"Generated {path} ({builder.sx}x{builder.sy}x{builder.sz}, {len(builder.grid)} blocks)")


if __name__ == "__main__":
    main()
