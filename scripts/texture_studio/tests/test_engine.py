"""Automated unit tests for sdf_engine v2, palette_oklab, and compositor."""

import os
import sys
import unittest
import numpy as np

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.texture_studio.compositor import LayerStack, apply_emissive_bloom, channels_to_labpbr_s, normals_to_labpbr
from scripts.texture_studio.materials import MATERIALS
from scripts.texture_studio.palette_oklab import PALETTES, oklab_to_rgb, rgb_to_oklab
from scripts.texture_studio.sdf_engine import (
    bayer_quantize,
    compute_height_normals,
    compute_sdf_curvature,
    compute_sdf_lighting_v2,
    compute_sdf_normals,
    domain_warp,
    floyd_steinberg_quantize,
    gray_scott_toroidal,
    make_grid,
    sdf_bezier,
    sdf_circle,
    sdf_onion,
    sdf_polygon,
    sdf_subtract,
    sdf_to_pom_height,
    toroidal_fbm,
    toroidal_value_fbm,
    toroidal_value_noise,
)


class TestTextureStudioV2(unittest.TestCase):
    def test_sdf_boolean_and_primitives(self):
        xx, yy = make_grid(32, 32)
        c1 = sdf_circle(xx, yy, 0.0, 0.0, 0.5)
        c2 = sdf_circle(xx, yy, 0.0, 0.0, 0.25)
        ring = sdf_subtract(c1, c2)
        self.assertGreater(ring[16, 16], 0.0)  # center carved out
        self.assertLess(ring[16, 22], 0.0)     # inside ring

        onion = sdf_onion(c1, 0.05)
        self.assertLess(onion.min(), 0.0)

        poly = sdf_polygon(xx, yy, 0.0, 0.0, 0.5, sides=6)
        self.assertLess(poly[16, 16], 0.0)

        bez = sdf_bezier(xx, yy, (-0.8, 0.0), (0.0, 0.4), (0.8, 0.0), 0.1)
        self.assertLess(bez.min(), 0.0)

    def test_toroidal_seamless_continuity(self):
        noise = toroidal_fbm(32, 32, seed=77, freq=2.0)
        self.assertEqual(noise.shape, (32, 32))
        self.assertGreaterEqual(float(noise.min()), 0.0)
        self.assertLessEqual(float(noise.max()), 1.0)
        # Opposite borders should be close in periodic space
        self.assertLess(float(np.mean(np.abs(noise[:, 0] - noise[:, -1]))), 0.25)

        xx, yy = make_grid(32, 32)
        wx, wy = domain_warp(xx, yy, seed=77, strength=0.15, freq=2.0)
        self.assertEqual(wx.shape, (32, 32))
        self.assertEqual(wy.shape, (32, 32))

    def test_normals_lighting_v2_and_bayer(self):
        xx, yy = make_grid(32, 32)
        d = sdf_circle(xx, yy, 0.0, 0.0, 0.6)
        nx, ny, nz = compute_sdf_normals(d)
        mag = np.sqrt(nx * nx + ny * ny + nz * nz)
        self.assertTrue(np.allclose(mag, 1.0, atol=1e-4))

        lit = compute_sdf_lighting_v2(d)
        self.assertGreaterEqual(float(lit.min()), 0.0)
        self.assertLessEqual(float(lit.max()), 1.0)

        for m in (4, 8):
            q = bayer_quantize(lit, PALETTES["mycelial_violet"], matrix=m)
            self.assertEqual(q.shape, (32, 32, 4))
            self.assertEqual(q.dtype, np.uint8)

    def test_oklab_roundtrip(self):
        rgb = np.array([[0.2, 0.5, 0.8]], dtype=np.float32)
        rt = oklab_to_rgb(rgb_to_oklab(rgb))
        self.assertTrue(np.allclose(rgb, rt, atol=1e-3))
        self.assertGreaterEqual(len(PALETTES), 19)
        for name, ramp in PALETTES.items():
            self.assertEqual(ramp.shape, (8, 4), f"{name} expected 8-step RGBA ramp")

    def test_compositor_and_labpbr(self):
        base = np.full((16, 16, 4), [60, 40, 80, 255], dtype=np.uint8)
        top = np.full((16, 16, 4), [120, 200, 220, 128], dtype=np.uint8)
        for mode in ("normal", "multiply", "screen", "add", "overlay"):
            out = LayerStack(16, 16).add_layer(base).add_layer(top, mode=mode, opacity=0.8).flatten()
            self.assertEqual(out.shape, (16, 16, 4))
            self.assertEqual(out.dtype, np.uint8)

        emis = np.zeros((16, 16, 4), dtype=np.uint8)
        emis[7:9, 7:9] = [100, 255, 240, 255]
        bloomed = apply_emissive_bloom(emis, radius=2, intensity=0.5)
        self.assertGreater(int(bloomed[6, 6, 3]), 0)  # halo spilled to neighbor

        xx, yy = make_grid(16, 16)
        d_c = sdf_circle(xx, yy, 0.0, 0.0, 0.5)
        nx, ny, nz = compute_sdf_normals(d_c)
        pom = sdf_to_pom_height(d_c, depth=0.65)
        self.assertGreaterEqual(float(pom.min()), 0.06)
        self.assertLessEqual(float(pom.max()), 1.0)
        n_map = normals_to_labpbr(nx, ny, nz, height_map=pom)
        self.assertEqual(n_map.shape, (16, 16, 4))
        self.assertGreaterEqual(int(n_map[..., 3].min()), 16)
        self.assertLess(int(n_map[..., 3].min()), int(n_map[..., 3].max()))

    def test_v3_gray_scott_curvature_floyd_and_labpbr_s(self):
        gs = gray_scott_toroidal(32, 32, steps=80, seed=11)
        self.assertEqual(gs.shape, (32, 32))
        self.assertGreaterEqual(float(gs.min()), 0.0)
        self.assertLessEqual(float(gs.max()), 1.0)

        xx, yy = make_grid(32, 32)
        curv = compute_sdf_curvature(sdf_circle(xx, yy, 0.0, 0.0, 0.5))
        self.assertGreaterEqual(float(curv.min()), -1.0)
        self.assertLessEqual(float(curv.max()), 1.0)

        fs = floyd_steinberg_quantize(gs, PALETTES["abyssal_obsidian"])
        self.assertEqual(fs.shape, (32, 32, 4))
        self.assertEqual(fs.dtype, np.uint8)

        mat = MATERIALS["PURIFICATION_CRYSTAL"]
        s_map = channels_to_labpbr_s(
            np.full((16, 16), mat.smoothness, dtype=np.float32),
            np.full((16, 16), mat.f0, dtype=np.float32),
            np.full((16, 16), mat.porosity_sss, dtype=np.float32),
            np.where(xx[:16, :16] > 0, mat.emissive, 0.0).astype(np.float32),
        )
        self.assertEqual(s_map.shape, (16, 16, 4))
        self.assertEqual(int(s_map[..., 3].max()), 255)  # non-emissive encoded as 255 per LabPBR 1.3

    def test_resource_textures_mcmeta_and_lang_parity(self):
        import json
        from PIL import Image
        res_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", "src", "main", "resources", "assets", "mycelialmind"))
        tex_root = os.path.join(res_root, "textures")
        for root, _, files in os.walk(tex_root):
            for fname in files:
                fpath = os.path.join(root, fname)
                if fname.endswith(".png"):
                    with Image.open(fpath) as im:
                        self.assertGreater(im.width, 0)
                        self.assertGreater(im.height, 0)
                        if fname.endswith("_n.png"):
                            arr = np.asarray(im.convert("RGBA"))
                            self.assertGreaterEqual(int(arr[..., 3].min()), 1, f"Invalid POM=0 in {fname}")
                        elif fname.endswith("_s.png"):
                            arr = np.asarray(im.convert("RGBA"))
                            self.assertFalse(np.any(arr[..., 1] == 255), f"Invalid F0=255 in {fname}")
                    for suf in ("_n.png", "_s.png", "_emissive.png"):
                        if fname.endswith(suf):
                            base_path = fpath[: -len(suf)] + ".png"
                            self.assertTrue(os.path.exists(base_path), f"Orphan auxiliary map {fpath}")
                elif fname.endswith(".png.mcmeta"):
                    png_path = fpath[:-7]
                    self.assertTrue(os.path.exists(png_path), f"Orphan .mcmeta {fpath}")
                    with open(fpath, encoding="utf-8") as mf:
                        meta = json.load(mf)
                    self.assertIn("animation", meta)
                    with Image.open(png_path) as im:
                        self.assertEqual(im.height % im.width, 0, f"Flipbook {png_path} height not multiple of width")
                        self.assertGreaterEqual(im.height // im.width, 2, f"Flipbook {png_path} needs >= 2 frames")

        with open(os.path.join(res_root, "lang", "en_us.json"), encoding="utf-8") as f_en:
            en = json.load(f_en)
        with open(os.path.join(res_root, "lang", "es_es.json"), encoding="utf-8") as f_es:
            es = json.load(f_es)
        self.assertEqual(set(en.keys()), set(es.keys()), "en_us.json and es_es.json key mismatch")
        for k, v in en.items():
            self.assertTrue(str(v).strip(), f"Empty en_us value for {k}")
            self.assertTrue(str(es[k]).strip(), f"Empty es_es value for {k}")

    def test_v4_scharr_frangi_poisson_wfc_and_labpbr_sss(self):
        from scripts.texture_studio.sdf_engine import (
            compute_sdf_normals,
            fast_cellular_warped,
            frangi_vesselness_toroidal,
            make_grid,
            poisson_disc_toroidal,
            sdf_circle,
            wfc_overlap_toroidal,
        )
        from scripts.texture_studio.compositor import channels_to_labpbr_s

        xx, yy = make_grid(32, 32)
        sdf = sdf_circle(xx, yy, 0.0, 0.0, 0.5)
        nx, ny, nz = compute_sdf_normals(sdf)
        self.assertEqual(nx.shape, (32, 32))
        self.assertTrue(np.all(nz > 0.0))

        # Ridged filament field -> Frangi vesselness should detect ridges in [0, 1]
        ridge = np.exp(-((xx - yy) ** 2) * 30.0).astype(np.float32)
        vessel = frangi_vesselness_toroidal(ridge)
        self.assertGreater(float(vessel.max()), 0.5)
        self.assertGreaterEqual(float(vessel.min()), 0.0)
        self.assertLessEqual(float(vessel.max()), 1.0)

        # Poisson-disc toroidal minimum distance invariant
        pts, dist_field = poisson_disc_toroidal(32, 32, min_dist=5.0, seed=7)
        self.assertGreater(len(pts), 5)
        self.assertEqual(dist_field.shape, (32, 32))
        for i in range(len(pts)):
            for j in range(i + 1, len(pts)):
                dx = min(abs(pts[i, 0] - pts[j, 0]), 32.0 - abs(pts[i, 0] - pts[j, 0]))
                dy = min(abs(pts[i, 1] - pts[j, 1]), 32.0 - abs(pts[i, 1] - pts[j, 1]))
                self.assertGreaterEqual(dx * dx + dy * dy, 5.0 * 5.0 - 1e-3)

        # WFC overlapping synthesis produces valid 16x16 output from exemplar values
        exemplar = np.array([
            [0, 1, 0, 1],
            [1, 2, 1, 0],
            [0, 1, 0, 1],
            [1, 0, 1, 2],
        ], dtype=np.int32)
        wfc_out = wfc_overlap_toroidal(exemplar, 16, 16, n=2, seed=11)
        self.assertEqual(wfc_out.shape, (16, 16))
        self.assertTrue(set(np.unique(wfc_out)).issubset({0, 1, 2}))

        # Fast cellular warped parity
        f1, edge = fast_cellular_warped(16, 16, seed=42, cells=6, warp=0.15)
        self.assertEqual(f1.shape, (16, 16))
        self.assertGreaterEqual(float(edge.min()), 0.0)
        self.assertLessEqual(float(edge.max()), 1.0)

        # LabPBR 1.3 Porosity (0..64) vs SSS (65..255) & Metal ID (230..254)
        ones = np.ones((8, 8), dtype=np.float32) * 0.5
        zeros = np.zeros((8, 8), dtype=np.float32)
        por_s = channels_to_labpbr_s(ones, ones * 0.05, ones, zeros, is_subsurface=False)
        sss_s = channels_to_labpbr_s(ones, ones * 0.05, ones, zeros, is_subsurface=True)
        gold_s = channels_to_labpbr_s(ones, np.ones((8, 8), dtype=np.float32) * 0.94, zeros, zeros, metal_id=231)
        self.assertLessEqual(int(por_s[..., 2].max()), 64)
        self.assertGreaterEqual(int(sss_s[..., 2].min()), 65)
        self.assertTrue(np.all(gold_s[..., 1] == 231))

        # Meijering, Sato, D4-symmetric WFC, and SimpleTiled WFC
        from scripts.texture_studio.sdf_engine import (
            meijering_neuriteness_toroidal,
            sato_ridge_toroidal,
            wfc_simple_tiled,
        )
        neur = meijering_neuriteness_toroidal(ridge)
        sato_r = sato_ridge_toroidal(ridge)
        self.assertEqual(neur.shape, (32, 32))
        self.assertEqual(sato_r.shape, (32, 32))
        wfc_sym = wfc_overlap_toroidal(exemplar, 16, 16, n=2, seed=19, symmetry=8)
        self.assertEqual(wfc_sym.shape, (16, 16))
        tiled = wfc_simple_tiled(
            ["A", "B"],
            {"A": ("0", "1", "1", "0"), "B": ("1", "0", "0", "1")},
            6, 6, seed=7,
        )
        self.assertEqual(len(tiled), 6)
        self.assertEqual(len(tiled[0]), 6)

    def test_value_noise_and_height_normals_invariants(self):
        vn = toroidal_value_noise(32, 32, 4, 4, seed=77)
        self.assertEqual(vn.shape, (32, 32))
        self.assertGreaterEqual(float(vn.min()), 0.0)
        self.assertLessEqual(float(vn.max()), 1.0)
        self.assertLess(float(np.mean(np.abs(vn[:, 0] - vn[:, -1]))), 0.25)

        vfbm = toroidal_value_fbm(16, 16, seed=77, base_cells=4, octaves=3)
        self.assertEqual(vfbm.shape, (16, 16))
        self.assertGreaterEqual(float(vfbm.min()), 0.0)
        self.assertLessEqual(float(vfbm.max()), 1.0)
        self.assertGreater(float(np.std(vfbm)), 0.10)

        flat = np.full((16, 16), 0.5, dtype=np.float32)
        fx, fy, fz = compute_height_normals(flat, strength=2.5)
        self.assertTrue(np.allclose(fx, 0.0, atol=1e-5))
        self.assertTrue(np.allclose(fy, 0.0, atol=1e-5))
        self.assertTrue(np.allclose(fz, 1.0, atol=1e-5))

        grain = 0.5 + 0.02 * (toroidal_value_noise(16, 16, 4, 4, seed=91) - 0.5)
        gx, gy, gz = compute_height_normals(grain, strength=2.5)
        mag = np.sqrt(gx * gx + gy * gy + gz * gz)
        self.assertTrue(np.allclose(mag, 1.0, atol=1e-4))
        self.assertGreater(float(gz.min()), 0.95)

    def test_3d_atlases_and_animated_crystal_flow(self):
        import tempfile
        from PIL import Image
        from scripts.texture_studio.generators.atlas_3d import ATLAS_SPECS, generate_3d_atlases

        with tempfile.TemporaryDirectory() as tmp:
            out = generate_3d_atlases(tmp, emit_pbr=True)
            self.assertEqual(len(out), len(ATLAS_SPECS) + 1)
            for name in ATLAS_SPECS:
                with Image.open(os.path.join(tmp, f"{name}.png")) as im_base:
                    self.assertEqual(im_base.size, (64, 64))
                with Image.open(os.path.join(tmp, f"{name}_n.png")) as im_n:
                    self.assertEqual(im_n.size, (64, 64))
                    n_arr = np.asarray(im_n.convert("RGBA"))
                    self.assertGreaterEqual(int(n_arr[..., 3].min()), 16)
                    self.assertLess(int(n_arr[..., 3].min()), int(n_arr[..., 3].max()))
                    nx = (n_arr[..., 0].astype(np.float32) / 255.0) * 2.0 - 1.0
                    self.assertLess(float(np.mean(np.abs(nx))), 0.45)
                with Image.open(os.path.join(tmp, f"{name}_s.png")) as im_s:
                    s_arr = np.asarray(im_s.convert("RGBA"))
                self.assertEqual(s_arr.shape, (64, 64, 4))
                self.assertFalse(np.any(s_arr[..., 1] == 255))
                if name == "atlas_blocks_hive":
                    self.assertLessEqual(int(s_arr[0:16, 0:16, 2].max()), 64)
                    self.assertGreaterEqual(int(s_arr[0:16, 16:32, 2].min()), 65)
                    self.assertTrue(np.all(s_arr[32:48, 32:48, 1] == 231))
            relic_atlases = [name for name in ATLAS_SPECS if name.startswith("atlas_relic_")]
            self.assertEqual(len(relic_atlases), 7)
            relic_pixels = []
            for name in relic_atlases:
                with Image.open(os.path.join(tmp, f"{name}.png")) as im:
                    relic_pixels.append(np.asarray(im.convert("RGBA")).tobytes())
            self.assertEqual(len(set(relic_pixels)), 7, "Each hand relic should have its own visible material atlas")
            with Image.open(os.path.join(tmp, "crystal_pulse_flow.png")) as im_flow:
                self.assertEqual(im_flow.size, (64, 256))
            self.assertTrue(os.path.exists(os.path.join(tmp, "crystal_pulse_flow.png.mcmeta")))

            from scripts.texture_studio.generators.items import generate_mob_effects
            eff_dir = os.path.join(tmp, "mob_effect")
            generate_mob_effects(eff_dir)
            for ename in ("mycelial_infection", "spore_rot", "synaptic_insight", "synaptic_vision"):
                ep = os.path.join(eff_dir, f"{ename}.png")
                self.assertTrue(os.path.exists(ep))
                with Image.open(ep) as eim:
                    self.assertEqual(eim.size, (18, 18))
                    self.assertGreater(eim.getextrema()[3][1], 0)


class TestContactSheetLayout(unittest.TestCase):
    def test_layout_never_overflows_512_atlases(self):
        from PIL import Image
        from scripts.texture_studio.build_textures import layout_contact_rows

        entries = [(f"big_{i}", Image.new("RGBA", (512, 512)), 0.5) for i in range(5)]
        entries += [(f"small_{i}", Image.new("RGBA", (256, 256)), 1.0) for i in range(3)]
        rows, canvas_w, canvas_h = layout_contact_rows(entries, 1120)
        self.assertEqual(canvas_w, 1120)
        for row, row_w, _row_h in rows:
            self.assertLessEqual(row_w, 1120)
            for _label, _image, width, _height in row:
                self.assertLessEqual(width, 1120)
        self.assertEqual(canvas_h, sum(height + 16 + 14 for _, _, height in rows) + 28)


if __name__ == "__main__":
    unittest.main()
