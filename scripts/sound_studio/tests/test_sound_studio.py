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
from scripts.sound_studio.boss_synth import BOSS_CINEMATIC_IDS, BOSS_PART_IMPACT_IDS
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
from scripts.sound_studio.mastering import SEAMLESS_LOOP_IDS, measure_true_peak_dbfs
from scripts.sound_studio.metrics import (
    _normalized_lag_correlation,
    analyze_waveform,
    compare_against_baseline,
    decode_ogg_to_float,
    evaluate_quality,
    load_acoustic_baseline,
    max_pairwise_correlation,
    save_acoustic_baseline,
)
from scripts.sound_studio.system_env_synth import SYSTEM_ENV_IDS


class SoundStudioTests(unittest.TestCase):
    def test_catalog_size_species_loudness_and_attenuation_invariants(self) -> None:
        self.assertEqual(len(SOUND_CATALOG), 226)
        self.assertEqual(len(SOUND_BY_ID), 226)
        consts = {s.java_const for s in SOUND_CATALOG}
        self.assertEqual(len(consts), 226)
        total_oggs = sum(s.variations for s in SOUND_CATALOG)
        self.assertEqual(total_oggs, 596)
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
                or spec.sound_id in BOSS_PART_IMPACT_IDS
                or spec.sound_id in BLOCK_FUNCTIONAL_IDS
                or spec.sound_id in ENTITY_SYNTH_IDS
                or spec.sound_id in ITEM_PROJECTILE_IDS
                or spec.sound_id in SYSTEM_ENV_IDS
                or spec.recipe.startswith("surface_")
            )
            self.assertTrue(is_dedicated, msg=f"{spec.sound_id} fell back to generic recipe")
            wave, m = self._synth_spec(spec.sound_id)
            self.assertEqual(wave.ndim, 1)
            self.assertGreater(len(wave), int(SAMPLE_RATE * 0.08))
            self.assertTrue(np.all(np.isfinite(wave)))
            issues = evaluate_quality(m, spec.loudness_group, is_loop=spec.sound_id in SEAMLESS_LOOP_IDS)
            self.assertEqual(issues, [], msg=f"{spec.sound_id} failed QA: {issues}")

    def test_loudness_hierarchy_footsteps_vs_boss_cinematic(self) -> None:
        _, m_step = self._synth_spec("mob_step_chitin")
        _, m_boss = self._synth_spec("apex_matriarch_wail")
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
            waves = [self._synth_spec(sid, variation_idx=i)[0] for i in range(spec.variations)]
            # Determinism check
            again, _ = self._synth_spec(sid, variation_idx=0)
            np.testing.assert_allclose(waves[0], again)
            corr = max_pairwise_correlation(waves)
            self.assertLess(corr, 0.85, msg=f"{sid} variations too correlated: {corr}")

    def test_bio_horror_vs_purifier_tech_spectral_contrast(self) -> None:
        _, m_brute = self._synth_spec("chitin_brute_growl")
        _, m_scan = self._synth_spec("bio_scanner_ping")
        self.assertGreater(m_brute["low_band_ratio"], 0.25)
        self.assertGreater(m_scan["spectral_centroid_hz"], m_brute["spectral_centroid_hz"] * 2.0)

    def test_ogg_vorbis_44100hz_mono_encode_and_decode_roundtrip(self) -> None:
        wave, _ = self._synth_spec("synaptic_pulse")
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

    def test_ogg_encoding_limits_decoded_true_peak(self) -> None:
        # Dense transients can overshoot after lossy encoding even with safe PCM input.
        from scripts.sound_studio.dsp_engine import TRUE_PEAK_CEILING
        from scipy.signal import resample_poly
        rng = np.random.default_rng(1927)
        wave = rng.uniform(-0.94, 0.94, SAMPLE_RATE // 4)
        with tempfile.TemporaryDirectory() as tmp:
            ogg = Path(tmp) / "transients.ogg"
            encode_ogg_vorbis_mono(float_to_pcm16(wave), ogg)
            decoded = decode_ogg_to_float(ogg)
            self.assertLessEqual(float(np.max(np.abs(resample_poly(decoded, 4, 1)))), TRUE_PEAK_CEILING)

    def test_generated_sounds_json_variants_and_attenuation(self) -> None:
        sounds_json = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind" / "sounds.json"
        if not sounds_json.is_file():
            self.skipTest("sounds.json not present")
        data = json.loads(sounds_json.read_text(encoding="utf-8"))
        self.assertEqual(len(data), 226)
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
        self.assertEqual(qr["total_events"], 226)
        self.assertEqual(qr["total_ogg_files"], 596)
        self.assertLess(qr["worst_correlation"], 0.85)
        self.assertLessEqual(float(qr.get("max_true_peak_dbfs", -96.0)), -0.25)
        self.assertTrue(qr.get("loop_seam_ok", False))
        self.assertTrue((qr.get("psychoacoustic_masking") or {}).get("ok", False))
        self.assertTrue((qr.get("stem_qc_summary") or {}).get("ok", False))
        self.assertGreater(float(qr.get("mean_timbral_diversity", 0.0)), 0.25)
        self.assertTrue(html_path.is_file())
        html_text = html_path.read_text(encoding="utf-8")
        self.assertIn("apex_matriarch_wail", html_text)
        self.assertIn("sinkhole_updraft_loop", html_text)
        self.assertIn("hive_tension_bed_low", html_text)
        self.assertIn("sanctum_harmonic_chime", html_text)
        self.assertIn("44100", html_text)
        self.assertIn("Consola Local de Audio v7", html_text)
        self.assertIn("mean_organic_index", html_text)
        self.assertIn("Bloques Físicos", html_text)
        self.assertIn("Biomas y Ambiente (23)", html_text)
        self.assertIn("fftCanvas", html_text)
        self.assertIn("specCanvas", html_text)
        self.assertIn("barkCanvas", html_text)
        self.assertIn("efxEnvSelect", html_text)
        self.assertIn("occSlider", html_text)
        self.assertIn("mix-deck", html_text)
        self.assertIn("seamStatus", html_text)
        self.assertIn("TP 4x", html_text)

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
        self.assertEqual(len(func_ids), 28)
        waves: list[np.ndarray] = []
        metrics: dict[str, dict[str, float]] = {}
        for sid in func_ids:
            w, m = self._synth_spec(sid)
            self.assertEqual(evaluate_quality(m, SOUND_BY_ID[sid].loudness_group, is_loop=sid in SEAMLESS_LOOP_IDS), [], msg=sid)
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
        """All 18 boss & ritual cinematics + 4 part/tactical impacts must span multi-act durations and separate identities."""
        self.assertEqual(len(BOSS_CINEMATIC_IDS), 18)
        self.assertEqual(len(BOSS_PART_IMPACT_IDS), 4)
        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        report = json.loads((exports_dir / "sound_quality_report.json").read_text(encoding="utf-8"))
        audition_html = (exports_dir / "sound_audition.html").read_text(encoding="utf-8")
        self.assertIn("Jefes y Cinemática (18)", audition_html)

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
            "colony_raid_horn",
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
        self.assertEqual(len(ENTITY_SYNTH_IDS), 71)
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
        """All 30 item, UI, weapon, consumable, relic & projectile events must be mutually distinct."""
        self.assertEqual(len(ITEM_PROJECTILE_IDS), 30)
        tech_tools = [
            "bio_scanner_ping",
            "bio_scanner_thermal_pulse",
            "bio_extractor_draw",
            "spore_antidote_cure",
            "counter_serum_inject",
            "neural_decoy_deploy",
            "purifier_drone_deploy",
            "ui_codex_tab",
            "ui_action_confirm",
            "resonant_elixir_drink",
        ]
        waves = [self._synth_spec(sid)[0] for sid in tech_tools]
        self.assertLess(max_pairwise_correlation(waves), 0.65)

    def test_biome_ambient_loops_and_tactical_alerts_meet_quality_invariants(self) -> None:
        """All biome ambient loops, beds, and tactical HUD alerts must synthesize cleanly with seamless loops."""
        self.assertEqual(len(SYSTEM_ENV_IDS), 42)
        self.assertEqual(len(SEAMLESS_LOOP_IDS), 11)
        biome_loops = ("biome_caverns_loop", "biome_sanctum_loop", "biome_forest_loop")
        loop_waves = []
        for sid in biome_loops:
            w, m = self._synth_spec(sid)
            self.assertGreaterEqual(m["duration_secs"], 5.0, sid)
            self.assertLessEqual(m["edge_start_abs"], 0.01, sid)
            self.assertLessEqual(m["edge_end_abs"], 0.01, sid)
            loop_waves.append(w)
        self.assertLess(max_pairwise_correlation(loop_waves), 0.55)
        for sid in sorted(SEAMLESS_LOOP_IDS):
            _, m = self._synth_spec(sid)
            self.assertLessEqual(m["loop_seam_delta"], 0.025, sid)
            self.assertLessEqual(m["true_peak_dbfs"], -0.25, sid)

    def test_stage1_pedalboard_lufs_and_convolution_ir_bank(self) -> None:
        """Stage 1: 6 mono 44.1 kHz IR .wav files, Pedalboard convolution, 4x True-Peak & LUFS must back all 200 sounds."""
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

        self.assertEqual(len(manifest["sounds"]), 226)
        for entry in manifest["sounds"]:
            self.assertIn(entry.get("ir_space"), IR_SPACE_NAMES, entry["sound_id"])
            lufs = entry["metrics"].get("integrated_lufs")
            self.assertIsNotNone(lufs, entry["sound_id"])
            self.assertTrue(-50.0 < float(lufs) < 0.0, f"{entry['sound_id']} LUFS out of range: {lufs}")
            tp = entry["metrics"].get("true_peak_dbfs")
            self.assertIsNotNone(tp, entry["sound_id"])
            self.assertLessEqual(float(tp), -0.25, entry["sound_id"])

        t = np.linspace(0.0, 0.8, int(SAMPLE_RATE * 0.8), endpoint=False)
        probe = np.sin(2.0 * np.pi * 110.0 * t) * np.exp(-t / 0.25)
        mastered = master_with_pedalboard(probe, sound_id="hive_core_resonance", species="synaptic_eldritch", loudness_group="boss_cinematic")
        self.assertEqual(len(mastered), len(probe))
        self.assertTrue(np.all(np.isfinite(mastered)))
        self.assertLessEqual(float(np.max(np.abs(mastered))), 0.945)
        self.assertLessEqual(measure_true_peak_dbfs(mastered), -0.25)
        self.assertTrue(-35.0 < measure_integrated_lufs(mastered, SAMPLE_RATE) < -3.0)

    def test_stage2_rtx4060_stable_audio_open_small_stems_and_hybrid_blend(self) -> None:
        """Stage 2: 95 RTX 4060 Stable Audio Open Small stems (steps=28) must back all 226 sounds with worst_corr < 0.55."""
        import wave as std_wave
        from scripts.sound_studio.ai_stem_generator import (
            AI_STEMS_DIR,
            AI_STEM_ARCHETYPES,
            INFERENCE_STEPS,
            SOUND_TO_ARCHETYPE,
            STEM_MANIFEST_PATH,
            audit_all_stems,
        )

        self.assertEqual(INFERENCE_STEPS, 28)
        self.assertEqual(len(AI_STEM_ARCHETYPES), 95)
        self.assertEqual(len(SOUND_TO_ARCHETYPE), 226)
        self.assertTrue(STEM_MANIFEST_PATH.is_file(), f"Missing stem manifest: {STEM_MANIFEST_PATH}")
        stem_manifest = json.loads(STEM_MANIFEST_PATH.read_text(encoding="utf-8"))
        self.assertEqual(stem_manifest["stem_count"], 95)
        self.assertEqual(stem_manifest["mapped_sound_events"], 226)
        self.assertEqual(stem_manifest["steps"], 28)
        self.assertIn("RTX 4060", stem_manifest["device_name"])
        self.assertEqual(stem_manifest["sample_rate"], SAMPLE_RATE)

        stem_qc = audit_all_stems(AI_STEMS_DIR)
        self.assertTrue(stem_qc["ok"], msg=f"Stem QC failed: {stem_qc['failed_stems']}")
        self.assertEqual(stem_qc["checked_count"], 95)

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
        self.assertIn("AI-Hybrid · Stem:", audition_html)
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

    def test_stage3_true_peak_baseline_diff_and_incremental_cache_contract(self) -> None:
        """Stage 3: Acoustic baseline comparison, incremental cache hashing & boss part spectral contrast."""
        from scripts.sound_studio.build_sounds import (
            BASELINE_PATH,
            BUILD_CACHE_PATH,
            _compute_engine_fingerprint,
            _compute_spec_cache_hash,
        )

        _, m_crit = self._synth_spec("boss_weakpoint_crit")
        _, m_rico = self._synth_spec("boss_carapace_ricochet")
        self.assertGreater(m_rico["spectral_centroid_hz"], m_crit["spectral_centroid_hz"])

        fp = _compute_engine_fingerprint()
        h1 = _compute_spec_cache_hash(SOUND_BY_ID["sovereign_nexus_hum"], fp)
        h2 = _compute_spec_cache_hash(SOUND_BY_ID["sovereign_nexus_hum"], fp)
        self.assertEqual(h1, h2)
        self.assertEqual(len(h1), 24)

        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        report = json.loads((exports_dir / "sound_quality_report.json").read_text(encoding="utf-8"))

        with tempfile.TemporaryDirectory() as tmp:
            bpath = Path(tmp) / "baseline.json"
            base = save_acoustic_baseline(manifest, report, bpath)
            loaded = load_acoustic_baseline(bpath)
            self.assertIsNotNone(loaded)
            same_diff = compare_against_baseline(manifest, report, loaded)  # type: ignore[arg-type]
            self.assertTrue(same_diff["ok"])
            self.assertEqual(same_diff["drifted_events"], [])

            mutated_base = json.loads(json.dumps(base))
            mutated_base["events"]["boss_weakpoint_crit"]["active_rms_dbfs"] -= 4.0
            drift_diff = compare_against_baseline(manifest, report, mutated_base)
            self.assertFalse(drift_diff["ok"])
            self.assertEqual(len(drift_diff["drifted_events"]), 1)

        if BASELINE_PATH.is_file():
            disk_base = load_acoustic_baseline(BASELINE_PATH)
            self.assertIsNotNone(disk_base)
            self.assertEqual(disk_base["total_events"], 226)  # type: ignore[index]
        if BUILD_CACHE_PATH.is_file():
            cache_data = json.loads(BUILD_CACHE_PATH.read_text(encoding="utf-8"))
            self.assertEqual(len(cache_data.get("event_hashes", {})), 226)

    def test_stage4_dsp_v4_hpss_bark_masking_lra_and_stem_qc(self) -> None:
        """Stage 4: DSP v4 primitives, 3-band HPSS parity, Bark 24-band masking, EBU R128 LRA & new v4 cues."""
        from scripts.sound_studio import ai_stem_generator, cuda_dsp
        from scripts.sound_studio.dsp_engine import (
            articulated_vocal_tract,
            fdn4_hadamard_diffuser,
            granular_bio_cloud,
            karplus_strong_chitin,
            rosenberg_glottal_pulse,
        )
        from scripts.sound_studio.metrics import (
            audit_spectral_masking,
            compute_bark_profile,
            compute_loudness_dynamics,
        )

        rng = np.random.default_rng(4242)
        n = int(SAMPLE_RATE * 0.65)
        t = np.arange(n, dtype=np.float64) / SAMPLE_RATE

        # 1) All 4 new physical/bio DSP v4 primitives produce bounded finite audio
        glottal = rosenberg_glottal_pulse(np.cumsum(np.linspace(110.0, 82.0, n)) / SAMPLE_RATE)
        vocal = articulated_vocal_tract(
            t,
            np.linspace(130.0, 95.0, n),
            [(320.0, 190.0, 2.8, 1.0), (920.0, 540.0, 3.2, 0.8)],
            rng,
        )
        cloud = granular_bio_cloud(n, rng, freq_range=(320.0, 2800.0), density_hz=45.0, organic_mode="viscous")
        chitin = karplus_strong_chitin(n, 380.0, rng, excitation="strike")
        diffused = fdn4_hadamard_diffuser(chitin + vocal * 0.4, rng, rt60=0.45, wet=0.25)
        for sig in (glottal, vocal, cloud, chitin, diffused):
            self.assertEqual(len(sig), n)
            self.assertTrue(np.all(np.isfinite(sig)))
            self.assertGreater(float(np.max(np.abs(sig))), 1e-3)

        # 2) CPU & GPU STFT Wiener HPSS parity (attenuates impulse spike while preserving sustained harmonic tone)
        probe = np.sin(2.0 * np.pi * 440.0 * t) * 0.5
        probe[n // 2] += 1.0
        cpu_hpss = ai_stem_generator._cpu_hpss_harmonic_soft(probe, SAMPLE_RATE, strength=0.60)
        self.assertEqual(len(cpu_hpss), n)
        self.assertTrue(np.all(np.isfinite(cpu_hpss)))
        self.assertLess(abs(float(cpu_hpss[n // 2])), abs(float(probe[n // 2])))
        if cuda_dsp.is_cuda_available():
            gpu_hpss = cuda_dsp.gpu_dualband_blend_shape(
                probe,
                np.sin(2.0 * np.pi * 440.0 * t),
                sample_rate=SAMPLE_RATE,
                band_lo_hz=60.0,
                band_hi_hz=6400.0,
                apply_hpss=True,
            )
            self.assertIsNotNone(gpu_hpss)
            self.assertEqual(len(gpu_hpss), n)
            self.assertTrue(np.all(np.isfinite(gpu_hpss)))

        # 3) Bark 24-band profile & EBU R128 dynamics (Momentary/Short-Term LUFS + LRA)
        bark_prof, peak_band, mud_r, harsh_r = compute_bark_profile(np.sin(2.0 * np.pi * 440.0 * t), SAMPLE_RATE)
        self.assertEqual(len(bark_prof), 24)
        self.assertAlmostEqual(sum(bark_prof), 1.0, delta=0.02)
        self.assertIn(peak_band, (3, 4))
        self.assertGreater(mud_r, 0.80)
        self.assertLess(harsh_r, 0.05)

        dyn = compute_loudness_dynamics(diffused, SAMPLE_RATE)
        self.assertGreater(dyn["momentary_lufs_max"], -70.0)
        self.assertGreater(dyn["short_term_lufs_max"], -70.0)
        self.assertGreaterEqual(dyn["lra_lu"], 0.0)

        # 4) Concurrent Bark spectral masking audit on live manifest passes cleanly
        exports_dir = REPO_ROOT / "scripts" / "sound_studio" / "exports"
        manifest = json.loads((exports_dir / "sound_manifest.json").read_text(encoding="utf-8"))
        masking = audit_spectral_masking(manifest["sounds"])
        self.assertTrue(masking["ok"], msg=f"Masking high-risk pairs: {masking['high_risk_pairs']}")
        self.assertEqual(masking["beds_checked"], 11)
        self.assertGreaterEqual(masking["pairs_evaluated"], 200)
        self.assertTrue(masking.get("bark_unmasking_applied"))
        self.assertEqual(masking.get("unmasking_carve_bands_hz"), [1450.0, 3200.0])

        # 5) All 12 new v4 events present & sinkhole_updraft_loop seamless
        new_v4_ids = (
            "ui_codex_tab",
            "ui_action_confirm",
            "sinkhole_updraft_loop",
            "root_sap_harvest",
            "root_cap_bounce",
            "resonant_elixir_drink",
            "boss_telegraph_lock",
            "boss_stagger_break",
            "brute_wall_stun",
            "pyro_shroom_defuse",
            "station_upkeep_low",
            "amber_vein_resonate",
        )
        for sid in new_v4_ids:
            self.assertIn(sid, SOUND_BY_ID, sid)
        _, m_updraft = self._synth_spec("sinkhole_updraft_loop")
        self.assertLessEqual(m_updraft["loop_seam_delta"], 0.025)
        self.assertLessEqual(m_updraft["true_peak_dbfs"], -0.25)

    def test_stage5_dsp_v5_morphing_modal_unmasking_and_adaptive_beds(self) -> None:
        """Stage 5: Morphing formant tract, modal dispersed impact, Bark unmasking carve, spectral tilt & 6 v5 cues."""
        from scripts.sound_studio import ai_stem_generator, cuda_dsp
        from scripts.sound_studio.dsp_engine import (
            modal_dispersed_impact,
            morphing_formant_tract,
        )
        from scripts.sound_studio.mastering import (
            BARK_UNMASK_CARVE_BANDS_HZ,
            apply_bark_unmasking_carve,
        )

        rng = np.random.default_rng(5050)
        n = int(SAMPLE_RATE * 0.70)
        t = np.arange(n, dtype=np.float64) / SAMPLE_RATE

        # 1) Morphing formant tract & modal dispersed impact primitives
        morph = morphing_formant_tract(
            t,
            np.linspace(140.0, 96.0, n),
            [
                (0.0, [(280.0, 2.8, 1.0), (880.0, 3.2, 0.7)]),
                (0.5, [(420.0, 3.0, 1.0), (1420.0, 3.4, 0.8)]),
                (1.0, [(260.0, 2.7, 0.9), (790.0, 3.1, 0.65)]),
            ],
            rng,
        )
        self.assertEqual(len(morph), n)
        self.assertTrue(np.all(np.isfinite(morph)))
        self.assertGreater(float(np.std(morph)), 1e-3)

        soft_modal = modal_dispersed_impact(n, 220.0, rng, num_modes=7, hardness=0.25, dispersion=0.04, decay_secs=0.35)
        hard_modal = modal_dispersed_impact(n, 220.0, rng, num_modes=7, hardness=0.90, dispersion=0.28, decay_secs=0.35)
        self.assertEqual(len(soft_modal), n)
        self.assertEqual(len(hard_modal), n)
        self.assertTrue(np.all(np.isfinite(hard_modal)))
        m_soft = analyze_waveform(soft_modal, SAMPLE_RATE)
        m_hard = analyze_waveform(hard_modal, SAMPLE_RATE)
        self.assertGreater(m_hard["spectral_centroid_hz"], m_soft["spectral_centroid_hz"])

        # 2) Bark unmasking dual-notch carve attenuates 1450/3200 Hz while leaving 220 Hz low band intact
        self.assertEqual(BARK_UNMASK_CARVE_BANDS_HZ, (1450.0, 3200.0))
        probe_low = np.sin(2.0 * np.pi * 220.0 * t) * 0.40
        probe_mid = np.sin(2.0 * np.pi * 1450.0 * t) * 0.50 + np.sin(2.0 * np.pi * 3200.0 * t) * 0.50
        carved = apply_bark_unmasking_carve(probe_low + probe_mid, SAMPLE_RATE)
        freqs = np.fft.rfftfreq(n, 1.0 / SAMPLE_RATE)
        spec_dry = np.abs(np.fft.rfft(probe_low + probe_mid))
        spec_carved = np.abs(np.fft.rfft(carved))
        mask_220 = (freqs >= 200.0) & (freqs <= 240.0)
        mask_1450 = (freqs >= 1400.0) & (freqs <= 1500.0)
        mask_3200 = (freqs >= 3120.0) & (freqs <= 3280.0)
        self.assertGreater(float(np.sum(spec_carved[mask_220])), float(np.sum(spec_dry[mask_220])) * 0.94)
        self.assertLess(float(np.sum(spec_carved[mask_1450])), float(np.sum(spec_dry[mask_1450])) * 0.85)
        self.assertLess(float(np.sum(spec_carved[mask_3200])), float(np.sum(spec_dry[mask_3200])) * 0.83)

        # 3) Spectral-centroid tilt alignment matches brightness toward target DSP on CPU & GPU
        stem_dark = np.sin(2.0 * np.pi * 160.0 * t) * 0.8 + np.sin(2.0 * np.pi * 2400.0 * t) * 0.12
        dsp_bright = np.sin(2.0 * np.pi * 160.0 * t) * 0.2 + np.sin(2.0 * np.pi * 2400.0 * t) * 0.85
        aligned_cpu = ai_stem_generator._cpu_align_stem_spectral_tilt(stem_dark, dsp_bright, SAMPLE_RATE, strength=0.45)
        c_before = analyze_waveform(stem_dark, SAMPLE_RATE)["spectral_centroid_hz"]
        c_after = analyze_waveform(aligned_cpu, SAMPLE_RATE)["spectral_centroid_hz"]
        self.assertGreater(c_after, c_before)
        if cuda_dsp.is_cuda_available():
            import torch

            dev = torch.device("cuda")
            t_stem = torch.tensor(stem_dark.astype(np.float32), device=dev)
            t_dsp = torch.tensor(dsp_bright.astype(np.float32), device=dev)
            aligned_gpu_t = cuda_dsp._gpu_align_spectral_tilt(torch, t_stem, t_dsp, SAMPLE_RATE, strength=0.45)
            aligned_gpu = aligned_gpu_t.detach().cpu().numpy().astype(np.float64)
            self.assertGreater(analyze_waveform(aligned_gpu, SAMPLE_RATE)["spectral_centroid_hz"], c_before)

        # 4) All 6 new v5 events exist and both new adaptive beds form seamless loops
        new_v5_ids = (
            "hive_tension_bed_low",
            "hive_swarm_combat_bed",
            "cavern_spore_drip",
            "subterranean_hyphal_creak",
            "deep_mycelium_whisper",
            "sanctum_harmonic_chime",
        )
        for sid in new_v5_ids:
            self.assertIn(sid, SOUND_BY_ID, sid)
        for bed_id in ("hive_tension_bed_low", "hive_swarm_combat_bed"):
            _, bm = self._synth_spec(bed_id)
            self.assertLessEqual(bm["loop_seam_delta"], 0.025, bed_id)
            self.assertLessEqual(bm["true_peak_dbfs"], -0.25, bed_id)
            self.assertEqual(evaluate_quality(bm, SOUND_BY_ID[bed_id].loudness_group, is_loop=True), [], msg=bed_id)

    def test_stage6_bio_organic_humanization_and_95_stems(self) -> None:
        """Stage 6: Bijective 95-stem coverage, de-vocoded blend, damped short reverb & organic_index scoring."""
        from scripts.sound_studio import ai_stem_generator, dsp_engine
        from scripts.sound_studio.metrics import compute_organic_metrics

        arches = ai_stem_generator.AI_STEM_ARCHETYPES
        self.assertEqual(len(arches), 95)
        mapped = [sid for a in arches for sid in a.sound_ids]
        self.assertEqual(len(mapped), 226)
        self.assertEqual(set(mapped), set(SOUND_BY_ID.keys()))

        # Loop archetypes must match only loop sounds (not one-shots like suppressor_discharge or mood cues)
        loop_mapped = {sid for a in arches if a.seamless_loop for sid in a.sound_ids}
        self.assertNotIn("suppressor_discharge", loop_mapped)
        self.assertNotIn("biome_caverns_mood", loop_mapped)
        self.assertIn("biome_caverns_loop", loop_mapped)
        self.assertIn("purification_pylon_hum", loop_mapped)

        # organic_index must clearly separate a static pure sine wave from organic creature audio
        t = np.linspace(0.0, 1.0, SAMPLE_RATE, endpoint=False)
        static_sine = np.sin(2.0 * np.pi * 440.0 * t) * 0.5
        sine_org = compute_organic_metrics(static_sine, SAMPLE_RATE)
        mob_wave, mob_metrics = self._synth_spec("chitin_brute_growl", variation_idx=0)
        self.assertGreater(mob_metrics["organic_index"], sine_org["organic_index"] + 0.15)
        self.assertGreater(len(mob_wave), SAMPLE_RATE // 4)

        # Short-SFX _cavern_reverb must be tightly damped compared to long cinematic tail
        impulse = np.zeros(SAMPLE_RATE, dtype=np.float64)
        impulse[100] = 0.9
        short_wet = dsp_engine._cavern_reverb(impulse, np.random.default_rng(7), wet=0.25, rt60=0.35)
        long_wet = dsp_engine._cavern_reverb(impulse, np.random.default_rng(7), wet=0.25, rt60=1.20)
        tail_short_rms = float(np.sqrt(np.mean(short_wet[int(0.05 * SAMPLE_RATE) : int(0.30 * SAMPLE_RATE)] ** 2)))
        tail_long_rms = float(np.sqrt(np.mean(long_wet[int(0.05 * SAMPLE_RATE) : int(0.30 * SAMPLE_RATE)] ** 2)))
        self.assertGreater(tail_long_rms, tail_short_rms * 1.8)

        # Macro de-vocoded blend preserves audible stem body even when DSP dips briefly
        gapped_dsp = np.sin(2.0 * np.pi * 220.0 * t) * 0.25
        gapped_dsp[int(0.35 * SAMPLE_RATE) : int(0.65 * SAMPLE_RATE)] *= 0.05
        blended = ai_stem_generator.blend_with_ai_stem(gapped_dsp, "chitin_brute_growl", variation_idx=0)
        self.assertEqual(len(blended), SAMPLE_RATE)
        self.assertFalse(np.any(np.isnan(blended)))

    def test_stage7_minecraft_foley_craft_and_copper_amethyst_primitives(self) -> None:
        """Stage 7: Zero sci-fi/synth stem prompts, copper-amethyst/wooden/particle physical primitives, schema v7."""
        from scripts.sound_studio import ai_stem_generator, dsp_engine
        from scripts.sound_studio.build_sounds import BASELINE_PATH

        banned = ("sci-fi", "synth", "futuristic", "quadcopter", "electronic", "radar", "plasma", "telemetry", "siren", "kaiju")
        for arch in ai_stem_generator.AI_STEM_ARCHETYPES:
            low = arch.prompt.lower()
            for word in banned:
                self.assertNotIn(word, low, msg=f"Stem {arch.stem_id} prompt still uses robotic word '{word}'")

        rng = np.random.default_rng(2026)
        t = np.linspace(0.0, 0.35, int(SAMPLE_RATE * 0.35), endpoint=False)
        chime = dsp_engine.amethyst_copper_chime(t, 520.0, rng, decay=0.12, crystal_mix=0.65)
        knock = dsp_engine.wooden_chitin_knock(len(t), 440.0, rng, decay=0.04, hollowness=0.6)
        crackle = dsp_engine.foley_particle_crackle(len(t), rng, count=6)
        for arr in (chime, knock, crackle):
            self.assertEqual(len(arr), len(t))
            self.assertTrue(np.all(np.isfinite(arr)))
            self.assertGreater(float(np.max(np.abs(arr))), 0.05)

        if BASELINE_PATH.is_file():
            base = json.loads(BASELINE_PATH.read_text(encoding="utf-8"))
            self.assertEqual(base.get("schema_version"), 7)


if __name__ == "__main__":
    unittest.main()
