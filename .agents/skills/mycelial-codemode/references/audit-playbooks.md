# Playbooks de Auditoría y Anti-Patrones en `CodeMode`

## Playbook 1 — Auditoría Pre-Commit Paralela
```python
import asyncio

skills, models, textures, particles, sounds, masking, codex, assets, cache = await asyncio.gather(
    call_tool("validate_skills", {}),
    call_tool("run_model_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_texture_studio", {"check_only": True, "pbr": True, "run_tests": True}),
    call_tool("run_particle_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_sound_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_sound_masking", {"top_n": 8}),
    call_tool("run_codex_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_mod_assets", {}),
    call_tool("manage_verify_cache", {"action": "status", "scope": "all"}),
)
verify = await call_tool("run_verify_mod", {"include_server_smoke": True, "no_daemon": False})
return {
    "skills_ok": skills["ok"],
    "models_ok": models["ok"],
    "textures_ok": textures["ok"],
    "particles_ok": particles["ok"],
    "sounds_ok": sounds["ok"],
    "masking_ok": masking["ok"],
    "codex_ok": codex["ok"],
    "assets_ok": assets["ok"],
    "all_cached": cache.get("all_cached"),
    "verify_ok": verify["ok"],
    "summary": verify.get("summary_markers", []),
    "errors": verify.get("error_lines", []),
}
```

## Playbook 2 — Cambio en Modelos 3D / Texturas PBR / Estructuras
```python
m = await call_tool("run_model_studio", {"check_only": False, "run_tests": True})
t = await call_tool("run_texture_studio", {"check_only": False, "pbr": True, "export_extras": True, "run_tests": True})
s = await call_tool("run_structure_studio", {})
v = await call_tool("run_gradle_task", {"tasks": ["runVisualSmoke"]})
vis = await call_tool("check_visual_regression", {"include_diff_regions": True, "include_lpips": True})
return {"models": m["ok"], "textures": t["ok"], "structures": s["ok"], "smoke": v["ok"], "vis": vis}
```

## Playbook 3 — Depuración de IA / GameTests / JUnit
```python
ai = await call_tool("get_hive_ai_overview", {})
ut = await call_tool("list_unit_tests", {"suite_filter": "HiveBrainEngineUnitTest"})
gt = await call_tool("list_gametests", {"batch_filter": "hive_brain"})
res = await call_tool("run_gradle_task", {
    "tasks": ["test", "runGameTestServer"],
    "isolated_gametests": True,
    "test_filter": "HiveBrainEngineUnitTest",
})
junit = await call_tool("get_latest_junit_report", {})
return {"enums": ai["enums"], "unit_tests": ut["count"], "gametests": gt["count"], "gradle_ok": res["ok"], "junit": junit}
```

## Playbook 4 — Diagnóstico Guiado por Síntoma y Comparativa A/B (`/mycelial-diagnostics`)
```python
# 1. Paso 'Delimitar y preservar' + ruta exacta por síntoma
bundle = await call_tool("collect_diagnostic_bundle", {"log_tail_lines": 40, "max_artifacts": 15})
triage = await call_tool("diagnose_symptom", {"symptom": "tps_lag", "include_live_state": True})

# 2. Reproducir y medir en sesión aislada con -Pdiagnostics + JFR/spark
srv = await call_tool("start_smoke_server", {"world_type": "flat", "enable_diagnostics": True, "timeout_secs": 180})
if not srv.get("ok"):
    return {"bundle": bundle, "triage": triage, "srv": srv}
sid = srv["session_id"]
try:
    baseline = await call_tool("run_live_scenario", {
        "preset": "progression_ramp",
        "session_id": sid,
        "capture_perf": True,
        "jfr_duration_secs": 10,
    })
    load_run = await call_tool("run_live_scenario", {
        "preset": "swarm_load",
        "session_id": sid,
        "capture_perf": True,
        "jfr_duration_secs": 10,
    })
finally:
    stop = await call_tool("stop_smoke_server", {"session_id": sid, "cleanup_dir": True})

delta = await call_tool("compare_live_reports", {
    "before_path": baseline["report_path"],
    "after_path": load_run["report_path"],
})
return {
    "mc_pid": bundle["java_processes"]["minecraft_pid"],
    "recommended_tools": triage["tools"],
    "baseline_jfr": (baseline.get("perf") or {}).get("jfr_recorded"),
    "delta": delta.get("deltas"),
    "graceful_stop": stop.get("graceful_rcon_stop"),
}
```

