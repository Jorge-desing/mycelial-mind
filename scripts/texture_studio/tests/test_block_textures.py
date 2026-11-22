"""Unit tests for the block texture families and their LabPBR companions."""

import hashlib
import json
import os
import sys
import tempfile
import unittest

import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.texture_studio.generators.blocks import generate_blocks

BLOCK_DIR = os.path.join(
    REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures", "block"
)
ANIMATED = {
    "creeping_mycelium_top", "spore_vent_top", "purification_pylon_side", "mycelial_node",
    "incubator_node", "sentinel_node", "symbiotic_node", "mycelial_rift",
    "luminescent_amber_vein", "antibody_cyst",
}
CUTOUT = {
    "bio_chitin_lattice", "bioluminescent_mycelial_cap", "mycelial_tendrils", "spore_bulb",
    "hyphal_vein_dot", "hyphal_vein_line", "hyphal_vein_climb",
}
REQUIRED = {
    "creeping_mycelium_top", "creeping_mycelium_side", "creeping_mycelium_bottom",
    "spore_vent_top", "spore_vent_side", "spore_vent_bottom", "purification_pylon_side",
    "purification_pylon_top", "purification_turret", "mycelial_node", "incubator_node",
    "sentinel_node", "symbiotic_node", "mycelial_stone", "mycelial_rift",
    "reinforced_bio_barrier", "synaptic_research_bench", "hive_altar",
    "matriarch_altar_top", "matriarch_altar_side", "antibody_cyst",
    "antibody_cyst_withered", "ephemeral_chitin", "mycelial_tendrils", "spore_bulb",
    "hyphal_vein_dot", "hyphal_vein_line", "hyphal_vein_climb",
}


def _block_names() -> list[str]:
    return sorted(
        name[:-4] for name in os.listdir(BLOCK_DIR)
        if name.endswith(".png") and not name[:-4].endswith(("_n", "_s"))
    )


class TestBlockTextures(unittest.TestCase):
    def test_families_are_complete_and_rich(self):
        names = _block_names()
        self.assertGreaterEqual(len(names), 31)
        self.assertTrue(REQUIRED.issubset(set(names)), sorted(REQUIRED - set(names)))
        for name in names:
            image = Image.open(os.path.join(BLOCK_DIR, f"{name}.png")).convert("RGBA")
            if name in ANIMATED:
                self.assertEqual(image.size, (16, 64), name)
                image = image.crop((0, 0, 16, 16))
            else:
                self.assertEqual(image.size, (16, 16), name)
            rgba = np.asarray(image)
            alpha = rgba[..., 3] > 0
            if name in CUTOUT:
                self.assertLessEqual(float(alpha.mean()), 0.95, name)
            else:
                self.assertTrue(bool(alpha.all()), name)
            luminance = 0.2126 * rgba[..., 0] + 0.7152 * rgba[..., 1] + 0.0722 * rgba[..., 2]
            values = luminance[alpha]
            contrast = float(np.percentile(values, 90) - np.percentile(values, 10))
            self.assertGreaterEqual(contrast, 35.0, name)
            colours = len({tuple(int(c) for c in row) for row in rgba[alpha][:, :3]})
            self.assertGreaterEqual(colours, 6, name)

    def test_animated_metadata(self):
        for name in ANIMATED:
            path = os.path.join(BLOCK_DIR, f"{name}.png")
            self.assertEqual(Image.open(path).size, (16, 64), name)
            with open(path + ".mcmeta", encoding="utf-8") as handle:
                meta = json.load(handle)
            self.assertIn("animation", meta, name)
            self.assertGreaterEqual(len(meta["animation"].get("frames", [])), 1, name)

    def test_labpbr_pairs_cover_every_block(self):
        for name in _block_names():
            for suffix in ("_n", "_s"):
                path = os.path.join(BLOCK_DIR, f"{name}{suffix}.png")
                self.assertTrue(os.path.exists(path), path)
                pbr = np.asarray(Image.open(path).convert("RGBA"))
                self.assertEqual(pbr.shape, (16, 16, 4), path)
                if suffix == "_n":
                    self.assertGreaterEqual(int(pbr[..., 3].min()), 16, path)
                else:
                    self.assertFalse(bool(np.any(pbr[..., 1] == 255)), path)

    def test_generation_is_deterministic(self):
        keys = ("mycelial_stone", "spore_vent_side", "purification_turret", "creeping_mycelium_top")
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            generate_blocks(first)
            generate_blocks(second)
            for key in keys:
                with open(os.path.join(first, f"{key}.png"), "rb") as handle:
                    digest_a = hashlib.sha1(handle.read()).hexdigest()
                with open(os.path.join(second, f"{key}.png"), "rb") as handle:
                    digest_b = hashlib.sha1(handle.read()).hexdigest()
                self.assertEqual(digest_a, digest_b, key)

    def test_required_layouts_avoid_vanilla_placeholders(self):
        # Every block model must resolve to a mycelialmind texture, never a vanilla block texture.
        model_dir = os.path.join(
            REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "models", "block"
        )
        for filename in sorted(os.listdir(model_dir)):
            if not filename.endswith(".json"):
                continue
            with open(os.path.join(model_dir, filename), encoding="utf-8") as handle:
                model = json.load(handle)
            for key, value in model.get("textures", {}).items():
                self.assertFalse(value.startswith("minecraft:block/"), f"{filename}:{key}={value}")


if __name__ == "__main__":
    unittest.main()
