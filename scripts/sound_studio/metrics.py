"""Acoustic quality metrics, psychoacoustic QA v4 (Bark/LUFS/LRA/masking), and local HTML audition console v4."""

from __future__ import annotations

import html
import json
import os
import subprocess
from pathlib import Path
from typing import Any

import numpy as np
from scipy import signal

from scripts.sound_studio.dsp_engine import (
    LOUDNESS_TARGETS_DBFS,
    SAMPLE_RATE,
    active_rms,
    find_ffmpeg,
)

# Standard Zwicker 24 Bark critical-band edges (Hz)
BARK_EDGES_HZ: tuple[float, ...] = (
    20.0, 100.0, 200.0, 300.0, 400.0, 510.0, 630.0, 770.0,
    920.0, 1080.0, 1270.0, 1480.0, 1720.0, 2000.0, 2320.0, 2700.0,
    3150.0, 3700.0, 4400.0, 5300.0, 6400.0, 7700.0, 9500.0, 12000.0, 15500.0,
)
BARK_CENTERS_HZ: tuple[float, ...] = tuple(
    round(float(np.sqrt(BARK_EDGES_HZ[i] * BARK_EDGES_HZ[i + 1])), 1)
    for i in range(len(BARK_EDGES_HZ) - 1)
)
_K_WEIGHT_SOS_CACHE: dict[int, tuple[np.ndarray, np.ndarray]] = {}


def _get_k_weighting_filters(sample_rate: int) -> tuple[np.ndarray, np.ndarray]:
    """Return ITU-R BS.1770-4 Stage-1 pre-filter + Stage-2 RLB high-pass SOS filters."""
    cached = _K_WEIGHT_SOS_CACHE.get(sample_rate)
    if cached is not None:
        return cached
    # Exact digital bilinear design for BS.1770-4 at arbitrary sample_rate
    # Stage 1: +4 dB high-shelf at ~1681.97 Hz (Q=0.707175)
    f0 = 1681.974450955533
    g_db = 3.999843853973347
    q = 0.7071752369554196
    k = np.tan(np.pi * f0 / float(sample_rate))
    vh = 10.0 ** (g_db / 20.0)
    vb = vh ** 0.4996667741545416
    a0 = 1.0 + k / q + k * k
    b_shelf = np.array([
        (vh + vb * k / q + k * k) / a0,
        2.0 * (k * k - vh) / a0,
        (vh - vb * k / q + k * k) / a0,
    ], dtype=np.float64)
    a_shelf = np.array([
        1.0,
        2.0 * (k * k - 1.0) / a0,
        (1.0 - k / q + k * k) / a0,
    ], dtype=np.float64)
    sos1 = signal.tf2sos(b_shelf, a_shelf)
    # Stage 2: 2nd-order RLB high-pass at 38.135 Hz (Q=0.500327)
    f_hp = 38.13547087602444
    q_hp = 0.5003270373238773
    k_hp = np.tan(np.pi * f_hp / float(sample_rate))
    a0_hp = 1.0 + k_hp / q_hp + k_hp * k_hp
    b_hp = np.array([1.0 / a0_hp, -2.0 / a0_hp, 1.0 / a0_hp], dtype=np.float64)
    a_hp = np.array([1.0, 2.0 * (k_hp * k_hp - 1.0) / a0_hp, (1.0 - k_hp / q_hp + k_hp * k_hp) / a0_hp], dtype=np.float64)
    sos2 = signal.tf2sos(b_hp, a_hp)
    _K_WEIGHT_SOS_CACHE[sample_rate] = (sos1, sos2)
    return sos1, sos2


