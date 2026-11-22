"""Declarative PBR material presets for Mycelial Mind Texture Studio v3."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Dict


@dataclass(frozen=True)
class Material:
    ramp_key: str
    smoothness: float      # [0.0, 1.0] -> LabPBR _s.png R
    f0: float              # [0.0, 1.0] (< 0.90 dielectric 0..229, >= 0.90 metal ID 230..254) -> G
    porosity_sss: float    # [0.0, 1.0] -> B (0..64 porosity if is_subsurface=False, 65..255 SSS if True)
    emissive: float        # [0.0, 1.0] (0.0 -> 255 in LabPBR _s.png A; >0 -> 1..254)
    dither_mode: str = "bayer8"
    is_subsurface: bool = False
    metal_id: int | None = None  # LabPBR 1.3 hardcoded metal ID (e.g. 230=Iron, 231=Gold, 234=Copper)


MATERIALS: Dict[str, Material] = {
    "OBSIDIAN_CHITIN": Material("abyssal_obsidian", smoothness=0.48, f0=0.06, porosity_sss=0.10, emissive=0.0, dither_mode="floyd", is_subsurface=False),
    "BIOLUM_MYCELIUM": Material("mycelial_violet", smoothness=0.72, f0=0.05, porosity_sss=0.45, emissive=0.45, dither_mode="bayer8", is_subsurface=True),
    "BIOLUM_CYAN": Material("biolum_cyan", smoothness=0.85, f0=0.08, porosity_sss=0.55, emissive=0.85, dither_mode="bayer8", is_subsurface=True),
    "PURIFICATION_GOLD": Material("royal_gold", smoothness=0.82, f0=0.94, porosity_sss=0.0, emissive=0.0, dither_mode="bayer4", is_subsurface=False, metal_id=231),
    "PURIFICATION_CRYSTAL": Material("purification_aqua", smoothness=0.92, f0=0.12, porosity_sss=0.65, emissive=0.80, dither_mode="bayer8", is_subsurface=True),
    "TOXIC_SPORE": Material("spore_chartreuse", smoothness=0.65, f0=0.04, porosity_sss=0.75, emissive=0.70, dither_mode="bayer8", is_subsurface=True),
    "CHITIN_BONE": Material("chitin_bone", smoothness=0.38, f0=0.05, porosity_sss=0.18, emissive=0.0, dither_mode="bayer8", is_subsurface=False),
    "LIVING_AMETHYST": Material("mycelial_violet", smoothness=0.88, f0=0.10, porosity_sss=0.50, emissive=0.35, dither_mode="bayer8", is_subsurface=True),
    "BASALT_SLATE": Material("basalt_slate", smoothness=0.30, f0=0.04, porosity_sss=0.08, emissive=0.0, dither_mode="floyd", is_subsurface=False),
}
