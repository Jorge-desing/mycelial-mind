# Motor Nativo `HiveBrain 3.0`, Sensores, Steering, BehaviorTree y Utilidad

Arquitectura clean-room (`entity/ai/brain/{core,sensor,behaviour,btree,steering,hive}`, `62` clases Java en `brain/`), **cero dependencias externas de librerías cerebrales, cero Access Transformers y cero Mixins**. Documentación complementaria de clases e invariantes en [HIVE_BRAIN_ARCHITECTURE.md](../../../../docs/HIVE_BRAIN_ARCHITECTURE.md).

---

## 1. Cobertura Actual y Contrato `HiveBrainOwner<T>` (`20/20` entidades)

Las **20/20** entidades con IA del mod implementan `HiveBrainOwner<T extends LivingEntity & HiveBrainOwner<T>>`:
- **Hostiles (`MycelialMob`):** `Sporeling`, `ChitinBrute`, `MycelialStalker`, `AcidSpitter`, `Burrower`, `PyroShroom`, `SynapseWeaver`, `GoliathCarrier`, `LeechParasite`, `ColonyWarden`, `SporeArchitect`, `SynapticMimic`, `RootKeeperEntity`.
- **Jefes (`MycelialBossMob`):** `SporeBehemothEntity`, `HiveCoreBossEntity`, `ApexMatriarchEntity`.
- **Aliados y Fauna:** `MycelialChimeraEntity`, `ColonySymbiontEntity`, `PurifierDroneEntity`, `SporeGliderEntity`.

Contrato primario en las clases raíz:
- `getSensors()`, `getCoreTasks()`, `getIdleTasks()`, `getFightTasks()`, `getAdditionalTasks()`, `getAlwaysRunningActivities()`, `getDefaultActivity()`, `getActivityPriorities()`, `getBrainSchedule()`.
- `getHiveBrain()` / `setHiveBrain(@Nullable HiveBrain<T>)` (con puente `default` `getHiveBrainRuntime()` / `setHiveBrainRuntime(...)`).
- `replaceVanillaBrain(Brain<T>)` (reasigna `LivingEntity.brain` `protected`, cero ATs/Mixins).
- `shouldTickBrain(ServerLevel, T)` delegando en `HiveBrainOwner.evaluateStandardLod(level, entity, highPriority)`.
- `customServerAiStep()` invocando `tickBrain(this)` cuando `shouldTickBrain` es verdadero.
- `addAdditionalSaveData` / `readAdditionalSaveData` llamando a `saveBrainState(tag)` / `loadBrainState(tag)`.

---

## 2. Subsistema `HiveBrain 3.0` (`entity/ai/brain/core/`)

1. **Perfiles Comportamentales Ejecutables (`HiveBrainProfile<T>`):**
   - Registrados por `getBrainProfiles()` (`applyProfile` / `applyBrainProfile(level, entity, name)`).
   - Incluyen prioridades, `HiveBrainSchedule`, sensores, `lodIntervalMultiplier` y reconstrucción completa de `BrainActivityGroup` preservando memorias en `HiveBrainFactory.rebuildBrainForProfile`:
     - `MycelialMob`: `"frenzy"`, `"desync"`
     - `MycelialBossMob`: `"phase_2"`, `"phase_3"`, `"staggered"`
     - `MycelialChimeraEntity`: `"follow"`, `"patrol"`, `"wait"`
2. **Módulos y Arbitraje de Recursos (`HiveBrainModule<T>`, `HiveResourceChannel`, `HiveResourceArbiter`):**
   - `HiveBrainModule` se ejecuta dentro de `HiveBrain.tick()` respetando pausa/LOD (`"tactical_btree"` unificado).
   - **5 canales exclusivos (`HiveResourceChannel`):** `MOVE`, `LOOK`, `COMBAT`, `ABILITY`, `TERRAFORM`.
   - `HiveResourceArbiter` arbitra con preempción por prioridad declarada en `HiveBehaviour` (opt-in con `.requireChannels(...)` para coexistir sin bloquear tareas legacy o `GoalSelector`).
