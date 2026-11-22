"""Unit tests for the Icon Language v2 compositor and the generated 16x16 icon set."""

import itertools
import os
import sys
import unittest

import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.texture_studio.icon_language import IconCanvas, rot_box
from scripts.texture_studio.materials import MATERIALS
from scripts.texture_studio.palette_oklab import PALETTES
from scripts.texture_studio.sdf_engine import sdf_box, sdf_circle, sdf_capsule

ITEM_DIR = os.path.join(
    REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures", "item"
)


def _two_part_icon() -> IconCanvas:
    canvas = IconCanvas(seed=7)
    xx, yy = canvas.xx, canvas.yy
    canvas.paint(sdf_box(xx, yy, -0.10, 0.05, 0.40, 0.45), "chitin_bone", "CHITIN_BONE", tone=0.55, height=0.55)
    canvas.paint(sdf_circle(xx, yy, 0.22, -0.10, 0.28), "predator_amethyst", "LIVING_AMETHYST", tone=0.70, height=0.75, emissive=True)
    return canvas


class TestIconLanguage(unittest.TestCase):
    def test_union_silhouette_and_part_zones(self):
        canvas = _two_part_icon()
        rgba = canvas.finish()
        mask = rgba[..., 3] > 0
        self.assertTrue(mask[8, 8])
        self.assertTrue(mask[6, 8])
        # The round part is amethyst (material index of LIVING_AMETHYST) and the box is chitin.
        self.assertEqual(canvas.material[6, 8], list(MATERIALS).index("LIVING_AMETHYST"))
        self.assertEqual(canvas.material[10, 6], list(MATERIALS).index("CHITIN_BONE"))

    def test_labpbr_material_zones(self):
        canvas = _two_part_icon()
        normal, spec = canvas.pbr_maps()
        self.assertEqual(normal.shape, (16, 16, 4))
        self.assertEqual(spec.shape, (16, 16, 4))
        # Dielectric chitin: F0 <= 229 and porosity <= 64
        self.assertLessEqual(int(spec[12, 3, 1]), 229)
        self.assertLessEqual(int(spec[12, 3, 2]), 64)
        # Living amethyst is a subsurface material: B in the SSS range
        self.assertGreaterEqual(int(spec[6, 8, 2]), 65)
        # Emissive accent: alpha channel below 255
        self.assertLess(int(spec[6, 8, 3]), 255)

    def test_metal_material_uses_labpbr_metal_code(self):
        canvas = IconCanvas(seed=9)
        xx, yy = canvas.xx, canvas.yy
        canvas.paint(sdf_circle(xx, yy, 0.0, 0.0, 0.5), "royal_gold", "PURIFICATION_GOLD", tone=0.7)
        _, spec = canvas.pbr_maps()
        self.assertGreaterEqual(int(spec[8, 8, 1]), 230)
        self.assertLessEqual(int(spec[8, 8, 1]), 254)

    def test_outline_margin_and_dark_edge(self):
        canvas = IconCanvas(seed=5)
        xx, yy = canvas.xx, canvas.yy
        canvas.paint(sdf_box(xx, yy, 0.0, 0.0, 0.55, 0.55), "spore_chartreuse", "TOXIC_SPORE", tone=0.6)
        rgba = canvas.finish()
        alpha = rgba[..., 3] > 0
        ys, xs = np.nonzero(alpha)
        self.assertGreaterEqual(min(int(xs.min()), int(ys.min()), 15 - int(xs.max()), 15 - int(ys.max())), 1)
        outline = alpha & ~canvas.mask
        self.assertTrue(np.any(outline))
        self.assertLess(int(rgba[outline][0, :3].max()), 40)

    def test_boolean_mask_zone_is_local(self):
        canvas = _two_part_icon()
        before = canvas.ramp.copy()
        local = np.zeros((16, 16), dtype=bool)
        local[5:7, 5:7] = True
        canvas.zone(local, "leech_crimson", "BIOLUM_MYCELIUM", tone=0.9)
        changed = canvas.ramp != before
        self.assertEqual(int(changed.sum()), int((local & canvas.mask).sum()))
        self.assertFalse(np.any(changed[12, :]))

    def test_detail_layers_add_colours_and_contrast(self):
        canvas = _two_part_icon()
        xx, yy = canvas.xx, canvas.yy
        canvas.zone(sdf_capsule(xx, yy, -0.40, -0.30, 0.30, 0.30, 0.035), "abyssal_obsidian", "OBSIDIAN_CHITIN", tone=0.15)
        canvas.emboss(np.abs(sdf_capsule(xx, yy, -0.35, 0.20, 0.15, -0.20, 0.03)) < 0.03, 0.3)
        rgba = canvas.finish()
        opaque = rgba[rgba[..., 3] > 0][:, :3]
        self.assertGreaterEqual(len({tuple(int(c) for c in row) for row in opaque}), 6)
        luminance = 0.2126 * opaque[:, 0] + 0.7152 * opaque[:, 1] + 0.0722 * opaque[:, 2]
        self.assertGreater(float(np.percentile(luminance, 90) - np.percentile(luminance, 10)), 60.0)

    def test_rotated_box_and_determinism(self):
        xx, yy = IconCanvas(seed=1).xx, IconCanvas(seed=1).yy
        rotated = rot_box(xx, yy, 0.0, 0.0, 0.4, 0.4, 0.35)
        self.assertLess(float(rotated[8, 8]), 0.0)
        first = _two_part_icon().finish()
        second = _two_part_icon().finish()
        self.assertTrue(np.array_equal(first, second))


