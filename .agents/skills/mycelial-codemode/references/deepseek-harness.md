# Conexión de `mycelial-mind-dev` con DeepSeek Harness (DSH)

## 1. Cómo se monta

DSH compone el Host con Cordis. El bundle `@deepseek-ai/dsh-web-app` trae la fila **`mcp-manager`** (`@deepseek-ai/dsh-mcp-manager`), que monta una instancia de `@deepseek-ai/dsh-mcp-client` por cada servidor declarado y registra sus herramientas en `ctx.tools`.

- **Ficheros de usuario:** `~/.dsh/profiles/desktop/cordis.patch.yml` (capa del perfil) y `~/.dsh/cordis.patch.yml` (capa global, tiene prioridad).
- **Semántica de patch:** un `- id: mcp-manager` **reemplaza el `config` completo de esa fila**; hay que reescribir todos los campos que quieras conservar.
- **Recarga:** `dsh-hmr` vigila el manifiesto (`package.json`) y ambos `cordis.patch.yml`; al guardar, recompone el árbol. En la práctica el servidor MCP arranca en segundos y las herramientas quedan disponibles para las sesiones siguientes. Si no ocurre, reinicia la app.
- **Nombre de herramienta:** `mcp__<serverName>__<rawName>`. Con `serverName: mycelial-mind-dev` quedan `mcp__mycelial-mind-dev__search`, `__get_schema` y `__execute` (los guiones son válidos; solo se normaliza lo ilegal, y una pérdida por longitud recibe un sufijo hash de 12 hex).
- **Timeout por llamada:** `toolCallTimeoutMs` (default 60 s). El proyecto necesita **30 min** para no cortar `run_verify_mod`, `run_gradle_task` y `run_sound_studio`.
- **Recursos MCP:** el servidor publica 5 recursos y 2 plantillas de solo lectura (`mycelial://bridge/{health,tools,tags}`, `mycelial://memory/tail`, `mycelial://skills`, `mycelial://skills/{skill_name}`, `mycelial://skills/{skill_name}/{document*}`), además de sus *instructions* (que DSH añade al prompt del sistema) y las 3 meta-herramientas.

## 2. Entrada canónica (perfil `desktop`)

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

| Campo | Valor del proyecto | Por qué |
| --- | --- | --- |
| `transport` | `stdio` | Proceso local; DSH negocia con un probe efímero y luego arranca el servidor real. |
| `command` | `scripts/mcp/.venv/Scripts/python.exe` | Venv con `fastmcp[code-mode]`, `numpy`, `pillow`; evita el Python global. |
| `args` | `scripts/mcp/server.py` | `mcp.run()` usa stdio por defecto. |
| `cwd` | raíz del repo | `REPO_ROOT` se deriva de `server.py`, pero Gradle y los scripts resuelven rutas relativas. |
| `startupTimeoutMs` | `180000` | Import de FastMCP + numpy en frío. |
| `toolCallTimeoutMs` | `1800000` | Suites completas (`run_verify_mod` ≈ 55–90 s; build + GameTests más). |
| `enabled` | `true` | `false` desmonta la instancia y conserva la entrada. |

## 3. Verificación en tres niveles

**a) Proceso vivo** (hijo del host que sirve la GUI):

```powershell
Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like '*scripts\mcp\server.py*' } |
  Select-Object ProcessId, ParentProcessId
Get-NetTCPConnection -LocalPort 49560 | Select-Object State, OwningProcess
```

El `ParentProcessId` del `python.exe` del venv debe ser el PID dueño del puerto de la GUI.

**b) Handshake directo** (sin DSH; sirve también para otros clientes):

```python
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

ROOT = r"C:\Users\Alex\Documents\Mod Minecraft"
PY = ROOT + r"\scripts\mcp\.venv\Scripts\python.exe"

async def main():
    p = StdioServerParameters(command=PY, args=[ROOT + r"\scripts\mcp\server.py"], cwd=ROOT)
    async with stdio_client(p) as (r, w):
        async with ClientSession(r, w) as s:
            init = await s.initialize()
            print(init.server_info.name, init.server_info.version)   # MycelialMindDev 4.2.1
            print([t.name for t in (await s.list_tools()).tools])    # execute, get_schema, search

asyncio.run(main())
```

**c) Llamada real desde el modelo:** `mcp__mycelial-mind-dev__execute` con `get_bridge_health()` → `ok=true`, `tool_count=63`, `jdk21` detectado e `instance_id`/`instance_pid` (útiles cuando DSH, Codex y Antigravity tienen cada uno su proceso). `list_mcp_resources` debe mostrar 5 recursos y `get_schema(["execute"])` debe responder con su esquema.

## 4. Problemas conocidos y solución

| Síntoma | Causa | Acción |
| --- | --- | --- |
| No aparece ninguna herramienta `mcp__mycelial-mind-dev__*` | Patch no aplicado (YAML inválido, `enabled: false`, entrada fuera del array raíz) | Validar YAML, revisar el log del host y reiniciar la app. |
| `get_schema(["execute"])` responde `Tools not found` | Instancia conectada antes de la corrección (código antiguo en memoria) | Reconectar/renovar la instancia: el servidor actual resuelve también las 3 meta-herramientas. |
| Fallo de conexión al arrancar | `command` inexistente, venv roto o `server.py` movido | Ejecutar el handshake de §3b; reinstalar con `uv pip install --system-certs --python scripts/mcp/.venv/Scripts/python.exe -r scripts/mcp/requirements.txt`. |
| `waiting-credentials` | Algún campo usa `${credential:...}` sin proveedor | Usar rutas literales (no hacen falta secretos aquí). |
| Llamadas cortadas a los 60 s | `toolCallTimeoutMs` sin subir | Volver a 1800000 en el patch. |
| Segundo servidor con el mismo nombre rechazado | `serverName` único por scope | Renombrar la clave. |
| Cambios en `server.py` no se reflejan | El proceso MCP ya está conectado; el cliente no recarga código | Reiniciar la app (o `restart` desde `ctx.mcpManager`), luego revalidar §3c. |
| Tras 10 reconexiones fallidas desaparecen las herramientas | Presupuesto de reconexión agotado | Corregir la causa y reiniciar el Host; el manager reinicia la instancia. |

## 5. Desactivar o mover a otro equipo

- Desactivar sin perder la entrada: `enabled: false` en el servidor.
- Otro equipo: copiar `scripts/mcp/` (con `.venv` recreado), `requirements.txt` y esta fila YAML ajustando rutas absolutas.
- Antes de tocar la configuración, cerrar sesiones vivas: `stop_live_session` / `stop_smoke_server` / `stop_mcpilot_client`.
