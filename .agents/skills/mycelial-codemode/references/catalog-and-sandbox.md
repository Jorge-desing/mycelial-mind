# Catálogo de Herramientas, Etiquetas, Caché y Sandbox

## 0. Identidad y nombres

- El bridge registra **67 herramientas** (`get_bridge_health()["tool_count"]`) y las expone con CodeMode detrás de **3 meta-herramientas**: `search`, `get_schema`, `execute`.
- `get_schema` resuelve las 67 del catálogo **y** las 3 meta-herramientas (un `get_schema` propio, `MycelialCodeMode`, fusiona ambas listas); solo responde `Tools not found` para nombres inexistentes. Dentro de `execute` solo existe `call_tool`, no los meta-tools.
- `list_tool_tags()` devuelve `{"ok", "tags", "tagged_tools"}`; un tool puede pertenecer a varias etiquetas.

## 1. Etiquetas semánticas (miembros reales)

| Etiqueta | Nº | Herramientas |
| --- | --- | --- |
| `meta` | 9 | `get_bridge_health`, `get_bridge_metrics`, `list_tool_tags`, `clear_bridge_cache`, `capture_audit_snapshot`, `diff_audit_snapshots`, `get_memory_tail`, `diagnose_symptom`, `manage_verify_cache` |
| `observability` | 5 | `get_bridge_health`, `get_bridge_metrics`, `list_tool_tags`, `clear_bridge_cache`, `collect_diagnostic_bundle` |
| `code` | 10 | `list_mod_sources`, `search_mod_code`, `read_mod_file`, `get_registered_content`, `get_hive_ai_overview`, `get_memory_tail`, `list_gametests`, `list_unit_tests`, `list_datapack_content`, `get_gradle_config` |
| `registry` | 2 | `get_registered_content`, `list_datapack_content` |
| `hive` | 3 | `get_hive_ai_overview`, `get_live_hive_status`, `get_live_colony_status` |
| `skills` | 5 | `list_skills`, `get_skill_guide`, `validate_skills`, `diagnose_symptom`, `get_diagnostics_guide` |
| `studios` | 10 | `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio`, `inspect_sound_event`, `audit_sound_masking`, `run_codex_studio`, `run_structure_studio`, `get_uv_manifest`, `audit_mod_assets` |
| `assets` | 2 | `inspect_sound_event`, `audit_sound_masking` |
| `structures` | 3 | `run_structure_studio`, `check_structure_visual_regression`, `get_latest_structure_visual_smoke` |
| `visual` | 4 | `check_visual_regression`, `check_structure_visual_regression`, `get_latest_structure_visual_smoke`, `capture_mcpilot_screenshot` |
| `gradle` | 8 | `run_gradle_task`, `run_verify_mod`, `manage_verify_cache`, `get_latest_junit_report`, `get_latest_pitest_report`, `list_gametests`, `list_unit_tests`, `get_gradle_config` |
| `reports` | 6 | `get_latest_junit_report`, `get_latest_pitest_report`, `get_latest_worldgen_audit`, `get_latest_live_report`, `compare_live_reports`, `collect_diagnostic_bundle` |
| `lifecycle` | 14 | `start_smoke_server`, `smoke_server_status`, `stop_smoke_server`, `start_live_session`, `stop_live_session`, `run_live_scenario`, `run_live_verify_scenario`, `get_live_hive_status`, `get_live_colony_status`, `collect_spark_tps`, `capture_mcpilot_screenshot`, `prepare_mcpilot_client`, `mcpilot_command`, `stop_mcpilot_client` |
| `rcon` | 11 | `rcon_command`, `run_rcon_batch`, `run_rcon_verify_suite`, `get_live_hive_status`, `get_live_colony_status`, `run_live_verify_scenario`, `run_live_scenario`, `start_smoke_server`, `start_live_session`, `stop_smoke_server`, `stop_live_session` |
| `mcpilot` | 9 | `mcpilot_command`, `prepare_mcpilot_client`, `build_mcpilot_compat`, `install_mcpilot_compat`, `stop_mcpilot_client`, `collect_spark_tps`, `capture_mcpilot_screenshot`, `start_live_session`, `stop_live_session` |
| `jfr` | 3 | `list_java_processes`, `run_jfr`, `summarize_jfr` |
| `diagnostics` | 23 | `collect_diagnostic_bundle`, `diagnose_symptom`, `get_diagnostics_guide`, `list_diagnostic_artifacts`, `read_diagnostic_logs`, `run_live_scenario`, `run_live_verify_scenario`, `run_rcon_verify_suite`, `get_latest_live_report`, `compare_live_reports`, `start_smoke_server`, `smoke_server_status`, `run_jfr`, `summarize_jfr`, `list_java_processes`, `audit_mod_assets`, `audit_sound_masking`, `check_visual_regression`, `capture_audit_snapshot`, `diff_audit_snapshots`, `get_latest_worldgen_audit`, `collect_spark_tps`, `run_verify_mod` |

