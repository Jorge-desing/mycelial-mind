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
    _sos_lowpass,
    _swept_resonant_filter,
    _wet_squelch_layer,
    adsr_envelope,
    exp_decay,
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
    # 1. SPORELING — Hyper-agile insectoid scout (mandible stridulation + high trachea)
    # =========================================================================
    if sound_id.startswith("sporeling_"):
        action = sound_id.split("_", 1)[1]
        if action == "chitter":
            # Rapid mandible stridulation bursts + high insectoid tracheal chirps
            wave = np.zeros(n, dtype=np.float64)
            bursts = int(rng.integers(5, 8))
            for idx, pos in enumerate(_bounce_train(n, rng, bursts, start_frac=0.03, end_frac=0.86, accelerate=False)):
                blen = min(n - pos, max(64, int(SAMPLE_RATE * rng.uniform(0.038, 0.068))))
                bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
                f_chirp = bf * rng.uniform(0.92, 1.38) * (1.0 + 0.25 * np.sin(2.0 * np.pi * (mf * 1.4) * bt))
                chirp = _fm_tone(bt, f_chirp, f_chirp * 1.5, 2.1, rng.uniform(0, 6.28))
                click = _modal_bank(bt, bf * 2.6, (1.0, 2.14, 3.85), (0.015, 0.010, 0.006), (0.7, 0.45, 0.25), rng)
                wave[pos : pos + blen] += (chirp * 0.72 + click * 0.65) * np.sin(np.linspace(0, np.pi, blen))
            tract = _apply_formant_bank(wave, [(bf * 1.35 * formant_shift, 3.8, 1.0), (bf * 2.9 * formant_shift, 4.5, 0.85)])
            return _cavern_reverb(_sos_highpass(tract, 240.0), rng, wet=0.12, rt60=0.28, lp_hz=7200.0)
        if action == "hurt":
            # Sharp chitin shell crack + high-pitched downward insectoid squeal
            snap = _modal_bank(t, bf * 2.2, (1.0, 2.45, 4.12), (0.025, 0.016, 0.010), (0.8, 0.55, 0.35), rng)
            f_sq = bf * (1.45 - 0.82 * (t / dur))
            squeal = _fm_tone(t, f_sq, mf * 3.2, 2.4, pjit) * adsr_envelope(n, 0.02, 0.20, 0.40, 0.40)
            tract = _apply_formant_bank(squeal, [(bf * 1.6 * formant_shift, 3.5, 1.1), (bf * 3.2 * formant_shift, 4.2, 0.75)])
            return _cavern_reverb(_sos_highpass(snap * 0.85 + tract, 220.0), rng, wet=0.14, rt60=0.30, lp_hz=6800.0)
        if action == "death":
            # Desperate fluttering mandible rattle + collapsing tracheal whistle + tiny husk drop
            f_down = np.maximum(140.0, bf * 1.25 * np.exp(-1.55 * t / dur))
            whistle = _fm_tone(t, f_down, mf * 2.5, 2.2, pjit) * (0.6 + 0.4 * np.sin(2.0 * np.pi * 28.0 * t))
            rattle = _chitin_modal_hits(n, rng, bf * 1.8, 7, 0.028, spread_frac=0.78) * 0.85
            raw = (whistle * adsr_envelope(n, 0.02, 0.22, 0.38, 0.45) + rattle)
            return _cavern_reverb(_sos_highpass(raw, 200.0), rng, wet=0.16, rt60=0.36, lp_hz=6400.0)
        # sporeling_burst
        creak_n = min(n // 5, int(SAMPLE_RATE * 0.036))
        wave = np.zeros(n, dtype=np.float64)
        if creak_n > 16:
            ct = np.arange(creak_n, dtype=np.float64) / SAMPLE_RATE
            wave[:creak_n] = np.sin(2.0 * np.pi * np.linspace(bf * 0.8, bf * 1.9, creak_n) * ct) * 0.55
        pop = np.sin(2.0 * np.pi * np.cumsum(np.maximum(80.0, bf * (0.4 + 1.8 * exp_decay(t, 0.025)))) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.35)
        hiss = _sos_bandpass(pink * 0.6 + white * 0.5, 1200.0, 8200.0) * adsr_envelope(n, 0.04, 0.25, 0.35, 0.42) * 0.85
        return _cavern_reverb(wave + pop * 1.1 + hiss, rng, wet=0.16, rt60=0.35, lp_hz=5800.0)

    # =========================================================================
    # 2. CHITIN BRUTE — Heavy armored frontline shock-trooper
    # =========================================================================
    if sound_id.startswith("chitin_brute_"):
        action = sound_id.split("chitin_brute_", 1)[1]
        plate_ratios = (1.0, 2.38, 4.15, 6.40)
        if action == "growl":
            # Deep armored chest rumble + scraping chitin pauldrons
            f_curve = bf * (1.08 - 0.26 * (t / dur) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit))
            glottal = _glottal_sub_source(t, f_curve, rng, harmonics=12, sub_weight=0.92, rattle_hz=21.0)
            chest = _apply_formant_bank(glottal, [(115.0 * formant_shift, 2.4, 1.2), (285.0 * formant_shift, 2.9, 0.9), (740.0, 3.5, 0.45)])
            plate_clack = _modal_bank(t, bf * 3.6, plate_ratios, (0.06, 0.04, 0.025, 0.015), (0.6, 0.42, 0.26, 0.14), rng) * 0.50
            raw = np.tanh((chest * 1.35 + plate_clack) * 1.35) * adsr_envelope(n, 0.06, 0.22, 0.58, 0.30)
            return _cavern_reverb(raw, rng, wet=0.22, rt60=0.48, lp_hz=2600.0)
        if action == "hurt":
            # Heavy blunt carapace plate impact + short guttural grunt
            armor_hit = _modal_bank(t, bf * 3.1, plate_ratios, (0.055, 0.038, 0.024, 0.014), (0.85, 0.62, 0.40, 0.22), rng) * 1.05
            f_gr = bf * (1.25 - 0.50 * (t / dur))
            grunt = _apply_formant_bank(_glottal_sub_source(t, f_gr, rng, harmonics=10, sub_weight=0.85, rattle_hz=24.0), [(135.0 * formant_shift, 2.5, 1.15), (340.0 * formant_shift, 3.0, 0.8)])
            raw = np.tanh((armor_hit + grunt * adsr_envelope(n, 0.03, 0.22, 0.45, 0.35)) * 1.3)
            return _cavern_reverb(raw, rng, wet=0.20, rt60=0.42, lp_hz=2800.0)
        if action == "death":
            # Bellowing roar dropping into sub-bass + heavy armored body crashing onto stone
            f_down = np.maximum(34.0, bf * 1.15 * np.exp(-1.15 * t / dur))
            roar = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=11, sub_weight=0.95, rattle_hz=18.0), [(110.0 * formant_shift, 2.4, 1.15), (270.0, 2.8, 0.75)])
            crash_idx = int(rng.uniform(0.42, 0.54) * n)
            crash = np.zeros(n, dtype=np.float64)
            if n - crash_idx > 48:
                ct = np.arange(n - crash_idx, dtype=np.float64) / SAMPLE_RATE
                sub_thud = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, 125.0 * exp_decay(ct, 0.045))) / SAMPLE_RATE) * exp_decay(ct, 0.22) * 1.25
                plates = _modal_bank(ct, bf * 2.8, plate_ratios, (0.09, 0.06, 0.04, 0.025), (0.8, 0.55, 0.35, 0.20), rng) * 0.85
                crash[crash_idx:] = sub_thud + plates
            raw = np.tanh((roar * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + crash) * 1.35)
            return _cavern_reverb(raw, rng, wet=0.26, rt60=0.58, lp_hz=2500.0)
        # chitin_brute_smash
        f_drop = np.maximum(28.0, bf * (0.38 + 2.4 * exp_decay(t, 0.045)))
        sub = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.48) * 1.3
        armor_slam = _modal_bank(t, bf * 3.4, plate_ratios, (0.08, 0.055, 0.036, 0.022), (0.9, 0.65, 0.45, 0.25), rng) * 0.95
        debris = _sos_bandpass(brown * 0.8 + pink * 0.5, 45.0, 1100.0) * exp_decay(t, dur * 0.36) * 0.85
        return _cavern_reverb(np.tanh((sub + armor_slam + debris) * 1.4), rng, wet=0.25, rt60=0.52, lp_hz=2400.0)

    # =========================================================================
    # 3. MYCELIAL STALKER — Razor-clawed stealth ambush predator
    # =========================================================================
    if sound_id.startswith("stalker_"):
        action = sound_id.split("stalker_", 1)[1]
        if action == "hiss":
            # Bifurcated slit-throat aspirate hiss + subtle metallic claw click
            aspirate = _swept_resonant_filter(pink * 0.65 + white * 0.55, bf * 4.5, bf * 1.2, q=3.2)
            sibilant = _sos_bandpass(white, 2600.0, 11800.0) * (0.45 + 0.55 * np.sin(2.0 * np.pi * mf * t + pjit)) * 0.55
            throat = _glottal_sub_source(t, np.linspace(bf * 0.38, bf * 0.26, n), rng, harmonics=6, sub_weight=0.4, rattle_hz=34.0) * 0.28
            claw_pos = min(n - 64, int(rng.uniform(0.18, 0.42) * n))
            claw = np.zeros(n, dtype=np.float64)
            ct = np.arange(n - claw_pos, dtype=np.float64) / SAMPLE_RATE
            claw[claw_pos:] = _modal_bank(ct, bf * 3.8, (1.0, 2.71, 4.95), (0.024, 0.015, 0.009), (0.65, 0.45, 0.25), rng) * 0.55
            raw = (aspirate * 1.15 + sibilant + throat) * adsr_envelope(n, 0.05, 0.22, 0.48, 0.32) + claw
            return _cavern_reverb(raw, rng, wet=0.16, rt60=0.38, lp_hz=7200.0)
        if action == "hurt":
            # Razor rasp shriek + chitin snap
            f_sh = bf * (1.38 - 0.62 * (t / dur))
            shriek = _fm_tone(t, f_sh, f_sh * 0.72, 2.6, pjit) * 0.75
            rasp = _swept_resonant_filter(white * 0.6 + pink * 0.5, 5800.0, 1400.0, q=2.6) * 0.70
            snap = _modal_bank(t, bf * 2.9, (1.0, 2.6, 4.8), (0.03, 0.018, 0.010), (0.75, 0.5, 0.3), rng) * 0.65
            raw = (shriek + rasp + snap) * adsr_envelope(n, 0.015, 0.20, 0.42, 0.38)
            return _cavern_reverb(raw, rng, wet=0.15, rt60=0.34, lp_hz=6800.0)
        if action == "death":
            # Strangled predator exhale + rapid claw spasm + collapse
            exhale = _swept_resonant_filter(pink * 0.7 + white * 0.5, 4800.0, 420.0, q=2.4) * adsr_envelope(n, 0.03, 0.25, 0.40, 0.42)
            f_down = np.maximum(85.0, bf * np.exp(-1.5 * t / dur))
            raspy_wail = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=9, sub_weight=0.5, rattle_hz=32.0), [(bf * 1.2 * formant_shift, 3.0, 1.0), (bf * 2.8, 3.8, 0.75)])
            spasm = _chitin_modal_hits(n, rng, bf * 2.4, 5, 0.032, spread_frac=0.68) * 0.65
            return _cavern_reverb(exhale * 0.9 + raspy_wail * 0.75 + spasm, rng, wet=0.18, rt60=0.42, lp_hz=5600.0)
        # stalker_pounce
        whoosh = _swept_resonant_filter(pink * 0.7 + white * 0.5, 950.0, 6800.0, q=2.2) * np.sin(np.pi * t / dur) * 1.15
        slash = _modal_bank(t, bf * 3.5, (1.0, 2.15, 3.92, 5.80), (0.045, 0.030, 0.018, 0.010), (0.8, 0.55, 0.35, 0.2), rng, glide_factor=-0.35, glide_tau=0.04)
        snarl = _sos_bandpass(white, 2200.0, 9600.0) * exp_decay(t, dur * 0.32) * 0.55
        return _cavern_reverb(np.tanh((whoosh + slash * 0.95 + snarl) * 1.3), rng, wet=0.14, rt60=0.32, lp_hz=7000.0)

    # =========================================================================
    # 4. ACID SPITTER — Peristaltic corrosive bio-sac artillery
    # =========================================================================
    if sound_id.startswith("acid_spitter_"):
        action = sound_id.split("acid_spitter_", 1)[1]
        if action == "gurgle":
            # Peristaltic stomach churn + bubbling acid sac + wet valve gulp
            churn_f = bf * (0.75 + 0.35 * np.sin(2.0 * np.pi * (mf * 0.45) * t + pjit))
            sac = _apply_formant_bank(_glottal_sub_source(t, churn_f, rng, harmonics=7, sub_weight=0.65, rattle_hz=19.0), [(220.0 * formant_shift, 3.2, 1.1), (580.0 * formant_shift, 3.8, 0.85)])
            bubbles = _wet_squelch_layer(n, rng, bf * 1.25, int(rng.integers(9, 15)), intensity=1.25)
            return _cavern_reverb(np.tanh((sac * 0.75 + bubbles * 1.1) * 1.25) * adsr_envelope(n, 0.06, 0.20, 0.58, 0.28), rng, wet=0.16, rt60=0.38, lp_hz=3800.0)
        if action == "hurt":
            # Wet pressurized sac slap + acid splash hiss + gargled croak
            slap = _swept_resonant_filter(pink, 1400.0, 320.0, q=3.0) * exp_decay(t, 0.065) * 1.05
            splash = _wet_squelch_layer(n, rng, bf * 1.4, 5, intensity=1.05) * exp_decay(t, dur * 0.55)
            sizzle = _sos_bandpass(white, 3200.0, 11000.0) * exp_decay(t, dur * 0.40) * 0.45
            return _cavern_reverb(np.tanh((slap + splash + sizzle) * 1.25), rng, wet=0.15, rt60=0.35, lp_hz=4800.0)
        if action == "death":
            # Bloated acid sac rupture -> deflating wet gurgle -> corrosive floor sizzle
            pop = np.sin(2.0 * np.pi * np.cumsum(np.maximum(45.0, bf * (0.45 + 1.9 * exp_decay(t, 0.032)))) / SAMPLE_RATE + pjit) * exp_decay(t, 0.14) * 1.15
            deflate = _wet_squelch_layer(n, rng, bf * 1.1, 11, intensity=1.25) * adsr_envelope(n, 0.04, 0.24, 0.48, 0.42)
            fizz = _sos_bandpass(white, 2800.0, 11500.0) * adsr_envelope(n, 0.15, 0.25, 0.45, 0.40) * 0.55
            return _cavern_reverb(np.tanh((pop + deflate + fizz) * 1.3), rng, wet=0.20, rt60=0.45, lp_hz=4600.0)
        # acid_spitter_spit (jittered sac draw-back onset, pitch arc & corrosive hiss band)
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
        ptoo = (np.sin(phase_spit) + 0.34 * np.sin(1.48 * phase_spit + pjit * 0.7)) * exp_decay(pt, float(rng.uniform(0.065, 0.11))) * 1.15
        hiss_lo = float(rng.uniform(1950.0, 3400.0))
        hiss_hi = float(rng.uniform(8400.0, 12400.0))
        spray = (
            _wet_squelch_layer(len(pt), rng, bf * float(rng.uniform(1.25, 1.85)), int(rng.integers(3, 6)), intensity=0.95)
            + _sos_bandpass(white[draw_n:], hiss_lo, hiss_hi) * 0.58
        ) * exp_decay(pt, dur * float(rng.uniform(0.32, 0.48)))
        wave[draw_n:] = np.tanh((ptoo + spray) * 1.3)
        return _cavern_reverb(wave, rng, wet=0.14, rt60=0.32, lp_hz=5400.0)

    # =========================================================================
    # 5. BURROWER — Subterranean basalt-drilling mandible worm/beetle
    # =========================================================================
    if sound_id.startswith("burrower_"):
        action = sound_id.split("burrower_", 1)[1]
        if action == "grind":
            # Rhythmic rotary mandible grinding against basalt + cascading rock grit
            rotary = 0.35 + 0.65 * np.abs(np.sin(2.0 * np.pi * (mf * 0.55) * t + pjit))
            rock_grind = _apply_formant_bank((pink * 0.7 + white * 0.4) * rotary, [(320.0 * formant_shift, 2.2, 1.1), (1380.0 * formant_shift, 2.6, 0.9), (3200.0, 3.2, 0.55)])
            mandibles = _chitin_modal_hits(n, rng, bf * 2.1, int(rng.integers(6, 10)), 0.038, spread_frac=0.84) * 0.75
            sub_drill = _glottal_sub_source(t, bf * (0.85 + 0.18 * np.sin(2.0 * np.pi * mf * t)), rng, harmonics=6, sub_weight=0.7) * 0.45
            raw = np.tanh((rock_grind * 1.2 + mandibles + sub_drill) * 1.3) * adsr_envelope(n, 0.04, 0.22, 0.55, 0.28)
            return _cavern_reverb(raw, rng, wet=0.22, rt60=0.46, lp_hz=3600.0)
        if action == "hurt":
            # Flinty stone-chitin ricochet + low subterranean groan
            flint = _sos_bandpass(white, 2400.0, 10200.0) * exp_decay(t, 0.022) * 0.95
            shell = _modal_bank(t, bf * 2.5, (1.0, 2.18, 3.75), (0.045, 0.030, 0.018), (0.8, 0.55, 0.32), rng) * 0.85
            groan = _glottal_sub_source(t, bf * (1.15 - 0.45 * (t / dur)), rng, harmonics=8, sub_weight=0.75) * exp_decay(t, dur * 0.45) * 0.65
            return _cavern_reverb(np.tanh((flint + shell + groan) * 1.25), rng, wet=0.18, rt60=0.40, lp_hz=4200.0)
        if action == "death":
            # Stalling drill motor cadence + cracking carapace + falling gravel shower
            f_stall = np.maximum(42.0, bf * 1.2 * np.exp(-1.4 * t / dur))
            drill_down = _glottal_sub_source(t, f_stall, rng, harmonics=9, sub_weight=0.8, rattle_hz=15.0) * adsr_envelope(n, 0.03, 0.22, 0.42, 0.44)
            gravel = np.zeros(n, dtype=np.float64)
            for pos in _bounce_train(n, rng, 10, start_frac=0.12, end_frac=0.88, accelerate=True):
                plen = min(n - pos, int(SAMPLE_RATE * 0.028))
                pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
                gravel[pos : pos + plen] += _sos_bandpass(rng.normal(0.0, 1.0, plen), 1400.0, 6400.0) * exp_decay(pt, 0.010) * 0.75
            return _cavern_reverb(np.tanh((drill_down + gravel) * 1.3), rng, wet=0.24, rt60=0.52, lp_hz=3400.0)
        # burrower_tunnel
        sub = np.sin(2.0 * np.pi * np.cumsum(np.maximum(34.0, bf * (0.45 + 1.6 * exp_decay(t, 0.04)))) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.45) * 1.15
        crush = _apply_formant_bank((brown * 0.7 + pink * 0.6) * adsr_envelope(n, 0.03, 0.25, 0.45, 0.35), [(280.0, 2.0, 1.1), (1250.0, 2.4, 0.85)])
        pebbles = _chitin_modal_hits(n, rng, bf * 3.2, 6, 0.032, spread_frac=0.75) * 0.70
        return _cavern_reverb(np.tanh((sub + crush + pebbles) * 1.35), rng, wet=0.24, rt60=0.50, lp_hz=3000.0)

    # =========================================================================
    # 6. PYRO-SHROOM — Volatile thermal-gas & ember fungal bomb
    # =========================================================================
    if sound_id.startswith("pyro_shroom_"):
        action = sound_id.split("pyro_shroom_", 1)[1]
        if action == "crackle":
            # Internal furnace draft + snapping ember sparks + pressurized gas bubble pops
            furnace = _sos_bandpass(brown * 0.8 + pink * 0.4, 85.0, 480.0) * (0.55 + 0.45 * np.sin(2.0 * np.pi * (mf * 0.4) * t + pjit)) * 0.75
            sparks = np.zeros(n, dtype=np.float64)
            for pos in _bounce_train(n, rng, int(rng.integers(12, 20)), start_frac=0.02, end_frac=0.90, accelerate=False):
                slen = min(n - pos, max(24, int(SAMPLE_RATE * rng.uniform(0.006, 0.022))))
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                sf = bf * rng.uniform(2.2, 6.5)
                sparks[pos : pos + slen] += (_fm_tone(st, sf, sf * 0.4, 1.5) + rng.normal(0, 0.5, slen)) * exp_decay(st, 0.005) * rng.uniform(0.5, 1.1)
            return _cavern_reverb((furnace + sparks * 1.15) * adsr_envelope(n, 0.04, 0.20, 0.56, 0.28), rng, wet=0.14, rt60=0.32, lp_hz=6800.0)
        if action == "hurt":
            # Pressurized steam vent shriek + hollow fungal cap thump + cinder scatter
            thump = np.sin(2.0 * np.pi * np.cumsum(np.maximum(65.0, bf * (0.55 + 1.3 * exp_decay(t, 0.025)))) / SAMPLE_RATE + pjit) * exp_decay(t, 0.075) * 1.0
            steam = _swept_resonant_filter(white, 6200.0, 1800.0, q=3.2) * exp_decay(t, dur * 0.48) * 0.85
            cinders = _micro_arc_zaps(n, rng, 4, lo_hz=2800.0, hi_hz=9500.0, spread=(0.05, 0.65)) * 0.55
            return _cavern_reverb(np.tanh((thump + steam + cinders) * 1.25), rng, wet=0.14, rt60=0.32, lp_hz=6200.0)
        if action == "death":
            # Mini thermal deflagration puff -> collapsing ash hiss -> dying ember crackles
            woof = _sos_bandpass(brown * 0.8 + pink * 0.6, 60.0, 950.0) * exp_decay(t, dur * 0.38) * 1.25
            whistle = _fm_tone(t, np.maximum(90.0, bf * 1.4 * np.exp(-1.6 * t / dur)), mf * 1.5, 1.4, pjit) * exp_decay(t, dur * 0.35) * 0.60
            embers = _micro_arc_zaps(n, rng, 9, lo_hz=2200.0, hi_hz=10500.0, spread=(0.12, 0.88)) * 0.65
            return _cavern_reverb(np.tanh((woof + whistle + embers) * 1.25), rng, wet=0.18, rt60=0.40, lp_hz=5400.0)
        # pyro_shroom_ignite (accelerating thermal fuse!)
        frac = t / dur
        f_rise = bf * (0.65 + 2.15 * (frac ** 1.7))
        kettle = _fm_tone(t, f_rise, f_rise * 0.33, 1.8, pjit) * (frac ** 1.2) * 0.85
        jet = _swept_resonant_filter(white * 0.6 + pink * 0.5, 900.0, 7800.0, q=2.8) * (0.25 + 0.75 * (frac ** 1.3)) * 0.95
        ticks = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 11, start_frac=0.04, end_frac=0.92, accelerate=True):
            tlen = min(n - pos, int(SAMPLE_RATE * 0.016))
            tt = np.arange(tlen, dtype=np.float64) / SAMPLE_RATE
            ticks[pos : pos + tlen] += _sos_bandpass(rng.normal(0, 1, tlen), 2400.0, 9800.0) * exp_decay(tt, 0.004) * 0.8
        return _cavern_reverb(np.tanh((kettle + jet + ticks) * 1.3) * adsr_envelope(n, 0.03, 0.15, 0.75, 0.14), rng, wet=0.14, rt60=0.32, lp_hz=7200.0)

    # =========================================================================
    # 7. SYNAPSE WEAVER — Psionic quartz-harp support caster
    # =========================================================================
    if sound_id.startswith("synapse_weaver_"):
        action = sound_id.split("synapse_weaver_", 1)[1]
        q_ratios = (1.0, 1.414, 2.32, 3.65, 5.12)
        q_amps = (0.58, 0.46, 0.34, 0.22, 0.12)
        if action == "hum":
            # Binaural psionic quartz resonance + subtle neural arc discharge
            lfo = 1.0 + 0.016 * np.sin(2.0 * np.pi * mf * t + pjit)
            harp = np.zeros(n, dtype=np.float64)
            for r, a in zip(q_ratios, q_amps):
                harp += a * np.sin(2.0 * np.pi * np.cumsum(bf * r * lfo) / SAMPLE_RATE + rng.uniform(0, 6.28))
            whisper = _swept_resonant_filter(pink, bf * 0.8, bf * 3.6, q=3.8) * 0.35
            zaps = _micro_arc_zaps(n, rng, 3, lo_hz=3800.0, hi_hz=12000.0, spread=(0.15, 0.80)) * 0.35
            raw = (harp + whisper + zaps) * adsr_envelope(n, 0.09, 0.22, 0.58, 0.34)
            return _cavern_reverb(raw, rng, wet=0.26, rt60=0.58, lp_hz=4800.0)
        if action == "hurt":
            # Shattering crystal chord + discordant frequency bend
            shatter = _modal_bank(t, bf * 1.45, q_ratios, (0.09, 0.07, 0.05, 0.035, 0.02), q_amps, rng, glide_factor=-0.24, glide_tau=0.04)
            zaps = _micro_arc_zaps(n, rng, 4, lo_hz=3200.0, hi_hz=12500.0, spread=(0.0, 0.55)) * 0.65
            return _cavern_reverb(shatter * 1.05 + zaps, rng, wet=0.22, rt60=0.48, lp_hz=5600.0)
        if action == "death":
            # Cascading quartz prism collapse + descending psionic void sigh
            shards = np.zeros(n, dtype=np.float64)
            for idx, pos in enumerate(_bounce_train(n, rng, 7, start_frac=0.02, end_frac=0.78, accelerate=True)):
                slen = n - pos
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                shards[pos:] += _modal_bank(st, bf * rng.uniform(1.1, 2.4), q_ratios, (0.11, 0.08, 0.05, 0.03, 0.02), q_amps, rng) * (0.75 * (0.86 ** idx))
            f_down = np.maximum(55.0, bf * np.exp(-1.35 * t / dur))
            sigh = _fm_tone(t, f_down, f_down * 0.707, 2.2, pjit) * adsr_envelope(n, 0.04, 0.22, 0.45, 0.44) * 0.65
            return _cavern_reverb(shards + sigh, rng, wet=0.28, rt60=0.62, lp_hz=4800.0)
        # synapse_weaver_buff (3-stage ascending psionic promotion weave)
        wave = np.zeros(n, dtype=np.float64)
        for idx, ratio in enumerate((1.0, 1.335, 1.782)):
            pos = int((idx * 0.24) * n)
            slen = n - pos
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            wave[pos:] += _modal_bank(st, bf * ratio, (1.0, 1.5, 2.32, 3.65), (0.22, 0.16, 0.11, 0.07), (0.65, 0.45, 0.30, 0.18), rng)
        swell = _swept_resonant_filter(pink, bf * 0.7, bf * 4.5, q=3.0) * np.sin(np.pi * t / dur) * 0.55
        return _cavern_reverb(wave * 0.9 + swell, rng, wet=0.25, rt60=0.55, lp_hz=5800.0)

    # =========================================================================
    # 8. GOLIATH CARRIER — Bloated brood-sac colossus
    # =========================================================================
    if sound_id.startswith("goliath_carrier_"):
        action = sound_id.split("goliath_carrier_", 1)[1]
        if action == "thud":
            # Heavy visceral belly thud + internal brood-fluid slosh + deep groan
            f_drop = np.maximum(26.0, bf * (0.45 + 1.85 * exp_decay(t, 0.05)))
            belly = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.48) * 1.15
            groan = _apply_formant_bank(_glottal_sub_source(t, np.full(n, bf * 0.92), rng, harmonics=10, sub_weight=0.95, rattle_hz=16.0), [(95.0 * formant_shift, 2.2, 1.1), (240.0 * formant_shift, 2.8, 0.85)]) * adsr_envelope(n, 0.08, 0.22, 0.52, 0.34) * 0.75
            slosh = _wet_squelch_layer(n, rng, bf * 1.8, 6, intensity=0.85) * adsr_envelope(n, 0.10, 0.25, 0.45, 0.35)
            return _cavern_reverb(np.tanh((belly + groan + slosh) * 1.3), rng, wet=0.24, rt60=0.52, lp_hz=2200.0)
        if action == "hurt":
            # Deep rubbery sac impact + wet brood-chamber spasm (varied onset & drop decay per take)
            hit_s = int(rng.uniform(0.01, 0.12) * n)
            t_hit = np.maximum(0.0, t - t[hit_s])
            tau_drop = rng.uniform(0.025, 0.050)
            thump = np.zeros(n, dtype=np.float64)
            f_thump = np.maximum(36.0, bf * (0.5 + rng.uniform(1.3, 1.8) * np.exp(-t_hit[hit_s:] / tau_drop)))
            thump[hit_s:] = np.sin(2.0 * np.pi * np.cumsum(f_thump) / SAMPLE_RATE + rng.uniform(0.0, 6.28)) * np.exp(-t_hit[hit_s:] / 0.12) * 1.15
            grunt_env = adsr_envelope(n - hit_s, 0.03, 0.22, 0.45, 0.36)
            grunt_src = _glottal_sub_source(t[hit_s:], bf * (1.15 - 0.42 * (t[hit_s:] / max(0.1, dur))), rng, harmonics=9, sub_weight=0.9, rattle_hz=rng.uniform(15.0, 23.0))
            grunt = np.zeros(n, dtype=np.float64)
            grunt[hit_s:] = _apply_formant_bank(grunt_src, [(115.0 * formant_shift, 2.4, 1.15), (290.0 * formant_shift, 3.0, 0.75)]) * grunt_env
            wet = _wet_squelch_layer(n, rng, bf * 1.6, int(rng.integers(4, 8)), intensity=0.95) * exp_decay(t, dur * 0.55)
            return _cavern_reverb(np.tanh((thump + grunt + wet) * 1.3), rng, wet=0.22, rt60=0.46, lp_hz=2600.0)
        if action == "death":
            # Massive visceral collapse + multiple brood-sac ruptures
            f_down = np.maximum(28.0, bf * 1.1 * np.exp(-1.1 * t / dur))
            bellow = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=11, sub_weight=0.98, rattle_hz=15.0), [(92.0 * formant_shift, 2.3, 1.2), (235.0, 2.8, 0.8)]) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.44)
            ruptures = _wet_squelch_layer(n, rng, bf * 1.9, 12, intensity=1.3) * adsr_envelope(n, 0.12, 0.25, 0.50, 0.38)
            slam_idx = int(0.38 * n)
            slam = np.zeros(n, dtype=np.float64)
            st = np.arange(n - slam_idx, dtype=np.float64) / SAMPLE_RATE
            slam[slam_idx:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(26.0, 110.0 * exp_decay(st, 0.05))) / SAMPLE_RATE) * exp_decay(st, 0.28) * 1.25
            return _cavern_reverb(np.tanh((bellow + ruptures + slam) * 1.35), rng, wet=0.28, rt60=0.62, lp_hz=2300.0)
        # goliath_carrier_spawn (membrane tear -> wet amniotic burst -> sporeling chitter emergence)
        tear = _swept_resonant_filter(pink, 220.0, 2400.0, q=2.8) * exp_decay(t, dur * 0.42) * 0.95
        sac_pop = _wet_squelch_layer(n, rng, bf * 1.7, 9, intensity=1.25) * adsr_envelope(n, 0.04, 0.22, 0.52, 0.35)
        brood_s = int(0.42 * n)
        brood = np.zeros(n, dtype=np.float64)
        bt = np.arange(n - brood_s, dtype=np.float64) / SAMPLE_RATE
        brood[brood_s:] = _fm_tone(bt, 640.0 * (1.0 + 0.25 * np.sin(2.0 * np.pi * 16.0 * bt)), 95.0, 2.0) * adsr_envelope(len(bt), 0.08, 0.25, 0.45, 0.38) * 0.55
        return _cavern_reverb(np.tanh((tear + sac_pop + brood) * 1.3), rng, wet=0.20, rt60=0.44, lp_hz=4200.0)

    # =========================================================================
    # 9. LEECH PARASITE — High-frequency lamprey sucker & blood/vitality siphon
    # =========================================================================
    if sound_id.startswith("leech_parasite_"):
        action = sound_id.split("leech_parasite_", 1)[1]
        if action == "screech":
            # Piercing toothed-sucker shriek + wet mouthpart flutter
            curve = np.sin(np.pi * t / dur) ** 0.85
            f_sc = bf * (0.88 + 0.85 * curve + 0.06 * np.sin(2.0 * np.pi * 42.0 * t + pjit))
            shriek = _fm_tone(t, f_sc, f_sc * 0.5, 2.8, pjit)
            tract = _apply_formant_bank(shriek, [(bf * 1.15 * formant_shift, 3.4, 0.95), (min(6200.0, bf * 2.65 * formant_shift), 4.2, 1.25), (min(8500.0, bf * 4.1), 4.8, 0.80)])
            wet_lip = _wet_squelch_layer(n, rng, bf * 0.8, 4, intensity=0.55)
            raw = np.tanh((tract * 1.15 + wet_lip) * 1.25) * adsr_envelope(n, 0.03, 0.18, 0.55, 0.30)
            return _cavern_reverb(_sos_highpass(raw, 280.0), rng, wet=0.16, rt60=0.36, lp_hz=7800.0)
        if action == "hurt":
            # High wet pinch + sharp parasitic squeal
            f_sq = bf * (1.45 - 0.72 * (t / dur))
            pinch = _fm_tone(t, f_sq, mf * 4.0, 2.5, pjit) * adsr_envelope(n, 0.015, 0.20, 0.40, 0.40)
            squish = _wet_squelch_layer(n, rng, bf * 0.9, 3, intensity=0.85) * exp_decay(t, dur * 0.48)
            return _cavern_reverb(_sos_highpass(pinch + squish, 250.0), rng, wet=0.14, rt60=0.30, lp_hz=7400.0)
        if action == "death":
            # High-pitched spiralling shriek deflating into wet splatter
            f_down = np.maximum(160.0, bf * 1.3 * np.exp(-1.65 * t / dur))
            wail = _fm_tone(t, f_down, f_down * 0.65, 2.4, pjit) * adsr_envelope(n, 0.02, 0.20, 0.40, 0.45)
            splat = _wet_squelch_layer(n, rng, bf * 0.7, 7, intensity=1.15) * adsr_envelope(n, 0.08, 0.25, 0.45, 0.40)
            return _cavern_reverb(_sos_highpass(wail * 0.85 + splat, 210.0), rng, wet=0.18, rt60=0.38, lp_hz=6800.0)
        # leech_parasite_siphon (rhythmic double suction pump + wet latch)
        wave = np.zeros(n, dtype=np.float64)
        for idx, start_f in enumerate((0.02, 0.46)):
            pos = int(start_f * n)
            slen = min(n - pos, int(0.44 * n))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            f_suck = bf * (0.55 + 0.95 * (st / max(1e-4, st[-1])))
            suck = np.sin(2.0 * np.pi * np.cumsum(f_suck) / SAMPLE_RATE + idx) * np.sin(np.linspace(0, np.pi, slen))
            slurp = _swept_resonant_filter(pink[pos : pos + slen], 380.0, 2600.0, q=3.4) * np.sin(np.linspace(0, np.pi, slen))
            wave[pos : pos + slen] += (suck * 0.85 + slurp * 0.95)
        return _cavern_reverb(wave + _wet_squelch_layer(n, rng, bf * 0.8, 5, intensity=0.8), rng, wet=0.14, rt60=0.32, lp_hz=5800.0)

    # =========================================================================
    # 10. COLONY WARDEN — Heavy pretorian commander (thoracic warhorn + shield)
    # =========================================================================
    if sound_id.startswith("colony_warden_"):
        action = sound_id.split("colony_warden_", 1)[1]
        shield_ratios = (1.0, 1.52, 2.28, 3.42, 4.85)
        if action == "bellow":
            # Commanding dual-tone thoracic warhorn bellow + resonant carapace chest
            f_cmd = bf * (0.96 + 0.16 * np.sin(np.pi * t / dur) - 0.12 * (t / dur))
            src = _glottal_sub_source(t, f_cmd, rng, harmonics=13, sub_weight=0.95, rattle_hz=20.0)
            fifth = _glottal_sub_source(t, f_cmd * 1.498, rng, harmonics=8, sub_weight=0.45, rattle_hz=20.0) * 0.55
            horn = _apply_formant_bank(src + fifth, [(125.0 * formant_shift, 2.5, 1.2), (340.0 * formant_shift, 3.0, 0.95), (880.0, 3.6, 0.55)])
            sub = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, f_cmd * 0.5)) / SAMPLE_RATE + pjit) * 0.75
            raw = np.tanh((horn + sub) * 1.4) * adsr_envelope(n, 0.09, 0.20, 0.62, 0.32)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.62, lp_hz=2600.0)
        if action == "hurt":
            # Heavy bone-shield clang + deep commander snarl
            shield = _modal_bank(t, bf * 3.2, shield_ratios, (0.08, 0.06, 0.04, 0.025, 0.015), (0.85, 0.65, 0.45, 0.30, 0.18), rng) * 1.0
            snarl = _apply_formant_bank(_glottal_sub_source(t, bf * (1.2 - 0.45 * (t / dur)), rng, harmonics=10, sub_weight=0.88), [(130.0 * formant_shift, 2.5, 1.15), (360.0, 3.0, 0.8)]) * adsr_envelope(n, 0.02, 0.22, 0.46, 0.35)
            return _cavern_reverb(np.tanh((shield + snarl) * 1.3), rng, wet=0.22, rt60=0.48, lp_hz=3200.0)
        if action == "death":
            # Falling warhorn groan + monumental shield/armor slam
            f_down = np.maximum(32.0, bf * 1.18 * np.exp(-1.15 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.95), [(105.0 * formant_shift, 2.4, 1.2), (280.0, 2.9, 0.8)]) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.44)
            imp = int(0.44 * n)
            crash = np.zeros(n, dtype=np.float64)
            ct = np.arange(n - imp, dtype=np.float64) / SAMPLE_RATE
            crash[imp:] = (
                np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, 130.0 * exp_decay(ct, 0.045))) / SAMPLE_RATE) * exp_decay(ct, 0.25) * 1.2
                + _modal_bank(ct, bf * 2.6, shield_ratios, (0.12, 0.09, 0.06, 0.04, 0.025), (0.85, 0.65, 0.45, 0.3, 0.18), rng) * 0.9
            )
            return _cavern_reverb(np.tanh((groan + crash) * 1.35), rng, wet=0.28, rt60=0.64, lp_hz=2800.0)
        # colony_warden_shockwave
        charge_n = min(n // 5, int(SAMPLE_RATE * 0.09))
        wave = np.zeros(n, dtype=np.float64)
        if charge_n > 24:
            ct = np.arange(charge_n, dtype=np.float64) / SAMPLE_RATE
            wave[:charge_n] = _swept_resonant_filter(pink[:charge_n], 160.0, 1800.0, q=2.8) * (ct / max(1e-4, ct[-1])) * 0.85
        bt = np.arange(n - charge_n, dtype=np.float64) / SAMPLE_RATE
        f_drop = np.maximum(28.0, bf * (0.36 + 2.3 * exp_decay(bt, 0.065)))
        sub_blast = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(bt, dur * 0.55) * 1.35
        ring = _modal_bank(bt, bf * 3.0, shield_ratios, (0.18, 0.13, 0.09, 0.06, 0.04), (0.8, 0.6, 0.45, 0.3, 0.18), rng) * 0.85
        roar = _sos_bandpass(brown[charge_n:] * 0.8 + pink[charge_n:] * 0.5, 42.0, 1600.0) * exp_decay(bt, dur * 0.45) * 0.95
        wave[charge_n:] = np.tanh((sub_blast + ring + roar) * 1.35)
        return _cavern_reverb(wave, rng, wet=0.28, rt60=0.62, lp_hz=3000.0)

    # =========================================================================
    # 11. SPORE ARCHITECT — Bio-printing builder & node constructor
    # =========================================================================
    if sound_id.startswith("spore_architect_"):
        action = sound_id.split("spore_architect_", 1)[1]
        if action == "pulse":
            # Rhythmic 3-step bio-printer extrusion pulse + crystalline node ping
            wave = np.zeros(n, dtype=np.float64)
            for idx, ratio in enumerate((1.0, 1.189, 1.498)):
                pos = int((0.04 + idx * 0.26) * n)
                plen = min(n - pos, int(SAMPLE_RATE * 0.22))
                pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
                tone = _fm_tone(pt, bf * ratio, bf * 0.5, 1.4, rng.uniform(0, 6.28)) * np.sin(np.linspace(0, np.pi, plen))
                extrude = _wet_squelch_layer(plen, rng, bf * 0.9, 2, intensity=0.55)
                wave[pos : pos + plen] += tone * 0.75 + extrude * 0.50
            return _cavern_reverb(wave, rng, wet=0.20, rt60=0.45, lp_hz=4400.0)
        if action == "hurt":
            # Wet gland compression + fractured crystal nozzle
            nozzle = _modal_bank(t, bf * 1.85, (1.0, 2.32, 3.85), (0.06, 0.04, 0.025), (0.75, 0.5, 0.3), rng)
            squelch = _wet_squelch_layer(n, rng, bf * 1.1, 3, intensity=0.85) * exp_decay(t, dur * 0.5)
            groan = _fm_tone(t, bf * (1.2 - 0.5 * (t / dur)), mf * 2.0, 1.8, pjit) * adsr_envelope(n, 0.02, 0.2, 0.4, 0.4) * 0.65
            return _cavern_reverb(nozzle + squelch + groan, rng, wet=0.18, rt60=0.40, lp_hz=4800.0)
        if action == "death":
            # Unwinding bio-loom chord + collapsing wet hypha scaffold
            f_down = np.maximum(52.0, bf * np.exp(-1.3 * t / dur))
            loom = (_fm_tone(t, f_down, f_down * 0.5, 2.0, pjit) * 0.7 + _modal_bank(t, bf * 1.4, (1.0, 2.32, 3.85), (0.14, 0.09, 0.05), (0.6, 0.4, 0.25), rng)) * adsr_envelope(n, 0.03, 0.22, 0.45, 0.44)
            scaffold = _wet_squelch_layer(n, rng, bf * 0.95, 7, intensity=0.95) * exp_decay(t, dur * 0.55)
            return _cavern_reverb(loom + scaffold, rng, wet=0.24, rt60=0.52, lp_hz=4200.0)
        # spore_architect_construct (weaving root fibers -> seating node -> crystal activation chord)
        weave = _wet_squelch_layer(n, rng, bf * 1.1, 6, intensity=0.85) * exp_decay(t, dur * 0.48)
        thud = _sos_bandpass(brown, 65.0, 320.0) * exp_decay(t, 0.14) * 0.95
        chord_s = int(0.32 * n)
        chord = np.zeros(n, dtype=np.float64)
        ct = np.arange(n - chord_s, dtype=np.float64) / SAMPLE_RATE
        chord[chord_s:] = _modal_bank(ct, bf * 1.65, (1.0, 1.498, 2.32, 3.85), (0.20, 0.15, 0.10, 0.06), (0.75, 0.58, 0.40, 0.24), rng)
        return _cavern_reverb(weave + thud + chord * 1.05, rng, wet=0.22, rt60=0.48, lp_hz=5200.0)

    # =========================================================================
    # 12. SYNAPTIC MIMIC — Comb-filtered uncanny double-voice shapeshifter
    # =========================================================================
    if sound_id.startswith("synaptic_mimic_"):
        action = sound_id.split("synaptic_mimic_", 1)[1]
        delay_ms = float(rng.uniform(13.0, 22.0))
        if action == "echo":
            # Uncanny dual-pitch mirrored vocal phrase + metallic phase comb-filter
            f_mir = bf * (0.92 + 0.24 * np.sin(2.0 * np.pi * (mf * 0.45) * t + pjit))
            v1 = _glottal_sub_source(t, f_mir, rng, harmonics=10, sub_weight=0.45, rattle_hz=29.0)
            v2 = _fm_tone(t, f_mir * 1.035, f_mir * 0.707, 2.1, pjit) * 0.65
            tract = _apply_formant_bank(v1 + v2, [(bf * 1.1 * formant_shift, 3.6, 1.0), (bf * 2.45 * formant_shift, 4.2, 1.1), (bf * 4.2, 4.8, 0.65)])
            combed = _comb_echo(tract * adsr_envelope(n, 0.05, 0.20, 0.55, 0.34), delay_ms, feedback=0.62)
            return _cavern_reverb(combed, rng, wet=0.24, rt60=0.52, lp_hz=5800.0)
        if action == "hurt":
            # Glitching double-voice fracture + phase-inverted metallic snap
            f_gl = bf * (1.35 - 0.55 * (t / dur)) * (1.0 + 0.12 * np.sign(np.sin(2.0 * np.pi * 24.0 * t)))
            voice = _fm_tone(t, f_gl, bf * 0.707, 2.5, pjit) * adsr_envelope(n, 0.015, 0.20, 0.42, 0.38)
            snap = _modal_bank(t, bf * 2.4, (1.0, 1.414, 2.756, 4.12), (0.06, 0.045, 0.03, 0.018), (0.8, 0.6, 0.4, 0.25), rng)
            return _cavern_reverb(_comb_echo(voice + snap * 0.85, delay_ms, feedback=0.58), rng, wet=0.20, rt60=0.44, lp_hz=6200.0)
        if action == "death":
            # Two diverging vocal pitches splitting apart (one rising, one falling) + glass-chitin shatter
            f_low = np.maximum(55.0, bf * np.exp(-1.45 * t / dur))
            f_high = np.minimum(2400.0, bf * (0.9 + 0.85 * (t / dur)))
            split_v = _fm_tone(t, f_low, mf * 1.8, 2.3, pjit) * 0.75 + _fm_tone(t, f_high, mf * 2.6, 1.9, pjit + 1.2) * exp_decay(t, dur * 0.48) * 0.55
            shatter = _chitin_modal_hits(n, rng, bf * 1.9, 6, 0.065, spread_frac=0.65) * 0.75
            raw = _comb_echo(split_v * adsr_envelope(n, 0.03, 0.22, 0.45, 0.44) + shatter, delay_ms, feedback=0.55)
            return _cavern_reverb(raw, rng, wet=0.28, rt60=0.60, lp_hz=5400.0)
        # synaptic_mimic_strike (stance-shift phase warp + razor twin-slash)
        warp = _swept_resonant_filter(pink * 0.6 + white * 0.5, 4200.0, 520.0, q=3.2) * exp_decay(t, dur * 0.45)
        twin = _modal_bank(t, bf * 2.2, (1.0, 1.414, 2.32, 3.85), (0.055, 0.04, 0.028, 0.018), (0.85, 0.65, 0.45, 0.28), rng, glide_factor=-0.28, glide_tau=0.035)
        return _cavern_reverb(_comb_echo(warp + twin, delay_ms=16.0, feedback=0.65), rng, wet=0.18, rt60=0.38, lp_hz=6400.0)

    # =========================================================================
    # 13. PURIFIER DRONE — Allied clinical-tech autonomous hover escort
    # =========================================================================
    if sound_id.startswith("purifier_drone_"):
        action = sound_id.split("purifier_drone_", 1)[1]
        if action == "hum":
            # Twin-rotor harmonic hum + telemetry confirmation micro-blip (varied pitch & phases per take)
            f_scale = rng.uniform(0.89, 1.11)
            lfo_rate = mf * rng.uniform(0.75, 1.30)
            f_rot = (bf * f_scale) * (1.0 + 0.016 * np.sin(2.0 * np.pi * lfo_rate * t + pjit))
            ph1, ph2, ph3 = rng.uniform(0.0, 6.28, size=3)
            hum_start = int(rng.uniform(0.0, 0.09) * n)
            rotor = np.zeros(n, dtype=np.float64)
            rotor[hum_start:] = (
                np.sin(2.0 * np.pi * np.cumsum(f_rot[hum_start:]) / SAMPLE_RATE + ph1) * 0.62
                + np.sin(2.0 * np.pi * np.cumsum(f_rot[hum_start:] * 1.5) / SAMPLE_RATE + ph2) * 0.36
                + np.sin(2.0 * np.pi * np.cumsum(f_rot[hum_start:] * 2.0) / SAMPLE_RATE + ph3) * 0.20
            ) * adsr_envelope(n - hum_start, 0.12, 0.20, 0.58, 0.32)
            blip_s = min(n - 64, int(rng.uniform(0.24, 0.66) * n))
            blip_len = min(n - blip_s, int(SAMPLE_RATE * rng.uniform(0.038, 0.058)))
            blip = np.zeros(n, dtype=np.float64)
            bt = np.arange(blip_len, dtype=np.float64) / SAMPLE_RATE
            blip_f = bf * rng.uniform(2.1, 2.9)
            blip[blip_s : blip_s + blip_len] = np.sin(2.0 * np.pi * blip_f * bt + rng.uniform(0, 6.28)) * np.sin(np.linspace(0, np.pi, blip_len)) * 0.40
            air = _sos_bandpass(pink, 650.0, 2600.0) * 0.19
            return _cavern_reverb(_sos_highpass(rotor + blip + air, 180.0), rng, wet=0.06, rt60=0.18, lp_hz=8200.0)
        if action == "hurt":
            # Titanium chassis ping + servo stall whine + electrical arc spark (varied onset & drop)
            hit_s = int(rng.uniform(0.01, 0.13) * n)
            rt = t[hit_s:] - t[hit_s]
            chassis = np.zeros(n, dtype=np.float64)
            chassis[hit_s:] = _modal_bank(
                rt,
                bf * rng.uniform(1.75, 2.25),
                (1.0, rng.uniform(1.52, 1.68), rng.uniform(2.05, 2.28), rng.uniform(3.40, 3.80)),
                (0.055, 0.038, 0.025, 0.015),
                (0.8, 0.55, 0.38, 0.22),
                rng,
            )
            servo = np.zeros(n, dtype=np.float64)
            slope = rng.uniform(0.55, 0.95)
            f_srv = np.maximum(120.0, bf * rng.uniform(0.92, 1.09) * (1.35 - slope * (rt / max(0.08, dur))))
            servo[hit_s:] = np.sin(2.0 * np.pi * np.cumsum(f_srv) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(rt, dur * 0.45) * 0.65
            spark = _micro_arc_zaps(n, rng, int(rng.integers(3, 6)), lo_hz=3400.0, hi_hz=13200.0, spread=(hit_s / float(n), 0.58)) * 0.65
            return _cavern_reverb(_sos_highpass(chassis + servo + spark, 180.0), rng, wet=0.08, rt60=0.20, lp_hz=8000.0)
        if action == "death":
            # Turbine gyro spin-down + 2 warning beeps + mechanical latch cutoff
            f_down = np.maximum(58.0, bf * 1.35 * np.exp(-1.85 * t / dur))
            turbine = (np.sin(2.0 * np.pi * np.cumsum(f_down) / SAMPLE_RATE + pjit) * 0.75 + _fm_tone(t, f_down * 1.5, 28.0, 0.9) * 0.35) * adsr_envelope(n, 0.02, 0.20, 0.48, 0.42)
            beeps = np.zeros(n, dtype=np.float64)
            for b_pos in (int(0.08 * n), int(0.28 * n)):
                blen = min(n - b_pos, int(SAMPLE_RATE * 0.055))
                bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
                beeps[b_pos : b_pos + blen] = np.sin(2.0 * np.pi * 1480.0 * bt) * exp_decay(bt, 0.025) * 0.50
            cutoff_s = int(0.74 * n)
            ct = np.arange(n - cutoff_s, dtype=np.float64) / SAMPLE_RATE
            beeps[cutoff_s:] += _modal_bank(ct, 780.0, (1.0, 2.1, 3.4), (0.028, 0.018, 0.010), (0.7, 0.45, 0.25), rng) * 0.75
            return _cavern_reverb(_sos_highpass(turbine + beeps, 120.0), rng, wet=0.10, rt60=0.24, lp_hz=7600.0)
        # purifier_drone_fire (crisp electromagnetic pulse dart: varied onset & chirp)
        hit_s = int(rng.uniform(0.01, 0.13) * n)
        rt = t[hit_s:] - t[hit_s]
        tau_d = rng.uniform(0.017, 0.033)
        f_dart = np.maximum(180.0, bf * rng.uniform(0.90, 1.12) * (0.28 + 1.45 * np.exp(-rt / tau_d)))
        ph_d = rng.uniform(0.0, 6.28)
        dart = np.zeros(n, dtype=np.float64)
        dart[hit_s:] = (
            np.sin(2.0 * np.pi * np.cumsum(f_dart) / SAMPLE_RATE + ph_d)
            + 0.45 * np.sin(4.0 * np.pi * np.cumsum(f_dart) / SAMPLE_RATE + rng.uniform(0, 6.28))
        ) * np.exp(-rt / 0.068) * 1.1
        snap = np.zeros(n, dtype=np.float64)
        snap[hit_s:] = _modal_bank(rt, bf * rng.uniform(1.65, 2.30), (1.0, 1.85, 3.12), (0.014, 0.009, 0.005), (0.7, 0.45, 0.25), rng) * 0.75
        air = _sos_bandpass(white, 2400.0, 10800.0) * np.pad(np.exp(-rt / 0.032), (hit_s, 0)) * 0.50
        return _cavern_reverb(_sos_highpass(dart + snap + air, 200.0), rng, wet=0.07, rt60=0.18, lp_hz=8500.0)

    # =========================================================================
    # 13b. COLONY SYMBIONT — Cooperative polymorphic worker & guardian caste
    # =========================================================================
    if sound_id.startswith("colony_symbiont_"):
        action = sound_id.removeprefix("colony_symbiont_")
        if action == "ambient":
            # Varied two-syllable harmonic chirp + tactile chitin clicks
            dir_s = float(rng.choice((-1.0, 1.0)))
            s1 = int(rng.uniform(0.01, 0.14) * n)
            rt = t[s1:] - t[s1]
            f_warble = bf * rng.uniform(0.88, 1.14) * (1.0 + dir_s * 0.32 * np.sin(np.pi * rt / max(0.08, dur)))
            chirp = np.zeros(n, dtype=np.float64)
            chirp[s1:] = (
                np.sin(2.0 * np.pi * np.cumsum(f_warble) / SAMPLE_RATE + rng.uniform(0, 6.28)) * 0.62
                + _fm_tone(rt, f_warble * 1.5, mf * rng.uniform(0.9, 1.5), 1.2, rng.uniform(0, 6.28)) * 0.42
            ) * adsr_envelope(len(rt), 0.08, 0.22, 0.52, 0.36)
            clicks = _chitin_modal_hits(n, rng, bf * rng.uniform(1.8, 2.8), int(rng.integers(2, 4)), 0.018, spread_frac=0.65) * 0.55
            return _cavern_reverb(_sos_highpass(chirp + clicks, 180.0), rng, wet=0.12, rt60=0.28, lp_hz=6400.0)
        if action == "work":
            # Rhythmic 3-tap crystalline/chitin craft cadence + varied resin servo sweep
            wave = np.zeros(n, dtype=np.float64)
            for idx, pos in enumerate(_bounce_train(n, rng, 3, start_frac=rng.uniform(0.01, 0.10), end_frac=0.72, accelerate=(rng.random() > 0.5))):
                slen = min(n - pos, int(SAMPLE_RATE * 0.07))
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                f_tap = bf * rng.uniform(0.95, 1.85) * (1.0 + 0.12 * idx)
                wave[pos : pos + slen] += _modal_bank(st, f_tap, (1.0, 1.78, 3.10), (0.030, 0.018, 0.010), (0.85, 0.52, 0.28), rng)
            f_lo = rng.uniform(420.0, 880.0)
            f_hi = rng.uniform(1800.0, 3600.0)
            servo = _swept_resonant_filter(pink, f_lo if rng.random() > 0.5 else f_hi, f_hi if rng.random() > 0.5 else f_lo, q=2.6) * adsr_envelope(n, 0.06, 0.22, 0.45, 0.32) * 0.45
            return _cavern_reverb(_sos_highpass(wave + servo, 160.0), rng, wet=0.10, rt60=0.24, lp_hz=6800.0)
        # colony_symbiont_promote (ascending ceremonial triadic rank chime + shimmer aura)
        wave = np.zeros(n, dtype=np.float64)
        jit0 = rng.uniform(0.01, 0.06)
        for idx, ratio in enumerate((1.0, 1.25, 1.498, 2.0)):
            pos = min(n - 64, int((jit0 + idx * rng.uniform(0.11, 0.15)) * n))
            nt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            wave[pos:] += _modal_bank(nt, bf * ratio * rng.uniform(0.97, 1.03), (1.0, 1.50, 2.42), (0.22, 0.16, 0.10), (0.78, 0.50, 0.26), rng) * (0.75 + 0.08 * idx)
        aura = _swept_resonant_filter(pink, 320.0, 3800.0, q=2.4) * adsr_envelope(n, 0.12, 0.22, 0.55, 0.38) * 0.42
        return _cavern_reverb(np.tanh((wave + aura) * 1.15), rng, wet=0.24, rt60=0.55, lp_hz=5800.0)

    # =========================================================================
    # 14. MYCELIAL CHIMERA — Bio-forged allied apex companion (dual vocal + symbiotic)
    # =========================================================================
    if sound_id.startswith("mycelial_chimera_") or sound_id == "chimera_roar":
        if sound_id == "mycelial_chimera_growl":
            # Dual-layered symbiotic purr-growl: warm beast chest + allied emerald harmonic overtone
            f_g = bf * (1.02 - 0.16 * (t / dur) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit))
            beast = _glottal_sub_source(t, f_g, rng, harmonics=10, sub_weight=0.80, rattle_hz=24.0)
            tract = _apply_formant_bank(beast, [(165.0 * formant_shift, 2.5, 1.1), (420.0 * formant_shift, 3.0, 0.9), (1120.0, 3.5, 0.5)])
            sym_fifth = np.sin(2.0 * np.pi * np.cumsum(f_g * 3.0) / SAMPLE_RATE + pjit) * 0.24
            raw = np.tanh((tract + sym_fifth) * 1.3) * adsr_envelope(n, 0.07, 0.22, 0.58, 0.30)
            return _cavern_reverb(raw, rng, wet=0.18, rt60=0.42, lp_hz=3600.0)
        if sound_id == "mycelial_chimera_hurt":
            # Fierce bio-alloy snarl-bark + chitin plate ring
            f_b = bf * (1.32 - 0.55 * (t / dur))
            bark = _apply_formant_bank(_glottal_sub_source(t, f_b, rng, harmonics=11, sub_weight=0.75, rattle_hz=28.0), [(210.0 * formant_shift, 2.6, 1.15), (540.0 * formant_shift, 3.2, 0.85)])
            plate = _modal_bank(t, bf * 2.8, (1.0, 1.85, 3.12), (0.045, 0.03, 0.018), (0.7, 0.45, 0.25), rng) * 0.65
            return _cavern_reverb(np.tanh((bark * adsr_envelope(n, 0.02, 0.2, 0.45, 0.36) + plate) * 1.3), rng, wet=0.18, rt60=0.38, lp_hz=4000.0)
        if sound_id == "mycelial_chimera_death":
            # Mournful dual-harmonic howl dropping into heavy body thud
            f_down = np.maximum(42.0, bf * 1.22 * np.exp(-1.25 * t / dur))
            howl = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=11, sub_weight=0.88, rattle_hz=20.0), [(160.0 * formant_shift, 2.5, 1.15), (410.0, 3.0, 0.85)])
            sym_echo = _fm_tone(t, f_down * 1.5, mf, 1.4, pjit) * 0.35
            thud_s = int(0.48 * n)
            thud = np.zeros(n, dtype=np.float64)
            tt = np.arange(n - thud_s, dtype=np.float64) / SAMPLE_RATE
            thud[thud_s:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(32.0, 115.0 * exp_decay(tt, 0.04))) / SAMPLE_RATE) * exp_decay(tt, 0.22) * 0.95
            raw = np.tanh(((howl + sym_echo) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + thud) * 1.3)
            return _cavern_reverb(raw, rng, wet=0.24, rt60=0.54, lp_hz=3400.0)
        if sound_id == "chimera_roar":
            # Heroic-feral dual roar: deep beast roar + resonant open-fifth bio-forge horn
            f_r = bf * (0.94 + 0.22 * np.sin(np.pi * t / dur) - 0.14 * (t / dur))
            roar_src = _glottal_sub_source(t, f_r, rng, harmonics=12, sub_weight=0.90, rattle_hz=23.0)
            tract = _apply_formant_bank(roar_src, [(175.0 * formant_shift, 2.4, 1.2), (460.0 * formant_shift, 3.0, 0.95), (1250.0, 3.6, 0.60)])
            chord = (np.sin(2.0 * np.pi * np.cumsum(f_r * 1.5) / SAMPLE_RATE + pjit) * 0.45 + np.sin(2.0 * np.pi * np.cumsum(f_r * 2.0) / SAMPLE_RATE) * 0.28)
            raw = np.tanh((tract + chord) * 1.38) * adsr_envelope(n, 0.08, 0.20, 0.60, 0.32)
            return _cavern_reverb(raw, rng, wet=0.26, rt60=0.58, lp_hz=3800.0)
        # mycelial_chimera_strike (feral claw tear + bio-gene surge discharge)
        slash = _swept_resonant_filter(pink * 0.7 + white * 0.5, 4800.0, 380.0, q=2.2) * exp_decay(t, dur * 0.42) * 1.1
        claws = _modal_bank(t, bf * 2.6, (1.0, 1.75, 2.95, 4.4), (0.045, 0.030, 0.020, 0.012), (0.8, 0.55, 0.35, 0.2), rng)
        surge = _fm_tone(t, bf * (1.6 - 0.8 * (t / dur)), mf * 2.5, 1.8, pjit) * exp_decay(t, dur * 0.38) * 0.65
        return _cavern_reverb(np.tanh((slash + claws + surge) * 1.3), rng, wet=0.16, rt60=0.35, lp_hz=5600.0)

    # =========================================================================
    # 14b. SPORE GLIDER & ROOT KEEPER — Sinkhole Vertical Ecosystem Fauna
    # =========================================================================
    if sound_id.startswith("spore_glider_"):
        action = sound_id.removeprefix("spore_glider_")
        if action == "ambient":
            s0 = int(rng.uniform(0.01, 0.14) * n)
            rt = t[s0:] - t[s0]
            flutter_hz = rng.uniform(14.0, 24.0)
            wing = _swept_resonant_filter(pink[s0:], rng.uniform(520.0, 880.0), rng.uniform(1600.0, 2900.0), q=2.8)
            wing *= (0.55 + 0.45 * np.sin(2.0 * np.pi * flutter_hz * rt + pjit))
            chime = _modal_bank(rt, bf * rng.uniform(0.92, 1.12), (1.0, 1.50, 2.25), (0.16, 0.11, 0.06), (0.65, 0.42, 0.22), rng)
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = (wing * 0.55 + chime * 0.72) * adsr_envelope(len(rt), 0.08, 0.22, 0.50, 0.36)
            return _cavern_reverb(_sos_highpass(wave, 190.0), rng, wet=0.22, rt60=0.45, lp_hz=7200.0)
        if action == "hurt":
            s0 = int(rng.uniform(0.0, 0.08) * n)
            rt = t[s0:] - t[s0]
            f_up = bf * rng.uniform(1.08, 1.28) * np.exp(-1.1 * rt / max(0.08, dur))
            squeak = _fm_tone(rt, f_up, mf * rng.uniform(0.9, 1.4), 1.6, pjit)
            snap = _sos_bandpass(white[s0:], 2100.0, 7400.0) * exp_decay(rt, 0.03) * 0.50
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = (squeak * 0.85 + snap) * adsr_envelope(len(rt), 0.02, 0.16, 0.40, 0.35)
            return _cavern_reverb(_sos_highpass(wave, 220.0), rng, wet=0.14, rt60=0.30, lp_hz=7600.0)
        f_down = np.maximum(140.0, bf * 1.15 * np.exp(-1.35 * t / dur))
        sigh = _fm_tone(t, f_down, mf, 1.2, pjit) * 0.68 + _swept_resonant_filter(pink, 1800.0, 320.0, q=2.4) * 0.48
        return _cavern_reverb(_sos_highpass(sigh * adsr_envelope(n, 0.03, 0.20, 0.46, 0.44), 160.0), rng, wet=0.24, rt60=0.52, lp_hz=6400.0)

    if sound_id.startswith("root_keeper_"):
        action = sound_id.removeprefix("root_keeper_")
        if action == "ambient":
            s0 = int(rng.uniform(0.01, 0.15) * n)
            rt = t[s0:] - t[s0]
            f_g = bf * rng.uniform(0.90, 1.12) * (1.0 - 0.18 * (rt / max(0.1, dur)) + 0.06 * np.sin(2.0 * np.pi * mf * rt + pjit))
            src = _glottal_sub_source(rt, f_g, rng, harmonics=11, sub_weight=0.88, rattle_hz=rng.uniform(16.0, 26.0))
            tract = _apply_formant_bank(src, [(135.0 * formant_shift, 2.3, 1.2), (340.0 * formant_shift, 2.9, 0.85)])
            wood = _modal_bank(rt, bf * 2.1, (1.0, 1.62, 2.55), (0.12, 0.07, 0.04), (0.45, 0.28, 0.15), rng) * 0.55
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = np.tanh((tract + wood) * 1.32) * adsr_envelope(len(rt), 0.10, 0.24, 0.56, 0.34)
            return _cavern_reverb(wave, rng, wet=0.24, rt60=0.58, lp_hz=3000.0)
        if action == "hurt":
            s0 = int(rng.uniform(0.01, 0.17) * n)
            rt = t[s0:] - t[s0]
            slope = float(rng.choice((-0.42, 0.36)))
            f_b = bf * rng.uniform(0.84, 1.24) * (1.15 + slope * (rt / max(0.08, dur)))
            bark = _apply_formant_bank(
                _glottal_sub_source(rt, f_b, rng, harmonics=10, sub_weight=0.82, rattle_hz=rng.uniform(18.0, 38.0)),
                [(rng.uniform(150.0, 230.0) * formant_shift, 2.5, 1.15), (rng.uniform(410.0, 580.0) * formant_shift, 3.0, 0.85)],
            )
            crack = _chitin_modal_hits(len(rt), rng, bf * rng.uniform(2.2, 3.9), int(rng.integers(2, 5)), 0.025, spread_frac=0.68) * 0.65
            wood_snap = _sos_bandpass(pink[s0:], rng.uniform(600.0, 1200.0), rng.uniform(2200.0, 4200.0)) * exp_decay(rt, 0.045) * 0.55
            wave = np.zeros(n, dtype=np.float64)
            wave[s0:] = np.tanh((bark * adsr_envelope(len(rt), 0.02, 0.18, 0.42, 0.36) + crack + wood_snap) * 1.3)
            return _cavern_reverb(wave, rng, wet=0.18, rt60=0.40, lp_hz=3800.0)
        if action == "death":
            f_d = np.maximum(36.0, bf * 1.18 * np.exp(-1.28 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_d, rng, harmonics=11, sub_weight=0.90, rattle_hz=18.0), [(140.0 * formant_shift, 2.4, 1.2), (360.0, 2.9, 0.82)])
            thud_s = int(0.52 * n)
            thud = np.zeros(n, dtype=np.float64)
            tt = np.arange(n - thud_s, dtype=np.float64) / SAMPLE_RATE
            thud[thud_s:] = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, 105.0 * exp_decay(tt, 0.045))) / SAMPLE_RATE) * exp_decay(tt, 0.24) * 1.05
            raw = np.tanh((groan * adsr_envelope(n, 0.04, 0.22, 0.48, 0.42) + thud) * 1.32)
            return _cavern_reverb(raw, rng, wet=0.26, rt60=0.62, lp_hz=3200.0)
        # root_keeper_slam
        s0 = int(rng.uniform(0.01, 0.07) * n)
        rt = t[s0:] - t[s0]
        f_sub = np.maximum(32.0, bf * rng.uniform(0.9, 1.1) * (0.5 + 1.6 * np.exp(-rt / 0.036)))
        sub = np.sin(2.0 * np.pi * np.cumsum(f_sub) / SAMPLE_RATE + pjit) * exp_decay(rt, dur * 0.42) * 1.15
        roots = _chitin_modal_hits(len(rt), rng, bf * 3.2, 5, 0.032, spread_frac=0.72) * 0.72
        grit = _sos_bandpass(pink[s0:], 140.0, 2600.0) * exp_decay(rt, dur * 0.34) * 0.72
        wave = np.zeros(n, dtype=np.float64)
        wave[s0:] = np.tanh((sub + roots + grit) * 1.35)
        return _cavern_reverb(wave, rng, wet=0.22, rt60=0.50, lp_hz=3200.0)

    # =========================================================================
    # 15. ANATOMICAL STEP PROFILES (4)
    # =========================================================================
    if sound_id == "mob_step_chitin":
        # Crisp double tarsus tap-clack on stone/mycelium
        wave = np.zeros(n, dtype=np.float64)
        for idx, pos in enumerate((0, min(n - 48, int(SAMPLE_RATE * rng.uniform(0.028, 0.052))))):
            slen = n - pos
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            g = 1.0 if idx == 0 else rng.uniform(0.60, 0.85)
            wave[pos:] += _modal_bank(st, bf * rng.uniform(0.95, 1.25), (1.0, 2.65, 5.12), (0.022, 0.014, 0.008), (0.7, 0.48, 0.26), rng) * g
            wave[pos:] += _sos_bandpass(white[pos:], 1600.0, 7800.0) * exp_decay(st, 0.009) * 0.42 * g
        return _cavern_reverb(_sos_highpass(wave, 200.0), rng, wet=0.10, rt60=0.22, lp_hz=6400.0)

    if sound_id == "mob_step_arachnid":
        # Rapid 4-leg articulated skitter cascade across takes
        wave = np.zeros(n, dtype=np.float64)
        leg_count = int(rng.integers(3, 5))
        for idx, pos in enumerate(_bounce_train(n, rng, leg_count, start_frac=0.01, end_frac=0.78, accelerate=False)):
            slen = min(n - pos, int(SAMPLE_RATE * 0.045))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            f_leg = bf * rng.uniform(1.05, 1.95)
            wave[pos : pos + slen] += _modal_bank(st, f_leg, (1.0, 2.756, 5.40), (0.018, 0.011, 0.006), (0.75, 0.50, 0.28), rng) * (0.92 ** idx)
            wave[pos : pos + slen] += _sos_bandpass(rng.normal(0, 1, slen), 2400.0, 10500.0) * exp_decay(st, 0.006) * 0.40
        return _cavern_reverb(_sos_highpass(wave, 240.0), rng, wet=0.12, rt60=0.26, lp_hz=7200.0)

    if sound_id == "mob_step_heavy":
        # Heavy armored plantigrade stomp: wide onset jitter, varied sub-end/decay & stone-chitin scatter
        stomp_s = int(rng.uniform(0.01, 0.21) * n)
        rt = t[stomp_s:] - t[stomp_s]
        tau_drop = rng.uniform(0.018, 0.048)
        f_end = rng.uniform(28.0, 62.0)
        f_drop = np.maximum(f_end, bf * rng.uniform(0.84, 1.18) * (0.45 + rng.uniform(1.1, 1.75) * np.exp(-rt / tau_drop)))
        sub = np.zeros(n, dtype=np.float64)
        sub_tau = dur * rng.uniform(0.24, 0.56)
        sub[stomp_s:] = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + rng.uniform(0, 6.28)) * np.exp(-rt / sub_tau) * 1.05
        plate = np.zeros(n, dtype=np.float64)
        plate[stomp_s:] = _modal_bank(rt, bf * rng.uniform(1.85, 3.25), (1.0, 2.18, 3.85), (0.038, 0.025, 0.015), (0.68, 0.45, 0.25), rng) * 0.70
        scatter = _chitin_modal_hits(n, rng, bf * rng.uniform(2.8, 4.6), int(rng.integers(2, 5)), 0.028, spread_frac=0.72) * 0.52
        grit = _sos_bandpass(pink, 140.0, 2900.0) * np.pad(np.exp(-rt / (dur * 0.30)), (stomp_s, 0)) * 0.70
        return _cavern_reverb(np.tanh((sub + plate + scatter + grit) * 1.28), rng, wet=0.16, rt60=0.36, lp_hz=2800.0)

    if sound_id == "mob_step_hover":
        # Doppler thruster pulse + FM rotor flutter (varied direction, sweep shape & phases per take)
        dir_sign = float(rng.choice((-1.0, 1.0)))
        u_shape = (t / max(0.05, dur)) ** rng.uniform(0.55, 1.65)
        depth = rng.uniform(0.18, 0.42) * dir_sign
        f_pulse = bf * rng.uniform(0.84, 1.18) * (1.0 + depth * np.sin(np.pi * u_shape + rng.uniform(0.0, 1.4)))
        rotor_fm = rng.uniform(28.0, 68.0)
        fm_idx = rng.uniform(0.8, 2.1)
        hum = (
            _fm_tone(t, f_pulse, rotor_fm, fm_idx, rng.uniform(0, 6.28), rng.uniform(0, 6.28)) * 0.62
            + np.sin(2.0 * np.pi * np.cumsum(f_pulse * rng.uniform(1.42, 1.62)) / SAMPLE_RATE + rng.uniform(0, 6.28)) * 0.36
        )
        f_air_a = rng.uniform(420.0, 950.0)
        f_air_b = rng.uniform(1700.0, 3400.0)
        air = _swept_resonant_filter(pink, f_air_a if dir_sign > 0 else f_air_b, f_air_b if dir_sign > 0 else f_air_a, q=2.8) * 0.52
        shift = int(rng.uniform(0.0, 0.19) * n)
        env_h = np.pad(adsr_envelope(n - shift, rng.uniform(0.08, 0.22), 0.22, 0.44, 0.32), (shift, 0))
        return _cavern_reverb((hum + air) * env_h, rng, wet=0.08, rt60=0.22, lp_hz=6800.0)

    raise ValueError(f"Unhandled entity sound_id in entity_synth: {sound_id}")
