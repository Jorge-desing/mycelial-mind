"""Acoustic quality metrics, variation correlation checks, and local HTML audition console."""

from __future__ import annotations

import html
import json
import os
import subprocess
from pathlib import Path
from typing import Any

import numpy as np

from scripts.sound_studio.dsp_engine import (
    LOUDNESS_TARGETS_DBFS,
    SAMPLE_RATE,
    active_rms,
    find_ffmpeg,
)

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


def analyze_waveform(wave: np.ndarray, sample_rate: int = SAMPLE_RATE) -> dict[str, float]:
    """Compute acoustic loudness, peak ceiling, edge click guard, and spectral metrics."""
    n = len(wave)
    if n == 0:
        return {
            "rms_dbfs": -96.0,
            "active_rms_dbfs": -96.0,
            "integrated_lufs": -70.0,
            "peak_dbfs": -96.0,
            "crest_db": 0.0,
            "dc_offset": 0.0,
            "edge_start_abs": 0.0,
            "edge_end_abs": 0.0,
            "spectral_centroid_hz": 0.0,
            "spectral_flatness": 0.0,
            "zero_crossing_rate": 0.0,
            "low_band_ratio": 0.0,
            "high_band_ratio": 0.0,
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

    from scripts.sound_studio.mastering import measure_integrated_lufs

    lufs = measure_integrated_lufs(wave, sample_rate)

    return {
        "rms_dbfs": round(float(rms_db), 2),
        "active_rms_dbfs": round(float(act_db), 2),
        "integrated_lufs": round(float(lufs), 2),
        "peak_dbfs": round(float(peak_db), 2),
        "crest_db": round(float(crest_db), 2),
        "dc_offset": round(float(np.mean(wave)), 5),
        "edge_start_abs": round(edge_s, 5),
        "edge_end_abs": round(edge_e, 5),
        "spectral_centroid_hz": round(centroid, 1),
        "spectral_flatness": round(flatness, 4),
        "zero_crossing_rate": round(zcr, 4),
        "low_band_ratio": round(low_ratio, 3),
        "high_band_ratio": round(high_ratio, 3),
        "duration_secs": round(n / float(sample_rate), 3),
    }


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
) -> tuple[list[dict[str, float]], float]:
    """Compute acoustic metrics for all takes of an event + pairwise correlation (GPU-batched when available)."""
    max_lag = int(sample_rate * 0.020)
    gpu_res = _gpu_analyze_event_variations(waves, sample_rate=sample_rate, max_lag_samples=max_lag)
    if gpu_res is not None:
        metrics_list, max_corr = gpu_res
        from scripts.sound_studio.mastering import measure_integrated_lufs

        for w, m in zip(waves, metrics_list):
            m["integrated_lufs"] = round(float(measure_integrated_lufs(w, sample_rate)), 2)
        return metrics_list, max_corr

    file_metrics = [analyze_waveform(w, sample_rate) for w in waves]
    max_corr = max_pairwise_correlation(waves)
    return file_metrics, max_corr


def evaluate_quality(metrics: dict[str, float], loudness_group: str) -> list[str]:
    """Return any acoustic quality violations for a generated sound."""
    issues: list[str] = []
    if metrics["peak_dbfs"] > -0.25:
        issues.append(f"peak_dbfs too high ({metrics['peak_dbfs']} > -0.25)")
    if abs(metrics["dc_offset"]) > 0.01:
        issues.append(f"dc_offset too large ({metrics['dc_offset']})")
    if metrics["edge_start_abs"] > 0.02:
        issues.append(f"edge_start_abs click risk ({metrics['edge_start_abs']})")
    if metrics["edge_end_abs"] > 0.02:
        issues.append(f"edge_end_abs click risk ({metrics['edge_end_abs']})")
    target = LOUDNESS_TARGETS_DBFS.get(loudness_group)
    if target is not None:
        delta = metrics["active_rms_dbfs"] - target
        if abs(delta) > 3.0:
            issues.append(f"active_rms_dbfs {metrics['active_rms_dbfs']} outside {target} +-3.0 dB")
    return issues


