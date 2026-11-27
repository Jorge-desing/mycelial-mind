"""Chapters 1–4 of the Mycelial Mind Codex (Fundamentals, Progression, Memory, Tactical AI)."""

from __future__ import annotations

from .helpers import callout_page, chunk_paired_blocks, grid_xy, lt, spot_page, text_page
from .schema import CodexChapter, CodexEntry


def build_chapters_1_to_4() -> tuple[CodexChapter, ...]:
    # ==================================================================
    # CHAPTER 1: FUNDAMENTOS, DÍA 1, BIO-ESCÁNER, EFECTOS Y PARTÍCULAS
    # ==================================================================
    ch1_entries = (
        CodexEntry(
            id="identidad_organismica",
            order=1,
            title=lt("El Organismo Adaptativo", "The Adaptive Organism"),
            subtitle=lt(
                "Filosofía y arquitectura de Mycelial Mind v2.0.0",
                "Philosophy & architecture of Mycelial Mind v2.0.0",
            ),
            icon_item="mycelialmind:hive_heart",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=("mycelialmind:hive_heart", "DamageTelemetryTracker", "HiveMindSavedData"),
            pages=(
                text_page(
                    "intro_arquitectura",
                    "Una Mente Única y Evolutiva",
                    "One Living, Evolving Mind",
                    (
                        "**Mycelial Mind v2.0.0** (MC **1.21.1**, NeoForge **21.1.176**, Java **21**) sustituye "
                        "la dificultad estática por una colmena con memoria persistente (`HiveMindSavedData`).\n"
                        "Cada daño (`StimulusType`) y hábito (`PlayerHabit`) recalibran sus rasgos "
                        "(`AdaptationTrait`), sinergias, oleadas y red subterránea."
                    ),
                    (
                        "**Mycelial Mind v2.0.0** (MC **1.21.1**, NeoForge **21.1.176**, Java **21**) replaces "
                        "static difficulty with a persistent-memory hive (`HiveMindSavedData`).\n"
                        "Every damage stimulus (`StimulusType`) and tactical habit (`PlayerHabit`) recalibrates "
                        "traits (`AdaptationTrait`), synergies, raids, and subterranean nodes."
                    ),
                ),
                callout_page(
                    "intro_regla_oro",
                    "Regla de Oro: Varía tu Arsenal",
                    "Golden Rule: Vary Your Arsenal",
                    (
                        "Repetir un mismo tipo de ataque dispara el umbral adaptativo (**70.0** en Nivel 1 "
                        "y **200.0** efectivo en Nivel 2) y blinda al enjambre.\n"
                        "Alterna armas melee, proyectiles, fuego, explosivos y magia; purga Nodos "
                        "y sintetiza contramedidas en la Mesa Sináptica para drenar su memoria."
                    ),
                    (
                        "Repeating one damage type fills its threshold (**70.0** at Tier 1 and effective "
                        "**200.0** at Tier 2), hardening the swarm against that tactic.\n"
                        "Rotate melee, projectiles, fire, blasts, and magic; purge Nodes and craft "
                        "countermeasures at the Synaptic Bench to bleed Hive memory."
                    ),
                    accent_color="#9B4DFF",
                ),
            ),
        ),
        CodexEntry(
            id="supervivencia_dia_uno",
            order=2,
            title=lt("Manual de Día 1", "Day-1 Survival Protocol"),
            subtitle=lt(
                "Del primer brote de micelio a la Mesa Sináptica",
                "From your first mycelium patch to the Synaptic Bench",
            ),
            icon_item="mycelialmind:bio_extractor",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("identidad_organismica",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:creeping_mycelium",
                "mycelialmind:bio_extractor",
                "mycelialmind:bio_scanner",
                "mycelialmind:spore_antidote",
                "mycelialmind:synaptic_research_bench",
                "mycelialmind:purification_pylon",
            ),
            pages=(
                spot_page(
                    "dia1_p1",
                    "mycelialmind:creeping_mycelium",
                    "1. Detección y Muestreo",
                    "1. Detection & Sampling",
                    (
                        "Localiza `creeping_mycelium` y brotes.\n"
                        "Forja `bio_scanner` y `bio_extractor` para extraer `genetic_sample` codificadas."
                    ),
                    (
                        "Find `creeping_mycelium` outcrops.\n"
                        "Craft `bio_scanner` & `bio_extractor` to extract coded `genetic_sample` specimens."
                    ),
                ),
                spot_page(
                    "dia1_p2",
                    "mycelialmind:spore_antidote",
                    "2. Profilaxis y Contención",
                    "2. Prophylaxis & Containment",
                    (
                        "Prepara `spore_antidote` y rompe `mycelial_node`.\n"
                        "Al **25%** Amenaza, erige `purification_pylon` y `synaptic_research_bench`."
                    ),
                    (
                        "Brew `spore_antidote` & break `mycelial_node`.\n"
                        "At **25%** Threat, build `purification_pylon` & `synaptic_research_bench`."
                    ),
                ),
            ),
        ),
        CodexEntry(
            id="bio_scanner_telemetria",
            order=3,
            title=lt("Bio-Escáner y Visión Térmica", "Bio-Scanner & Thermal Vision"),
            subtitle=lt(
                "Tecla K, 5 pestañas, radar ±128m y códigos RGB",
                "Key K, 5 tabs, ±128m radar & wireframe RGB codes",
            ),
            icon_item="mycelialmind:bio_scanner",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("supervivencia_dia_uno",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:bio_scanner",
                "BioScannerCodexScreen",
                "BioScannerHudOverlay",
                "key.mycelialmind.open_codex",
                "key.mycelialmind.toggle_hud_compact",
            ),
            pages=(
                spot_page(
                    "scanner_tabs",
                    "mycelialmind:bio_scanner",
                    "Uso Activo y Códice (K)",
                    "Active Use & Codex (K)",
                    (
                        "Clic Derecho escanea mobs, nodos y aliados.\n"
                        "Pulsa **K** (`open_codex`) para abrir las **5 pestañas** y radar `±128m`."
                    ),
                    (
                        "Right-click scans mobs, nodes, and allies.\n"
                        "Press **K** (`open_codex`) to open all **5 tabs** and `±128m` radar."
                    ),
                ),
                *chunk_paired_blocks(
                    "scanner_thermal",
                    "Visión Sináptica Térmica",
                    "Synaptic Thermal Vision",
                    [
                        (
                            "• **Pestañas HUD (`BioScannerCodexScreen`):** Telemetría, Evolución, Territorio "
                            "(`±128m`), Diplomacia y Guía; atajo opcional `toggle_hud_compact` para modo compacto.",
                            "• **HUD & Tabs (`BioScannerCodexScreen`):** Telemetry, Evolutions, Territory "
                            "(`±128m`), Diplomacy & Guide; optional key `toggle_hud_compact` toggles compact HUD.",
                        ),
                        (
                            "• **Activación:** `Shift + Clic Derecho` aplica `synaptic_vision`; "
                            "detecta mobs a **32m** y bloques clave a **36m** tras muros.",
                            "• **Trigger:** `Shift + Right-Click` grants `synaptic_vision`; "
                            "reveals mobs at **32m** and key blocks at **36m** through walls.",
                        ),
                        (
                            "• **Entidades (32m):** Primordial = Lima-oro pulsante; Líder = Dorado; "
                            "Hostil = Rojo-naranja; Dron/Quimera/Jugador = Menta-cian.",
                            "• **Entities (32m):** Primordial = pulsing Lime-Gold; Leader = Gold; "
                            "Hostile = Red-Orange; Drone/Chimera/Player = Mint-Cyan.",
                        ),
                        (
                            "• **Nodos y Defensas (36m):** Nodo Simbiótico = Verde; Nodo Hostil = Púrpura; "
                            "Pilón/Torreta activa = Cian; Torreta atascada = Naranja.",
                            "• **Nodes & Defense (36m):** Symbiotic Node = Green; Hostile Node = Purple; "
                            "Pylon/Active Turret = Cyan; Jammed Turret = Orange.",
                        ),
                        (
                            "• **Amenazas Ocultas (36m):** Pilón Sofocado = Rojo; Quiste (`antibody_cyst`) "
                            "= Ámbar; Bóveda sellada sin abrir = Oro pulsante.",
                            "• **Hidden Hazards (36m):** Smothered Pylon = Red; Cyst (`antibody_cyst`) "
                            "= Amber; Sealed unopened Bio-Vault = pulsing Gold.",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="efectos_clinicos",
            order=4,
            title=lt("Efectos de Estado (4)", "Clinical Status Effects (4)"),
            subtitle=lt(
                "Los 4 ModEffects: infección, podredumbre, perspicacia y visión",
                "All 4 ModEffects: infection, rot, insight, and thermal vision",
            ),
            icon_item="mycelialmind:concentrated_antidote",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=("supervivencia_dia_uno",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:mycelial_infection",
                "mycelialmind:spore_rot",
                "mycelialmind:synaptic_insight",
                "mycelialmind:synaptic_vision",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "effects",
                    "Efectos Clínicos (ModEffects)",
                    "Clinical Effects (ModEffects)",
                    [
                        (
                            "• `mycelial_infection` (#7B2CBF, Perjudicial): aumenta el daño recibido de "
                            "`MycelialMob` en **+15%** por nivel (`1.0 + 0.15*(amp+1)`), bloquea Viaje "
                            "Micorrizal y convierte mobs/aldeanos cercanos; cada pieza de Quitina reduce "
                            "su duración un **20%**.",
                            "• `mycelial_infection` (#7B2CBF, Harmful): amplifies incoming damage from "
                            "`MycelialMob` by **+15%** per level (`1.0 + 0.15*(amp+1)`), blocks Fast Travel, "
                            "and converts nearby victims; each Chitin armor piece cuts duration **20%**.",
                        ),
                        (
                            "• `spore_rot` (#9B2226, Perjudicial): contra-adaptación del hábito "
                            "`COMBAT_MEDIC` y contacto con `antibody_cyst`; reduce **toda curación en -30%** "
                            "(`* 0.70F`). Purga con `concentrated_antidote`.",
                            "• `spore_rot` (#9B2226, Harmful): counter-adaptation from `COMBAT_MEDIC` "
                            "habit and `antibody_cyst` contact; cuts **all healing by -30%** (`* 0.70F`). "
                            "Cleanse with `concentrated_antidote`.",
                        ),
                        (
                            "• `synaptic_insight` (#38B000, Beneficioso): lectura táctica obtenida al "
                            "absorber `volatile_engram`, purgar Nodos o desplegar `neural_echo_decoy`; "
                            "concede **+15%** de daño adicional contra unidades de la Colmena.",
                            "• `synaptic_insight` (#38B000, Beneficial): tactical clarity gained from "
                            "`volatile_engram` consumption, Node purges, or `neural_echo_decoy`; grants "
                            "**+15%** bonus damage against Hive entities.",
                        ),
                        (
                            "• `synaptic_vision` (#00F5D4, Beneficioso): visión térmica por paredes (**32m** "
                            "entidades, **36m** nodos/pilones/torretas/quistes/bóvedas) con borde HUD cian.",
                            "• `synaptic_vision` (#00F5D4, Beneficial): through-wall thermal overlay (**32m** "
                            "entities, **36m** nodes/pylons/turrets/cysts/vaults) with cyan HUD border.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="atlas_particulas",
            order=5,
            title=lt("Atlas de Partículas (34)", "Particle Atlas (34)"),
            subtitle=lt(
                "Telemetría visual de los 34 ModParticles, cepas y presupuestos",
                "Visual telemetry across all 34 ModParticles, strains & budgets",
            ),
            icon_item="mycelialmind:volatile_engram",
            x=grid_xy(4, 3)[0],
            y=grid_xy(4, 3)[1],
            parents=("efectos_clinicos",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:infection",
                "mycelialmind:rot",
                "mycelialmind:insight",
                "mycelialmind:vision",
                "mycelialmind:recovery",
                "mycelialmind:purge",
                "mycelialmind:scan",
                "mycelialmind:serum_melee",
                "mycelialmind:serum_projectile",
                "mycelialmind:serum_fire",
                "mycelialmind:serum_explosion",
                "mycelialmind:serum_magic",
                "mycelialmind:engram",
                "mycelialmind:impact",
                "mycelialmind:trail",
                "mycelialmind:spore_mote",
                "mycelialmind:spore_cloud",
                "mycelialmind:spore_fall",
                "mycelialmind:bio_lume",
                "mycelialmind:amber_glint",
                "mycelialmind:chitin_shard",
                "mycelialmind:acid_droplet",
                "mycelialmind:acid_splash",
                "mycelialmind:necrotic_mist",
                "mycelialmind:synapse_spark",
                "mycelialmind:hive_pulse",
                "mycelialmind:psychic_wave",
                "mycelialmind:tide_mote",
                "mycelialmind:hive_ember",
                "mycelialmind:purifier_cross",
                "mycelialmind:thermal_reticle",
                "mycelialmind:histamine_pulse",
                "mycelialmind:hive_dust",
                "mycelialmind:desync_fragment",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "particles",
                    "Partículas Personalizadas (34)",
                    "Custom Particles (34)",
                    [
                        (
                            "• **Estados clínicos:** `infection` (racimo violeta con cilios), `rot` "
                            "(gota necrótica umber), `insight` (cristal esmeralda), `vision` (iris térmico), "
                            "`recovery` (corazones verde-lima giratorios), `purge` (cruz clínica) y `scan` (chevrón de barrido).",
                            "• **Clinical states:** `infection` (violet ciliated cluster), `rot` (umber necrotic "
                            "droplet), `insight` (emerald crystal), `vision` (thermal iris), `recovery` "
                            "(rotating lime hearts), `purge` (clinical cross) and `scan` (sweep chevron).",
                        ),
                        (
                            "• **Sueros y telemetría:** `serum_melee`, `serum_projectile`, `serum_fire`, "
                            "`serum_explosion` y `serum_magic` (tajo, dardo, ascua, onda y glifo); `engram` "
                            "(hexágono ámbar), `impact` (estrella menta) y `trail` (espiral).",
                            "• **Serums & telemetry:** `serum_melee`, `serum_projectile`, `serum_fire`, "
                            "`serum_explosion`, `serum_magic` (slash, dart, ember, wave, glyph); `engram` "
                            "(amber hexagon), `impact` (mint star) and `trail` (spiral).",
                        ),
                        (
                            "• **Colmena y mundo:** `spore_mote`, `spore_cloud` (bruma 64px), `spore_fall`, "
                            "`bio_lume`, `amber_glint`, `chitin_shard`, `acid_droplet`, `acid_splash` y "
                            "`necrotic_mist`.",
                            "• **Hive & world:** `spore_mote`, `spore_cloud` (64px mist), `spore_fall`, "
                            "`bio_lume`, `amber_glint`, `chitin_shard`, `acid_droplet`, `acid_splash` and "
                            "`necrotic_mist`.",
                        ),
                        (
                            "• **Red neural:** `synapse_spark`, `hive_pulse` (anillo rúnico), `psychic_wave`, "
                            "`tide_mote`, `hive_ember`, `purifier_cross`, `thermal_reticle` (retícula 64px), "
                            "`histamine_pulse`, `hive_dust` (polvo tintable) y `desync_fragment`.",
                            "• **Neural web:** `synapse_spark`, `hive_pulse` (rune ring), `psychic_wave`, "
                            "`tide_mote`, `hive_ember`, `purifier_cross`, `thermal_reticle` (64px reticle), "
                            "`histamine_pulse`, `hive_dust` (tintable dust) and `desync_fragment`.",
                        ),
                        (
                            "• **Presupuesto y escala:** máximo 24 fragmentos por emisión y 48 por escena "
                            "(64 en jefes); tamaño 100 %→80 %, reducción monótona cerca de cámara y nada de "
                            "crecimiento posterior.",
                            "• **Budget & scale:** at most 24 fragments per emission and 48 per scene "
                            "(64 for bosses); size 100%→80%, monotonic near-camera shrink and no regrowth.",
                        ),
                        (
                            "• **Render y cepas:** glow aditivo propio (desactivable en `[particles]`) y "
                            "`hive_dust` tintado por cepa (`#9B4DFF`, `#F2A93B`, `#E03C5C`).",
                            "• **Render & strains:** custom additive glow (toggleable in `[particles]`) and "
                            "`hive_dust` tinted per strain (`#9B4DFF`, `#F2A93B`, `#E03C5C`).",
                        ),
                    ],
                )
            ),
        ),
    )
    ch1 = CodexChapter(
        id="fundamentos",
        number=1,
        title=lt("Fundamentos y Día 1", "Fundamentals & Day 1"),
        subtitle=lt(
            "Inicio de partida, Bio-Escáner, 4 efectos y 34 partículas",
            "Opening loop, Bio-Scanner, 4 clinical effects & 34 particles",
        ),
        icon_item="mycelialmind:bio_scanner",
        entries=ch1_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 2: LOS 4 ACTOS, OLEADAS, METAS, MAREA FÚNGICA Y REMISIÓN
    # ==================================================================
    ch2_entries = (
        CodexEntry(
            id="actos_y_amenaza",
            order=1,
            title=lt("Los 4 Actos y Amenaza Global", "The 4 Acts & Global Threat"),
            subtitle=lt(
                "HiveProgressionTier: DORMANT, AWAKENED, ADAPTIVE y OVERMIND",
                "HiveProgressionTier: DORMANT, AWAKENED, ADAPTIVE & OVERMIND",
            ),
            icon_item="mycelialmind:synaptic_core",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "HiveProgressionTier",
                "DORMANT",
                "AWAKENED",
                "ADAPTIVE",
                "OVERMIND",
                "SURFACE_DAY_SPAWN_THREAT_THRESHOLD",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "tiers",
                    "Escalado por Acto (0–100%)",
                    "Act Progression Scaling (0–100%)",
                    [
                        (
                            "• **Acto I `DORMANT` (0–25%):** HP **x1.00**, Daño **x1.00**, **0.0 XP**/int., "
                            "Vel. **x1.00**, Mutación **0%**, Decay Bioma **0.85**.",
                            "• **Act I `DORMANT` (0–25%):** HP **x1.00**, Dmg **x1.00**, **0.0 XP**/int., "
                            "Speed **x1.00**, Mutation **0%**, Biome Decay **0.85**.",
                        ),
                        (
                            "• **Acto II `AWAKENED` (25–50%):** HP **x1.15**, Daño **x1.10**, **1.0 XP**/int., "
                            "Vel. **x1.03**, Mutación **5%**, Decay **1.00**. Activa asedios y síntesis N2.",
                            "• **Act II `AWAKENED` (25–50%):** HP **x1.15**, Dmg **x1.10**, **1.0 XP**/int., "
                            "Speed **x1.03**, Mutation **5%**, Decay **1.00**. Unlocks raids & Lab T2.",
                        ),
                        (
                            "• **Acto III `ADAPTIVE` (50–75%):** HP **x1.30**, Daño **x1.22**, **2.5 XP**/int., "
                            "Vel. **x1.06**, Mutación **12%**, Decay **1.15**. Desde **50.0%** "
                            "(`SURFACE_DAY_SPAWN_THREAT_THRESHOLD`) brotan de día en superficie.",
                            "• **Act III `ADAPTIVE` (50–75%):** HP **x1.30**, Dmg **x1.22**, **2.5 XP**/int., "
                            "Speed **x1.06**, Mutation **12%**, Decay **1.15**. At **50.0%** "
                            "(`SURFACE_DAY_SPAWN_THREAT_THRESHOLD`) daylight surface spawns unlock.",
                        ),
                        (
                            "• **Acto IV `OVERMIND` (>=75%):** HP **x1.50**, Daño **x1.35**, **4.0 XP**/int., "
                            "Vel. **x1.10**, Mutación **20%**, Decay **1.30**. Escala con `playerCountScaling` "
                            "(**+0.15**/jugador), `deathThreatPenalty` (**+5.0%**) y se sincroniza al Micelio Profundo.",
                            "• **Act IV `OVERMIND` (>=75%):** HP **x1.50**, Dmg **x1.35**, **4.0 XP**/int., "
                            "Speed **x1.10**, Mutation **20%**, Decay **1.30**. Scales via `playerCountScaling` "
                            "(**+0.15**/player), `deathThreatPenalty` (**+5.0%**), syncing Overworld & Deep Mycelium.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="motor_metas_progresion",
            order=2,
            title=lt("Motor de Metas y Maestría", "Progression Goal Engine"),
            subtitle=lt(
                "ShortTermGoal (6), MidTermGoal (5) y pilares a largo plazo",
                "ShortTermGoal (6), MidTermGoal (5) & Long-Term Mastery",
            ),
            icon_item="mycelialmind:overmind_essence",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("actos_y_amenaza",),
            unlock_gate="always",
            registry_refs=(
                "ProgressionGoalEngine",
                "ShortTermGoal",
                "MidTermGoal",
                "MILESTONE_BIO_SCANNER",
                "MILESTONE_SYNAPTIC_BENCH",
                "MILESTONE_BIO_EXTRACTOR",
                "MILESTONE_SYNTHESIZED_BIOTECH",
                "MILESTONE_STATION_UPGRADED",
                "MILESTONE_CHITIN_EVOLVED",
                "MILESTONE_HIVE_CORE_DEFEATED",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "goals",
                    "Metas de Progresión",
                    "Progression Goals",
                    [
                        (
                            "• **6 Metas a Corto Plazo (`ShortTermGoal`):** `CRAFT_SCANNER`, "
                            "`BUILD_RESEARCH_BENCH`, `EXTRACT_GENETIC_SAMPLE`, `SYNTHESIZE_BIOTECH`, "
                            "`UPGRADE_GEAR_OR_STATION`, `MASTER_EXPERIMENTS`.",
                            "• **6 Short-Term Goals (`ShortTermGoal`):** `CRAFT_SCANNER`, "
                            "`BUILD_RESEARCH_BENCH`, `EXTRACT_GENETIC_SAMPLE`, `SYNTHESIZE_BIOTECH`, "
                            "`UPGRADE_GEAR_OR_STATION`, `MASTER_EXPERIMENTS`.",
                        ),
                        (
                            "• **5 Metas a Medio Plazo (`MidTermGoal`):** `ESTABLISH_PERIMETER` (Pilón), "
                            "`CONQUER_HIVE_SANCTUM` (Hive Heart), `BREACH_DEEP_MYCELIUM` (Grieta), "
                            "`OVERCOME_TIDE_AND_MATRIARCH`, `MAINTAIN_DOMINION`.",
                            "• **5 Mid-Term Goals (`MidTermGoal`):** `ESTABLISH_PERIMETER` (Pylon), "
                            "`CONQUER_HIVE_SANCTUM` (Hive Heart), `BREACH_DEEP_MYCELIUM` (Rift), "
                            "`OVERCOME_TIDE_AND_MATRIARCH`, `MAINTAIN_DOMINION`.",
                        ),
                        (
                            "• **7 Hitos (`MILESTONE_*`):** Bio-Escáner, Mesa Sináptica, Bio-Extractor, "
                            "Síntesis Biotech, Mejora Estación, Quitina Evolucionada y Núcleo Derrotado.\n"
                            "• **Maestría Larga:** **17/17** especies, **5/5** rasgos, **8/8** secretos, "
                            "**3/3** cepas, Primordiales y **BastionScore 100/100**.",
                            "• **7 Milestones (`MILESTONE_*`):** Bio-Scanner, Synaptic Bench, Bio-Extractor, "
                            "Synthesized Biotech, Upgraded Station, Evolved Chitin, Hive Core Defeated.\n"
                            "• **Long-Term Mastery:** **17/17** species, **5/5** traits, **8/8** secrets, "
                            "**3/3** strains, Primordials & **100/100 BastionScore**.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="oleadas_marea_remision",
            order=3,
            title=lt("Oleadas, Marea Fúngica y Remisión", "Raids, Fungal Tide & Remission"),
            subtitle=lt(
                "Hitos 25/50/75/100%, Marea >=75% y Remisión de 20 min",
                "25/50/75/100% raids, >=75% Fungal Tide & 20m Remission",
            ),
            icon_item="mycelialmind:tide_beacon",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("actos_y_amenaza",),
            unlock_gate="act_2",
            registry_refs=(
                "HiveWaveDirector",
                "mycelialmind:tide_beacon",
                "MATRIARCH_REMISSION_DURATION_TICKS",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "waves_tide",
                    "Oleadas, Marea y Remisión",
                    "Raids, Tide & Remission",
                    [
                        (
                            "• **Oleadas (`HiveWaveDirector`):** evalúa cada **100 ticks** los hitos "
                            "**25%, 50%, 75% y 100%** (enfriamiento `waveCooldownTicks = 3600t` = 3 min). "
                            "Las zonas bajo `PurificationZoneTracker` desvían el epicentro del asedio.",
                            "• **Raids (`HiveWaveDirector`):** checks every **100 ticks** at **25%, 50%, "
                            "75%, and 100%** (`waveCooldownTicks = 3600t` = 3 min). Protected zones under "
                            "`PurificationZoneTracker` repel direct raid spawns.",
                        ),
                        (
                            "• **Marea Fúngica:** estalla al alcanzar **>=75%** Amenaza (`fungalTideDurationTicks "
                            "= 6000t` = 5 min; cooldown `24000t` = 20 min). Invoca un `spore_behemoth` cuyo "
                            "botín da `tide_beacon`; suena `fungal_tide_horn`.",
                            "• **Fungal Tide:** erupts at **>=75%** Threat (`fungalTideDurationTicks = 6000t` "
                            "= 5 min; cooldown `24000t` = 20 min). Spawns a `spore_behemoth` dropping "
                            "`tide_beacon`; broadcasts `fungal_tide_horn`.",
                        ),
                        (
                            "• **Remisión de Matriarca:** derrotar a `apex_matriarch` abre **20 minutos** "
                            "(`24000t`) de remisión global: atenúa la presión de Amenaza, calma nodos "
                            "y reduce el estrés homeostático tras conquistar el Acto IV.",
                            "• **Matriarch Remission:** slaying `apex_matriarch` opens a **20-minute** "
                            "(`24000t`) global remission: dampens Threat escalation, calms nodes, "
                            "and eases dimensional homeostasis stress after Act IV.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch2 = CodexChapter(
        id="progresion",
        number=2,
        title=lt("Progresión, Actos y Oleadas", "Progression, Acts & Waves"),
        subtitle=lt(
            "4 Actos, ProgressionGoalEngine, Marea Fúngica y Remisión",
            "4 Acts, ProgressionGoalEngine, Fungal Tide & Remission",
        ),
        icon_item="mycelialmind:synaptic_core",
        entries=ch2_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 3: MEMORIA INMUNE, RASGOS, SINERGIAS, HÁBITOS Y ENGAÑO
    # ==================================================================
    ch3_entries = (
        CodexEntry(
            id="enrutamiento_memoria",
            order=1,
            title=lt("Memoria Inmune y Enrutador", "Immune Memory & Router"),
            subtitle=lt(
                "40% directo / 60% búfer nodal (48m) con consolidación en 30s",
                "40% direct / 60% node buffer (48m) consolidating in 30s",
            ),
            icon_item="mycelialmind:volatile_engram",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=("SynapticMemoryRouter", "mycelialmind:volatile_engram"),
            pages=tuple(
                chunk_paired_blocks(
                    "memory_router",
                    "Enrutamiento Sináptico",
                    "Synaptic Memory Routing",
                    [
                        (
                            "• **División 40/60 (`SynapticMemoryRouter`):** si existe un Nodo hostil a "
                            "**<=48m** (`BUFFER_NODE_RANGE`), solo el **40%** del estímulo sube directo a la "
                            "Colmena y el **60%** queda en búfer volátil del nodo.",
                            "• **40/60 Split (`SynapticMemoryRouter`):** if a hostile Node lies within "
                            "**48m** (`BUFFER_NODE_RANGE`), only **40%** enters global memory directly while "
                            "**60%** stays in the node's volatile buffer.",
                        ),
                        (
                            "• **Consolidación vs Purga:** tras **600 ticks** (**30s**) el búfer se consolida "
                            "en la red. Romper o purgar el Nodo antes disipa el búfer sin consolidar, "
                            "dropea `volatile_engram` y sangra `nodeDestructionStimulusBleed` (**18.0**).",
                            "• **Consolidation vs Purge:** after **600 ticks** (**30s**) the buffer commits "
                            "globally. Shattering the Node earlier voids uncommitted data, drops "
                            "`volatile_engram`, and bleeds `nodeDestructionStimulusBleed` (**18.0**).",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="estimulos_y_rasgos",
            order=2,
            title=lt("Estímulos (5) y Rasgos (5)", "Stimuli (5) & Adaptation Traits (5)"),
            subtitle=lt(
                "StimulusType -> AdaptationTrait Nivel 1 (70) y Nivel 2 (200)",
                "StimulusType -> AdaptationTrait Tier 1 (70) & Tier 2 (200)",
            ),
            icon_item="mycelialmind:genetic_sample",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("enrutamiento_memoria",),
            unlock_gate="always",
            registry_refs=(
                "StimulusType",
                "MELEE",
                "PROJECTILE",
                "FIRE",
                "EXPLOSION",
                "MAGIC",
                "AdaptationTrait",
                "MELEE_THORNS",
                "PROJECTILE_DEFLECT",
                "FIRE_RESISTANT",
                "BLAST_SHIELDED",
                "MAGIC_CARAPACE",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "traits_map",
                    "5 Estímulos y 5 Rasgos",
                    "5 Stimuli & 5 Adaptation Traits",
                    [
                        (
                            "• **MELEE -> `MELEE_THORNS`** (N1 >= **70**, N2 `flanking_swarm` >= **200**): "
                            "refleja castigo cuerpo a cuerpo y coordina flanqueos en masa.",
                            "• **MELEE -> `MELEE_THORNS`** (T1 >= **70**, T2 `flanking_swarm` >= **200**): "
                            "punishes melee strikes with thorns and triggers pack flanking.",
                        ),
                        (
                            "• **PROJECTILE -> `PROJECTILE_DEFLECT`** (N2 `evasive_strafe`): prob. de desvío "
                            "(`projectileDeflectChance = 0.30`) y maniobras laterales evasivas.",
                            "• **PROJECTILE -> `PROJECTILE_DEFLECT`** (T2 `evasive_strafe`): deflection "
                            "chance (`projectileDeflectChance = 0.30`) plus evasive strafing.",
                        ),
                        (
                            "• **FIRE -> `FIRE_RESISTANT`** (N2 `pyro_surge`): inmunidad/mitigación térmica "
                            "y asaltos agresivos a través de fuego.",
                            "• **FIRE -> `FIRE_RESISTANT`** (T2 `pyro_surge`): thermal resilience and "
                            "aggressive pyro-surge advances.",
                        ),
                        (
                            "• **EXPLOSION -> `BLAST_SHIELDED`** (N2 `phalanx_breach`): eleva armadura base "
                            "mínima a **8.0** y acelera la brecha de muros (`Burrower` cada **18t**).",
                            "• **EXPLOSION -> `BLAST_SHIELDED`** (T2 `phalanx_breach`): raises base armor "
                            "floor to **8.0** and speeds siege breaching (`Burrower` every **18t**).",
                        ),
                        (
                            "• **MAGIC -> `MAGIC_CARAPACE`** (N2 `psychic_disrupt`): amortigua daño arcano/veneno "
                            "y disrumpe al conjurador. Decaimiento: **2.0/min** (x**1.75** en N2).",
                            "• **MAGIC -> `MAGIC_CARAPACE`** (T2 `psychic_disrupt`): dampens magic/potion bursts "
                            "and disrupts casters. Decay: **2.0/min** (x**1.75** in Tier 2).",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="sinergias_evolutivas",
            order=3,
            title=lt("Las 5 Sinergias (`HiveSynergy`)", "The 5 Hive Synergies"),
            subtitle=lt(
                "Combinaciones compuestas al coincidir pares de rasgos",
                "Compound evolutions unlocked when paired traits align",
            ),
            icon_item="mycelialmind:hive_altar",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("estimulos_y_rasgos",),
            unlock_gate="act_2",
            registry_refs=(
                "HiveSynergy",
                "PYROLYTIC_SHELL",
                "VENOMOUS_COUNTER",
                "KINETIC_MIRROR",
                "ARCANE_ABSORPTION",
                "SIEGE_JUGGERNAUT",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "synergies",
                    "Sinergias Compuestas (5)",
                    "Compound Synergies (5)",
                    [
                        (
                            "• `PYROLYTIC_SHELL` (`FIRE_RESISTANT` + `BLAST_SHIELDED`): caparazón pirolítico "
                            "que emite pulsos ígneos reactivos al recibir presión sostenida.",
                            "• `PYROLYTIC_SHELL` (`FIRE_RESISTANT` + `BLAST_SHIELDED`): pyrolytic shell "
                            "discharging reactive flame pulses under sustained pressure.",
                        ),
                        (
                            "• `VENOMOUS_COUNTER` (`MELEE_THORNS` + `MAGIC_CARAPACE`): reparte réplicas "
                            "tóxicas e infecciosas ante castigo cuerpo a cuerpo o arcano.",
                            "• `VENOMOUS_COUNTER` (`MELEE_THORNS` + `MAGIC_CARAPACE`): inflicts toxic "
                            "infection counters against melee and arcane strikes.",
                        ),
                        (
                            "• `KINETIC_MIRROR` (`PROJECTILE_DEFLECT` + `MELEE_THORNS`): dispara fragmentos "
                            "de ácido (`AcidSpitProjectile`) hacia el atacante tras recibir impacto.",
                            "• `KINETIC_MIRROR` (`PROJECTILE_DEFLECT` + `MELEE_THORNS`): fires reflective "
                            "acid shard fragments (`AcidSpitProjectile`) back at attackers.",
                        ),
                        (
                            "• `ARCANE_ABSORPTION` (`MAGIC_CARAPACE` + `FIRE_RESISTANT`): mitiga conjuros, "
                            "pociones y calor elemental mientras sostiene curación en combate.",
                            "• `ARCANE_ABSORPTION` (`MAGIC_CARAPACE` + `FIRE_RESISTANT`): dampens magic, "
                            "potions, and fire while sustaining combat vitality.",
                        ),
                        (
                            "• `SIEGE_JUGGERNAUT` (`BLAST_SHIELDED` + `PROJECTILE_DEFLECT`): garantiza "
                            "**>=0.75** resistencia al empuje y potencia ruptura de fortificaciones (`18t`).",
                            "• `SIEGE_JUGGERNAUT` (`BLAST_SHIELDED` + `PROJECTILE_DEFLECT`): grants "
                            "**>=0.75** knockback resistance floor and turbo wall breaching (`18t`).",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="habitos_telemetria",
            order=4,
            title=lt("Los 4 Hábitos del Jugador", "The 4 Player Habits"),
            subtitle=lt(
                "HabitTelemetryTracker: contra-adaptaciones desde 60.0",
                "HabitTelemetryTracker: Vanilla+ counter-adaptations at 60.0",
            ),
            icon_item="mycelialmind:antibody_cyst",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=("enrutamiento_memoria",),
            unlock_gate="act_2",
            registry_refs=(
                "PlayerHabit",
                "HabitTelemetryTracker",
                "COMBAT_MEDIC",
                "HIGH_MOBILITY",
                "BASTION_BUILDER",
                "SHADOW_INFILTRATOR",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "habits",
                    "Hábitos Tácticos (>=60 / 500)",
                    "Tactical Habits (>=60 / 500)",
                    [
                        (
                            "• `COMBAT_MEDIC`: curarte >=1.5 HP en combate (<16m) acumula hábito; a >=**60.0** "
                            "aplica `spore_rot` reduciendo curaciones un **-30%** (`* 0.70F`).",
                            "• `COMBAT_MEDIC`: healing >=1.5 HP near Hive mobs (<16m) charges this habit; at "
                            ">=**60.0** triggers `spore_rot`, cutting healing by **-30%** (`* 0.70F`).",
                        ),
                        (
                            "• `HIGH_MOBILITY`: kiting/sprint constante provoca emboscadas rápidas y ajuste "
                            "vectorial de flanco (`Stalker`, `SwarmFlankGoal`).",
                            "• `HIGH_MOBILITY`: sustained kiting/sprinting provokes rapid flanking intercepts "
                            "(`Stalker`, `SwarmFlankGoal`).",
                        ),
                        (
                            "• `BASTION_BUILDER`: levantar muros, torretas o mejoras de laboratorio despierta "
                            "prioridad zapadora (`Burrower`, `SporeArchitect`, carga de asedio).",
                            "• `BASTION_BUILDER`: building walls, turrets, or bench tiers escalates sapper "
                            "priority (`Burrower`, `SporeArchitect`, siege charges).",
                        ),
                        (
                            "• `SHADOW_INFILTRATOR`: abuso de invisibilidad/agacharse activa detección sináptica "
                            "de escuadrón. Rota tu estilo para enfriar cada marcador.",
                            "• `SHADOW_INFILTRATOR`: heavy stealth/crouching triggers synaptic sweep reveals. "
                            "Vary tactics to let habit gauges cool down.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="engano_pavloviano",
            order=5,
            title=lt("Engaño Pavloviano y Contramedidas", "Pavlovian Deception & Purges"),
            subtitle=lt(
                "FalseEngramInjector, CounterSerum y SynapticSuppressor",
                "FalseEngramInjector, CounterSerum & SynapticSuppressor",
            ),
            icon_item="mycelialmind:false_engram_injector",
            x=grid_xy(4, 3)[0],
            y=grid_xy(4, 3)[1],
            parents=("estimulos_y_rasgos", "habitos_telemetria"),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:false_engram_injector",
                "mycelialmind:counter_serum",
                "mycelialmind:synaptic_suppressor",
                "DECEPTION_DURATION_TICKS",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "deception",
                    "Contra-Ingeniería Cognitiva",
                    "Cognitive Counter-Engineering",
                    [
                        (
                            "• `false_engram_injector`: inyecta **+150.0** de estímulo falso durante **90s** "
                            "(`1800t`), forzando sobre-adaptación a un elemento para infligir **+25% de daño** "
                            "con los tipos opuestos (cooldown **60s** si se descubre / **20s** natural).",
                            "• `false_engram_injector`: injects **+150.0** false stimulus for **90s** "
                            "(`1800t`), forcing over-adaptation to one trait so off-type attacks deal "
                            "**+25% bonus damage** (cooldown **60s** exposed / **20s** natural).",
                        ),
                        (
                            "• `counter_serum`: drena el estímulo codificado según `counterSerumBleedRatio` "
                            "(**50%** por defecto) para desmontar rasgos activos.\n"
                            "• `synaptic_suppressor`: silencia globalmente el aprendizaje "
                            "(`suppressorCooldownSeconds = 120s`).",
                            "• `counter_serum`: bleeds the coded stimulus by `counterSerumBleedRatio` "
                            "(**50%** default) to strip active adaptations.\n"
                            "• `synaptic_suppressor`: triggers global learning suppression "
                            "(`suppressorCooldownSeconds = 120s`).",
                        ),
                    ],
                )
            ),
        ),
    )
    ch3 = CodexChapter(
        id="memoria_colmena",
        number=3,
        title=lt("Memoria Inmune y Rasgos", "Immune Memory & Traits"),
        subtitle=lt(
            "Router 40/60, 5 Rasgos, 5 Sinergias, 4 Hábitos y Engaño",
            "40/60 Router, 5 Traits, 5 Synergies, 4 Habits & Deception",
        ),
        icon_item="mycelialmind:false_engram_injector",
        entries=ch3_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 4: IA TÁCTICA (HIVEBRAIN NATIVO, GDX-AI, ESCUADRONES Y CAMPO VIVO)
    # ==================================================================
    ch4_entries = (
        CodexEntry(
            id="arquitectura_hivebrain",
            order=1,
            title=lt("Arquitectura Nativa HiveBrain", "Native HiveBrain Architecture"),
            subtitle=lt(
                "HiveBrain, HiveBehaviorTree, HiveSteeringEngine y 11 ModMemoryTypes",
                "HiveBrain, HiveBehaviorTree, HiveSteeringEngine & 11 ModMemoryTypes",
            ),
            icon_item="mycelialmind:neural_echo_core",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "HiveBrain",
                "HiveBrainBridge",
                "HiveBrainOwner",
                "HiveBrainSchedule",
                "UtilitySelectorBehaviour",
                "HiveBehaviorTree",
                "HiveSteeringEngine",
                "HiveMindSensor",
                "HiveTacticalTree",
                "HiveUtilityEvaluator",
                "HiveAiBlackboard",
                "TacticalIntent",
                "mycelialmind:shared_target_pos",
                "mycelialmind:hive_alert_level",
                "mycelialmind:nearest_node_pos",
                "mycelialmind:stalker_flank_pos",
                "mycelialmind:weaver_support_pos",
                "mycelialmind:nearest_pylon_pos",
                "mycelialmind:hive_order",
                "mycelialmind:mob_combat_xp",
                "mycelialmind:last_attack_stimulus",
                "mycelialmind:nearby_ally_count",
                "mycelialmind:incoming_projectile_pos",
                "mycelialmind:hive_mind_sensor",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "hivebrain_native",
                    "HiveBrain Nativo, Memorias y Steering",
                    "Native HiveBrain, Memories & Steering",
                    [
                        (
                            "• **Motor `HiveBrain`:** `HiveBrain` + `HiveBrainSchedule` + "
                            "`UtilitySelectorBehaviour` (con histéresis) + LOD adaptativo e inspección `/mycelial ai`.",
                            "• **`HiveBrain` Engine:** `HiveBrain` + `HiveBrainSchedule` + "
                            "`UtilitySelectorBehaviour` (hysteresis) + adaptive LOD & `/mycelial ai` telemetry.",
                        ),
                        (
                            "• **Sensores y Conductas (`HiveBrainBridge`):** `HiveMindSensor` + "
                            "`HiveNodeRegenBehaviour`, `HiveHotspotPatrolBehaviour`, `AcidSpitterCombatBehaviour`, "
                            "`StalkerFlankBehaviour`, `WeaverSupportBehaviour`.",
                            "• **Sensors & Behaviours (`HiveBrainBridge`):** `HiveMindSensor` + "
                            "`HiveNodeRegenBehaviour`, `HiveHotspotPatrolBehaviour`, `AcidSpitterCombatBehaviour`, "
                            "`StalkerFlankBehaviour`, `WeaverSupportBehaviour`.",
                        ),
                        (
                            "• **11 `ModMemoryTypes`:** `shared_target_pos`, `hive_alert_level`, "
                            "`nearest_node_pos`, `stalker_flank_pos`, `weaver_support_pos`, "
                            "`nearest_pylon_pos`, `hive_order`, `mob_combat_xp`, `last_attack_stimulus`, "
                            "`nearby_ally_count`, `incoming_projectile_pos`, `hive_mind_sensor`.",
                            "• **11 `ModMemoryTypes`:** `shared_target_pos`, `hive_alert_level`, "
                            "`nearest_node_pos`, `stalker_flank_pos`, `weaver_support_pos`, "
                            "`nearest_pylon_pos`, `hive_order`, `mob_combat_xp`, `last_attack_stimulus`, "
                            "`nearby_ally_count`, `incoming_projectile_pos`, `hive_mind_sensor`.",
                        ),
                        (
                            "• **Motor Táctico Clean-Room:** `HiveBehaviorTree` (`HiveTacticalTree`) + "
                            "`HiveUtilityEvaluator` + `HiveAiBlackboard` (Doctrina, Trauma y Memoria Episódica).",
                            "• **Clean-Room Tactical Layer:** `HiveBehaviorTree` (`HiveTacticalTree`) + "
                            "`HiveUtilityEvaluator` + `HiveAiBlackboard` (Doctrine, Trauma & Episodic Memory).",
                        ),
                        (
                            "• **Steering Nativo:** `HiveSteeringEngine` zero-alloc (slots angulares + "
                            "repulsión de Pilones) y campos `FastNoiseLite`.",
                            "• **Native Steering:** zero-alloc `HiveSteeringEngine` (attack slots + "
                            "Pylon repulsion) & `FastNoiseLite` organic flow.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="doctrina_escuadron_pacing",
            order=2,
            title=lt("Doctrinas, Escuadrones y Pacing", "Doctrines, Squads & Pacing"),
            subtitle=lt(
                "HiveDoctrine, HiveOrder, SwarmRole, Frenesí y Desync (5s)",
                "HiveDoctrine, HiveOrder, SwarmRole, Frenzy & Desync (5s)",
            ),
            icon_item="mycelialmind:purifier_grenade",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("arquitectura_hivebrain",),
            unlock_gate="always",
            registry_refs=(
                "HiveDoctrine",
                "HiveOrder",
                "SwarmRole",
                "CommandRole",
                "SquadTacticalStatus",
                "PacingPhase",
                "HiveInfluenceMap",
                "HiveHotspot",
                "HiveSquadComposer",
                "HiveSwarmCoordinator",
                "SynapticCommandSystem",
                "HiveBlackboard",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "command_pacing",
                    "Mando Táctico e Influencia",
                    "Tactical Command & Influence",
                    [
                        (
                            "• **5 Doctrinas (`HiveDoctrine`) y 4 Órdenes (`HiveOrder`):** `DORMANT`->`NONE`, "
                            "`FORAGING`->`FLANK_PERIMETER`, `ADAPTIVE_HUNT`->`FLANK_PERIMETER`, "
                            "`SIEGE_ASSAULT`->`PRESSURE_TARGET`, `RECOVERY`->`RALLY_NODE`.",
                            "• **5 Doctrines (`HiveDoctrine`) & 4 Orders (`HiveOrder`):** `DORMANT`->`NONE`, "
                            "`FORAGING`->`FLANK_PERIMETER`, `ADAPTIVE_HUNT`->`FLANK_PERIMETER`, "
                            "`SIEGE_ASSAULT`->`PRESSURE_TARGET`, `RECOVERY`->`RALLY_NODE`.",
                        ),
                        (
                            "• **Escuadrón (`SwarmRole` / `CommandRole`):** roles `FRONT_ANCHOR`, "
                            "`FLANK_LEFT`, `FLANK_RIGHT`, `SUPPORT_REAR` con `LEADER` (`ALERT_CALL_RADIUS = 20m`). "
                            "Activa balística predictiva y Frenesí por bajas.",
                            "• **Squad (`SwarmRole` / `CommandRole`):** assigns `FRONT_ANCHOR`, "
                            "`FLANK_LEFT`, `FLANK_RIGHT`, `SUPPORT_REAR` under `LEADER` (`20m`). "
                            "Enables predictive lead aim and casualty Frenzy.",
                        ),
                        (
                            "• **Contramedida `Desync` (5s):** abate al `LEADER` o impacta con "
                            "`purifier_grenade` para romper el enlace (`SquadTacticalStatus`), anular el "
                            "plomo balístico e inducir `10.0` de imprecisión.",
                            "• **Counter `Desync` (5s):** snipe the `LEADER` or detonate `purifier_grenade` "
                            "to shatter command links (`SquadTacticalStatus`), zero out ballistic lead, "
                            "and inflict `10.0` inaccuracy.",
                        ),
                        (
                            "• **Pacing y Mapa de Calor:** `HiveBlackboard` (`PacingPhase`) alterna presión/tregua; "
                            "`HiveInfluenceMap` guarda hasta **512** celdas (difusión **0.35**, poda **0.01**) "
                            "y `HiveHotspot` máx. **24** focos.",
                            "• **Pacing & Influence Map:** `HiveBlackboard` (`PacingPhase`) modulates assault "
                            "intensity; `HiveInfluenceMap` tracks **512** cells (`0.35` diffusion, `0.01` "
                            "prune) alongside max **24** `HiveHotspot` foci.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="campo_batalla_vivo",
            order=3,
            title=lt("IA de Campo Vivo y Goals", "Living Battlefield AI & Goals"),
            subtitle=lt(
                "Trauma, Carroña, Quitina Efímera, Atascos y Goals personalizados",
                "Trauma, Carrion, Ephemeral Chitin, Jams & Custom Goals",
            ),
            icon_item="mycelialmind:ephemeral_chitin",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("doctrina_escuadron_pacing",),
            unlock_gate="always",
            registry_refs=(
                "LivingBattlefieldSystem",
                "mycelialmind:ephemeral_chitin",
                "BruteChargeGoal",
                "EvasiveStrafeGoal",
                "FlankingAmbushGoal",
                "HiveAlertSupportGoal",
                "HiveRegroupGoal",
                "KitingRetreatGoal",
                "NecrophagyGoal",
                "SwarmFlankGoal",
                "TacticalTerraformingGoal",
                "WeaverSupportGoal",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "living_field",
                    "Mecánicas de Campo Vivo",
                    "Living Battlefield Mechanics",
                    [
                        (
                            "• **Trauma y Zapadores (`LivingBattlefieldSystem`):** si una celda acumula "
                            "trauma `>=0.55`, la infantería la rodea; solo Zapadores (`Burrower`, "
                            "`ColonyWarden`, `ELITE` o `BLAST_SHIELDED`) la cruzan directo.",
                            "• **Trauma & Sappers (`LivingBattlefieldSystem`):** cells >=`0.55` trauma "
                            "force infantry detours; only Sappers (`Burrower`, `ColonyWarden`, `ELITE`, "
                            "or `BLAST_SHIELDED`) push straight through.",
                        ),
                        (
                            "• **Terraforming y Anti-Pilar (`ephemeral_chitin`):** si el jugador se eleva "
                            "**>=2 bloques** o dispara a distancia, levantan rampas y barricadas temporales "
                            "de `ephemeral_chitin` que decaen en **240t (12s)**.",
                            "• **Anti-Pillar & Barricades (`ephemeral_chitin`):** pillaring **>=2 blocks** "
                            "or sniping prompts temporary `ephemeral_chitin` ramps/barricades that crumble "
                            "after **240t (12s)**.",
                        ),
                        (
                            "• **Necrofagia (`NecrophagyGoal`):** cadáveres no-jefe dejan carroña **240t**: "
                            "comerla cura **+6.0 HP** y otorga **+20 XP** de rango. El fuego o zonas de Pilón "
                            "impiden la carroña (`shouldDropCarrion`).",
                            "• **Necrophagy (`NecrophagyGoal`):** non-boss kills leave carrion for **240t**: "
                            "feeding heals **+6.0 HP** and grants **+20 XP** toward promotion. Fire damage "
                            "or Pylon zones prevent carrion drops.",
                        ),
                        (
                            "• **Atascos:** `AcidSpit` atasca torretas **120t (6s)** y `Leech` atasca drones **80t (4s)**.",
                            "• **Jams:** `AcidSpit` jams turrets **120t (6s)** and `Leech` jams drones **80t (4s)**.",
                        ),
                        (
                            "• **Goals (10):** `BruteCharge`, `EvasiveStrafe`, `FlankingAmbush`, "
                            "`HiveAlertSupport`, `HiveRegroup`, `KitingRetreat`, `Necrophagy`, "
                            "`SwarmFlank`, `TacticalTerraforming`, `WeaverSupport`.",
                            "• **Goals (10):** `BruteCharge`, `EvasiveStrafe`, `FlankingAmbush`, "
                            "`HiveAlertSupport`, `HiveRegroup`, `KitingRetreat`, `Necrophagy`, "
                            "`SwarmFlank`, `TacticalTerraforming`, `WeaverSupport`.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch4 = CodexChapter(
        id="ia_tactica",
        number=4,
        title=lt("IA Táctica y Campo Vivo", "Tactical AI & Living Field"),
        subtitle=lt(
            "HiveBrain, gdx-AI, Escuadrones, Trauma, Carroña y Goals",
            "HiveBrain, gdx-AI, Squads, Trauma, Carrion & Custom Goals",
        ),
        icon_item="mycelialmind:neural_echo_core",
        entries=ch4_entries,
        unlock_gate="always",
    )

    return (ch1, ch2, ch3, ch4)
