"""Deterministic bio-horror & clinical-tech DSP synthesis + 44.1 kHz mono Ogg Vorbis encoding."""

from __future__ import annotations

import hashlib
import os
import shutil
import struct
import subprocess
import time
from pathlib import Path
from typing import Any

import numpy as np
from scipy import signal

from ._cuda_compat import cuda_dsp as _cuda

_gpu_fftconvolve_1d = _cuda.gpu_fftconvolve_1d
_gpu_harmonic_sum = _cuda.gpu_harmonic_sum
_gpu_spectral_noise = _cuda.gpu_spectral_noise

SAMPLE_RATE = 44100
VORBIS_QUALITY = "6"
TRUE_PEAK_CEILING = 0.94
DEFAULT_PEAK = TRUE_PEAK_CEILING
PHYSICAL_MACHINE_CUES = frozenset({
    "bio_scanner_ping", "bio_scanner_thermal_pulse", "purification_pylon_pulse",
    "purification_turret_fire", "purification_turret_upgrade", "purification_turret_repair",
    "synaptic_bench_synthesize", "station_upkeep_low",
})

# Category-calibrated active loudness targets (dBFS)
LOUDNESS_TARGETS_DBFS: dict[str, float] = {
    "footstep": -21.5,
    "block_light": -18.0,
    "block_heavy": -15.5,
    "ambient": -16.5,
    "ui_item": -15.0,
    "combat": -13.5,
    "death": -13.0,
    "boss_cinematic": -11.5,
}


def find_ffmpeg() -> str:
    """Locate ffmpeg executable for libvorbis encoding."""
    on_path = shutil.which("ffmpeg")
    if on_path:
        return on_path
    candidates = [
        Path(r"C:\Users\Alex\Downloads\yt-dlp_win_x86\ffmpeg\bin\ffmpeg.exe"),
        Path(r"C:\ffmpeg\bin\ffmpeg.exe"),
        Path(r"C:\Program Files\ffmpeg\bin\ffmpeg.exe"),
    ]
    for candidate in candidates:
        if candidate.is_file():
            return str(candidate)
    raise FileNotFoundError("ffmpeg executable not found on PATH or standard Windows directories.")


def deterministic_rng(sound_id: str, seed_offset: int = 0, variation_idx: int = 0) -> np.random.Generator:
    """Create a deterministic NumPy Generator seeded from sound_id, seed_offset, and take index."""
    token = f"{sound_id}:{seed_offset}" if variation_idx == 0 else f"{sound_id}:{seed_offset}:v{variation_idx}"
    digest = hashlib.sha256(token.encode("utf-8")).digest()
    seed = int.from_bytes(digest[:8], "little")
    return np.random.default_rng(seed)


def fold_seamless_loop(wave_ext: np.ndarray, target_len: int, crossfade_ms: float = 120.0) -> np.ndarray:
    """Fold an extended waveform ``len >= target_len + cf`` into an exact-length seamless loop."""
    if len(wave_ext) <= target_len + 16:
        return wave_ext[:target_len]
    cf = min(len(wave_ext) - target_len, int(SAMPLE_RATE * crossfade_ms / 1000.0))
    if cf < 32:
        return wave_ext[:target_len]
    if cf % 2 == 1:
        cf -= 1
    half = cf // 2
    theta = np.linspace(0.0, 0.5 * np.pi, cf, dtype=np.float64)
    head = wave_ext[:cf]
    mid = wave_ext[cf:target_len]
    tail = wave_ext[target_len : target_len + cf]
    bridge = tail * np.cos(theta) + head * np.sin(theta)
    return np.concatenate([bridge[half:], mid, bridge[:half]])


