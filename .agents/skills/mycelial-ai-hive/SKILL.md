---
name: mycelial-ai-hive
description: >-
  Router maestro de arquitectura, recetas y diagnóstico para la IA adaptativa de
  la Mente Colmena y el motor nativo HiveBrain 3.0 en Mycelial Mind
  (HiveMindSavedData, HiveBrainOwner, Colonia Soberana, Homeostasis y
  ecosistemas).
---

# Mycelial Hive AI & `HiveBrain 3.0` — Router Maestro

Guía modular de arquitectura, flujos de desarrollo y diagnóstico para los subsistemas de IA, Colmena, Colonia y ecosistemas de **Mycelial Mind** (`2.0.0`, Minecraft `1.21.1`, NeoForge `21.1.267`, Java `21`, `ModNetworking v"27"`).

> **Línea base verificada:** `20/20` entidades sobre `HiveBrainOwner` (`63` clases en `entity/ai/brain/`, `88` en `entity/ai/`, `47` en `hive/`, `19` en `colony/`), `12` registros `ModMemoryTypes` (`11` `MemoryModuleType` + `1` `SensorType`), `179` tests JUnit (`18` suites), `213` `@GameTest`s, `2356/2356` claves EN/ES, `226` eventos de sonido / `596` `.ogg`.

---

## 1. Mapa de Documentación y Cuándo Leer Cada Guía

| Documento | Tamaño / Enfoque | Úsalo cuando trabajes en… |
| --- | --- | --- |
| [hive-brain-engine.md](references/hive-brain-engine.md) | Motor nativo `HiveBrain 3.0` | `HiveBrainOwner`, `HiveBrainProfile`, `HiveBrainModule`, `HiveResourceChannel`/`Arbiter`, `HiveBrainSignal`, `HiveBrainKey`/Blackboard, `HiveBrainTrace`, sensores, `HiveBehaviorTree`, `HiveSteeringEngine`, `HiveUtilityEvaluator`, `HiveInfluenceMap` y arbitraje de metas |
| [hive-mind-and-combat.md](references/hive-mind-and-combat.md) | Mente Colmena y Combate | `HiveMindSavedData`, progresión 4 Actos y Remisión, `DamageTelemetryTracker`, `EvolutionRank`, `HiveDoctrine`/`HiveSquadComposer`, Bio-Acústica, `SynapticMimic`, Quimeras, `PlayerHabit`/Engaño Pavloviano, Guerra Civil, IA Táctica/`LivingBattlefieldSystem` y Jefes multi-parte |
| [sovereign-colony-and-endgame.md](references/sovereign-colony-and-endgame.md) | Endgame y Colonia Soberana | `SovereignColonySavedData`, `ColonySymbiontEntity` (`5` castas/`3` rangos), `ColonyOrderBoard`, Memoria Territorial (`ColonySiteMemory`), Dialectos (`ColonyDialect`), `PostMatriarchExpedition*`, `HiveCycle*` (`6` mutadores) y `FieldEngramId` (`10`) |
| [world-ecosystems-and-integration.md](references/world-ecosystems-and-integration.md) | Ecosistemas, Worldgen y Vanilla+ | `HyphalVeinEntity` Verlet 3D y `BlindSpotSites`, `DimensionalHomeostasis*` (`deep_mycelium`), `MycelialSinkholeFeature`, `TerrainPlacementPolicy`, estructuras (`HiveSanctum`, `MatriarchCitadel`, `TwistedCitadel`), `EcosystemPurposeEngine`, `AcquisitionDifficultyEngine`, `UxJuiceEngine` y APIs NeoForge `21.1.267` |
| [recipes-and-pitfalls.md](references/recipes-and-pitfalls.md) | Recetas, `/mycelial`, CodeMode y Trampas | Paso a paso para crear/migrar entidades `HiveBrainOwner`, perfiles/módulos/canales, nuevos estímulos/rasgos/hábitos, jefes/Colonia, catálogo de comandos `/mycelial`, playbooks `execute` y checklist de `10` trampas comprobadas |
| [HIVE_BRAIN_ARCHITECTURE.md](../../../docs/HIVE_BRAIN_ARCHITECTURE.md) | Arquitectura base de paquetes | Visión de paquetes y ciclo de vida en `docs/` |

