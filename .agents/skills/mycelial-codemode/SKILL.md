---
name: mycelial-codemode
description: >-
  Guía operativa maestra de FastMCP CodeMode (mycelial-mind-dev) para Mycelial
  Mind: conexión con DeepSeek Harness y otros clientes MCP, descubrimiento con
  search/get_schema, sandbox Monty en execute (50 llamadas, 600 s, 100 MB), las
  63 herramientas del bridge, ciclo servidor+cliente aislado, auditorías y
  fallback de consola.
---

# Guía Maestra de FastMCP `CodeMode` (`mycelial-mind-dev`)

`scripts/mcp/server.py` (**FastMCP 4.2.1** + `CodeMode`/`MontySandboxProvider`) expone **63 herramientas** detrás de tres meta-herramientas: `search`, `get_schema` y `execute`. Es la interfaz obligatoria del proyecto para auditar suites, inspeccionar registros/IA/activos y operar Minecraft/NeoForge ([.agents/rules/codemode-first.md](../../rules/codemode-first.md)).

## 1. Conexión

### 1.1 DeepSeek Harness (DSH) — conexión ya instalada en este equipo

El perfil `desktop` monta el servidor con la fila `mcp-manager` en `C:\Users\Alex\.dsh\profiles\desktop\cordis.patch.yml`:

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
        toolCallTimeoutMs: 1800000
        enabled: true
```

- `dsh-hmr` vigila el manifiesto del perfil y **ambos** `cordis.patch.yml` (perfil y `~/.dsh/cordis.patch.yml`), por lo que el cambio se aplica en vivo; si no aparece, reinicia la app.
- El namespace lo define la clave del servidor: el modelo llama **`mcp__mycelial-mind-dev__search`**, **`...__get_schema`** y **`...__execute`**. Nombres con guiones son válidos (`[A-Za-z0-9_-]`, 64 caracteres máx.; si se recorta, DSH añade un hash de 12 hex).
- `toolCallTimeoutMs: 1800000` (30 min) es deliberado: `run_verify_mod`, `run_gradle_task(["build","runGameTestServer"])` y `run_sound_studio` superan con holgura el default de 60 s.
- El servidor inyecta sus instrucciones como texto en el prompt del sistema y publica **7 recursos MCP** de solo lectura: `mycelial://bridge/health`, `mycelial://bridge/tools`, `mycelial://bridge/tags`, `mycelial://memory/tail`, `mycelial://skills`, más las plantillas `mycelial://skills/{skill_name}` y `mycelial://skills/{skill_name}/{document*}` (el `*` permite `references/...md`).
- **Instancias múltiples:** cada cliente arranca su propio proceso; `get_bridge_health()` devuelve `instance_id`, `instance_pid`, `instance_started_utc` e `instance_uptime_secs` para distinguirlas.
- Diagnóstico, verificación y problemas conocidos: [references/deepseek-harness.md](references/deepseek-harness.md).

### 1.2 Otros clientes (Claude, Cursor, Codex, Antigravity, VS Code)

`.mcp.json` en la raíz declara `mcpServers.mycelial-mind-dev` con `command` = python del venv y `args` = `scripts/mcp/server.py`. Instalación del venv: `python -m venv scripts/mcp/.venv` + `uv pip install --system-certs --python scripts/mcp/.venv/Scripts/python.exe -r scripts/mcp/requirements.txt`.

### 1.3 Autocomprobación (una sola llamada)

`execute` → `get_bridge_health()` devuelve `ok=true`, `tool_count=63`, `server="MycelialMindDev"`, `fastmcp_version`, `python_executable`, `java_home_detected=C:\Program Files\Java\jdk-21`, `jcmd_available=true`, `temp_dir`, `repo_root`, `active_live_sessions`, `cache_entries` e identidad de instancia (`instance_id`, `instance_pid`, `instance_started_utc`, `instance_uptime_secs`). Es el primer `call_tool` de cualquier pasada.

### 1.4 Coexistencia con Codex y Antigravity

