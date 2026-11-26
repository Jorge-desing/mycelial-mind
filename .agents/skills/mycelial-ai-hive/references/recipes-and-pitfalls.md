# Recetas Paso a Paso, Comandos, CodeMode y Trampas Comprobadas

Guía práctica para implementar o extender IA/subsistemas en `HiveBrain 3.0`, operar diagnósticos con `/mycelial` y `CodeMode` ([mycelial-codemode](../../mycelial-codemode/SKILL.md) / [mycelial-diagnostics](../../mycelial-diagnostics/SKILL.md)), y evitar regresiones conocidas.

---

## 1. Recetas de Ingeniería

### Receta 1 — Migrar o crear entidad `HiveBrainOwner<T>` (estándar 3.0)
1. **Contrato de interfaz:** Declara `implements HiveBrainOwner<MiEntidad>` (nunca dependas de librerías cerebrales externas, Access Transformers ni Mixins).
2. **Campo e inicialización:**
   - Campo `@Nullable private transient HiveBrain<MiEntidad> hiveBrain;` (o subclase compartida como `MycelialMob`/`MycelialBossMob`).
   - Implementa `getHiveBrain()` / `setHiveBrain(@Nullable HiveBrain<MiEntidad>)` y `replaceVanillaBrain(Brain<MiEntidad> newBrain)` (en `LivingEntity.brain`, `protected`, sin reflection/AT).
   - En `registerGoals()` o constructor de IA, inicializa con `HiveBrainFactory` (cachea memorias por `Class<?>` e incluye automáticamente memorias de `getBrainProfiles()`).
3. **Sensores, Actividades y Tareas:**
   - Sobrescribe `getSensors()` usando `HiveBrainBridge.createDefaultHiveSensors()` (o `createDefaultHiveSensorsWithProjectiles()` si necesitas `PROJECTILE_INCOMING`).
   - Define `getCoreTasks()`, `getIdleTasks()`, `getFightTasks()`, `getAdditionalTasks()` (opcional `Activity.INVESTIGATE`/`REST`/`PANIC`) y `getActivityPriorities()`.
4. **Tick, LOD, Pizarrón y Persistencia:**
   - `shouldTickBrain(ServerLevel, MiEntidad)` -> `return HiveBrainOwner.evaluateStandardLod(level, this, isAggressiveOrAlert);`
   - `customServerAiStep()` -> `if (this.shouldTickBrain(serverLevel, this)) { this.tickBrain(this); }` (coexistiendo limpiamente con cualquier `Goal` legado).
   - En `addAdditionalSaveData` / `readAdditionalSaveData`, invoca `saveBrainState(tag)` / `loadBrainState(tag)`.
5. **Verificación obligatoria:** Cubre transición idle->combate, LOD wake por señal y persistencia NBT en `@GameTest` (`isolated_gametests=True`) + lógica pura de claves/perfil en JUnit (`HiveBrainEngineUnitTest`).

### Receta 2 — Agregar `HiveBrainProfile`, `HiveBrainModule`, canales exclusivos y señales
1. **Perfiles dinámicos (`getBrainProfiles()`):**
   - Devuelve `Map.of("nombre_perfil", HiveBrainProfile.<MiEntidad>builder()...)` con `lodIntervalMultiplier`, prioridades, sensores o grupos de actividad específicos.
   - Cambia de estado en caliente llamando `applyBrainProfile(serverLevel, this, "nombre_perfil")` (reconstruye `BrainActivityGroup` preservando memorias y registra `EventKind.PROFILE` en `HiveBrainTrace`).
2. **Módulos continuos (`getBrainModules()`):**
   - Registra `HiveBrainModule<MiEntidad>` (como `"tactical_btree"`) para lógica ortogonal que deba respetar pausa de diagnóstico (`/mycelial ai pause`) y LOD en `HiveBrain.tick()`.
3. **Arbitraje de canales (`HiveResourceChannel`):**
   - Usa `.requireChannels(HiveResourceChannel.MOVE, ...)` + prioridad explícita **solo** en `HiveBehaviour` nuevos diseñados para exclusión mutua (`HiveResourceArbiter`). No lo actives sobre tareas legadas concurrentes que dependan de correr en paralelo.
4. **Señales e interrupciones (`HiveBrainSignal`):**
   - Emite `dispatchBrainSignal(level, entity, signal)` (o `enqueueBrainSignal(signal)` desde ticks pasivos/eventos).
   - Maneja en `.interruptOnSignal(SignalType.PROJECTILE_INCOMING, ...)` en el `HiveBehaviour` o en `onBrainSignal(level, entity, brain, signal)` de la entidad (despertará entidades throttled por LOD y quedará trazado en `HiveBrainTrace`).