def build_audition_html(
    manifest: dict[str, Any],
    quality_report: dict[str, Any],
    sounds_dir: Path,
    out_path: Path,
) -> None:
    """Write a self-contained local dark-mode HTML audition console for reviewing all 167 events & takes."""
    rel_sounds = Path(os.path.relpath(sounds_dir, out_path.parent)).as_posix()
    by_event_corr = quality_report.get("event_correlations", {})

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
                "attenuation_distance": entry["attenuation_distance"],
                "en_subtitle": entry["en_subtitle"],
                "es_subtitle": entry["es_subtitle"],
                "files": [f"{rel_sounds}/{fname}" for fname in entry["files"]],
                "metrics": entry["metrics"],
                "max_corr": by_event_corr.get(sid, 0.0),
            }
        )

    from scripts.sound_studio.boss_synth import BOSS_CINEMATIC_IDS

    biome_ambient_ids = sorted(
        e["sound_id"] for e in rows if e["category"] == "biome_ambient" or e["sound_id"].startswith("hive_t") or e["sound_id"] == "hive_synergy_unlock"
    )

    payload_json = json.dumps(
        {
            "sample_rate": manifest.get("sample_rate", SAMPLE_RATE),
            "total_events": len(rows),
            "total_ogg_files": manifest.get("ogg_count", 0),
            "total_kb": round(manifest.get("total_bytes", 0) / 1024.0, 1),
            "quality_ok": quality_report.get("ok", False),
            "boss_ids": sorted(BOSS_CINEMATIC_IDS),
            "biome_ambient_ids": biome_ambient_ids,
            "events": rows,
        },
        ensure_ascii=False,
    ).replace("</", "<\\/")

    html_doc = f"""<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Mycelial Mind — Sound Studio Audition Console v2 ({len(rows)} eventos)</title>
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
    .viz-deck {{
      display: grid; grid-template-columns: 1fr 1fr 310px; gap: 12px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; margin-bottom: 14px;
    }}
    @media (max-width: 960px) {{ .viz-deck {{ grid-template-columns: 1fr; }} }}
    .viz-box {{
      background: #090a0f; border: 1px solid var(--border); border-radius: 9px; padding: 8px 10px; display: flex; flex-direction: column; gap: 4px;
    }}
    .viz-title {{ font-size: 0.76rem; color: var(--muted); display: flex; justify-content: space-between; font-family: ui-monospace, Consolas, monospace; }}
    canvas {{ width: 100%; height: 84px; display: block; border-radius: 6px; background: #07080c; }}
    .ab-box {{
      background: var(--panel-2); border: 1px solid var(--border); border-radius: 9px; padding: 10px 12px;
      display: flex; flex-direction: column; justify-content: space-between; gap: 8px; font-size: 0.82rem;
    }}
    .ab-slot {{ display: flex; align-items: center; justify-content: space-between; gap: 6px; }}
    .ab-name {{ font-family: ui-monospace, Consolas, monospace; font-size: 0.78rem; color: #f3e8ff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 155px; }}
    .controls {{
      display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px;
      background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 16px;
    }}
    input, select {{
      width: 100%; background: var(--panel-2); color: var(--text); border: 1px solid var(--border);
      border-radius: 8px; padding: 8px 10px; font-size: 0.9rem;
    }}
    .rank-bar {{ display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }}
    .btn {{
      background: var(--panel-2); color: var(--text); border: 1px solid var(--border);
      border-radius: 7px; padding: 6px 10px; cursor: pointer; font-size: 0.80rem; transition: 0.15s;
    }}
    .btn:hover {{ border-color: var(--accent); }}
    .btn.active {{ background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; }}
    .btn.play {{ background: #261b3a; border-color: var(--accent); }}
    .btn.tech {{ background: #122936; border-color: var(--tech); }}
    .btn.ab {{ padding: 4px 7px; font-size: 0.74rem; color: var(--muted); }}
    table {{ width: 100%; border-collapse: collapse; background: var(--panel); border-radius: 12px; overflow: hidden; border: 1px solid var(--border); }}
    th, td {{ padding: 10px 12px; border-bottom: 1px solid var(--border); text-align: left; vertical-align: middle; font-size: 0.86rem; }}
    th {{ background: var(--panel-2); color: var(--muted); font-weight: 600; }}
    tr:hover td {{ background: rgba(168, 85, 247, 0.06); }}
    .id {{ font-family: ui-monospace, Consolas, monospace; font-weight: 600; color: #f3e8ff; }}
    .sub {{ color: var(--muted); font-size: 0.8rem; margin-top: 2px; }}
    .takes {{ display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }}
    .metrics {{ font-family: ui-monospace, Consolas, monospace; font-size: 0.78rem; color: var(--muted); line-height: 1.45; }}
    .pill {{ display: inline-block; padding: 2px 7px; border-radius: 6px; background: var(--panel-2); border: 1px solid var(--border); font-size: 0.76rem; }}
  </style>
</head>
<body>
  <header>
    <div>
      <h1>{html.escape("Mycelial Mind — Consola Local de Audio v2 (Bio-Horror DSP 44.1 kHz + WebAudio FFT)")}</h1>
      <div class="sub">Audita los 5 motores DSP, osciloscopio y espectro FFT en vivo, comparador A/B, rangos evolutivos, cepas de Cisma, Frenesí y oclusión de cuevas.</div>
    </div>
    <div class="stats" id="stats"></div>
  </header>

  <section class="viz-deck">
    <div class="viz-box">
      <div class="viz-title"><span>OSCILOSCOPIO TEMPORAL (WebAudio PCM)</span><span id="nowPlaying">En espera...</span></div>
      <canvas id="waveCanvas" width="480" height="84"></canvas>
    </div>
    <div class="viz-box">
      <div class="viz-title"><span>ANALIZADOR ESPECTRAL FFT (80 Hz – 14 kHz)</span><span id="liveRateLabel">Pitch: 1.00x · Vol: 100%</span></div>
      <canvas id="fftCanvas" width="480" height="84"></canvas>
    </div>
    <div class="ab-box">
      <div style="font-weight:600;color:var(--tech);">Comparador Acústico A / B</div>
      <div class="ab-slot">
        <span>Ref A: <span class="ab-name" id="slotAName">(ninguno)</span></span>
        <button class="btn play" id="playABtn">▶ A</button>
      </div>
      <div class="ab-slot">
        <span>Ref B: <span class="ab-name" id="slotBName">(ninguno)</span></span>
        <button class="btn tech" id="playBBtn">▶ B</button>
      </div>
    </div>
  </section>

  <section class="controls">
    <div class="rank-bar" style="grid-column: 1 / -1;">
      <span style="font-size:0.82rem;color:var(--muted);">Vista rápida:</span>
      <button class="btn active" data-quick="">Todos</button>
      <button class="btn" data-quick="block_physical">Bloques Físicos (7 materiales)</button>
      <button class="btn" data-quick="block_functional">Bloques Funcionales (14)</button>
      <button class="btn" data-quick="boss_cinematic">Jefes y Cinemática (17)</button>
      <button class="btn" data-quick="ai_hybrid">AI-Hybrid GPU</button>
      <button class="btn" data-quick="biome_ambient">Biomas y Ambiente (14)</button>
      <button class="btn" data-quick="entity">Entidades (17 criaturas)</button>
      <button class="btn" data-quick="item_proj">Ítems / Proyectiles (26)</button>
      <button class="btn" data-quick="system">Sistema / IA</button>
    </div>
    <input id="search" type="search" placeholder="Buscar id o subtítulo (ej. matriarch, spore, biome_caverns, block_chitin)..." />
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
        <button class="btn" id="occludedToggle">Oclusión Cueva (LPF 950 Hz)</button>
      </div>
    </div>
  </section>

  <table>
    <thead>
      <tr>
        <th>Evento / Subtítulos</th>
        <th>Perfil</th>
        <th>Reproducción, Variaciones & A/B</th>
        <th>Métricas Acústicas</th>
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
    let occludedOn = false;
    let activeAudio = null;
    let audioCtx = null;
    let analyser = null;
    let biquadFilter = null;
    let gainNode = null;
    let slotA = null;
    let slotB = null;

    const statsEl = document.getElementById("stats");
    statsEl.innerHTML = [
      `<span class="badge">${{DATA.total_events}} eventos ModSounds</span>`,
      `<span class="badge">${{DATA.total_ogg_files}} archivos .ogg mono</span>`,
      `<span class="badge">${{DATA.sample_rate}} Hz · ${{DATA.total_kb}} KB</span>`,
      `<span class="badge ${{DATA.quality_ok ? 'ok' : ''}}">QA ${{DATA.quality_ok ? 'PASS' : 'CHECK'}}</span>`,
    ].join("");

    const catSel = document.getElementById("catFilter");
    const spSel = document.getElementById("speciesFilter");
    const lSel = document.getElementById("loudnessFilter");
    const irSel = document.getElementById("irFilter");
    const searchEl = document.getElementById("search");
    const tbody = document.getElementById("tbody");
    const nowPlayingEl = document.getElementById("nowPlaying");
    const liveRateEl = document.getElementById("liveRateLabel");

    const uniq = (fn) => [...new Set(DATA.events.map(fn))].sort();
    uniq(e => e.category).forEach(v => catSel.add(new Option(v, v)));
    uniq(e => e.species).forEach(v => spSel.add(new Option(v, v)));
    uniq(e => e.loudness_group).forEach(v => lSel.add(new Option(v, v)));
    uniq(e => e.ir_space).forEach(v => irSel.add(new Option(v, v)));

    const BOSS_IDS = new Set(DATA.boss_ids || []);
    const BIOME_IDS = new Set(DATA.biome_ambient_ids || []);
    let quickCat = "";

    function computeEffectiveRate() {{
      let r = rankRate * strainRate;
      if (frenzyOn) r *= 1.10;
      if (leaderOn) r *= 0.94;
      if (occludedOn) r *= 0.92;
      return Math.max(0.45, Math.min(2.0, r));
    }}

    function computeEffectiveGain() {{
      let g = 1.0;
      if (frenzyOn) g *= 1.08;
      if (leaderOn) g *= 1.12;
      if (occludedOn) g *= 0.58;
      return Math.max(0.2, Math.min(1.5, g));
    }}

    function updateRateLabel() {{
      const r = computeEffectiveRate().toFixed(2);
      const g = Math.round(computeEffectiveGain() * 100);
      liveRateEl.textContent = `Pitch: ${{r}}x · Vol: ${{g}}%${{occludedOn ? ' · LPF 950Hz' : ''}}`;
      if (biquadFilter && audioCtx) {{
        biquadFilter.frequency.setValueAtTime(occludedOn ? 950 : 20000, audioCtx.currentTime);
      }}
      if (gainNode && audioCtx) {{
        gainNode.gain.setValueAtTime(computeEffectiveGain(), audioCtx.currentTime);
      }}
      if (activeAudio) {{
        activeAudio.playbackRate = computeEffectiveRate();
      }}
    }}

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
      occludedOn = !occludedOn;
      e.currentTarget.classList.toggle("active", occludedOn);
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
        biquadFilter.frequency.value = occludedOn ? 950 : 20000;
        gainNode = audioCtx.createGain();
        gainNode.gain.value = computeEffectiveGain();
        biquadFilter.connect(gainNode);
        gainNode.connect(analyser);
        analyser.connect(audioCtx.destination);
        requestAnimationFrame(drawVisualizers);
      }}
      if (audioCtx.state === "suspended") audioCtx.resume();
      return audioCtx;
    }}

    function drawVisualizers() {{
      requestAnimationFrame(drawVisualizers);
      if (!analyser) return;
      const wCanvas = document.getElementById("waveCanvas");
      const fCanvas = document.getElementById("fftCanvas");
      const wCtx = wCanvas.getContext("2d");
      const fCtx = fCanvas.getContext("2d");

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
    }}

    function playFile(src, label) {{
      if (activeAudio) {{ activeAudio.pause(); activeAudio = null; }}
      const audio = new Audio(src);
      audio.preservesPitch = false;
      audio.playbackRate = computeEffectiveRate();
      nowPlayingEl.textContent = label || src.split("/").pop();
      try {{
        const ctx = ensureAudioGraph();
        if (ctx) {{
          const node = ctx.createMediaElementSource(audio);
          node.connect(biquadFilter);
        }}
      }} catch (e) {{
        // Fallback to direct element playback under file:// restrictions if MediaElementSource is blocked
      }}
      audio.play().catch(err => alert("No se pudo reproducir " + src + ": " + err));
      activeAudio = audio;
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
        const abBtns = `<button class="btn ab" data-seta="${{e.sound_id}}">→A</button><button class="btn ab" data-setb="${{e.sound_id}}">→B</button>`;
        const m = e.metrics;
        const corrText = e.files.length > 1 ? ` · corr≤${{e.max_corr}}` : " · único";
        const stemLabel = e.stem_id ? `<span class="pill" style="border-color:#22d3ee;color:#22d3ee;">AI-Hybrid (RTX 4060) · Stem: ${{e.stem_id}}</span>` : "";
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
          <td><div class="takes">${{randBtn}}${{varBtns}}${{abBtns}}</div></td>
          <td class="metrics">
            RMS activo: ${{m.active_rms_dbfs}} dBFS · LUFS: ${{m.integrated_lufs}} · Pico: ${{m.peak_dbfs}} dBFS<br/>
            Centroide: ${{m.spectral_centroid_hz}} Hz · Flatness: ${{m.spectral_flatness}} · Dur: ${{m.duration_secs}}s${{corrText}}
          </td>
        </tr>`;
      }}).join("");

      tbody.querySelectorAll("[data-src]").forEach(b => b.addEventListener("click", () => playFile(b.dataset.src, b.dataset.label)));
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
