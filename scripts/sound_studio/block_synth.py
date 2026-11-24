"""Dedicated physical-modeling & multi-stage narrative synthesizers for all 49 block sound events."""

from __future__ import annotations

import numpy as np

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
    fold_seamless_loop,
    modal_resonator_bank,
)

BLOCK_FUNCTIONAL_IDS: frozenset[str] = frozenset(
    {
        "spore_vent_puff",
        "purification_pylon_pulse",
        "purification_turret_fire",
        "purification_turret_upgrade",
        "purification_turret_repair",
        "synaptic_node_mature",
        "symbiotic_node_charge",
        "symbiotic_node_teleport",
        "spore_bulb_burst",
        "ephemeral_chitin_crumble",
        "mycelial_rift_hum",
        "hive_altar_awaken",
        "matriarch_altar_awaken",
        "synaptic_bench_synthesize",
        "sovereign_nexus_pulse",
        "colony_dialect_tone_1",
        "colony_dialect_tone_2",
        "colony_dialect_tone_3",
        "reclaimed_bloom_harvest",
        "reclaimed_bloom_pulse",
        "colony_dialect_mastered",
        "colony_dialect_conflict",
        "colony_reconstruction_pulse",
        "spore_updraft_vent_pulse",
    }
)


def _modal_bank(
    t: np.ndarray,
    base_freq: float,
    ratios: tuple[float, ...],
    decays: tuple[float, ...],
    amps: tuple[float, ...],
    rng: np.random.Generator,
    *,
    mode_jitter: float = 0.035,
    glide_factor: float = 0.0,
    glide_tau: float = 0.025,
) -> np.ndarray:
    """Synthesize physical plate/crystal/bell eigenmodes with realistic off-center strike variation."""
    n = len(t)
    out = np.zeros(n, dtype=np.float64)
    nyquist_guard = SAMPLE_RATE * 0.45
    for idx, (r, tau, amp) in enumerate(zip(ratios, decays, amps)):
        f_mode = float(base_freq) * r * float(rng.uniform(1.0 - mode_jitter, 1.0 + mode_jitter))
        if f_mode >= nyquist_guard or f_mode < 22.0:
            continue
        strike_amp = amp * float(rng.uniform(0.58, 1.38))
        phase = float(rng.uniform(0.0, 2.0 * np.pi))
        if abs(glide_factor) > 1e-4:
            inst_f = np.maximum(22.0, f_mode * (1.0 + glide_factor * exp_decay(t, glide_tau)))
            ph = 2.0 * np.pi * np.cumsum(inst_f) / SAMPLE_RATE + phase
        else:
            ph = 2.0 * np.pi * f_mode * t + phase
        # Slight mode beating from imperfect geometry on main partials
        if idx < 2:
            ph2 = ph * float(rng.uniform(1.002, 1.008)) + float(rng.uniform(0.0, 6.28))
            partial = 0.72 * np.sin(ph) + 0.35 * np.sin(ph2)
        else:
            partial = np.sin(ph)
        out += strike_amp * partial * exp_decay(t, max(0.008, float(tau) * float(rng.uniform(0.85, 1.15))))
    return out


def _bounce_train(
    n: int,
    rng: np.random.Generator,
    count: int,
    start_frac: float = 0.08,
    end_frac: float = 0.90,
    accelerate: bool = True,
) -> list[int]:
    """Generate realistic gravity-accelerating or settling shard/pebble impact indices."""
    span = max(0.02, end_frac - start_frac)
    points: list[int] = []
    for i in range(max(1, count)):
        u = i / max(1, count)
        if accelerate:
            # Earlier gaps wider, bounces speed up toward rest
            pos_frac = start_frac + span * (1.0 - (1.0 - u) ** 1.45)
        else:
            pos_frac = start_frac + span * u
        jit = float(rng.uniform(-0.025, 0.025))
        idx = int(np.clip(pos_frac + jit, 0.0, 0.94) * n)
        points.append(min(max(0, idx), max(0, n - 48)))
    return sorted(points)


def _micro_arc_zaps(
    n: int,
    rng: np.random.Generator,
    count: int,
    lo_hz: float = 3500.0,
    hi_hz: float = 13500.0,
    spread: tuple[float, float] = (0.04, 0.85),
) -> np.ndarray:
    """Short electrical/synaptic spark discharges."""
    out = np.zeros(n, dtype=np.float64)
    for _ in range(max(1, count)):
        pos = min(max(0, n - 48), int(rng.uniform(spread[0], spread[1]) * n))
        zlen = min(n - pos, max(48, int(SAMPLE_RATE * rng.uniform(0.004, 0.015))))
        st = np.linspace(0.0, zlen / SAMPLE_RATE, zlen, endpoint=False)
        snap = _sos_bandpass(rng.normal(0.0, 1.0, zlen), lo_hz, hi_hz) * exp_decay(st, 0.003)
        chirp = np.sin(2.0 * np.pi * np.linspace(hi_hz * 0.6, lo_hz * 0.7, zlen) * st + rng.uniform(0, 6.28)) * 0.45
        out[pos : pos + zlen] += (snap + chirp) * rng.uniform(0.55, 1.15)
    return out


# =============================================================================
# 7 Independent Block Material Physical-Modeling Engines
# =============================================================================

def _surf_mycelium(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Warm damp fibrous fungal carpet & root tendrils (no metallic ring, no cartoon bubbles)."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)

    if action == "break":
        # Zipper-like tearing of root fibers + soft damp loam crumble + airy spore exhale
        tears = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 14, start_frac=0.0, end_frac=0.68, accelerate=False):
            flen = min(n - pos, max(96, int(SAMPLE_RATE * rng.uniform(0.018, 0.042))))
            ft = np.linspace(0.0, flen / SAMPLE_RATE, flen, endpoint=False)
            fc = rng.uniform(340.0, 1450.0)
            rip = _sos_bandpass(rng.normal(0.0, 1.0, flen), fc * 0.72, fc * 1.48) * np.sin(np.linspace(0.0, np.pi, flen))
            tears[pos : pos + flen] += rip * rng.uniform(0.55, 1.15)
        loam = _sos_bandpass(brown * 0.7 + pink * 0.5, 95.0, 560.0) * exp_decay(t, dur * 0.40) * 0.90
        spore_exhale = _sos_bandpass(pink, 1400.0, 3200.0) * adsr_envelope(n, 0.12, 0.25, 0.35, 0.42) * 0.30
        raw = tears * 1.15 + loam + spore_exhale
    elif action == "step":
        # Cushioned velvet/moss press + dry-damp root brush release (zero sharp clicks)
        press = _sos_lowpass(_sos_bandpass(brown, 65.0, 260.0), 320.0) * adsr_envelope(n, 0.08, 0.28, 0.30, 0.38) * 1.05
        brush_start = min(n - 64, int(SAMPLE_RATE * rng.uniform(0.032, 0.055)))
        brush = np.zeros(n, dtype=np.float64)
        blen = n - brush_start
        bt = np.linspace(0.0, blen / SAMPLE_RATE, blen, endpoint=False)
        brush[brush_start:] = _sos_bandpass(pink[brush_start:], 420.0, 1450.0) * exp_decay(bt, dur * 0.28) * 0.52
        raw = press + brush
    elif action == "place":
        seat = _sos_bandpass(brown * 0.8 + pink * 0.4, 75.0, 390.0) * exp_decay(t, dur * 0.30) * 1.15
        latches = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 3, start_frac=0.14, end_frac=0.65, accelerate=False):
            rlen = min(n - pos, int(SAMPLE_RATE * 0.045))
            rt = np.linspace(0.0, rlen / SAMPLE_RATE, rlen, endpoint=False)
            latches[pos : pos + rlen] += _sos_bandpass(rng.normal(0.0, 1.0, rlen), 360.0, 1650.0) * exp_decay(rt, 0.020) * 0.65
        raw = seat + latches
    elif action == "hit":
        # Ultra-damped dull spongey punch (low centroid < 700 Hz, dry zero-ring)
        sponge = _sos_bandpass(brown * 0.85 + pink * 0.35, 70.0, 460.0) * exp_decay(t, 0.045) * 1.25
        pat = _sos_bandpass(pink, 320.0, 1100.0) * exp_decay(t, 0.025) * 0.45
        raw = sponge + pat
    else:  # fall
        thump = _sos_bandpass(brown, 48.0, 210.0) * exp_decay(t, dur * 0.36) * 1.30
        puff = _sos_bandpass(pink, 650.0, 2700.0) * adsr_envelope(n, 0.06, 0.26, 0.35, 0.40) * 0.48
        raw = np.tanh((thump + puff) * 1.25)

    return _cavern_reverb(_sos_lowpass(raw, 3400.0), rng, wet=0.10, rt60=0.28, lp_hz=2000.0)


