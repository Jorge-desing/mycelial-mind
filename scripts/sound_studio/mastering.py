"""Headless Pedalboard + Convolution IR + ITU-R BS.1770-4 LUFS mastering chain."""

from __future__ import annotations

import threading
from pathlib import Path
from typing import Any
import wave

import numpy as np
from pedalboard import (
    Chorus,
    Compressor,
    Convolution,
    HighShelfFilter,
    HighpassFilter,
    Limiter,
    LowShelfFilter,
    PeakFilter,
    Pedalboard,
)
import pyloudnorm as pyln
from scipy import signal

from scripts.sound_studio.ir_bank import ensure_ir_bank, select_ir_space

try:
    from .cuda_dsp import (
        gpu_ir_convolve as _gpu_ir_convolve,
        is_cuda_available as _is_cuda_available,
        preload_ir_bank_gpu as _preload_ir_bank_gpu,
    )
except ImportError:
    try:
        from cuda_dsp import (
            gpu_ir_convolve as _gpu_ir_convolve,
            is_cuda_available as _is_cuda_available,
            preload_ir_bank_gpu as _preload_ir_bank_gpu,
        )
    except ImportError:
        from sound_studio.cuda_dsp import (
            gpu_ir_convolve as _gpu_ir_convolve,
            is_cuda_available as _is_cuda_available,
            preload_ir_bank_gpu as _preload_ir_bank_gpu,
        )

SAMPLE_RATE = 44100
TRUE_PEAK_CEILING = 0.94

SEAMLESS_LOOP_IDS: frozenset[str] = frozenset(
    {
        "biome_caverns_loop",
        "biome_sanctum_loop",
        "biome_forest_loop",
        "fungal_tide_ambience",
        "purification_pylon_hum",
        "mycelial_rift_hum",
        "sovereign_nexus_hum",
        "boss_combat_tension_bed",
        "sinkhole_updraft_loop",
        "hive_tension_bed_low",
        "hive_swarm_combat_bed",
    }
)

BARK_UNMASK_CARVE_BANDS_HZ: tuple[float, ...] = (1450.0, 3200.0)


