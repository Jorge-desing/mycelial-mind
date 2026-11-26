# Mente Colmena, Telemetría, Combate Táctico y Jefes

Subsistemas de memoria global (`hive/`), progresión en 4 Actos, combate de enjambre, campo vivo y arquitectura unificada de jefes (`MycelialBossMob` + `BossCombatArchitectureEngine` + `MycelialBossPart`).

---

## 1. Memoria de la Mente Colmena y Progresión en 4 Actos

- **`HiveMindSavedData` (`mycelialmind_hive_memory`):**
  - Persiste `threatLevel` (`0.0–100.0`, escala `0.10x` por estímulo), `highestTierReached` (`HiveProgressionTier`: `DORMANT` 0–24%, `AWAKENED` 25–49%, `ADAPTIVE` 50–74%, `OVERMIND` 75–100%), puntajes por `StimulusType` (`MELEE`, `PROJECTILE`, `FIRE`, `EXPLOSION`, `MAGIC`, `0.0–1000.0`), rasgos N1 (`>= 70.0`), rasgos N2 conductuales (`>= 200.0`), `HiveSynergy`, `isFungalTideActive`, `suppressedUntilGameTime`, `activeTraitStateRevision`, hasta `24` `HiveHotspot`s, `casualtiesByStimulus` (`getEffectivenessPressure()`), `traitMastery` (`[0.0, 1.0]`), racha *momentum* (hasta `1.45x`) y *pivot detection* (`1.35x`).
  - `tickDecay` corre cada `1200` ticks con decaimiento escalonado (`TIER2_DECAY_MULTIPLIER` sobre umbral N1) y reduce `traitMastery` en `0.05/min` cuando el estímulo cae bajo umbral (incluso en `0.0`).
- **4 Actos y Remisión Post-Matriarca (`HiveProgressionTier`, `ModDimensions`, `ApexMatriarchEntity`):**
  - `actNumber()` (`1–4`), `meetsRequiredAct(...)`, `resolveEffectiveTier(threat, highestActTier)` (conserva el Acto máximo alcanzado durante Remisión), y escalado puro (`primordialSpawnChance`, `primordialAllyHealAmount`, `floraRegrowthMultiplier`, `floraBonusYieldChance`, `pylonTier3PurgeDamage`, `requiredActForSecret`).
  - Morir frente a la Colmena suma `+5%` (`DEATH_THREAT_PENALTY`).
  - Transiciones: `HiveAltarBlock` promueve `AWAKENED` al invocar; muerte de `HiveCoreBossEntity` otorga `MILESTONE_HIVE_CORE_DEFEATED` a `48m`; `MatriarchAltarBlock` promueve `OVERMIND` y rango `ELITE`; derrotar a `ApexMatriarchEntity` activa **Remisión Post-Matriarca (`24000t`)** en ambas dimensiones (`-35%` amenaza, fin de Marea Fúngica, `0.50x` pacing/estrés y sedación en Ciudadela).
  - Cruzar a `deep_mycelium` sincroniza piso de amenaza (`computeSyncedDeepThreat`), `highestTierReached`, rasgos y sinergias desde el Overworld; abrir la grieta antes de Acto III (`ADAPTIVE >= 50%`) dispara `+25%` de estrés inmune inicial.
- **`DamageTelemetryTracker`:**
  - `LivingIncomingDamageEvent`: mitigaciones/sinergias (desvío base `0.30` tope `0.75`; desvío real llama `emitProjectileDeflectionSignal` -> `SYNAPSE_SPARK`/`CHITIN_SHARD` + `HIVE_CORE_SHIELD` + `RESEARCH_BIT_DEFLECTION_WITNESSED`). Clasifica daños mod (`ModdedDamageClassifier` para `ars_nouveau`, `irons_spellbooks`, etc.).
  - `LivingDamageEvent.Post`: alimenta estímulos (`x1.5` cerca de Relés) y restringe conversión/asimilación agresiva a `threatLevel >= 50.0` o `FungalTide` activa.
  - `BlockEvent.BreakEvent`: sangra estímulos y amenaza (`applyNodeDestructionBleed`), registra `lastThreatDropPermille` y alerta nodos vecinos.
- **Rangos Evolutivos (`EvolutionRank`: `BASE`, `MUTATED`, `ELITE`) y Ritmo Nocturno/Diurno:**
  - `MycelialMob` escala `Attributes.SCALE`, vida/daño/XP, transición visual (`20/30` ticks) y texturas por rango; Primordiales bioluminiscentes garantizan extracción con `BioExtractorItem` (`<= 60%` HP vs `<= 50%` normal).
  - En superficie del Overworld (`< 50%`), spawn solo nocturno con retirada al amanecer (`tryDawnSurfaceRetreat`); Incubadoras despiertan de día en `>= 25%` (o en alerta); con `>= 50%` o `FungalTide` hay resistencia solar y spawn diurno sobre micelio/sombra. Cuevas y `deep_mycelium` activos 24/7.