3. **Señales Reactivas e Interrupciones (`HiveBrainSignal`, `SignalType`):**
   - **8 tipos:** `DAMAGE_TAKEN`, `ALLY_ALERT`, `ACOUSTIC_PULSE`, `DESYNC_TRIGGERED`, `LEADER_ORDER`, `PROJECTILE_INCOMING`, `PHASE_SHIFT`, `CUSTOM`.
   - `IncomingProjectilesSensor` emite `PROJECTILE_INCOMING`; jefes emiten `PHASE_SHIFT`; líderes de escuadrón emiten `LEADER_ORDER`.
   - Soporta `.interruptOnSignal(...)` en `HiveBehaviour`, listeners `brain.onSignal(...)`, `dispatchBrainSignal(...)` inmediato y `enqueueBrainSignal(...)` en cola (despertando entidades throttled por LOD).
4. **Pizarrón Local Tipado (`HiveBrainKey<V>` + `HiveBrainBlackboard`):**
   - Cero `BuiltInRegistries` (100% testeable en JUnit 5 puro).
   - Soporta TTL, cooldowns y persistencia NBT bajo `"HiveBrainState"`.
5. **Observabilidad (`HiveBrainTrace`, `HiveBrainSnapshot`):**
   - `HiveBrainTrace`: ring-buffer de `16` entradas con **7 `EventKind`:** `ACTIVITY`, `PROFILE`, `SIGNAL`, `INTERRUPT`, `CHANNEL_PREEMPT`, `MODULE`, `PAUSE`.
   - `HiveBrainSnapshot` (`mod`, `ch`), inspección con `/mycelial ai [radio|trace|profile|pause|signal]` y sección `[HiveBrain]` en `BioScannerItem`.

---

## 3. Sensores, Memorias y Comportamientos (`ModMemoryTypes`, `sensor/`, `behaviour/`)

- **12 registros en `ModMemoryTypes`:**
  - **11 `MemoryModuleType`:** `SHARED_TARGET_POS`, `HIVE_ALERT_LEVEL`, `NEAREST_NODE_POS`, `STALKER_FLANK_POS`, `WEAVER_SUPPORT_POS`, `NEAREST_PYLON_POS`, `HIVE_ORDER`, `MOB_COMBAT_XP`, `LAST_ATTACK_STIMULUS`, `NEARBY_ALLY_COUNT`, `INCOMING_PROJECTILE_POS`.
  - **1 `SensorType`:** `hive_mind_sensor`.
