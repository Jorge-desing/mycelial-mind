# Ecosistemas, Homeostasis, Worldgen, Propósito en Capas e Integración Vanilla

Subsistemas físicos (`HyphalVeinEntity`), homeostasis dimensional (`deep_mycelium`), ecosistema vertical (`MycelialSinkholeFeature`), worldgen en 4 dimensiones y acoplamiento Vanilla/mods.

---

## 1. Hifas 3D con Física Verlet (`HyphalVeinEntity`, `HyphalVerletRope`, `HyphalSiegeDirector`)

- **Entidad Unificada (`mycelialmind:hyphal_vein`):**
  - Simula hebras 3D de `2..32` nodos Verlet (`HyphalVerletRope.stepVerlet`) sincronizadas por `DATA_PACKED_NODES` y AABB dinámico (`recenterEntity()` + `getBoundingBoxForCulling()`). `ModItems.HYPHAL_VEIN` es solo material (`TooltipMaterialItem`), no bloque colocable.
  - Solo `rootAnchor` (`pinStart`) y `tipAnchor` (`pinEnd`) pueden fijarse a soporte sólido; nodos interiores colisionan con terreno (`resolveSolidCollision`) modulados por turgencia (`0.0–1.0` según nutrientes + lluvia/agua `+0.25F` - fuego/lava/Nether `-0.35F`). Si ambos extremos pierden soporte, la hebra cae y se desintegra.
- **Crecimiento, Corte, Ondas Sísmicas y Render:**
  - El rastro (`TrailRecord` + `supercoverLine`) siembra hasta `8` hebras/chunk (`growthIntervalTicks = 10t`).
  - Cortar en interior (`severAt` / `splitRope`, `raycastClosestSegment` `PICK_TOLERANCE = 0.45D`) divide hebras `>= 4` nodos en dos `HyphalVeinEntity` con retroceso (`applySeverRecoil`), rocío a presión si `turgor >= 0.65F` (`MOVEMENT_SLOWDOWN` + `MYCELIAL_INFECTION`, mitigado con `>= 2` piezas Quitina) y marchitado de la mitad suelta (`~60t`).
  - Fricción (`computeEntityDragFactor`): ralentiza no-miceliales en `strength >= 2`, impulsa `1.10F` a `MycelialMob`/`MycelialChimeraEntity`, mitigado `75%` por `CHITIN_BOOTS` o sigilo; pasos rápidos inyectan ondas (`0.45F`, `1 nodo / 2t`, `WAVE_DECAY_PER_HOP = 0.78F`, corte `< 0.08F`, propagación `1.8m`, explosiones `6.5m` hasta `1.0F`).
- **Puntos Ciegos (`BlindSpotSites`, máx. `6`, `BaseAnchors` máx. `16`) y Fotofobia:**
  - Nuclean en puertas/vallas/trampillas/esquinas oscuras (`LightLayer.BLOCK <= 4` fuera de Pilones) con ritmo multi-día (`1800/1500/1200/900t`); iluminar (`>= 9`) frena con aviso (`>= 12` o `4` pulsos marchita el sitio); completar asciende el núcleo a `INCUBATOR_NODE` + `ELITE`.
  - **Trampa NeoForge 1.21.1:** nunca llamar `ThreadedLevelLightEngine.runLightUpdates()` (lanza `UnsupportedOperationException`); usar `HyphalSiegeDirector.measureEffectiveBlockLight` (ignora luz bioluminiscente propia y usa `ANTIBODY_CYST` temporal en `< 6m` para evitar auto-fotofobia).
- **Acento Arquitectónico (`ArchitecturalAccent`, `ArchitecturalAccentProcessor`):** captura bloque primario, pilar compañero (`TOWER_SPIRE >= 1.35`, `BUNKER_VAULT <= 0.75`, `HALL_GALLERY`) y densidad lumínica para transformar `hive_sanctum` y `twisted_citadel` (`twisted_reflection/{spire_tower,gallery_hall,vault_bunker}.nbt`).

---

## 2. Homeostasis Dimensional (`deep_mycelium`, `DimensionalHomeostasis*`)