- **Descubrimiento y Gancho Inicial (`ProgressionGoalEngine`, `PlayerDiscoveryData`):**
  - Adquirir `latent_spore` desbloquea `bio_scanner` y marca `RESEARCH_BIT_SPORE_FOUND`; escanear evalúa `resolveCreatureObservation`; `Guide` (`Ongoing Research`) y `PinExpeditionGoalPayload` fijan `USE_TO_ADVANTAGE` o `RECLAIM_CAVE`.
  - `PlayerDiscoveryData` (`ModAttachments.DISCOVERY`, `copyOnDeath`) persiste Bestiario (`17` especies), `5` rasgos, `3` cepas, `8` secretos, score `0–100` de Bastión Perfecto, `10` Engramas (`discoveredEngramsMask`) e Investigación en Curso sincronizados por `DiscoverySyncPayload` (`15` campos, `ModNetworking` v`"27"`).

---

## 2. Doctrina Macro, Acústica Micorrízica, Mimic y Quimeras

- **Estrategia Macro (`HiveDoctrine`, `HiveOrder`, `HiveBlackboard`, `HiveSquadComposer`):**
  - `HiveBlackboard` guarda estado transitorio por instancia de `ServerLevel` y **se limpia en `LevelEvent.Unload`** (nunca filtra entre partidas ni dimensiones).
  - `HiveDoctrine` (`DORMANT`, `FORAGING`, `ADAPTIVE_HUNT`, `SIEGE_ASSAULT`, `RECOVERY`) modula oleadas en `HiveWaveDirector` e Incubadoras; `HiveSquadComposer.composeWaveSquad` ajusta tamaño según `PacingPhase`, `FungalTide` (`+1` escolta) y `symbioticCount > 0` (fuerza `SPORE_ARCHITECT`).
- **Bio-Acústica y Mimic (`MycorrhizalAcousticTracker`, `SynapticMimicArchitectureEngine`, `SynapticMimicEntity`):**
  - Correr/aterrizar/picar sobre `CreepingMyceliumBlock` o `HyphalVeinEntity` cargada emite pulsos sísmicos (`<= 32m`, `+6` biomasa + alerta), silenciados por `Sneak`, `ChitinBoots`, `ChitinPillarBlock` (`isPlayerStealthDamped`), `PurificationPylon`, `SymbioticNodeBlock` (`16m`) o `NeuralEchoDecoyItem` (`12s`).
  - `SynapticMimicEntity` (`65 HP`, posturas `BLADE_DANCER`/`SPORE_MARKSMAN`/`AEGIS_MIRROR`, facetas `MimicPersona`) suplanta a escoltas (visible bajo `SYNAPTIC_VISION`), sabotea Torretas/Drones/Nodos Simbióticos/Señuelos, falsifica nodos en HUD/Jade, refleja `SymbioticMutagen` y se desenmascara con `unmaskWithSynapticShock` (`BioScanner`/Pulso Térmico/`Desync`, `+25%` daño).
- **Quimeras Modulares (`MycelialChimeraEntity`, `ChimeraOvumItem`, `ChimeraOwnershipSavedData`):**
  - Sintetizado con `GeneticSample` + `MycelialBiomass` + `NeuralEchoCore`; incubar en `SymbioticNodeBlock` cuesta `35%` estabilidad (máx. `1`/jugador, `52–68 HP` base, `1.15x/1.35x` en `MUTATED/ELITE`), genes aleatorios `chassisGene`/`mutationGene`, modos `FOLLOW`/`PATROL`/`WAIT` alineados con perfiles de `HiveBrain`.

---

## 3. Engaño Pavloviano, Guerra Civil, IA Táctica y Campo Vivo

- **Memoria Sináptica (`HabitTelemetryTracker`, `SynapticMemoryRouter`):**
  - Observa 4 `PlayerHabit` (`COMBAT_MEDIC`, `HIGH_MOBILITY`, `BASTION_BUILDER`, `SHADOW_INFILTRATOR`, umbral `60.0`) contando solo mobs vivos no amistosos al jugador.
  - Divide estímulos `40%` directo / `60%` al Búfer Volátil del nodo enemigo más cercano (`<= 48m`, consolida cada `30s`); destruir/injertar/suprimir el nodo purga el búfer y suelta `VolatileEngramItem` (`SYNAPTIC_INSIGHT` `+15%` daño `45s` o `FalseEngramInjectorItem`: sobre-especialización falsa `90s` con `+25%` vulnerabilidad a otros tipos y `60s` CD).
