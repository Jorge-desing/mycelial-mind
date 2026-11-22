---
trigger: always_on
description: Política obligatoria para usar FastMCP CodeMode (mycelial-mind-dev) al auditar todas las suites, explorar el mod y operar el juego Minecraft/NeoForge
---

# Política Obligatoria: FastMCP `CodeMode` (`mycelial-mind-dev`)

Cuando el servidor MCP **`mycelial-mind-dev`** ([scripts/mcp/server.py](file:///c:/Users/Alex/Documents/Mod%20Minecraft/scripts/mcp/server.py)) esté conectado, **es obligatorio utilizar FastMCP `CodeMode` (`search`, `get_schema`, `execute`) para auditar todas las suites de pruebas, consultar registros/activos y operar el mod/juego**, en lugar de invocar manualmente por consola los scripts individuales que reemplaza.

Para recetas completas y detalle del sandbox, consultar [.agents/skills/mycelial-codemode/SKILL.md](file:///c:/Users/Alex/Documents/Mod%20Minecraft/.agents/skills/mycelial-codemode/SKILL.md).

## 1. Cómo operar `execute` (`MontySandboxProvider`)
- **Nombres en DeepSeek Harness:** el servidor se monta vía `mcp-manager` (`~/.dsh/profiles/desktop/cordis.patch.yml`) y el modelo lo invoca como `mcp__mycelial-mind-dev__search`, `...__get_schema` y `...__execute`. Detalle en `.agents/skills/mycelial-codemode/references/deepseek-harness.md`.
- Llama herramientas con `await call_tool("nombre", {...})` (todas devuelven `dict`).
- Usa `import asyncio` + `await asyncio.gather(...)` para paralelizar comprobaciones independientes (`validate_skills`, `run_model_studio`, `run_texture_studio`, `run_sound_studio`, `audit_mod_assets`).
- **Límite de sandbox:** cada bloque `execute()` permite como máximo **50 llamadas a `call_tool()`** (`max_feed_duration_secs=600.0`, `max_memory=100 MB`). Divide orquestaciones largas en bloques `execute()` separados.
- Devuelve siempre un `dict` resumen compacto; no vuelques logs completos ni uses imports de archivo/red directos del host dentro de `execute`.

## 2. Equivalencias Obligatorias

| En lugar de ejecutar manualmente… | Usar en `CodeMode` (`execute`) |
| --- | --- |
| `.\gradlew.bat build` / `test` / `pitest` / `runData` / `runGameTestServer` / `runVisualSmoke` / `runStructureVisualSmoke` / `runHiveLifecycleSmoke` | `run_gradle_task` (+ `get_latest_junit_report` / `get_latest_pitest_report`) |
| `.\scripts\Verify-Mod.ps1 ...` / `verify_cache.py` | `run_verify_mod` / `manage_verify_cache` |
| `python scripts/model_studio/build_models.py ...` + `unittest` | `run_model_studio` / `get_uv_manifest` |
| `python scripts/texture_studio/build_textures.py ...` + `unittest` | `run_texture_studio` / `audit_mod_assets` |
| `python scripts/particle_studio/build_particles.py ...` + `unittest` | `run_particle_studio` / `audit_mod_assets` |
| `python scripts/sound_studio/build_sounds.py ...` + `unittest` | `run_sound_studio` / `audit_mod_assets` |
| `python scripts/build_structures.py` | `run_structure_studio` |
| `python scripts/validate_agent_skills.py` | `validate_skills` / `list_skills` / `get_skill_guide` |
| `python scripts/visual_regression.py` / `structure_visual_check.py` | `check_visual_regression` / `check_structure_visual_regression` / `get_latest_structure_visual_smoke` |
| `python scripts/rcon_verify.py ...` o comandos `/mycelial ...` | `run_rcon_verify_suite` / `rcon_command` / `run_rcon_batch` / `get_live_hive_status` / `run_live_verify_scenario` |
| Arrancar/parar servidor smoke o escenarios en vivo + reportes | `start_smoke_server` / `smoke_server_status` / `stop_smoke_server` / `start_live_session` / `stop_live_session` / `run_live_scenario` / `get_latest_live_report` / `compare_live_reports` |
| `Invoke-McPilot.ps1` / `Prepare-McPilotClient.ps1` / `Build-McPilotCompat.ps1` / `Install-McPilotCompat.ps1` / `Stop-McPilotClient.ps1` | `mcpilot_command` / `prepare_mcpilot_client` / `build_mcpilot_compat` / `install_mcpilot_compat` / `stop_mcpilot_client` / `collect_spark_tps` / `capture_mcpilot_screenshot` |
| `jcmd -l` / `Invoke-Jfr.ps1` / `jfr summary` | `list_java_processes` / `run_jfr` / `summarize_jfr` |
| Salud, etiquetas, métricas, caché y comparativas del bridge | `get_bridge_health` / `list_tool_tags` / `get_bridge_metrics` / `clear_bridge_cache` / `capture_audit_snapshot` / `diff_audit_snapshots` |
| Triaje de `/mycelial-diagnostics` y lectura de logs/IA/tests | `collect_diagnostic_bundle` / `diagnose_symptom` / `get_diagnostics_guide` / `read_diagnostic_logs` / `list_diagnostic_artifacts` / `get_registered_content` / `get_hive_ai_overview` / `list_gametests` / `list_unit_tests` / `get_latest_worldgen_audit` |

## 3. Cómo Manejar el Mod y el Juego desde `CodeMode`
- **Sesión aislada y escenarios (`start_smoke_server` / `run_live_scenario`):** usa `start_smoke_server` o `start_live_session` (nunca mundos de usuario) y `run_live_scenario(preset="progression_ramp|swarm_load|diplomacy_and_memory|homeostasis_and_dimension|full_smoke_matrix|custom", capture_perf=True)` con `stop_live_session(..., cleanup_dir=True)`.
- **Comandos del mod en servidor vivo (`rcon_command` / `run_rcon_batch` / `get_live_hive_status` / `run_live_verify_scenario`):** `mycelial status`, `mycelial reset`, `mycelial stimulus <MELEE|PROJECTILE|FIRE|EXPLOSION|MAGIC> <0.1..500.0>`, `mycelial symbiosis <status|clear|grant <mutagen>>`, `mycelial diplomacy <strain> <-100..100>`, `mycelial schism <strain>`, `mycelial habits`, `mycelial routes`, `mycelial homeostasis <status|stress <0..100>|sedate>`, `mycelial worldgen audit [radius] [grid]`, `mycelial verify <escenario|all>`.
- **Cliente real / UI / HUD / `spark` / capturas (`mcpilot_command`):** `prepare_mcpilot_client` → `mcpilot_command(["client", "launch", "mycelial-1211", "--no-server"])` (o `start_live_session(launch_client=True, enable_diagnostics=True)`) → `collect_spark_tps` / `capture_mcpilot_screenshot` / `chat command` / `position` → `stop_mcpilot_client` / `stop_live_session`.
- **Escenarios automáticos de motor (`run_gradle_task`):** `runGameTestServer`, `runHiveLifecycleSmoke`, `runVisualSmoke`, `runStructureVisualSmoke`, `test`, `pitest`.

## 4. Plantilla Estándar de Auditoría Completa en `execute`
```python
import asyncio

skills, models, textures, sounds, assets, cache = await asyncio.gather(
    call_tool("validate_skills", {}),
    call_tool("run_model_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_texture_studio", {"check_only": True, "pbr": True, "run_tests": True}),
    call_tool("run_sound_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_mod_assets", {}),
    call_tool("manage_verify_cache", {"action": "status", "scope": "all"}),
)
vis = await call_tool("check_visual_regression", {})
verify = await call_tool("run_verify_mod", {"include_server_smoke": False, "include_visual_smoke": False, "no_daemon": False})
log_errs = await call_tool("read_diagnostic_logs", {"log_path": "run/logs/latest.log", "tail_lines": 50, "filter_pattern": "ERROR|WARN|FAIL|Exception"})
return {
    "skills_ok": skills["ok"],
    "all_cached": cache.get("all_cached"),
    "models_ok": models["ok"],
    "textures_ok": textures["ok"],
    "sounds_ok": sounds["ok"],
    "assets_ok": assets["ok"],
    "visual_ok": vis.get("ok"),
    "verify_mod_ok": verify["ok"],
    "error_lines": verify.get("error_lines", []),
    "log_errors": log_errs.get("matched_lines", 0),
}
```

## 5. Reglas de Cumplimiento y Fallback
1. **Un solo turno de orquestación:** Encadena descubrimiento, ejecución de suites y lectura de errores de log dentro de `execute`.
2. **Descubrimiento bajo demanda:** Usa `search` y `get_schema` para confirmar nombres o argumentos; `get_bridge_health` confirma versión y JDK 21 en <1 ms.
3. **Autoprueba del servidor MCP:** Cuando se modifique `scripts/mcp/server.py`, verifica el propio servidor ejecutando `scripts/mcp/.venv/Scripts/python.exe scripts/mcp/test_server.py`.
4. **Fallback explícito:** Ejecuta `.bat`, `.ps1` o `.py` directamente por consola **únicamente** si `mycelial-mind-dev` no está conectado en la sesión actual o al depurar `scripts/mcp/server.py`.
