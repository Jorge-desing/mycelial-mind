# Verificación de Mycelial Mind en Windows

Ejecuta los comandos desde la raíz del repositorio. Para los estudios ligeros necesitas uv y Go Task en `PATH`. uv usa Python 3.12.10 según `.python-version` y puede instalarlo si falta.

```powershell
uv sync --locked
task verify:assets
```

`task verify:assets` ejecuta los tests de modelos, texturas y partículas y comprueba los recursos versionados sin regenerarlos. También puedes ejecutar `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Verify-Assets.ps1`.

Si la caché predeterminada de uv no es accesible en Windows, define `$env:UV_CACHE_DIR = Join-Path $PWD '.tools/uv-cache'` antes de ejecutar los comandos. Si uv necesita los certificados de Windows, define `$env:UV_SYSTEM_CERTS = 'true'`.

## Verificación completa

Estas tareas usan el entorno local que ya requiere `scripts/Verify-Mod.ps1`: Java 21, Python con las dependencias de los estudios y FFmpeg. Si activaste el entorno virtual de uv, desactívalo primero para que PowerShell encuentre el Python con las dependencias de audio.

| Comando | Qué ejecuta |
| --- | --- |
| `task verify` | Estudios de recursos, compilación y GameTests. |
| `task verify:server` | Verificación completa y escenarios en servidor dedicado mediante RCON. |
| `task verify:visual` | Verificación completa y comparaciones visuales de entidades y estructuras. Requiere una sesión gráfica de Windows. |

Para agentes conectados al bridge del proyecto, el punto de entrada de la verificación completa sigue siendo `run_verify_mod` de CodeMode. Las capturas se comparan con las referencias del proyecto.

## GitHub Actions

**Verify Mycelial Mind** se ejecuta en los pull requests a `main`, en los pushes a `main` y bajo demanda desde la pestaña **Actions**. **Asset checks** ejecuta el mismo script que `task verify:assets`. **Build and GameTests** ejecuta JUnit, compila el mod y verifica que NeoForge informe que aprobó los GameTests requeridos.

En la página de la ejecución, **Artifacts → gradle-verification** contiene la salida de Gradle y los informes JUnit disponibles. Consulta los logs de **Asset checks** para los fallos de recursos. Las pruebas gráficas, RCON y los estudios de audio con CUDA se ejecutan localmente.

## Actualizar las dependencias de los estudios

Edita las versiones de NumPy, Pillow o scikit-image en `pyproject.toml`, ejecuta `uv lock` y vuelve a correr `task verify:assets`. Versiona el manifiesto y `uv.lock` juntos. scikit-image selecciona las operaciones de imagen usadas para generar las partículas versionadas. El servidor MCP usa su propio entorno.