Restringida a `ModDimensions.DEEP_MYCELIUM_LEVEL` (`DimensionalHomeostasisSavedData`: `mycelialmind_dimensional_homeostasis`):
- **Estrés por Chunk (`0–100`):** `STABLE`, `IRRITATED`, `IMMUNE_RESPONSE` + sensibilización antigénica (`Nv.0–3`), escalado por Acto (`0.85x–1.30x`), trauma por excavar bloques nativos y difusión inflamatoria vecinal (tope `45%` sin bloques ajenos).
- **Patrulla Leucocitaria y Lluvia Ácida:** en Tormenta de Citocinas (`>= 85%`, CD `1200t`) aparece patrulla; bajo cielo natural (`isExposedToCeilingAcidRain`), la lluvia ácida cura `+1.5 HP` + vel. a `MycelialMob`, atasca/desgasta torretas y drones (salvo `BioChitinLatticeBlock` encima o simbiontes junto a nodos/matriz); set `4/4` `ChitinArmorItem` reduce daño a `0.25x` sin desgaste ni `SPORE_ROT`.
- **Quistes Anticuerpo (`AntibodyCystBlock`):** encapsula bloques ajenos por caras libres (tope configurable `homeostasisMaxCystsPerChunk`), ahoga Pilones con `>= 4` caras cubiertas (`setPylonSmothered`: anula protección/enlaces/Redstone `0`), emite pulso de histamina (`2.5m`) al romperse en `IMMUNE_RESPONSE` y se marchita al sedar a `STABLE`.
- **Sedación:** `SymbioticNodeBlock`, `SovereignNexusBlock` (`-1.5 * tier`), `SynapticSuppressorItem`, `TrucePheromoneItem`, Pilones T2/T3 y Remisión (`0.50x`).

---

## 3. Ecosistema Sima Micelial (`MycelialSinkholeFeature`, `SinkholeEcosystemEngine`)

- **3 Estratos Verticales + Galerías Laterales (`mycelialmind:mycelial_sinkhole`):**
  1. **Corona Superficial:** borde orgánico adaptado al bioma huésped (`resolveSurfaceBiomePalette`).
  2. **Garganta Crepuscular (`dy = -1..-21`):** pozo espiral con `RootCapShelfBlock`, pilares y vetas ámbar.
  3. **Caverna Raíz (`dy = -22..-32`):** suelo `RootSapMossBlock` (`root_sap_vial`), chorro `SporeUpdraftVentBlock` (empuja `14–26m` / `20m` purificado, ascenso/descenso con sigilo), cofre `chests/sinkhole_root_vault`, `RootKeeperEntity` (`10–28m`, `6m` + Debilidad/Lentitud junto a Pilón) y `2` `SporeGliderEntity` polinizadores; al Norte cámara húmeda de savia y al Sur túnel dinámico conectado a cuevas (`findSouthCaveTargetFloorDy`).
- Purificar el fondo sella la brecha (`isBreachSealed`) y convierte el chorro en elevador seguro; `ResonantSapElixirItem` (Mesa Sináptica o Alambique con `root_sap_vial`) protege contra chorros tóxicos y otorga Visión Nocturna, Caída Lenta, Regeneración e `Insight`.

---

## 4. Worldgen, Reglas de Terreno (`TerrainPlacementPolicy`) y Estructuras

- **Dimensión `deep_mycelium` (`0..192`, `seaLevel = 28`):** pilares verticales (`Noises.PILLAR*`), terrazas multi-nivel (`Y = 28..156`), `SurfaceRules` para `latent_spore_forest`, `hive_heart_sanctum` y `deep_mycelium_caverns`, `3` features de bioma (`CAVERN_STALACTITES`, `SPORE_CANOPY_ARCH`, `SANCTUM_AMBER_MONOLITH`), llegada segura `ModDimensions.findSafeArrival` (`Y = 76`).
- **Terreno Overworld (`TerrainPlacementPolicy`, `#mycelialmind:surface_excluded`):**
  - El modificador del Overworld genera solo `mycelial_outbreak` subterráneo (`center.getY() < surfaceY - 6`) con `hasSafeSubterraneanOutbreakFooting`. No registra manchas, colmenas ni sumideros superficiales independientes.
  - En el Overworld, los nodos de brotes subterráneos conservan su marca al guardar y madurar. `OutbreakNaturalGrowth` siembra y extiende un bloque por pulso a partir del micelio conectado, con búsqueda acotada en suelo seco transitable; `CreepingMyceliumBlock` también se propaga por las cuevas. El Arquitecto solo amplía colonias desde micelio adyacente. En el Overworld, los puntos ciegos necesitan suelo de micelio existente y sus cimientos solo colocan micelio conectado.
  - Cuando el crecimiento conectado alcanza la superficie, `OutbreakSurfaceEmergence` puede originar una colmena o un sumidero. Comprueba chunks cargados, protección, líquidos y bloques ajenos, evita cortar micelio subterráneo y conserva el suelo ya colonizado; las estructuras enlazadas no generan alfombras de micelio. Micelio Profundo conserva su terreno natural.
  - Las búsquedas de estructuras vanilla usan `level.hasChunk(...)` + `ChunkStatus.STRUCTURE_REFERENCES` y nunca `findNearestMapStructure` en worldgen.
  - Pendiente 2 anillos (`deltaY <= 3` / `<= 4`), cimentación hasta `8m` (`6m` Outbreak) y columna seca en Sima (`lava == 0`, `water <= 12`, `ratio <= 0.12`).