## Playbook 5 — Colonia Soberana, Dialectos, GameTests Aislados y UI Nexo
```python
# 1. Enumeraciones/clases de colony/ + audio dedicado de dialectos y GameTests aislados
ai = await call_tool("get_hive_ai_overview", {})
snd = await call_tool("inspect_sound_event", {"sound_id": "colony_dialect_mastered"})
gt = await call_tool("run_gradle_task", {
    "tasks": ["test", "runGameTestServer"],
    "isolated_gametests": True,  # inyecta -PisolatedGameTests=true
    "timeout_secs": 300,
})
ui = await call_tool("run_gradle_task", {
    "tasks": ["runVisualSmoke"],
    "visual_smoke_mode": "dialect",
    "timeout_secs": 240,
})
return {
    "colony_enums": {k: len(v) for k, v in ai.get("colony_enums", {}).items()},
    "dialect_sound_ok": snd.get("ok"),
    "gametests_ok": gt["ok"],
    "dialect_ui_ok": ui["ok"],
}
```

## Playbook 6 — Audio OpenAL EFX, Física de Hifas y DataGen/Compat
```python
import asyncio

mask, efx, hyphal, dg = await asyncio.gather(
    call_tool("audit_sound_masking", {"top_n": 10}),
    call_tool("run_gradle_task", {"tasks": ["test"], "test_filter": "OpenAlEfxLoopbackTest"}),
    call_tool("run_gradle_task", {"tasks": ["test"], "test_filter": "HyphalPhysicsTest"}),
    call_tool("run_gradle_task", {"tasks": ["runData", "test"], "test_filter": "DataGenIntegrityTest"}),
)
return {
    "masking_ok": mask.get("ok"),
    "efx_ok": efx.get("ok"),
    "hyphal_ok": hyphal.get("ok"),
    "datagen_ok": dg.get("ok"),
}
```

Catálogo completo de suites JUnit (`147`), batches `@GameTest` (`201`) y modos visuales: [test-matrix.md](test-matrix.md).

### Alternativas Reproducibles (`Taskfile.yml` + `uv`) y Fallback Gradle Offline
- **Aliases locales (`Taskfile.yml` / CI GitHub Actions):** `task verify:assets` (usa `uv` para modelos/texturas/partículas), `task verify`, `task verify:server`, `task verify:visual`, `task verify:renovate`, `task release:dry-run`.
- **Fallback si falla el lock del wrapper global (`C:\Users\Alex\.gradle`):** ejecuta Gradle directamente en modo offline apuntando a `.gradle-user-home` local y JDK 21 (`$env:GRADLE_USER_HOME="$PWD\.gradle-user-home"; $env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat --offline --no-daemon test runGameTestServer -PisolatedGameTests=true`).

## Anti-Patrones a Evitar
1. **Devolver logs crudos enteros:** filtra siempre con `read_diagnostic_logs(..., filter_pattern="ERROR|WARN|FAIL|Exception")` o usa `error_lines`.
2. **Olvidar `stop_live_session`:** cierra siempre el servidor smoke/cliente al terminar el bloque `execute`.
3. **Llamar secuencialmente checks independientes:** usa `asyncio.gather(...)` para `validate_skills`, `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio`, `audit_sound_masking`, `run_codex_studio` y `audit_mod_assets`.
4. **Confiar en caché tras editar archivos manualmente:** ejecuta `clear_bridge_cache()` antes de `capture_audit_snapshot("after")`.