def apply_bark_unmasking_carve(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> np.ndarray:
    """Apply gentle dual-band psychoacoustic unmasking carve on continuous beds so foreground cues stay crystal clear."""
    n = len(wave)
    if n < 256:
        return wave
    out = np.asarray(wave, dtype=np.float64)
    nyq = 0.5 * float(sample_rate)
    for center_hz, q_val, db_cut in ((1450.0, 1.2, -2.0), (3200.0, 1.4, -2.2)):
        if center_hz >= nyq * 0.90:
            continue
        gain = 10.0 ** (db_cut / 20.0)
        w0 = 2.0 * np.pi * center_hz / float(sample_rate)
        alpha = np.sin(w0) / (2.0 * q_val)
        sqrt_a = np.sqrt(gain)
        b0 = 1.0 + alpha * sqrt_a
        b1 = -2.0 * np.cos(w0)
        b2 = 1.0 - alpha * sqrt_a
        a0 = 1.0 + alpha / sqrt_a
        a1 = -2.0 * np.cos(w0)
        a2 = 1.0 - alpha / sqrt_a
        b = np.array([b0 / a0, b1 / a0, b2 / a0], dtype=np.float64)
        a = np.array([1.0, a1 / a0, a2 / a0], dtype=np.float64)
        out = signal.lfilter(b, a, out)
    return out

# Must match dsp_engine.LOUDNESS_TARGETS_DBFS
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

_IR_INIT_LOCK = threading.RLock()
_IR_PATHS_CACHE: dict[str, Path] | None = None
_IR_WAVES_CACHE: dict[str, np.ndarray] | None = None
_THREAD_LOCAL = threading.local()


def _get_ir_paths() -> dict[str, Path]:
    global _IR_PATHS_CACHE
    if _IR_PATHS_CACHE is None:
        with _IR_INIT_LOCK:
            if _IR_PATHS_CACHE is None:
                _IR_PATHS_CACHE = ensure_ir_bank()
    return _IR_PATHS_CACHE


def _read_mono_wav_float(path: Path) -> np.ndarray:
    with wave.open(str(path), "rb") as wf:
        ch = wf.getnchannels()
        raw = wf.readframes(wf.getnframes())
    pcm = np.frombuffer(raw, dtype="<i2").astype(np.float64) / 32768.0
    if ch > 1:
        pcm = pcm.reshape(-1, ch).mean(axis=1)
    # Match JUCE/Pedalboard Convolution (Normalise::yes) gain scaling: 0.125 / ||ir||_2
    l2 = float(np.sqrt(np.sum(pcm * pcm)))
    if l2 > 1e-8:
        pcm = pcm * (0.125 / l2)
    return pcm


def _get_ir_waves() -> dict[str, np.ndarray]:
    global _IR_WAVES_CACHE
    if _IR_WAVES_CACHE is not None:
        return _IR_WAVES_CACHE
    paths = _get_ir_paths()
    with _IR_INIT_LOCK:
        if _IR_WAVES_CACHE is None:
            _IR_WAVES_CACHE = {name: _read_mono_wav_float(p) for name, p in paths.items()}
    return _IR_WAVES_CACHE


def preload_ir_bank_for_gpu() -> int:
    """Load the 6 room IR .wavs once and upload them to VRAM when CUDA is available."""
    waves = _get_ir_waves()
    return _preload_ir_bank_gpu(waves) if waves else 0


def _active_rms(wave: np.ndarray, gate_ratio: float = 0.05) -> float:
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
    out = wave.copy()
    abs_w = np.abs(out)
    over = abs_w > threshold
    if np.any(over):
        headroom = ceiling - threshold
        excess = (abs_w[over] - threshold) / max(1e-6, headroom)
        shaped = threshold + headroom * np.tanh(excess)
        out[over] = np.sign(out[over]) * shaped
    return np.clip(out, -ceiling, ceiling)


def _apply_fade_guards(wave: np.ndarray, fade_in_ms: float = 3.0, fade_out_ms: float = 14.0) -> np.ndarray:
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


def _circular_loop_crossfade(wave: np.ndarray, crossfade_ms: float = 90.0) -> np.ndarray:
    """Wrap end-of-buffer reverb & drone tails smoothly into start-of-buffer for seamless looping."""
    n = len(wave)
    cf = min(n // 5, int(SAMPLE_RATE * crossfade_ms / 1000.0))
    if cf < 32:
        return wave
    out = wave.copy()
    theta = np.linspace(0.0, 0.5 * np.pi, cf, dtype=np.float64)
    fade_in = np.sin(theta)
    fade_out = np.cos(theta)
    head = out[:cf].copy()
    tail = out[-cf:].copy()
    out[:cf] = head * fade_in + tail * fade_out
    out[-cf:] = tail * fade_out + head * fade_in
    return out


def measure_true_peak_linear(wave: np.ndarray) -> float:
    """Measure 4x oversampled inter-sample linear true peak."""
    w = np.nan_to_num(np.asarray(wave, dtype=np.float64), nan=0.0, posinf=0.0, neginf=0.0)
    if len(w) == 0:
        return 0.0
    sample_peak = float(np.max(np.abs(w)))
    if sample_peak < 1e-8 or len(w) < 16:
        return sample_peak
    oversampled = signal.resample_poly(w, 4, 1)
    return max(sample_peak, float(np.max(np.abs(oversampled))))


def measure_true_peak_dbfs(wave: np.ndarray) -> float:
    """Measure 4x oversampled inter-sample true peak in dBFS."""
    tp = measure_true_peak_linear(wave)
    return float(20.0 * np.log10(max(tp, 1e-6)))


def true_peak_limit_4x(wave: np.ndarray, ceiling: float = TRUE_PEAK_CEILING) -> np.ndarray:
    """Enforce 4x oversampled true-peak ceiling below target threshold."""
    out = np.clip(wave, -ceiling, ceiling)
    tp = measure_true_peak_linear(out)
    if tp > ceiling and tp > 1e-8:
        out = out * ((ceiling * 0.992) / tp)
    return np.clip(out, -ceiling, ceiling)


def measure_integrated_lufs(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> float:
    """Measure ITU-R BS.1770-4 integrated LUFS, adapting block size for short SFX clips."""
    w = np.nan_to_num(np.asarray(wave, dtype=np.float64), nan=0.0, posinf=0.0, neginf=0.0)
    if len(w) == 0 or float(np.max(np.abs(w))) < 1e-8:
        return -70.0

    dur = len(w) / float(sample_rate)
    act_db = 20.0 * np.log10(max(_active_rms(w), 1e-6))
    try:
        block_size = 0.400 if dur >= 0.45 else max(0.060, round((dur * 0.65) / 0.02) * 0.02)
        meters: dict[tuple[int, float], pyln.Meter] | None = getattr(_THREAD_LOCAL, "lufs_meters", None)
        if meters is None:
            meters = {}
            _THREAD_LOCAL.lufs_meters = meters
        m_key = (int(sample_rate), round(float(block_size), 3))
        meter = meters.get(m_key)
        if meter is None:
            meter = pyln.Meter(sample_rate, block_size=block_size)
            meters[m_key] = meter
        lufs = float(meter.integrated_loudness(w))
        if np.isfinite(lufs) and -90.0 <= lufs <= 12.0:
            return lufs
    except Exception:
        pass
    return float(max(-70.0, act_db - 0.6))


def _profile_params(sound_id: str, species: str, loudness_group: str, ir_space: str) -> tuple[Any, ...]:
    sp = species.lower()
    is_boss = loudness_group == "boss_cinematic"

    if is_boss or sp == "colossal_organic":
        hp_hz = 26.0
        low_db = 1.6
        peak_hz, peak_db, peak_q = 420.0, 0.8, 0.9
        high_db = 1.0 if "matriarch" in sound_id else 0.4
        comp_thresh, comp_ratio = -17.0, 2.8
    elif ir_space in ("purifier_hall",) or "purif" in sp or "synaptic_node" in sound_id:
        hp_hz = 42.0
        low_db = -0.5
        peak_hz, peak_db, peak_q = 2600.0, 1.4, 1.1
        high_db = 1.6
        comp_thresh, comp_ratio = -18.5, 2.3
    elif ir_space == "spore_canopy":
        hp_hz = 36.0
        low_db = 0.2
        peak_hz, peak_db, peak_q = 1250.0, 0.9, 0.9
        high_db = 1.1
        comp_thresh, comp_ratio = -19.0, 2.0
    elif loudness_group in ("footstep", "block_light"):
        hp_hz = 38.0
        low_db = 0.0
        peak_hz, peak_db, peak_q = 1400.0, 0.8, 1.0
        high_db = 0.8
        comp_thresh, comp_ratio = -20.0, 1.9
    else:
        hp_hz = 32.0
        low_db = 0.8
        peak_hz, peak_db, peak_q = 950.0, 0.9, 0.9
        high_db = 0.9
        comp_thresh, comp_ratio = -18.0, 2.2

    if loudness_group == "footstep":
        ir_mix = 0.07
    elif is_boss or ir_space == "dimensional_rift":
        ir_mix = 0.16
    elif loudness_group in ("block_light", "block_heavy"):
        ir_mix = 0.09
    else:
        ir_mix = 0.11

    chorus_mix = 0.07 if ir_space in ("dimensional_rift", "monolith_vault") else 0.0
    return (ir_space, hp_hz, low_db, peak_hz, peak_db, peak_q, high_db, comp_thresh, comp_ratio, ir_mix, chorus_mix)


def _build_pre_chain(key: tuple[Any, ...]) -> list[Any]:
    _, hp_hz, low_db, peak_hz, peak_db, peak_q, high_db, comp_thresh, comp_ratio, _, chorus_mix = key
    chain: list[Any] = [
        HighpassFilter(cutoff_frequency_hz=hp_hz),
        LowShelfFilter(cutoff_frequency_hz=110.0, gain_db=low_db, q=0.75),
        PeakFilter(cutoff_frequency_hz=peak_hz, gain_db=peak_db, q=peak_q),
        HighShelfFilter(cutoff_frequency_hz=5200.0, gain_db=high_db, q=0.75),
        Compressor(threshold_db=comp_thresh, ratio=comp_ratio, attack_ms=6.0, release_ms=80.0),
    ]
    if chorus_mix > 0.0:
        chain.append(Chorus(rate_hz=0.85, depth=0.08, centre_delay_ms=7.0, feedback=0.12, mix=float(chorus_mix)))
    return chain


def _get_thread_board(key: tuple[Any, ...]) -> Pedalboard:
    cache: dict[tuple[Any, ...], Pedalboard] | None = getattr(_THREAD_LOCAL, "boards", None)
    if cache is None:
        cache = {}
        _THREAD_LOCAL.boards = cache
    board = cache.get(key)
    if board is not None:
        return board

    ir_space, _, _, _, _, _, _, _, _, ir_mix, _ = key
    ir_path = _get_ir_paths()[ir_space]
    chain = _build_pre_chain(key)
    chain.extend(
        [
            Convolution(impulse_response_filename=str(ir_path), mix=ir_mix),
            Limiter(threshold_db=-0.65, release_ms=35.0),
        ]
    )
    board = Pedalboard(chain)
    cache[key] = board
    return board


def _get_thread_boards_gpu_split(key: tuple[Any, ...]) -> tuple[Pedalboard, Pedalboard]:
    cache: dict[tuple[Any, ...], tuple[Pedalboard, Pedalboard]] | None = getattr(_THREAD_LOCAL, "split_boards", None)
    if cache is None:
        cache = {}
        _THREAD_LOCAL.split_boards = cache
    pair = cache.get(key)
    if pair is not None:
        return pair

    pre_board = Pedalboard(_build_pre_chain(key))
    post_board = Pedalboard([Limiter(threshold_db=-0.65, release_ms=35.0)])
    pair = (pre_board, post_board)
    cache[key] = pair
    return pair


def master_with_pedalboard(
    wave: np.ndarray,
    *,
    sound_id: str = "",
    species: str = "default",
    loudness_group: str = "combat",
    category: str = "",
    target_peak: float | None = None,
) -> np.ndarray:
    """Process a waveform through EQ, compression, convolution IR, LUFS-aware leveling, and limiting."""
    w = np.nan_to_num(np.asarray(wave, dtype=np.float64), nan=0.0, posinf=0.0, neginf=0.0)
    n = len(w)
    if n == 0:
        return w
    w = w - np.mean(w)

    peak_in = float(np.max(np.abs(w)))
    if peak_in < 1e-8:
        return np.zeros(n, dtype=np.float64)
    # Soft analog-style pre-saturation + normalized pre-FX headroom
    w_norm = w * (0.82 / peak_in)
    drive = 1.18 if (loudness_group == "boss_cinematic" or species == "colossal_organic") else 1.08
    w_in = (np.tanh(w_norm * drive) / np.tanh(drive)).astype(np.float32)

    ir_space = select_ir_space(sound_id, species, loudness_group, category)
    key = _profile_params(sound_id, species, loudness_group, ir_space)
    is_loop = sound_id in SEAMLESS_LOOP_IDS
    preroll = min(n // 2, int(SAMPLE_RATE * 1.25)) if (is_loop and n > 2048) else 0
    postroll = min(n // 4, int(SAMPLE_RATE * 0.25)) if (is_loop and n > 2048) else 0
    if postroll % 2 == 1:
        postroll -= 1
    w_proc_in = np.concatenate([w_in[-preroll:], w_in, w_in[:postroll]]) if (preroll > 0 and postroll > 0) else (
        np.concatenate([w_in[-preroll:], w_in]) if preroll > 0 else w_in
    )
    n_proc = len(w_proc_in)

    processed: np.ndarray | None = None
    if _is_cuda_available() and n_proc >= 2048:
        pre_board, post_board = _get_thread_boards_gpu_split(key)
        pre_out = pre_board.process(w_proc_in, SAMPLE_RATE, reset=True).astype(np.float64)
        ir_wave = _get_ir_waves().get(ir_space)
        conv_out = _gpu_ir_convolve(pre_out, ir_space, ir_wave, mix=float(key[9]))
        if conv_out is not None:
            processed = post_board.process(conv_out.astype(np.float32), SAMPLE_RATE, reset=True).astype(np.float64)
    if processed is None:
        board = _get_thread_board(key)
        processed = board.process(w_proc_in, SAMPLE_RATE, reset=True).astype(np.float64)
    if len(processed) > n_proc:
        processed = processed[:n_proc]
    elif len(processed) < n_proc:
        processed = np.pad(processed, (0, n_proc - len(processed)))
    if preroll > 0:
        if postroll >= 32:
            seg = processed[preroll : preroll + n + postroll]
            alpha = np.linspace(0.0, 1.0, postroll, dtype=np.float64)
            bridge = seg[n : n + postroll] * (1.0 - alpha) + seg[:postroll] * alpha
            half = postroll // 2
            processed = np.concatenate([bridge[half:], seg[postroll:n], bridge[:half]])
        else:
            processed = processed[preroll : preroll + n]

    processed = processed - np.mean(processed)

    target_dbfs = LOUDNESS_TARGETS_DBFS.get(loudness_group, -14.0)
    target_rms = 10.0 ** (target_dbfs / 20.0)
    ceiling = float(target_peak) if target_peak is not None else TRUE_PEAK_CEILING

    # Shape boundaries early so short 0.16s hits account for fade-in/out attenuation,
    # while loops keep full boundary energy warmed up from preroll.
    if is_loop:
        # Carve subtle spectral pockets at Bark 11 (1450 Hz) & Bark 16 (3200 Hz) BEFORE circular wrap fade
        ext = min(n // 4, 2048)
        padded = np.concatenate([processed[-ext:], processed])
        processed = apply_bark_unmasking_carve(padded, SAMPLE_RATE)[ext : ext + n]
    else:
        processed = _apply_fade_guards(processed, fade_in_ms=2.5, fade_out_ms=14.0)

    # Converge toward calibrated active RMS while factoring ITU-R BS.1770-4 LUFS perceptually
    for pass_idx in range(3):
        cur_rms = _active_rms(processed)
        if cur_rms > 1e-7:
            rms_gain = target_rms / cur_rms
            if pass_idx in (0, 2):
                lufs_db = measure_integrated_lufs(processed, SAMPLE_RATE)
                lufs_delta_db = float(np.clip((target_dbfs - 0.5) - lufs_db, -1.2, 1.2))
                perceptual_nudge = 10.0 ** (0.20 * lufs_delta_db / 20.0)
                gain = float(np.clip(rms_gain * perceptual_nudge, 0.08, 12.0))
            else:
                gain = float(np.clip(rms_gain, 0.08, 12.0))
            processed = processed * gain
        processed = _soft_knee_limit(processed, threshold=min(0.76, ceiling * 0.82), ceiling=ceiling)

    # Strict post-fade active RMS lock so no short percussive hit drifts > +-1.2 dB
    for _ in range(2):
        cur_rms = _active_rms(processed)
        if cur_rms > 1e-7:
            delta_db = 20.0 * np.log10(cur_rms) - target_dbfs
            if abs(delta_db) > 1.2:
                processed *= float(np.clip(target_rms / cur_rms, 0.25, 4.0))
                processed = _soft_knee_limit(processed, threshold=min(0.76, ceiling * 0.82), ceiling=ceiling)

    if is_loop:
        processed = _apply_fade_guards(processed, fade_in_ms=1.5, fade_out_ms=1.5)
    else:
        processed = _apply_fade_guards(processed, fade_in_ms=0.5, fade_out_ms=4.0)
    processed = processed - np.mean(processed)
    # Re-zero extreme edge samples if DC subtraction nudged sample 0 / N-1 slightly
    if len(processed) >= 8:
        processed[:4] *= np.linspace(0.0, 1.0, 4)
        processed[-4:] *= np.linspace(1.0, 0.0, 4)
    return true_peak_limit_4x(processed, ceiling=ceiling)