- **Estructuras (`MycelialStructureBootstrap`, `WorldgenAuditEngine`):**
  - `HIVE_SANCTUM` (`HiveSanctumStructure`: spawn `>= 160m`, `hasDrySolidSubterraneanEnvelope`, `ENCAPSULATE`, `IGNORE_WATERLOGGING`, `DimensionPadding(8, 224)`), procesadores `MycelialFluidClearProcessor`, `MycelialFoundationPillarProcessor`, `ArchitecturalAccentProcessor`, `ProtectedBlockProcessor`.
  - `MATRIARCH_CITADEL` y `TWISTED_CITADEL` (`BEARD_THIN`, sondeo 9 columnas `findCavernPlacementY`, `selectConsistentFloorCluster`, rampa Sur 6-conectada `dz = 25..26` y `anchorBridgePier`).
  - `SURFACE_HIVE_CACHE` conserva invariante de auditoría (`ensureSurfaceHiveAuditCore = true`).

---

## 5. Propósito en Capas, Adquisición, UX y Compatibilidad Vanilla+

- **4 Capas (`EcosystemPurposeEngine`):**
  1. *Arquitectura Viva:* `BioChitinLatticeBlock` (aspillera permeable a proyectiles aliados/escudo ácido), `LuminescentAmberVeinBlock` (`+4m` Pilón, `+1` LabTier efectivo, acelera flora, comparador `7`/`15`), `ChitinPillarBlock` (sigilo acústico), `MycelialArchBrickBlock`/`ReinforcedBioBarrierBlock` (inmunes a micelio/hifas/quistes; `>= 2` barreras suman `+3m` al Pilón).
  2. *Auras de Jardín:* `ReclaimedBloomBlock.emitSanctuaryAura` (`6m`, cada `80t`, `6` colores).
  3. *Excedentes Diegéticos:* `FieldEngramItem`, `NEURAL_ECHO_CORE`, `HIVE_HEART`, `OVERMIND_ESSENCE` recargan/purgan/otorgan Gracia en estaciones.
  4. *Counterplay:* `ChitinBrute` se aturde al embestir `ChitinPillar`/`ReinforcedBioBarrier`; `PyroShroom` cauteriza quistes y se desactiva con agua/`NeutralizingSpore`; `ColonyWarden` pierde aura al desincronizarse; `LeechParasite` limpia infección al morir.
- **Economía, Gating y Mantenimiento (`AcquisitionDifficultyEngine`):**
  - `MonumentFocusBlockEntity` (bioenergía `0–100`; `0` -> `setPylonDepleted`) y `PurificationTurretBlockEntity` (munición `0–64`, inicial `32`, limpieza con antídoto/quitina) operan manualmente o por tolva respetando Acto II/III (Creative bypass via `instabuild`).
  - `BioExtractorItem`: `+2.0` amenaza y alerta `14m`; exige `<= 50%` HP (`<= 60%` Primordial).
  - Bóvedas Selladas (`outbreak_sealed_vault.json` en Acto I) evitan *sequence breaks*.
