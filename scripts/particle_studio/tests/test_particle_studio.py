"""Regression tests for the procedural particle studio."""

from __future__ import annotations

import json
import os
import sys
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.particle_studio.build_particles import (
    JSON_DIR,
    MANIFEST_PATH,
    SPECS,
    frame_digest,
    frame_names,
    json_payload,
    registered_ids,
    validate_catalog,
    validate_on_disk,
    validate_sprite,
)
from scripts.particle_studio.generators import render_spec


class ParticleStudioTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.rendered = {spec.id: render_spec(spec) for spec in SPECS}

    def test_registry_matches_catalog_exactly(self) -> None:
        self.assertEqual(sorted(registered_ids()), sorted(spec.id for spec in SPECS))
        self.assertEqual(len(SPECS), 34)
        self.assertGreaterEqual(len({spec.id for spec in SPECS}), 30)

    def test_every_sprite_is_transparent_cornered_legible_and_animated(self) -> None:
        errors = []
        for spec in SPECS:
            errors.extend(validate_sprite(spec, self.rendered[spec.id]))
        self.assertEqual([], errors)

    def test_catalog_silhouettes_are_unique(self) -> None:
        self.assertEqual([], validate_catalog(self.rendered))

    def test_descriptors_and_sprites_are_in_sync(self) -> None:
        errors = validate_on_disk(self.rendered)
        self.assertEqual([], errors)
        for spec in SPECS:
            with open(os.path.join(JSON_DIR, f"{spec.id}.json"), encoding="utf-8") as fh:
                payload = json.loads(fh.read())
            self.assertEqual(json_payload(spec), payload)

    def test_manifest_hashes_match_generated_frames(self) -> None:
        self.assertTrue(os.path.isfile(MANIFEST_PATH), "run build_particles.py --all first")
        with open(MANIFEST_PATH, encoding="utf-8") as fh:
            manifest = json.loads(fh.read())
        self.assertEqual(manifest["particle_count"], len(SPECS))
        for spec in SPECS:
            entry = manifest["specs"][spec.id]
            self.assertEqual(entry["textures"], [f"mycelialmind:{n}" for n in frame_names(spec)])
            self.assertEqual(
                entry["frame_sha256"],
                [frame_digest(frame) for frame in self.rendered[spec.id]],
            )

    def test_generation_is_deterministic_and_non_destructive(self) -> None:
        again = {spec.id: render_spec(spec) for spec in SPECS}
        for spec in SPECS:
            self.assertEqual(
                [frame_digest(f) for f in self.rendered[spec.id]],
                [frame_digest(f) for f in again[spec.id]],
            )


if __name__ == "__main__":
    unittest.main()
