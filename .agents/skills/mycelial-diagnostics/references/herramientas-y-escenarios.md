# Herramientas y escenarios de diagnóstico

Ejecutar desde la raíz del proyecto en PowerShell. Comprobar primero `gradle.properties`, `build.gradle` y los scripts: pueden haber cambiado. Para Gradle usar JDK 21 y, en este equipo, `GRADLE_OPTS` con `-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` si hace falta.

## Selección por síntoma

Usa `diagnose_symptom("<id>")` dentro de `CodeMode` (`execute`) para obtener esta tabla y el estado vivo actual de forma programática:

| Síntoma (`symptom_id`) | Primera evidencia | Siguiente paso y límite | Herramientas `CodeMode` (`execute`) |
| --- | --- | --- | --- |
| Rasgos, daño, infección, nodos o oleadas (`hive_gameplay`) | `runGameTestServer`, estado y logs | Reproducir umbral, distancia y ticks; crear GameTest de comportamiento y persistencia. Los tests actuales no cubren todos los casos. | `get_hive_ai_overview`, `list_gametests`, `run_gradle_task(["runGameTestServer"])`, `run_live_verify_scenario` |
| Comandos o integración de servidor (`server_rcon`) | `/mycelial status` y `/mycelial verify <escenario>` en servidor de prueba | RCON smoke y `verify all` ejercen 21 escenarios definidos, no una partida completa. Inspeccionar efectos sobre datos antes de usarlos. | `start_smoke_server`, `get_live_hive_status`, `run_live_verify_scenario`, `run_rcon_batch`, `stop_smoke_server` |
| HUD, GUI, animación, shaders o red (`client_ui_net`) | Log y cliente real; MC Pilot para entradas, estado y capturas | Comprobar también estado del servidor y payloads; una captura de menú no demuestra interacción ni sincronización. | `prepare_mcpilot_client`, `start_live_session(launch_client=True)`, `capture_mcpilot_screenshot`, `read_diagnostic_logs`, `stop_live_session` |
| Modelos, UV, texturas o recursos (`models_textures`) | Validadores de model/texture studio y captura 3D nueva | `runVisualSmoke` no cubre todas las vistas ni animaciones; revisar imagen y logs. Si hay shaders, probar tanto sin ellos como con la combinación concreta. | `run_model_studio`, `run_texture_studio`, `audit_mod_assets`, `run_gradle_task(["runVisualSmoke"])`, `check_visual_regression` |
| Bajadas de TPS o tirones (`tps_lag`) | spark: TPS/MSPT, muestras y carga reproducible | Observable ayuda a localizar entidades/bloques; contrastar con JFR si se sospecha GC/asignaciones. No atribuir el coste al mod solo por coexistencia. | `run_live_scenario(capture_perf=True, enable_diagnostics=True)`, `collect_spark_tps`, `run_jfr`, `summarize_jfr`, `compare_live_reports` |
| Baja tasa de FPS (`fps_render`) | Perfil de cliente y comparación visual controlada | Distinguir render, GPU y servidor; TPS estable no demuestra ausencia de problemas de render. | `start_live_session(launch_client=True)`, `capture_mcpilot_screenshot`, `check_visual_regression`, `read_diagnostic_logs`, `stop_live_session` |
| Cuelgue o fuga de memoria (`hang_memory`) | PID, logs, `jcmd` hilos, JFR | Comparar grabaciones y tiempos; una muestra o heap creciente aislado no demuestra fuga. | `collect_diagnostic_bundle`, `list_java_processes`, `run_jfr` (`Threads`/`Start`/`Stop`), `summarize_jfr`, `read_diagnostic_logs` |
| Biomas, estructuras o dimensión (`worldgen_structures`) | Mundos de prueba con semillas y coordenadas registradas | Comprobar varias semillas, generación, entrada, salida y recarga; ningún smoke determina distribución estadística por sí solo. | `run_structure_studio`, `get_latest_worldgen_audit`, `run_gradle_task(["runStructureVisualSmoke"])`, `check_structure_visual_regression`, `get_latest_structure_visual_smoke` |

