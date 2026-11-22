from __future__ import annotations

import json
import os
import sys
import tempfile
import time
import types
import unittest
from unittest import mock
import numpy as np
from PIL import Image

import scripts.visual_regression as vr
from scripts.structure_visual_check import (
    EXPECTED_SHOTS,
    compute_foreground_ratio,
    validate_structure_shots,
)
from scripts.visual_regression import (
    compare_images,
    compute_diff_regions,
    compute_lpips,
    compute_phash,
    compute_ssim,
    hamming_distance,
    save_diff_heatmap,
)


class VisualRegressionTests(unittest.TestCase):
    def test_identical_and_perturbed_images(self) -> None:
        rng = np.random.default_rng(42)
        arr = rng.integers(30, 220, size=(64, 64), dtype=np.uint8)
        with tempfile.TemporaryDirectory() as tmp:
            p1 = os.path.join(tmp, "a.png")
            p2 = os.path.join(tmp, "b.png")
            p3 = os.path.join(tmp, "c.png")
            Image.fromarray(arr, mode="L").save(p1)
            Image.fromarray(arr, mode="L").save(p2)
            Image.fromarray(255 - arr, mode="L").save(p3)

            ssim_same, dist_same = compare_images(p1, p2)
            self.assertAlmostEqual(ssim_same, 1.0, places=5)
            self.assertEqual(dist_same, 0)

            ssim_inv = compute_ssim(arr.astype(np.float64), (255 - arr).astype(np.float64))
            dist_inv = hamming_distance(compute_phash(p1), compute_phash(p3))
            self.assertLess(ssim_inv, 0.2)
            self.assertGreater(dist_inv, 10)

    def test_diff_regions_and_heatmap_localization(self) -> None:
        base = np.full((64, 64), 100, dtype=np.uint8)
        curr = base.copy()
        curr[16:32, 32:48] = 220
        regions = compute_diff_regions(base.astype(np.float64), curr.astype(np.float64), threshold=18.0, grid_size=16)
        self.assertEqual(len(regions), 1)
        x0, y0, x1, y1, max_delta = regions[0]
        self.assertEqual((x0, y0, x1, y1), (32, 16, 48, 32))
        self.assertGreaterEqual(max_delta, 100.0)

        with tempfile.TemporaryDirectory() as tmp:
            p1 = os.path.join(tmp, "base.png")
            p2 = os.path.join(tmp, "curr.png")
            hm = os.path.join(tmp, "heatmap.png")
            Image.fromarray(base, mode="L").save(p1)
            Image.fromarray(curr, mode="L").save(p2)
            out_regions = save_diff_heatmap(p1, p2, hm)
            self.assertEqual(len(out_regions), 1)
            self.assertTrue(os.path.isfile(hm))
            lp = compute_lpips(p1, p1)
            if lp is not None:
                self.assertLess(lp, 0.01)

    def test_lpips_preprocessing_and_model_contract(self) -> None:
        captured: list[tuple[np.ndarray, np.ndarray]] = []

        class _FakeTensor:
            def __init__(self, arr: np.ndarray) -> None:
                self.arr = arr

            @property
            def shape(self) -> tuple[int, ...]:
                return self.arr.shape

            def permute(self, *axes: int) -> "_FakeTensor":
                return _FakeTensor(np.transpose(self.arr, axes))

            def unsqueeze(self, axis: int) -> "_FakeTensor":
                return _FakeTensor(np.expand_dims(self.arr, axis=axis))

            def __truediv__(self, other: float) -> "_FakeTensor":
                return _FakeTensor(self.arr / other)

            def __sub__(self, other: float) -> "_FakeTensor":
                return _FakeTensor(self.arr - other)

            def item(self) -> float:
                return float(self.arr.item())

        class _FakeNoGrad:
            def __enter__(self) -> None:
                return None

            def __exit__(self, *args: object) -> bool:
                return False

        class _FakeModel:
            def eval(self) -> None:
                pass

            def __call__(self, a: _FakeTensor, b: _FakeTensor) -> _FakeTensor:
                captured.append((a.arr, b.arr))
                return _FakeTensor(np.array([float(np.mean(np.abs(a.arr - b.arr)))]))

        fake_torch = types.ModuleType("torch")
        fake_torch.from_numpy = lambda arr: _FakeTensor(arr)
        fake_torch.no_grad = lambda: _FakeNoGrad()

        fake_lpips = types.ModuleType("lpips")
        fake_lpips.LPIPS = lambda net="alex", verbose=False: _FakeModel()

        with tempfile.TemporaryDirectory() as tmp:
            p1 = os.path.join(tmp, "large_a.png")
            p2 = os.path.join(tmp, "large_b.png")
            img_a = np.full((400, 600, 3), 40, dtype=np.uint8)
            img_b = np.full((200, 300, 3), 210, dtype=np.uint8)
            Image.fromarray(img_a, mode="RGB").save(p1)
            Image.fromarray(img_b, mode="RGB").save(p2)

            with mock.patch.dict(sys.modules, {"torch": fake_torch, "lpips": fake_lpips}):
                with mock.patch.object(vr, "_LPIPS_MODEL", None):
                    val = compute_lpips(p1, p2)

        self.assertIsNotNone(val)
        assert val is not None
        self.assertGreater(val, 0.0)
        self.assertEqual(len(captured), 1)
        ta, tb = captured[0]
        self.assertEqual(ta.shape, tb.shape)
        self.assertEqual(ta.ndim, 4)
        self.assertEqual(ta.shape[0], 1)
        self.assertEqual(ta.shape[1], 3)
        self.assertLessEqual(max(ta.shape[2], ta.shape[3]), 512)
        self.assertGreaterEqual(float(ta.min()), -1.01)
        self.assertLessEqual(float(tb.max()), 1.01)


