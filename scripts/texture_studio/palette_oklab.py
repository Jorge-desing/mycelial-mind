"""Oklab perceptual color space conversions, hue-shifted ramps, and palette exporters."""

from __future__ import annotations

import os
from typing import Dict, Tuple
import numpy as np

RGBA = Tuple[int, int, int, int]


def hex_to_rgb01(hex_str: str) -> np.ndarray:
    s = hex_str.lstrip("#")
    return np.array([int(s[i : i + 2], 16) for i in (0, 2, 4)], dtype=np.float32) / 255.0


def srgb_to_linear(c: np.ndarray) -> np.ndarray:
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def linear_to_srgb(c: np.ndarray) -> np.ndarray:
    c = np.clip(c, 0.0, 1.0)
    return np.where(c <= 0.0031308, c * 12.92, 1.055 * (c ** (1.0 / 2.4)) - 0.055)


def rgb_to_oklab(rgb: np.ndarray) -> np.ndarray:
    lin = srgb_to_linear(rgb)
    r, g, b = lin[..., 0], lin[..., 1], lin[..., 2]
    l = np.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    m = np.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    s = np.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    b_out = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return np.stack([L, a, b_out], axis=-1)


def oklab_to_rgb(lab: np.ndarray) -> np.ndarray:
    L, a, b = lab[..., 0], lab[..., 1], lab[..., 2]
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    r = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    b_out = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    return linear_to_srgb(np.stack([r, g, b_out], axis=-1))


def oklab_lerp_rgba(c1: RGBA, c2: RGBA, t: float) -> RGBA:
    rgb1 = np.array(c1[:3], dtype=np.float32) / 255.0
    rgb2 = np.array(c2[:3], dtype=np.float32) / 255.0
    lab = rgb_to_oklab(rgb1) * (1.0 - t) + rgb_to_oklab(rgb2) * t
    out = np.clip(oklab_to_rgb(lab) * 255.0, 0, 255).astype(np.uint8)
    a = int(c1[3] * (1.0 - t) + c2[3] * t)
    return (int(out[0]), int(out[1]), int(out[2]), a)


def build_oklab_ramp(shadow_hex: str, mid_hex: str, highlight_hex: str, steps: int = 8) -> np.ndarray:
    """Create a hue-shifted N-step RGBA ramp interpolated through Oklab."""
    s_lab = rgb_to_oklab(hex_to_rgb01(shadow_hex))
    m_lab = rgb_to_oklab(hex_to_rgb01(mid_hex))
    h_lab = rgb_to_oklab(hex_to_rgb01(highlight_hex))
    ramp = []
    for i in range(steps):
        t = i / max(1, steps - 1)
        if t <= 0.5:
            lab = s_lab * (1.0 - t * 2.0) + m_lab * (t * 2.0)
        else:
            lab = m_lab * (1.0 - (t - 0.5) * 2.0) + h_lab * ((t - 0.5) * 2.0)
        rgb = np.clip(oklab_to_rgb(lab) * 255.0, 0, 255).astype(np.uint8)
        ramp.append([int(rgb[0]), int(rgb[1]), int(rgb[2]), 255])
    return np.array(ramp, dtype=np.uint8)


# Curated 8-step hue-shifted ramps anchored to Vanilla Minecraft Mycelium (dusty mauve, earthy umber, mushroom stem ivory)
PALETTES: Dict[str, np.ndarray] = {
    "abyssal_obsidian": build_oklab_ramp("#261e22", "#40343a", "#5e4f56", 8),
    "mycelial_violet": build_oklab_ramp("#443642", "#725e6e", "#a48ea0", 8),
    "biolum_cyan": build_oklab_ramp("#244c4a", "#4a8e88", "#98dcd2", 8),
    "purification_aqua": build_oklab_ramp("#28524e", "#52a89c", "#aeeae0", 8),
    "spore_chartreuse": build_oklab_ramp("#38461e", "#6c8a34", "#bad868", 8),
    "chitin_bone": build_oklab_ramp("#5a5248", "#988e80", "#d8d0c2", 8),
    "royal_gold": build_oklab_ramp("#4e3818", "#9e7834", "#e2c068", 8),
    "pyro_magma": build_oklab_ramp("#3a1c14", "#a84420", "#f09c44", 8),
    "leech_crimson": build_oklab_ramp("#421618", "#8c282c", "#d45c58", 8),
    "basalt_slate": build_oklab_ramp("#2e2826", "#4e4440", "#766862", 8),
    "mutant_amber": build_oklab_ramp("#4e3212", "#b87628", "#ecc064", 8),
    "elite_crimson": build_oklab_ramp("#2e1018", "#7e2234", "#cc586c", 8),
    "elite_carapace": build_oklab_ramp("#241c22", "#4e404a", "#887682", 8),
    # Archetype ramps harmonized with Minecraft Mycelium
    "abyssal_indigo": build_oklab_ramp("#322a32", "#524652", "#786878", 8),
    "predator_amethyst": build_oklab_ramp("#362232", "#64405c", "#9c6a90", 8),
    "venom_emerald": build_oklab_ramp("#1c2a1e", "#38543c", "#628864", 8),
    "volcanic_ember": build_oklab_ramp("#2c1c16", "#663c28", "#a86a42", 8),
    "platinum_tech": build_oklab_ramp("#3c3a3e", "#7a767e", "#c4c0c8", 8),
    "imperial_brass": build_oklab_ramp("#463218", "#926c30", "#d8b464", 8),
    "warm_ivory": build_oklab_ramp("#52463c", "#8e7e6e", "#cfc2b2", 8),
    # Particle studio families (bio-horror organic + clinical purification tech).
    "acid_lime": build_oklab_ramp("#26330e", "#6e8c1c", "#d8f46a", 8),
    "necrotic_umber": build_oklab_ramp("#2a1714", "#5e2a24", "#b06250", 8),
    "spectral_bone": build_oklab_ramp("#3a3448", "#8a80a0", "#ece6fa", 8),
    "tide_violet": build_oklab_ramp("#2a1638", "#5e2a86", "#b47ae8", 8),
    "histamine_rose": build_oklab_ramp("#3a1420", "#8c2c48", "#ee8aa4", 8),
    "purifier_glow": build_oklab_ramp("#163a3a", "#4aa8a0", "#d8fff6", 8),
    "ember_orange": build_oklab_ramp("#3a1608", "#b84a10", "#ffc46a", 8),
}


def export_gpl(out_path: str) -> None:
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    lines = ["GIMP Palette", "Name: Mycelial Mind Oklab", "Columns: 8", "#"]
    for name, ramp in PALETTES.items():
        for idx, (r, g, b, _) in enumerate(ramp):
            lines.append(f"{r:3d} {g:3d} {b:3d}\t{name}_{idx}")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def export_hex(out_path: str) -> None:
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    lines = []
    for ramp in PALETTES.values():
        for r, g, b, _ in ramp:
            lines.append(f"{r:02x}{g:02x}{b:02x}")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
