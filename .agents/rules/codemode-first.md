---
trigger: always_on
description: Política obligatoria para usar FastMCP CodeMode (mycelial-mind-dev) al auditar todas las suites, explorar el mod y operar el juego Minecraft/NeoForge
---

# Política Obligatoria: FastMCP `CodeMode` (`mycelial-mind-dev`)

Cuando **`mycelial-mind-dev`** ([scripts/mcp/server.py](file:///c:/Users/Alex/Documents/Mod%20Minecraft/scripts/mcp/server.py)) esté conectado, **usa siempre FastMCP `CodeMode` (`search`, `get_schema`, `execute`) para auditar suites, inspeccionar registros/activos y operar Minecraft/NeoForge** en vez de lanzar scripts manuales por consola. Guía completa: [.agents/skills/mycelial-codemode/SKILL.md](file:///c:/Users/Alex/Documents/Mod%20Minecraft/.agents/skills/mycelial-codemode/SKILL.md).

## 1. Operación en `execute` (`MontySandboxProvider`)
- Clientes MCP activos (`.mcp.json` en raíz, Antigravity y Codex): cada cliente lanza su propio proceso `stdio` (`get_bridge_health()` los distingue por `instance_id`/`instance_pid`); no lances `run_gradle_task` ni `run_verify_mod` en paralelo desde dos clientes.
- Invoca con `await call_tool("nombre", {...})` (devuelven `dict`); paraleliza checks independientes con `asyncio.gather(...)`.
- Límite duro por `execute()`: **50 `call_tool()`**, **600 s** y **100 MB**. Sin I/O de disco/red directo; devuelve un `dict` resumen compacto.

## 2. Equivalencias Obligatorias

| En lugar de ejecutar manualmente… | Usar en `CodeMode` (`execute`) |
| --- | --- |
| `task release:dry-run` / `publishMods` | `run_gradle_task(tasks=["publishMods"])`; CodeMode fuerza `publishDryRun=true` y rechaza propiedades de proyecto/sistema |
| `.\gradlew.bat build` / `test` / `runData` / `runGameTestServer` (`-PisolatedGameTests=true`) / `runVisualSmoke` (`-P*VisualSmoke=true`) / `runStructureVisualSmoke` / `runHiveLifecycleSmoke` | `run_gradle_task` (`isolated_gametests=True`, `test_filter=...`, `visual_smoke_mode="default\|dialect\|relic\|particle\|expedition"`) + `get_latest_junit_report` |
| `.\scripts\Verify-Mod.ps1 ...` / `verify_cache.py` / `task verify*` | `run_verify_mod` / `manage_verify_cache` |
| `python scripts/model_studio/build_models.py ...` + `unittest` | `run_model_studio` / `get_uv_manifest` |
| `python scripts/texture_studio/build_textures.py ...` + `unittest` | `run_texture_studio` / `audit_mod_assets` |
| `python scripts/particle_studio/build_particles.py ...` + `unittest` | `run_particle_studio` / `audit_mod_assets` |
| `python scripts/sound_studio/build_sounds.py ...` + `unittest` (v5) | `run_sound_studio` / `inspect_sound_event` / `audit_sound_masking` / `audit_mod_assets` |
| `python scripts/codex_studio/build_codex.py ...` + `unittest` | `run_codex_studio` |
| `python scripts/build_structures.py` | `run_structure_studio` |
| `python scripts/validate_agent_skills.py` | `validate_skills` / `list_skills` / `get_skill_guide` |
| `python scripts/visual_regression.py` / `structure_visual_check.py` | `check_visual_regression` / `check_structure_visual_regression` / `get_latest_structure_visual_smoke` |
| `python scripts/rcon_verify.py ...` o comandos `/mycelial ...` | `run_rcon_verify_suite` / `rcon_command` / `run_rcon_batch` / `get_live_hive_status` / `get_live_colony_status` / `run_live_verify_scenario` |
| Arrancar/parar servidor smoke o escenarios en vivo + reportes | `start_smoke_server` / `smoke_server_status` / `stop_smoke_server` / `start_live_session` / `stop_live_session` / `run_live_scenario` / `get_latest_live_report` / `compare_live_reports` |
| `Invoke-McPilot.ps1` / `Prepare-McPilotClient.ps1` / `Build-McPilotCompat.ps1` / `Install-McPilotCompat.ps1` / `Stop-McPilotClient.ps1` | `mcpilot_command` / `prepare_mcpilot_client` / `build_mcpilot_compat` / `install_mcpilot_compat` / `stop_mcpilot_client` / `collect_spark_tps` / `capture_mcpilot_screenshot` |
| `jcmd -l` / `Invoke-Jfr.ps1` / `jfr summary` | `list_java_processes` / `run_jfr` / `summarize_jfr` |
| Salud, etiquetas, métricas, caché y comparativas del bridge | `get_bridge_health` / `list_tool_tags` / `get_bridge_metrics` / `clear_bridge_cache` / `capture_audit_snapshot` / `diff_audit_snapshots` |
| Triaje de `/mycelial-diagnostics` y lectura de logs/IA/tests | `collect_diagnostic_bundle` / `diagnose_symptom` (`audio_efx`, `hyphal_physics`, `datagen_compat`, `colony_dialects`, ...) / `get_diagnostics_guide` / `read_diagnostic_logs` / `list_diagnostic_artifacts` / `get_registered_content` / `get_hive_ai_overview` / `list_gametests` / `list_unit_tests` (`suite_filter`) / `get_latest_worldgen_audit` |

## 3. Operación en vivo, auditoría y cumplimiento
- **Sesión aislada (`start_smoke_server` / `start_live_session`):** nunca usar mundos de usuario; presets de `run_live_scenario`: `progression_ramp`, `swarm_load`, `diplomacy_and_memory`, `homeostasis_and_dimension`, `colony_and_dialects`, `cycles_and_endgame`, `sinkhole_and_worldgen`, `audio_and_ai`, `full_smoke_matrix`, `custom`. Cerrar siempre con `stop_live_session(..., cleanup_dir=True)`.
- **Receta estándar de cierre:** ver §4 de [SKILL.md](../skills/mycelial-codemode/SKILL.md) (paraleliza `get_bridge_health`, `validate_skills`, estudios y `audit_mod_assets` con `asyncio.gather`, seguido de `run_verify_mod` y lectura filtrada de `latest.log`).
- **Cumplimiento y fallback:** (1) encadena descubrimiento + ejecución en un solo `execute`; (2) usa `search`/`get_schema` bajo demanda; (3) tras editar `scripts/mcp/server.py`, valida con `scripts/mcp/.venv/Scripts/python.exe scripts/mcp/test_server.py`; (4) usa `.bat`/`.ps1`/`.py` directos por consola **solo** si `mycelial-mind-dev` no está conectado o al depurar `server.py`.