## Herramientas existentes

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_OPTS = '-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
.\gradlew.bat runGameTestServer
.\gradlew.bat build
```

La suite más amplia es `.\scripts\Verify-Mod.ps1 -IncludeServerSmoke -IncludeVisualSmoke` (y opcionalmente `-IncludeWorldgenAudit -WorldgenAuditRadius 8` para auditar generación de terreno). El servidor smoke utiliza un directorio nuevo bajo `artifacts/diagnostics/` y los puertos locales 25577/25578. El resto del pipeline regenera datos y activos; revisar su impacto antes de ejecutarlo. Interpretar la salida y el código de salida; no fiarse de contadores fijos impresos por el script.

Para auditar generación de terreno bajo demanda (brotes en caverna/superficie, colmenas superficiales, distancias al vecino más cercano `min/mean/p50/p90/max`, conectividad de red sináptica, integridad de `SurfaceHive` y separación Jigsaw de `hive_sanctum`), usar `/mycelial worldgen audit [radiusChunks] [sanctumGridChunks]` o `/mycelial verify worldgen`. Emite una línea compacta en chat/RCON y exporta el esquema JSON completo en `artifacts/diagnostics/worldgen-audit-latest.json` (accesible desde `CodeMode` vía `get_latest_worldgen_audit()`). `WorldgenAuditEngine` falla explícitamente si la muestra queda vacía (`chunksScanned == 0`).

Para una escena visual actual, ejecutar `.\gradlew.bat runVisualSmoke`, verificar que `run/screenshots/visual_smoke_mobs.png` fue regenerada y examinarla; después ejecutar `python scripts/visual_regression.py`. Interpretar SSIM/pHash junto con inspección visual, ya que una igualdad no valida jugabilidad. Para modelos y texturas, consultar las skills `mycelial-model-studio` y `mycelial-texture-studio`, y comprobar qué archivos generan sus comandos antes de ejecutarlos.

Para visualizar las estructuras del mod (`MycelialOutbreakFeature`, `SurfaceHiveFeature`, `hive_sanctum/sanctum_atrium` exterior e interior, y `MatriarchCitadelPiece` frontal e interior), usar `run_gradle_task(["runStructureVisualSmoke"])` o `run_verify_mod(include_structure_visual_smoke=True)` (en PowerShell: `.\gradlew.bat runStructureVisualSmoke --max-workers=1 --no-daemon` o `.\scripts\Verify-Mod.ps1 -IncludeStructureVisualSmoke`). Captura 5 pantallas en `artifacts/diagnostics/structure-visual-client/screenshots/structure_*.png` + `structure_smoke_meta.json`, verifica frescura/primer plano/regresión `SSIM`+`pHash`+`LPIPS` contra `scripts/model_studio/exports/structure_baselines/` (actualizar referencias tras inspección visual con `python scripts/structure_visual_check.py --update-baseline`) y genera la hoja de contacto 3x2 etiquetada `artifacts/diagnostics/structures/structures_contact_sheet.png` + manifiesto `structure-visual-smoke-latest.json` vía `check_structure_visual_regression` (`scripts/structure_visual_check.py`).

`scripts/Start-Mcpmc.ps1` y `.agents/mcpmc.json` preparan un bot para servidor; requieren un servidor compatible y no sustituyen el render del cliente. Para GameTest y convenciones de plantillas y estado compartido, usar `neoforge-gametest`.

## MC Pilot: interacción en cliente

Comprobar `.\scripts\Invoke-McPilot.ps1 client list`; si falta la herramienta, instalar la versión fijada que documenta `Invoke-McPilot.ps1`. Si falta el cliente:

```powershell
.\scripts\Invoke-McPilot.ps1 client create mycelial-1211 --version 1.21.1 --loader neoforge --java 'C:\Program Files\Java\jdk-21\bin\java.exe'
.\scripts\Prepare-McPilotClient.ps1
.\scripts\Invoke-McPilot.ps1 client launch mycelial-1211 --no-server
.\scripts\Invoke-McPilot.ps1 --client mycelial-1211 screenshot --output artifacts/diagnostics/menu.png
```

Repetir `Prepare-McPilotClient.ps1` tras cambios para probar el JAR actual. Para reconstruir e instalar la corrección local de MC Pilot usar `Build-McPilotCompat.ps1` e `Install-McPilotCompat.ps1`. Para pruebas en mundo, preparar un servidor y mundo aislados, verificar versiones y conexión, consultar `client launch --help` y `schema`, y registrar las acciones y aserciones de ambos lados. No asumir que el servidor dev o un perfil están configurados automáticamente. Usar `Stop-McPilotClient.ps1` para verificar el cierre si ocurre el conflicto transitorio de puerto; ante un conflicto persistente, identificar el proceso antes de intervenir. No matar procesos ajenos.

La preparación exige una instancia NeoForge 1.21.1 detenida y con su puerto libre; respeta `MCT_HOME` y obtiene SmartBrainLib mediante la tarea Gradle `stageMcPilotDependency`. Si alguna comprobación falla, resolver el estado del cliente antes de reintentar.

Para conectar al servidor **de desarrollo**, `runBridgeDevClient` usa el directorio aislado `artifacts/diagnostics/bridge-dev-client`; colocar allí, en `mods/`, el JAR compatible de MC Pilot. El puente escucha en localhost, puerto `MCT_CLIENT_WS_PORT` (25560 por defecto). Usar un servidor de desarrollo aislado, esperar a que ambos terminen de cargar y solicitar `client.reconnect` por el protocolo WebSocket; confirmar `position.get` y la entrada en el log del servidor. Una solicitud durante la carga produjo un rechazo transitorio aunque ambos usaban NeoForge 21.1.176. Se comprobó la conexión en `bridge-dev-server.log` y `bridge-dev-retry-response.log`. También se verificó `position get` del CLI con `MCT_HOME` aislado y una entrada de cliente creada manualmente para el puerto del puente (`mcpilot-cli-isolated-position.json`); esta entrada no es una interfaz de adjunción documentada y puede cambiar con MC Pilot. No modificar el estado global ni asumir que `client stop` gestiona esa JVM. Al cerrar, detener solamente los procesos de esta prueba y preservar su mundo.

## spark y Observable

Arrancar el cliente o servidor de prueba con `-Pdiagnostics`, y confirmar en el log que se cargaron spark y Observable. Para el servidor usar los comandos disponibles en ese entorno, por ejemplo:

```text
/spark tps
/spark profiler start
# Reproducir el escenario controlado durante el intervalo elegido.
/spark profiler stop --save-to-file
```

En cliente comprobar el prefijo de comandos disponible: la documentación de spark indica `/sparkc` en instalaciones cliente Forge/Fabric; confirmar en NeoForge antes de usarlo. `--save-to-file` evita subir el perfil; registrar su ruta. La tecla de Observable puede variar o no estar asignada: comprobarla en controles, abrir la GUI, verificar `Overlay` y registrar intervalo y objetos localizados. En el perfil MC Pilot de esta prueba la tecla era `R`; el perfil desde la GUI y el JSON local se verificaron aunque la subida falló. Consultar [comandos de spark](https://spark.lucko.me/docs/Command-Usage).

En este entorno la respuesta asíncrona de `spark tps` no llegó por RCON. Con un jugador operador en el mundo aislado se verificó `Invoke-McPilot.ps1 --client mycelial-1211 chat command 'spark tps'`, seguido de `chat history --last 16`: allí aparecen TPS, duración de ticks y CPU. Registrar el intervalo que muestra spark y no atribuir esos valores a perfiles anteriores.

En la prueba aislada el servicio de subida de Observable respondió HTTP 500. Configurar `{"uploadURL":""}` en el `config/observable.json` de ese servidor desactiva la subida; se confirmó el mensaje de servidor `uploadURL not set, skipping upload` y el perfil JSON local. Observable aún muestra al cliente un aviso genérico de subida fallida incluso cuando se desactivó intencionalmente. No cambiar sin necesidad el destino de subida de otros servidores.

## Compatibilidad local de Observable 5.4.4

El tag oficial [5.4.4](https://github.com/tasgon/observable/tree/5.4.4) presentó dos fallos reproducibles en la instancia diagnóstica. `Overlay.createVBO()` conservaba la referencia al VBO después de cerrarlo cuando un perfil no tenía bloques; los frames siguientes intentaban enlazar y dibujar ese VBO cerrado (`GL_INVALID_OPERATION`). Cada perfil también creaba `Timer("Profiler", false)` sin cancelar el temporizador, dejando un hilo no daemon que podía mantener viva la JVM tras guardar y apagar el servidor.

El diff local está en `scripts/patches/observable-5.4.4-vbo-shutdown.patch`. Se aplica a una copia limpia del tag 5.4.4 y se construye allí con `:forge:remapJar` y JDK 21. La corrección evita crear/dibujar un VBO si no hay bloques visibles, y ejecuta el temporizador como daemon y lo cancela en `finally`. Mantener ese JAR solo en la instancia diagnóstica de MC Pilot; no agregarlo a dependencias de Mycelial Mind ni distribuirlo como parte del mod.

Para verificarlo, usar una copia de mundo y un cliente detenidos: guardar el JAR original, instalar temporalmente el compat, ejecutar un perfil Observable de 30 s sin bloques instrumentados y otro con bloques conocidos, y confirmar que sus JSON reflejan respectivamente cero y varios bloques. Tras el perfil vacío, revisar que el log del cliente no añada `GL_INVALID_OPERATION` después de `Initializing VBO`; abrir la GUI con la tecla asignada, confirmar `Overlay` marcado y que el perfil desde `Profile TPS` termina. Tras un perfil, `jcmd <pid> Thread.print` no debe mostrar hilos no daemon llamados `Profiler`; detener el servidor y comprobar que todos los mundos se guardaron y la JVM salió. Mantener el JAR parcheado solo en el perfil diagnóstico de MC Pilot detenido, conservar el respaldo original y restaurarlo con el cliente detenido si se retira la compatibilidad. Si `uploadURL` está vacío, el aviso genérico de fallo de subida es esperado mientras el JSON local exista.

Para un A/B que separe renderizado y cierre, mantener fija la versión del servidor y variar solo el JAR del cliente en la prueba de render; usar la misma copia del mundo, duración, escena y overlay, y conservar un control con bloques visibles más el perfil vacío. Confirmar `blocks: {}` en el JSON, dejar el cliente abierto al menos 20 s tras `Initializing VBO` y contar los errores durante esa ventana. Para el cierre, mantener fijo el cliente parcheado y variar solo el JAR del servidor; al terminar el perfil, tomar `jcmd <pid> Thread.print` de inmediato y otra vez tras la ventana de render, luego detener por RCON y registrar por separado el guardado de dimensiones y la salida del PID. Un servidor que guarda el mundo pero conserva el hilo no daemon `Profiler` tiene un fallo de cierre aunque el guardado haya terminado. Registrar SHA-256 de cada JAR y el log de inicio que muestra su carpeta de carga; en el informe, separar observaciones (conteos, líneas y tiempos) de inferencias sobre la causa.

## JFR y jcmd

```powershell
& 'C:\Program Files\Java\jdk-21\bin\jcmd.exe' -l
.\scripts\Invoke-Jfr.ps1 -ProcessId <PID_MINECRAFT> -Action Start -DurationSeconds 120
.\scripts\Invoke-Jfr.ps1 -ProcessId <PID_MINECRAFT> -Action Check
.\scripts\Invoke-Jfr.ps1 -ProcessId <PID_MINECRAFT> -Action Threads
& 'C:\Program Files\Java\jdk-21\bin\jfr.exe' summary '<ruta-del-archivo.jfr>'
```

Identificar el proceso Java de Minecraft por línea de comandos, no usar el PID del wrapper o daemon Gradle. El script guarda en `artifacts/diagnostics/`; esperar la duración o usar `-Action Stop` y verificar que el archivo exista y sea legible. `Invoke-Jfr.ps1` usa `Remove-JunctionSafe` (`[System.IO.Directory]::Delete($Path, $false)` + fallback `cmd /c rmdir`, nunca `Remove-Item -Recurse` sobre junctions NTFS en `%TEMP%`), `Remove-FileSafe` (borrado con reintentos de `.jfr.link.json`), y en `-Action Stop` limpia y devuelve salida `0` incluso si la grabación de duración fija ya había finalizado automáticamente (`$completed -gt 0`). `Threads` imprime pilas en consola: redirigir a un archivo si se necesita evidencia. JFR registra actividad JVM; correlacionar pausas y muestras con la escena y límites del perfil.

## Escenarios mínimos recomendados

Registrar para cada corrida versiones, semilla, dimensión, coordenadas, dificultad, configuración, estado inicial, duración, población y resultado esperado/observado; guardar logs y artefactos con una etiqueta de corrida. Usar copias de mundos (`start_smoke_server` / `start_live_session`).

1. **Aprendizaje y persistencia:** aplicar un tipo de daño controlado, comprobar estímulo/rasgos en servidor y cliente, guardar y recargar, y comprobar el estado sin asumir que el HUD representa el dato persistido. *(`CodeMode`: `run_live_scenario(preset="progression_ramp")` + `get_live_hive_status` + `run_live_verify_scenario("memory")`)*
2. **Contención y expansión:** repetir con igual disposición de nodos y terreno, con y sin pilón; observar ambos lados del radio, enlaces y cambios de bloques. *(`CodeMode`: `run_live_verify_scenario("pylon_containment")` / `run_live_verify_scenario("expansion")` / `run_gradle_task(["runGameTestServer"])`)*
3. **Interacción:** probar apertura del códice y radar, banco, armadura y arma secundaria con cliente real; corroborar efectos y sincronización en servidor. Registrar qué pasos están automatizados y cuáles son manuales. *(`CodeMode`: `start_live_session(launch_client=True)` + `mcpilot_command` + `capture_mcpilot_screenshot` + `read_diagnostic_logs`)*
4. **Carga:** comparar una escena de control y otra con cantidades conocidas de nodos, criaturas y Marea Fúngica, tras estabilización y durante el mismo intervalo. Tomar TPS/MSPT y perfil spark; usar Observable o JFR si la hipótesis lo requiere. Repetir para reducir variación y anotar hardware y ajustes. *(`CodeMode`: `run_live_scenario(preset="swarm_load", enable_diagnostics=True, capture_perf=True)` antes/después + `compare_live_reports`)*
5. **Mundo y recursos:** probar semillas diferentes para biomas/estructura y entrada-retorno dimensional; comprobar recursos en cliente con y sin la configuración gráfica relevante. *(`CodeMode`: `rcon_command("mycelial worldgen audit ...")` + `get_latest_worldgen_audit` + `run_gradle_task(["runVisualSmoke", "runStructureVisualSmoke"])` + `check_visual_regression` + `check_structure_visual_regression`)*

## Integración en FastMCP CodeMode (`scripts/mcp/server.py`)

El servidor MCP `mycelial-mind-dev` (`CodeMode` + `MontySandboxProvider`, tope 50 `call_tool()` por `execute()`) expone estas herramientas de diagnóstico para que los agentes encadenen triaje automático, recolección de evidencia inicial, ciclo de vida aislado, detección de PIDs JVM, JFR, `spark`, MC Pilot, `runVisualSmoke`, regresión SSIM/pHash/LPIPS, RCON y reportes comparativos en un bloque Python (ver también Playbook 4 en `mycelial-codemode/references/audit-playbooks.md`):

- `diagnose_symptom(symptom, include_live_state)` (`hive_gameplay` | `server_rcon` | `client_ui_net` | `models_textures` | `tps_lag` | `fps_render` | `hang_memory` | `worldgen_structures` | `all`), `collect_diagnostic_bundle(log_tail_lines, max_artifacts)`
- `get_diagnostics_guide` (`overview` | `tools` | `codemode`), `list_diagnostic_artifacts`, `read_diagnostic_logs`
- `start_smoke_server` (`enable_diagnostics=True`), `smoke_server_status`, `stop_smoke_server`, `start_live_session`, `stop_live_session`
- `run_live_scenario` (presets `progression_ramp`/`swarm_load`/`diplomacy_and_memory`/`homeostasis_and_dimension`/`full_smoke_matrix`/`custom`, + captura `spark`/JFR y PNG), `get_latest_live_report`, `compare_live_reports`
- `collect_spark_tps`, `capture_mcpilot_screenshot`, `get_live_hive_status`, `run_live_verify_scenario`, `rcon_command`, `run_rcon_batch`
- `list_java_processes` (`jcmd -l`), `run_jfr` (`Invoke-Jfr.ps1`), `summarize_jfr` (`jfr summary`)
- `mcpilot_command` (`Invoke-McPilot.ps1`), `prepare_mcpilot_client` (`Prepare-McPilotClient.ps1`), `stop_mcpilot_client`
- `run_gradle_task` (incluye `runVisualSmoke` / `runStructureVisualSmoke`), `check_visual_regression`, `check_structure_visual_regression`