- **Sensores en `HiveBrainBridge`:**
  - `createDefaultHiveSensors()`: `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `UnreachableTargetSensor`, `HiveMindSensor`.
  - `createDefaultHiveSensorsWithProjectiles()`: añade `IncomingProjectilesSensor`.
  - `createFightTasks(...)` incorpora `ReactToUnreachableTarget`.
- **Sincronización en `HiveMindSensor`:**
  - Nunca fija `NEAREST_NODE_POS` sobre nodos aliados `SYMBIOTIC` ni nodos protegidos por Pilón (`PurificationZoneTracker.findNearestEnemyNode`).
  - Detecta `PurifierDroneEntity`, `ColonySymbiontEntity`, nodos `SYMBIOTIC` y `SovereignColonySavedData.findContainingColony` para erosionar `control` (`[0, 1]`) y registrar amenaza en `HiveInfluenceMap`.
  - Consulta `MycorrhizalAcousticTracker.getNearestActiveDecoy(...)` para propagar señuelos a `SHARED_TARGET_POS`.
- **XP de Combate (`addCombatXp`):** autopromueve a `100` (`MUTATED`) / `250` (`ELITE`), excluyendo `HiveCoreBossEntity` y `ApexMatriarchEntity`, con retroalimentación `+2.0` a `HiveMindSavedData`.
- **Behaviours incluidos:** `HiveNodeRegenBehaviour`, `HiveHotspotPatrolBehaviour` (`idleTasks` + `Activity.INVESTIGATE`), `WeaverSupportBehaviour`, `StalkerFlankBehaviour`, `AcidSpitterCombatBehaviour`, `UtilitySelectorBehaviour`, `AllApplicableBehaviours`, `FirstApplicableBehaviour`, `SequentialBehaviour`, `DelayedBehaviour`, `CustomBehaviour`, `AnimatableMeleeAttack`, `StrafeTarget`, `AvoidEntity`, `FleeTarget`, `ReactToUnreachableTarget`, `HiveBrainSchedule` y actividades `INVESTIGATE`, `REST`, `PANIC`.

---

## 4. Steering, BehaviorTree, Utilidad y Mapa de Influencia

- **`HiveBehaviorTree` (`entity/ai/brain/btree/`) + `HiveTacticalTree` + `HiveAiBlackboard`:**
  - Arquitectura *"BT outside, utility inside"* con `HiveBtDynamicGuardSelector`, `HiveBtSelector`, `HiveBtSequence` y `HiveBtLeafTask`.
  - `HiveAiBlackboard` es zero-allocation y expone `HiveDoctrine`, `influenceTrauma01`, `MobEpisodicMemory` y cooldowns persistentes en cadencia adaptativa (`4`/`10`/`20` ticks con despertar al adquirir objetivo o señal).
  - En `MycelialMob`, `HiveTacticalTree` ordena `DesyncLeaf` > `FRENZY_CHARGE` > `PHALANX_GUARD`.
- **`HiveSteeringEngine` (`entity/ai/brain/steering/`) + `HiveSwarmCoordinator`:**
  - Steering nativo zero-alloc (cero `gdxAI`): `Separation`, `Cohesion`, `Alignment`, `Arrive`, `Pursuit`, `Evade`, repulsión de Pilón y `computeAttackSlotPosition` en anillo angular (`computeTacticalArriveSteering`).
  - Usa `MycelialFastNoise.organicFlow2D` / `foragingAffinity` (`ThreadLocal<FastNoiseLite>`, `OpenSimplex2` + `FBm`, MIT) para aproximación orgánica y dispersión de patrulla.
- **`HiveUtilityEvaluator` (`entity/ai/HiveUtilityEvaluator.java`):**
  - Puntuaciones `[0.0, 1.0]` con curvas `responseSigmoid`, `responseExponential`, `compensate(rawProduct, factorCount)` (preserva `0.0F` como veto duro), `applyHysteresis(rawScore, isCurrent, bonus)` (inercia solo si `rawScore > 0`) e `inverseLerp01`.
  - `evaluateChargeUtility`, `evaluateKiteUtility` y `evaluateRegroupUtility` leen `HiveInfluenceMap` en $O(1)$ (`threat`, `pheromone`, `1 - control`), más `combatXpChargeBonus`, `stimulusKiteOffset` y `stimulusRegroupBonus`.
- **`HiveInfluenceMap`:** rejilla gruesa ($16 \times 16$) por instancia de `ServerLevel` con canales `control`, `threat`, `trauma` y `pheromone` en `[0.0, 1.0]`, difusión cardinal `0.35F` y poda automática (`stepDecay`).

---

## 5. Arbitraje GoalSelector / HiveBrain en Unidades Clave

- **`SynapseWeaverEntity` (`WeaverSupportBehaviour` / `WeaverSupportGoal`):**
  - Registra posición de aliado cercano; el goal inicia a `> 8m` y continúa hasta `6m`, verificando un aliado vivo y no rival cerca de la posición memorizada.
  - Cede navegación a `EvasiveStrafeGoal` cuando `PROJECTILE_DEFLECT` está activo y un objetivo apunta con arco/ballesta a `<= 20m`, y se detiene ante amenaza a `<= 4m`.
- **`AcidSpitterEntity` (`AcidSpitterCombatBehaviour`):**
  - Sin objetivo, usa `evaluateRegroupUtility` hacia un nodo válido cuando supera `0.65`, tras atender orden vigente y señuelos activos (`getNearestActiveDecoy`).
  - Retirada táctica con navegación directa (`findRetreatDestination`) y respaldo `LandRandomPos`.
  - `AcidSpitProjectile`: daño mágico, Debilidad y Veneno contra cepas rivales sin infectar otros `MycelialMob`; atasca torretas y drones (`TURRET_CORROSIVE_JAM_TICKS` / `DRONE_PARASITIC_JAM_TICKS`).
- **`MycelialStalkerEntity` (`StalkerFlankBehaviour` / `SwarmFlankGoal`):**
  - Calcula `STALKER_FLANK_POS` solo con `FLANK_PERIMETER` vigente y lo borra en `tick()`/`stop()` al perder objetivo, cambiar orden o sufrir `DESYNC`. `SwarmFlankGoal` exige objetivo atacable para iniciar/continuar y conserva salto y ataque.
- **`HiveRegroupGoal`:** acepta `RALLY_NODE` con nodo enemigo cargado y válido; detiene ruta si la orden o el nodo dejan de ser válidos.
