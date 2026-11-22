# Regla: Commit y Push Automático tras Cambios Relevantes

- **Alcance:** Aplica a cualquier agente en cualquier sesión o pasada dentro del proyecto cuando exista repositorio Git inicializado y remoto configurado (por ejemplo, `origin`).
- **Cuándo ejecutar:** Al finalizar una pasada o sesión en la que se hayan realizado cambios que lo ameriten (código, configuración, documentación, reglas, skills, tests o correcciones verificadas).
- **Qué hacer:**
  1. Verificar `git status` para asegurarse de no incluir archivos temporales, secretos ni cambios no relacionados.
  2. Hacer `git add` de los archivos correspondientes a la tarea.
  3. Hacer `git commit` con un mensaje claro y descriptivo (ej. `feat(...)`, `fix(...)`, `docs(...)`, `chore(...)`).
  4. Hacer `git push` al remoto configurado (`origin`) y confirmar que la rama queda sincronizada.
- **Excepciones:** No hacer commit/push si el usuario pidió explícitamente no hacerlo, si hay errores de compilación/pruebas sin resolver que rompan la rama principal, o si no hay remoto configurado.
