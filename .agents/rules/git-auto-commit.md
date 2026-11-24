# Regla: Revisión, commit, push y merge obligatorios

- **Alcance:** Todos los agentes, en cada sesión y tras cambios significativos entre pasadas.
- Revisa el estado y el diff completo de los cambios propios, incluidos archivos nuevos; corrige los problemas encontrados y realiza las verificaciones pertinentes. No dejes cambios propios sin revisar. Conserva los cambios ajenos y registra las limitaciones reales.
- Selecciona solo los cambios propios, crea un commit descriptivo, haz push a `origin` y comprueba que el remoto contiene el commit. Si no hay cambios propios, no crees un commit vacío.
- Toda rama creada para la tarea debe integrarse en su rama de destino: actualiza la base, resuelve conflictos, revisa el diff final, verifica los checks requeridos y haz merge. Confirma que el commit integrado está en la rama de destino y que su publicación y CI terminaron correctamente. Usa un PR cuando corresponda.
- La instrucción explícita del usuario prevalece. Si faltan permisos o remoto, hay conflictos sin resolver, verificaciones fallidas o una protección de rama impide el merge, conserva el trabajo, informa del bloqueo y deja constancia exacta de lo pendiente; no declares completada la integración. No incluyas cambios ajenos, secretos ni temporales.
