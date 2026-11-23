"""Chapters 5–8 of the Mycelial Mind Codex (Bestiary, Synaptic Network, Biotech & Diplomacy, Arsenal & Bastion)."""

from __future__ import annotations

from .helpers import chunk_paired_blocks, entity_page, grid_xy, lt, spot_page, text_page
from .schema import CodexChapter, CodexEntry


def build_chapters_5_to_8() -> tuple[CodexChapter, ...]:
    # ==================================================================
    # CHAPTER 5: BESTIARIO TOTAL (17 VIVAS + 3 PROYECTILES + QUIMERA 5x5)
    # ==================================================================
    ch5_entries = (
        CodexEntry(
            id="rangos_y_primordiales",
            order=1,
            title=lt("Rangos Evolutivos y Primordiales", "Evolution Ranks & Primordials"),
            subtitle=lt(
                "BASE, MUTATED, ELITE, Primordiales y proyección vocal",
                "BASE, MUTATED, ELITE, Primordials & acoustic voice scaling",
            ),
            icon_item="mycelialmind:spore_behemoth_spawn_egg",
            x=grid_xy(0, 4)[0],
            y=grid_xy(0, 4)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=("EvolutionRank", "BASE", "MUTATED", "ELITE", "MycelialMob"),
            pages=tuple(
                chunk_paired_blocks(
                    "ranks",
                    "Rangos y Variantes Primordiales",
                    "Ranks & Primordial Variants",
                    [
                        (
                            "• **`BASE`:** **1.00x** HP, **1.00x** daño, **1.00** escala, **1.00x** XP, "
                            "tono vocal **1.00**, vol. **1.00**.",
                            "• **`BASE`:** **1.00x** HP, **1.00x** dmg, **1.00** scale, **1.00x** XP, "
                            "voice pitch **1.00**, vol **1.00**.",
                        ),
                        (
                            "• **`MUTATED`:** **1.25x** HP, **1.15x** daño, **1.12** escala, **1.25x** XP, "
                            "tono **0.89**, vol. **1.10**.",
                            "• **`MUTATED`:** **1.25x** HP, **1.15x** dmg, **1.12** scale, **1.25x** XP, "
                            "pitch **0.89**, vol **1.10**.",
                        ),
                        (
                            "• **`ELITE`:** **1.60x** HP, **1.35x** daño, **1.25** escala, **1.75x** XP, "
                            "tono **0.78**, vol. **1.22** (aura de esporas carmesí).",
                            "• **`ELITE`:** **1.60x** HP, **1.35x** dmg, **1.25** scale, **1.75x** XP, "
                            "pitch **0.78**, vol **1.22** (crimson spore aura).",
                        ),
                        (
                            "• **Primordiales Bioluminiscentes:** **+25%** HP máx. (`primordial_variant_health`), "
                            "pulsos `bio_scanner_ping` cada **200t**, curan aliados de igual cepa en **6m** "
                            "cada **40t** y desbloquean el Secreto #7.",
                            "• **Bioluminescent Primordials:** **+25%** max HP (`primordial_variant_health`), "
                            "pulse `bio_scanner_ping` every **200t**, heal same-strain allies in **6m** "
                            "every **40t**, and unlock Secret #7.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="bestiario_enjambre_ligero",
            order=2,
            title=lt("Enjambre Ligero y Parasitario", "Light & Parasitic Swarm"),
            subtitle=lt(
                "sporeling, leech_parasite y mycelial_stalker",
                "sporeling, leech_parasite & mycelial_stalker",
            ),
            icon_item="mycelialmind:sporeling_spawn_egg",
            x=grid_xy(1, 4)[0],
            y=grid_xy(1, 4)[1],
            parents=("rangos_y_primordiales",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:sporeling",
                "mycelialmind:leech_parasite",
                "mycelialmind:mycelial_stalker",
            ),
            pages=(
                entity_page(
                    "ent_sporeling",
                    "mycelialmind:sporeling",
                    "Sporeling",
                    "Sporeling",
                    "**14 HP | 3.5 Daño | 0.34 Vel**\nEnjambre velocista: gana Velocidad en grupo (`6m`) y suma **+1** daño/aliado (máx. **+3**). Cerca de tu cama genera **+5.0 MELEE** al morir.",
                    "**14 HP | 3.5 Dmg | 0.34 Spd**\nPack rusher: gains Speed near allies (`6m`) and deals **+1** bonus dmg per nearby ally (max **+3**). Dying near respawn injects **+5.0 MELEE**.",
                    scale=1.1,
                ),
                entity_page(
                    "ent_leech",
                    "mycelialmind:leech_parasite",
                    "Sanguijuela Parásita",
                    "Leech Parasite",
                    "**12 HP | 3.0 Daño | 0.35 Vel**\nSifón: aplica Hambre II, Fatiga, `mycelial_infection` (**160t**), drena saturación, se cura **+2 HP** y atasca Drones **80t** o Gólems.",
                    "**12 HP | 3.0 Dmg | 0.35 Spd**\nSiphon bite: inflicts Hunger II, Mining Fatigue, `mycelial_infection` (**160t**), drains saturation, heals **+2 HP**, and jams Drones **80t**.",
                    scale=1.1,
                ),
                entity_page(
                    "ent_stalker",
                    "mycelialmind:mycelial_stalker",
                    "Acechador Micelial",
                    "Mycelial Stalker",
                    "**22 HP | 5.0 Daño | 0.36 Vel**\nInvisibilidad sobre `creeping_mycelium`. Golpear por la espalda o invisible inflige **+3.0** daño extra antes de saltar lateralmente.",
                    "**22 HP | 5.0 Dmg | 0.36 Spd**\nCloaks with Invisibility while standing on `creeping_mycelium`. Strikes from behind or cloaked deal **+3.0** bonus damage then evade.",
                    scale=0.9,
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_artilleria_fuego",
            order=3,
            title=lt("Artillería Ácida y Pirófitos", "Acid Artillery & Pyrophytes"),
            subtitle=lt(
                "acid_spitter y pyro_shroom",
                "acid_spitter & pyro_shroom",
            ),
            icon_item="mycelialmind:acid_spitter_spawn_egg",
            x=grid_xy(2, 4)[0],
            y=grid_xy(2, 4)[1],
            parents=("rangos_y_primordiales",),
            unlock_gate="always",
            registry_refs=("mycelialmind:acid_spitter", "mycelialmind:pyro_shroom"),
            pages=(
                entity_page(
                    "ent_acid_spitter",
                    "mycelialmind:acid_spitter",
                    "Escupidor de Ácido",
                    "Acid Spitter",
                    "**20 HP | 4.0 Daño | 0.26 Vel**\nTiro parabólico `acid_spit` con predicción si está en escuadrón (`Desync` la anula). En `ELITE` dispara proyectil doble. Atasque a torretas: **120t**.",
                    "**20 HP | 4.0 Dmg | 0.26 Spd**\nLobs `acid_spit` with predictive squad lead (`Desync` breaks aim). `ELITE` rank fires twin volleys. Corrosive spit jams turrets for **120t**.",
                    scale=0.9,
                ),
                entity_page(
                    "ent_pyro_shroom",
                    "mycelialmind:pyro_shroom",
                    "Piro-Hongo",
                    "Pyro-Shroom",
                    "**24 HP | 5.0 Daño | 0.29 Vel**\nInmune al fuego (**+1 HP** si recibe daño ígneo). Al caer <=**35%** HP enciende mecha (**30t**, explosión **2.6F**); explota al morir (**2.2F**).",
                    "**24 HP | 5.0 Dmg | 0.29 Spd**\nFire-immune (**+1 HP** from fire hits). Below **35%** HP lights a **30t** fuse (**2.6F** incendiary blast); also detonates on death (**2.2F**).",
                    scale=0.95,
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_ingenieros_soporte",
            order=4,
            title=lt("Zapadores, Tejedores y Arquitectos", "Sappers, Weavers & Architects"),
            subtitle=lt(
                "burrower, synapse_weaver y spore_architect",
                "burrower, synapse_weaver & spore_architect",
            ),
            icon_item="mycelialmind:burrower_spawn_egg",
            x=grid_xy(3, 4)[0],
            y=grid_xy(3, 4)[1],
            parents=("rangos_y_primordiales",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:burrower",
                "mycelialmind:synapse_weaver",
                "mycelialmind:spore_architect",
            ),
            pages=(
                entity_page(
                    "ent_burrower",
                    "mycelialmind:burrower",
                    "Excavador",
                    "Burrower",
                    "**28 HP | 5 Dmg | 3 Arm | 0.25 Vel**\nZapador: rompe bloques cada **30t** (**18t** con `BLAST_SHIELDED` N2). Velocidad II a **>8m**.",
                    "**28 HP | 5 Dmg | 3 Arm | 0.25 Spd**\nTunneling sapper: breaks blocks every **30t** (**18t** with T2 `BLAST_SHIELDED`). Speed II **>8m**.",
                    scale=0.85,
                ),
                entity_page(
                    "ent_weaver",
                    "mycelialmind:synapse_weaver",
                    "Tejedor de Sinapsis",
                    "Synapse Weaver",
                    "**26 HP | 4 Dmg | 0.25 Vel**\nSoporte (`WeaverSupportGoal` + `EvasiveStrafeGoal`): guarda distancia y potencia al escuadrón.",
                    "**26 HP | 4 Dmg | 0.25 Spd**\nSupport (`WeaverSupportGoal` + `EvasiveStrafeGoal`): keeps range and bolsters nearby allies.",
                    scale=0.9,
                ),
                entity_page(
                    "ent_architect",
                    "mycelialmind:spore_architect",
                    "Arquitecto de Esporas",
                    "Spore Architect",
                    "**28 HP | 4 Dmg | 3 Arm | 0.27 Vel**\nConstructor (`BuildColonyGoal`): levanta nuevos `mycelial_node` fuera de zonas con Pilón.",
                    "**28 HP | 4 Dmg | 3 Arm | 0.27 Spd**\nBuilder (`BuildColonyGoal`): expands the network by erecting `mycelial_node`s outside Pylons.",
                    scale=0.85,
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_pesados_mimico",
            order=5,
            title=lt("Bruto, Góliat y Mímico Sináptico", "Brute, Goliath & Synaptic Mimic"),
            subtitle=lt(
                "chitin_brute, goliath_carrier y synaptic_mimic",
                "chitin_brute, goliath_carrier & synaptic_mimic",
            ),
            icon_item="mycelialmind:synaptic_mimic_spawn_egg",
            x=grid_xy(4, 4)[0],
            y=grid_xy(4, 4)[1],
            parents=("bestiario_enjambre_ligero",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:chitin_brute",
                "mycelialmind:goliath_carrier",
                "mycelialmind:synaptic_mimic",
                "MimicStance",
                "BLADE_DANCER",
                "SPORE_MARKSMAN",
                "AEGIS_MIRROR",
            ),
            pages=(
                entity_page(
                    "ent_brute",
                    "mycelialmind:chitin_brute",
                    "Bruto de Quitina",
                    "Chitin Brute",
                    "**40 HP | 7 Dmg | 4 Arm | 0.5 KB**\nVanguardia (`BruteChargeGoal`) con **0.23** vel. y alta resistencia al empuje.",
                    "**40 HP | 7 Dmg | 4 Arm | 0.5 KB**\nArmored vanguard (`BruteChargeGoal`) at **0.23** speed with strong knockback resistance.",
                    scale=0.75,
                ),
                entity_page(
                    "ent_goliath",
                    "mycelialmind:goliath_carrier",
                    "Portador Góliat",
                    "Goliath Carrier",
                    "**55 HP | 8 Dmg | 0.7 KB | 0.21 Vel**\nPisotón cada **110t** (`5m`, Lentitud II). Suelta **1** Sporeling <=**50% HP** y **2** al morir.",
                    "**55 HP | 8 Dmg | 0.7 KB | 0.21 Spd**\nStomp every **110t** (`5m`, Slowness II). Spawns **1** Sporeling <=**50% HP**, **2** on death.",
                    scale=0.65,
                ),
                entity_page(
                    "ent_mimic",
                    "mycelialmind:synaptic_mimic",
                    "Mímico Sináptico",
                    "Synaptic Mimic",
                    "**65 HP | 6 Dmg | 6 Arm | 0.32 Vel**\nAdapta **3 `MimicStance`**: `BLADE_DANCER`, `SPORE_MARKSMAN`, `AEGIS_MIRROR`.",
                    "**65 HP | 6 Dmg | 6 Arm | 0.32 Spd**\nMirrors gear via **3 `MimicStance`**: `BLADE_DANCER`, `SPORE_MARKSMAN`, `AEGIS_MIRROR`.",
                    scale=0.78,
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_elite_campeon",
            order=6,
            title=lt("Guardián y Behemot de Esporas", "Colony Warden & Spore Behemoth"),
            subtitle=lt(
                "colony_warden (110 HP) y spore_behemoth (200 HP)",
                "colony_warden (110 HP) & spore_behemoth (200 HP)",
            ),
            icon_item="mycelialmind:colony_warden_spawn_egg",
            x=grid_xy(5, 4)[0],
            y=grid_xy(5, 4)[1],
            parents=("bestiario_pesados_mimico",),
            unlock_gate="act_3",
            registry_refs=("mycelialmind:colony_warden", "mycelialmind:spore_behemoth"),
            pages=(
                entity_page(
                    "ent_warden",
                    "mycelialmind:colony_warden",
                    "Guardián de Colonia",
                    "Colony Warden",
                    "**110 HP | 11 Dmg | 8 Arm | 0.85 KB**\nAcelera enjambre (**60t**) y lanza Onda (**140t**: Fuerza+Vel `12m`, empuje `4m`).",
                    "**110 HP | 11 Dmg | 8 Arm | 0.85 KB**\nHastens swarm (**60t**) + Shockwave (**140t**: buffs allies `12m`, knockback `4m`).",
                    scale=0.62,
                ),
                entity_page(
                    "ent_behemoth",
                    "mycelialmind:spore_behemoth",
                    "Behemot de Esporas",
                    "Spore Behemoth",
                    "**200 HP | 10 Dmg | 8 Arm | 0.85 KB | 45 XP**\nCampeón de Marea y `MatriarchCitadel`. Suelta `tide_beacon`.",
                    "**200 HP | 10 Dmg | 8 Arm | 0.85 KB | 45 XP**\nTide & `MatriarchCitadel` champion. Drops `tide_beacon`.",
                    scale=0.52,
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_jefe_hive_core",
            order=7,
            title=lt("Jefe Overworld: Núcleo Colmena", "Overworld Boss: Hive Core"),
            subtitle=lt(
                "hive_core (350 HP) — 3 Fases y 3 Anclas Centinela",
                "hive_core (350 HP) — 3 Phases & 3 Sentinel Anchors",
            ),
            icon_item="mycelialmind:hive_core_spawn_egg",
            x=grid_xy(6, 4)[0],
            y=grid_xy(6, 4)[1],
            parents=("bestiario_elite_campeon",),
            unlock_gate="act_3",
            registry_refs=("mycelialmind:hive_core", "mycelialmind:hive_heart"),
            pages=(
                entity_page(
                    "ent_hive_core",
                    "mycelialmind:hive_core",
                    "Núcleo de la Colmena",
                    "The Hive Core",
                    "**350 HP | 14 Dmg | 10 Arm | 350 XP**\n`hive_altar`. F1: ácido x3. F2 (65–30%): invulnerable con **3 `sentinel_node`**.",
                    "**350 HP | 14 Dmg | 10 Arm | 350 XP**\n`hive_altar`. P1: 3-acid fan. P2 (65–30%): invulnerable via **3 `sentinel_node`**.",
                    scale=0.45,
                ),
                *chunk_paired_blocks(
                    "hive_core_tactics",
                    "Fase 3 y Botín del Núcleo",
                    "Phase 3 & Hive Core Loot",
                    [
                        (
                            "• **Anclas Fase 2:** destruye los **3** `sentinel_node` enlazados para aturdir al jefe y romper su escudo.",
                            "• **Phase 2 Anchors:** shatter all **3** tethered `sentinel_node`s to break its shield and stun it.",
                        ),
                        (
                            "• **Fase 3 (<30% HP) y Botín:** rota sinergias N2 + ondas de choque. Botín: `hive_heart`, `synaptic_core`, `neural_echo_core`, `synaptic_graft`.",
                            "• **Phase 3 (<30% HP) & Loot:** cycles Tier 2 synergies + shockwaves. Drops: `hive_heart`, `synaptic_core`, `neural_echo_core`, `synaptic_graft`.",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_jefe_matriarca",
            order=8,
            title=lt("Jefe Final: Matriarca Suprema", "Final Boss: Apex Matriarch"),
            subtitle=lt(
                "apex_matriarch (600 HP) en el Micelio Profundo",
                "apex_matriarch (600 HP) inside Deep Mycelium",
            ),
            icon_item="mycelialmind:apex_matriarch_spawn_egg",
            x=grid_xy(7, 4)[0],
            y=grid_xy(7, 4)[1],
            parents=("bestiario_jefe_hive_core",),
            unlock_gate="deep_mycelium",
            registry_refs=("mycelialmind:apex_matriarch", "mycelialmind:overmind_essence"),
            pages=(
                entity_page(
                    "ent_matriarch",
                    "mycelialmind:apex_matriarch",
                    "Matriarca Suprema",
                    "Apex Matriarch",
                    "**600 HP | 18 Dmg | 12 Arm | 750 XP**\n`matriarch_altar`. F1: andanada + blink. F2 (<60%): +20 estrés, 2 Centinelas + élite.",
                    "**600 HP | 18 Dmg | 12 Arm | 750 XP**\n`matriarch_altar`. P1: barrage + blink. P2 (<60%): +20 stress, 2 Sentinels + elite.",
                    scale=0.40,
                ),
                text_page(
                    "matriarch_f3_drops",
                    "Fase 3, Botín y Remisión",
                    "Phase 3, Loot & Remission",
                    (
                        "• **Fase 3 (<25% HP):** pulsos mágicos cada **100t** en **8m** (**6.0** daño mágico + "
                        "`mycelial_infection` II `160t`) y sinergias activas.\n"
                        "• **Recompensas:** `overmind_essence`, `hive_heart`, `quorum_seal`, `chimera_ovum`, "
                        "`echo_shard` y activa **20 min** de Remisión global."
                    ),
                    (
                        "• **Phase 3 (<25% HP):** AoE pulses every **100t** in **8m** (**6.0** magic dmg + "
                        "`mycelial_infection` II `160t`) with active synergies.\n"
                        "• **Rewards:** `overmind_essence`, `hive_heart`, `quorum_seal`, `chimera_ovum`, "
                        "`echo_shard` and starts **20-min** global Remission."
                    ),
                ),
            ),
        ),
        CodexEntry(
            id="bestiario_aliados_quimera",
            order=9,
            title=lt("Aliados Bioforjados: Dron, Quimera y Colonia", "Allies: Drone, Chimera & Colony"),
            subtitle=lt(
                "purifier_drone, mycelial_chimera y colony_symbiont (Colonia Soberana)",
                "purifier_drone, mycelial_chimera & colony_symbiont (Sovereign Colony)",
            ),
            icon_item="mycelialmind:chimera_ovum",
            x=grid_xy(8, 4)[0],
            y=grid_xy(8, 4)[1],
            parents=("rangos_y_primordiales",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:purifier_drone",
                "mycelialmind:mycelial_chimera",
                "mycelialmind:colony_symbiont",
                "CommandMode",
                "FOLLOW",
                "PATROL",
                "WAIT",
            ),
            pages=(
                entity_page(
                    "ent_drone",
                    "mycelialmind:purifier_drone",
                    "Dron Purificador",
                    "Purifier Drone",
                    "**60 HP | 6 Arm | 0.45 Vuelo**\n`purifier_drone_core`: dispara `neutralizing_spore`, limpia micelio y patrulla. Sanguijuela atasca **80t**.",
                    "**60 HP | 6 Arm | 0.45 Fly**\n`purifier_drone_core`: fires `neutralizing_spore`, cleanses mycelium & patrols. Leech jams **80t**.",
                    scale=0.85,
                ),
                entity_page(
                    "ent_chimera",
                    "mycelialmind:mycelial_chimera",
                    "Quimera Micelial",
                    "Mycelial Chimera",
                    "**52/68 HP | 5.5/8.5 Dmg | 4/8 Arm**\nDe `chimera_ovum` en `symbiotic_node` (>=35%). Montura `CommandMode` (`FOLLOW`, `PATROL`, `WAIT`).",
                    "**52/68 HP | 5.5/8.5 Dmg | 4/8 Arm**\nHatched from `chimera_ovum` on `symbiotic_node` (>=35%). Rideable `CommandMode` (`FOLLOW`, `PATROL`, `WAIT`).",
                    scale=0.68,
                ),
                entity_page(
                    "ent_colony_symbiont",
                    "mycelialmind:colony_symbiont",
                    "Simbionte de Colonia",
                    "Colony Symbiont",
                    "**32 HP | 4 Arm | 5 Castas**\nDe `sovereign_nexus`: `HARVESTER`, `EXCAVATOR`, `COURIER`, `ARCHITECT`, `SENTINEL`.",
                    "**32 HP | 4 Arm | 5 Castes**\nFrom `sovereign_nexus`: `HARVESTER`, `EXCAVATOR`, `COURIER`, `ARCHITECT`, `SENTINEL`.",
                    scale=0.82,
                ),
                *chunk_paired_blocks(
                    "chimera_matrix",
                    "Matriz Genética 5×5 de la Quimera",
                    "Chimera 5×5 Gene Matrix",
                    [
                        (
                            "• **Chasis (`ChassisGene`, +Base):** `MELEE_THORNS` (**8.5** daño), "
                            "`PROJECTILE_DEFLECT` (**0.35** vel.), `FIRE_RESISTANT` (inmune fuego), "
                            "`BLAST_SHIELDED` (**68 HP**, **8.0 Arm**), `MAGIC_CARAPACE` (cura **1 HP/100t**).",
                            "• **Chassis (`ChassisGene`):** `MELEE_THORNS` (**8.5** dmg), "
                            "`PROJECTILE_DEFLECT` (**0.35** spd), `FIRE_RESISTANT` (fire immunity), "
                            "`BLAST_SHIELDED` (**68 HP**, **8.0 Arm**), `MAGIC_CARAPACE` (**1 HP/100t**).",
                        ),
                        (
                            "• **Mutación Activa (`140t` / `160t` montada):** `MELEE_THORNS` (salto + Veneno), "
                            "`PROJECTILE_DEFLECT` (doble espora `16m`), `FIRE_RESISTANT` (aura ígnea + Res. jinete), "
                            "`BLAST_SHIELDED` (onda empujón), `MAGIC_CARAPACE` (Absorción a jinete).",
                            "• **Active Mutation (`140t` / `160t` ridden):** `MELEE_THORNS` (leap + Poison), "
                            "`PROJECTILE_DEFLECT` (twin spores `16m`), `FIRE_RESISTANT` (flame burst + rider Fire Res), "
                            "`BLAST_SHIELDED` (blast slow), `MAGIC_CARAPACE` (rider Absorption).",
                        ),
                        (
                            "• **Lluvia Ácida (Homeostasis):** con `FIRE_RESISTANT` y `MAGIC_CARAPACE` regenera "
                            "**+0.8F**; con uno de ambos es inmune (**0.0F**); sin ellos sufre **-0.75F**.",
                            "• **Acid Rain (Homeostasis):** having both `FIRE_RESISTANT` & `MAGIC_CARAPACE` "
                            "heals **+0.8F**; either grants immunity (**0.0F**); neither takes **-0.75F**.",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="proyectiles_y_huevos",
            order=10,
            title=lt("Proyectiles (3) y Spawn Eggs (18)", "Projectiles (3) & Spawn Eggs (18)"),
            subtitle=lt(
                "acid_spit, neutralizing_spore, purifier_grenade y los 18 huevos",
                "acid_spit, neutralizing_spore, purifier_grenade & 18 spawn eggs",
            ),
            icon_item="mycelialmind:hive_core_spawn_egg",
            x=grid_xy(9, 4)[0],
            y=grid_xy(9, 4)[1],
            parents=("rangos_y_primordiales",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:acid_spit",
                "mycelialmind:neutralizing_spore",
                "mycelialmind:purifier_grenade",
                "mycelialmind:sporeling_spawn_egg",
                "mycelialmind:chitin_brute_spawn_egg",
                "mycelialmind:mycelial_stalker_spawn_egg",
                "mycelialmind:acid_spitter_spawn_egg",
                "mycelialmind:burrower_spawn_egg",
                "mycelialmind:pyro_shroom_spawn_egg",
                "mycelialmind:synapse_weaver_spawn_egg",
                "mycelialmind:goliath_carrier_spawn_egg",
                "mycelialmind:leech_parasite_spawn_egg",
                "mycelialmind:colony_warden_spawn_egg",
                "mycelialmind:spore_architect_spawn_egg",
                "mycelialmind:synaptic_mimic_spawn_egg",
                "mycelialmind:mycelial_chimera_spawn_egg",
                "mycelialmind:colony_symbiont_spawn_egg",
                "mycelialmind:spore_behemoth_spawn_egg",
                "mycelialmind:purifier_drone_spawn_egg",
                "mycelialmind:hive_core_spawn_egg",
                "mycelialmind:apex_matriarch_spawn_egg",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "proj_eggs",
                    "Proyectiles y Huevos Generadores",
                    "Projectiles & Spawn Eggs",
                    [
                        (
                            "• **3 Proyectiles Registrados (`ModEntities`):** `acid_spit` (escupitajo corrosivo "
                            "que atasca torretas **120t** y refleja `KINETIC_MIRROR`), `neutralizing_spore` "
                            "(lanzador/dron purificador) y `purifier_grenade` (desincroniza escuadrones **5s**).",
                            "• **3 Registered Projectiles (`ModEntities`):** `acid_spit` (corrosive spit "
                            "jamming turrets **120t** & `KINETIC_MIRROR` shards), `neutralizing_spore` "
                            "(launcher/drone ammo), and `purifier_grenade` (triggers **5s** squad Desync).",
                        ),
                        (
                            "• **Huevos 1/4:** `sporeling_spawn_egg`, `chitin_brute_spawn_egg`, "
                            "`mycelial_stalker_spawn_egg`, `acid_spitter_spawn_egg`.",
                            "• **Eggs 1/4:** `sporeling_spawn_egg`, `chitin_brute_spawn_egg`, "
                            "`mycelial_stalker_spawn_egg`, `acid_spitter_spawn_egg`.",
                        ),
                        (
                            "• **Huevos 2/4:** `burrower_spawn_egg`, `pyro_shroom_spawn_egg`, "
                            "`synapse_weaver_spawn_egg`, `goliath_carrier_spawn_egg`, `leech_parasite_spawn_egg`.",
                            "• **Eggs 2/4:** `burrower_spawn_egg`, `pyro_shroom_spawn_egg`, "
                            "`synapse_weaver_spawn_egg`, `goliath_carrier_spawn_egg`, `leech_parasite_spawn_egg`.",
                        ),
                        (
                            "• **Huevos 3/4:** `colony_warden_spawn_egg`, `spore_architect_spawn_egg`, "
                            "`synaptic_mimic_spawn_egg`, `spore_behemoth_spawn_egg`.",
                            "• **Eggs 3/4:** `colony_warden_spawn_egg`, `spore_architect_spawn_egg`, "
                            "`synaptic_mimic_spawn_egg`, `spore_behemoth_spawn_egg`.",
                        ),
                        (
                            "• **Huevos 4/4:** `hive_core_spawn_egg`, `apex_matriarch_spawn_egg`, "
                            "`purifier_drone_spawn_egg`, `mycelial_chimera_spawn_egg`, `colony_symbiont_spawn_egg`.",
                            "• **Eggs 4/4:** `hive_core_spawn_egg`, `apex_matriarch_spawn_egg`, "
                            "`purifier_drone_spawn_egg`, `mycelial_chimera_spawn_egg`, `colony_symbiont_spawn_egg`.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch5 = CodexChapter(
        id="bestiario",
        number=5,
        title=lt("Bestiario y Quimeras", "Bestiary & Chimeras"),
        subtitle=lt(
            "17 especies vivas, 3 proyectiles, 17 huevos y genes 5x5",
            "17 living species, 3 projectiles, 17 spawn eggs & 5x5 genes",
        ),
        icon_item="mycelialmind:apex_matriarch_spawn_egg",
        entries=ch5_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 6: RED SINÁPTICA, NODOS, ACÚSTICA Y VIAJE MICORRIZAL
    # ==================================================================
    ch6_entries = (
        CodexEntry(
            id="nodos_y_malla_sinaptica",
            order=1,
            title=lt("Los 4 Nodos Sinápticos", "The 4 Synaptic Node Types"),
            subtitle=lt(
                "SynapticNodeType: RELAY, INCUBATOR, SENTINEL y SYMBIOTIC",
                "SynapticNodeType: RELAY, INCUBATOR, SENTINEL & SYMBIOTIC",
            ),
            icon_item="mycelialmind:mycelial_node",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "SynapticNodeType",
                "RELAY",
                "INCUBATOR",
                "SENTINEL",
                "SYMBIOTIC",
                "mycelialmind:mycelial_node",
                "mycelialmind:incubator_node",
                "mycelialmind:sentinel_node",
                "mycelialmind:symbiotic_node",
                "mycelialmind:synaptic_node",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "nodes",
                    "Tipología de Red Nodal",
                    "Synaptic Node Typology",
                    [
                        (
                            "• `RELAY` (`mycelial_node`): enlaza nodos dentro de `synapticNodeLinkRadius` "
                            "(**28m**), amplifica **x1.5** el aprendizaje local y alberga búfer `60/40` (`600t`).",
                            "• `RELAY` (`mycelial_node`): links hubs within `synapticNodeLinkRadius` (**28m**), "
                            "amplifies local stimulus **x1.5**, and holds **60/40** buffers (`600t`).",
                        ),
                        (
                            "• `INCUBATOR` (`incubator_node`): engendra refuerzos y `spore_architect`; tope global "
                            "autónomo `maxAutonomousNodes = 32` con espaciado mínimo `minNodeSpacing = 18m`.",
                            "• `INCUBATOR` (`incubator_node`): hatches combat squads and `spore_architect`; global "
                            "cap `maxAutonomousNodes = 32` with `minNodeSpacing = 18m`.",
                        ),
                        (
                            "• `SENTINEL` (`sentinel_node`): blinda mobs cercanos y actúa como ancla de "
                            "invulnerabilidad en Fase 2 de `hive_core` y `apex_matriarch`.",
                            "• `SENTINEL` (`sentinel_node`): shields nearby swarmers and anchors Phase 2 "
                            "invulnerability for `hive_core` and `apex_matriarch`.",
                        ),
                        (
                            "• `SYMBIOTIC` (`symbiotic_node`, BE `synaptic_node`): nodo reprogramado aliado "
                            "silenciador y portal de viaje rápido.",
                            "• `SYMBIOTIC` (`symbiotic_node`, BE `synaptic_node`): reprogrammed allied hub "
                            "for acoustic silencing and fast travel.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="injerto_y_viaje_micorrizal",
            order=2,
            title=lt("Injerto y Viaje Micorrizal", "Synaptic Graft & Fast Travel"),
            subtitle=lt(
                "Reprogramación nodal y salto rápido a 96m con Quimeras y Drones",
                "Node hijacking & 96m teleport transferring Chimeras & Drones",
            ),
            icon_item="mycelialmind:symbiotic_node",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("nodos_y_malla_sinaptica",),
            unlock_gate="act_2",
            registry_refs=("mycelialmind:synaptic_graft", "mycelialmind:symbiotic_node"),
            pages=(
                spot_page(
                    "graft_usage",
                    "mycelialmind:synaptic_graft",
                    "Secuestro con Injerto Sináptico",
                    "Hijacking with Synaptic Graft",
                    (
                        "Aplica `synaptic_graft` en un nodo hostil -> `symbiotic_node` "
                        "(máx. `maxSymbioticNodes = 4`). Recarga **+15%** con `mycelial_biomass`."
                    ),
                    (
                        "Apply `synaptic_graft` to a hostile node -> `symbiotic_node` "
                        "(cap `maxSymbioticNodes = 4`). Recharge **+15%** using `mycelial_biomass`."
                    ),
                ),
                *chunk_paired_blocks(
                    "fast_travel_rules",
                    "Viaje Rápido Micorrizal",
                    "Mycorrhizal Fast Travel",
                    [
                        (
                            "• **Enlace (`Shift + Clic`):** conecta con `symbiotic_node` dentro de **96m** "
                            "(`mycorrhizalTravelRange`, rango `16–256m`) sin spawns hostiles.",
                            "• **Link (`Shift + Right-Click`):** warps between `symbiotic_node`s within "
                            "**96m** (`mycorrhizalTravelRange`, `16–256m`).",
                        ),
                        (
                            "• **Costo y Bloqueo:** **6** comida, **15** estabilidad, `240t` `mycelial_infection`, "
                            "`60t` Náusea (`1500t` CD); bloqueado en combate (`100t`) o ya infectado.",
                            "• **Cost & Locks:** **6** food, **15** stability, `240t` `mycelial_infection`, "
                            "`60t` Nausea (`1500t` CD); blocked if hurt `<100t` or infected.",
                        ),
                        (
                            "• **Escolta Aliada:** transfiere Quimeras (`FOLLOW` o montadas) y Drones "
                            "(sin patrulla) en un radio de **12m** al instante.",
                            "• **Escort Transfer:** teleports owned `FOLLOW`/ridden Chimeras and non-patrol "
                            "Drones within **12m** right alongside you.",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="acustica_micorrizal_senuelo",
            order=3,
            title=lt("Acústica Micorrizal y Señuelo Eco", "Mycorrhizal Acoustics & Decoy"),
            subtitle=lt(
                "MycorrhizalAcousticTracker (32m/18m) y neural_echo_decoy (16m/20m)",
                "MycorrhizalAcousticTracker (32m/18m) & neural_echo_decoy (16m/20m)",
            ),
            icon_item="mycelialmind:neural_echo_decoy",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("nodos_y_malla_sinaptica",),
            unlock_gate="always",
            registry_refs=("MycorrhizalAcousticTracker", "mycelialmind:neural_echo_decoy"),
            pages=(
                spot_page(
                    "acoustic_net",
                    "mycelialmind:neural_echo_decoy",
                    "Vibraciones y Silencio Acústico",
                    "Vibrations & Acoustic Silencing",
                    (
                        "Pisar `creeping_mycelium` alerta Nodos (`32m`) y patrullas (`18m`, `40t` CD). "
                        "`neural_echo_decoy` silencia **16m** y atrae en **20m**."
                    ),
                    (
                        "Stepping on `creeping_mycelium` pings Nodes (`32m`) and patrols (`18m`, `40t` CD). "
                        "`neural_echo_decoy` silences **16m** and lures **20m**."
                    ),
                ),
            ),
        ),
    )
    ch6 = CodexChapter(
        id="red_sinaptica",
        number=6,
        title=lt("Red Sináptica y Nodos", "Synaptic Network & Nodes"),
        subtitle=lt(
            "4 tipos de nodo, Injerto, Viaje Micorrizal 96m y Acústica",
            "4 node types, Synaptic Graft, 96m Fast Travel & Acoustics",
        ),
        icon_item="mycelialmind:symbiotic_node",
        entries=ch6_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 7: BIOTECNOLOGÍA, MESA SINÁPTICA, SIMBIOSIS Y DIPLOMACIA
    # ==================================================================
    ch7_entries = (
        CodexEntry(
            id="mesa_investigacion_sinaptica",
            order=1,
            title=lt("Mesa Sináptica y Niveles Lab 1–3", "Synaptic Bench & Lab Tiers 1–3"),
            subtitle=lt(
                "SynapticBenchBlockEntity, ranuras, tolvas y 4 síntesis base",
                "SynapticBenchBlockEntity, slots, hoppers & 4 core syntheses",
            ),
            icon_item="mycelialmind:synaptic_research_bench",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:synaptic_research_bench",
                "mycelialmind:synaptic_bench",
                "SynapticBenchRecipeDefinitions",
                "SidedSynapticBenchHandler",
            ),
            pages=(
                spot_page(
                    "bench_upgrades",
                    "mycelialmind:synaptic_research_bench",
                    "Mejora de Laboratorio (N1->N2->N3)",
                    "Laboratory Upgrades (T1->T2->T3)",
                    (
                        "`Shift + Clic`: **N1->N2** (`synaptic_core`, **25%** ahorro biomasa); "
                        "**N2->N3** (`neural_echo_core`/`overmind_essence`, **50%** y **x2** sueros)."
                    ),
                    (
                        "`Shift + Click`: **T1->T2** (`synaptic_core`, **25%** biomass save); "
                        "**T2->T3** (`neural_echo_core`/`overmind_essence`, **50%** save + **2x** serums)."
                    ),
                ),
                *chunk_paired_blocks(
                    "bench_recipes",
                    "Las 4 Síntesis Oficiales",
                    "The 4 Official Syntheses",
                    [
                        (
                            "• `symbiotic_mutagen`: `GeneticSample` (con rasgo) + `MycelialBiomass` + "
                            "`SynapticCore`.",
                            "• `symbiotic_mutagen`: `GeneticSample` (typed) + `MycelialBiomass` + "
                            "`SynapticCore`.",
                        ),
                        (
                            "• `counter_serum`: `GeneticSample` (con rasgo) + `LatentSpore` + (`PurifierGrenade` "
                            "o Amatista) -> **x1** (o **x2** en N3).",
                            "• `counter_serum`: `GeneticSample` (typed) + `LatentSpore` + (`PurifierGrenade` "
                            "or Amethyst) -> **1x** (**2x** at T3).",
                        ),
                        (
                            "• `concentrated_antidote`: `SporeAntidote` + `LatentSpore` + Amatista -> **x1** "
                            "(**x2** en N3).\n"
                            "• `chimera_ovum`: `GeneticSample` + `MycelialBiomass` + `NeuralEchoCore`. Tolvas "
                            "vía `SidedSynapticBenchHandler` (menú `synaptic_bench`).",
                            "• `concentrated_antidote`: `SporeAntidote` + `LatentSpore` + Amethyst -> **1x** "
                            "(**2x** at T3).\n"
                            "• `chimera_ovum`: `GeneticSample` + `MycelialBiomass` + `NeuralEchoCore`. Hoppers "
                            "via `SidedSynapticBenchHandler` (`synaptic_bench`).",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="simbiosis_mutagenos",
            order=2,
            title=lt("Simbiosis y los 5 Mutágenos", "Symbiosis & The 5 Mutagens"),
            subtitle=lt(
                "SymbioticMutagen: injertos corporales, límites y riesgos",
                "SymbioticMutagen: body implants, slot caps & risks",
            ),
            icon_item="mycelialmind:symbiotic_mutagen",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("mesa_investigacion_sinaptica",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:symbiosis",
                "mycelialmind:symbiotic_mutagen",
                "SymbioticMutagen",
                "CHITIN_THORNS",
                "KINETIC_STRAFE",
                "PYRO_GLAND",
                "BLAST_CARAPACE",
                "ARCANE_SYNAPSE",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "mutagens",
                    "Los 5 Mutágenos Simbióticos",
                    "The 5 Symbiotic Mutagens",
                    [
                        (
                            "• **Reglas (`symbiosis`):** máx. `maxMutagenSlots = 2`, añade **+15%** Amenaza/slot "
                            "(`1.0 + 0.15*n`), aplica `mycelial_infection` (`120t`) cada `60t` sobre micelio sin "
                            "proteger y se limpia al morir.",
                            "• **Rules (`symbiosis`):** max `maxMutagenSlots = 2`, adds **+15%** Threat/slot "
                            "(`1.0 + 0.15*n`), inflicts `mycelial_infection` (`120t`) every `60t` on bare "
                            "mycelium, and clears upon death.",
                        ),
                        (
                            "• `CHITIN_THORNS` (`chitin_thorns`, `MELEE`): **+2.0** Armadura y refleja **25%** del "
                            "daño melee recibido (mín. `1.0F`) como espinas.",
                            "• `CHITIN_THORNS` (`chitin_thorns`, `MELEE`): **+2.0** Armor and reflects **25%** "
                            "melee damage back as thorns (min `1.0F`).",
                        ),
                        (
                            "• `KINETIC_STRAFE` (`kinetic_strafe`, `PROJECTILE`): **30%** prob. de esquivar "
                            "proyectiles cancelando el golpe y ganar Velocidad I (`60t`).",
                            "• `KINETIC_STRAFE` (`kinetic_strafe`, `PROJECTILE`): **30%** chance to dodge "
                            "projectiles completely and burst Speed I (`60t`).",
                        ),
                        (
                            "• `PYRO_GLAND` (`pyro_gland`, `FIRE`): inmunidad total al fuego (cancela `IS_FIRE` y "
                            "apaga llamas) + ataques cuerpo a cuerpo prenden `3s`.",
                            "• `PYRO_GLAND` (`pyro_gland`, `FIRE`): full fire immunity (cancels `IS_FIRE` & "
                            "douses flames) + melee strikes ignite attackers for `3s`.",
                        ),
                        (
                            "• `BLAST_CARAPACE` (`blast_carapace`, `EXPLOSION`): **+0.5** Empuje Res. y reduce daño "
                            "explosivo al **55%** (**-45%**).\n"
                            "• `ARCANE_SYNAPSE` (`arcane_synapse`, `MAGIC`): Visión Nocturna continua y daño mágico "
                            "al **60%** (**-40%**).",
                            "• `BLAST_CARAPACE` (`blast_carapace`, `EXPLOSION`): **+0.5** KB Res. and cuts blast "
                            "damage to **55%** (**-45%**).\n"
                            "• `ARCANE_SYNAPSE` (`arcane_synapse`, `MAGIC`): permanent Night Vision and magic "
                            "damage reduced to **60%** (**-40%**).",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="cisma_y_diplomacia_quimica",
            order=3,
            title=lt("Cismas y Diplomacia Química", "Strain Schisms & Diplomacy"),
            subtitle=lt(
                "Cepas ORTHODOX/AMBER/CRIMSON, Pactos, Truce y Quorum",
                "ORTHODOX/AMBER/CRIMSON Strains, Pacts, Truce & Quorum",
            ),
            icon_item="mycelialmind:quorum_seal",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("simbiosis_mutagenos",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:diplomacy",
                "HiveStrain",
                "ORTHODOX",
                "AMBER",
                "CRIMSON",
                "Standing",
                "HOSTILE",
                "NEUTRAL",
                "PACT",
                "ALLIED",
                "mycelialmind:schism_injector",
                "mycelialmind:truce_pheromone",
                "mycelialmind:quorum_seal",
            ),
            pages=(
                spot_page(
                    "schism_mechanics",
                    "mycelialmind:schism_injector",
                    "Guerra Civil de Cepas",
                    "Strain Civil War",
                    (
                        "Usa `schism_injector` en un nodo/mob: `ORTHODOX` (#9B4DFF, -100) vs "
                        "`AMBER` (#F2A93B) vs `CRIMSON` (#E03C5C). Luchan entre sí."
                    ),
                    (
                        "Inject a node/mob with `schism_injector`: `ORTHODOX` (#9B4DFF, -100) vs "
                        "`AMBER` (#F2A93B) vs `CRIMSON` (#E03C5C) fight on sight."
                    ),
                ),
                *chunk_paired_blocks(
                    "diplomacy_rules",
                    "Reputación, Tributo y Traición",
                    "Standing, Tribute & Betrayal",
                    [
                        (
                            "• **Rangos (`Standing`, `-100..+100`):** `HOSTILE` `<=-40`, `NEUTRAL` `-39..24`, "
                            "`PACT` `>=+25` (`BASE` ignora `>4.5m`), `ALLIED` `>=+60` (`BASE` y `MUTATED`). "
                            "Ganar rep aplica **`-0.5x`** en la cepa opuesta.",
                            "• **Ranks (`Standing`, `-100..+100`):** `HOSTILE` `<=-40`, `NEUTRAL` `-39..24`, "
                            "`PACT` `>=+25` (`BASE` ignores `>4.5m`), `ALLIED` `>=+60` (`BASE` & `MUTATED`). "
                            "Positive rep applies **`-0.5x`** to rival strain.",
                        ),
                        (
                            "• **Herramientas y Reglas:** `ELITE` y Jefes siempre atacan. Traición: **-35** "
                            "rep y **30s** (`600t`) de bloqueo. `truce_pheromone` da tregua; `quorum_seal` "
                            "tributa **4** `mycelial_biomass` (**+12** rep) e invoca hasta **2** escoltas (**45s**).",
                            "• **Tools & Limits:** `ELITE` and Bosses never respect pacts. Betrayal triggers "
                            "**-35** rep and **30s** (`600t`) lockout. `truce_pheromone` pacifies temporarily; "
                            "`quorum_seal` tributes **4** biomass (**+12** rep) / max **2** escorts (**45s**).",
                        ),
                    ],
                ),
            ),
        ),
    )
    ch7 = CodexChapter(
        id="biotecnologia",
        number=7,
        title=lt("Biotecnología y Diplomacia", "Biotech & Diplomacy"),
        subtitle=lt(
            "Mesa Sináptica N1–N3, 5 Mutágenos, Cismas y Diplomacia Química",
            "Synaptic Bench T1–T3, 5 Mutagens, Schisms & Chemical Diplomacy",
        ),
        icon_item="mycelialmind:synaptic_research_bench",
        entries=ch7_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 8: QUITINA, ARSENAL, RELIQUIAS Y DEFENSA DEL BASTIÓN
    # ==================================================================
    ch8_entries = (
        CodexEntry(
            id="armadura_quitina_reactiva",
            order=1,
            title=lt("Armadura de Quitina Evolutiva", "Evolving Chitin Armor"),
            subtitle=lt(
                "Material chitin, DataComponents (armor_rank, trait_id) y evolución",
                "chitin material, DataComponents (armor_rank, trait_id) & tiers",
            ),
            icon_item="mycelialmind:chitin_chestplate",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:chitin",
                "mycelialmind:chitin_helmet",
                "mycelialmind:chitin_chestplate",
                "mycelialmind:chitin_leggings",
                "mycelialmind:chitin_boots",
                "mycelialmind:armor_rank",
                "mycelialmind:trait_id",
            ),
            pages=(
                spot_page(
                    "chitin_stats",
                    "mycelialmind:chitin_chestplate",
                    "Estadísticas del Material `chitin`",
                    "Stats of `chitin` Material",
                    (
                        "Defensa `[2, 5, 6, 2]` (Cuerpo **9**), Encant. **14**, Dureza **1.0F**, "
                        "repara con `chitin_plate`. Recorta infección **-20%** por pieza."
                    ),
                    (
                        "Armor `[2, 5, 6, 2]` (Body **9**), Ench **14**, Tough **1.0F**, repaired with "
                        "`chitin_plate`. Cuts infection duration **-20%** per piece."
                    ),
                ),
                *chunk_paired_blocks(
                    "chitin_evolution",
                    "Evolución y `DataComponents`",
                    "Evolution & `DataComponents`",
                    [
                        (
                            "• **`armor_rank` (`BASE` -> `MUTATED` -> `ELITE`):** sube a `MUTATED` con "
                            "`synaptic_core` (autorepara sobre micelio/Marea) y a `ELITE` con "
                            "`overmind_essence` o `tide_beacon`.",
                            "• **`armor_rank` (`BASE` -> `MUTATED` -> `ELITE`):** upgrade to `MUTATED` "
                            "with `synaptic_core` (self-repairs on mycelium/Tide) and to `ELITE` with "
                            "`overmind_essence` or `tide_beacon`.",
                        ),
                        (
                            "• **Reflejo Élite y `trait_id`:** `ELITE` otorga Absorción periódica y refleja "
                            "`1.25 * piezas` daño mágico; `trait_id` codifica afinidad elemental en piezas, "
                            "`genetic_sample` y sueros.",
                            "• **Elite Reflect & `trait_id`:** `ELITE` grants Absorption & reflects "
                            "`1.25 * pieces` magic dmg; `trait_id` stores elemental affinity on armor, "
                            "`genetic_sample`, and serums.",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="arsenal_tactico_completo",
            order=2,
            title=lt("Arsenal de Combate y Campo", "Combat & Field Arsenal"),
            subtitle=lt(
                "Espadas, lanzas, lanzadores, granadas y herramientas de campo",
                "Swords, lances, launchers, grenades & field biotech tools",
            ),
            icon_item="mycelialmind:symbiotic_blade",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("armadura_quitina_reactiva",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:symbiotic_blade",
                "mycelialmind:enzyme_lance",
                "mycelialmind:neutralizing_spore_launcher",
                "mycelialmind:purifier_grenade",
                "mycelialmind:bio_extractor",
                "mycelialmind:spore_antidote",
                "mycelialmind:concentrated_antidote",
                "mycelialmind:genetic_sample",
                "mycelialmind:chimera_ovum",
                "mycelialmind:neural_echo_core",
                "mycelialmind:mycelial_biomass",
                "mycelialmind:chitin_plate",
                "mycelialmind:latent_spore",
                "mycelialmind:synaptic_core",
                "mycelialmind:hive_heart",
                "mycelialmind:overmind_essence",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "arsenal",
                    "Armas y Consumibles Tácticos",
                    "Weapons & Tactical Consumables",
                    [
                        (
                            "• `symbiotic_blade`: espada simbiótica de tier diamante alimentada por `hive_heart`.\n"
                            "• `enzyme_lance`: lanza enzimática ligera que perfora tejido quitinoso.",
                            "• `symbiotic_blade`: diamond-class symbiotic blade powered by a `hive_heart`.\n"
                            "• `enzyme_lance`: lightweight enzymatic lance engineered to cleave chitin.",
                        ),
                        (
                            "• `neutralizing_spore_launcher` y `purifier_grenade`: proyectiles neutralizantes; "
                            "las granadas desincronizan escuadrones (**5s**) y dañan carroña/esporas.",
                            "• `neutralizing_spore_launcher` & `purifier_grenade`: neutralizing ranged suite; "
                            "grenades force **5s** squad Desync and purge spores.",
                        ),
                        (
                            "• `bio_extractor` -> extrae `genetic_sample`; `spore_antidote` / `concentrated_antidote` "
                            "curan infección y podredumbre; `chimera_ovum` eclosiona Quimeras.",
                            "• `bio_extractor` -> extracts `genetic_sample`; `spore_antidote` / "
                            "`concentrated_antidote` cure infection & rot; `chimera_ovum` hatches Chimeras.",
                        ),
                        (
                            "• **Reactivos Clave:** `mycelial_biomass`, `chitin_plate`, `latent_spore`, "
                            "`synaptic_core`, `neural_echo_core`, `hive_heart` y `overmind_essence`.",
                            "• **Key Reagents:** `mycelial_biomass`, `chitin_plate`, `latent_spore`, "
                            "`synaptic_core`, `neural_echo_core`, `hive_heart`, and `overmind_essence`.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="reliquias_endgame",
            order=3,
            title=lt("Las 4 Reliquias Endgame", "The 4 Endgame Relics"),
            subtitle=lt(
                "TideBeacon, SynapticSuppressor, RiftCatalyst y OvermindCrown",
                "TideBeacon, SynapticSuppressor, RiftCatalyst & OvermindCrown",
            ),
            icon_item="mycelialmind:overmind_crown",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("arsenal_tactico_completo",),
            unlock_gate="act_3",
            registry_refs=(
                "mycelialmind:tide_beacon",
                "mycelialmind:synaptic_suppressor",
                "mycelialmind:rift_catalyst",
                "mycelialmind:overmind_crown",
            ),
            pages=(
                spot_page(
                    "relic_crown",
                    "mycelialmind:overmind_crown",
                    "Corona de la Supramente",
                    "Overmind Crown",
                    (
                        "• **En el aire:** purga **15.0%** de Amenaza Global (cooldown **1200t / 60s**).\n"
                        "• **Sobre `purification_pylon`:** sobrecarga y limpia **12m** alrededor (**400t / 20s**)."
                    ),
                    (
                        "• **In mid-air:** purges **15.0%** Global Threat (**1200t / 60s** cooldown).\n"
                        "• **On `purification_pylon`:** overcharges and cleanses a **12m** radius (**400t / 20s**)."
                    ),
                ),
                *chunk_paired_blocks(
                    "relics_other",
                    "Faro, Supresor y Catalizador",
                    "Beacon, Suppressor & Catalyst",
                    [
                        (
                            "• `tide_beacon`: disipa la Marea Fúngica activa y recarga su núcleo con amatista y "
                            "quitina (`tide_beacon_recharge`).\n"
                            "• `synaptic_suppressor`: congela la adaptación de la Colmena (`120s` CD).",
                            "• `tide_beacon`: dispels an active Fungal Tide; rechargeable via amethyst and "
                            "chitin (`tide_beacon_recharge`).\n"
                            "• `synaptic_suppressor`: locks global Hive adaptation (`120s` CD).",
                        ),
                        (
                            "• `rift_catalyst`: usado dentro de un marco válido de `reinforced_bio_barrier`, "
                            "abre el portal `mycelial_rift` hacia el **Micelio Profundo** (`deep_mycelium`).",
                            "• `rift_catalyst`: ignites a `mycelial_rift` portal inside a valid "
                            "`reinforced_bio_barrier` frame toward **Deep Mycelium** (`deep_mycelium`).",
                        ),
                    ],
                ),
            ),
        ),
        CodexEntry(
            id="bastion_defensivo",
            order=4,
            title=lt("Defensa del Bastión y Torretas", "Bastion Defense & Turrets"),
            subtitle=lt(
                "Pilón (monument_focus), Torreta (3 tiers), Dron y Bio-Barrera",
                "Pylon (monument_focus), Turret (3 tiers), Drone & Bio-Barrier",
            ),
            icon_item="mycelialmind:purification_pylon",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=("reliquias_endgame",),
            unlock_gate="act_2",
            registry_refs=(
                "mycelialmind:purification_pylon",
                "mycelialmind:monument_focus",
                "mycelialmind:purification_turret",
                "mycelialmind:purifier_drone_core",
                "mycelialmind:reinforced_bio_barrier",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "bastion",
                    "Ingeniería del Bastión",
                    "Bastion Defense Engineering",
                    [
                        (
                            "• `purification_pylon` (`monument_focus`): frena expansión micelial (`24m` base, "
                            "`pylonPurificationRadius`), cura infección, impide carroña y sofoca si sufre **>=4** "
                            "`antibody_cyst` adyacentes en el Micelio Profundo.",
                            "• `purification_pylon` (`monument_focus`): blocks mycelium (`24m` default, "
                            "`pylonPurificationRadius`), cures infection, blocks carrion, and smothers if "
                            "**>=4** `antibody_cyst` cover its faces in Deep Mycelium.",
                        ),
                        (
                            "• `purification_turret` (Niveles 1–3): dispara esporas neutralizantes; reparable "
                            "con Antídoto, Quitina o Hierro. Si `AcidSpit` la atasca (**120t**), límpiala al vuelo.\n"
                            "• `purifier_drone_core` despliega al Dron; `reinforced_bio_barrier` blinda muros.",
                            "• `purification_turret` (Tiers 1–3): auto-fires neutralizing spores; repairable with "
                            "Antidote, Chitin, or Iron (cleanse if jammed **120t**).\n"
                            "• `purifier_drone_core` deploys Drones; `reinforced_bio_barrier` fortifies walls.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch8 = CodexChapter(
        id="arsenal_defensa",
        number=8,
        title=lt("Arsenal, Quitina y Bastión", "Arsenal, Chitin & Bastion"),
        subtitle=lt(
            "Quitina evolutiva, armas, 4 Reliquias, Pilones y Torretas",
            "Evolving Chitin, weapons, 4 Relics, Pylons & Turrets",
        ),
        icon_item="mycelialmind:overmind_crown",
        entries=ch8_entries,
        unlock_gate="always",
    )

    return (ch5, ch6, ch7, ch8)