Cada cliente MCP lanza **su propio proceso stdio**: no comparten puerto, caché (TTL 15 s) ni estado en memoria, así que añadir o quitar un cliente no altera a los demás.

| Cliente | Dónde se declara |
| --- | --- |
| DeepSeek Harness | `~/.dsh/profiles/desktop/cordis.patch.yml` (fila `mcp-manager`) |
| Codex | `~/.codex/config.toml` → `[mcp_servers.mycelial-mind-dev]` |
| Antigravity | `~/.gemini/config/mcp_config.json` → `mcpServers["mycelial-mind-dev"]` |
| Otros / proyecto | `.mcp.json` en la raíz del repo |

Los cuatro apuntan al mismo par `scripts/mcp/.venv/Scripts/python.exe` + `scripts/mcp/server.py`, de modo que actualizar `server.py` o el venv afecta a todos a la vez (reconectar cada cliente para recargar el código).

- **No edites la configuración de otro cliente** al conectar uno nuevo; añade tu entrada y deja las demás intactas.
- **Riesgo real de concurrencia:** lo compartido es el *workspace*, no el protocolo. Dos clientes que lancen `run_verify_mod`, `run_gradle_task` o `start_live_session` simultáneamente compiten por `build/`, `run/`, el daemon de Gradle y `artifacts/diagnostics/`. Serializa esas operaciones (un cliente cada vez) o coordina antes de lanzarlas.

## 2. Descubrimiento y ejecución

1. **Buscar:** `search(query)` → lista ranking de herramientas con descripción corta (`N of 63 tools`).
2. **Confirmar contrato:** `get_schema(tools=[...], detail="brief|detailed|full")` → parámetros con tipos y defaults. También resuelve las propias meta-herramientas (`get_schema(["execute","search","get_schema"])`); solo responde `Tools not found` para nombres inexistentes.
3. **Ejecutar:** `execute(code)` con un bloque Python `async`.

### Reglas del sandbox `MontySandboxProvider`

- En `execute` solo existen `call_tool(name, params) -> dict` y la librería estándar del sandbox: **no** hay I/O de disco/red del host ni lectura de archivos.
- **Límite duro:** 50 llamadas `call_tool()` por bloque, `max_feed_duration_secs=600`, `max_memory=100 MB`. Divide auditorías largas en varios `execute` y guarda lo esencial en el resultado.
- Paraleliza comprobaciones independientes con `import asyncio` + `await asyncio.gather(...)`.
- Devuelve siempre un **dict resumen compacto** (booleanos, conteos, rutas, primeros errores). Nunca vuelques logs completos: usa `error_lines`, `matched_lines`, `summary_markers`.
- Los 3 meta-tools **no** son invocables dentro de `execute` (solo las 63 herramientas del bridge). `list_tool_tags()` sí es una herramienta del bridge.
- Valores por defecto conservadores: `check_only=True` y `run_tests=True` en los estudios; los mutantes regeneran activos y tardan más.

## 3. Enrutador rápido por objetivo

