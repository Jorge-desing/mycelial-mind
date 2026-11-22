"""LayerStack compositor, emissive bloom post-FX, and LabPBR normal map encoder."""

from __future__ import annotations

import numpy as np
from PIL import Image, ImageFilter


def _blend_rgb(dst: np.ndarray, src: np.ndarray, mode: str) -> np.ndarray:
    if mode == "normal":
        return src
    if mode == "multiply":
        return dst * src
    if mode == "screen":
        return 1.0 - (1.0 - dst) * (1.0 - src)
    if mode == "add":
        return np.clip(dst + src, 0.0, 1.0)
    if mode == "overlay":
        return np.where(dst < 0.5, 2.0 * dst * src, 1.0 - 2.0 * (1.0 - dst) * (1.0 - src))
    raise ValueError(f"Unsupported blend mode: {mode}")


class LayerStack:
    """Alpha-aware RGBA layer compositor supporting standard blend modes."""

    def __init__(self, w: int, h: int):
        self.w = w
        self.h = h
        self.canvas = np.zeros((h, w, 4), dtype=np.float32)

    def add_layer(self, rgba: np.ndarray, mode: str = "normal", opacity: float = 1.0) -> "LayerStack":
        src = rgba.astype(np.float32) / 255.0
        src_a = np.clip(src[..., 3:4] * opacity, 0.0, 1.0)
        dst_rgb = self.canvas[..., :3]
        dst_a = self.canvas[..., 3:4]

        blended = _blend_rgb(dst_rgb, src[..., :3], mode)
        # Porter-Duff source-over compositing with blended RGB over opaque regions
        mixed_rgb = np.where(dst_a > 1e-4, blended, src[..., :3])
        out_a = src_a + dst_a * (1.0 - src_a)
        safe_a = np.maximum(out_a, 1e-6)
        out_rgb = (mixed_rgb * src_a + dst_rgb * dst_a * (1.0 - src_a)) / safe_a
        self.canvas = np.concatenate([np.clip(out_rgb, 0.0, 1.0), np.clip(out_a, 0.0, 1.0)], axis=-1)
        return self

    def flatten(self) -> np.ndarray:
        return np.clip(self.canvas * 255.0, 0, 255).astype(np.uint8)


def apply_emissive_bloom(emis_rgba: np.ndarray, radius: int = 2, intensity: float = 0.45) -> np.ndarray:
    """Add a soft bioluminescent halo around emissive pixels without altering opaque core pixels."""
    pil_im = Image.fromarray(emis_rgba, mode="RGBA")
    blurred = np.array(pil_im.filter(ImageFilter.GaussianBlur(radius=radius)), dtype=np.float32)
    orig = emis_rgba.astype(np.float32)
    halo = np.clip(blurred * intensity, 0.0, 255.0)
    out = np.where(orig[..., 3:4] > 0, orig, halo)
    return np.clip(out, 0, 255).astype(np.uint8)


def normals_to_labpbr(
    nx: np.ndarray,
    ny: np.ndarray,
    nz: np.ndarray,
    ao: np.ndarray | None = None,
    height_map: np.ndarray | None = None,
) -> np.ndarray:
    """Encode tangent-space normals + AO + POM height into LabPBR 1.3 _n.png (R=nx, G=ny, B=ao, A=POM height)."""
    r = np.clip((nx * 0.5 + 0.5) * 255.0, 0, 255).astype(np.uint8)
    g = np.clip((-ny * 0.5 + 0.5) * 255.0, 0, 255).astype(np.uint8)
    if ao is None:
        b = np.clip((nz * 0.5 + 0.5) * 255.0, 0, 255).astype(np.uint8)
    else:
        b = np.clip(ao * 255.0, 0, 255).astype(np.uint8)
    if height_map is None:
        a = np.full_like(r, 255, dtype=np.uint8)
    else:
        # LabPBR POM: 255 = surface level, 16..254 = recessed depth (0 avoided as invalid cutout)
        a = np.clip(height_map * 255.0, 16, 255).astype(np.uint8)
    return np.stack([r, g, b, a], axis=-1)


def channels_to_labpbr_s(
    smoothness: np.ndarray,
    f0: np.ndarray,
    porosity_sss: np.ndarray,
    emissive: np.ndarray,
    is_subsurface: bool = False,
    metal_id: int | None = None,
) -> np.ndarray:
    """Encode LabPBR 1.3 _s.png (R=Smoothness, G=F0 0..229 / Metal 230..254, B=Porosity 0..64 / SSS 65..255, A=Emissive with 255=none)."""
    r = np.clip(smoothness * 255.0, 0, 255).astype(np.uint8)
    m_code = int(np.clip(231 if metal_id is None else metal_id, 230, 254))
    g_dielectric = np.clip(f0 * 255.0, 0, 229).astype(np.uint8)
    g = np.where(f0 >= 0.90, np.full_like(r, m_code, dtype=np.uint8), g_dielectric)
    p = np.clip(porosity_sss, 0.0, 1.0)
    if is_subsurface:
        b = np.where(p > 0.01, np.clip(65.0 + p * 190.0, 65, 255), 0.0).astype(np.uint8)
    else:
        b = np.clip(p * 64.0, 0, 64).astype(np.uint8)
    # LabPBR 1.3 spec: A=255 means NO emission; A=1..254 encodes emissive intensity
    em_scaled = np.clip(emissive * 254.0, 1, 254).astype(np.uint8)
    a = np.where(emissive > 0.02, em_scaled, np.full_like(r, 255, dtype=np.uint8))
    return np.stack([r, g, b, a], axis=-1)
