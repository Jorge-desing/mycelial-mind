# Colonia Soberana, Memoria Territorial, Dialectos, Expediciones y Ciclos

Subsistemas endgame Post-Matriarca (`colony/`, `entity/ai/colony/`, `PostMatriarchExpedition*`, `HiveCycle*`, `FieldEngramId`).

---

## 1. Colonia Soberana (`colony/`, `entity/ai/colony/`, `SovereignNexus*`)

Desbloqueada tras derrotar a `ApexMatriarchEntity` (o vía `/mycelial colony ...`), anclada en `SovereignNexusBlock` / `SovereignNexusBlockEntity` y persistida en `SovereignColonySavedData` (`19` clases en `colony/`):
- **Tiers y Alcance:** Tiers **1–3** con tope **4/8/12** trabajadores y radio base **24/36/48m** + **+8m** por hasta **4** puestos `symbiotic_node` cercanos (`PurificationZoneTracker.getNearbySymbioticOutposts`).
- **Economía, Cola de Órdenes y Automatización:**
  - Acepta `mycelial_biomass` manual o por tolvas (`Capabilities.ItemHandler.BLOCK`).
  - `ColonyOrderBoard` (`ColonyOrder`) mantiene cola priorizada con deduplicación espacial y reservas exclusivas TTL (`DEFAULT_LEASE_TICKS = 200L`).
  - `SovereignCommandStaffItem` (`ModDataComponents.STAFF_MODE`) marca `INPUT`, `OUTPUT` y zonas tácticas.
- **Entidades `ColonySymbiontEntity`:**
  - **5 castas (`SymbiontCaste`):** `HARVESTER`, `EXCAVATOR`, `COURIER`, `ARCHITECT`, `SENTINEL` (goals dedicados en `entity/ai/colony/`).
  - **5 temperamentos (`SymbiontTemperament`)** y **3 rangos (`SymbiontRank`: `NOVICE`, `ADEPT`, `SOVEREIGN`)**.
  - Necesidades internas (`nutrition`, `fatigue`, `morale`) atendidas por `SymbiontRestAndFeedGoal` en Nexo/puestos y mitigadas por aura de `ReclaimedBloomBlock`.
- **Dominio, Auras e Integración Vanilla+:**
  - XP de Dominio desbloquea **5 rasgos globales (`SovereignTrait`)**; `SovereignColonyDirector` otorga aura cooperativa con `MycelialChimeraEntity` y `PurifierDroneEntity` y dispara `triggerRivalRaid` con amenaza `>= 50%` o Ciclos activos.
  - **Inmunidad y curación:** `DamageTelemetryTracker` cancela fuego amigo sobre `ColonySymbiontEntity` y bloquea infección/conversión; `ColonySymbiontEntity.canBeAffected` bloquea `SPORE_ROT`; `NeutralizingSporeProjectile` (`+4 HP`) y `PurifierGrenadeProjectile` (`+6 HP`) curan y limpian infecciones; `BurrowerEntity` y worldgen protegen `sovereign_nexus` y `reclaimed_bloom`.
  - **Redstone, Escáner, Jade y JEI/EMI:** `MycelialRedstoneTelemetry.sovereignNexusSignal` (`0..15`); `ClientSetup` resalta simbiontes y `CACHED_THERMAL_NEXUSES` bajo `SYNAPTIC_VISION`; `MycelialJadePlugin` (`NexusServerDataProvider`); y `ModDataGenerators` registra las `6` variantes `reclaimed_bloom*` como compostables (`0.65F`).

---

## 2. Memoria Territorial y Reconstrucción (`ColonySiteMemory`, `ColonySiteHabit`, `ColonyReconstruction*`)

- Cada Nexo y puesto `SYMBIOTIC` vinculado conserva observaciones, testigos (`ColonyWitnessMemory`), ensayos (`ColonyReconstruction`, `ColonyReconstructionRules`, `ColonyReconstructionStatus`, `ColonyReconstructionDirector`) y una conducta aprendida propios por sitio (nunca convertidos en regla global ciega del enjambre).
- **Ciclo de aprendizaje por sitio:**
  - `2` incidentes locales abren un ensayo: los simbiontes prueban escolta de mensajeros, descanso preventivo o recolección por turnos (`SymbiontMemoryWitnessGoal`).
  - `2` resultados favorables consolidan el primer hábito (`ColonySiteHabit`); un segundo requiere adopción explícita en la pestaña `Memory` de `SovereignNexusScreen`.
  - Los mensajeros (`COURIER`) pueden portar un hábito aprendido entre sitios (`SovereignColonySavedData` persiste recuerdos y trayectos), donde vuelve a validarse con trabajo local.
- Inspección completa en `SovereignNexusScreen`, `SovereignColonySyncPayload` y `BioScannerItem`.

---

## 3. Dialectos de Colonia (`ColonyDialect`, `ColonyDialectLesson`, `ColonyDialectCallDirector`, `SymbiontDialectLessonGoal`)

