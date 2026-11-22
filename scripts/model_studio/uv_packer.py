"""Data structures and 2D shelf bin-packer for Minecraft EntityModel UV atlases."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple


@dataclass
class CubeSpec:
    name: str
    origin: Tuple[float, float, float]
    size: Tuple[int, int, int]
    role: str = "skin"          # skin | armor | bone | glow_eye | glow_core | spore_sac | metal | magma
    emissive: bool = False
    inflation: float = 0.0
    mirror: bool = False
    eye_pattern: Optional[str] = None  # two_eyes | three_eyes | four_eyes | t_visor | lens | core_heart
    detail_tags: Tuple[str, ...] = ()
    uv: Tuple[int, int] = (0, 0)       # assigned by pack_model_uvs

    @property
    def uv_rect(self) -> Tuple[int, int]:
        w, h, d = self.size
        return (2 * (w + d), d + h)


@dataclass
class PartNode:
    name: str
    pivot: Tuple[float, float, float] = (0.0, 0.0, 0.0)
    rotation: Tuple[float, float, float] = (0.0, 0.0, 0.0)
    cubes: Tuple[CubeSpec, ...] = ()
    children: Tuple["PartNode", ...] = ()
    anim_role: Optional[str] = None
    anim_phase: float = 0.0
    anim_amp: float = 1.0
    rank_min: Optional[str] = None   # None | "MUTATED" | "ELITE"
    phase_min: Optional[int] = None  # Optional boss phase threshold (e.g. 2 or 3)
    gene_match: Optional[str] = None # Optional AdaptationTrait enum name for MycelialChimeraEntity


@dataclass
class ModelDef:
    class_name: str
    registry_id: str
    description: str
    tex_size: Tuple[int, int]
    parts: Tuple[PartNode, ...]
    palettes: Dict[str, str] = field(default_factory=dict)


def _collect_cubes(parts: Tuple[PartNode, ...], out: List[CubeSpec]) -> None:
    for p in parts:
        for c in p.cubes:
            out.append(c)
        _collect_cubes(p.children, out)


def pack_model_uvs(model: ModelDef) -> List[Dict]:
    """Pack all cubes in a ModelDef into its (tex_w, tex_h) atlas with zero overlaps."""
    tex_w, tex_h = model.tex_size
    cubes: List[CubeSpec] = []
    _collect_cubes(model.parts, cubes)

    # Sort tallest first for tight shelf packing
    sorted_cubes = sorted(cubes, key=lambda c: (-c.uv_rect[1], -c.uv_rect[0], c.name))
    cursor_x, cursor_y, shelf_h = 0, 0, 0
    pad = 1
    placed_rects: List[Tuple[int, int, int, int, str]] = []

    for c in sorted_cubes:
        rw, rh = c.uv_rect
        if cursor_x + rw > tex_w:
            cursor_x = 0
            cursor_y += shelf_h + pad
            shelf_h = 0
        if cursor_y + rh > tex_h or rw > tex_w:
            raise ValueError(
                f"UV overflow in {model.class_name} for cube '{c.name}' ({rw}x{rh}) at ({cursor_x},{cursor_y}) in {tex_w}x{tex_h}"
            )
        c.uv = (cursor_x, cursor_y)
        placed_rects.append((cursor_x, cursor_y, rw, rh, c.name))
        cursor_x += rw + pad
        shelf_h = max(shelf_h, rh)

    # Verify zero overlaps
    for i in range(len(placed_rects)):
        ax, ay, aw, ah, an = placed_rects[i]
        for j in range(i + 1, len(placed_rects)):
            bx, by, bw, bh, bn = placed_rects[j]
            if ax < bx + bw and ax + aw > bx and ay < by + bh and ay + ah > by:
                raise AssertionError(f"UV overlap in {model.class_name}: {an} vs {bn}")

    manifest_cubes = []
    for c in cubes:
        manifest_cubes.append({
            "name": c.name,
            "u": c.uv[0],
            "v": c.uv[1],
            "w": c.size[0],
            "h": c.size[1],
            "d": c.size[2],
            "role": c.role,
            "emissive": c.emissive,
            "mirror": c.mirror,
            "eye_pattern": c.eye_pattern,
            "detail_tags": list(c.detail_tags),
        })
    return manifest_cubes
