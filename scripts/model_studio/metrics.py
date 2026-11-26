"""Pure geometric metrics for EntityModel definitions (bounds, layers, fingerprints)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict, Iterator, List, Optional, Sequence, Tuple

from .uv_packer import CubeSpec, ModelDef, PartNode, pack_model_uvs

Condition = Optional[Tuple[Optional[str], Optional[int]]]


@dataclass(frozen=True)
class Bounds:
    min_x: float
    min_y: float
    min_z: float
    max_x: float
    max_y: float
    max_z: float

    @property
    def width(self) -> float:
        return self.max_x - self.min_x

    @property
    def height(self) -> float:
        return self.max_y - self.min_y

    @property
    def depth(self) -> float:
        return self.max_z - self.min_z


def iter_parts(parts: Sequence[PartNode]) -> Iterator[PartNode]:
    for part in parts:
        yield part
        yield from iter_parts(part.children)


def iter_cubes(parts: Sequence[PartNode]) -> Iterator[CubeSpec]:
    for part in iter_parts(parts):
        yield from part.cubes


def _condition(part: PartNode) -> Condition:
    if part.rank_min is not None or part.phase_min is not None:
        return (part.rank_min, part.phase_min)
    return None


def collect_conditioned_cubes(parts: Sequence[PartNode]) -> List[Tuple[Condition, CubeSpec]]:
    """Return (condition, cube) pairs; the condition belongs to the nearest conditional ancestor."""
    out: List[Tuple[Condition, CubeSpec]] = []

    def walk(nodes: Sequence[PartNode], inherited: Condition) -> None:
        for part in nodes:
            cond = _condition(part) or inherited
            for cube in part.cubes:
                out.append((cond, cube))
            walk(part.children, cond)

    walk(parts, None)
    return out


def rest_pose_bounds(model: ModelDef, include_conditional: bool = False) -> Bounds:
    xs: List[float] = []
    ys: List[float] = []
    zs: List[float] = []

    def walk(parts: Sequence[PartNode], px: float, py: float, pz: float, cond: Condition) -> None:
        for part in parts:
            wx, wy, wz = px + part.pivot[0], py + part.pivot[1], pz + part.pivot[2]
            active = _condition(part) or cond
            if include_conditional or active is None:
                for cube in part.cubes:
                    xs.extend((wx + cube.origin[0], wx + cube.origin[0] + cube.size[0]))
                    ys.extend((wy + cube.origin[1], wy + cube.origin[1] + cube.size[1]))
                    zs.extend((wz + cube.origin[2], wz + cube.origin[2] + cube.size[2]))
            walk(part.children, wx, wy, wz, active)

    walk(model.parts, 0.0, 0.0, 0.0, None)
    if not xs:
        return Bounds(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    return Bounds(min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))


def base_height_px(model: ModelDef) -> float:
    return 24.0 - rest_pose_bounds(model, include_conditional=False).min_y


def layer_cubes(model: ModelDef, *, rank: Optional[str] = None, phase: Optional[int] = None) -> List[CubeSpec]:
    out: List[CubeSpec] = []
    for cond, cube in collect_conditioned_cubes(model.parts):
        if cond is None:
            continue
        cond_rank, cond_phase = cond
        if rank is not None and cond_rank == rank:
            out.append(cube)
        elif phase is not None and cond_phase == phase:
            out.append(cube)
    return out


def layer_part_count(model: ModelDef, *, rank: Optional[str] = None, phase: Optional[int] = None) -> int:
    count = 0
    for part in iter_parts(model.parts):
        if rank is not None and part.rank_min == rank:
            count += 1
        elif phase is not None and part.phase_min == phase:
            count += 1
    return count


def emissive_count(model: ModelDef, include_conditional: bool = True) -> int:
    if include_conditional:
        return sum(1 for cube in iter_cubes(model.parts) if cube.emissive)
    return sum(
        1
        for cond, cube in collect_conditioned_cubes(model.parts)
        if cond is None and cube.emissive
    )


def silhouette_fingerprint(model: ModelDef) -> Tuple[Tuple[float, ...], ...]:
    """Geometry-only fingerprint in rest-pose world space (names intentionally excluded)."""
    rows: List[Tuple[float, ...]] = []

    def walk(parts: Sequence[PartNode], px: float, py: float, pz: float) -> None:
        for part in parts:
            wx, wy, wz = px + part.pivot[0], py + part.pivot[1], pz + part.pivot[2]
            for cube in part.cubes:
                rows.append((
                    round(wx + cube.origin[0], 1),
                    round(wy + cube.origin[1], 1),
                    round(wz + cube.origin[2], 1),
                    float(cube.size[0]),
                    float(cube.size[1]),
                    float(cube.size[2]),
                    1.0 if cube.mirror else 0.0,
                ))
            walk(part.children, wx, wy, wz)

    walk(model.parts, 0.0, 0.0, 0.0)
    rows.sort()
    return tuple(rows)


def atlas_usage(model: ModelDef) -> Dict[str, float]:
    cubes = pack_model_uvs(model)
    tex_w, tex_h = model.tex_size
    area = sum(2 * (c["w"] + c["d"]) * (c["h"] + c["d"]) for c in cubes)
    used_h = max(c["v"] + c["h"] + c["d"] for c in cubes)
    used_w = max(c["u"] + 2 * (c["w"] + c["d"]) for c in cubes)
    return {
        "used_width": float(used_w),
        "used_height": float(used_h),
        "height_fraction": round(used_h / tex_h, 4),
        "area_fraction": round(area / (tex_w * tex_h), 4),
    }


def condition_counts(model: ModelDef) -> Dict[str, int]:
    counts = {"base": 0, "mutated": 0, "elite": 0, "phase2": 0, "phase3": 0}
    for cond, _cube in collect_conditioned_cubes(model.parts):
        if cond is None:
            counts["base"] += 1
            continue
        cond_rank, cond_phase = cond
        if cond_rank == "MUTATED":
            counts["mutated"] += 1
        elif cond_rank == "ELITE":
            counts["elite"] += 1
        elif cond_phase == 2:
            counts["phase2"] += 1
        elif cond_phase == 3:
            counts["phase3"] += 1
    return counts


def model_metrics(model: ModelDef) -> Dict[str, object]:
    base = rest_pose_bounds(model, include_conditional=False)
    full = rest_pose_bounds(model, include_conditional=True)
    counts = condition_counts(model)
    return {
        "cube_count": sum(1 for _ in iter_cubes(model.parts)),
        "base_cube_count": counts["base"],
        "mutated_cube_count": counts["mutated"],
        "elite_cube_count": counts["elite"],
        "phase2_cube_count": counts["phase2"],
        "phase3_cube_count": counts["phase3"],
        "emissive_count": emissive_count(model),
        "base_height_px": round(24.0 - base.min_y, 2),
        "base_width_px": round(base.width, 2),
        "base_depth_px": round(base.depth, 2),
        "full_height_px": round(24.0 - full.min_y, 2),
        "atlas": list(model.tex_size),
        "atlas_usage": atlas_usage(model),
    }
