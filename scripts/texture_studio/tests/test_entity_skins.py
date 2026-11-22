"""Regression tests for the per-species entity skin pipeline."""

from __future__ import annotations

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

from scripts.texture_studio.generators.entities import generate_entities
from scripts.texture_studio.generators.entity_skins import (
    ENTITY_SKINS,
    EYE_STYLES,
    MOTIFS,
    NO_RANK_ENTITIES,
    RANK_THEMES,
    box_face_rects,
)
from scripts.texture_studio.palette_oklab import PALETTES, rgb_to_oklab

MANIFEST_PATH = os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json")
TEST_NAMES = {"sporeling", "chitin_brute", "colony_warden"}


def _manifest() -> dict:
    with open(MANIFEST_PATH, encoding="utf-8") as f:
        return json.load(f)


def _ranks(reg_id: str) -> tuple[str, ...]:
    return ("",) if reg_id in NO_RANK_ENTITIES else ("", "_mutated", "_elite")


def _rgba(path: str) -> np.ndarray:
    with Image.open(path) as im:
        return np.asarray(im.convert("RGBA"))


def _island_mask(spec: dict) -> np.ndarray:
    tex_w, tex_h = spec["tex_size"]
    mask = np.zeros((tex_h, tex_w), dtype=bool)
    for cube in spec["cubes"]:
        for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
            mask[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w] = True
    return mask


def _pillow_score(face: np.ndarray) -> float:
    h, w = face.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    xn = (xx - (w - 1) / 2.0) / max(1.0, w / 2.0)
    yn = (yy - (h - 1) / 2.0) / max(1.0, h / 2.0)
    gray = (0.2126 * face[..., 0] + 0.7152 * face[..., 1] + 0.0722 * face[..., 2]) / 255.0
    design = np.stack([np.ones_like(xn), xn, yn, xn * xn, yn * yn, xn * yn], axis=-1).reshape(-1, 6)
    values = gray.reshape(-1)
    coef, *_ = np.linalg.lstsq(design, values, rcond=None)
    residual = ((values - design @ coef) ** 2).sum()
    total = ((values - values.mean()) ** 2).sum()
    return float(1.0 - residual / max(1e-9, total))


