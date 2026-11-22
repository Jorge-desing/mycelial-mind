---
trigger: glob
globs: "{src/main/java/**/client/model/*Model.java,scripts/model_studio/model_defs.py,scripts/model_studio/block_item_3d.py}"
---

# Regla de Sincronización de Modelos 3D y Texturas UV

1. **Prohibido editar `*Model.java` manualmente:** Los modelos de entidad en `src/main/java/com/alex/mycelialmind/client/model/*Model.java` son artefactos generados por `scripts/model_studio/`.
2. **Flujo obligatorio tras cualquier cambio de geometría o animación:**
   - Editar `scripts/model_studio/model_defs.py`, `scripts/model_studio/block_item_3d.py` o `scripts/model_studio/java_emitter.py`.
   - Vía **FastMCP `CodeMode`** (`mycelial-mind-dev` `execute`), regenerar/validar y comprobar visualmente:
     1. `run_model_studio(check_only=False, run_tests=True)` (emite modelos + `uv_manifest.json` y ejecuta tests OBB SAT 3D/continuidad).
     2. `run_texture_studio(check_only=False, pbr=True, export_extras=True, run_tests=True)` (repinta UVs 1:1 + LabPBR 1.3 + `atlas_3d`).
     3. `run_gradle_task(tasks=["runVisualSmoke"])` + `check_visual_regression(include_diff_regions=True, include_lpips=True)` (o `run_verify_mod(include_visual_smoke=True)`).
   - Fallback por consola únicamente si `mycelial-mind-dev` no está conectado:
     - `python scripts/model_studio/build_models.py --all`
     - `python scripts/texture_studio/build_textures.py --all --export-extras --pbr`
     - `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"; .\gradlew.bat runVisualSmoke; python scripts/visual_regression.py`