def _surf_antibody_cyst(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Pressurized viscous mucus sac: taut rubber membrane, viscoelastic pitch sag & suction pops."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    pink = _pink_noise(rng, n)

    if action == "break":
        # 35 ms membrane stretch -> sudden sac RUPTURE POP -> heavy downward viscous splat + 7 mucus drip pops
        pre_n = min(n // 4, int(SAMPLE_RATE * 0.038))
        wave = np.zeros(n, dtype=np.float64)
        if pre_n > 24:
            pt = np.linspace(0.0, pre_n / SAMPLE_RATE, pre_n, endpoint=False)
            wave[:pre_n] = np.sin(2.0 * np.pi * np.linspace(bf * 0.9, bf * 1.9, pre_n) * pt) * (pt / max(1e-4, pt[-1])) * 0.45
        pop_start = max(16, pre_n - 12)
        rlen = n - pop_start
        rt = np.linspace(0.0, rlen / SAMPLE_RATE, rlen, endpoint=False)
        pop = np.sin(2.0 * np.pi * np.cumsum(np.maximum(45.0, bf * (0.42 + 2.2 * exp_decay(rt, 0.024)))) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(rt, dur * 0.26) * 1.05
        splat = _swept_resonant_filter(pink[pop_start:], 1800.0, 260.0, q=2.8) * exp_decay(rt, dur * 0.40) * 1.15
        drips = _wet_squelch_layer(rlen, rng, bf * 1.35, 7, intensity=1.15) * adsr_envelope(rlen, 0.08, 0.25, 0.45, 0.40)
        wave[pop_start:] += pop + splat + drips * 0.85
        raw = np.tanh(wave * 1.3)
    elif action == "step":
        # Soft jelly push + unmistakable sticky wet suction peel-off chirp (+45..70 ms)
        push = np.sin(2.0 * np.pi * np.cumsum(bf * (0.65 + 0.35 * np.sin(np.pi * t / dur))) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(t, dur * 0.35) * 0.72
        suck = np.zeros(n, dtype=np.float64)
        s_idx = min(n - 64, int(SAMPLE_RATE * rng.uniform(0.044, 0.072)))
        slen = min(n - s_idx, int(SAMPLE_RATE * 0.068))
        if slen > 32:
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            f_up = rng.uniform(340.0, 480.0) * np.exp(1.25 * st / max(1e-4, st[-1]))
            suck[s_idx : s_idx + slen] = np.sin(2.0 * np.pi * np.cumsum(f_up) / SAMPLE_RATE) * np.sin(np.linspace(0.0, np.pi, slen)) * 0.95
        squish = _wet_squelch_layer(n, rng, bf * 1.1, 3, intensity=0.65) * exp_decay(t, dur * 0.42)
        raw = push + suck + squish
    elif action == "place":
        # Heavy wet gelatinous schlop + membrane jelly wobble
        f_wob = np.maximum(48.0, bf * (0.72 + 0.45 * exp_decay(t, 0.04)) * (1.0 + 0.14 * np.sin(2.0 * np.pi * 21.0 * t)))
        jiggle = np.sin(2.0 * np.pi * np.cumsum(f_wob) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(t, dur * 0.44)
        slosh = _wet_squelch_layer(n, rng, bf * 0.95, 5, intensity=0.90) * exp_decay(t, dur * 0.40)
        raw = np.tanh((jiggle * 0.85 + slosh) * 1.25)
    elif action == "hit":
        # Bouncy fluid-filled rubber drum slap + internal fluid shimmer (distinct per take)
        hit_pos = min(n - 64, int(SAMPLE_RATE * rng.uniform(0.0, 0.014)))
        skin = np.zeros(n, dtype=np.float64)
        ht = np.linspace(0.0, (n - hit_pos) / SAMPLE_RATE, n - hit_pos, endpoint=False)
        f_drop = np.maximum(65.0, bf * rng.uniform(0.85, 1.25) * (0.72 + 1.05 * exp_decay(ht, 0.022)))
        skin[hit_pos:] = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(ht, 0.068) * 1.05
        slosh = _swept_resonant_filter(pink, rng.uniform(1100.0, 1950.0), rng.uniform(320.0, 560.0), q=3.4) * exp_decay(t, 0.075) * 0.90
        pops = _wet_squelch_layer(n, rng, bf * 1.4, 3, intensity=0.80) * exp_decay(t, 0.085)
        raw = skin + slosh + pops
    else:  # fall
        belly = np.sin(2.0 * np.pi * np.cumsum(np.maximum(42.0, bf * (0.48 + 1.45 * exp_decay(t, 0.036)))) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(t, dur * 0.42) * 1.1
        splat = _wet_squelch_layer(n, rng, bf * 1.25, 8, intensity=1.2) * adsr_envelope(n, 0.03, 0.25, 0.40, 0.42)
        raw = np.tanh((belly + splat) * 1.3)

    return _cavern_reverb(raw, rng, wet=0.16, rt60=0.38, lp_hz=3000.0)


def _surf_chitin(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Dry hollow insectoid exoskeleton: clamped-plate eigenmodes [1.0, 2.756, 5.404, 8.93], zero wetness."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    ratios = (1.0, 2.756, 5.404, 8.93)
    decays = (0.045, 0.028, 0.018, 0.010)
    amps = (0.55, 0.42, 0.26, 0.14)

    if action == "break":
        # Crisp hollow carapace SNAP-CRACK + 10 dry accelerating plate shards
        snap = _sos_bandpass(rng.normal(0.0, 1.0, n), 2400.0, 11800.0) * exp_decay(t, 0.018) * 1.15
        main_plate = _modal_bank(t, bf * 0.92, ratios, decays, amps, rng) * 0.90
        shards = np.zeros(n, dtype=np.float64)
        for s_idx, pos in enumerate(_bounce_train(n, rng, 10, start_frac=0.07, end_frac=0.88, accelerate=True)):
            slen = n - pos
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            f_sh = bf * rng.uniform(1.05, 2.35)
            d_sh = (0.022, 0.014, 0.009, 0.006)
            shards[pos:] += _modal_bank(st, f_sh, ratios, d_sh, amps, rng) * (0.70 * (0.86 ** s_idx))
        raw = snap + main_plate + shards
    elif action == "step":
        # 2 crisp hollow wood/shell CLACK-clack footfalls (dry, zero mud)
        raw = np.zeros(n, dtype=np.float64)
        taps = (0, min(n - 64, int(SAMPLE_RATE * rng.uniform(0.032, 0.056))))
        for idx_t, pos in enumerate(taps):
            tlen = n - pos
            tt = np.linspace(0.0, tlen / SAMPLE_RATE, tlen, endpoint=False)
            gain = 1.0 if idx_t == 0 else rng.uniform(0.62, 0.88)
            raw[pos:] += _modal_bank(tt, bf * rng.uniform(0.95, 1.25), ratios, (0.024, 0.015, 0.010, 0.006), amps, rng) * gain
            raw[pos:] += _sos_bandpass(rng.normal(0.0, 1.0, tlen), 2200.0, 9500.0) * exp_decay(tt, 0.009) * 0.45 * gain
    elif action == "place":
        # Double hollow seating knock *tok-klak*
        k1 = _modal_bank(t, bf * 0.85, ratios, (0.040, 0.026, 0.016, 0.010), amps, rng)
        p2 = min(n - 64, int(SAMPLE_RATE * rng.uniform(0.045, 0.075)))
        t2 = np.linspace(0.0, (n - p2) / SAMPLE_RATE, n - p2, endpoint=False)
        k2 = np.zeros(n, dtype=np.float64)
        k2[p2:] = _modal_bank(t2, bf * 1.18, ratios, (0.032, 0.020, 0.012, 0.007), amps, rng) * 0.80
        raw = k1 + k2 + _sos_bandpass(rng.normal(0.0, 1.0, n), 1800.0, 8200.0) * exp_decay(t, 0.012) * 0.38
    elif action == "hit":
        # Ultra-tight dry hollow shell *tock!*
        body = _modal_bank(t, bf * 1.15, ratios, (0.028, 0.018, 0.012, 0.007), amps, rng) * 1.15
        click = _sos_bandpass(rng.normal(0.0, 1.0, n), 2400.0, 10500.0) * exp_decay(t, 0.009) * 0.55
        raw = body + click
    else:  # fall
        raw = _chitin_modal_hits(n, rng, bf, 5, 0.038, spread_frac=0.45)
        raw += _modal_bank(t, bf * 0.80, ratios, (0.060, 0.036, 0.022, 0.012), amps, rng) * 0.90

    # Strictly dry hollow chitin (remove sub-210 Hz boom, minimal bright room air)
    return _cavern_reverb(_sos_highpass(raw, 210.0), rng, wet=0.09, rt60=0.22, lp_hz=6800.0)


def _surf_mycelial_stone(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Dense mineral subterranean basalt: ZERO tonal chirps; broadband rock formants + gravel rattle."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)

    if action == "break":
        # Sharp broadband granite split + heavy low basalt mass + 12 cascading gravel pebbles
        split_src = (white * 0.65 + pink * 0.55) * exp_decay(t, 0.048)
        rock_body = _apply_formant_bank(split_src, [(340.0 * rng.uniform(0.9, 1.1), 2.0, 1.0), (1450.0 * rng.uniform(0.9, 1.1), 2.4, 0.85)])
        mass = _sos_bandpass(brown, 45.0, 185.0) * exp_decay(t, dur * 0.34) * 1.25
        pebbles = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 12, start_frac=0.08, end_frac=0.88, accelerate=True):
            plen = min(n - pos, int(SAMPLE_RATE * 0.026))
            pt = np.linspace(0.0, plen / SAMPLE_RATE, plen, endpoint=False)
            lo = rng.uniform(1600.0, 4200.0)
            pebbles[pos : pos + plen] += _sos_bandpass(rng.normal(0.0, 1.0, plen), lo, lo * 2.1) * exp_decay(pt, 0.008) * rng.uniform(0.45, 0.95)
        raw = np.tanh((rock_body + mass + pebbles) * 1.25)
    elif action == "step":
        # Crisp dry cavern boot scuff + tiny loose grit crunch (high grit, zero sub-boom)
        raw = np.zeros(n, dtype=np.float64)
        steps = (0, min(n - 48, int(SAMPLE_RATE * rng.uniform(0.036, 0.062))))
        for idx_s, pos in enumerate(steps):
            slen = n - pos
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            g = 1.0 if idx_s == 0 else 0.72
            grit = _sos_bandpass(white[pos:], 1200.0, 6800.0) * exp_decay(st, 0.018) * 0.85
            heel = _apply_formant_bank(pink[pos:] * exp_decay(st, 0.026), [(420.0, 1.8, 0.8), (1850.0, 2.2, 0.55)])
            raw[pos:] += (grit + heel) * g
    elif action == "place":
        slab = _sos_bandpass(brown * 0.8 + pink * 0.5, 50.0, 290.0) * exp_decay(t, dur * 0.25) * 1.35
        scrape = _sos_bandpass(white, 950.0, 4200.0) * exp_decay(t, 0.042) * 0.72
        raw = slab + scrape
    elif action == "hit":
        # Flinty chisel/pickaxe clink-thwack (pure broadband mineral spark + dense rock thud, no sine tone)
        spark = _sos_bandpass(white, 2600.0, 10800.0) * exp_decay(t, 0.016) * 1.05
        stone_knock = _apply_formant_bank(pink * exp_decay(t, 0.042), [(360.0, 1.9, 1.1), (1580.0, 2.3, 0.75)])
        raw = spark + stone_knock
    else:  # fall
        impact = _sos_bandpass(brown, 42.0, 220.0) * exp_decay(t, dur * 0.36) * 1.35
        rock = _apply_formant_bank(pink * exp_decay(t, dur * 0.22), [(320.0, 1.8, 1.0), (1400.0, 2.2, 0.7)])
        slide = _sos_bandpass(white, 1500.0, 6200.0) * adsr_envelope(n, 0.04, 0.28, 0.35, 0.38) * 0.50
        raw = np.tanh((impact + rock + slide) * 1.25)

    return _cavern_reverb(raw, rng, wet=0.22, rt60=0.48, lp_hz=2800.0)


def _surf_synaptic_node(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Hybrid wet bio-organ + eerie quartz crystal-harp modes [1.0, 2.32, 3.85, 5.91] + synapse arc zaps."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    q_ratios = (1.0, 2.32, 3.85, 5.91)
    q_amps = (0.55, 0.42, 0.28, 0.16)

    if action == "break":
        # Shattering crystal cluster + downward synaptic drain whine + wet organ tear
        f_down = np.maximum(75.0, bf * 1.15 * np.exp(-1.5 * t / dur))
        drain = _fm_tone(t, f_down, f_down * 0.5, 2.1, rng.uniform(0, 6.28)) * exp_decay(t, dur * 0.45) * 0.65
        shards = np.zeros(n, dtype=np.float64)
        for s_idx, pos in enumerate(_bounce_train(n, rng, 8, start_frac=0.0, end_frac=0.82, accelerate=True)):
            slen = n - pos
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            shards[pos:] += _modal_bank(st, bf * rng.uniform(1.2, 2.6), q_ratios, (0.09, 0.06, 0.04, 0.02), q_amps, rng) * (0.75 * (0.88 ** s_idx))
        wet_tear = _wet_squelch_layer(n, rng, bf * 0.65, 4, intensity=0.65) * exp_decay(t, dur * 0.35)
        zaps = _micro_arc_zaps(n, rng, 5)
        raw = drain + shards + wet_tear + zaps * 0.65
    elif action == "step":
        # Delicate crystal-glass tink + soft pulsing neural hum underfoot
        tink = _modal_bank(t, bf * 1.45, q_ratios, (0.06, 0.04, 0.025, 0.015), q_amps, rng) * 0.85
        sub_hum = np.sin(2.0 * np.pi * (bf * 0.28) * t + rng.uniform(0, 6.28)) * adsr_envelope(n, 0.08, 0.25, 0.45, 0.35) * 0.35
        zap = _micro_arc_zaps(n, rng, 2, spread=(0.1, 0.5)) * 0.45
        raw = tink + sub_hum + zap
    elif action == "place":
        # Wet organ seat + 2-note eerie tritone crystal link-up (bf -> bf*1.414)
        wet = _wet_squelch_layer(n, rng, bf * 0.55, 2, intensity=0.55) * exp_decay(t, 0.09)
        c1 = _modal_bank(t, bf, q_ratios, (0.18, 0.12, 0.07, 0.04), q_amps, rng) * 0.8
        p2 = min(n - 64, int(n * 0.28))
        c2 = np.zeros(n, dtype=np.float64)
        t2 = np.linspace(0.0, (n - p2) / SAMPLE_RATE, n - p2, endpoint=False)
        c2[p2:] = _modal_bank(t2, bf * 1.414, q_ratios, (0.18, 0.11, 0.06, 0.03), q_amps, rng) * 0.85
        raw = wet + c1 + c2 + _micro_arc_zaps(n, rng, 3) * 0.5
    elif action == "hit":
        # Sharp resonant quartz ping + quick static zap
        ping = _modal_bank(t, bf * 1.35, q_ratios, (0.095, 0.065, 0.040, 0.022), q_amps, rng) * 1.1
        zaps = _micro_arc_zaps(n, rng, 3, spread=(0.0, 0.4)) * 0.6
        raw = ping + zaps
    else:  # fall
        cluster = _modal_bank(t, bf * 0.90, q_ratios, (0.18, 0.11, 0.07, 0.04), q_amps, rng) * 0.95
        thump = _sos_bandpass(_brown_noise(rng, n), 55.0, 260.0) * exp_decay(t, dur * 0.25) * 0.55
        zaps = _micro_arc_zaps(n, rng, 6, spread=(0.05, 0.78)) * 0.65
        raw = cluster + thump + zaps

    return _cavern_reverb(raw, rng, wet=0.22, rt60=0.50, lp_hz=5200.0)


def _surf_purification_tech(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Sterile steel/titanium alloy plate modes [1.0, 1.59, 2.14, 2.92, 3.60, 4.95] + precision bolt latches."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    ratios = (1.0, 1.59, 2.14, 2.92, 3.60, 4.95)
    amps = (0.50, 0.42, 0.34, 0.24, 0.16, 0.10)

    if action == "break":
        # Armored alloy clang + glass pane shatter + clean capacitor discharge drop
        clang = _modal_bank(t, bf * 0.82, ratios, (0.18, 0.14, 0.10, 0.07, 0.05, 0.03), amps, rng)
        drop = np.sin(2.0 * np.pi * np.cumsum(np.linspace(1150.0, 160.0, n)) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(t, dur * 0.35) * 0.52
        glass = _micro_arc_zaps(n, rng, 9, lo_hz=4200.0, hi_hz=14000.0, spread=(0.02, 0.78)) * 0.80
        raw = clang + drop + glass
    elif action == "step":
        # Hollow cold steel catwalk deckplate clank-tap (clean, zero organic mud)
        raw = np.zeros(n, dtype=np.float64)
        for idx_s, pos in enumerate((0, min(n - 64, int(SAMPLE_RATE * rng.uniform(0.034, 0.058))))):
            slen = n - pos
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            g = 1.0 if idx_s == 0 else 0.72
            plate = _modal_bank(st, bf * 0.75 * rng.uniform(0.94, 1.08), ratios, (0.045, 0.032, 0.022, 0.014, 0.009, 0.006), amps, rng)
            tap = _sos_bandpass(rng.normal(0.0, 1.0, slen), 1800.0, 7500.0) * exp_decay(st, 0.010) * 0.38
            raw[pos:] += (plate + tap) * g
    elif action == "place":
        # Solid steel seat + dual precision mechanical bolt-latch CLACK-CHINK + crisp lock ping
        seat = _modal_bank(t, bf * 0.70, ratios, (0.08, 0.055, 0.035, 0.02, 0.012, 0.008), amps, rng) * 0.90
        latches = np.zeros(n, dtype=np.float64)
        for ms, f_l in ((rng.uniform(34.0, 48.0), bf * 1.4), (rng.uniform(76.0, 96.0), bf * 1.85)):
            pos = min(n - 48, int(ms * SAMPLE_RATE / 1000.0))
            lt = np.linspace(0.0, (n - pos) / SAMPLE_RATE, n - pos, endpoint=False)
            latches[pos:] += _modal_bank(lt, f_l, (1.0, 2.1, 3.4), (0.028, 0.016, 0.010), (0.65, 0.45, 0.25), rng)
        raw = seat + latches
    elif action == "hit":
        # Unmistakable wrench-on-steel metallic *TING-CLANK!* (tight tonal ringing eigenmodes)
        strike = _sos_bandpass(rng.normal(0.0, 1.0, n), 2400.0, 11000.0) * exp_decay(t, 0.008) * 0.55
        ting = _modal_bank(t, bf * 1.15, ratios, (0.11, 0.085, 0.060, 0.042, 0.028, 0.018), amps, rng) * 1.15
        raw = strike + ting
    else:  # fall
        # Hollow steel bulkhead boom + sustained metallic ring
        bulkhead = _modal_bank(t, bf * 0.62, ratios, (0.20, 0.15, 0.10, 0.07, 0.045, 0.030), amps, rng) * 1.10
        impact = _sos_bandpass(_pink_noise(rng, n), 140.0, 1400.0) * exp_decay(t, 0.045) * 0.65
        raw = bulkhead + impact

    # Tight clinical laboratory space (nearly zero muddy cavern tail)
    return _cavern_reverb(_sos_highpass(raw, 140.0), rng, wet=0.06, rt60=0.18, lp_hz=8000.0)


def _surf_hive_altar(action: str, n: int, dur: float, bf: float, rng: np.random.Generator) -> np.ndarray:
    """Ancient obsidian-chitin monolith: funeral minor-third bell modes [0.5, 1.0, 1.189, 1.498, 2.0, 2.996, 4.18] + cavern."""
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    bell_ratios = (0.5, 1.0, 1.189, 1.498, 2.0, 2.996, 4.18)
    bell_amps = (0.52, 0.65, 0.48, 0.42, 0.32, 0.22, 0.14)
    brown = _brown_noise(rng, n)

    if action == "break":
        # Monolith shatter + descending minor-third funeral bell + subterranean avalanche
        decays = (0.34, 0.28, 0.22, 0.17, 0.12, 0.08, 0.05)
        bell = _modal_bank(t, bf, bell_ratios, decays, bell_amps, rng, glide_factor=-0.22, glide_tau=dur * 0.45) * 1.15
        seismic = _sos_bandpass(brown, 42.0, 220.0) * exp_decay(t, dur * 0.42) * 0.62
        crack = _apply_formant_bank(_pink_noise(rng, n) * exp_decay(t, 0.075), [(310.0, 2.0, 0.9), (1250.0, 2.4, 0.70)])
        raw = np.tanh((bell + seismic + crack) * 1.2)
    elif action == "step":
        # Solemn stone-cathedral step + ghostly obsidian bell resonance underfoot
        thud = _sos_bandpass(brown, 48.0, 260.0) * exp_decay(t, 0.065) * 0.60
        ghost_bell = _modal_bank(t, bf * 0.95, bell_ratios, (0.14, 0.12, 0.09, 0.07, 0.05, 0.03, 0.02), bell_amps, rng) * 0.95
        raw = thud + ghost_bell
    elif action == "place":
        # Monumental altar slam + ominous temple gong
        slam = _sos_bandpass(brown, 44.0, 240.0) * exp_decay(t, 0.10) * 0.65
        gong = _modal_bank(t, bf, bell_ratios, (0.26, 0.22, 0.17, 0.12, 0.08, 0.05, 0.03), bell_amps, rng) * 1.15
        raw = np.tanh((slam + gong) * 1.2)
    elif action == "hit":
        # Heavy obsidian-gong knock
        stone = _sos_bandpass(brown, 52.0, 360.0) * exp_decay(t, 0.048) * 0.58
        toll = _modal_bank(t, bf * 1.05, bell_ratios, (0.16, 0.14, 0.10, 0.075, 0.05, 0.035, 0.02), bell_amps, rng) * 1.15
        raw = stone + toll
    else:  # fall
        quake = _sos_bandpass(brown, 40.0, 230.0) * exp_decay(t, dur * 0.42) * 0.72
        toll = _modal_bank(t, bf * 0.92, bell_ratios, (0.32, 0.26, 0.20, 0.14, 0.09, 0.06, 0.03), bell_amps, rng) * 1.10
        raw = np.tanh((quake + toll) * 1.2)

    return _cavern_reverb(raw, rng, wet=0.34, rt60=0.82, lp_hz=3200.0)


_SURFACE_ENGINES = {
    "mycelium": _surf_mycelium,
    "antibody_cyst": _surf_antibody_cyst,
    "chitin": _surf_chitin,
    "mycelial_stone": _surf_mycelial_stone,
    "synaptic_node": _surf_synaptic_node,
    "purification_tech": _surf_purification_tech,
    "hive_altar": _surf_hive_altar,
}


def synthesize_block_surface(
    material: str,
    action: str,
    *,
    duration: float,
    base_freq: float,
    rng: np.random.Generator,
) -> np.ndarray:
    """Dispatch physical surface synthesis to its dedicated material engine."""
    n = max(int(SAMPLE_RATE * 0.11), int(round(SAMPLE_RATE * duration)))
    engine = _SURFACE_ENGINES.get(material)
    if engine is None:
        raise ValueError(f"Unknown block surface material: {material}")
    return engine(action, n, duration, base_freq, rng)


# =============================================================================
# 14 Bespoke Multi-Stage Functional Block Synthesizers
# =============================================================================

def synthesize_block_functional(
    sound_id: str,
    *,
    duration: float,
    base_freq: float,
    mod_freq: float,
    rng: np.random.Generator,
    formant_shift: float = 1.0,
) -> np.ndarray:
    """Synthesize a unique, unmistakable multi-stage narrative waveform for a functional block."""
    n = max(int(SAMPLE_RATE * 0.12), int(round(SAMPLE_RATE * duration)))
    t = np.linspace(0.0, duration, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)
    pjit = float(rng.uniform(0.0, 2.0 * np.pi))

    if sound_id == "spore_vent_puff":
        # Organic valve pop (22..55 ms offset jitter) -> independent dual resonant breath bursts -> spore drift
        valve_s = min(n // 8, int(SAMPLE_RATE * rng.uniform(0.0, 0.028)))
        valve_n = min(n - valve_s, int(SAMPLE_RATE * rng.uniform(0.025, 0.045)))
        valve = np.zeros(n, dtype=np.float64)
        if valve_n > 24:
            vt = np.linspace(0.0, valve_n / SAMPLE_RATE, valve_n, endpoint=False)
            valve[valve_s : valve_s + valve_n] = np.sin(2.0 * np.pi * np.linspace(rng.uniform(110.0, 170.0), rng.uniform(260.0, 390.0), valve_n) * vt + pjit) * np.sin(np.linspace(0, np.pi, valve_n)) * 0.85
        f_mid = rng.uniform(1350.0, 2400.0)
        breath_src = _sos_bandpass(pink * 0.7 + white * 0.45, 320.0, 4800.0)
        exhale = _swept_resonant_filter(breath_src, rng.uniform(420.0, 680.0) * formant_shift, f_mid, q=3.0)
        exhale = _swept_resonant_filter(exhale, f_mid, rng.uniform(300.0, 520.0), q=2.5) * adsr_envelope(n, rng.uniform(0.06, 0.14), 0.28, 0.36, 0.40) * 1.25
        bubbles = _wet_squelch_layer(n, rng, 280.0, 4, intensity=0.45) * exp_decay(t, duration * 0.40)
        return _cavern_reverb(valve + exhale + bubbles, rng, wet=0.14, rt60=0.35, lp_hz=3800.0)

    if sound_id == "purification_pylon_pulse":
        # Pure warm harmonic electromagnetic stabilizer pulse + clean sterile air sweep (NO arpeggio!)
        env = np.sin(np.pi * t / duration) ** 1.2
        f1 = base_freq * 0.32  # ~166 Hz warm harmonic anchor
        stabilizer = (
            np.sin(2.0 * np.pi * f1 * t + pjit) * 0.65
            + np.sin(2.0 * np.pi * (f1 * 2.0) * t + pjit) * 0.38
            + np.sin(2.0 * np.pi * (f1 * 3.0) * t) * 0.22
            + np.sin(2.0 * np.pi * (f1 * 4.0) * t) * 0.10
        ) * env
        air_sweep = _swept_resonant_filter(pink, 1800.0, 5200.0, q=3.0) * env * 0.42
        return _cavern_reverb(stabilizer + air_sweep, rng, wet=0.08, rt60=0.22, lp_hz=6800.0)

    if sound_id == "purification_turret_fire":
        # Crisp pneumatic bolt CLACK (4ms) + punchy plasma-spore THWUMP-ZIP (980->130 Hz) + air jet
        bolt = _modal_bank(t, 1250.0, (1.0, 2.2, 3.6), (0.012, 0.008, 0.005), (0.7, 0.5, 0.3), rng) * 0.85
        f_zip = np.maximum(110.0, base_freq * (0.18 + 1.18 * exp_decay(t, 0.032)))
        thwump = np.sin(2.0 * np.pi * np.cumsum(f_zip) / SAMPLE_RATE + pjit) * exp_decay(t, 0.075) * 1.15
        jet = _sos_bandpass(white, 1900.0, 8800.0) * exp_decay(t, 0.045) * 0.65
        return _cavern_reverb(np.tanh((bolt + thwump + jet) * 1.25), rng, wet=0.08, rt60=0.20, lp_hz=7200.0)

    if sound_id == "purification_turret_upgrade":
        # Stage 1: rapid servo ratchet clicks (0..0.24s) -> Stage 2: steel bolt CLACK (0.26s) -> Stage 3: 2-tone confirm
        wave = np.zeros(n, dtype=np.float64)
        for c_idx in range(12):
            u = c_idx / 11.0
            pos = int((0.02 + 0.24 * (u ** 1.35)) * n)
            clen = min(n - pos, int(SAMPLE_RATE * 0.018))
            ct = np.linspace(0.0, clen / SAMPLE_RATE, clen, endpoint=False)
            fc = 720.0 + u * 950.0
            wave[pos : pos + clen] += np.sin(2.0 * np.pi * fc * ct) * exp_decay(ct, 0.006) * 0.55
        lock_pos = int(0.36 * n)
        if n - lock_pos > 64:
            lt = np.linspace(0.0, (n - lock_pos) / SAMPLE_RATE, n - lock_pos, endpoint=False)
            wave[lock_pos:] += _modal_bank(lt, 480.0, (1.0, 1.59, 2.14, 3.60), (0.045, 0.03, 0.02, 0.012), (0.65, 0.45, 0.3, 0.2), rng)
        for idx_n, ratio in enumerate((1.15, 1.725)):
            start = int((0.46 + idx_n * 0.16) * n)
            if n - start > 64:
                nt = np.linspace(0.0, (n - start) / SAMPLE_RATE, n - start, endpoint=False)
                f_n = base_freq * ratio
                wave[start:] += (np.sin(2.0 * np.pi * f_n * nt) * 0.7 + np.sin(2.0 * np.pi * (f_n * 2.0) * nt) * 0.25) * exp_decay(nt, 0.14)
        return _cavern_reverb(wave, rng, wet=0.08, rt60=0.22, lp_hz=7200.0)

    if sound_id == "purification_turret_repair":
        # Heavy wrench steel taps (0s, 0.09s) -> mid pneumatic steam flush (0.14..0.55s) -> servo reboot hum
        wave = _modal_bank(t, 640.0, (1.0, 1.59, 2.92), (0.06, 0.04, 0.025), (0.75, 0.55, 0.32), rng) * 1.15
        p2 = int(0.10 * n)
        t2 = np.linspace(0.0, (n - p2) / SAMPLE_RATE, n - p2, endpoint=False)
        wave[p2:] += _modal_bank(t2, 820.0, (1.0, 1.59, 2.92), (0.05, 0.035, 0.02), (0.65, 0.45, 0.25), rng) * 0.90
        flush_s = int(0.18 * n)
        flush_e = int(0.64 * n)
        if flush_e > flush_s + 32:
            wave[flush_s:flush_e] += _sos_bandpass(pink[flush_s:flush_e], 950.0, 4200.0) * np.sin(np.linspace(0, np.pi, flush_e - flush_s)) * 0.75
        reb_s = int(0.45 * n)
        if n - reb_s > 48:
            rt = np.linspace(0.0, (n - reb_s) / SAMPLE_RATE, n - reb_s, endpoint=False)
            f_up = np.linspace(190.0, 560.0, len(rt))
            wave[reb_s:] += np.sin(2.0 * np.pi * np.cumsum(f_up) / SAMPLE_RATE + pjit) * adsr_envelope(len(rt), 0.12, 0.2, 0.5, 0.35) * 0.90
        return _cavern_reverb(wave, rng, wet=0.08, rt60=0.20, lp_hz=6200.0)

    if sound_id == "synaptic_node_mature":
        # 3 accelerating vascular heartbeats -> wet fibrous stretch crackle -> dark alien vocal choir bloom
        wave = np.zeros(n, dtype=np.float64)
        for b_pos, f_b in ((int(0.02 * n), 68.0), (int(0.24 * n), 76.0), (int(0.40 * n), 88.0)):
            blen = min(n - b_pos, int(SAMPLE_RATE * 0.14))
            if blen > 24:
                bt = np.linspace(0.0, blen / SAMPLE_RATE, blen, endpoint=False)
                wave[b_pos : b_pos + blen] += np.sin(2.0 * np.pi * np.cumsum(f_b * (0.6 + 1.1 * exp_decay(bt, 0.03))) / SAMPLE_RATE) * exp_decay(bt, 0.055)
        bloom_s = int(0.32 * n)
        blen = n - bloom_s
        if blen > 64:
            bt = np.linspace(0.0, blen / SAMPLE_RATE, blen, endpoint=False)
            glottal = _glottal_sub_source(bt, np.linspace(base_freq * 0.55, base_freq * 0.85, blen), rng, harmonics=10, sub_weight=0.75)
            choir = _apply_formant_bank(glottal, [(290.0 * formant_shift, 3.0, 1.0), (760.0 * formant_shift, 3.4, 0.85), (1820.0, 4.0, 0.55)])
            crackle = _wet_squelch_layer(blen, rng, 280.0, 5, intensity=0.65)
            wave[bloom_s:] += (choir + crackle * 0.5) * adsr_envelope(blen, 0.15, 0.2, 0.6, 0.35)
        return _cavern_reverb(np.tanh(wave * 1.25), rng, wet=0.24, rt60=0.55, lp_hz=3400.0)

    if sound_id == "symbiotic_node_charge":
        # Warm organic absorption + harmonious allied emerald open-fifth crystal chord (330/495/660/990)
        swell = adsr_envelope(n, 0.14, 0.20, 0.60, 0.36)
        root = base_freq * 0.61 * rng.uniform(0.96, 1.04)  # ~330 Hz
        chord = (
            np.sin(2.0 * np.pi * root * t + pjit) * 0.58
            + np.sin(2.0 * np.pi * (root * 1.5) * t + rng.uniform(0, 6.28)) * 0.45
            + np.sin(2.0 * np.pi * (root * 2.0) * t + rng.uniform(0, 6.28)) * 0.30
            + np.sin(2.0 * np.pi * (root * 3.0) * t) * 0.18
        ) * swell
        absorb = _swept_resonant_filter(pink, 240.0, 1400.0, q=2.4) * exp_decay(t, duration * 0.38) * 0.45
        return _cavern_reverb(chord + absorb, rng, wet=0.18, rt60=0.45, lp_hz=5200.0)

    if sound_id == "symbiotic_node_teleport":
        # Vacuum suction rise (0..0.38) -> spatial implosion bass thump (0.39) -> root-crystal emergence echo
        imp_idx = int(0.36 * n)
        wave = np.zeros(n, dtype=np.float64)
        if imp_idx > 48:
            st1 = t[:imp_idx]
            frac = st1 / max(1e-4, st1[-1])
            suction = _swept_resonant_filter(pink[:imp_idx], 200.0, 4200.0, q=2.6) * (frac ** 1.3) * 1.1
            f_up = 160.0 * np.exp(2.1 * frac)
            whine = np.sin(2.0 * np.pi * np.cumsum(f_up) / SAMPLE_RATE + pjit) * (frac ** 1.5) * 0.65
            wave[:imp_idx] = suction + whine
        rem = n - imp_idx
        if rem > 48:
            rt = np.linspace(0.0, rem / SAMPLE_RATE, rem, endpoint=False)
            implosion = np.sin(2.0 * np.pi * np.cumsum(np.maximum(34.0, 140.0 * exp_decay(rt, 0.045))) / SAMPLE_RATE) * exp_decay(rt, 0.16) * 1.25
            echo = _modal_bank(rt, base_freq, (1.0, 1.5, 2.32, 3.85), (0.24, 0.18, 0.12, 0.07), (0.55, 0.42, 0.25, 0.14), rng) * 0.80
            wave[imp_idx:] += implosion + echo
        return _cavern_reverb(np.tanh(wave * 1.2), rng, wet=0.26, rt60=0.58, lp_hz=4400.0)

    if sound_id == "spore_bulb_burst":
        # Tight 38 ms pressurized pod creak -> hollow-gourd PLOP-POP! -> rapid spore cloud scatter
        creak_n = min(n // 6, int(SAMPLE_RATE * 0.042))
        wave = np.zeros(n, dtype=np.float64)
        if creak_n > 24:
            ct = np.linspace(0.0, creak_n / SAMPLE_RATE, creak_n, endpoint=False)
            wave[:creak_n] = np.sin(2.0 * np.pi * np.linspace(240.0, 480.0, creak_n) * ct) * (ct / max(1e-4, ct[-1])) * 0.55
        pop_s = max(16, creak_n - 8)
        rt = np.linspace(0.0, (n - pop_s) / SAMPLE_RATE, n - pop_s, endpoint=False)
        # Resonant gourd pitch jump up then fast drop: unmistakable PLOP-POP!
        f_gourd = 160.0 + 420.0 * np.exp(-((rt - 0.014) / 0.012) ** 2) - 70.0 * (rt / max(1e-4, rt[-1]))
        gourd = np.sin(2.0 * np.pi * np.cumsum(np.maximum(55.0, f_gourd)) / SAMPLE_RATE + pjit) * exp_decay(rt, 0.095) * 1.2
        spray = _sos_bandpass(pink[pop_s:] * 0.7 + white[pop_s:] * 0.5, 900.0, 6200.0) * exp_decay(rt, duration * 0.34) * 0.85
        wet_d = _wet_squelch_layer(len(rt), rng, 280.0, 4, intensity=0.70) * exp_decay(rt, duration * 0.30)
        wave[pop_s:] += np.tanh((gourd + spray + wet_d) * 1.25)
        return _cavern_reverb(wave, rng, wet=0.16, rt60=0.38, lp_hz=3800.0)

    if sound_id == "ephemeral_chitin_crumble":
        # Cascading dry sand-like shower of 22 desiccated husk flakes thinning into dust (zero heavy thud!)
        wave = np.zeros(n, dtype=np.float64)
        for f_idx, pos in enumerate(_bounce_train(n, rng, 22, start_frac=0.0, end_frac=0.90, accelerate=False)):
            flen = min(n - pos, int(SAMPLE_RATE * rng.uniform(0.015, 0.036)))
            if flen <= 12:
                continue
            fade_tail = 1.0 - 0.68 * (f_idx / 21.0)
            ft = np.linspace(0.0, flen / SAMPLE_RATE, flen, endpoint=False)
            ff = rng.uniform(1100.0, 3400.0)
            flake = _modal_bank(ft, ff, (1.0, 2.756, 5.4), (0.016, 0.010, 0.006), (0.55, 0.42, 0.25), rng)
            dust = _sos_bandpass(rng.normal(0.0, 1.0, flen), 2800.0, 11000.0) * exp_decay(ft, 0.014) * 0.48
            wave[pos : pos + flen] += (flake + dust) * fade_tail
        return _cavern_reverb(_sos_highpass(wave, 260.0), rng, wet=0.12, rt60=0.28, lp_hz=7200.0)

    if sound_id == "mycelial_rift_hum":
        # Dimensional abyss drone: slow beating integer-locked sub + howling narrow-Q void wind (seamless fold)
        cf = int(SAMPLE_RATE * 0.24)
        ext_n = n + cf
        ext_t = np.arange(ext_n, dtype=np.float64) / SAMPLE_RATE
        ext_pink = _pink_noise(rng, ext_n)
        ext_brown = _brown_noise(rng, ext_n)
        f_sub = max(44.0, round(52.0 * duration) / max(0.2, duration))
        beat_hz = max(1.0, round(2.2 * duration) / max(0.2, duration))
        mod_amp = 0.80 + 0.20 * np.sin(2.0 * np.pi * beat_hz * ext_t + pjit)
        sub = (
            np.sin(2.0 * np.pi * f_sub * ext_t + pjit) * 0.62
            + np.sin(2.0 * np.pi * (f_sub + beat_hz) * ext_t + rng.uniform(0, 6.28)) * 0.50
            + np.sin(2.0 * np.pi * (f_sub * 2.0) * ext_t) * 0.28
            + np.sin(2.0 * np.pi * (f_sub * 2.828) * ext_t) * 0.20
        ) * mod_amp
        howl1 = _sos_bandpass(ext_pink, 380.0, 740.0) * (0.72 + 0.28 * np.sin(2.0 * np.pi * (beat_hz * 0.5) * ext_t)) * 0.72
        howl2 = _sos_bandpass(ext_brown, 110.0, 260.0) * 0.58
        raw = _cavern_reverb(sub + howl1 + howl2, rng, wet=0.34, rt60=0.72, lp_hz=2800.0)
        return fold_seamless_loop(raw, n, crossfade_ms=240.0)

    if sound_id == "hive_altar_awaken":
        # Monumental 3-stage ritual: stone grind & obsidian bell -> colossal bio-horror horn & growl -> cavern ring
        bell_ratios = (0.5, 1.0, 1.189, 1.498, 2.0)
        grind_len = int(0.42 * n)
        stone = _sos_bandpass(brown[:grind_len] * 0.8 + pink[:grind_len] * 0.6, 65.0, 950.0) * np.sin(np.linspace(0, np.pi, grind_len))
        wave = np.zeros(n, dtype=np.float64)
        wave[:grind_len] += stone * 0.90
        bell_s = int(0.18 * n)
        bt = np.linspace(0.0, (n - bell_s) / SAMPLE_RATE, n - bell_s, endpoint=False)
        wave[bell_s:] += _modal_bank(bt, 98.0, bell_ratios, (0.48, 0.36, 0.28, 0.20, 0.12), (0.7, 0.6, 0.4, 0.3, 0.2), rng) * 0.80
        horn_s = int(0.28 * n)
        ht = t[: n - horn_s]
        f_horn = 68.0 * (0.94 + 0.14 * np.sin(np.pi * ht / max(1e-4, ht[-1])))
        horn_src = _glottal_sub_source(ht, f_horn, rng, harmonics=13, sub_weight=0.95, rattle_hz=20.0)
        horn = _apply_formant_bank(horn_src, [(135.0 * formant_shift, 2.6, 1.15), (380.0 * formant_shift, 3.2, 0.85), (960.0, 3.8, 0.50)])
        wave[horn_s:] += np.tanh(horn * 1.35) * adsr_envelope(len(ht), 0.14, 0.20, 0.60, 0.42)
        return _cavern_reverb(wave, rng, wet=0.35, rt60=0.80, lp_hz=2600.0)

    if sound_id == "matriarch_altar_awaken":
        # Eldritch 3-stage awakening: shattering crystal seals -> rising void scream -> tritone cathedral horn + choir
        wave = np.zeros(n, dtype=np.float64)
        for pos in _bounce_train(n, rng, 9, start_frac=0.0, end_frac=0.34, accelerate=True):
            slen = n - pos
            st = np.linspace(0.0, slen / SAMPLE_RATE, slen, endpoint=False)
            wave[pos:] += _modal_bank(st, rng.uniform(520.0, 1250.0), (1.0, 2.32, 3.85), (0.09, 0.06, 0.03), (0.55, 0.4, 0.25), rng) * 0.72
        rise_end = int(0.45 * n)
        rt_r = t[:rise_end]
        tear = _swept_resonant_filter(pink[:rise_end], 220.0, 4800.0, q=3.0) * (rt_r / max(1e-4, rt_r[-1]))
        wave[:rise_end] += tear * 0.85
        choir_s = int(0.30 * n)
        ct = t[: n - choir_s]
        f_ch = np.linspace(110.0, 195.0, len(ct)) * (1.0 + 0.03 * np.sin(2.0 * np.pi * 4.5 * ct))
        src = (
            _glottal_sub_source(ct, f_ch, rng, harmonics=12, sub_weight=0.85, rattle_hz=28.0)
            + _fm_tone(ct, f_ch * 1.414, 24.0, 2.4, pjit) * 0.55
        )
        choir = _apply_formant_bank(src, [(310.0 * formant_shift, 3.2, 1.0), (880.0 * formant_shift, 3.6, 1.05), (2450.0, 4.2, 0.70)])
        sub_drop = np.sin(2.0 * np.pi * np.cumsum(np.maximum(32.0, 120.0 * exp_decay(ct, 0.22))) / SAMPLE_RATE) * exp_decay(ct, 0.45) * 0.85
        wave[choir_s:] += (np.tanh(choir * 1.3) + sub_drop) * adsr_envelope(len(ct), 0.12, 0.20, 0.58, 0.44)
        return _cavern_reverb(wave, rng, wet=0.38, rt60=0.85, lp_hz=3600.0)

    if sound_id == "synaptic_bench_synthesize":
        # Lab biotech sequence: pipette glass bubbles (0..0.40) -> soft vacuum seal (0.40..0.64) -> crisp lab glass ping
        wave = np.zeros(n, dtype=np.float64)
        b_end = int(0.44 * n)
        wave[:b_end] += _wet_squelch_layer(b_end, rng, 560.0, 5, intensity=0.95) * adsr_envelope(b_end, 0.05, 0.2, 0.6, 0.3)
        for pos in _bounce_train(n, rng, 4, start_frac=0.04, end_frac=0.38, accelerate=False):
            gt = np.linspace(0.0, (n - pos) / SAMPLE_RATE, n - pos, endpoint=False)
            wave[pos:] += _modal_bank(gt, 960.0 * rng.uniform(0.9, 1.2), (1.0, 2.32), (0.055, 0.030), (0.65, 0.35), rng)
        vac_s, vac_e = int(0.38 * n), int(0.65 * n)
        wave[vac_s:vac_e] += _sos_bandpass(pink[vac_s:vac_e], 1800.0, 6200.0) * np.sin(np.linspace(0, np.pi, vac_e - vac_s)) * 0.42
        ping_s = int(0.58 * n)
        pt = np.linspace(0.0, (n - ping_s) / SAMPLE_RATE, n - ping_s, endpoint=False)
        wave[ping_s:] += (
            _modal_bank(pt, 920.0, (1.0, 1.5, 2.32), (0.16, 0.12, 0.07), (0.95, 0.62, 0.30), rng)
            + _sos_bandpass(white[ping_s:], 3000.0, 7500.0) * exp_decay(pt, 0.010) * 0.25
        )
        return _cavern_reverb(_sos_highpass(wave, 110.0), rng, wet=0.08, rt60=0.22, lp_hz=6800.0)

    if sound_id == "sovereign_nexus_pulse":
        # Royal sovereign beacon chime: staggered crown harmonic bell + mycelial sub-thrum + swept iris shimmer
        shift = int(rng.uniform(0.01, 0.10) * n)
        rt = t[shift:] - t[shift]
        root = base_freq * rng.uniform(0.92, 1.08)
        wave = np.zeros(n, dtype=np.float64)
        bell = _modal_bank(
            rt,
            root,
            (0.5, 1.0, rng.uniform(1.48, 1.52), 2.0, rng.uniform(2.96, 3.04)),
            (0.28, 0.24, 0.18, 0.12, 0.08),
            (0.45, 0.85, 0.62, 0.42, 0.24),
            rng,
        )
        sub = np.sin(2.0 * np.pi * (root * 0.25) * rt + rng.uniform(0, 6.28)) * exp_decay(rt, duration * 0.42) * 0.48
        shimmer = _swept_resonant_filter(pink[shift:], 360.0 if rng.random() > 0.5 else 2400.0, 2400.0 if rng.random() > 0.5 else 360.0, q=2.8) * exp_decay(rt, duration * 0.36) * 0.42
        wave[shift:] = bell + sub + shimmer
        return _cavern_reverb(np.tanh(wave * 1.15), rng, wet=0.22, rt60=0.52, lp_hz=5600.0)

    if sound_id in {"colony_dialect_tone_1", "colony_dialect_tone_2", "colony_dialect_tone_3"}:
        # Three distinct organic bio-synaptic syllable pulses (~330 / 495 / 660 Hz)
        onset_s = int(rng.uniform(0.012, 0.045) * n)
        pt = t[onset_s:] - t[onset_s]
        tone_idx = int(sound_id.rsplit("_", 1)[1])
        root = base_freq * rng.uniform(0.96, 1.04)
        wave = np.zeros(n, dtype=np.float64)
        ratios = (0.5, 1.0, 1.5 if tone_idx < 3 else 1.498, 2.0, 3.0)
        decays = (0.20, 0.17, 0.12, 0.08, 0.05)
        amps = (0.35, 0.90, 0.55, 0.36, 0.18)
        bell = _modal_bank(pt, root, ratios, decays, amps, rng, mode_jitter=0.045)
        env = (1.0 - np.exp(-pt / 0.006)) * exp_decay(pt, duration * 0.42)
        chirp = np.sin(2.0 * np.pi * (root * np.linspace(0.94, 1.03, len(pt))) * pt + rng.uniform(0, 6.28)) * env * 0.42
        air = _sos_bandpass(pink[onset_s:], root * 1.4, min(6800.0, root * 4.5)) * exp_decay(pt, 0.055) * 0.22
        wave[onset_s:] = bell + chirp + air
        return _cavern_reverb(np.tanh(wave * 1.10), rng, wet=0.18, rt60=0.38, lp_hz=6200.0)

    if sound_id == "reclaimed_bloom_harvest":
        # Crisp stem snap + bioluminescent glass-petal chime + soft spore release puff
        snap_len = min(n // 4, int(SAMPLE_RATE * 0.055))
        wave = np.zeros(n, dtype=np.float64)
        if snap_len > 24:
            st = np.arange(snap_len, dtype=np.float64) / SAMPLE_RATE
            wave[:snap_len] = _sos_bandpass(rng.normal(0.0, 1.0, snap_len), 1400.0, 6800.0) * exp_decay(st, 0.012) * 0.65
        chime_s = max(12, int(rng.uniform(0.02, 0.07) * n))
        ct = np.arange(n - chime_s, dtype=np.float64) / SAMPLE_RATE
        root = base_freq * rng.uniform(0.95, 1.05)
        petals = _modal_bank(
            ct,
            root,
            (1.0, 1.498, 2.0, 2.996, 4.0),
            (0.24, 0.19, 0.14, 0.09, 0.05),
            (0.85, 0.68, 0.50, 0.30, 0.18),
            rng,
        )
        puff = _swept_resonant_filter(pink[chime_s:], 700.0, 3200.0, q=2.0) * exp_decay(ct, 0.14) * 0.35
        wave[chime_s:] += petals + puff
        return _cavern_reverb(np.tanh(wave * 1.12), rng, wet=0.20, rt60=0.45, lp_hz=6800.0)

    if sound_id == "reclaimed_bloom_pulse":
        # Gentle organic luminescence swell + harmonic dewdrop shimmer
        onset = int(rng.uniform(0.02, 0.08) * n)
        pt = np.arange(n - onset, dtype=np.float64) / SAMPLE_RATE
        root = base_freq * rng.uniform(0.94, 1.06)
        wave = np.zeros(n, dtype=np.float64)
        env = adsr_envelope(len(pt), 0.18, 0.24, 0.48, 0.34)
        chime = modal_resonator_bank(
            len(pt),
            (root, root * 1.5, root * 2.5, root * 3.0),
            (0.26, 0.20, 0.14, 0.09),
            rng,
            amplitudes=(0.80, 0.58, 0.35, 0.22),
        )
        shimmer = np.sin(2.0 * np.pi * root * pt + pjit) * env * 0.42
        wave[onset:] = chime + shimmer
        return _cavern_reverb(wave, rng, wet=0.24, rt60=0.50, lp_hz=6200.0)

    if sound_id == "colony_dialect_mastered":
        # Ascending 3-note symbiotic triad resolving into a sovereign unison shimmer
        wave = np.zeros(n, dtype=np.float64)
        ratios = (1.0, 1.498, 2.0)
        onsets = (0.02, rng.uniform(0.20, 0.26), rng.uniform(0.40, 0.48))
        root = base_freq * rng.uniform(0.96, 1.04)
        for ratio, frac in zip(ratios, onsets):
            pos = int(frac * n)
            rem = n - pos
            nt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
            wave[pos:] += _modal_bank(
                nt,
                root * ratio,
                (0.5, 1.0, 1.498, 2.0),
                (0.28, 0.26, 0.18, 0.11),
                (0.35, 0.85, 0.55, 0.30),
                rng,
            )
        bed = np.sin(2.0 * np.pi * (root * 0.5) * t + pjit) * adsr_envelope(n, 0.12, 0.22, 0.55, 0.36) * 0.42
        return _cavern_reverb(np.tanh((wave + bed) * 1.12), rng, wet=0.26, rt60=0.60, lp_hz=6600.0)

    if sound_id == "colony_dialect_conflict":
        # Brief dissonant tritone + minor-second clash & dampening static pulse
        onset = int(rng.uniform(0.01, 0.05) * n)
        rem = n - onset
        rt = np.arange(rem, dtype=np.float64) / SAMPLE_RATE
        root = base_freq * rng.uniform(0.94, 1.06)
        wave = np.zeros(n, dtype=np.float64)
        clash = modal_resonator_bank(
            rem,
            (root, root * 1.065, root * 1.414, root * 2.11),
            (0.22, 0.20, 0.15, 0.10),
            rng,
            amplitudes=(0.82, 0.78, 0.64, 0.42),
        )
        buzz = _fm_tone(rt, root * (1.0 - 0.18 * (rt / max(1e-4, rt[-1]))), root * 0.707, 2.4, pjit) * exp_decay(rt, 0.22) * 0.48
        wave[onset:] = clash + buzz
        return _cavern_reverb(np.tanh(wave * 1.18), rng, wet=0.20, rt60=0.44, lp_hz=5400.0)

    if sound_id == "colony_reconstruction_pulse":
        # Warm mnemonic echo wave: reverse-like breath into ancestral harmonic chord
        s1, e1 = 0, int(0.32 * n)
        rev_env = np.linspace(0.0, 1.0, max(2, e1 - s1)) ** 1.8
        wave = np.zeros(n, dtype=np.float64)
        wave[s1:e1] = _swept_resonant_filter(pink[s1:e1], 240.0, 1400.0, q=3.2) * rev_env * 0.55
        pulse_s = int(0.24 * n)
        pt = np.arange(n - pulse_s, dtype=np.float64) / SAMPLE_RATE
        root = base_freq * rng.uniform(0.95, 1.05)
        chord = _modal_bank(
            pt,
            root,
            (0.5, 1.0, 1.333, 1.998, 2.667),
            (0.38, 0.34, 0.26, 0.18, 0.12),
            (0.42, 0.88, 0.62, 0.44, 0.26),
            rng,
        )
        wave[pulse_s:] += chord
        return _cavern_reverb(np.tanh(wave * 1.15), rng, wet=0.28, rt60=0.64, lp_hz=6000.0)

    if sound_id == "spore_updraft_vent_pulse":
        # Ascending buoyant air column surge + resonant spore throat chime
        onset = int(rng.uniform(0.01, 0.08) * n)
        rt = np.arange(n - onset, dtype=np.float64) / SAMPLE_RATE
        f_lo = rng.uniform(220.0, 380.0)
        f_hi = rng.uniform(1800.0, 3200.0)
        air_rush = _swept_resonant_filter(pink[onset:], f_lo, f_hi, q=2.6) * adsr_envelope(len(rt), 0.14, 0.24, 0.50, 0.38) * 0.85
        hum = _modal_bank(rt, base_freq * rng.uniform(0.94, 1.06), (0.5, 1.0, 1.50, 2.0), (0.28, 0.24, 0.16, 0.10), (0.40, 0.72, 0.45, 0.24), rng)
        wave = np.zeros(n, dtype=np.float64)
        wave[onset:] = air_rush + hum * 0.65
        return _cavern_reverb(np.tanh(wave * 1.15), rng, wet=0.22, rt60=0.48, lp_hz=6400.0)

    raise ValueError(f"Unknown functional block sound_id: {sound_id}")
