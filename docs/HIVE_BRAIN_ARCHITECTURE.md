# `HiveBrain` Native AI Architecture (`com.alex.mycelialmind.entity.ai.brain`)

Mycelial Mind runs its creature, boss, and ally AI on a self-contained, clean-room `HiveBrain` engine built purely on public Minecraft/NeoForge 1.21.1 `Brain`, `BehaviorControl`, and `Sensor` APIs—with **zero external AI/brain-library dependencies** (no SmartBrainLib, no gdxAI), **zero Access Transformers**, and **zero Mixins**.

## Package Layout

```text
com.alex.mycelialmind.entity.ai.brain
├── core/
│   ├── HiveBrain.java               # First-class coordinator: profiles, modules, channel arbiter, traces, signals & zero-alloc tick
│   ├── HiveBrainOwner.java          # Primary entity contract (getHiveBrain/setHiveBrain, replaceVanillaBrain, profiles, modules, LOD)
│   ├── HiveBrainProfile.java        # Executable behavioral profiles (priorities, schedule, sensors, LOD multiplier & task-group swap)
│   ├── HiveBrainModule.java         # Pluggable per-tick cognitive modules (e.g. tactical_btree) unified under pause & LOD
│   ├── HiveResourceChannel.java     # Motor/cognitive resource lock channels (MOVE, LOOK, COMBAT, ABILITY, TERRAFORM)
│   ├── HiveResourceArbiter.java     # Zero-alloc priority preemption arbiter across concurrent HiveBehaviour tasks (JUnit)
│   ├── HiveBrainTrace.java          # 16-entry chronological decision ring-buffer (ACTIVITY, PROFILE, SIGNAL, INTERRUPT, CHANNEL_PREEMPT)
│   ├── HiveBrainSignal.java         # Immutable reactive interrupt record (DAMAGE_TAKEN, ALLY_ALERT, ACOUSTIC_PULSE, DESYNC_TRIGGERED, LEADER_ORDER, PROJECTILE_INCOMING, PHASE_SHIFT)
│   ├── HiveBrainKey.java            # Type-safe keys with ephemeral and persistent NBT factories (zero registry deps)
│   ├── HiveBrainBlackboard.java     # Per-entity typed storage, TTL expiration, unified named cooldowns & NBT serialization
│   ├── HiveBrainSnapshot.java       # Immutable live telemetry snapshot (activity, profile, signals, cooldowns, modules, channels, trace, pause)
│   ├── HiveBrainFactory.java        # Class-keyed memory caching + ThreadLocal spec handoff (single compilation)
│   ├── HiveBrainRuntime.java        # Backwards-compatible HiveBrain subclass returned by initializeBrain
│   ├── HiveBrainSchedule.java       # Clean-room day-tick windows, predicate rules & callbacks
│   ├── HiveBrainScheduler.java      # Pure deterministic scheduling, phasing, LOD wake windows, hysteresis utility & weights (JUnit)
│   ├── HiveBrainUtils.java          # Null-safe memory & target helpers over Brain<?>
│   ├── BrainActivityGroup.java      # Activity task group builder (CORE, IDLE, FIGHT, custom)
│   ├── HiveBehaviour.java           # BehaviorControl<E> base with fluent conditions, cooldowns, timeouts
│   ├── GroupBehaviour.java          # Composite base collecting child memories automatically
│   ├── FirstApplicableBehaviour.java
│   ├── OneRandomBehaviour.java
│   ├── AllApplicableBehaviours.java # Optional reEvaluateStoppedChildren(true) reactive mode
│   ├── UtilitySelectorBehaviour.java # Dynamic utility scorer with hysteresis & continuous preemption
│   ├── SequentialBehaviour.java
│   ├── DelayedBehaviour.java
│   └── CustomBehaviour.java
├── btree/
│   ├── HiveBehaviorTree.java        # Clean-room tactical BehaviorTree replacing gdxAI
│   ├── HiveBtTask.java              # Base node with guards, start/end, cancel, reset
│   ├── HiveBtLeafTask.java
│   ├── HiveBtDynamicGuardSelector.java
│   ├── HiveBtSelector.java
│   ├── HiveBtSequence.java
│   └── HiveBtStatus.java
├── steering/
│   └── HiveSteeringEngine.java      # Zero-alloc 2D XZ steering (Separation, Cohesion, Alignment, Arrive, Pursuit, Evade, Pylon repulsion, attack slots)
├── sensor/
│   ├── HiveSensor.java              # Sensor<E> base with per-instance scan rate & post-scan callback
│   ├── PredicateSensor.java
│   ├── NearbyLivingEntitySensor.java
│   ├── HurtBySensor.java
│   ├── NearbyPlayersSensor.java
│   ├── UnreachableTargetSensor.java
│   └── IncomingProjectilesSensor.java  # Tracks incoming airborne projectiles -> INCOMING_PROJECTILE_POS
├── behaviour/
│   ├── LookAtTarget.java
│   ├── MoveToWalkTarget.java
│   ├── TargetOrRetaliate.java
│   ├── SetPlayerLookTarget.java
│   ├── SetRandomLookTarget.java
│   ├── SetRandomWalkTarget.java
│   ├── Idle.java
│   ├── InvalidateAttackTarget.java
│   ├── SetWalkTargetToAttackTarget.java
│   ├── AnimatableMeleeAttack.java
│   ├── ReactToUnreachableTarget.java   # Clears stalled navigation + runs custom reaction after window
│   ├── StrafeTarget.java               # Orbital MoveControl strafing at target spacing
│   ├── AvoidEntity.java                # Evades close threats via DefaultRandomPos + LOS fallback
│   └── FleeTarget.java                 # Retreats from HURT_BY_ENTITY / ATTACK_TARGET
└── hive/
    ├── HiveBrainBridge.java         # Typed Hive memory accessors, XP promotion, sidestep/sync presets
    ├── HiveMindSensor.java          # Syncs HiveMindSavedData, HiveBlackboard, PurificationZoneTracker
    ├── HiveNodeRegenBehaviour.java
    ├── HiveHotspotPatrolBehaviour.java
    ├── AcidSpitterCombatBehaviour.java
    ├── StalkerFlankBehaviour.java
    └── WeaverSupportBehaviour.java
```