def apply_fade_guards(wave: np.ndarray, fade_in_ms: float = 3.0, fade_out_ms: float = 14.0) -> np.ndarray:
    """Apply smooth raised-cosine fades at buffer boundaries to eliminate clicks/pops."""
    n = len(wave)
    if n == 0:
        return wave
    out = wave.copy()
    fi = min(n // 4, max(2, int(SAMPLE_RATE * fade_in_ms / 1000.0)))
    fo = min(n // 2, max(2, int(SAMPLE_RATE * fade_out_ms / 1000.0)))
    if fi > 0:
        out[:fi] *= 0.5 * (1.0 - np.cos(np.linspace(0.0, np.pi, fi, dtype=np.float64)))
    if fo > 0:
        out[-fo:] *= 0.5 * (1.0 + np.cos(np.linspace(0.0, np.pi, fo, dtype=np.float64)))
    return out


def adsr_envelope(n: int, attack: float = 0.04, decay: float = 0.2, sustain: float = 0.5, release: float = 0.25) -> np.ndarray:
    """Build a normalized ADSR amplitude envelope of length n."""
    if n <= 0:
        return np.zeros(0, dtype=np.float64)
    a_n = max(1, int(n * attack))
    d_n = max(1, int(n * decay))
    r_n = max(1, int(n * release))
    if a_n + d_n + r_n >= n:
        scale = (n - 1) / max(1, (a_n + d_n + r_n))
        a_n = max(1, int(a_n * scale))
        d_n = max(1, int(d_n * scale))
        r_n = max(1, n - a_n - d_n)
    s_n = max(0, n - a_n - d_n - r_n)

    env = np.empty(n, dtype=np.float64)
    idx = 0
    env[idx:idx + a_n] = np.linspace(0.0, 1.0, a_n, endpoint=False)
    idx += a_n
    env[idx:idx + d_n] = np.linspace(1.0, sustain, d_n, endpoint=False)
    idx += d_n
    if s_n > 0:
        env[idx:idx + s_n] = sustain
        idx += s_n
    rem = n - idx
    if rem > 0:
        env[idx:] = np.linspace(sustain if s_n > 0 else 1.0, 0.0, rem)
    return env


def exp_decay(t: np.ndarray, tau: float) -> np.ndarray:
    """Exponential decay envelope."""
    return np.exp(-t / max(1e-4, tau))


def _clamp_norm(hz: float) -> float:
    return float(np.clip(hz / (0.5 * SAMPLE_RATE), 0.0006, 0.96))


def _sos_bandpass(sig: np.ndarray, low_hz: float, high_hz: float, order: int = 2) -> np.ndarray:
    lo = _clamp_norm(low_hz)
    hi = max(lo + 0.002, _clamp_norm(high_hz))
    hi = min(hi, 0.97)
    sos = signal.butter(order, [lo, hi], btype="bandpass", output="sos")
    return signal.sosfilt(sos, sig)


def _sos_lowpass(sig: np.ndarray, cutoff_hz: float, order: int = 2) -> np.ndarray:
    wn = _clamp_norm(cutoff_hz)
    sos = signal.butter(order, wn, btype="lowpass", output="sos")
    return signal.sosfilt(sos, sig)


def _sos_highpass(sig: np.ndarray, cutoff_hz: float, order: int = 2) -> np.ndarray:
    wn = _clamp_norm(cutoff_hz)
    sos = signal.butter(order, wn, btype="highpass", output="sos")
    return signal.sosfilt(sos, sig)


def _biquad_bpf_coefs(center_hz: float, q: float) -> tuple[np.ndarray, np.ndarray]:
    fc = float(np.clip(center_hz, 25.0, SAMPLE_RATE * 0.45))
    q_clamped = float(np.clip(q, 0.5, 24.0))
    w0 = 2.0 * np.pi * fc / SAMPLE_RATE
    alpha = np.sin(w0) / (2.0 * q_clamped)
    cos_w0 = np.cos(w0)
    b0 = alpha
    b1 = 0.0
    b2 = -alpha
    a0 = 1.0 + alpha
    a1 = -2.0 * cos_w0
    a2 = 1.0 - alpha
    return np.array([b0 / a0, b1 / a0, b2 / a0], dtype=np.float64), np.array([1.0, a1 / a0, a2 / a0], dtype=np.float64)


def _apply_formant_bank(sig: np.ndarray, formants: list[tuple[float, float, float]]) -> np.ndarray:
    """Pass excitation through parallel biological vocal-tract resonant bandpass filters."""
    out = np.zeros_like(sig)
    for center_hz, q, gain in formants:
        b, a = _biquad_bpf_coefs(center_hz, q)
        out += gain * signal.lfilter(b, a, sig)
    return out


def _swept_resonant_filter(sig: np.ndarray, f_start: float, f_end: float, q: float = 2.4, n_bands: int = 12) -> np.ndarray:
    """Overlap-add time-varying resonant bandpass filter for smooth organic sweeps."""
    n = len(sig)
    if n < 256:
        b, a = _biquad_bpf_coefs(0.5 * (f_start + f_end), q)
        return signal.lfilter(b, a, sig)
    hop = max(64, n // (n_bands + 1))
    win_len = min(n, hop * 2)
    hann = np.hanning(win_len)
    out = np.zeros(n, dtype=np.float64)
    weight = np.zeros(n, dtype=np.float64)
    pos = 0
    while pos < n:
        end = min(n, pos + win_len)
        seg = sig[pos:end]
        w = hann[: len(seg)]
        frac = (pos + 0.5 * len(seg)) / max(1, n)
        fc = f_start * ((f_end / max(25.0, f_start)) ** frac)
        b, a = _biquad_bpf_coefs(fc, q)
        filtered = signal.lfilter(b, a, seg) * w
        out[pos:end] += filtered
        weight[pos:end] += w
        if end == n:
            break
        pos += hop
    return out / np.maximum(weight, 1e-4)


def _pink_noise(rng: np.random.Generator, n: int) -> np.ndarray:
    """Generate 1/f pink noise via spectral shaping."""
    if n <= 0:
        return np.zeros(0, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    gpu_pink = _gpu_spectral_noise(white, 0.52, 25.0, SAMPLE_RATE)
    if gpu_pink is not None:
        return gpu_pink
    spec = np.fft.rfft(white)
    freqs = np.fft.rfftfreq(n, 1.0 / SAMPLE_RATE)
    scale = 1.0 / np.maximum(freqs, 25.0) ** 0.52
    scale[0] = 0.0
    pink = np.fft.irfft(spec * scale, n=n)
    std = float(np.std(pink))
    return pink / max(std, 1e-6)


def _brown_noise(rng: np.random.Generator, n: int) -> np.ndarray:
    """Generate deep 1/f^2 subterranean rumble noise."""
    if n <= 0:
        return np.zeros(0, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    gpu_brown = _gpu_spectral_noise(white, 1.0, 22.0, SAMPLE_RATE)
    if gpu_brown is not None:
        return gpu_brown
    spec = np.fft.rfft(white)
    freqs = np.fft.rfftfreq(n, 1.0 / SAMPLE_RATE)
    scale = 1.0 / np.maximum(freqs, 22.0) ** 1.0
    scale[0] = 0.0
    brown = np.fft.irfft(spec * scale, n=n)
    std = float(np.std(brown))
    return brown / max(std, 1e-6)


def _fm_tone(
    t: np.ndarray,
    carrier_hz: np.ndarray | float,
    mod_hz: float,
    mod_index: float,
    phase_jit: float = 0.0,
    mod_phase: float = 0.0,
) -> np.ndarray:
    """Synthesize organic FM waveform with subtle 1/f vibrato wander so tones avoid sterile oscillator lock."""
    n = len(t)
    if n >= 32:
        t_norm = t / max(1e-4, float(t[-1])) if len(t) > 1 else t
        wander = 1.0 + 0.008 * np.sin(2.0 * np.pi * (1.7 * t_norm + 0.37) + phase_jit * 0.5)
        mod_drift = 1.0 + 0.015 * np.cos(2.0 * np.pi * (2.3 * t_norm + 0.19) + mod_phase)
    else:
        wander = 1.0
        mod_drift = 1.0
    if np.ndim(carrier_hz) == 0:
        inst_c = np.maximum(16.0, float(carrier_hz) * wander)
        phase = 2.0 * np.pi * np.cumsum(inst_c) / SAMPLE_RATE
    else:
        inst_c = np.maximum(16.0, np.asarray(carrier_hz, dtype=np.float64) * wander)
        phase = 2.0 * np.pi * np.cumsum(inst_c) / SAMPLE_RATE
    mod = (mod_index * mod_drift) * np.sin(2.0 * np.pi * mod_hz * t + mod_phase)
    raw = np.sin(phase + phase_jit + mod)
    if n >= 32:
        cutoff = min(9200.0, max(2400.0, float(np.mean(inst_c)) * 6.5))
        return _sos_lowpass(raw, cutoff)
    return raw


def _glottal_sub_source(
    t: np.ndarray,
    freq_curve: np.ndarray,
    rng: np.random.Generator,
    *,
    harmonics: int = 11,
    sub_weight: float = 0.72,
    rattle_hz: float = 26.0,
) -> np.ndarray:
    """Bio-organic asymmetric glottal pulse source with 1/f pitch jitter, vocal shimmer, diplophonia & breath aspiration."""
    n = len(t)
    # Biological vocal fold pitch jitter (+/-3.0%) + subtle shimmer envelope turbulence
    jitter = 1.0 + 0.030 * _sos_lowpass(rng.normal(0.0, 1.0, n), 42.0)
    shimmer = 1.0 + 0.12 * _sos_lowpass(rng.normal(0.0, 1.0, n), 58.0)
    inst_f = np.maximum(24.0, freq_curve * jitter)
    base_phase = 2.0 * np.pi * np.cumsum(inst_f) / SAMPLE_RATE + rng.uniform(0.0, 2.0 * np.pi)
    ks = range(1, harmonics + 1)
    mults = [float(k) * (1.0 + 0.002 * (k - 1)) for k in ks]
    amps = [(1.0 if k % 2 == 1 else -0.82) / (k ** 1.18) for k in ks]
    offs = [0.35 * (k - 1) for k in ks]
    pulse = _gpu_harmonic_sum(base_phase / (2.0 * np.pi), mults, amps, offs)
    if pulse is None:
        pulse = np.zeros(n, dtype=np.float64)
        for mult_k, amp_k, off_k in zip(mults, amps, offs):
            pulse += amp_k * np.sin(mult_k * base_phase + off_k)
    # Irregular wet vocal-fold amplitude modulation (wander-modulated flutter rather than rigid sine)
    rattle_wander = 2.0 * np.pi * np.cumsum(
        np.maximum(4.0, rattle_hz * (1.0 + 0.18 * _sos_lowpass(rng.normal(0.0, 1.0, n), 14.0)))
    ) / SAMPLE_RATE
    rattle = 1.0 - 0.34 * (0.5 + 0.5 * np.sin(rattle_wander + rng.uniform(0.0, 6.28)))
    sub_oct = np.sin(0.5 * base_phase + rng.uniform(0.0, 6.28)) * (sub_weight * 0.72)
    sub_fifth = np.sin(0.333 * base_phase + rng.uniform(0.0, 6.28)) * (sub_weight * 0.34)
    # Glottal breath aspiration air turbulence tracking vocal envelope
    asp_noise = _sos_bandpass(rng.normal(0.0, 1.0, n), 850.0, 4200.0)
    breath = asp_noise * (0.14 * np.clip(shimmer, 0.5, 1.5))
    return (pulse * rattle * np.clip(shimmer, 0.65, 1.35)) + sub_oct + sub_fifth + breath


def _wet_squelch_layer(
    n: int,
    rng: np.random.Generator,
    base_freq: float,
    count: int,
    *,
    intensity: float = 1.0,
) -> np.ndarray:
    """Staggered organic mucus bubbles, membrane tears, and wet cavity resonance."""
    out = np.zeros(n, dtype=np.float64)
    for _ in range(max(1, count)):
        start = int(rng.uniform(0.0, 0.80) * n)
        dur = float(rng.uniform(0.016, 0.068))
        blen = min(n - start, max(48, int(dur * SAMPLE_RATE)))
        if blen <= 16:
            continue
        st = np.linspace(0.0, blen / SAMPLE_RATE, blen, endpoint=False)
        f0 = float(np.clip(base_freq * rng.uniform(0.55, 1.75), 95.0, 2800.0))
        direction = 1.0 if rng.random() > 0.25 else -0.55
        f_sweep = f0 * np.exp(direction * 1.25 * st / max(1e-4, st[-1]))
        phase = 2.0 * np.pi * np.cumsum(f_sweep) / SAMPLE_RATE + rng.uniform(0.0, 6.28)
        bub = np.sin(phase) + 0.35 * np.sin(2.0 * phase + 0.4)
        env = np.sin(np.linspace(0.0, np.pi, blen)) ** 1.15
        out[start : start + blen] += bub * env * rng.uniform(0.45, 0.95) * intensity
    sticky = _sos_bandpass(_pink_noise(rng, n), max(160.0, base_freq * 0.65), min(4200.0, base_freq * 4.0))
    return out + sticky * (0.28 * intensity)


def modal_resonator_bank(
    n: int,
    modes: tuple[float, ...] | list[float],
    decay_rates: tuple[float, ...] | list[float],
    rng: np.random.Generator,
    sample_rate: int = SAMPLE_RATE,
    *,
    amplitudes: tuple[float, ...] | list[float] | None = None,
    detune_jitter: float = 0.025,
) -> np.ndarray:
    """Synthesize a physical modal resonator bank for chitin, crystal, and armored carapace impacts."""
    if n <= 0 or not modes:
        return np.zeros(max(0, n), dtype=np.float64)
    t = np.arange(n, dtype=np.float64) / float(sample_rate)
    out = np.zeros(n, dtype=np.float64)
    count = min(len(modes), len(decay_rates))
    for idx in range(count):
        freq = float(modes[idx]) * float(rng.uniform(1.0 - detune_jitter, 1.0 + detune_jitter))
        if freq <= 12.0 or freq >= 0.46 * sample_rate:
            continue
        tau = max(0.004, float(decay_rates[idx]))
        amp = float(amplitudes[idx]) if amplitudes and idx < len(amplitudes) else (1.0 / (1.0 + 0.55 * idx))
        phase = float(rng.uniform(0.0, 2.0 * np.pi))
        out += amp * np.sin(2.0 * np.pi * freq * t + phase) * np.exp(-t / tau)
    return out


def _chitin_modal_hits(
    n: int,
    rng: np.random.Generator,
    base_freq: float,
    hit_count: int,
    decay_s: float,
    *,
    spread_frac: float = 0.72,
) -> np.ndarray:
    """Hollow inharmonic exoskeleton clicks/snaps and brittle impact transients."""
    out = np.zeros(n, dtype=np.float64)
    ratios = (1.0, 1.57, 2.29, 3.16, 4.41)
    weights = (0.48, 0.32, 0.24, 0.16, 0.10)
    for i in range(max(1, hit_count)):
        if hit_count == 1:
            pos = 0
        else:
            anchor = (i / hit_count) * spread_frac
            pos = min(n - 32, max(0, int((anchor + rng.uniform(0.0, spread_frac / (hit_count * 1.6))) * n)))
        hlen = min(n - pos, max(48, int(SAMPLE_RATE * decay_s * rng.uniform(0.75, 1.35))))
        if hlen <= 16:
            continue
        st = np.linspace(0.0, hlen / SAMPLE_RATE, hlen, endpoint=False)
        f_jit = base_freq * rng.uniform(0.84, 1.22)
        modal = np.zeros(hlen, dtype=np.float64)
        for idx_r, (r, w) in enumerate(zip(ratios, weights)):
            f_mode = min(14000.0, f_jit * r)
            modal += w * np.sin(2.0 * np.pi * f_mode * st + rng.uniform(0.0, 6.28)) * exp_decay(st, decay_s / (1.0 + 0.45 * idx_r))
        snap = _sos_bandpass(rng.normal(0.0, 1.0, hlen), 1800.0, 13500.0) * exp_decay(st, min(0.008, decay_s * 0.25))
        out[pos : pos + hlen] += (modal * 0.72 + snap * 0.55) * rng.uniform(0.65, 1.05)
    return out


def _cavern_reverb(sig: np.ndarray, rng: np.random.Generator, *, wet: float = 0.20, rt60: float = 0.48, lp_hz: float = 2800.0) -> np.ndarray:
    """Deterministic subterranean mono early reflections + low-pass damped tail (auto-damped on short SFX)."""
    effective_wet = wet * 0.40 if rt60 < 0.60 else wet
    if effective_wet <= 1e-4 or len(sig) < 64:
        return sig
    ir_len = min(len(sig), max(int(SAMPLE_RATE * 0.12), int(SAMPLE_RATE * min(rt60, 0.75))))
    ir = np.zeros(ir_len, dtype=np.float64)
    ir[0] = 1.0
    # Dense diffuse noise tail decaying at rt60
    t_ir = np.linspace(0.0, ir_len / SAMPLE_RATE, ir_len, endpoint=False)
    decay = np.exp(-6.9078 * t_ir / max(0.08, rt60))
    diffuse = _sos_lowpass(rng.normal(0.0, 1.0, ir_len), lp_hz) * decay * 0.085
    # Distinct subterranean wall early reflections (beyond tiny phase correlation offsets)
    taps_ms = [rng.uniform(7.0, 12.0), rng.uniform(14.0, 22.0), rng.uniform(25.0, 36.0), rng.uniform(41.0, 56.0), rng.uniform(62.0, 82.0)]
    for idx_t, ms in enumerate(taps_ms):
        tap_pos = int(ms * SAMPLE_RATE / 1000.0)
        if tap_pos < ir_len:
            ir[tap_pos] += (0.48 * (0.68 ** idx_t)) * (1.0 if idx_t % 2 == 0 else -1.0)
    tail_ir = _sos_lowpass(ir + diffuse, lp_hz)
    tail_ir[0] = 0.0
    wet_sig = _gpu_fftconvolve_1d(sig, tail_ir)
    if wet_sig is None:
        wet_sig = signal.fftconvolve(sig, tail_ir, mode="full")[: len(sig)]
    return sig * (1.0 - effective_wet * 0.35) + wet_sig * effective_wet


def rosenberg_glottal_pulse(
    phase_cycles: np.ndarray,
    *,
    open_quotient: float = 0.56,
    speed_quotient: float = 2.2,
) -> np.ndarray:
    """Physically modeled Rosenberg glottal pulse (differentiated for lip radiation)."""
    n = len(phase_cycles)
    if n == 0:
        return np.zeros(0, dtype=np.float64)
    p = np.mod(phase_cycles, 1.0)
    oq = float(np.clip(open_quotient, 0.25, 0.85))
    sq = float(np.clip(speed_quotient, 1.1, 4.0))
    tp = oq * (sq / (sq + 1.0))
    tn = max(1e-4, oq - tp)

    pulse = np.zeros(n, dtype=np.float64)
    open_mask = p < tp
    close_mask = (p >= tp) & (p < oq)
    if np.any(open_mask):
        pulse[open_mask] = 0.5 * (1.0 - np.cos(np.pi * p[open_mask] / max(1e-4, tp)))
    if np.any(close_mask):
        pulse[close_mask] = np.cos(0.5 * np.pi * (p[close_mask] - tp) / tn)
    # Differentiated glottal flow models lip radiation (+6 dB/oct brilliance)
    diff = np.diff(pulse, prepend=pulse[0])
    peak = float(np.max(np.abs(diff)))
    return diff / max(peak, 1e-6)


def articulated_vocal_tract(
    t: np.ndarray,
    freq_curve: np.ndarray,
    formant_trajectories: list[tuple[float, float, float, float]],
    rng: np.random.Generator,
    *,
    open_quotient: float = 0.55,
    sub_weight: float = 0.45,
    breath_noise: float = 0.14,
) -> np.ndarray:
    """Bio-organic vocal tract combining Rosenberg glottal excitation with time-varying formant glides."""
    n = len(t)
    if n == 0:
        return np.zeros(0, dtype=np.float64)
    f_safe = np.maximum(24.0, np.asarray(freq_curve, dtype=np.float64))
    phase_cycles = np.cumsum(f_safe) / float(SAMPLE_RATE) + rng.uniform(0.0, 1.0)
    glottal = rosenberg_glottal_pulse(phase_cycles, open_quotient=open_quotient)
    sub = np.sin(2.0 * np.pi * (phase_cycles * 0.5) + rng.uniform(0.0, 6.28)) * sub_weight
    breath = _sos_bandpass(_pink_noise(rng, n), 650.0, 4800.0) * breath_noise
    excitation = glottal + sub + breath

    out = np.zeros(n, dtype=np.float64)
    for f_start, f_end, q, gain in formant_trajectories:
        out += gain * _swept_resonant_filter(excitation, float(f_start), float(f_end), q=float(q), n_bands=10)
    return out


def granular_bio_cloud(
    n: int,
    rng: np.random.Generator,
    *,
    freq_range: tuple[float, float] = (280.0, 2400.0),
    density_hz: float = 45.0,
    grain_ms: float = 26.0,
    organic_mode: str = "spore",
) -> np.ndarray:
    """Deterministic Hann-windowed granular bio-texture cloud (spore | chitin | viscous)."""
    if n <= 32:
        return np.zeros(max(0, n), dtype=np.float64)
    dur = n / float(SAMPLE_RATE)
    grain_count = max(4, min(320, int(round(dur * max(4.0, density_hz)))))
    lo_hz, hi_hz = float(freq_range[0]), max(float(freq_range[0]) + 40.0, float(freq_range[1]))
    base_glen = max(48, int(SAMPLE_RATE * grain_ms / 1000.0))
    out = np.zeros(n, dtype=np.float64)

    for _ in range(grain_count):
        glen = min(n, max(32, int(base_glen * rng.uniform(0.65, 1.45))))
        pos = int(rng.integers(0, max(1, n - glen + 1)))
        gt = np.arange(glen, dtype=np.float64) / float(SAMPLE_RATE)
        win = np.hanning(glen)
        f0 = float(rng.uniform(lo_hz, hi_hz))
        amp = float(rng.uniform(0.35, 1.0))
        phase = float(rng.uniform(0.0, 2.0 * np.pi))

        if organic_mode == "chitin":
            grain = (
                np.sin(2.0 * np.pi * f0 * gt + phase) * 0.65
                + np.sin(2.0 * np.pi * min(15500.0, f0 * 2.41) * gt + phase) * 0.35
            ) * np.exp(-gt / max(0.004, (glen / SAMPLE_RATE) * 0.35))
        elif organic_mode == "viscous":
            f_curve = f0 * (0.72 + 0.68 * (gt / max(1e-4, gt[-1])))
            grain = np.sin(2.0 * np.pi * np.cumsum(f_curve) / float(SAMPLE_RATE) + phase)
        else:  # spore
            grain = (
                np.sin(2.0 * np.pi * f0 * gt + phase) * 0.58
                + np.sin(2.0 * np.pi * min(14000.0, f0 * 1.498) * gt + phase * 0.7) * 0.25
                + rng.normal(0.0, 0.22, glen)
            )
        out[pos : pos + glen] += grain * win * amp

    rms = float(np.sqrt(np.mean(out * out)))
    return out / max(rms, 1e-6) * 0.45 if rms > 1e-8 else out


def morphing_formant_tract(
    t: np.ndarray,
    f0_hz: np.ndarray | float,
    formant_keyframes: list[tuple[float, list[tuple[float, float, float]]]],
    rng: np.random.Generator,
    *,
    open_quotient: float = 0.54,
    speed_quotient: float = 2.1,
    sub_weight: float = 0.55,
    breath_noise: float = 0.14,
) -> np.ndarray:
    """Multi-keyframe morphing vocal tract filtering a Rosenberg glottal pulse train.

    Each keyframe is (time_fraction_0_to_1, [(freq_hz, q, gain), ...]).
    """
    n = len(t)
    if n <= 8:
        return np.zeros(max(0, n), dtype=np.float64)
    if np.ndim(f0_hz) == 0:
        f_arr = np.full(n, max(24.0, float(f0_hz)), dtype=np.float64)
    else:
        f_arr = np.maximum(24.0, np.asarray(f0_hz, dtype=np.float64))
    phase_cyc = np.cumsum(f_arr) / float(SAMPLE_RATE) + rng.uniform(0.0, 1.0)
    glottal = rosenberg_glottal_pulse(phase_cyc, open_quotient=open_quotient, speed_quotient=speed_quotient)
    sub = np.sin(np.pi * phase_cyc + rng.uniform(0.0, 2.0 * np.pi)) * float(sub_weight)
    exc = glottal + sub
    if breath_noise > 1e-4:
        exc = exc + _sos_bandpass(rng.normal(0.0, 1.0, n), 700.0, 4800.0) * float(breath_noise)

    if not formant_keyframes:
        return exc
    sorted_kf = sorted(formant_keyframes, key=lambda kf: float(kf[0]))
    if len(sorted_kf) == 1:
        specs = sorted_kf[0][1]
        bank = [(float(f), float(q), float(g)) for f, q, g in specs]
        return _apply_formant_bank(exc, bank)

    seg_count = len(sorted_kf) - 1
    out = np.zeros(n, dtype=np.float64)
    for idx in range(seg_count):
        t0_f, formants_a = sorted_kf[idx]
        t1_f, formants_b = sorted_kf[idx + 1]
        s0 = max(0, min(n, int(round(float(t0_f) * n))))
        s1 = n if idx == seg_count - 1 else max(s0, min(n, int(round(float(t1_f) * n))))
        if s1 - s0 <= 8:
            continue
        pad = min(64, s0)
        seg_exc = exc[s0 - pad : s1]
        seg_out = np.zeros(len(seg_exc), dtype=np.float64)
        for j in range(min(len(formants_a), len(formants_b))):
            fa, qa, ga = formants_a[j]
            fb, qb, gb = formants_b[j]
            q_mid = max(0.5, 0.5 * (float(qa) + float(qb)))
            g_mid = 0.5 * (float(ga) + float(gb))
            seg_out += _swept_resonant_filter(seg_exc, float(fa), float(fb), q=q_mid, n_bands=10) * g_mid
        core = seg_out[pad:]
        if idx > 0 and len(core) > 48:
            xf = np.linspace(0.0, 1.0, 48)
            out[s0 : s0 + 48] = out[s0 : s0 + 48] * (1.0 - xf) + core[:48] * xf
            out[s0 + 48 : s1] = core[48:]
        else:
            out[s0:s1] = core
    return out


def modal_dispersed_impact(
    n: int,
    base_freq: float,
    rng: np.random.Generator,
    *,
    num_modes: int = 8,
    hardness: float = 0.65,
    dispersion: float = 0.14,
    decay_secs: float = 0.42,
) -> np.ndarray:
    """Stiff-plate inharmonic modal dispersion synthesizer using frequency stretching f_k = f0 * k^(1 + dispersion)."""
    if n <= 16:
        return np.zeros(max(0, n), dtype=np.float64)
    t = np.arange(n, dtype=np.float64) / float(SAMPLE_RATE)
    out = np.zeros(n, dtype=np.float64)
    f0 = max(45.0, float(base_freq) * float(rng.uniform(0.92, 1.08)))
    hard = float(np.clip(hardness, 0.1, 1.0))
    disp = float(np.clip(dispersion, 0.0, 0.45))
    for k in range(1, max(2, int(num_modes)) + 1):
        stretch = (k ** (1.0 + disp)) * (1.0 + float(rng.uniform(-0.025, 0.025)))
        f_k = f0 * stretch
        if f_k >= 0.44 * SAMPLE_RATE:
            break
        tau_k = max(0.012, float(decay_secs) / (1.0 + 0.55 * (k - 1) * (1.15 - 0.35 * hard)))
        amp_k = (1.0 / (k ** (1.25 - 0.45 * hard)))
        phase_k = float(rng.uniform(0.0, 2.0 * np.pi))
        out += amp_k * np.sin(2.0 * np.pi * f_k * t + phase_k) * np.exp(-t / tau_k)
    strike = _sos_bandpass(rng.normal(0.0, 1.0, n), 1100.0, min(14000.0, 4200.0 + hard * 7500.0))
    out += strike * exp_decay(t, 0.004 + 0.010 * (1.0 - hard)) * (0.28 + 0.42 * hard)
    peak = float(np.max(np.abs(out)))
    return out / max(peak, 1e-6) if peak > 1e-8 else out


def amethyst_copper_chime(
    t: np.ndarray,
    base_freq: float,
    rng: np.random.Generator,
    *,
    decay: float = 0.14,
    crystal_mix: float = 0.55,
) -> np.ndarray:
    """Warm Minecraft-craft struck amethyst shard & damped copper plate resonance (replaces sterile beeps)."""
    n = len(t)
    if n <= 16:
        return np.zeros(max(0, n), dtype=np.float64)
    f0 = max(65.0, float(base_freq) * float(rng.uniform(0.95, 1.05)))
    ratios = (1.0, 1.498, 2.32, 3.85)
    weights = (0.72, 0.50 * crystal_mix + 0.18, 0.34 * crystal_mix, 0.20 * crystal_mix)
    out = np.zeros(n, dtype=np.float64)
    for k, (r, w) in enumerate(zip(ratios, weights)):
        fk = f0 * r * float(rng.uniform(0.992, 1.008))
        if fk >= 0.44 * SAMPLE_RATE:
            continue
        tau = max(0.012, float(decay) / (1.0 + 0.55 * k))
        ph = float(rng.uniform(0.0, 2.0 * np.pi))
        out += w * np.sin(2.0 * np.pi * fk * t + ph) * np.exp(-t / tau)
    mallet = _sos_bandpass(rng.normal(0.0, 1.0, n), 1400.0, min(11500.0, max(3200.0, f0 * 5.0)))
    out += mallet * exp_decay(t, 0.006) * (0.26 + 0.22 * (1.0 - float(np.clip(crystal_mix, 0.0, 1.0))))
    return _sos_lowpass(out, min(11000.0, max(3600.0, f0 * 7.5)))


def wooden_chitin_knock(
    n: int,
    base_freq: float,
    rng: np.random.Generator,
    *,
    decay: float = 0.045,
    hollowness: float = 0.60,
) -> np.ndarray:
    """Dry hollow bone/wood/chitin tap combining damped body modes and close grit click."""
    if n <= 16:
        return np.zeros(max(0, n), dtype=np.float64)
    t = np.arange(n, dtype=np.float64) / float(SAMPLE_RATE)
    f0 = max(75.0, float(base_freq) * float(rng.uniform(0.90, 1.12)))
    h = float(np.clip(hollowness, 0.1, 1.0))
    body = np.zeros(n, dtype=np.float64)
    for idx, (r, w) in enumerate(((1.0, 0.85), (1.76, 0.52 * h), (2.94, 0.26))):
        fk = f0 * r
        if fk < 0.44 * SAMPLE_RATE:
            body += w * np.sin(2.0 * np.pi * fk * t + float(rng.uniform(0.0, 6.28))) * np.exp(-t / max(0.008, decay / (1.0 + 0.7 * idx)))
    grit = _sos_bandpass(rng.normal(0.0, 1.0, n), 1100.0, 7600.0) * exp_decay(t, 0.007) * 0.48
    return body + grit


def foley_particle_crackle(
    n: int,
    rng: np.random.Generator,
    *,
    lo_hz: float = 1200.0,
    hi_hz: float = 6800.0,
    count: int = 8,
    wet: bool = False,
) -> np.ndarray:
    """Discrete gravel/moss/spore particle micro-impacts scattered across n samples."""
    if n <= 32 or count <= 0:
        return np.zeros(max(0, n), dtype=np.float64)
    out = np.zeros(n, dtype=np.float64)
    for k in range(int(count)):
        pos = int(rng.uniform(0.0, 0.82) * (n - 32))
        plen = min(n - pos, int(SAMPLE_RATE * rng.uniform(0.014, 0.036)))
        if plen < 16:
            continue
        pt = np.arange(plen, dtype=np.float64) / float(SAMPLE_RATE)
        noise = rng.normal(0.0, 1.0, plen)
        lo = max(120.0, float(lo_hz) * float(rng.uniform(0.80, 1.25)))
        hi = min(14000.0, max(lo + 300.0, float(hi_hz) * float(rng.uniform(0.80, 1.15))))
        grain = _sos_bandpass(noise, lo, hi) * exp_decay(pt, 0.006 if not wet else 0.011)
        out[pos : pos + plen] += grain * float(rng.uniform(0.45, 0.95))
    return out


def karplus_strong_chitin(
    n: int,
    freq_hz: float,
    rng: np.random.Generator,
    *,
    damping: float = 0.988,
    brightness: float = 0.65,
    stiffness: float = 0.10,
    excitation: str = "pluck",
) -> np.ndarray:
    """Block-vectorized Karplus-Strong physical waveguide for chitin plates, elastic caps & amber chimes."""
    if n <= 16:
        return np.zeros(max(0, n), dtype=np.float64)
    period = max(8, min(n // 2, int(round(SAMPLE_RATE / max(32.0, float(freq_hz))))))
    out = np.zeros(n, dtype=np.float64)

    exc = rng.normal(0.0, 1.0, period)
    if excitation == "bounce":
        et = np.linspace(0.0, 1.0, period, endpoint=False)
        exc = np.sin(np.pi * et) * 0.85 + exc * 0.22
    elif excitation == "click":
        exc *= np.exp(-np.linspace(0.0, 4.5, period))
    elif excitation == "strike":
        exc = _sos_lowpass(exc, min(12000.0, float(freq_hz) * 6.5))
    exc = exc - np.mean(exc)
    peak_e = float(np.max(np.abs(exc)))
    if peak_e > 1e-8:
        exc /= peak_e
    out[:period] = exc

    damp = float(np.clip(damping, 0.80, 0.9985))
    b_hi = float(np.clip(brightness, 0.15, 0.95))
    b_lo = 1.0 - b_hi
    stiff = float(np.clip(stiffness, 0.0, 0.35))

    pos = period
    while pos < n:
        end = min(n, pos + period)
        chunk = end - pos
        prev = out[pos - period : pos - period + chunk]
        prev_shift = np.empty(chunk, dtype=np.float64)
        prev_shift[0] = out[pos - period - 1] if (pos - period - 1) >= 0 else prev[0]
        if chunk > 1:
            prev_shift[1:] = prev[:-1]
        lp = b_hi * prev + b_lo * prev_shift
        if stiff > 1e-4 and chunk > 2:
            # Light 2nd-order dispersion approximates rigid chitin / crystalline overtone stretch
            disp = np.roll(lp, 1)
            disp[0] = lp[0]
            lp = (1.0 - stiff) * lp + stiff * (0.5 * (lp + disp))
        out[pos:end] = damp * lp
        pos = end

    out = out - np.mean(out)
    peak = float(np.max(np.abs(out)))
    return out / max(peak, 1e-6) if peak > 1e-8 else out


def fdn4_hadamard_diffuser(
    sig: np.ndarray,
    rng: np.random.Generator,
    *,
    base_delays_ms: tuple[float, float, float, float] = (17.3, 23.9, 31.7, 43.1),
    rt60: float = 0.55,
    wet: float = 0.26,
    damping_hz: float = 4200.0,
) -> np.ndarray:
    """Block-vectorized 4x4 Hadamard Feedback Delay Network for lush, non-metallic organic tails."""
    n = len(sig)
    if wet <= 1e-4 or n < 128:
        return sig
    delays = [
        max(64, int(round((ms + float(rng.uniform(-1.1, 1.1))) * SAMPLE_RATE / 1000.0)))
        for ms in base_delays_ms
    ]
    d_min = min(delays)
    d_max = max(delays)
    buf_len = n + d_max
    bufs = np.zeros((4, buf_len), dtype=np.float64)

    rt = max(0.08, float(rt60))
    gains = np.array([10.0 ** (-3.0 * (d / float(SAMPLE_RATE)) / rt) for d in delays], dtype=np.float64)
    gains = np.clip(gains, 0.15, 0.94)
    lp_a = float(np.clip(np.exp(-2.0 * np.pi * max(400.0, damping_hz) / float(SAMPLE_RATE)), 0.05, 0.92))
    lp_b = 1.0 - lp_a

    # Orthogonal 4x4 Hadamard mixing matrix (unitary: H @ H.T = I)
    hadamard = 0.5 * np.array(
        [
            [1.0, 1.0, 1.0, 1.0],
            [1.0, -1.0, 1.0, -1.0],
            [1.0, 1.0, -1.0, -1.0],
            [1.0, -1.0, -1.0, 1.0],
        ],
        dtype=np.float64,
    )
    wet_out = np.zeros(n, dtype=np.float64)
    lp_state = np.zeros(4, dtype=np.float64)
    in_scale = np.array([0.52, -0.48, 0.50, -0.50], dtype=np.float64)

    pos = 0
    while pos < n:
        block = min(d_min, n - pos)
        w_idx = d_max + pos
        read = np.empty((4, block), dtype=np.float64)
        for ch in range(4):
            r_start = w_idx - delays[ch]
            read[ch] = bufs[ch, r_start : r_start + block]
        wet_out[pos : pos + block] = 0.5 * (read[0] + read[1] + read[2] + read[3])

        # Vectorized one-pole damping along each channel block
        damped = np.empty_like(read)
        for ch in range(4):
            y_ch, zf = signal.lfilter([lp_b], [1.0, -lp_a], read[ch], zi=[lp_state[ch] * lp_a])
            damped[ch] = y_ch * gains[ch]
            lp_state[ch] = float(y_ch[-1])

        mixed = hadamard @ damped
        x_blk = sig[pos : pos + block]
        bufs[:, w_idx : w_idx + block] = mixed + np.outer(in_scale, x_blk)
        pos += block

    return sig * (1.0 - wet * 0.30) + wet_out * wet


def active_rms(wave: np.ndarray, gate_ratio: float = 0.05) -> float:
    """Compute RMS over the active signal envelope so reverb tails don't skew loudness."""
    if len(wave) == 0:
        return 0.0
    abs_w = np.abs(wave)
    peak = float(np.max(abs_w))
    if peak < 1e-9:
        return 0.0
    active = abs_w[abs_w >= peak * gate_ratio]
    if len(active) < 8:
        active = abs_w
    return float(np.sqrt(np.mean(active * active)))


def _soft_knee_limit(wave: np.ndarray, threshold: float = 0.78, ceiling: float = TRUE_PEAK_CEILING) -> np.ndarray:
    """Smooth nonlinear true-peak soft limiter preserving punch without hard digital clipping."""
    out = wave.copy()
    abs_w = np.abs(out)
    over = abs_w > threshold
    if np.any(over):
        headroom = ceiling - threshold
        excess = (abs_w[over] - threshold) / max(1e-6, headroom)
        shaped = threshold + headroom * np.tanh(excess)
        out[over] = np.sign(out[over]) * shaped
    return np.clip(out, -ceiling, ceiling)


def master_waveform(
    wave: np.ndarray,
    *,
    loudness_group: str = "combat",
    target_peak: float | None = None,
    sound_id: str = "",
    species: str = "default",
    category: str = "",
) -> np.ndarray:
    """High-pass clean, process through Pedalboard + Convolution IR, converge LUFS/RMS, and apply fade guards."""
    from scripts.sound_studio.mastering import master_with_pedalboard

    return master_with_pedalboard(
        wave,
        sound_id=sound_id,
        species=species,
        loudness_group=loudness_group,
        category=category,
        target_peak=target_peak,
    )


def synthesize_waveform(
    sound_id: str,
    *,
    recipe: str,
    duration: float,
    base_freq: float,
    mod_freq: float = 8.0,
    variant: str = "default",
    species: str = "default",
    loudness_group: str = "combat",
    seed_offset: int = 0,
    variation_idx: int = 0,
    target_peak: float | None = None,
    category: str = "",
) -> np.ndarray:
    """Synthesize a deterministic 44.1 kHz mono float64 waveform in [-TRUE_PEAK_CEILING, +TRUE_PEAK_CEILING]."""
    rng = deterministic_rng(sound_id, seed_offset, variation_idx)

    # Intentional acoustic variation across takes so multi-variation playback never sounds identical
    if variation_idx > 0:
        # Spread deterministic pitch shifts wider across variations while staying organic
        pitch_ladder = (0.93, 1.07, 0.87, 1.13)
        pitch_mult = pitch_ladder[(variation_idx - 1) % len(pitch_ladder)] * float(rng.uniform(0.975, 1.025))
        dur_mult = float(rng.uniform(0.91, 1.09))
        formant_shift = pitch_ladder[(variation_idx + 1) % len(pitch_ladder)] * float(rng.uniform(0.96, 1.04))
    else:
        pitch_mult = 1.0
        dur_mult = 1.0
        formant_shift = 1.0

    dur = max(0.11, float(duration) * dur_mult)
    bf = max(32.0, float(base_freq) * pitch_mult)
    mf = max(1.0, float(mod_freq) * float(rng.uniform(0.92, 1.08)))

    n = max(int(SAMPLE_RATE * 0.11), int(round(SAMPLE_RATE * dur)))
    t = np.linspace(0.0, dur, n, endpoint=False, dtype=np.float64)
    white = rng.normal(0.0, 1.0, n)
    pink = _pink_noise(rng, n)
    brown = _brown_noise(rng, n)
    pjit = float(rng.uniform(0.0, 2.0 * np.pi))

    from scripts.sound_studio.block_synth import (
        BLOCK_FUNCTIONAL_IDS,
        synthesize_block_functional,
        synthesize_block_surface,
    )
    from scripts.sound_studio.boss_synth import (
        BOSS_CINEMATIC_IDS,
        BOSS_PART_IMPACT_IDS,
        synthesize_boss_sound,
    )
    from scripts.sound_studio.entity_synth import (
        ENTITY_SYNTH_IDS,
        synthesize_entity_sound,
    )
    from scripts.sound_studio.item_synth import (
        ITEM_PROJECTILE_IDS,
        synthesize_item_sound,
    )
    from scripts.sound_studio.system_env_synth import (
        SYSTEM_ENV_IDS,
        synthesize_system_env_sound,
    )

    is_boss = loudness_group == "boss_cinematic" or species == "colossal_organic" and dur >= 0.85
    rev_wet = 0.28 if is_boss else 0.18
    rev_rt60 = 0.62 if is_boss else 0.42

    if sound_id in BOSS_CINEMATIC_IDS or sound_id in BOSS_PART_IMPACT_IDS:
        wave = synthesize_boss_sound(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )
    elif sound_id in BLOCK_FUNCTIONAL_IDS:
        wave = synthesize_block_functional(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )
    elif sound_id in ENTITY_SYNTH_IDS:
        wave = synthesize_entity_sound(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )
    elif sound_id in ITEM_PROJECTILE_IDS:
        wave = synthesize_item_sound(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )
    elif sound_id in SYSTEM_ENV_IDS or recipe == "system_env":
        wave = synthesize_system_env_sound(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )
    elif recipe == "block_functional":
        wave = synthesize_block_functional(
            sound_id,
            duration=dur,
            base_freq=bf,
            mod_freq=mf,
            rng=rng,
            formant_shift=formant_shift,
        )

    elif recipe == "chitter":
        # Mandible clicks + insectoid tracheal formant bursts
        snaps = _chitin_modal_hits(n, rng, bf * 1.4, rng.integers(5, 9), 0.042, spread_frac=0.80)
        pulses = int(rng.integers(4, 8))
        vocal = np.zeros(n, dtype=np.float64)
        for i in range(pulses):
            start = int((i / pulses) * n * 0.84 + rng.uniform(0.0, n * 0.04))
            plen = min(n - start, max(128, int(n * rng.uniform(0.07, 0.15))))
            if plen <= 32:
                continue
            seg_t = t[:plen]
            f_sw = bf * rng.uniform(0.88, 1.24) * (1.0 + 0.38 * np.sin(2.0 * np.pi * mf * seg_t + rng.uniform(0, 6.28)))
            tone = _fm_tone(seg_t, f_sw, mf * 3.4, 2.6, rng.uniform(0, 6.28))
            env = np.sin(np.linspace(0.0, np.pi, plen)) ** 1.3
            vocal[start : start + plen] += tone * env
        vocal = _apply_formant_bank(vocal, [(bf * 1.15 * formant_shift, 3.2, 1.0), (bf * 2.55 * formant_shift, 4.0, 0.75)])
        air = _sos_bandpass(pink, bf * 0.9, min(11000.0, bf * 4.0)) * 0.25
        wave = _cavern_reverb((vocal * 0.85 + snaps * 0.72 + air) * adsr_envelope(n, 0.03, 0.25, 0.45, 0.28), rng, wet=0.14, rt60=0.35)

    elif recipe == "growl":
        # Bio-horror guttural roar with glottal sub-harmonics, chest formants, and throat mucus rattle
        f_env = bf * (1.06 - 0.28 * (t / dur) + 0.07 * np.sin(2.0 * np.pi * (mf * 0.85) * t + pjit))
        source = _glottal_sub_source(t, f_env, rng, harmonics=12, sub_weight=0.85, rattle_hz=mf * 4.5 + rng.uniform(2, 8))
        tract = _apply_formant_bank(
            source,
            [
                (max(75.0, bf * 1.45 * formant_shift), 2.8, 1.1),
                (bf * 3.8 * formant_shift, 3.4, 0.85),
                (min(4200.0, bf * 8.2 * formant_shift), 4.2, 0.50),
            ],
        )
        mucus = _wet_squelch_layer(n, rng, bf * 1.8, int(rng.integers(3, 7)), intensity=0.45)
        sub_bed = np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, f_env * 0.5)) / SAMPLE_RATE + pjit) * 0.55
        rumble = _sos_bandpass(brown, 42.0, max(180.0, bf * 1.6)) * 0.42
        raw = np.tanh((tract * 1.35 + sub_bed + mucus + rumble) * 1.45) * adsr_envelope(n, 0.07, 0.22, 0.56, 0.30)
        wave = _cavern_reverb(raw, rng, wet=rev_wet, rt60=rev_rt60, lp_hz=2600.0)

    elif recipe == "hiss":
        # Swept serpentine/spitter respiration + saliva crackle + sinister throat undertone
        breath = _swept_resonant_filter(pink * 0.8 + white * 0.35, min(9500.0, bf * 3.8), max(180.0, bf * 0.72), q=2.2)
        sibilance = _sos_bandpass(white, max(1600.0, bf * 2.2), 11500.0) * 0.48
        undertone = _glottal_sub_source(t, np.full(n, max(65.0, bf * 0.26)), rng, harmonics=5, sub_weight=0.5, rattle_hz=28.0) * 0.24
        wet_droplets = _wet_squelch_layer(n, rng, bf * 1.4, int(rng.integers(4, 8)), intensity=0.35)
        raw = (breath * 1.15 + sibilance + undertone + wet_droplets) * adsr_envelope(n, 0.04, 0.24, 0.45, 0.32)
        wave = _cavern_reverb(raw, rng, wet=0.16, rt60=0.40)

    elif recipe == "gurgle":
        # Visceral boiling bio-sac mucus + dark sub cavity
        wet = _wet_squelch_layer(n, rng, bf, int(rng.integers(7, 13)), intensity=1.15)
        cavity = _apply_formant_bank(
            wet + brown * 0.3,
            [(max(85.0, bf * 0.85 * formant_shift), 3.0, 1.0), (bf * 2.1 * formant_shift, 3.8, 0.72)],
        )
        sub = np.sin(2.0 * np.pi * (max(48.0, bf * 0.45)) * t + pjit) * 0.28
        raw = np.tanh((cavity * 1.2 + wet * 0.5 + sub) * 1.25) * adsr_envelope(n, 0.05, 0.20, 0.60, 0.25)
        wave = _cavern_reverb(raw, rng, wet=0.18, rt60=0.44)

    elif recipe == "grind":
        # Abrasive chitin-on-basalt scraping + serrated resonance
        scrapes = 0.38 + 0.62 * np.abs(np.sin(2.0 * np.pi * (mf * 0.75) * t + 0.9 * np.sin(2.0 * np.pi * 4.2 * t + pjit)))
        grit = _swept_resonant_filter(pink + white * 0.45, bf * 1.1, min(7500.0, bf * 3.9), q=3.0) * scrapes
        chitin_crunch = _chitin_modal_hits(n, rng, bf * 1.6, int(rng.integers(5, 9)), 0.055, spread_frac=0.82) * 0.65
        low_grind = _glottal_sub_source(t, bf * (0.9 + 0.15 * np.sin(2.0 * np.pi * mf * t)), rng, harmonics=7, sub_weight=0.55) * 0.42
        raw = np.tanh((grit * 1.35 + chitin_crunch + low_grind) * 1.3) * adsr_envelope(n, 0.03, 0.24, 0.52, 0.26)
        wave = _cavern_reverb(raw, rng, wet=0.20, rt60=0.45)

    elif recipe == "crackle":
        # Smoldering fungal cinder + snapping pyro embers + low thermal draft
        draft = _sos_bandpass(brown, 75.0, max(350.0, bf * 1.2)) * 0.55 + _sos_bandpass(pink, 600.0, 4200.0) * 0.40
        pops = np.zeros(n, dtype=np.float64)
        for _ in range(int(rng.integers(10, 22))):
            idx = int(rng.uniform(0.02, 0.90) * n)
            plen = min(n - idx, max(32, int(SAMPLE_RATE * rng.uniform(0.008, 0.034))))
            st = np.linspace(0.0, plen / SAMPLE_RATE, plen, endpoint=False)
            pf = bf * rng.uniform(1.1, 4.8)
            pop = (_fm_tone(st, pf, pf * 0.35, 1.4, rng.uniform(0, 6.28)) + rng.normal(0.0, 0.6, plen)) * exp_decay(st, 0.0065)
            pops[idx : idx + plen] += pop * rng.uniform(0.45, 1.15)
        raw = (draft + pops * 1.15) * adsr_envelope(n, 0.03, 0.20, 0.55, 0.28)
        wave = _cavern_reverb(raw, rng, wet=0.14, rt60=0.35)

    elif recipe == "crystal_hum":
        if species == "synaptic_eldritch":
            # Haunted eldritch synaptic dissonance (tritone/minor-2nd crystal partials + dark sub-pulse)
            partials = (0.5, 1.0, 1.414, 1.89, 2.71, 3.86)
            amps = (0.48, 0.52, 0.36, 0.26, 0.16, 0.09)
            lfo = 1.0 + 0.018 * np.sin(2.0 * np.pi * mf * t + pjit)
            wave_tone = np.zeros(n, dtype=np.float64)
            for r, a in zip(partials, amps):
                wave_tone += a * np.sin(2.0 * np.pi * np.cumsum(bf * r * lfo) / SAMPLE_RATE + rng.uniform(0, 6.28))
            eerie_air = _sos_bandpass(pink, max(150.0, bf * 0.7), min(5200.0, bf * 4.5)) * 0.32
            raw = (wave_tone + eerie_air) * adsr_envelope(n, 0.08, 0.24, 0.58, 0.34)
            wave = _cavern_reverb(raw, rng, wet=0.26, rt60=0.58, lp_hz=3600.0)
        else:
            # Clean bio-relic / purifier crystal hum
            partials = (1.0, 1.50, 2.005, 3.01, 4.02)
            amps = (0.55, 0.32, 0.24, 0.14, 0.08)
            lfo = 1.0 + 0.010 * np.sin(2.0 * np.pi * mf * t + pjit)
            wave_tone = np.zeros(n, dtype=np.float64)
            for r, a in zip(partials, amps):
                wave_tone += a * np.sin(2.0 * np.pi * np.cumsum(bf * r * lfo) / SAMPLE_RATE + rng.uniform(0, 6.28))
            wave = _cavern_reverb(wave_tone * adsr_envelope(n, 0.06, 0.22, 0.55, 0.32), rng, wet=0.16, rt60=0.40)

    elif recipe == "heavy_thud":
        # Seismic body/stone slam with sub-bass drop + chitin/rock scatter + cavern boom
        f_drop = np.maximum(28.0, bf * (0.42 + 2.2 * exp_decay(t, max(0.032, dur * 0.18))))
        sub = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.44)
        transient = _chitin_modal_hits(n, rng, bf * 3.2, 2, 0.045, spread_frac=0.10) * 0.75
        debris = _sos_bandpass(brown * 0.7 + pink * 0.45, 55.0, max(900.0, bf * 6.0)) * exp_decay(t, dur * 0.32)
        raw = np.tanh((sub * 1.15 + transient + debris * 0.85) * 1.45)
        wave = _cavern_reverb(raw, rng, wet=0.26, rt60=0.54, lp_hz=2200.0)

    elif recipe == "screech":
        # Shrieking bio-horror predator/matriarch vocal tract + breathless rasp + dark cavern tail
        curve = np.sin(np.pi * t / dur)
        f_sweep = bf * (0.82 + 0.78 * curve + 0.04 * np.sin(2.0 * np.pi * 34.0 * t + pjit))
        vocal_src = _glottal_sub_source(t, f_sweep, rng, harmonics=13, sub_weight=0.45, rattle_hz=36.0)
        fm_bite = _fm_tone(t, f_sweep * 1.01, mf * 5.2, 3.6, pjit, rng.uniform(0, 6.28)) * 0.55
        shriek = _apply_formant_bank(
            vocal_src + fm_bite,
            [
                (max(240.0, bf * 1.2 * formant_shift), 2.8, 0.9),
                (min(4800.0, bf * 2.7 * formant_shift), 3.6, 1.1),
                (min(7200.0, bf * 4.4 * formant_shift), 4.2, 0.65),
            ],
        )
        rasp = _swept_resonant_filter(pink, min(6800.0, bf * 2.5), max(400.0, bf * 1.1), q=2.0) * 0.45
        raw = np.tanh((shriek + rasp) * 1.35) * adsr_envelope(n, 0.04, 0.18, 0.56, 0.28)
        wave = _cavern_reverb(raw, rng, wet=0.30 if is_boss else 0.20, rt60=0.65 if is_boss else 0.44, lp_hz=4200.0)

    elif recipe == "bellow":
        # Massive subterranean boss roar: raspy inhale -> tectonic sub-octave + dark chest formants
        inhale_len = min(n // 5, int(SAMPLE_RATE * 0.14))
        inhale = np.zeros(n, dtype=np.float64)
        if inhale_len > 32:
            st_in = np.linspace(0.0, inhale_len / SAMPLE_RATE, inhale_len, endpoint=False)
            inhale[:inhale_len] = _sos_bandpass(pink[:inhale_len], 140.0, 1400.0) * (st_in / max(1e-4, st_in[-1])) * 0.65
        f_roar = bf * (1.04 - 0.18 * (t / dur) + 0.045 * np.sin(2.0 * np.pi * mf * t + pjit))
        roar_src = _glottal_sub_source(t, f_roar, rng, harmonics=12, sub_weight=0.95, rattle_hz=22.0)
        chest = _apply_formant_bank(
            roar_src,
            [
                (max(70.0, bf * 1.65 * formant_shift), 2.5, 1.15),
                (bf * 4.2 * formant_shift, 3.2, 0.85),
                (min(3200.0, bf * 8.5 * formant_shift), 3.8, 0.45),
            ],
        )
        sub_boom = np.sin(2.0 * np.pi * np.cumsum(np.maximum(30.0, f_roar * 0.5)) / SAMPLE_RATE + pjit) * 0.72
        rumble = _sos_bandpass(brown, 36.0, 260.0) * 0.45
        body = np.tanh((chest + sub_boom + rumble) * 1.4) * adsr_envelope(n, 0.10, 0.20, 0.62, 0.34)
        wave = _cavern_reverb(inhale + body, rng, wet=0.32, rt60=0.68, lp_hz=2400.0)

    elif recipe == "bio_pulse":
        # Deep neural surge + dark harmonic swell + synaptic micro-crackles
        env_p = np.sin(np.pi * t / dur) ** 1.25
        f_mod = bf * (1.0 + 0.16 * np.sin(2.0 * np.pi * mf * t + pjit))
        core = _fm_tone(t, f_mod, bf * 0.5, 1.6, pjit, rng.uniform(0, 6.28)) * 0.72
        sub = np.sin(2.0 * np.pi * (max(40.0, bf * 0.5)) * t + rng.uniform(0, 6.28)) * 0.48
        swept = _swept_resonant_filter(pink, max(160.0, bf * 0.7), min(5400.0, bf * 3.8), q=2.6) * 0.45
        raw = (core + sub + swept) * env_p
        wave = _cavern_reverb(raw, rng, wet=0.22, rt60=0.48, lp_hz=3400.0)

    elif recipe == "tech_chime":
        # Sterile, clinical Purification Tech — pure harmonic triad + crisp switch click, minimal reverb
        notes = (1.0, 1.25, 1.498)
        wave_c = np.zeros(n, dtype=np.float64)
        step = n // 3
        for i, ratio in enumerate(notes):
            start = min(n - 32, int(i * step * 0.52))
            st = t[: n - start]
            f = min(9000.0, bf * ratio)
            note_p = rng.uniform(0.0, 6.28)
            tone = (
                np.sin(2.0 * np.pi * f * st + note_p) * 0.72
                + np.sin(2.0 * np.pi * min(14000.0, f * 2.0) * st + note_p) * 0.22
                + np.sin(2.0 * np.pi * min(16000.0, f * 3.0) * st) * 0.08
            )
            wave_c[start:] += tone * exp_decay(st, max(0.045, dur * 0.42))
        relay_click = _sos_bandpass(white, 2400.0, 11000.0) * exp_decay(t, 0.012) * 0.35
        wave = _cavern_reverb(wave_c + relay_click, rng, wet=0.10, rt60=0.24, lp_hz=6500.0)

    elif recipe == "weapon_slash":
        # Whipping air arc + wet/chitinous tearing edge + resonant steel/enzyme ring
        air = _swept_resonant_filter(pink * 0.8 + white * 0.4, min(10500.0, bf * 5.2), max(180.0, bf * 0.65), q=1.9) * exp_decay(t, dur * 0.44)
        tear = _wet_squelch_layer(n, rng, bf * 1.4, 3, intensity=0.5) * exp_decay(t, dur * 0.35)
        edge = _fm_tone(t, bf * (1.5 - 0.85 * (t / dur)), mf * 2.4, 1.8, pjit) * exp_decay(t, dur * 0.30) * 0.55
        raw = air * 1.1 + tear + edge
        wave = _cavern_reverb(raw, rng, wet=0.14, rt60=0.32, lp_hz=4500.0)

    elif recipe == "projectile_pop":
        # Pressurized bio-sac pop / spore burst + wet droplet splatter
        f_drop = np.maximum(45.0, bf * (0.48 + 1.65 * exp_decay(t, max(0.015, dur * 0.16))))
        pop = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.34)
        splash = _wet_squelch_layer(n, rng, bf * 1.3, 4, intensity=0.75) * exp_decay(t, dur * 0.32)
        snap = _sos_bandpass(white, 1200.0, 9500.0) * exp_decay(t, 0.025) * 0.45
        raw = np.tanh((pop * 1.05 + splash + snap) * 1.35)
        wave = _cavern_reverb(raw, rng, wet=0.12, rt60=0.30, lp_hz=4000.0)

    elif recipe == "explosion_purge":
        # Cinematic multi-stage blast: crack transient -> tectonic sub shockwave -> roar + purge shimmer
        crack = _sos_bandpass(white, 1400.0, 11000.0) * exp_decay(t, 0.022) * 0.9
        sub = np.sin(2.0 * np.pi * np.cumsum(np.maximum(28.0, bf * (0.35 + 2.1 * exp_decay(t, 0.075)))) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.55)
        roar = _sos_bandpass(brown * 0.85 + pink * 0.55, 45.0, 2200.0) * exp_decay(t, dur * 0.44)
        rise_f = np.minimum(3200.0, bf * (1.8 + 2.6 * (t / dur)))
        shimmer = np.sin(2.0 * np.pi * np.cumsum(rise_f) / SAMPLE_RATE + rng.uniform(0, 6.28)) * adsr_envelope(n, 0.06, 0.25, 0.35, 0.40) * 0.32
        raw = np.tanh((crack + sub * 1.15 + roar * 1.1 + shimmer) * 1.35)
        wave = _cavern_reverb(raw, rng, wet=0.28, rt60=0.62, lp_hz=3200.0)

    elif recipe == "hurt":
        # Species-differentiated damage reactions
        if species == "purifier_tech":
            ring = _chitin_modal_hits(n, rng, bf * 1.8, 2, 0.09, spread_frac=0.12)
            spark = _sos_bandpass(white, 2800.0, 12000.0) * exp_decay(t, 0.05) * 0.55
            servo = np.sin(2.0 * np.pi * np.cumsum(bf * (1.2 - 0.65 * (t / dur))) / SAMPLE_RATE + pjit) * exp_decay(t, dur * 0.45) * 0.5
            wave = _cavern_reverb(ring + spark + servo, rng, wet=0.12, rt60=0.25, lp_hz=6200.0)
        elif species == "wet_visceral":
            slap = _sos_bandpass(pink, 140.0, 1800.0) * exp_decay(t, 0.045) * 0.9
            squelch = _wet_squelch_layer(n, rng, bf * 1.1, 4, intensity=0.95) * exp_decay(t, dur * 0.5)
            grunt = _apply_formant_bank(_glottal_sub_source(t, bf * (1.15 - 0.45 * (t / dur)), rng, harmonics=8, sub_weight=0.7), [(bf * 1.6 * formant_shift, 2.8, 1.0)]) * 0.85
            wave = _cavern_reverb(np.tanh((slap + squelch + grunt) * 1.3) * adsr_envelope(n, 0.02, 0.2, 0.45, 0.35), rng, wet=0.18, rt60=0.40)
        elif species == "pyro_fungal":
            pop = np.sin(2.0 * np.pi * np.cumsum(bf * (1.4 - 0.7 * (t / dur))) / SAMPLE_RATE + pjit) * exp_decay(t, 0.05)
            steam = _sos_bandpass(white, 2200.0, 10000.0) * exp_decay(t, dur * 0.45) * 0.65
            cinder = _chitin_modal_hits(n, rng, bf * 2.0, 3, 0.03, spread_frac=0.35) * 0.5
            wave = _cavern_reverb((pop + steam + cinder) * adsr_envelope(n, 0.015, 0.22, 0.40, 0.38), rng, wet=0.15, rt60=0.34)
        elif species == "synaptic_eldritch":
            fracture = _chitin_modal_hits(n, rng, bf * 1.414, 3, 0.11, spread_frac=0.22)
            bent = _fm_tone(t, bf * (1.2 - 0.5 * (t / dur)), bf * 0.707, 2.2, pjit) * 0.72
            wave = _cavern_reverb((fracture * 0.7 + bent) * adsr_envelope(n, 0.02, 0.2, 0.42, 0.38), rng, wet=0.24, rt60=0.50)
        elif species == "colossal_organic":
            impact = _sos_bandpass(brown, 45.0, 350.0) * exp_decay(t, 0.12) * 1.2
            grunt = _apply_formant_bank(_glottal_sub_source(t, bf * (1.05 - 0.38 * (t / dur)), rng, harmonics=10, sub_weight=0.9), [(bf * 1.5 * formant_shift, 2.5, 1.1), (bf * 3.5 * formant_shift, 3.0, 0.65)])
            wave = _cavern_reverb(np.tanh((impact + grunt) * 1.35) * adsr_envelope(n, 0.02, 0.22, 0.46, 0.36), rng, wet=0.26, rt60=0.54)
        else:  # insectoid_chitin / default
            snap = _chitin_modal_hits(n, rng, bf * 1.4, 3, 0.045, spread_frac=0.18)
            squeak = _fm_tone(t, bf * (1.3 - 0.65 * (t / dur)), mf * 2.8, 2.3, pjit) * 0.68
            wave = _cavern_reverb((snap + squeak) * adsr_envelope(n, 0.015, 0.2, 0.40, 0.38), rng, wet=0.16, rt60=0.36)

    elif recipe == "death":
        # Species-differentiated dramatic collapses
        if species == "purifier_tech":
            f_down = np.maximum(45.0, bf * np.exp(-1.7 * t / dur))
            whine = (np.sin(2.0 * np.pi * np.cumsum(f_down) / SAMPLE_RATE + pjit) * 0.75 + _fm_tone(t, f_down * 1.5, 35.0, 0.8) * 0.3)
            click_pos = min(n - 64, int(n * 0.72))
            clicks = np.zeros(n, dtype=np.float64)
            clicks[click_pos:] += _chitin_modal_hits(n - click_pos, rng, 950.0, 2, 0.04, spread_frac=0.35) * 0.7
            static = _sos_bandpass(white, 1500.0, 8500.0) * exp_decay(t, dur * 0.25) * 0.35
            wave = _cavern_reverb((whine * adsr_envelope(n, 0.02, 0.2, 0.45, 0.40) + clicks + static), rng, wet=0.12, rt60=0.28)
        elif species == "wet_visceral":
            f_down = np.maximum(38.0, bf * np.exp(-1.1 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=9, sub_weight=0.85), [(bf * 1.2 * formant_shift, 2.5, 1.0)]) * adsr_envelope(n, 0.03, 0.2, 0.4, 0.45)
            deflate = _wet_squelch_layer(n, rng, bf * 1.2, 8, intensity=1.15) * adsr_envelope(n, 0.05, 0.25, 0.45, 0.45)
            wave = _cavern_reverb(np.tanh((groan + deflate) * 1.35), rng, wet=0.24, rt60=0.52)
        elif species == "pyro_fungal":
            f_down = np.maximum(45.0, bf * np.exp(-1.4 * t / dur))
            puff = _sos_bandpass(pink, 180.0, 2400.0) * exp_decay(t, dur * 0.42) * 0.9
            ember = _chitin_modal_hits(n, rng, bf * 2.4, 6, 0.04, spread_frac=0.7) * 0.55
            whistle = _fm_tone(t, f_down, mf * 1.4, 1.5, pjit) * adsr_envelope(n, 0.02, 0.2, 0.35, 0.45) * 0.55
            wave = _cavern_reverb(puff + ember + whistle, rng, wet=0.18, rt60=0.40)
        elif species == "synaptic_eldritch":
            f_down = np.maximum(38.0, bf * np.exp(-1.35 * t / dur))
            shatter = _chitin_modal_hits(n, rng, bf * 1.5, 5, 0.14, spread_frac=0.45)
            void = (_fm_tone(t, f_down, f_down * 0.707, 2.6, pjit) * 0.7 + np.sin(2.0 * np.pi * np.cumsum(f_down * 0.5) / SAMPLE_RATE) * 0.55)
            wave = _cavern_reverb((shatter * 0.65 + void * adsr_envelope(n, 0.03, 0.22, 0.45, 0.46)), rng, wet=0.32, rt60=0.68, lp_hz=3800.0)
        elif species == "colossal_organic":
            f_down = np.maximum(32.0, bf * np.exp(-1.15 * t / dur))
            groan = _apply_formant_bank(_glottal_sub_source(t, f_down, rng, harmonics=12, sub_weight=0.95), [(max(65.0, bf * 1.4 * formant_shift), 2.4, 1.1), (bf * 3.1 * formant_shift, 3.0, 0.7)]) * adsr_envelope(n, 0.04, 0.22, 0.48, 0.44)
            # Heavy body hitting cavern floor halfway through death
            collapse = np.zeros(n, dtype=np.float64)
            imp = int(n * rng.uniform(0.36, 0.48))
            clen = n - imp
            if clen > 32:
                st_c = np.linspace(0.0, clen / SAMPLE_RATE, clen, endpoint=False)
                collapse[imp:] = (np.sin(2.0 * np.pi * np.cumsum(48.0 * (0.6 + 1.5 * exp_decay(st_c, 0.04))) / SAMPLE_RATE) + _sos_bandpass(brown[imp:], 40.0, 320.0)) * exp_decay(st_c, dur * 0.26)
            wave = _cavern_reverb(np.tanh((groan + collapse * 0.85) * 1.4), rng, wet=0.32, rt60=0.70, lp_hz=2400.0)
        else:  # insectoid_chitin
            f_down = np.maximum(55.0, bf * np.exp(-1.45 * t / dur))
            rattle = _chitin_modal_hits(n, rng, bf * 1.3, 6, 0.055, spread_frac=0.72)
            wail = _fm_tone(t, f_down, mf * 1.8, 2.4, pjit) * adsr_envelope(n, 0.02, 0.2, 0.4, 0.46) * 0.65
            wave = _cavern_reverb(rattle * 0.8 + wail, rng, wet=0.20, rt60=0.45)

    elif recipe == "rift_hum":
        f1 = bf * (1.0 + 0.024 * np.sin(2.0 * np.pi * 1.7 * t + pjit))
        pad = (
            np.sin(2.0 * np.pi * np.cumsum(f1 * 0.5) / SAMPLE_RATE + pjit) * 0.65
            + np.sin(2.0 * np.pi * np.cumsum(f1) / SAMPLE_RATE + rng.uniform(0, 6.28)) * 0.48
            + np.sin(2.0 * np.pi * np.cumsum(f1 * 1.414) / SAMPLE_RATE + rng.uniform(0, 6.28)) * 0.34
            + _fm_tone(t, f1 * 2.0, mf, 1.5, pjit) * 0.22
        )
        air = _sos_bandpass(brown * 0.6 + pink * 0.5, 90.0, max(400.0, bf * 3.5)) * 0.45
        wave = _cavern_reverb((pad + air) * adsr_envelope(n, 0.16, 0.22, 0.62, 0.36), rng, wet=0.34, rt60=0.72, lp_hz=2600.0)

    elif recipe == "heartbeat":
        # Deep lub-dub biological pulse with dark chest resonance (timing shifts > 25 ms prevent correlation!)
        wave_h = np.zeros(n, dtype=np.float64)
        second_offset = dur * rng.uniform(0.36, 0.48)
        for beat_t, gain in ((rng.uniform(0.02, 0.06), 1.0), (second_offset, 0.74)):
            idx = min(n - 32, int(beat_t * SAMPLE_RATE))
            blen = min(n - idx, int(SAMPLE_RATE * 0.24))
            st = np.linspace(0.0, blen / SAMPLE_RATE, blen, endpoint=False)
            f_b = max(36.0, bf) * (0.58 + 1.25 * exp_decay(st, 0.042))
            thump = np.sin(2.0 * np.pi * np.cumsum(f_b) / SAMPLE_RATE + rng.uniform(0, 6.28)) * exp_decay(st, 0.068)
            rush = _sos_bandpass(pink[idx : idx + blen], 65.0, 420.0) * exp_decay(st, 0.075) * 0.45
            wave_h[idx : idx + blen] += (thump + rush) * gain
        wave = _cavern_reverb(np.tanh(wave_h * 1.45), rng, wet=0.28, rt60=0.55, lp_hz=1400.0)

    elif recipe == "sizzle":
        chem = _sos_bandpass(white, 2400.0, 12500.0)
        melt = _swept_resonant_filter(pink, 1400.0, 420.0, q=2.5) * (0.45 + 0.55 * np.sin(2.0 * np.pi * mf * 2.2 * t + pjit))
        fizz = _wet_squelch_layer(n, rng, bf * 1.8, 5, intensity=0.55)
        raw = (chem * 0.72 + melt * 0.55 + fizz * 0.48) * adsr_envelope(n, 0.03, 0.20, 0.55, 0.30)
        wave = _cavern_reverb(raw, rng, wet=0.08, rt60=0.22, lp_hz=6800.0)

    elif recipe == "feast":
        crunch = _chitin_modal_hits(n, rng, bf * 1.4, 5, 0.045, spread_frac=0.76) * 0.75
        tear = _sos_bandpass(pink, 220.0, 2800.0) * (0.35 + 0.65 * np.abs(np.sin(2.0 * np.pi * mf * t + pjit)))
        squelch = _wet_squelch_layer(n, rng, bf * 1.1, 6, intensity=0.95)
        raw = np.tanh((crunch + tear * 0.8 + squelch) * 1.3) * adsr_envelope(n, 0.04, 0.24, 0.52, 0.28)
        wave = _cavern_reverb(raw, rng, wet=0.14, rt60=0.35)

    elif recipe == "hatch":
        crack_len = min(n // 3, int(SAMPLE_RATE * 0.11))
        wave_e = np.zeros(n, dtype=np.float64)
        if crack_len > 32:
            wave_e[:crack_len] += _chitin_modal_hits(crack_len, rng, bf * 2.1, 3, 0.032, spread_frac=0.65)
        chirp_start = min(n - 32, int(crack_len * 0.65))
        rem = n - chirp_start
        st2 = np.linspace(0.0, rem / SAMPLE_RATE, rem, endpoint=False)
        f_c = bf * (0.85 + 0.65 * (st2 / max(1e-4, st2[-1])))
        bio = _glottal_sub_source(st2, f_c, rng, harmonics=7, sub_weight=0.5) * 0.65 + _wet_squelch_layer(rem, rng, bf, 3, intensity=0.55)
        wave_e[chirp_start:] += bio * exp_decay(st2, dur * 0.45)
        wave = _cavern_reverb(wave_e, rng, wet=0.18, rt60=0.40)

    elif recipe == "portal_ignite":
        f_up = bf * (0.50 + 1.55 * (t / dur))
        beam = _glottal_sub_source(t, f_up, rng, harmonics=9, sub_weight=0.8) * 0.65 + _fm_tone(t, f_up * 1.414, bf * 0.5, 2.2, pjit) * 0.45
        tear = _swept_resonant_filter(pink, 280.0, 5600.0, q=2.4) * adsr_envelope(n, 0.06, 0.25, 0.45, 0.30) * 0.65
        wave = _cavern_reverb(np.tanh((beam + tear) * 1.3), rng, wet=0.32, rt60=0.68, lp_hz=3600.0)

    elif recipe.startswith("surface_"):
        action = recipe.split("_", 1)[1]
        wave = synthesize_block_surface(
            variant,
            action,
            duration=dur,
            base_freq=bf,
            rng=rng,
        )

    elif recipe.startswith("step_"):
        if recipe == "step_hover":
            pulse_f = bf * (1.0 + 0.06 * np.sin(2.0 * np.pi * mf * t + pjit))
            hum = np.sin(2.0 * np.pi * np.cumsum(pulse_f) / SAMPLE_RATE + pjit) * 0.45 + _fm_tone(t, pulse_f * 1.5, mf, 0.8, pjit) * 0.30
            air = _sos_bandpass(pink, 500.0, 2600.0) * 0.45
            wave = _cavern_reverb((hum + air) * adsr_envelope(n, 0.14, 0.22, 0.42, 0.32), rng, wet=0.12, rt60=0.28)
        elif recipe == "step_arachnid":
            wave_s = np.zeros(n, dtype=np.float64)
            taps = (0.008, rng.uniform(0.24, 0.36), rng.uniform(0.50, 0.64), rng.uniform(0.72, 0.84))
            for idx_f in taps[: int(rng.integers(3, 5))]:
                pos = min(n - 32, int(idx_f * n))
                clen = min(n - pos, int(SAMPLE_RATE * 0.048))
                wave_s[pos : pos + clen] += _chitin_modal_hits(clen, rng, bf * rng.uniform(1.1, 1.8), 1, 0.022)
            wave = _cavern_reverb(wave_s, rng, wet=0.14, rt60=0.32)
        elif recipe == "step_colossal":
            f_drop = np.maximum(30.0, bf * (0.45 + 1.45 * exp_decay(t, 0.042)))
            sub = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * 0.92
            rumble = _sos_bandpass(brown, 42.0, 750.0) * 0.72
            gravel = _chitin_modal_hits(n, rng, bf * 3.5, 4, 0.04, spread_frac=0.45) * 0.45
            wave = _cavern_reverb(np.tanh((sub + rumble + gravel) * exp_decay(t, dur * 0.44) * 1.35), rng, wet=0.24, rt60=0.52, lp_hz=1800.0)
        elif recipe == "step_heavy":
            f_drop = np.maximum(38.0, bf * (0.55 + 1.15 * exp_decay(t, 0.032)))
            sub = np.sin(2.0 * np.pi * np.cumsum(f_drop) / SAMPLE_RATE + pjit) * 0.75
            grit = _sos_bandpass(pink, 140.0, 2600.0) * 0.65
            chitin = _chitin_modal_hits(n, rng, bf * 2.2, 2, 0.03, spread_frac=0.25) * 0.40
            wave = _cavern_reverb((sub + grit + chitin) * exp_decay(t, dur * 0.38), rng, wet=0.18, rt60=0.40, lp_hz=2200.0)
        else:  # step_chitin
            click = _chitin_modal_hits(n, rng, bf * 1.1, 2, 0.028, spread_frac=0.28)
            grit = _sos_bandpass(pink, 450.0, 3800.0) * exp_decay(t, dur * 0.25) * 0.5
            wave = _cavern_reverb(click + grit, rng, wet=0.12, rt60=0.28)
    else:
        raise ValueError(f"Unknown sound synthesis recipe: {recipe}")

    from scripts.sound_studio.ai_stem_generator import blend_with_ai_stem, is_ai_hybrid

    # Physical, close-miked machine cues: avoid adding the generated stem's
    # spectral texture to the latch, breath, housing and piston transients.
    if is_ai_hybrid(sound_id) and sound_id not in PHYSICAL_MACHINE_CUES:
        wave = blend_with_ai_stem(
            wave,
            sound_id,
            variation_idx=variation_idx,
            rng=rng,
        )

    return master_waveform(
        wave,
        loudness_group=loudness_group,
        target_peak=target_peak,
        sound_id=sound_id,
        species=species,
        category=category,
    )


def float_to_pcm16(wave: np.ndarray) -> bytes:
    """Convert normalized float64 [-1, 1] array to little-endian signed 16-bit PCM bytes."""
    clipped = np.clip(wave, -1.0, 1.0)
    pcm = np.round(clipped * 32767.0).astype("<i2")
    return pcm.tobytes()


def encode_ogg_vorbis_mono(pcm16_bytes: bytes, out_path: Path, ffmpeg_exe: str | None = None, *, is_loop: bool = False) -> None:
    """Encode atomically, allowing for peaks introduced by Vorbis compression."""
    ffmpeg = ffmpeg_exe or find_ffmpeg()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = out_path.with_name(f"{out_path.stem}.{os.getpid()}.tmp.ogg")
    cmd = [
        ffmpeg,
        "-y",
        "-loglevel",
        "error",
        "-f",
        "s16le",
        "-ar",
        str(SAMPLE_RATE),
        "-ac",
        "1",
        "-i",
        "pipe:0",
        "-c:a",
        "libvorbis",
        "-q:a",
        VORBIS_QUALITY,
        "-f",
        "ogg",
        str(tmp_path),
    ]
    source = np.frombuffer(pcm16_bytes, dtype="<i2").astype(np.float64)
    gain = 1.0
    edge_fade_ms = 0.0
    last_err = ""
    try:
        for attempt in range(5):
            adjusted = source * gain
            if edge_fade_ms:
                fade_n = min(len(adjusted) // 2, int(SAMPLE_RATE * edge_fade_ms / 1000.0))
                if fade_n:
                    adjusted[:fade_n] *= np.linspace(0.0, 1.0, fade_n)
                    adjusted[-fade_n:] *= np.linspace(1.0, 0.0, fade_n)
            encoded_pcm = np.round(adjusted).astype("<i2").tobytes()
            proc = subprocess.run(cmd, input=encoded_pcm, capture_output=True, check=False)
            if proc.returncode != 0 or not tmp_path.is_file() or tmp_path.stat().st_size <= 64:
                last_err = proc.stderr.decode("utf-8", errors="replace")
                time.sleep(0.08 * (attempt + 1))
                continue
            decoded = subprocess.run(
                [ffmpeg, "-v", "error", "-xerror", "-i", str(tmp_path), "-f", "f32le", "pipe:1"],
                capture_output=True, check=False,
            )
            if decoded.returncode != 0 or not decoded.stdout:
                last_err = decoded.stderr.decode("utf-8", errors="replace") or "empty decoded audio"
                continue
            samples = np.frombuffer(decoded.stdout, dtype="<f4")
            peak = float(np.max(np.abs(signal.resample_poly(samples, 4, 1))))
            if not np.isfinite(peak):
                last_err = "non-finite decoded true peak"
                continue
            edges_ok = is_loop or (np.max(np.abs(samples[:4])) <= 0.019 and np.max(np.abs(samples[-4:])) <= 0.019)
            if peak <= TRUE_PEAK_CEILING and edges_ok:
                os.replace(tmp_path, out_path)
                return
            # Re-encode from the original PCM, never from a lossy decoded copy.
            if peak > TRUE_PEAK_CEILING:
                gain *= (TRUE_PEAK_CEILING * 0.98) / peak
            if not edges_ok:
                edge_fade_ms = max(5.0, edge_fade_ms * 2.0)
            last_err = f"decoded true peak {peak:.4f}, edge samples {samples[0]:.4f}/{samples[-1]:.4f}"
        raise RuntimeError(f"ffmpeg failed encoding {out_path}: {last_err}")
    finally:
        if tmp_path.exists():
            tmp_path.unlink()


def inspect_ogg_vorbis(path: Path) -> dict[str, Any]:
    """Validate OggS container and parse Vorbis identification header (mono + 44100 Hz)."""
    data = path.read_bytes()
    if len(data) < 64 or not data.startswith(b"OggS"):
        return {"ok": False, "error": "Missing OggS capture pattern or truncated file"}
    marker = data.find(b"\x01vorbis")
    if marker < 0 or marker + 30 > len(data):
        return {"ok": False, "error": "Missing Vorbis identification header"}
    version, channels, sample_rate = struct.unpack_from("<IBI", data, marker + 7)
    return {
        "ok": version == 0 and channels == 1 and sample_rate == SAMPLE_RATE,
        "version": version,
        "channels": channels,
        "sample_rate": sample_rate,
        "size_bytes": len(data),
    }