def compute_bark_profile(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> tuple[list[float], int, float, float]:
    """Compute normalized 24-band Bark profile, peak band index, mud (200-500 Hz) & harshness (2.5-6 kHz) ratios."""
    n = len(wave)
    if n < 16:
        return [0.0] * 24, 0, 0.0, 0.0
    spec = np.abs(np.fft.rfft(wave))
    power = spec * spec
    total_pow = float(np.sum(power)) + 1e-12
    freqs = np.fft.rfftfreq(n, 1.0 / float(sample_rate))

    mud_ratio = float(np.sum(power[(freqs >= 200.0) & (freqs <= 500.0)]) / total_pow)
    harsh_ratio = float(np.sum(power[(freqs >= 2500.0) & (freqs <= 6000.0)]) / total_pow)

    bins = np.zeros(24, dtype=np.float64)
    edges = BARK_EDGES_HZ
    idx_edges = np.searchsorted(freqs, edges)
    for b in range(24):
        lo_i = int(idx_edges[b])
        hi_i = int(idx_edges[b + 1])
        if hi_i > lo_i:
            bins[b] = float(np.sum(power[lo_i:hi_i]))
    b_sum = float(np.sum(bins))
    if b_sum > 1e-12:
        bins /= b_sum
    peak_band = int(np.argmax(bins))
    return [round(float(x), 4) for x in bins], peak_band, round(mud_ratio, 3), round(harsh_ratio, 3)


def compute_loudness_dynamics(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> dict[str, float]:
    """Compute EBU R128 Momentary (400 ms), Short-Term (3.0 s) max LUFS, and Loudness Range (LRA in LU)."""
    n = len(wave)
    if n < 32 or float(np.max(np.abs(wave))) < 1e-7:
        return {
            "momentary_lufs_max": -70.0,
            "short_term_lufs_max": -70.0,
            "lra_lu": 0.0,
        }
    sos1, sos2 = _get_k_weighting_filters(sample_rate)
    kw = signal.sosfilt(sos2, signal.sosfilt(sos1, np.asarray(wave, dtype=np.float64)))
    sq = kw * kw
    csum = np.empty(n + 1, dtype=np.float64)
    csum[0] = 0.0
    np.cumsum(sq, out=csum[1:])

    def _window_lufs_series(win_secs: float, hop_secs: float) -> np.ndarray:
        win = min(n, max(16, int(round(win_secs * sample_rate))))
        hop = max(16, int(round(hop_secs * sample_rate)))
        starts = np.arange(0, max(1, n - win + 1), hop)
        means = (csum[starts + win] - csum[starts]) / float(win)
        return -0.691 + 10.0 * np.log10(np.maximum(means, 1e-12))

    mom_series = _window_lufs_series(0.400, 0.100)
    st_series = _window_lufs_series(3.000, 0.200)
    mom_max = float(np.max(mom_series))
    st_max = float(np.max(st_series))

    # Gated Loudness Range (10th to 95th percentile of active blocks >= -60 LUFS & >= max - 20 dB)
    lra_src = st_series if n >= int(3.4 * sample_rate) else _window_lufs_series(0.200, 0.050)
    peak_blk = float(np.max(lra_src))
    gated = lra_src[(lra_src >= -60.0) & (lra_src >= peak_blk - 20.0)]
    if len(gated) >= 2:
        lra = float(np.percentile(gated, 95) - np.percentile(gated, 10))
    else:
        lra = 0.0
    return {
        "momentary_lufs_max": round(float(max(-70.0, mom_max)), 2),
        "short_term_lufs_max": round(float(max(-70.0, st_max)), 2),
        "lra_lu": round(float(max(0.0, lra)), 2),
    }


def _augment_psychoacoustic_metrics(wave: np.ndarray, m: dict[str, Any], sample_rate: int = SAMPLE_RATE) -> dict[str, Any]:
    """Attach Bark 24-band profile, mud/harshness ratios, and EBU R128 dynamics to a metrics dict."""
    bark_profile, bark_peak, mud_ratio, harsh_ratio = compute_bark_profile(wave, sample_rate)
    dyn = compute_loudness_dynamics(wave, sample_rate)
    m["mud_ratio"] = mud_ratio
    m["harshness_ratio"] = harsh_ratio
    m["bark_peak_band"] = bark_peak
    m["bark_peak_hz"] = BARK_CENTERS_HZ[bark_peak]
    m["bark_profile"] = bark_profile
    m["momentary_lufs_max"] = dyn["momentary_lufs_max"]
    m["short_term_lufs_max"] = dyn["short_term_lufs_max"]
    m["lra_lu"] = dyn["lra_lu"]
    return m


def compute_timbral_diversity(file_metrics: list[dict[str, Any]], max_corr: float) -> float:
    """Compute a [0, 1] timbral micro-diversity index across variations of an event."""
    if len(file_metrics) < 2:
        return 0.0
    barks = [np.asarray(fm.get("bark_profile", []), dtype=np.float64) for fm in file_metrics]
    bark_dists: list[float] = []
    for i in range(len(barks)):
        for j in range(i + 1, len(barks)):
            if len(barks[i]) == 24 and len(barks[j]) == 24:
                na = float(np.linalg.norm(barks[i]))
                nb = float(np.linalg.norm(barks[j]))
                if na > 1e-9 and nb > 1e-9:
                    cos_sim = float(np.clip(np.dot(barks[i], barks[j]) / (na * nb), 0.0, 1.0))
                    bark_dists.append(1.0 - cos_sim)
    mean_bark_dist = float(np.mean(bark_dists)) if bark_dists else 0.0

    centroids = np.array([float(fm.get("spectral_centroid_hz", 0.0)) for fm in file_metrics], dtype=np.float64)
    cent_cv = float(np.std(centroids) / (np.mean(centroids) + 1.0))
    flats = np.array([float(fm.get("spectral_flatness", 0.0)) for fm in file_metrics], dtype=np.float64)
    flat_spread = float(np.std(flats))

    decorr = float(np.clip(1.0 - max_corr, 0.0, 1.0))
    score = 0.45 * decorr + 0.30 * min(1.0, mean_bark_dist * 4.5) + 0.15 * min(1.0, cent_cv * 4.0) + 0.10 * min(1.0, flat_spread * 8.0)
    return round(float(np.clip(score, 0.0, 1.0)), 4)


MASKING_BED_IDS: tuple[str, ...] = (
    "biome_caverns_loop",
    "biome_sanctum_loop",
    "biome_forest_loop",
    "fungal_tide_ambience",
    "purification_pylon_hum",
    "mycelial_rift_hum",
    "sovereign_nexus_hum",
    "boss_combat_tension_bed",
    "sinkhole_updraft_loop",
)
MASKING_FOREGROUND_GROUPS: dict[str, tuple[str, ...]] = {
    "alerts_ui": (
        "hive_trait_adapt",
        "hive_synergy_unlock",
        "hive_threat_milestone",
        "colony_raid_horn",
        "ui_codex_tab",
        "ui_action_confirm",
        "station_upkeep_low",
    ),
    "boss_tactical": (
        "boss_telegraph_lock",
        "boss_stagger_break",
        "boss_weakpoint_crit",
        "boss_carapace_ricochet",
        "boss_phase_shift",
        "hive_core_resonance",
        "apex_matriarch_wail",
    ),
    "entity_vocals": (
        "spore_glider_ambient",
        "root_keeper_slam",
        "brute_wall_stun",
        "pyro_shroom_defuse",
        "stalker_hiss",
        "acid_spitter_spit",
    ),
}


def audit_spectral_masking(manifest_entries: list[dict[str, Any]]) -> dict[str, Any]:
    """Audit Bark 24-band spectral overlap and loudness headroom between concurrent beds and priority cues."""
    by_id = {str(e["sound_id"]): e for e in manifest_entries if isinstance(e, dict) and "sound_id" in e}
    beds = [by_id[bid] for bid in MASKING_BED_IDS if bid in by_id]
    fg_entries: list[tuple[str, dict[str, Any]]] = []
    for layer_name, ids in MASKING_FOREGROUND_GROUPS.items():
        for fid in ids:
            if fid in by_id:
                fg_entries.append((layer_name, by_id[fid]))

    pairs: list[dict[str, Any]] = []
    high_risk: list[dict[str, Any]] = []

    for bed in beds:
        b_m = bed.get("metrics", {})
        b_bark = np.asarray(b_m.get("bark_profile", []), dtype=np.float64)
        b_norm = float(np.linalg.norm(b_bark))
        b_lufs = float(b_m.get("integrated_lufs", -24.0))
        if len(b_bark) != 24 or b_norm < 1e-9:
            continue
        for layer_name, fg in fg_entries:
            f_m = fg.get("metrics", {})
            f_bark = np.asarray(f_m.get("bark_profile", []), dtype=np.float64)
            f_norm = float(np.linalg.norm(f_bark))
            f_lufs = float(f_m.get("integrated_lufs", -14.0))
            if len(f_bark) != 24 or f_norm < 1e-9:
                continue
            overlap = float(np.clip(np.dot(b_bark, f_bark) / (b_norm * f_norm), 0.0, 1.0))
            headroom_db = f_lufs - b_lufs
            # Foreground cues sit above beds in-game (+ducking 0.35x); risk rises when overlap is high AND headroom < 0
            headroom_penalty = float(np.clip(1.0 - ((headroom_db + 2.0) / 14.0), 0.12, 1.0))
            risk = overlap * headroom_penalty
            row = {
                "bed_id": str(bed["sound_id"]),
                "foreground_id": str(fg["sound_id"]),
                "layer": layer_name,
                "bark_overlap": round(overlap, 4),
                "headroom_db": round(headroom_db, 2),
                "masking_risk": round(risk, 4),
            }
            pairs.append(row)
            if risk > 0.72 and headroom_db < 1.5:
                high_risk.append(row)

    pairs.sort(key=lambda r: float(r["masking_risk"]), reverse=True)

    def _avg_bark(entries: list[dict[str, Any]]) -> list[float]:
        vecs = [
            np.asarray(e.get("metrics", {}).get("bark_profile", []), dtype=np.float64)
            for e in entries
            if len(e.get("metrics", {}).get("bark_profile", [])) == 24
        ]
        if not vecs:
            return [0.0] * 24
        mean_v = np.mean(vecs, axis=0)
        return [round(float(x), 4) for x in mean_v]

    layer_barks: dict[str, list[float]] = {"ambient_beds": _avg_bark(beds)}
    for layer_name in MASKING_FOREGROUND_GROUPS:
        layer_barks[layer_name] = _avg_bark([fg for ln, fg in fg_entries if ln == layer_name])

    return {
        "ok": len(high_risk) == 0,
        "beds_checked": len(beds),
        "foregrounds_checked": len(fg_entries),
        "pairs_evaluated": len(pairs),
        "mean_bark_overlap": round(float(np.mean([p["bark_overlap"] for p in pairs])), 4) if pairs else 0.0,
        "mean_headroom_db": round(float(np.mean([p["headroom_db"] for p in pairs])), 2) if pairs else 0.0,
        "max_masking_risk": round(float(pairs[0]["masking_risk"]), 4) if pairs else 0.0,
        "high_risk_count": len(high_risk),
        "high_risk_pairs": high_risk,
        "top_overlap_pairs": pairs[:12],
        "bark_centers_hz": list(BARK_CENTERS_HZ),
        "layer_bark_averages": layer_barks,
    }

try:
    from .cuda_dsp import (
        gpu_analyze_event_variations as _gpu_analyze_event_variations,
        gpu_max_pairwise_correlation as _gpu_max_pairwise_correlation,
    )
except ImportError:
    try:
        from cuda_dsp import (
            gpu_analyze_event_variations as _gpu_analyze_event_variations,
            gpu_max_pairwise_correlation as _gpu_max_pairwise_correlation,
        )
    except ImportError:
        from sound_studio.cuda_dsp import (
            gpu_analyze_event_variations as _gpu_analyze_event_variations,
            gpu_max_pairwise_correlation as _gpu_max_pairwise_correlation,
        )


def decode_ogg_to_float(ogg_path: Path, ffmpeg_exe: str | None = None) -> np.ndarray:
    """Decode an .ogg file back to mono float64 samples via ffmpeg."""
    ffmpeg = ffmpeg_exe or find_ffmpeg()
    cmd = [
        ffmpeg,
        "-v",
        "error",
        "-i",
        str(ogg_path),
        "-f",
        "s16le",
        "-ac",
        "1",
        "-ar",
        str(SAMPLE_RATE),
        "pipe:1",
    ]
    proc = subprocess.run(cmd, capture_output=True, check=False)
    if proc.returncode != 0:
        err = proc.stderr.decode("utf-8", errors="replace")
        raise RuntimeError(f"ffmpeg failed decoding {ogg_path}: {err}")
    pcm = np.frombuffer(proc.stdout, dtype="<i2")
    return pcm.astype(np.float64) / 32768.0


def measure_loop_seam_delta(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> float:
    """Measure circular seam discontinuity: boundary sample step + head/tail 50ms RMS mismatch."""
    n = len(wave)
    if n < 16 or float(np.max(np.abs(wave))) < 1e-8:
        return 0.0
    sample_jump = abs(float(wave[0]) - float(wave[-1]))
    win = min(n // 4, max(8, int(sample_rate * 0.050)))
    rms_head = float(np.sqrt(np.mean(wave[:win] * wave[:win])))
    rms_tail = float(np.sqrt(np.mean(wave[-win:] * wave[-win:])))
    return float(sample_jump + abs(rms_head - rms_tail))


def analyze_waveform(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> dict[str, Any]:
    """Compute acoustic loudness, peak ceiling, 4x true-peak, loop seam, Bark 24-band & R128 dynamics."""
    n = len(wave)
    if n == 0:
        return {
            "rms_dbfs": -96.0,
            "active_rms_dbfs": -96.0,
            "integrated_lufs": -70.0,
            "momentary_lufs_max": -70.0,
            "short_term_lufs_max": -70.0,
            "lra_lu": 0.0,
            "peak_dbfs": -96.0,
            "true_peak_dbfs": -96.0,
            "crest_db": 0.0,
            "dc_offset": 0.0,
            "edge_start_abs": 0.0,
            "edge_end_abs": 0.0,
            "loop_seam_delta": 0.0,
            "spectral_centroid_hz": 0.0,
            "spectral_flatness": 0.0,
            "zero_crossing_rate": 0.0,
            "low_band_ratio": 0.0,
            "high_band_ratio": 0.0,
            "mud_ratio": 0.0,
            "harshness_ratio": 0.0,
            "bark_peak_band": 0,
            "bark_peak_hz": BARK_CENTERS_HZ[0],
            "bark_profile": [0.0] * 24,
            "duration_secs": 0.0,
        }
    abs_w = np.abs(wave)
    peak = float(np.max(abs_w))
    full_rms = float(np.sqrt(np.mean(wave * wave)))
    act_rms = active_rms(wave)

    rms_db = 20.0 * np.log10(max(full_rms, 1e-6))
    act_db = 20.0 * np.log10(max(act_rms, 1e-6))
    peak_db = 20.0 * np.log10(max(peak, 1e-6))
    crest_db = peak_db - act_db

    spec = np.abs(np.fft.rfft(wave))
    power = spec * spec
    total_pow = float(np.sum(power)) + 1e-12
    freqs = np.fft.rfftfreq(n, 1.0 / sample_rate)
    centroid = float(np.sum(freqs * power) / total_pow)
    low_ratio = float(np.sum(power[freqs < 250.0]) / total_pow)
    high_ratio = float(np.sum(power[freqs > 4000.0]) / total_pow)

    band = power[(freqs >= 80.0) & (freqs <= 6000.0)]
    if len(band) > 0:
        arith_mean = float(np.mean(band)) + 1e-18
        geom_mean = float(np.exp(np.mean(np.log(band + 1e-18))))
        flatness = float(np.clip(geom_mean / arith_mean, 0.0, 1.0))
    else:
        flatness = 0.0
    zcr = float(np.mean(wave[:-1] * wave[1:] < 0.0)) if n > 1 else 0.0

    edge_s = float(np.max(abs_w[: min(4, n)]))
    edge_e = float(np.max(abs_w[-min(4, n) :]))

    from scripts.sound_studio.mastering import measure_integrated_lufs, measure_true_peak_dbfs

    lufs = measure_integrated_lufs(wave, sample_rate)
    true_peak_db = measure_true_peak_dbfs(wave)
    seam_delta = measure_loop_seam_delta(wave, sample_rate)

    out: dict[str, Any] = {
        "rms_dbfs": round(float(rms_db), 2),
        "active_rms_dbfs": round(float(act_db), 2),
        "integrated_lufs": round(float(lufs), 2),
        "peak_dbfs": round(float(peak_db), 2),
        "true_peak_dbfs": round(float(true_peak_db), 2),
        "crest_db": round(float(crest_db), 2),
        "dc_offset": round(float(np.mean(wave)), 5),
        "edge_start_abs": round(edge_s, 5),
        "edge_end_abs": round(edge_e, 5),
        "loop_seam_delta": round(float(seam_delta), 5),
        "spectral_centroid_hz": round(centroid, 1),
        "spectral_flatness": round(flatness, 4),
        "zero_crossing_rate": round(zcr, 4),
        "low_band_ratio": round(low_ratio, 3),
        "high_band_ratio": round(high_ratio, 3),
        "duration_secs": round(n / float(sample_rate), 3),
    }
    return _augment_psychoacoustic_metrics(wave, out, sample_rate)


def _normalized_lag_correlation(a: np.ndarray, b: np.ndarray, max_lag_samples: int = int(SAMPLE_RATE * 0.020)) -> float:
    """Compute maximum absolute normalized cross-correlation over lags [-max_lag, +max_lag]."""
    if len(a) == 0 or len(b) == 0:
        return 0.0
    a0 = a - np.mean(a)
    b0 = b - np.mean(b)
    norm = float(np.sqrt(np.sum(a0 * a0) * np.sum(b0 * b0)))
    if norm < 1e-9:
        return 0.0
    n_len = max(len(a0), len(b0))
    n_fft = 1 << int(np.ceil(np.log2(n_len * 2)))
    fa = np.fft.rfft(a0, n=n_fft)
    fb = np.fft.rfft(b0, n=n_fft)
    xcorr = np.fft.irfft(fa * np.conj(fb), n=n_fft)
    # Circular shift so lag 0 is index max_lag_samples
    lag_window = np.concatenate([xcorr[-max_lag_samples:], xcorr[: max_lag_samples + 1]])
    return float(np.max(np.abs(lag_window)) / norm)


def max_pairwise_correlation(waves: list[np.ndarray]) -> float:
    """Maximum pairwise lag-tolerant normalized correlation across variations of an event."""
    if len(waves) < 2:
        return 0.0
    max_lag = int(SAMPLE_RATE * 0.020)
    gpu_corr = _gpu_max_pairwise_correlation(waves, max_lag_samples=max_lag)
    if gpu_corr is not None:
        return gpu_corr
    worst = 0.0
    for i in range(len(waves)):
        for j in range(i + 1, len(waves)):
            corr = _normalized_lag_correlation(waves[i], waves[j], max_lag_samples=max_lag)
            if corr > worst:
                worst = corr
    return round(worst, 4)


def analyze_event_variations(
    waves: list[np.ndarray],
    sample_rate: int = SAMPLE_RATE,
) -> tuple[list[dict[str, Any]], float]:
    """Compute acoustic & psychoacoustic QA v4 metrics for all takes of an event + pairwise correlation."""
    max_lag = int(sample_rate * 0.020)
    gpu_res = _gpu_analyze_event_variations(waves, sample_rate=sample_rate, max_lag_samples=max_lag)
    if gpu_res is not None:
        metrics_list, max_corr = gpu_res
        from scripts.sound_studio.mastering import measure_integrated_lufs, measure_true_peak_dbfs

        out_list: list[dict[str, Any]] = []
        for w, m in zip(waves, metrics_list):
            m_any: dict[str, Any] = dict(m)
            m_any["integrated_lufs"] = round(float(measure_integrated_lufs(w, sample_rate)), 2)
            m_any["true_peak_dbfs"] = round(float(measure_true_peak_dbfs(w)), 2)
            m_any["loop_seam_delta"] = round(float(measure_loop_seam_delta(w, sample_rate)), 5)
            _augment_psychoacoustic_metrics(w, m_any, sample_rate)
            out_list.append(m_any)
        return out_list, max_corr

    file_metrics = [analyze_waveform(w, sample_rate) for w in waves]
    max_corr = max_pairwise_correlation(waves)
    return file_metrics, max_corr


CREST_DB_BOUNDS: dict[str, tuple[float, float]] = {
    "ambient": (4.5, 19.0),
    "boss_cinematic": (4.5, 19.0),
    "footstep": (4.5, 20.0),
    "block_light": (4.5, 20.0),
    "block_heavy": (4.5, 20.0),
    "combat": (4.5, 20.0),
    "death": (4.5, 20.0),
    "ui_item": (4.5, 20.0),
}
MAX_LOOP_SEAM_DELTA = 0.025


def evaluate_quality(
    metrics: dict[str, float],
    loudness_group: str,
    *,
    is_loop: bool = False,
) -> list[str]:
    """Return any acoustic quality violations for a generated sound."""
    issues: list[str] = []
    if metrics["peak_dbfs"] > -0.25:
        issues.append(f"peak_dbfs too high ({metrics['peak_dbfs']} > -0.25)")
    tp_db = metrics.get("true_peak_dbfs")
    if tp_db is not None and float(tp_db) > -0.25:
        issues.append(f"true_peak_dbfs too high ({tp_db} > -0.25)")
    if abs(metrics["dc_offset"]) > 0.01:
        issues.append(f"dc_offset too large ({metrics['dc_offset']})")
    if metrics["edge_start_abs"] > 0.02:
        issues.append(f"edge_start_abs click risk ({metrics['edge_start_abs']})")
    if metrics["edge_end_abs"] > 0.02:
        issues.append(f"edge_end_abs click risk ({metrics['edge_end_abs']})")
    if is_loop:
        seam = float(metrics.get("loop_seam_delta", 0.0))
        if seam > MAX_LOOP_SEAM_DELTA:
            issues.append(f"loop_seam_delta too high ({seam} > {MAX_LOOP_SEAM_DELTA})")
    crest = float(metrics.get("crest_db", 10.0))
    lo_crest, hi_crest = CREST_DB_BOUNDS.get(loudness_group, (4.5, 20.0))
    if crest < lo_crest or crest > hi_crest:
        issues.append(f"crest_db {crest} outside [{lo_crest}, {hi_crest}]")
    target = LOUDNESS_TARGETS_DBFS.get(loudness_group)
    if target is not None:
        delta = metrics["active_rms_dbfs"] - target
        if abs(delta) > 3.0:
            issues.append(f"active_rms_dbfs {metrics['active_rms_dbfs']} outside {target} +-3.0 dB")
    return issues


def save_acoustic_baseline(
    manifest: dict[str, Any],
    quality_report: dict[str, Any],
    baseline_path: Path,
) -> dict[str, Any]:
    """Persist compact acoustic reference snapshot for regression diffing."""
    events_snap: dict[str, dict[str, Any]] = {}
    corrs = quality_report.get("event_correlations", {})
    for entry in manifest.get("sounds", []):
        sid = entry["sound_id"]
        m = entry["metrics"]
        events_snap[sid] = {
            "loudness_group": entry["loudness_group"],
            "ir_space": entry.get("ir_space", ""),
            "stem_id": entry.get("stem_id", ""),
            "active_rms_dbfs": m.get("active_rms_dbfs"),
            "integrated_lufs": m.get("integrated_lufs"),
            "short_term_lufs_max": m.get("short_term_lufs_max"),
            "lra_lu": m.get("lra_lu"),
            "true_peak_dbfs": m.get("true_peak_dbfs", m.get("peak_dbfs")),
            "crest_db": m.get("crest_db"),
            "loop_seam_delta": m.get("loop_seam_delta", 0.0),
            "spectral_centroid_hz": m.get("spectral_centroid_hz"),
            "mud_ratio": m.get("mud_ratio"),
            "harshness_ratio": m.get("harshness_ratio"),
            "bark_peak_band": m.get("bark_peak_band"),
            "timbral_diversity": entry.get("timbral_diversity", 0.0),
            "max_variant_corr": corrs.get(sid, entry.get("max_variant_corr", 0.0)),
        }
    baseline = {
        "schema_version": 4,
        "total_events": len(events_snap),
        "total_ogg_files": manifest.get("ogg_count", 0),
        "worst_correlation": quality_report.get("worst_correlation", 0.0),
        "mean_timbral_diversity": quality_report.get("mean_timbral_diversity", 0.0),
        "events": events_snap,
    }
    baseline_path.parent.mkdir(parents=True, exist_ok=True)
    baseline_path.write_text(json.dumps(baseline, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return baseline


def load_acoustic_baseline(baseline_path: Path) -> dict[str, Any] | None:
    if not baseline_path.is_file():
        return None
    try:
        data = json.loads(baseline_path.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else None
    except (OSError, json.JSONDecodeError):
        return None


def compare_against_baseline(
    manifest: dict[str, Any],
    quality_report: dict[str, Any],
    baseline: dict[str, Any],
) -> dict[str, Any]:
    """Compare current manifest + quality_report against saved acoustic baseline."""
    base_events: dict[str, dict[str, Any]] = baseline.get("events", {}) or {}
    cur_events = {e["sound_id"]: e for e in manifest.get("sounds", [])}
    corrs = quality_report.get("event_correlations", {})

    new_events = sorted(set(cur_events) - set(base_events))
    removed_events = sorted(set(base_events) - set(cur_events))
    drifted_events: list[dict[str, Any]] = []
    event_deltas: dict[str, dict[str, float]] = {}

    for sid in sorted(set(cur_events) & set(base_events)):
        cur_m = cur_events[sid]["metrics"]
        old_m = base_events[sid]
        d_rms = round(float(cur_m.get("active_rms_dbfs", 0.0)) - float(old_m.get("active_rms_dbfs", 0.0)), 2)
        d_lufs = round(float(cur_m.get("integrated_lufs", 0.0)) - float(old_m.get("integrated_lufs", 0.0)), 2)
        cur_tp = float(cur_m.get("true_peak_dbfs", cur_m.get("peak_dbfs", 0.0)))
        old_tp = float(old_m.get("true_peak_dbfs", 0.0))
        d_tp = round(cur_tp - old_tp, 2)
        cur_c = float(cur_m.get("spectral_centroid_hz", 0.0))
        old_c = float(old_m.get("spectral_centroid_hz", 0.0))
        d_cent = round(cur_c - old_c, 1)
        d_corr = round(float(corrs.get(sid, 0.0)) - float(old_m.get("max_variant_corr", 0.0)), 4)

        event_deltas[sid] = {
            "active_rms_delta_db": d_rms,
            "lufs_delta": d_lufs,
            "true_peak_delta_db": d_tp,
            "centroid_delta_hz": d_cent,
            "corr_delta": d_corr,
        }
        reasons: list[str] = []
        if abs(d_rms) > 1.5:
            reasons.append(f"active_rms {d_rms:+.2f} dB")
        if abs(d_lufs) > 1.8:
            reasons.append(f"lufs {d_lufs:+.2f}")
        if abs(d_tp) > 1.5:
            reasons.append(f"true_peak {d_tp:+.2f} dB")
        cent_lim = max(140.0, abs(old_c) * 0.25)
        if abs(d_cent) > cent_lim:
            reasons.append(f"centroid {d_cent:+.1f} Hz")
        if abs(d_corr) > 0.18:
            reasons.append(f"corr {d_corr:+.3f}")
        if reasons:
            drifted_events.append({"sound_id": sid, "reasons": reasons, "deltas": event_deltas[sid]})

    worst_corr_delta = round(
        float(quality_report.get("worst_correlation", 0.0)) - float(baseline.get("worst_correlation", 0.0)),
        4,
    )
    ok = (len(drifted_events) == 0) and (len(removed_events) == 0) and (abs(worst_corr_delta) <= 0.15)
    return {
        "ok": ok,
        "checked_events": len(event_deltas),
        "new_events": new_events,
        "removed_events": removed_events,
        "drifted_events": drifted_events,
        "worst_correlation_delta": worst_corr_delta,
        "event_deltas": event_deltas,
    }


def build_audition_html(
    manifest: dict[str, Any],
    quality_report: dict[str, Any],
    sounds_dir: Path,
    out_path: Path,
    *,
    baseline_diff: dict[str, Any] | None = None,
) -> None:
    """Write a self-contained local dark-mode HTML audition console v4 (Bark masking, OpenAL EFX/occlusion sim, QA v4)."""
    rel_sounds = Path(os.path.relpath(sounds_dir, out_path.parent)).as_posix()
    by_event_corr = quality_report.get("event_correlations", {})
    event_deltas = (baseline_diff or {}).get("event_deltas", {})

    from scripts.sound_studio.boss_synth import BOSS_CINEMATIC_IDS
    from scripts.sound_studio.mastering import SEAMLESS_LOOP_IDS

    rows: list[dict[str, Any]] = []
    for entry in manifest.get("sounds", []):
        sid = entry["sound_id"]
        rows.append(
            {
                "sound_id": sid,
                "category": entry["category"],
                "species": entry["species"],
                "loudness_group": entry["loudness_group"],
                "ir_space": entry.get("ir_space", "fungal_chamber"),
                "ai_hybrid": bool(entry.get("ai_hybrid", False)),
                "stem_id": entry.get("stem_id", ""),
                "is_loop": sid in SEAMLESS_LOOP_IDS,
                "attenuation_distance": entry["attenuation_distance"],
                "en_subtitle": entry["en_subtitle"],
                "es_subtitle": entry["es_subtitle"],
                "files": [f"{rel_sounds}/{fname}" for fname in entry["files"]],
                "metrics": entry["metrics"],
                "max_corr": by_event_corr.get(sid, 0.0),
                "timbral_diversity": entry.get("timbral_diversity", 0.0),
                "baseline_delta": event_deltas.get(sid),
            }
        )

    biome_ambient_ids = sorted(
        e["sound_id"] for e in rows if e["category"] == "biome_ambient" or e["sound_id"].startswith("hive_t") or e["sound_id"] == "hive_synergy_unlock"
    )
    func_count = sum(1 for e in rows if e["category"] == "block_functional")
    boss_count = len(BOSS_CINEMATIC_IDS)
    biome_count = len(biome_ambient_ids)
    entity_count = sum(1 for e in rows if e["category"] == "entity")
    item_proj_count = sum(1 for e in rows if e["category"] in ("item", "projectile"))
    mix_bed_ids = [
        "biome_caverns_loop",
        "purification_pylon_hum",
        "sovereign_nexus_hum",
        "boss_combat_tension_bed",
        "sinkhole_updraft_loop",
    ]
    mix_bed_sources = {
        bid: next((e["files"][0] for e in rows if e["sound_id"] == bid and e["files"]), "")
        for bid in mix_bed_ids
    }
    masking_data = quality_report.get("psychoacoustic_masking") or audit_spectral_masking(manifest.get("sounds", []))
    stem_qc = quality_report.get("stem_qc_summary") or {}

    payload_json = json.dumps(
        {
            "sample_rate": manifest.get("sample_rate", SAMPLE_RATE),
            "total_events": len(rows),
            "total_ogg_files": manifest.get("ogg_count", 0),
            "total_kb": round(manifest.get("total_bytes", 0) / 1024.0, 1),
            "quality_ok": quality_report.get("ok", False),
            "max_true_peak_dbfs": quality_report.get("max_true_peak_dbfs"),
            "loop_seam_ok": quality_report.get("loop_seam_ok", True),
            "mean_timbral_diversity": quality_report.get("mean_timbral_diversity", 0.0),
            "baseline_ok": (baseline_diff or {}).get("ok"),
            "masking": masking_data,
            "stem_qc": stem_qc,
            "boss_ids": sorted(BOSS_CINEMATIC_IDS),
            "biome_ambient_ids": biome_ambient_ids,
            "mix_bed_sources": mix_bed_sources,
            "events": rows,
        },
        ensure_ascii=False,
    ).replace("</", "<\\/")

    html_doc = f"""<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Mycelial Mind — Sound Studio Audition Console v4 ({len(rows)} eventos)</title>
  <style>
    :root {{
      --bg: #0c0d12;
      --panel: #151722;
      --panel-2: #1c1f2e;
      --border: #2a2f45;
      --text: #e6e9f2;
      --muted: #98a2b8;
      --accent: #a855f7;
      --tech: #22d3ee;
      --good: #34d399;
      --warn: #fbbf24;
      --crimson: #f43f5e;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0; font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
      background: var(--bg); color: var(--text); padding: 20px;
    }}
    header {{
      display: flex; flex-wrap: wrap; gap: 16px; align-items: center; justify-content: space-between;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 16px 20px; margin-bottom: 14px;
    }}
    h1 {{ margin: 0; font-size: 1.25rem; letter-spacing: 0.02em; }}
    .stats {{ display: flex; flex-wrap: wrap; gap: 10px; font-size: 0.85rem; color: var(--muted); }}
    .badge {{
      padding: 4px 10px; border-radius: 999px; background: var(--panel-2); border: 1px solid var(--border); color: var(--text);
    }}
    .badge.ok {{ border-color: var(--good); color: var(--good); }}
    .badge.warn {{ border-color: var(--warn); color: var(--warn); }}
    .viz-deck {{
      display: grid; grid-template-columns: 1fr 1fr 1fr 320px; gap: 12px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; margin-bottom: 14px;
    }}
    @media (max-width: 1200px) {{ .viz-deck {{ grid-template-columns: 1fr 1fr; }} }}
    @media (max-width: 760px) {{ .viz-deck {{ grid-template-columns: 1fr; }} }}
    .viz-box {{
      background: #090a0f; border: 1px solid var(--border); border-radius: 9px; padding: 8px 10px; display: flex; flex-direction: column; gap: 4px;
    }}
    .viz-title {{ font-size: 0.74rem; color: var(--muted); display: flex; justify-content: space-between; font-family: ui-monospace, Consolas, monospace; }}
    canvas {{ width: 100%; height: 84px; display: block; border-radius: 6px; background: #07080c; }}
    .ab-box {{
      background: var(--panel-2); border: 1px solid var(--border); border-radius: 9px; padding: 10px 12px;
      display: flex; flex-direction: column; justify-content: space-between; gap: 6px; font-size: 0.82rem;
    }}
    .ab-slot {{ display: flex; align-items: center; justify-content: space-between; gap: 6px; }}
    .ab-name {{ font-family: ui-monospace, Consolas, monospace; font-size: 0.76rem; color: #f3e8ff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 155px; }}
    .mix-deck {{
      display: grid; grid-template-columns: repeat(auto-fit, minmax(195px, 1fr)); gap: 10px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; margin-bottom: 14px; align-items: center;
    }}
    .mix-item {{ display: flex; flex-direction: column; gap: 4px; font-size: 0.78rem; color: var(--muted); }}
    .mix-item label {{ display: flex; justify-content: space-between; font-family: ui-monospace, Consolas, monospace; color: var(--text); }}
    .qa-v4-deck {{
      display: grid; grid-template-columns: 1.2fr 1fr; gap: 12px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; margin-bottom: 14px;
    }}
    @media (max-width: 1000px) {{ .qa-v4-deck {{ grid-template-columns: 1fr; }} }}
    .mask-table {{ width: 100%; border-collapse: collapse; font-size: 0.76rem; font-family: ui-monospace, Consolas, monospace; }}
    .mask-table th, .mask-table td {{ padding: 4px 6px; border-bottom: 1px solid var(--border); }}
    .controls {{
      display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 16px;
    }}
    input, select {{
      width: 100%; background: var(--panel-2); color: var(--text); border: 1px solid var(--border);
      border-radius: 8px; padding: 8px 10px; font-size: 0.9rem;
    }}
    input[type="range"] {{ padding: 0; accent-color: var(--tech); }}
    .rank-bar {{ display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }}
    .btn {{
      background: var(--panel-2); color: var(--text); border: 1px solid var(--border);
      border-radius: 7px; padding: 6px 10px; cursor: pointer; font-size: 0.80rem; transition: 0.15s;
    }}
    .btn:hover {{ border-color: var(--accent); }}
    .btn.active {{ background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; }}
    .btn.play {{ background: #261b3a; border-color: var(--accent); }}
    .btn.tech {{ background: #122936; border-color: var(--tech); }}
    .btn.seam {{ background: #142b23; border-color: var(--good); color: #d1fae5; }}
    .btn.ab {{ padding: 4px 7px; font-size: 0.74rem; color: var(--muted); }}
    table.main {{ width: 100%; border-collapse: collapse; background: var(--panel); border-radius: 12px; overflow: hidden; border: 1px solid var(--border); }}
    table.main th, table.main td {{ padding: 10px 12px; border-bottom: 1px solid var(--border); text-align: left; vertical-align: middle; font-size: 0.86rem; }}
    table.main th {{ background: var(--panel-2); color: var(--muted); font-weight: 600; }}
    table.main tr:hover td {{ background: rgba(168, 85, 247, 0.06); }}
    .id {{ font-family: ui-monospace, Consolas, monospace; font-weight: 600; color: #f3e8ff; }}
    .sub {{ color: var(--muted); font-size: 0.8rem; margin-top: 2px; }}
    .takes {{ display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }}
    .metrics {{ font-family: ui-monospace, Consolas, monospace; font-size: 0.75rem; color: var(--muted); line-height: 1.48; }}
    .pill {{ display: inline-block; padding: 2px 7px; border-radius: 6px; background: var(--panel-2); border: 1px solid var(--border); font-size: 0.75rem; }}
  </style>
</head>
<body>
  <header>
    <div>
      <h1>{html.escape("Mycelial Mind — Consola Local de Audio v4 (DSP v4 + 3-Band HPSS + OpenAL EFX Sim + Bark QA)")}</h1>
      <div class="sub">Audita los 5 motores DSP v4, 66 stems RTX 4060, EBU R128 LRA/ST-LUFS, enmascaramiento espectral Bark 24-bandas y simulador OpenAL EFX/Oclusión.</div>
    </div>
    <div class="stats" id="stats"></div>
  </header>

  <section class="viz-deck">
    <div class="viz-box">
      <div class="viz-title"><span>OSCILOSCOPIO TEMPORAL</span><span id="nowPlaying">En espera...</span></div>
      <canvas id="waveCanvas" width="380" height="84"></canvas>
    </div>
    <div class="viz-box">
      <div class="viz-title"><span>ANALIZADOR ESPECTRAL FFT</span><span id="liveRateLabel">Pitch: 1.00x · Vol: 100%</span></div>
      <canvas id="fftCanvas" width="380" height="84"></canvas>
    </div>
    <div class="viz-box">
      <div class="viz-title"><span>ESPECTROGRAMA WATERFALL</span><span id="seamStatus">Seam Tester: listo</span></div>
      <canvas id="specCanvas" width="380" height="84"></canvas>
    </div>
    <div class="ab-box">
      <div style="font-weight:600;color:var(--tech);">Comparador A / B &amp; Loop Seam</div>
      <div class="ab-slot">
        <span>Ref A: <span class="ab-name" id="slotAName">(ninguno)</span></span>
        <button class="btn play" id="playABtn">▶ A</button>
      </div>
      <div class="ab-slot">
        <span>Ref B: <span class="ab-name" id="slotBName">(ninguno)</span></span>
        <button class="btn tech" id="playBBtn">▶ B</button>
      </div>
      <div class="ab-slot">
        <button class="btn seam" id="stopLoopBtn" style="width:100%;">⏹ Detener Loop / Camas</button>
      </div>
    </div>
  </section>

  <section class="mix-deck">
    <div class="mix-item">
      <strong style="color:var(--tech);">Simulador OpenAL EFX &amp; Camas</strong>
      <button class="btn active" id="duckingToggle">Ducking Cinematográfico: ON</button>
    </div>
    <div class="mix-item">
      <label><span>Entorno EFX (Reverb)</span><span id="efxPresetLabel">OPEN_AIR</span></label>
      <select id="efxEnvSelect">
        <option value="OPEN_AIR">OPEN_AIR (Seco)</option>
        <option value="FOREST_CANOPY">FOREST_CANOPY (0.7s)</option>
        <option value="CAVERN_SINKHOLE">CAVERN_SINKHOLE (1.6s)</option>
        <option value="SANCTUM_VAULT">SANCTUM_VAULT (2.4s)</option>
        <option value="DEEP_MYCELIUM_ABYSS">DEEP_MYCELIUM_ABYSS (3.2s)</option>
      </select>
    </div>
    <div class="mix-item">
      <label><span>Oclusión Raycast</span><span id="occSliderVal">0% (20000 Hz)</span></label>
      <input type="range" id="occSlider" min="0" max="100" value="0" />
    </div>
    <div class="mix-item">
      <label><span>biome_caverns_loop</span><span id="mixVal_biome_caverns_loop">0%</span></label>
      <input type="range" min="0" max="100" value="0" data-mix="biome_caverns_loop" />
    </div>
    <div class="mix-item">
      <label><span>purification_pylon_hum</span><span id="mixVal_purification_pylon_hum">0%</span></label>
      <input type="range" min="0" max="100" value="0" data-mix="purification_pylon_hum" />
    </div>
    <div class="mix-item">
      <label><span>sovereign_nexus_hum</span><span id="mixVal_sovereign_nexus_hum">0%</span></label>
      <input type="range" min="0" max="100" value="0" data-mix="sovereign_nexus_hum" />
    </div>
    <div class="mix-item">
      <label><span>boss_combat_tension_bed</span><span id="mixVal_boss_combat_tension_bed">0%</span></label>
      <input type="range" min="0" max="100" value="0" data-mix="boss_combat_tension_bed" />
    </div>
    <div class="mix-item">
      <label><span>sinkhole_updraft_loop</span><span id="mixVal_sinkhole_updraft_loop">0%</span></label>
      <input type="range" min="0" max="100" value="0" data-mix="sinkhole_updraft_loop" />
    </div>
  </section>

  <section class="qa-v4-deck">
    <div class="viz-box">
      <div class="viz-title">
        <span>PERFIL BARK 24-BANDAS POR CAPA (CAMAS vs JEFES vs ALERTAS vs VOCALES)</span>
        <span id="maskSummary"></span>
      </div>
      <canvas id="barkCanvas" width="560" height="120" style="height:120px;"></canvas>
      <div class="sub" style="display:flex;gap:12px;flex-wrap:wrap;">
        <span style="color:#94a3b8;">■ Camas Ambientales</span>
        <span style="color:#f43f5e;">■ Jefes Tácticos</span>
        <span style="color:#22d3ee;">■ Alertas / UI</span>
        <span style="color:#34d399;">■ Vocales / Contrajuego</span>
      </div>
    </div>
    <div class="viz-box" style="max-height:190px;overflow:auto;">
      <div class="viz-title"><span>MATRIZ DE ENMASCARAMIENTO CONCURRENTE (TOP PARES)</span><span id="stemQcLabel"></span></div>
      <table class="mask-table">
        <thead><tr><th>Cama</th><th>Primer Plano</th><th>Bark</th><th>Headroom</th><th>Riesgo</th></tr></thead>
        <tbody id="maskTbody"></tbody>
      </table>
    </div>
  </section>

  <section class="controls">
    <div class="rank-bar" style="grid-column: 1 / -1;">
      <span style="font-size:0.82rem;color:var(--muted);">Vista rápida:</span>
      <button class="btn active" data-quick="">Todos</button>
      <button class="btn" data-quick="block_physical">Bloques Físicos (7 materiales)</button>
      <button class="btn" data-quick="block_functional">Bloques Funcionales ({func_count})</button>
      <button class="btn" data-quick="boss_cinematic">Jefes y Cinemática ({boss_count})</button>
      <button class="btn" data-quick="ai_hybrid">AI-Hybrid GPU</button>
      <button class="btn" data-quick="biome_ambient">Biomas y Ambiente ({biome_count})</button>
      <button class="btn" data-quick="entity">Entidades ({entity_count})</button>
      <button class="btn" data-quick="item_proj">Ítems / Proyectiles ({item_proj_count})</button>
      <button class="btn" data-quick="system">Sistema / IA</button>
    </div>
    <input id="search" type="search" placeholder="Buscar id o subtítulo (ej. sinkhole, stagger, updraft, codex, amber)..." />
    <select id="catFilter"><option value="">Todas las categorías</option></select>
    <select id="speciesFilter"><option value="">Todas las especies/materiales</option></select>
    <select id="loudnessFilter"><option value="">Todos los grupos RMS</option></select>
    <select id="irFilter"><option value="">Todas las salas IR (6)</option></select>
    <div class="rank-bar" style="grid-column: 1 / -1; justify-content: space-between;">
      <div class="rank-bar">
        <span style="font-size:0.82rem;color:var(--muted);">Rango:</span>
        <button class="btn active" data-rate="1.0">BASE 1.00x</button>
        <button class="btn" data-rate="0.89">MUTATED 0.89x</button>
        <button class="btn" data-rate="0.78">ELITE 0.78x</button>
      </div>
      <div class="rank-bar">
        <span style="font-size:0.82rem;color:var(--muted);">Cepa:</span>
        <button class="btn active" data-strain="1.0">ORTHODOX</button>
        <button class="btn" data-strain="1.06">AMBER (+6%)</button>
        <button class="btn" data-strain="0.93">CRIMSON (-7%)</button>
      </div>
      <div class="rank-bar">
        <span style="font-size:0.82rem;color:var(--muted);">Dinámica Táctica:</span>
        <button class="btn" id="frenzyToggle">Frenesí (+10% pitch)</button>
        <button class="btn" id="leaderToggle">Líder (-6% pitch)</button>
        <button class="btn" id="occludedToggle">Oclusión Cueva (65%)</button>
      </div>
    </div>
  </section>

  <table class="main">
    <thead>
      <tr>
        <th>Evento / Subtítulos</th>
        <th>Perfil</th>
        <th>Reproducción, Variaciones &amp; Seam</th>
        <th>Métricas Psicoacústicas QA v4</th>
      </tr>
    </thead>
    <tbody id="tbody"></tbody>
  </table>

  <script>
    const DATA = {payload_json};
    let rankRate = 1.0;
    let strainRate = 1.0;
    let frenzyOn = false;
    let leaderOn = false;
    let occlusionFactor = 0.0;
    let efxEnv = "OPEN_AIR";
    let duckingOn = true;
    let activeAudio = null;
    let seamLoopSource = null;
    let audioCtx = null;
    let analyser = null;
    let biquadFilter = null;
    let gainNode = null;
    let dryGain = null;
    let wetGain = null;
    let convolver = null;
    let slotA = null;
    let slotB = null;
    const mixBeds = {{}};
    let duckTimer = null;
    let duckFactor = 1.0;

    const EFX_PRESETS = {{
      OPEN_AIR: {{ decay: 0.25, wet: 0.0 }},
      FOREST_CANOPY: {{ decay: 0.72, wet: 0.18 }},
      CAVERN_SINKHOLE: {{ decay: 1.65, wet: 0.34 }},
      SANCTUM_VAULT: {{ decay: 2.35, wet: 0.44 }},
      DEEP_MYCELIUM_ABYSS: {{ decay: 3.15, wet: 0.52 }},
    }};

    const statsEl = document.getElementById("stats");
    const tpBadge = DATA.max_true_peak_dbfs != null ? `<span class="badge">TP 4x ≤ ${{DATA.max_true_peak_dbfs}} dBFS</span>` : "";
    const seamBadge = `<span class="badge ${{DATA.loop_seam_ok ? 'ok' : 'warn'}}">Seam ${{DATA.loop_seam_ok ? 'SEAMLESS' : 'CHECK'}}</span>`;
    const maskOk = DATA.masking ? Boolean(DATA.masking.ok) : true;
    const maskBadge = `<span class="badge ${{maskOk ? 'ok' : 'warn'}}">Bark Masking ${{maskOk ? 'CLEAR' : 'REVIEW'}}</span>`;
    const divBadge = `<span class="badge">Diversidad μ=${{DATA.mean_timbral_diversity ?? 0}}</span>`;
    statsEl.innerHTML = [
      `<span class="badge">${{DATA.total_events}} eventos ModSounds</span>`,
      `<span class="badge">${{DATA.total_ogg_files}} archivos .ogg mono</span>`,
      `<span class="badge">${{DATA.sample_rate}} Hz · ${{DATA.total_kb}} KB</span>`,
      tpBadge,
      seamBadge,
      maskBadge,
      divBadge,
      `<span class="badge ${{DATA.quality_ok ? 'ok' : 'warn'}}">QA v4 ${{DATA.quality_ok ? 'PASS' : 'CHECK'}}</span>`,
    ].join("");

    // Populate Bark Masking matrix & layer chart
    const maskSummaryEl = document.getElementById("maskSummary");
    const stemQcLabelEl = document.getElementById("stemQcLabel");
    const maskTbody = document.getElementById("maskTbody");
    if (DATA.masking) {{
      maskSummaryEl.textContent = `Pares: ${{DATA.masking.pairs_evaluated}} · Headroom μ=${{DATA.masking.mean_headroom_db}} dB · Risk max=${{DATA.masking.max_masking_risk}}`;
      maskTbody.innerHTML = (DATA.masking.top_overlap_pairs || []).map(p => {{
        const riskCls = p.masking_risk > 0.72 && p.headroom_db < 1.5 ? "color:var(--crimson);" : (p.masking_risk > 0.55 ? "color:var(--warn);" : "color:var(--good);");
        return `<tr><td>${{p.bed_id}}</td><td>${{p.foreground_id}}</td><td>${{Math.round(p.bark_overlap * 100)}}%</td><td>${{p.headroom_db > 0 ? '+' : ''}}${{p.headroom_db}} dB</td><td style="${{riskCls}}">${{p.masking_risk}}</td></tr>`;
      }}).join("");
      const bCanvas = document.getElementById("barkCanvas");
      const bCtx = bCanvas.getContext("2d");
      bCtx.fillStyle = "#07080c";
      bCtx.fillRect(0, 0, bCanvas.width, bCanvas.height);
      const layers = [
        ["ambient_beds", "#94a3b8"],
        ["boss_tactical", "#f43f5e"],
        ["alerts_ui", "#22d3ee"],
        ["entity_vocals", "#34d399"],
      ];
      const avgs = DATA.masking.layer_bark_averages || {{}};
      let maxVal = 0.05;
      layers.forEach(([k]) => (avgs[k] || []).forEach(v => {{ if (v > maxVal) maxVal = v; }}));
      layers.forEach(([key, color]) => {{
        const vec = avgs[key] || [];
        if (!vec.length) return;
        bCtx.strokeStyle = color;
        bCtx.lineWidth = 2.0;
        bCtx.beginPath();
        vec.forEach((v, idx) => {{
          const x = 10 + (idx / 23.0) * (bCanvas.width - 20);
          const y = (bCanvas.height - 10) - (v / maxVal) * (bCanvas.height - 20);
          if (idx === 0) bCtx.moveTo(x, y);
          else bCtx.lineTo(x, y);
        }});
        bCtx.stroke();
      }});
    }}
    if (DATA.stem_qc && DATA.stem_qc.stem_count) {{
      stemQcLabelEl.textContent = `Stems RTX: ${{DATA.stem_qc.checked_count}}/${{DATA.stem_qc.stem_count}} (${{DATA.stem_qc.ok ? 'PASS' : 'CHECK'}})`;
    }}

    const catSel = document.getElementById("catFilter");
    const spSel = document.getElementById("speciesFilter");
    const lSel = document.getElementById("loudnessFilter");
    const irSel = document.getElementById("irFilter");
    const searchEl = document.getElementById("search");
    const tbody = document.getElementById("tbody");
    const nowPlayingEl = document.getElementById("nowPlaying");
    const liveRateEl = document.getElementById("liveRateLabel");
    const seamStatusEl = document.getElementById("seamStatus");
    const occSlider = document.getElementById("occSlider");
    const occSliderVal = document.getElementById("occSliderVal");
    const efxSelect = document.getElementById("efxEnvSelect");
    const efxPresetLabel = document.getElementById("efxPresetLabel");

    const uniq = (fn) => [...new Set(DATA.events.map(fn))].sort();
    uniq(e => e.category).forEach(v => catSel.add(new Option(v, v)));
    uniq(e => e.species).forEach(v => spSel.add(new Option(v, v)));
    uniq(e => e.loudness_group).forEach(v => lSel.add(new Option(v, v)));
    uniq(e => e.ir_space).forEach(v => irSel.add(new Option(v, v)));

    const BOSS_IDS = new Set(DATA.boss_ids || []);
    const BIOME_IDS = new Set(DATA.biome_ambient_ids || []);
    let quickCat = "";

    function computeOcclusionCutoffHz() {{
      return Math.round(20000 * Math.pow(520 / 20000, occlusionFactor));
    }}

    function computeEffectiveRate() {{
      let r = rankRate * strainRate;
      if (frenzyOn) r *= 1.10;
      if (leaderOn) r *= 0.94;
      r *= (1.0 - 0.08 * occlusionFactor);
      return Math.max(0.45, Math.min(2.0, r));
    }}

    function computeEffectiveGain() {{
      let g = 1.0;
      if (frenzyOn) g *= 1.08;
      if (leaderOn) g *= 1.12;
      g *= (1.0 - 0.56 * occlusionFactor);
      return Math.max(0.2, Math.min(1.5, g));
    }}

    function buildSyntheticIr(ctx, decaySecs) {{
      const sr = ctx.sampleRate;
      const len = Math.max(256, Math.floor(sr * decaySecs));
      const buf = ctx.createBuffer(2, len, sr);
      for (let ch = 0; ch < 2; ch++) {{
        const data = buf.getChannelData(ch);
        let lp = 0.0;
        for (let i = 0; i < len; i++) {{
          const t = i / len;
          const env = Math.pow(1.0 - t, 2.8) * Math.exp(-3.5 * t);
          const n = (Math.random() * 2.0 - 1.0) * env;
          lp = lp * 0.68 + n * 0.32;
          data[i] = lp * 0.85;
        }}
      }}
      return buf;
    }}

    function applyEfxPreset() {{
      efxPresetLabel.textContent = efxEnv;
      if (!audioCtx || !convolver || !dryGain || !wetGain) return;
      const p = EFX_PRESETS[efxEnv] || EFX_PRESETS.OPEN_AIR;
      convolver.buffer = buildSyntheticIr(audioCtx, p.decay);
      wetGain.gain.setValueAtTime(p.wet, audioCtx.currentTime);
      dryGain.gain.setValueAtTime(1.0 - p.wet * 0.35, audioCtx.currentTime);
    }}

    function updateMixVolumes() {{
      Object.entries(mixBeds).forEach(([bid, entry]) => {{
        const vol = (entry.sliderVal / 100.0) * (duckingOn ? duckFactor : 1.0);
        entry.audio.volume = Math.max(0.0, Math.min(1.0, vol));
      }});
    }}

    function triggerDucking() {{
      if (!duckingOn) return;
      duckFactor = 0.35;
      updateMixVolumes();
      if (duckTimer) clearTimeout(duckTimer);
      duckTimer = setTimeout(() => {{
        duckFactor = 1.0;
        updateMixVolumes();
      }}, 1100);
    }}

    function updateRateLabel() {{
      const r = computeEffectiveRate().toFixed(2);
      const g = Math.round(computeEffectiveGain() * 100);
      const cut = computeOcclusionCutoffHz();
      occSliderVal.textContent = `${{Math.round(occlusionFactor * 100)}}% (${{cut}} Hz)`;
      liveRateEl.textContent = `Pitch: ${{r}}x · Vol: ${{g}}% · LPF ${{cut}}Hz · ${{efxEnv}}`;
      if (biquadFilter && audioCtx) {{
        biquadFilter.frequency.setValueAtTime(cut, audioCtx.currentTime);
      }}
      if (gainNode && audioCtx) {{
        gainNode.gain.setValueAtTime(computeEffectiveGain(), audioCtx.currentTime);
      }}
      if (activeAudio) {{
        activeAudio.playbackRate = computeEffectiveRate();
      }}
    }}

    occSlider.addEventListener("input", () => {{
      occlusionFactor = (parseInt(occSlider.value, 10) || 0) / 100.0;
      document.getElementById("occludedToggle").classList.toggle("active", occlusionFactor >= 0.5);
      updateRateLabel();
    }});

    efxSelect.addEventListener("change", () => {{
      efxEnv = efxSelect.value;
      applyEfxPreset();
      updateRateLabel();
    }});

    document.getElementById("duckingToggle").addEventListener("click", (e) => {{
      duckingOn = !duckingOn;
      e.currentTarget.classList.toggle("active", duckingOn);
      e.currentTarget.textContent = `Ducking Cinematográfico: ${{duckingOn ? 'ON' : 'OFF'}}`;
      if (!duckingOn) duckFactor = 1.0;
      updateMixVolumes();
    }});

    document.querySelectorAll("[data-mix]").forEach(inp => {{
      inp.addEventListener("input", () => {{
        const bid = inp.dataset.mix;
        const pct = parseInt(inp.value, 10) || 0;
        const lbl = document.getElementById(`mixVal_${{bid}}`);
        if (lbl) lbl.textContent = `${{pct}}%`;
        const src = (DATA.mix_bed_sources || {{}})[bid];
        if (!src) return;
        if (!mixBeds[bid]) {{
          const a = new Audio(src);
          a.loop = true;
          mixBeds[bid] = {{ audio: a, sliderVal: pct }};
        }}
        mixBeds[bid].sliderVal = pct;
        updateMixVolumes();
        if (pct > 0 && mixBeds[bid].audio.paused) {{
          mixBeds[bid].audio.play().catch(() => {{}});
        }} else if (pct === 0 && !mixBeds[bid].audio.paused) {{
          mixBeds[bid].audio.pause();
        }}
      }});
    }});

    function stopAllLoops() {{
      if (seamLoopSource) {{
        try {{ seamLoopSource.stop(); }} catch (e) {{}}
        seamLoopSource = null;
      }}
      if (activeAudio && activeAudio.loop) {{
        activeAudio.pause();
        activeAudio = null;
      }}
      Object.entries(mixBeds).forEach(([bid, entry]) => {{
        entry.sliderVal = 0;
        entry.audio.pause();
        const inp = document.querySelector(`[data-mix="${{bid}}"]`);
        if (inp) inp.value = "0";
        const lbl = document.getElementById(`mixVal_${{bid}}`);
        if (lbl) lbl.textContent = "0%";
      }});
      seamStatusEl.textContent = "Seam Tester: detenido";
    }}
    document.getElementById("stopLoopBtn").addEventListener("click", stopAllLoops);

    document.querySelectorAll("[data-quick]").forEach(btn => {{
      btn.addEventListener("click", () => {{
        document.querySelectorAll("[data-quick]").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        quickCat = btn.dataset.quick;
        if (quickCat === "item_proj" || quickCat === "boss_cinematic" || quickCat === "biome_ambient" || quickCat === "ai_hybrid") {{
          catSel.value = "";
        }} else {{
          catSel.value = quickCat;
        }}
        render();
      }});
    }});

    document.querySelectorAll("[data-rate]").forEach(btn => {{
      btn.addEventListener("click", () => {{
        document.querySelectorAll("[data-rate]").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        rankRate = parseFloat(btn.dataset.rate);
        updateRateLabel();
      }});
    }});

    document.querySelectorAll("[data-strain]").forEach(btn => {{
      btn.addEventListener("click", () => {{
        document.querySelectorAll("[data-strain]").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        strainRate = parseFloat(btn.dataset.strain);
        updateRateLabel();
      }});
    }});

    document.getElementById("frenzyToggle").addEventListener("click", (e) => {{
      frenzyOn = !frenzyOn;
      e.currentTarget.classList.toggle("active", frenzyOn);
      updateRateLabel();
    }});
    document.getElementById("leaderToggle").addEventListener("click", (e) => {{
      leaderOn = !leaderOn;
      e.currentTarget.classList.toggle("active", leaderOn);
      updateRateLabel();
    }});
    document.getElementById("occludedToggle").addEventListener("click", (e) => {{
      occlusionFactor = occlusionFactor >= 0.5 ? 0.0 : 0.65;
      occSlider.value = String(Math.round(occlusionFactor * 100));
      e.currentTarget.classList.toggle("active", occlusionFactor >= 0.5);
      updateRateLabel();
    }});

    function ensureAudioGraph() {{
      if (!audioCtx) {{
        const Ctx = window.AudioContext || window.webkitAudioContext;
        if (!Ctx) return null;
        audioCtx = new Ctx();
        analyser = audioCtx.createAnalyser();
        analyser.fftSize = 1024;
        analyser.smoothingTimeConstant = 0.78;
        biquadFilter = audioCtx.createBiquadFilter();
        biquadFilter.type = "lowpass";
        biquadFilter.frequency.value = computeOcclusionCutoffHz();
        gainNode = audioCtx.createGain();
        gainNode.gain.value = computeEffectiveGain();
        dryGain = audioCtx.createGain();
        wetGain = audioCtx.createGain();
        convolver = audioCtx.createConvolver();
        biquadFilter.connect(gainNode);
        gainNode.connect(dryGain);
        gainNode.connect(convolver);
        convolver.connect(wetGain);
        dryGain.connect(analyser);
        wetGain.connect(analyser);
        analyser.connect(audioCtx.destination);
        applyEfxPreset();
        requestAnimationFrame(drawVisualizers);
      }}
      if (audioCtx.state === "suspended") audioCtx.resume();
      return audioCtx;
    }}

    function waterfallColor(v) {{
      if (v < 0.25) return `rgb(${{Math.round(v * 240)}}, 8, ${{Math.round(40 + v * 360)}})`;
      if (v < 0.65) return `rgb(${{Math.round((v - 0.25) * 420)}}, ${{Math.round((v - 0.25) * 320)}}, 235)`;
      return `rgb(250, ${{Math.round(210 + (v - 0.65) * 120)}}, ${{Math.round(120 + (v - 0.65) * 300)}})`;
    }}

    function drawVisualizers() {{
      requestAnimationFrame(drawVisualizers);
      if (!analyser) return;
      const wCanvas = document.getElementById("waveCanvas");
      const fCanvas = document.getElementById("fftCanvas");
      const sCanvas = document.getElementById("specCanvas");
      const wCtx = wCanvas.getContext("2d");
      const fCtx = fCanvas.getContext("2d");
      const sCtx = sCanvas.getContext("2d");

      const timeBuf = new Uint8Array(analyser.fftSize);
      analyser.getByteTimeDomainData(timeBuf);
      wCtx.fillStyle = "#07080c";
      wCtx.fillRect(0, 0, wCanvas.width, wCanvas.height);
      wCtx.strokeStyle = "#22d3ee";
      wCtx.lineWidth = 1.6;
      wCtx.beginPath();
      const slice = wCanvas.width / timeBuf.length;
      for (let i = 0; i < timeBuf.length; i++) {{
        const y = (timeBuf[i] / 128.0) * (wCanvas.height / 2);
        if (i === 0) wCtx.moveTo(0, y);
        else wCtx.lineTo(i * slice, y);
      }}
      wCtx.stroke();

      const freqBuf = new Uint8Array(analyser.frequencyBinCount);
      analyser.getByteFrequencyData(freqBuf);
      fCtx.fillStyle = "#07080c";
      fCtx.fillRect(0, 0, fCanvas.width, fCanvas.height);
      const bars = 64;
      const barW = (fCanvas.width / bars) - 1.5;
      for (let i = 0; i < bars; i++) {{
        const bin = Math.min(freqBuf.length - 1, Math.floor(Math.pow(i / bars, 1.7) * (freqBuf.length * 0.65)));
        const val = freqBuf[bin] / 255.0;
        const h = Math.max(2, val * (fCanvas.height - 6));
        fCtx.fillStyle = i < 16 ? "#a855f7" : (i < 42 ? "#22d3ee" : "#34d399");
        fCtx.fillRect(i * (barW + 1.5), fCanvas.height - h, barW, h);
      }}

      const img = sCtx.getImageData(2, 0, sCanvas.width - 2, sCanvas.height);
      sCtx.putImageData(img, 0, 0);
      const rowsSpec = sCanvas.height;
      for (let y = 0; y < rowsSpec; y++) {{
        const normY = 1.0 - (y / Math.max(1, rowsSpec - 1));
        const bin = Math.min(freqBuf.length - 1, Math.floor(Math.pow(normY, 1.8) * (freqBuf.length * 0.65)));
        const val = freqBuf[bin] / 255.0;
        sCtx.fillStyle = waterfallColor(val);
        sCtx.fillRect(sCanvas.width - 2, y, 2, 1);
      }}
    }}

    function playFile(src, label, loopMode = false) {{
      if (activeAudio) {{ activeAudio.pause(); activeAudio = null; }}
      if (seamLoopSource) {{ try {{ seamLoopSource.stop(); }} catch (e) {{}} seamLoopSource = null; }}
      const audio = new Audio(src);
      audio.loop = loopMode;
      audio.preservesPitch = false;
      audio.playbackRate = computeEffectiveRate();
      nowPlayingEl.textContent = label || src.split("/").pop();
      triggerDucking();
      try {{
        const ctx = ensureAudioGraph();
        if (ctx) {{
          const node = ctx.createMediaElementSource(audio);
          node.connect(biquadFilter);
        }}
      }} catch (e) {{
        // Fallback under file:// if MediaElementSource is blocked
      }}
      audio.play().catch(err => alert("No se pudo reproducir " + src + ": " + err));
      activeAudio = audio;
    }}

    async function testSeamlessSeam(src, label) {{
      const ctx = ensureAudioGraph();
      if (!ctx) {{
        playFile(src, `Seam Loop: ${{label}}`, true);
        return;
      }}
      try {{
        const resp = await fetch(src);
        const buf = await ctx.decodeAudioData(await resp.arrayBuffer());
        const sr = buf.sampleRate;
        const win = Math.min(Math.floor(buf.length / 2), Math.floor(sr * 1.0));
        const spliceBuf = ctx.createBuffer(1, win * 2, sr);
        const srcData = buf.getChannelData(0);
        const dstData = spliceBuf.getChannelData(0);
        dstData.set(srcData.subarray(buf.length - win), 0);
        dstData.set(srcData.subarray(0, win), win);
        if (activeAudio) {{ activeAudio.pause(); activeAudio = null; }}
        if (seamLoopSource) {{ try {{ seamLoopSource.stop(); }} catch (e) {{}} }}
        const node = ctx.createBufferSource();
        node.buffer = spliceBuf;
        node.loop = true;
        node.connect(biquadFilter);
        node.start();
        seamLoopSource = node;
        nowPlayingEl.textContent = `Seam Loop: ${{label}}`;
        seamStatusEl.textContent = `Probando costura: ${{label}}`;
      }} catch (e) {{
        seamStatusEl.textContent = `Loop continuo (file://): ${{label}}`;
        playFile(src, `Seam Loop: ${{label}}`, true);
      }}
    }}

    document.getElementById("playABtn").addEventListener("click", () => {{
      if (slotA) playFile(slotA.src, "Ref A: " + slotA.id);
    }});
    document.getElementById("playBBtn").addEventListener("click", () => {{
      if (slotB) playFile(slotB.src, "Ref B: " + slotB.id);
    }});

    function render() {{
      const q = searchEl.value.trim().toLowerCase();
      const cat = catSel.value;
      const sp = spSel.value;
      const lg = lSel.value;
      const ir = irSel.value;
      const filtered = DATA.events.filter(e =>
        (quickCat === "item_proj"
          ? (e.category === "item" || e.category === "projectile")
          : quickCat === "boss_cinematic"
            ? BOSS_IDS.has(e.sound_id)
            : quickCat === "biome_ambient"
              ? BIOME_IDS.has(e.sound_id)
              : quickCat === "ai_hybrid"
                ? Boolean(e.ai_hybrid)
                : (!cat || e.category === cat)) &&
        (!sp || e.species === sp) &&
        (!lg || e.loudness_group === lg) &&
        (!ir || e.ir_space === ir) &&
        (!q || e.sound_id.toLowerCase().includes(q) || (e.stem_id || "").toLowerCase().includes(q) || e.en_subtitle.toLowerCase().includes(q) || e.es_subtitle.toLowerCase().includes(q))
      );
      tbody.innerHTML = filtered.map(e => {{
        const isTech = e.species === "purifier_tech" || e.species === "purification_tech" || e.species === "synaptic_node";
        const randBtn = `<button class="btn ${{isTech ? 'tech' : 'play'}}" data-rand="${{e.sound_id}}">Aleatorio (${{e.files.length}})</button>`;
        const varBtns = e.files.map((f, idx) => `<button class="btn" data-src="${{f}}" data-label="${{e.sound_id}} (v${{idx + 1}})">v${{idx + 1}}</button>`).join("");
        const seamBtn = e.is_loop ? `<button class="btn seam" data-seam="${{e.sound_id}}">🔁 Seam</button>` : "";
        const abBtns = `<button class="btn ab" data-seta="${{e.sound_id}}">→A</button><button class="btn ab" data-setb="${{e.sound_id}}">→B</button>`;
        const m = e.metrics;
        const corrText = e.files.length > 1 ? ` · corr≤${{e.max_corr}} · Div:${{e.timbral_diversity ?? 0}}` : " · único";
        const tpText = m.true_peak_dbfs != null ? ` · TP4x:${{m.true_peak_dbfs}}` : "";
        const lraText = m.lra_lu != null ? ` · ST:${{m.short_term_lufs_max}} · LRA:${{m.lra_lu}}LU` : "";
        const mudText = m.mud_ratio != null ? ` · Mud:${{Math.round(m.mud_ratio * 100)}}% Harsh:${{Math.round(m.harshness_ratio * 100)}}% · Bark#${{m.bark_peak_band}} (${{m.bark_peak_hz}}Hz)` : "";
        const seamText = e.is_loop && m.loop_seam_delta != null ? ` · SeamΔ:${{m.loop_seam_delta}}` : "";
        const baseD = e.baseline_delta ? ` · BaseΔRMS:${{e.baseline_delta.active_rms_delta_db > 0 ? '+' : ''}}${{e.baseline_delta.active_rms_delta_db}}dB` : "";
        const stemLabel = e.stem_id ? `<span class="pill" style="border-color:#22d3ee;color:#22d3ee;">AI-Hybrid · Stem: ${{e.stem_id}}</span>` : "";
        return `<tr>
          <td>
            <div class="id">${{e.sound_id}}</div>
            <div class="sub">${{e.es_subtitle}} / ${{e.en_subtitle}}</div>
          </td>
          <td>
            <span class="pill">${{e.category}}</span>
            <span class="pill">${{e.species}}</span>
            <span class="pill">${{e.loudness_group}} · ${{e.attenuation_distance}}m</span>
            <span class="pill">IR: ${{e.ir_space}}</span>
            ${{stemLabel}}
          </td>
          <td><div class="takes">${{randBtn}}${{varBtns}}${{seamBtn}}${{abBtns}}</div></td>
          <td class="metrics">
            RMS:${{m.active_rms_dbfs}} · LUFS:${{m.integrated_lufs}}${{lraText}}${{tpText}} · Crest:${{m.crest_db}}dB<br/>
            Cent:${{m.spectral_centroid_hz}}Hz · Dur:${{m.duration_secs}}s${{mudText}}${{corrText}}${{seamText}}${{baseD}}
          </td>
        </tr>`;
      }}).join("");

      tbody.querySelectorAll("[data-src]").forEach(b => b.addEventListener("click", () => playFile(b.dataset.src, b.dataset.label)));
      tbody.querySelectorAll("[data-seam]").forEach(b => {{
        b.addEventListener("click", () => {{
          const ev = DATA.events.find(x => x.sound_id === b.dataset.seam);
          if (ev && ev.files.length) testSeamlessSeam(ev.files[0], ev.sound_id);
        }});
      }});
      tbody.querySelectorAll("[data-rand]").forEach(b => {{
        b.addEventListener("click", () => {{
          const ev = DATA.events.find(x => x.sound_id === b.dataset.rand);
          if (ev && ev.files.length) {{
            const idx = Math.floor(Math.random() * ev.files.length);
            playFile(ev.files[idx], `${{ev.sound_id}} (v${{idx + 1}})`);
          }}
        }});
      }});
      tbody.querySelectorAll("[data-seta]").forEach(b => {{
        b.addEventListener("click", () => {{
          const ev = DATA.events.find(x => x.sound_id === b.dataset.seta);
          if (ev && ev.files.length) {{
            slotA = {{ id: ev.sound_id, src: ev.files[0] }};
            document.getElementById("slotAName").textContent = ev.sound_id;
          }}
        }});
      }});
      tbody.querySelectorAll("[data-setb]").forEach(b => {{
        b.addEventListener("click", () => {{
          const ev = DATA.events.find(x => x.sound_id === b.dataset.setb);
          if (ev && ev.files.length) {{
            slotB = {{ id: ev.sound_id, src: ev.files[0] }};
            document.getElementById("slotBName").textContent = ev.sound_id;
          }}
        }});
      }});
    }}

    [searchEl, spSel, lSel, irSel].forEach(el => el.addEventListener("input", render));
    catSel.addEventListener("input", () => {{
      quickCat = "";
      document.querySelectorAll("[data-quick]").forEach(b => b.classList.toggle("active", b.dataset.quick === catSel.value));
      render();
    }});
    updateRateLabel();
    render();
  </script>
</body>
</html>
"""
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(html_doc, encoding="utf-8")
