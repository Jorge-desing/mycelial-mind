# Estudio de modelos de la flor recuperada

La flor debe leerse como una corola abierta desde arriba y como una planta con tallo desde el sendero. Usa el verde base como referencia de forma y comprueba también las variantes oscura y cálida bajo la luz del Micelio Profundo.

## Diseños por tipo

Cada paleta conserva su propio modelo al cosechar y al colocarse. La base tiene corola abierta; cuerpo a cuerpo forma una corona robusta; proyectil crece estrecha y erguida; fuego despliega una llama alta; explosión se abre baja y ancha; magia alza un pistilo central. Cada modelo parte de un tallo y cáliz conectados a su base.

Las caras teñidas usan `tintindex: 0`. El tallo y el cáliz usan materiales sin teñir. En inventario, cada ítem hereda el modelo correspondiente de bloque.

## Cómo continuar el diseño

1. Edita `build_reclaimed_bloom_model()` o `build_reclaimed_bloom_variant()` en `scripts/model_studio/block_item_3d.py` y regenera `models/block/reclaimed_bloom.json` mediante Model Studio.
2. Ejecuta `python -m unittest scripts.model_studio.tests.test_models -q` para comprobar UV, conectividad e interpenetración.
3. Abre `runVisualSmoke -PexpeditionVisualSmoke=true -PexpeditionKeepSmokeEntities=true` con JDK 21. Revisa las seis paletas antes y después de cosechar. Para cambios de silueta, inspecciona el perfil desde el sendero y una toma desde arriba. Para cambios en el modelo de ítem, comprueba también el inventario.
