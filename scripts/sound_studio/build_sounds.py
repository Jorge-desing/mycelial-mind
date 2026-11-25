"""CLI for Mycelial Mind's 44.1 kHz bio-horror procedural DSP & Ogg Vorbis Sound Studio."""

from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
from dataclasses import asdict
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
from typing import Any

import numpy as np

REPO_ROOT = Path(__file__).resolve().parents[2]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.sound_studio.catalog import SOUND_BY_ID, SOUND_CATALOG, SoundSpec
from scripts.sound_studio.dsp_engine import (
    LOUDNESS_TARGETS_DBFS,
    SAMPLE_RATE,
    VORBIS_QUALITY,
    deterministic_rng,
    encode_ogg_vorbis_mono,
    find_ffmpeg,
    float_to_pcm16,
    inspect_ogg_vorbis,
    synthesize_waveform,
)
from scripts.sound_studio.metrics import (
    BARK_CENTERS_HZ,
    analyze_event_variations,
    analyze_waveform,
    audit_spectral_masking,
    build_audition_html,
    compare_against_baseline,
    compute_timbral_diversity,
    evaluate_quality,
    load_acoustic_baseline,
    max_pairwise_correlation,
    save_acoustic_baseline,
)

ASSETS_ROOT = REPO_ROOT / "src" / "main" / "resources" / "assets" / "mycelialmind"
SOUNDS_DIR = ASSETS_ROOT / "sounds"
SOUNDS_JSON_PATH = ASSETS_ROOT / "sounds.json"
LANG_EN_PATH = ASSETS_ROOT / "lang" / "en_us.json"
LANG_ES_PATH = ASSETS_ROOT / "lang" / "es_es.json"
MOD_SOUNDS_JAVA = REPO_ROOT / "src" / "main" / "java" / "com" / "alex" / "mycelialmind" / "registry" / "ModSounds.java"
STUDIO_DIR = Path(__file__).resolve().parent
EXPORTS_DIR = STUDIO_DIR / "exports"
MANIFEST_PATH = EXPORTS_DIR / "sound_manifest.json"
QUALITY_REPORT_PATH = EXPORTS_DIR / "sound_quality_report.json"
AUDITION_HTML_PATH = EXPORTS_DIR / "sound_audition.html"
BUILD_CACHE_PATH = EXPORTS_DIR / "sound_build_cache.json"
BASELINE_PATH = EXPORTS_DIR / "sound_baseline.json"

MAX_VARIATION_CORR = 0.85

_ENGINE_FILES_FOR_HASH: tuple[str, ...] = (
    "ai_stem_generator.py",
    "block_synth.py",
    "boss_synth.py",
    "cuda_dsp.py",
    "dsp_engine.py",
    "entity_synth.py",
    "ir_bank.py",
    "item_synth.py",
    "mastering.py",
    "metrics.py",
    "system_env_synth.py",
)


def expected_variant_stems(spec: SoundSpec) -> list[str]:
    if spec.variations <= 1:
        return [spec.sound_id]
    return [f"{spec.sound_id}_{idx + 1}" for idx in range(spec.variations)]