## 2. Argumentos con defaults que importan (verificados con `get_schema`)

| Herramienta | Defaults / notas |
| --- | --- |
| `read_mod_file` | `start_line=1`, `end_line=200`; el servidor recorta a **400 líneas** por llamada. |
| `search_mod_code` | `scope="src"`, `file_ext=".java"`, `max_results=50`, `ignore_case=false` (regex). |
| `list_gametests` | `batch_filter=""` filtra por batch. |
| `list_unit_tests` | `suite_filter=""` filtra por clase/archivo JUnit 6 (ej. `OpenAlEfxLoopbackTest`); devuelve `count`, `suites` y `tests`. |
| `run_gradle_task` | `tasks` obligatorio; `timeout_secs=300`, `max_workers=2`, `no_daemon=false`, `offline=None` (auto `--offline` con `.gradle-user-home`), **`isolated_gametests=false`** (`-PisolatedGameTests=true` si `runGameTestServer` está en `tasks`), **`test_filter=""`** (`--tests <filter>` si `test` está en `tasks`), **`visual_smoke_mode=""`** (`default\|dialect\|relic\|particle\|expedition` → `-P<mode>VisualSmoke=true` si `runVisualSmoke` está en `tasks`). `publishMods` solo corre en dry-run estricto y rechaza filtros/banderas extra. Devuelve `ok`, `error_lines`, `summary_markers`. |
| `run_verify_mod` | `timeout_secs=540`; flags `include_server_smoke`, `include_visual_smoke`, `include_structure_visual_smoke`, `include_worldgen_audit`, `worldgen_audit_radius=8`, `force_regenerate`, `no_daemon`. |
| `manage_verify_cache` | `action="status"\|"clear"`, `scope="all"\|"gradle"\|"studios"` sobre `build/.verify-stamps/`. |
| `run_model_studio` | `check_only=true`, `run_tests=true`. |
| `run_particle_studio` | `check_only=true`, `export_extras=false`, `run_tests=true`; valida sprites/descriptores/registro y paridad con `particles.json`. |
| `run_texture_studio` | `check_only=true`, **`pbr=true`**, `export_extras=false`, `run_tests=true`. |
| `run_sound_studio` | `check_only=true`, `run_tests=true`, `only=None`, `changed_only=false`, `check_baseline=false`, `update_baseline=false`; no expone `--regen-ai-stems` (hazlo por consola con `build_sounds.py --regen-ai-stems`). |
| `inspect_sound_event` | `sound_id` obligatorio; inspecciona telemetría acústica + QA psicoacústico Sound Studio v5 (clave `psychoacoustic_v4`: `lra_lu`, `short_term_lufs_max`, `mud_ratio`, `harshness_ratio`, `bark_peak_band`, `timbral_diversity`), IR room, stem IA, `.ogg` y delta respecto a `sound_baseline.json`. |
| `audit_sound_masking` | `top_n=12`; audita enmascaramiento espectral Bark 24-bandas en matriz 11×20 (`220` pares Bark) entre camas y primer plano, headroom dB, LRA, diversidad tímbrica y QC de los `69` stems RTX. |
| `run_codex_studio` | `check_only=true`, `report=false`, `run_tests=true`; valida cobertura/refs/presupuesto del códice o exporta con `check_only=false`. |
| `get_hive_ai_overview` | Sin args; resume `hive/` (`enums`, `hive_files`), **`colony/` (`colony_enums`, `colony_files`)**, `entity/ai/**` (`ai_files`) y `ModMemoryTypes`. |
| `get_live_colony_status` | `host="127.0.0.1"`, `port=25575`, `password="mycelialtest"`, `include_cycle=true`; parsa `/mycelial colony status` y `/mycelial cycle status`. |
| `check_visual_regression` | baseline `scripts/model_studio/exports/baseline_visual_smoke.png`, actual `run/screenshots/visual_smoke_mobs.png`, `threshold_ssim=0.9`, `include_diff_regions=false`, `include_lpips=false`. |
| `start_smoke_server` | `world_type="flat"`, puertos `0` (auto), `enable_diagnostics=false`, `timeout_secs=120`. |
| `start_live_session` | `client_name="mycelial-1211"`, `world_type="flat"`, `launch_client=true`, `enable_diagnostics=false`, `timeout_secs=120`. |
| `run_live_scenario` | `preset="progression_ramp"`, `rcon_password="mycelialtest"`, `capture_perf=false`, `jfr_duration_secs=10`, `capture_screenshot=false`. |
| `rcon_command` / `run_rcon_batch` | puerto `25575`, password `mycelialtest`, `timeout_secs` 10/15. |
| `run_rcon_verify_suite` | `timeout_secs=120`, `include_worldgen_audit=false`, `worldgen_audit_radius=8`. |
| `read_diagnostic_logs` | `log_path="run/logs/latest.log"`, `tail_lines=120`, `filter_pattern=""`. |
| `list_diagnostic_artifacts` | `glob_pattern="*"`, `max_results=200`, orden más reciente primero; si `subdir=""`, omite `.tmp` y subdirectorios internos de mundo/assets (`region`, `entities`, `poi`, `libraries`, etc.). |
| `run_jfr` | `action="Check"` (`Start`/`Stop`/`Threads`), `duration_seconds=120`, requiere `process_id`. |