## Registered Brain Memories & Sensors (`ModMemoryTypes`)

- **`11` `MemoryModuleType` entries:** `SHARED_TARGET_POS`, `HIVE_ALERT_LEVEL`, `NEAREST_NODE_POS`, `STALKER_FLANK_POS`, `WEAVER_SUPPORT_POS`, `NEAREST_PYLON_POS`, `HIVE_ORDER`, `MOB_COMBAT_XP`, `LAST_ATTACK_STIMULUS`, `NEARBY_ALLY_COUNT`, and `INCOMING_PROJECTILE_POS` (plus standard vanilla combat/navigation/entity memories pre-registered in `HiveBrainFactory`).
- **`SensorType`:** `HIVE_MIND_SENSOR`.
- **Default Presets (`HiveBrainBridge`):**
  - `createDefaultHiveSensors()`: `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `UnreachableTargetSensor`, `HiveMindSensor`.
  - `createDefaultHiveSensorsWithProjectiles()`: adds `IncomingProjectilesSensor`.
  - `createFightTasks(int attackDelayTicks)`: `InvalidateAttackTarget`, `ReactToUnreachableTarget(60L)`, `SetWalkTargetToAttackTarget`, `AnimatableMeleeAttack`.

## Integrated Entities, Bosses & Allies (`HiveBrainOwner`)

| Entity Class | Sensors | Core / Idle / Fight & Additional Activities | Coexistence Model |
|---|---|---|---|
| `MycelialMob` (default) | `createDefaultHiveSensors()` | `LookAtTarget`, `MoveToWalkTarget`, `TargetOrRetaliate`, `OneRandomBehaviour` (`HiveNodeRegen`, `HiveHotspotPatrol`, `SetRandomWalkTarget`, `Idle`), `ReactToUnreachableTarget` | Coexists with subclass `GoalSelector` |
| `AcidSpitterEntity` | + `NearbyPlayersSensor`, `UnreachableTargetSensor`, `IncomingProjectilesSensor` | `AcidSpitterCombatBehaviour` + `AllApplicableBehaviours` (`.reEvaluateStoppedChildren(true)`); `FIGHT`: `ReactToUnreachableTarget` | Brain owns target & combat nav; `FloatGoal`/`TacticalTerraformingGoal` remain in `GoalSelector` |
| `MycelialStalkerEntity` | `createDefaultHiveSensorsWithProjectiles()` | `StalkerFlankBehaviour`, `FirstApplicableBehaviour` (low-HP `FleeTarget` / projectile sidestep), `MoveToWalkTarget`; `FIGHT`: `ReactToUnreachableTarget` | Coexists with leap/ambush/flank Goals |
| `SynapseWeaverEntity` | `createDefaultHiveSensorsWithProjectiles()` | `WeaverSupportBehaviour`, `UtilitySelectorBehaviour` (`projectileSidestep` vs `AvoidEntity` with hysteresis), reactive `AllApplicableBehaviours` idle regen+look; `FIGHT`: `ReactToUnreachableTarget` | Yields gracefully to `WeaverSupportGoal` when safe |
| `ChitinBruteEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (speed surge) + `SequentialBehaviour` (look+press) + melee; `Activity.INVESTIGATE`: `HiveHotspotPatrolBehaviour` when `HIVE_ALERT_LEVEL >= 0.4` | Coexists with `BruteChargeGoal`/`TacticalTerraformingGoal` |
| `BurrowerEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (30t ambush speed burst/particles) + `DelayedBehaviour` tunnel tremor + melee | Coexists with tunneling & flanking Goals |
| `PyroShroomEntity` | `createDefaultHiveSensorsWithProjectiles()` | `FIGHT`: `FirstApplicableBehaviour` (`AvoidEntity` when defused/weakened, `SequentialBehaviour` thermal telegraph `DelayedBehaviour`+advance) | Coexists with fuse detonation logic |
| `ColonyWardenEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (rallying bellow + ally speed pulse); `Activity.INVESTIGATE`: `AllApplicableBehaviours` (`SetPlayerLookTarget` + `HiveNodeRegenBehaviour`) | Coexists with aura & shockwave Goals |
| `SynapticMimicEntity` | `createDefaultHiveSensorsWithProjectiles()` | `UtilitySelectorBehaviour`: shocked unmasked `FleeTarget`, `StrafeTarget` (`SPORE_MARKSMAN`), covert `DelayedBehaviour` observation (disguised) | Coexists with Mimic stance/sabotage AI |
| `SporeBehemothEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, `LookAtTarget`, projectile awareness; `FIGHT`: `InvalidateAttackTarget`, `ReactToUnreachableTarget` roar; `Activity.INVESTIGATE` hotspot orientation | Preserves `BossStateGatedMeleeGoal` & carapace phases |
| `HiveCoreBossEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, `AllApplicableBehaviours` (`LookAtTarget` + projectile `HiveBlackboard` alert); empty `FIGHT` | Preserves Phase 2 Sentinel anchors & state machine |
| `ApexMatriarchEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, multi-player awareness + projectile flare; `FIGHT`: `ReactToUnreachableTarget` + `DelayedBehaviour` psychic pulse | Preserves 3-phase boss state machine |
| `MycelialChimeraEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `IncomingProjectilesSensor` | Projectile sidestep (when unmounted & not `WAIT`); `Activity.REST` when `CommandMode == WAIT` (`MemoryModuleType.HOME`) | Preserves `CommandMode` (`FOLLOW`/`PATROL`/`WAIT`) & riding; LOD + NBT |
| `ColonySymbiontEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `UnreachableTargetSensor` | `LookAtTarget` + `ReactToUnreachableTarget` (clears stalled paths); `Activity.PANIC` (`HURT_BY_ENTITY` -> `FleeTarget` for non-`SENTINEL` castes when active) | Preserves all 5 Sovereign Colony caste Goals; LOD + NBT |
| `PurifierDroneEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `IncomingProjectilesSensor` | `LookAtTarget`, hover evade/sidestep & jam awareness | Preserves drone patrol/purification Goals & NBT |
| `SporeGliderEntity` | `IncomingProjectilesSensor`, `NearbyPlayersSensor`, `HurtBySensor` | `LookAtTarget`, evade & ambient gliding | Preserves `FlyingMoveControl`, home anchor & moss pollination |
| `RootKeeperEntity` | Inherits `MycelialMob` default | Inherits `MycelialMob` reactive signals, LOD & NBT | Dynamic range & pylon weakening via `SinkholeEcosystemEngine` |

