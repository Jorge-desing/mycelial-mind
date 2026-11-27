"""Dedicated multi-stage narrative synthesizers for all 26 item, weapon, armor, relic & projectile sound events.

Replaces shared generic chimes/slashes/pulses with bespoke acoustic sequences tailored to the
physical mechanism and lore of each tool, weapon, vial, injector, projectile, and endgame relic.
"""

from __future__ import annotations

import numpy as np

from scripts.sound_studio.block_synth import (
    _micro_arc_zaps,
    _modal_bank,
)
from scripts.sound_studio.boss_synth import (
    _abyssal_reverb,
    _brass_braaam,
    _eldritch_choir,
    _reverse_suction,
    _sub_bomb,
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
    amethyst_copper_chime,
    exp_decay,
    fdn4_hadamard_diffuser,
    foley_particle_crackle,
    granular_bio_cloud,
    karplus_strong_chitin,
    wooden_chitin_knock,
)

ITEM_PROJECTILE_IDS: frozenset[str] = frozenset(
    {
        "bio_scanner_ping",
        "bio_scanner_thermal_pulse",
        "bio_extractor_draw",
        "symbiotic_blade_slash",
        "enzyme_lance_thrust",
        "spore_launcher_fire",
        "purifier_grenade_throw",
        "purifier_grenade_detonate",
        "acid_spit_impact",
        "neutralizing_spore_impact",
        "chitin_armor_equip",
        "chitin_armor_evolve",
        "spore_antidote_cure",
        "counter_serum_inject",
        "symbiotic_mutagen_implant",
        "synaptic_graft_splice",
        "neural_decoy_deploy",
        "volatile_engram_pulse",
        "schism_injector_sting",
        "truce_pheromone_spray",
        "quorum_seal_pact",
        "chimera_ovum_hatch",
        "purifier_drone_deploy",
        "tide_beacon_activate",
        "rift_catalyst_ignite",
        "overmind_crown_surge",
        "command_staff_mark",
        "ui_codex_tab",
        "ui_action_confirm",
        "resonant_elixir_drink",
    }
)


