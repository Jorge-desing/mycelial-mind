# Preparar una release

El proyecto aplica Mod Publish Plugin `1.1.0`, compatible con el wrapper actual Gradle `8.14.2`. La serie `2.x` requiere Gradle `9.0.0` o posterior, así que no se añade hasta que el wrapper y la cadena NeoForge se actualicen y verifiquen juntos.

## Simulación local

```powershell
task release:dry-run
```

La tarea construye el JAR de `jar` y ejecuta `publishMods` en modo simulación. El valor predeterminado es `publishDryRun=true`; los archivos generados quedan bajo `build/` y no se envían a GitHub.

## Publicación en GitHub

El destino configurado es `Jorge-desing/mycelial-mind`.

### Opción recomendada (GitHub Actions + Draft Release gratuita)
El workflow `.github/workflows/release.yml` compila y verifica (`test build runGameTestServer`), calcula el checksum `.sha256` del JAR y crea automáticamente una **Draft Release** con notas generadas usando `GITHUB_TOKEN`:

1. Al hacer push de un tag `v*` (por ejemplo, `git tag v2.0.0 && git push origin v2.0.0`), se crea el borrador automáticamente.
2. O manualmente con GitHub CLI:
   ```powershell
   task gh:release:draft
   ```
   Después revisa y publica el borrador desde la interfaz web de GitHub o con `scripts/Invoke-Gh.ps1 release edit <tag> --draft=false`.

### Opción manual local con Mod Publish Plugin
Solo para una release aprobada, se puede desactivar explícitamente la simulación:

```powershell
$env:GITHUB_TOKEN = '<token con permiso Contents: write>'
./gradlew.bat publishMods -PpublishDryRun=false
```

No guardes el token en `gradle.properties` ni en el repositorio.

Modrinth y CurseForge quedan fuera de esta configuración hasta que existan los proyectos correspondientes y se proporcionen sus IDs y tokens. No se inventan valores ni se almacenan credenciales.
