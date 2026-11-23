"""Automated unit tests for model_studio UV packing, hierarchies, and Java emission."""

import os
import sys
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from scripts.model_studio.java_emitter import emit_java_model
from scripts.model_studio.model_defs import build_all_models
from scripts.model_studio.uv_packer import pack_model_uvs


class TestModelStudio(unittest.TestCase):
    def test_stalker_preserves_rest_pose_and_springs_do_not_advance_without_time(self):
        models = build_all_models()
        stalker = next(m for m in models if m.registry_id == "mycelial_stalker")
        pack_model_uvs(stalker)
        java = emit_java_model(stalker)
        self.assertIn("this.rightLeg.xRot = -0.22F +", java)
        self.assertIn("this.leftLeg.xRot = -0.22F +", java)
        for model in models:
            pack_model_uvs(model)
            java = emit_java_model(model)
            if "springState" in java:
                self.assertIn("* 0.05F, 0.0F, 0.10F", java)
                self.assertIn("if (dt > 0.0F) sp[2] = entity.getYRot();", java)

    def test_drill_phase_and_stalker_shins_use_continuous_movement(self):
        models = {m.registry_id: m for m in build_all_models()}
        burrower = models["burrower"]
        pack_model_uvs(burrower)
        java = emit_java_model(burrower)
        self.assertIn("this.drillHead.zRot = (ageInTicks * 0.45F + limbSwing * 0.35F)", java)
        stalker = models["mycelial_stalker"]
        pack_model_uvs(stalker)
        java = emit_java_model(stalker)
        shin_lines = [line for line in java.splitlines() if "this.rShin.xRot =" in line or "this.lShin.xRot =" in line]
        self.assertEqual(2, len(shin_lines))
        for line in shin_lines:
            self.assertIn("limbSwingAmount", line)
            self.assertNotIn("attackSwing", line)
            self.assertNotIn("ageInTicks", line)

    def test_melee_arm_clearance_from_body(self):
        models = {m.registry_id: m for m in build_all_models()}
        brute_torso = next(p for p in models["chitin_brute"].parts if p.name == "torso")
        children = {p.name: p for p in brute_torso.children}
        # These limbs rotate around X, so their X extent is invariant throughout
        # the attack. Even the wider knuckle guards must clear the hip tassets.
        for side, sign in (("r", -1), ("l", 1)):
            arm = children["arm_" + side]
            forearm = arm.children[0]
            for cube in forearm.cubes:
                inner = arm.pivot[0] + forearm.pivot[0] + cube.origin[0]
                if sign < 0:
                    inner += cube.size[0]
                    self.assertLess(inner, -6.5)
                else:
                    self.assertGreater(inner, 6.5)

        warden = next(p for p in models["colony_warden"].parts if p.name == "torso")
        torso_cubes = {cube.name: cube for cube in warden.cubes}
        belt = torso_cubes["warden_belt"]
        for side in ("r", "l"):
            tasset = torso_cubes["tasset_" + side]
            self.assertNotEqual(belt.origin[2], tasset.origin[2])
            self.assertNotEqual(belt.origin[2] + belt.size[2], tasset.origin[2] + tasset.size[2])
        arms = {p.name: p for p in warden.children}
        right, left = arms["arm_r"], arms["arm_l"]
        self.assertLess(right.pivot[0] + right.cubes[0].origin[0] + right.cubes[0].size[0], -5.0)
        self.assertGreater(left.pivot[0] + left.cubes[0].origin[0], 5.0)
        arm_cubes = {cube.name: cube for cube in right.cubes}
        arm = arm_cubes["arm_r"]
        shaft = arm_cubes["halberd_shaft"]
        # Their former shared Y face produced interference when the arm swung.
        self.assertGreater(shaft.origin[1], arm.origin[1] + arm.size[1])

        stalker = next(p for p in models["mycelial_stalker"].parts if p.name == "torso")
        for side in ("r", "l"):
            arm = next(p for p in stalker.children if p.name == "arm_" + side)
            cubes = {cube.name: cube for cube in arm.cubes}
            limb, blade = cubes["arm_" + side], cubes["scythe_" + side]
            self.assertGreater(blade.origin[1], limb.origin[1] + limb.size[1])

        architect = next(p for p in models["spore_architect"].parts if p.name == "torso")
        arm = next(p for p in architect.children if p.name == "arm_r")
        cubes = {cube.name: cube for cube in arm.cubes}
        self.assertGreater(cubes["injector_claw"].origin[1],
                           cubes["arm_r"].origin[1] + cubes["arm_r"].size[1])
        for side, sign in (("r", -1), ("l", 1)):
            arm = next(p for p in architect.children if p.name == "arm_" + side)
            limb = next(c for c in arm.cubes if c.name == "arm_" + side)
            inner = arm.pivot[0] + limb.origin[0] + (limb.size[0] if sign < 0 else 0)
            self.assertLess(inner, -4.0) if sign < 0 else self.assertGreater(inner, 4.0)

    def test_combat_arm_pose_keeps_mantis_arms_outward(self):
        models = {m.registry_id: m for m in build_all_models()}
        stalker = models["mycelial_stalker"]
        pack_model_uvs(stalker)
        java = emit_java_model(stalker)
        self.assertIn("this.armR.zRot = 0.0F + aggro * 0.18F + 0.06F;", java)
        self.assertIn("this.armL.zRot = 0.0F - aggro * 0.18F - 0.06F;", java)
        warden = models["colony_warden"]
        pack_model_uvs(warden)
        self.assertIn("this.armL.yRot = 0.0F;", emit_java_model(warden))

    def test_combat_jaw_oscillation_does_not_jump_on_aggro_change(self):
        # Switching aggression must not change the phase or amplitude of an
        # age-based oscillation; the independently interpolated attack remains.
        for model in build_all_models():
            pack_model_uvs(model)
            java = emit_java_model(model)
            for line in java.splitlines():
                if ("jaw" in line.lower() or "mandible" in line.lower()) and "attackSwing *" in line:
                    self.assertNotIn("aggro", line, model.class_name)

    def test_all_17_models_pack_and_emit_cleanly(self):
        models = build_all_models()
        self.assertEqual(len(models), 18)
        for m in models:
            cubes = pack_model_uvs(m)
            self.assertGreater(len(cubes), 4, f"{m.class_name} should have rich multi-cube silhouette")
            java = emit_java_model(m)
            self.assertIn(f"public class {m.class_name}<T extends Entity> extends EntityModel<T>", java)
            self.assertIn(f'LayerDefinition.create(mesh, {m.tex_size[0]}, {m.tex_size[1]})', java)
            self.assertIn("public void setupAnim(", java)
            self.assertIn(".mirror(true)", java)
            if m.registry_id in ("hive_core", "apex_matriarch", "spore_behemoth"):
                self.assertEqual(m.tex_size, (512, 512))
                self.assertTrue(any(c.get("detail_tags") for c in cubes), f"{m.class_name} should define detail_tags")
            if m.registry_id in ("goliath_carrier", "colony_warden"):
                self.assertEqual(m.tex_size, (256, 256))
                self.assertTrue(any(c.get("detail_tags") for c in cubes), f"{m.class_name} should define detail_tags")
            if m.registry_id not in ("purifier_drone", "hive_core", "apex_matriarch", "colony_symbiont"):
                self.assertIn("getEvolutionRank()", java)
                self.assertIn(".visible =", java)
            if m.registry_id == "colony_symbiont":
                self.assertIn("getCaste()", java)
                self.assertIn(".visible =", java)
            if m.registry_id == "hive_core":
                self.assertIn("isShieldedPhaseTwo()", java)
            if m.registry_id == "apex_matriarch":
                self.assertIn("getMatriarchPhase()", java)

    def test_no_duplicate_part_names_and_manifest_sync(self):
        import json
        models = build_all_models()
        manifest_path = os.path.join(REPO_ROOT, "scripts", "model_studio", "exports", "uv_manifest.json")
        with open(manifest_path, encoding="utf-8") as f:
            manifest = json.load(f)
        self.assertEqual(set(manifest.keys()), {m.registry_id for m in models})

        def walk_parts(parts, seen_names, class_name):
            for p in parts:
                self.assertNotIn(p.name, seen_names, f"Duplicate PartNode '{p.name}' in {class_name}")
                seen_names.add(p.name)
                for c in p.cubes:
                    self.assertTrue(all(isinstance(d, int) and d >= 1 for d in c.size),
                                    f"Invalid CubeSpec size {c.size} in {class_name}.{c.name}")
                walk_parts(p.children, seen_names, class_name)

        for m in models:
            walk_parts(m.parts, set(), m.class_name)
            self.assertEqual(tuple(manifest[m.registry_id]["tex_size"]), m.tex_size)

    def test_spring_damper_exact_fabrik_and_bbmodel_exports(self):
        from scripts.model_studio.bbmodel_exporter import export_bbmodel_dict, export_bedrock_geo_dict

        models = build_all_models()
        for m in models:
            pack_model_uvs(m)
            java = emit_java_model(m)
            if "springState" in java:
                self.assertIn("stepSpringDamperExact(", java)
                self.assertIn("damperExact(", java)
            if "arachnid_tibia" in java or "solveTwoBoneIK" in java:
                self.assertIn("solveFabrikChainAngles(", java)
                self.assertIn("stepLift_", java)

            bb = export_bbmodel_dict(m)
            self.assertEqual(bb["meta"]["model_format"], "modded_entity")
            self.assertEqual(bb["resolution"]["width"], m.tex_size[0])
            self.assertGreater(len(bb["elements"]), 0)
            self.assertGreater(len(bb["outliner"]), 0)
            self.assertGreater(len(bb["textures"]), 0)
            self.assertGreater(len(bb["animations"]), 0)

            geo = export_bedrock_geo_dict(m)
            self.assertEqual(geo["format_version"], "1.12.0")
            self.assertGreater(len(geo["minecraft:geometry"][0]["bones"]), 0)

    def test_3d_block_and_item_models_valid(self):
        from scripts.model_studio.block_item_3d import build_3d_block_models, build_3d_item_models

        blocks = build_3d_block_models()
        items = build_3d_item_models()
        self.assertEqual(len(blocks), 24)  # Includes chitin_pillar_horizontal and reclaimed_bloom.
        self.assertEqual(len(items), 35)   # 11 masterwork + 4 chitin armor + 17 tactical/biotech/relics + 3 sculpted materials
        atlas_root = os.path.join(REPO_ROOT, "src", "main", "resources", "assets", "mycelialmind", "textures")
        for name, m in list(blocks.items()) + list(items.items()):
            if "elements" not in m:
                self.assertIn("parent", m, f"{name} missing both elements and parent")
                continue
            self.assertGreaterEqual(len(m["elements"]), 4, f"{name} should be multi-element 3D")
            if name in items:
                self.assertGreaterEqual(len(m["elements"]), 6, f"{name} should have rich sculpted 3D item geometry")
            self.assertIn("textures", m, f"{name} missing textures")
            for key, tex_ref in m["textures"].items():
                if tex_ref.startswith("mycelialmind:"):
                    rel = tex_ref.split(":", 1)[1] + ".png"
                    self.assertTrue(os.path.exists(os.path.join(atlas_root, rel)), f"{name} missing texture {tex_ref}")
            for el in m["elements"]:
                for c in el["from"] + el["to"]:
                    self.assertTrue(-16.0 <= c <= 32.0, f"{name} coord out of range: {c}")
                dx = abs(el["to"][0] - el["from"][0])
                dy = abs(el["to"][1] - el["from"][1])
                dz = abs(el["to"][2] - el["from"][2])
                dims_by_face = {
                    "north": (dx, dy),
                    "south": (dx, dy),
                    "east": (dz, dy),
                    "west": (dz, dy),
                    "up": (dx, dz),
                    "down": (dx, dz),
                }
                for face_name, face_data in el["faces"].items():
                    u0, v0, u1, v1 = face_data["uv"]
                    for u in (u0, v0, u1, v1):
                        self.assertTrue(0.0 <= u <= 16.0, f"{name} UV out of range: {u}")
                    if face_data["texture"] == "#atlas":
                        col = int(u0 // 4.0)
                        row = int(v0 // 4.0)
                        self.assertGreaterEqual(u0, col * 4.0 + 0.149, f"{name}.{face_name} UV bleeds left of tile")
                        self.assertLessEqual(u1, col * 4.0 + 3.851, f"{name}.{face_name} UV bleeds right of tile")
                        self.assertGreaterEqual(v0, row * 4.0 + 0.149, f"{name}.{face_name} UV bleeds top of tile")
                        self.assertLessEqual(v1, row * 4.0 + 3.851, f"{name}.{face_name} UV bleeds bottom of tile")
                        fw, fh = dims_by_face[face_name]
                        if 1.0 <= fw <= 14.8 and 1.0 <= fh <= 14.8:
                            uv_aspect = (u1 - u0) / (v1 - v0)
                            face_aspect = fw / fh
                            self.assertAlmostEqual(
                                uv_aspect,
                                face_aspect,
                                delta=face_aspect * 0.05,
                                msg=f"{name}.{face_name} distorted UV aspect {uv_aspect:.3f} vs face {face_aspect:.3f}",
                            )
        for name, m in items.items():
            self.assertIn("display", m, f"3D item {name} missing display transforms")

    def test_seven_hand_relics_use_individual_atlases(self):
        from scripts.model_studio.block_item_3d import build_3d_item_models
        from scripts.texture_studio.generators.atlas_3d import ATLAS_SPECS

        relic_atlases = {
            "synaptic_suppressor": "atlas_relic_synaptic_suppressor",
            "tide_beacon": "atlas_relic_tide_beacon",
            "overmind_crown": "atlas_relic_overmind_crown",
            "quorum_seal": "atlas_relic_quorum_seal",
            "rift_catalyst": "atlas_relic_rift_catalyst",
            "hive_heart": "atlas_relic_hive_heart",
            "overmind_essence": "atlas_relic_overmind_essence",
        }
        items = build_3d_item_models()
        self.assertEqual(len(set(relic_atlases.values())), len(relic_atlases))
        for item_id, atlas_name in relic_atlases.items():
            with self.subTest(item=item_id):
                self.assertIn(atlas_name, ATLAS_SPECS)
                self.assertEqual(items[item_id]["textures"]["atlas"], f"mycelialmind:model_atlas/{atlas_name}")
                self.assertGreaterEqual(len(items[item_id]["elements"]), 8)
                self.assertIn("firstperson_righthand", items[item_id]["display"])
                self.assertIn("thirdperson_righthand", items[item_id]["display"])

    def test_3d_items_no_interpenetration_and_connected(self):
        import math
        from collections import deque
        from scripts.model_studio.block_item_3d import build_3d_block_models, build_3d_item_models

        def element_obb(el):
            fx, fy, fz = el["from"]
            tx, ty, tz = el["to"]
            cx, cy, cz = (fx + tx) * 0.5, (fy + ty) * 0.5, (fz + tz) * 0.5
            hx, hy, hz = (tx - fx) * 0.5, (ty - fy) * 0.5, (tz - fz) * 0.5
            axes = [(1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0)]
            rot = el.get("rotation")
            if rot:
                ox, oy, oz = rot["origin"]
                ang = math.radians(rot["angle"])
                c, s = math.cos(ang), math.sin(ang)
                ax = rot["axis"]

                def rot_vec(vx, vy, vz):
                    if ax == "x":
                        return (vx, vy * c - vz * s, vy * s + vz * c)
                    if ax == "y":
                        return (vx * c + vz * s, vy, -vx * s + vz * c)
                    return (vx * c - vy * s, vx * s + vy * c, vz)

                dx, dy, dz = rot_vec(cx - ox, cy - oy, cz - oz)
                cx, cy, cz = ox + dx, oy + dy, oz + dz
                axes = [rot_vec(*a) for a in axes]
            return (cx, cy, cz), (hx, hy, hz), axes

        def obb_overlap(obb_a, obb_b, margin):
            ca, ha, aa = obb_a
            cb, hb, ab = obb_b
            ha = (max(0.0, ha[0] + margin), max(0.0, ha[1] + margin), max(0.0, ha[2] + margin))
            hb = (max(0.0, hb[0] + margin), max(0.0, hb[1] + margin), max(0.0, hb[2] + margin))
            t = (cb[0] - ca[0], cb[1] - ca[1], cb[2] - ca[2])

            def dot(u, v):
                return u[0] * v[0] + u[1] * v[1] + u[2] * v[2]

            def cross(u, v):
                return (u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0])

            test_axes = list(aa) + list(ab)
            for u in aa:
                for v in ab:
                    cr = cross(u, v)
                    if dot(cr, cr) > 1e-8:
                        test_axes.append(cr)

            for axis in test_axes:
                norm = math.sqrt(dot(axis, axis))
                if norm < 1e-8:
                    continue
                ax = (axis[0] / norm, axis[1] / norm, axis[2] / norm)
                ra = sum(ha[i] * abs(dot(aa[i], ax)) for i in range(3))
                rb = sum(hb[i] * abs(dot(ab[i], ax)) for i in range(3))
                if abs(dot(t, ax)) >= ra + rb:
                    return False
            return True

        items = build_3d_item_models()
        blocks = build_3d_block_models()
        fingerprints = {}
        for name, m in list(items.items()) + list(blocks.items()):
            if "elements" not in m:
                continue
            fp = tuple(sorted(
                (tuple(el["from"]), tuple(el["to"]), el.get("rotation", {}).get("angle", 0.0))
                for el in m["elements"]
            ))
            self.assertNotIn(
                fp,
                fingerprints,
                f"3D model '{name}' shares identical geometry with '{fingerprints.get(fp)}' (must be 100% unique!)",
            )
            fingerprints[fp] = name
            obbs = [element_obb(el) for el in m["elements"]]
            n = len(obbs)
            adj = {i: set() for i in range(n)}
            for i in range(n):
                for j in range(i + 1, n):
                    self.assertFalse(
                        obb_overlap(obbs[i], obbs[j], -0.02),
                        f"3D model '{name}' elements {i} and {j} interpenetrate!",
                    )
                    if obb_overlap(obbs[i], obbs[j], 0.15):
                        adj[i].add(j)
                        adj[j].add(i)
            seen = {0}
            q = deque([0])
            while q:
                cur = q.popleft()
                for nxt in adj[cur]:
                    if nxt not in seen:
                        seen.add(nxt)
                        q.append(nxt)
            self.assertEqual(len(seen), n, f"3D model '{name}' has disconnected floating parts! Connected {sorted(seen)} of {n}")

    def test_mob_skeleton_continuity_and_proportions(self):
        from scripts.model_studio.block_item_3d import build_3d_block_models, build_3d_item_models

        ORBITAL_ROLES = {"orbit_ring_a", "orbit_ring_b", "orbit_orb"}
        ORBITAL_NAMES = {
            "rib_n", "rib_s", "rib_e", "rib_w", "rib_ne", "rib_nw", "rib_se", "rib_sw",
            "elite_crown", "elite_echo_halo", "elite_halo", "elite_satellite",
            "mutated_ring", "phase2_aegis_halo", "phase2_aura", "phase3_spire",
        }

        def cubes_world_aabb(cubes, ox, oy, oz):
            xs, ys, zs = [], [], []
            for c in cubes:
                cx, cy, cz = c.origin
                sx, sy, sz = c.size
                xs.extend([ox + cx, ox + cx + sx])
                ys.extend([oy + cy, oy + cy + sy])
                zs.extend([oz + cz, oz + cz + sz])
            return (min(xs), min(ys), min(zs), max(xs), max(ys), max(zs))

        def aabb_gap(a, b):
            dx = max(0.0, max(a[0] - b[3], b[0] - a[3]))
            dy = max(0.0, max(a[1] - b[4], b[1] - a[4]))
            dz = max(0.0, max(a[2] - b[5], b[2] - a[5]))
            return (dx * dx + dy * dy + dz * dz) ** 0.5

        for m in build_all_models():
            def check_tree(nodes, parent_aabb, px, py, pz):
                for p in nodes:
                    wx, wy, wz = px + p.pivot[0], py + p.pivot[1], pz + p.pivot[2]
                    my_aabb = cubes_world_aabb(p.cubes, wx, wy, wz) if p.cubes else (wx, wy, wz, wx, wy, wz)
                    is_orbital = (p.anim_role in ORBITAL_ROLES) or (p.name in ORBITAL_NAMES)
                    if parent_aabb is not None and not is_orbital:
                        gap = aabb_gap(my_aabb, parent_aabb)
                        self.assertLessEqual(
                            gap,
                            1.5,
                            f"{m.class_name}.{p.name} detached from parent (gap={gap:.2f})",
                        )
                    check_tree(p.children, my_aabb, wx, wy, wz)

            check_tree(m.parts, None, 0.0, 0.0, 0.0)

        items = build_3d_item_models()
        for weapon in ("symbiotic_blade", "enzyme_lance"):
            ys = [c for el in items[weapon]["elements"] for c in (el["from"][1], el["to"][1])]
            self.assertGreaterEqual(max(ys) - min(ys), 15.5, f"{weapon} should span full weapon length")

        # Slimness proportion assertions for 70/20/10 weapon ergonomics
        blade_els = items["symbiotic_blade"]["elements"]
        blade_grip = min(
            (el for el in blade_els if el["from"][1] < 3.0 and el["to"][1] <= 5.0),
            key=lambda e: e["to"][0] - e["from"][0],
        )
        self.assertLessEqual(blade_grip["to"][0] - blade_grip["from"][0], 1.5)
        blade_fuller = min(
            (el for el in blade_els if el["from"][1] >= 5.0 and el["to"][1] >= 12.0),
            key=lambda e: e["to"][0] - e["from"][0],
        )
        self.assertLessEqual(blade_fuller["to"][0] - blade_fuller["from"][0], 1.2)

        lance_els = items["enzyme_lance"]["elements"]
        lance_shaft = min(
            (el for el in lance_els if (el["to"][1] - el["from"][1]) >= 2.5 and el["to"][1] <= 8.6),
            key=lambda e: e["to"][0] - e["from"][0],
        )
        self.assertLessEqual(lance_shaft["to"][0] - lance_shaft["from"][0], 1.2)


if __name__ == "__main__":
    unittest.main()
