"""Dedicated multi-stage synthesizers for all 30 system, tactical AI, homeostasis, biome & HUD alert sound events.

Covers the 16 non-boss system/hive events plus the 14 biome ambient loops/moods/additions,
positional soundscape loops, and HUD adaptation alert cues.
"""

from __future__ import annotations

import numpy as np

from scripts.sound_studio.block_synth import (
    _bounce_train,
    _micro_arc_zaps,
    _modal_bank,
)
from scripts.sound_studio.boss_synth import (
    _abyssal_reverb,
    _brass_braaam,
    _eldritch_choir,
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
    _sos_lowpass,
    _swept_resonant_filter,
    _wet_squelch_layer,
    adsr_envelope,
    amethyst_copper_chime,
    articulated_vocal_tract,
    exp_decay,
    fdn4_hadamard_diffuser,
    foley_particle_crackle,
    fold_seamless_loop,
    fungal_cap_resonance,
    granular_bio_cloud,
    karplus_strong_chitin,
    modal_dispersed_impact,
    modal_resonator_bank,
    morphing_formant_tract,
    mycelial_hyphae_tear,
    spore_sac_burst,
    wooden_chitin_knock,
)

SYSTEM_ENV_IDS: frozenset[str] = frozenset(
    {
        # System, Hive Mind, Tactical AI & Homeostasis (17; boss_phase_shift, fungal_tide_horn, colony_raid_horn are in BOSS_CINEMATIC_IDS)
        "hive_mutate_tier2",
        "synaptic_pulse",
        "suppressor_discharge",
        "squad_desync",
        "corrosive_jam",
        "immune_surge",
        "mob_promote_mutated",
        "mob_promote_elite",
        "mob_dawn_retreat",
        "infected_mob_convert",
        "mycorrhizal_tremor",
        "homeostasis_heartbeat",
        "antibody_cyst_encapsulate",
        "acid_rain_sizzle",
        "necrophagy_feast",
        "swarm_flank_call",
        "primordial_presence_pulse",
        "brute_wall_stun",
        "pyro_shroom_defuse",
        # Biome Soundscapes (9)
        "biome_caverns_loop",
        "biome_caverns_mood",
        "biome_caverns_additions",
        "biome_sanctum_loop",
        "biome_sanctum_mood",
        "biome_sanctum_additions",
        "biome_forest_loop",
        "biome_forest_mood",
        "biome_forest_additions",
        # Positional Loops, Adaptive Beds, Micro-Emitters & Hive Alerts (14)
        "fungal_tide_ambience",
        "purification_pylon_hum",
        "sovereign_nexus_hum",
        "boss_combat_tension_bed",
        "sinkhole_updraft_loop",
        "hive_tension_bed_low",
        "hive_swarm_combat_bed",
        "cavern_spore_drip",
        "subterranean_hyphal_creak",
        "deep_mycelium_whisper",
        "sanctum_harmonic_chime",
        "hive_trait_adapt",
        "hive_synergy_unlock",
        "hive_threat_milestone",
    }
)


