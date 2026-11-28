# Regla Obligatoria de Memoria Local (`MEMORY.md`)

- **Al iniciar cualquier chat, sesión o pasada (PRIMER PASO):** Antes de planificar, responder o editar, ejecuta `read_file` sobre `MEMORY.md` para consultar el contexto y las entradas recientes.
- **Al finalizar cualquier tarea con cambios, decisiones o pruebas (ÚLTIMO PASO):** Antes de tu respuesta final, anexa una nueva entrada fechada al final de `MEMORY.md` usando `write_file(..., Append=true)`.
- **Solo anexión (`append-only`):** Nunca edites, sobrescribas ni elimines entradas existentes en `MEMORY.md`.

# Uso Obligatorio de `CodeMode` (`mycelial-mind-dev`) para Auditoría y Control del Mod/Juego

- **Regla permanente:** Toda auditoría, ejecución o depuración de las suites de pruebas del proyecto y el control automatizado del mod/juego (comandos RCON `/mycelial ...`, MC Pilot, Gradle GameTest/Visual/Structure Smoke y JFR) **debe realizarse a través de FastMCP `CodeMode`** (`mycelial-mind-dev`: `search`, `get_schema`, `execute`) encadenando las comprobaciones en un bloque Python único (ver `.agents/rules/codemode-first.md` y `.agents/skills/mycelial-codemode/SKILL.md`).
- Usa los comandos `.bat`/`.ps1`/`.py` directos únicamente como fallback si el servidor MCP `mycelial-mind-dev` no está conectado en la sesión actual o al depurar `scripts/mcp/server.py`.

# Revisión, commit, push y merge obligatorios

- Antes de cada push, verifica los checks del SHA actual de la rama de destino y corrige los fallos; ejecuta en local los controles de CI aplicables. No subas cambios ordinarios con checks rojos, pendientes o sin evidencia suficiente. Si GitHub necesita un push para comprobar una reparación, publica únicamente la reparación acotada y validada y espera sus checks antes de continuar. Sigue `.agents/rules/git-auto-commit.md`.
- Revisa todos los cambios propios y corrige los problemas detectados; ejecuta las verificaciones pertinentes. Selecciona solo los archivos propios, haz commit descriptivo, push y comprueba su publicación.
- Tras cada `commit` y `push`, verifica los checks/workflows de GitHub del commit publicado (`scripts/Invoke-Gh.ps1` / `task gh:ci:watch` / `gh pr checks`); si detectas fallos de código, tests, assets o configuración, lee los logs (`gh run view <id> --log-failed`), corrígelos, vuelve a subir y re-verifica hasta quedar en verde (o informa el bloqueo exacto si es externo, como cuota de GitHub Actions).
- Integra toda rama creada para la tarea en su destino y sincroniza siempre los cambios del PR: antes del merge actualiza la rama del PR con `origin/<destino>`, resuelve conflictos, revisa el diff final, comprueba los checks requeridos y haz merge; inmediatamente después del merge (o si el checkout local quedó detrás de `origin/<destino>`), sincroniza siempre la copia local de la rama de destino en el workspace principal (`git fetch origin` + `--ff-only`, resguardando y reaplicando cambios locales propios/ajenos y entradas *append-only* de `MEMORY.md` sin perder ni las del PR ni las locales), y confirma que el commit integrado y su CI en destino terminaron en verde antes de limpiar worktrees/ramas temporales. Sigue `.agents/rules/git-auto-commit.md`.
- Respeta las instrucciones explícitas del usuario; informa cualquier bloqueo de permisos, remoto, conflictos, checks o protección de rama y deja claro lo pendiente.

# Objetivo del Mod e Iniciativa Inédita (Obligatorio)

- **Preservar el objetivo actual:** Mantén intacto el núcleo de *Mycelial Mind* (organismo micelial subterráneo adaptativo con Mente Colmena por dimensión, memoria de combate/hábitos, red sináptica, homeostasis dimensional, simbiosis, Colonia Soberana y contraofensiva biotecnológica).
- **Iniciativa inédita y única en cada cambio buscado:** Toda nueva mecánica, sistema o evolución diseñada para el mod debe aportar **una iniciativa inédita, irrepetible y que ningún otro mod de Minecraft tenga**, evitando mecánicas genéricas de infección, escalados planos o fórmulas copiadas. Antes de planificar o implementar nuevas features de gameplay, justifica qué hace única la propuesta y cómo profundiza el ecosistema vivo del mod (ver `.agents/rules/mod-vision-and-originality.md`).

## Higiene del Repositorio y Gestión de Archivos (Obligatorio)

Para evitar la acumulación de archivos obsoletos, código muerto o basura de pruebas, cumple estrictamente estas reglas en cada tarea:

1. **Zona Temporal Aislada (`.agent-tmp/`):**
   - Nunca crees scripts de Python, bash, logs, JSONs de prueba o notas temporales en la raíz del proyecto ni dentro de `src/`.
   - Si necesitas generar un script o archivo temporal para depurar o verificar datos, guárdalo exclusivamente en `.agent-tmp/` (asegúrate de que esté en `.gitignore`) y elimínalo antes de finalizar la tarea.

2. **Control de Tests y GameTests (`src/test` y `src/gametest`):**
   - Antes de crear una clase de prueba nueva, revisa las existentes para ampliar la que corresponda al módulo.
   - Si refactorizas, renombras o eliminas una clase/método en `src/main`, **debes actualizar o borrar inmediatamente** sus tests asociados para no dejar tests rotos ni huérfanos.

3. **Artifacts y Planes de Trabajo (Antigravity):**
   - No generes múltiples *Artifacts* de planificación para una misma funcionalidad; actualiza y sobrescribe el documento activo.
   - Al completar una implementación, marca el *Artifact* como finalizado o limpia los borradores intermedios que ya no aporten valor.

4. **Recursos y Datagen (`src/generated` vs `src/main/resources`):**
   - No edites a mano ni dupliques JSONs dentro de `src/generated/resources` si pertenecen al pipeline de *Data Generation* de NeoForge; modifica el *DataProvider* en Java y limpia archivos residuales de bloques/ítems eliminados.

5. **Autolimpieza Pre-Entrega:**
   - Antes de dar por terminada tu respuesta, ejecuta una revisión rápida de los archivos creados o modificados (`git status`) y elimina cualquier archivo auxiliar, clase vacía o importación sin usar que haya quedado como residuo.