# `HiveBrain` Native AI Architecture (`com.alex.mycelialmind.entity.ai.brain`)

Mycelial Mind runs its creature, boss, and ally AI on a self-contained, clean-room `HiveBrain` engine built purely on public Minecraft/NeoForge 1.21.1 `Brain`, `BehaviorControl`, and `Sensor` APIs—with **zero external brain-library dependencies**, **zero Access Transformers**, and **zero Mixins**.

## Package Layout

```text
com.alex.mycelialmind.entity.ai.brain
├── core/
│   ├── HiveBrainOwner.java          # Entity contract (sensors, task groups, priorities, tickBrain)
│   ├── HiveBrainFactory.java        # Recursive memory collection/caching + vanilla Brain wiring
│   ├── HiveBrainRuntime.java        # Per-entity sensor cadence + brain.tick + activity switching
│   ├── HiveBrainScheduler.java      # Pure deterministic scheduling/cooldown/weighting helpers (JUnit)
│   ├── HiveBrainUtils.java          # Null-safe memory & target helpers over Brain<?>
│   ├── BrainActivityGroup.java      # Activity task group builder (CORE, IDLE, FIGHT, custom)
│   ├── HiveBehaviour.java           # BehaviorControl<E> base with fluent conditions, cooldowns, timeouts
│   ├── GroupBehaviour.java          # Composite base collecting child memories automatically
│   ├── FirstApplicableBehaviour.java
│   ├── OneRandomBehaviour.java
│   ├── AllApplicableBehaviours.java
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
| `AcidSpitterEntity` | + `NearbyPlayersSensor`, `UnreachableTargetSensor`, `IncomingProjectilesSensor` | `AcidSpitterCombatBehaviour` + `AllApplicableBehaviours` (projectile sidestep & spark look); `FIGHT`: `ReactToUnreachableTarget` | Brain owns target & combat nav; `FloatGoal`/`TacticalTerraformingGoal` remain in `GoalSelector` |
| `MycelialStalkerEntity` | `createDefaultHiveSensorsWithProjectiles()` | `StalkerFlankBehaviour`, `FirstApplicableBehaviour` (low-HP `FleeTarget` / projectile sidestep), `MoveToWalkTarget`; `FIGHT`: `ReactToUnreachableTarget` | Coexists with leap/ambush/flank Goals |
| `SynapseWeaverEntity` | `createDefaultHiveSensorsWithProjectiles()` | `WeaverSupportBehaviour`, `FirstApplicableBehaviour` (`AvoidEntity` players `>= 6.5m` / projectile sidestep), `AllApplicableBehaviours` idle regen+look; `FIGHT`: `ReactToUnreachableTarget` | Yields gracefully to `WeaverSupportGoal` when safe |
| `ChitinBruteEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (speed surge) + `SequentialBehaviour` (look+press) + melee; `Activity.INVESTIGATE`: `HiveHotspotPatrolBehaviour` when `HIVE_ALERT_LEVEL >= 0.4` | Coexists with `BruteChargeGoal`/`TacticalTerraformingGoal` |
| `BurrowerEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (30t ambush speed burst/particles) + `DelayedBehaviour` tunnel tremor + melee | Coexists with tunneling & flanking Goals |
| `PyroShroomEntity` | `createDefaultHiveSensorsWithProjectiles()` | `FIGHT`: `FirstApplicableBehaviour` (`AvoidEntity` when defused/weakened, `SequentialBehaviour` thermal telegraph `DelayedBehaviour`+advance) | Coexists with fuse detonation logic |
| `ColonyWardenEntity` | Inherits default + unreachable | `FIGHT`: `ReactToUnreachableTarget` (rallying bellow + ally speed pulse); `Activity.INVESTIGATE`: `AllApplicableBehaviours` (`SetPlayerLookTarget` + `HiveNodeRegenBehaviour`) | Coexists with aura & shockwave Goals |
| `SynapticMimicEntity` | `createDefaultHiveSensorsWithProjectiles()` | `FirstApplicableBehaviour`: shocked unmasked `FleeTarget`, `StrafeTarget` (`SPORE_MARKSMAN`), covert `DelayedBehaviour` observation (disguised) | Coexists with Mimic stance/sabotage AI |
| `SporeBehemothEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, `LookAtTarget`, projectile awareness; `FIGHT`: `InvalidateAttackTarget`, `ReactToUnreachableTarget` roar; `Activity.INVESTIGATE` hotspot orientation | Preserves `BossStateGatedMeleeGoal` & carapace phases |
| `HiveCoreBossEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, `AllApplicableBehaviours` (`LookAtTarget` + projectile `HiveBlackboard` alert); empty `FIGHT` | Preserves Phase 2 Sentinel anchors & state machine |
| `ApexMatriarchEntity` | `createDefaultHiveSensorsWithProjectiles()` | Additive non-hijacking: boss target sync, multi-player awareness + projectile flare; `FIGHT`: `ReactToUnreachableTarget` + `DelayedBehaviour` psychic pulse | Preserves 3-phase boss state machine |
| `MycelialChimeraEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `IncomingProjectilesSensor` | Projectile sidestep (when unmounted & not `WAIT`); `Activity.REST` when `CommandMode == WAIT` (`MemoryModuleType.HOME`) | Preserves `CommandMode` (`FOLLOW`/`PATROL`/`WAIT`) & riding |
| `ColonySymbiontEntity` | `NearbyLivingEntitySensor`, `NearbyPlayersSensor`, `HurtBySensor`, `UnreachableTargetSensor` | `LookAtTarget` + `ReactToUnreachableTarget` (clears stalled paths); `Activity.PANIC` (`HURT_BY_ENTITY` -> `FleeTarget` for non-`SENTINEL` castes when active) | Preserves all 5 Sovereign Colony caste Goals |

## Lifecycle & Zero-AT Design

1. **Construction (`HiveBrainOwner`):**
   - `brainProvider()` delegates to `HiveBrainFactory.createProvider(this)`, collecting every `MemoryModuleType<?>` declared by `getSensors()` and all `BrainActivityGroup` behaviours (including nested `GroupBehaviour` children) and caching them per `EntityType<?>`.
   - `makeBrain(Dynamic<?>)` invokes `super.makeBrain(dynamic)` and binds `this.hiveBrainRuntime = HiveBrainFactory.initializeBrain(this, brain)`, registering activities via standard public `Brain.addActivityAndRemoveMemoriesWhenStopped(...)` and setting `CORE` + `IDLE` defaults.
2. **Tick (`customServerAiStep` -> `tickBrain(this)`):**
   - `HiveBrainRuntime.tick(serverLevel, entity, brain)`:
     1. Ticks each `HiveSensor` (`Sensor.tick`), gating execution via `HiveBrainScheduler.shouldRunSensor` (first scan always fires immediately at any `gameTime`).
     2. Calls `brain.tick(serverLevel, entity)` so vanilla handles memory expiration and active `BehaviorControl` lifecycles.
     3. Calls `brain.setActiveActivityToFirstValid(activityPriorities)` so entities transition seamlessly between `FIGHT`, `INVESTIGATE`, `REST`, `PANIC`, and `IDLE`.