## Lifecycle & Zero-AT Design (`HiveBrain 3.0`)

1. **Construction & Primary Contract (`HiveBrainOwner` & `HiveBrainFactory`):**
   - `HiveBrainOwner<T>` defines `getHiveBrain()` / `setHiveBrain(@Nullable HiveBrain<T>)` as the primary abstract contract across all 5 root entity classes (`MycelialMob`, `MycelialChimeraEntity`, `ColonySymbiontEntity`, `PurifierDroneEntity`, `SporeGliderEntity` — covering `20/20` AI entities), while default `getHiveBrainRuntime()` / `setHiveBrainRuntime(...)` bridge cleanly. Each root entity also implements `replaceVanillaBrain` (`LivingEntity.brain` is `protected`) so profiles can rebuild vanilla `Brain` instances without ATs/Mixins while preserving memories (`ExpirableValue`).
   - `brainProvider()` delegates to `HiveBrainFactory.createProvider(this)`, collecting every `MemoryModuleType<?>` declared by `getSensors()`, default `BrainActivityGroup`s, and `getBrainProfiles()` task overrides, caching them per `Class<?>`.
   - `makeBrain(Dynamic<?>)` binds `this.hiveBrain = HiveBrainFactory.initializeBrain(this, brain)`, registering `getBrainSchedule()`, `getBrainProfiles()`, and `getBrainModules()`.
