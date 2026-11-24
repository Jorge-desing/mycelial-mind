# Regla Obligatoria de Memoria Local (`MEMORY.md`)

- **Al iniciar cualquier chat, sesión o pasada (PRIMER PASO):** Antes de planificar, responder o editar, ejecuta `read_file` sobre `MEMORY.md` para consultar el contexto y las entradas recientes.
- **Al finalizar cualquier tarea con cambios, decisiones o pruebas (ÚLTIMO PASO):** Antes de tu respuesta final, anexa una nueva entrada fechada al final de `MEMORY.md` usando `write_file(..., Append=true)`.
- **Solo anexión (`append-only`):** Nunca edites, sobrescribas ni elimines entradas existentes en `MEMORY.md`.

# Uso Obligatorio de `CodeMode` (`mycelial-mind-dev`) para Auditoría y Control del Mod/Juego

- **Regla permanente:** Toda auditoría, ejecución o depuración de las suites de pruebas del proyecto y el control automatizado del mod/juego (comandos RCON `/mycelial ...`, MC Pilot, Gradle GameTest/Visual/Structure Smoke y JFR) **debe realizarse a través de FastMCP `CodeMode`** (`mycelial-mind-dev`: `search`, `get_schema`, `execute`) encadenando las comprobaciones en un bloque Python único (ver `.agents/rules/codemode-first.md` y `.agents/skills/mycelial-codemode/SKILL.md`).
- Usa los comandos `.bat`/`.ps1`/`.py` directos únicamente como fallback si el servidor MCP `mycelial-mind-dev` no está conectado en la sesión actual o al depurar `scripts/mcp/server.py`.

# Revisión, commit, push y merge obligatorios

- Revisa todos los cambios propios y corrige los problemas detectados; ejecuta las verificaciones pertinentes. Selecciona solo los archivos propios, haz commit descriptivo, push y comprueba su publicación.
- Integra cada rama creada para la tarea en su destino: actualiza la base, resuelve conflictos, revisa el diff final, comprueba los checks, haz merge y confirma el commit integrado y CI en destino. Sigue `.agents/rules/git-auto-commit.md`.
- Respeta las instrucciones explícitas del usuario; informa cualquier bloqueo de permisos, remoto, conflictos, checks o protección de rama y deja claro lo pendiente.