### Receta 3 — Añadir `StimulusType`, `AdaptationTrait`, `PlayerHabit` o `HiveOrder`
1. **Enum & Pure Logic First (`hive/`):**
   - Añade el valor en `StimulusType`, `AdaptationTrait`, `PlayerHabit` o `HiveOrder`, manteniendo sus métodos de cálculo como funciones puras sin dependencias de registro para testearlos en JUnit (`HiveAlgorithmsUnitTest`).
2. **Telemetría y Persistencia (`HiveMindSavedData`, `DamageTelemetryTracker`, `HabitTelemetryTracker`):**
   - Mapea la fuente de daño en `DamageTelemetryTracker.classifyDamage` / `ModdedDamageClassifier` (o patrón del jugador en `HabitTelemetryTracker`, contando únicamente mobs vivos no amistosos).
   - Añade persistencia NBT en `HiveMindSavedData` y verifica retrocompatibilidad cuando el campo falte en saves antiguos.
3. **Sincronización, Audio, Localización y Escáner:**
   - Si se expone a cliente, actualiza `HiveSyncPayload` / `DiscoverySyncPayload` y `ModNetworking` (manteniendo `ClientHiveCache` sincronizado).
   - Añade claves EN/ES en `src/main/resources/assets/mycelialmind/lang/{en_us,es_es}.json` (paridad exacta) y consulta `BioScannerItem` / `MycelialJadePlugin`.
4. **DataGen y CodeMode:** actualiza `get_hive_ai_overview()` si introduces un nuevo enum raíz, corre `test_filter="HiveAlgorithmsUnitTest"`, `audit_mod_assets()` y `runGameTestServer`.

### Receta 4 — Extender Jefes Multi-Parte o Colonia Soberana / Dialectos
- **Jefes (`MycelialBossMob` + `BossCombatArchitectureEngine`):**
  - Toda nueva habilidad debe encajar en `IDLE -> WINDUP -> ACTIVE -> RECOVERY` / `STAGGERED`, respetar `TELEGRAPH_LOCK_TICKS = 7`, añadir su `MoveType` al selector de utilidad y escalar integridad/poses con `HiveCycleEngine` y `scripts/model_studio/java_emitter.py`.
  - Usa `EntityDataAccessor` para estados visuales/HUD y valida hitboxes `MycelialBossPart` (`WEAK_POINT` / `ARMORED_CARAPACE`).
- **Colonia Soberana y Dialectos (`colony/`, `entity/ai/colony/`):**
  - Nuevas tareas de `ColonySymbiontEntity` deben reservar mediante `ColonyOrderBoard` (`DEFAULT_LEASE_TICKS = 200L`), actualizar `nutrition`/`fatigue`/`morale` y respetar inmunidad aliada + `ColonySiteMemory` ligado al sitio (nunca filtrar recuerdos locales como regla global).
  - Si introduces nuevas clases de órdenes/memorias/pulsos, sincroniza `SovereignColonySyncPayload`, `SovereignNexusScreen`, `BioScannerItem`, `MycelialJadePlugin` y `SovereignColonyUnitTest` + `colony_dialect_integration`.

---

## 2. Catálogo de Comandos In-Game (`/mycelial ...`)

Definidos en `src/main/java/com/alex/mycelialmind/command/MycelialCommands.java`:

| Comando | Uso |
| --- | --- |
| `/mycelial status` | Amenaza global, Acto actual, rasgos N1/N2, sinergias y Marea Fúngica |
| `/mycelial reset` | Reinicia estado de la Mente Colmena en la dimensión |
| `/mycelial stimulus <tipo> <cantidad>` | Inyecta estímulo (`MELEE`, `PROJECTILE`, `FIRE`, `EXPLOSION`, `MAGIC`) |
| `/mycelial ai [radio\|trace\|profile\|pause\|signal]` | Inspección y control vivo de `HiveBrain`: radio, traza `16` eventos, cambiar perfil, pausar/reanudar tick o disparar señales |
| `/mycelial colony [status\|incubate\|upgrade\|raid]` | Estado de la Colonia Soberana, incubar simbiontes, subir Tier o forzar raid rival |
| `/mycelial cycle <status\|start\|abort\|reset>` | Protocolo de Ciclos Post-Matriarca y mutadores activos |
| `/mycelial homeostasis <status\|stress\|sedate>` | Inspección y control de homeostasis dimensional (`deep_mycelium`) |
| `/mycelial diplomacy <amber\|crimson> <valor>` / `schism` | Reputación de cepas e inducción de guerra civil |
| `/mycelial symbiosis` / `habits` / `routes` | Ranuras simbióticas, hábitos detectados y red hifal de rutas |
| `/mycelial filament <posA> <posB>` | Invoca hebra 3D `HyphalVeinEntity` entre dos anclajes |
| `/mycelial audio` / `/mycelial worldgen audit` | Telemetría de audio/EFX y auditoría worldgen multi-dimensión |
| `/mycelial verify <escenario\|all>` | Ejecuta cualquiera de los `24` escenarios RCON aislados (`hive_ai`, `tactical_ai`, `field_ai`, `colony`, `cycles`, `homeostasis`, `hyphal_siege`, `sinkhole`, `loot`, `polish`, `dimension_progression`, etc.) o `all` |

