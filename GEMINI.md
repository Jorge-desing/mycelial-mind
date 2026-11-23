# Regla Obligatoria de Memoria Local (`MEMORY.md`)

- **Al iniciar cualquier chat, sesión o pasada (PRIMER PASO):** Antes de planificar, responder o editar, ejecuta `read_file` sobre `MEMORY.md` para consultar el contexto y las entradas recientes.
- **Al finalizar cualquier tarea con cambios, decisiones o pruebas (ÚLTIMO PASO):** Antes de tu respuesta final, anexa una nueva entrada fechada al final de `MEMORY.md` usando `write_file(..., Append=true)`.
- **Solo anexión (`append-only`):** Nunca edites, sobrescribas ni elimines entradas existentes en `MEMORY.md`.

# Uso Obligatorio de `CodeMode` (`mycelial-mind-dev`) para Auditoría y Control del Mod/Juego

- **Regla permanente:** Toda auditoría, ejecución o depuración de las suites de pruebas del proyecto y el control automatizado del mod/juego (comandos RCON `/mycelial ...`, MC Pilot, Gradle GameTest/Visual/Structure Smoke y JFR) **debe realizarse a través de FastMCP `CodeMode`** (`mycelial-mind-dev`: `search`, `get_schema`, `execute`) encadenando las comprobaciones en un bloque Python único (ver `.agents/rules/codemode-first.md` y `.agents/skills/mycelial-codemode/SKILL.md`).
- Usa los comandos `.bat`/`.ps1`/`.py` directos únicamente como fallback si el servidor MCP `mycelial-mind-dev` no está conectado en la sesión actual o al depurar `scripts/mcp/server.py`.

# Commit y Push Obligatorio tras cada Sesión o Cambios Significativos

- **Regla permanente:** Al finalizar cada sesión o tras realizar **cambios significativos entre pasadas**, ejecuta siempre `git status`, `git add` de los archivos propios de la tarea, `git commit` descriptivo y `git push` hacia `origin` (ver `.agents/rules/git-auto-commit.md`), salvo indicación explícita del usuario en contra o si existen errores de compilación/pruebas sin resolver que rompan la rama principal.