def _synthesize_and_encode_spec(
    spec: SoundSpec,
    ffmpeg_exe: str,
    encode_pool: ThreadPoolExecutor | None = None,
) -> dict[str, object]:
    stems = expected_variant_stems(spec)
    variant_waves: list[np.ndarray] = []
    encode_tasks: list[tuple[Any, Path, str]] = []

    for v_idx, stem in enumerate(stems):
        wave = synthesize_waveform(
            spec.sound_id,
            recipe=spec.recipe,
            duration=spec.duration,
            base_freq=spec.base_freq,
            mod_freq=spec.mod_freq,
            variant=spec.variant,
            species=spec.species,
            loudness_group=spec.loudness_group,
            seed_offset=spec.seed_offset,
            variation_idx=v_idx,
            category=spec.category,
        )
        pcm = float_to_pcm16(wave)
        pcm_q = np.frombuffer(pcm, dtype="<i2").astype(np.float64) / 32768.0
        ogg_path = SOUNDS_DIR / f"{stem}.ogg"
        if encode_pool is not None:
            fut = encode_pool.submit(encode_ogg_vorbis_mono, pcm, ogg_path, ffmpeg_exe=ffmpeg_exe)
            encode_tasks.append((fut, ogg_path, stem))
        else:
            encode_ogg_vorbis_mono(pcm, ogg_path, ffmpeg_exe=ffmpeg_exe)
            encode_tasks.append((None, ogg_path, stem))
        variant_waves.append(pcm_q)

    # Overlap batched GPU metrics + pairwise xcorr with background ffmpeg Ogg Vorbis subprocesses
    file_metrics, max_corr = analyze_event_variations(variant_waves, SAMPLE_RATE)

    from scripts.sound_studio.mastering import SEAMLESS_LOOP_IDS

    is_loop = spec.sound_id in SEAMLESS_LOOP_IDS
    file_names: list[str] = []
    file_sizes: list[int] = []
    variant_issues: list[dict[str, object]] = []
    for (fut, ogg_path, stem), m in zip(encode_tasks, file_metrics):
        if fut is not None:
            fut.result()
        info = inspect_ogg_vorbis(ogg_path)
        if not info.get("ok"):
            raise RuntimeError(f"Generated invalid Ogg Vorbis file for '{stem}': {info}")
        issues = evaluate_quality(m, spec.loudness_group, is_loop=is_loop)
        if issues:
            variant_issues.append({"file": ogg_path.name, "issues": issues, "metrics": m})
        file_names.append(ogg_path.name)
        file_sizes.append(int(info["size_bytes"]))

    mean_metrics: dict[str, Any] = {}
    for k in file_metrics[0]:
        sample_val = file_metrics[0][k]
        if isinstance(sample_val, list):
            arr = np.mean([np.asarray(fm[k], dtype=np.float64) for fm in file_metrics], axis=0)
            mean_metrics[k] = [round(float(x), 4) for x in arr]
        else:
            mean_metrics[k] = round(sum(float(fm[k]) for fm in file_metrics) / len(file_metrics), 3)
    if isinstance(mean_metrics.get("bark_profile"), list) and len(mean_metrics["bark_profile"]) == 24:
        peak_b = int(np.argmax(mean_metrics["bark_profile"]))
        mean_metrics["bark_peak_band"] = peak_b
        mean_metrics["bark_peak_hz"] = BARK_CENTERS_HZ[peak_b]
    timbral_diversity = compute_timbral_diversity(file_metrics, max_corr)
    from scripts.sound_studio.ai_stem_generator import get_stem_id, is_ai_hybrid
    from scripts.sound_studio.ir_bank import select_ir_space

    return {
        "sound_id": spec.sound_id,
        "java_const": spec.java_const,
        "category": spec.category,
        "recipe": spec.recipe,
        "species": spec.species,
        "loudness_group": spec.loudness_group,
        "ir_space": select_ir_space(spec.sound_id, spec.species, spec.loudness_group, spec.category),
        "ai_hybrid": is_ai_hybrid(spec.sound_id),
        "stem_id": get_stem_id(spec.sound_id),
        "variations": spec.variations,
        "attenuation_distance": spec.attenuation_distance,
        "en_subtitle": spec.en_subtitle,
        "es_subtitle": spec.es_subtitle,
        "files": file_names,
        "sample_rate": SAMPLE_RATE,
        "channels": 1,
        "total_bytes": sum(file_sizes),
        "max_variant_corr": max_corr,
        "timbral_diversity": timbral_diversity,
        "metrics": mean_metrics,
        "variant_issues": variant_issues,
    }