---

## 3. Playbooks Rápidos en FastMCP `CodeMode` (`mycelial-mind-dev`)

```python
import asyncio
# 1) Inspección rápida de arquitectura HiveBrain + enums + conteo de tests
overview, units, gametests = await asyncio.gather(
    call_tool("get_hive_ai_overview", {}),
    call_tool("list_unit_tests", {"suite_filter": "Hive"}),
    call_tool("list_gametests", {"batch_filter": "hive"}),
)
return {
    "brain_files": overview.get("brain_files_count"),
    "brain_core_enums": overview.get("brain_core_enums"),
    "unit_suites": units,
    "gametest_batches": gametests,
}
```

```python
# 2) Suite de verificación IA / Colonia (JUnit + @GameTest aislado)
junit = await call_tool("run_gradle_task", {
    "tasks": ["test"],
    "test_filter": "HiveBrainEngineUnitTest",
    "no_daemon": True,
    "max_workers": 1,
})
gt = await call_tool("run_gradle_task", {
    "tasks": ["runGameTestServer"],
    "isolated_gametests": True,
    "no_daemon": True,
    "max_workers": 1,
})
rpt = await call_tool("get_latest_junit_report", {})
return {"junit_ok": junit["ok"], "gt_ok": gt["ok"], "report": rpt}
```

> **Nota `:pitest`:** Evita `tasks=["test", "pitest"]` mientras exista la incompatibilidad conocida de `afterEvaluate` en `build.gradle`; usa `run_gradle_task(["runData", "test", "build", "runGameTestServer"], isolated_gametests=True)` + `get_latest_junit_report()` + `run_live_scenario(preset="audio_and_ai")` / `colony_and_dialects` para verificación completa.

---

## 4. Checklist de Trampas Comprobadas (Proven Pitfalls)

1. **`ModMemoryTypes.<clinit>` en JUnit 5 puro:** carga `BuiltInRegistries` sin bootstrap FML. Prueba lógica pura (`HiveBrainKey`, `HiveBrainBlackboard`, `HiveBrainSignal`, `HiveBrainSnapshot`, `HiveResourceArbiter`, `HiveBrainTrace`, `*Engine`) en JUnit 5; prueba memorias vanilla y `rebuildBrainForProfile` en `@GameTest`.
2. **`AllApplicableBehaviours` con `noTimeout()`:** por defecto NO reinicia hijos detenidos mientras otro hijo `noTimeout()` siga ejecutándose salvo activar `.reEvaluateStoppedChildren(true)`.
3. **Instanciación en `HiveBrainFactory`:** cachea memorias por `Class<?>` (incluidas las de `getBrainProfiles()`) y limpia los `ThreadLocal` de sensores/grupos en `finally` para impedir doble instanciación o fugas.
4. **Arbitraje de canales (`requireChannels`):** opt-in exclusivo en `HiveBehaviour` nuevos; activarlo en tareas legacy rompe la ejecución paralela esperada con `GoalSelector`.
5. **Visibilidad y ventanas en `@GameTest` sobre `empty.nbt`:** limpia `Blocks.AIR` en `y = 1..3` antes de comprobar `hasLineOfSight`, espera `gameTime > threshold` antes de validar ventanas de tiempo y cerca/fuerza chunks (`forceArenaChunks` / `ServerLevel.setChunkForced`) para evitar congelamiento al cruzar bordes de chunk.
6. **Iluminación en `HyphalSiegeDirector`:** **nunca** llames `ThreadedLevelLightEngine.runLightUpdates()` en NeoForge `1.21.1` (`UnsupportedOperationException`); usa `measureEffectiveBlockLight(...)`.
7. **Worldgen en runtime (`TerrainPlacementPolicy`):** **nunca** llames `findNearestMapStructure` durante generación de terreno (deadlock/lag de chunk); consulta únicamente `level.hasChunk(...)` + `ChunkStatus.STRUCTURE_REFERENCES`.
8. **Estado entre partidas (`HiveBlackboard` / `Client*Cache`):** limpia `HiveBlackboard` en `LevelEvent.Unload` por instancia de `ServerLevel` y limpia cachés cliente (`ClientHiveCache`, `ClientExpeditionCache`, `ClientColonyCache`, `ClientCycleCache`) al desconectar.
9. **Decay residual (`HiveMindSavedData.tickDecay`):** sigue reduciendo `traitMastery` en `0.05/min` aunque el puntaje del estímulo ya haya bajado a `0.0`, evitando dominios permanentes fantasma.
10. **Rendimiento y render de jefes (`RENDER_SPECS`):** preserva `aoe_inflate` (`2.2D`/`7.2D`/`7.6D`) en `java_emitter.py` para evitar clipping de poses multi-parte en bordes de frustum.
