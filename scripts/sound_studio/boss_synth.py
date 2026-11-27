"""Dedicated multi-act cinematic boss synthesizer for Mycelial Mind.

Elevates all 17 boss, mini-boss, colossal step, and boss-summon ritual sound
events into distinct, trailer-grade three-act sonic identities:
  1. Spore Behemoth   — Primal tectonic Kaiju (infrasonic drops, glottal sub-roars, rock rumble)
  2. Hive Core        — Cosmic psychic monolith (Reaper-style detuned brass braaams, bells, arcs)
  3. Apex Matriarch   — Eldritch sovereign banshee-choir empress (dissonant choral wails, void tears)
  4. Shared Cinematics — Phase shift detonations, fungal tide warhorns, monolith & rift awakenings
"""

from __future__ import annotations

import numpy as np

from ._cuda_compat import cuda_dsp as _cuda

_gpu_harmonic_sum = _cuda.gpu_harmonic_sum

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
    articulated_vocal_tract,
    exp_decay,
    fdn4_hadamard_diffuser,
    fungal_cap_resonance,
    granular_bio_cloud,
    karplus_strong_chitin,
    modal_resonator_bank,
    mycelial_hyphae_tear,
    spore_sac_burst,
)

BOSS_CINEMATIC_IDS: frozenset[str] = frozenset(
    {
        # Spore Behemoth (4)
        "spore_behemoth_roar",
        "spore_behemoth_hurt",
        "spore_behemoth_death",
        "spore_behemoth_slam",
        # Hive Core (4)
        "hive_core_resonance",
        "hive_core_hurt",
        "hive_core_death",
        "hive_core_shield",
        # Apex Matriarch (4)
        "apex_matriarch_wail",
        "apex_matriarch_hurt",
        "apex_matriarch_death",
        "apex_matriarch_barrage",
        # Shared Boss & Ritual Cinematics (6)
        "boss_phase_shift",
        "fungal_tide_horn",
        "colony_raid_horn",
        "hive_altar_awaken",
        "matriarch_altar_awaken",
        "mob_step_colossal",
    }
)

BOSS_PART_IMPACT_IDS: frozenset[str] = frozenset(
    {
        "boss_weakpoint_crit",
        "boss_carapace_ricochet",
        "boss_telegraph_lock",
        "boss_stagger_break",
    }
)


def _window_slice(n: int, start_frac: float, end_frac: float) -> tuple[int, int]:
    """Convert normalized fractions into clamped sample indices [start, end)."""
    s = int(max(0, min(n - 1, round(start_frac * n))))
    e = int(max(s + 1, min(n, round(end_frac * n))))
    return s, e


def _envelope_segment(n: int, start_idx: int, end_idx: int, attack_frac: float = 0.08, release_frac: float = 0.25) -> np.ndarray:
    """Create a smooth localized window over [start_idx, end_idx) inside a buffer of length n."""
    env = np.zeros(n, dtype=np.float64)
    length = max(2, end_idx - start_idx)
    seg = adsr_envelope(length, attack=attack_frac, decay=0.18, sustain=0.72, release=release_frac)
    env[start_idx:end_idx] = seg
    return env


