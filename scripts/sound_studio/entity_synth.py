"""Dedicated physical-modeling and vocal-tract synthesizers for all 61 non-boss entity sound events.

Provides distinct, anatomically motivated sonic identities for the 14 non-boss living creatures
(4 events each: ambient, hurt, death, special + chimera_roar = 57) and the 4 non-colossal
anatomical step profiles (mob_step_chitin, mob_step_arachnid, mob_step_heavy, mob_step_hover).
"""

from __future__ import annotations

import numpy as np

from scripts.sound_studio.block_synth import (
    _bounce_train,
    _micro_arc_zaps,
    _modal_bank,
)
from scripts.sound_studio.dsp_engine import (
    SAMPLE_RATE,
    _apply_formant_bank,
    _brown_noise,
    _cavern_reverb,
    _chitin_modal_hits,
    _fm_tone,
    _glottal_sub_source,
    _pink_noise,
    _sos_bandpass,
    _sos_highpass,
    _swept_resonant_filter,
    _wet_squelch_layer,
    adsr_envelope,
    exp_decay,
    fungal_cap_resonance,
    mycelial_hyphae_tear,
    spore_sac_burst,
)

ENTITY_SYNTH_IDS: frozenset[str] = frozenset(
    {
        # 1. Sporeling (4)
        "sporeling_chitter",
        "sporeling_hurt",
        "sporeling_death",
        "sporeling_burst",
        # 2. Chitin Brute (4)
        "chitin_brute_growl",
        "chitin_brute_hurt",
        "chitin_brute_death",
        "chitin_brute_smash",
        # 3. Mycelial Stalker (4)
        "stalker_hiss",
        "stalker_hurt",
        "stalker_death",
        "stalker_pounce",
        # 4. Acid Spitter (4)
        "acid_spitter_gurgle",
        "acid_spitter_hurt",
        "acid_spitter_death",
        "acid_spitter_spit",
        # 5. Burrower (4)
        "burrower_grind",
        "burrower_hurt",
        "burrower_death",
        "burrower_tunnel",
        # 6. Pyro-Shroom (4)
        "pyro_shroom_crackle",
        "pyro_shroom_hurt",
        "pyro_shroom_death",
        "pyro_shroom_ignite",
        # 7. Synapse Weaver (4)
        "synapse_weaver_hum",
        "synapse_weaver_hurt",
        "synapse_weaver_death",
        "synapse_weaver_buff",
        # 8. Goliath Carrier (4)
        "goliath_carrier_thud",
        "goliath_carrier_hurt",
        "goliath_carrier_death",
        "goliath_carrier_spawn",
        # 9. Leech Parasite (4)
        "leech_parasite_screech",
        "leech_parasite_hurt",
        "leech_parasite_death",
        "leech_parasite_siphon",
        # 10. Colony Warden (4)
        "colony_warden_bellow",
        "colony_warden_hurt",
        "colony_warden_death",
        "colony_warden_shockwave",
        # 11. Spore Architect (4)
        "spore_architect_pulse",
        "spore_architect_hurt",
        "spore_architect_death",
        "spore_architect_construct",
        # 12. Synaptic Mimic (4)
        "synaptic_mimic_echo",
        "synaptic_mimic_hurt",
        "synaptic_mimic_death",
        "synaptic_mimic_strike",
        # 13. Purifier Drone (4)
        "purifier_drone_hum",
        "purifier_drone_hurt",
        "purifier_drone_death",
        "purifier_drone_fire",
        # 13b. Colony Symbiont (3)
        "colony_symbiont_ambient",
        "colony_symbiont_work",
        "colony_symbiont_promote",
        # 14. Mycelial Chimera (5)
        "mycelial_chimera_growl",
        "mycelial_chimera_hurt",
        "mycelial_chimera_death",
        "chimera_roar",
        "mycelial_chimera_strike",
        # 14b. Spore Glider & Root Keeper (7)
        "spore_glider_ambient",
        "spore_glider_hurt",
        "spore_glider_death",
        "root_keeper_ambient",
        "root_keeper_hurt",
        "root_keeper_death",
        "root_keeper_slam",
        # 15. Anatomical Steps (4; mob_step_colossal is in BOSS_CINEMATIC_IDS)
        "mob_step_chitin",
        "mob_step_arachnid",
        "mob_step_heavy",
        "mob_step_hover",
    }
)


def _comb_echo(sig: np.ndarray, delay_ms: float, feedback: float = 0.52) -> np.ndarray:
    """Apply a short resonant comb-filter delay for uncanny mimic/double-voice timbre."""
    d = max(12, int(SAMPLE_RATE * delay_ms / 1000.0))
    out = sig.copy()
    if d < len(sig):
        out[d:] += sig[:-d] * feedback
    if 2 * d < len(sig):
        out[2 * d :] += sig[: -2 * d] * (feedback * 0.55)
    return out