def synthesize_item_sound(
    sound_id: str,
    *,
    duration: float,
    base_freq: float,
    mod_freq: float,
    rng: np.random.Generator,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a bespoke multi-stage item, weapon, consumable, relic or projectile waveform."""
    dur = max(0.16, float(duration))
    n = max(64, int(round(dur * SAMPLE_RATE)))
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)
    bf = max(36.0, float(base_freq) * float(rng.uniform(0.96, 1.04)))
    mf = max(1.0, float(mod_freq) * float(rng.uniform(0.92, 1.08)))
    pjit = float(rng.uniform(0.0, 2.0 * np.pi))

    # =========================================================================
    # 1. SCANNING & GENETIC EXTRACTION TOOLS (3)
    # =========================================================================
    if sound_id == "bio_scanner_ping":
        # Tactile wooden-copper compass click -> clear struck amethyst resonance -> warm harmonic echo
        click = wooden_chitin_knock(n, 1650.0, rng, decay=0.016, hollowness=0.42) * 0.62
        ping = amethyst_copper_chime(t, bf * 1.18, rng, decay=dur * 0.34, crystal_mix=0.72) * 1.12
        echo_s = min(n - 64, int(rng.uniform(0.40, 0.52) * n))
        echo = np.zeros(n, dtype=np.float64)
        et = np.arange(n - echo_s, dtype=np.float64) / SAMPLE_RATE
        echo[echo_s:] = amethyst_copper_chime(et, bf * 1.62, rng, decay=0.055, crystal_mix=0.80) * 0.48
        return _cavern_reverb(_sos_highpass(click + ping + echo, 320.0), rng, wet=0.06, rt60=0.20, lp_hz=9200.0)

    if sound_id == "bio_scanner_thermal_pulse":
        # Wooden copper latch click -> warm air crystal sweep + micro particles -> double amethyst confirmation chime
        latch = wooden_chitin_knock(n, 880.0, rng, decay=0.025, hollowness=0.55) * 0.68
        wave = latch
        sweep = _swept_resonant_filter(pink * 0.6 + white * 0.45, 4200.0, 560.0, q=3.0) * adsr_envelope(n, 0.16, 0.22, 0.45, 0.35) * 0.95
        dust = foley_particle_crackle(n, rng, lo_hz=1800.0, hi_hz=7600.0, count=6) * 0.45
        ring_s = int(0.28 * n)
        rt = np.arange(n - ring_s, dtype=np.float64) / SAMPLE_RATE
        wave[ring_s:] += amethyst_copper_chime(rt, bf * 1.35, rng, decay=0.16, crystal_mix=0.68) * 0.92
        return _cavern_reverb(_sos_highpass(wave + sweep + dust, 200.0), rng, wet=0.08, rt60=0.28, lp_hz=8200.0)

    if sound_id == "bio_extractor_draw":
        # Hollow needle puncture snap (0s) -> vacuum vial fluid draw slurp (0.08..0.76) -> glass stopper seal click
        puncture = _modal_bank(t, 1450.0, (1.0, 2.18, 3.65), (0.018, 0.012, 0.007), (0.8, 0.55, 0.30), rng) * 0.85
        suck_s = int(0.10 * n)
        suck_e = int(0.80 * n)
        wave = puncture.copy()
        if suck_e > suck_s + 32:
            slen = suck_e - suck_s
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            f_draw = np.linspace(bf * 0.65, bf * 1.85, slen)
            fluid = _swept_resonant_filter(pink[suck_s:suck_e], 280.0, 2400.0, q=3.6) * np.sin(np.linspace(0, np.pi, slen))
            bubbles = _wet_squelch_layer(slen, rng, bf * 1.2, 5, intensity=1.1)
            tone = np.sin(2.0 * np.pi * np.cumsum(f_draw) / SAMPLE_RATE + pjit) * np.sin(np.linspace(0, np.pi, slen)) * 0.45
            wave[suck_s:suck_e] += fluid * 0.85 + bubbles * 0.90 + tone
        seal_s = int(0.82 * n)
        lt = np.arange(n - seal_s, dtype=np.float64) / SAMPLE_RATE
        wave[seal_s:] += _modal_bank(lt, 1950.0, (1.0, 2.32, 3.85), (0.024, 0.016, 0.010), (0.75, 0.5, 0.3), rng)
        return _cavern_reverb(wave, rng, wet=0.12, rt60=0.28, lp_hz=6400.0)

    # =========================================================================
    # 2. WEAPONS & PROJECTILES (7)
    # =========================================================================
    if sound_id == "symbiotic_blade_slash":
        # Living chitin-steel broadsword arc: deep air cleave + singing bio-steel harmonic + wet tendril snap
        cleave = _swept_resonant_filter(pink * 0.75 + white * 0.45, 3600.0, 260.0, q=2.1) * adsr_envelope(n, 0.04, 0.24, 0.40, 0.36) * 1.15
        blade_ring = _modal_bank(
            t,
            bf * 1.65,
            (1.0, 1.59, 2.32, 3.65, 5.12),
            (0.14, 0.11, 0.08, 0.055, 0.035),
            (0.75, 0.58, 0.42, 0.28, 0.16),
            rng,
            glide_factor=-0.22,
            glide_tau=0.05,
        )
        tendril = _wet_squelch_layer(n, rng, bf * 1.3, 3, intensity=0.55) * exp_decay(t, dur * 0.42)
        return _cavern_reverb(np.tanh((cleave + blade_ring * 0.95 + tendril) * 1.3), rng, wet=0.15, rt60=0.34, lp_hz=6200.0)

    if sound_id == "enzyme_lance_thrust":
        # Pneumatic piston unlock (0s) -> supersonic rapier piercing whistle -> corrosive enzyme hiss
        piston = _modal_bank(t, 980.0, (1.0, 1.85, 2.92), (0.022, 0.014, 0.009), (0.85, 0.6, 0.35), rng) * 0.85
        f_pierce = bf * (2.4 - 1.55 * ((t / dur) ** 0.7))
        whistle = _fm_tone(t, f_pierce, f_pierce * 0.5, 1.6, pjit) * adsr_envelope(n, 0.03, 0.20, 0.45, 0.36) * 0.85
        enzyme_hiss = _sos_bandpass(white, 2800.0, 11800.0) * adsr_envelope(n, 0.10, 0.25, 0.45, 0.35) * 0.70
        return _cavern_reverb(_sos_highpass(piston + whistle + enzyme_hiss, 220.0), rng, wet=0.12, rt60=0.28, lp_hz=8200.0)

    if sound_id == "spore_launcher_fire":
        # Breach valve click -> hollow tube mortar THWUMP (620 -> 115 Hz) -> pressurized gas puff
        valve = _modal_bank(t, 1320.0, (1.0, 2.14, 3.6), (0.015, 0.010, 0.006), (0.75, 0.5, 0.3), rng) * 0.70
        f_tube = np.maximum(95.0, bf * (0.24 + 1.35 * exp_decay(t, 0.034)))
        thwump = np.sin(2.0 * np.pi * np.cumsum(f_tube) / SAMPLE_RATE + pjit) * exp_decay(t, 0.095) * 1.25
        puff = _swept_resonant_filter(pink * 0.6 + white * 0.5, 3200.0, 680.0, q=2.5) * exp_decay(t, dur * 0.42) * 0.75
        return _cavern_reverb(np.tanh((valve + thwump + puff) * 1.3), rng, wet=0.12, rt60=0.28, lp_hz=6200.0)

    if sound_id == "purifier_grenade_throw":
        # Safety pin metallic *TINK!* (0s) -> spinning canister Doppler whoosh
        pin = _modal_bank(t, 2450.0, (1.0, 1.89, 3.12), (0.035, 0.022, 0.014), (0.85, 0.6, 0.35), rng) * 0.85
        spin_mod = 0.5 + 0.5 * np.sin(2.0 * np.pi * 16.0 * t + pjit)
        whoosh = _swept_resonant_filter(pink * 0.7 + white * 0.4, 650.0, 2800.0, q=2.4) * spin_mod * adsr_envelope(n, 0.08, 0.25, 0.45, 0.35) * 0.95
        return _cavern_reverb(_sos_highpass(pin + whoosh, 200.0), rng, wet=0.10, rt60=0.24, lp_hz=7600.0)

    if sound_id == "purifier_grenade_detonate":
        # Stage 1: 45 ms EMP implosion charge -> Stage 2: Clean sub-shockwave + alloy shatter -> Stage 3: Cyan purge chime
        imp_idx = min(n // 5, int(SAMPLE_RATE * 0.048))
        wave = _reverse_suction(n, 0, imp_idx, rng, f_lo=260.0, f_hi=4200.0) * 0.75
        bomb = _sub_bomb(t, imp_idx, f_start=175.0, f_end=34.0, drop_tau=0.065, sustain_tau=dur * 0.48, punch_gain=0.65)
        rt = np.arange(n - imp_idx, dtype=np.float64) / SAMPLE_RATE
        shimmer = _modal_bank(
            rt,
            bf * 3.2,
            (1.0, 1.25, 1.498, 2.0, 3.0),
            (0.28, 0.24, 0.20, 0.15, 0.10),
            (0.65, 0.55, 0.48, 0.35, 0.22),
            rng,
        )
        blast_air = _sos_bandpass(pink[imp_idx:] * 0.7 + white[imp_idx:] * 0.5, 120.0, 5800.0) * exp_decay(rt, dur * 0.36) * 0.90
        wave[imp_idx:] += bomb[imp_idx:] * 1.25 + shimmer * 0.85 + blast_air
        return _cavern_reverb(np.tanh(wave * 1.3), rng, wet=0.22, rt60=0.52, lp_hz=6800.0)

    if sound_id == "acid_spit_impact":
        # Wet glob splat transient + intense high-frequency chemical corrosion sizzle + bubble pops
        splat = _swept_resonant_filter(pink, 1600.0, 280.0, q=2.8) * exp_decay(t, 0.055) * 1.05
        fizz = _sos_bandpass(white, 3200.0, 13200.0) * adsr_envelope(n, 0.03, 0.22, 0.50, 0.35) * 0.85
        bubbles = _wet_squelch_layer(n, rng, bf * 1.6, 6, intensity=0.95) * exp_decay(t, dur * 0.55)
        return _cavern_reverb(np.tanh((splat + fizz + bubbles) * 1.25), rng, wet=0.10, rt60=0.24, lp_hz=8500.0)

    if sound_id == "neutralizing_spore_impact":
        # Pressurized capsule crack-pop + sterile crystalline neutralizing chime triad
        pop = np.sin(2.0 * np.pi * np.cumsum(np.maximum(110.0, bf * (0.45 + 1.65 * exp_decay(t, 0.022)))) / SAMPLE_RATE + pjit) * exp_decay(t, 0.065) * 1.05
        chime = _modal_bank(t, bf * 1.5, (1.0, 1.25, 1.498, 2.0), (0.12, 0.10, 0.08, 0.05), (0.7, 0.55, 0.45, 0.28), rng) * 0.90
        mist = _sos_bandpass(white, 2400.0, 10500.0) * exp_decay(t, 0.045) * 0.50
        return _cavern_reverb(_sos_highpass(pop + chime + mist, 200.0), rng, wet=0.10, rt60=0.24, lp_hz=8200.0)

    # =========================================================================
    # 3. ARMOR, CONSUMABLES & INJECTORS (10)
    # =========================================================================
    if sound_id == "chitin_armor_equip":
        # 2-stage articulated carapace plate clack + leather/tendon strap cinch
        p1 = _modal_bank(t, bf * 1.1, (1.0, 2.756, 5.40), (0.038, 0.024, 0.015), (0.75, 0.52, 0.30), rng)
        s2 = int(0.34 * n)
        t2 = np.arange(n - s2, dtype=np.float64) / SAMPLE_RATE
        p2 = np.zeros(n, dtype=np.float64)
        p2[s2:] = _modal_bank(t2, bf * 1.42, (1.0, 2.756, 5.40), (0.032, 0.020, 0.012), (0.8, 0.55, 0.32), rng)
        strap = _swept_resonant_filter(pink, 320.0, 1450.0, q=2.6) * adsr_envelope(n, 0.12, 0.25, 0.40, 0.35) * 0.55
        return _cavern_reverb(_sos_highpass(p1 + p2 + strap, 180.0), rng, wet=0.09, rt60=0.22, lp_hz=6800.0)

    if sound_id == "chitin_armor_evolve":
        # Exoskeleton molt crackle -> ascending 4-note bio-crystalline resonance chord
        molt = _chitin_modal_hits(n, rng, bf * 1.6, 6, 0.035, spread_frac=0.55) * 0.70
        chord = np.zeros(n, dtype=np.float64)
        for idx, ratio in enumerate((1.0, 1.25, 1.498, 2.0)):
            pos = int((0.06 + idx * 0.16) * n)
            ct = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            chord[pos:] += _modal_bank(ct, bf * ratio, (1.0, 2.0, 3.01), (0.24, 0.16, 0.10), (0.7, 0.45, 0.25), rng) * 0.65
        return _cavern_reverb(molt + chord, rng, wet=0.20, rt60=0.45, lp_hz=6800.0)

    if sound_id == "spore_antidote_cure":
        # Glass vial pop (0s) -> sparkling effervescent fizz -> warm soothing major-sixth relief chord
        cork = np.sin(2.0 * np.pi * np.cumsum(np.linspace(280.0, 920.0, min(n, int(SAMPLE_RATE * 0.028)))) / SAMPLE_RATE)
        wave = np.zeros(n, dtype=np.float64)
        wave[: len(cork)] = cork * np.sin(np.linspace(0, np.pi, len(cork))) * 0.75
        fizz = _sos_bandpass(white * 0.5 + pink * 0.5, 2200.0, 9600.0) * exp_decay(t, dur * 0.36) * 0.45
        for idx, ratio in enumerate((1.0, 1.25, 1.667)):
            pos = int((0.12 + idx * 0.14) * n)
            rt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            f_n = bf * ratio
            wave[pos:] += (np.sin(2.0 * np.pi * f_n * rt + idx) * 0.65 + np.sin(2.0 * np.pi * (f_n * 2.0) * rt) * 0.25) * exp_decay(rt, 0.18)
        return _cavern_reverb(_sos_highpass(wave + fizz, 200.0), rng, wet=0.14, rt60=0.34, lp_hz=7800.0)

    if sound_id == "counter_serum_inject":
        # Pneumatic autoinjector spring SNAP (0s) -> downward memory-bleed phase sweep (780 -> 140 Hz)
        spring = _modal_bank(t, 1580.0, (1.0, 1.92, 3.25), (0.022, 0.014, 0.008), (0.85, 0.58, 0.32), rng) * 0.90
        bleed_s = int(0.10 * n)
        bt = np.arange(n - bleed_s, dtype=np.float64) / SAMPLE_RATE
        f_bleed = np.maximum(95.0, bf * 1.75 * np.exp(-1.6 * bt / max(1e-4, dur)))
        drain = _fm_tone(bt, f_bleed, f_bleed * 0.5, 1.8, pjit) * adsr_envelope(len(bt), 0.06, 0.22, 0.48, 0.38) * 0.95
        hiss = _swept_resonant_filter(pink[bleed_s:], 2800.0, 420.0, q=3.0) * exp_decay(bt, dur * 0.40) * 0.65
        wave = spring.copy()
        wave[bleed_s:] += drain + hiss
        return _cavern_reverb(wave, rng, wet=0.14, rt60=0.32, lp_hz=6200.0)

    if sound_id == "symbiotic_mutagen_implant":
        # Visceral flesh graft squelch -> rapid double heartbeat thump-thump -> dark symbiotic sub-chord
        graft = _wet_squelch_layer(n, rng, bf * 1.3, 6, intensity=1.05) * exp_decay(t, dur * 0.45)
        beats = np.zeros(n, dtype=np.float64)
        for b_frac, gain in ((0.18, 1.0), (0.44, 1.15)):
            pos = int(b_frac * n)
            blen = min(n - pos, int(SAMPLE_RATE * 0.16))
            bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
            f_b = 72.0 * (0.6 + 1.2 * exp_decay(bt, 0.032))
            beats[pos : pos + blen] += np.sin(2.0 * np.pi * np.cumsum(f_b) / SAMPLE_RATE) * exp_decay(bt, 0.065) * gain
        chord = (
            np.sin(2.0 * np.pi * (bf * 0.5) * t + pjit) * 0.55
            + _fm_tone(t, bf, bf * 0.707, 1.5, pjit) * 0.48
        ) * adsr_envelope(n, 0.25, 0.20, 0.55, 0.35)
        return _cavern_reverb(np.tanh((graft + beats * 1.15 + chord) * 1.3), rng, wet=0.20, rt60=0.46, lp_hz=4200.0)

    if sound_id == "synaptic_graft_splice":
        # Electrical neural arc zaps -> 3-step crystal node reprogramming arpeggio (tritone -> perfect fifth!)
        zaps = _micro_arc_zaps(n, rng, 6, lo_hz=3200.0, hi_hz=12800.0, spread=(0.02, 0.55)) * 0.75
        notes = np.zeros(n, dtype=np.float64)
        for idx, ratio in enumerate((1.0, 1.414, 1.50)):
            pos = int((0.06 + idx * 0.24) * n)
            nt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            notes[pos:] += _modal_bank(nt, bf * ratio, (1.0, 2.32, 3.85), (0.16, 0.11, 0.07), (0.75, 0.52, 0.32), rng) * 0.85
        return _cavern_reverb(zaps + notes, rng, wet=0.18, rt60=0.42, lp_hz=6800.0)

    if sound_id == "neural_decoy_deploy":
        # Mechanical tripod unfold clack -> 3 rapid radar-lure sonar pulses
        tripod = _modal_bank(t, 680.0, (1.0, 1.59, 2.92), (0.04, 0.026, 0.016), (0.8, 0.55, 0.32), rng) * 0.90
        pulses = np.zeros(n, dtype=np.float64)
        for idx, p_frac in enumerate((0.20, 0.46, 0.72)):
            pos = int(p_frac * n)
            plen = min(n - pos, int(SAMPLE_RATE * 0.085))
            pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
            f_p = bf * (1.15 + 0.12 * idx)
            pulses[pos : pos + plen] += (np.sin(2.0 * np.pi * f_p * pt) + 0.4 * np.sin(4.0 * np.pi * f_p * pt)) * np.sin(np.linspace(0, np.pi, plen)) * 0.78
        return _cavern_reverb(_sos_highpass(tripod + pulses, 180.0), rng, wet=0.12, rt60=0.28, lp_hz=7600.0)

    if sound_id == "volatile_engram_pulse":
        # Crystalline engram shatter -> Pavlovian psychic phase-wobble wave
        shatter = _modal_bank(t, bf * 1.8, (1.0, 1.414, 2.32, 3.85), (0.07, 0.05, 0.035, 0.02), (0.8, 0.6, 0.4, 0.25), rng)
        f_wobble = bf * (1.0 + 0.28 * np.sin(2.0 * np.pi * mf * t + pjit))
        mirage = _fm_tone(t, f_wobble, f_wobble * 0.707, 2.4, pjit) * adsr_envelope(n, 0.08, 0.22, 0.55, 0.36) * 0.85
        return _cavern_reverb(shatter * 0.85 + mirage, rng, wet=0.24, rt60=0.54, lp_hz=6200.0)

    if sound_id == "schism_injector_sting":
        # Sharp dart sting -> violent bifurcating dissonance (two voices splitting into AMBER high & CRIMSON low)
        sting = _sos_bandpass(white, 2800.0, 11800.0) * exp_decay(t, 0.018) * 0.90
        u = t / dur
        f_amber = bf * (1.0 + 0.55 * u)
        f_crimson = bf * (1.0 - 0.48 * u)
        split = (
            _fm_tone(t, f_amber, mf * 3.0, 2.2, pjit) * 0.65
            + _glottal_sub_source(t, f_crimson, rng, harmonics=9, sub_weight=0.75, rattle_hz=28.0) * 0.65
        ) * adsr_envelope(n, 0.04, 0.20, 0.55, 0.34)
        return _cavern_reverb(np.tanh((sting + split) * 1.25), rng, wet=0.18, rt60=0.40, lp_hz=6400.0)

    if sound_id == "truce_pheromone_spray":
        # Pressurized aerosol nozzle pssht -> calming warm low-Q harmonic mist
        nozzle = _sos_bandpass(white * 0.6 + pink * 0.5, 1800.0, 8800.0) * adsr_envelope(n, 0.04, 0.24, 0.42, 0.38) * 0.80
        mist = (
            np.sin(2.0 * np.pi * (bf * 0.75) * t + pjit) * 0.55
            + np.sin(2.0 * np.pi * (bf * 1.125) * t) * 0.40
            + np.sin(2.0 * np.pi * (bf * 1.50) * t) * 0.25
        ) * adsr_envelope(n, 0.16, 0.22, 0.55, 0.36)
        return _cavern_reverb(nozzle + mist, rng, wet=0.18, rt60=0.42, lp_hz=6200.0)

    # =========================================================================
    # 4. RELICS, PACTS & ENDGAME CATALYSTS (6)
    # =========================================================================
    if sound_id == "quorum_seal_pact":
        # Ceremonial obsidian-gold wax seal stamp thud -> solemn cathedral open-fifth pact chord
        stamp = _sos_bandpass(brown, 55.0, 340.0) * exp_decay(t, 0.085) * 1.05
        bell_ratios = (0.5, 1.0, 1.498, 2.0, 2.996)
        bell = _modal_bank(t, bf, bell_ratios, (0.34, 0.28, 0.22, 0.16, 0.11), (0.65, 0.85, 0.60, 0.45, 0.28), rng)
        return _cavern_reverb(np.tanh((stamp + bell) * 1.2), rng, wet=0.28, rt60=0.65, lp_hz=4800.0)

    if sound_id == "chimera_ovum_hatch":
        # 3 escalating eggshell fractures -> wet amniotic membrane tear -> vibrant newborn Chimera trill
        wave = np.zeros(n, dtype=np.float64)
        for idx, c_frac in enumerate((0.02, 0.16, 0.28)):
            pos = int(c_frac * n)
            clen = min(n - pos, int(SAMPLE_RATE * 0.065))
            ct = np.arange(clen, dtype=np.float64) / SAMPLE_RATE
            wave[pos : pos + clen] += _modal_bank(ct, bf * (1.8 + 0.3 * idx), (1.0, 2.45, 4.12), (0.025, 0.016, 0.010), (0.75, 0.5, 0.3), rng) * (0.75 + 0.2 * idx)
        tear_s = int(0.30 * n)
        tt = np.arange(n - tear_s, dtype=np.float64) / SAMPLE_RATE
        wet = _wet_squelch_layer(len(tt), rng, bf * 1.2, 5, intensity=0.95) * exp_decay(tt, 0.18)
        f_trill = bf * (0.95 + 0.65 * (tt / max(1e-4, tt[-1])) + 0.08 * np.sin(2.0 * np.pi * 18.0 * tt))
        cry = _apply_formant_bank(_glottal_sub_source(tt, f_trill, rng, harmonics=9, sub_weight=0.6), [(bf * 1.4 * formant_shift, 2.8, 1.1), (bf * 3.1, 3.4, 0.85)])
        wave[tear_s:] += wet + cry * adsr_envelope(len(tt), 0.08, 0.22, 0.52, 0.38)
        return _cavern_reverb(np.tanh(wave * 1.25), rng, wet=0.20, rt60=0.45, lp_hz=5200.0)

    if sound_id == "purifier_drone_deploy":
        # Mechanical latch release -> 3-note ascending boot chime -> turbine spin-up hum
        wave = _modal_bank(t, 760.0, (1.0, 1.85, 3.1), (0.03, 0.02, 0.012), (0.7, 0.45, 0.25), rng) * 0.75
        for idx, ratio in enumerate((1.0, 1.25, 1.50)):
            pos = int((0.08 + idx * 0.14) * n)
            nt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            f_n = bf * ratio
            wave[pos:] += np.sin(2.0 * np.pi * f_n * nt) * exp_decay(nt, 0.095) * 0.65
        spin_s = int(0.32 * n)
        st = np.arange(n - spin_s, dtype=np.float64) / SAMPLE_RATE
        f_up = np.linspace(220.0, 610.0, len(st))
        wave[spin_s:] += np.sin(2.0 * np.pi * np.cumsum(f_up) / SAMPLE_RATE + pjit) * adsr_envelope(len(st), 0.14, 0.2, 0.55, 0.35) * 0.75
        return _cavern_reverb(_sos_highpass(wave, 160.0), rng, wet=0.08, rt60=0.22, lp_hz=8000.0)

    if sound_id == "tide_beacon_activate":
        # Obelisk capacitor charge -> skyward fungal war-beam surge + distant thunder rumble
        f_up = bf * (0.45 + 1.45 * ((t / dur) ** 1.3))
        beam = _brass_braaam(t, f_up, rng, detune_cents=15.0, sub_oct=0.80, shimmer_mix=0.22, formant_shift=formant_shift) * adsr_envelope(n, 0.10, 0.22, 0.55, 0.32)
        sky_tear = _swept_resonant_filter(pink, 240.0, 4800.0, q=2.8) * np.sin(np.pi * t / dur) * 0.75
        sub = _sub_bomb(t, int(0.18 * n), f_start=140.0, f_end=32.0, drop_tau=0.075, sustain_tau=dur * 0.48)
        return _cavern_reverb(np.tanh((beam * 0.95 + sky_tear + sub * 0.90) * 1.3), rng, wet=0.28, rt60=0.65, lp_hz=4400.0)

    if sound_id == "rift_catalyst_ignite":
        # Vacuum dimensional tear -> sub-bomb implosion -> shimmering portal rift chord
        tear_idx = int(0.28 * n)
        suck = _reverse_suction(n, 0, tear_idx, rng, f_lo=180.0, f_hi=4800.0) * 0.95
        bomb = _sub_bomb(t, tear_idx, f_start=170.0, f_end=30.0, drop_tau=0.07, sustain_tau=dur * 0.50, punch_gain=0.65)
        rt = np.arange(n - tear_idx, dtype=np.float64) / SAMPLE_RATE
        portal = _modal_bank(
            rt,
            bf * 1.25,
            (0.5, 1.0, 1.414, 2.0, 2.828, 4.0),
            (0.55, 0.48, 0.40, 0.32, 0.24, 0.16),
            (0.65, 0.85, 0.62, 0.48, 0.34, 0.20),
            rng,
        )
        wave = suck + bomb * 1.25
        wave[tear_idx:] += portal * 0.90
        return _abyssal_reverb(np.tanh(wave * 1.3), rng, predelay_ms=16.0, wet=0.34, rt60=0.92, lp_hz=5200.0, shimmer=0.28)

    if sound_id == "overmind_crown_surge":
        # Sovereign Overmind decree: crystalline crown chime + imperial psionic choir + sub-shockwave purge
        imp_idx = int(0.14 * n)
        bomb = _sub_bomb(t, imp_idx, f_start=165.0, f_end=29.0, drop_tau=0.07, sustain_tau=dur * 0.52, punch_gain=0.68)
        f_cr = bf * (1.08 - 0.22 * (t / dur))
        choir = _eldritch_choir(t, f_cr, rng, voices=5, dissonance="minor_lament", breath_mix=0.22, formant_shift=formant_shift) * adsr_envelope(n, 0.08, 0.20, 0.58, 0.38)
        crown = _modal_bank(
            t,
            bf * 2.2,
            (1.0, 1.498, 2.0, 3.0, 4.18),
            (0.50, 0.42, 0.34, 0.25, 0.18),
            (0.75, 0.60, 0.48, 0.32, 0.20),
            rng,
        )
        raw = np.tanh((bomb * 1.25 + choir * 1.10 + crown * 0.85) * 1.3)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.34, rt60=0.96, lp_hz=5800.0, shimmer=0.30)

    if sound_id == "command_staff_mark":
        # Carved wooden-copper staff ferrule tap -> crisp amethyst waypoint resonance chime
        tap_s = int(rng.uniform(0.01, 0.08) * n)
        rem_t = max(32, n - tap_s)
        tap = np.zeros(n, dtype=np.float64)
        tap[tap_s:] = wooden_chitin_knock(rem_t, bf * rng.uniform(0.92, 1.12), rng, decay=0.055, hollowness=0.65)
        lock_s = min(n - 48, tap_s + int(rng.uniform(0.07, 0.14) * SAMPLE_RATE))
        lt = np.arange(n - lock_s, dtype=np.float64) / SAMPLE_RATE
        lock = np.zeros(n, dtype=np.float64)
        lock[lock_s:] = amethyst_copper_chime(lt, bf * 1.45, rng, decay=0.12, crystal_mix=0.72) * 0.85
        return _cavern_reverb(_sos_highpass(tap + lock, 180.0), rng, wet=0.08, rt60=0.26, lp_hz=7600.0)

    if sound_id == "ui_codex_tab":
        # Tactile parchment leaf flick + warm dry hollow wooden button knock (low fatigue)
        onset = int(rng.uniform(0.01, 0.05) * n)
        rem = max(32, n - onset)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        leaf = _swept_resonant_filter(pink[onset:], 1100.0, 3800.0, q=1.8) * exp_decay(rt, 0.036) * 0.65
        tap = wooden_chitin_knock(rem, bf * rng.uniform(0.94, 1.08), rng, decay=0.038, hollowness=0.68) * 0.85
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = leaf + tap
        return _cavern_reverb(_sos_highpass(wave, 220.0), rng, wet=0.04, rt60=0.14, lp_hz=7800.0)

    if sound_id == "ui_action_confirm":
        # Two-step crisp wooden-copper latch -> warm ascending amethyst fifth chime
        wave = np.zeros(n, dtype=np.float64)
        s1 = int(rng.uniform(0.01, 0.04) * n)
        s2 = min(n - 64, s1 + int(rng.uniform(0.055, 0.085) * SAMPLE_RATE))
        rem1 = max(32, n - s1)
        rem2 = max(32, n - s2)
        t1 = np.arange(rem1, dtype=np.float64) / SAMPLE_RATE
        t2 = np.arange(rem2, dtype=np.float64) / SAMPLE_RATE
        f1 = bf * rng.uniform(0.86, 0.92)
        f2 = bf * rng.uniform(1.30, 1.38)
        latch = wooden_chitin_knock(rem1, f1 * 1.4, rng, decay=0.032, hollowness=0.55) * 0.72
        tone1 = amethyst_copper_chime(t1, f1, rng, decay=0.075, crystal_mix=0.45) * 0.65
        chime2 = amethyst_copper_chime(t2, f2, rng, decay=0.115, crystal_mix=0.72) * 0.95
        wave[s1:] += latch + tone1
        wave[s2:] += chime2
        return _cavern_reverb(_sos_highpass(np.tanh(wave * 1.2), 240.0), rng, wet=0.05, rt60=0.18, lp_hz=8200.0)

    if sound_id == "resonant_elixir_drink":
        # Bioluminescent subterranean elixir gulp: viscous liquid cloud -> rising harmonic crystalline resonance
        gulps = granular_bio_cloud(n, rng, freq_range=(260.0, 1700.0), density_hz=44.0, grain_ms=26.0, organic_mode="viscous") * adsr_envelope(n, 0.04, 0.28, 0.45, 0.36) * 0.85
        shimmer_s = int(0.24 * n)
        rem = max(32, n - shimmer_s)
        st = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        chime = (
            karplus_strong_chitin(rem, bf, rng, damping=0.992, brightness=0.76, stiffness=0.09, excitation="pluck") * 0.60
            + karplus_strong_chitin(rem, bf * 1.498, rng, damping=0.992, brightness=0.78, stiffness=0.10, excitation="pluck") * 0.48
        ) * adsr_envelope(rem, 0.08, 0.22, 0.50, 0.42)
        swell = _swept_resonant_filter(pink[shimmer_s:], 420.0, 2800.0, q=2.6) * exp_decay(st, 0.24) * 0.35
        wave = gulps
        wave[shimmer_s:] += chime + swell
        return fdn4_hadamard_diffuser(np.tanh(wave * 1.14), rng, rt60=0.52, wet=0.24, damping_hz=6400.0)

    raise ValueError(f"Unhandled item/projectile sound_id in item_synth: {sound_id}")
