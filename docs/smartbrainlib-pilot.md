# Piloto de SmartBrainLib para el Acid Spitter

**Estado** · Implementado. La sección de resultados recoge la validación del piloto.

## Objetivo

Dar al Acid Spitter un perfil de combate a distancia que use el contexto de la Colmena. Debe poder buscar una posición de tiro, disparar con línea de visión y completar el repliegue cuando la utilidad táctica lo justifique.

El mod ya usa SmartBrainLib 1.16.11 como dependencia obligatoria. El perfil heredado de `MycelialMob` ofrece persecución y ataque cuerpo a cuerpo, mientras que el Acid Spitter registra goals de disparo, repliegue y esquiva. La revisión estática confirma la coexistencia. La línea base en juego deberá determinar si se disputan el objetivo o la navegación y con qué frecuencia.

## Implementación

1. **Capturar la línea base.** En un mundo aislado de NeoForge 21.1.176, registrar el comportamiento del Acid Spitter con un blanco visible, detrás de cobertura y durante un repliegue. Registrar disparos, cambios de objetivo y recálculos de ruta.
2. **Dar al Acid Spitter su propio perfil.** Especializar sus actividades de SmartBrainLib y retirar de este arquetipo los goals de combate, selección de objetivo y movimiento que queden sustituidos. Conservar la flotación como prioridad de emergencia y hacer que el perfil ceda la navegación mientras esté activa. Revisar las ramas del árbol táctico que escriban objetivos o rutas para que el Brain ejecute esas decisiones. Mantener el perfil compartido de las demás especies.
3. **Componer las decisiones de combate.** Priorizar repliegue, disparo y búsqueda de posición, en ese orden. Reutilizar el cálculo de utilidad, el proyectil y la rutina de disparo del mob. Usar como referencia inicial el alcance de 16 bloques y el intervalo de 35 ticks declarados en el goal actual. Al interrumpir una acción, liberar su destino de marcha y reevaluar. Aplicar las reglas vigentes de selección de objetivo, diplomacia y represalia también al invalidar un blanco.
4. **Consumir las señales existentes.** Usar `HiveMindSensor` y las memorias del Brain como entrada. Con `RALLY_NODE`, buscar un nodo propio válido y accesible cuando corresponda reagruparse. Con `PRESSURE_TARGET`, usar la posición compartida para buscar un tiro dentro de alcance. La posición compartida orienta el movimiento. El disparo requiere una entidad válida y visible. Ignorar órdenes vencidas y nodos destruidos, descargados o protegidos. Mantener `HiveMindSavedData` como fuente de aprendizaje persistente y `HiveBlackboard` como fuente de señales temporales por nivel. El piloto utiliza las memorias existentes y prescinde de cambios de NBT y de red.

## Validación y aceptación

- Ejecutar compilación, GameTests y smoke de servidor mediante CodeMode. Añadir cobertura focalizada para las transiciones de disparo a repliegue y de repliegue a combate, y para invalidación de objetivos.
- En juego, comprobar que el mob dispara con línea de visión y dentro de alcance, busca una posición al perder visibilidad, completa el repliegue sin que otra acción sobrescriba su ruta y libera objetivos muertos o protegidos según las reglas de diplomacia. Probar además una orden vencida, un nodo destruido y una ruta interrumpida por flotación.
- Comparar antes y después en el mismo mundo aislado, con la misma posición del jugador, dificultad y composición de mobs. Usar ventanas de 60 segundos tras el calentamiento. Guardar cadencia efectiva, recálculos de ruta y MSPT mediano y p95. Repetir cualquier degradación observada para distinguirla de la variación entre pasadas.
- Aceptar el piloto cuando cumpla los casos funcionales, mantenga la cadencia de referencia en condiciones equivalentes y la comparación repetida descarte una regresión de ruta o MSPT atribuible al cambio. Si falla, revertir los cambios del perfil del Acid Spitter. Tras la aceptación, evaluar al Stalker con `FLANK_PERIMETER` como siguiente arquetipo.

## Referencias

- [Perfil SBL compartido](../src/main/java/com/alex/mycelialmind/compat/sbl/MycelialSblBridge.java)
- [Acid Spitter](../src/main/java/com/alex/mycelialmind/entity/AcidSpitterEntity.java)
- [Wiki de SmartBrainLib](https://github.com/Tslat/SmartBrainLib/wiki)

## Resultado del piloto

El perfil de SmartBrainLib del Acid Spitter concentra la selección de objetivo y la navegación. Conserva el proyectil y su cadencia de 35 ticks, la flotación de emergencia, la esquiva ante armas a distancia y el atasco corrosivo de torretas. El árbol táctico sigue calculando intenciones para la telemetría. Al recibir `RALLY_NODE` tras la pérdida de un nodo, el sensor busca un nodo propio superviviente y cargado. El perfil comprueba que el nodo siga siendo válido y que la ruta pueda alcanzarlo.

CodeMode verificó el build, 30 pruebas JUnit, 131 GameTests y 21 escenarios RCON. En tres capturas de 1.200 ticks con 12 Acid Spitters, el piloto disparó 408 veces por captura y sostuvo la posición de tiro sin búsquedas de ruta. El perfil anterior registró 0 disparos y 3.600 búsquedas en el mismo montaje. La versión de entrega midió 2,03 ms de MSPT mediano y 3,26 ms de p95, frente a 2,80 y 3,88 ms en la línea base. [Resultados y trazas](../artifacts/diagnostics/sbl-pilot/results.md)

El GameTest del piloto verificó que el mob alcance por sí solo la distancia mínima tras replegarse, reanude el disparo y suelte el objetivo retirado. Las pruebas en mundo comprobaron pérdida de línea de visión, diplomacia, flotación, selección de un relé superviviente y caducidad de órdenes y avisos.

El Stalker utiliza `StalkerFlankBehaviour` para publicar en su Brain un destino calculado por el coordinador de enjambre mientras está vigente `FLANK_PERIMETER`. `SwarmFlankGoal` controla la navegación y consume ese destino. El perfil conserva los goals de salto y ataque. Los GameTests verifican lados opuestos, publicación para dos Stalkers, invalidación por orden, objetivo o desincronización y salto durante combate. En un servidor aislado dos Stalkers adquirieron un Brute rival y combatieron cerca de él.
