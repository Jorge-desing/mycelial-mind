# Preparar una release

El proyecto aplica Mod Publish Plugin `1.1.0`, compatible con el wrapper actual Gradle `8.14.2`. La serie `2.x` requiere Gradle `9.0.0` o posterior, así que no se añade hasta que el wrapper y la cadena NeoForge se actualicen y verifiquen juntos.

## Simulación local

```powershell
task release:dry-run
```

La tarea construye el JAR de `jar` y ejecuta `publishMods` en modo simulación. El valor predeterminado es `publishDryRun=true`; los archivos generados quedan bajo `build/` y no se envían a GitHub.

## Publicación en GitHub

El destino configurado es `Jorge-desing/mycelial-mind`. Solo para una release aprobada, se puede desactivar explícitamente la simulación:

```powershell
$env:GITHUB_TOKEN = '<token con permiso Contents: write>'
./gradlew.bat publishMods -PpublishDryRun=false
```

No guardes el token en `gradle.properties` ni en el repositorio. La tarea no está conectada a un evento automático de GitHub Actions: ejecutar el comando anterior es una acción deliberada de publicación.

Modrinth y CurseForge quedan fuera de esta configuración hasta que existan los proyectos correspondientes y se proporcionen sus IDs y tokens. No se inventan valores ni se almacenan credenciales.