| Objetivo | Herramientas clave | Detalle |
| --- | --- | --- |
| Explorar código, registros, IA, tests, métricas o caché del bridge | `list_tool_tags`, `get_bridge_health`, `get_bridge_metrics`, `get_registered_content`, `get_hive_ai_overview`, `list_gametests`, `list_unit_tests`, `get_memory_tail`, `capture_audit_snapshot`, `diff_audit_snapshots`, `clear_bridge_cache` | [references/catalog-and-sandbox.md](references/catalog-and-sandbox.md) |
| Servidor aislado, RCON `/mycelial`, `spark`, MC Pilot, JFR y reportes en vivo | `start_smoke_server`, `start_live_session`, `run_live_scenario`, `get_live_hive_status`, `run_live_verify_scenario`, `rcon_command`, `run_rcon_batch`, `run_rcon_verify_suite`, `collect_spark_tps`, `capture_mcpilot_screenshot`, `run_jfr`, `summarize_jfr`, `get_latest_live_report`, `compare_live_reports`, `stop_live_session` | [references/live-game-lifecycle.md](references/live-game-lifecycle.md) |
| Auditorías de activos, modelos 3D, texturas PBR, partículas, audio Ogg, GameTests, build | `validate_skills`, `run_model_studio`, `run_texture_studio`, `run_particle_studio`, `run_sound_studio`, `run_structure_studio`, `audit_mod_assets`, `run_gradle_task`, `run_verify_mod`, `manage_verify_cache`, `check_visual_regression`, `check_structure_visual_regression`, `read_diagnostic_logs` | [references/audit-playbooks.md](references/audit-playbooks.md) |
| Diagnosticar fallos, regresiones, cuelgues o TPS/FPS (`/mycelial-diagnostics`) | `collect_diagnostic_bundle`, `diagnose_symptom`, `get_diagnostics_guide`, `list_diagnostic_artifacts`, `run_live_scenario`, `compare_live_reports` | [references/audit-playbooks.md](references/audit-playbooks.md) y [../mycelial-diagnostics/SKILL.md](../mycelial-diagnostics/SKILL.md) |

## 4. Recetas esenciales

La comprobación reproducible de recursos de GitHub Actions comparte `scripts/Verify-Assets.ps1` con `task verify:assets`. Usa el entorno de uv para modelos, texturas y partículas. Para auditorías desde el bridge y para la verificación completa se mantienen los tools de CodeMode. Consulta [Verificación de desarrollo](../../../docs/development-automation.md).

**Salud + auditoría paralela (una pasada típica de cierre):**

```python
import asyncio
health, skills, models, textures, sounds, assets = await asyncio.gather(
    call_tool("get_bridge_health", {}),
    call_tool("validate_skills", {}),
    call_tool("run_model_studio", {"check_only": True, "run_tests": True}),
    call_tool("run_texture_studio", {"check_only": True, "pbr": True, "run_tests": True}),
    call_tool("run_sound_studio", {"check_only": True, "run_tests": True}),
    call_tool("audit_mod_assets", {}),
)
return {"health": health["ok"], "tools": health["tool_count"],
        "skills": skills["ok"], "models": models["ok"],
        "textures": textures["ok"], "sounds": sounds["ok"],
        "assets": assets["ok"], "lang": assets.get("lang", {}).get("en_count")}
```

**Build + reportes estructurados:**

```python
build = await call_tool("run_gradle_task", {"tasks": ["test", "pitest"], "timeout_secs": 600})
return {"ok": build["ok"], "markers": build.get("summary_markers"),
        "errors": build.get("error_lines", [])[:10],
        "junit": await call_tool("get_latest_junit_report", {}),
        "pitest": await call_tool("get_latest_pitest_report", {})}
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
- En este harness la política de aprobación es **sin confirmaciones interactivas**: la disciplina de las reglas anteriores es la única barrera, así que no lances comandos destructivos "para probar".
- Tras editar Java/JSON por fuera del bridge, llama `clear_bridge_cache()` (TTL de 15 s) antes de capturar métricas o snapshots.
- Filtra logs con `filter_pattern` (`ERROR|WARN|FAIL|Exception`) en lugar de leer archivos completos.

## 6. Mantenimiento

- Al añadir/renombrar herramientas en `scripts/mcp/server.py`, actualiza [references/catalog-and-sandbox.md](references/catalog-and-sandbox.md) (conteo, etiquetas, args) y verifica con `validate_skills` + `python scripts/mcp/.venv/Scripts/python.exe scripts/mcp/test_server.py`.
- El conteo canónico vive en `get_bridge_health()["tool_count"]` (**63**); `list_tool_tags()` da los grupos semánticos (meta 9, code 10, gradle 8, lifecycle 13, rcon 10, mcpilot 9, jfr 3, diagnostics 22, studios 7, structures 3, visual 4, reports 6, skills 5, registry 2, hive 2, observability 5).
- Fallback por consola **solo** si el servidor no está conectado o al depurar el propio `server.py` (ver `AGENTS.md` §5).