class TestGeneratedItemIcons(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.names = sorted(
            name[:-4]
            for name in os.listdir(ITEM_DIR)
            if name.endswith(".png") and not name[:-4].endswith(("_n", "_s"))
        )
        cls.arrays = {}
        for name in cls.names:
            image = Image.open(os.path.join(ITEM_DIR, f"{name}.png")).convert("RGBA")
            if image.height > image.width:
                image = image.crop((0, 0, 16, 16))
            cls.arrays[name] = np.asarray(image)

    def test_all_registered_items_have_icons(self):
        for required in (
            "bio_scanner", "mycelial_codex", "enzyme_lance", "latent_spore", "hive_heart",
            "chitin_helmet", "chitin_chestplate", "chitin_leggings", "chitin_boots",
            "symbiotic_blade", "tide_beacon", "overmind_crown",
        ):
            self.assertIn(required, self.names)
        self.assertGreaterEqual(len(self.names), 36)

    def test_every_icon_is_readable(self):
        for name, rgba in self.arrays.items():
            alpha = rgba[..., 3] > 0
            coverage = float(alpha.mean())
            self.assertGreaterEqual(coverage, 0.10, name)
            self.assertLessEqual(coverage, 0.55, name)
            ys, xs = np.nonzero(alpha)
            margin = min(int(xs.min()), int(ys.min()), 15 - int(xs.max()), 15 - int(ys.max()))
            self.assertGreaterEqual(margin, 1, name)
            luminance = 0.2126 * rgba[..., 0] + 0.7152 * rgba[..., 1] + 0.0722 * rgba[..., 2]
            values = luminance[alpha]
            contrast = float(np.percentile(values, 90) - np.percentile(values, 10))
            self.assertGreaterEqual(contrast, 90.0, name)

    def test_silhouettes_are_distinct(self):
        masks = {name: rgba[..., 3] > 0 for name, rgba in self.arrays.items()}
        worst = 0.0
        for left, right in itertools.combinations(self.names, 2):
            intersection = np.logical_and(masks[left], masks[right]).sum()
            union = np.logical_or(masks[left], masks[right]).sum()
            worst = max(worst, float(intersection) / float(union) if union else 0.0)
        self.assertLessEqual(worst, 0.95)

    def test_icon_families_use_distinct_ramps(self):
        from scripts.texture_studio.icon_language import RAMP_NAMES

        self.assertGreaterEqual(len(RAMP_NAMES), 25)
        self.assertIn("spore_chartreuse", PALETTES)
        self.assertIn("predator_amethyst", PALETTES)

    def test_codex_book_gui_atlas_exists_and_has_rich_regions(self):
        gui_path = os.path.join(
            REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures", "gui", "codex_book.png"
        )
        self.assertTrue(os.path.exists(gui_path))
        rgba = np.asarray(Image.open(gui_path).convert("RGBA"))
        self.assertEqual(rgba.shape, (256, 256, 4))
        # Main tome frame (0..200, 0..256) is fully opaque
        self.assertTrue(np.all(rgba[:200, :256, 3] == 255))
        # Widget sprites row (slot frame, pedestal, arrow, chapter tabs, medallion) are populated
        self.assertTrue(np.all(rgba[204:222, 0:18, 3] == 255))
        self.assertTrue(np.all(rgba[204:226, 20:42, 3] == 255))
        self.assertGreater(int(np.count_nonzero(rgba[204:220, 44:66, 3] > 0)), 40)
        self.assertTrue(np.all(rgba[204:222, 68:88, 3] == 255))
        self.assertTrue(np.all(rgba[204:222, 90:110, 3] == 255))
        self.assertGreater(int(np.count_nonzero(rgba[204:220, 112:128, 3] > 0)), 100)


if __name__ == "__main__":
    unittest.main()