def _brass_braaam(
    t: np.ndarray,
    f_curve: np.ndarray,
    rng: np.random.Generator,
    *,
    detune_cents: float = 19.0,
    sub_oct: float = 0.72,
    shimmer_mix: float = 0.24,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a multi-voice detuned Reaper-horn / alien brass braaam bed."""
    n = len(t)
    cents_offsets = (-detune_cents, -detune_cents * 0.45, 0.0, detune_cents * 0.48, detune_cents * 1.04)
    voice_weights = (0.72, 0.90, 1.0, 0.88, 0.70)
    harmonics = 12

    phase_base = np.cumsum(np.maximum(24.0, f_curve)) / SAMPLE_RATE
    mults: list[float] = []
    amps: list[float] = []
    offs: list[float] = []
    for cents, w in zip(cents_offsets, voice_weights):
        ratio = 2.0 ** (cents / 1200.0)
        for h in range(1, harmonics + 1):
            amp = w * (1.0 / (h**0.62)) * (1.18 if h % 2 == 1 else 0.82)
            p0 = float(rng.uniform(0.0, 2.0 * np.pi))
            mults.append(float(h) * ratio)
            amps.append(amp)
            offs.append(p0)

    brass = _gpu_harmonic_sum(phase_base, mults, amps, offs, min_work=180_000)
    if brass is None:
        brass = np.zeros(n, dtype=np.float64)
        for mult, amp, off in zip(mults, amps, offs):
            brass += amp * np.sin(2.0 * np.pi * mult * phase_base + off)

    # Sub-octave monolith weight + gritty FM growl core
    sub_phase = rng.uniform(0.0, 2.0 * np.pi)
    sub = np.sin(np.pi * phase_base + sub_phase) * sub_oct
    grit = _fm_tone(t, np.maximum(28.0, f_curve * 0.995), np.maximum(18.0, f_curve * 0.5), 1.85, rng.uniform(0.0, 6.28)) * 0.52

    driven = np.tanh((brass * 0.46 + sub * 0.75 + grit) * 1.65)
    warm = _apply_formant_bank(
        driven,
        [
            (max(85.0, 165.0 * formant_shift), 2.0, 0.88),
            (max(190.0, 440.0 * formant_shift), 2.4, 1.12),
            (max(480.0, 980.0 * formant_shift), 2.8, 0.95),
            (max(950.0, 2150.0 * formant_shift), 3.2, 0.52 + shimmer_mix * 1.5),
        ],
    )
    return warm + brass * 0.18 + sub * 0.30


def _sub_bomb(
    t: np.ndarray,
    impact_idx: int,
    *,
    f_start: float = 155.0,
    f_end: float = 29.0,
    drop_tau: float = 0.068,
    sustain_tau: float = 0.56,
    punch_gain: float = 0.48,
) -> np.ndarray:
    """Create a chest-thumping seismic 808/taiko sub-drop starting at impact_idx."""
    n = len(t)
    out = np.zeros(n, dtype=np.float64)
    if impact_idx >= n - 8:
        return out

    dt = np.maximum(0.0, t[impact_idx:] - t[impact_idx])
    f_drop = np.maximum(24.0, f_end + (f_start - f_end) * np.exp(-dt / max(0.012, drop_tau)))
    phase = 2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE
    amp = (1.0 - np.exp(-dt / 0.0045)) * np.exp(-dt / max(0.08, sustain_tau))

    sub_wave = np.sin(phase) + 0.28 * np.sin(2.0 * phase + 0.4)
    punch = np.sin(phase * 1.5) * np.exp(-dt / 0.022) * punch_gain
    out[impact_idx:] = np.tanh((sub_wave * 1.25 + punch) * amp)
    return out


def _reverse_suction(
    n: int,
    start_idx: int,
    length_samples: int,
    rng: np.random.Generator,
    *,
    f_lo: float = 140.0,
    f_hi: float = 2900.0,
) -> np.ndarray:
    """Create an accelerating backward vacuum/inhale draw ending in a crisp cutoff before impact."""
    out = np.zeros(n, dtype=np.float64)
    start = max(0, min(n - 32, start_idx))
    end = min(n, start + max(32, length_samples))
    seg_len = end - start
    if seg_len < 32:
        return out

    u = np.linspace(0.0, 1.0, seg_len, endpoint=False)
    swell = (np.exp(3.4 * u) - 1.0) / (np.exp(3.4) - 1.0)
    # Brief vacuum gap in the last ~15 ms to make the subsequent blast hit harder
    vacuum_cut = max(8, int(SAMPLE_RATE * rng.uniform(0.012, 0.022)))
    if seg_len > vacuum_cut * 2:
        swell[-vacuum_cut:] *= np.linspace(1.0, 0.0, vacuum_cut) ** 2

    noise = _pink_noise(rng, seg_len)
    swept = _swept_resonant_filter(noise, f_lo, f_hi, q=2.8, n_bands=12)

    seg_t = np.arange(seg_len, dtype=np.float64) / SAMPLE_RATE
    ring = _modal_bank(
        seg_t,
        f_lo * 1.25,
        (1.0, 1.498, 2.21, 3.12, 4.35),
        (0.18, 0.14, 0.11, 0.08, 0.06),
        (0.45, 0.35, 0.25, 0.18, 0.12),
        rng,
        mode_jitter=0.05,
    )
    ring_rev = ring[::-1]

    out[start:end] = np.tanh((swept * 0.95 + ring_rev * 0.72) * swell * 1.4)
    return out


def _eldritch_choir(
    t: np.ndarray,
    f_curve: np.ndarray,
    rng: np.random.Generator,
    *,
    voices: int = 5,
    dissonance: str = "tritone",
    breath_mix: float = 0.28,
    vibrato_hz: float = 5.4,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a haunted multi-voice banshee/sovereign choir using vocal formants."""
    n = len(t)
    if dissonance == "crown_shriek":
        intervals = (1.0, 1.260, 1.587, 1.888, 2.378)
    elif dissonance == "minor_lament":
        intervals = (0.5, 1.0, 1.189, 1.498, 2.008)
    else:  # tritone
        intervals = (1.0, 1.059, 1.414, 1.995, 2.828)

    choir = np.zeros(n, dtype=np.float64)
    pink = _pink_noise(rng, n)

    for idx in range(min(voices, len(intervals))):
        r = intervals[idx] * rng.uniform(0.992, 1.008)
        vib_rate = vibrato_hz * rng.uniform(0.84, 1.18)
        vib_depth = rng.uniform(0.014, 0.032)
        vib_phase = rng.uniform(0.0, 2.0 * np.pi)
        f_v = np.maximum(75.0, f_curve * r * (1.0 + vib_depth * np.sin(2.0 * np.pi * vib_rate * t + vib_phase)))
        ph = 2.0 * np.pi * np.cumsum(f_v) / SAMPLE_RATE + rng.uniform(0.0, 2.0 * np.pi)
        v_weight = 1.0 / (1.0 + 0.18 * idx)
        # Vocal pulse approximation (rich odd + even harmonics with upper banshee brilliance)
        choir_mults = (1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        choir_amps = [v_weight * a for a in (1.0, 0.68, 0.48, 0.34, 0.24, 0.16)]
        choir_offs = (0.0, 0.3, 0.7, 1.1, 0.0, 0.5)
        v_wave = _gpu_harmonic_sum(ph / (2.0 * np.pi), choir_mults, choir_amps, choir_offs, min_work=240_000)
        if v_wave is None:
            v_wave = (
                np.sin(ph)
                + 0.68 * np.sin(2.0 * ph + 0.3)
                + 0.48 * np.sin(3.0 * ph + 0.7)
                + 0.34 * np.sin(4.0 * ph + 1.1)
                + 0.24 * np.sin(5.0 * ph)
                + 0.16 * np.sin(6.0 * ph + 0.5)
            ) * v_weight
        choir += v_wave

    breath = _sos_bandpass(pink, 950.0, 7400.0) * breath_mix
    shaped = _apply_formant_bank(
        choir * 0.44 + breath,
        [
            (max(260.0, 460.0 * formant_shift), 3.6, 0.72),
            (max(700.0, 1180.0 * formant_shift), 4.2, 1.15),
            (min(5200.0, 2480.0 * formant_shift), 4.6, 1.12),
            (min(6800.0, 3850.0 * formant_shift), 5.0, 0.82),
        ],
    )
    return np.tanh(shaped * 1.2)


def _abyssal_reverb(
    sig: np.ndarray,
    rng: np.random.Generator,
    *,
    predelay_ms: float = 16.0,
    wet: float = 0.28,
    rt60: float = 0.88,
    lp_hz: float = 2600.0,
    shimmer: float = 0.0,
) -> np.ndarray:
    """Layer a pre-delayed subterranean cavern hall with asymmetric echoes and optional shimmer."""
    n = len(sig)
    pre_samples = max(1, int(SAMPLE_RATE * max(4.0, predelay_ms) / 1000.0))
    delayed = np.zeros(n, dtype=np.float64)
    if pre_samples < n:
        delayed[pre_samples:] = sig[: n - pre_samples]

    cavern = _cavern_reverb(delayed, rng, wet=0.92, rt60=rt60, lp_hz=lp_hz)

    # Two long cathedral/cavern reflections with randomized tap offsets per take
    tap_ms = (rng.uniform(62.0, 86.0), rng.uniform(118.0, 152.0))
    tap_gains = (0.28, 0.17)
    long_tail = np.zeros(n, dtype=np.float64)
    for ms, g in zip(tap_ms, tap_gains):
        d = int(SAMPLE_RATE * ms / 1000.0)
        if 0 < d < n:
            long_tail[d:] += delayed[: n - d] * g
    long_tail = _sos_lowpass(long_tail, max(700.0, lp_hz * 0.82), order=2)

    wet_sig = cavern * 0.72 + long_tail * 0.55
    if shimmer > 0.01:
        upper = _sos_bandpass(delayed, 1400.0, min(8800.0, lp_hz * 1.45), order=2)
        mod = upper * np.cos(2.0 * np.pi * rng.uniform(380.0, 620.0) * (np.arange(n) / SAMPLE_RATE))
        wet_sig += _cavern_reverb(mod, rng, wet=0.85, rt60=rt60 * 1.15, lp_hz=min(7500.0, lp_hz * 1.25)) * shimmer

    return sig * (1.0 - wet * 0.35) + wet_sig * wet


def _epic_fungal_chorus_horn(
    t: np.ndarray,
    f_curve: np.ndarray,
    rng: np.random.Generator,
    *,
    formant_shift: float = 1.0,
    monstrous_grit: float = 0.65,
) -> np.ndarray:
    """Fuse titanic warhorn brass fifths with monstrous fungal chest vocal folds and giant cap resonance."""
    n = len(t)
    g = float(np.clip(monstrous_grit, 0.20, 1.25))
    horn = _brass_braaam(t, f_curve, rng, detune_cents=18.0, sub_oct=0.95, shimmer_mix=0.18, formant_shift=formant_shift)
    fifth = _brass_braaam(t, f_curve * 1.498, rng, detune_cents=12.0, sub_oct=0.45, shimmer_mix=0.14, formant_shift=formant_shift) * 0.45
    beast = _glottal_sub_source(t, np.maximum(26.0, f_curve * 0.992), rng, harmonics=13, sub_weight=0.92, rattle_hz=17.0)
    beast_chest = _apply_formant_bank(
        beast,
        [
            (max(65.0, 88.0 * formant_shift), 2.2, 1.15),
            (max(165.0, 245.0 * formant_shift), 2.7, 1.05),
            (max(420.0, 660.0 * formant_shift), 3.1, 0.72),
        ],
    )
    mean_f = float(np.mean(f_curve)) if n > 0 else 80.0
    cap_body = fungal_cap_resonance(n, max(48.0, mean_f * 1.45), rng, decay=0.34, sponginess=0.72) * 0.78
    return np.tanh((horn * 0.85 + fifth + beast_chest * (0.78 * g) + cap_body * (0.68 * g)) * 1.18)


def synthesize_boss_sound(
    sound_id: str,
    *,
    duration: float,
    base_freq: float,
    mod_freq: float,
    rng: np.random.Generator,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Generate a three-act cinematic boss or ritual waveform (before final mastering)."""
    dur = max(0.32, float(duration))
    n = max(64, int(round(dur * SAMPLE_RATE)))
    t = np.linspace(0.0, dur, n, endpoint=False)
    brown = _brown_noise(rng, n)
    pink = _pink_noise(rng, n)
    white = rng.normal(0.0, 1.0, n)

    # Randomized per-take offset/pitch factors keep pairwise variation lag-correlation low (< 0.60)
    p_scale = rng.uniform(0.91, 1.09)
    bf = max(28.0, float(base_freq) * p_scale)
    mf = max(0.8, float(mod_freq) * rng.uniform(0.85, 1.18))
    pjit = rng.uniform(0.0, 2.0 * np.pi)

    # =========================================================================
    # 1. SPORE BEHEMOTH — Colossal Fungal-Spore & Root Titan
    # =========================================================================
    if sound_id == "spore_behemoth_roar":
        roar_start_frac = rng.uniform(0.15, 0.27)
        s1, e1 = _window_slice(n, 0.01, roar_start_frac + 0.06)
        inhale_env = _envelope_segment(n, s1, e1, attack_frac=0.35, release_frac=0.22)
        inhale_air = _swept_resonant_filter(pink * 0.7 + brown * 0.5, bf * 1.1, bf * 3.8, q=2.5) * inhale_env
        inhale_gurgle = _wet_squelch_layer(n, rng, bf * 1.6, int(rng.integers(3, 6)), intensity=0.55) * inhale_env

        s2, e2 = _window_slice(n, roar_start_frac, 0.78)
        roar_env = _envelope_segment(n, s2, e2, attack_frac=0.06, release_frac=0.28)
        u_roar = np.clip((t - t[s2]) / max(0.12, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_roar = np.maximum(28.0, bf * (1.28 - 0.54 * (u_roar**0.85) + 0.06 * np.sin(2.0 * np.pi * mf * t + pjit)))
        epic_horn = _epic_fungal_chorus_horn(t, f_roar, rng, formant_shift=formant_shift, monstrous_grit=0.85) * roar_env
        sub_impact = _sub_bomb(t, s2, f_start=bf * 2.2, f_end=29.0, drop_tau=0.08, sustain_tau=dur * 0.45)

        s3, e3 = _window_slice(n, 0.60, 0.96)
        vent_env = _envelope_segment(n, s3, e3, attack_frac=0.14, release_frac=0.55)
        sac_vent = spore_sac_burst(n, rng, base_freq=bf * 1.8, pressure=0.88, duration_frac=0.85) * vent_env * 0.45
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.2, count=7, wetness=0.55) * 0.30

        raw = np.tanh((inhale_air * 0.65 + inhale_gurgle * 0.50 + epic_horn * 1.45 + sub_impact * 1.18 + sac_vent + hyphae) * 1.35)
        raw = _sos_lowpass(raw, 1350.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.32, rt60=0.98, lp_hz=1100.0)

    elif sound_id == "spore_behemoth_hurt":
        hit_frac = rng.uniform(0.03, 0.14)
        s_hit, e_hit = _window_slice(n, hit_frac, 0.88)
        env = _envelope_segment(n, s_hit, e_hit, attack_frac=0.05, release_frac=0.45)
        u = np.clip((t - t[s_hit]) / max(0.08, dur - t[s_hit]), 0.0, 1.0)
        f_bark = np.maximum(32.0, bf * (1.35 - 0.68 * (u**0.72)))
        bark = _glottal_sub_source(t, f_bark, rng, harmonics=12, sub_weight=0.82, rattle_hz=rng.uniform(20.0, 30.0))
        bark = _apply_formant_bank(bark, [(125.0 * formant_shift, 2.3, 1.0), (340.0 * formant_shift, 2.8, 1.15), (860.0 * formant_shift, 3.2, 0.72)])
        cap_crunch = fungal_cap_resonance(n, bf * 1.8, rng, decay=0.18, sponginess=0.72) * 0.85
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.2, count=5, wetness=0.60) * 0.60
        stagger_thud = _sub_bomb(t, s_hit, f_start=bf * 1.8, f_end=31.0, drop_tau=0.055, sustain_tau=0.22)
        raw = np.tanh((bark * env * 1.38 + cap_crunch + hyphae + stagger_thud * 0.75 + pink * 0.28 * env) * 1.35)
        raw = _sos_lowpass(raw, 2800.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=13.0, wet=0.26, rt60=0.68, lp_hz=2150.0)

    elif sound_id == "spore_behemoth_death":
        fall_frac = rng.uniform(0.42, 0.54)
        s1, e1 = _window_slice(n, 0.01, fall_frac + 0.08)
        roar_env = _envelope_segment(n, s1, e1, attack_frac=0.06, release_frac=0.30)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_die = np.maximum(24.0, bf * (1.26 - 0.82 * (u1**0.90) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit)))
        bellow = _epic_fungal_chorus_horn(t, f_die, rng, formant_shift=formant_shift, monstrous_grit=0.90) * roar_env

        impact_idx = int(fall_frac * n)
        body_bomb = _sub_bomb(t, impact_idx, f_start=148.0, f_end=25.0, drop_tau=0.09, sustain_tau=dur * 0.48)
        sac_rupture = np.zeros(n, dtype=np.float64)
        if n - impact_idx > 64:
            sac_rupture[impact_idx:] = spore_sac_burst(n - impact_idx, rng, base_freq=bf * 1.6, pressure=1.05, duration_frac=0.88) * 0.85
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.9, count=9, wetness=0.62) * 0.62

        rumble_env = _envelope_segment(n, max(0, impact_idx - 64), n, attack_frac=0.04, release_frac=0.55)
        rumble = _sos_bandpass(brown, 32.0, 420.0) * rumble_env * 0.95
        raw = np.tanh((bellow * 1.25 + body_bomb * 1.38 + sac_rupture + hyphae + rumble) * 1.35)
        raw = _sos_lowpass(raw, 1900.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.36, rt60=1.12, lp_hz=1400.0)

    elif sound_id == "spore_behemoth_slam":
        impact_frac = rng.uniform(0.07, 0.16)
        impact_idx = int(impact_frac * n)
        suck = _reverse_suction(n, 0, max(32, impact_idx - 60), rng, f_lo=75.0, f_hi=650.0) * 0.58
        bomb = _sub_bomb(t, impact_idx, f_start=154.0, f_end=26.0, drop_tau=0.075, sustain_tau=dur * 0.54, punch_gain=0.68)

        crack = np.zeros(n, dtype=np.float64)
        c_len = min(n - impact_idx, int(SAMPLE_RATE * 0.35))
        if c_len > 32:
            crack[impact_idx : impact_idx + c_len] = (
                fungal_cap_resonance(c_len, bf * 1.75, rng, decay=0.22, sponginess=0.68) * 0.95
                + spore_sac_burst(c_len, rng, base_freq=bf * 2.1, pressure=0.92, duration_frac=0.80) * 0.72
            )
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.1, count=6, wetness=0.55) * 0.52

        tail_env = _envelope_segment(n, impact_idx, n, attack_frac=0.02, release_frac=0.62)
        tail = _sos_bandpass(brown, 34.0, 480.0) * tail_env * 0.88
        raw = np.tanh((suck + bomb * 1.42 + crack + hyphae + tail) * 1.40)
        raw = _sos_lowpass(raw, 2000.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=16.0, wet=0.32, rt60=0.92, lp_hz=1500.0)

    # =========================================================================
    # 2. HIVE CORE — Ancient Legendary Living Fungal Heart & Cathedral Organ
    # =========================================================================
    elif sound_id == "hive_core_resonance":
        onset_frac = rng.uniform(0.06, 0.16)
        s_pre, e_pre = _window_slice(n, 0.0, onset_frac + 0.14)
        pre_env = _envelope_segment(n, s_pre, e_pre, attack_frac=0.45, release_frac=0.30)
        pre_hum = _fm_tone(t, bf * 1.25, bf * 0.45, 1.6, pjit) * pre_env * 0.42

        s_main, e_main = _window_slice(n, onset_frac, 0.88)
        main_env = _envelope_segment(n, s_main, e_main, attack_frac=0.07, release_frac=0.34)
        u_m = np.clip((t - t[s_main]) / max(0.2, t[e_main - 1] - t[s_main]), 0.0, 1.0)
        f_horn = bf * (1.0 + 0.08 * np.sin(np.pi * u_m) - 0.14 * (u_m**1.6) + 0.015 * np.sin(2.0 * np.pi * mf * t + pjit))
        epic_organ = _epic_fungal_chorus_horn(t, f_horn, rng, formant_shift=formant_shift, monstrous_grit=0.68) * main_env
        heart_gourd = fungal_cap_resonance(n, bf * 1.4, rng, decay=0.42, sponginess=0.72) * main_env * 0.80
        sub_drop = _sub_bomb(t, s_main, f_start=bf * 1.9, f_end=31.0, drop_tau=0.085, sustain_tau=dur * 0.54)
        sac_swarm = spore_sac_burst(n, rng, base_freq=bf * 1.5, pressure=0.65, duration_frac=0.80) * main_env * 0.42
        amethyst_bell = _modal_bank(t, bf * 2.0, (1.0, 1.498, 2.005, 2.756), (dur * 0.55, dur * 0.45, dur * 0.35, dur * 0.25), (0.34, 0.24, 0.24, 0.16), rng) * 0.40

        raw = np.tanh((pre_hum + epic_organ * 1.15 + heart_gourd + sub_drop * 0.82 + sac_swarm + amethyst_bell * main_env) * 1.32)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.34, rt60=1.04, lp_hz=1850.0, shimmer=0.06)

    elif sound_id == "hive_core_hurt":
        hit_frac = rng.uniform(0.02, 0.11)
        hit_idx = int(hit_frac * n)
        u = np.clip((t - t[hit_idx]) / max(0.08, dur - t[hit_idx]), 0.0, 1.0)
        f_groan = np.maximum(44.0, bf * (1.24 - 0.56 * (u**0.75)))
        groan_env = _envelope_segment(n, hit_idx, n, attack_frac=0.04, release_frac=0.45)
        groan = _epic_fungal_chorus_horn(t, f_groan, rng, formant_shift=formant_shift, monstrous_grit=0.75) * groan_env
        heart_crack = np.zeros(n, dtype=np.float64)
        heart_crack[hit_idx:] = fungal_cap_resonance(n - hit_idx, bf * 1.85, rng, decay=0.20, sponginess=0.74) * 0.95
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.9, count=6, wetness=0.62) * 0.65
        sac = spore_sac_burst(n, rng, base_freq=bf * 1.6, pressure=0.78, duration_frac=0.70) * 0.55
        raw = np.tanh((groan * 0.98 + heart_crack + hyphae + sac) * 1.28)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.26, rt60=0.72, lp_hz=3900.0, shimmer=0.08)

    elif sound_id == "hive_core_death":
        tear_frac = rng.uniform(0.36, 0.46)
        tear_idx = int(tear_frac * n)
        s1, e1 = _window_slice(n, 0.0, tear_frac)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_rise = bf * (0.88 + 0.85 * (u1**1.8))
        rise_env = _envelope_segment(n, s1, e1, attack_frac=0.14, release_frac=0.08)
        swell = _epic_fungal_chorus_horn(t, f_rise, rng, formant_shift=formant_shift, monstrous_grit=0.72) * rise_env
        suck = _reverse_suction(n, max(0, tear_idx - int(0.32 * n)), int(0.30 * n), rng, f_lo=140.0, f_hi=3600.0) * 0.95

        bomb = _sub_bomb(t, tear_idx, f_start=180.0, f_end=27.0, drop_tau=0.095, sustain_tau=dur * 0.50, punch_gain=0.72)
        rupture = np.zeros(n, dtype=np.float64)
        if n - tear_idx > 64:
            rupture[tear_idx:] = (
                spore_sac_burst(n - tear_idx, rng, base_freq=bf * 1.6, pressure=1.08, duration_frac=0.88) * 0.92
                + fungal_cap_resonance(n - tear_idx, bf * 1.45, rng, decay=0.30, sponginess=0.74) * 0.85
            )
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.8, count=10, wetness=0.64) * 0.65

        s3, e3 = _window_slice(n, tear_frac + 0.04, 0.98)
        die_env = _envelope_segment(n, s3, e3, attack_frac=0.05, release_frac=0.52)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_fall = np.maximum(30.0, bf * (1.35 - 0.92 * (u3**0.80)))
        die_horn = _epic_fungal_chorus_horn(t, f_fall, rng, formant_shift=formant_shift, monstrous_grit=0.85) * die_env

        raw = np.tanh((swell * 0.85 + suck + bomb * 1.38 + rupture + hyphae + die_horn * 0.95) * 1.30)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.38, rt60=1.22, lp_hz=3800.0, shimmer=0.18)

    elif sound_id == "hive_core_shield":
        lock_frac = rng.uniform(0.03, 0.11)
        lock_idx = int(lock_frac * n)
        clen = n - lock_idx
        gourd_wall = np.zeros(n, dtype=np.float64)
        gourd_wall[lock_idx:] = fungal_cap_resonance(clen, bf * 0.72, rng, decay=0.35, sponginess=0.72) * 0.95
        sac = np.zeros(n, dtype=np.float64)
        sac[lock_idx:] = spore_sac_burst(clen, rng, base_freq=bf * 0.85, pressure=0.85, duration_frac=0.75) * 0.65
        ct = np.arange(clen, dtype=np.float64) / SAMPLE_RATE
        chime = np.zeros(n, dtype=np.float64)
        chime[lock_idx:] = _modal_bank(ct, bf * 0.95, (0.5, 1.0, 1.498, 2.004, 2.996), (dur * 0.65, dur * 0.58, dur * 0.48, dur * 0.40, dur * 0.28), (0.45, 0.68, 0.52, 0.42, 0.26), rng, glide_factor=0.28, glide_tau=0.065) * 0.65
        hum_env = _envelope_segment(n, lock_idx, n, attack_frac=0.05, release_frac=0.45)
        f_hum = bf * (0.5 + 0.04 * np.sin(2.0 * np.pi * mf * t + pjit))
        hum = _epic_fungal_chorus_horn(t, f_hum, rng, formant_shift=formant_shift, monstrous_grit=0.58) * hum_env * 0.75
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.95, count=5, wetness=0.55) * 0.45
        raw = np.tanh((gourd_wall + sac + chime + hum + hyphae) * 1.25)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.30, rt60=0.82, lp_hz=4600.0, shimmer=0.18)

    # =========================================================================
    # 3. APEX MATRIARCH — Legendary Monstrous Fungal Empress
    # =========================================================================
    elif sound_id == "apex_matriarch_wail":
        wail_frac = rng.uniform(0.11, 0.23)
        wail_idx = int(wail_frac * n)
        suck = _reverse_suction(n, 0, max(32, wail_idx), rng, f_lo=220.0, f_hi=4200.0) * 0.80

        s2, e2 = _window_slice(n, wail_frac - 0.02, 0.88)
        wail_env = _envelope_segment(n, s2, e2, attack_frac=0.07, release_frac=0.36)
        u2 = np.clip((t - t[s2]) / max(0.15, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_wail = bf * (1.42 + 0.85 * np.sin(np.pi * (u2**0.72)) - 0.16 * (u2**1.8))
        choir = _eldritch_choir(t, f_wail, rng, voices=5, dissonance="crown_shriek", breath_mix=0.34, vibrato_hz=rng.uniform(4.4, 6.8), formant_shift=formant_shift)
        monstrous_throat = _apply_formant_bank(
            _glottal_sub_source(t, np.maximum(48.0, f_wail * 0.45), rng, harmonics=12, sub_weight=0.88, rattle_hz=21.0),
            [(145.0 * formant_shift, 2.5, 1.15), (380.0 * formant_shift, 3.0, 0.92)],
        ) * 0.22
        cap_crown = fungal_cap_resonance(n, bf * 0.85, rng, decay=0.34, sponginess=0.72) * 0.18
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 0.90, count=6, wetness=0.58) * 0.48
        sub_anchor = _sub_bomb(t, wail_idx, f_start=122.0, f_end=34.0, drop_tau=0.08, sustain_tau=dur * 0.46) * 0.12

        raw = np.tanh((suck + (choir * 1.85 + monstrous_throat + cap_crown) * wail_env + sub_anchor + hyphae) * 1.28)
        raw = _sos_highpass(raw, 280.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.38, rt60=1.12, lp_hz=7800.0, shimmer=0.58)

    elif sound_id == "apex_matriarch_hurt":
        hit_frac = rng.uniform(0.02, 0.11)
        hit_idx = int(hit_frac * n)
        gasp_env = _envelope_segment(n, hit_idx, n, attack_frac=0.04, release_frac=0.45)
        u = np.clip((t - t[hit_idx]) / max(0.08, dur - t[hit_idx]), 0.0, 1.0)
        f_gasp = bf * (1.42 - 0.65 * (u**0.68))
        choir = _eldritch_choir(t, f_gasp, rng, voices=4, dissonance="tritone", breath_mix=0.42, vibrato_hz=rng.uniform(6.0, 8.2), formant_shift=formant_shift)
        monstrous_hiss = _glottal_sub_source(t, np.maximum(52.0, f_gasp * 0.48), rng, harmonics=10, sub_weight=0.78, rattle_hz=26.0) * 0.72

        slen = n - hit_idx
        snap = np.zeros(n, dtype=np.float64)
        snap[hit_idx:] = fungal_cap_resonance(slen, bf * 1.10, rng, decay=0.14, sponginess=0.74) * 0.85
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.0, count=6, wetness=0.62) * 0.62
        raw = np.tanh(((choir * 1.12 + monstrous_hiss) * gasp_env + snap + hyphae) * 1.25)
        raw = _sos_highpass(raw, 48.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.28, rt60=0.74, lp_hz=5800.0, shimmer=0.20)

    elif sound_id == "apex_matriarch_death":
        tear_frac = rng.uniform(0.36, 0.46)
        tear_idx = int(tear_frac * n)
        s1, e1 = _window_slice(n, 0.0, tear_frac)
        scream_env = _envelope_segment(n, s1, e1, attack_frac=0.08, release_frac=0.10)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_ascend = bf * (0.95 + 0.72 * (u1**1.4))
        scream = (_eldritch_choir(t, f_ascend, rng, voices=5, dissonance="crown_shriek", breath_mix=0.26, vibrato_hz=rng.uniform(5.8, 7.6), formant_shift=formant_shift) + _glottal_sub_source(t, f_ascend * 0.46, rng, harmonics=11, sub_weight=0.85) * 0.65) * scream_env
        suck = _reverse_suction(n, max(0, tear_idx - int(0.28 * n)), int(0.26 * n), rng, f_lo=260.0, f_hi=5800.0) * 0.92

        bomb = _sub_bomb(t, tear_idx, f_start=185.0, f_end=29.0, drop_tau=0.085, sustain_tau=dur * 0.48, punch_gain=0.68)
        veil = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.05, count=11, wetness=0.64) * 0.72
        sac = np.zeros(n, dtype=np.float64)
        if n - tear_idx > 64:
            sac[tear_idx:] = spore_sac_burst(n - tear_idx, rng, base_freq=bf * 0.95, pressure=1.05, duration_frac=0.88) * 0.88

        s3, e3 = _window_slice(n, tear_frac + 0.02, 0.98)
        lament_env = _envelope_segment(n, s3, e3, attack_frac=0.05, release_frac=0.58)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_desc = np.maximum(64.0, bf * (1.45 - 0.88 * (u3**0.78)))
        lament = (_eldritch_choir(t, f_desc, rng, voices=4, dissonance="minor_lament", breath_mix=0.40, vibrato_hz=rng.uniform(4.2, 5.8), formant_shift=formant_shift) + fungal_cap_resonance(n, bf * 0.78, rng, decay=0.35, sponginess=0.72) * 0.68) * lament_env

        raw = np.tanh((scream * 1.10 + suck + bomb * 1.25 + veil + sac + lament * 1.08) * 1.25)
        raw = _sos_highpass(raw, 44.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=22.0, wet=0.40, rt60=1.28, lp_hz=6200.0, shimmer=0.34)

    elif sound_id == "apex_matriarch_barrage":
        bolts = np.zeros(n, dtype=np.float64)
        volley_count = int(rng.integers(5, 8))
        positions = sorted(rng.uniform(0.04, 0.72) for _ in range(volley_count))
        for b_idx, pos_frac in enumerate(positions):
            pos = int(pos_frac * n)
            blen = min(n - pos, int(SAMPLE_RATE * rng.uniform(0.09, 0.16)))
            if blen < 32:
                continue
            bolts[pos : pos + blen] += spore_sac_burst(blen, rng, base_freq=bf * rng.uniform(1.1, 1.8), pressure=0.88, duration_frac=0.85) * (0.78 + 0.25 * (b_idx / volley_count))
            bolts[pos : pos + blen] += fungal_cap_resonance(blen, bf * rng.uniform(0.8, 1.3), rng, decay=0.07, sponginess=0.70) * 0.52

        tail_start = rng.uniform(0.18, 0.28)
        s_tail, e_tail = _window_slice(n, tail_start, 0.96)
        shriek_env = _envelope_segment(n, s_tail, e_tail, attack_frac=0.14, release_frac=0.45)
        f_shriek = bf * (1.25 - 0.35 * ((t - t[s_tail]) / max(0.1, dur)))
        choir_tail = _eldritch_choir(t, np.maximum(100.0, f_shriek), rng, voices=3, dissonance="tritone", breath_mix=0.30, formant_shift=formant_shift) * shriek_env * 0.62
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.15, count=5, wetness=0.55) * 0.48
        raw = np.tanh((bolts * 1.05 + choir_tail + hyphae) * 1.25)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.29, rt60=0.74, lp_hz=6000.0, shimmer=0.18)

    # =========================================================================
    # 4. SHARED BOSS & RITUAL CINEMATICS
    # =========================================================================
    elif sound_id == "boss_phase_shift":
        drop_frac = rng.uniform(0.28, 0.34)
        drop_idx = int(drop_frac * n)
        s1, e1 = _window_slice(n, 0.0, drop_frac)
        u1 = np.clip(t / max(0.15, t[e1 - 1]), 0.0, 1.0)
        rise_env = _envelope_segment(n, s1, e1, attack_frac=0.18, release_frac=0.06)
        suck = _reverse_suction(n, 0, max(32, drop_idx - 48), rng, f_lo=130.0, f_hi=4200.0) * 1.05
        alarm = _fm_tone(t, bf * (0.9 + 1.15 * (u1**1.6)), bf * 0.75, 2.2, pjit) * rise_env * 0.48

        bomb = _sub_bomb(t, drop_idx, f_start=184.0, f_end=28.0, drop_tau=0.075, sustain_tau=dur * 0.52, punch_gain=0.72)

        s2, e2 = _window_slice(n, drop_frac, 0.98)
        chord_env = _envelope_segment(n, s2, e2, attack_frac=0.04, release_frac=0.48)
        u2 = np.clip((t - t[s2]) / max(0.2, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_pow = bf * (0.98 - 0.18 * (u2**1.3))
        epic_horn = _epic_fungal_chorus_horn(t, f_pow, rng, formant_shift=formant_shift, monstrous_grit=0.80)
        choir = _eldritch_choir(t, f_pow * 2.0, rng, voices=4, dissonance="tritone", breath_mix=0.24, formant_shift=formant_shift)
        sac = np.zeros(n, dtype=np.float64)
        sac[drop_idx:] = spore_sac_burst(n - drop_idx, rng, base_freq=bf * 1.4, pressure=0.95, duration_frac=0.80) * 0.65

        raw = np.tanh((suck + alarm + bomb * 1.40 + (epic_horn * 0.92 + choir * 0.58) * chord_env + sac) * 1.32)
        return _abyssal_reverb(raw, rng, predelay_ms=19.0, wet=0.36, rt60=1.16, lp_hz=4400.0, shimmer=0.20)

    elif sound_id == "fungal_tide_horn":
        s1, e1 = _window_slice(n, 0.0, 0.14)
        inhale = _swept_resonant_filter(pink * 0.6 + brown * 0.6, 120.0, 480.0, q=2.2) * _envelope_segment(n, s1, e1, attack_frac=0.45, release_frac=0.20)

        horn_start = int(0.11 * n)
        s2, e2 = _window_slice(n, 0.11, 0.84)
        horn_env = _envelope_segment(n, s2, e2, attack_frac=0.09, release_frac=0.32)
        u2 = np.clip((t - t[s2]) / max(0.25, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_horn = bf * (0.94 + 0.14 * np.exp(-u2 / 0.18) - 0.14 * (u2**1.4))
        epic_horn = _epic_fungal_chorus_horn(t, f_horn, rng, formant_shift=formant_shift, monstrous_grit=0.85) * horn_env
        horn_punch = _sub_bomb(t, horn_start, f_start=122.0, f_end=29.0, drop_tau=0.08, sustain_tau=dur * 0.46) * 0.88
        sac = spore_sac_burst(n, rng, base_freq=bf * 1.6, pressure=0.78, duration_frac=0.75) * horn_env * 0.50

        s3, e3 = _window_slice(n, 0.58, 0.96)
        ans_env = _envelope_segment(n, s3, e3, attack_frac=0.22, release_frac=0.52)
        ans = _sos_bandpass(_glottal_sub_source(t, f_horn * 1.25, rng, harmonics=7, rattle_hz=23.0), 85.0, 950.0) * ans_env * 0.44

        raw = np.tanh((inhale * 0.62 + epic_horn * 1.15 + horn_punch + sac + ans) * 1.28)
        raw = _sos_lowpass(raw, 2700.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=21.0, wet=0.36, rt60=1.22, lp_hz=2150.0)

    elif sound_id == "hive_altar_awaken":
        s1, e1 = _window_slice(n, 0.0, 0.40)
        grind_env = _envelope_segment(n, s1, e1, attack_frac=0.15, release_frac=0.22)
        stone_scrape = _swept_resonant_filter(brown * 1.1 + pink * 0.45, 55.0, 380.0, q=3.2) * grind_env
        hyphae_awaken = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.5, count=7, wetness=0.58) * 0.55

        toll_idx = int(0.37 * n)
        toll_bomb = _sub_bomb(t, toll_idx, f_start=136.0, f_end=27.0, drop_tau=0.09, sustain_tau=dur * 0.50, punch_gain=0.68)
        blen = n - toll_idx
        bell = np.zeros(n, dtype=np.float64)
        bell[toll_idx:] = fungal_cap_resonance(blen, bf * 1.35, rng, decay=0.45, sponginess=0.70) * 0.95

        s3, e3 = _window_slice(n, 0.42, 0.98)
        brass_env = _envelope_segment(n, s3, e3, attack_frac=0.18, release_frac=0.45)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_rise = bf * (0.88 + 0.18 * (u3**0.85))
        epic_horn = _epic_fungal_chorus_horn(t, f_rise, rng, formant_shift=formant_shift, monstrous_grit=0.78) * brass_env

        raw = np.tanh((stone_scrape * 0.80 + hyphae_awaken + toll_bomb * 1.30 + bell + epic_horn * 1.05) * 1.32)
        raw = _sos_lowpass(raw, 2500.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.36, rt60=1.22, lp_hz=2100.0)

    elif sound_id == "matriarch_altar_awaken":
        seals = np.zeros(n, dtype=np.float64)
        for idx_s, pos_frac in enumerate((0.03, 0.11, 0.20, 0.28, 0.34)):
            pos = int(pos_frac * n)
            slen = min(n - pos, int(SAMPLE_RATE * 0.18))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            seals[pos : pos + slen] += (
                _modal_bank(st, bf * (2.8 + 0.55 * idx_s), (1.0, 1.46, 2.18, 3.25), (0.11, 0.08, 0.055, 0.038), (0.65, 0.50, 0.36, 0.22), rng) * 0.58
                + fungal_cap_resonance(slen, bf * (0.8 + 0.2 * idx_s), rng, decay=0.14, sponginess=0.70) * 0.55
            )
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.2, count=8, wetness=0.62) * 0.58

        tear_idx = int(0.46 * n)
        suck = _reverse_suction(n, int(0.24 * n), max(32, tear_idx - int(0.24 * n)), rng, f_lo=240.0, f_hi=5600.0) * 1.02
        rift_bomb = _sub_bomb(t, tear_idx, f_start=178.0, f_end=30.0, drop_tau=0.08, sustain_tau=dur * 0.46, punch_gain=0.65)

        s3, e3 = _window_slice(n, 0.46, 0.98)
        choir_env = _envelope_segment(n, s3, e3, attack_frac=0.10, release_frac=0.46)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_choir = bf * 2.05 * (0.94 + 0.28 * np.sin(np.pi * (u3**0.75)))
        choir = (
            _eldritch_choir(t, f_choir, rng, voices=5, dissonance="crown_shriek", breath_mix=0.32, vibrato_hz=5.8, formant_shift=formant_shift)
            + _glottal_sub_source(t, f_choir * 0.45, rng, harmonics=11, sub_weight=0.82) * 0.65
        ) * choir_env
        sac = np.zeros(n, dtype=np.float64)
        sac[tear_idx:] = spore_sac_burst(n - tear_idx, rng, base_freq=bf * 1.15, pressure=0.92, duration_frac=0.85) * 0.65

        raw = np.tanh((seals + hyphae + suck + rift_bomb * 1.12 + choir * 1.18 + sac) * 1.25)
        raw = _sos_highpass(raw, 44.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=19.0, wet=0.38, rt60=1.24, lp_hz=6000.0, shimmer=0.32)

    elif sound_id == "boss_weakpoint_crit":
        onset = int(rng.uniform(0.015, 0.065) * n)
        bomb = _sub_bomb(t, onset, f_start=168.0, f_end=32.0, drop_tau=0.065, sustain_tau=dur * 0.42, punch_gain=0.72)
        cap_rup = fungal_cap_resonance(n, bf * 0.95, rng, decay=0.18, sponginess=0.74) * 0.85
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.3, count=6, wetness=0.65) * 0.68
        sac = spore_sac_burst(n, rng, base_freq=bf * 1.1, pressure=0.88, duration_frac=0.72) * 0.72
        wail = _apply_formant_bank(
            _glottal_sub_source(t, bf * (1.32 - 0.48 * (t / dur)), rng, harmonics=10, sub_weight=0.78, rattle_hz=23.0),
            [(340.0 * formant_shift, 2.9, 1.05), (980.0 * formant_shift, 3.5, 0.8)],
        ) * adsr_envelope(n, 0.04, 0.22, 0.40, 0.38) * 0.78
        raw = np.tanh((bomb * 1.25 + cap_rup + hyphae + sac + wail) * 1.28)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.27, rt60=0.65, lp_hz=5200.0)

    elif sound_id == "boss_carapace_ricochet":
        onset = int(rng.uniform(0.010, 0.050) * n)
        rem = n - onset
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        modal = np.zeros(n, dtype=np.float64)
        modal[onset:] = (
            modal_resonator_bank(rem, (bf * 1.0, bf * 1.62, bf * 2.44, bf * 3.66, bf * 5.12), (0.14, 0.10, 0.07, 0.045, 0.025), rng, amplitudes=(0.72, 0.80, 0.60, 0.40, 0.22))
            + fungal_cap_resonance(rem, bf * 0.85, rng, decay=0.11, sponginess=0.65) * 0.72
        )
        zip_up = np.zeros(n, dtype=np.float64)
        f_ric = bf * (1.1 + 1.85 * np.exp(-rt / 0.09))
        zip_up[onset:] = _fm_tone(rt, f_ric, bf * 0.62, 1.9, pjit) * exp_decay(rt, 0.14) * 0.62
        snap = _sos_bandpass(rng.normal(0.0, 1.0, n), 1600.0, 8800.0) * exp_decay(np.maximum(0.0, t - t[onset]), 0.018) * 0.48
        raw = np.tanh((modal + zip_up + snap) * 1.22)
        return _cavern_reverb(_sos_highpass(raw, 110.0), rng, wet=0.19, rt60=0.42, lp_hz=6400.0)

    elif sound_id == "boss_telegraph_lock":
        lock_idx = int(rng.uniform(0.26, 0.36) * n)
        suck = _reverse_suction(n, 0, max(32, lock_idx), rng, f_lo=290.0, f_hi=4200.0) * 0.82
        rem = max(32, n - lock_idx)
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        pin = np.zeros(n, dtype=np.float64)
        pin[lock_idx:] = (
            fungal_cap_resonance(rem, bf * 0.85, rng, decay=0.16, sponginess=0.70) * 0.82
            + karplus_strong_chitin(rem, bf * 1.4, rng, damping=0.985, brightness=0.74, stiffness=0.18, excitation="strike") * exp_decay(rt, 0.16) * 0.55
            + spore_sac_burst(rem, rng, base_freq=bf * 0.90, pressure=0.70, duration_frac=0.70) * 0.48
        )
        pulse = np.zeros(n, dtype=np.float64)
        pulse[lock_idx:] = _fm_tone(rt, bf * (1.25 - 0.35 * (rt / max(1e-4, rt[-1]))), bf * 0.5, 2.1, pjit) * exp_decay(rt, 0.18) * 0.58
        raw = np.tanh((suck + pin + pulse) * 1.22)
        return fdn4_hadamard_diffuser(_sos_highpass(raw, 130.0), rng, rt60=0.44, wet=0.23, damping_hz=5400.0)

    elif sound_id == "boss_stagger_break":
        shatter_idx = int(rng.uniform(0.02, 0.14) * n)
        bomb = _sub_bomb(
            t,
            shatter_idx,
            f_start=rng.uniform(135.0, 184.0),
            f_end=rng.uniform(28.0, 42.0),
            drop_tau=rng.uniform(0.052, 0.088),
            sustain_tau=dur * 0.38,
            punch_gain=0.80,
        )
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 1.6, count=8, wetness=0.60) * 0.68
        cap_break = fungal_cap_resonance(n, bf * 1.15, rng, decay=0.22, sponginess=0.72) * 0.82
        sac = spore_sac_burst(n, rng, base_freq=bf * 1.35, pressure=0.88, duration_frac=0.78) * 0.65
        f_groan = bf * rng.uniform(0.92, 1.14) * (1.18 - 0.46 * ((t / dur) ** 0.85))
        groan = articulated_vocal_tract(
            t,
            f_groan,
            [
                (290.0 * formant_shift, 150.0 * formant_shift, 2.8, 1.08),
                (820.0 * formant_shift, 440.0 * formant_shift, 3.2, 0.90),
                (1750.0 * formant_shift, 960.0 * formant_shift, 3.8, 0.55),
            ],
            rng,
            open_quotient=0.58,
            sub_weight=0.62,
            breath_noise=0.22,
        ) * adsr_envelope(n, 0.08, 0.24, 0.48, 0.42) * 1.05
        raw = np.tanh((bomb * 0.88 + hyphae + cap_break + sac + groan) * 1.25)
        return fdn4_hadamard_diffuser(raw, rng, base_delays_ms=(19.1, 27.3, 37.9, 51.7), rt60=0.90, wet=0.31, damping_hz=3600.0)

    elif sound_id == "colony_raid_horn":
        s1, e1 = _window_slice(n, 0.02, 0.44)
        s2, e2 = _window_slice(n, 0.46, 0.96)
        e_b1 = _envelope_segment(n, s1, e1, attack_frac=0.14, release_frac=0.30)
        e_b2 = _envelope_segment(n, s2, e2, attack_frac=0.10, release_frac=0.40)
        f_h1 = bf * (0.96 + 0.12 * (t / dur))
        f_h2 = bf * (1.12 - 0.10 * (t / dur))
        horn1 = _epic_fungal_chorus_horn(t, f_h1, rng, formant_shift=formant_shift, monstrous_grit=0.72) * e_b1
        horn2 = _epic_fungal_chorus_horn(t, f_h2, rng, formant_shift=formant_shift, monstrous_grit=0.82) * e_b2
        punch = _sub_bomb(t, s2, f_start=138.0, f_end=31.0, drop_tau=0.075, sustain_tau=dur * 0.38) * 0.85
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.2, count=5, wetness=0.52) * 0.36
        raw = np.tanh((horn1 * 0.90 + horn2 * 1.05 + punch + hyphae) * 1.28)
        raw = _sos_lowpass(raw, 3200.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.36, rt60=1.18, lp_hz=2600.0)

    else:  # "mob_step_colossal"
        stomp_frac = rng.uniform(0.03, 0.13)
        stomp_idx = int(stomp_frac * n)
        creak = _sos_bandpass(brown, 45.0, 260.0) * _envelope_segment(n, 0, max(16, stomp_idx + 120), attack_frac=0.3, release_frac=0.3) * 0.45
        bomb = _sub_bomb(t, stomp_idx, f_start=bf * 2.2, f_end=29.0, drop_tau=0.055, sustain_tau=dur * 0.48, punch_gain=0.62)
        cap_stomp = np.zeros(n, dtype=np.float64)
        cap_stomp[stomp_idx:] = fungal_cap_resonance(n - stomp_idx, bf * 1.5, rng, decay=0.22, sponginess=0.78) * 0.88
        hyphae = mycelial_hyphae_tear(n, rng, base_freq=bf * 2.0, count=5, wetness=0.60) * 0.48
        squish = _wet_squelch_layer(n, rng, bf * 1.7, int(rng.integers(2, 5)), intensity=0.45) * _envelope_segment(n, stomp_idx, n, attack_frac=0.04, release_frac=0.5)
        ground_shake = _sos_bandpass(brown, 34.0, 460.0) * _envelope_segment(n, stomp_idx, n, attack_frac=0.03, release_frac=0.58) * 0.85

        raw = np.tanh((creak + bomb * 1.38 + cap_stomp + hyphae + squish + ground_shake) * 1.35)
        raw = _sos_lowpass(raw, 1850.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.29, rt60=0.74, lp_hz=1500.0)
