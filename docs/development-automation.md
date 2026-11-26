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

Si LWJGL o JNA no pueden extraer sus DLL en el directorio temporal predeterminado, configura uno escribible para esa sesión de PowerShell y vuelve a ejecutar la tarea visual.

```powershell
$verifyTemp = (New-Item -ItemType Directory -Force '.tools/verify-temp').FullName
$env:TEMP = $verifyTemp
$env:TMP = $verifyTemp
$env:JAVA_TOOL_OPTIONS = ('{0} -Djava.io.tmpdir="{1}" -Djna.tmpdir="{1}"' -f $env:JAVA_TOOL_OPTIONS, $verifyTemp).Trim()
task verify:visual
```

## GitHub Actions (optimizado para GitHub Free)

**Verify Mycelial Mind** (`.github/workflows/verify.yml`) se ejecuta en los pull requests a `main`, en los pushes a `main` y bajo demanda desde la pestaña **Actions** o mediante `gh`. Para ahorrar minutos del plan **GitHub Free** (2,000 min/mes en repositorios privados):

- Los 3 jobs (`automation-config`, `assets`, `gradle`) corren en **`ubuntu-latest`** (consumo `1x` frente a `2x` de Windows).
- Se ignoran cambios puramente documentales o de memoria/skills (`paths-ignore` para `MEMORY.md`, `AGENTS.md`, `GEMINI.md`, `README.md`, `docs/**`, `.agents/**`).
- `concurrency` cancela automáticamente ejecuciones previas en el mismo PR cuando se sube un nuevo commit.
- El job `gradle` anula `org.gradle.jvmargs` con `-Xmx2G` para evitar `WINDOWS-ROOT` en Linux y retiene los artefactos durante 7 días.

**Asset checks** ejecuta las mismas pruebas que `task verify:assets`. **Build and GameTests** ejecuta JUnit, compila el mod y verifica que NeoForge informe que aprobó los GameTests requeridos.

En la página de la ejecución, **Artifacts → gradle-verification** contiene la salida de Gradle y los informes JUnit disponibles. Consulta los logs de **Asset checks** para los fallos de recursos. Las pruebas gráficas, RCON y los estudios de audio con CUDA se ejecutan localmente.

## Integración con GitHub CLI (`gh`) y Taskfile

Toda la operativa diaria con GitHub está disponible en `Taskfile.yml` mediante `scripts/Invoke-Gh.ps1` (funciona tanto si `gh` está en `PATH` como en `C:\Program Files\GitHub CLI\gh.exe`, sin requerir funciones de pago como branch protection en repos privados):

| Tarea | Qué ejecuta |
| --- | --- |
| `task gh:status` | Estado de autenticación, resumen del repo, PRs e issues abiertos. |
| `task gh:ci` | Lista las últimas 10 ejecuciones de GitHub Actions. |
| `task gh:ci:watch` | Sigue en vivo la ejecución más reciente hasta que termine. |
| `task gh:pr:list` | Lista los Pull Requests abiertos. |
| `task gh:issues` | Lista los issues abiertos. |
| `task gh:release:draft` | Lanza `.github/workflows/release.yml` creando una Release en modo borrador (`draft=true`). |

## Actualizaciones de dependencias (Renovate)

`renovate.json` configura Mend Renovate sin `automerge` y optimizado para ahorrar minutos en **GitHub Free**:

- **Cadencia y control de ruido:** programación semanal (`before 6am on Monday`, `America/Chihuahua`), `prHourlyLimit: 2`, `rebaseWhen: "conflicted"`, `minimumReleaseAge: "3 days"` y etiquetas automáticas (`dependencies` en PRs y Dependency Dashboard; `area:ci-automation` en PRs de CI).
- **Agrupación:** agrupa actualizaciones en PRs revisables (`GitHub Actions & CI`, `Gradle plugins`, `NeoForge 1.21.1`, `MC 1.21.1 mod dependencies` y `Python tooling`).
- **Límites de compatibilidad MC 1.21.1 / JDK 21:** acota `neo_version` a `21.1.x`, `java-jdk` a `21`, `gradle` a `8.x`, `iris` a `+1.21.1-neoforge`, `emi` a `+1.21.1`, `jade` a `15.x`, `observable` a `5.x`, `python` a `3.12.x`, y mantiene excluidos `spark` y `kotlin-for-forge` (versionado por hash de Modrinth). También rastrea la versión fijada del validador `renovate` en `Taskfile.yml` y `.github/workflows/verify.yml`.

Ejecuta `task verify:renovate` para validar la configuración de forma estricta antes de hacer commit. La app hospedada Mend Renovate ya está instalada y activa en `Jorge-desing/mycelial-mind` (Issue `#3` Dependency Dashboard).

CI valida la configuración de Renovate y ejecuta `publishMods` con `publishDryRun=true` como parte de la verificación Gradle, comprobando que el release plugin prepara la vista previa sin publicar. Las releases locales también usan simulación por defecto. Consulta [Preparar una release](releasing.md) para generar una release sin subirla y para ver los requisitos de una publicación aprobada.

## Actualizar las dependencias de los estudios

Edita las versiones de NumPy, Pillow o scikit-image en `pyproject.toml`, ejecuta `uv lock` y vuelve a correr `task verify:assets`. Versiona el manifiesto y `uv.lock` juntos. scikit-image selecciona las operaciones de imagen usadas para generar las partículas versionadas. El servidor MCP usa su propio entorno.
