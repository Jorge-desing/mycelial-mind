# Mycelial Mind — FastMCP CodeMode Server

Servidor MCP local para que agentes de IA operen sobre el repositorio **Mycelial Mind** usando **[FastMCP CodeMode](https://gofastmcp.com/docs/servers/transforms/code-mode)** (`MontySandboxProvider`).

## Por qué CodeMode
En lugar de exponer todos los esquemas de herramientas de golpe en el contexto del agente, `CodeMode` expone únicamente meta-herramientas de descubrimiento (`search`, `get_schema`, `tags`) y ejecución (`execute`). El agente busca lo que necesita bajo demanda y encadena múltiples llamadas en un único bloque Python dentro del sandbox, devolviendo solo el resultado final.

## Herramientas encapsuladas (`scripts/mcp/server.py`)
- **Exploración, registros y metadata:** `list_mod_sources`, `search_mod_code`, `read_mod_file` (tope seguro de 400 líneas), `get_registered_content`, `get_hive_ai_overview` (`hive/` + `colony/` + `HiveBrain`), `get_mixin_overview` (`mycelialmind.mixins.json` + paridad `mixin/**/*.java` + `neoforge.mods.toml`), `get_memory_tail`, `list_gametests` (`batch_filter`, 218 tests / 62 batches), `list_unit_tests` (`suite_filter`, 207 tests JUnit 6 / 21 suites), `get_latest_junit_report`, `get_latest_pitest_report`, `get_uv_manifest` (`model_id`), `audit_mod_assets` (paridad bilingüe dual-root, claves estáticas `Component.translatable` + `sounds.json`, `blockstates_count`, `sounds_json_count`, `model_atlas`, `model_json_counts` y sincronización UV), `list_datapack_content`, `get_gradle_config`
- **Skills del proyecto:** `list_skills`, `get_skill_guide` (`mycelial-codemode`, `mycelial-ai-hive`, `mycelial-diagnostics`, `mycelial-model-studio`, `mycelial-particle-studio`, `mycelial-sound-studio`, `mycelial-texture-studio`, `neoforge-gametest`), `validate_skills` (recarga automática del validador en memoria)
- **Estudios y QA visual:** `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio` v9 (`only`, `changed_only`, `check_baseline`, `update_baseline`), `inspect_sound_event`, `audit_sound_masking`, `run_codex_studio`, `run_structure_studio` (`build_structures.py`), `check_visual_regression` (`include_diff_regions`, `include_lpips`), `check_structure_visual_regression` (`structure_visual_check.py`), `get_latest_structure_visual_smoke`
- **Gradle y suite completa:** `run_gradle_task` (`build`, `runData`, `test` con `test_filter`, `runGameTestServer` con `isolated_gametests=True` → `-PisolatedGameTests=true`, `runHiveLifecycleSmoke`, `runVisualSmoke` con `visual_smoke_mode="default|dialect|relic|particle|expedition"`, `runAnimationVisualSmoke` con `animation_pair=...`, `runStructureVisualSmoke`, `compileJava`, `stageMcPilotDependency`, `publishMods` solo en simulación forzada), `run_verify_mod` (`include_server_smoke`, `include_visual_smoke`, `include_worldgen_audit`, `worldgen_audit_radius`, `force_regenerate`, `no_daemon=False`), `manage_verify_cache` (`status`/`clear` sobre `build/.verify-stamps/`)
- **Observabilidad, caché, repo y snapshots:** `get_bridge_health` (`server_file_modified`, `restart_recommended`), `get_bridge_metrics`, `get_repo_status`, `run_bridge_self_tests`, `list_tool_tags`, `clear_bridge_cache`, `capture_audit_snapshot`, `diff_audit_snapshots`
- **Ciclo de vida servidor+cliente, diagnósticos, `spark`, RCON y JFR:** `collect_diagnostic_bundle`, `diagnose_symptom` (incl. `colony_dialects`, `expeditions_cycles`, `audio_efx`, `hyphal_physics`, `datagen_compat`, `mixins_client`), `get_diagnostics_guide` (`overview`/`tools`/`codemode`), `start_smoke_server(enable_diagnostics)`, `smoke_server_status`, `stop_smoke_server`, `start_live_session(enable_diagnostics)`, `stop_live_session`, `run_live_scenario` (incl. `colony_and_dialects`, `cycles_and_endgame`, `sinkhole_and_worldgen`, `audio_and_ai`), `collect_spark_tps`, `capture_mcpilot_screenshot`, `get_latest_live_report`, `compare_live_reports`, `get_live_hive_status`, `get_live_colony_status`, `run_live_verify_scenario` (`24` escenarios), `list_diagnostic_artifacts` (`glob_pattern`, `max_results`, poda interna en `subdir=""`), `read_diagnostic_logs`, `get_latest_worldgen_audit`, `list_java_processes` (`jcmd -l`), `run_jfr`, `summarize_jfr`, `mcpilot_command`, `prepare_mcpilot_client`, `build_mcpilot_compat`, `install_mcpilot_compat`, `stop_mcpilot_client`, `rcon_command`, `run_rcon_batch`, `run_rcon_verify_suite`

## Instalación
```powershell
python -m venv scripts/mcp/.venv
uv pip install --system-certs --python scripts/mcp/.venv/Scripts/python.exe -r scripts/mcp/requirements.txt
```
*(Si usas `pip` estándar en lugar de `uv`: `.\scripts\mcp\.venv\Scripts\python.exe -m pip install -r scripts/mcp/requirements.txt`.)*

## Verificación rápida
```powershell
.\scripts\mcp\.venv\Scripts\python.exe -m unittest scripts/mcp/test_live_lifecycle.py scripts/mcp/test_bridge_unit.py
.\scripts\mcp\.venv\Scripts\python.exe scripts/mcp/test_server.py
```

## Registro en cliente MCP (Antigravity / Claude / Cursor / Codex)
Agrega esta entrada en tu configuración de servidores MCP (`mcp_config.json`):

```json
{
  "mcpServers": {
    "mycelial-mind-dev": {
      "command": "c:\\Users\\Alex\\Documents\\Mod Minecraft\\scripts\\mcp\\.venv\\Scripts\\python.exe",
      "args": [
        "c:\\Users\\Alex\\Documents\\Mod Minecraft\\scripts\\mcp\\server.py"
      ]
    }
  }
}
```

## Coexistencia entre clientes MCP (`.mcp.json`, Antigravity, Codex)

Cada cliente MCP levanta su propio subproceso `stdio` (`scripts/mcp/.venv/Scripts/python.exe` → `scripts/mcp/server.py`).

| Cliente | Archivo de configuración | Notas |
| --- | --- | --- |
| Workspace estándar | `.mcp.json` (raíz del repo) | Ya configurado con `mycelial-mind-dev` |
| Antigravity | `~/.gemini/config/mcp_config.json` | Usa rutas absolutas al `.venv` y `server.py` |
| Codex | `~/.codex/config.toml` | Bloque `[mcp_servers.mycelial-mind-dev]` en modo `stdio` |

- **Verificación rápida:** dentro de `execute`, llama `get_bridge_health()` → debe devolver `ok=true`, `tool_count=70`, `java_home_detected` con JDK 21 e `instance_id`/`instance_pid` propios del proceso (más `server_file_modified`/`restart_recommended`). El job `Validate automation config` de GitHub Actions ejecuta además `validate_agent_skills.py`, `py_compile`, `test_live_lifecycle.py` y `test_bridge_unit.py`.
- **Regla de coexistencia y lock de Gradle:** aunque varios clientes estén conectados a la vez, `run_gradle_task` y `run_verify_mod` serializan llamadas en proceso y usan `artifacts/diagnostics/.tmp/gradle.lock` (`lock_wait_secs=30`, limpieza automática de locks huérfanos); si otro cliente vivo sigue compilando tras expirar la espera, devuelven un error estructurado con `owner_pid`/`operation`.

## Recursos MCP y esquemas de las meta-herramientas

- `get_schema` resuelve también las propias meta-herramientas: `get_schema(["search", "get_schema", "execute"])` devuelve sus contratos en lugar de `Tools not found`.
- Recursos de solo lectura (visibles en `list_mcp_resources` / `list_mcp_resource_templates`, 9 en total): `mycelial://bridge/health`, `mycelial://bridge/tools`, `mycelial://bridge/tags`, `mycelial://repo/status`, `mycelial://mixins/overview`, `mycelial://memory/tail`, `mycelial://skills`, `mycelial://skills/{skill_name}` y `mycelial://skills/{skill_name}/{document*}` (`*` acepta rutas como `references/test-matrix.md`).
- `get_bridge_health()` incluye `instance_id`, `instance_pid`, `instance_started_utc` e `instance_uptime_secs`: con varios clientes (Antigravity, Codex, otros clientes MCP) cada uno tiene su propio proceso y así se distinguen.
