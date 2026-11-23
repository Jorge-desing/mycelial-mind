# Regla: Commit y Push Obligatorio tras cada Sesión o Cambios Significativos

- **Alcance:** Aplica a cualquier agente en cualquier sesión o pasada dentro del proyecto cuando exista repositorio Git inicializado y remoto configurado (`origin`).
- **Cuándo ejecutar:** **Siempre** al finalizar cada sesión o tras realizar **cambios significativos entre pasadas** (código, configuración, documentación, reglas, skills, tests, assets o correcciones verificadas).
- **Qué hacer:**
  1. Verificar `git status` para asegurarse de no incluir archivos temporales, secretos ni cambios ajenos/no relacionados con la tarea.
  2. Hacer `git add` únicamente de los archivos correspondientes a la tarea o pasada actual.
  3. Hacer `git commit` con un mensaje claro y descriptivo (ej. `feat(...)`, `fix(...)`, `docs(...)`, `chore(...)`).
  4. Hacer `git push` al remoto configurado (`origin`) y confirmar que la rama queda sincronizada.
- **Excepciones:** No hacer commit/push si el usuario pidió explícitamente no hacerlo, si hay errores de compilación/pruebas sin resolver que rompan la rama principal, o si no hay remoto configurado.