class StructureVisualCheckTests(unittest.TestCase):
    @staticmethod
    def _make_scene(w: int = 360, h: int = 240, *, include_structure: bool = True, seed: int = 1) -> np.ndarray:
        img = np.zeros((h, w, 3), dtype=np.uint8)
        # Flat-world sky (top half) and green grass (bottom half)
        img[: h // 2, :] = (128, 174, 245)
        img[h // 2 :, :] = (92, 140, 62)
        if include_structure:
            rng = np.random.default_rng(seed)
            # Central mycelial citadel/dome silhouette in purple/dark basalt + amber veins
            y0, y1 = h // 5, (4 * h) // 5
            x0, x1 = w // 4, (3 * w) // 4
            patch = rng.integers(35, 110, size=(y1 - y0, x1 - x0, 3), dtype=np.uint8)
            patch[..., 0] = np.clip(patch[..., 0].astype(np.int16) + 35, 0, 255).astype(np.uint8)
            patch[..., 2] = np.clip(patch[..., 2].astype(np.int16) + 55, 0, 255).astype(np.uint8)
            img[y0:y1, x0:x1] = patch
            # Amber accents
            img[y0 + 10 : y0 + 18, x0 + 10 : x1 - 10] = (210, 165, 48)
        return img

    def test_foreground_ratio_rejects_empty_grass_and_accepts_structure(self) -> None:
        empty = self._make_scene(include_structure=False)
        structured = self._make_scene(include_structure=True)
        self.assertLess(compute_foreground_ratio(empty), 0.02)
        self.assertGreater(compute_foreground_ratio(structured), 0.15)

    def test_validate_structure_shots_lifecycle_and_regression(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            shot_dir = os.path.join(tmp, "shots")
            base_dir = os.path.join(tmp, "baselines")
            arch_dir = os.path.join(tmp, "archive")
            os.makedirs(shot_dir, exist_ok=True)

            for idx, (fname, _title) in enumerate(EXPECTED_SHOTS):
                arr = self._make_scene(include_structure=True, seed=100 + idx)
                Image.fromarray(arr, mode="RGB").save(os.path.join(shot_dir, fname))

            meta_path = os.path.join(shot_dir, "structure_smoke_meta.json")
            valid_meta = {
                "outbreak_placed": True,
                "hive_placed": True,
                "sanctum_placed": True,
                "citadel_placed": True,
                "frozen_guardians": 3,
                "shots_captured": [name for name, _ in EXPECTED_SHOTS],
                "all_placed": True,
                "timed_out": False,
            }
            with open(meta_path, "w", encoding="utf-8") as fh:
                json.dump(valid_meta, fh)

            # 1) Create baselines after passing validation
            code_upd, rep_upd = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
                update_baseline=True,
            )
            self.assertEqual(code_upd, 0, rep_upd)
            self.assertEqual(rep_upd["baseline_mode"], "updated")
            self.assertTrue(os.path.isfile(os.path.join(arch_dir, "structures_contact_sheet.png")))
            self.assertTrue(os.path.isfile(os.path.join(arch_dir, "structure-visual-smoke-latest.json")))
            for fname, _ in EXPECTED_SHOTS:
                self.assertTrue(os.path.isfile(os.path.join(base_dir, fname)))

            # 2) Clean run against baselines passes with SSIM=1.0, Hamming=0
            code_ok, rep_ok = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
                require_baseline=True,
            )
            self.assertEqual(code_ok, 0, rep_ok)
            self.assertEqual(rep_ok["baseline_mode"], "compared")
            for fname, _ in EXPECTED_SHOTS:
                self.assertGreaterEqual(rep_ok["shots"][fname]["ssim"], 0.999)
                self.assertEqual(rep_ok["shots"][fname]["phash_hamming"], 0)

            # 3) Freshness guard fails when min_mtime is in the future
            code_stale, rep_stale = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
                min_mtime=time.time() + 3600.0,
            )
            self.assertEqual(code_stale, 1, rep_stale)
            self.assertFalse(rep_stale["ok"])

            # 4) Regression fail on perturbed shot + diff heatmap created
            target_shot = EXPECTED_SHOTS[1][0]
            perturbed = 255 - self._make_scene(include_structure=True, seed=999)
            Image.fromarray(perturbed, mode="RGB").save(os.path.join(shot_dir, target_shot))
            code_reg, rep_reg = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
                require_baseline=True,
            )
            self.assertEqual(code_reg, 2, rep_reg)
            self.assertIn(target_shot, rep_reg["failed_shots"])
            self.assertTrue(os.path.isfile(os.path.join(arch_dir, f"diff_{target_shot}")))

            # Restore clean shot for subsequent checks
            Image.fromarray(self._make_scene(include_structure=True, seed=101), mode="RGB").save(
                os.path.join(shot_dir, target_shot)
            )

            # 5) Foreground guard fails on empty grass+sky
            Image.fromarray(self._make_scene(include_structure=False), mode="RGB").save(
                os.path.join(shot_dir, EXPECTED_SHOTS[0][0])
            )
            code_fg, rep_fg = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
            )
            self.assertEqual(code_fg, 1, rep_fg)
            self.assertTrue(any("foreground guard" in e for e in rep_fg["errors"]))

            # Restore shot 0 and verify all_placed=False fails metadata guard
            Image.fromarray(self._make_scene(include_structure=True, seed=100), mode="RGB").save(
                os.path.join(shot_dir, EXPECTED_SHOTS[0][0])
            )
            bad_meta = dict(valid_meta, all_placed=False, citadel_placed=False)
            with open(meta_path, "w", encoding="utf-8") as fh:
                json.dump(bad_meta, fh)
            code_meta, rep_meta = validate_structure_shots(
                screenshot_dir=shot_dir,
                baseline_dir=base_dir,
                archive_dir=arch_dir,
            )
            self.assertEqual(code_meta, 1, rep_meta)
            self.assertTrue(any("all_placed=False" in e for e in rep_meta["errors"]))

    def test_particle_drift_high_ssim_phash_tolerance(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            shot_dir = os.path.join(tmp, "shots")
            base_dir = os.path.join(tmp, "baselines")
            arch_dir = os.path.join(tmp, "archive")
            os.makedirs(shot_dir, exist_ok=True)
            os.makedirs(base_dir, exist_ok=True)

            for idx, (fname, _title) in enumerate(EXPECTED_SHOTS):
                arr = self._make_scene(include_structure=True, seed=200 + idx)
                Image.fromarray(arr, mode="RGB").save(os.path.join(shot_dir, fname))
                Image.fromarray(arr, mode="RGB").save(os.path.join(base_dir, fname))

            meta_path = os.path.join(shot_dir, "structure_smoke_meta.json")
            with open(meta_path, "w", encoding="utf-8") as fh:
                json.dump(
                    {
                        "outbreak_placed": True,
                        "hive_placed": True,
                        "sanctum_placed": True,
                        "citadel_placed": True,
                        "frozen_guardians": 3,
                        "shots_captured": [name for name, _ in EXPECTED_SHOTS],
                        "all_placed": True,
                        "timed_out": False,
                    },
                    fh,
                )

            with mock.patch("scripts.structure_visual_check.compare_images", return_value=(0.992, 10)):
                with mock.patch("scripts.structure_visual_check.compute_lpips", return_value=0.02):
                    code_pass, rep_pass = validate_structure_shots(
                        screenshot_dir=shot_dir,
                        baseline_dir=base_dir,
                        archive_dir=arch_dir,
                        require_baseline=True,
                    )
                    self.assertEqual(code_pass, 0, rep_pass)
                    self.assertTrue(rep_pass["ok"])

            with mock.patch("scripts.structure_visual_check.compare_images", return_value=(0.95, 10)):
                with mock.patch("scripts.structure_visual_check.compute_lpips", return_value=0.08):
                    code_fail, rep_fail = validate_structure_shots(
                        screenshot_dir=shot_dir,
                        baseline_dir=base_dir,
                        archive_dir=arch_dir,
                        require_baseline=True,
                    )
                    self.assertEqual(code_fail, 2, rep_fail)
                    self.assertFalse(rep_fail["ok"])


if __name__ == "__main__":
    unittest.main()