def synthesize_entity_sound(
    sound_id: str,
    *,
    duration: float,
    base_freq: float,
    mod_freq: float,
    rng: np.random.Generator,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a bespoke multi-stage creature or anatomical footstep waveform."""
    dur = max(0.14, float(duration))
    n = max(64, int(round(dur * SAMPLE_RATE)))
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)
    bf = max(32.0, float(base_freq) * float(rng.uniform(0.95, 1.05)))
    mf = max(1.0, float(mod_freq) * float(rng.uniform(0.92, 1.08)))
    pjit = float(rng.uniform(0.0, 2.0 * np.pi))

    # =========================================================================
    # 1. SPORELING — Hyper-agile fungal-spore scout (spongy cap + hyphae chirps)
    # =========================================================================
    if sound_id.startswith("sporeling_"):
        action = sound_id.split("_", 1)[1]
        if action == "chitter":
            wave = np.zeros(n, dtype=np.float64)
            bursts = int(rng.integers(5, 8))
            for idx, pos in enumerate(_bounce_train(n, rng, bursts, start_frac=0.03, end_frac=0.86, accelerate=False)):
                blen = min(n - pos, max(64, int(SAMPLE_RATE * rng.uniform(0.038, 0.068))))
                bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
                f_chirp = bf * rng.uniform(0.92, 1.38) * (1.0 + 0.25 * np.sin(2.0 * np.pi * (mf * 1.4) * bt))
                chirp = _fm_tone(bt, f_chirp, f_chirp * 1.5, 2.1, rng.uniform(0, 6.28))
                click = _modal_bank(bt, bf * 2.4, (1.0, 2.04, 3.65), (0.015, 0.010, 0.006), (0.65, 0.42, 0.22), rng)
                cap_tap = fungal_cap_resonance(blen, bf * 0.65, rng, decay=0.035, sponginess=0.70)
                wave[pos : pos + blen] += (chirp * 0.64 + click * 0.52 + cap_tap * 0.45) * np.sin(np.linspace(0, np.pi, blen))
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.55, count=4, wetness=0.45) * 0.42
            tract = _apply_formant_bank(wave + hyphae * 0.35, [(bf * 1.25 * formant_shift, 3.6, 1.0), (bf * 2.7 * formant_shift, 4.3, 0.80)])
            return _cavern_reverb(_sos_highpass(tract + hyphae * 0.45, 200.0), rng, wet=0.14, rt60=0.30, lp_hz=6800.0)
        if action == "hurt":
            snap = _modal_bank(t, bf * 2.1, (1.0, 2.35, 3.95), (0.025, 0.016, 0.010), (0.72, 0.50, 0.30), rng)
            cap = fungal_cap_resonance(n, bf * 0.52, rng, decay=0.08, sponginess=0.72) * 0.65
            f_sq = bf * (1.42 - 0.82 * (t / dur))
            squeal = _fm_tone(t, f_sq, mf * 3.0, 2.3, pjit) * adsr_envelope(n, 0.02, 0.20, 0.40, 0.40)
            tract = _apply_formant_bank(squeal, [(bf * 1.5 * formant_shift, 3.4, 1.05), (bf * 2.9 * formant_shift, 4.0, 0.72)])
            return _cavern_reverb(_sos_highpass(snap * 0.72 + cap + tract, 180.0), rng, wet=0.15, rt60=0.32, lp_hz=6400.0)
        if action == "death":
            f_down = np.maximum(125.0, bf * 1.22 * np.exp(-1.55 * t / dur))
            whistle = _fm_tone(t, f_down, mf * 2.4, 2.1, pjit) * (0.6 + 0.4 * np.sin(2.0 * np.pi * 26.0 * t))
            rattle = _chitin_modal_hits(n, rng, bf * 1.7, 7, 0.028, spread_frac=0.78) * 0.72
            sac_pop = spore_sac_burst(n, rng, base_freq=bf * 0.65, pressure=0.68, duration_frac=0.75) * 0.62
            raw = whistle * adsr_envelope(n, 0.02, 0.22, 0.38, 0.45) + rattle + sac_pop
            return _cavern_reverb(_sos_highpass(raw, 175.0), rng, wet=0.17, rt60=0.38, lp_hz=6200.0)
        # sporeling_burst
        creak_n = min(n // 5, int(SAMPLE_RATE * 0.036))
        wave = np.zeros(n, dtype=np.float64)
        if creak_n > 16:
            ct = np.arange(creak_n, dtype=np.float64) / SAMPLE_RATE
            wave[:creak_n] = np.sin(2.0 * np.pi * np.linspace(bf * 0.75, bf * 1.8, creak_n) * ct) * 0.55
        sac = spore_sac_burst(n, rng, base_freq=bf * 0.78, pressure=0.92, duration_frac=0.85) * 0.95
        cap = fungal_cap_resonance(n, bf * 0.62, rng, decay=0.10, sponginess=0.82) * 0.58
        return _cavern_reverb(wave + sac + cap, rng, wet=0.18, rt60=0.36, lp_hz=5600.0)

    # =========================================================================
    # 2. CHITIN BRUTE — Heavy fungal-armored monstrous shock-trooper
    # =========================================================================
    if sound_id.startswith("chitin_brute_"):
        action = sound_id.split("chitin_brute_", 1)[1]
        plate_ratios = (1.0, 2.28, 3.95, 6.10)
        if action == "growl":
            f_curve = bf * (1.08 - 0.26 * (t / dur) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit))
            glottal = _glottal_sub_source(t, f_curve, rng, harmonics=13, sub_weight=0.95, rattle_hz=20.0)
            chest = _apply_formant_bank(glottal, [(86.0 * formant_shift, 2.0, 1.65), (235.0 * formant_shift, 2.8, 0.88), (640.0, 3.5, 0.30)])
            sub_chest = np.sin(2.0 * np.pi * np.cumsum(np.maximum(34.0, f_curve * 0.85)) / SAMPLE_RATE + pjit) * 1.15
            cap_chest = fungal_cap_resonance(n, bf * 1.45, rng, decay=0.28, sponginess=0.65) * 0.50
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.2, count=4, wetness=0.45) * 0.22
            plate_clack = _modal_bank(t, bf * 3.4, plate_ratios, (0.06, 0.04, 0.025, 0.015), (0.55, 0.38, 0.24, 0.14), rng) * 0.22
            raw = np.tanh((chest * 1.35 + sub_chest + cap_chest + hyphae + plate_clack) * 1.35) * adsr_envelope(n, 0.06, 0.22, 0.58, 0.30)
            return _cavern_reverb(raw, rng, wet=0.24, rt60=0.52, lp_hz=1750.0)
        if action == "hurt":
            armor_hit = _modal_bank(t, bf * 2.9, plate_ratios, (0.055, 0.038, 0.024, 0.014), (0.80, 0.58, 0.36, 0.20), rng) * 0.85
            cap_hit = fungal_cap_resonance(n, bf * 1.5, rng, decay=0.14, sponginess=0.72) * 0.85
            f_gr = bf * (1.25 - 0.50 * (t / dur))
            grunt = _apply_formant_bank(_glottal_sub_source(t, f_gr, rng, harmonics=11, sub_weight=0.88, rattle_hz=23.0), [(125.0 * formant_shift, 2.5, 1.18), (325.0 * formant_shift, 3.0, 0.82)])
            raw = np.tanh((armor_hit + cap_hit + grunt * adsr_envelope(n, 0.03, 0.22, 0.45, 0.35)) * 1.3)
            return _cavern_reverb(raw, rng, wet=0.22, rt60=0.44, lp_hz=2800.0)
        if action == "death":
            f_down = np.maximum(32.0, bf * 1.15 * np.exp(-1.15 * t / dur))
            roar = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.98, rattle_hz=17.0), [(102.0 * formant_shift, 2.4, 1.25), (260.0, 2.8, 0.78)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.0, count=6, wetness=0.55) * 0.48
            crash_idx = int(rng.uniform(0.42, 0.54) * n)
            crash = np.zeros(n, dtype=np.float64)
            if n - crash_idx > 48:
                ct = np.arange(n - crash_idx, dtype=np.float64) / SAMPLE_RATE
                sub_thud = np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, 125.0 * exp_decay(ct, 0.048))) / SAMPLE_RATE) * exp_decay(ct, 0.24) * 1.25
                plates = _modal_bank(ct, bf * 2.6, plate_ratios, (0.09, 0.06, 0.04, 0.025), (0.75, 0.52, 0.32, 0.18), rng) * 0.75
                cap_crush = fungal_cap_resonance(len(ct), bf * 1.35, rng, decay=0.20, sponginess=0.70) * 0.75
                crash[crash_idx:] = sub_thud + plates + cap_crush
            raw = np.tanh((roar * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + hyphae + crash) * 1.35)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.60, lp_hz=2500.0)
        # chitin_brute_smash
        f_drop = np.maximum(28.0, bf * (0.38 + 2.4 * exp_decay(t, 0.045)))
        sub = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.48) * 1.3
        armor_slam = _modal_bank(t, bf * 3.1, plate_ratios, (0.08, 0.055, 0.036, 0.022), (0.85, 0.60, 0.40, 0.22), rng) * 0.85
        cap_boom = fungal_cap_resonance(n, bf * 1.4, rng, decay=0.18, sponginess=0.72) * 0.75
        puff = spore_sac_burst(n, rng, base_freq=bf * 1.9, pressure=0.65, duration_frac=0.55) * 0.45
        debris = _sos_bandpass(brown * 0.8 + pink * 0.5, 45.0, 1100.0) * exp_decay(t, dur * 0.36) * 0.80
        return _cavern_reverb(np.tanh((sub + armor_slam + cap_boom + puff + debris) * 1.4), rng, wet=0.26, rt60=0.54, lp_hz=2400.0)

    # =========================================================================
    # 3. MYCELIAL STALKER — Razor-clawed fungal-hyphae ambush predator
    # =========================================================================
    if sound_id.startswith("stalker_"):
        action = sound_id.split("stalker_", 1)[1]
        if action == "hiss":
            aspirate = _swept_resonant_filter(pink * 0.65 + white * 0.50, bf * 4.2, bf * 1.1, q=3.2)
            sibilant = _sos_bandpass(white, 2400.0, 10800.0) * (0.45 + 0.55 * np.sin(2.0 * np.pi * mf * t + pjit)) * 0.48
            throat = _glottal_sub_source(t, np.linspace(bf * 0.35, bf * 0.24, n), rng, harmonics=7, sub_weight=0.52, rattle_hz=32.0) * 0.42
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.48, count=5, wetness=0.52) * 0.52
            claw_pos = min(n - 64, int(rng.uniform(0.18, 0.42) * n))
            claw = np.zeros(n, dtype=np.float64)
            ct = np.arange(n - claw_pos, dtype=np.float64) / SAMPLE_RATE
            claw[claw_pos:] = _modal_bank(ct, bf * 3.4, (1.0, 2.55, 4.65), (0.024, 0.015, 0.009), (0.58, 0.40, 0.22), rng) * 0.48
            raw = (aspirate * 1.05 + sibilant + throat + hyphae * 0.72) * adsr_envelope(n, 0.05, 0.22, 0.48, 0.32) + claw
            return _cavern_reverb(raw, rng, wet=0.18, rt60=0.40, lp_hz=6800.0)
        if action == "hurt":
            f_sh = bf * (1.36 - 0.62 * (t / dur))
            shriek = _fm_tone(t, f_sh, f_sh * 0.72, 2.5, pjit) * 0.72
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.65, count=5, wetness=0.58) * 0.65
            snap = _modal_bank(t, bf * 2.7, (1.0, 2.45, 4.5), (0.03, 0.018, 0.010), (0.70, 0.46, 0.26), rng) * 0.58
            raw = (shriek + hyphae + snap) * adsr_envelope(n, 0.015, 0.20, 0.42, 0.38)
            return _cavern_reverb(raw, rng, wet=0.16, rt60=0.36, lp_hz=6400.0)
        if action == "death":
            exhale = _swept_resonant_filter(pink * 0.7 + white * 0.45, 4200.0, 380.0, q=2.4) * adsr_envelope(n, 0.03, 0.25, 0.40, 0.42)
            f_down = np.maximum(78.0, bf * np.exp(-1.5 * t / dur))
            raspy_wail = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=10, sub_weight=0.58, rattle_hz=29.0), [(bf * 1.1 * formant_shift, 3.0, 1.0), (bf * 2.5, 3.6, 0.75)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.52, count=6, wetness=0.60) * 0.62
            return _cavern_reverb(exhale * 0.85 + raspy_wail * 0.80 + hyphae, rng, wet=0.20, rt60=0.44, lp_hz=5400.0)
        # stalker_pounce
        whoosh = _swept_resonant_filter(pink * 0.7 + white * 0.45, 880.0, 6200.0, q=2.2) * np.sin(np.pi * t / dur) * 1.05
        slash = _modal_bank(t, bf * 3.2, (1.0, 2.08, 3.72, 5.40), (0.045, 0.030, 0.018, 0.010), (0.75, 0.52, 0.32, 0.18), rng, glide_factor=-0.35, glide_tau=0.04)
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.60, count=4, wetness=0.45) * 0.58
        return _cavern_reverb(np.tanh((whoosh + slash * 0.88 + hyphae) * 1.3), rng, wet=0.16, rt60=0.34, lp_hz=6800.0)

    # =========================================================================
    # 4. ACID SPITTER — Peristaltic corrosive spore-sac bio-artillery
    # =========================================================================
    if sound_id.startswith("acid_spitter_"):
        action = sound_id.split("acid_spitter_", 1)[1]
        if action == "gurgle":
            churn_f = bf * (0.75 + 0.35 * np.sin(2.0 * np.pi * (mf * 0.45) * t + pjit))
            sac = _apply_formant_bank(_glottal_sub_source(t, churn_f, rng, harmonics=8, sub_weight=0.70, rattle_hz=18.0), [(205.0 * formant_shift, 3.1, 1.15), (540.0 * formant_shift, 3.6, 0.85)])
            cap_gourd = fungal_cap_resonance(n, bf * 0.75, rng, decay=0.18, sponginess=0.78) * 0.55
            bubbles = _wet_squelch_layer(n, rng, bf * 1.2, int(rng.integers(9, 15)), intensity=1.18)
            return _cavern_reverb(np.tanh((sac * 0.75 + cap_gourd + bubbles * 1.05) * 1.25) * adsr_envelope(n, 0.06, 0.20, 0.58, 0.28), rng, wet=0.18, rt60=0.40, lp_hz=3600.0)
        if action == "hurt":
            slap = _swept_resonant_filter(pink, 1300.0, 290.0, q=3.0) * exp_decay(t, 0.065) * 0.95
            cap_slap = fungal_cap_resonance(n, bf * 0.82, rng, decay=0.11, sponginess=0.80) * 0.75
            splash = _wet_squelch_layer(n, rng, bf * 1.35, 5, intensity=1.0) * exp_decay(t, dur * 0.55)
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.9, count=4, wetness=0.72) * 0.48
            return _cavern_reverb(np.tanh((slap + cap_slap + splash + hyphae) * 1.25), rng, wet=0.17, rt60=0.38, lp_hz=4600.0)
        if action == "death":
            pop = spore_sac_burst(n, rng, base_freq=bf * 0.85, pressure=0.98, duration_frac=0.65) * 0.95
            deflate = _wet_squelch_layer(n, rng, bf * 1.05, 11, intensity=1.20) * adsr_envelope(n, 0.04, 0.24, 0.48, 0.42)
            fizz = _sos_bandpass(white, 2600.0, 10800.0) * adsr_envelope(n, 0.15, 0.25, 0.45, 0.40) * 0.50
            return _cavern_reverb(np.tanh((pop + deflate + fizz) * 1.3), rng, wet=0.22, rt60=0.46, lp_hz=4400.0)
        # acid_spitter_spit (jittered sac draw-back onset, pitch arc & corrosive spore hiss band)
        draw_sec = float(rng.uniform(0.024, 0.092))
        draw_n = min(n // 3, int(SAMPLE_RATE * draw_sec))
        wave = np.zeros(n, dtype=np.float64)
        if draw_n > 20:
            dt = np.arange(draw_n, dtype=np.float64) / SAMPLE_RATE
            draw_lo = float(rng.uniform(190.0, 340.0))
            draw_hi = float(rng.uniform(900.0, 1650.0))
            wave[:draw_n] = _swept_resonant_filter(pink[:draw_n], draw_lo, draw_hi, q=float(rng.uniform(2.4, 3.8))) * (dt / max(1e-4, dt[-1])) * 0.78
        pt = np.arange(n - draw_n, dtype=np.float64) / SAMPLE_RATE
        spit_mult = float(rng.uniform(0.84, 1.18))
        arc_depth = float(rng.uniform(1.35, 2.35))
        arc_tau = float(rng.uniform(0.018, 0.044))
        f_spit = np.maximum(72.0, bf * spit_mult * (0.52 + arc_depth * exp_decay(pt, arc_tau)))
        phase_spit = 2.0 * np.pi * np.cumsum(f_spit) / SAMPLE_RATE + pjit
        ptoo = (np.sin(phase_spit) + 0.34 * np.sin(1.48 * phase_spit + pjit * 0.7)) * exp_decay(pt, float(rng.uniform(0.065, 0.11))) * 1.10
        sac_ej = spore_sac_burst(len(pt), rng, base_freq=bf * spit_mult, pressure=0.72, duration_frac=0.75) * 0.55
        hiss_lo = float(rng.uniform(1950.0, 3400.0))
        hiss_hi = float(rng.uniform(8400.0, 12000.0))
        spray = (
            _wet_squelch_layer(len(pt), rng, bf * float(rng.uniform(1.25, 1.85)), int(rng.integers(3, 6)), intensity=0.90)
            + _sos_bandpass(white[draw_n:], hiss_lo, hiss_hi) * 0.54
        ) * exp_decay(pt, dur * float(rng.uniform(0.32, 0.48)))
        wave[draw_n:] = np.tanh((ptoo + sac_ej + spray) * 1.28)
        return _cavern_reverb(wave, rng, wet=0.15, rt60=0.34, lp_hz=5200.0)

    # =========================================================================
    # 5. BURROWER — Subterranean fungal-root tearing mandible beetle
    # =========================================================================
    if sound_id.startswith("burrower_"):
        action = sound_id.split("burrower_", 1)[1]
        if action == "grind":
            rotary = 0.35 + 0.65 * np.abs(np.sin(2.0 * np.pi * (mf * 0.55) * t + pjit))
            rock_grind = _apply_formant_bank((pink * 0.7 + white * 0.38) * rotary, [(295.0 * formant_shift, 2.2, 1.1), (1280.0 * formant_shift, 2.6, 0.85), (2950.0, 3.2, 0.50)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.25, count=7, wetness=0.52) * 0.62
            mandibles = _chitin_modal_hits(n, rng, bf * 2.0, int(rng.integers(6, 10)), 0.038, spread_frac=0.84) * 0.68
            sub_drill = _glottal_sub_source(t, bf * (0.85 + 0.18 * np.sin(2.0 * np.pi * mf * t)), rng, harmonics=7, sub_weight=0.75) * 0.52
            raw = np.tanh((rock_grind * 1.10 + hyphae + mandibles + sub_drill) * 1.3) * adsr_envelope(n, 0.04, 0.22, 0.55, 0.28)
            return _cavern_reverb(raw, rng, wet=0.24, rt60=0.48, lp_hz=3400.0)
        if action == "hurt":
            flint = _sos_bandpass(white, 2100.0, 9400.0) * exp_decay(t, 0.022) * 0.82
            cap = fungal_cap_resonance(n, bf * 1.1, rng, decay=0.12, sponginess=0.68) * 0.72
            shell = _modal_bank(t, bf * 2.3, (1.0, 2.08, 3.55), (0.045, 0.030, 0.018), (0.75, 0.52, 0.30), rng) * 0.75
            groan = _glottal_sub_source(t, bf * (1.15 - 0.45 * (t / dur)), rng, harmonics=9, sub_weight=0.78) * exp_decay(t, dur * 0.45) * 0.72
            return _cavern_reverb(np.tanh((flint + cap + shell + groan) * 1.25), rng, wet=0.20, rt60=0.42, lp_hz=3900.0)
        if action == "death":
            f_stall = np.maximum(40.0, bf * 1.18 * np.exp(-1.4 * t / dur))
            drill_down = _glottal_sub_source(t, f_stall, rng, harmonics=10, sub_weight=0.85, rattle_hz=15.0) * adsr_envelope(n, 0.03, 0.22, 0.42, 0.44)
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.15, count=6, wetness=0.58) * 0.55
            gravel = np.zeros(n, dtype=np.float64)
            for pos in _bounce_train(n, rng, 10, start_frac=0.12, end_frac=0.88, accelerate=True):
                plen = min(n - pos, int(SAMPLE_RATE * 0.028))
                pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
                gravel[pos : pos + plen] += _sos_bandpass(rng.normal(0.0, 1.0, plen), 1200.0, 5800.0) * exp_decay(pt, 0.010) * 0.68
            return _cavern_reverb(np.tanh((drill_down + hyphae + gravel) * 1.3), rng, wet=0.25, rt60=0.54, lp_hz=3200.0)
        # burrower_tunnel
        sub = np.sin(2.0 * np.pi * np.cumsum(np.maximum(32.0, bf * (0.45 + 1.6 * exp_decay(t, 0.04)))) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.45) * 1.18
        crush = _apply_formant_bank((brown * 0.7 + pink * 0.6) * adsr_envelope(n, 0.03, 0.25, 0.45, 0.35), [(260.0, 2.0, 1.1), (1180.0, 2.4, 0.85)])
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.4, count=5, wetness=0.55) * 0.55
        pebbles = _chitin_modal_hits(n, rng, bf * 3.0, 6, 0.032, spread_frac=0.75) * 0.62
        return _cavern_reverb(np.tanh((sub + crush + hyphae + pebbles) * 1.35), rng, wet=0.25, rt60=0.52, lp_hz=2900.0)

    # =========================================================================
    # 6. PYRO-SHROOM — Volatile swollen ember-fungal bomb
    # =========================================================================
    if sound_id.startswith("pyro_shroom_"):
        action = sound_id.split("pyro_shroom_", 1)[1]
        if action == "crackle":
            furnace = _sos_bandpass(brown * 0.8 + pink * 0.4, 85.0, 480.0) * (0.55 + 0.45 * np.sin(2.0 * np.pi * (mf * 0.4) * t + pjit)) * 0.75
            cap_warm = fungal_cap_resonance(n, bf * 0.48, rng, decay=0.16, sponginess=0.72) * 0.58
            sparks = np.zeros(n, dtype=np.float64)
            for pos in _bounce_train(n, rng, int(rng.integers(12, 20)), start_frac=0.02, end_frac=0.90, accelerate=False):
                slen = min(n - pos, max(24, int(SAMPLE_RATE * rng.uniform(0.006, 0.022))))
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                sf = bf * rng.uniform(2.2, 6.2)
                sparks[pos : pos + slen] += (_fm_tone(st, sf, sf * 0.4, 1.5) + rng.normal(0, 0.45, slen)) * exp_decay(st, 0.005) * rng.uniform(0.5, 1.0)
            return _cavern_reverb((furnace + cap_warm + sparks * 1.05) * adsr_envelope(n, 0.04, 0.20, 0.56, 0.28), rng, wet=0.15, rt60=0.34, lp_hz=6400.0)
        if action == "hurt":
            cap_thump = fungal_cap_resonance(n, bf * 0.55, rng, decay=0.12, sponginess=0.80) * 0.90
            steam = _swept_resonant_filter(white, 5600.0, 1650.0, q=3.2) * exp_decay(t, dur * 0.48) * 0.80
            sac = spore_sac_burst(n, rng, base_freq=bf * 0.72, pressure=0.68, duration_frac=0.65) * 0.60
            cinders = _micro_arc_zaps(n, rng, 4, lo_hz=2600.0, hi_hz=8800.0, spread=(0.05, 0.65)) * 0.45
            return _cavern_reverb(np.tanh((cap_thump + steam + sac + cinders) * 1.25), rng, wet=0.16, rt60=0.34, lp_hz=6000.0)
        if action == "death":
            woof = _sos_bandpass(brown * 0.8 + pink * 0.6, 60.0, 920.0) * exp_decay(t, dur * 0.38) * 1.15
            sac = spore_sac_burst(n, rng, base_freq=bf * 0.65, pressure=0.90, duration_frac=0.72) * 0.85
            whistle = _fm_tone(t, np.maximum(85.0, bf * 1.35 * np.exp(-1.6 * t / dur)), mf * 1.5, 1.4, pjit) * exp_decay(t, dur * 0.35) * 0.55
            embers = _micro_arc_zaps(n, rng, 8, lo_hz=2100.0, hi_hz=9600.0, spread=(0.12, 0.88)) * 0.52
            return _cavern_reverb(np.tanh((woof + sac + whistle + embers) * 1.25), rng, wet=0.20, rt60=0.42, lp_hz=5200.0)
        # pyro_shroom_ignite (accelerating thermal fungal spore-sac fuse!)
        frac = t / dur
        f_rise = bf * (0.65 + 2.05 * (frac ** 1.7))
        kettle = _fm_tone(t, f_rise, f_rise * 0.33, 1.8, pjit) * (frac ** 1.2) * 0.80
        cap_swell = fungal_cap_resonance(n, bf * 0.60, rng, decay=0.22, sponginess=0.75) * (0.35 + 0.65 * frac) * 0.72
        jet = _swept_resonant_filter(white * 0.55 + pink * 0.5, 850.0, 7200.0, q=2.8) * (0.25 + 0.75 * (frac ** 1.3)) * 0.90
        ticks = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 11, start_frac=0.04, end_frac=0.92, accelerate=True):
            tlen = min(n - pos, int(SAMPLE_RATE * 0.016))
            tt = np.arange(tlen, dtype=np.float64) / SAMPLE_RATE
            ticks[pos : pos + tlen] += _sos_bandpass(rng.normal(0, 1, tlen), 2200.0, 9200.0) * exp_decay(tt, 0.004) * 0.75
        return _cavern_reverb(np.tanh((kettle + cap_swell + jet + ticks) * 1.3) * adsr_envelope(n, 0.03, 0.15, 0.75, 0.14), rng, wet=0.16, rt60=0.35, lp_hz=6800.0)

    # =========================================================================
    # 7. SYNAPSE WEAVER — Eerie fungal-mycelial neural spore caster
    # =========================================================================
    if sound_id.startswith("synapse_weaver_"):
        action = sound_id.split("synapse_weaver_", 1)[1]
        q_ratios = (1.0, 1.46, 2.24, 3.38)
        q_amps = (0.32, 0.24, 0.16, 0.10)
        if action == "hum":
            lfo = 1.0 + 0.018 * np.sin(2.0 * np.pi * mf * t + pjit)
            gourd_bell = fungal_cap_resonance(n, bf * 0.72, rng, decay=0.30, sponginess=0.72) * 0.85
            murmur = _apply_formant_bank(
                _glottal_sub_source(t, bf * 0.42 * lfo, rng, harmonics=8, sub_weight=0.62, rattle_hz=20.0),
                [(210.0 * formant_shift, 2.6, 1.0), (540.0 * formant_shift, 3.2, 0.78)],
            ) * 0.62
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.85, count=4, wetness=0.52) * 0.52
            overtone = sum(
                a * np.sin(2.0 * np.pi * np.cumsum(bf * r * lfo) / SAMPLE_RATE + rng.uniform(0, 6.28))
                for r, a in zip(q_ratios, q_amps)
            )
            raw = (gourd_bell + murmur + hyphae + overtone * 0.48) * adsr_envelope(n, 0.09, 0.22, 0.58, 0.34)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.62, lp_hz=4200.0)
        if action == "hurt":
            cap_crack = fungal_cap_resonance(n, bf * 0.88, rng, decay=0.14, sponginess=0.68) * 0.90
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.95, count=5, wetness=0.60) * 0.68
            shatter = _modal_bank(t, bf * 1.25, q_ratios, (0.08, 0.06, 0.04, 0.028), q_amps, rng, glide_factor=-0.24, glide_tau=0.04) * 0.58
            murmur = _glottal_sub_source(t, np.maximum(65.0, bf * (0.78 - 0.32 * (t / dur))), rng, harmonics=9, sub_weight=0.65) * 0.48
            return _cavern_reverb(cap_crack + hyphae + shatter + murmur, rng, wet=0.22, rt60=0.48, lp_hz=4800.0)
        if action == "death":
            shards = np.zeros(n, dtype=np.float64)
            for idx, pos in enumerate(_bounce_train(n, rng, 6, start_frac=0.02, end_frac=0.78, accelerate=True)):
                slen = min(n - pos, int(SAMPLE_RATE * 0.14))
                shards[pos : pos + slen] += fungal_cap_resonance(slen, bf * rng.uniform(0.75, 1.35), rng, decay=0.12, sponginess=0.72) * (0.75 * (0.86 ** idx))
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.82, count=6, wetness=0.58) * 0.62
            f_down = np.maximum(52.0, bf * 0.88 * np.exp(-1.35 * t / dur))
            sigh = _glottal_sub_source(t, f_down, rng, harmonics=9, sub_weight=0.75, rattle_hz=21.0) * adsr_envelope(n, 0.04, 0.22, 0.45, 0.44) * 0.65
            return _cavern_reverb(shards + hyphae + sigh, rng, wet=0.28, rt60=0.62, lp_hz=4400.0)
        # synapse_weaver_buff (3-stage fungal-spore promotion pulse + eerie gourd chord)
        wave = np.zeros(n, dtype=np.float64)
        for idx, ratio in enumerate((1.0, 1.26, 1.58)):
            pos = int((idx * 0.24) * n)
            slen = n - pos
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            wave[pos:] += fungal_cap_resonance(slen, bf * ratio * 0.85, rng, decay=0.22, sponginess=0.68) * 0.85
            wave[pos:] += _modal_bank(st, bf * ratio, (1.0, 1.48, 2.24), (0.18, 0.12, 0.08), (0.38, 0.26, 0.16), rng)
        sac = spore_sac_burst(n, rng, base_freq=bf * 0.80, pressure=0.65, duration_frac=0.80) * 0.58
        return _cavern_reverb(wave * 0.9 + sac, rng, wet=0.26, rt60=0.56, lp_hz=4800.0)

    # =========================================================================
    # 8. GOLIATH CARRIER — Bloated fungal brood-sac colossus
    # =========================================================================
    if sound_id.startswith("goliath_carrier_"):
        action = sound_id.split("goliath_carrier_", 1)[1]
        if action == "thud":
            f_drop = np.maximum(26.0, bf * (0.45 + 1.85 * exp_decay(t, 0.05)))
            belly = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.48) * 1.15
            cap_belly = fungal_cap_resonance(n, bf * 1.15, rng, decay=0.26, sponginess=0.82) * 0.82
            groan = _apply_formant_bank(_glottal_sub_source(t, np.full(n, bf * 0.90), rng, harmonics=11, sub_weight=0.95, rattle_hz=15.0), [(92.0 * formant_shift, 2.2, 1.15), (235.0 * formant_shift, 2.8, 0.85)]) * adsr_envelope(n, 0.08, 0.22, 0.52, 0.34) * 0.78
            slosh = _wet_squelch_layer(n, rng, bf * 1.7, 6, intensity=0.85) * adsr_envelope(n, 0.10, 0.25, 0.45, 0.35)
            return _cavern_reverb(np.tanh((belly + cap_belly + groan + slosh) * 1.3), rng, wet=0.25, rt60=0.54, lp_hz=2200.0)
        if action == "hurt":
            hit_s = int(rng.uniform(0.01, 0.12) * n)
            t_hit = np.maximum(0.0, t - t[hit_s])
            tau_drop = rng.uniform(0.025, 0.050)
            thump = np.zeros(n, dtype=np.float64)
            f_thump = np.maximum(34.0, bf * (0.5 + rng.uniform(1.3, 1.8) * np.exp(-t_hit[hit_s:] / tau_drop)))
            thump[hit_s:] = np.sin(2.0 * np.pi * np.cumsum(f_thump) / SAMPLE_RATE + rng.uniform(0.0, 6.28)) * np.exp(-t_hit[hit_s:] / 0.12) * 1.12
            grunt_env = adsr_envelope(n - hit_s, 0.03, 0.22, 0.45, 0.36)
            grunt_src = _glottal_sub_source(t[hit_s:], bf * (1.15 - 0.42 * (t[hit_s:] / max(0.1, dur))), rng, harmonics=10, sub_weight=0.92, rattle_hz=rng.uniform(15.0, 23.0))
            grunt = np.zeros(n, dtype=np.float64)
            grunt[hit_s:] = _apply_formant_bank(grunt_src, [(110.0 * formant_shift, 2.4, 1.18), (280.0 * formant_shift, 3.0, 0.78)]) * grunt_env
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.6, count=4, wetness=0.72) * 0.55
            wet = _wet_squelch_layer(n, rng, bf * 1.55, int(rng.integers(4, 8)), intensity=0.92) * exp_decay(t, dur * 0.55)
            return _cavern_reverb(np.tanh((thump + grunt + hyphae + wet) * 1.3), rng, wet=0.22, rt60=0.46, lp_hz=2600.0)
        if action == "death":
            f_down = np.maximum(26.0, bf * 1.1 * np.exp(-1.1 * t / dur))
            bellow = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.98, rattle_hz=14.0), [(90.0 * formant_shift, 2.3, 1.25), (230.0, 2.8, 0.80)]) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.44)
            sac = spore_sac_burst(n, rng, base_freq=bf * 1.4, pressure=0.95, duration_frac=0.80) * 0.85
            ruptures = _wet_squelch_layer(n, rng, bf * 1.85, 11, intensity=1.25) * adsr_envelope(n, 0.12, 0.25, 0.50, 0.38)
            slam_idx = int(0.38 * n)
            slam = np.zeros(n, dtype=np.float64)
            st = np.arange(n - slam_idx, dtype=np.float64) / SAMPLE_RATE
            slam[slam_idx:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(26.0, 110.0 * exp_decay(st, 0.05))) / SAMPLE_RATE) * exp_decay(st, 0.28) * 1.25
            return _cavern_reverb(np.tanh((bellow + sac + ruptures + slam) * 1.35), rng, wet=0.28, rt60=0.64, lp_hz=2300.0)
        # goliath_carrier_spawn (hyphae membrane tear -> wet fungal brood sac burst -> sporeling emergence)
        tear = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.3, count=6, wetness=0.78) * exp_decay(t, dur * 0.52) * 0.90
        sac_pop = spore_sac_burst(n, rng, base_freq=bf * 1.15, pressure=0.90, duration_frac=0.75) * 0.88
        brood_s = int(0.42 * n)
        brood = np.zeros(n, dtype=np.float64)
        bt = np.arange(n - brood_s, dtype=np.float64) / SAMPLE_RATE
        brood[brood_s:] = _fm_tone(bt, 600.0 * (1.0 + 0.25 * np.sin(2.0 * np.pi * 16.0 * bt)), 90.0, 2.0) * adsr_envelope(len(bt), 0.08, 0.25, 0.45, 0.38) * 0.52
        return _cavern_reverb(np.tanh((tear + sac_pop + brood) * 1.3), rng, wet=0.22, rt60=0.46, lp_hz=4000.0)

    # =========================================================================
    # 9. LEECH PARASITE — Lamprey spore-sucker & hyphae sap siphon
    # =========================================================================
    if sound_id.startswith("leech_parasite_"):
        action = sound_id.split("leech_parasite_", 1)[1]
        if action == "screech":
            curve = np.sin(np.pi * t / dur) ** 0.85
            f_sc = bf * (0.86 + 0.82 * curve + 0.06 * np.sin(2.0 * np.pi * 38.0 * t + pjit))
            shriek = _fm_tone(t, f_sc, f_sc * 0.5, 2.7, pjit)
            tract = _apply_formant_bank(shriek, [(bf * 1.10 * formant_shift, 3.4, 0.95), (min(5800.0, bf * 2.45 * formant_shift), 4.0, 1.20), (min(8200.0, bf * 3.8), 4.6, 0.75)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.55, count=4, wetness=0.68) * 0.52
            raw = np.tanh((tract * 1.10 + hyphae) * 1.25) * adsr_envelope(n, 0.03, 0.18, 0.55, 0.30)
            return _cavern_reverb(_sos_highpass(raw, 250.0), rng, wet=0.16, rt60=0.36, lp_hz=7400.0)
        if action == "hurt":
            f_sq = bf * (1.42 - 0.72 * (t / dur))
            pinch = _fm_tone(t, f_sq, mf * 3.8, 2.4, pjit) * adsr_envelope(n, 0.015, 0.20, 0.40, 0.40)
            cap = fungal_cap_resonance(n, bf * 0.58, rng, decay=0.07, sponginess=0.76) * 0.58
            squish = _wet_squelch_layer(n, rng, bf * 0.88, 3, intensity=0.82) * exp_decay(t, dur * 0.48)
            return _cavern_reverb(_sos_highpass(pinch + cap + squish, 220.0), rng, wet=0.15, rt60=0.32, lp_hz=7000.0)
        if action == "death":
            f_down = np.maximum(150.0, bf * 1.28 * np.exp(-1.65 * t / dur))
            wail = _fm_tone(t, f_down, f_down * 0.65, 2.3, pjit) * adsr_envelope(n, 0.02, 0.20, 0.40, 0.45)
            sac = spore_sac_burst(n, rng, base_freq=bf * 0.52, pressure=0.72, duration_frac=0.70) * 0.68
            splat = _wet_squelch_layer(n, rng, bf * 0.7, 6, intensity=1.08) * adsr_envelope(n, 0.08, 0.25, 0.45, 0.40)
            return _cavern_reverb(_sos_highpass(wail * 0.82 + sac + splat, 195.0), rng, wet=0.18, rt60=0.38, lp_hz=6600.0)
        # leech_parasite_siphon
        wave = np.zeros(n, dtype=np.float64)
        for idx, start_f in enumerate((0.02, 0.46)):
            pos = int(start_f * n)
            slen = min(n - pos, int(0.44 * n))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            f_suck = bf * (0.55 + 0.95 * (st / max(1e-4, st[-1])))
            suck = np.sin(2.0 * np.pi * np.cumsum(f_suck) / SAMPLE_RATE + idx) * np.sin(np.linspace(0, np.pi, slen))
            slurp = _swept_resonant_filter(pink[pos : pos + slen], 360.0, 2400.0, q=3.4) * np.sin(np.linspace(0, np.pi, slen))
            wave[pos : pos + slen] += suck * 0.82 + slurp * 0.90
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.60, count=4, wetness=0.75) * 0.48
        return _cavern_reverb(wave + hyphae + _wet_squelch_layer(n, rng, bf * 0.8, 5, intensity=0.75), rng, wet=0.15, rt60=0.34, lp_hz=5600.0)

    # =========================================================================
    # 10. COLONY WARDEN — Fungal-armored pretorian commander (deep root horn)
    # =========================================================================
    if sound_id.startswith("colony_warden_"):
        action = sound_id.split("colony_warden_", 1)[1]
        shield_ratios = (1.0, 1.48, 2.18, 3.25, 4.55)
        if action == "bellow":
            f_cmd = bf * (0.96 + 0.16 * np.sin(np.pi * t / dur) - 0.12 * (t / dur))
            src = _glottal_sub_source(t, f_cmd, rng, harmonics=13, sub_weight=0.96, rattle_hz=19.0)
            fifth = _glottal_sub_source(t, f_cmd * 1.498, rng, harmonics=8, sub_weight=0.48, rattle_hz=19.0) * 0.55
            horn = _apply_formant_bank(src + fifth, [(118.0 * formant_shift, 2.4, 1.25), (320.0 * formant_shift, 2.9, 0.95), (820.0, 3.5, 0.52)])
            cap_chest = fungal_cap_resonance(n, bf * 1.35, rng, decay=0.32, sponginess=0.70) * 0.62
            sub = np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, f_cmd * 0.5)) / SAMPLE_RATE + pjit) * 0.78
            raw = np.tanh((horn + cap_chest + sub) * 1.4) * adsr_envelope(n, 0.09, 0.20, 0.62, 0.32)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.64, lp_hz=2500.0)
        if action == "hurt":
            shield_s = int(rng.uniform(0.0, 0.14) * n)
            shield = np.zeros(n, dtype=np.float64)
            st = np.arange(n - shield_s, dtype=np.float64) / SAMPLE_RATE
            shield[shield_s:] = (
                _modal_bank(st, bf * rng.uniform(2.5, 3.6), shield_ratios, (0.08, 0.06, 0.04, 0.025, 0.015), (0.78, 0.58, 0.40, 0.26, 0.16), rng) * 0.85
                + fungal_cap_resonance(len(st), bf * rng.uniform(1.3, 1.8), rng, decay=0.14, sponginess=0.72) * 0.75
            )
            f_snarl = bf * rng.uniform(0.88, 1.14) * (rng.uniform(1.12, 1.32) - rng.uniform(0.35, 0.55) * (t / dur))
            fs = formant_shift * rng.uniform(0.88, 1.14)
            snarl = _apply_formant_bank(_glottal_sub_source(t, f_snarl, rng, harmonics=11, sub_weight=0.90), [(125.0 * fs, 2.5, 1.18), (345.0 * fs, 3.0, 0.82)]) * adsr_envelope(n, 0.02, 0.22, 0.46, 0.35)
            return _cavern_reverb(np.tanh((shield + snarl) * 1.3), rng, wet=0.16, rt60=0.40, lp_hz=3100.0)
        if action == "death":
            f_down = np.maximum(30.0, bf * 1.18 * np.exp(-1.15 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.96), [(100.0 * formant_shift, 2.4, 1.22), (270.0, 2.9, 0.82)]) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.44)
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.8, count=6, wetness=0.55) * 0.48
            imp = int(0.44 * n)
            crash = np.zeros(n, dtype=np.float64)
            ct = np.arange(n - imp, dtype=np.float64) / SAMPLE_RATE
            crash[imp:] = (
                np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, 130.0 * exp_decay(ct, 0.045))) / SAMPLE_RATE) * exp_decay(ct, 0.25) * 1.2
                + fungal_cap_resonance(len(ct), bf * 1.4, rng, decay=0.20, sponginess=0.72) * 0.85
            )
            return _cavern_reverb(np.tanh((groan + hyphae + crash) * 1.35), rng, wet=0.28, rt60=0.64, lp_hz=2600.0)
        # colony_warden_shockwave
        charge_n = min(n // 5, int(SAMPLE_RATE * 0.09))
        wave = np.zeros(n, dtype=np.float64)
        if charge_n > 24:
            ct = np.arange(charge_n, dtype=np.float64) / SAMPLE_RATE
            wave[:charge_n] = _swept_resonant_filter(pink[:charge_n], 150.0, 1650.0, q=2.8) * (ct / max(1e-4, ct[-1])) * 0.85
        bt = np.arange(n - charge_n, dtype=np.float64) / SAMPLE_RATE
        f_drop = np.maximum(28.0, bf * (0.36 + 2.3 * exp_decay(bt, 0.065)))
        sub_blast = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(bt, dur * 0.55) * 1.35
        cap_slam = fungal_cap_resonance(len(bt), bf * 1.5, rng, decay=0.22, sponginess=0.68) * 0.85
        puff = spore_sac_burst(len(bt), rng, base_freq=bf * 1.9, pressure=0.78, duration_frac=0.65) * 0.62
        roar = _sos_bandpass(brown[charge_n:] * 0.8 + pink[charge_n:] * 0.5, 42.0, 1500.0) * exp_decay(bt, dur * 0.45) * 0.92
        wave[charge_n:] = np.tanh((sub_blast + cap_slam + puff + roar) * 1.35)
        return _cavern_reverb(wave, rng, wet=0.28, rt60=0.64, lp_hz=2800.0)

    # =========================================================================
    # 11. SPORE ARCHITECT — Hyphae-weaving fungal sculptor
    # =========================================================================
    if sound_id.startswith("spore_architect_"):
        action = sound_id.split("spore_architect_", 1)[1]
        if action == "pulse":
            wave = np.zeros(n, dtype=np.float64)
            for idx, ratio in enumerate((1.0, 1.189, 1.498)):
                pos = int((0.04 + idx * 0.26) * n)
                plen = min(n - pos, int(SAMPLE_RATE * 0.22))
                pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
                tone = _fm_tone(pt, bf * ratio * 0.90, bf * 0.45, 1.4, rng.uniform(0, 6.28)) * np.sin(np.linspace(0, np.pi, plen))
                cap = fungal_cap_resonance(plen, bf * ratio * 0.65, rng, decay=0.11, sponginess=0.72) * 0.65
                extrude = _wet_squelch_layer(plen, rng, bf * 0.9, 2, intensity=0.55)
                wave[pos : pos + plen] += tone * 0.58 + cap + extrude * 0.45
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.80, count=4, wetness=0.55) * 0.48
            return _cavern_reverb(wave + hyphae, rng, wet=0.22, rt60=0.48, lp_hz=4200.0)
        if action == "hurt":
            cap = fungal_cap_resonance(n, bf * 0.85, rng, decay=0.12, sponginess=0.72) * 0.85
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.95, count=5, wetness=0.62) * 0.65
            groan = _glottal_sub_source(t, np.maximum(58.0, bf * (1.1 - 0.5 * (t / dur))), rng, harmonics=9, sub_weight=0.70) * adsr_envelope(n, 0.02, 0.2, 0.4, 0.4) * 0.65
            return _cavern_reverb(cap + hyphae + groan, rng, wet=0.19, rt60=0.42, lp_hz=4500.0)
        if action == "death":
            f_down = np.maximum(48.0, bf * np.exp(-1.3 * t / dur))
            loom = (_glottal_sub_source(t, f_down, rng, harmonics=9, sub_weight=0.78) * 0.68 + fungal_cap_resonance(n, bf * 0.75, rng, decay=0.22, sponginess=0.76) * 0.75) * adsr_envelope(n, 0.03, 0.22, 0.45, 0.44)
            scaffold = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.90, count=7, wetness=0.62) * 0.72
            sac = spore_sac_burst(n, rng, base_freq=bf * 0.80, pressure=0.70, duration_frac=0.75) * 0.55
            return _cavern_reverb(loom + scaffold + sac, rng, wet=0.25, rt60=0.54, lp_hz=4000.0)
        # spore_architect_construct
        weave = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.0, count=6, wetness=0.68) * exp_decay(t, dur * 0.52) * 0.82
        thud = _sos_bandpass(brown, 65.0, 320.0) * exp_decay(t, 0.14) * 0.95
        sac = spore_sac_burst(n, rng, base_freq=bf * 0.90, pressure=0.72, duration_frac=0.65) * 0.62
        chord_s = int(0.32 * n)
        chord = np.zeros(n, dtype=np.float64)
        chord[chord_s:] = fungal_cap_resonance(n - chord_s, bf * 0.95, rng, decay=0.22, sponginess=0.70) * 0.95
        return _cavern_reverb(weave + thud + sac + chord, rng, wet=0.24, rt60=0.50, lp_hz=4800.0)

    # =========================================================================
    # 12. SYNAPTIC MIMIC — Double-voice fungal gourd shapeshifter
    # =========================================================================
    if sound_id.startswith("synaptic_mimic_"):
        action = sound_id.split("synaptic_mimic_", 1)[1]
        delay_ms = float(rng.uniform(13.0, 22.0))
        if action == "echo":
            f_mir = bf * (0.88 + 0.24 * np.sin(2.0 * np.pi * (mf * 0.45) * t + pjit))
            v1 = _glottal_sub_source(t, f_mir, rng, harmonics=10, sub_weight=0.58, rattle_hz=26.0)
            v2 = fungal_cap_resonance(n, bf * 0.72, rng, decay=0.22, sponginess=0.68) * 0.68
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.75, count=4, wetness=0.52) * 0.48
            tract = _apply_formant_bank(v1 + v2 * 0.6 + hyphae * 0.4, [(bf * 1.05 * formant_shift, 3.4, 1.0), (bf * 2.25 * formant_shift, 4.0, 1.05), (bf * 3.8, 4.6, 0.60)])
            combed = _comb_echo(tract * adsr_envelope(n, 0.05, 0.20, 0.55, 0.34) + v2 * 0.4, delay_ms, feedback=0.58)
            return _cavern_reverb(combed, rng, wet=0.25, rt60=0.54, lp_hz=5400.0)
        if action == "hurt":
            f_gl = bf * (1.32 - 0.55 * (t / dur)) * (1.0 + 0.12 * np.sign(np.sin(2.0 * np.pi * 22.0 * t)))
            voice = _glottal_sub_source(t, np.maximum(65.0, f_gl), rng, harmonics=9, sub_weight=0.55, rattle_hz=28.0) * adsr_envelope(n, 0.015, 0.20, 0.42, 0.38)
            cap = fungal_cap_resonance(n, bf * 0.85, rng, decay=0.10, sponginess=0.70) * 0.80
            return _cavern_reverb(_comb_echo(voice + cap, delay_ms, feedback=0.56), rng, wet=0.22, rt60=0.46, lp_hz=5600.0)
        if action == "death":
            f_low = np.maximum(52.0, bf * np.exp(-1.45 * t / dur))
            f_high = np.minimum(2100.0, bf * (0.88 + 0.80 * (t / dur)))
            split_v = _glottal_sub_source(t, f_low, rng, harmonics=10, sub_weight=0.72) * 0.75 + _fm_tone(t, f_high, mf * 2.4, 1.9, pjit + 1.2) * exp_decay(t, dur * 0.48) * 0.52
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.85, count=6, wetness=0.58) * 0.72
            raw = _comb_echo(split_v * adsr_envelope(n, 0.03, 0.22, 0.45, 0.44) + hyphae, delay_ms, feedback=0.54)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.60, lp_hz=5200.0)
        # synaptic_mimic_strike
        warp = _swept_resonant_filter(pink * 0.6 + white * 0.45, 3800.0, 480.0, q=3.2) * exp_decay(t, dur * 0.45)
        cap_hit = fungal_cap_resonance(n, bf * 0.92, rng, decay=0.11, sponginess=0.68) * 0.85
        sac = spore_sac_burst(n, rng, base_freq=bf * 0.80, pressure=0.72, duration_frac=0.65) * 0.58
        return _cavern_reverb(_comb_echo(warp + cap_hit + sac, delay_ms=16.0, feedback=0.62), rng, wet=0.19, rt60=0.40, lp_hz=5800.0)

    # =========================================================================
    # 13. PURIFIER DRONE — Symbiotic fungal-spore hover escort (organic gourd + wings)
    # =========================================================================
    if sound_id.startswith("purifier_drone_"):
        action = sound_id.split("purifier_drone_", 1)[1]
        if action == "hum":
            # Hollow mushroom-gourd hover resonance + fluttering leathery spore-wing pulse (wide per-take jitter)
            f_scale = rng.uniform(0.86, 1.14)
            lfo_rate = mf * rng.uniform(0.75, 1.35)
            wing_mod = 0.65 + 0.35 * np.sin(2.0 * np.pi * lfo_rate * t + pjit)
            hum_start = int(rng.uniform(0.0, 0.12) * n)
            rt = t[hum_start:] - t[hum_start]
            gourd = np.zeros(n, dtype=np.float64)
            gourd[hum_start:] = fungal_cap_resonance(len(rt), (bf * 0.48) * f_scale, rng, decay=0.28, sponginess=0.72) * 0.90
            throat = np.zeros(n, dtype=np.float64)
            throat[hum_start:] = _apply_formant_bank(
                _glottal_sub_source(rt, (bf * 0.44) * f_scale * (1.0 + 0.05 * np.sin(2.0 * np.pi * lfo_rate * rt)), rng, harmonics=8, sub_weight=0.55, rattle_hz=rng.uniform(16.0, 26.0)),
                [(260.0 * formant_shift, 2.8, 0.95), (680.0 * formant_shift, 3.2, 0.72)],
            ) * 0.65
            wing_air = _swept_resonant_filter(pink, rng.uniform(340.0, 620.0), rng.uniform(1100.0, 1950.0), q=2.4) * wing_mod * 0.52
            env = np.pad(adsr_envelope(len(rt), 0.12, 0.22, 0.56, 0.34), (hum_start, 0))
            return _cavern_reverb(_sos_highpass((gourd + throat) * env + wing_air * env, 110.0), rng, wet=0.16, rt60=0.38, lp_hz=5200.0)
        if action == "hurt":
            # Spongy fungal-husk thump + tearing hyphae + pressurized spore cough (wide per-take onset/pitch)
            hit_s = int(rng.uniform(0.01, 0.14) * n)
            rt = t[hit_s:] - t[hit_s]
            husk = np.zeros(n, dtype=np.float64)
            husk[hit_s:] = fungal_cap_resonance(len(rt), bf * rng.uniform(0.55, 0.90), rng, decay=0.12, sponginess=0.78) * 1.05
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * rng.uniform(0.65, 1.05), count=4, wetness=0.65) * 0.62
            slope = rng.uniform(0.48, 0.88)
            f_cough = np.maximum(85.0, bf * rng.uniform(0.52, 0.78) * (1.35 - slope * (rt / max(0.08, dur))))
            cough = np.zeros(n, dtype=np.float64)
            cough[hit_s:] = _glottal_sub_source(rt, f_cough, rng, harmonics=8, sub_weight=0.65, rattle_hz=26.0) * exp_decay(rt, dur * 0.42) * 0.68
            return _cavern_reverb(_sos_highpass(husk + hyphae + cough, 110.0), rng, wet=0.16, rt60=0.36, lp_hz=5400.0)
        if action == "death":
            # Deflating hollow fungal-sac groan + rupturing spore puff + settling hyphae
            f_down = np.maximum(48.0, bf * 0.68 * np.exp(-1.75 * t / dur))
            deflate = _glottal_sub_source(t, f_down, rng, harmonics=9, sub_weight=0.78, rattle_hz=18.0) * adsr_envelope(n, 0.02, 0.22, 0.46, 0.44) * 0.80
            sac = spore_sac_burst(n, rng, base_freq=bf * 0.62, pressure=0.82, duration_frac=0.78) * 0.78
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.70, count=5, wetness=0.60) * 0.58
            return _cavern_reverb(_sos_highpass(deflate + sac + hyphae, 90.0), rng, wet=0.20, rt60=0.45, lp_hz=4800.0)
        # purifier_drone_fire (pressurized organic fungal spore-dart blowgun eject; varied onset/pitch)
        hit_s = int(rng.uniform(0.01, 0.14) * n)
        rt = t[hit_s:] - t[hit_s]
        rem = len(rt)
        pop_d = np.zeros(n, dtype=np.float64)
        pop_d[hit_s:] = spore_sac_burst(rem, rng, base_freq=bf * rng.uniform(0.45, 0.72), pressure=0.92, duration_frac=0.85) * 0.92
        cap_knock = np.zeros(n, dtype=np.float64)
        cap_knock[hit_s:] = fungal_cap_resonance(rem, bf * rng.uniform(0.75, 1.15), rng, decay=0.075, sponginess=0.68) * 0.80
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * rng.uniform(0.75, 1.15), count=3, wetness=0.50) * 0.45
        return _cavern_reverb(_sos_highpass(pop_d + cap_knock + hyphae, 120.0), rng, wet=0.14, rt60=0.30, lp_hz=5800.0)

    # =========================================================================
    # 13b. COLONY SYMBIONT — Symbiotic fungal-root worker & guardian caste
    # =========================================================================
    if sound_id.startswith("colony_symbiont_"):
        action = sound_id.removeprefix("colony_symbiont_")
        if action == "ambient":
            dir_s = float(rng.choice((-1.0, 1.0)))
            s1 = int(rng.uniform(0.01, 0.14) * n)
            rt = t[s1:] - t[s1]
            f_warble = bf * rng.uniform(0.86, 1.14) * (1.0 + dir_s * 0.30 * np.sin(np.pi * rt / max(0.08, dur)))
            chirp = np.zeros(n, dtype=np.float64)
            chirp[s1:] = (
                np.sin(2.0 * np.pi * np.cumsum(f_warble) / SAMPLE_RATE + rng.uniform(0, 6.28)) * 0.56
                + _fm_tone(rt, f_warble * 1.45, mf * rng.uniform(0.9, 1.5), 1.2, rng.uniform(0, 6.28)) * 0.38
            ) * adsr_envelope(len(rt), 0.08, 0.22, 0.52, 0.36)
            cap = fungal_cap_resonance(n, bf * 0.78, rng, decay=0.14, sponginess=0.70) * 0.62
            clicks = _chitin_modal_hits(n, rng, bf * rng.uniform(1.8, 2.8), int(rng.integers(2, 4)), 0.018, spread_frac=0.65) * 0.48
            return _cavern_reverb(_sos_highpass(chirp + cap + clicks, 160.0), rng, wet=0.14, rt60=0.32, lp_hz=6000.0)
        if action == "work":
            wave = np.zeros(n, dtype=np.float64)
            for idx, pos in enumerate(_bounce_train(n, rng, 3, start_frac=rng.uniform(0.01, 0.10), end_frac=0.72, accelerate=(rng.random() > 0.5))):
                slen = min(n - pos, int(SAMPLE_RATE * 0.07))
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                f_tap = bf * rng.uniform(0.85, 1.65) * (1.0 + 0.12 * idx)
                wave[pos : pos + slen] += fungal_cap_resonance(slen, f_tap, rng, decay=0.05, sponginess=0.68) * 0.85 + _modal_bank(st, f_tap, (1.0, 1.72, 2.95), (0.030, 0.018, 0.010), (0.65, 0.40, 0.22), rng) * 0.55
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.85, count=4, wetness=0.58) * 0.48
            return _cavern_reverb(_sos_highpass(wave + hyphae, 150.0), rng, wet=0.13, rt60=0.28, lp_hz=6200.0)
        # colony_symbiont_promote
        wave = np.zeros(n, dtype=np.float64)
        jit0 = rng.uniform(0.01, 0.06)
        for idx, ratio in enumerate((1.0, 1.25, 1.498, 2.0)):
            pos = min(n - 64, int((jit0 + idx * rng.uniform(0.11, 0.15)) * n))
            nt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            wave[pos:] += fungal_cap_resonance(len(nt), bf * ratio * 0.85, rng, decay=0.20, sponginess=0.62) * 0.65 + _modal_bank(nt, bf * ratio * rng.uniform(0.97, 1.03), (1.0, 1.50, 2.42), (0.22, 0.16, 0.10), (0.68, 0.44, 0.22), rng) * (0.68 + 0.08 * idx)
        sac = spore_sac_burst(n, rng, base_freq=bf * 0.90, pressure=0.60, duration_frac=0.70) * 0.46
        return _cavern_reverb(np.tanh((wave + sac) * 1.15), rng, wet=0.25, rt60=0.56, lp_hz=5400.0)

    # =========================================================================
    # 14. MYCELIAL CHIMERA — Monstrous fungal-apex companion
    # =========================================================================
    if sound_id.startswith("mycelial_chimera_") or sound_id == "chimera_roar":
        if sound_id == "mycelial_chimera_growl":
            f_g = bf * (1.00 - 0.18 * (t / dur) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit))
            beast = _glottal_sub_source(t, f_g, rng, harmonics=11, sub_weight=0.86, rattle_hz=23.0)
            tract = _apply_formant_bank(beast, [(150.0 * formant_shift, 2.4, 1.15), (395.0 * formant_shift, 3.0, 0.92), (1050.0, 3.5, 0.50)])
            cap = fungal_cap_resonance(n, bf * 1.15, rng, decay=0.24, sponginess=0.68) * 0.55
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.4, count=3, wetness=0.45) * 0.35
            raw = np.tanh((tract + cap + hyphae) * 1.32) * adsr_envelope(n, 0.07, 0.22, 0.58, 0.30)
            return _cavern_reverb(raw, rng, wet=0.20, rt60=0.44, lp_hz=3400.0)
        if sound_id == "mycelial_chimera_hurt":
            f_b = bf * (1.30 - 0.55 * (t / dur))
            bark = _apply_formant_bank(_glottal_sub_source(t, f_b, rng, harmonics=11, sub_weight=0.80, rattle_hz=27.0), [(195.0 * formant_shift, 2.6, 1.18), (510.0 * formant_shift, 3.2, 0.85)])
            cap = fungal_cap_resonance(n, bf * 1.25, rng, decay=0.12, sponginess=0.72) * 0.72
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.5, count=4, wetness=0.55) * 0.50
            return _cavern_reverb(np.tanh((bark * adsr_envelope(n, 0.02, 0.2, 0.45, 0.36) + cap + hyphae) * 1.3), rng, wet=0.19, rt60=0.40, lp_hz=3800.0)
        if sound_id == "mycelial_chimera_death":
            f_down = np.maximum(38.0, bf * 1.20 * np.exp(-1.25 * t / dur))
            howl = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.92, rattle_hz=19.0), [(148.0 * formant_shift, 2.5, 1.18), (385.0, 3.0, 0.85)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.3, count=6, wetness=0.58) * 0.52
            thud_s = int(0.48 * n)
            thud = np.zeros(n, dtype=np.float64)
            tt = np.arange(n - thud_s, dtype=np.float64) / SAMPLE_RATE
            thud[thud_s:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, 115.0 * exp_decay(tt, 0.04))) / SAMPLE_RATE) * exp_decay(tt, 0.24) * 1.05
            raw = np.tanh(((howl + hyphae) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + thud) * 1.3)
            return _cavern_reverb(raw, rng, wet=0.25, rt60=0.56, lp_hz=3200.0)
        if sound_id == "chimera_roar":
            f_r = bf * (0.92 + 0.22 * np.sin(np.pi * t / dur) - 0.14 * (t / dur))
            roar_src = _glottal_sub_source(t, f_r, rng, harmonics=13, sub_weight=0.92, rattle_hz=22.0)
            tract = _apply_formant_bank(roar_src, [(162.0 * formant_shift, 2.4, 1.22), (435.0 * formant_shift, 3.0, 0.95), (1180.0, 3.6, 0.58)])
            cap_boom = fungal_cap_resonance(n, bf * 1.25, rng, decay=0.28, sponginess=0.70) * 0.65
            chord = (np.sin(2.0 * np.pi * np.cumsum(f_r * 1.5) / SAMPLE_RATE + pjit) * 0.45 + np.sin(2.0 * np.pi * np.cumsum(f_r * 2.0) / SAMPLE_RATE) * 0.26)
            raw = np.tanh((tract + cap_boom + chord) * 1.38) * adsr_envelope(n, 0.08, 0.20, 0.60, 0.32)
            return _cavern_reverb(raw, rng, wet=0.27, rt60=0.60, lp_hz=3600.0)
        # mycelial_chimera_strike
        slash = _swept_resonant_filter(pink * 0.7 + white * 0.45, 4200.0, 340.0, q=2.2) * exp_decay(t, dur * 0.42) * 1.05
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.35, count=5, wetness=0.52) * 0.62
        cap = fungal_cap_resonance(n, bf * 1.15, rng, decay=0.11, sponginess=0.65) * 0.65
        return _cavern_reverb(np.tanh((slash + hyphae + cap) * 1.3), rng, wet=0.17, rt60=0.36, lp_hz=5200.0)

    # =========================================================================
    # 14b. SPORE GLIDER & ROOT KEEPER — Sinkhole Vertical Fungal Fauna
    # =========================================================================
    if sound_id.startswith("spore_glider_"):
        action = sound_id.removeprefix("spore_glider_")
        if action == "ambient":
            s0 = int(rng.uniform(0.01, 0.06) * n)
            rt = t[s0:] - t[s0]
            flutter_hz = rng.uniform(14.0, 24.0)
            wing = _swept_resonant_filter(pink[s0:], rng.uniform(460.0, 780.0), rng.uniform(1400.0, 2500.0), q=2.8)
            wing *= (0.68 + 0.32 * np.sin(2.0 * np.pi * flutter_hz * rt + pjit)) * 1.10
            cap = fungal_cap_resonance(len(rt), bf * rng.uniform(0.75, 0.98), rng, decay=0.20, sponginess=0.72) * 0.72
            sac = spore_sac_burst(len(rt), rng, base_freq=bf * 0.85, pressure=0.45, duration_frac=0.75) * 0.35
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = (wing * 0.55 + cap + sac) * adsr_envelope(len(rt), 0.08, 0.22, 0.50, 0.36)
            return _cavern_reverb(_sos_highpass(wave, 160.0), rng, wet=0.22, rt60=0.45, lp_hz=6400.0)
        if action == "hurt":
            s0 = int(rng.uniform(0.0, 0.08) * n)
            rt = t[s0:] - t[s0]
            f_up = bf * rng.uniform(1.04, 1.24) * np.exp(-1.1 * rt / max(0.08, dur))
            squeak = _fm_tone(rt, f_up, mf * rng.uniform(0.9, 1.4), 1.6, pjit)
            cap = fungal_cap_resonance(len(rt), bf * 0.72, rng, decay=0.09, sponginess=0.78) * 0.72
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = (squeak * 0.75 + cap) * adsr_envelope(len(rt), 0.02, 0.16, 0.40, 0.35)
            return _cavern_reverb(_sos_highpass(wave, 180.0), rng, wet=0.15, rt60=0.32, lp_hz=6800.0)
        f_down = np.maximum(125.0, bf * 1.12 * np.exp(-1.35 * t / dur))
        sigh = _fm_tone(t, f_down, mf, 1.2, pjit) * 0.62 + spore_sac_burst(n, rng, base_freq=bf * 0.68, pressure=0.65, duration_frac=0.75) * 0.62
        return _cavern_reverb(_sos_highpass(sigh * adsr_envelope(n, 0.03, 0.20, 0.46, 0.44), 140.0), rng, wet=0.24, rt60=0.52, lp_hz=6000.0)

    if sound_id.startswith("root_keeper_"):
        action = sound_id.removeprefix("root_keeper_")
        if action == "ambient":
            s0 = int(rng.uniform(0.01, 0.15) * n)
            rt = t[s0:] - t[s0]
            f_g = bf * rng.uniform(0.90, 1.12) * (1.0 - 0.18 * (rt / max(0.1, dur)) + 0.06 * np.sin(2.0 * np.pi * mf * rt + pjit))
            src = _glottal_sub_source(rt, f_g, rng, harmonics=12, sub_weight=0.90, rattle_hz=rng.uniform(15.0, 25.0))
            tract = _apply_formant_bank(src, [(125.0 * formant_shift, 2.3, 1.22), (325.0 * formant_shift, 2.9, 0.85)])
            cap = fungal_cap_resonance(len(rt), bf * 1.6, rng, decay=0.24, sponginess=0.68) * 0.68
            hyphae = mycelial_hyphae_tear(len(rt), rng, base_freq=bf * 2.0, count=4, wetness=0.55) * 0.45
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = np.tanh((tract + cap + hyphae) * 1.32) * adsr_envelope(len(rt), 0.10, 0.24, 0.56, 0.34)
            return _cavern_reverb(wave, rng, wet=0.25, rt60=0.60, lp_hz=2800.0)
        if action == "hurt":
            s0 = int(rng.uniform(0.01, 0.17) * n)
            rt = t[s0:] - t[s0]
            slope = float(rng.choice((-0.42, 0.36)))
            f_b = bf * rng.uniform(0.84, 1.24) * (1.15 + slope * (rt / max(0.08, dur)))
            bark = _apply_formant_bank(
                _glottal_sub_source(rt, f_b, rng, harmonics=10, sub_weight=0.85, rattle_hz=rng.uniform(18.0, 36.0)),
                [(rng.uniform(140.0, 220.0) * formant_shift, 2.5, 1.18), (rng.uniform(380.0, 540.0) * formant_shift, 3.0, 0.85)],
            )
            hyphae = mycelial_hyphae_tear(len(rt), rng, base_freq=bf * rng.uniform(1.8, 3.0), count=5, wetness=0.58) * 0.65
            cap_snap = fungal_cap_resonance(len(rt), bf * rng.uniform(1.3, 1.9), rng, decay=0.14, sponginess=0.70) * 0.65
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = np.tanh((bark * adsr_envelope(len(rt), 0.02, 0.18, 0.42, 0.36) + hyphae + cap_snap) * 1.3)
            return _cavern_reverb(wave, rng, wet=0.20, rt60=0.42, lp_hz=3600.0)
        if action == "death":
            f_d = np.maximum(34.0, bf * 1.18 * np.exp(-1.28 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_d, rng, harmonics=12, sub_weight=0.92, rattle_hz=17.0), [(132.0 * formant_shift, 2.4, 1.22), (345.0, 2.9, 0.82)])
            hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.8, count=7, wetness=0.58) * 0.58
            thud_s = int(0.52 * n)
            thud = np.zeros(n, dtype=np.float64)
            tt = np.arange(n - thud_s, dtype=np.float64) / SAMPLE_RATE
            thud[thud_s:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, 105.0 * exp_decay(tt, 0.045))) / SAMPLE_RATE) * exp_decay(tt, 0.24) * 1.05
            raw = np.tanh((groan * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + hyphae + thud) * 1.32)
            return _cavern_reverb(raw, rng, wet=0.27, rt60=0.64, lp_hz=3000.0)
        # root_keeper_slam
        s0 = int(rng.uniform(0.01, 0.07) * n)
        rt = t[s0:] - t[s0]
        f_sub = np.maximum(32.0, bf * rng.uniform(0.9, 1.1) * (0.5 + 1.6 * np.exp(-rt / 0.036)))
        sub = np.sin(2.0 * np.pi * np.cumsum(f_sub) / SAMPLE_RATE + pjit) * exp_decay(rt, dur * 0.42) * 1.15
        roots = mycelial_hyphae_tear(len(rt), rng, base_freq=bf * 2.1, count=6, wetness=0.55) * 0.72
        cap = fungal_cap_resonance(len(rt), bf * 1.5, rng, decay=0.18, sponginess=0.70) * 0.72
        grit = _sos_bandpass(pink[s0:], 140.0, 2400.0) * exp_decay(rt, dur * 0.34) * 0.68
        wave = np.zeros(n, dtype=np.float64)
        wave[s0:] = np.tanh((sub + roots + cap + grit) * 1.35)
        return _cavern_reverb(wave, rng, wet=0.23, rt60=0.52, lp_hz=3000.0)

    # =========================================================================
    # 15. ANATOMICAL STEP PROFILES (4) — Fungal-Mycelial Surface Response
    # =========================================================================
    if sound_id == "mob_step_chitin":
        wave = np.zeros(n, dtype=np.float64)
        for idx, pos in enumerate((0, min(n - 48, int(SAMPLE_RATE * rng.uniform(0.028, 0.052))))):
            slen = n - pos
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            g = 1.0 if idx == 0 else rng.uniform(0.60, 0.85)
            wave[pos:] += _modal_bank(st, bf * rng.uniform(0.95, 1.25), (1.0, 2.55, 4.95), (0.022, 0.014, 0.008), (0.64, 0.44, 0.24), rng) * g
            wave[pos:] += fungal_cap_resonance(slen, bf * 0.72, rng, decay=0.045, sponginess=0.70) * 0.42 * g
            wave[pos:] += _sos_bandpass(white[pos:], 1500.0, 7200.0) * exp_decay(st, 0.009) * 0.38 * g
        return _cavern_reverb(_sos_highpass(wave, 180.0), rng, wet=0.11, rt60=0.24, lp_hz=6000.0)

    if sound_id == "mob_step_arachnid":
        wave = np.zeros(n, dtype=np.float64)
        leg_count = int(rng.integers(3, 5))
        for idx, pos in enumerate(_bounce_train(n, rng, leg_count, start_frac=0.01, end_frac=0.78, accelerate=False)):
            slen = min(n - pos, int(SAMPLE_RATE * 0.045))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            f_leg = bf * rng.uniform(1.02, 1.88)
            wave[pos : pos + slen] += _modal_bank(st, f_leg, (1.0, 2.65, 5.10), (0.018, 0.011, 0.006), (0.68, 0.46, 0.25), rng) * (0.92 ** idx)
            wave[pos : pos + slen] += _sos_bandpass(rng.normal(0, 1, slen), 2200.0, 9600.0) * exp_decay(st, 0.006) * 0.38
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.85, count=3, wetness=0.45) * 0.40
        return _cavern_reverb(_sos_highpass(wave + hyphae, 210.0), rng, wet=0.13, rt60=0.28, lp_hz=6800.0)

    if sound_id == "mob_step_heavy":
        stomp_s = int(rng.uniform(0.01, 0.21) * n)
        rt = t[stomp_s:] - t[stomp_s]
        tau_drop = rng.uniform(0.018, 0.048)
        f_end = rng.uniform(28.0, 58.0)
        f_drop = np.maximum(f_end, bf * rng.uniform(0.84, 1.18) * (0.45 + rng.uniform(1.1, 1.75) * np.exp(-rt / tau_drop)))
        sub = np.zeros(n, dtype=np.float64)
        sub_tau = dur * rng.uniform(0.24, 0.56)
        sub[stomp_s:] = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + rng.uniform(0, 6.28)) * np.exp(-rt / sub_tau) * 1.05
        cap_stomp = np.zeros(n, dtype=np.float64)
        cap_stomp[stomp_s:] = fungal_cap_resonance(len(rt), bf * rng.uniform(1.3, 2.0), rng, decay=0.09, sponginess=0.78) * 0.72
        squish = _wet_squelch_layer(n, rng, bf * 2.2, int(rng.integers(2, 4)), intensity=0.55) * 0.55
        grit = _sos_bandpass(pink, 140.0, 2700.0) * np.pad(np.exp(-rt / (dur * 0.30)), (stomp_s, 0)) * 0.65
        return _cavern_reverb(np.tanh((sub + cap_stomp + squish + grit) * 1.28), rng, wet=0.17, rt60=0.38, lp_hz=2600.0)

    if sound_id == "mob_step_hover":
        # Organic flapping leathery spore-wing / hollow fungal-sac hover swoosh (wide per-take variation)
        dir_sign = float(rng.choice((-1.0, 1.0)))
        u_shape = (t / max(0.05, dur)) ** rng.uniform(0.55, 1.65)
        depth = rng.uniform(0.20, 0.42) * dir_sign
        f_pulse = bf * rng.uniform(0.75, 1.12) * (1.0 + depth * np.sin(np.pi * u_shape + rng.uniform(0.0, 1.4)))
        wing_beat = rng.uniform(16.0, 32.0)
        shift = int(rng.uniform(0.0, 0.19) * n)
        rem = n - shift
        hum = np.zeros(n, dtype=np.float64)
        hum[shift:] = fungal_cap_resonance(rem, float(np.mean(f_pulse)) * 0.75, rng, decay=0.12, sponginess=0.76) * (0.72 + 0.28 * np.sin(2.0 * np.pi * wing_beat * (t[shift:] - t[shift])))
        f_air_a = rng.uniform(320.0, 740.0)
        f_air_b = rng.uniform(1350.0, 2800.0)
        air = _swept_resonant_filter(pink, f_air_a if dir_sign > 0 else f_air_b, f_air_b if dir_sign > 0 else f_air_a, q=2.5) * 0.85
        puff = spore_sac_burst(n, rng, base_freq=bf * 0.55, pressure=0.52, duration_frac=0.70) * 0.45
        env_h = np.pad(adsr_envelope(rem, rng.uniform(0.08, 0.22), 0.22, 0.44, 0.32), (shift, 0))
        return _cavern_reverb((hum + air * env_h + puff * env_h), rng, wet=0.14, rt60=0.32, lp_hz=5600.0)

    raise ValueError(f"Unhandled entity sound_id in entity_synth: {sound_id}")