- **Guerra Civil y Diplomacia (`HiveStrain`, `PlayerDiplomacyData`):**
  - Cepas `ORTHODOX`, `AMBER`, `CRIMSON`; `induceSchism` excluye muertos, jefes y escoltas aliadas (`SchismInjectorItem` verifica `!isAlive()`).
  - `ModAttachments.DIPLOMACY` (`-100..+100`, `PACT >= 25`, `ALLIED >= 60`): nunca protege ante `ELITE` ni Jefes; atacar aliados dispara Traición Química (`-35` rep + `30s` bloqueo). `MycelialMob.isFriendlyToPlayer` unifica inmunidad en torretas, drones, armas y proyectiles aliados.
- **IA Táctica del Enjambre (`SynapticCommandSystem`, `MobThreatTable`, `MobEpisodicMemory`, `TacticalEvasionEngine`):**
  - Escuadrones en `20m` (`LEADER`: `SynapseWeaver > ColonyWarden > ELITE`, excluyendo jefes): **Phalanx** (`-20%` daño frontal no-mágico), **Emboscada Dorsal** fuera de FOV (`SCULK_CLICKING`), **Desync (`100t`)** por muerte de líder/purificación/Pilón, **Predicción Balística** en `AcidSpitter` (`0.75s` sync / `0.35s` solo / `0.0s` desync), **Swarm Frenzy** (`8s`, `+22%` vel. al caer `>= 2` aliados en `10s`).
  - `MobThreatTable` (`MAX_ENTRIES = 12`, decaimiento `400t`, histéresis `1.15x`, rencor `1.25x`), `MobEpisodicMemory` (`MycelialEpisodicMemory`, máx. `16` claves), `ImminentHazardEvadeGoal` (`6.5m -> 8.5m` con trauma), `TacticalShelterGoal` (`TacticalAlarm`), `KitingRetreatGoal` (bandas `STANDARD`/`EMERGENCY`).
- **IA "Campo Vivo" (`LivingBattlefieldSystem`):**
  - `TacticalTerraformingGoal` + `EphemeralChitinBlock` (decaimiento `240t`, `noLootTable`, prohibido en Pilones o bajo `DESYNC`), barricadas ante proyectiles y **Atasco Corrosivo (`120t`)** a torretas.
  - Memoria de Zonas Letales en `HiveInfluenceMap` (`trauma >= 0.55F` rodeado por ligeros vía `computeTraumaDetourVector`; zapadores `Burrower`/`ColonyWarden`/`ELITE`/`BLAST_SHIELDED` encabezan).
  - `NecrophagyGoal` (`12s` `CarrionSite`, `+6 HP` + `+20 XP`; bloqueado por fuego/Pilón) y sabotaje parasitario de `LeechParasiteEntity` sobre drones (`80t`) e `IronGolem`.

---

## 4. Arquitectura Unificada de Jefes (`BossCombatArchitectureEngine`, `MycelialBossMob`, `MycelialBossPart`)

- **3 Jefes Multi-Parte (`PartEntity<MycelialMob>`):** `SporeBehemothEntity` (2 fases), `HiveCoreBossEntity` (3 fases, anclas `DATA_REMAINING_ANCHORS`) y `ApexMatriarchEntity` (3 fases).
- **Máquina de Estados Exclusiva:** `IDLE -> WINDUP -> ACTIVE -> RECOVERY` + `STAGGERED` con `BossStateGatedMeleeGoal` (bloquea melee vanilla fuera de `IDLE`), `TELEGRAPH_LOCK_TICKS = 7`, selector por utilidad anti-pilar (`>= 4.0m`), combos de 1 paso, `clampPhaseTransitionDamage` en `hurt`, y aterrizaje validado (`findOpenGroundPosition`).
- **Partes y Ruptura:** `WEAK_POINT` (`1.35x`, sube a `1.55x` y baja físicamente en `STAGGERED`) y `ARMORED_CARAPACE` (`0.75x`; al romperse dispara `STAGGERED` `40–50t` y regenera tras `130–160t` o transición de fase). Escala por jugadores extra (`+25%` hasta `+100%`) y `HiveCycleEngine` (`computeScaledCarapaceIntegrity`).
- Telemetría vía `EntityDataAccessor` alimenta HUD (`COMBO!`, `Anchors X/3`, `LOCKED`), `SYNAPTIC_VISION` y poses 3D en `scripts/model_studio/java_emitter.py` (preservar `aoe_inflate` de `RENDER_SPECS`: `2.2D`/`7.2D`/`7.6D`).
