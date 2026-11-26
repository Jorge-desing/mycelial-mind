---
name: mycelial-codemode
description: >-
  Guía operativa maestra de FastMCP CodeMode (mycelial-mind-dev) para Mycelial
  Mind: conexión con clientes MCP (.mcp.json, Antigravity, Codex), descubrimiento
  con search/get_schema, sandbox Monty en execute (50 llamadas, 600 s, 100 MB),
  las 67 herramientas del bridge (NeoForge 21.1.267, Colonia Soberana/Dialectos,
  Ciclos, Expediciones, HiveBrain y Sound Studio v5), matriz de 157 JUnit / 205
  GameTests, ciclo servidor+cliente aislado, auditorías y fallback de consola.
---

# Guía Maestra de FastMCP `CodeMode` (`mycelial-mind-dev`)

`scripts/mcp/server.py` (**FastMCP 4.2.1** + `CodeMode`/`MontySandboxProvider`) expone **67 herramientas** para **Mycelial Mind (Minecraft 1.21.1 / NeoForge 21.1.267, JUnit 6, Observable 5, Mod Publish Plugin v2)** detrás de tres meta-herramientas: `search`, `get_schema` y `execute`. Es la interfaz obligatoria del proyecto para auditar suites, inspeccionar registros/IA/Colonia/activos y operar Minecraft/NeoForge ([.agents/rules/codemode-first.md](../../rules/codemode-first.md)).

## 1. Conexión, salud y coexistencia

- **Montaje activo:** `.mcp.json` en la raíz declara `mycelial-mind-dev` (`scripts/mcp/.venv/Scripts/python.exe` → `scripts/mcp/server.py`), y cada cliente activo (Antigravity `~/.gemini/config/mcp_config.json`, Codex `~/.codex/config.toml`) lanza su propio subproceso `stdio` identificado por `instance_id`/`instance_pid`.
- **Instalación y coexistencia:** el setup del `.venv` y la tabla de registro viven en [scripts/mcp/README.md](../../../scripts/mcp/README.md); con varios clientes abiertos a la vez, **no lances `run_gradle_task` ni `run_verify_mod` en paralelo** desde dos instancias distintas.
- **Recursos MCP de solo lectura (7):** `mycelial://bridge/{health,tools,tags}`, `mycelial://memory/tail`, `mycelial://skills`, `mycelial://skills/{skill_name}` y `mycelial://skills/{skill_name}/{document*}`.
- **Autocomprobación:** `get_bridge_health()` devuelve en `<1 ms` `ok=true`, `tool_count=67`, `server="MycelialMindDev"`, `fastmcp_version`, `python_executable`, `java_home_detected`, `jcmd_available`, `temp_dir`, `repo_root`, `active_live_sessions`, `cache_entries` e identidad de instancia (`instance_id`, `instance_pid`, `instance_started_utc`, `instance_uptime_secs`). Es el primer `call_tool` de cualquier pasada.

## 2. Descubrimiento y ejecución

1. **Buscar:** `search(query)` → lista ranking de herramientas con descripción corta (`N of 67 tools`).
2. **Confirmar contrato:** `get_schema(tools=[...], detail="brief|detailed|full")` → parámetros con tipos y defaults. También resuelve las propias meta-herramientas (`get_schema(["execute","search","get_schema"])`); solo responde `Tools not found` para nombres inexistentes.
3. **Ejecutar:** `execute(code)` con un bloque Python `async`.

### Reglas del sandbox `MontySandboxProvider`

- En `execute` solo existen `call_tool(name, params) -> dict` y la librería estándar del sandbox: **no** hay I/O de disco/red del host ni lectura de archivos.
- **Límite duro:** 50 llamadas `call_tool()` por bloque, `max_feed_duration_secs=600`, `max_memory=100 MB`. Divide auditorías largas en varios `execute` y guarda lo esencial en el resultado.
- Paraleliza comprobaciones independientes con `import asyncio` + `await asyncio.gather(...)`.
- Devuelve siempre un **dict resumen compacto** (booleanos, conteos, rutas, primeros errores). Nunca vuelques logs completos: usa `error_lines`, `matched_lines`, `summary_markers`.
- Los 3 meta-tools **no** son invocables dentro de `execute` (solo las 67 herramientas del bridge). `list_tool_tags()` sí es una herramienta del bridge.
- Valores por defecto conservadores: `check_only=True` y `run_tests=True` en los estudios; los mutantes regeneran activos y tardan más.

