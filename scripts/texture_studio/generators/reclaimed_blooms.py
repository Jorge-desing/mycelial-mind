"""Deterministic material atlases for recovered expedition blooms."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

from ..compositor import channels_to_labpbr_s, normals_to_labpbr
from ..sdf_engine import compute_height_normals

PALETTES = {
    "base": (76, 168, 126), "melee": (171, 68, 73),
    "projectile": (62, 161, 192), "fire": (218, 127, 43),
    "explosion": (169, 122, 65), "magic": (147, 92, 190),
}


def generate_reclaimed_blooms(directory, emit_pbr: bool = False):
    Path(directory).mkdir(parents=True, exist_ok=True)
    for name, color in PALETTES.items():
        image = Image.new("RGB", (128, 128), (31, 26, 37))
        for row in range(4):
            for col in range(4):
                tile = Image.new("RGB", (32, 32))
                draw = ImageDraw.Draw(tile)
                x, y = 0, 0
                factor = (0.45, 0.65, 0.83, 1.0)[(row + col) % 4]
                base = tuple(int(c * factor) for c in color)
                light = tuple(min(255, int(c * factor + 45)) for c in color)
                dark = tuple(int(c * factor * 0.55) for c in color)
                draw.rectangle((x, y, x + 31, y + 31), fill=base)
                # Existing model tile coordinates identify distinct organic materials.
                if (col, row) in ((0, 0), (1, 0), (1, 2)):
                    material = {(0, 0): (42, 38, 44), (1, 0): (61, 96, 72),
                                (1, 2): (76, 82, 70)}[(col, row)]
                    if name == "fire" and (col, row) == (1, 2):
                        material = (66, 48, 40)
                    draw.rectangle((x, y, x + 31, y + 31), fill=material)
                    highlight = tuple(min(255, channel + 25) for channel in material)
                    for line in (8, 16, 24):
                        draw.line((x + line, y + 2, x + line - 2, y + 29), fill=highlight, width=2)
                    image.paste(tile, (col * 32, row * 32))
                    continue
                if (col, row) == (2, 0):
                    draw.rectangle((x, y, x + 31, y + 31), fill=(202, 188, 145) if name == "base" else base)
                    draw.ellipse((x + 10, y + 10, x + 21, y + 21), fill=light)
                    image.paste(tile, (col * 32, row * 32))
                    continue
                if name == "base":
                    draw.line((x + 16, y + 30, x + 16, y + 2), fill=light, width=2)
                    for branch in (9, 16, 23):
                        draw.line((x + 16, y + branch + 4, x + 7, y + branch - 2), fill=dark, width=1)
                        draw.line((x + 16, y + branch + 4, x + 25, y + branch - 2), fill=dark, width=1)
                elif name == "melee":
                    for stripe in range(5, 30, 6):
                        draw.line((x + stripe, y + 28, x + stripe + 3, y + 4), fill=dark, width=3)
                        draw.line((x + stripe + 2, y + 28, x + stripe + 5, y + 4), fill=light)
                elif name == "projectile":
                    for stripe in (7, 16, 25):
                        draw.polygon(((x + stripe - 2, y + 28), (x + stripe, y + 2), (x + stripe + 2, y + 28)), fill=light)
                elif name == "fire":
                    for stripe in (5, 13, 21, 29):
                        draw.line((x + stripe, y + 30, x + stripe - 3, y + 19, x + stripe + 2, y + 8, x + stripe, y + 2), fill=light, width=2)
                elif name == "explosion":
                    for edge in (3, 10, 22, 29):
                        draw.line((x + 16, y + 16, x + edge, y + 3), fill=dark, width=2)
                        draw.line((x + 16, y + 16, x + edge, y + 29), fill=dark, width=2)
                    draw.ellipse((x + 13, y + 13, x + 19, y + 19), fill=light)
                else:
                    for offset in (5, 12, 19, 26):
                        draw.line((x + offset, y + 29, x + 16, y + 16, x + offset, y + 3), fill=light, width=1)
                    draw.polygon(((x + 16, y + 5), (x + 22, y + 16), (x + 16, y + 27), (x + 10, y + 16)), outline=dark, width=2)
                # Duplicate edge pixels as gutters around each independently painted tile.
                tile.paste(tile.crop((1, 1, 31, 2)).resize((30, 1)), (1, 0))
                tile.paste(tile.crop((1, 30, 31, 31)).resize((30, 1)), (1, 31))
                tile.paste(tile.crop((1, 0, 2, 32)).resize((1, 32)), (0, 0))
                tile.paste(tile.crop((30, 0, 31, 32)).resize((1, 32)), (31, 0))
                image.paste(tile, (col * 32, row * 32))
        image.save(Path(directory) / f"reclaimed_bloom_{name}.png")
        particle = image.crop((32, 96, 64, 128)).resize((16, 16), Image.Resampling.NEAREST)
        particle.save(Path(directory) / f"reclaimed_bloom_{name}_particle.png")
        if emit_pbr:
            rgb = np.asarray(image.convert("RGB"), dtype=np.float32) / 255.0
            height = np.clip(0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2], 0.0, 1.0)
            nx, ny, nz = compute_height_normals(height, strength=1.8)
            ao = np.clip(0.55 + 0.45 * height, 0.0, 1.0)
            norm_img = normals_to_labpbr(nx, ny, nz, ao=ao, height_map=height)
            smooth = np.clip(0.32 + 0.35 * height, 0.05, 0.85)
            f0 = np.full_like(height, 0.04)
            spec_img = channels_to_labpbr_s(
                smooth,
                f0=f0,
                porosity_sss=np.full_like(height, 0.25),
                emissive=np.zeros_like(height),
            )
            Image.fromarray(norm_img, "RGBA").save(Path(directory) / f"reclaimed_bloom_{name}_n.png")
            Image.fromarray(spec_img, "RGBA").save(Path(directory) / f"reclaimed_bloom_{name}_s.png")
