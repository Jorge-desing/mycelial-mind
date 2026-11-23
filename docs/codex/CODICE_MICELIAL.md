# Códice de la Colmena Viva

> **Versión:** `Mycelial Mind v2.0.0` | **Minecraft:** `1.21.1` | **NeoForge:** `21.1.176`  
> **Cobertura Auditada:** `12` capítulos, `61` entradas y `223` páginas de libro validadas al **100%** contra el código fuente.

## Índice General

- **Capítulo 1 — Fundamentos y Día 1** (`fundamentos`): Inicio de partida, Bio-Escáner, 4 efectos y 34 partículas
  - [El Organismo Adaptativo](#cap1-identidad_organismica) — _Filosofía y arquitectura de Mycelial Mind v2.0.0_
  - [Manual de Día 1](#cap1-supervivencia_dia_uno) — _Del primer brote de micelio a la Mesa Sináptica_
  - [Bio-Escáner y Visión Térmica](#cap1-bio_scanner_telemetria) — _Tecla K, 5 pestañas, radar ±128m y códigos RGB_
  - [Efectos de Estado (4)](#cap1-efectos_clinicos) — _Los 4 ModEffects: infección, podredumbre, perspicacia y visión_
  - [Atlas de Partículas (34)](#cap1-atlas_particulas) — _Telemetría visual de los 34 ModParticles, cepas y presupuestos_
- **Capítulo 2 — Progresión, Actos y Oleadas** (`progresion`): 4 Actos, ProgressionGoalEngine, Marea Fúngica y Remisión
  - [Los 4 Actos y Amenaza Global](#cap2-actos_y_amenaza) — _HiveProgressionTier: DORMANT, AWAKENED, ADAPTIVE y OVERMIND_
  - [Motor de Metas y Maestría](#cap2-motor_metas_progresion) — _ShortTermGoal (6), MidTermGoal (5) y pilares a largo plazo_
  - [Oleadas, Marea Fúngica y Remisión](#cap2-oleadas_marea_remision) — _Hitos 25/50/75/100%, Marea >=75% y Remisión de 20 min_
- **Capítulo 3 — Memoria Inmune y Rasgos** (`memoria_colmena`): Router 40/60, 5 Rasgos, 5 Sinergias, 4 Hábitos y Engaño
  - [Memoria Inmune y Enrutador](#cap3-enrutamiento_memoria) — _40% directo / 60% búfer nodal (48m) con consolidación en 30s_
  - [Estímulos (5) y Rasgos (5)](#cap3-estimulos_y_rasgos) — _StimulusType -> AdaptationTrait Nivel 1 (70) y Nivel 2 (200)_
  - [Las 5 Sinergias (`HiveSynergy`)](#cap3-sinergias_evolutivas) — _Combinaciones compuestas al coincidir pares de rasgos_
  - [Los 4 Hábitos del Jugador](#cap3-habitos_telemetria) — _HabitTelemetryTracker: contra-adaptaciones desde 60.0_
  - [Engaño Pavloviano y Contramedidas](#cap3-engano_pavloviano) — _FalseEngramInjector, CounterSerum y SynapticSuppressor_
- **Capítulo 4 — IA Táctica y Campo Vivo** (`ia_tactica`): SmartBrainLib, gdx-AI, Escuadrones, Trauma, Carroña y Goals
  - [Arquitectura Híbrida SBL + gdx-AI](#cap4-arquitectura_sbl_gdxai) — _MycelialSblBridge, 11 ModMemoryTypes y HiveTacticalTree_
  - [Doctrinas, Escuadrones y Pacing](#cap4-doctrina_escuadron_pacing) — _HiveDoctrine, HiveOrder, SwarmRole, Frenesí y Desync (5s)_
  - [IA de Campo Vivo y Goals](#cap4-campo_batalla_vivo) — _Trauma, Carroña, Quitina Efímera, Atascos y Goals personalizados_
- **Capítulo 5 — Bestiario y Quimeras** (`bestiario`): 17 especies vivas, 3 proyectiles, 17 huevos y genes 5x5
  - [Rangos Evolutivos y Primordiales](#cap5-rangos_y_primordiales) — _BASE, MUTATED, ELITE, Primordiales y proyección vocal_
  - [Enjambre Ligero y Parasitario](#cap5-bestiario_enjambre_ligero) — _sporeling, leech_parasite y mycelial_stalker_
  - [Artillería Ácida y Pirófitos](#cap5-bestiario_artilleria_fuego) — _acid_spitter y pyro_shroom_
  - [Zapadores, Tejedores y Arquitectos](#cap5-bestiario_ingenieros_soporte) — _burrower, synapse_weaver y spore_architect_
  - [Bruto, Góliat y Mímico Sináptico](#cap5-bestiario_pesados_mimico) — _chitin_brute, goliath_carrier y synaptic_mimic_
  - [Guardián y Behemot de Esporas](#cap5-bestiario_elite_campeon) — _colony_warden (110 HP) y spore_behemoth (200 HP)_
  - [Jefe Overworld: Núcleo Colmena](#cap5-bestiario_jefe_hive_core) — _hive_core (350 HP) — 3 Fases y 3 Anclas Centinela_
  - [Jefe Final: Matriarca Suprema](#cap5-bestiario_jefe_matriarca) — _apex_matriarch (600 HP) en el Micelio Profundo_
  - [Aliados Bioforjados: Dron y Quimera](#cap5-bestiario_aliados_quimera) — _purifier_drone (60 HP) y mycelial_chimera (matriz genética 5x5)_
  - [Proyectiles (3) y Spawn Eggs (17)](#cap5-proyectiles_y_huevos) — _acid_spit, neutralizing_spore, purifier_grenade y los 17 huevos_
- **Capítulo 6 — Red Sináptica y Nodos** (`red_sinaptica`): 4 tipos de nodo, Injerto, Viaje Micorrizal 96m y Acústica
  - [Los 4 Nodos Sinápticos](#cap6-nodos_y_malla_sinaptica) — _SynapticNodeType: RELAY, INCUBATOR, SENTINEL y SYMBIOTIC_
  - [Injerto y Viaje Micorrizal](#cap6-injerto_y_viaje_micorrizal) — _Reprogramación nodal y salto rápido a 96m con Quimeras y Drones_
  - [Acústica Micorrizal y Señuelo Eco](#cap6-acustica_micorrizal_senuelo) — _MycorrhizalAcousticTracker (32m/18m) y neural_echo_decoy (16m/20m)_
- **Capítulo 7 — Biotecnología y Diplomacia** (`biotecnologia`): Mesa Sináptica N1–N3, 5 Mutágenos, Cismas y Diplomacia Química
  - [Mesa Sináptica y Niveles Lab 1–3](#cap7-mesa_investigacion_sinaptica) — _SynapticBenchBlockEntity, ranuras, tolvas y 4 síntesis base_
  - [Simbiosis y los 5 Mutágenos](#cap7-simbiosis_mutagenos) — _SymbioticMutagen: injertos corporales, límites y riesgos_
  - [Cismas y Diplomacia Química](#cap7-cisma_y_diplomacia_quimica) — _Cepas ORTHODOX/AMBER/CRIMSON, Pactos, Truce y Quorum_
- **Capítulo 8 — Arsenal, Quitina y Bastión** (`arsenal_defensa`): Quitina evolutiva, armas, 4 Reliquias, Pilones y Torretas
  - [Armadura de Quitina Evolutiva](#cap8-armadura_quitina_reactiva) — _Material chitin, DataComponents (armor_rank, trait_id) y evolución_
  - [Arsenal de Combate y Campo](#cap8-arsenal_tactico_completo) — _Espadas, lanzas, lanzadores, granadas y herramientas de campo_
  - [Las 4 Reliquias Endgame](#cap8-reliquias_endgame) — _TideBeacon, SynapticSuppressor, RiftCatalyst y OvermindCrown_
  - [Defensa del Bastión y Torretas](#cap8-bastion_defensivo) — _Pilón (monument_focus), Torreta (3 tiers), Dron y Bio-Barrera_
- **Capítulo 9 — Economía, Recetas y Botín** (`economia_datos`): 24 Bloques, 53 Ítems, 37 Recetas, Brews, Compost y 49 Botines
  - [Catálogo: 24 Bloques y 53 Ítems](#cap9-catalogo_bloques_items) — _Inventario íntegro de flora, bloques e ítems registrados_
  - [Recetario Total (37 Datapack)](#cap9-recetas_36_datapack) — _29 Mesa, 3 Horno, 1 Ahumador, 1 Alto Horno y 3 Cortapiedras_
  - [Alambique, Compostaje y Comercio](#cap9-vanilla_brew_compost_trades) — _Poción Awkward, 5 compostables NeoForge y ofertas Clérigo/Albañil_
  - [Las 50 Tablas de Botín](#cap9-botin_48_tablas) — _22 bloques, 10 cofres, 17 entidades y aldea + multiplicadores_
- **Capítulo 10 — Mundo, Estructuras y Dimensión** (`mundo_dimension`): Overworld, Micelio Profundo (3 biomas), Santuario, Ciudadela y Homeostasis
  - [Brotes y Colmenas del Overworld](#cap10-generacion_overworld_ruido) — _MycelialOutbreak (56m), SurfaceHive (192m) y FastNoiseLite_
  - [Dimensión Micelio Profundo (3 Biomas)](#cap10-micelio_profundo_y_biomas) — _Portal mycelial_rift, WfcCavernLayout y los 3 biomas_
  - [Santuario Jigsaw y Ciudadela Matriarca](#cap10-megaestructuras_santuario_ciudadela) — _hive_sanctum (11 plantillas NBT) y matriarch_citadel_
  - [Homeostasis Dimensional y Quistes](#cap10-homeostasis_dimensional) — _3 etapas STABLE/IRRITATED/IMMUNE_RESPONSE, Lluvia Ácida y Quistes_
  - [Protocolo de Ciclos y Mutadores](#cap10-ciclos_de_la_colmena) — _Rejugabilidad post-Matriarca con 1–3 mutadores y rango S+..C_
- **Capítulo 11 — Secretos, Logros y Compat** (`secretos_logros`): 8 Secretos (Pistas + Spoilers), 30 Logros, 7 Redstone y 9 Compat
  - [Los 8 Secretos: Pistas de Campo](#cap11-secretos_pistas_seguras) — _Indicios de investigación experimental y ecológica sin spoilers_
  - [Los 8 Secretos: Revelación Exacta](#cap11-secretos_revelaciones_spoiler) — _[SPOILER] Fórmulas y disparadores exactos en código_
  - [Los 30 Logros (`advancement`)](#cap11-logros_30_advancements) — _25 logros de campaña y 5 desbloqueos automáticos de recetas_
  - [Redstone (7) y Ecosistema de Mods](#cap11-redstone_y_ecosistema_mods) — _MycelialRedstoneTelemetry + SBL, gdx-AI, Jade, JEI, EMI, Iris, Modonomicon_
  - [Engrama I: Señal de Brote](#cap11-engram_outbreak_signal) — _Registro inicial del elemento field_engram (engram_id)_
  - [Engrama II: Floración Sináptica](#cap11-engram_synaptic_bloom) — _Topología temprana de nodos y pilones_
  - [Engrama III: Resonancia Quitina](#cap11-engram_chitin_resonance) — _Eco estructural de hive_core_
  - [Engrama IV: Presión de Marea](#cap11-engram_tide_pressure) — _Fractura multipartita de spore_behemoth_
  - [Engrama V: Engaño del Mímico](#cap11-engram_mimic_deception) — _Mimetismo adaptativo de synaptic_mimic_
  - [Engrama VI: Memoria Hifal](#cap11-engram_hyphal_memory) — _Rutas micorrízicas y engramas de santuario_
  - [Engrama VII: Trauma Inmune](#cap11-engram_homeostasis_trauma) — _Sensibilización y quistes de deep_mycelium_
  - [Engrama VIII: Linaje Primordial](#cap11-engram_primordial_lineage) — _Extracción genética de variantes primordiales_
  - [Engrama IX: Réquiem Matriarcal](#cap11-engram_matriarch_requiem) — _Remisión dimensional y protocolo de enclave_
  - [Engrama X: Ciclo de la Supramente](#cap11-engram_overmind_cycle) — _Maestría post-Matriarca con mutadores activos_
- **Capítulo 12 — Técnica, Config y Comandos** (`tecnica_comandos`): 181 Sonidos, LabPBR, 49 Configs, Red v14, /mycelial y 22 Verify
  - [Configuraciones (13 Cliente / 43 Servidor)](#cap12-configuracion_cliente_servidor) — _MycelialClientConfig, KeyBindings y MycelialConfig TOML completo_
  - [Red v21, `/mycelial` y Verify](#cap12-comandos_red_y_verificacion) — _Payloads v21, /mycelial cycle, field_engram y verify cycles_

---

## Capítulo 1: Fundamentos y Día 1

_Inicio de partida, Bio-Escáner, 4 efectos y 34 partículas_ | Puerta recomendada: `always` | Ícono: `mycelialmind:bio_scanner`

<a id="cap1-identidad_organismica"></a>
### 1. El Organismo Adaptativo

- **ID de Entrada:** `fundamentos/identidad_organismica` | **Ícono:** `mycelialmind:hive_heart` | **Unlock:** `always`
- **Resumen:** Filosofía y arquitectura de Mycelial Mind v2.0.0

#### [Libro: Cap.1 / identidad_organismica / Pág.1] Una Mente Única y Evolutiva

**Mycelial Mind v2.0.0** (MC **1.21.1**, NeoForge **21.1.176**, Java **21**) sustituye la dificultad estática por una colmena con memoria persistente (`HiveMindSavedData`).
Cada daño (`StimulusType`) y hábito (`PlayerHabit`) recalibran sus rasgos (`AdaptationTrait`), sinergias, oleadas y red subterránea.

#### [Libro: Cap.1 / identidad_organismica / Pág.2] Regla de Oro: Varía tu Arsenal

Repetir un mismo tipo de ataque dispara el umbral adaptativo (**70.0** en Nivel 1 y **200.0** efectivo en Nivel 2) y blinda al enjambre.
Alterna armas melee, proyectiles, fuego, explosivos y magia; purga Nodos y sintetiza contramedidas en la Mesa Sináptica para drenar su memoria.

<a id="cap1-supervivencia_dia_uno"></a>
### 2. Manual de Día 1

- **ID de Entrada:** `fundamentos/supervivencia_dia_uno` | **Ícono:** `mycelialmind:bio_extractor` | **Unlock:** `always`
- **Resumen:** Del primer brote de micelio a la Mesa Sináptica

#### [Libro: Cap.1 / supervivencia_dia_uno / Pág.1] 1. Detección y Muestreo

> **[Vista Ítem]** `mycelialmind:creeping_mycelium`

Localiza `creeping_mycelium` y brotes.
Forja `bio_scanner` y `bio_extractor` para extraer `genetic_sample` codificadas.

#### [Libro: Cap.1 / supervivencia_dia_uno / Pág.2] 2. Profilaxis y Contención

> **[Vista Ítem]** `mycelialmind:spore_antidote`

Prepara `spore_antidote` y rompe `mycelial_node`.
Al **25%** Amenaza, erige `purification_pylon` y `synaptic_research_bench`.

<a id="cap1-bio_scanner_telemetria"></a>
### 3. Bio-Escáner y Visión Térmica

- **ID de Entrada:** `fundamentos/bio_scanner_telemetria` | **Ícono:** `mycelialmind:bio_scanner` | **Unlock:** `always`
- **Resumen:** Tecla K, 5 pestañas, radar ±128m y códigos RGB

#### [Libro: Cap.1 / bio_scanner_telemetria / Pág.1] Uso Activo y Códice (K)

> **[Vista Ítem]** `mycelialmind:bio_scanner`

Clic Derecho escanea mobs, nodos y aliados.
Pulsa **K** (`open_codex`) para abrir las **5 pestañas** y radar `±128m`.

#### [Libro: Cap.1 / bio_scanner_telemetria / Pág.2] Visión Sináptica Térmica (1/3)

• **Pestañas HUD (`BioScannerCodexScreen`):** Telemetría, Evolución, Territorio (`±128m`), Diplomacia y Guía; atajo opcional `toggle_hud_compact` para modo compacto.
• **Activación:** `Shift + Clic Derecho` aplica `synaptic_vision`; detecta mobs a **32m** y bloques clave a **36m** tras muros.

#### [Libro: Cap.1 / bio_scanner_telemetria / Pág.3] Visión Sináptica Térmica (2/3)

• **Entidades (32m):** Primordial = Lima-oro pulsante; Líder = Dorado; Hostil = Rojo-naranja; Dron/Quimera/Jugador = Menta-cian.
• **Nodos y Defensas (36m):** Nodo Simbiótico = Verde; Nodo Hostil = Púrpura; Pilón/Torreta activa = Cian; Torreta atascada = Naranja.

#### [Libro: Cap.1 / bio_scanner_telemetria / Pág.4] Visión Sináptica Térmica (3/3)

• **Amenazas Ocultas (36m):** Pilón Sofocado = Rojo; Quiste (`antibody_cyst`) = Ámbar; Bóveda sellada sin abrir = Oro pulsante.

<a id="cap1-efectos_clinicos"></a>
### 4. Efectos de Estado (4)

- **ID de Entrada:** `fundamentos/efectos_clinicos` | **Ícono:** `mycelialmind:concentrated_antidote` | **Unlock:** `always`
- **Resumen:** Los 4 ModEffects: infección, podredumbre, perspicacia y visión

#### [Libro: Cap.1 / efectos_clinicos / Pág.1] Efectos Clínicos (ModEffects) (1/4)

• `mycelial_infection` (#7B2CBF, Perjudicial): aumenta el daño recibido de `MycelialMob` en **+15%** por nivel (`1.0 + 0.15*(amp+1)`), bloquea Viaje Micorrizal y convierte mobs/aldeanos cercanos; cada pieza de Quitina reduce su duración un **20%**.

#### [Libro: Cap.1 / efectos_clinicos / Pág.2] Efectos Clínicos (ModEffects) (2/4)

• `spore_rot` (#9B2226, Perjudicial): contra-adaptación del hábito `COMBAT_MEDIC` y contacto con `antibody_cyst`; reduce **toda curación en -30%** (`* 0.70F`). Purga con `concentrated_antidote`.

#### [Libro: Cap.1 / efectos_clinicos / Pág.3] Efectos Clínicos (ModEffects) (3/4)

• `synaptic_insight` (#38B000, Beneficioso): lectura táctica obtenida al absorber `volatile_engram`, purgar Nodos o desplegar `neural_echo_decoy`; concede **+15%** de daño adicional contra unidades de la Colmena.

#### [Libro: Cap.1 / efectos_clinicos / Pág.4] Efectos Clínicos (ModEffects) (4/4)

• `synaptic_vision` (#00F5D4, Beneficioso): visión térmica por paredes (**32m** entidades, **36m** nodos/pilones/torretas/quistes/bóvedas) con borde HUD cian.

<a id="cap1-atlas_particulas"></a>
### 5. Atlas de Partículas (34)

- **ID de Entrada:** `fundamentos/atlas_particulas` | **Ícono:** `mycelialmind:volatile_engram` | **Unlock:** `always`
- **Resumen:** Telemetría visual de los 34 ModParticles, cepas y presupuestos

#### [Libro: Cap.1 / atlas_particulas / Pág.1] Partículas Personalizadas (34) (1/6)

• **Estados clínicos:** `infection` (racimo violeta con cilios), `rot` (gota necrótica umber), `insight` (cristal esmeralda), `vision` (iris térmico), `recovery` (arcos verde-lima), `purge` (cruz clínica) y `scan` (chevrón de barrido).

#### [Libro: Cap.1 / atlas_particulas / Pág.2] Partículas Personalizadas (34) (2/6)

• **Sueros y telemetría:** `serum_melee`, `serum_projectile`, `serum_fire`, `serum_explosion` y `serum_magic` (tajo, dardo, ascua, onda y glifo); `engram` (hexágono ámbar), `impact` (estrella menta) y `trail` (espiral).

#### [Libro: Cap.1 / atlas_particulas / Pág.3] Partículas Personalizadas (34) (3/6)

• **Colmena y mundo:** `spore_mote`, `spore_cloud` (bruma 64px), `spore_fall`, `bio_lume`, `amber_glint`, `chitin_shard`, `acid_droplet`, `acid_splash` y `necrotic_mist`.

#### [Libro: Cap.1 / atlas_particulas / Pág.4] Partículas Personalizadas (34) (4/6)

• **Red neural:** `synapse_spark`, `hive_pulse` (anillo rúnico), `psychic_wave`, `tide_mote`, `hive_ember`, `purifier_cross`, `thermal_reticle` (retícula 64px), `histamine_pulse`, `hive_dust` (polvo tintable) y `desync_fragment`.

#### [Libro: Cap.1 / atlas_particulas / Pág.5] Partículas Personalizadas (34) (5/6)

• **Presupuesto y escala:** máximo 24 fragmentos por emisión y 48 por escena (64 en jefes); tamaño 100 %→80 %, reducción monótona cerca de cámara y nada de crecimiento posterior.

#### [Libro: Cap.1 / atlas_particulas / Pág.6] Partículas Personalizadas (34) (6/6)

• **Render y cepas:** glow aditivo propio (desactivable en `[particles]`) y `hive_dust` tintado por cepa (`#9B4DFF`, `#F2A93B`, `#E03C5C`).

---

## Capítulo 2: Progresión, Actos y Oleadas

_4 Actos, ProgressionGoalEngine, Marea Fúngica y Remisión_ | Puerta recomendada: `always` | Ícono: `mycelialmind:synaptic_core`

<a id="cap2-actos_y_amenaza"></a>
### 1. Los 4 Actos y Amenaza Global

- **ID de Entrada:** `progresion/actos_y_amenaza` | **Ícono:** `mycelialmind:synaptic_core` | **Unlock:** `always`
- **Resumen:** HiveProgressionTier: DORMANT, AWAKENED, ADAPTIVE y OVERMIND

#### [Libro: Cap.2 / actos_y_amenaza / Pág.1] Escalado por Acto (0–100%) (1/3)

• **Acto I `DORMANT` (0–25%):** HP **x1.00**, Daño **x1.00**, **0.0 XP**/int., Vel. **x1.00**, Mutación **0%**, Decay Bioma **0.85**.
• **Acto II `AWAKENED` (25–50%):** HP **x1.15**, Daño **x1.10**, **1.0 XP**/int., Vel. **x1.03**, Mutación **5%**, Decay **1.00**. Activa asedios y síntesis N2.

#### [Libro: Cap.2 / actos_y_amenaza / Pág.2] Escalado por Acto (0–100%) (2/3)

• **Acto III `ADAPTIVE` (50–75%):** HP **x1.30**, Daño **x1.22**, **2.5 XP**/int., Vel. **x1.06**, Mutación **12%**, Decay **1.15**. Desde **50.0%** (`SURFACE_DAY_SPAWN_THREAT_THRESHOLD`) brotan de día en superficie.

#### [Libro: Cap.2 / actos_y_amenaza / Pág.3] Escalado por Acto (0–100%) (3/3)

• **Acto IV `OVERMIND` (>=75%):** HP **x1.50**, Daño **x1.35**, **4.0 XP**/int., Vel. **x1.10**, Mutación **20%**, Decay **1.30**. Escala con `playerCountScaling` (**+0.15**/jugador), `deathThreatPenalty` (**+5.0%**) y se sincroniza al Micelio Profundo.

<a id="cap2-motor_metas_progresion"></a>
### 2. Motor de Metas y Maestría

- **ID de Entrada:** `progresion/motor_metas_progresion` | **Ícono:** `mycelialmind:overmind_essence` | **Unlock:** `always`
- **Resumen:** ShortTermGoal (6), MidTermGoal (5) y pilares a largo plazo

#### [Libro: Cap.2 / motor_metas_progresion / Pág.1] Metas de Progresión (1/3)

• **6 Metas a Corto Plazo (`ShortTermGoal`):** `CRAFT_SCANNER`, `BUILD_RESEARCH_BENCH`, `EXTRACT_GENETIC_SAMPLE`, `SYNTHESIZE_BIOTECH`, `UPGRADE_GEAR_OR_STATION`, `MASTER_EXPERIMENTS`.

#### [Libro: Cap.2 / motor_metas_progresion / Pág.2] Metas de Progresión (2/3)

• **5 Metas a Medio Plazo (`MidTermGoal`):** `ESTABLISH_PERIMETER` (Pilón), `CONQUER_HIVE_SANCTUM` (Hive Heart), `BREACH_DEEP_MYCELIUM` (Grieta), `OVERCOME_TIDE_AND_MATRIARCH`, `MAINTAIN_DOMINION`.

#### [Libro: Cap.2 / motor_metas_progresion / Pág.3] Metas de Progresión (3/3)

• **7 Hitos (`MILESTONE_*`):** Bio-Escáner, Mesa Sináptica, Bio-Extractor, Síntesis Biotech, Mejora Estación, Quitina Evolucionada y Núcleo Derrotado.
• **Maestría Larga:** **17/17** especies, **5/5** rasgos, **8/8** secretos, **3/3** cepas, Primordiales y **BastionScore 100/100**.

<a id="cap2-oleadas_marea_remision"></a>
### 3. Oleadas, Marea Fúngica y Remisión

- **ID de Entrada:** `progresion/oleadas_marea_remision` | **Ícono:** `mycelialmind:tide_beacon` | **Unlock:** `act_2`
- **Resumen:** Hitos 25/50/75/100%, Marea >=75% y Remisión de 20 min

#### [Libro: Cap.2 / oleadas_marea_remision / Pág.1] Oleadas, Marea y Remisión (1/3)

• **Oleadas (`HiveWaveDirector`):** evalúa cada **100 ticks** los hitos **25%, 50%, 75% y 100%** (enfriamiento `waveCooldownTicks = 3600t` = 3 min). Las zonas bajo `PurificationZoneTracker` desvían el epicentro del asedio.

#### [Libro: Cap.2 / oleadas_marea_remision / Pág.2] Oleadas, Marea y Remisión (2/3)

• **Marea Fúngica:** estalla al alcanzar **>=75%** Amenaza (`fungalTideDurationTicks = 6000t` = 5 min; cooldown `24000t` = 20 min). Invoca un `spore_behemoth` cuyo botín da `tide_beacon`; suena `fungal_tide_horn`.

#### [Libro: Cap.2 / oleadas_marea_remision / Pág.3] Oleadas, Marea y Remisión (3/3)

• **Remisión de Matriarca:** derrotar a `apex_matriarch` abre **20 minutos** (`24000t`) de remisión global: atenúa la presión de Amenaza, calma nodos y reduce el estrés homeostático tras conquistar el Acto IV.

---

## Capítulo 3: Memoria Inmune y Rasgos

_Router 40/60, 5 Rasgos, 5 Sinergias, 4 Hábitos y Engaño_ | Puerta recomendada: `always` | Ícono: `mycelialmind:false_engram_injector`

<a id="cap3-enrutamiento_memoria"></a>
### 1. Memoria Inmune y Enrutador

- **ID de Entrada:** `memoria_colmena/enrutamiento_memoria` | **Ícono:** `mycelialmind:volatile_engram` | **Unlock:** `always`
- **Resumen:** 40% directo / 60% búfer nodal (48m) con consolidación en 30s

#### [Libro: Cap.3 / enrutamiento_memoria / Pág.1] Enrutamiento Sináptico (1/2)

• **División 40/60 (`SynapticMemoryRouter`):** si existe un Nodo hostil a **<=48m** (`BUFFER_NODE_RANGE`), solo el **40%** del estímulo sube directo a la Colmena y el **60%** queda en búfer volátil del nodo.

#### [Libro: Cap.3 / enrutamiento_memoria / Pág.2] Enrutamiento Sináptico (2/2)

• **Consolidación vs Purga:** tras **600 ticks** (**30s**) el búfer se consolida en la red. Romper o purgar el Nodo antes disipa el búfer sin consolidar, dropea `volatile_engram` y sangra `nodeDestructionStimulusBleed` (**18.0**).

<a id="cap3-estimulos_y_rasgos"></a>
### 2. Estímulos (5) y Rasgos (5)

- **ID de Entrada:** `memoria_colmena/estimulos_y_rasgos` | **Ícono:** `mycelialmind:genetic_sample` | **Unlock:** `always`
- **Resumen:** StimulusType -> AdaptationTrait Nivel 1 (70) y Nivel 2 (200)

#### [Libro: Cap.3 / estimulos_y_rasgos / Pág.1] 5 Estímulos y 5 Rasgos (1/5)

• **MELEE -> `MELEE_THORNS`** (N1 >= **70**, N2 `flanking_swarm` >= **200**): refleja castigo cuerpo a cuerpo y coordina flanqueos en masa.

#### [Libro: Cap.3 / estimulos_y_rasgos / Pág.2] 5 Estímulos y 5 Rasgos (2/5)

• **PROJECTILE -> `PROJECTILE_DEFLECT`** (N2 `evasive_strafe`): prob. de desvío (`projectileDeflectChance = 0.30`) y maniobras laterales evasivas.

#### [Libro: Cap.3 / estimulos_y_rasgos / Pág.3] 5 Estímulos y 5 Rasgos (3/5)

• **FIRE -> `FIRE_RESISTANT`** (N2 `pyro_surge`): inmunidad/mitigación térmica y asaltos agresivos a través de fuego.

#### [Libro: Cap.3 / estimulos_y_rasgos / Pág.4] 5 Estímulos y 5 Rasgos (4/5)

• **EXPLOSION -> `BLAST_SHIELDED`** (N2 `phalanx_breach`): eleva armadura base mínima a **8.0** y acelera la brecha de muros (`Burrower` cada **18t**).

#### [Libro: Cap.3 / estimulos_y_rasgos / Pág.5] 5 Estímulos y 5 Rasgos (5/5)

• **MAGIC -> `MAGIC_CARAPACE`** (N2 `psychic_disrupt`): amortigua daño arcano/veneno y disrumpe al conjurador. Decaimiento: **2.0/min** (x**1.75** en N2).

<a id="cap3-sinergias_evolutivas"></a>
### 3. Las 5 Sinergias (`HiveSynergy`)

- **ID de Entrada:** `memoria_colmena/sinergias_evolutivas` | **Ícono:** `mycelialmind:hive_altar` | **Unlock:** `act_2`
- **Resumen:** Combinaciones compuestas al coincidir pares de rasgos

#### [Libro: Cap.3 / sinergias_evolutivas / Pág.1] Sinergias Compuestas (5) (1/5)

• `PYROLYTIC_SHELL` (`FIRE_RESISTANT` + `BLAST_SHIELDED`): caparazón pirolítico que emite pulsos ígneos reactivos al recibir presión sostenida.

#### [Libro: Cap.3 / sinergias_evolutivas / Pág.2] Sinergias Compuestas (5) (2/5)

• `VENOMOUS_COUNTER` (`MELEE_THORNS` + `MAGIC_CARAPACE`): reparte réplicas tóxicas e infecciosas ante castigo cuerpo a cuerpo o arcano.

#### [Libro: Cap.3 / sinergias_evolutivas / Pág.3] Sinergias Compuestas (5) (3/5)

• `KINETIC_MIRROR` (`PROJECTILE_DEFLECT` + `MELEE_THORNS`): dispara fragmentos de ácido (`AcidSpitProjectile`) hacia el atacante tras recibir impacto.

#### [Libro: Cap.3 / sinergias_evolutivas / Pág.4] Sinergias Compuestas (5) (4/5)

• `ARCANE_ABSORPTION` (`MAGIC_CARAPACE` + `FIRE_RESISTANT`): mitiga conjuros, pociones y calor elemental mientras sostiene curación en combate.

#### [Libro: Cap.3 / sinergias_evolutivas / Pág.5] Sinergias Compuestas (5) (5/5)

• `SIEGE_JUGGERNAUT` (`BLAST_SHIELDED` + `PROJECTILE_DEFLECT`): garantiza **>=0.75** resistencia al empuje y potencia ruptura de fortificaciones (`18t`).

<a id="cap3-habitos_telemetria"></a>
### 4. Los 4 Hábitos del Jugador

- **ID de Entrada:** `memoria_colmena/habitos_telemetria` | **Ícono:** `mycelialmind:antibody_cyst` | **Unlock:** `act_2`
- **Resumen:** HabitTelemetryTracker: contra-adaptaciones desde 60.0

#### [Libro: Cap.3 / habitos_telemetria / Pág.1] Hábitos Tácticos (>=60 / 500) (1/3)

• `COMBAT_MEDIC`: curarte >=1.5 HP en combate (<16m) acumula hábito; a >=**60.0** aplica `spore_rot` reduciendo curaciones un **-30%** (`* 0.70F`).
• `HIGH_MOBILITY`: kiting/sprint constante provoca emboscadas rápidas y ajuste vectorial de flanco (`Stalker`, `SwarmFlankGoal`).

#### [Libro: Cap.3 / habitos_telemetria / Pág.2] Hábitos Tácticos (>=60 / 500) (2/3)

• `BASTION_BUILDER`: levantar muros, torretas o mejoras de laboratorio despierta prioridad zapadora (`Burrower`, `SporeArchitect`, carga de asedio).

#### [Libro: Cap.3 / habitos_telemetria / Pág.3] Hábitos Tácticos (>=60 / 500) (3/3)

• `SHADOW_INFILTRATOR`: abuso de invisibilidad/agacharse activa detección sináptica de escuadrón. Rota tu estilo para enfriar cada marcador.

<a id="cap3-engano_pavloviano"></a>
### 5. Engaño Pavloviano y Contramedidas

- **ID de Entrada:** `memoria_colmena/engano_pavloviano` | **Ícono:** `mycelialmind:false_engram_injector` | **Unlock:** `act_2`
- **Resumen:** FalseEngramInjector, CounterSerum y SynapticSuppressor

#### [Libro: Cap.3 / engano_pavloviano / Pág.1] Contra-Ingeniería Cognitiva (1/2)

• `false_engram_injector`: inyecta **+150.0** de estímulo falso durante **90s** (`1800t`), forzando sobre-adaptación a un elemento para infligir **+25% de daño** con los tipos opuestos (cooldown **60s** si se descubre / **20s** natural).

#### [Libro: Cap.3 / engano_pavloviano / Pág.2] Contra-Ingeniería Cognitiva (2/2)

• `counter_serum`: drena el estímulo codificado según `counterSerumBleedRatio` (**50%** por defecto) para desmontar rasgos activos.
• `synaptic_suppressor`: silencia globalmente el aprendizaje (`suppressorCooldownSeconds = 120s`).

---

## Capítulo 4: IA Táctica y Campo Vivo

_SmartBrainLib, gdx-AI, Escuadrones, Trauma, Carroña y Goals_ | Puerta recomendada: `always` | Ícono: `mycelialmind:neural_echo_core`

<a id="cap4-arquitectura_sbl_gdxai"></a>
### 1. Arquitectura Híbrida SBL + gdx-AI

- **ID de Entrada:** `ia_tactica/arquitectura_sbl_gdxai` | **Ícono:** `mycelialmind:neural_echo_core` | **Unlock:** `always`
- **Resumen:** MycelialSblBridge, 11 ModMemoryTypes y HiveTacticalTree

#### [Libro: Cap.4 / arquitectura_sbl_gdxai / Pág.1] SmartBrainLib, Memorias y gdx-AI (1/3)

• **Capa SBL (`MycelialSblBridge`):** sensor `HiveMindSensor` + 5 conductas (`HiveNodeRegenBehaviour`, `HiveHotspotPatrolBehaviour`, `AcidSpitterCombatBehaviour`, `StalkerFlankBehaviour`, `WeaverSupportBehaviour`).

#### [Libro: Cap.4 / arquitectura_sbl_gdxai / Pág.2] SmartBrainLib, Memorias y gdx-AI (2/3)

• **11 `ModMemoryTypes`:** `shared_target_pos`, `hive_alert_level`, `nearest_node_pos`, `stalker_flank_pos`, `weaver_support_pos`, `nearest_pylon_pos`, `hive_order`, `mob_combat_xp`, `last_attack_stimulus`, `nearby_ally_count`, `hive_mind_sensor`.

#### [Libro: Cap.4 / arquitectura_sbl_gdxai / Pág.3] SmartBrainLib, Memorias y gdx-AI (3/3)

• **Capa gdx-AI (`HiveTacticalTree`):** Árbol de Comportamiento + evaluador de utilidad (`HiveUtilityEvaluator`) + pizarra táctica (`HiveAiBlackboard`, `TacticalIntent`) y campos orgánicos `FastNoiseLite`.

<a id="cap4-doctrina_escuadron_pacing"></a>
### 2. Doctrinas, Escuadrones y Pacing

- **ID de Entrada:** `ia_tactica/doctrina_escuadron_pacing` | **Ícono:** `mycelialmind:purifier_grenade` | **Unlock:** `always`
- **Resumen:** HiveDoctrine, HiveOrder, SwarmRole, Frenesí y Desync (5s)

#### [Libro: Cap.4 / doctrina_escuadron_pacing / Pág.1] Mando Táctico e Influencia (1/4)

• **5 Doctrinas (`HiveDoctrine`) y 4 Órdenes (`HiveOrder`):** `DORMANT`->`NONE`, `FORAGING`->`FLANK_PERIMETER`, `ADAPTIVE_HUNT`->`FLANK_PERIMETER`, `SIEGE_ASSAULT`->`PRESSURE_TARGET`, `RECOVERY`->`RALLY_NODE`.

#### [Libro: Cap.4 / doctrina_escuadron_pacing / Pág.2] Mando Táctico e Influencia (2/4)

• **Escuadrón (`SwarmRole` / `CommandRole`):** roles `FRONT_ANCHOR`, `FLANK_LEFT`, `FLANK_RIGHT`, `SUPPORT_REAR` con `LEADER` (`ALERT_CALL_RADIUS = 20m`). Activa balística predictiva y Frenesí por bajas.

#### [Libro: Cap.4 / doctrina_escuadron_pacing / Pág.3] Mando Táctico e Influencia (3/4)

• **Contramedida `Desync` (5s):** abate al `LEADER` o impacta con `purifier_grenade` para romper el enlace (`SquadTacticalStatus`), anular el plomo balístico e inducir `10.0` de imprecisión.

#### [Libro: Cap.4 / doctrina_escuadron_pacing / Pág.4] Mando Táctico e Influencia (4/4)

• **Pacing y Mapa de Calor:** `HiveBlackboard` (`PacingPhase`) alterna presión/tregua; `HiveInfluenceMap` guarda hasta **512** celdas (difusión **0.35**, poda **0.01**) y `HiveHotspot` máx. **24** focos.

<a id="cap4-campo_batalla_vivo"></a>
### 3. IA de Campo Vivo y Goals

- **ID de Entrada:** `ia_tactica/campo_batalla_vivo` | **Ícono:** `mycelialmind:ephemeral_chitin` | **Unlock:** `always`
- **Resumen:** Trauma, Carroña, Quitina Efímera, Atascos y Goals personalizados

#### [Libro: Cap.4 / campo_batalla_vivo / Pág.1] Mecánicas de Campo Vivo (1/4)

• **Trauma y Zapadores (`LivingBattlefieldSystem`):** si una celda acumula trauma `>=0.55`, la infantería la rodea; solo Zapadores (`Burrower`, `ColonyWarden`, `ELITE` o `BLAST_SHIELDED`) la cruzan directo.

#### [Libro: Cap.4 / campo_batalla_vivo / Pág.2] Mecánicas de Campo Vivo (2/4)

• **Terraforming y Anti-Pilar (`ephemeral_chitin`):** si el jugador se eleva **>=2 bloques** o dispara a distancia, levantan rampas y barricadas temporales de `ephemeral_chitin` que decaen en **240t (12s)**.

#### [Libro: Cap.4 / campo_batalla_vivo / Pág.3] Mecánicas de Campo Vivo (3/4)

• **Necrofagia (`NecrophagyGoal`):** cadáveres no-jefe dejan carroña **240t**: comerla cura **+6.0 HP** y otorga **+20 XP** de rango. El fuego o zonas de Pilón impiden la carroña (`shouldDropCarrion`).
• **Atascos:** `AcidSpit` atasca torretas **120t (6s)** y `Leech` atasca drones **80t (4s)**.

#### [Libro: Cap.4 / campo_batalla_vivo / Pág.4] Mecánicas de Campo Vivo (4/4)

• **Goals (10):** `BruteCharge`, `EvasiveStrafe`, `FlankingAmbush`, `HiveAlertSupport`, `HiveRegroup`, `KitingRetreat`, `Necrophagy`, `SwarmFlank`, `TacticalTerraforming`, `WeaverSupport`.

---

## Capítulo 5: Bestiario y Quimeras

_17 especies vivas, 3 proyectiles, 17 huevos y genes 5x5_ | Puerta recomendada: `always` | Ícono: `mycelialmind:apex_matriarch_spawn_egg`

<a id="cap5-rangos_y_primordiales"></a>
### 1. Rangos Evolutivos y Primordiales

- **ID de Entrada:** `bestiario/rangos_y_primordiales` | **Ícono:** `mycelialmind:spore_behemoth_spawn_egg` | **Unlock:** `always`
- **Resumen:** BASE, MUTATED, ELITE, Primordiales y proyección vocal

#### [Libro: Cap.5 / rangos_y_primordiales / Pág.1] Rangos y Variantes Primordiales (1/2)

• **`BASE`:** **1.00x** HP, **1.00x** daño, **1.00** escala, **1.00x** XP, tono vocal **1.00**, vol. **1.00**.
• **`MUTATED`:** **1.25x** HP, **1.15x** daño, **1.12** escala, **1.25x** XP, tono **0.89**, vol. **1.10**.
• **`ELITE`:** **1.60x** HP, **1.35x** daño, **1.25** escala, **1.75x** XP, tono **0.78**, vol. **1.22** (aura de esporas carmesí).

#### [Libro: Cap.5 / rangos_y_primordiales / Pág.2] Rangos y Variantes Primordiales (2/2)

• **Primordiales Bioluminiscentes:** **+25%** HP máx. (`primordial_variant_health`), pulsos `bio_scanner_ping` cada **200t**, curan aliados de igual cepa en **6m** cada **40t** y desbloquean el Secreto #7.

<a id="cap5-bestiario_enjambre_ligero"></a>
### 2. Enjambre Ligero y Parasitario

- **ID de Entrada:** `bestiario/bestiario_enjambre_ligero` | **Ícono:** `mycelialmind:sporeling_spawn_egg` | **Unlock:** `always`
- **Resumen:** sporeling, leech_parasite y mycelial_stalker

#### [Libro: Cap.5 / bestiario_enjambre_ligero / Pág.1] Sporeling

> **[Modelo 3D Entidad]** `mycelialmind:sporeling` (Sporeling, escala `1.1`)

**14 HP | 3.5 Daño | 0.34 Vel**
Enjambre velocista: gana Velocidad en grupo (`6m`) y suma **+1** daño/aliado (máx. **+3**). Cerca de tu cama genera **+5.0 MELEE** al morir.

#### [Libro: Cap.5 / bestiario_enjambre_ligero / Pág.2] Sanguijuela Parásita

> **[Modelo 3D Entidad]** `mycelialmind:leech_parasite` (Sanguijuela Parásita, escala `1.1`)

**12 HP | 3.0 Daño | 0.35 Vel**
Sifón: aplica Hambre II, Fatiga, `mycelial_infection` (**160t**), drena saturación, se cura **+2 HP** y atasca Drones **80t** o Gólems.

#### [Libro: Cap.5 / bestiario_enjambre_ligero / Pág.3] Acechador Micelial

> **[Modelo 3D Entidad]** `mycelialmind:mycelial_stalker` (Acechador Micelial, escala `0.9`)

**22 HP | 5.0 Daño | 0.36 Vel**
Invisibilidad sobre `creeping_mycelium`. Golpear por la espalda o invisible inflige **+3.0** daño extra antes de saltar lateralmente.

<a id="cap5-bestiario_artilleria_fuego"></a>
### 3. Artillería Ácida y Pirófitos

- **ID de Entrada:** `bestiario/bestiario_artilleria_fuego` | **Ícono:** `mycelialmind:acid_spitter_spawn_egg` | **Unlock:** `always`
- **Resumen:** acid_spitter y pyro_shroom

#### [Libro: Cap.5 / bestiario_artilleria_fuego / Pág.1] Escupidor de Ácido

> **[Modelo 3D Entidad]** `mycelialmind:acid_spitter` (Escupidor de Ácido, escala `0.9`)

**20 HP | 4.0 Daño | 0.26 Vel**
Tiro parabólico `acid_spit` con predicción si está en escuadrón (`Desync` la anula). En `ELITE` dispara proyectil doble. Atasque a torretas: **120t**.

#### [Libro: Cap.5 / bestiario_artilleria_fuego / Pág.2] Piro-Hongo

> **[Modelo 3D Entidad]** `mycelialmind:pyro_shroom` (Piro-Hongo, escala `0.95`)

**24 HP | 5.0 Daño | 0.29 Vel**
Inmune al fuego (**+1 HP** si recibe daño ígneo). Al caer <=**35%** HP enciende mecha (**30t**, explosión **2.6F**); explota al morir (**2.2F**).

<a id="cap5-bestiario_ingenieros_soporte"></a>
### 4. Zapadores, Tejedores y Arquitectos

- **ID de Entrada:** `bestiario/bestiario_ingenieros_soporte` | **Ícono:** `mycelialmind:burrower_spawn_egg` | **Unlock:** `act_2`
- **Resumen:** burrower, synapse_weaver y spore_architect

#### [Libro: Cap.5 / bestiario_ingenieros_soporte / Pág.1] Excavador

> **[Modelo 3D Entidad]** `mycelialmind:burrower` (Excavador, escala `0.85`)

**28 HP | 5 Dmg | 3 Arm | 0.25 Vel**
Zapador: rompe bloques cada **30t** (**18t** con `BLAST_SHIELDED` N2). Velocidad II a **>8m**.

#### [Libro: Cap.5 / bestiario_ingenieros_soporte / Pág.2] Tejedor de Sinapsis

> **[Modelo 3D Entidad]** `mycelialmind:synapse_weaver` (Tejedor de Sinapsis, escala `0.9`)

**26 HP | 4 Dmg | 0.25 Vel**
Soporte (`WeaverSupportGoal` + `EvasiveStrafeGoal`): guarda distancia y potencia al escuadrón.

#### [Libro: Cap.5 / bestiario_ingenieros_soporte / Pág.3] Arquitecto de Esporas

> **[Modelo 3D Entidad]** `mycelialmind:spore_architect` (Arquitecto de Esporas, escala `0.85`)

**28 HP | 4 Dmg | 3 Arm | 0.27 Vel**
Constructor (`BuildColonyGoal`): levanta nuevos `mycelial_node` fuera de zonas con Pilón.

<a id="cap5-bestiario_pesados_mimico"></a>
### 5. Bruto, Góliat y Mímico Sináptico

- **ID de Entrada:** `bestiario/bestiario_pesados_mimico` | **Ícono:** `mycelialmind:synaptic_mimic_spawn_egg` | **Unlock:** `act_2`
- **Resumen:** chitin_brute, goliath_carrier y synaptic_mimic

#### [Libro: Cap.5 / bestiario_pesados_mimico / Pág.1] Bruto de Quitina

> **[Modelo 3D Entidad]** `mycelialmind:chitin_brute` (Bruto de Quitina, escala `0.75`)

**40 HP | 7 Dmg | 4 Arm | 0.5 KB**
Vanguardia (`BruteChargeGoal`) con **0.23** vel. y alta resistencia al empuje.

#### [Libro: Cap.5 / bestiario_pesados_mimico / Pág.2] Portador Góliat

> **[Modelo 3D Entidad]** `mycelialmind:goliath_carrier` (Portador Góliat, escala `0.65`)

**55 HP | 8 Dmg | 0.7 KB | 0.21 Vel**
Pisotón cada **110t** (`5m`, Lentitud II). Suelta **1** Sporeling <=**50% HP** y **2** al morir.

#### [Libro: Cap.5 / bestiario_pesados_mimico / Pág.3] Mímico Sináptico

> **[Modelo 3D Entidad]** `mycelialmind:synaptic_mimic` (Mímico Sináptico, escala `0.78`)

**65 HP | 6 Dmg | 6 Arm | 0.32 Vel**
Adapta **3 `MimicStance`**: `BLADE_DANCER`, `SPORE_MARKSMAN`, `AEGIS_MIRROR`.

<a id="cap5-bestiario_elite_campeon"></a>
### 6. Guardián y Behemot de Esporas

- **ID de Entrada:** `bestiario/bestiario_elite_campeon` | **Ícono:** `mycelialmind:colony_warden_spawn_egg` | **Unlock:** `act_3`
- **Resumen:** colony_warden (110 HP) y spore_behemoth (200 HP)

#### [Libro: Cap.5 / bestiario_elite_campeon / Pág.1] Guardián de Colonia

> **[Modelo 3D Entidad]** `mycelialmind:colony_warden` (Guardián de Colonia, escala `0.62`)

**110 HP | 11 Dmg | 8 Arm | 0.85 KB**
Acelera enjambre (**60t**) y lanza Onda (**140t**: Fuerza+Vel `12m`, empuje `4m`).

#### [Libro: Cap.5 / bestiario_elite_campeon / Pág.2] Behemot de Esporas

> **[Modelo 3D Entidad]** `mycelialmind:spore_behemoth` (Behemot de Esporas, escala `0.52`)

**200 HP | 10 Dmg | 8 Arm | 0.85 KB | 45 XP**
Campeón de Marea y `MatriarchCitadel`. Suelta `tide_beacon`.

<a id="cap5-bestiario_jefe_hive_core"></a>
### 7. Jefe Overworld: Núcleo Colmena

- **ID de Entrada:** `bestiario/bestiario_jefe_hive_core` | **Ícono:** `mycelialmind:hive_core_spawn_egg` | **Unlock:** `act_3`
- **Resumen:** hive_core (350 HP) — 3 Fases y 3 Anclas Centinela

#### [Libro: Cap.5 / bestiario_jefe_hive_core / Pág.1] Núcleo de la Colmena

> **[Modelo 3D Entidad]** `mycelialmind:hive_core` (Núcleo de la Colmena, escala `0.45`)

**350 HP | 14 Dmg | 10 Arm | 350 XP**
`hive_altar`. F1: ácido x3. F2 (65–30%): invulnerable con **3 `sentinel_node`**.

#### [Libro: Cap.5 / bestiario_jefe_hive_core / Pág.2] Fase 3 y Botín del Núcleo

• **Anclas Fase 2:** destruye los **3** `sentinel_node` enlazados para aturdir al jefe y romper su escudo.
• **Fase 3 (<30% HP) y Botín:** rota sinergias N2 + ondas de choque. Botín: `hive_heart`, `synaptic_core`, `neural_echo_core`, `synaptic_graft`.

<a id="cap5-bestiario_jefe_matriarca"></a>
### 8. Jefe Final: Matriarca Suprema

- **ID de Entrada:** `bestiario/bestiario_jefe_matriarca` | **Ícono:** `mycelialmind:apex_matriarch_spawn_egg` | **Unlock:** `deep_mycelium`
- **Resumen:** apex_matriarch (600 HP) en el Micelio Profundo

#### [Libro: Cap.5 / bestiario_jefe_matriarca / Pág.1] Matriarca Suprema

> **[Modelo 3D Entidad]** `mycelialmind:apex_matriarch` (Matriarca Suprema, escala `0.4`)

**600 HP | 18 Dmg | 12 Arm | 750 XP**
`matriarch_altar`. F1: andanada + blink. F2 (<60%): +20 estrés, 2 Centinelas + élite.

#### [Libro: Cap.5 / bestiario_jefe_matriarca / Pág.2] Fase 3, Botín y Remisión

• **Fase 3 (<25% HP):** pulsos mágicos cada **100t** en **8m** (**6.0** daño mágico + `mycelial_infection` II `160t`) y sinergias activas.
• **Recompensas:** `overmind_essence`, `hive_heart`, `quorum_seal`, `chimera_ovum`, `echo_shard` y activa **20 min** de Remisión global.

<a id="cap5-bestiario_aliados_quimera"></a>
### 9. Aliados Bioforjados: Dron y Quimera

- **ID de Entrada:** `bestiario/bestiario_aliados_quimera` | **Ícono:** `mycelialmind:chimera_ovum` | **Unlock:** `act_2`
- **Resumen:** purifier_drone (60 HP) y mycelial_chimera (matriz genética 5x5)

#### [Libro: Cap.5 / bestiario_aliados_quimera / Pág.1] Dron Purificador

> **[Modelo 3D Entidad]** `mycelialmind:purifier_drone` (Dron Purificador, escala `0.85`)

**60 HP | 6 Arm | 0.45 Vuelo**
`purifier_drone_core`: dispara `neutralizing_spore`, limpia micelio y patrulla. Sanguijuela atasca **80t**.

#### [Libro: Cap.5 / bestiario_aliados_quimera / Pág.2] Quimera Micelial

> **[Modelo 3D Entidad]** `mycelialmind:mycelial_chimera` (Quimera Micelial, escala `0.68`)

**52/68 HP | 5.5/8.5 Dmg | 4/8 Arm**
De `chimera_ovum` en `symbiotic_node` (>=35%). Montura `CommandMode` (`FOLLOW`, `PATROL`, `WAIT`).

#### [Libro: Cap.5 / bestiario_aliados_quimera / Pág.3] Matriz Genética 5×5 de la Quimera (1/3)

• **Chasis (`ChassisGene`, +Base):** `MELEE_THORNS` (**8.5** daño), `PROJECTILE_DEFLECT` (**0.35** vel.), `FIRE_RESISTANT` (inmune fuego), `BLAST_SHIELDED` (**68 HP**, **8.0 Arm**), `MAGIC_CARAPACE` (cura **1 HP/100t**).

#### [Libro: Cap.5 / bestiario_aliados_quimera / Pág.4] Matriz Genética 5×5 de la Quimera (2/3)

• **Mutación Activa (`140t` / `160t` montada):** `MELEE_THORNS` (salto + Veneno), `PROJECTILE_DEFLECT` (doble espora `16m`), `FIRE_RESISTANT` (aura ígnea + Res. jinete), `BLAST_SHIELDED` (onda empujón), `MAGIC_CARAPACE` (Absorción a jinete).

#### [Libro: Cap.5 / bestiario_aliados_quimera / Pág.5] Matriz Genética 5×5 de la Quimera (3/3)

• **Lluvia Ácida (Homeostasis):** con `FIRE_RESISTANT` y `MAGIC_CARAPACE` regenera **+0.8F**; con uno de ambos es inmune (**0.0F**); sin ellos sufre **-0.75F**.

<a id="cap5-proyectiles_y_huevos"></a>
### 10. Proyectiles (3) y Spawn Eggs (17)

- **ID de Entrada:** `bestiario/proyectiles_y_huevos` | **Ícono:** `mycelialmind:hive_core_spawn_egg` | **Unlock:** `always`
- **Resumen:** acid_spit, neutralizing_spore, purifier_grenade y los 17 huevos

#### [Libro: Cap.5 / proyectiles_y_huevos / Pág.1] Proyectiles y Huevos Generadores (1/5)

• **3 Proyectiles Registrados (`ModEntities`):** `acid_spit` (escupitajo corrosivo que atasca torretas **120t** y refleja `KINETIC_MIRROR`), `neutralizing_spore` (lanzador/dron purificador) y `purifier_grenade` (desincroniza escuadrones **5s**).

#### [Libro: Cap.5 / proyectiles_y_huevos / Pág.2] Proyectiles y Huevos Generadores (2/5)

• **Huevos 1/4:** `sporeling_spawn_egg`, `chitin_brute_spawn_egg`, `mycelial_stalker_spawn_egg`, `acid_spitter_spawn_egg`.

#### [Libro: Cap.5 / proyectiles_y_huevos / Pág.3] Proyectiles y Huevos Generadores (3/5)

• **Huevos 2/4:** `burrower_spawn_egg`, `pyro_shroom_spawn_egg`, `synapse_weaver_spawn_egg`, `goliath_carrier_spawn_egg`, `leech_parasite_spawn_egg`.

#### [Libro: Cap.5 / proyectiles_y_huevos / Pág.4] Proyectiles y Huevos Generadores (4/5)

• **Huevos 3/4:** `colony_warden_spawn_egg`, `spore_architect_spawn_egg`, `synaptic_mimic_spawn_egg`, `spore_behemoth_spawn_egg`.

#### [Libro: Cap.5 / proyectiles_y_huevos / Pág.5] Proyectiles y Huevos Generadores (5/5)

• **Huevos 4/4:** `hive_core_spawn_egg`, `apex_matriarch_spawn_egg`, `purifier_drone_spawn_egg`, `mycelial_chimera_spawn_egg`.

---

## Capítulo 6: Red Sináptica y Nodos

_4 tipos de nodo, Injerto, Viaje Micorrizal 96m y Acústica_ | Puerta recomendada: `always` | Ícono: `mycelialmind:symbiotic_node`

<a id="cap6-nodos_y_malla_sinaptica"></a>
### 1. Los 4 Nodos Sinápticos

- **ID de Entrada:** `red_sinaptica/nodos_y_malla_sinaptica` | **Ícono:** `mycelialmind:mycelial_node` | **Unlock:** `always`
- **Resumen:** SynapticNodeType: RELAY, INCUBATOR, SENTINEL y SYMBIOTIC

#### [Libro: Cap.6 / nodos_y_malla_sinaptica / Pág.1] Tipología de Red Nodal (1/3)

• `RELAY` (`mycelial_node`): enlaza nodos dentro de `synapticNodeLinkRadius` (**28m**), amplifica **x1.5** el aprendizaje local y alberga búfer `60/40` (`600t`).

#### [Libro: Cap.6 / nodos_y_malla_sinaptica / Pág.2] Tipología de Red Nodal (2/3)

• `INCUBATOR` (`incubator_node`): engendra refuerzos y `spore_architect`; tope global autónomo `maxAutonomousNodes = 32` con espaciado mínimo `minNodeSpacing = 18m`.

#### [Libro: Cap.6 / nodos_y_malla_sinaptica / Pág.3] Tipología de Red Nodal (3/3)

• `SENTINEL` (`sentinel_node`): blinda mobs cercanos y actúa como ancla de invulnerabilidad en Fase 2 de `hive_core` y `apex_matriarch`.
• `SYMBIOTIC` (`symbiotic_node`, BE `synaptic_node`): nodo reprogramado aliado silenciador y portal de viaje rápido.

<a id="cap6-injerto_y_viaje_micorrizal"></a>
### 2. Injerto y Viaje Micorrizal

- **ID de Entrada:** `red_sinaptica/injerto_y_viaje_micorrizal` | **Ícono:** `mycelialmind:symbiotic_node` | **Unlock:** `act_2`
- **Resumen:** Reprogramación nodal y salto rápido a 96m con Quimeras y Drones

#### [Libro: Cap.6 / injerto_y_viaje_micorrizal / Pág.1] Secuestro con Injerto Sináptico

> **[Vista Ítem]** `mycelialmind:synaptic_graft`

Aplica `synaptic_graft` en un nodo hostil -> `symbiotic_node` (máx. `maxSymbioticNodes = 4`). Recarga **+15%** con `mycelial_biomass`.

#### [Libro: Cap.6 / injerto_y_viaje_micorrizal / Pág.2] Viaje Rápido Micorrizal (1/2)

• **Enlace (`Shift + Clic`):** conecta con `symbiotic_node` dentro de **96m** (`mycorrhizalTravelRange`, rango `16–256m`) sin spawns hostiles.
• **Costo y Bloqueo:** **6** comida, **15** estabilidad, `240t` `mycelial_infection`, `60t` Náusea (`1500t` CD); bloqueado en combate (`100t`) o ya infectado.

#### [Libro: Cap.6 / injerto_y_viaje_micorrizal / Pág.3] Viaje Rápido Micorrizal (2/2)

• **Escolta Aliada:** transfiere Quimeras (`FOLLOW` o montadas) y Drones (sin patrulla) en un radio de **12m** al instante.

<a id="cap6-acustica_micorrizal_senuelo"></a>
### 3. Acústica Micorrizal y Señuelo Eco

- **ID de Entrada:** `red_sinaptica/acustica_micorrizal_senuelo` | **Ícono:** `mycelialmind:neural_echo_decoy` | **Unlock:** `always`
- **Resumen:** MycorrhizalAcousticTracker (32m/18m) y neural_echo_decoy (16m/20m)

#### [Libro: Cap.6 / acustica_micorrizal_senuelo / Pág.1] Vibraciones y Silencio Acústico

> **[Vista Ítem]** `mycelialmind:neural_echo_decoy`

Pisar `creeping_mycelium` alerta Nodos (`32m`) y patrullas (`18m`, `40t` CD). `neural_echo_decoy` silencia **16m** y atrae en **20m**.

---

## Capítulo 7: Biotecnología y Diplomacia

_Mesa Sináptica N1–N3, 5 Mutágenos, Cismas y Diplomacia Química_ | Puerta recomendada: `always` | Ícono: `mycelialmind:synaptic_research_bench`

<a id="cap7-mesa_investigacion_sinaptica"></a>
### 1. Mesa Sináptica y Niveles Lab 1–3

- **ID de Entrada:** `biotecnologia/mesa_investigacion_sinaptica` | **Ícono:** `mycelialmind:synaptic_research_bench` | **Unlock:** `always`
- **Resumen:** SynapticBenchBlockEntity, ranuras, tolvas y 4 síntesis base

#### [Libro: Cap.7 / mesa_investigacion_sinaptica / Pág.1] Mejora de Laboratorio (N1->N2->N3)

> **[Vista Ítem]** `mycelialmind:synaptic_research_bench`

`Shift + Clic`: **N1->N2** (`synaptic_core`, **25%** ahorro biomasa); **N2->N3** (`neural_echo_core`/`overmind_essence`, **50%** y **x2** sueros).

#### [Libro: Cap.7 / mesa_investigacion_sinaptica / Pág.2] Las 4 Síntesis Oficiales (1/2)

• `symbiotic_mutagen`: `GeneticSample` (con rasgo) + `MycelialBiomass` + `SynapticCore`.
• `counter_serum`: `GeneticSample` (con rasgo) + `LatentSpore` + (`PurifierGrenade` o Amatista) -> **x1** (o **x2** en N3).

#### [Libro: Cap.7 / mesa_investigacion_sinaptica / Pág.3] Las 4 Síntesis Oficiales (2/2)

• `concentrated_antidote`: `SporeAntidote` + `LatentSpore` + Amatista -> **x1** (**x2** en N3).
• `chimera_ovum`: `GeneticSample` + `MycelialBiomass` + `NeuralEchoCore`. Tolvas vía `SidedSynapticBenchHandler` (menú `synaptic_bench`).

<a id="cap7-simbiosis_mutagenos"></a>
### 2. Simbiosis y los 5 Mutágenos

- **ID de Entrada:** `biotecnologia/simbiosis_mutagenos` | **Ícono:** `mycelialmind:symbiotic_mutagen` | **Unlock:** `act_2`
- **Resumen:** SymbioticMutagen: injertos corporales, límites y riesgos

#### [Libro: Cap.7 / simbiosis_mutagenos / Pág.1] Los 5 Mutágenos Simbióticos (1/4)

• **Reglas (`symbiosis`):** máx. `maxMutagenSlots = 2`, añade **+15%** Amenaza/slot (`1.0 + 0.15*n`), aplica `mycelial_infection` (`120t`) cada `60t` sobre micelio sin proteger y se limpia al morir.

#### [Libro: Cap.7 / simbiosis_mutagenos / Pág.2] Los 5 Mutágenos Simbióticos (2/4)

• `CHITIN_THORNS` (`chitin_thorns`, `MELEE`): **+2.0** Armadura y refleja **25%** del daño melee recibido (mín. `1.0F`) como espinas.
• `KINETIC_STRAFE` (`kinetic_strafe`, `PROJECTILE`): **30%** prob. de esquivar proyectiles cancelando el golpe y ganar Velocidad I (`60t`).

#### [Libro: Cap.7 / simbiosis_mutagenos / Pág.3] Los 5 Mutágenos Simbióticos (3/4)

• `PYRO_GLAND` (`pyro_gland`, `FIRE`): inmunidad total al fuego (cancela `IS_FIRE` y apaga llamas) + ataques cuerpo a cuerpo prenden `3s`.

#### [Libro: Cap.7 / simbiosis_mutagenos / Pág.4] Los 5 Mutágenos Simbióticos (4/4)

• `BLAST_CARAPACE` (`blast_carapace`, `EXPLOSION`): **+0.5** Empuje Res. y reduce daño explosivo al **55%** (**-45%**).
• `ARCANE_SYNAPSE` (`arcane_synapse`, `MAGIC`): Visión Nocturna continua y daño mágico al **60%** (**-40%**).

<a id="cap7-cisma_y_diplomacia_quimica"></a>
### 3. Cismas y Diplomacia Química

- **ID de Entrada:** `biotecnologia/cisma_y_diplomacia_quimica` | **Ícono:** `mycelialmind:quorum_seal` | **Unlock:** `act_2`
- **Resumen:** Cepas ORTHODOX/AMBER/CRIMSON, Pactos, Truce y Quorum

#### [Libro: Cap.7 / cisma_y_diplomacia_quimica / Pág.1] Guerra Civil de Cepas

> **[Vista Ítem]** `mycelialmind:schism_injector`

Usa `schism_injector` en un nodo/mob: `ORTHODOX` (#9B4DFF, -100) vs `AMBER` (#F2A93B) vs `CRIMSON` (#E03C5C). Luchan entre sí.

#### [Libro: Cap.7 / cisma_y_diplomacia_quimica / Pág.2] Reputación, Tributo y Traición (1/2)

• **Rangos (`Standing`, `-100..+100`):** `HOSTILE` `<=-40`, `NEUTRAL` `-39..24`, `PACT` `>=+25` (`BASE` ignora `>4.5m`), `ALLIED` `>=+60` (`BASE` y `MUTATED`). Ganar rep aplica **`-0.5x`** en la cepa opuesta.

#### [Libro: Cap.7 / cisma_y_diplomacia_quimica / Pág.3] Reputación, Tributo y Traición (2/2)

• **Herramientas y Reglas:** `ELITE` y Jefes siempre atacan. Traición: **-35** rep y **30s** (`600t`) de bloqueo. `truce_pheromone` da tregua; `quorum_seal` tributa **4** `mycelial_biomass` (**+12** rep) e invoca hasta **2** escoltas (**45s**).

---

## Capítulo 8: Arsenal, Quitina y Bastión

_Quitina evolutiva, armas, 4 Reliquias, Pilones y Torretas_ | Puerta recomendada: `always` | Ícono: `mycelialmind:overmind_crown`

<a id="cap8-armadura_quitina_reactiva"></a>
### 1. Armadura de Quitina Evolutiva

- **ID de Entrada:** `arsenal_defensa/armadura_quitina_reactiva` | **Ícono:** `mycelialmind:chitin_chestplate` | **Unlock:** `always`
- **Resumen:** Material chitin, DataComponents (armor_rank, trait_id) y evolución

#### [Libro: Cap.8 / armadura_quitina_reactiva / Pág.1] Estadísticas del Material `chitin`

> **[Vista Ítem]** `mycelialmind:chitin_chestplate`

Defensa `[2, 5, 6, 2]` (Cuerpo **9**), Encant. **14**, Dureza **1.0F**, repara con `chitin_plate`. Recorta infección **-20%** por pieza.

#### [Libro: Cap.8 / armadura_quitina_reactiva / Pág.2] Evolución y `DataComponents` (1/2)

• **`armor_rank` (`BASE` -> `MUTATED` -> `ELITE`):** sube a `MUTATED` con `synaptic_core` (autorepara sobre micelio/Marea) y a `ELITE` con `overmind_essence` o `tide_beacon`.

#### [Libro: Cap.8 / armadura_quitina_reactiva / Pág.3] Evolución y `DataComponents` (2/2)

• **Reflejo Élite y `trait_id`:** `ELITE` otorga Absorción periódica y refleja `1.25 * piezas` daño mágico; `trait_id` codifica afinidad elemental en piezas, `genetic_sample` y sueros.

<a id="cap8-arsenal_tactico_completo"></a>
### 2. Arsenal de Combate y Campo

- **ID de Entrada:** `arsenal_defensa/arsenal_tactico_completo` | **Ícono:** `mycelialmind:symbiotic_blade` | **Unlock:** `always`
- **Resumen:** Espadas, lanzas, lanzadores, granadas y herramientas de campo

#### [Libro: Cap.8 / arsenal_tactico_completo / Pág.1] Armas y Consumibles Tácticos (1/4)

• `symbiotic_blade`: espada simbiótica de tier diamante alimentada por `hive_heart`.
• `enzyme_lance`: lanza enzimática ligera que perfora tejido quitinoso.

#### [Libro: Cap.8 / arsenal_tactico_completo / Pág.2] Armas y Consumibles Tácticos (2/4)

• `neutralizing_spore_launcher` y `purifier_grenade`: proyectiles neutralizantes; las granadas desincronizan escuadrones (**5s**) y dañan carroña/esporas.

#### [Libro: Cap.8 / arsenal_tactico_completo / Pág.3] Armas y Consumibles Tácticos (3/4)

• `bio_extractor` -> extrae `genetic_sample`; `spore_antidote` / `concentrated_antidote` curan infección y podredumbre; `chimera_ovum` eclosiona Quimeras.

#### [Libro: Cap.8 / arsenal_tactico_completo / Pág.4] Armas y Consumibles Tácticos (4/4)

• **Reactivos Clave:** `mycelial_biomass`, `chitin_plate`, `latent_spore`, `synaptic_core`, `neural_echo_core`, `hive_heart` y `overmind_essence`.

<a id="cap8-reliquias_endgame"></a>
### 3. Las 4 Reliquias Endgame

- **ID de Entrada:** `arsenal_defensa/reliquias_endgame` | **Ícono:** `mycelialmind:overmind_crown` | **Unlock:** `act_3`
- **Resumen:** TideBeacon, SynapticSuppressor, RiftCatalyst y OvermindCrown

#### [Libro: Cap.8 / reliquias_endgame / Pág.1] Corona de la Supramente

> **[Vista Ítem]** `mycelialmind:overmind_crown`

• **En el aire:** purga **15.0%** de Amenaza Global (cooldown **1200t / 60s**).
• **Sobre `purification_pylon`:** sobrecarga y limpia **12m** alrededor (**400t / 20s**).

#### [Libro: Cap.8 / reliquias_endgame / Pág.2] Faro, Supresor y Catalizador (1/2)

• `tide_beacon`: disipa la Marea Fúngica activa y recarga su núcleo con amatista y quitina (`tide_beacon_recharge`).
• `synaptic_suppressor`: congela la adaptación de la Colmena (`120s` CD).

#### [Libro: Cap.8 / reliquias_endgame / Pág.3] Faro, Supresor y Catalizador (2/2)

• `rift_catalyst`: usado dentro de un marco válido de `reinforced_bio_barrier`, abre el portal `mycelial_rift` hacia el **Micelio Profundo** (`deep_mycelium`).

<a id="cap8-bastion_defensivo"></a>
### 4. Defensa del Bastión y Torretas

- **ID de Entrada:** `arsenal_defensa/bastion_defensivo` | **Ícono:** `mycelialmind:purification_pylon` | **Unlock:** `act_2`
- **Resumen:** Pilón (monument_focus), Torreta (3 tiers), Dron y Bio-Barrera

#### [Libro: Cap.8 / bastion_defensivo / Pág.1] Ingeniería del Bastión (1/2)

• `purification_pylon` (`monument_focus`): frena expansión micelial (`24m` base, `pylonPurificationRadius`), cura infección, impide carroña y sofoca si sufre **>=4** `antibody_cyst` adyacentes en el Micelio Profundo.

#### [Libro: Cap.8 / bastion_defensivo / Pág.2] Ingeniería del Bastión (2/2)

• `purification_turret` (Niveles 1–3): dispara esporas neutralizantes; reparable con Antídoto, Quitina o Hierro. Si `AcidSpit` la atasca (**120t**), límpiala al vuelo.
• `purifier_drone_core` despliega al Dron; `reinforced_bio_barrier` blinda muros.

---

## Capítulo 9: Economía, Recetas y Botín

_24 Bloques, 53 Ítems, 37 Recetas, Brews, Compost y 49 Botines_ | Puerta recomendada: `always` | Ícono: `mycelialmind:chitin_plate`

<a id="cap9-catalogo_bloques_items"></a>
### 1. Catálogo: 24 Bloques y 53 Ítems

- **ID de Entrada:** `economia_datos/catalogo_bloques_items` | **Ícono:** `mycelialmind:mycelial_biomass` | **Unlock:** `always`
- **Resumen:** Inventario íntegro de flora, bloques e ítems registrados

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.1] Flora, Geología y Bloques Estructurales (1/4)

• `creeping_mycelium`: propaga micelio y transmisiones acústicas (**32m**); horneable a `mycelial_stone`.
• `hyphal_vein`: vena negra en forma de rayo sobre tus rutas habituales (`CHARGED` si densidad >=60%). Cortable con Tijeras, Azada o Hoja Simbiótica.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.2] Flora, Geología y Bloques Estructurales (2/4)

• `spore_vent`: emite gases de infección/podredumbre; purificable con `spore_antidote` (Secreto #5).

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.3] Flora, Geología y Bloques Estructurales (3/4)

• `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`: flora regenerativa con estado `MATURE` y señal Redstone; fertilizable para revelar Bóvedas (Secreto #8).

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.4] Flora, Geología y Bloques Estructurales (4/4)

• `mycelial_stone`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `mycelial_rift`, `hive_altar`, `matriarch_altar`, `ephemeral_chitin`, `antibody_cyst`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.5] Los 24 Bloques (`ModBlocks`) (1/3)

• **Red y Contención (9):** `creeping_mycelium`, `hyphal_vein`, `spore_vent`, `mycelial_node`, `incubator_node`, `sentinel_node`, `symbiotic_node`, `purification_pylon`, `purification_turret`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.6] Los 24 Bloques (`ModBlocks`) (2/3)

• **Fortificación y Flora (8):** `reinforced_bio_barrier`, `synaptic_research_bench`, `hive_altar`, `matriarch_altar`, `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`, `antibody_cyst`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.7] Los 24 Bloques (`ModBlocks`) (3/3)

• **Arquitectura y Dimensión (8):** `mycelial_stone`, `mycelial_rift`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `ephemeral_chitin`, `reclaimed_bloom`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.8] Ítems y Armadura (`ModItems`) (1/5)

• **Instrumentos y Armas (10):** `bio_scanner`, `bio_extractor`, `enzyme_lance`, `neutralizing_spore_launcher`, `symbiotic_blade`, `purifier_grenade`, `synaptic_suppressor`, `overmind_crown`, `rift_catalyst`, `mycelial_codex`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.9] Ítems y Armadura (`ModItems`) (2/5)

• **Armadura y Sueros (10):** `chitin_helmet`, `chitin_chestplate`, `chitin_leggings`, `chitin_boots`, `spore_antidote`, `concentrated_antidote`, `genetic_sample`, `symbiotic_mutagen`, `counter_serum`, `tide_beacon`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.10] Ítems y Armadura (`ModItems`) (3/5)

• **Diplomacia y Cognición (10):** `purifier_drone_core`, `neural_echo_core`, `synaptic_graft`, `neural_echo_decoy`, `schism_injector`, `truce_pheromone`, `quorum_seal`, `chimera_ovum`, `volatile_engram`, `field_engram`.

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.11] Ítems y Armadura (`ModItems`) (4/5)

• **Inyección y Reactivos (7):** `false_engram_injector`, `mycelial_biomass`, `chitin_plate`, `latent_spore`, `synaptic_core`, `hive_heart`, `overmind_essence` (+ **17** `*_spawn_egg` en Cap.5).

#### [Libro: Cap.9 / catalogo_bloques_items / Pág.12] Ítems y Armadura (`ModItems`) (5/5)

• **Brotes Recuperados (5):** `reclaimed_bloom_melee`, `reclaimed_bloom_projectile`, `reclaimed_bloom_fire`, `reclaimed_bloom_explosion`, `reclaimed_bloom_magic`.

<a id="cap9-recetas_36_datapack"></a>
### 2. Recetario Total (37 Datapack)

- **ID de Entrada:** `economia_datos/recetas_36_datapack` | **Ícono:** `mycelialmind:chitin_plate` | **Unlock:** `always`
- **Resumen:** 29 Mesa, 3 Horno, 1 Ahumador, 1 Alto Horno y 3 Cortapiedras

#### [Libro: Cap.9 / recetas_36_datapack / Pág.1] Bio-Escáner y Bio-Extractor

> **[Receta 1]** `[crafting_shaped]  R /ICI/ S  (C=compass, I=copper_ingot, R=redstone, S=latent_spore) -> 1x bio_scanner`
> **[Receta 2]** `[crafting_shaped]  A /IBI/ S  (A=amethyst_shard, B=mycelial_biomass, I=iron_ingot, S=stick) -> 1x bio_extractor`

Telemetría inicial: Brújula, Redstone, Cobre y Espora (`bio_scanner`); Amatista, Hierro y Biomasa (`bio_extractor`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.2] Antídoto de Esporas y Concentrado

> **[Receta 1]** `[crafting_shapeless] glass_bottle + latent_spore + sugar -> 1x spore_antidote`
> **[Receta 2]** `[crafting_shapeless] spore_antidote + latent_spore + amethyst_shard -> 1x concentrated_antidote`

Profilaxis clínica sin mesa: Frasco + `latent_spore` + Azúcar; concentra con Amatista.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.3] Casco y Peto de Quitina

> **[Receta 1]** `[crafting_shaped] CCC/C C (C=chitin_plate) -> 1x chitin_helmet`
> **[Receta 2]** `[crafting_shaped] C C/CSC/CCC (C=chitin_plate, S=synaptic_core) -> 1x chitin_chestplate`

Forja con `chitin_plate`; el Peto integra un `synaptic_core` central.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.4] Grebas y Botas de Quitina

> **[Receta 1]** `[crafting_shaped] CCC/C C/C C (C=chitin_plate) -> 1x chitin_leggings`
> **[Receta 2]** `[crafting_shaped] C C/C C (C=chitin_plate) -> 1x chitin_boots`

Completan el conjunto de Quitina (`[2,5,6,2]`) reduciendo infección `-20%` por pieza.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.5] Lanza Enzimática y Hoja Simbiótica

> **[Receta 1]** `[crafting_shaped]   C/ S /I   (C=chitin_plate, I=iron_ingot, S=latent_spore) -> 1x enzyme_lance`
> **[Receta 2]** `[crafting_shaped]  H / C / D  (C=chitin_plate, D=diamond_sword, H=hive_heart) -> 1x symbiotic_blade`

Armas melee progresivas: desde `chitin_plate` hasta `hive_heart` sobre Espada de Diamante.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.6] Lanzador Neutralizante y Granada (x2)

> **[Receta 1]** `[crafting_shaped] LSL/ C / B  (B=amethyst_shard, C=chitin_plate, L=latent_spore, S=synaptic_core) -> 1x neutralizing_spore_launcher`
> **[Receta 2]** `[crafting_shaped]  A /LGL/ B  (A=amethyst_shard, B=mycelial_biomass, G=gunpowder, L=latent_spore) -> 2x purifier_grenade`

Artillería purificadora y granadas de área que rompen coordinación sináptica (`Desync`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.7] Pilón y Torreta de Purificación

> **[Receta 1]** `[crafting_shaped]  A /CSC/BBB (A=amethyst_shard, B=polished_deepslate, C=chitin_plate, S=synaptic_core) -> 1x purification_pylon`
> **[Receta 2]** `[crafting_shaped]  A /ILI/ P  (A=amethyst_shard, I=iron_ingot, L=neutralizing_spore_launcher, P=purification_pylon) -> 1x purification_turret`

Perímetro defensivo (`pylonPurificationRadius = 24m`) y defensa automática (`purification_turret`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.8] Bio-Barrera (x4) y Núcleo de Dron

> **[Receta 1]** `[crafting_shaped] CPC/PDP/CPC (C=chitin_plate, D=mycelial_biomass, P=polished_deepslate) -> 4x reinforced_bio_barrier`
> **[Receta 2]** `[crafting_shaped]  G /CPC/ S  (C=chitin_plate, G=gold_ingot, P=purification_pylon, S=synaptic_core) -> 1x purifier_drone_core`

Muro antibrecha + marco del portal; `purifier_drone_core` despliega soporte aéreo.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.9] Mesa Sináptica e Injerto Sináptico

> **[Receta 1]** `[crafting_shaped] ASA/ICI/BBB (A=amethyst_shard, B=chitin_plate, C=crafting_table, I=iron_ingot, S=mycelial_biomass) -> 1x synaptic_research_bench`
> **[Receta 2]** `[crafting_shaped]  E /CSC/ L  (C=copper_ingot, E=neural_echo_core, L=latent_spore, S=synaptic_core) -> 1x synaptic_graft`

Estación de síntesis biotecnológica (`synaptic_research_bench`) e hijacker de nodos (`synaptic_graft`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.10] Señuelo Eco (x2) e Inyector Falso

> **[Receta 1]** `[crafting_shaped]  L /CEC/ L  (C=copper_ingot, E=neural_echo_core, L=latent_spore) -> 2x neural_echo_decoy`
> **[Receta 2]** `[crafting_shaped]  V /CVC/ L  (C=copper_ingot, L=latent_spore, V=volatile_engram) -> 1x false_engram_injector`

Contramedidas acústicas (`neural_echo_decoy`) y sobre-adaptación Pavloviana (`false_engram_injector`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.11] Inyector de Cisma y Feromona (x2)

> **[Receta 1]** `[crafting_shaped]  L /CEC/ B  (B=blaze_powder, C=chitin_plate, E=neural_echo_core, L=latent_spore) -> 1x schism_injector`
> **[Receta 2]** `[crafting_shapeless] mycelial_biomass + latent_spore + glass_bottle + fermented_spider_eye -> 2x truce_pheromone`

Induce guerra civil Ámbar/Carmesí o rocía tregua química temporal.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.12] Sello de Quórum y Óvulo Quimérico

> **[Receta 1]** `[crafting_shaped]  C /ESA/ C  (A=amethyst_shard, C=chitin_plate, E=neural_echo_core, S=synaptic_core) -> 1x quorum_seal`
> **[Receta 2]** `[crafting_shaped]  B /GEG/ S  (B=mycelial_biomass, E=neural_echo_core, G=genetic_sample, S=synaptic_core) -> 1x chimera_ovum`

Negocia escoltas aliadas (`quorum_seal`) o sintetiza el embrión modular (`chimera_ovum`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.13] Supresor Sináptico y Catalizador Grieta

> **[Receta 1]** `[crafting_shaped]  S /PHP/ D  (D=diamond, H=hive_heart, P=purification_pylon, S=synaptic_core) -> 1x synaptic_suppressor`
> **[Receta 2]** `[crafting_shaped]  E /SHS/ C  (C=chitin_plate, E=ender_pearl, H=hive_heart, S=synaptic_core) -> 1x rift_catalyst`

Reliquias de Acto III+: congela aprendizaje global o abre grietas al Micelio Profundo.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.14] Corona Supramente y Recarga Faro

> **[Receta 1]** `[crafting_shaped]  O /DHD/ S  (D=diamond, H=hive_heart, O=overmind_essence, S=synaptic_core) -> 1x overmind_crown`
> **[Receta 2]** `[crafting_shapeless] tide_beacon + amethyst_shard + chitin_plate -> 1x tide_beacon`

Pináculo Acto IV (`overmind_essence` + `hive_heart`); recarga `tide_beacon` con amatista y quitina.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.15] Códice de la Colmena Viva

> **[Receta 1]** `[crafting_shapeless] book + latent_spore + mycelial_biomass -> 1x mycelial_codex`

Manual in-game sin mesa: Libro + `latent_spore` + `mycelial_biomass` (`mycelial_codex`).

#### [Libro: Cap.9 / recetas_36_datapack / Pág.16] Fundición: Espora y Piedra Micelial

> **[Receta 1]** `[smelting] bioluminescent_mycelial_cap -> 1x latent_spore`
> **[Receta 2]** `[smelting] creeping_mycelium -> 1x mycelial_stone`

Hornea `bioluminescent_mycelial_cap` -> `latent_spore` (0.15 XP) o `creeping_mycelium` -> `mycelial_stone`.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.17] Horno: Quiste -> Placa de Quitina

> **[Receta 1]** `[smelting] antibody_cyst -> 1x chitin_plate`

Refina `antibody_cyst` en Horno normal (`200t`, `0.2 XP`) -> `chitin_plate`.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.18] Ahumador: Sombrero -> Espora

> **[Receta 1]** `[smoking] bioluminescent_mycelial_cap -> 1x latent_spore`

Deshidratación rápida en Ahumador (`100t`, `0.15 XP`): `bioluminescent_mycelial_cap` -> `latent_spore`.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.19] Alto Horno: Quiste -> Quitina

> **[Receta 1]** `[blasting] antibody_cyst -> 1x chitin_plate`

Calcina `antibody_cyst` al doble de velocidad (`100t`, `0.2 XP`) -> `chitin_plate`.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.20] Cortapiedras: Ladrillo y Pilar

> **[Receta 1]** `[stonecutting] mycelial_stone -> 1x mycelial_arch_brick`
> **[Receta 2]** `[stonecutting] mycelial_arch_brick -> 1x chitin_pillar`

`mycelial_stone` -> `mycelial_arch_brick` -> `chitin_pillar` sin desperdicio.

#### [Libro: Cap.9 / recetas_36_datapack / Pág.21] Cortapiedras: Celosía Bio-Quitina

> **[Receta 1]** `[stonecutting] chitin_plate -> 2x bio_chitin_lattice`

1 `chitin_plate` se talla en **2** `bio_chitin_lattice` para galerías y ventanales.

<a id="cap9-vanilla_brew_compost_trades"></a>
### 3. Alambique, Compostaje y Comercio

- **ID de Entrada:** `economia_datos/vanilla_brew_compost_trades` | **Ícono:** `mycelialmind:latent_spore` | **Unlock:** `always`
- **Resumen:** Poción Awkward, 5 compostables NeoForge y ofertas Clérigo/Albañil

#### [Libro: Cap.9 / vanilla_brew_compost_trades / Pág.1] Integración Económica Vanilla+ (1/3)

• **Alambique (`onRegisterBrewingRecipes`):** Poción Rara (`Awkward`) + `latent_spore` -> `spore_antidote`; luego + Amatista -> `concentrated_antidote`.

#### [Libro: Cap.9 / vanilla_brew_compost_trades / Pág.2] Integración Económica Vanilla+ (2/3)

• **5 Compostables (`compostables.json`):** `mycelial_biomass` (**0.65**), `bioluminescent_mycelial_cap` (**0.65**), `spore_bulb` (**0.65**), `latent_spore` (**0.50**), `mycelial_tendrils` (**0.50**).

#### [Libro: Cap.9 / vanilla_brew_compost_trades / Pág.3] Integración Económica Vanilla+ (3/3)

• **Aldeanos y Errante:** Clérigos (N2–N5) comercian `mycelial_biomass`, `spore_antidote`, `latent_spore`, `concentrated_antidote`; Albañiles y Vendedor Errante ofrecen bloques miceliales y `truce_pheromone`.

<a id="cap9-botin_48_tablas"></a>
### 4. Las 50 Tablas de Botín

- **ID de Entrada:** `economia_datos/botin_48_tablas` | **Ícono:** `mycelialmind:hive_heart` | **Unlock:** `always`
- **Resumen:** 22 bloques, 10 cofres, 17 entidades y aldea + multiplicadores

#### [Libro: Cap.9 / botin_48_tablas / Pág.1] Botín Multi-Pool (50 Tablas) (1/9)

• **Bloques 1/4:** `blocks/antibody_cyst`, `blocks/bio_chitin_lattice`, `blocks/bioluminescent_mycelial_cap`, `blocks/chitin_pillar`, `blocks/creeping_mycelium`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.2] Botín Multi-Pool (50 Tablas) (2/9)

• **Bloques 2/4:** `blocks/hive_altar`, `blocks/hyphal_vein`, `blocks/incubator_node`, `blocks/luminescent_amber_vein`, `blocks/matriarch_altar`, `blocks/mycelial_arch_brick`, `blocks/mycelial_node`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.3] Botín Multi-Pool (50 Tablas) (3/9)

• **Bloques 3/4:** `blocks/mycelial_stone`, `blocks/mycelial_tendrils`, `blocks/purification_pylon`, `blocks/purification_turret`, `blocks/reinforced_bio_barrier`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.4] Botín Multi-Pool (50 Tablas) (4/9)

• **Bloques 4/4:** `blocks/sentinel_node`, `blocks/spore_bulb`, `blocks/spore_vent`, `blocks/symbiotic_node`, `blocks/synaptic_research_bench`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.5] Botín Multi-Pool (50 Tablas) (5/9)

• **Cofres 1/2:** `chests/outbreak_sealed_vault`, `chests/surface_hive_cache`, `chests/surface_hive_crypt`, `chests/surface_hive_gallery`, `chests/sanctum_spore_vault`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.6] Botín Multi-Pool (50 Tablas) (6/9)

• **Cofres 2/2:** `chests/sanctum_forbidden_lab`, `chests/sanctum_ossuary_crypt`, `chests/citadel_amber_chapel`, `chests/citadel_crimson_chapel`, `chests/citadel_royal_crypt`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.7] Botín Multi-Pool (50 Tablas) (7/9)

• **Entidades 1/3:** `entities/acid_spitter`, `entities/apex_matriarch`, `entities/burrower`, `entities/chitin_brute`, `entities/colony_warden`, `entities/goliath_carrier`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.8] Botín Multi-Pool (50 Tablas) (8/9)

• **Entidades 2/3:** `entities/hive_core`, `entities/leech_parasite`, `entities/mycelial_chimera`, `entities/mycelial_stalker`, `entities/purifier_drone`, `entities/pyro_shroom`.

#### [Libro: Cap.9 / botin_48_tablas / Pág.9] Botín Multi-Pool (50 Tablas) (9/9)

• **Entidades 3/3:** `entities/spore_architect`, `entities/spore_behemoth`, `entities/sporeling`, `entities/synapse_weaver`, `entities/synaptic_mimic` (escala con Saqueo/Rango/Cepa).
• **Aldea / Héroe:** `gameplay/hero_of_the_village/cleric_mycelial_gift` (regalo de Clérigo al defender aldeas).

---

## Capítulo 10: Mundo, Estructuras y Dimensión

_Overworld, Micelio Profundo (3 biomas), Santuario, Ciudadela y Homeostasis_ | Puerta recomendada: `always` | Ícono: `mycelialmind:rift_catalyst`

<a id="cap10-generacion_overworld_ruido"></a>
### 1. Brotes y Colmenas del Overworld

- **ID de Entrada:** `mundo_dimension/generacion_overworld_ruido` | **Ícono:** `mycelialmind:luminescent_amber_vein` | **Unlock:** `always`
- **Resumen:** MycelialOutbreak (56m), SurfaceHive (192m) y FastNoiseLite

#### [Libro: Cap.10 / generacion_overworld_ruido / Pág.1] Estructuras y Características Overworld (1/3)

• `mycelial_outbreak`: garra orgánica altura **9**, conectividad 6-vecinos estricta (0 flotantes), `MIN_OUTBREAK_SPACING = 56` y bóveda `outbreak_sealed_vault`.

#### [Libro: Cap.10 / generacion_overworld_ruido / Pág.2] Estructuras y Características Overworld (2/3)

• `surface_hive`: megacápsula radio **8**, altura **16**, `MIN_HIVE_SPACING = 192`, `MIN_NODE_CLEARANCE = 64`, cripta subterránea, galería superior, conductos sinápticos y cofres `surface_hive_cache`/`gallery`/`crypt`.

#### [Libro: Cap.10 / generacion_overworld_ruido / Pág.3] Estructuras y Características Overworld (3/3)

• **Motores (`hive_tier_rarity` / `FastNoiseLite`):** filtran rareza por Acto vía `StructureProgressionPolicy` usando ruido `FastNoiseLite` (`SIMPLEX_SMOOTH`, `CELLULAR`, `DOMAIN_WARP`) auditado con `WorldgenAuditEngine`.

<a id="cap10-micelio_profundo_y_biomas"></a>
### 2. Dimensión Micelio Profundo (3 Biomas)

- **ID de Entrada:** `mundo_dimension/micelio_profundo_y_biomas` | **Ícono:** `mycelialmind:rift_catalyst` | **Unlock:** `act_3`
- **Resumen:** Portal mycelial_rift, WfcCavernLayout y los 3 biomas

#### [Libro: Cap.10 / micelio_profundo_y_biomas / Pág.1] Dimensión y Topología WFC (1/3)

• **Dimensión `mycelialmind:deep_mycelium`:** accede abriendo `mycelial_rift` con `rift_catalyst` en un marco de `reinforced_bio_barrier`. Sincroniza Actos desde el Overworld al instante.

#### [Libro: Cap.10 / micelio_profundo_y_biomas / Pág.2] Dimensión y Topología WFC (2/3)

• **3 Biomas:** `deep_mycelium_caverns` (cavernas arteriales), `hive_heart_sanctum` (cámaras del santuario) y `latent_spore_forest` (bosques de esporas latentes). Disponen de audio/niebla ambiental propia.

#### [Libro: Cap.10 / micelio_profundo_y_biomas / Pág.3] Dimensión y Topología WFC (3/3)

• **`WfcCavernLayout` (`Tile`):** algoritmo Wave Function Collapse para cavernas conectadas sin callejones sellados.

<a id="cap10-megaestructuras_santuario_ciudadela"></a>
### 3. Santuario Jigsaw y Ciudadela Matriarca

- **ID de Entrada:** `mundo_dimension/megaestructuras_santuario_ciudadela` | **Ícono:** `mycelialmind:matriarch_altar` | **Unlock:** `deep_mycelium`
- **Resumen:** hive_sanctum (11 plantillas NBT) y matriarch_citadel

#### [Libro: Cap.10 / megaestructuras_santuario_ciudadela / Pág.1] Megaestructuras End-Game (1/3)

• **`hive_sanctum` (Jigsaw, 11 piezas `.nbt`):** `sanctum_atrium`, `forbidden_lab`, `ossuary_crypt`, `spore_vault`, `incubation_wing`, `ribbed_nave` y corredores con botín `sanctum_*`.

#### [Libro: Cap.10 / megaestructuras_santuario_ciudadela / Pág.2] Megaestructuras End-Game (2/3)

• **`matriarch_citadel` (`matriarch_citadel_piece`):** incluye capillas `AMBER` y `CRIMSON` (`citadel_amber_chapel`, `citadel_crimson_chapel`), Cripta Real (`citadel_royal_crypt`) y guardianes persistentes (`spore_behemoth` + 2 `colony_warden`).

#### [Libro: Cap.10 / megaestructuras_santuario_ciudadela / Pág.3] Megaestructuras End-Game (3/3)

• **`twisted_citadel` (`twisted_citadel_piece`, `architectural_accent`):** reflejo arquitectónico en Micelio Profundo con plantillas `.nbt` (`spire_tower`, `gallery_hall`, `vault_bunker`) adaptadas a tu paleta y proporción.

<a id="cap10-homeostasis_dimensional"></a>
### 4. Homeostasis Dimensional y Quistes

- **ID de Entrada:** `mundo_dimension/homeostasis_dimensional` | **Ícono:** `mycelialmind:antibody_cyst` | **Unlock:** `deep_mycelium`
- **Resumen:** 3 etapas STABLE/IRRITATED/IMMUNE_RESPONSE, Lluvia Ácida y Quistes

#### [Libro: Cap.10 / homeostasis_dimensional / Pág.1] Sistema Inmune de la Dimensión (1/2)

• **3 Etapas (`HomeostasisStage`):** `STABLE` (<40), `IRRITATED` (>=40) e `IMMUNE_RESPONSE` (>=70; tormenta >=85). Explosiones, abuso de Pilones y Fase 2 Matriarca (+20) sensibilizan cada chunk (`homeostasisSensitization` 0–3).

#### [Libro: Cap.10 / homeostasis_dimensional / Pág.2] Sistema Inmune de la Dimensión (2/2)

• **Lluvia Ácida y `antibody_cyst`:** en `IMMUNE_RESPONSE` bajo cielo abierto cae lluvia corrosiva (`homeostasisAcidRainDamage = 2.0`). Brotan quistes (máx. `64`/chunk); si cubren **>=4 caras** (`SMOTHER_FACE_THRESHOLD`) sofocan Pilones hasta cosecharlos.

<a id="cap10-ciclos_de_la_colmena"></a>
### 5. Protocolo de Ciclos y Mutadores

- **ID de Entrada:** `mundo_dimension/ciclos_de_la_colmena` | **Ícono:** `mycelialmind:matriarch_altar` | **Unlock:** `post_matriarch`
- **Resumen:** Rejugabilidad post-Matriarca con 1–3 mutadores y rango S+..C

#### [Libro: Cap.10 / ciclos_de_la_colmena / Pág.1] Protocolo de Ciclos y Mutadores (1/4)

• **Activación Post-Matriarca:** Tras derrotar a `apex_matriarch`, usa `matriarch_altar` sin ofrenda válida para abrir `CycleProtocolScreen`, elegir **1–3** de los **6 mutadores** e iniciar el **Ciclo N** (limpia remisión).

#### [Libro: Cap.10 / ciclos_de_la_colmena / Pág.2] Protocolo de Ciclos y Mutadores (2/4)

• **Mutadores I–III:** `hyper_adaptation` (+45% aprendizaje, umbrales x0.85, +35% pts), `perpetual_tide` (+50% marea y menor umbral final, +40% pts) y `acute_homeostasis` (+40% estrés inmune, +35% pts).

#### [Libro: Cap.10 / ciclos_de_la_colmena / Pág.3] Protocolo de Ciclos y Mutadores (3/4)

• **Mutadores IV–VI:** `synaptic_swarm` (+atacantes y élite en oleadas/escuadrones, +40% pts), `primordial_apex` (+25% HP/+15% daño de jefes y cooldown menor, +50% pts) y `hyphal_overload` (+35% presión hifal, +30% pts).

#### [Libro: Cap.10 / ciclos_de_la_colmena / Pág.4] Protocolo de Ciclos y Mutadores (4/4)

• **Cierre y Récords:** Al derrotar a `apex_matriarch` durante el Ciclo se calcula `1000 * mult + velocidad(<=500) - muertes*120` (mín. `250`) otorgando rango **S+**..**C**, récord y el engrama `overmind_cycle`.

---

## Capítulo 11: Secretos, Logros y Compat

_8 Secretos (Pistas + Spoilers), 30 Logros, 7 Redstone y 9 Compat_ | Puerta recomendada: `always` | Ícono: `mycelialmind:neural_echo_core`

<a id="cap11-secretos_pistas_seguras"></a>
### 1. Los 8 Secretos: Pistas de Campo

- **ID de Entrada:** `secretos_logros/secretos_pistas_seguras` | **Ícono:** `mycelialmind:bio_scanner` | **Unlock:** `secret_hint`
- **Resumen:** Indicios de investigación experimental y ecológica sin spoilers

#### [Libro: Cap.11 / secretos_pistas_seguras / Pág.1] Pistas de Descubrimiento (1–8) (1/3)

• **Secreto 1 (Acto II / Lab N2):** prueba combinar Muestra Genética, Biomasa y Amatista.
• **Secreto 2 (Lab):** experimenta con Antídoto de Esporas, Biomasa y Amatista.
• **Secreto 3 (Acto II / Lab N2):** combina Muestra Genética, Espora Latente y Núcleo Sináptico.

#### [Libro: Cap.11 / secretos_pistas_seguras / Pág.2] Pistas de Descubrimiento (1–8) (2/3)

• **Secreto 4 (Acto III / Lab N3):** prueba Antídoto de Esporas, Espora Latente y Núcleo Sináptico.
• **Secreto 5 (Mundo):** aplica un Antídoto directamente sobre un Respiradero tóxico (`spore_vent`).
• **Secreto 6 (Mundo):** interactúa con Amatista sobre Micelio Rastrero protegido por un Pilón.

#### [Libro: Cap.11 / secretos_pistas_seguras / Pág.3] Pistas de Descubrimiento (1–8) (3/3)

• **Secreto 7 (Campo):** escanea, extrae o derrota un espécimen Primordial bioluminiscente.
• **Secreto 8 (Campo):** pulsa el Bio-Escáner bajo brotes o fertiliza Sombreros bioluminiscentes.

<a id="cap11-secretos_revelaciones_spoiler"></a>
### 2. Los 8 Secretos: Revelación Exacta

- **ID de Entrada:** `secretos_logros/secretos_revelaciones_spoiler` | **Ícono:** `mycelialmind:neural_echo_core` | **Unlock:** `secret_spoiler`
- **Resumen:** [SPOILER] Fórmulas y disparadores exactos en código

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.1] [SPOILER] Soluciones Exactas (1–8) (1/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#1 `SECRET_EXP_SYNAPTIC_CORE`:** `GeneticSample` (con rasgo) + `MycelialBiomass` + `AmethystShard` en Acto II (`AWAKENED`) o Lab N2+ -> fabrica `synaptic_core` artificial.

</details>

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.2] [SPOILER] Soluciones Exactas (1–8) (2/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#2 `SECRET_EXP_TRUCE_PHEROMONE`:** `SporeAntidote` + `MycelialBiomass` + `AmethystShard` en Mesa -> genera **x2** `truce_pheromone` (**x3** en Lab N3).

</details>

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.3] [SPOILER] Soluciones Exactas (1–8) (3/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#3 `SECRET_EXP_VOLATILE_ENGRAM`:** `GeneticSample` (con rasgo) + `LatentSpore` + `SynapticCore` (Acto II o Lab N2+) -> sintetiza `volatile_engram`.

</details>

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.4] [SPOILER] Soluciones Exactas (1–8) (4/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#4 `SECRET_EXP_NEURAL_ECHO`:** `SporeAntidote` + `LatentSpore` + `SynapticCore` (Acto III `ADAPTIVE` o Lab N3) -> destila `neural_echo_core`.

</details>

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.5] [SPOILER] Soluciones Exactas (1–8) (5/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#5 `SECRET_WORLD_VENT_PURIFICATION`:** Clic con `spore_antidote` en `SporeVentBlock`.
• **#6 `SECRET_WORLD_AMBER_CRYSTALLIZATION`:** Amatista en `CreepingMycelium` bajo Pilón.

</details>

#### [Libro: Cap.11 / secretos_revelaciones_spoiler / Pág.6] [SPOILER] Soluciones Exactas (1–8) (6/6)

<details>
<summary>Ver revelación exacta (SPOILER)</summary>

• **#7 `SECRET_PRIMORDIAL_SPECIMEN`:** escanear, extraer o matar un mob Primordial.
• **#8 `SECRET_SEALED_BIO_VAULT`:** localizar `outbreak_sealed_vault` vía Bio-Escáner o fertilizar `BioluminescentMycelialCap`.

</details>

<a id="cap11-logros_30_advancements"></a>
### 3. Los 30 Logros (`advancement`)

- **ID de Entrada:** `secretos_logros/logros_30_advancements` | **Ícono:** `mycelialmind:overmind_crown` | **Unlock:** `always`
- **Resumen:** 25 logros de campaña y 5 desbloqueos automáticos de recetas

#### [Libro: Cap.11 / logros_30_advancements / Pág.1] Cadena Completa de Logros (1/4)

• **Acto I y Base (9):** `root`, `first_adaptation`, `genetic_sampler`, `sever_the_network`, `biotech_research`, `targeted_purge`, `chitin_bulwark`, `tier2_awakening`, `base_sentinel`.

#### [Libro: Cap.11 / logros_30_advancements / Pág.2] Cadena Completa de Logros (2/4)

• **Táctica y Biotecnología (9):** `bastion_tier3`, `purifier_drone_deployed`, `symbiotic_vessel`, `synaptic_graft_network`, `pavlovian_deceiver`, `chemical_diplomat`, `architect_bane`, `chimera_bioforge`, `hive_core_slayer`.

#### [Libro: Cap.11 / logros_30_advancements / Pág.3] Cadena Completa de Logros (3/4)

• **Endgame y Maestría (9):** `synaptic_mastery`, `tide_breaker`, `counteroffensive_master`, `enter_deep_mycelium`, `immune_homeostasis`, `overmind_ascendant`, `apex_matriarch_slayer`, `recovered_garden`, `recovered_route` (`discovery`).

#### [Libro: Cap.11 / logros_30_advancements / Pág.4] Cadena Completa de Logros (4/4)

• **Desbloqueos de Receta (5):** `recipes/unlock_biomass_recipes`, `recipes/unlock_chitin_recipes`, `recipes/unlock_hive_heart_recipes`, `recipes/unlock_latent_spore_recipes`, `recipes/unlock_synaptic_core_recipes`.

<a id="cap11-redstone_y_ecosistema_mods"></a>
### 4. Redstone (7) y Ecosistema de Mods

- **ID de Entrada:** `secretos_logros/redstone_y_ecosistema_mods` | **Ícono:** `mycelialmind:purification_turret` | **Unlock:** `always`
- **Resumen:** MycelialRedstoneTelemetry + SBL, gdx-AI, Jade, JEI, EMI, Iris, Modonomicon

#### [Libro: Cap.11 / redstone_y_ecosistema_mods / Pág.1] Telemetría Redstone y Compatibilidad (1/4)

• **Comparadores Redstone 1/2:** `antibody_cyst` (estrés chunk 1–15), `bioluminescent_mycelial_cap` y `mycelial_tendrils` (`MATURE`), `mycelial_node`/`symbiotic_node`.

#### [Libro: Cap.11 / redstone_y_ecosistema_mods / Pág.2] Telemetría Redstone y Compatibilidad (2/4)

• **Comparadores Redstone 2/2 (`MycelialRedstoneTelemetry`):** `purification_pylon` (estado/sofoco), `purification_turret` (tier/atasco) y `synaptic_research_bench` (entradas/salida).

#### [Libro: Cap.11 / redstone_y_ecosistema_mods / Pág.3] Telemetría Redstone y Compatibilidad (3/4)

• **Compat IA/Mundo:** **Vanilla+** (Trades/Brew/Compost/Loot), **SmartBrainLib** (`MycelialSblBridge`), **gdx-AI** (`HiveTacticalTree`) y **Jade** (`MycelialJadePlugin`).

#### [Libro: Cap.11 / redstone_y_ecosistema_mods / Pág.4] Telemetría Redstone y Compatibilidad (4/4)

• **Compat UI/Render:** **JEI** (`MycelialJeiPlugin`), **EMI** (`MycelialEmiPlugin`), **Iris** (`MycelialIrisCompat` PBR), **Modonomicon** (`MycelialModonomiconCompat`) y **AzureLib**.

<a id="cap11-engram_outbreak_signal"></a>
### 5. Engrama I: Señal de Brote

- **ID de Entrada:** `secretos_logros/engram_outbreak_signal` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:outbreak_signal`
- **Resumen:** Registro inicial del elemento field_engram (engram_id)

#### [Libro: Cap.11 / engram_outbreak_signal / Pág.1] Engrama I: Señal de Brote (1/2)

• **Bitácora de Campo:** Todo `field_engram` porta su identificador en `engram_id` y se absorbe con clic derecho directo al Códice sin ocupar inventario.

#### [Libro: Cap.11 / engram_outbreak_signal / Pág.2] Engrama I: Señal de Brote (2/2)

• **Lectura Táctica (Acto I):** Las bóvedas selladas de brotes guardan bio-muestras tempranas antes de que la Colmena aprenda tus daños dominantes.

<a id="cap11-engram_synaptic_bloom"></a>
### 6. Engrama II: Floración Sináptica

- **ID de Entrada:** `secretos_logros/engram_synaptic_bloom` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:synaptic_bloom`
- **Resumen:** Topología temprana de nodos y pilones

#### [Libro: Cap.11 / engram_synaptic_bloom / Pág.1] Engrama II: Floración Sináptica

• **Bitácora de Campo:** Los nodos relé multiplican por `1.5x` el aprendizaje en combate cercano y enlazan hasta `28` bloques entre sí.
• **Lectura Táctica (Acto I):** Interponiendo un `purification_pylon` cortas los enlaces activos y evitas la alerta en cascada antes de romper el nodo.

<a id="cap11-engram_chitin_resonance"></a>
### 7. Engrama III: Resonancia Quitina

- **ID de Entrada:** `secretos_logros/engram_chitin_resonance` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:chitin_resonance`
- **Resumen:** Eco estructural de hive_core

#### [Libro: Cap.11 / engram_chitin_resonance / Pág.1] Engrama III: Resonancia Quitina (1/2)

• **Bitácora de Campo:** Recuperado tras colapsar a `hive_core`. Sus placas quitinosas vibran con armaduras de rango `ELITE` y núcleos sinápticos.

#### [Libro: Cap.11 / engram_chitin_resonance / Pág.2] Engrama III: Resonancia Quitina (2/2)

• **Lectura Táctica (Acto II):** Asciende el banco a **Lab T2** con `synaptic_core` para desbloquear fórmulas experimentales y ahorrar biomasa.

<a id="cap11-engram_tide_pressure"></a>
### 8. Engrama IV: Presión de Marea

- **ID de Entrada:** `secretos_logros/engram_tide_pressure` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:tide_pressure`
- **Resumen:** Fractura multipartita de spore_behemoth

#### [Libro: Cap.11 / engram_tide_pressure / Pág.1] Engrama IV: Presión de Marea

• **Bitácora de Campo:** Extraído del colapso de `spore_behemoth`. Su caparazón posee cajas de impacto diferenciadas (`ARMORED_CARAPACE` y `WEAK_POINT`).
• **Lectura Táctica (Acto III):** Rompe el caparazón para provocar `STAGGERED`: el punto débil desciende físicamente y recibe `1.35x` de daño.

<a id="cap11-engram_mimic_deception"></a>
### 9. Engrama V: Engaño del Mímico

- **ID de Entrada:** `secretos_logros/engram_mimic_deception` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:mimic_deception`
- **Resumen:** Mimetismo adaptativo de synaptic_mimic

#### [Libro: Cap.11 / engram_mimic_deception / Pág.1] Engrama V: Engaño del Mímico

• **Bitácora de Campo:** Los `synaptic_mimic` copian tu patrón de daño dominante y alternan posturas para castigar ataques repetitivos.
• **Lectura Táctica (Acto III):** Alterna fuego, magia o `purifier_grenade`, o usa `false_engram_injector` para sesgar su lectura antes del choque.

<a id="cap11-engram_hyphal_memory"></a>
### 10. Engrama VI: Memoria Hifal

- **ID de Entrada:** `secretos_logros/engram_hyphal_memory` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:hyphal_memory`
- **Resumen:** Rutas micorrízicas y engramas de santuario

#### [Libro: Cap.11 / engram_hyphal_memory / Pág.1] Engrama VI: Memoria Hifal (1/2)

• **Bitácora de Campo:** Las rutas frecuentes cristalizan en `hyphal_vein` tras superar el umbral de tránsito y aceleran patrullas hacia bases activas.

#### [Libro: Cap.11 / engram_hyphal_memory / Pág.2] Engrama VI: Memoria Hifal (2/2)

• **Lectura Táctica (Acto II):** Reprograma hasta 4 nodos aliados con `synaptic_graft` o usa `neural_echo_decoy` para desviar asedios de ruta.

<a id="cap11-engram_homeostasis_trauma"></a>
### 11. Engrama VII: Trauma Inmune

- **ID de Entrada:** `secretos_logros/engram_homeostasis_trauma` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:homeostasis_trauma`
- **Resumen:** Sensibilización y quistes de deep_mycelium

#### [Libro: Cap.11 / engram_homeostasis_trauma / Pág.1] Engrama VII: Trauma Inmune

• **Bitácora de Campo:** En `deep_mycelium`, explosiones y exceso de pilones elevan el estrés hasta `IMMUNE_RESPONSE`, lluvia ácida y sensibilización `0–3`.
• **Lectura Táctica (Acto III):** Si `antibody_cyst` cubre **>=4 caras** de un pilón lo sofoca hasta despejarlo; sedar el chunk detiene patrullas.

<a id="cap11-engram_primordial_lineage"></a>
### 12. Engrama VIII: Linaje Primordial

- **ID de Entrada:** `secretos_logros/engram_primordial_lineage` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:primordial_lineage`
- **Resumen:** Extracción genética de variantes primordiales

#### [Libro: Cap.11 / engram_primordial_lineage / Pág.1] Engrama VIII: Linaje Primordial (1/2)

• **Bitácora de Campo:** Obtenido al usar `bio_extractor` sobre ejemplares primordiales debilitados (`<=60%` HP) portadores de esencias ancestrales.

#### [Libro: Cap.11 / engram_primordial_lineage / Pág.2] Engrama VIII: Linaje Primordial (2/2)

• **Lectura Táctica (Acto IV):** Asciende a **Lab T3** con `neural_echo_core` o `overmind_essence` para duplicar consumibles apilables y abrir Secreto #4.

<a id="cap11-engram_matriarch_requiem"></a>
### 13. Engrama IX: Réquiem Matriarcal

- **ID de Entrada:** `secretos_logros/engram_matriarch_requiem` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:matriarch_requiem`
- **Resumen:** Remisión dimensional y protocolo de enclave

#### [Libro: Cap.11 / engram_matriarch_requiem / Pág.1] Engrama IX: Réquiem Matriarcal (1/2)

• **Bitácora de Campo:** Sellado al caer `apex_matriarch`. La caída activa una ventana de remisión sináptica que preserva tu rango de Acto adquirido.

#### [Libro: Cap.11 / engram_matriarch_requiem / Pág.2] Engrama IX: Réquiem Matriarcal (2/2)

• **Lectura Táctica (Acto IV):** Con la Matriarca derrotada puedes usar `matriarch_altar` sin ofrenda para abrir el protocolo de Ciclos.

<a id="cap11-engram_overmind_cycle"></a>
### 14. Engrama X: Ciclo de la Supramente

- **ID de Entrada:** `secretos_logros/engram_overmind_cycle` | **Ícono:** `mycelialmind:field_engram` | **Unlock:** `engram:overmind_cycle`
- **Resumen:** Maestría post-Matriarca con mutadores activos

#### [Libro: Cap.11 / engram_overmind_cycle / Pág.1] Engrama X: Ciclo de la Supramente (1/2)

• **Bitácora de Campo:** Emitido al completar un **Ciclo de la Colmena** venciendo a `apex_matriarch` bajo mutadores tácticos en `CycleProtocolScreen`.

#### [Libro: Cap.11 / engram_overmind_cycle / Pág.2] Engrama X: Ciclo de la Supramente (2/2)

• **Lectura Táctica (Ciclos):** Combina hasta 3 mutadores de alto riesgo (`primordial_apex` + `perpetual_tide` + `synaptic_swarm`) y minimiza bajas para **S+**.

---

## Capítulo 12: Técnica, Config y Comandos

_181 Sonidos, LabPBR, 49 Configs, Red v14, /mycelial y 22 Verify_ | Puerta recomendada: `always` | Ícono: `mycelialmind:bio_scanner`

<a id="cap12-configuracion_cliente_servidor"></a>
### 1. Configuraciones (13 Cliente / 43 Servidor)

- **ID de Entrada:** `tecnica_comandos/configuracion_cliente_servidor` | **Ícono:** `mycelialmind:synaptic_research_bench` | **Unlock:** `always`
- **Resumen:** MycelialClientConfig, KeyBindings y MycelialConfig TOML completo

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.1] Claves de Configuración TOML (1/12)

• `[hud] hudAnchor` = `TOP_LEFT` (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`).
• `[hud] hudOffsetX` = `0` (`-200..200`); `hudOffsetY` = `0` (`-200..200`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.2] Claves de Configuración TOML (2/12)

• `[hud] hudCompactMode` = `false`; `hudContextualCombat` = `true`; `hudRequireHeld` = `true`; `showSynapticVisionBorder` = `true`.
• `[atmosphere] sporeFogDensityScale` = `1.0` (`0.25..1.0`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.3] Claves de Configuración TOML (3/12)

• `[audio] ambientSoundscapeVolume` = `1.0` (`0..1`); `hiveAdaptationAudioAlerts` = `true`; `caveSoundOcclusion` = `true`.
• `[particles]` `particleDensityScale` = `1.0` (`0.25..1.5`); `enableAdditiveParticleGlow` = `true`.

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.4] Claves de Configuración TOML (4/12)

• `[hive_memory]` `learningMultiplier`=`1.0` (`0.1..10`), `decayPerMinute`=`2.0` (`0..50`), `traitActivationThreshold`=`70.0` (`5..500`).
• `[hive_memory]` `tier2ActivationThreshold`=`200.0` (`25..500`), `tier2DecayMultiplier`=`1.75` (`1..5`), `synergiesEnabled`=`true`.

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.5] Claves de Configuración TOML (5/12)

• `[hive_memory]` `suppressorCooldownSeconds`=`120` (`15..600`), `projectileDeflectChance`=`0.30` (`0..0.95`), `symbiosisThreatMultiplierPerSlot`=`0.15` (`0..1`).
• `[hive_memory]` `maxMutagenSlots`=`2` (`1..5`), `counterSerumBleedRatio`=`0.50` (`0.1..0.95`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.6] Claves de Configuración TOML (6/12)

• `[territory]` `myceliumSpreadEnabled`=`true`, `myceliumSpreadChance`=`0.12` (`0..1`), `pylonPurificationRadius`=`24` (`4..64`), `pylonTier3ChunkLoadingEnabled`=`true`.

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.7] Claves de Configuración TOML (7/12)

• `[territory]` `synapticNodeLinkRadius`=`28` (`8..64`), `nodeDestructionStimulusBleed`=`18.0` (`0..100`), `burrowerBlockBreaking`=`true`.
• `[territory]` `playerCountScaling`=`0.15` (`0..1`), `wavesEnabled`=`true`, `waveCooldownTicks`=`3600` (`200..72000`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.8] Claves de Configuración TOML (8/12)

• `[territory]` `autonomousExpansionEnabled`=`true`, `aggressiveVillageAssimilation`=`true`, `maxAutonomousNodes`=`32` (`4..128`), `minNodeSpacing`=`18` (`8..48`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.9] Claves de Configuración TOML (9/12)

• `[territory]` `infectionConversionEnabled`=`true`, `fungalTideEnabled`=`true`, `fungalTideDurationTicks`=`6000` (`600..24000`).
• `[territory]` `fungalTideCooldownTicks`=`24000` (`1200..72000`), `mycorrhizalAcousticsEnabled`=`true`, `maxSymbioticNodes`=`4` (`1..16`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.10] Claves de Configuración TOML (10/12)

• `[territory]` `mycorrhizalTravelRange`=`96` (`16..256`), `mycorrhizalTravelCooldownTicks`=`1500` (`100..6000`).
• `[territory]` `progressiveThreatScaling`=`true`, `deathThreatPenalty`=`5.0` (`0..25`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.11] Claves de Configuración TOML (11/12)

• `[dimensional_homeostasis]` `homeostasisEnabled`=`true`, `homeostasisExplosionStressScale`=`1.0` (`0.1..5`), `homeostasisAcidRainDamage`=`2.0` (`0.5..10`).

#### [Libro: Cap.12 / configuracion_cliente_servidor / Pág.12] Claves de Configuración TOML (12/12)

• `[dimensional_homeostasis]` `homeostasisAntibodyEncapsulationEnabled`=`true`, `homeostasisMaxCystsPerChunk`=`64` (`8..256`).
• `[hyphal_network]` `rootTraversalThreshold`=`5` (`2..32`), `growthIntervalTicks`=`10` (`1..100`), `maxTrailPointsPerChunk`=`64` (`16..256`).

<a id="cap12-comandos_red_y_verificacion"></a>
### 2. Red v21, `/mycelial` y Verify

- **ID de Entrada:** `tecnica_comandos/comandos_red_y_verificacion` | **Ícono:** `mycelialmind:bio_scanner` | **Unlock:** `admin`
- **Resumen:** Payloads v21, /mycelial cycle, field_engram y verify cycles

#### [Libro: Cap.12 / comandos_red_y_verificacion / Pág.1] Comandos Admin, Payloads y Verify (1/5)

• **Red (`ModNetworking` v21):** `HiveSyncPayload` (`<=64` nodos, `<=32` pilones, `<=64` chunks inflamados), `SymbiosisSyncPayload`, `DiplomacySyncPayload`, `DiscoverySyncPayload`, `CycleProtocolSyncPayload` y `CycleProtocolActionPayload`.

#### [Libro: Cap.12 / comandos_red_y_verificacion / Pág.2] Comandos Admin, Payloads y Verify (2/5)

• **Engramas y Comandos (`perm 2`):** `field_engram` (`engram_id`), `/mycelial status`, `/mycelial reset`, `/mycelial stimulus <type> <0.1..500>`, `/mycelial habits`, `/mycelial schism <strain>`, `/mycelial diplomacy <strain> <-100..100>`.

#### [Libro: Cap.12 / comandos_red_y_verificacion / Pág.3] Comandos Admin, Payloads y Verify (3/5)

• **Simbiosis, Ciclos, Homeostasis y Auditoría:** `/mycelial symbiosis <status|clear|grant <mutagen>>`, `/mycelial cycle <status|start|abort|reset>`, `/mycelial homeostasis <status|stress <0..100>|sedate>`, `/mycelial worldgen audit [radius] [grid]`.

#### [Libro: Cap.12 / comandos_red_y_verificacion / Pág.4] Comandos Admin, Payloads y Verify (4/5)

• **`/mycelial verify <escenario|worldgen|all>` (1–11):** `projectile_adaptation`, `pylon_containment`, `boss_encounter`, `tier2_synergy`, `synaptic_network`, `symbiosis`, `units`, `expansion`, `tide`, `counteroffensive`, `arsenal`.

#### [Libro: Cap.12 / comandos_red_y_verificacion / Pág.5] Comandos Admin, Payloads y Verify (5/5)

• **`/mycelial verify` (12–23):** `mycorrhizal`, `hive_ai`, `chimera`, `memory`, `tactical_ai`, `field_ai`, `hyphal_siege`, `loot`, `homeostasis`, `polish`, `dimension_progression`, `cycles`.

---
