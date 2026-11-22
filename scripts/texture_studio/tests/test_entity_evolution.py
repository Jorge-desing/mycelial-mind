"""Contract tests for the mob skin v2 evolution: bevel, signatures, eyes and determinism."""

import hashlib
import json
import os
import sys
import tempfile
import unittest
from types import SimpleNamespace

import numpy as np
from PIL import Image

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.texture_studio.generators.entities import generate_entities
from scripts.texture_studio.generators.entity_skins import (
    ENTITY_SKINS,
    SIGNATURES,
    _apply_edge,
    _apply_signature,
    box_face_rects,
)
from scripts.texture_studio.palette_oklab import PALETTES

TEX_DIR = os.path.join(
    REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures", "entity"
)
MANIFEST = os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json")


def _head_face(name: str, kind: str = "front"):
    with open(MANIFEST, encoding="utf-8") as handle:
        spec = json.load(handle)[name]
    candidates = [cube for cube in spec["cubes"] if cube.get("eye_pattern")]
    if not candidates:
        candidates = [cube for cube in spec["cubes"] if cube.get("role") == "skin"]
    cube = max(candidates, key=lambda c: c["w"] * c["h"])
    for rect in box_face_rects(cube["u"], cube["v"], cube["w"], cube["h"], cube["d"]):
        if rect.kind == kind:
            return rect
    raise AssertionError(f"No {kind} face for {name}")


class TestMobSkinV2(unittest.TestCase):
    def test_bevel_lights_top_left_and_darkens_bottom_right(self):
        face = np.full((8, 8, 4), 100, dtype=np.uint8)
        face[..., 3] = 255
        _apply_edge(face, 8, 8, "hard", "front")
        self.assertGreater(float(face[0, :, :3].mean()), 100.0)
        self.assertLess(float(face[-1, :, :3].mean()), 100.0)
        self.assertGreater(float(face[:, 0, :3].mean()), 100.0)
        self.assertLess(float(face[:, -1, :3].mean()), 100.0)

    def test_every_species_has_a_signature(self):
        self.assertEqual(set(SIGNATURES), set(ENTITY_SKINS))
        for name in ENTITY_SKINS:
            ctx = SimpleNamespace(
                rect=SimpleNamespace(w=8, h=8, kind="front"),
                role="skin",
                reg_id=name,
                rank="base",
                ramp=PALETTES["venom_emerald"],
                accent_ramp=PALETTES["spore_chartreuse"],
            )
            face = np.full((8, 8, 4), 96, dtype=np.uint8)
            face[..., 3] = 255
            before = face.copy()
            out = _apply_signature(ctx, face)
            self.assertFalse(np.array_equal(out, before), f"{name} signature painted nothing")

    def test_head_faces_have_glowing_eyes_and_contrast(self):
        with open(MANIFEST, encoding="utf-8") as handle:
            manifest = json.load(handle)
        for name in manifest:
            rect = _head_face(name)
            rgba = np.asarray(Image.open(os.path.join(TEX_DIR, f"{name}.png")).convert("RGBA"))
            emis = np.asarray(Image.open(os.path.join(TEX_DIR, f"{name}_emissive.png")).convert("RGBA"))
            face = rgba[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w]
            glow = emis[rect.y : rect.y + rect.h, rect.x : rect.x + rect.w]
            self.assertGreater(int((glow[..., 3] > 8).sum()), 0, f"{name} has no glowing eye pixels")
            luminance = 0.2126 * face[..., 0] + 0.7152 * face[..., 1] + 0.0722 * face[..., 2]
            contrast = float(np.percentile(luminance, 90) - np.percentile(luminance, 10))
            self.assertGreaterEqual(contrast, 55.0, f"{name} head face too flat ({contrast:.1f})")

    def test_generation_is_deterministic_for_two_species(self):
        names = {"sporeling", "chitin_brute"}
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            generate_entities(first, pbr_dir=first, names=names)
            generate_entities(second, pbr_dir=second, names=names)
            for filename in sorted(os.listdir(first)):
                if not filename.endswith(".png"):
                    continue
                with open(os.path.join(first, filename), "rb") as handle:
                    digest_a = hashlib.sha1(handle.read()).hexdigest()
                with open(os.path.join(second, filename), "rb") as handle:
                    digest_b = hashlib.sha1(handle.read()).hexdigest()
                self.assertEqual(digest_a, digest_b, filename)


if __name__ == "__main__":
    unittest.main()
