---
trigger: always_on
description: Protocolo obligatorio de consulta al inicio de cada chat/sesión y anexión al final en MEMORY.md
---

# Protocolo Obligatorio de Memoria Local (`MEMORY.md`)

1. **PRIMER PASO OBLIGATORIO EN CADA CHAT O SESIÓN:**
   - En tu **primer turno**, antes de planificar, responder sobre el estado del proyecto o editar cualquier archivo, ejecuta **`read_file` sobre `c:\Users\Alex\Documents\Mod Minecraft\MEMORY.md`** (o las últimas entradas si el archivo crece) para cargar la memoria y el historial reciente.
   - **No omitas este paso** aunque `AGENTS.md` ya aparezca en el contexto del sistema: `MEMORY.md` contiene la bitácora cronológica más reciente.

2. **ÚLTIMO PASO OBLIGATORIO ANTES DE CERRAR:**
   - Antes de emitir tu respuesta final en cualquier sesión o pasada donde hayas realizado cambios, tomado decisiones, ejecutado pruebas (`build`, `runGameTestServer`, `Verify-Mod.ps1`) o encontrado hallazgos relevantes, ejecuta **`write_file` con `Append: true` sobre `c:\Users\Alex\Documents\Mod Minecraft\MEMORY.md`** para agregar una nueva entrada fechada:
     ```markdown
     ## [YYYY-MM-DD HH:MM] — Título corto del hito o contexto
     - **Tipo:** `DECISIÓN` | `IMPLEMENTACIÓN` | `CORRECCIÓN` | `VERIFICACIÓN` | `NOTA`
     - **Resumen:** Descripción concisa de lo realizado o decidido.
     - **Archivos / Símbolos:** Rutas o clases relevantes.
     - **Estado / Siguiente paso:** Qué queda verificado o pendiente.
     ```

3. **Inmutabilidad estricta (`append-only`):**
   - `MEMORY.md` es exclusivamente para **leer, consultar y agregar al final**.
   - **Prohibido** usar `edit_file` o `write_file(..., Overwrite=true)` sobre `MEMORY.md`, ni editar, reordenar o eliminar entradas ya existentes.
