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

try:
    from .cuda_dsp import gpu_harmonic_sum as _gpu_harmonic_sum
except ImportError:
    try:
        from cuda_dsp import gpu_harmonic_sum as _gpu_harmonic_sum
    except ImportError:
        from sound_studio.cuda_dsp import gpu_harmonic_sum as _gpu_harmonic_sum

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
    modal_resonator_bank,
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
    # 1. SPORE BEHEMOTH — Primal Tectonic Kaiju
    # =========================================================================
    if sound_id == "spore_behemoth_roar":
        # Act I: Guttural inhale + spore-sac swell (shifted 0.15..0.28 across takes)
        roar_start_frac = rng.uniform(0.15, 0.27)
        s1, e1 = _window_slice(n, 0.01, roar_start_frac + 0.06)
        inhale_env = _envelope_segment(n, s1, e1, attack_frac=0.35, release_frac=0.22)
        inhale_air = _swept_resonant_filter(pink * 0.7 + brown * 0.5, bf * 1.1, bf * 3.8, q=2.5) * inhale_env
        inhale_gurgle = _wet_squelch_layer(n, rng, bf * 1.6, int(rng.integers(3, 6)), intensity=0.55) * inhale_env

        # Act II: Chest-shattering tectonic glottal roar + brass sub-horn
        s2, e2 = _window_slice(n, roar_start_frac, 0.78)
        roar_env = _envelope_segment(n, s2, e2, attack_frac=0.06, release_frac=0.28)
        u_roar = np.clip((t - t[s2]) / max(0.12, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_roar = np.maximum(30.0, bf * (1.28 - 0.54 * (u_roar**0.85) + 0.06 * np.sin(2.0 * np.pi * mf * t + pjit)))
        glottal = _glottal_sub_source(
            t,
            f_roar,
            rng,
            harmonics=12,
            sub_weight=0.78,
            rattle_hz=rng.uniform(16.0, 26.0),
        )
        tract = _apply_formant_bank(
            glottal,
            [
                (max(65.0, 120.0 * formant_shift), 2.2, 0.90),
                (max(190.0, 340.0 * formant_shift), 2.6, 1.18),
                (max(440.0, 820.0 * formant_shift), 2.9, 0.92),
                (max(850.0, 1550.0 * formant_shift), 3.3, 0.52),
            ],
        )
        sub_impact = _sub_bomb(t, s2, f_start=bf * 2.2, f_end=31.0, drop_tau=0.08, sustain_tau=dur * 0.42)

        # Act III: Tectonic slam + venting spore hiss + cavern tail
        s3, e3 = _window_slice(n, 0.64, 0.96)
        vent_env = _envelope_segment(n, s3, e3, attack_frac=0.14, release_frac=0.55)
        vent = _sos_bandpass(brown * 0.9 + pink * 0.6, 85.0, 1250.0) * vent_env
        rubble = _chitin_modal_hits(n, rng, bf * 2.6, int(rng.integers(4, 7)), 0.055, spread_frac=0.55) * 0.48

        raw = np.tanh((inhale_air * 0.65 + inhale_gurgle * 0.50 + tract * roar_env * 1.48 + sub_impact * 0.72 + vent * 0.75 + rubble) * 1.35)
        raw = _sos_lowpass(raw, 2850.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.30, rt60=0.92, lp_hz=2200.0)

    elif sound_id == "spore_behemoth_hurt":
        # Massive guttural bark + heavy armor/chitin crunch + tectonic stagger thud
        hit_frac = rng.uniform(0.03, 0.14)
        s_hit, e_hit = _window_slice(n, hit_frac, 0.88)
        env = _envelope_segment(n, s_hit, e_hit, attack_frac=0.05, release_frac=0.45)
        u = np.clip((t - t[s_hit]) / max(0.08, dur - t[s_hit]), 0.0, 1.0)
        f_bark = np.maximum(34.0, bf * (1.35 - 0.68 * (u**0.72)))
        bark = _glottal_sub_source(t, f_bark, rng, harmonics=11, sub_weight=0.76, rattle_hz=rng.uniform(22.0, 32.0))
        bark = _apply_formant_bank(bark, [(135.0 * formant_shift, 2.3, 0.95), (360.0 * formant_shift, 2.8, 1.15), (920.0 * formant_shift, 3.2, 0.72)])
        plate_crack = _modal_bank(
            t,
            bf * 2.4,
            (1.0, 1.62, 2.38, 3.45),
            (0.06, 0.045, 0.032, 0.022),
            (0.85, 0.72, 0.52, 0.35),
            rng,
        ) * 0.78
        stagger_thud = _sub_bomb(t, s_hit, f_start=bf * 1.8, f_end=33.0, drop_tau=0.055, sustain_tau=0.22)
        raw = np.tanh((bark * env * 1.40 + plate_crack + stagger_thud * 0.68 + pink * 0.30 * env) * 1.35)
        raw = _sos_lowpass(raw, 2900.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=13.0, wet=0.24, rt60=0.65, lp_hz=2200.0)

    elif sound_id == "spore_behemoth_death":
        # Act I: Agonizing descending Kaiju bellow (0..0.46)
        fall_frac = rng.uniform(0.42, 0.54)
        s1, e1 = _window_slice(n, 0.01, fall_frac + 0.08)
        roar_env = _envelope_segment(n, s1, e1, attack_frac=0.06, release_frac=0.30)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_die = np.maximum(26.0, bf * (1.26 - 0.82 * (u1**0.90) + 0.05 * np.sin(2.0 * np.pi * mf * t + pjit)))
        bellow = _glottal_sub_source(t, f_die, rng, harmonics=9, sub_weight=0.95, rattle_hz=rng.uniform(14.0, 22.0))
        bellow = _apply_formant_bank(bellow, [(82.0 * formant_shift, 2.2, 1.2), (220.0 * formant_shift, 2.8, 0.78)])

        # Act II: Cataclysmic carcass floor impact at fall_frac
        impact_idx = int(fall_frac * n)
        body_bomb = _sub_bomb(t, impact_idx, f_start=145.0, f_end=26.0, drop_tau=0.09, sustain_tau=dur * 0.45)

        # Act III: Cascading stone/chitin debris + subterranean rupture rumble
        debris = np.zeros(n, dtype=np.float64)
        for idx_hit in _bounce_train(n, rng, int(rng.integers(5, 9)), start_frac=fall_frac + 0.03, end_frac=0.92, accelerate=True):
            seg_len = min(n - idx_hit, int(SAMPLE_RATE * 0.095))
            if seg_len > 24:
                st = np.arange(seg_len, dtype=np.float64) / SAMPLE_RATE
                debris[idx_hit : idx_hit + seg_len] += (
                    _modal_bank(
                        st,
                        bf * rng.uniform(1.4, 3.2),
                        (1.0, 1.54, 2.31, 3.18),
                        (0.05, 0.035, 0.025, 0.016),
                        (0.7, 0.5, 0.35, 0.2),
                        rng,
                    )
                    * 0.65
                )

        rumble_env = _envelope_segment(n, max(0, impact_idx - 64), n, attack_frac=0.04, release_frac=0.55)
        rumble = _sos_bandpass(brown, 34.0, 420.0) * rumble_env * 0.95
        raw = np.tanh((bellow * roar_env * 1.25 + body_bomb * 1.35 + debris + rumble) * 1.35)
        raw = _sos_lowpass(raw, 1850.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.34, rt60=1.08, lp_hz=1380.0)

    elif sound_id == "spore_behemoth_slam":
        # Act I: Ground fracture anticipation -> Act II: Subterranean seismic shockwave bomb -> Act III: Rock scatter
        impact_frac = rng.uniform(0.07, 0.16)
        impact_idx = int(impact_frac * n)
        suck = _reverse_suction(n, 0, max(32, impact_idx - 60), rng, f_lo=80.0, f_hi=650.0) * 0.55
        bomb = _sub_bomb(t, impact_idx, f_start=152.0, f_end=27.0, drop_tau=0.075, sustain_tau=dur * 0.52, punch_gain=0.65)

        crack = np.zeros(n, dtype=np.float64)
        c_len = min(n - impact_idx, int(SAMPLE_RATE * 0.28))
        if c_len > 32:
            ct = np.arange(c_len, dtype=np.float64) / SAMPLE_RATE
            crust = _modal_bank(
                ct,
                bf * 1.9,
                (1.0, 1.48, 2.24, 3.05, 4.18),
                (0.11, 0.08, 0.055, 0.038, 0.024),
                (0.95, 0.72, 0.52, 0.34, 0.20),
                rng,
                glide_factor=-0.28,
                glide_tau=0.04,
            )
            crack[impact_idx : impact_idx + c_len] = crust + _sos_bandpass(white[impact_idx : impact_idx + c_len], 90.0, 1850.0) * exp_decay(ct, 0.09) * 0.7

        debris = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, int(rng.integers(5, 8)), start_frac=impact_frac + 0.14, end_frac=0.90, accelerate=True):
            slen = min(n - pos, int(SAMPLE_RATE * 0.07))
            if slen > 24:
                debris[pos : pos + slen] += _chitin_modal_hits(slen, rng, bf * rng.uniform(2.2, 4.0), 1, 0.032) * 0.55

        tail_env = _envelope_segment(n, impact_idx, n, attack_frac=0.02, release_frac=0.62)
        tail = _sos_bandpass(brown, 36.0, 480.0) * tail_env * 0.88
        raw = np.tanh((suck + bomb * 1.40 + crack * 0.92 + debris + tail) * 1.40)
        raw = _sos_lowpass(raw, 2000.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=16.0, wet=0.30, rt60=0.88, lp_hz=1500.0)

    # =========================================================================
    # 2. HIVE CORE — Cosmic Psychic Monolith / Reaper Organ
    # =========================================================================
    elif sound_id == "hive_core_resonance":
        # Act I: Telepathic pre-hum & harmonic focus (shifted > 60 ms across takes)
        onset_frac = rng.uniform(0.06, 0.16)
        s_pre, e_pre = _window_slice(n, 0.0, onset_frac + 0.12)
        pre_env = _envelope_segment(n, s_pre, e_pre, attack_frac=0.45, release_frac=0.30)
        pre_hum = _fm_tone(t, bf * 1.5, bf * 0.5, 1.8, pjit) * pre_env * 0.45

        # Act II: Monumental alien Reaper brass braaam + monolith chord
        s_main, e_main = _window_slice(n, onset_frac, 0.86)
        main_env = _envelope_segment(n, s_main, e_main, attack_frac=0.07, release_frac=0.34)
        u_m = np.clip((t - t[s_main]) / max(0.2, t[e_main - 1] - t[s_main]), 0.0, 1.0)
        f_horn = bf * (1.0 + 0.08 * np.sin(np.pi * u_m) - 0.14 * (u_m**1.6) + 0.015 * np.sin(2.0 * np.pi * mf * t + pjit))
        braaam = _brass_braaam(t, f_horn, rng, detune_cents=rng.uniform(16.0, 26.0), sub_oct=0.92, shimmer_mix=0.25, formant_shift=formant_shift)

        # Monolith bell harmonics + synaptic electricity arcs
        bell = _modal_bank(
            t,
            bf * 2.0,
            (1.0, 1.498, 2.005, 2.756, 3.89, 5.14),
            (dur * 0.65, dur * 0.55, dur * 0.45, dur * 0.35, dur * 0.28, dur * 0.22),
            (0.50, 0.36, 0.42, 0.28, 0.18, 0.11),
            rng,
        ) * 0.52
        arcs = _micro_arc_zaps(n, rng, int(rng.integers(4, 8)), lo_hz=2600.0, hi_hz=9200.0, spread=(onset_frac + 0.05, 0.85)) * 0.36
        sub_drop = _sub_bomb(t, s_main, f_start=bf * 1.9, f_end=34.0, drop_tau=0.085, sustain_tau=dur * 0.52)

        raw = np.tanh((pre_hum + braaam * main_env * 1.25 + bell * main_env + arcs + sub_drop * 0.90) * 1.32)
        return _abyssal_reverb(raw, rng, predelay_ms=17.0, wet=0.32, rt60=0.98, lp_hz=3800.0, shimmer=0.14)

    elif sound_id == "hive_core_hurt":
        # Prismatic crystal-metal fracture + brass core groan dropping in pitch + synaptic overload
        hit_frac = rng.uniform(0.02, 0.11)
        hit_idx = int(hit_frac * n)
        u = np.clip((t - t[hit_idx]) / max(0.08, dur - t[hit_idx]), 0.0, 1.0)
        f_groan = np.maximum(48.0, bf * (1.24 - 0.56 * (u**0.75)))
        groan_env = _envelope_segment(n, hit_idx, n, attack_frac=0.04, release_frac=0.45)
        groan = _brass_braaam(t, f_groan, rng, detune_cents=24.0, sub_oct=0.65, shimmer_mix=0.30, formant_shift=formant_shift)

        fracture = np.zeros(n, dtype=np.float64)
        flen = n - hit_idx
        ft = np.arange(flen, dtype=np.float64) / SAMPLE_RATE
        fracture[hit_idx:] = _modal_bank(
            ft,
            bf * 3.1,
            (1.0, 1.414, 2.32, 3.17, 4.46, 5.82),
            (0.22, 0.17, 0.12, 0.09, 0.06, 0.04),
            (0.85, 0.68, 0.54, 0.40, 0.28, 0.16),
            rng,
            glide_factor=-0.18,
            glide_tau=0.035,
        )
        zaps = _micro_arc_zaps(n, rng, int(rng.integers(5, 9)), lo_hz=3000.0, hi_hz=11000.0, spread=(hit_frac, 0.78)) * 0.55
        raw = np.tanh((groan * groan_env * 0.95 + fracture * 0.85 + zaps) * 1.28)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.25, rt60=0.68, lp_hz=4400.0, shimmer=0.10)

    elif sound_id == "hive_core_death":
        # Act I: Rising unstable reactor siren -> suction implosion at tear_frac
        tear_frac = rng.uniform(0.36, 0.46)
        tear_idx = int(tear_frac * n)
        s1, e1 = _window_slice(n, 0.0, tear_frac)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_rise = bf * (0.88 + 0.92 * (u1**1.8))
        rise_env = _envelope_segment(n, s1, e1, attack_frac=0.14, release_frac=0.08)
        siren = _brass_braaam(t, f_rise, rng, detune_cents=28.0, sub_oct=0.75, shimmer_mix=0.32, formant_shift=formant_shift) * rise_env
        suck = _reverse_suction(n, max(0, tear_idx - int(0.32 * n)), int(0.30 * n), rng, f_lo=160.0, f_hi=4200.0) * 0.95

        # Act II: Monumental sub-bomb implosion -> shattering monolith prisms
        bomb = _sub_bomb(t, tear_idx, f_start=178.0, f_end=28.0, drop_tau=0.095, sustain_tau=dur * 0.48, punch_gain=0.70)
        shatter = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, int(rng.integers(7, 12)), start_frac=tear_frac + 0.01, end_frac=0.88, accelerate=False):
            slen = min(n - pos, int(SAMPLE_RATE * 0.22))
            if slen > 32:
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                shatter[pos : pos + slen] += (
                    _modal_bank(
                        st,
                        bf * rng.uniform(2.8, 5.4),
                        (1.0, 1.38, 2.09, 2.94, 4.12),
                        (0.16, 0.12, 0.09, 0.06, 0.04),
                        (0.75, 0.58, 0.42, 0.28, 0.16),
                        rng,
                    )
                    * 0.58
                )

        # Act III: Dying descending monolith horn sigh + psionic arcs + void wash
        s3, e3 = _window_slice(n, tear_frac + 0.04, 0.98)
        die_env = _envelope_segment(n, s3, e3, attack_frac=0.05, release_frac=0.52)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_fall = np.maximum(34.0, bf * (1.35 - 0.92 * (u3**0.80)))
        die_horn = _brass_braaam(t, f_fall, rng, detune_cents=22.0, sub_oct=0.95, shimmer_mix=0.20, formant_shift=formant_shift) * die_env
        zaps = _micro_arc_zaps(n, rng, int(rng.integers(8, 13)), lo_hz=2200.0, hi_hz=11500.0, spread=(tear_frac, 0.90)) * 0.48

        raw = np.tanh((siren * 0.85 + suck + bomb * 1.35 + shatter * 0.78 + die_horn * 0.95 + zaps) * 1.30)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.36, rt60=1.18, lp_hz=4200.0, shimmer=0.22)

    elif sound_id == "hive_core_shield":
        # Prismatic geodesic lock-on: bright rising metallic shield chime + tension hum + static repulsion arcs
        lock_frac = rng.uniform(0.03, 0.11)
        lock_idx = int(lock_frac * n)
        chime = np.zeros(n, dtype=np.float64)
        clen = n - lock_idx
        ct = np.arange(clen, dtype=np.float64) / SAMPLE_RATE
        chime[lock_idx:] = _modal_bank(
            ct,
            bf * 1.0,
            (0.5, 1.0, 1.498, 2.004, 2.996, 4.18, 5.42),
            (dur * 0.72, dur * 0.65, dur * 0.55, dur * 0.48, dur * 0.36, dur * 0.26, dur * 0.18),
            (0.55, 0.92, 0.75, 0.64, 0.42, 0.25, 0.14),
            rng,
            glide_factor=0.32,
            glide_tau=0.065,
        )
        hum_env = _envelope_segment(n, lock_idx, n, attack_frac=0.05, release_frac=0.45)
        f_hum = bf * (0.5 + 0.04 * np.sin(2.0 * np.pi * mf * t + pjit))
        hum = _brass_braaam(t, f_hum, rng, detune_cents=14.0, sub_oct=0.60, shimmer_mix=0.35, formant_shift=formant_shift) * hum_env * 0.72
        zaps = _micro_arc_zaps(n, rng, int(rng.integers(5, 9)), lo_hz=3400.0, hi_hz=12800.0, spread=(lock_frac, 0.86)) * 0.58
        raw = np.tanh((chime * 0.95 + hum + zaps) * 1.25)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.28, rt60=0.78, lp_hz=5200.0, shimmer=0.24)

    # =========================================================================
    # 3. APEX MATRIARCH — Eldritch Sovereign Banshee-Choir Empress
    # =========================================================================
    elif sound_id == "apex_matriarch_wail":
        # Act I: Whispering void suction inhale (shifted > 90 ms across takes)
        wail_frac = rng.uniform(0.11, 0.23)
        wail_idx = int(wail_frac * n)
        suck = _reverse_suction(n, 0, max(32, wail_idx), rng, f_lo=260.0, f_hi=4600.0) * 0.78

        # Act II: Soaring multi-voice dissonant banshee-choir shriek
        s2, e2 = _window_slice(n, wail_frac - 0.02, 0.88)
        wail_env = _envelope_segment(n, s2, e2, attack_frac=0.07, release_frac=0.36)
        u2 = np.clip((t - t[s2]) / max(0.15, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_wail = bf * (0.86 + 0.62 * np.sin(np.pi * (u2**0.72)) - 0.22 * (u2**1.8))
        choir = _eldritch_choir(
            t,
            f_wail,
            rng,
            voices=5,
            dissonance="crown_shriek",
            breath_mix=0.34,
            vibrato_hz=rng.uniform(4.4, 6.8),
            formant_shift=formant_shift,
        )

        # Subterranean anchor + crystalline crown overtones (keeps it colossal, not thin)
        sub_anchor = _sub_bomb(t, wail_idx, f_start=118.0, f_end=36.0, drop_tau=0.08, sustain_tau=dur * 0.45) * 0.46
        crown_bell = _modal_bank(
            t,
            bf * 2.5,
            (1.0, 1.587, 2.378, 3.32),
            (dur * 0.55, dur * 0.42, dur * 0.30, dur * 0.20),
            (0.42, 0.32, 0.22, 0.14),
            rng,
        ) * 0.42

        # Act III: Spectral portal shimmer decay
        raw = np.tanh((suck + choir * wail_env * 1.48 + sub_anchor + crown_bell * wail_env) * 1.28)
        raw = _sos_highpass(raw, 48.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=18.0, wet=0.36, rt60=1.08, lp_hz=7200.0, shimmer=0.45)

    elif sound_id == "apex_matriarch_hurt":
        # Obsidian crystal shriek-snap + discordant banshee gasp
        hit_frac = rng.uniform(0.02, 0.11)
        hit_idx = int(hit_frac * n)
        gasp_env = _envelope_segment(n, hit_idx, n, attack_frac=0.04, release_frac=0.45)
        u = np.clip((t - t[hit_idx]) / max(0.08, dur - t[hit_idx]), 0.0, 1.0)
        f_gasp = bf * (1.42 - 0.65 * (u**0.68))
        choir = _eldritch_choir(t, f_gasp, rng, voices=4, dissonance="tritone", breath_mix=0.42, vibrato_hz=rng.uniform(6.0, 8.2), formant_shift=formant_shift)

        snap = np.zeros(n, dtype=np.float64)
        slen = n - hit_idx
        st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
        snap[hit_idx:] = _modal_bank(
            st,
            bf * 3.4,
            (1.0, 1.49, 2.38, 3.65, 4.92),
            (0.09, 0.065, 0.045, 0.032, 0.020),
            (0.85, 0.65, 0.45, 0.30, 0.18),
            rng,
        )
        raw = np.tanh((choir * gasp_env * 1.25 + snap * 0.85) * 1.25)
        raw = _sos_highpass(raw, 55.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.28, rt60=0.72, lp_hz=6200.0, shimmer=0.24)

    elif sound_id == "apex_matriarch_death":
        # Act I: Anguished sovereign choral crescendo (0..tear_frac)
        tear_frac = rng.uniform(0.36, 0.46)
        tear_idx = int(tear_frac * n)
        s1, e1 = _window_slice(n, 0.0, tear_frac)
        scream_env = _envelope_segment(n, s1, e1, attack_frac=0.08, release_frac=0.10)
        u1 = np.clip(t / max(0.2, t[e1 - 1]), 0.0, 1.0)
        f_ascend = bf * (0.95 + 0.72 * (u1**1.4))
        scream = _eldritch_choir(t, f_ascend, rng, voices=5, dissonance="crown_shriek", breath_mix=0.26, vibrato_hz=rng.uniform(5.8, 7.6), formant_shift=formant_shift) * scream_env
        suck = _reverse_suction(n, max(0, tear_idx - int(0.28 * n)), int(0.26 * n), rng, f_lo=280.0, f_hi=6200.0) * 0.92

        # Act II: Gravitational dimensional rupture bomb
        bomb = _sub_bomb(t, tear_idx, f_start=185.0, f_end=31.0, drop_tau=0.085, sustain_tau=dur * 0.44, punch_gain=0.65)

        # Act III: Crystal-veil disintegration + descending lament choir fading into the void
        shatter = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, int(rng.integers(8, 14)), start_frac=tear_frac + 0.01, end_frac=0.90, accelerate=False):
            slen = min(n - pos, int(SAMPLE_RATE * 0.24))
            if slen > 32:
                st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
                shatter[pos : pos + slen] += (
                    _modal_bank(
                        st,
                        bf * rng.uniform(3.2, 6.2),
                        (1.0, 1.414, 2.23, 3.16, 4.42),
                        (0.18, 0.14, 0.10, 0.07, 0.045),
                        (0.75, 0.56, 0.40, 0.26, 0.14),
                        rng,
                    )
                    * 0.52
                )

        s3, e3 = _window_slice(n, tear_frac + 0.02, 0.98)
        lament_env = _envelope_segment(n, s3, e3, attack_frac=0.05, release_frac=0.58)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_desc = np.maximum(72.0, bf * (1.45 - 0.88 * (u3**0.78)))
        lament = _eldritch_choir(t, f_desc, rng, voices=4, dissonance="minor_lament", breath_mix=0.40, vibrato_hz=rng.uniform(4.2, 5.8), formant_shift=formant_shift) * lament_env

        raw = np.tanh((scream * 1.15 + suck + bomb * 1.20 + shatter * 0.82 + lament * 1.10) * 1.25)
        raw = _sos_highpass(raw, 48.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=22.0, wet=0.38, rt60=1.24, lp_hz=6600.0, shimmer=0.38)

    elif sound_id == "apex_matriarch_barrage":
        # Whip-crack void tear -> rapid staggered plasma/crystal spore volley -> high psychic trailing screech
        bolts = np.zeros(n, dtype=np.float64)
        volley_count = int(rng.integers(5, 8))
        positions = sorted(rng.uniform(0.04, 0.72) for _ in range(volley_count))
        for b_idx, pos_frac in enumerate(positions):
            pos = int(pos_frac * n)
            blen = min(n - pos, int(SAMPLE_RATE * rng.uniform(0.09, 0.16)))
            if blen < 32:
                continue
            bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
            f_bolt = bf * rng.uniform(1.8, 3.2) * (1.35 * np.exp(-bt / 0.028) + 0.55)
            chirp = _fm_tone(bt, f_bolt, f_bolt * 0.71, 2.4, rng.uniform(0.0, 6.28))
            snap = _modal_bank(bt, bf * 3.8, (1.0, 1.62, 2.54), (0.04, 0.028, 0.018), (0.7, 0.45, 0.25), rng)
            env_b = (1.0 - np.exp(-bt / 0.0025)) * np.exp(-bt / 0.045)
            bolts[pos : pos + blen] += (chirp * 0.75 + snap * 0.65) * env_b * (0.82 + 0.25 * (b_idx / volley_count))

        tail_start = rng.uniform(0.18, 0.28)
        s_tail, e_tail = _window_slice(n, tail_start, 0.96)
        shriek_env = _envelope_segment(n, s_tail, e_tail, attack_frac=0.14, release_frac=0.45)
        f_shriek = bf * (1.25 - 0.35 * ((t - t[s_tail]) / max(0.1, dur)))
        choir_tail = _eldritch_choir(t, np.maximum(110.0, f_shriek), rng, voices=3, dissonance="tritone", breath_mix=0.30, formant_shift=formant_shift) * shriek_env * 0.65
        zaps = _micro_arc_zaps(n, rng, int(rng.integers(6, 10)), lo_hz=3600.0, hi_hz=12500.0, spread=(0.03, 0.85)) * 0.48
        raw = np.tanh((bolts * 1.15 + choir_tail + zaps) * 1.25)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.28, rt60=0.72, lp_hz=6400.0, shimmer=0.22)

    # =========================================================================
    # 4. SHARED BOSS & RITUAL CINEMATICS
    # =========================================================================
    elif sound_id == "boss_phase_shift":
        # Act I: Escalating harmonic alarm draw -> Act II: Vacuum cut -> Sub bomb + Brass/Choir power swell
        drop_frac = rng.uniform(0.28, 0.34)
        drop_idx = int(drop_frac * n)
        s1, e1 = _window_slice(n, 0.0, drop_frac)
        u1 = np.clip(t / max(0.15, t[e1 - 1]), 0.0, 1.0)
        rise_env = _envelope_segment(n, s1, e1, attack_frac=0.18, release_frac=0.06)
        suck = _reverse_suction(n, 0, max(32, drop_idx - 48), rng, f_lo=130.0, f_hi=4400.0) * 1.05
        alarm = _fm_tone(t, bf * (0.9 + 1.15 * (u1**1.6)), bf * 0.75, 2.2, pjit) * rise_env * 0.52

        bomb = _sub_bomb(t, drop_idx, f_start=182.0, f_end=30.0, drop_tau=0.075, sustain_tau=dur * 0.50, punch_gain=0.72)

        s2, e2 = _window_slice(n, drop_frac, 0.98)
        chord_env = _envelope_segment(n, s2, e2, attack_frac=0.04, release_frac=0.48)
        u2 = np.clip((t - t[s2]) / max(0.2, t[e2 - 1] - t[s2]), 0.0, 1.0)
        f_pow = bf * (0.98 - 0.18 * (u2**1.3))
        brass = _brass_braaam(t, f_pow, rng, detune_cents=20.0, sub_oct=0.90, shimmer_mix=0.28, formant_shift=formant_shift)
        choir = _eldritch_choir(t, f_pow * 2.0, rng, voices=4, dissonance="tritone", breath_mix=0.22, formant_shift=formant_shift)
        shock_snap = _micro_arc_zaps(n, rng, 6, lo_hz=2800.0, hi_hz=11200.0, spread=(drop_frac, drop_frac + 0.25)) * 0.52

        raw = np.tanh((suck + alarm + bomb * 1.40 + (brass * 0.85 + choir * 0.62) * chord_env + shock_snap) * 1.32)
        return _abyssal_reverb(raw, rng, predelay_ms=19.0, wet=0.35, rt60=1.12, lp_hz=4800.0, shimmer=0.22)

    elif sound_id == "fungal_tide_horn":
        # Doom / War-of-the-Worlds colossal tribal bio-warhorn in cavern fifths + distant fungal answer
        s1, e1 = _window_slice(n, 0.0, 0.14)
        inhale = _swept_resonant_filter(pink * 0.6 + brown * 0.6, 120.0, 480.0, q=2.2) * _envelope_segment(n, s1, e1, attack_frac=0.45, release_frac=0.20)

        horn_start = int(0.11 * n)
        s2, e2 = _window_slice(n, 0.11, 0.82)
        horn_env = _envelope_segment(n, s2, e2, attack_frac=0.09, release_frac=0.32)
        u2 = np.clip((t - t[s2]) / max(0.25, t[e2 - 1] - t[s2]), 0.0, 1.0)
        # Heroic-ominous pitch bend down into deep sustained warhorn drone
        f_horn = bf * (0.94 + 0.14 * np.exp(-u2 / 0.18) - 0.14 * (u2**1.4))
        root_brass = _brass_braaam(t, f_horn, rng, detune_cents=16.0, sub_oct=1.0, shimmer_mix=0.14, formant_shift=formant_shift)
        fifth_brass = _brass_braaam(t, f_horn * 1.498, rng, detune_cents=12.0, sub_oct=0.45, shimmer_mix=0.12, formant_shift=formant_shift) * 0.55
        glottal_bellow = _glottal_sub_source(t, f_horn * 0.99, rng, harmonics=8, sub_weight=0.78, rattle_hz=19.0)
        glottal_bellow = _apply_formant_bank(glottal_bellow, [(110.0, 2.5, 1.0), (290.0, 3.0, 0.65)]) * 0.72
        horn_punch = _sub_bomb(t, horn_start, f_start=120.0, f_end=31.0, drop_tau=0.08, sustain_tau=dur * 0.45) * 0.85

        # Distant answering roar in Act III
        s3, e3 = _window_slice(n, 0.58, 0.96)
        ans_env = _envelope_segment(n, s3, e3, attack_frac=0.22, release_frac=0.52)
        ans = _sos_bandpass(_glottal_sub_source(t, f_horn * 1.25, rng, harmonics=7, rattle_hz=23.0), 85.0, 950.0) * ans_env * 0.42

        raw = np.tanh((inhale * 0.62 + (root_brass + fifth_brass + glottal_bellow) * horn_env * 0.88 + horn_punch + ans) * 1.28)
        raw = _sos_lowpass(raw, 2800.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=21.0, wet=0.34, rt60=1.18, lp_hz=2200.0)

    elif sound_id == "hive_altar_awaken":
        # Distinct Hive Monolith ritual: heavy carved stone slab grinding -> deep bronze monolith bell + Reaper brass rise
        # Act I (0..0.38): Heavy ancient carved stone blocks sliding into lock
        s1, e1 = _window_slice(n, 0.0, 0.40)
        grind_env = _envelope_segment(n, s1, e1, attack_frac=0.15, release_frac=0.22)
        stone_scrape = _swept_resonant_filter(brown * 1.1 + pink * 0.45, 55.0, 380.0, q=3.2) * grind_env
        stones = np.zeros(n, dtype=np.float64)
        for pos_frac in (0.04, 0.18, 0.30, 0.36):
            pos = int(pos_frac * n)
            slen = min(n - pos, int(SAMPLE_RATE * 0.16))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            stones[pos : pos + slen] += _modal_bank(st, 68.0, (1.0, 1.54, 2.28), (0.09, 0.06, 0.04), (0.85, 0.55, 0.32), rng) * 0.72

        # Act II (0.37): Monumental subterranean monolith bronze/stone bell toll + sub drop
        toll_idx = int(0.37 * n)
        toll_bomb = _sub_bomb(t, toll_idx, f_start=132.0, f_end=28.0, drop_tau=0.09, sustain_tau=dur * 0.48, punch_gain=0.65)
        bell = np.zeros(n, dtype=np.float64)
        blen = n - toll_idx
        bt = np.arange(blen, dtype=np.float64) / SAMPLE_RATE
        bell[toll_idx:] = _modal_bank(
            bt,
            bf * 1.15,
            (0.5, 1.0, 1.498, 2.0, 2.756, 3.89),
            (dur * 0.65, dur * 0.60, dur * 0.48, dur * 0.40, dur * 0.28, dur * 0.18),
            (0.60, 0.92, 0.62, 0.50, 0.32, 0.18),
            rng,
        )

        # Act III (0.42..0.98): Dark Hive Reaper brass chord rising from beneath the earth
        s3, e3 = _window_slice(n, 0.42, 0.98)
        brass_env = _envelope_segment(n, s3, e3, attack_frac=0.18, release_frac=0.45)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_rise = bf * (0.88 + 0.18 * (u3**0.85))
        brass = _brass_braaam(t, f_rise, rng, detune_cents=18.0, sub_oct=0.92, shimmer_mix=0.16, formant_shift=formant_shift) * brass_env

        raw = np.tanh((stone_scrape * 0.82 + stones * 0.75 + toll_bomb * 1.25 + bell * 0.85 + brass * 0.95) * 1.32)
        raw = _sos_lowpass(raw, 2500.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.35, rt60=1.18, lp_hz=2100.0)

    elif sound_id == "matriarch_altar_awaken":
        # Distinct Matriarch ritual: crystalline seals shattering -> dimensional suction tear -> high sovereign banshee choir
        # Act I (0..0.40): Rapid sequence of obsidian crystal seals cracking & shattering
        seals = np.zeros(n, dtype=np.float64)
        for idx_s, pos_frac in enumerate((0.03, 0.11, 0.20, 0.28, 0.34)):
            pos = int(pos_frac * n)
            slen = min(n - pos, int(SAMPLE_RATE * 0.18))
            st = np.arange(slen, dtype=np.float64) / SAMPLE_RATE
            seals[pos : pos + slen] += (
                _modal_bank(
                    st,
                    bf * (3.2 + 0.65 * idx_s),
                    (1.0, 1.49, 2.29, 3.45, 4.78),
                    (0.11, 0.08, 0.055, 0.038, 0.024),
                    (0.75, 0.58, 0.42, 0.28, 0.16),
                    rng,
                )
                * 0.72
            )

        # Act II (0.28..0.48): Vacuum dimensional tear -> sub-bomb rupture
        tear_idx = int(0.46 * n)
        suck = _reverse_suction(n, int(0.24 * n), max(32, tear_idx - int(0.24 * n)), rng, f_lo=260.0, f_hi=6400.0) * 1.05
        rift_bomb = _sub_bomb(t, tear_idx, f_start=175.0, f_end=33.0, drop_tau=0.08, sustain_tau=dur * 0.42, punch_gain=0.62)

        # Act III (0.46..0.98): Sovereign high banshee choir announcing her arrival + dimensional sparks
        s3, e3 = _window_slice(n, 0.46, 0.98)
        choir_env = _envelope_segment(n, s3, e3, attack_frac=0.10, release_frac=0.46)
        u3 = np.clip((t - t[s3]) / max(0.2, t[e3 - 1] - t[s3]), 0.0, 1.0)
        f_choir = bf * 2.15 * (0.94 + 0.28 * np.sin(np.pi * (u3**0.75)))
        choir = _eldritch_choir(t, f_choir, rng, voices=5, dissonance="crown_shriek", breath_mix=0.32, vibrato_hz=5.8, formant_shift=formant_shift) * choir_env
        sparks = _micro_arc_zaps(n, rng, 10, lo_hz=3800.0, hi_hz=12800.0, spread=(0.45, 0.92)) * 0.45

        raw = np.tanh((seals * 0.85 + suck + rift_bomb * 1.05 + choir * 1.32 + sparks) * 1.25)
        raw = _sos_highpass(raw, 48.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=19.0, wet=0.36, rt60=1.20, lp_hz=6400.0, shimmer=0.38)

    elif sound_id == "boss_weakpoint_crit":
        # Tactile dorsal weak-point rupture: crisp organic membrane split -> resonant neural scream -> sub crunch
        onset = int(rng.uniform(0.015, 0.065) * n)
        bomb = _sub_bomb(t, onset, f_start=168.0, f_end=34.0, drop_tau=0.065, sustain_tau=dur * 0.42, punch_gain=0.72)
        crack = _chitin_modal_hits(n, rng, bf * 3.4, 5, 0.045, spread_frac=0.32) * 0.88
        modal = modal_resonator_bank(
            n,
            (bf * 1.5, bf * 2.38, bf * 3.72, bf * 5.25),
            (0.24, 0.16, 0.10, 0.06),
            rng,
            amplitudes=(0.80, 0.62, 0.44, 0.25),
        )
        squelch = _wet_squelch_layer(n, rng, bf * 1.3, 6, intensity=0.95) * adsr_envelope(n, 0.02, 0.20, 0.45, 0.35)
        wail = _apply_formant_bank(
            _glottal_sub_source(t, bf * (1.32 - 0.48 * (t / dur)), rng, harmonics=9, sub_weight=0.75, rattle_hz=24.0),
            [(380.0 * formant_shift, 3.0, 1.0), (1120.0 * formant_shift, 3.6, 0.8)],
        ) * adsr_envelope(n, 0.04, 0.22, 0.40, 0.38) * 0.72
        raw = np.tanh((bomb * 1.25 + crack + modal * 0.75 + squelch + wail) * 1.28)
        return _abyssal_reverb(raw, rng, predelay_ms=12.0, wet=0.26, rt60=0.62, lp_hz=5600.0)

    elif sound_id == "boss_carapace_ricochet":
        # Heavy armored carapace deflection: stiff modal chitin clang -> metallic pitch ricochet -> muted low thud
        onset = int(rng.uniform(0.010, 0.050) * n)
        rem = n - onset
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        modal = np.zeros(n, dtype=np.float64)
        modal[onset:] = modal_resonator_bank(
            rem,
            (bf * 1.0, bf * 1.68, bf * 2.54, bf * 3.86, bf * 5.42),
            (0.14, 0.10, 0.07, 0.045, 0.025),
            rng,
            amplitudes=(0.75, 0.85, 0.65, 0.45, 0.25),
        )
        zip_up = np.zeros(n, dtype=np.float64)
        f_ric = bf * (1.1 + 1.85 * np.exp(-rt / 0.09))
        zip_up[onset:] = _fm_tone(rt, f_ric, bf * 0.62, 1.9, pjit) * exp_decay(rt, 0.14) * 0.68
        snap = _sos_bandpass(rng.normal(0.0, 1.0, n), 1800.0, 9600.0) * exp_decay(np.maximum(0.0, t - t[onset]), 0.018) * 0.52
        raw = np.tanh((modal + zip_up + snap) * 1.22)
        return _cavern_reverb(_sos_highpass(raw, 120.0), rng, wet=0.18, rt60=0.40, lp_hz=6800.0)

    elif sound_id == "colony_raid_horn":
        # Urgent rival-swarm bronze/chitin siege warhorn: staggered dual-blast + dissonant swarm drone
        s1, e1 = _window_slice(n, 0.02, 0.44)
        s2, e2 = _window_slice(n, 0.46, 0.96)
        e_b1 = _envelope_segment(n, s1, e1, attack_frac=0.14, release_frac=0.30)
        e_b2 = _envelope_segment(n, s2, e2, attack_frac=0.10, release_frac=0.40)
        f_h1 = bf * (0.96 + 0.12 * (t / dur))
        f_h2 = bf * (1.12 - 0.10 * (t / dur))
        horn1 = _brass_braaam(t, f_h1, rng, detune_cents=15.0, sub_oct=0.90, shimmer_mix=0.16, formant_shift=formant_shift) * e_b1
        horn2 = (
            _brass_braaam(t, f_h2, rng, detune_cents=18.0, sub_oct=0.95, shimmer_mix=0.22, formant_shift=formant_shift)
            + _brass_braaam(t, f_h2 * 1.414, rng, detune_cents=12.0, sub_oct=0.40, shimmer_mix=0.18, formant_shift=formant_shift) * 0.55
        ) * e_b2
        punch = _sub_bomb(t, s2, f_start=135.0, f_end=33.0, drop_tau=0.075, sustain_tau=dur * 0.36) * 0.85
        rattle = _chitin_modal_hits(n, rng, bf * 4.8, 7, 0.04, spread_frac=0.75) * 0.32
        raw = np.tanh((horn1 * 0.90 + horn2 * 1.05 + punch + rattle) * 1.28)
        raw = _sos_lowpass(raw, 3400.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=20.0, wet=0.35, rt60=1.15, lp_hz=2800.0)

    else:  # "mob_step_colossal"
        # Heavy pre-step floor groan -> chest-thumping tectonic footfall bomb -> stone crunch & mycelium squish
        stomp_frac = rng.uniform(0.03, 0.13)
        stomp_idx = int(stomp_frac * n)
        creak = _sos_bandpass(brown, 45.0, 260.0) * _envelope_segment(n, 0, max(16, stomp_idx + 120), attack_frac=0.3, release_frac=0.3) * 0.45
        bomb = _sub_bomb(t, stomp_idx, f_start=bf * 2.2, f_end=30.0, drop_tau=0.055, sustain_tau=dur * 0.48, punch_gain=0.60)

        crunch = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, int(rng.integers(3, 6)), start_frac=stomp_frac + 0.02, end_frac=0.72, accelerate=True):
            slen = min(n - pos, int(SAMPLE_RATE * 0.055))
            if slen > 24:
                crunch[pos : pos + slen] += _chitin_modal_hits(slen, rng, bf * rng.uniform(2.5, 4.5), 1, 0.028) * 0.55

        squish = _wet_squelch_layer(n, rng, bf * 1.7, int(rng.integers(2, 5)), intensity=0.45) * _envelope_segment(n, stomp_idx, n, attack_frac=0.04, release_frac=0.5)
        ground_shake = _sos_bandpass(brown, 36.0, 460.0) * _envelope_segment(n, stomp_idx, n, attack_frac=0.03, release_frac=0.58) * 0.85

        raw = np.tanh((creak + bomb * 1.38 + crunch + squish + ground_shake) * 1.35)
        raw = _sos_lowpass(raw, 1850.0, order=2)
        return _abyssal_reverb(raw, rng, predelay_ms=14.0, wet=0.28, rt60=0.72, lp_hz=1500.0)