## 3. Caché TTL (30 s) y métricas

- Se cachean **8** herramientas (`TTL = 30 s`): `validate_skills`, `get_registered_content`, `list_gametests`, `list_unit_tests`, `get_hive_ai_overview`, `audit_mod_assets`, `list_datapack_content`, `get_gradle_config`. Además, `get_bridge_health()` cachea la detección estática del host (`JAVA_HOME`, `jcmd_available`, `temp_dir`).
- Las mutantes invalidan toda la caché (incluida la del entorno host): `run_model_studio`/`run_texture_studio`/`run_particle_studio`/`run_sound_studio`/`run_codex_studio` con `check_only=false`, `run_structure_studio`, `run_gradle_task`, `run_verify_mod`.
- Tras editar Java/JSON por fuera del bridge: `clear_bridge_cache()` antes de leer o de capturar el snapshot `after`.
- `get_bridge_metrics()` da llamadas/errores/latencia por herramienta y `hits`/`misses`; `get_bridge_health()` da `tool_count`, `cache_entries`, `active_live_sessions`, `jdk21`/`jcmd`.

## 4. Comparativa entre corridas

```python
await call_tool("capture_audit_snapshot", {"label": "before"})
# ... cambios, regeneración de activos, suites ...
await call_tool("clear_bridge_cache", {})
await call_tool("capture_audit_snapshot", {"label": "after"})
return await call_tool("diff_audit_snapshots", {"before_label": "before", "after_label": "after"})
```

`diff_audit_snapshots` informa deltas numéricos/booleanos (tests, pitest, registros, activos, datapacks, SSIM) y respeta el tope de 50 llamadas por bloque.

## 5. Límites del sandbox

- Máximo **50 `call_tool()`** por `execute()`, `max_feed_duration_secs=600`, `max_memory=100 MB`.
- Sin acceso al host: nada de `open()`, `pathlib`, `requests` ni `subprocess`; usa `read_mod_file`/`search_mod_code`/`read_diagnostic_logs`.
- Devuelve un `dict` compacto; para listas largas devuelve conteos, rutas y los primeros N elementos.
- En el servidor MCP (`_run_subprocess`), si `%TEMP%` del host no es utilizable, hace fallback automático a `artifacts/diagnostics/.tmp` y propaga `TEMP`/`TMP` a los subprocesos.

## 6. Recursos MCP publicados (solo lectura)

`list_mcp_resources` devuelve 5 recursos estáticos y `list_mcp_resource_templates` 2 plantillas; ninguno muta el repo ni arranca JVMs:

| Recurso | Tipo | Contenido |
| --- | --- | --- |
| `mycelial://bridge/health` | JSON | `get_bridge_health()` (incluye `instance_id`/`pid`/uptime) |
| `mycelial://bridge/tools` | JSON | `{count, tools:[{name, tags}]}` de las 67 herramientas |
| `mycelial://bridge/tags` | JSON | `list_tool_tags()` |
| `mycelial://memory/tail` | Markdown | últimas 60 líneas de `MEMORY.md` |
| `mycelial://skills` | JSON | índice de skills y sus referencias |
| `mycelial://skills/{skill_name}` | Markdown | `SKILL.md` de la skill |
| `mycelial://skills/{skill_name}/{document*}` | Markdown | cualquier doc, p. ej. `references/test-matrix.md` (el `*` acepta `/`) |

Los recursos se leen con `read_mcp_resource(server=..., uri=...)` en el cliente; `get_schema` sigue siendo el canal para parámetros de herramientas.