def _sync_lang_file(path: Path, subtitles: dict[str, str]) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    cleaned = {k: v for k, v in data.items() if not k.startswith("subtitles.mycelialmind.")}
    for sub_key in sorted(subtitles):
        cleaned[sub_key] = subtitles[sub_key]
    path.write_text(json.dumps(cleaned, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def _compute_engine_fingerprint() -> str:
    h = hashlib.sha256()
    for rel in _ENGINE_FILES_FOR_HASH:
        p = STUDIO_DIR / rel
        if p.is_file():
            h.update(rel.encode("utf-8"))
            h.update(p.read_bytes())
    return h.hexdigest()[:24]


def _compute_spec_cache_hash(spec: SoundSpec, engine_fp: str) -> str:
    from scripts.sound_studio.ai_stem_generator import AI_STEMS_DIR, get_stem_id
    from scripts.sound_studio.ir_bank import IRS_DIR, select_ir_space

    stem_id = get_stem_id(spec.sound_id)
    ir_space = select_ir_space(spec.sound_id, spec.species, spec.loudness_group, spec.category)
    stem_sig = "none"
    if stem_id:
        sp = AI_STEMS_DIR / f"{stem_id}.wav"
        if sp.is_file():
            st = sp.stat()
            stem_sig = f"{stem_id}:{st.st_size}:{st.st_mtime_ns}"
    ir_sig = ir_space
    ip = IRS_DIR / f"{ir_space}.wav"
    if ip.is_file():
        ist = ip.stat()
        ir_sig = f"{ir_space}:{ist.st_size}:{ist.st_mtime_ns}"

    payload = {
        "engine": engine_fp,
        "spec": asdict(spec),
        "ir": ir_sig,
        "stem": stem_sig,
    }
    raw = json.dumps(payload, sort_keys=True, ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()[:24]


def _can_reuse_cached_entry(
    spec: SoundSpec,
    prev_entry: dict[str, object] | None,
) -> bool:
    if not isinstance(prev_entry, dict):
        return False
    expected_names = [f"{s}.ogg" for s in expected_variant_stems(spec)]
    if prev_entry.get("files") != expected_names:
        return False
    metrics = prev_entry.get("metrics")
    if (
        not isinstance(metrics, dict)
        or "true_peak_dbfs" not in metrics
        or "loop_seam_delta" not in metrics
        or "bark_profile" not in metrics
        or "lra_lu" not in metrics
    ):
        return False
    for fname in expected_names:
        p = SOUNDS_DIR / fname
        if not p.is_file() or not inspect_ogg_vorbis(p).get("ok"):
            return False
    return True


def generate_all(
    workers: int = 8,
    *,
    regen_ai_stems: bool = False,
    changed_only: bool = False,
    only_ids: set[str] | None = None,
    category: str | None = None,
    loudness_group: str | None = None,
    update_baseline: bool = False,
    check_baseline: bool = False,
) -> dict[str, object]:
    """Generate mono 44.1 kHz .ogg variants, sounds.json, subtitles, quality report & audition HTML."""
    from scripts.sound_studio import cuda_dsp, mastering
    from scripts.sound_studio.ai_stem_generator import ensure_ai_stems, preload_all_stems
    from scripts.sound_studio.ir_bank import ensure_ir_bank
    from scripts.sound_studio.mastering import SEAMLESS_LOOP_IDS

    t0 = time.perf_counter()
    ffmpeg_exe = find_ffmpeg()
    ensure_ir_bank()
    ensure_ai_stems(force_regen=regen_ai_stems)
    SOUNDS_DIR.mkdir(parents=True, exist_ok=True)
    EXPORTS_DIR.mkdir(parents=True, exist_ok=True)

    # Remove stale or unnumbered .ogg files no longer in SOUND_CATALOG
    expected_files = {f"{stem}.ogg" for spec in SOUND_CATALOG for stem in expected_variant_stems(spec)}
    for existing in SOUNDS_DIR.glob("*.ogg"):
        if existing.name not in expected_files:
            existing.unlink()

    prev_manifest_by_id: dict[str, dict[str, object]] = {}
    if MANIFEST_PATH.is_file():
        try:
            old_manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
            for item in old_manifest.get("sounds", []):
                if isinstance(item, dict) and "sound_id" in item:
                    prev_manifest_by_id[str(item["sound_id"])] = item
        except (OSError, json.JSONDecodeError):
            prev_manifest_by_id = {}

    prev_hashes: dict[str, str] = {}
    if BUILD_CACHE_PATH.is_file():
        try:
            old_cache = json.loads(BUILD_CACHE_PATH.read_text(encoding="utf-8"))
            if isinstance(old_cache, dict):
                prev_hashes = {str(k): str(v) for k, v in (old_cache.get("event_hashes", {}) or {}).items()}
        except (OSError, json.JSONDecodeError):
            prev_hashes = {}

    engine_fp = _compute_engine_fingerprint()
    cur_hashes = {spec.sound_id: _compute_spec_cache_hash(spec, engine_fp) for spec in SOUND_CATALOG}

    specs_to_build: list[SoundSpec] = []
    reused_entries: dict[str, dict[str, object]] = {}
    has_filter = bool(only_ids or category or loudness_group)

    for spec in SOUND_CATALOG:
        sid = spec.sound_id
        prev_entry = prev_manifest_by_id.get(sid)
        in_filter = (
            (not only_ids or sid in only_ids)
            and (not category or spec.category == category)
            and (not loudness_group or spec.loudness_group == loudness_group)
        )
        can_reuse = _can_reuse_cached_entry(spec, prev_entry)
        if has_filter and not in_filter and can_reuse:
            reused = dict(prev_entry)  # type: ignore[arg-type]
            reused["variant_issues"] = evaluate_quality(
                reused["metrics"],  # type: ignore[arg-type]
                spec.loudness_group,
                is_loop=sid in SEAMLESS_LOOP_IDS,
            )
            reused_entries[sid] = reused
        elif changed_only and not regen_ai_stems and prev_hashes.get(sid) == cur_hashes[sid] and can_reuse:
            reused = dict(prev_entry)  # type: ignore[arg-type]
            reused["variant_issues"] = evaluate_quality(
                reused["metrics"],  # type: ignore[arg-type]
                spec.loudness_group,
                is_loop=sid in SEAMLESS_LOOP_IDS,
            )
            reused_entries[sid] = reused
        else:
            specs_to_build.append(spec)

    gpu_name = ""
    built_by_id: dict[str, dict[str, object]] = {}
    if specs_to_build:
        gpu_name = cuda_dsp.warmup_cuda()
        mastering.preload_ir_bank_for_gpu()
        preload_all_stems()
        encode_workers = max(4, min(12, os.cpu_count() or 8))
        try:
            with ThreadPoolExecutor(max_workers=encode_workers) as encode_pool, ThreadPoolExecutor(max_workers=max(1, workers)) as synth_pool:
                futures = [
                    (spec.sound_id, synth_pool.submit(_synthesize_and_encode_spec, spec, ffmpeg_exe, encode_pool))
                    for spec in specs_to_build
                ]
                for sid, fut in futures:
                    built_by_id[sid] = fut.result()
        finally:
            cuda_dsp.clear_gpu_caches()

    manifest_entries: list[dict[str, object]] = []
    for spec in SOUND_CATALOG:
        sid = spec.sound_id
        if sid in built_by_id:
            manifest_entries.append(built_by_id[sid])
        else:
            manifest_entries.append(reused_entries[sid])

    sounds_json: dict[str, object] = {}
    en_subtitles: dict[str, str] = {}
    es_subtitles: dict[str, str] = {}
    failed_files: list[dict[str, object]] = []
    event_correlations: dict[str, float] = {}
    failed_corr_events: list[dict[str, object]] = []
    group_accum: dict[str, list[dict[str, float]]] = {}
    loop_seam_failures: list[str] = []

    for spec, entry in zip(SOUND_CATALOG, manifest_entries):
        stems = expected_variant_stems(spec)
        sound_variants: list[dict[str, object]] = []
        for idx, stem in enumerate(stems):
            rng = deterministic_rng(spec.sound_id, spec.seed_offset, idx)
            vol_jit = 1.0 if len(stems) == 1 else float(rng.uniform(0.97, 1.03))
            pitch_jit = 1.0 if len(stems) == 1 else float(rng.uniform(0.96, 1.04))
            sound_variants.append(
                {
                    "name": f"mycelialmind:{stem}",
                    "volume": round(spec.volume * vol_jit, 3),
                    "pitch": round(spec.pitch * pitch_jit, 3),
                    "attenuation_distance": spec.attenuation_distance,
                }
            )
        sounds_json[spec.sound_id] = {
            "subtitle": f"subtitles.mycelialmind.{spec.sound_id}",
            "sounds": sound_variants,
        }
        sub_key = f"subtitles.mycelialmind.{spec.sound_id}"
        en_subtitles[sub_key] = spec.en_subtitle
        es_subtitles[sub_key] = spec.es_subtitle

        issues = entry.pop("variant_issues", [])  # type: ignore[arg-type]
        if isinstance(issues, list) and issues:
            if isinstance(issues[0], dict):
                failed_files.extend(issues)
            else:
                failed_files.append({"file": spec.sound_id, "issues": issues, "metrics": entry["metrics"]})
        if spec.sound_id in SEAMLESS_LOOP_IDS:
            seam_val = float(entry["metrics"].get("loop_seam_delta", 0.0))  # type: ignore[union-attr]
            if seam_val > 0.025:
                loop_seam_failures.append(spec.sound_id)
        corr = float(entry["max_variant_corr"])
        event_correlations[spec.sound_id] = corr
        if spec.variations > 1 and corr >= MAX_VARIATION_CORR:
            failed_corr_events.append({"sound_id": spec.sound_id, "max_variant_corr": corr})
        group_accum.setdefault(spec.loudness_group, []).append(entry["metrics"])  # type: ignore[arg-type]

    SOUNDS_JSON_PATH.write_text(json.dumps(sounds_json, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    _sync_lang_file(LANG_EN_PATH, en_subtitles)
    _sync_lang_file(LANG_ES_PATH, es_subtitles)

    total_ogg_count = sum(len(e["files"]) for e in manifest_entries)  # type: ignore[arg-type]
    total_bytes = sum(int(e["total_bytes"]) for e in manifest_entries)
    manifest = {
        "count": len(manifest_entries),
        "ogg_count": total_ogg_count,
        "sample_rate": SAMPLE_RATE,
        "channels": 1,
        "vorbis_quality": VORBIS_QUALITY,
        "total_bytes": total_bytes,
        "sounds": manifest_entries,
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    group_summary = {}
    for g_name, items in sorted(group_accum.items()):
        group_summary[g_name] = {
            "count": len(items),
            "target_dbfs": LOUDNESS_TARGETS_DBFS.get(g_name),
            "mean_active_rms_dbfs": round(sum(i["active_rms_dbfs"] for i in items) / len(items), 2),
            "max_peak_dbfs": round(max(i["peak_dbfs"] for i in items), 2),
            "max_true_peak_dbfs": round(max(i.get("true_peak_dbfs", i["peak_dbfs"]) for i in items), 2),
            "mean_centroid_hz": round(sum(i["spectral_centroid_hz"] for i in items) / len(items), 1),
        }

    from scripts.sound_studio.ai_stem_generator import audit_all_stems

    stem_qc_full = audit_all_stems()
    stem_qc_summary = {
        "ok": bool(stem_qc_full["ok"]),
        "stem_count": int(stem_qc_full["stem_count"]),
        "checked_count": int(stem_qc_full["checked_count"]),
        "missing_files": list(stem_qc_full["missing_files"]),
        "failed_stems": list(stem_qc_full["failed_stems"]),
    }
    masking_report = audit_spectral_masking(manifest_entries)
    multi_divs = [float(e.get("timbral_diversity", 0.0)) for e in manifest_entries if int(e.get("variations", 1)) > 1]
    mean_timbral_diversity = round(sum(multi_divs) / len(multi_divs), 4) if multi_divs else 0.0

    worst_corr = round(max(event_correlations.values(), default=0.0), 4)
    max_true_peak = round(
        max((float(e["metrics"].get("true_peak_dbfs", e["metrics"]["peak_dbfs"])) for e in manifest_entries), default=-96.0),  # type: ignore[index]
        2,
    )
    build_secs = round(time.perf_counter() - t0, 2)
    backend_label = f"CUDA: {gpu_name}" if gpu_name else ("Incremental Cache" if not specs_to_build else "CPU SciPy")

    quality_report: dict[str, object] = {
        "ok": (
            len(failed_files) == 0
            and len(failed_corr_events) == 0
            and len(loop_seam_failures) == 0
            and bool(masking_report["ok"])
            and bool(stem_qc_summary["ok"])
        ),
        "sample_rate": SAMPLE_RATE,
        "channels": 1,
        "vorbis_quality": VORBIS_QUALITY,
        "total_events": len(SOUND_CATALOG),
        "total_ogg_files": total_ogg_count,
        "rebuilt_events": len(specs_to_build),
        "cached_events": len(reused_entries),
        "max_correlation_threshold": MAX_VARIATION_CORR,
        "worst_correlation": worst_corr,
        "mean_timbral_diversity": mean_timbral_diversity,
        "max_true_peak_dbfs": max_true_peak,
        "loop_seam_ok": len(loop_seam_failures) == 0,
        "loop_seam_failures": loop_seam_failures,
        "stem_qc_summary": stem_qc_summary,
        "psychoacoustic_masking": masking_report,
        "build_duration_secs": build_secs,
        "acceleration_backend": backend_label,
        "failed_files": failed_files,
        "failed_correlation_events": failed_corr_events,
        "group_summary": group_summary,
        "event_correlations": event_correlations,
    }

    if update_baseline:
        save_acoustic_baseline(manifest, quality_report, BASELINE_PATH)

    baseline_diff: dict[str, Any] | None = None
    if check_baseline or BASELINE_PATH.is_file():
        baseline = load_acoustic_baseline(BASELINE_PATH)
        if baseline is not None:
            baseline_diff = compare_against_baseline(manifest, quality_report, baseline)
            quality_report["baseline_ok"] = bool(baseline_diff["ok"])
            quality_report["baseline_drifted_count"] = len(baseline_diff["drifted_events"])
            quality_report["baseline_worst_corr_delta"] = baseline_diff["worst_correlation_delta"]

    QUALITY_REPORT_PATH.write_text(json.dumps(quality_report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    build_audition_html(manifest, quality_report, SOUNDS_DIR, AUDITION_HTML_PATH, baseline_diff=baseline_diff)
    BUILD_CACHE_PATH.write_text(
        json.dumps({"engine_fingerprint": engine_fp, "event_hashes": cur_hashes}, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return {"manifest": manifest, "quality_report": quality_report, "baseline_diff": baseline_diff}


def run_checks(*, skip_java_sync: bool = False, check_baseline: bool = False) -> list[str]:
    """Validate all .ogg files, headers (44.1 kHz mono), variations, quality report, sounds.json, subtitles & ModSounds."""
    errors: list[str] = []
    if not SOUNDS_JSON_PATH.is_file():
        return [f"Missing sounds.json at {SOUNDS_JSON_PATH}"]

    try:
        sounds_json = json.loads(SOUNDS_JSON_PATH.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return [f"Invalid sounds.json: {exc}"]

    if not isinstance(sounds_json, dict):
        return ["sounds.json top-level must be an object"]

    expected_ids = set(SOUND_BY_ID.keys())
    actual_ids = set(sounds_json.keys())
    if expected_ids != actual_ids:
        missing = sorted(expected_ids - actual_ids)
        extra = sorted(actual_ids - expected_ids)
        if missing:
            errors.append(f"Missing sound IDs in sounds.json: {missing[:10]}")
        if extra:
            errors.append(f"Unexpected sound IDs in sounds.json: {extra[:10]}")

    referenced_oggs: set[str] = set()
    for sid, entry in sounds_json.items():
        if not isinstance(entry, dict):
            errors.append(f"{sid}: sounds.json entry is not an object")
            continue
        spec = SOUND_BY_ID.get(sid)
        expected_sub = f"subtitles.mycelialmind.{sid}"
        if entry.get("subtitle") != expected_sub:
            errors.append(f"{sid}: expected subtitle '{expected_sub}', got '{entry.get('subtitle')}'")
        snd_list = entry.get("sounds")
        if not isinstance(snd_list, list) or not snd_list:
            errors.append(f"{sid}: empty or invalid sounds array")
            continue
        if spec is not None and len(snd_list) != spec.variations:
            errors.append(f"{sid}: expected {spec.variations} variations in sounds.json, got {len(snd_list)}")
        for item in snd_list:
            if not isinstance(item, dict):
                errors.append(f"{sid}: sound element is not an object")
                continue
            if item.get("type") == "event":
                errors.append(f"{sid}: forbidden vanilla redirect 'type: event'")
            atten = item.get("attenuation_distance")
            if not isinstance(atten, int) or atten < 8 or atten > 64:
                errors.append(f"{sid}: invalid attenuation_distance '{atten}' (expected int in [8, 64])")
            name = str(item.get("name", ""))
            if not name.startswith("mycelialmind:"):
                errors.append(f"{sid}: sound name '{name}' does not use 'mycelialmind:' namespace")
                continue
            rel_sound = name.split(":", 1)[1]
            ogg_path = SOUNDS_DIR / f"{rel_sound}.ogg"
            referenced_oggs.add(ogg_path.name)
            if not ogg_path.is_file():
                errors.append(f"{sid}: missing Ogg file {ogg_path.as_posix()}")
            else:
                info = inspect_ogg_vorbis(ogg_path)
                if not info.get("ok"):
                    errors.append(f"{sid}: invalid Ogg Vorbis header in {ogg_path.name}: {info}")

    # Ensure no stale/orphaned .ogg files linger on disk
    if SOUNDS_DIR.is_dir():
        on_disk_oggs = {p.name for p in SOUNDS_DIR.glob("*.ogg")}
        orphaned = sorted(on_disk_oggs - referenced_oggs)
        if orphaned:
            errors.append(f"Orphaned .ogg files in {SOUNDS_DIR}: {orphaned[:10]} (total {len(orphaned)})")

    # Lang subtitle check
    for label, lang_path in (("en_us", LANG_EN_PATH), ("es_es", LANG_ES_PATH)):
        if not lang_path.is_file():
            errors.append(f"Missing lang file: {lang_path}")
            continue
        lang_data = json.loads(lang_path.read_text(encoding="utf-8"))
        for sid in expected_ids:
            sub_key = f"subtitles.mycelialmind.{sid}"
            if sub_key not in lang_data or not str(lang_data[sub_key]).strip():
                errors.append(f"{label}: missing or empty subtitle key '{sub_key}'")

    # ModSounds.java registration parity
    if not skip_java_sync and MOD_SOUNDS_JAVA.is_file():
        java_text = MOD_SOUNDS_JAVA.read_text(encoding="utf-8")
        reg_ids = set(re.findall(r'register\(\s*"([a-z0-9_]+)"\s*\)', java_text))
        if reg_ids != expected_ids:
            missing_java = sorted(expected_ids - reg_ids)
            extra_java = sorted(reg_ids - expected_ids)
            if missing_java:
                errors.append(f"Missing in ModSounds.java: {missing_java[:10]} (total {len(missing_java)})")
            if extra_java:
                errors.append(f"Extra in ModSounds.java: {extra_java[:10]} (total {len(extra_java)})")

    if not MANIFEST_PATH.is_file():
        errors.append(f"Missing manifest: {MANIFEST_PATH}")

    if not QUALITY_REPORT_PATH.is_file():
        errors.append(f"Missing acoustic quality report: {QUALITY_REPORT_PATH}")
    else:
        try:
            qr = json.loads(QUALITY_REPORT_PATH.read_text(encoding="utf-8"))
            if not qr.get("ok"):
                errors.append(
                    f"Quality report failed: {len(qr.get('failed_files', []))} bad files, "
                    f"{len(qr.get('failed_correlation_events', []))} correlated events"
                )
            if qr.get("total_ogg_files") != len(referenced_oggs):
                errors.append(
                    f"Quality report ogg count mismatch: {qr.get('total_ogg_files')} vs {len(referenced_oggs)} referenced"
                )
        except json.JSONDecodeError as exc:
            errors.append(f"Invalid quality report JSON: {exc}")

    if not AUDITION_HTML_PATH.is_file():
        errors.append(f"Missing local audition console HTML: {AUDITION_HTML_PATH}")

    if check_baseline:
        baseline = load_acoustic_baseline(BASELINE_PATH)
        if baseline is None:
            errors.append(f"Missing acoustic baseline: {BASELINE_PATH}")
        elif MANIFEST_PATH.is_file() and QUALITY_REPORT_PATH.is_file():
            m_data = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
            q_data = json.loads(QUALITY_REPORT_PATH.read_text(encoding="utf-8"))
            diff = compare_against_baseline(m_data, q_data, baseline)
            if not diff["ok"]:
                errors.append(
                    f"Acoustic baseline drift detected: {len(diff['drifted_events'])} drifted, "
                    f"removed={len(diff['removed_events'])}, worst_corr_delta={diff['worst_correlation_delta']}"
                )

    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Mycelial Mind Procedural Sound Studio")
    parser.add_argument("--all", action="store_true", help="Synthesize all .ogg variations, sounds.json, subtitles, quality report & audition HTML.")
    parser.add_argument("--changed-only", action="store_true", help="Incrementally synthesize only sound events whose code, spec, IR, or AI stem changed.")
    parser.add_argument("--only", type=str, default="", help="Comma-separated sound_ids to regenerate.")
    parser.add_argument("--category", type=str, default="", help="Filter sound synthesis to a single category.")
    parser.add_argument("--group", type=str, default="", help="Filter sound synthesis to a single loudness group.")
    parser.add_argument("--update-baseline", action="store_true", help="Save/update exports/sound_baseline.json after generation.")
    parser.add_argument("--check-baseline", action="store_true", help="Verify current acoustic metrics against exports/sound_baseline.json.")
    parser.add_argument("--check", action="store_true", help="Validate .ogg assets, headers, quality report, sounds.json, subtitles & ModSounds parity.")
    parser.add_argument("--regen-ai-stems", action="store_true", help="Regenerate cached RTX 4060 Stable Audio Open Small stems in scripts/sound_studio/ai_stems/.")
    parser.add_argument("--skip-java-sync", action="store_true", help="Skip ModSounds.java parity check.")
    parser.add_argument("--workers", type=int, default=8, help="Parallel encoding workers.")
    args = parser.parse_args(argv)

    only_ids = {s.strip() for s in args.only.split(",") if s.strip()} if args.only else None
    should_build = bool(args.all or args.changed_only or only_ids or args.category or args.group)

    if not should_build and not args.check and not args.regen_ai_stems and not args.update_baseline and not args.check_baseline:
        parser.print_help()
        return 1

    if args.regen_ai_stems and not should_build:
        from scripts.sound_studio.ai_stem_generator import ensure_ai_stems

        stems = ensure_ai_stems(force_regen=True)
        print(f"[Sound Studio] Regenerated {len(stems)} AI stems on GPU.")

    if should_build:
        res = generate_all(
            workers=args.workers,
            regen_ai_stems=args.regen_ai_stems,
            changed_only=args.changed_only,
            only_ids=only_ids,
            category=args.category or None,
            loudness_group=args.group or None,
            update_baseline=args.update_baseline,
            check_baseline=args.check_baseline,
        )
        manifest = res["manifest"]
        qr = res["quality_report"]
        print(
            f"[Sound Studio] Generated {manifest['ogg_count']} mono Ogg Vorbis files across {manifest['count']} events "
            f"(rebuilt={qr['rebuilt_events']}, cached={qr['cached_events']}, {manifest['total_bytes'] / 1024.0:.1f} KB total @ {SAMPLE_RATE} Hz, -q:a {VORBIS_QUALITY}) "
            f"in {qr['build_duration_secs']}s [{qr['acceleration_backend']}]."
        )
        print(
            f"[Sound Studio] Acoustic QA ok={qr['ok']} | worst_corr={qr['worst_correlation']} | div={qr.get('mean_timbral_diversity')} "
            f"| max_tp={qr['max_true_peak_dbfs']} dBFS | seam_ok={qr['loop_seam_ok']} "
            f"| masking_ok={(qr.get('psychoacoustic_masking') or {}).get('ok')} | stems_ok={(qr.get('stem_qc_summary') or {}).get('ok')} "
            f"| failed_files={len(qr['failed_files'])}"
        )
    elif args.update_baseline:
        if not MANIFEST_PATH.is_file() or not QUALITY_REPORT_PATH.is_file():
            print("ERROR: Run --all before --update-baseline", file=sys.stderr)
            return 1
        save_acoustic_baseline(
            json.loads(MANIFEST_PATH.read_text(encoding="utf-8")),
            json.loads(QUALITY_REPORT_PATH.read_text(encoding="utf-8")),
            BASELINE_PATH,
        )
        print(f"[Sound Studio] Updated acoustic baseline at {BASELINE_PATH}")

    if args.check or args.check_baseline:
        errors = run_checks(skip_java_sync=args.skip_java_sync, check_baseline=args.check_baseline)
        if errors:
            for err in errors:
                print(f"ERROR: {err}", file=sys.stderr)
            return 1
        print(f"[Sound Studio] All {len(SOUND_CATALOG)} events ({sum(s.variations for s in SOUND_CATALOG)} .ogg files @ {SAMPLE_RATE} Hz mono), quality metrics & subtitles PASSED.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
