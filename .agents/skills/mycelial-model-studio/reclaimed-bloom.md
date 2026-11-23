# Estudio de modelos de la flor recuperada

La flor debe leerse como una corola abierta desde arriba y como una planta con tallo desde el sendero. Usa el verde base como referencia de forma y comprueba también las variantes oscura y cálida bajo la luz del Micelio Profundo.

## Anatomía y proporciones

| Zona | Decisión de diseño |
|---|---|
| Base, Y 0 a 4 | Cepa ancha y baja con cuatro hojas. Dos hojas opuestas se levantan para que no parezca una peana simétrica. |
| Tallo y cáliz, Y 1,5 a 7,25 | Tallo estrecho y oscuro, con un ensanche que sostiene la corola. Conserva el hueco entre la base y los pétalos. |
| Corola, Y 5,75 a 10,5 | Cuatro pétalos largos con puntas alternas en altura y cuatro lóbulos bajos que rellenan parcialmente los huecos. Desde arriba se deben distinguir las puntas de los pétalos principales. |
| Pistilo, Y 7,25 a 10,75 | Cuello estrecho y cabeza más ancha, visibles por encima de los lóbulos bajos. |

La geometría se dibuja en una cuadrícula Minecraft de 16 unidades. El generador gira las piezas en cuartos de vuelta alrededor de X=8 y Z=8. Las uniones deben compartir superficie y las piezas adyacentes deben conservar su separación para evitar caras que se pisan.

## Materiales, paletas y cosecha

- Aplica `tintindex: 0` a la corola y al pistilo. Usa marfil en las superficies expuestas y hueso en la parte baja para conservar contraste en las seis paletas.
- Usa el atlas de la Colmena para el tallo, el cáliz y las hojas sin teñir. Así se reconoce la misma planta en las seis variantes.
- Los dos estados de cosecha y los seis modelos de inventario heredan la misma geometría. Mantén el color y la silueta del jardín después de recoger la flor.

## Cómo continuar el diseño

1. Edita `build_reclaimed_bloom_model()` en `scripts/model_studio/block_item_3d.py` y regenera `models/block/reclaimed_bloom.json` mediante Model Studio.
2. Ejecuta `python -m unittest scripts.model_studio.tests.test_models -q` para comprobar UV, conectividad e interpenetración.
3. Abre `runVisualSmoke -PexpeditionVisualSmoke=true -PexpeditionKeepSmokeEntities=true` con JDK 21. Revisa las seis paletas antes y después de cosechar. Para cambios de silueta, inspecciona el perfil desde el sendero y una toma desde arriba. Para cambios en el modelo de ítem, comprueba también el inventario.
