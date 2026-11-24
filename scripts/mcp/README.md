# Mycelial Mind — FastMCP CodeMode Server

Servidor MCP local para que agentes de IA operen sobre el repositorio **Mycelial Mind** usando **[FastMCP CodeMode](https://gofastmcp.com/docs/servers/transforms/code-mode)** (`MontySandboxProvider`).

## Por qué CodeMode
En lugar de exponer todos los esquemas de herramientas de golpe en el contexto del agente, `CodeMode` expone únicamente meta-herramientas de descubrimiento (`search`, `get_schema`, `tags`) y ejecución (`execute`). El agente busca lo que necesita bajo demanda y encadena múltiples llamadas en un único bloque Python dentro del sandbox, devolviendo solo el resultado final.

## Herramientas encapsuladas (`scripts/mcp/server.py`)
- **Exploración, registros y metadata:** `list_mod_sources`, `search_mod_code`, `read_mod_file` (tope seguro de 400 líneas), `get_registered_content`, `get_hive_ai_overview` (`hive/` + `colony/`), `get_memory_tail`, `list_gametests` (soporta `batch_filter`), `list_unit_tests` (JUnit 5/6), `get_latest_junit_report`, `get_latest_pitest_report`, `get_uv_manifest` (soporta `model_id`), `audit_mod_assets` (paridad bilingüe, claves estáticas `Component.translatable` + `sounds.json`, `blockstates_count`, `sounds_json_count`, `model_atlas`, `model_json_counts` y sincronización UV), `list_datapack_content`, `get_gradle_config`
- **Skills del proyecto:** `list_skills`, `get_skill_guide` (`mycelial-codemode`, `mycelial-ai-hive`, `mycelial-diagnostics`, `mycelial-model-studio`, `mycelial-particle-studio`, `mycelial-sound-studio`, `mycelial-texture-studio`, `neoforge-gametest`), `validate_skills`
- **Estudios y QA visual:** `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio` (`only`, `changed_only`, `check_baseline`, `update_baseline`), `inspect_sound_event`, `run_codex_studio`, `run_structure_studio` (`build_structures.py`), `check_visual_regression` (soporta `include_diff_regions`, `include_lpips`), `check_structure_visual_regression` (`structure_visual_check.py`), `get_latest_structure_visual_smoke`
- **Gradle y suite completa:** `run_gradle_task` (`build`, `runData`, `runGameTestServer` con `isolated_gametests=True` → `-PisolatedGameTests=true`, `runHiveLifecycleSmoke`, `runVisualSmoke` con `-PdialectVisualSmoke=true`, `runAnimationVisualSmoke`, `runStructureVisualSmoke`, `compileJava`, `stageMcPilotDependency`, `publishMods` solo en simulación forzada), `run_verify_mod` (soporta `include_server_smoke`, `include_visual_smoke`, `include_worldgen_audit`, `worldgen_audit_radius`, `force_regenerate`, `no_daemon=False` por defecto), `manage_verify_cache` (`status`/`clear` sobre `build/.verify-stamps/`)
- **Observabilidad, caché y snapshots:** `get_bridge_health`, `get_bridge_metrics`, `list_tool_tags`, `clear_bridge_cache`, `capture_audit_snapshot`, `diff_audit_snapshots`
- **Ciclo de vida servidor+cliente, diagnósticos, `spark`, RCON y JFR:** `collect_diagnostic_bundle`, `diagnose_symptom` (incl. `colony_dialects`, `expeditions_cycles`), `get_diagnostics_guide` (`overview`/`tools`/`codemode`), `start_smoke_server(enable_diagnostics)`, `smoke_server_status`, `stop_smoke_server`, `start_live_session(enable_diagnostics)`, `stop_live_session`, `run_live_scenario` (incl. `colony_and_dialects`, `cycles_and_endgame`), `collect_spark_tps`, `capture_mcpilot_screenshot`, `get_latest_live_report`, `compare_live_reports`, `get_live_hive_status`, `get_live_colony_status`, `run_live_verify_scenario`, `list_diagnostic_artifacts` (soporta `glob_pattern`, `max_results`, orden más reciente primero), `read_diagnostic_logs`, `get_latest_worldgen_audit`, `list_java_processes` (`jcmd -l`), `run_jfr`, `summarize_jfr`, `mcpilot_command`, `prepare_mcpilot_client`, `build_mcpilot_compat`, `install_mcpilot_compat`, `stop_mcpilot_client`, `rcon_command`, `run_rcon_batch`, `run_rcon_verify_suite`

## Instalación
```powershell
python -m venv scripts/mcp/.venv
uv pip install --system-certs --python scripts/mcp/.venv/Scripts/python.exe -r scripts/mcp/requirements.txt
```
*(Si usas `pip` estándar en lugar de `uv`: `.\scripts\mcp\.venv\Scripts\python.exe -m pip install -r scripts/mcp/requirements.txt`.)*

## Verificación rápida
```powershell
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

## Registro en DeepSeek Harness (DSH)

DSH monta el servidor con la fila `mcp-manager` (`@deepseek-ai/dsh-mcp-manager`) en `~/.dsh/profiles/desktop/cordis.patch.yml`. `dsh-hmr` aplica el cambio en vivo; el namespace produce `mcp__mycelial-mind-dev__search`, `...__get_schema` y `...__execute`:

```yaml
- id: mcp-manager
  config:
    servers:
      mycelial-mind-dev:
        transport: stdio
        command: 'C:\Users\Alex\Documents\Mod Minecraft\scripts\mcp\.venv\Scripts\python.exe'
        args: ['C:\Users\Alex\Documents\Mod Minecraft\scripts\mcp\server.py']
        cwd: 'C:\Users\Alex\Documents\Mod Minecraft'
        startupTimeoutMs: 180000
        toolCallTimeoutMs: 1800000   # run_verify_mod / build superan los 60 s por defecto
        enabled: true
```

Verificación: debe existir un `python.exe` hijo del proceso de la GUI con `scripts\mcp\server.py`; después `execute` → `get_bridge_health()` reporta `ok=true` y `tool_count=66`. Guía completa: [.agents/skills/mycelial-codemode/references/deepseek-harness.md](../../.agents/skills/mycelial-codemode/references/deepseek-harness.md).

## Recursos MCP y esquemas de las meta-herramientas

- `get_schema` resuelve también las propias meta-herramientas: `get_schema(["search", "get_schema", "execute"])` devuelve sus contratos en lugar de `Tools not found`.
- Recursos de solo lectura (visibles en `list_mcp_resources` / `list_mcp_resource_templates`): `mycelial://bridge/health`, `mycelial://bridge/tools`, `mycelial://bridge/tags`, `mycelial://memory/tail`, `mycelial://skills`, `mycelial://skills/{skill_name}` y `mycelial://skills/{skill_name}/{document*}` (`*` acepta rutas como `references/deepseek-harness.md`).
- `get_bridge_health()` incluye `instance_id`, `instance_pid`, `instance_started_utc` e `instance_uptime_secs`: con varios clientes (DSH, Codex, Antigravity) cada uno tiene su propio proceso y así se distinguen.