class TestEntitySkins(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls._tmp = tempfile.TemporaryDirectory()
        cls.out_dir = cls._tmp.name
        generate_entities(cls.out_dir, pbr_dir=cls.out_dir, names=TEST_NAMES)
        cls.manifest = _manifest()

    @classmethod
    def tearDownClass(cls) -> None:
        cls._tmp.cleanup()

    def _base(self, reg_id: str, rank: str = "") -> np.ndarray:
        return _rgba(os.path.join(self.out_dir, f"{reg_id}{rank}.png"))

    def test_entity_profiles_cover_manifest_and_registries(self):
        self.assertEqual(set(ENTITY_SKINS), set(self.manifest))
        for reg_id, skin in ENTITY_SKINS.items():
            self.assertIn(skin.motif, MOTIFS, reg_id)
            self.assertIn(skin.eye, EYE_STYLES, reg_id)
            self.assertIn(skin.rank_theme, RANK_THEMES, reg_id)
            self.assertIn(skin.accent, PALETTES, reg_id)
            self.assertIn(skin.glow, PALETTES, reg_id)
            self.assertTrue(0.0 <= skin.lift <= 0.15, reg_id)
            self.assertTrue(0.9 <= skin.contrast <= 1.3, reg_id)
            valid_roles = {"skin", "armor", "bone", "spore_sac", "glow_core", "glow_eye", "magma", "metal"}
            for role, palette in skin.role_overrides:
                self.assertIn(role, valid_roles, reg_id)
                self.assertIn(palette, PALETTES, reg_id)

    def test_entity_generation_islands_alpha_and_detail(self):
        for reg_id in TEST_NAMES:
            spec = self.manifest[reg_id]
            expected = tuple(spec["tex_size"])
            islands = _island_mask(spec)
            for rank in _ranks(reg_id):
                base = self._base(reg_id, rank)
                emis = _rgba(os.path.join(self.out_dir, f"{reg_id}{rank}_emissive.png"))
                self.assertEqual(base.shape[:2], (expected[1], expected[0]))
                self.assertEqual(emis.shape, base.shape)
                self.assertTrue(np.all(base[..., 3][islands] == 255), f"{reg_id}{rank} has transparent island texels")
                self.assertFalse(np.any(base[..., 3][~islands] > 0), f"{reg_id}{rank} has opaque texels outside islands")
                self.assertFalse(np.any(emis[..., 3][~islands] > 0), f"{reg_id}{rank} has emissive texels outside islands")

    def test_entity_faces_are_not_flat_or_pillowed(self):
        checked = 0
        for reg_id in TEST_NAMES:
            spec = self.manifest[reg_id]
            base = self._base(reg_id)
            for cube in spec["cubes"]:
                for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
                    if rect.w < 4 or rect.h < 4:
                        continue
                    face = base[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w]
                    opaque = face[..., 3] > 0
                    if int(opaque.sum()) < 8:
                        continue
                    gray = (0.2126 * face[..., 0] + 0.7152 * face[..., 1] + 0.0722 * face[..., 2]) / 255.0
                    self.assertGreater(float(gray[opaque].std()), 0.02, f"{reg_id}/{cube['name']} face is flat")
                    colors = np.unique((face[..., :3][opaque] // 16).dot(np.array([1, 16, 256])))
                    self.assertGreaterEqual(len(colors), 4, f"{reg_id}/{cube['name']} face has too few colors")
                    if rect.w >= 6 and rect.h >= 6:
                        self.assertLess(_pillow_score(face), 0.92, f"{reg_id}/{cube['name']} looks pillow-shaded")
                    checked += 1
        self.assertGreater(checked, 12)

    def test_entity_emissive_is_accent_only_and_has_bloom(self):
        for reg_id in TEST_NAMES:
            spec = self.manifest[reg_id]
            islands = _island_mask(spec)
            for rank in _ranks(reg_id):
                base = self._base(reg_id, rank)
                emis = _rgba(os.path.join(self.out_dir, f"{reg_id}{rank}_emissive.png"))
                ratio = np.count_nonzero(emis[..., 3] > 8) / max(1, int(islands.sum()))
                self.assertGreater(ratio, 0.002, f"{reg_id}{rank} has no emissive accents")
                limit = 0.50 if reg_id in ("hive_core", "apex_matriarch", "spore_behemoth") else 0.35
                self.assertLessEqual(ratio, limit, f"{reg_id}{rank} emissive covers too much")
                self.assertGreater(int(emis[..., 3].max()), 100, f"{reg_id}{rank} emissive is too weak")
                self.assertTrue(np.all(base[..., 3][islands] == 255))

    def test_entity_rank_variants_are_distinct(self):
        for reg_id in TEST_NAMES:
            base = self._base(reg_id)
            mask = base[..., 3] > 0
            lab_base = rgb_to_oklab(base[..., :3][mask].astype(np.float32) / 255.0)
            hashes = {hash(base.tobytes())}
            for rank in ("_mutated", "_elite"):
                variant = self._base(reg_id, rank)
                lab_variant = rgb_to_oklab(variant[..., :3][mask].astype(np.float32) / 255.0)
                delta = np.linalg.norm(lab_base - lab_variant, axis=1)
                self.assertGreater(float(delta.mean()), 0.04, f"{reg_id}{rank} mean delta too small")
                self.assertGreater(float((delta > 0.08).mean()), 0.08, f"{reg_id}{rank} too few changed pixels")
                hashes.add(hash(variant.tobytes()))
            self.assertEqual(len(hashes), 3, f"{reg_id} rank variants are not distinct")

    def test_entity_pbr_matches_height_and_labpbr(self):
        for reg_id in TEST_NAMES:
            spec = self.manifest[reg_id]
            expected = tuple(spec["tex_size"])
            for rank in _ranks(reg_id):
                normal = _rgba(os.path.join(self.out_dir, f"{reg_id}{rank}_n.png"))
                specular = _rgba(os.path.join(self.out_dir, f"{reg_id}{rank}_s.png"))
                self.assertEqual(normal.shape[:2], (expected[1], expected[0]))
                self.assertEqual(specular.shape, normal.shape)
                self.assertGreaterEqual(int(normal[..., 3].min()), 16)
                self.assertFalse(np.any(specular[..., 1] == 255))
                nx = normal[..., 0].astype(np.float32) / 255.0 * 2.0 - 1.0
                self.assertGreater(float(nx.std()), 0.01, f"{reg_id}{rank} normal map is flat")

    def test_entity_generation_is_deterministic(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            generate_entities(first, pbr_dir=None, names={"sporeling"})
            generate_entities(second, pbr_dir=None, names={"sporeling"})
            for suffix in ("", "_emissive", "_mutated", "_mutated_emissive", "_elite", "_elite_emissive"):
                with open(os.path.join(first, f"sporeling{suffix}.png"), "rb") as fa, open(
                    os.path.join(second, f"sporeling{suffix}.png"), "rb"
                ) as fb:
                    self.assertEqual(fa.read(), fb.read(), f"sporeling{suffix} is not deterministic")


if __name__ == "__main__":
    unittest.main()
