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
  - `SurfaceHiveFeature`, `MycelialSinkholeFeature` y `MycelialOutbreakFeature` superficiales rechazan océanos/ríos/playas/`mushroom_fields`/picos extremos, respetan distancias seguras al spawn por Acto y evitan estructuras vanilla mediante lectura no bloqueante `level.hasChunk(...)` + `ChunkStatus.STRUCTURE_REFERENCES` (nunca `findNearestMapStructure` en worldgen).
  - Brote subterráneo (`center.getY() < surfaceY - 6`) permitido bajo cualquier bioma con `hasSafeSubterraneanOutbreakFooting`.
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
  - Tolvas (`Capabilities.ItemHandler.BLOCK`), Alambique (`RegisterBrewingRecipesEvent` + `DataComponentIngredient`), Clérigo/Albañil/Mercader, GLM (`MycelialTraceLootModifier`), `EffectCure` híbrido (`MILK` vs `MYCELIAL_ANTIDOTE_CURE`), `TicketController` (`MycelialChunkTicketManager` en Pilón T3 activo), `ClaimProtectionCompat` (FTB Chunks/OPAC, solo destrucciones), `MycelialJadePlugin`, JEI/EMI (`SynapticBenchRecipeDefinitions.allCompatInfoEntries()`), Redstone (`MycelialRedstoneTelemetry`), DataGen dual-root verificado por `DataGenIntegrityTest`, utilidades compartidas `WorldgenFeatureSupport`/`StructurePieceSupport`, `2305/2305` EN/ES (`MycelialText`) y `226` eventos / `596` `.ogg` (`ModSounds`).