- Puestos `SYMBIOTIC` vinculados registran episodios por `ColonySiteHabit` y aprenden llamadas de **2 pulsos tonales (`0..8`)** con **3 eventos `.ogg` propios** (`colony_dialect_tone_{1,2,3}` en `ModSounds`) y barra de acción en emisión, demostración presenciada, consolidación y cancelación.
- `SovereignColonyData.startDialectLesson` acepta cualquier maestro del roster distinto al aprendiz incluso al acuñar (la UI toma por defecto al protagonista del episodio).
- `ColonyDialect.clearCall` / `SovereignColonyData.clearDialectCall` / acción `ACTION_CLEAR_DIALECT_CALL (16)` resuelven colisiones de pulsos desde el botón de conflictos de `SovereignNexusScreen`.
- Telemetría integrada en `BioScannerItem` (`scanner_outpost_dialect`, `scanner_symbiont_dialect_role`), `MycelialJadePlugin` (`symbiont_dialect_observing`, `node_dialect_calls`), `codex_studio` (`sovereign_colony_guide`) y `@GameTest` (`colony_dialect_integration`, `colony_dialect_journey`, `dialect_live_navigation`).

---

## 4. Expediciones de Recuperación Post-Matriarca (`PostMatriarchExpedition*`)

En `deep_mycelium`, `isMatriarchDefeated()` desbloquea también mundos existentes (`PostMatriarchExpeditionData` compartido por dimensión en `SavedData`):
- **Dos rutas de enclave:**
  1. **Ruta defensiva:** `3` pulsos de defensa iniciados con `bio_scanner` sobre el nodo o Pilón elegido (`HiveSquadComposer` + `prepareWaveMob`), con UUID de atacantes persistidos hasta eliminación definitiva (descargar chunks no resuelve el pulso; un atacante promovido a escolta aliada deja de contar sin ser eliminado).
  2. **Jardín simbiótico:** nodo aliado por `SynapticGraftItem` + `3` focos de `ReclaimedBloomBlock` (indestructibles, rebrote `1200t`, emisión de aura en `6m` cada `80t`, visibles bajo `SYNAPTIC_VISION` en `24m`).
- **Invariantes críticas de pulso e interrupción:**
  - El avance se pausa fuera del área y se conserva al guardar; retirar o ahogar el Pilón vinculado llama `onDefensePylonDisabled`, interrumpe solo el pulso en curso (atacantes descargados quedan marcados para retirarse al cargar) y permite re-vincular otro Pilón conservando pulsos defendidos.
  - El vínculo del Pilón no puede cambiar durante un pulso activo; un pulso vacío distingue terreno sin espacio, ausencia de atacables y fallo de creación sin avanzar en ninguno.
  - Al formar escuadrón, cada atacante elige al jugador válido más cercano dentro de `32m` y evalúa diplomacia `canAttack` **después** de aplicar promociones.
  - Si el nodo activo ya no existe, la pestaña `Territory` permite abandonar con confirmación y validación de chunk en servidor.
  - `ClientExpeditionCache` se limpia íntegramente al cerrar sesión cliente (incluyendo directorio, marcadores y destino personal).
  - Mesa sináptica duplica `2` flores del mismo `PALETTE` con `1` flor + `1` biomasa (sin catalizador, compatible con tolvas y JEI/EMI).

---

## 5. Ciclos de la Colmena y Engramas de Campo (`HiveCycle*`, `FieldEngramId`)

- **Engramas de Campo (`FieldEngramId` — `10` IDs + `ModItems.FIELD_ENGRAM`):**
  - Se absorben con clic derecho directo a `PlayerDiscoveryData` (`discoveredEngramsMask`), desbloqueando capítulos `engram:<id>` en el Códice (`12` capítulos / `62` entradas / `249` páginas), o archivan excedentes destilando XP, `SYNAPTIC_INSIGHT` y `-2%` amenaza.
- **Ciclos Post-Matriarca (`HiveCycleMutator`, `HiveCycleEngine`, `HiveCycleSavedData`, `HiveCycleDirector`, `CycleProtocolScreen`):**
  - Tras derrotar a `ApexMatriarchEntity`, interactuar con `MatriarchAltarBlock` sin ofrenda válida (devuelve `FAIL` antes de derrotarla) abre `CycleProtocolScreen` (`CycleProtocolSyncPayload` / `CycleProtocolActionPayload`).
  - Activa **1–3** de los **6 mutadores (`HiveCycleMutator`):** `HYPER_ADAPTATION`, `PERPETUAL_TIDE`, `ACUTE_HOMEOSTASIS`, `SYNAPTIC_SWARM`, `PRIMORDIAL_APEX`, `HYPHAL_OVERLOAD`.
  - Escala jefes (`computeScaledCarapaceIntegrity`), oleadas, expediciones, aprendizaje, homeostasis y presión hifal hasta derrotar de nuevo a la Matriarca, evaluando puntuación, rango `S+..C`, récord histórico y engrama `OVERMIND_CYCLE` (`+50 * ciclo XP`, `+16` biomasa para el Nexo).
