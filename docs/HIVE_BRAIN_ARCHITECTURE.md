# `HiveBrain` Native AI Architecture (`com.alex.mycelialmind.entity.ai.brain`)

Mycelial Mind runs its creature AI on a self-contained, clean-room `HiveBrain` engine built purely on public Minecraft/NeoForge 1.21.1 `Brain`, `BehaviorControl`, and `Sensor` APIs—with **zero external brain-library dependencies**, **zero Access Transformers**, and **zero Mixins**.

## Package Layout

```text
com.alex.mycelialmind.entity.ai.brain
├── core/
│   ├── HiveBrainOwner.java          # Entity contract (sensors, task groups, priorities, tickBrain)
│   ├── HiveBrainFactory.java        # Recursive memory collection/caching + vanilla Brain wiring
│   ├── HiveBrainRuntime.java        # Per-entity sensor cadence + brain.tick + activity switching
│   ├── HiveBrainScheduler.java      # Pure deterministic scheduling/cooldown/weighting helpers (JUnit + Pitest)
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
│   └── UnreachableTargetSensor.java
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
│   └── AnimatableMeleeAttack.java
└── hive/
    ├── HiveBrainBridge.java         # Typed Hive memory accessors, XP promotion, default task presets
    ├── HiveMindSensor.java          # Syncs HiveMindSavedData, HiveBlackboard, PurificationZoneTracker
    ├── HiveNodeRegenBehaviour.java
    ├── HiveHotspotPatrolBehaviour.java
    ├── AcidSpitterCombatBehaviour.java
    ├── StalkerFlankBehaviour.java
    └── WeaverSupportBehaviour.java
```

## Lifecycle & Zero-AT Design

1. **Construction (`MycelialMob`):**
   - `brainProvider()` delegates to `HiveBrainFactory.createProvider(this)`, collecting every `MemoryModuleType<?>` declared by `getSensors()` and all `BrainActivityGroup` behaviours (including nested `GroupBehaviour` children) and caching them per `EntityType<?>`.
   - `makeBrain(Dynamic<?>)` invokes `super.makeBrain(dynamic)` and binds `this.hiveBrainRuntime = HiveBrainFactory.initializeBrain(this, brain)`, registering activities via standard public `Brain.addActivityAndRemoveMemoriesWhenStopped(...)` and setting `CORE` + `IDLE` defaults.
2. **Tick (`customServerAiStep` -> `tickBrain(this)`):**
   - `HiveBrainRuntime.tick(serverLevel, entity, brain)`:
     1. Ticks each `HiveSensor` (`Sensor.tick`), which gates execution via `HiveBrainScheduler.shouldRunSensor` (first scan always fires immediately at any `gameTime`).
     2. Calls `brain.tick(serverLevel, entity)` so vanilla handles memory expiration and active `BehaviorControl` lifecycle.
     3. Calls `brain.setActiveActivityToFirstValid(activityPriorities)` so entities seamlessly transition between `FIGHT`, `IDLE`, and custom activities.

## Extensibility for Future AI Improvements

- **Custom Activities:** Override `getAdditionalTasks()`, `getActivityPriorities()`, and `getAlwaysRunningActivities()` on any `HiveBrainOwner` subclass (e.g., boss phases, foraging, ambush states).
- **Composite Behaviour Trees inside Brain Activities:** Combine `FirstApplicableBehaviour`, `OneRandomBehaviour`, `AllApplicableBehaviours`, `SequentialBehaviour`, `DelayedBehaviour`, and `CustomBehaviour` freely—their child memory requirements are automatically registered on the entity's `Brain`.
- **Built-in Extensible Sensors:** `NearbyPlayersSensor` and `UnreachableTargetSensor` are ready to drop into `getSensors()` without additional registration boilerplate.