2. **Tick, Adaptive LOD, Profiles, Modules & Channel Arbitration (`HiveBrain.tick`):**
   - Applies pending NBT-restored profile if needed, checks `isPaused()` and `shouldTickBrain` (`evaluateStandardLod` + profile `lodIntervalMultiplier`), then invokes `onBrainTickStart`.
   - Drains up to `16` queued `HiveBrainSignal`s, prunes expired `HiveBrainBlackboard` entries/cooldowns periodically, ticks enabled `HiveSensor`s, and steps enabled `HiveBrainModule`s (including `MycelialMob`'s `tactical_btree` module).
   - `HiveBehaviour` instances declaring `requireChannels(...)` + `channelPriority(...)` arbitrate exclusive ownership via `HiveResourceArbiter` (preempting lower-priority holders cleanly), while `.interruptOnSignal(...)` immediately stops matching `RUNNING` behaviours when matching signals arrive.
   - `HiveBrainProfile` dynamically swaps priorities, schedule, sensor toggles, LOD cadence, and vanilla `Brain` task groups across `MycelialMob` (`default`/`frenzy`/`desync`), `MycelialBossMob` (`default`/`phase_2`/`phase_3`/`staggered`), and `MycelialChimeraEntity` (`follow`/`patrol`/`wait`).
3. **Reactive Signals, Typed Blackboard, Decision Traces & Live Controls:**
   - Immediate `dispatchSignal` and queued `enqueueSignal` wake LOD, interrupt matching `HiveBehaviour`s, trigger urgent sensors (guarded against re-entrancy), notify `brain.onSignal(type, listener)`, modules, and `onBrainSignal`. Real hooks emit `DAMAGE_TAKEN`, `ALLY_ALERT`, `ACOUSTIC_PULSE`, `DESYNC_TRIGGERED`, `LEADER_ORDER` (squad leader broadcasts), `PROJECTILE_INCOMING` (`IncomingProjectilesSensor`), and `PHASE_SHIFT` (`MycelialBossMob`).
   - `HiveBrainTrace` records the latest `16` transitions (`ACTIVITY`, `PROFILE`, `SIGNAL`, `INTERRUPT`, `CHANNEL_PREEMPT`, `MODULE`, `PAUSE`).
   - `captureSnapshot(gameTime)` -> `HiveBrainSnapshot` + `/mycelial ai [radius|trace|profile|pause|signal]` and `[HiveBrain]` telemetry in `BioScannerItem` expose full live control and inspection across `20/20` entities.