**Skills hermanas obligatorias:**
- Auditoría, JUnit, `@GameTest`, Smoke y studios vía FastMCP: [mycelial-codemode](../mycelial-codemode/SKILL.md)
- Triaje de síntomas, RCON, MC Pilot, spark y JFR: [mycelial-diagnostics](../mycelial-diagnostics/SKILL.md)

---

## 2. Seis Invariantes No Negociables

1. **Zero external AI libs / Zero ATs / Zero Mixins:** Todo el motor vive en `entity/ai/brain/` en código Java clean-room sobre `LivingEntity.brain` (`protected` via `replaceVanillaBrain`).
2. **Coexistencia segura y arbitraje opt-in:** Usa `requireChannels(...)` (`MOVE`, `LOOK`, `COMBAT`, `ABILITY`, `TERRAFORM`) solo en `HiveBehaviour` nuevos diseñados para exclusión mutua; los jefes preservan `BossStateGatedMeleeGoal` y los aliados preservan castas/`CommandMode`/`FlyingMoveControl`.
3. **Pure Logic First (sin `BuiltInRegistries` en JUnit 5):** No referenciar `ModMemoryTypes.<clinit>` ni bloques/ítems en unit tests puros; testear `*Engine`, `HiveBrainKey`, `HiveBrainBlackboard`, `HiveBrainSignal`, `HiveResourceArbiter`, `HiveBrainTrace` y `HiveBrainSnapshot` en JUnit 5, y `BrainActivityGroup`/memorias/rebuilds en `@GameTest`.
4. **Aislamiento estricto por nivel y sesión:** `HiveBlackboard` e `HiveInfluenceMap` van por instancia de `ServerLevel` y se purgan en `LevelEvent.Unload`; en cliente, `Client*Cache` se limpia al desconectar.
5. **Worldgen y Luz sin bloqueos en NeoForge `1.21.1`:** Prohibido `ThreadedLevelLightEngine.runLightUpdates()` (`UnsupportedOperationException`; usar `measureEffectiveBlockLight`) y prohibido `findNearestMapStructure` en worldgen (usar `level.hasChunk` + `ChunkStatus.STRUCTURE_REFERENCES`).
6. **Presupuesto de mantenimiento del skill (`<= 32 KB`):** `scripts/validate_agent_skills.py` impone `MAX_SKILL_MD_BYTES = 32_768`. Cualquier detalle extenso nuevo va al archivo temático correspondiente en `references/`, manteniendo este `SKILL.md` como router compacto.

---

## 3. Verificación Rápida (`/mycelial` y FastMCP `CodeMode`)

En juego/RCON:
- `/mycelial ai [radio|trace|profile|pause|signal]` — traza `16` eventos, perfiles en caliente y señales de `HiveBrain`.
- `/mycelial status`, `/mycelial colony`, `/mycelial cycle`, `/mycelial homeostasis`, `/mycelial verify <escenario|all>`.

En `mycelial-mind-dev` `execute()`:
```python
import asyncio
overview, units, gt = await asyncio.gather(
    call_tool("get_hive_ai_overview", {}),
    call_tool("list_unit_tests", {"suite_filter": "Hive"}),
    call_tool("list_gametests", {"batch_filter": "hive"}),
)
return {"brain_files": overview.get("brain_files_count"), "enums": overview.get("brain_core_enums"), "units": units["count"], "gametests": gt["count"]}
```

Consulta [recipes-and-pitfalls.md](references/recipes-and-pitfalls.md) para la guía completa de comandos, playbooks de test y trampas resueltas.
