# Estudio de modelos de la flor recuperada

La flor debe leerse como una corola abierta desde arriba y como una planta con tallo desde el sendero. Usa el verde base como referencia de forma y comprueba también las variantes oscura y cálida bajo la luz del Micelio Profundo.

## Diseños por tipo

Cada paleta conserva su propio modelo al cosechar y al colocarse. La base tiene corola abierta; cuerpo a cuerpo forma una corona robusta; proyectil crece estrecha y erguida; fuego despliega una llama alta; explosión se abre baja y ancha; magia alza un pistilo central. Cada modelo parte de un tallo y cáliz conectados a su base.

Cada variante usa su propio atlas `reclaimed_bloom_<tipo>.png`, generado por `scripts/texture_studio/generators/reclaimed_blooms.py`. Los colores se incluyen en la textura. El tallo, la raíz y el cáliz tienen materiales propios. En inventario, cada ítem hereda el modelo correspondiente de bloque. Las regiones de 32 × 32 píxeles corresponden a la cuadrícula 4 × 4 usada por Model Studio. Los mosaicos (0,0), (1,0), (1,2), (2,0) y (1,3) corresponden a raíz, tallo, cáliz, núcleo y pétalos respectivamente.

## Cómo continuar el diseño

1. Edita `build_reclaimed_bloom_model()` o `build_reclaimed_bloom_variant()` en `scripts/model_studio/block_item_3d.py` y regenera `models/block/reclaimed_bloom.json` mediante Model Studio.
2. Ejecuta `python -m unittest scripts.model_studio.tests.test_models scripts.texture_studio.tests.test_reclaimed_blooms -q` para comprobar UV, conectividad, interpenetración, texturas, estados e ítems.
3. Abre `runVisualSmoke -PexpeditionVisualSmoke=true -PexpeditionKeepSmokeEntities=true` con JDK 21. Revisa las seis paletas antes y después de cosechar. Para cambios de silueta, inspecciona el perfil desde el sendero y una toma desde arriba. Para cambios en el modelo de ítem, comprueba también el inventario.

Cada atlas incluye un PNG `_particle` de 16 × 16 para las partículas de rotura. Regenera ambos con `generate_reclaimed_blooms` o con `build_textures.py --blocks`. Para una ejecución aislada usa `-PexpeditionSmokeDirectory=artifacts/diagnostics/expedition-bloom-final`. Las capturas de la escena incluyen el jardín antes y después de cosechar, una vista superior y el inventario.

Las cinco variantes incorporan tres niveles de hojas y placas conectadas al tallo, con 24–30 piezas; la flor base conserva 42. Para evitar capturas durante la transición dimensional, la escena espera a que desaparezca `ReceivingLevelScreen` y encuadra la fila antes de capturar el perfil.