## 3. Enrutador rápido por objetivo

| Objetivo | Herramientas clave | Detalle |
| --- | --- | --- |
| Explorar código, registros, IA/Colonia, tests, métricas o caché del bridge | `list_tool_tags`, `get_bridge_health`, `get_bridge_metrics`, `get_registered_content`, `get_hive_ai_overview` (`hive/` + `colony/`), `list_gametests`, `list_unit_tests` (`suite_filter`), `get_memory_tail`, `capture_audit_snapshot`, `diff_audit_snapshots`, `clear_bridge_cache` | [references/catalog-and-sandbox.md](references/catalog-and-sandbox.md) |
| Servidor aislado, RCON (`/mycelial colony`, `cycle`, `audio`, `ai`, `filament`, `verify`), `spark`, MC Pilot, JFR y reportes en vivo | `start_smoke_server`, `start_live_session`, `run_live_scenario` (incl. `colony_and_dialects`, `cycles_and_endgame`, `sinkhole_and_worldgen`, `audio_and_ai`), `get_live_hive_status`, `get_live_colony_status`, `run_live_verify_scenario` (`24` escenarios), `rcon_command`, `run_rcon_batch`, `run_rcon_verify_suite`, `collect_spark_tps`, `capture_mcpilot_screenshot`, `run_jfr`, `summarize_jfr`, `get_latest_live_report`, `compare_live_reports`, `stop_live_session` | [references/live-game-lifecycle.md](references/live-game-lifecycle.md) |
| Matriz de tests y auditorías (157 JUnit, 205 GameTests, 5 modos VisualSmoke, 6 estudios Python) | `validate_skills`, `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio` v5 (`only`, `changed_only`, `check_baseline`, `update_baseline`), `inspect_sound_event`, `audit_sound_masking`, `run_codex_studio`, `run_structure_studio`, `audit_mod_assets`, `run_gradle_task` (`isolated_gametests=True`, `test_filter=...`, `visual_smoke_mode="default\|dialect\|relic\|particle\|expedition"`), `run_verify_mod`, `manage_verify_cache`, `check_visual_regression`, `check_structure_visual_regression`, `read_diagnostic_logs` | [references/test-matrix.md](references/test-matrix.md) y [references/audit-playbooks.md](references/audit-playbooks.md) |
| Diagnosticar fallos, regresiones, OpenAL/Bark, Hifas Verlet, DataGen/Compat, Colonia o Ciclos (`/mycelial-diagnostics`) | `collect_diagnostic_bundle`, `diagnose_symptom` (`audio_efx`, `hyphal_physics`, `datagen_compat`, `colony_dialects`, `expeditions_cycles`, ...), `get_diagnostics_guide`, `list_diagnostic_artifacts`, `run_live_scenario`, `compare_live_reports` | [references/audit-playbooks.md](references/audit-playbooks.md) y [../mycelial-diagnostics/SKILL.md](../mycelial-diagnostics/SKILL.md) |

## 4. Recetas esenciales

La comprobación reproducible de recursos de GitHub Actions comparte `scripts/Verify-Assets.ps1` con `task verify:assets`. Usa el entorno de uv para modelos, texturas y partículas. Para auditorías desde el bridge y para la verificación completa se mantienen los tools de CodeMode. Consulta [Verificación de desarrollo](../../../docs/development-automation.md).

Para revisar una release sin subirla, `run_gradle_task` acepta `publishMods` y fuerza `publishDryRun=true`; no admite propiedades ni opciones que puedan desactivar el modo de simulación. También existe el alias local `task release:dry-run`. Consulta [Preparar una release](../../../docs/releasing.md). La publicación real requiere un comando local explícito y las credenciales correspondientes.

**Salud + auditoría paralela (una pasada típica de cierre):**