- **UX / Juice (`UxJuiceEngine`, `ClientJuiceController`, `ClientUiAudio`, `ClientTooltipHelper`):** vibración de cámara acotada (`<= 2.5°`), viñetas (`SPORE_ROT` > `INFECTION` > `SUPPRESSED` > `HIGH_THREAT`), `DiegeticToastManager`, alertas de carga/munición baja (`HIVE_EMBER`), detalles con `Shift` (`FMLEnvironment.dist.isClient()`).
  - **APIs NeoForge 21.1.267 & DataGen:**
  - Tolvas (`Capabilities.ItemHandler.BLOCK`), Alambique (`RegisterBrewingRecipesEvent` + `DataComponentIngredient`), Clérigo/Albañil/Mercader, GLM (`MycelialTraceLootModifier`), `ModAttributes` (`infection_resistance`, `acid_resistance` en `Player` + Armadura de Quitina/lluvia ácida), `ModRecipes` (`mycelialmind:synaptic_synthesis` 100% vía `RecipeManager`), `DispenserBlock`/`ProjectileItem` (`PurifierGrenadeItem`, `SporeAntidote`, `ConcentratedAntidote`, `TrucePheromone`) + `ItemAbilities` (`SHEARS_HARVEST`, `SHIELD_DISABLE`/`canDisableShield`), `ModDamageTypes` (`5` `DamageType`s data-driven + `DamageTypeTags` + `10` mensajes de muerte EN/ES), `ModEnchantments` (`antimycotic` con feedback `SYNAPSE_SPARK` + `ENZYME_LANCE_THRUST`, `bio_ward` conectado a `ModAttributes`, y `synaptic_harvest` integrado con tooltip dinámico en `BioExtractorItem`, solo tesoro `TREASURE`/`TRADEABLE`/`ON_RANDOM_LOOT` en cofres, aldeanos y jefes `20%/40%/75%` en `MycelialEntityLootSubProvider`, con `DAMAGE_EXCLUSIVE`), `ModDataMaps` (`damage_stimulus` en `DamageTelemetryTracker`, `symbiotic_nutrient` en `SynapticNodeBlockEntity`, `pylon_fuel` en `MonumentFocusBlockEntity`, `turret_ammo` en `PurificationTurretBlockEntity`, y tooltips Shift-only vía `MycelialDataMapTooltips` / `ItemTooltipEvent`), eventos reactivos `ModConfigEvent.Reloading` (re-sincronización de `HiveSyncPayload` en servidor + reinicio de `ClientSoundscapeDirector`/`ClientAcousticController` en cliente), `MobEffectEvent.Applicable` (inmunidad nativa de criaturas/aliados del mod a `MYCELIAL_INFECTION`/`SPORE_ROT`) y `BlockEvent.BlockToolModificationEvent` (limpieza de `CREEPING_MYCELIUM` -> `DIRT` con azada `HOE_TILL`), datapacks opcionales embebidos (`MycelialBuiltInPacks` `hardcore_overmind` y `relaxed_research` vía `AddPackFindersEvent` con detector `ActivePreset`, omitidos bajo `GameTestHooks.isGametestServer()`), `DeepMyceliumSpecialEffects` (`RegisterDimensionSpecialEffectsEvent`), `MycelialItemDecorators` (`RegisterItemDecorationsEvent`), `MycelialClientTooltips` (`RegisterClientTooltipComponentFactoriesEvent`), `MycelialClientExtensions` (`RegisterClientExtensionsEvent`: `IClientMobEffectExtensions`, `IClientItemExtensions` y `IClientBlockExtensions` 28/28 `ModBlocks` con partículas orgánicas en hit/destroy), `MycelialClientCommands` (`RegisterClientCommandsEvent`: `/mycelialclient status|hud|juice|particles|audio|packs|codex`), `neoforge.mods.toml` + `mycelialmind_logo.png`, `ModCriteriaTriggers` (`hive_milestone`, `synaptic_synthesis` en lugar de `ImpossibleTrigger`), `ModStats` (`5` estadísticas personalizadas), `EffectCure` híbrido (`MILK` vs `MYCELIAL_ANTIDOTE_CURE`), `TicketController` (`MycelialChunkTicketManager` en Pilón T3 activo), librerías Java embebidas vía `jarJar` (`Caffeine 3.2.0` en `PurificationZoneTracker`, `JGraphT 1.5.3` en `SynapticNetworkTopology` para clústeres/nodos críticos/ruta mínima, `RoaringBitmap 1.6.30` en `MycelialSpatialBitset` para índices espaciales comprimidos de pilones/nodos/chunks disputados, `Apache Commons Statistics 1.3` en `HiveCombatTelemetryStats` para media/desviación/picos z-score por `StimulusType` y `Bucket4j 8.22.0` en `MycelialRateLimiter` para control de ráfagas en `DamageTelemetryTracker`), `ClaimProtectionCompat` (FTB Chunks/OPAC, solo destrucciones), `ModBlocks.HIVE_PROTECTED_BLOCKS` (`#mycelialmind:hive_protected_blocks`, incl. Waystones, inmune a Burrower/Ácido/Micelio/Quistes), `MycelialCuriosCompat` (`bio_scanner` belt, `overmind_crown` head `-35%` infección/`-30%` lluvia ácida, `quorum_seal` charm `+20%` afinidad/`-50%` cooldown traición), `MycelialJadePlugin`, `MycelialTopCompat` (The One Probe IMC `getTheOneProbe`), `MycelialJourneyMapPlugin` + `MycelialMapOverlayGeometry` (`ClientHiveCache` listeners + `MycelialClientConfig.JOURNEYMAP_OVERLAYS_ENABLED`), `MycelialKubeJSPlugin` + `MycelialKubeJSBindings` (`kubejs.plugins.txt`, binding global `MycelialMind`), JEI/EMI (`SynapticBenchRecipeDefinitions.allCompatInfoEntries()`), Redstone (`MycelialRedstoneTelemetry`), DataGen dual-root verificado por `DataGenIntegrityTest`, utilidades compartidas `WorldgenFeatureSupport`/`StructurePieceSupport`, `2364/2364` EN/ES (`MycelialText`) y `226` eventos / `596` `.ogg` (`ModSounds`).
