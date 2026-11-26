# Matriz Completa de Tests de Mycelial Mind (`CodeMode` `mycelial-mind-dev`)

Mapa canónico de las suites actuales del repositorio (`147` JUnit, `201` `@GameTest`, `5` modos de `runVisualSmoke`, `runAnimationVisualSmoke`, `runStructureVisualSmoke`, `runHiveLifecycleSmoke`, `24` escenarios RCON y `6` estudios Python) y su ejecución directa desde `execute()`.

---

## 0. Qué correr según qué archivo tocaste

| Área modificada | Verificación mínima recomendada en `execute` |
| --- | --- |
| `hive/` / `colony/` / `entity/ai/` | `run_gradle_task(["test"], test_filter="HiveBrainEngineUnitTest")` + `run_gradle_task(["runGameTestServer"], isolated_gametests=True)` + preset `colony_and_dialects` / `audio_and_ai` |
| `hyphal/` / `audio/` / `scripts/sound_studio/` | `run_sound_studio(check_only=True, run_tests=True)` + `audit_sound_masking()` + `test_filter="OpenAlEfxLoopbackTest"` / `"HyphalPhysicsTest"` / `"AcousticPhysicsEngineTest"` |
| `datagen/` / `compat/` / recetas / `lang/` | `run_gradle_task(["runData", "test"], test_filter="DataGenIntegrityTest")` + `test_filter="ModCompatTest"` + `audit_mod_assets()` |
| `scripts/{model,texture,particle,codex}_studio/` | Estudio correspondiente (`check_only=True, run_tests=True`) + `runVisualSmoke` (`visual_smoke_mode="default"` o `"particle"`) + `check_visual_regression()` |
| `worldgen/` / `structure/` / `scripts/build_structures.py` | `run_structure_studio()` + `runStructureVisualSmoke` + `check_structure_visual_regression()` + preset `sinkhole_and_worldgen` |
| Pantallas UI / Dialectos / Reliquias / Expediciones | `run_gradle_task(["runVisualSmoke"], visual_smoke_mode="dialect\|relic\|expedition")` + `test_filter="CodexBookScreenLogicTest"` / `"PostMatriarchExpeditionRulesTest"` |
| `scripts/mcp/` / `.agents/skills/` | `validate_skills()` + `get_bridge_health()` + `python -m py_compile scripts/mcp/server.py scripts/mcp/test_server.py` (y `test_server.py` en host) |

---

## 1. Pruebas Headless JUnit 6 (**147** tests en **17** suites, ~2–4 s)

Se ejecutan sin bootstrap de FML/Minecraft (`src/test/java/com/alex/mycelialmind/*.java`).

- **Suite completa:** `await call_tool("run_gradle_task", {"tasks": ["test"], "no_daemon": True, "max_workers": 1})`
- **Suite filtrada:** `await call_tool("run_gradle_task", {"tasks": ["test"], "test_filter": "OpenAlEfxLoopbackTest"})` (o `Clase.metodo`)
- **Descubrimiento dinámico:** `await call_tool("list_unit_tests", {"suite_filter": "HyphalPhysicsTest"})` y reporte XML en `get_latest_junit_report()`