```python
import asyncio
health, skills, models, textures, particles, sounds, masking, codex, assets = await asyncio.gather(
    call_tool("get_bridge_health", {}),
    call_tool("validate_skills", {}),
    call_tool("run_model_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_texture_studio", {"check_only": True, "pbr": True, "run_tests": True}),
    call_tool("run_particle_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_sound_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_sound_masking", {"top_n": 8}),
    call_tool("run_codex_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_mod_assets", {}),
)
return {"health": health["ok"], "tools": health["tool_count"],
        "skills": skills["ok"], "models": models["ok"],
        "textures": textures["ok"], "particles": particles["ok"],
        "sounds": sounds["ok"], "masking": masking["ok"],
        "codex": codex["ok"], "assets": assets["ok"],
        "lang": assets.get("lang", {}).get("en_count")}
```

**Build + tests completos (o suite puntual con `test_filter`):**

```python
build = await call_tool("run_gradle_task", {
    "tasks": ["runData", "test", "build", "runGameTestServer"],
    "isolated_gametests": True, "no_daemon": True, "max_workers": 1, "timeout_secs": 600,
})
# Iteración rápida: run_gradle_task({"tasks": ["test"], "test_filter": "OpenAlEfxLoopbackTest"})
# Nota: :pitest tiene incompatibilidad conocida con ModDevGradle 2.0.148 + Pitest 1.19.0
return {"ok": build["ok"], "markers": build.get("summary_markers"),
        "errors": build.get("error_lines", [])[:10],
        "junit": await call_tool("get_latest_junit_report", {})}
```

**Sesión viva aislada y siempre cerrada:**

```python
sess = await call_tool("start_live_session", {"launch_client": False, "world_type": "flat"})
if not sess.get("ok"):
    return sess
try:
    status = await call_tool("get_live_hive_status", {
        "port": sess["server"]["rcon_port"], "password": sess["server"]["rcon_password"]})
    verify = await call_tool("run_live_verify_scenario", {
        "scenario": "polish", "port": sess["server"]["rcon_port"],
        "password": sess["server"]["rcon_password"]})
finally:
    stop = await call_tool("stop_live_session", {"session_id": sess["session_id"], "cleanup_dir": True})
return {"status": status.get("threat_pct"), "verify_ok": verify.get("ok"), "stopped": stop.get("ok")}
```

## 5. Seguridad operativa

- **Nunca** `/mycelial reset` ni escenarios RCON sobre mundos de usuario: usa `start_smoke_server`/`start_live_session` (directorios `artifacts/diagnostics/live-smoke-*`).
- Cierra siempre con `stop_live_session(cleanup_dir=True)` / `stop_smoke_server`; nunca mates procesos ajenos.
- Las llamadas dentro de `execute` no piden confirmación interactiva: la disciplina de estas reglas es la única barrera, así que no lances comandos destructivos "para probar".
- Tras editar Java/JSON por fuera del bridge, llama `clear_bridge_cache()` (TTL de 30 s) antes de capturar métricas o snapshots.
- Filtra logs con `filter_pattern` (`ERROR|WARN|FAIL|Exception`) en lugar de leer archivos completos.

## 6. Mantenimiento

- Al añadir/renombrar herramientas en `scripts/mcp/server.py`, actualiza [references/catalog-and-sandbox.md](references/catalog-and-sandbox.md) (conteo, etiquetas, args) y verifica con `validate_skills` + `python scripts/mcp/.venv/Scripts/python.exe scripts/mcp/test_server.py`.
- El conteo canónico vive en `get_bridge_health()["tool_count"]` (**67**); `list_tool_tags()` da los grupos semánticos (`meta` 9, `code` 10, `gradle` 8, `lifecycle` 14, `rcon` 11, `mcpilot` 9, `jfr` 3, `diagnostics` 23, `studios` 10, `assets` 2, `structures` 3, `visual` 4, `reports` 6, `skills` 5, `registry` 2, `hive` 3, `observability` 5).
- Fallback por consola **solo** si el servidor no está conectado o al depurar el propio `server.py` (ver `AGENTS.md` §5).