def synthesize_system_env_sound(
    sound_id: str,
    *,
    duration: float,
    base_freq: float,
    mod_freq: float,
    rng: np.random.Generator,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a bespoke system, tactical AI, homeostasis, biome ambient or HUD alert waveform."""
    dur = max(0.18, float(duration))
    n = max(64, int(round(dur * SAMPLE_RATE)))
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)
    bf = max(32.0, float(base_freq) * float(rng.uniform(0.95, 1.05)))
    mf = max(0.8, float(mod_freq) * float(rng.uniform(0.90, 1.10)))
    pjit = float(rng.uniform(0.0, 2.0 * np.pi))

    # =========================================================================
    # 1. SYSTEM, HIVE MIND, TACTICAL AI & HOMEOSTASIS (16)
    # =========================================================================
    if sound_id == "hive_mutate_tier2":
        # Ascending neural mutation shriek + dissonant tritone choir + sub-bass evolution thump
        u = t / dur
        f_mut = bf * (0.78 + 0.95 * (u ** 1.15))
        choir = _eldritch_choir(t, f_mut, rng, voices=4, dissonance="tritone", breath_mix=0.25, formant_shift=formant_shift)
        bomb = _sub_bomb(t, int(0.15 * n), f_start=155.0, f_end=32.0, drop_tau=0.075, sustain_tau=dur * 0.45)
        zaps = _micro_arc_zaps(n, rng, 6, lo_hz=3000.0, hi_hz=12000.0, spread=(0.10, 0.85)) * 0.55
        raw = np.tanh((choir * adsr_envelope(n, 0.08, 0.20, 0.58, 0.34) * 1.2 + bomb + zaps) * 1.3)
        return _abyssal_reverb(raw, rng, predelay_ms=16.0, wet=0.30, rt60=0.82, lp_hz=5400.0, shimmer=0.22)

    if sound_id == "synaptic_pulse":
        # Neural relay network pulse: deep sub throb + swept crystal-vein harmonic + micro-spark
        env = np.sin(np.pi * t / dur) ** 1.2
        f_p = bf * (0.92 + 0.22 * np.sin(np.pi * t / dur))
        core = _fm_tone(t, f_p, f_p * 0.5, 1.6, pjit) * 0.75 + np.sin(2.0 * np.pi * (bf * 0.5) * t) * 0.50
        ring = _modal_bank(t, bf * 1.414, (1.0, 1.5, 2.32), (0.18, 0.12, 0.08), (0.55, 0.38, 0.22), rng)
        return _cavern_reverb((core + ring) * env, rng, wet=0.22, rt60=0.48, lp_hz=4800.0)

    if sound_id == "suppressor_discharge":
        # Copper coil wind-up + crystal crackle -> heavy stone shockwave -> resonant amethyst-bronze discharge triad
        imp = int(rng.uniform(0.13, 0.25) * n)
        wave = np.zeros(n, dtype=np.float64)
        ct = t[:imp]
        f_up_lo = rng.uniform(210.0, 320.0)
        f_up_hi = rng.uniform(1180.0, 1580.0)
        windup = _swept_resonant_filter(pink[:imp] * 0.7 + white[:imp] * 0.4, f_up_lo, f_up_hi, q=2.8)
        wave[:imp] = windup * (ct / max(1e-4, ct[-1])) * 0.85 + foley_particle_crackle(imp, rng, count=4) * 0.38
        bomb = _sub_bomb(t, imp, f_start=rng.uniform(145.0, 185.0), f_end=35.0, drop_tau=rng.uniform(0.05, 0.08), sustain_tau=dur * 0.45)
        rt = np.arange(n - imp, dtype=np.float64) / SAMPLE_RATE
        triad_bf = bf * rng.uniform(0.91, 1.10)
        emp_chime = (
            amethyst_copper_chime(rt, triad_bf, rng, decay=0.22, crystal_mix=0.65) * 0.85
            + _modal_bank(rt, triad_bf * 1.498, (1.0, 1.5, 2.32), (0.18, 0.12, 0.07), (0.55, 0.38, 0.22), rng) * 0.55
        )
        snaps = foley_particle_crackle(n, rng, lo_hz=2600.0, hi_hz=9600.0, count=6) * 0.52
        wave[imp:] += bomb[imp:] * 1.15 + emp_chime
        return _cavern_reverb(np.tanh((wave + snaps) * 1.25), rng, wet=0.14, rt60=0.36, lp_hz=7200.0)

    if sound_id == "squad_desync":
        # Severed neural link: shattering crystal link + stepped downward pitch-glitch + static fizz
        shatter = _modal_bank(t, bf * 2.1, (1.0, 1.414, 2.32, 3.85), (0.06, 0.045, 0.03, 0.02), (0.8, 0.6, 0.4, 0.25), rng)
        steps = np.floor(6.0 * (t / dur)) / 6.0
        f_glitch = np.maximum(65.0, bf * (1.35 - 0.95 * steps))
        glitch = _fm_tone(t, f_glitch, 42.0, 2.2, pjit) * adsr_envelope(n, 0.03, 0.20, 0.48, 0.38) * 0.85
        zaps = _micro_arc_zaps(n, rng, 7, lo_hz=2800.0, hi_hz=12500.0, spread=(0.04, 0.88)) * 0.65
        return _cavern_reverb(shatter * 0.85 + glitch + zaps, rng, wet=0.16, rt60=0.36, lp_hz=6800.0)

    if sound_id == "corrosive_jam":
        # Grinding jammed steel gear clanks + aggressive acid corrosion sizzle + wet bubble clog
        gears = np.zeros(n, dtype=np.float64)
        for idx, g_frac in enumerate((0.02, 0.22, 0.38)):
            pos = int(g_frac * n)
            gt = np.arange(n - pos, dtype=np.float64) / SAMPLE_RATE
            gears[pos:] += _modal_bank(gt, 520.0 - 70.0 * idx, (1.0, 1.59, 2.92), (0.045, 0.03, 0.018), (0.8, 0.55, 0.32), rng)
        sizzle = _sos_bandpass(white, 2600.0, 12000.0) * adsr_envelope(n, 0.05, 0.22, 0.52, 0.34) * 0.75
        clog = _wet_squelch_layer(n, rng, bf * 1.2, 6, intensity=0.95) * adsr_envelope(n, 0.08, 0.22, 0.50, 0.35)
        return _cavern_reverb(np.tanh((gears + sizzle + clog) * 1.25), rng, wet=0.12, rt60=0.28, lp_hz=7400.0)

    if sound_id == "immune_surge":
        # Dimensional immune response eruption: arterial double-thump -> deep visceral roar + spore wind
        beats = _sub_bomb(t, int(0.03 * n), f_start=145.0, f_end=30.0, drop_tau=0.06, sustain_tau=0.35)
        beats += _sub_bomb(t, int(0.24 * n), f_start=168.0, f_end=28.0, drop_tau=0.075, sustain_tau=dur * 0.50) * 1.15
        f_roar = bf * (1.06 - 0.25 * (t / dur) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit))
        roar = _apply_formant_bank(_glottal_sub_source(t, f_roar, rng, harmonics=12, sub_weight=0.92, rattle_hz=21.0), [(130.0 * formant_shift, 2.4, 1.15), (360.0 * formant_shift, 3.0, 0.9), (980.0, 3.6, 0.55)])
        raw = np.tanh((beats * 1.1 + roar * adsr_envelope(n, 0.12, 0.20, 0.58, 0.32) * 1.15) * 1.3)
        return _abyssal_reverb(raw, rng, predelay_ms=16.0, wet=0.30, rt60=0.82, lp_hz=3200.0)

    if sound_id == "mob_promote_mutated":
        # Rapid carapace bone expansion crackle -> ascending amber harmonic surge
        cracks = _chitin_modal_hits(n, rng, bf * 1.5, 5, 0.035, spread_frac=0.50) * 0.75
        f_up = bf * (0.75 + 0.85 * ((t / dur) ** 0.9))
        surge = (_fm_tone(t, f_up, f_up * 0.5, 1.7, pjit) * 0.75 + np.sin(2.0 * np.pi * np.cumsum(f_up * 1.5) / SAMPLE_RATE) * 0.45) * adsr_envelope(n, 0.06, 0.22, 0.55, 0.32)
        return _cavern_reverb(cracks + surge, rng, wet=0.20, rt60=0.45, lp_hz=5400.0)

    if sound_id == "mob_promote_elite":
        # Apex ascension: sub-bass shock thump + crimson brass swell + shimmering crystal crown chord
        bomb = _sub_bomb(t, int(0.08 * n), f_start=150.0, f_end=32.0, drop_tau=0.065, sustain_tau=dur * 0.48)
        f_up = bf * (0.68 + 1.15 * ((t / dur) ** 1.1))
        brass = _brass_braaam(t, f_up, rng, detune_cents=16.0, sub_oct=0.80, shimmer_mix=0.25, formant_shift=formant_shift) * adsr_envelope(n, 0.08, 0.20, 0.58, 0.34)
        crown = _modal_bank(t, bf * 2.4, (1.0, 1.498, 2.0, 3.0), (0.35, 0.28, 0.22, 0.16), (0.65, 0.5, 0.38, 0.24), rng)
        return _cavern_reverb(np.tanh((bomb + brass * 0.95 + crown * 0.75) * 1.3), rng, wet=0.26, rt60=0.60, lp_hz=5600.0)

    if sound_id == "mob_dawn_retreat":
        # Rapid digging claw scratches + downward soil/mycelium sink + muffled underground thud
        dig = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 8, start_frac=0.02, end_frac=0.72, accelerate=True):
            dlen = min(n - pos, int(SAMPLE_RATE * 0.036))
            dt = np.arange(dlen, dtype=np.float64) / SAMPLE_RATE
            dig[pos : pos + dlen] += _sos_bandpass(rng.normal(0, 1, dlen), 350.0, 2200.0) * exp_decay(dt, 0.015) * 0.85
        sink = _swept_resonant_filter(brown * 0.7 + pink * 0.5, 650.0, 95.0, q=2.4) * adsr_envelope(n, 0.06, 0.25, 0.45, 0.38) * 1.1
        return _cavern_reverb(_sos_lowpass(dig + sink, 2800.0), rng, wet=0.16, rt60=0.36, lp_hz=2200.0)

    if sound_id == "infected_mob_convert":
        # Rapid host convulsions -> wet fungal husk rupture -> newly assimilated sporeling/stalker shriek
        convulse = _wet_squelch_layer(n, rng, bf * 1.2, 8, intensity=1.15) * exp_decay(t, dur * 0.55)
        crack = _chitin_modal_hits(n, rng, bf * 2.0, 5, 0.038, spread_frac=0.45) * 0.80
        cry_s = int(0.36 * n)
        ct = np.arange(n - cry_s, dtype=np.float64) / SAMPLE_RATE
        f_cry = bf * (0.90 + 0.75 * np.sin(np.pi * ct / max(1e-4, ct[-1])))
        cry = _apply_formant_bank(_glottal_sub_source(ct, f_cry, rng, harmonics=10, sub_weight=0.65, rattle_hz=28.0), [(bf * 1.5 * formant_shift, 3.0, 1.1), (bf * 3.2, 3.6, 0.85)])
        wave = convulse + crack
        wave[cry_s:] += cry * adsr_envelope(len(ct), 0.08, 0.20, 0.52, 0.36)
        return _cavern_reverb(np.tanh(wave * 1.3), rng, wet=0.22, rt60=0.48, lp_hz=5200.0)

    if sound_id == "mycorrhizal_tremor":
        # Subterranean root-network acoustic tremor: low seismic vibration + muffled crystalline root pulse
        tremor_mod = 0.45 + 0.55 * np.sin(2.0 * np.pi * mf * t + pjit)
        sub = (np.sin(2.0 * np.pi * (bf * 0.35) * t + pjit) * 0.75 + _sos_bandpass(brown, 38.0, 220.0) * 0.85) * tremor_mod
        root_ping = _modal_bank(t, bf * 1.2, (1.0, 1.498, 2.32), (0.12, 0.08, 0.05), (0.45, 0.30, 0.18), rng)
        return _cavern_reverb((sub + root_ping) * adsr_envelope(n, 0.10, 0.22, 0.52, 0.34), rng, wet=0.24, rt60=0.52, lp_hz=2400.0)

    if sound_id == "homeostasis_heartbeat":
        # Deep arterial lub-dub of the living dimension (distinct timing & vascular rush per take)
        wave = np.zeros(n, dtype=np.float64)
        lub_t = float(rng.uniform(0.03, 0.08))
        dub_t = dur * float(rng.uniform(0.36, 0.48))
        for beat_sec, gain, f_mult in ((lub_t, 1.0, 1.0), (dub_t, 0.82, 1.18)):
            idx = min(n - 48, int(beat_sec * SAMPLE_RATE))
            blen = min(n - idx, int(SAMPLE_RATE * 0.26))
            bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
            f_b = max(34.0, bf * f_mult) * (0.55 + 1.35 * exp_decay(bt, 0.042))
            thump = np.sin(2.0 * np.pi * np.cumsum(f_b) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(bt, 0.078)
            valve_rush = _sos_bandpass(pink[idx : idx + blen], 60.0, 380.0) * exp_decay(bt, 0.085) * 0.50
            wave[idx : idx + blen] += (thump + valve_rush) * gain
        return _cavern_reverb(np.tanh(wave * 1.45), rng, wet=0.28, rt60=0.58, lp_hz=1350.0)

    if sound_id == "antibody_cyst_encapsulate":
        # Pressurized mucus sac inflation + wet rubbery seal suction around foreign block
        inflate = np.sin(2.0 * np.pi * np.cumsum(np.linspace(bf * 0.45, bf * 1.25, n)) / SAMPLE_RATE + pjit) * np.sin(np.pi * t / dur) * 0.85
        seal = _wet_squelch_layer(n, rng, bf * 1.15, 6, intensity=1.15) * adsr_envelope(n, 0.05, 0.22, 0.52, 0.36)
        return _cavern_reverb(np.tanh((inflate + seal) * 1.3), rng, wet=0.16, rt60=0.38, lp_hz=3200.0)

    if sound_id == "acid_rain_sizzle":
        # Ceiling acid droplet splatters + continuous high-frequency chemical corrosion hiss
        drops = _wet_squelch_layer(n, rng, bf * 1.4, 5, intensity=0.75)
        sizzle = _sos_bandpass(white, 2800.0, 13000.0) * (0.55 + 0.45 * np.sin(2.0 * np.pi * mf * t + pjit)) * 0.85
        melt = _swept_resonant_filter(pink, 1800.0, 520.0, q=2.8) * 0.55
        return _cavern_reverb((drops + sizzle + melt) * adsr_envelope(n, 0.05, 0.20, 0.55, 0.32), rng, wet=0.10, rt60=0.24, lp_hz=8200.0)

    if sound_id == "necrophagy_feast":
        # Mandible carrion crunch -> wet biomass tearing -> ascending vitality absorption hum
        crunch = _chitin_modal_hits(n, rng, bf * 1.6, 5, 0.038, spread_frac=0.65) * 0.80
        tear = _wet_squelch_layer(n, rng, bf * 1.15, 7, intensity=1.10) * adsr_envelope(n, 0.04, 0.22, 0.52, 0.34)
        absorb = np.sin(2.0 * np.pi * np.cumsum(np.linspace(bf * 0.8, bf * 1.45, n)) / SAMPLE_RATE + pjit) * adsr_envelope(n, 0.20, 0.25, 0.45, 0.35) * 0.45
        return _cavern_reverb(np.tanh((crunch + tear + absorb) * 1.3), rng, wet=0.14, rt60=0.34, lp_hz=5200.0)

    if sound_id == "swarm_flank_call":
        # Urgent 2-note tactical pincer-attack call (short staccato chirp -> higher sustained command trill)
        wave = np.zeros(n, dtype=np.float64)
        n1_end = int(0.32 * n)
        t1 = t[:n1_end]
        c1 = _fm_tone(t1, bf * 1.05, mf * 3.0, 2.2, pjit) * np.sin(np.linspace(0, np.pi, n1_end))
        n2_start = int(0.38 * n)
        t2 = t[: n - n2_start]
        f2 = bf * 1.42 * (1.0 + 0.08 * np.sin(2.0 * np.pi * (mf * 1.5) * t2))
        c2 = _fm_tone(t2, f2, mf * 3.5, 2.4, pjit + 1.0) * adsr_envelope(len(t2), 0.06, 0.20, 0.55, 0.35)
        wave[:n1_end] = c1
        wave[n2_start:] = c2
        tract = _apply_formant_bank(wave, [(bf * 1.3 * formant_shift, 3.4, 1.1), (bf * 2.8 * formant_shift, 4.0, 0.85)])
        clicks = _chitin_modal_hits(n, rng, bf * 2.2, 4, 0.025, spread_frac=0.75) * 0.55
        return _cavern_reverb(_sos_highpass(tract + clicks, 220.0), rng, wet=0.16, rt60=0.36, lp_hz=6800.0)

    # =========================================================================
    # 2. BIOME AMBIENT SOUNDSCAPES (9: 3 biomes x [loop, mood, additions])
    # =========================================================================
    if sound_id == "biome_caverns_loop":
        # Deep Mycelium Caverns 5.5s seamless loop: subterranean draft + integer-locked mycorrhizal drone + drips
        cf = int(SAMPLE_RATE * 0.30)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        ext_brown = _brown_noise(rng, ext_n)
        lfo_w = max(1.0, round(0.35 * dur)) / dur
        f0 = max(36.0, 2.0 * round(0.25 * bf * dur) / dur)
        sub_drone = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.55
            + np.sin(2.0 * np.pi * (f0 * 1.5) * ext_t) * 0.35
        ) * (0.88 + 0.12 * np.sin(2.0 * np.pi * lfo_w * ext_t + pjit))
        wind = _sos_bandpass(ext_brown * 0.7 + ext_pink * 0.5, 95.0, 420.0) * (0.76 + 0.24 * np.sin(2.0 * np.pi * lfo_w * ext_t + pjit)) * 0.85
        drips = np.zeros(ext_n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 5, start_frac=0.14, end_frac=0.82, accelerate=False):
            dlen = min(ext_n - pos, int(SAMPLE_RATE * 0.09))
            dt = np.arange(dlen, dtype=np.float64) / SAMPLE_RATE
            f_d = rng.uniform(680.0, 1350.0) * (0.85 + 0.45 * (dt / max(1e-4, dt[-1])))
            drips[pos : pos + dlen] += np.sin(2.0 * np.pi * np.cumsum(f_d) / SAMPLE_RATE) * exp_decay(dt, 0.028) * 0.35
        dry = sub_drone + wind + drips
        preroll = min(ext_n // 2, int(SAMPLE_RATE * 1.0))
        reverb_in = np.concatenate([dry[-preroll:], dry])
        raw = _abyssal_reverb(reverb_in, rng, predelay_ms=22.0, wet=0.38, rt60=0.95, lp_hz=2400.0)[
            preroll : preroll + ext_n
        ]
        return fold_seamless_loop(raw, n, crossfade_ms=300.0)

    if sound_id == "biome_caverns_mood":
        # Ominous tectonic cavern groan + distant hollow chitin resonance
        f_m = bf * (1.05 - 0.28 * (t / dur) + 0.04 * np.sin(2.0 * np.pi * 1.8 * t + pjit))
        groan = _brass_braaam(t, f_m, rng, detune_cents=18.0, sub_oct=0.95, shimmer_mix=0.10, formant_shift=formant_shift)
        chitin_echo = _chitin_modal_hits(n, rng, bf * 3.8, 4, 0.065, spread_frac=0.68) * 0.45
        raw = (groan * 0.85 + chitin_echo) * adsr_envelope(n, 0.18, 0.22, 0.58, 0.36)
        return _abyssal_reverb(_sos_lowpass(raw, 2200.0), rng, predelay_ms=24.0, wet=0.42, rt60=1.15, lp_hz=1850.0)

    if sound_id == "biome_caverns_additions":
        # Close-up spore trickle, damp root stretch & resonant cave droplet
        stretch = _swept_resonant_filter(pink, 220.0, 880.0, q=3.0) * adsr_envelope(n, 0.10, 0.24, 0.45, 0.35) * 0.65
        drops = _wet_squelch_layer(n, rng, bf * 1.4, 4, intensity=0.75)
        clicks = _chitin_modal_hits(n, rng, bf * 2.4, 3, 0.035, spread_frac=0.72) * 0.45
        return _cavern_reverb(stretch + drops + clicks, rng, wet=0.28, rt60=0.65, lp_hz=3800.0)

    if sound_id == "biome_sanctum_loop":
        # Hive Heart Sanctum 5.5s seamless loop: arterial heart-chamber pulse + synaptic organ drone
        cf = int(SAMPLE_RATE * 0.16)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        organ = (
            np.sin(2.0 * np.pi * (bf * 0.5) * ext_t + pjit) * 0.52
            + np.sin(2.0 * np.pi * bf * ext_t) * 0.44
            + np.sin(2.0 * np.pi * (bf * 1.189) * ext_t + 0.8) * 0.34
            + np.sin(2.0 * np.pi * (bf * 1.498) * ext_t + 1.7) * 0.28
        ) * (0.74 + 0.26 * np.sin(2.0 * np.pi * 0.55 * ext_t + pjit))
        pulse = np.zeros(ext_n, dtype=np.float64)
        for p_frac in (0.14, 0.24, 0.62, 0.72):
            pos = int(p_frac * n)
            plen = min(ext_n - pos, int(SAMPLE_RATE * 0.24))
            pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
            f_b = 58.0 * (0.6 + 1.1 * exp_decay(pt, 0.04))
            pulse[pos : pos + plen] += np.sin(2.0 * np.pi * np.cumsum(f_b) / SAMPLE_RATE) * exp_decay(pt, 0.085) * 0.65
        raw = _abyssal_reverb(organ + pulse, rng, predelay_ms=20.0, wet=0.38, rt60=0.98, lp_hz=3200.0, shimmer=0.16)
        return fold_seamless_loop(raw, n, crossfade_ms=160.0)

    if sound_id == "biome_sanctum_mood":
        # Haunted synaptic choir murmur + monolith funeral bell toll
        f_ch = bf * (0.95 + 0.18 * np.sin(np.pi * t / dur))
        choir = _eldritch_choir(t, f_ch, rng, voices=4, dissonance="minor_lament", breath_mix=0.32, formant_shift=formant_shift)
        bell = _modal_bank(t, bf * 0.75, (0.5, 1.0, 1.189, 1.498, 2.0), (1.1, 0.9, 0.7, 0.5, 0.35), (0.55, 0.75, 0.50, 0.40, 0.25), rng) * 0.65
        raw = (choir * 0.90 + bell) * adsr_envelope(n, 0.16, 0.22, 0.58, 0.36)
        return _abyssal_reverb(raw, rng, predelay_ms=22.0, wet=0.42, rt60=1.18, lp_hz=4600.0, shimmer=0.25)

    if sound_id == "biome_sanctum_additions":
        # Neural amber vein zap + quartz crystal hum + incubator sac bubble
        crystal = _modal_bank(t, bf * 1.4, (1.0, 2.32, 3.85), (0.35, 0.24, 0.15), (0.65, 0.45, 0.28), rng)
        zaps = _micro_arc_zaps(n, rng, 4, lo_hz=3000.0, hi_hz=11500.0, spread=(0.10, 0.82)) * 0.48
        sac = _wet_squelch_layer(n, rng, bf * 0.75, 3, intensity=0.55)
        return _cavern_reverb((crystal + zaps + sac) * adsr_envelope(n, 0.08, 0.22, 0.55, 0.34), rng, wet=0.28, rt60=0.65, lp_hz=5400.0)

    if sound_id == "biome_forest_loop":
        # Latent Spore Forest 5.5s seamless loop: warm breeze through giant fungal caps + soft bioluminescent chimes
        cf = int(SAMPLE_RATE * 0.16)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        breeze = _swept_resonant_filter(ext_pink, 240.0, 1150.0, q=2.6) * (0.62 + 0.38 * np.sin(2.0 * np.pi * 0.40 * ext_t + pjit)) * 0.75
        pad = (
            np.sin(2.0 * np.pi * bf * ext_t + pjit) * 0.45
            + np.sin(2.0 * np.pi * (bf * 1.498) * ext_t + 1.1) * 0.35
            + np.sin(2.0 * np.pi * (bf * 2.0) * ext_t + 2.2) * 0.22
        )
        chimes = np.zeros(ext_n, dtype=np.float64)
        for idx, pos in enumerate(_bounce_train(n, rng, 4, start_frac=0.14, end_frac=0.84, accelerate=False)):
            ct = np.arange(ext_n - pos, dtype=np.float64) / SAMPLE_RATE
            ratio = (1.25, 1.50, 1.875, 2.25)[idx % 4]
            chimes[pos:] += _modal_bank(ct, bf * 2.0 * ratio, (1.0, 2.0, 3.01), (0.38, 0.24, 0.14), (0.42, 0.25, 0.14), rng)
        raw = _abyssal_reverb(breeze + pad + chimes, rng, predelay_ms=18.0, wet=0.34, rt60=0.88, lp_hz=4800.0, shimmer=0.18)
        return fold_seamless_loop(raw, n, crossfade_ms=160.0)

    if sound_id == "biome_forest_mood":
        # Resonant hollow wood-cap horn call echoing across the spore canopy
        f_call = bf * (0.92 + 0.24 * np.sin(np.pi * t / dur) - 0.12 * (t / dur))
        horn = _apply_formant_bank(_glottal_sub_source(t, f_call, rng, harmonics=8, sub_weight=0.65, rattle_hz=14.0), [(260.0 * formant_shift, 2.8, 1.1), (640.0 * formant_shift, 3.4, 0.85)])
        shimmer = _modal_bank(t, bf * 2.0, (1.0, 1.5, 2.25, 3.0), (0.7, 0.55, 0.4, 0.28), (0.55, 0.42, 0.28, 0.16), rng) * 0.65
        raw = (horn * 0.85 + shimmer) * adsr_envelope(n, 0.18, 0.22, 0.56, 0.36)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.38, rt60=1.05, lp_hz=4200.0, shimmer=0.20)

    if sound_id == "biome_forest_additions":
        # Gentle spore puff + tendril rustle + distant fungal insect chirp
        rustle = _swept_resonant_filter(pink, 380.0, 1850.0, q=2.5) * adsr_envelope(n, 0.10, 0.25, 0.45, 0.35) * 0.70
        chirp_s = int(0.35 * n)
        clen = min(n - chirp_s, int(SAMPLE_RATE * 0.28))
        ct = np.arange(clen, dtype=np.float64) / SAMPLE_RATE
        chirp = np.zeros(n, dtype=np.float64)
        chirp[chirp_s : chirp_s + clen] = _fm_tone(ct, bf * 1.6 * (1.0 + 0.15 * np.sin(2.0 * np.pi * 16.0 * ct)), bf * 0.8, 1.4) * np.sin(np.linspace(0, np.pi, clen)) * 0.55
        return _cavern_reverb(rustle + chirp, rng, wet=0.24, rt60=0.55, lp_hz=5600.0)

    # =========================================================================
    # 3. POSITIONAL LOOPS & HIVE ADAPTATION ALERTS (5)
    # =========================================================================
    if sound_id == "fungal_tide_ambience":
        # Swelling violet spore-storm gale + ominous distant swarm drone (seamless loop)
        cf = int(SAMPLE_RATE * 0.28)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        ext_brown = _brown_noise(rng, ext_n)
        lfo_g = max(1.0, round(0.65 * dur)) / dur
        lfo_d = max(1.0, round(0.80 * dur)) / dur
        gale = _swept_resonant_filter(ext_pink * 0.7 + ext_brown * 0.5, 180.0, 1250.0, q=3.2) * (0.72 + 0.28 * np.sin(2.0 * np.pi * lfo_g * ext_t + pjit)) * 0.95
        f_dr = bf * (0.98 + 0.04 * np.sin(2.0 * np.pi * lfo_d * ext_t + pjit))
        drone = _brass_braaam(ext_t, f_dr, rng, detune_cents=16.0, sub_oct=0.85, shimmer_mix=0.14, formant_shift=formant_shift) * 0.65
        raw = _abyssal_reverb(gale + drone, rng, predelay_ms=18.0, wet=0.34, rt60=0.88, lp_hz=2800.0)
        return fold_seamless_loop(raw, n, crossfade_ms=280.0)

    if sound_id == "purification_pylon_hum":
        # Clean sterile containment field hum: continuous integer-locked harmonic triad + seamless fold
        cf = int(SAMPLE_RATE * 0.14)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        f0 = max(90.0, round((bf * 0.5) * dur) / dur)
        lfo_hz = max(1.0, round(2.2 * dur) / dur)
        field = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.58
            + np.sin(2.0 * np.pi * (f0 * 1.5) * ext_t) * 0.38
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t) * 0.24
            + np.sin(2.0 * np.pi * (f0 * 3.0) * ext_t) * 0.12
        ) * (0.84 + 0.16 * np.sin(2.0 * np.pi * lfo_hz * ext_t + pjit))
        ion = _sos_bandpass(ext_pink, 2200.0, 6400.0) * 0.22
        raw = _cavern_reverb(_sos_highpass(field + ion, 150.0), rng, wet=0.08, rt60=0.22, lp_hz=7600.0)
        return fold_seamless_loop(raw, n, crossfade_ms=140.0)

    if sound_id == "hive_trait_adapt":
        # Bio-Scanner telemetry alert blip -> sinister organic-crystal adaptation chord (minor-second -> tritone)
        blip_len = min(n // 4, int(SAMPLE_RATE * 0.085))
        wave = np.zeros(n, dtype=np.float64)
        bt = np.arange(blip_len, dtype=np.float64) / SAMPLE_RATE
        wave[:blip_len] = np.sin(2.0 * np.pi * 1180.0 * bt) * np.sin(np.linspace(0, np.pi, blip_len)) * 0.72
        adapt_s = int(0.14 * n)
        at = np.arange(n - adapt_s, dtype=np.float64) / SAMPLE_RATE
        chord = _modal_bank(at, bf, (1.0, 1.189, 1.414, 2.32), (0.34, 0.28, 0.22, 0.15), (0.75, 0.62, 0.50, 0.32), rng, glide_factor=0.18, glide_tau=0.06)
        growl = _apply_formant_bank(_glottal_sub_source(at, np.full(len(at), bf * 0.36), rng, harmonics=8, sub_weight=0.75), [(240.0 * formant_shift, 2.8, 0.9), (620.0, 3.4, 0.7)]) * adsr_envelope(len(at), 0.08, 0.22, 0.50, 0.36) * 0.65
        wave[adapt_s:] += chord + growl
        return _cavern_reverb(np.tanh(wave * 1.25), rng, wet=0.22, rt60=0.48, lp_hz=6200.0)

    if sound_id == "hive_synergy_unlock":
        # Dual-strain convergence: low sub-drop + interlocking tritone/fifth brass chord + synaptic arc burst
        bomb = _sub_bomb(t, int(0.10 * n), f_start=155.0, f_end=32.0, drop_tau=0.07, sustain_tau=dur * 0.46)
        f_syn = bf * (0.85 + 0.35 * np.sin(np.pi * t / dur))
        brass = _brass_braaam(t, f_syn, rng, detune_cents=20.0, sub_oct=0.85, shimmer_mix=0.26, formant_shift=formant_shift) * adsr_envelope(n, 0.10, 0.20, 0.58, 0.34)
        crystal = _modal_bank(t, bf * 2.2, (1.0, 1.414, 2.0, 2.828), (0.42, 0.34, 0.26, 0.18), (0.7, 0.55, 0.42, 0.28), rng)
        zaps = _micro_arc_zaps(n, rng, 5, lo_hz=3200.0, hi_hz=12500.0, spread=(0.12, 0.80)) * 0.55
        return _abyssal_reverb(np.tanh((bomb + brass * 0.95 + crystal * 0.80 + zaps) * 1.3), rng, predelay_ms=16.0, wet=0.28, rt60=0.72, lp_hz=5800.0, shimmer=0.22)

    if sound_id == "hive_threat_milestone":
        # 2 rapid Bio-Scanner warning pulses -> deep Hive warhorn escalation swell
        wave = np.zeros(n, dtype=np.float64)
        for idx, p_frac in enumerate((0.02, 0.16)):
            pos = int(p_frac * n)
            plen = min(n - pos, int(SAMPLE_RATE * 0.075))
            pt = np.arange(plen, dtype=np.float64) / SAMPLE_RATE
            f_p = 960.0 + 240.0 * idx
            wave[pos : pos + plen] += (np.sin(2.0 * np.pi * f_p * pt) + 0.35 * np.sin(4.0 * np.pi * f_p * pt)) * np.sin(np.linspace(0, np.pi, plen)) * 0.72
        horn_s = int(0.28 * n)
        ht = np.arange(n - horn_s, dtype=np.float64) / SAMPLE_RATE
        f_h = bf * (0.92 + 0.24 * (ht / max(1e-4, ht[-1])))
        horn = _brass_braaam(ht, f_h, rng, detune_cents=16.0, sub_oct=0.90, shimmer_mix=0.18, formant_shift=formant_shift) * adsr_envelope(len(ht), 0.08, 0.20, 0.58, 0.36)
        wave[horn_s:] += horn * 1.15
        return _cavern_reverb(np.tanh(wave * 1.25), rng, wet=0.26, rt60=0.58, lp_hz=4800.0)

    if sound_id == "primordial_presence_pulse":
        # Bioluminescent elder aura: shimmering crystalline drone + deep ancestral sub throb
        onset = int(rng.uniform(0.02, 0.08) * n)
        pt = np.arange(n - onset, dtype=np.float64) / SAMPLE_RATE
        env = adsr_envelope(len(pt), 0.14, 0.24, 0.55, 0.38)
        sub = np.sin(2.0 * np.pi * (bf * 0.25) * pt + pjit) * env * 0.62
        harm = modal_resonator_bank(
            len(pt),
            (bf, bf * 1.498, bf * 2.25, bf * 3.16),
            (0.52, 0.42, 0.30, 0.20),
            rng,
            amplitudes=(0.75, 0.55, 0.38, 0.22),
        )
        breath = _swept_resonant_filter(pink[onset:], 260.0, 1450.0, q=3.4) * env * 0.48
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = sub + harm + breath
        return _abyssal_reverb(np.tanh(wave * 1.15), rng, predelay_ms=16.0, wet=0.34, rt60=0.86, lp_hz=5600.0, shimmer=0.26)

    if sound_id == "sovereign_nexus_hum":
        # Continuous warm Sovereign Nexus field hum (integer-cycle harmonics + seamless loop fold)
        cf = int(SAMPLE_RATE * 0.14)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        f0 = max(64.0, round(bf * dur) / dur)
        lfo_hz = max(1.0, round(1.5 * dur) / dur)
        organ = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.52
            + np.sin(2.0 * np.pi * (f0 * 1.5) * ext_t + rng.uniform(0, 6.28)) * 0.36
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t + rng.uniform(0, 6.28)) * 0.25
            + np.sin(2.0 * np.pi * (f0 * 3.0) * ext_t + rng.uniform(0, 6.28)) * 0.14
        ) * (0.86 + 0.14 * np.sin(2.0 * np.pi * lfo_hz * ext_t + pjit))
        air = _sos_bandpass(ext_pink, 320.0, 2400.0) * 0.28
        raw = _cavern_reverb(_sos_highpass(organ + air, 55.0), rng, wet=0.18, rt60=0.45, lp_hz=4800.0)
        return fold_seamless_loop(raw, n, crossfade_ms=140.0)

    if sound_id == "boss_combat_tension_bed":
        # Continuous legendary fungal-boss dread loop (arterial mushroom-heart drum + warhorn fifths + dark fungal air)
        cf = int(SAMPLE_RATE * 0.28)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        ext_brown = _brown_noise(rng, ext_n)
        f0 = max(44.0, 2.0 * round(0.5 * bf * dur) / dur)
        pulse_hz = max(0.25, round(0.5 * dur) / dur)
        sub = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.72
            + np.sin(2.0 * np.pi * (f0 * 1.498) * ext_t + rng.uniform(0, 6.28)) * 0.42
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t + rng.uniform(0, 6.28)) * 0.26
        ) * (0.86 + 0.14 * np.sin(2.0 * np.pi * pulse_hz * ext_t + pjit))
        # Periodic arterial fungal heart-drum hits locked cleanly to integer loop subdivisions
        heart = np.zeros(ext_n, dtype=np.float64)
        for p_frac in (0.12, 0.22, 0.62, 0.72):
            pos = int(p_frac * n)
            hlen = min(ext_n - pos, int(SAMPLE_RATE * 0.24))
            ht = np.arange(hlen, dtype=np.float64) / SAMPLE_RATE
            heart[pos : pos + hlen] += fungal_cap_resonance(hlen, f0 * 1.35, rng, decay=0.18, sponginess=0.72) * exp_decay(ht, 0.16) * 0.75
        rumble = _sos_bandpass(ext_brown * 0.8 + ext_pink * 0.35, 38.0, 320.0) * 0.68
        choir_air = _sos_bandpass(ext_pink, 140.0, 440.0) * 0.36
        dry = np.tanh((sub + heart + rumble + choir_air) * 1.20)
        preroll = min(ext_n // 2, int(SAMPLE_RATE * 0.85))
        reverb_in = np.concatenate([dry[-preroll:], dry])
        raw = _abyssal_reverb(reverb_in, rng, predelay_ms=18.0, wet=0.32, rt60=0.76, lp_hz=1850.0)[
            preroll : preroll + ext_n
        ]
        return fold_seamless_loop(raw, n, crossfade_ms=280.0)

    if sound_id == "brute_wall_stun":
        # Heavy armored impact on chitin pillar -> rattling shell fracture (mid/upper Bark clarity) -> dazed Rosenberg vocal huff
        imp_idx = int(rng.uniform(0.015, 0.105) * n)
        thud = _sub_bomb(
            t,
            imp_idx,
            f_start=rng.uniform(135.0, 185.0),
            f_end=rng.uniform(42.0, 58.0),
            drop_tau=rng.uniform(0.032, 0.052),
            sustain_tau=dur * 0.13,
            punch_gain=0.82,
        )
        rem = max(32, n - imp_idx)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        shell = np.zeros(n, dtype=np.float64)
        shell[imp_idx:] = (
            karplus_strong_chitin(rem, bf * rng.uniform(2.3, 3.6), rng, damping=0.982, brightness=0.76, stiffness=0.24, excitation="strike")
            * exp_decay(rt, 0.16)
            * 1.25
            + karplus_strong_chitin(rem, bf * rng.uniform(4.1, 6.2), rng, damping=0.976, brightness=0.82, stiffness=0.28, excitation="strike")
            * exp_decay(rt, 0.11)
            * 0.85
            + granular_bio_cloud(rem, rng, freq_range=(420.0, 3800.0), density_hz=64.0, grain_ms=15.0, organic_mode="chitin")
            * exp_decay(rt, 0.24)
            * 1.05
        )
        daze_s = int(rng.uniform(0.16, 0.26) * n)
        dt = np.arange(max(32, n - daze_s), dtype=np.float64) / SAMPLE_RATE
        f_huff = bf * rng.uniform(1.15, 1.48) * (1.05 - 0.35 * (dt / max(1e-4, dt[-1])))
        huff = np.zeros(n, dtype=np.float64)
        huff[daze_s : daze_s + len(dt)] = articulated_vocal_tract(
            dt,
            f_huff,
            [
                (380.0 * formant_shift, 220.0 * formant_shift, 2.6, 1.05),
                (960.0 * formant_shift, 560.0 * formant_shift, 3.2, 0.95),
                (2150.0 * formant_shift, 1280.0 * formant_shift, 3.8, 0.65),
            ],
            rng,
            open_quotient=0.56,
            sub_weight=0.32,
            breath_noise=0.28,
        ) * adsr_envelope(len(dt), 0.08, 0.24, 0.48, 0.38) * 1.15
        raw = np.tanh((thud * 0.46 + shell + huff) * 1.22)
        return fdn4_hadamard_diffuser(_sos_highpass(raw, 72.0), rng, rt60=0.44, wet=0.20, damping_hz=4400.0)

    if sound_id == "pyro_shroom_defuse":
        # Pressurized steam ember quench: falling resonant hiss -> spore granular cool-down -> amber chime drop
        steam = _swept_resonant_filter(pink * 0.75 + white * 0.45, 5800.0, 480.0, q=2.2) * adsr_envelope(n, 0.03, 0.24, 0.48, 0.42) * 0.92
        cool = granular_bio_cloud(n, rng, freq_range=(900.0, 5600.0), density_hz=64.0, grain_ms=18.0, organic_mode="spore") * exp_decay(t, dur * 0.40) * 0.62
        drop_s = int(0.32 * n)
        rem = max(32, n - drop_s)
        chime = np.zeros(n, dtype=np.float64)
        chime[drop_s:] = karplus_strong_chitin(rem, bf * 1.5, rng, damping=0.990, brightness=0.76, stiffness=0.10, excitation="pluck") * exp_decay(np.arange(rem, dtype=np.float64) / SAMPLE_RATE, 0.18) * 0.52
        return fdn4_hadamard_diffuser(np.tanh((steam + cool + chime) * 1.16), rng, rt60=0.38, wet=0.18, damping_hz=6800.0)

    if sound_id == "sinkhole_updraft_loop":
        # 9th seamless loop: vertical subterranean thermal-spore wind chimney (even-cycle column + granular spores + circular FDN)
        cf = int(SAMPLE_RATE * 0.26)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        f0 = max(56.0, 2.0 * round(0.5 * bf * dur) / dur)
        lfo_hz = max(0.25, round(0.5 * dur) / dur)
        column = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.48
            + np.sin(2.0 * np.pi * (f0 * 1.5) * ext_t + rng.uniform(0, 6.28)) * 0.30
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t + rng.uniform(0, 6.28)) * 0.20
        ) * (0.88 + 0.12 * np.sin(2.0 * np.pi * lfo_hz * ext_t + pjit))
        wind = _sos_bandpass(ext_pink, 140.0, 1450.0) * (0.86 + 0.14 * np.sin(2.0 * np.pi * lfo_hz * ext_t + pjit)) * 0.68
        spores = granular_bio_cloud(ext_n, rng, freq_range=(380.0, 2400.0), density_hz=32.0, grain_ms=34.0, organic_mode="spore") * 0.42
        dry = np.tanh((column + wind + spores) * 1.12)
        preroll = min(ext_n // 2, int(SAMPLE_RATE * 0.90))
        reverb_in = np.concatenate([dry[-preroll:], dry])
        raw = fdn4_hadamard_diffuser(reverb_in, rng, base_delays_ms=(19.3, 27.1, 35.9, 47.3), rt60=0.68, wet=0.28, damping_hz=3200.0)[
            preroll : preroll + ext_n
        ]
        return fold_seamless_loop(raw, n, crossfade_ms=260.0)

    if sound_id == "hive_tension_bed_low":
        # 10th seamless loop: damp fungal cavern draft + slow mushroom-stalk pulse + distant monstrous murmur
        cf = int(SAMPLE_RATE * 0.28)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        f0 = max(116.0, 2.0 * round(0.5 * max(124.0, bf * 2.0) * dur) / dur)
        pulse_hz = max(0.25, round(0.5 * dur) / dur)
        pad = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.50
            + np.sin(2.0 * np.pi * (f0 * 1.498) * ext_t + rng.uniform(0, 6.28)) * 0.36
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t + rng.uniform(0, 6.28)) * 0.24
        ) * (0.88 + 0.12 * np.sin(2.0 * np.pi * pulse_hz * ext_t + pjit))
        whisper_bed = morphing_formant_tract(
            ext_t,
            f0 * 1.15,
            [
                (0.0, [(240.0, 2.6, 0.78), (640.0, 3.2, 0.52)]),
                (0.5, [(330.0, 2.8, 0.82), (820.0, 3.0, 0.56)]),
                (1.0, [(240.0, 2.6, 0.78), (640.0, 3.2, 0.52)]),
            ],
            rng,
            sub_weight=0.22,
            breath_noise=0.24,
        ) * 0.48
        spores = granular_bio_cloud(ext_n, rng, freq_range=(260.0, 880.0), density_hz=18.0, grain_ms=34.0, organic_mode="spore") * 0.40
        air = _sos_bandpass(ext_pink, 200.0, 920.0) * 0.65
        dry = _sos_bandpass(np.tanh((pad + whisper_bed + spores + air) * 1.14), 105.0, 2500.0)
        preroll = min(ext_n // 2, int(SAMPLE_RATE * 0.90))
        reverb_in = np.concatenate([dry[-preroll:], dry])
        raw = fdn4_hadamard_diffuser(reverb_in, rng, base_delays_ms=(23.1, 31.7, 41.9, 57.3), rt60=0.84, wet=0.33, damping_hz=2500.0)[
            preroll : preroll + ext_n
        ]
        return fold_seamless_loop(raw, n, crossfade_ms=280.0)

    if sound_id == "hive_swarm_combat_bed":
        # 11th seamless loop: epic hollow mushroom-cap tribal drum + tearing mycelial tension + predatory swarm pulse
        cf = int(SAMPLE_RATE * 0.26)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_brown = _brown_noise(rng, ext_n)
        f0 = max(136.0, 2.0 * round(0.5 * max(152.0, bf * 1.9) * dur) / dur)
        bpm_hz = max(1.0, round(2.0 * dur) / dur)
        throb_env = 0.52 + 0.48 * np.maximum(0.0, np.sin(2.0 * np.pi * bpm_hz * ext_t + pjit)) ** 2.2
        mid_pulse = (
            np.sin(2.0 * np.pi * f0 * ext_t + pjit) * 0.48
            + np.sin(2.0 * np.pi * (f0 * 1.498) * ext_t + rng.uniform(0, 6.28)) * 0.36
            + np.sin(2.0 * np.pi * (f0 * 2.0) * ext_t + rng.uniform(0, 6.28)) * 0.22
        ) * throb_env
        chitin_pulse = _sos_bandpass(ext_brown, 155.0, 920.0) * throb_env * 0.85
        cap_drums = np.zeros(ext_n, dtype=np.float64)
        for p_frac in (0.06, 0.31, 0.56, 0.81):
            pos = int(p_frac * n)
            dlen = min(ext_n - pos, int(SAMPLE_RATE * 0.16))
            cap_drums[pos : pos + dlen] += fungal_cap_resonance(dlen, f0 * 0.92, rng, decay=0.11, sponginess=0.72) * 0.62
        tension_harm = _fm_tone(ext_t, f0 * 2.0, f0 * 0.75, 1.35, pjit) * 0.28 * (0.7 + 0.3 * np.sin(2.0 * np.pi * 0.5 * bpm_hz * ext_t))
        skirmish_grains = granular_bio_cloud(ext_n, rng, freq_range=(240.0, 880.0), density_hz=26.0, grain_ms=26.0, organic_mode="chitin") * 0.50
        dry = _sos_bandpass(np.tanh((mid_pulse + chitin_pulse + cap_drums + tension_harm + skirmish_grains) * 1.16), 110.0, 2800.0)
        preroll = min(ext_n // 2, int(SAMPLE_RATE * 0.85))
        reverb_in = np.concatenate([dry[-preroll:], dry])
        raw = fdn4_hadamard_diffuser(reverb_in, rng, base_delays_ms=(17.9, 25.3, 35.1, 46.7), rt60=0.70, wet=0.29, damping_hz=2700.0)[
            preroll : preroll + ext_n
        ]
        return fold_seamless_loop(raw, n, crossfade_ms=260.0)

    if sound_id == "cavern_spore_drip":
        # Spatial micro-emitter: echoing liquid bioluminescent spore droplet ping + dispersed cavern ring
        onset = int(rng.uniform(0.02, 0.14) * n)
        rem = max(64, n - onset)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        f_drop = bf * rng.uniform(0.90, 1.25) * (1.0 + 0.52 * (1.0 - np.exp(-rt / 0.032)))
        ping = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(rt, 0.11) * 0.92
        ring = modal_dispersed_impact(rem, bf * rng.uniform(1.4, 1.9), rng, num_modes=6, hardness=0.78, dispersion=0.08, decay_secs=0.28) * 0.58
        splash = granular_bio_cloud(rem, rng, freq_range=(680.0, 4200.0), density_hz=36.0, grain_ms=14.0, organic_mode="viscous") * exp_decay(rt, 0.14) * 0.42
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = ping + ring + splash
        return fdn4_hadamard_diffuser(np.tanh(wave * 1.18), rng, rt60=0.74, wet=0.34, damping_hz=5600.0)

    if sound_id == "subterranean_hyphal_creak":
        # Spatial micro-emitter: deep strained chitinous root fiber groan + sticky tear grains
        onset = int(rng.uniform(0.01, 0.08) * n)
        rem = max(64, n - onset)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        f_creak = bf * rng.uniform(0.86, 1.18) * (1.0 - 0.24 * (rt / max(1e-4, rt[-1]))) * (1.0 + 0.07 * np.sin(2.0 * np.pi * 11.0 * rt))
        groan = morphing_formant_tract(
            rt,
            f_creak,
            [
                (0.0, [(210.0 * formant_shift, 2.8, 1.0), (540.0 * formant_shift, 3.2, 0.80)]),
                (0.55, [(320.0 * formant_shift, 3.0, 1.05), (780.0 * formant_shift, 3.4, 0.85)]),
                (1.0, [(165.0 * formant_shift, 2.6, 0.90), (430.0 * formant_shift, 3.0, 0.65)]),
            ],
            rng,
            open_quotient=0.48,
            sub_weight=0.65,
            breath_noise=0.16,
        ) * adsr_envelope(rem, 0.10, 0.25, 0.55, 0.35)
        snaps = modal_dispersed_impact(rem, bf * 2.1, rng, num_modes=7, hardness=0.48, dispersion=0.19, decay_secs=0.30) * 0.45
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = groan + snaps
        return fdn4_hadamard_diffuser(np.tanh(wave * 1.22), rng, rt60=0.62, wet=0.26, damping_hz=3200.0)

    if sound_id == "deep_mycelium_whisper":
        # Spatial micro-emitter: morphing formant alien whisper across abyssal mycelium threads
        onset = int(rng.uniform(0.02, 0.10) * n)
        rem = max(64, n - onset)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        f_wh = bf * rng.uniform(0.90, 1.16) * (0.95 + 0.12 * np.sin(2.0 * np.pi * mf * rt))
        whisper = morphing_formant_tract(
            rt,
            f_wh,
            [
                (0.0, [(340.0 * formant_shift, 3.0, 0.85), (1120.0 * formant_shift, 3.5, 0.90)]),
                (0.45, [(480.0 * formant_shift, 3.2, 0.95), (1680.0 * formant_shift, 3.8, 0.95)]),
                (1.0, [(280.0 * formant_shift, 2.8, 0.80), (960.0 * formant_shift, 3.2, 0.70)]),
            ],
            rng,
            open_quotient=0.66,
            sub_weight=0.35,
            breath_noise=0.34,
        ) * adsr_envelope(rem, 0.16, 0.22, 0.52, 0.40)
        spores = granular_bio_cloud(rem, rng, freq_range=(520.0, 3400.0), density_hz=42.0, grain_ms=26.0, organic_mode="spore") * 0.46
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = whisper + spores
        return fdn4_hadamard_diffuser(np.tanh(wave * 1.16), rng, rt60=0.78, wet=0.34, damping_hz=4800.0)

    if sound_id == "sanctum_harmonic_chime":
        # Spatial micro-emitter: resonant crystal monolith overtone triad using dispersed modal resonators
        onset = int(rng.uniform(0.01, 0.08) * n)
        rem = max(64, n - onset)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        root = bf * rng.uniform(0.92, 1.12)
        c1 = modal_dispersed_impact(rem, root, rng, num_modes=6, hardness=0.72, dispersion=0.06, decay_secs=0.72) * 0.65
        c2 = modal_dispersed_impact(rem, root * 1.498, rng, num_modes=5, hardness=0.76, dispersion=0.05, decay_secs=0.64) * 0.52
        c3 = modal_dispersed_impact(rem, root * 2.0, rng, num_modes=4, hardness=0.80, dispersion=0.04, decay_secs=0.52) * 0.36
        shimmer = granular_bio_cloud(rem, rng, freq_range=(1100.0, 4800.0), density_hz=28.0, grain_ms=30.0, organic_mode="spore") * exp_decay(rt, 0.48) * 0.30
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = (c1 + c2 + c3 + shimmer) * adsr_envelope(rem, 0.02, 0.22, 0.55, 0.42)
        return fdn4_hadamard_diffuser(np.tanh(wave * 1.12), rng, base_delays_ms=(21.3, 31.1, 43.7, 59.9), rt60=0.86, wet=0.36, damping_hz=6400.0)

    raise ValueError(f"Unhandled system/environment sound_id in system_env_synth: {sound_id}")