| Suite JUnit 6 (`src/test/java/com/alex/mycelialmind/`) | Tests | Cobertura principal |
| --- | --- | --- |
| `HiveAlgorithmsUnitTest` | 53 | Amenaza, N1/N2, sinergias, hábitos, arquitectura hifal, `EcosystemPurposeEngine`, `AcquisitionDifficultyEngine`, `BossCombatArchitectureEngine`, `UxJuiceEngine`, sondeo `TerrainPlacementPolicy` |
| `HiveBrainEngineUnitTest` | 11 | Motor nativo clean-room `HiveBrain`, `HiveBrainSchedule`, `UtilitySelectorBehaviour`, `HiveBehaviorTree`, `HiveSteeringEngine`, LOD adaptativo |
| `HyphalPhysicsTest` | 10 | Física Verlet 3D (`HyphalVerletRope`), arrastre/turgencia, ondas sísmicas, raycast de corte diagonal, retroceso y marchitamiento |
| `MigratedGameTestLogicTest` | 8 | Serialización NBT de `HiveMindSavedData`, snapshots de reconstrucción, lógica pura migrada desde `@GameTest` |
| `ColonyDialectLessonTest` | 7 | Lecciones origen→destino, protagonista maestro, colisiones/variantes de secuencia, limpieza |
| `ColonyReconstructionTest` | 7 | Recuerdos de ruta/patrulla/hábito por testigo, estados bloqueados y abandono |
| `DataGenIntegrityTest` | 7 | Paridad dual-root `src/main/resources` vs `src/generated/resources` (`0` solapamientos, `28` bloques, `96` ítems, `128` JSON, logros/EN+ES) |
| `SovereignColonyUnitTest` | 7 | Cola `ColonyOrderBoard`, arrendamientos TTL, fatiga/moral, rasgos `SovereignTrait` T1–T3 |
| `AcousticPhysicsEngineTest` | 6 | Absorción por material (`MaterialAbsorption`), oclusión multi-bloque, `SubmersionState`, `SwarmVoiceBudget`, capas EFX |
| `CodexBookScreenLogicTest` | 6 | Parser Markdown del Códice, tintes hex, latido por Amenaza, selección de atlas por Acto I–IV |
| `OpenAlEfxLoopbackTest` | 6 | Audio OpenAL `ALC_SOFT_loopback` + `ALC_EXT_EFX` real + Hann/FFT radix-2 (`4` slots `EAXReverb`, `16` LPF, inmersión, sin Minecraft) |
| `ModCompatTest` | 5 | `ClaimProtectionCompat` (FTB Chunks/OPAC), `ModdedDamageClassifier` (Ars Nouveau/Iron's Spells), `11` recetas `recipe/compat/`, Better Combat |
| `CleanRoomAiCapabilitiesUnitTest` | 4 | Primitivas de IA clean-room (`HiveBtSelector`/`Sequence`/`DynamicGuard`, `HiveSteeringEngine`) |
| `HiveCycleAndEngramUnitTest` | 4 | Mutadores de Ciclo Post-Matriarca, puntuación/grados y reglas de `FieldEngramId` |
| `PostMatriarchExpeditionRulesTest` | 3 | Reglas de ruta/jardín, recuperación y desbloqueo de expediciones |
| `CombatPoseTransitionTest` | 2 | Transiciones de poses de combate y suavizado en modelos |
| `SpringAnimationTest` | 1 | Amortiguación armónica/spring de extremidades en `ModelStudio` |

> **Nota `:pitest`:** Actualmente la tarea `:pitest` presenta incompatibilidad conocida en `build.gradle` (`afterEvaluate` leyendo `additionalClasspath.files` bajo ModDevGradle `2.0.148` + Pitest `1.19.0`); usa `run_gradle_task(["runData", "test", "build", "runGameTestServer"], isolated_gametests=True)` para validación completa y `get_latest_junit_report()` para resultados de pruebas.

---

## 2. `@GameTest` NeoForge (**201** tests en **41** batches, ~6 s)

- **Ejecución aislada canónica:** `await call_tool("run_gradle_task", {"tasks": ["runGameTestServer"], "isolated_gametests": True, "no_daemon": True, "max_workers": 1})` (usa `artifacts/diagnostics/isolated-gametest-server` sin contaminar mundos del usuario).
- **Inspección por batch:** `await call_tool("list_gametests", {"batch_filter": "hive_brain"})`.

| Categoría | Batches registrados (`41` totales) | Cobertura / Tests destacados |
| --- | --- | --- |
| Núcleo e IA `HiveBrain` | `hive_brain_clean_room` (6), `entity_ai_live` (6), `swarm_steering` (1), `weaver_support` (1), `spitter_regroup_utility` (1), `stalker_flank` (1), `rally_squad_movement` (1), `hive_state_sync` (1), `algorithm_integration` (9) | Motor `HiveBrain`, `UtilitySelectorBehaviour`, 18/18 entidades tickeando limpias, BTree/Steering/LOD nativos, detección de proyectiles |
| Hifas Verlet 3D y Acústica | `hyphal_route_veins` (9), `hyphal_verlet_filament` (1) | Física Verlet, crecimiento en rastro, ondas sísmicas, corte con división y latigazo, spray presurizado, acústica micorrízica |
| Colonia Soberana y Dialectos | `colony_integration` (1), `colony_dialect_integration` (1), `colony_dialect_journey` (1), `colony_creative_override` (1), `colony_progression_gate` (1), `colony_symbiont_incubation` (1), `dialect_live_navigation` (1), `sovereign_colony_sync` (1), `sovereign_nexus_hopper` (1), `sovereign_nexus_upgrade` (1), `sovereign_rival_raid` (1) | Incubación, tolvas, mejoras T1–T3, bloqueo Post-Matriarca, navegación real de aprendiz y conflictos de dialecto |
| Ecosistema, Progresión y UX | `acquisition_difficulty` (1), `ecosystem_purpose` (1), `first_outbreak_research` (1), `full_endgame_integration` (2), `homeostasis` (6), `symbiotic_blade_arc` (1), `ux_juice` (1), `general_polish` (1) | 4 capas funcionales, mantenimiento/cargas de estaciones, viñetas/sync UX, homeostasis y remisión |
| Worldgen y Estructuras | `sinkhole_ecosystem` (1), `worldgen_terrain_balance` (2), `structures` (4), `structures_surface_hive` (1), `architectural_reflection` (1), `outbreak_and_hive_fixes` (1), `outbreak_vertical_taproot` (1), `surface_hive_feature` (1), `blind_spot_outbreak` (1) | Topografía `deep_mycelium` `0..192`, Sima Micelial de 3 estratos, `NoiseColumn` / contrafuertes sin bloques flotantes |
| Generales | `default` (52), `defaultBatch` (74) | Mecánicas base, ítems, bloques, mutágenos, jefes y compatibilidad |

---

## 3. Pruebas Visuales y Lifecycle Client (`build.gradle`)

| Tarea Gradle | Modo / Propiedad (`tasks` / `visual_smoke_mode`) | Directorio aislado (`artifacts/diagnostics/`) | Qué verifica |
| --- | --- | --- | --- |
| `runVisualSmoke` | `visual_smoke_mode="default"` (o `""`) | `run/screenshots/visual_smoke_mobs.png` | Escena principal de mobs, flores y bloques (`check_visual_regression`) |
| `runVisualSmoke` | `visual_smoke_mode="dialect"` (`-PdialectVisualSmoke=true`) | `dialect-visual-client/` | Pantalla adaptativa `SovereignNexusScreen` + pestaña de Dialectos |
| `runVisualSmoke` | `visual_smoke_mode="relic"` (`-PrelicVisualSmoke=true`) | `relic-visual-client/` | Escena visual de reliquias y efectos |
| `runVisualSmoke` | `visual_smoke_mode="particle"` (`-PparticleVisualSmoke=true`) | `particle-visual-client/` | Renderizado aditivo y galería in-game de partículas |
| `runVisualSmoke` | `visual_smoke_mode="expedition"` (`-PexpeditionVisualSmoke=true` + `-PexpeditionHoldTicks`, `-PexpeditionForceFailure`, `-PexpeditionReloadWorld`, `-PexpeditionReloadPulse`, `-PexpeditionKeepSmokeEntities`) | `expedition-visual-client/` | Escena de expediciones, flores recuperadas y recarga en cliente |
| `runAnimationVisualSmoke` | `-PanimationPair=<par>` (ej. `brute_warden`), `-PanimationHideEmissive`, `-PanimationRank` | `animation-visual-client/` | Poses/animaciones de combate y capas emisivas |
| `runStructureVisualSmoke` | Sin flags extra + `check_structure_visual_regression()` | `structure-visual-client/` | `5/5` capturas de estructuras (SSIM/pHash/LPIPS deterministas) |
| `runHiveLifecycleSmoke` | `-PhiveLifecycleForceFailure`, `-PhiveLifecycleHoldTicks` | `hive-lifecycle-client/` | Ciclo completo de cliente integrado de la Colmena |

---

## 4. Smoke RCON Headless (**24** escenarios en `verify all`)

- **Suite completa aislada:** `start_smoke_server()` → `run_rcon_verify_suite(include_worldgen_audit=True)` → `stop_smoke_server(cleanup_dir=True)`.
- **Verificación individual:** `run_live_verify_scenario(scenario=...)` con cualquiera de los `24` escenarios (`projectile_adaptation`, `pylon_containment`, `boss_encounter`, `tier2_synergy`, `synaptic_network`, `symbiosis`, `units`, `expansion`, `tide`, `counteroffensive`, `arsenal`, `mycorrhizal`, `hive_ai`, `chimera`, `memory`, `tactical_ai`, `field_ai`, `hyphal_siege`, `loot`, `homeostasis`, `polish`, `dimension_progression`, `cycles`, `colony`, `sinkhole`) o `worldgen`.
- **Presets paramétricos en `run_live_scenario`:** `progression_ramp`, `swarm_load`, `diplomacy_and_memory`, `homeostasis_and_dimension`, `colony_and_dialects`, `cycles_and_endgame`, `sinkhole_and_worldgen`, `audio_and_ai`, `full_smoke_matrix`, `custom`.

---

## 5. Suites Python de Studios y MCP (`scripts/`)

| Estudio / Puente | Herramienta `CodeMode` | Tests | Alcance |
| --- | --- | --- | --- |
| Model Studio | `run_model_studio(check_only=True, run_tests=True)` | 26 | `18` EntityModels, UV manifests, geometría 3D |
| Texture Studio | `run_texture_studio(check_only=True, pbr=True, run_tests=True)` | 41 | Oklab/SDF/LabPBR `_n`/`_s`, dual-root `ReclaimedBloom`, atlas GUI I–IV |
| Particle Studio | `run_particle_studio(check_only=True, run_tests=True)` | 6 | `34` partículas procedurales y `particles/*.json` |
| Sound Studio v5 | `run_sound_studio(check_only=True, run_tests=True)` + `audit_sound_masking` + `inspect_sound_event` | 20 | `226` eventos / `596` `.ogg`, `69` stems RTX, True-Peak 4x, costuras `11` loops, matriz Bark `11×20` (`220` pares) GPU/CPU |
| Codex Studio | `run_codex_studio(check_only=True, run_tests=True)` | 5 | `12` capítulos / `62` entradas / `249` páginas, paridad EN/ES + Modonomicon |
| Structure Studio | `run_structure_studio()` | — | Validación/generación de `.nbt` de estructuras |
| FastMCP Bridge | `python scripts/mcp/test_server.py` (en host fuera de sandbox `.pyd`) | suite e2e | Handshake `CodeMode`, 67 tools, RCON mock, validadores de parámetros |
