# `HiveBrain` Native AI Architecture (`com.alex.mycelialmind.entity.ai.brain`)

Mycelial Mind runs its creature, boss, and ally AI on a self-contained, clean-room `HiveBrain` engine built purely on public Minecraft/NeoForge 1.21.1 `Brain`, `BehaviorControl`, and `Sensor` APIs—with **zero external brain-library dependencies**, **zero Access Transformers**, and **zero Mixins**.

## Package Layout

```text
com.alex.mycelialmind.entity.ai.brain
├── core/
│   ├── HiveBrain.java               # First-class coordinator: typed memories, schedules, activity tracking, zero-alloc tick
│   ├── HiveBrainOwner.java          # Entity contract + lifecycle hooks (shouldTickBrain, onBrainTickStart/End, onActivityChanged)
│   ├── HiveBrainFactory.java        # Class-keyed memory caching + ThreadLocal spec handoff (single compilation)
│   ├── HiveBrainRuntime.java        # Backwards-compatible HiveBrain subclass returned by initializeBrain
│   ├── HiveBrainSchedule.java       # Clean-room day-tick windows, predicate rules & callbacks
│   ├── HiveBrainScheduler.java      # Pure deterministic scheduling, phasing, hysteresis utility & weighting helpers (JUnit)
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
| `MycelialChimeraEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `IncomingProjectilesSensor` | Projectile sidestep (when unmounted & not `WAIT`); `Activity.REST` when `CommandMode == WAIT` (`MemoryModuleType.HOME`) | Preserves `CommandMode` (`FOLLOW`/`PATROL`/`WAIT`) & riding |
| `ColonySymbiontEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `UnreachableTargetSensor` | `LookAtTarget` + `ReactToUnreachableTarget` (clears stalled paths); `Activity.PANIC` (`HURT_BY_ENTITY` -> `FleeTarget` for non-`SENTINEL` castes when active) | Preserves all 5 Sovereign Colony caste Goals |

## Lifecycle & Zero-AT Design

1. **Construction (`HiveBrainOwner` & `HiveBrainFactory`):**
   - `brainProvider()` delegates to `HiveBrainFactory.createProvider(this)`, collecting every `MemoryModuleType<?>` declared by `getSensors()` and all `BrainActivityGroup` behaviours (including nested `GroupBehaviour` / `UtilitySelectorBehaviour` children) and caching them per `Class<?>`. On cache miss, a `ThreadLocal` spec handoff passes compiled task groups and sensors directly to `initializeBrain` so they are never instantiated twice.
   - `makeBrain(Dynamic<?>)` invokes `super.makeBrain(dynamic)` and binds `this.hiveBrainRuntime = HiveBrainFactory.initializeBrain(this, brain)` (returning `HiveBrainRuntime<E> extends HiveBrain<E>`), registering activities via `Brain.addActivityAndRemoveMemoriesWhenStopped(...)`, applying `getBrainSchedule()`, setting `CORE` + `IDLE` defaults, and invoking `handleAdditionalBrainSetup(hiveBrain)`.
2. **Tick (`customServerAiStep` -> `tickBrain(this)` -> `HiveBrain.tick`):**
   - Checks `shouldTickBrain` and invokes `onBrainTickStart`.
   - Ticks each `HiveSensor` (`Sensor.tick`), gating execution via `HiveBrainScheduler.shouldRunSensor` with deterministic per-entity `entity.getId()` phase offsets after the immediate initial scan (`computePhasedFirstTickTime`).
   - Runs `HiveBrainSchedule` callbacks & resolution (if present), calls `brain.tick(serverLevel, entity)`, and resolves `Activity` priorities with zero per-tick allocations.
   - Tracks `activeActivity` / `previousActivity` / `ticksInCurrentActivity`, firing `onActivityChanged` on transitions and `onBrainTickEnd` at the end of the tick.
