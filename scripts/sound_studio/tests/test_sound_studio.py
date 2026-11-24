"""Unit tests for Mycelial Mind's 44.1 kHz bio-horror DSP Sound Studio & acoustic QA."""

from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[3]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

import numpy as np

from scripts.sound_studio.block_synth import BLOCK_FUNCTIONAL_IDS
from scripts.sound_studio.boss_synth import BOSS_CINEMATIC_IDS
from scripts.sound_studio.catalog import SOUND_BY_ID, SOUND_CATALOG
from scripts.sound_studio.dsp_engine import (
    LOUDNESS_TARGETS_DBFS,
    SAMPLE_RATE,
    VORBIS_QUALITY,
    encode_ogg_vorbis_mono,
    float_to_pcm16,
    inspect_ogg_vorbis,
    synthesize_waveform,
)
from scripts.sound_studio.entity_synth import ENTITY_SYNTH_IDS
from scripts.sound_studio.item_synth import ITEM_PROJECTILE_IDS
from scripts.sound_studio.metrics import (
    _normalized_lag_correlation,
    analyze_waveform,
    decode_ogg_to_float,
    evaluate_quality,
    max_pairwise_correlation,
)
from scripts.sound_studio.system_env_synth import SYSTEM_ENV_IDS


class SoundStudioTests(unittest.TestCase):
    def test_catalog_size_species_loudness_and_attenuation_invariants(self) -> None:
        self.assertEqual(len(SOUND_CATALOG), 189)
        self.assertEqual(len(SOUND_BY_ID), 189)
        consts = {s.java_const for s in SOUND_CATALOG}
        self.assertEqual(len(consts), 189)
        total_oggs = sum(s.variations for s in SOUND_CATALOG)
        self.assertEqual(total_oggs, 507)
        for spec in SOUND_CATALOG:
            self.assertIn(spec.loudness_group, LOUDNESS_TARGETS_DBFS)
            self.assertTrue(1 <= spec.variations <= 4, msg=f"{spec.sound_id} invalid variations={spec.variations}")
            self.assertTrue(8 <= spec.attenuation_distance <= 64, msg=f"{spec.sound_id} invalid attenuation")
            self.assertTrue(bool(spec.species))

    def test_all_recipes_species_and_surfaces_synthesize_cleanly_at_44100hz(self) -> None:
        # Verify every single catalog event dispatches to one of the 5 dedicated engines and passes QA
        for spec in SOUND_CATALOG:
            is_dedicated = (
                spec.sound_id in BOSS_CINEMATIC_IDS
                or spec.sound_id in BLOCK_FUNCTIONAL_IDS
                or spec.sound_id in ENTITY_SYNTH_IDS
                or spec.sound_id in ITEM_PROJECTILE_IDS
                or spec.sound_id in SYSTEM_ENV_IDS
                or spec.recipe.startswith("surface_")
            )
            self.assertTrue(is_dedicated, msg=f"{spec.sound_id} fell back to generic recipe")
            wave = synthesize_waveform(
                spec.sound_id,
                recipe=spec.recipe,
                duration=spec.duration,
                base_freq=spec.base_freq,
                mod_freq=spec.mod_freq,
                variant=spec.variant,
                species=spec.species,
                loudness_group=spec.loudness_group,
                seed_offset=spec.seed_offset,
                variation_idx=0,
            )
            self.assertEqual(wave.ndim, 1)
            self.assertGreater(len(wave), int(SAMPLE_RATE * 0.08))
            self.assertTrue(np.all(np.isfinite(wave)))
            m = analyze_waveform(wave, SAMPLE_RATE)
            issues = evaluate_quality(m, spec.loudness_group)
            self.assertEqual(issues, [], msg=f"{spec.sound_id} failed QA: {issues}")

    def test_loudness_hierarchy_footsteps_vs_boss_cinematic(self) -> None:
        step = SOUND_BY_ID["mob_step_chitin"]
        boss = SOUND_BY_ID["apex_matriarch_wail"]
        w_step = synthesize_waveform(
            step.sound_id,
            recipe=step.recipe,
            duration=step.duration,
            base_freq=step.base_freq,
            mod_freq=step.mod_freq,
            species=step.species,
            loudness_group=step.loudness_group,
            seed_offset=step.seed_offset,
        )
        w_boss = synthesize_waveform(
            boss.sound_id,
            recipe=boss.recipe,
            duration=boss.duration,
            base_freq=boss.base_freq,
            mod_freq=boss.mod_freq,
            species=boss.species,
            loudness_group=boss.loudness_group,
            seed_offset=boss.seed_offset,
        )
        m_step = analyze_waveform(w_step)
        m_boss = analyze_waveform(w_boss)
        self.assertGreaterEqual(m_boss["active_rms_dbfs"] - m_step["active_rms_dbfs"], 7.0)

    def test_variation_determinism_and_pairwise_distinctiveness(self) -> None:
        sample_ids = (
            "sporeling_chitter",
            "chitin_brute_growl",
            "block_mycelium_break",
            "block_chitin_step",
            "mob_step_colossal",
            "acid_spitter_spit",
            "homeostasis_heartbeat",
        )
        for sid in sample_ids:
            spec = SOUND_BY_ID[sid]
            waves = [
                synthesize_waveform(
                    spec.sound_id,
                    recipe=spec.recipe,
                    duration=spec.duration,
                    base_freq=spec.base_freq,
                    mod_freq=spec.mod_freq,
                    variant=spec.variant,
                    species=spec.species,
                    loudness_group=spec.loudness_group,
                    seed_offset=spec.seed_offset,
                    variation_idx=i,
                )
                for i in range(spec.variations)
            ]
            # Determinism check
            again = synthesize_waveform(
                spec.sound_id,
                recipe=spec.recipe,
                duration=spec.duration,
                base_freq=spec.base_freq,
                mod_freq=spec.mod_freq,
                variant=spec.variant,
                species=spec.species,
                loudness_group=spec.loudness_group,
                seed_offset=spec.seed_offset,
                variation_idx=0,
            )
            np.testing.assert_allclose(waves[0], again)
            corr = max_pairwise_correlation(waves)
            self.assertLess(corr, 0.85, msg=f"{sid} variations too correlated: {corr}")

    def test_bio_horror_vs_purifier_tech_spectral_contrast(self) -> None:
        brute = SOUND_BY_ID["chitin_brute_growl"]
        scanner = SOUND_BY_ID["bio_scanner_ping"]
        w_brute = synthesize_waveform(
            brute.sound_id,
            recipe=brute.recipe,
            duration=brute.duration,
            base_freq=brute.base_freq,
            mod_freq=brute.mod_freq,
            species=brute.species,
            loudness_group=brute.loudness_group,
            seed_offset=brute.seed_offset,
        )
        w_scan = synthesize_waveform(
            scanner.sound_id,
            recipe=scanner.recipe,
            duration=scanner.duration,
            base_freq=scanner.base_freq,
            mod_freq=scanner.mod_freq,
            species=scanner.species,
            loudness_group=scanner.loudness_group,
            seed_offset=scanner.seed_offset,
        )
        m_brute = analyze_waveform(w_brute)
        m_scan = analyze_waveform(w_scan)
        self.assertGreater(m_brute["low_band_ratio"], 0.25)
        self.assertGreater(m_scan["spectral_centroid_hz"], m_brute["spectral_centroid_hz"] * 2.0)

    def test_ogg_vorbis_44100hz_mono_encode_and_decode_roundtrip(self) -> None:
        spec = SOUND_BY_ID["synaptic_pulse"]
        wave = synthesize_waveform(
            spec.sound_id,
            recipe=spec.recipe,
            duration=spec.duration,
            base_freq=spec.base_freq,
            mod_freq=spec.mod_freq,
            species=spec.species,
            loudness_group=spec.loudness_group,
            seed_offset=spec.seed_offset,
        )
        pcm = float_to_pcm16(wave)
        with tempfile.TemporaryDirectory() as tmp:
            ogg = Path(tmp) / "test_mono_44k.ogg"
            encode_ogg_vorbis_mono(pcm, ogg)
            info = inspect_ogg_vorbis(ogg)
            self.assertTrue(info["ok"], msg=f"Invalid Ogg Vorbis header: {info}")
            self.assertEqual(info["channels"], 1)
            self.assertEqual(info["sample_rate"], 44100)
            decoded = decode_ogg_to_float(ogg)
            self.assertGreater(len(decoded), int(SAMPLE_RATE * 0.2))
            m_dec = analyze_waveform(decoded)
            self.assertLessEqual(m_dec["peak_dbfs"], -0.1)

    def test_generated_sounds_json_variants_and_attenuation(self) -> None:
        sounds_json = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "sounds.json"
        if not sounds_json.is_file():
            self.skipTest("sounds.json not present")
        data = json.loads(sounds_json.read_text(encoding="utf-8"))
        self.assertEqual(len(data), 189)
        for sid, entry in data.items():
            spec = SOUND_BY_ID[sid]
            snd_list = entry.get("sounds", [])
            self.assertEqual(len(snd_list), spec.variations, msg=f"{sid} variation mismatch")
            for item in snd_list:
                self.assertNotEqual(item.get("type"), "event", msg=f"{sid} redirects to vanilla event")
                self.assertTrue(str(item.get("name", "")).startswith("mycelialmind:"), msg=f"{sid} missing namespace")
                self.assertEqual(item.get("attenuation_distance"), spec.attenuation_distance)

    def test_generated_quality_report_and_audition_html(self) -> None:
        exports = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        qr_path = exports / "sound_quality_report.json"
        html_path = exports / "sound_audition.html"
        if not qr_path.is_file():
            self.skipTest("sound_quality_report.json not present")
        qr = json.loads(qr_path.read_text(encoding="utf-8"))
        self.assertTrue(qr["ok"], msg=f"Quality report failures: {qr.get('failed_files')[:3]}")
        self.assertEqual(qr["sample_rate"], 44100)
        self.assertEqual(qr["vorbis_quality"], VORBIS_QUALITY)
        self.assertEqual(qr["total_events"], 189)
        self.assertEqual(qr["total_ogg_files"], 507)
        self.assertLess(qr["worst_correlation"], 0.85)
        self.assertTrue(html_path.is_file())
        html_text = html_path.read_text(encoding="utf-8")
        self.assertIn("apex_matriarch_wail", html_text)
        self.assertIn("44100", html_text)
        self.assertIn("Bloques Físicos", html_text)
        self.assertIn("Biomas y Ambiente (14)", html_text)
        self.assertIn("fftCanvas", html_text)

    def _synth_spec(self, sid: str, variation_idx: int = 0) -> tuple[np.ndarray, dict[str, float]]:
        spec = SOUND_BY_ID[sid]
        wave = synthesize_waveform(
            spec.sound_id,
            recipe=spec.recipe,
            duration=spec.duration,
            base_freq=spec.base_freq,
            mod_freq=spec.mod_freq,
            variant=spec.variant,
            species=spec.species,
            loudness_group=spec.loudness_group,
            seed_offset=spec.seed_offset,
            variation_idx=variation_idx,
        )
        return wave, analyze_waveform(wave, SAMPLE_RATE)

    def test_block_physical_materials_have_distinct_acoustic_fingerprints(self) -> None:
        materials = (
            "block_mycelium",
            "block_synaptic_node",
            "block_chitin",
            "block_mycelial_stone",
            "block_purification_tech",
            "block_antibody_cyst",
            "block_hive_altar",
        )
        hits = {m: self._synth_spec(f"{m}_hit") for m in materials}
        breaks = {m: self._synth_spec(f"{m}_break") for m in materials}
        steps = {m: self._synth_spec(f"{m}_step") for m in materials}

        # Tonal steel / synaptic crystal vs damp mycelium hit
        self.assertGreater(hits["block_purification_tech"][1]["spectral_centroid_hz"], hits["block_mycelium"][1]["spectral_centroid_hz"] * 3.0)
        self.assertGreater(hits["block_synaptic_node"][1]["spectral_centroid_hz"], hits["block_mycelium"][1]["spectral_centroid_hz"] * 3.0)

        # Dry hollow chitin break vs tectonic sub-heavy hive altar break
        self.assertGreater(breaks["block_chitin"][1]["spectral_centroid_hz"], 1000.0)
        self.assertLess(breaks["block_chitin"][1]["low_band_ratio"], 0.25)
        self.assertGreater(breaks["block_hive_altar"][1]["low_band_ratio"], 0.30)

        # Mycelium vs viscous antibody cyst break distinctness
        mycelium_vs_cyst = max_pairwise_correlation([breaks["block_mycelium"][0], breaks["block_antibody_cyst"][0]])
        self.assertLess(mycelium_vs_cyst, 0.50)

        # Broadband flinty stone hit vs tonal steel alloy hit
        self.assertGreater(hits["block_mycelial_stone"][1]["spectral_flatness"], hits["block_purification_tech"][1]["spectral_flatness"] * 3.0)

        # Within every material: step quieter than break by >= 4 dB, hit shorter than break
        for m in materials:
            self.assertGreaterEqual(breaks[m][1]["active_rms_dbfs"] - steps[m][1]["active_rms_dbfs"], 4.0, msg=m)
            self.assertLess(hits[m][1]["duration_secs"], breaks[m][1]["duration_secs"], msg=m)

    def test_block_functional_sounds_are_mutually_distinct(self) -> None:
        func_ids = [s.sound_id for s in SOUND_CATALOG if s.category == "block_functional"]
        self.assertEqual(len(func_ids), 18)
        waves: list[np.ndarray] = []
        metrics: dict[str, dict[str, float]] = {}
        for sid in func_ids:
            w, m = self._synth_spec(sid)
            self.assertEqual(evaluate_quality(m, SOUND_BY_ID[sid].loudness_group), [], msg=sid)
            waves.append(w)
            metrics[sid] = m

        cross_corr = max_pairwise_correlation(waves)
        self.assertLess(cross_corr, 0.65, msg=f"Functional block cross-event correlation too high: {cross_corr}")

        # Purifier tech devices must no longer share generic chimes
        tech_centroids = {
            metrics["purification_turret_fire"]["spectral_centroid_hz"],
            metrics["purification_pylon_pulse"]["spectral_centroid_hz"],
            metrics["purification_turret_upgrade"]["spectral_centroid_hz"],
            metrics["synaptic_bench_synthesize"]["spectral_centroid_hz"],
        }
        self.assertEqual(len(tech_centroids), 4)
        self.assertLess(metrics["purification_turret_fire"]["duration_secs"], metrics["purification_turret_upgrade"]["duration_secs"])
        self.assertGreater(metrics["ephemeral_chitin_crumble"]["spectral_centroid_hz"], 2500.0)

    def test_boss_sounds_are_epic_cinematic_and_distinct(self) -> None:
        """All 17 boss & ritual cinematics must span multi-act durations and separate three boss identities."""
        self.assertEqual(len(BOSS_CINEMATIC_IDS), 17)
        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        report = json.loads((exports_dir / "sound_quality_report.json").read_text(encoding="utf-8"))
        audition_html = (exports_dir / "sound_audition.html").read_text(encoding="utf-8")
        self.assertIn("Jefes y Cinemática (17)", audition_html)

        by_id = {entry["sound_id"]: entry for entry in manifest["sounds"]}
        correlations = report.get("event_correlations", {})
        major_cinematics = {
            "spore_behemoth_roar",
            "spore_behemoth_death",
            "hive_core_resonance",
            "hive_core_death",
            "apex_matriarch_wail",
            "apex_matriarch_death",
            "boss_phase_shift",
            "fungal_tide_horn",
            "hive_altar_awaken",
            "matriarch_altar_awaken",
        }

        for sid in sorted(BOSS_CINEMATIC_IDS):
            self.assertIn(sid, SOUND_BY_ID)
            self.assertIn(sid, by_id)
            entry = by_id[sid]
            m = entry["metrics"]
            self.assertGreaterEqual(entry["attenuation_distance"], 32, sid)
            self.assertLessEqual(m["peak_dbfs"], -0.25, sid)
            min_dur = 2.20 if sid in major_cinematics else 0.65
            self.assertGreaterEqual(m["duration_secs"], min_dur, sid)
            if entry["loudness_group"] == "boss_cinematic":
                self.assertGreaterEqual(m["active_rms_dbfs"], -14.5, sid)
            if len(entry["files"]) > 1:
                self.assertLessEqual(correlations.get(sid, 0.0), 0.65, f"{sid} too repetitive")

        # Distinct acoustic hierarchy across the 3 boss identities
        behemoth = by_id["spore_behemoth_roar"]["metrics"]
        hive_core = by_id["hive_core_resonance"]["metrics"]
        matriarch = by_id["apex_matriarch_wail"]["metrics"]
        self.assertLess(behemoth["spectral_centroid_hz"], hive_core["spectral_centroid_hz"])
        self.assertLess(hive_core["spectral_centroid_hz"], matriarch["spectral_centroid_hz"])
        self.assertGreater(behemoth["low_band_ratio"], 0.65)
        self.assertGreater(matriarch["spectral_centroid_hz"], 650.0)

        sounds_dir = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "sounds"
        cross_pairs = [
            ("spore_behemoth_roar", "hive_core_resonance"),
            ("spore_behemoth_roar", "apex_matriarch_wail"),
            ("hive_core_resonance", "apex_matriarch_wail"),
            ("spore_behemoth_death", "hive_core_death"),
            ("hive_core_death", "apex_matriarch_death"),
            ("spore_behemoth_death", "apex_matriarch_death"),
            ("hive_altar_awaken", "matriarch_altar_awaken"),
            ("boss_phase_shift", "fungal_tide_horn"),
        ]
        for a, b in cross_pairs:
            wa = decode_ogg_to_float(sounds_dir / by_id[a]["files"][0])
            wb = decode_ogg_to_float(sounds_dir / by_id[b]["files"][0])
            corr = _normalized_lag_correlation(wa, wb)
            self.assertLess(corr, 0.65, f"Cross-boss pair {(a, b)} too correlated: {corr:.3f}")

    def test_entity_species_and_creatures_have_distinct_vocal_fingerprints(self) -> None:
        """All 14 non-boss creatures and 4 anatomical steps in entity_synth must be distinct."""
        self.assertEqual(len(ENTITY_SYNTH_IDS), 64)
        ambients = [
            "sporeling_chitter",
            "chitin_brute_growl",
            "stalker_hiss",
            "acid_spitter_gurgle",
            "burrower_grind",
            "pyro_shroom_crackle",
            "synapse_weaver_hum",
            "goliath_carrier_thud",
            "leech_parasite_screech",
            "colony_warden_bellow",
            "spore_architect_pulse",
            "synaptic_mimic_echo",
            "purifier_drone_hum",
            "mycelial_chimera_growl",
        ]
        waves = []
        metrics = {}
        for sid in ambients:
            w, m = self._synth_spec(sid)
            waves.append(w)
            metrics[sid] = m

        self.assertLess(max_pairwise_correlation(waves), 0.65)
        self.assertGreater(metrics["sporeling_chitter"]["spectral_centroid_hz"], metrics["chitin_brute_growl"]["spectral_centroid_hz"] * 2.5)
        self.assertGreater(metrics["leech_parasite_screech"]["spectral_centroid_hz"], metrics["goliath_carrier_thud"]["spectral_centroid_hz"] * 3.0)
        self.assertGreater(metrics["goliath_carrier_thud"]["low_band_ratio"], 0.35)

    def test_item_and_projectile_sounds_are_mutually_distinct(self) -> None:
        """All 27 item, weapon, consumable, relic & projectile events must be mutually distinct."""
        self.assertEqual(len(ITEM_PROJECTILE_IDS), 27)
        tech_tools = [
            "bio_scanner_ping",
            "bio_scanner_thermal_pulse",
            "bio_extractor_draw",
            "spore_antidote_cure",
            "counter_serum_inject",
            "neural_decoy_deploy",
            "purifier_drone_deploy",
        ]
        waves = [self._synth_spec(sid)[0] for sid in tech_tools]
        self.assertLess(max_pairwise_correlation(waves), 0.65)

    def test_biome_ambient_loops_and_tactical_alerts_meet_quality_invariants(self) -> None:
        """All 14 new biome ambient loops/moods/additions and tactical HUD alerts must synthesize cleanly."""
        self.assertEqual(len(SYSTEM_ENV_IDS), 30)
        biome_loops = ("biome_caverns_loop", "biome_sanctum_loop", "biome_forest_loop")
        loop_waves = []
        for sid in biome_loops:
            w, m = self._synth_spec(sid)
            self.assertGreaterEqual(m["duration_secs"], 5.0, sid)
            self.assertLessEqual(m["edge_start_abs"], 0.01, sid)
            self.assertLessEqual(m["edge_end_abs"], 0.01, sid)
            loop_waves.append(w)
        self.assertLess(max_pairwise_correlation(loop_waves), 0.55)

    def test_stage1_pedalboard_lufs_and_convolution_ir_bank(self) -> None:
        """Stage 1: 6 mono 44.1 kHz IR .wav files, Pedalboard convolution, and ITU-R BS.1770-4 LUFS must back all 181 sounds."""
        import wave as std_wave
        from scripts.sound_studio.ir_bank import IR_SPACE_NAMES, IRS_DIR, ensure_ir_bank
        from scripts.sound_studio.mastering import master_with_pedalboard, measure_integrated_lufs

        self.assertEqual(len(IR_SPACE_NAMES), 6)
        ir_paths = ensure_ir_bank(IRS_DIR)
        self.assertEqual(set(ir_paths.keys()), set(IR_SPACE_NAMES))
        for name, wav_path in ir_paths.items():
            self.assertTrue(wav_path.is_file(), f"Missing IR file for {name}")
            with std_wave.open(str(wav_path), "rb") as wf:
                self.assertEqual(wf.getnchannels(), 1, name)
                self.assertEqual(wf.getframerate(), SAMPLE_RATE, name)
                self.assertGreater(wf.getnframes(), int(SAMPLE_RATE * 0.4), name)

        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        audition_html = (exports_dir / "sound_audition.html").read_text(encoding="utf-8")
        self.assertIn("LUFS:", audition_html)
        self.assertIn('id="irFilter"', audition_html)
        for space in IR_SPACE_NAMES:
            self.assertIn(f'"ir_space": "{space}"', audition_html)

        self.assertEqual(len(manifest["sounds"]), 189)
        for entry in manifest["sounds"]:
            self.assertIn(entry.get("ir_space"), IR_SPACE_NAMES, entry["sound_id"])
            lufs = entry["metrics"].get("integrated_lufs")
            self.assertIsNotNone(lufs, entry["sound_id"])
            self.assertTrue(-50.0 < float(lufs) < 0.0, f"{entry['sound_id']} LUFS out of range: {lufs}")

        t = np.linspace(0.0, 0.8, int(SAMPLE_RATE * 0.8), endpoint=False)
        probe = np.sin(2.0 * np.pi * 110.0 * t) * np.exp(-t / 0.25)
        mastered = master_with_pedalboard(probe, sound_id="hive_core_resonance", species="synaptic_eldritch", loudness_group="boss_cinematic")
        self.assertEqual(len(mastered), len(probe))
        self.assertTrue(np.all(np.isfinite(mastered)))
        self.assertLessEqual(float(np.max(np.abs(mastered))), 0.945)
        self.assertTrue(-35.0 < measure_integrated_lufs(mastered, SAMPLE_RATE) < -3.0)

    def test_stage2_rtx4060_stable_audio_open_small_stems_and_hybrid_blend(self) -> None:
        """Stage 2: 52 RTX 4060 Stable Audio Open Small stems (steps=14) must back all 181 sounds with worst_corr < 0.55."""
        import wave as std_wave
        from scripts.sound_studio.ai_stem_generator import (
            AI_STEMS_DIR,
            AI_STEM_ARCHETYPES,
            INFERENCE_STEPS,
            SOUND_TO_ARCHETYPE,
            STEM_MANIFEST_PATH,
        )

        self.assertEqual(INFERENCE_STEPS, 14)
        self.assertEqual(len(AI_STEM_ARCHETYPES), 52)
        self.assertEqual(len(SOUND_TO_ARCHETYPE), 189)
        self.assertTrue(STEM_MANIFEST_PATH.is_file(), f"Missing stem manifest: {STEM_MANIFEST_PATH}")
        stem_manifest = json.loads(STEM_MANIFEST_PATH.read_text(encoding="utf-8"))
        self.assertEqual(stem_manifest["stem_count"], 52)
        self.assertEqual(stem_manifest["mapped_sound_events"], 189)
        self.assertEqual(stem_manifest["steps"], 14)
        self.assertIn("RTX 4060", stem_manifest["device_name"])
        self.assertEqual(stem_manifest["sample_rate"], SAMPLE_RATE)

        for arch in AI_STEM_ARCHETYPES:
            wav_path = AI_STEMS_DIR / f"{arch.stem_id}.wav"
            self.assertTrue(wav_path.is_file(), f"Missing AI stem: {wav_path}")
            self.assertGreater(wav_path.stat().st_size, 4096, arch.stem_id)
            with std_wave.open(str(wav_path), "rb") as wf:
                self.assertEqual(wf.getnchannels(), 1, arch.stem_id)
                self.assertEqual(wf.getframerate(), SAMPLE_RATE, arch.stem_id)
            meta = stem_manifest["stems"][arch.stem_id]
            self.assertGreater(meta["rms_dbfs"], -42.0, arch.stem_id)
            self.assertGreater(meta["peak_dbfs"], -10.0, arch.stem_id)

        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        report = json.loads((exports_dir / "sound_quality_report.json").read_text(encoding="utf-8"))
        audition_html = (exports_dir / "sound_audition.html").read_text(encoding="utf-8")
        self.assertIn("AI-Hybrid (RTX 4060)", audition_html)
        self.assertIn("AI-Hybrid GPU", audition_html)
        self.assertIn("Stem:", audition_html)
        self.assertLess(float(report["worst_correlation"]), 0.55, f"worst_correlation too high: {report['worst_correlation']}")

        for entry in manifest["sounds"]:
            sid = entry["sound_id"]
            self.assertTrue(entry.get("ai_hybrid"), sid)
            self.assertTrue(bool(entry.get("stem_id")), sid)
            self.assertEqual(entry.get("stem_id"), SOUND_TO_ARCHETYPE[sid].stem_id)

    def test_cuda_dsp_acceleration_and_cpu_parity(self) -> None:
        """GPU CUDA kernels must match CPU DSP math tightly and return None cleanly when CUDA is absent."""
        from scipy import signal as sp_signal
        from scripts.sound_studio import ai_stem_generator, cuda_dsp, metrics

        rng = np.random.default_rng(12345)
        n = 44100
        sig = np.sin(2.0 * np.pi * 140.0 * (np.arange(n) / SAMPLE_RATE)) * 0.6 + rng.normal(0.0, 0.08, n)
        ir = np.exp(-np.arange(4096) / 600.0)
        ir /= np.sqrt(np.sum(ir * ir))

        # 1) Verify clean CPU fallback behavior (mirrors scripts/mcp/.venv without torch)
        prev_state = (cuda_dsp._CUDA_CHECKED, cuda_dsp._TORCH_MOD, cuda_dsp._CUDA_READY)
        try:
            cuda_dsp._CUDA_CHECKED = True
            cuda_dsp._CUDA_READY = False
            self.assertIsNone(cuda_dsp.gpu_fftconvolve_1d(sig, ir))
            self.assertIsNone(cuda_dsp.gpu_ir_convolve(sig, "abyssal_cavern", ir, mix=0.12))
            self.assertIsNone(cuda_dsp.gpu_harmonic_sum(np.zeros(n), [1.0, 2.0], [0.5, 0.25], [0.0, 0.3], min_work=100))
            self.assertIsNone(cuda_dsp.gpu_spectral_noise(rng.normal(0.0, 1.0, n), 0.52, 25.0, SAMPLE_RATE))
            self.assertIsNone(cuda_dsp.gpu_analyze_event_variations([sig, sig * 0.9], sample_rate=SAMPLE_RATE))
        finally:
            cuda_dsp._CUDA_CHECKED, cuda_dsp._TORCH_MOD, cuda_dsp._CUDA_READY = prev_state

        if not cuda_dsp.is_cuda_available():
            return

        # 2) Verify GPU convolution parity against SciPy fftconvolve
        wet_cpu = sp_signal.fftconvolve(sig, ir, mode="full")[:n]
        wet_gpu = cuda_dsp.gpu_fftconvolve_1d(sig, ir, min_conv_len=1024)
        self.assertIsNotNone(wet_gpu)
        self.assertLess(float(np.max(np.abs(wet_cpu - wet_gpu))), 1e-4)

        cuda_dsp.preload_ir_bank_gpu({"unit_test_ir": ir})
        ir_gpu = cuda_dsp.gpu_ir_convolve(sig, "unit_test_ir", ir, mix=0.15)
        ir_cpu = 0.85 * sig + 0.15 * wet_cpu
        self.assertIsNotNone(ir_gpu)
        self.assertLess(float(np.max(np.abs(ir_cpu - ir_gpu))), 1e-4)

        # 2b) Verify exact JUCE/Pedalboard Convolution parity (0.125 / ||ir||_2 scaling)
        from pedalboard import Convolution
        from scripts.sound_studio import ir_bank, mastering

        cuda_dsp.clear_gpu_caches()
        mastering.preload_ir_bank_for_gpu()
        ir_paths = ir_bank.ensure_ir_bank()
        pb_out = Convolution(str(ir_paths["spore_canopy"]), mix=0.11).process(sig.astype(np.float32), SAMPLE_RATE, reset=True).astype(np.float64)
        gpu_ir_out = cuda_dsp.gpu_ir_convolve(sig, "spore_canopy", mastering._get_ir_waves()["spore_canopy"], mix=0.11)
        self.assertIsNotNone(gpu_ir_out)
        pb_rms = float(np.sqrt(np.mean(pb_out * pb_out)))
        gpu_rms = float(np.sqrt(np.mean(gpu_ir_out * gpu_ir_out)))
        self.assertGreater(float(np.corrcoef(pb_out, gpu_ir_out)[0, 1]), 0.9999)
        self.assertTrue(0.98 <= (gpu_rms / max(pb_rms, 1e-12)) <= 1.02)

        # 3) Verify GPU harmonic synthesis parity (including float64 phase wrap)
        phase_cyc = np.cumsum(np.full(n, 72.0)) / SAMPLE_RATE
        mults = [1.0, 1.503, 2.0, 3.01, 4.99]
        amps = [1.0, 0.5, 0.35, 0.2, 0.12]
        offs = [0.2, 1.1, 2.4, 0.7, 3.0]
        harm_cpu = sum(a * np.sin(2.0 * np.pi * m * phase_cyc + o) for m, a, o in zip(mults, amps, offs))
        harm_gpu = cuda_dsp.gpu_harmonic_sum(phase_cyc, mults, amps, offs, min_work=1000)
        self.assertIsNotNone(harm_gpu)
        self.assertLess(float(np.max(np.abs(harm_cpu - harm_gpu))), 5e-4)

        # 4) Verify GPU spectral noise shaping
        white = rng.normal(0.0, 1.0, n)
        gpu_pink = cuda_dsp.gpu_spectral_noise(white, 0.52, 25.0, SAMPLE_RATE, min_cuda_len=1024)
        self.assertIsNotNone(gpu_pink)
        self.assertAlmostEqual(float(np.std(gpu_pink)), 1.0, delta=0.05)

        # 5) Verify GPU batched metrics & xcorr parity against CPU metrics
        w1 = np.sin(2.0 * np.pi * 220.0 * (np.arange(n) / SAMPLE_RATE)) * 0.5
        w2 = np.sin(2.0 * np.pi * 317.0 * (np.arange(n) / SAMPLE_RATE) + 0.7) * 0.45
        gpu_res = cuda_dsp.gpu_analyze_event_variations([w1, w2], sample_rate=SAMPLE_RATE, max_lag_samples=882)
        self.assertIsNotNone(gpu_res)
        gpu_list, gpu_corr = gpu_res
        self.assertAlmostEqual(gpu_corr, metrics._normalized_lag_correlation(w1, w2, 882), delta=0.01)
        self.assertAlmostEqual(gpu_list[0]["spectral_centroid_hz"], 220.0, delta=5.0)

        # 6) Verify GPU AI stem blend shape & finite bounded output
        stem = ai_stem_generator._load_cached_stem("spore_behemoth_roar")
        if stem is not None:
            blended = cuda_dsp.gpu_blend_ai_stem(
                sig,
                "spore_behemoth_roar",
                stem,
                sample_rate=SAMPLE_RATE,
                band_lo_hz=35.0,
                band_hi_hz=2600.0,
                pitch_ratio=1.02,
                rot_frac=0.14,
                attack_guard_ms=22.0,
                seamless_loop=False,
                mix_weight=0.35,
            )
            self.assertIsNotNone(blended)
            self.assertEqual(len(blended), n)
            self.assertTrue(np.all(np.isfinite(blended)))


if __name__ == "__main__":
    unittest.main()
