"""Chapters 9–12 of the Mycelial Mind Codex (Data/Economy, World & Dimension, Secrets & Compat, Tech & Commands)."""

from __future__ import annotations

from .helpers import callout_page, chunk_paired_blocks, grid_xy, lt, recipe_page, spot_page, text_page
from .schema import CodexChapter, CodexEntry


def build_chapters_9_to_12() -> tuple[CodexChapter, ...]:
    # ==================================================================
    # CHAPTER 9: FLORA, 24 BLOQUES, 53 ÍTEMS, 37 RECETAS, BREW Y 49 LOOT
    # ==================================================================
    ch9_blocks_items_pages = (
        *chunk_paired_blocks(
            "flora_eco",
            "Flora, Geología y Bloques Estructurales",
            "Flora, Geology & Structural Blocks",
            [
                (
                    "• `creeping_mycelium`: propaga micelio y transmisiones acústicas (**32m**); horneable a `mycelial_stone`.",
                    "• `creeping_mycelium`: spreads mycelium and acoustic tremors (**32m**); smelts into `mycelial_stone`.",
                ),
                (
                    "• `hyphal_vein`: vena negra en forma de rayo sobre tus rutas habituales (`CHARGED` si densidad >=60%). Cortable con Tijeras, Azada o Hoja Simbiótica.",
                    "• `hyphal_vein`: lightning-black overlay vein crawling over player routes (`CHARGED` >=60% density). Sever cleanly with Shears, Hoes, or Symbiotic Blade.",
                ),
                (
                    "• `spore_vent`: emite gases de infección/podredumbre; purificable con `spore_antidote` (Secreto #5).",
                    "• `spore_vent`: vents toxic spores/infection; purified with `spore_antidote` (Secret #5).",
                ),
                (
                    "• `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`: flora regenerativa con estado `MATURE` y señal Redstone; fertilizable para revelar Bóvedas (Secreto #8).",
                    "• `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`: regenerative flora (`MATURE` comparator signal); bone-mealing/pulsing uncovers Vaults (Secret #8).",
                ),
                (
                    "• `mycelial_stone`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `mycelial_rift`, `hive_altar`, `matriarch_altar`, `ephemeral_chitin`, `antibody_cyst`.",
                    "• `mycelial_stone`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `mycelial_rift`, `hive_altar`, `matriarch_altar`, `ephemeral_chitin`, `antibody_cyst`.",
                ),
            ],
        ),
        *chunk_paired_blocks(
            "all_23_blocks",
            "Los 24 Bloques (`ModBlocks`)",
            "All 24 Blocks (`ModBlocks`)",
            [
                (
                    "• **Red y Contención (9):** `creeping_mycelium`, `hyphal_vein`, `spore_vent`, `mycelial_node`, `incubator_node`, `sentinel_node`, `symbiotic_node`, `purification_pylon`, `purification_turret`.",
                    "• **Network & Containment (9):** `creeping_mycelium`, `hyphal_vein`, `spore_vent`, `mycelial_node`, `incubator_node`, `sentinel_node`, `symbiotic_node`, `purification_pylon`, `purification_turret`.",
                ),
                (
                    "• **Fortificación y Flora (8):** `reinforced_bio_barrier`, `synaptic_research_bench`, `hive_altar`, `matriarch_altar`, `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`, `antibody_cyst`.",
                    "• **Bastion & Flora (8):** `reinforced_bio_barrier`, `synaptic_research_bench`, `hive_altar`, `matriarch_altar`, `bioluminescent_mycelial_cap`, `spore_bulb`, `mycelial_tendrils`, `antibody_cyst`.",
                ),
                (
                    "• **Arquitectura y Dimensión (7):** `mycelial_stone`, `mycelial_rift`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `ephemeral_chitin`.",
                    "• **Architecture & Rift (7):** `mycelial_stone`, `mycelial_rift`, `chitin_pillar`, `mycelial_arch_brick`, `bio_chitin_lattice`, `luminescent_amber_vein`, `ephemeral_chitin`.",
                ),
            ],
        ),
        *chunk_paired_blocks(
            "all_35_items",
            "Ítems y Armadura (`ModItems` 36 + 17 Huevos)",
            "Items & Armor (`ModItems` 36 + 17 Spawn Eggs)",
            [
                (
                    "• **Instrumentos y Armas (10):** `bio_scanner`, `bio_extractor`, `enzyme_lance`, `neutralizing_spore_launcher`, `symbiotic_blade`, `purifier_grenade`, `synaptic_suppressor`, `overmind_crown`, `rift_catalyst`, `mycelial_codex`.",
                    "• **Tools & Weapons (10):** `bio_scanner`, `bio_extractor`, `enzyme_lance`, `neutralizing_spore_launcher`, `symbiotic_blade`, `purifier_grenade`, `synaptic_suppressor`, `overmind_crown`, `rift_catalyst`, `mycelial_codex`.",
                ),
                (
                    "• **Armadura y Sueros (10):** `chitin_helmet`, `chitin_chestplate`, `chitin_leggings`, `chitin_boots`, `spore_antidote`, `concentrated_antidote`, `genetic_sample`, `symbiotic_mutagen`, `counter_serum`, `tide_beacon`.",
                    "• **Armor & Serums (10):** `chitin_helmet`, `chitin_chestplate`, `chitin_leggings`, `chitin_boots`, `spore_antidote`, `concentrated_antidote`, `genetic_sample`, `symbiotic_mutagen`, `counter_serum`, `tide_beacon`.",
                ),
                (
                    "• **Diplomacia y Cognición (9):** `purifier_drone_core`, `neural_echo_core`, `synaptic_graft`, `neural_echo_decoy`, `schism_injector`, `truce_pheromone`, `quorum_seal`, `chimera_ovum`, `volatile_engram`.",
                    "• **Diplomacy & Cognition (9):** `purifier_drone_core`, `neural_echo_core`, `synaptic_graft`, `neural_echo_decoy`, `schism_injector`, `truce_pheromone`, `quorum_seal`, `chimera_ovum`, `volatile_engram`.",
                ),
                (
                    "• **Inyección y Reactivos (7):** `false_engram_injector`, `mycelial_biomass`, `chitin_plate`, `latent_spore`, `synaptic_core`, `hive_heart`, `overmind_essence` (+ **17** `*_spawn_egg` en Cap.5).",
                    "• **Deception & Reagents (7):** `false_engram_injector`, `mycelial_biomass`, `chitin_plate`, `latent_spore`, `synaptic_core`, `hive_heart`, `overmind_essence` (+ **17** `*_spawn_egg` in Ch.5).",
                ),
            ],
        ),
    )

    # All 36 datapack recipes rendered as native recipe widgets paired strictly by identical recipe type!
    ch9_recipe_pages = (
        recipe_page(
            "rec_craft_1",
            "mycelialmind:bio_scanner",
            "Bio-Escáner y Bio-Extractor",
            "Bio-Scanner & Bio-Extractor",
            "Telemetría inicial: Brújula, Redstone, Cobre y Espora (`bio_scanner`); Amatista, Hierro y Biomasa (`bio_extractor`).",
            "Starter telemetry: Compass, Redstone, Copper & Spore (`bio_scanner`); Amethyst, Iron & Biomass (`bio_extractor`).",
            recipe_id_2="mycelialmind:bio_extractor",
            title2_es="Bio-Extractor",
            title2_en="Bio-Extractor",
        ),
        recipe_page(
            "rec_craft_2",
            "mycelialmind:spore_antidote",
            "Antídoto de Esporas y Concentrado",
            "Spore Antidote & Concentrated Antidote",
            "Profilaxis clínica sin mesa: Frasco + `latent_spore` + Azúcar; concentra con Amatista.",
            "Field prophylaxis: Bottle + `latent_spore` + Sugar; concentrate further using Amethyst Shard.",
            recipe_id_2="mycelialmind:concentrated_antidote",
            title2_es="Antídoto Concentrado",
            title2_en="Concentrated Antidote",
        ),
        recipe_page(
            "rec_craft_3",
            "mycelialmind:chitin_helmet",
            "Casco y Peto de Quitina",
            "Chitin Helmet & Chestplate",
            "Forja con `chitin_plate`; el Peto integra un `synaptic_core` central.",
            "Forged from `chitin_plate`; Chestplate anchors a central `synaptic_core`.",
            recipe_id_2="mycelialmind:chitin_chestplate",
            title2_es="Peto de Quitina",
            title2_en="Chitin Chestplate",
        ),
        recipe_page(
            "rec_craft_4",
            "mycelialmind:chitin_leggings",
            "Grebas y Botas de Quitina",
            "Chitin Leggings & Boots",
            "Completan el conjunto de Quitina (`[2,5,6,2]`) reduciendo infección `-20%` por pieza.",
            "Complete the Chitin armor set (`[2,5,6,2]`), cutting infection `-20%` per worn piece.",
            recipe_id_2="mycelialmind:chitin_boots",
            title2_es="Botas de Quitina",
            title2_en="Chitin Boots",
        ),
        recipe_page(
            "rec_craft_5",
            "mycelialmind:enzyme_lance",
            "Lanza Enzimática y Hoja Simbiótica",
            "Enzyme Lance & Symbiotic Blade",
            "Armas melee progresivas: desde `chitin_plate` hasta `hive_heart` sobre Espada de Diamante.",
            "Progressive melee arms: early chitin thrust to `hive_heart` infused Diamond Sword.",
            recipe_id_2="mycelialmind:symbiotic_blade",
            title2_es="Hoja Simbiótica",
            title2_en="Symbiotic Blade",
        ),
        recipe_page(
            "rec_craft_6",
            "mycelialmind:neutralizing_spore_launcher",
            "Lanzador Neutralizante y Granada (x2)",
            "Spore Launcher & Purifier Grenade (2x)",
            "Artillería purificadora y granadas de área que rompen coordinación sináptica (`Desync`).",
            "Purifying ranged launcher and AoE grenades that trigger **5s** squad Desync.",
            recipe_id_2="mycelialmind:purifier_grenade",
            title2_es="Granada Purificadora",
            title2_en="Purifier Grenade",
        ),
        recipe_page(
            "rec_craft_7",
            "mycelialmind:purification_pylon",
            "Pilón y Torreta de Purificación",
            "Purification Pylon & Turret",
            "Perímetro defensivo (`pylonPurificationRadius = 24m`) y defensa automática (`purification_turret`).",
            "Base containment field (`24m` default) plus automated neutralizing turret.",
            recipe_id_2="mycelialmind:purification_turret",
            title2_es="Torreta Purificadora",
            title2_en="Purification Turret",
        ),
        recipe_page(
            "rec_craft_8",
            "mycelialmind:reinforced_bio_barrier",
            "Bio-Barrera (x4) y Núcleo de Dron",
            "Bio-Barrier (4x) & Drone Core",
            "Muro antibrecha + marco del portal; `purifier_drone_core` despliega soporte aéreo.",
            "Breach-resistant masonry + portal frame; `purifier_drone_core` spawns hovering support.",
            recipe_id_2="mycelialmind:purifier_drone_core",
            title2_es="Núcleo de Dron",
            title2_en="Drone Core",
        ),
        recipe_page(
            "rec_craft_9",
            "mycelialmind:synaptic_research_bench",
            "Mesa Sináptica e Injerto Sináptico",
            "Synaptic Bench & Synaptic Graft",
            "Estación de síntesis biotecnológica (`synaptic_research_bench`) e hijacker de nodos (`synaptic_graft`).",
            "Biotech synthesis station (`synaptic_research_bench`) and hostile node hijacker (`synaptic_graft`).",
            recipe_id_2="mycelialmind:synaptic_graft",
            title2_es="Injerto Sináptico",
            title2_en="Synaptic Graft",
        ),
        recipe_page(
            "rec_craft_10",
            "mycelialmind:neural_echo_decoy",
            "Señuelo Eco (x2) e Inyector Falso",
            "Neural Echo Decoy (2x) & False Injector",
            "Contramedidas acústicas (`neural_echo_decoy`) y sobre-adaptación Pavloviana (`false_engram_injector`).",
            "Acoustic lure/silencer (`neural_echo_decoy`) and Pavlovian over-adapter (`false_engram_injector`).",
            recipe_id_2="mycelialmind:false_engram_injector",
            title2_es="Inyector Engrama Falso",
            title2_en="False Engram Injector",
        ),
        recipe_page(
            "rec_craft_11",
            "mycelialmind:schism_injector",
            "Inyector de Cisma y Feromona (x2)",
            "Schism Injector & Truce Pheromone (2x)",
            "Induce guerra civil Ámbar/Carmesí o rocía tregua química temporal.",
            "Incites Amber vs Crimson civil war or mists temporary chemical truce.",
            recipe_id_2="mycelialmind:truce_pheromone",
            title2_es="Feromona de Tregua",
            title2_en="Truce Pheromone",
        ),
        recipe_page(
            "rec_craft_12",
            "mycelialmind:quorum_seal",
            "Sello de Quórum y Óvulo Quimérico",
            "Quorum Seal & Chimera Ovum",
            "Negocia escoltas aliadas (`quorum_seal`) o sintetiza el embrión modular (`chimera_ovum`).",
            "Requisitions allied strain escorts (`quorum_seal`) or crafts the modular `chimera_ovum`.",
            recipe_id_2="mycelialmind:chimera_ovum",
            title2_es="Óvulo Quimérico",
            title2_en="Chimera Ovum",
        ),
        recipe_page(
            "rec_craft_13",
            "mycelialmind:synaptic_suppressor",
            "Supresor Sináptico y Catalizador Grieta",
            "Synaptic Suppressor & Rift Catalyst",
            "Reliquias de Acto III+: congela aprendizaje global o abre grietas al Micelio Profundo.",
            "Act III+ relics: halts global adaptation (`120s`) or ignites Deep Mycelium portals.",
            recipe_id_2="mycelialmind:rift_catalyst",
            title2_es="Catalizador de Grieta",
            title2_en="Rift Catalyst",
        ),
        recipe_page(
            "rec_craft_14",
            "mycelialmind:overmind_crown",
            "Corona Supramente y Recarga Faro",
            "Overmind Crown & Beacon Recharge",
            "Pináculo Acto IV (`overmind_essence` + `hive_heart`); recarga `tide_beacon` con amatista y quitina.",
            "Act IV pinnacle (`overmind_essence` + `hive_heart`); restores `tide_beacon` charges.",
            recipe_id_2="mycelialmind:tide_beacon_recharge",
            title2_es="Recarga Faro de Marea",
            title2_en="Tide Beacon Recharge",
        ),
        recipe_page(
            "rec_craft_15",
            "mycelialmind:mycelial_codex",
            "Códice de la Colmena Viva",
            "Codex of the Living Hive",
            "Manual in-game sin mesa: Libro + `latent_spore` + `mycelial_biomass` (`mycelial_codex`).",
            "In-game manual: Book + `latent_spore` + `mycelial_biomass` (`mycelial_codex`).",
        ),
        recipe_page(
            "rec_smelt_1",
            "mycelialmind:smelt_cap_to_latent_spore",
            "Fundición: Espora y Piedra Micelial",
            "Smelting: Spore & Mycelial Stone",
            "Hornea `bioluminescent_mycelial_cap` -> `latent_spore` (0.15 XP) o `creeping_mycelium` -> `mycelial_stone`.",
            "Smelt `bioluminescent_mycelial_cap` -> `latent_spore` (0.15 XP) or `creeping_mycelium` -> `mycelial_stone`.",
            recipe_id_2="mycelialmind:smelt_creeping_mycelium_to_stone",
            title2_es="Piedra Micelial",
            title2_en="Mycelial Stone",
        ),
        recipe_page(
            "rec_smelt_2",
            "mycelialmind:smelt_cyst_to_chitin_plate",
            "Horno: Quiste -> Placa de Quitina",
            "Smelt: Cyst -> Chitin Plate",
            "Refina `antibody_cyst` en Horno normal (`200t`, `0.2 XP`) -> `chitin_plate`.",
            "Smelts `antibody_cyst` in a standard Furnace (`200t`, `0.2 XP`) -> `chitin_plate`.",
        ),
        recipe_page(
            "rec_smoke_1",
            "mycelialmind:smoke_cap_to_latent_spore",
            "Ahumador: Sombrero -> Espora",
            "Smoking: Cap -> Latent Spore",
            "Deshidratación rápida en Ahumador (`100t`, `0.15 XP`): `bioluminescent_mycelial_cap` -> `latent_spore`.",
            "Fast dehydration in Smoker (`100t`, `0.15 XP`): `bioluminescent_mycelial_cap` -> `latent_spore`.",
        ),
        recipe_page(
            "rec_blast_1",
            "mycelialmind:blast_cyst_to_chitin_plate",
            "Alto Horno: Quiste -> Quitina",
            "Blasting: Cyst -> Chitin Plate",
            "Calcina `antibody_cyst` al doble de velocidad (`100t`, `0.2 XP`) -> `chitin_plate`.",
            "Blasts `antibody_cyst` at double speed (`100t`, `0.2 XP`) -> `chitin_plate`.",
        ),
        recipe_page(
            "rec_stonecut_1",
            "mycelialmind:stonecut_mycelial_stone_to_arch_brick",
            "Cortapiedras: Ladrillo y Pilar",
            "Stonecutting: Arch Brick & Pillar",
            "`mycelial_stone` -> `mycelial_arch_brick` -> `chitin_pillar` sin desperdicio.",
            "`mycelial_stone` -> `mycelial_arch_brick` -> `chitin_pillar` with 1:1 precision.",
            recipe_id_2="mycelialmind:stonecut_arch_brick_to_chitin_pillar",
            title2_es="Pilar de Quitina",
            title2_en="Chitin Pillar",
        ),
        recipe_page(
            "rec_stonecut_2",
            "mycelialmind:stonecut_chitin_plate_to_lattice",
            "Cortapiedras: Celosía Bio-Quitina",
            "Stonecutting: Bio-Chitin Lattice",
            "1 `chitin_plate` se talla en **2** `bio_chitin_lattice` para galerías y ventanales.",
            "1 `chitin_plate` cuts into **2** `bio_chitin_lattice` panels.",
        ),
    )

    ch9_entries = (
        CodexEntry(
            id="catalogo_bloques_items",
            order=1,
            title=lt("Catálogo: 24 Bloques y 53 Ítems", "Catalog: 24 Blocks & 53 Items"),
            subtitle=lt(
                "Inventario íntegro de flora, bloques e ítems registrados",
                "Complete registry inventory of flora, blocks & items",
            ),
            icon_item="mycelialmind:mycelial_biomass",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:creeping_mycelium",
                "mycelialmind:hyphal_vein",
                "mycelialmind:spore_vent",
                "mycelialmind:mycelial_node",
                "mycelialmind:incubator_node",
                "mycelialmind:sentinel_node",
                "mycelialmind:symbiotic_node",
                "mycelialmind:purification_pylon",
                "mycelialmind:purification_turret",
                "mycelialmind:reinforced_bio_barrier",
                "mycelialmind:synaptic_research_bench",
                "mycelialmind:hive_altar",
                "mycelialmind:bioluminescent_mycelial_cap",
                "mycelialmind:spore_bulb",
                "mycelialmind:mycelial_tendrils",
                "mycelialmind:mycelial_stone",
                "mycelialmind:mycelial_rift",
                "mycelialmind:chitin_pillar",
                "mycelialmind:mycelial_arch_brick",
                "mycelialmind:bio_chitin_lattice",
                "mycelialmind:luminescent_amber_vein",
                "mycelialmind:matriarch_altar",
                "mycelialmind:ephemeral_chitin",
                "mycelialmind:antibody_cyst",
                "mycelialmind:chitin_helmet",
                "mycelialmind:chitin_chestplate",
                "mycelialmind:chitin_leggings",
                "mycelialmind:chitin_boots",
                "mycelialmind:bio_scanner",
                "mycelialmind:bio_extractor",
                "mycelialmind:enzyme_lance",
                "mycelialmind:neutralizing_spore_launcher",
                "mycelialmind:symbiotic_blade",
                "mycelialmind:spore_antidote",
                "mycelialmind:concentrated_antidote",
                "mycelialmind:purifier_grenade",
                "mycelialmind:synaptic_suppressor",
                "mycelialmind:genetic_sample",
                "mycelialmind:symbiotic_mutagen",
                "mycelialmind:counter_serum",
                "mycelialmind:rift_catalyst",
                "mycelialmind:overmind_essence",
                "mycelialmind:overmind_crown",
                "mycelialmind:tide_beacon",
                "mycelialmind:purifier_drone_core",
                "mycelialmind:neural_echo_core",
                "mycelialmind:synaptic_graft",
                "mycelialmind:neural_echo_decoy",
                "mycelialmind:schism_injector",
                "mycelialmind:truce_pheromone",
                "mycelialmind:quorum_seal",
                "mycelialmind:chimera_ovum",
                "mycelialmind:volatile_engram",
                "mycelialmind:false_engram_injector",
                "mycelialmind:mycelial_biomass",
                "mycelialmind:chitin_plate",
                "mycelialmind:latent_spore",
                "mycelialmind:synaptic_core",
                "mycelialmind:hive_heart",
                "mycelialmind:mycelial_codex",
            ),
            pages=ch9_blocks_items_pages,
        ),
        CodexEntry(
            id="recetas_36_datapack",
            order=2,
            title=lt("Recetario Total (37 Datapack)", "Complete Recipe Book (37 Datapack)"),
            subtitle=lt(
                "29 Mesa, 3 Horno, 1 Ahumador, 1 Alto Horno y 3 Cortapiedras",
                "29 Crafting, 3 Smelting, 1 Smoking, 1 Blasting & 3 Stonecutting",
            ),
            icon_item="mycelialmind:chitin_plate",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("catalogo_bloques_items",),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:bio_extractor",
                "mycelialmind:bio_scanner",
                "mycelialmind:blast_cyst_to_chitin_plate",
                "mycelialmind:chimera_ovum",
                "mycelialmind:chitin_boots",
                "mycelialmind:chitin_chestplate",
                "mycelialmind:chitin_helmet",
                "mycelialmind:chitin_leggings",
                "mycelialmind:concentrated_antidote",
                "mycelialmind:enzyme_lance",
                "mycelialmind:false_engram_injector",
                "mycelialmind:mycelial_codex",
                "mycelialmind:neural_echo_decoy",
                "mycelialmind:neutralizing_spore_launcher",
                "mycelialmind:overmind_crown",
                "mycelialmind:purification_pylon",
                "mycelialmind:purification_turret",
                "mycelialmind:purifier_drone_core",
                "mycelialmind:purifier_grenade",
                "mycelialmind:quorum_seal",
                "mycelialmind:reinforced_bio_barrier",
                "mycelialmind:rift_catalyst",
                "mycelialmind:schism_injector",
                "mycelialmind:smelt_cap_to_latent_spore",
                "mycelialmind:smelt_creeping_mycelium_to_stone",
                "mycelialmind:smelt_cyst_to_chitin_plate",
                "mycelialmind:smoke_cap_to_latent_spore",
                "mycelialmind:spore_antidote",
                "mycelialmind:stonecut_arch_brick_to_chitin_pillar",
                "mycelialmind:stonecut_chitin_plate_to_lattice",
                "mycelialmind:stonecut_mycelial_stone_to_arch_brick",
                "mycelialmind:symbiotic_blade",
                "mycelialmind:synaptic_graft",
                "mycelialmind:synaptic_research_bench",
                "mycelialmind:synaptic_suppressor",
                "mycelialmind:tide_beacon_recharge",
                "mycelialmind:truce_pheromone",
            ),
            pages=ch9_recipe_pages,
        ),
        CodexEntry(
            id="vanilla_brew_compost_trades",
            order=3,
            title=lt("Alambique, Compostaje y Comercio", "Brewing, Compost & Trades"),
            subtitle=lt(
                "Poción Awkward, 5 compostables NeoForge y ofertas Clérigo/Albañil",
                "Awkward brewing, 5 NeoForge compostables & Villager trades",
            ),
            icon_item="mycelialmind:latent_spore",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("recetas_36_datapack",),
            unlock_gate="always",
            registry_refs=(
                "MycelialVanillaIntegration",
                "compostables",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "vanilla_econ",
                    "Integración Económica Vanilla+",
                    "Vanilla+ Economy Integration",
                    [
                        (
                            "• **Alambique (`onRegisterBrewingRecipes`):** Poción Rara (`Awkward`) + "
                            "`latent_spore` -> `spore_antidote`; luego + Amatista -> `concentrated_antidote`.",
                            "• **Brewing Stand (`onRegisterBrewingRecipes`):** `Awkward Potion` + "
                            "`latent_spore` -> `spore_antidote`; + Amethyst Shard -> `concentrated_antidote`.",
                        ),
                        (
                            "• **5 Compostables (`compostables.json`):** `mycelial_biomass` (**0.65**), "
                            "`bioluminescent_mycelial_cap` (**0.65**), `spore_bulb` (**0.65**), "
                            "`latent_spore` (**0.50**), `mycelial_tendrils` (**0.50**).",
                            "• **5 Compostables (`compostables.json`):** `mycelial_biomass` (**0.65**), "
                            "`bioluminescent_mycelial_cap` (**0.65**), `spore_bulb` (**0.65**), "
                            "`latent_spore` (**0.50**), `mycelial_tendrils` (**0.50**).",
                        ),
                        (
                            "• **Aldeanos y Errante:** Clérigos (N2–N5) comercian `mycelial_biomass`, "
                            "`spore_antidote`, `latent_spore`, `concentrated_antidote`; Albañiles y "
                            "Vendedor Errante ofrecen bloques miceliales y `truce_pheromone`.",
                            "• **Villagers & Wandering Trader:** Clerics (Lv 2–5) trade `mycelial_biomass`, "
                            "`spore_antidote`, `latent_spore`, `concentrated_antidote`; Masons & Traders "
                            "carry mycelial stonework and `truce_pheromone`.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="botin_48_tablas",
            order=4,
            title=lt("Las 49 Tablas de Botín", "All 49 Loot Tables"),
            subtitle=lt(
                "21 bloques, 10 cofres y 17 entidades + multiplicadores",
                "21 blocks, 10 structure chests, 17 entities & multipliers",
            ),
            icon_item="mycelialmind:hive_heart",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=("catalogo_bloques_items",),
            unlock_gate="always",
            registry_refs=(
                "MycelialLootSystem",
                "blocks/antibody_cyst",
                "blocks/bio_chitin_lattice",
                "blocks/bioluminescent_mycelial_cap",
                "blocks/chitin_pillar",
                "blocks/creeping_mycelium",
                "blocks/hive_altar",
                "blocks/hyphal_vein",
                "blocks/incubator_node",
                "blocks/luminescent_amber_vein",
                "blocks/matriarch_altar",
                "blocks/mycelial_arch_brick",
                "blocks/mycelial_node",
                "blocks/mycelial_stone",
                "blocks/mycelial_tendrils",
                "blocks/purification_pylon",
                "blocks/purification_turret",
                "blocks/reinforced_bio_barrier",
                "blocks/sentinel_node",
                "blocks/spore_bulb",
                "blocks/spore_vent",
                "blocks/symbiotic_node",
                "blocks/synaptic_research_bench",
                "chests/citadel_amber_chapel",
                "chests/citadel_crimson_chapel",
                "chests/citadel_royal_crypt",
                "chests/outbreak_sealed_vault",
                "chests/sanctum_forbidden_lab",
                "chests/sanctum_ossuary_crypt",
                "chests/sanctum_spore_vault",
                "chests/surface_hive_cache",
                "chests/surface_hive_crypt",
                "chests/surface_hive_gallery",
                "entities/acid_spitter",
                "entities/apex_matriarch",
                "entities/burrower",
                "entities/chitin_brute",
                "entities/colony_warden",
                "entities/goliath_carrier",
                "entities/hive_core",
                "entities/leech_parasite",
                "entities/mycelial_chimera",
                "entities/mycelial_stalker",
                "entities/purifier_drone",
                "entities/pyro_shroom",
                "entities/spore_architect",
                "entities/spore_behemoth",
                "entities/sporeling",
                "entities/synapse_weaver",
                "entities/synaptic_mimic",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "loot_tables",
                    "Botín Multi-Pool (49 Tablas)",
                    "Multi-Pool Loot (49 Tables)",
                    [
                        (
                            "• **Bloques 1/4:** `blocks/antibody_cyst`, `blocks/bio_chitin_lattice`, "
                            "`blocks/bioluminescent_mycelial_cap`, `blocks/chitin_pillar`, `blocks/creeping_mycelium`.",
                            "• **Blocks 1/4:** `blocks/antibody_cyst`, `blocks/bio_chitin_lattice`, "
                            "`blocks/bioluminescent_mycelial_cap`, `blocks/chitin_pillar`, `blocks/creeping_mycelium`.",
                        ),
                        (
                            "• **Bloques 2/4:** `blocks/hive_altar`, `blocks/hyphal_vein`, `blocks/incubator_node`, "
                            "`blocks/luminescent_amber_vein`, `blocks/matriarch_altar`, `blocks/mycelial_arch_brick`, `blocks/mycelial_node`.",
                            "• **Blocks 2/4:** `blocks/hive_altar`, `blocks/hyphal_vein`, `blocks/incubator_node`, "
                            "`blocks/luminescent_amber_vein`, `blocks/matriarch_altar`, `blocks/mycelial_arch_brick`, `blocks/mycelial_node`.",
                        ),
                        (
                            "• **Bloques 3/4:** `blocks/mycelial_stone`, `blocks/mycelial_tendrils`, "
                            "`blocks/purification_pylon`, `blocks/purification_turret`, `blocks/reinforced_bio_barrier`.",
                            "• **Blocks 3/4:** `blocks/mycelial_stone`, `blocks/mycelial_tendrils`, "
                            "`blocks/purification_pylon`, `blocks/purification_turret`, `blocks/reinforced_bio_barrier`.",
                        ),
                        (
                            "• **Bloques 4/4:** `blocks/sentinel_node`, `blocks/spore_bulb`, "
                            "`blocks/spore_vent`, `blocks/symbiotic_node`, `blocks/synaptic_research_bench`.",
                            "• **Blocks 4/4:** `blocks/sentinel_node`, `blocks/spore_bulb`, "
                            "`blocks/spore_vent`, `blocks/symbiotic_node`, `blocks/synaptic_research_bench`.",
                        ),
                        (
                            "• **Cofres 1/2:** `chests/outbreak_sealed_vault`, `chests/surface_hive_cache`, "
                            "`chests/surface_hive_crypt`, `chests/surface_hive_gallery`, `chests/sanctum_spore_vault`.",
                            "• **Chests 1/2:** `chests/outbreak_sealed_vault`, `chests/surface_hive_cache`, "
                            "`chests/surface_hive_crypt`, `chests/surface_hive_gallery`, `chests/sanctum_spore_vault`.",
                        ),
                        (
                            "• **Cofres 2/2:** `chests/sanctum_forbidden_lab`, `chests/sanctum_ossuary_crypt`, "
                            "`chests/citadel_amber_chapel`, `chests/citadel_crimson_chapel`, `chests/citadel_royal_crypt`.",
                            "• **Chests 2/2:** `chests/sanctum_forbidden_lab`, `chests/sanctum_ossuary_crypt`, "
                            "`chests/citadel_amber_chapel`, `chests/citadel_crimson_chapel`, `chests/citadel_royal_crypt`.",
                        ),
                        (
                            "• **Entidades 1/3:** `entities/acid_spitter`, `entities/apex_matriarch`, "
                            "`entities/burrower`, `entities/chitin_brute`, `entities/colony_warden`, `entities/goliath_carrier`.",
                            "• **Entities 1/3:** `entities/acid_spitter`, `entities/apex_matriarch`, "
                            "`entities/burrower`, `entities/chitin_brute`, `entities/colony_warden`, `entities/goliath_carrier`.",
                        ),
                        (
                            "• **Entidades 2/3:** `entities/hive_core`, `entities/leech_parasite`, "
                            "`entities/mycelial_chimera`, `entities/mycelial_stalker`, `entities/purifier_drone`, `entities/pyro_shroom`.",
                            "• **Entities 2/3:** `entities/hive_core`, `entities/leech_parasite`, "
                            "`entities/mycelial_chimera`, `entities/mycelial_stalker`, `entities/purifier_drone`, `entities/pyro_shroom`.",
                        ),
                        (
                            "• **Entidades 3/3:** `entities/spore_architect`, `entities/spore_behemoth`, "
                            "`entities/sporeling`, `entities/synapse_weaver`, `entities/synaptic_mimic` (escala con Saqueo/Rango/Cepa).",
                            "• **Entities 3/3:** `entities/spore_architect`, `entities/spore_behemoth`, "
                            "`entities/sporeling`, `entities/synapse_weaver`, `entities/synaptic_mimic` (scales via Looting/Rank/Strain).",
                        ),
                    ],
                )
            ),
        ),
    )
    ch9 = CodexChapter(
        id="economia_datos",
        number=9,
        title=lt("Economía, Recetas y Botín", "Economy, Recipes & Loot"),
        subtitle=lt(
            "24 Bloques, 53 Ítems, 37 Recetas, Brews, Compost y 49 Botines",
            "24 Blocks, 53 Items, 37 Recipes, Brews, Compost & 49 Loots",
        ),
        icon_item="mycelialmind:chitin_plate",
        entries=ch9_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 10: MUNDO, ESTRUCTURAS, MICELIO PROFUNDO Y HOMEOSTASIS
    # ==================================================================
    ch10_entries = (
        CodexEntry(
            id="generacion_overworld_ruido",
            order=1,
            title=lt("Brotes y Colmenas del Overworld", "Overworld Outbreaks & Hives"),
            subtitle=lt(
                "MycelialOutbreak (56m), SurfaceHive (192m) y FastNoiseLite",
                "MycelialOutbreak (56m), SurfaceHive (192m) & FastNoiseLite",
            ),
            icon_item="mycelialmind:luminescent_amber_vein",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "mycelialmind:mycelial_outbreak",
                "mycelialmind:surface_hive",
                "mycelialmind:hive_tier_rarity",
                "StructureProgressionPolicy",
                "MycelialFastNoise",
                "FastNoiseLite",
                "WorldgenAuditEngine",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "worldgen_ow",
                    "Estructuras y Características Overworld",
                    "Overworld Worldgen Features",
                    [
                        (
                            "• `mycelial_outbreak`: garra orgánica altura **9**, conectividad 6-vecinos estricta "
                            "(0 flotantes), `MIN_OUTBREAK_SPACING = 56` y bóveda `outbreak_sealed_vault`.",
                            "• `mycelial_outbreak`: 9-tall fungus claw with strict 6-neighbor physical welding "
                            "(zero floaters), `MIN_OUTBREAK_SPACING = 56` & `outbreak_sealed_vault`.",
                        ),
                        (
                            "• `surface_hive`: megacápsula radio **8**, altura **16**, `MIN_HIVE_SPACING = 192`, "
                            "`MIN_NODE_CLEARANCE = 64`, cripta subterránea, galería superior, conductos sinápticos "
                            "y cofres `surface_hive_cache`/`gallery`/`crypt`.",
                            "• `surface_hive`: mega-capsule (r=8, h=16), `MIN_HIVE_SPACING = 192`, "
                            "`MIN_NODE_CLEARANCE = 64`, subterranean crypt, mezzanine gallery, conduits, "
                            "and `surface_hive_cache`/`gallery`/`crypt` chests.",
                        ),
                        (
                            "• **Motores (`hive_tier_rarity` / `FastNoiseLite`):** filtran rareza por Acto vía "
                            "`StructureProgressionPolicy` usando ruido `FastNoiseLite` (`SIMPLEX_SMOOTH`, "
                            "`CELLULAR`, `DOMAIN_WARP`) auditado con `WorldgenAuditEngine`.",
                            "• **Engines (`hive_tier_rarity` / `FastNoiseLite`):** gate rarity per Act via "
                            "`StructureProgressionPolicy` and `FastNoiseLite` cellular/domain-warped fields "
                            "verified by `WorldgenAuditEngine`.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="micelio_profundo_y_biomas",
            order=2,
            title=lt("Dimensión Micelio Profundo (3 Biomas)", "Deep Mycelium & 3 Biomes"),
            subtitle=lt(
                "Portal mycelial_rift, WfcCavernLayout y los 3 biomas",
                "mycelial_rift portal, WfcCavernLayout & all 3 biomes",
            ),
            icon_item="mycelialmind:rift_catalyst",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("generacion_overworld_ruido",),
            unlock_gate="act_3",
            registry_refs=(
                "mycelialmind:deep_mycelium",
                "mycelialmind:deep_mycelium_caverns",
                "mycelialmind:hive_heart_sanctum",
                "mycelialmind:latent_spore_forest",
                "WfcCavernLayout",
                "mycelialmind:mycelial_rift",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "deep_dim",
                    "Dimensión y Topología WFC",
                    "Dimension & WFC Topology",
                    [
                        (
                            "• **Dimensión `mycelialmind:deep_mycelium`:** accede abriendo `mycelial_rift` con "
                            "`rift_catalyst` en un marco de `reinforced_bio_barrier`. Sincroniza Actos desde "
                            "el Overworld al instante.",
                            "• **Dimension `mycelialmind:deep_mycelium`:** entered via `mycelial_rift` ignited by "
                            "`rift_catalyst` inside a `reinforced_bio_barrier` frame. Syncs Overworld Act tiers.",
                        ),
                        (
                            "• **3 Biomas:** `deep_mycelium_caverns` (cavernas arteriales), "
                            "`hive_heart_sanctum` (cámaras del santuario) y `latent_spore_forest` (bosques de "
                            "esporas latentes). Disponen de audio/niebla ambiental propia.",
                            "• **3 Biomes:** `deep_mycelium_caverns`, `hive_heart_sanctum`, and "
                            "`latent_spore_forest`, each with dedicated looping biome ambience & spore fog.",
                        ),
                        (
                            "• **`WfcCavernLayout` (`Tile`):** algoritmo Wave Function Collapse para cavernas "
                            "conectadas sin callejones sellados.",
                            "• **`WfcCavernLayout` (`Tile`):** Wave Function Collapse cavern layout solver "
                            "preventing disconnected pockets.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="megaestructuras_santuario_ciudadela",
            order=3,
            title=lt("Santuario Jigsaw y Ciudadela Matriarca", "Hive Sanctum & Matriarch Citadel"),
            subtitle=lt(
                "hive_sanctum (11 plantillas NBT) y matriarch_citadel",
                "hive_sanctum (11 NBT pieces) & matriarch_citadel",
            ),
            icon_item="mycelialmind:matriarch_altar",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("micelio_profundo_y_biomas",),
            unlock_gate="deep_mycelium",
            registry_refs=(
                "mycelialmind:hive_sanctum",
                "mycelialmind:matriarch_citadel",
                "mycelialmind:matriarch_citadel_piece",
                "mycelialmind:twisted_citadel",
                "mycelialmind:twisted_citadel_piece",
                "mycelialmind:architectural_accent",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "megastructures",
                    "Megaestructuras End-Game",
                    "Endgame Megastructures",
                    [
                        (
                            "• **`hive_sanctum` (Jigsaw, 11 piezas `.nbt`):** `sanctum_atrium`, `forbidden_lab`, "
                            "`ossuary_crypt`, `spore_vault`, `incubation_wing`, `ribbed_nave` y corredores "
                            "con botín `sanctum_*`.",
                            "• **`hive_sanctum` (Jigsaw, 11 `.nbt` templates):** `sanctum_atrium`, `forbidden_lab`, "
                            "`ossuary_crypt`, `spore_vault`, `incubation_wing`, `ribbed_nave` with `sanctum_*` chests.",
                        ),
                        (
                            "• **`matriarch_citadel` (`matriarch_citadel_piece`):** incluye capillas `AMBER` y "
                            "`CRIMSON` (`citadel_amber_chapel`, `citadel_crimson_chapel`), Cripta Real "
                            "(`citadel_royal_crypt`) y guardianes persistentes (`spore_behemoth` + 2 `colony_warden`).",
                            "• **`matriarch_citadel` (`matriarch_citadel_piece`):** features `AMBER` & `CRIMSON` "
                            "chapels (`citadel_*_chapel`), Royal Apse (`citadel_royal_crypt`), and persistent "
                            "`spore_behemoth` + 2 `colony_warden` guardians.",
                        ),
                        (
                            "• **`twisted_citadel` (`twisted_citadel_piece`, `architectural_accent`):** reflejo arquitectónico "
                            "en Micelio Profundo con plantillas `.nbt` (`spire_tower`, `gallery_hall`, `vault_bunker`) adaptadas a tu paleta y proporción.",
                            "• **`twisted_citadel` (`twisted_citadel_piece`, `architectural_accent`):** twisted architectural reflection "
                            "in Deep Mycelium using `.nbt` templates (`spire_tower`, `gallery_hall`, `vault_bunker`) adapted to your block palette and proportions.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="homeostasis_dimensional",
            order=4,
            title=lt("Homeostasis Dimensional y Quistes", "Dimensional Homeostasis & Cysts"),
            subtitle=lt(
                "3 etapas STABLE/IRRITATED/IMMUNE_RESPONSE, Lluvia Ácida y Quistes",
                "3 stages STABLE/IRRITATED/IMMUNE_RESPONSE, Acid Rain & Cysts",
            ),
            icon_item="mycelialmind:antibody_cyst",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=("micelio_profundo_y_biomas",),
            unlock_gate="deep_mycelium",
            registry_refs=(
                "DimensionalHomeostasisEngine",
                "HomeostasisStage",
                "STABLE",
                "IRRITATED",
                "IMMUNE_RESPONSE",
                "mycelialmind:antibody_cyst",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "homeostasis",
                    "Sistema Inmune de la Dimensión",
                    "Dimension Immune System",
                    [
                        (
                            "• **3 Etapas (`HomeostasisStage`):** `STABLE` (<40), `IRRITATED` (>=40) e "
                            "`IMMUNE_RESPONSE` (>=70; tormenta >=85). Explosiones, abuso de Pilones y Fase 2 "
                            "Matriarca (+20) sensibilizan cada chunk (`homeostasisSensitization` 0–3).",
                            "• **3 Stages (`HomeostasisStage`):** `STABLE` (<40), `IRRITATED` (>=40) & "
                            "`IMMUNE_RESPONSE` (>=70; cytokine >=85). Blasts, Pylon spam & Matriarch P2 (+20) "
                            "raise chunk `homeostasisSensitization` (0–3).",
                        ),
                        (
                            "• **Lluvia Ácida y `antibody_cyst`:** en `IMMUNE_RESPONSE` bajo cielo abierto cae "
                            "lluvia corrosiva (`homeostasisAcidRainDamage = 2.0`). Brotan quistes (máx. `64`/chunk); "
                            "si cubren **>=4 caras** (`SMOTHER_FACE_THRESHOLD`) sofocan Pilones hasta cosecharlos.",
                            "• **Acid Rain & `antibody_cyst`:** under open sky during `IMMUNE_RESPONSE`, acid "
                            "rain deals `2.0` dmg. Cysts grow (cap `64`/chunk); covering **>=4 faces** "
                            "(`SMOTHER_FACE_THRESHOLD`) smothers Pylons until harvested/sedated.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch10 = CodexChapter(
        id="mundo_dimension",
        number=10,
        title=lt("Mundo, Estructuras y Dimensión", "Worldgen, Structures & Dimension"),
        subtitle=lt(
            "Overworld, Micelio Profundo (3 biomas), Santuario, Ciudadela y Homeostasis",
            "Overworld, Deep Mycelium (3 biomes), Sanctum, Citadel & Homeostasis",
        ),
        icon_item="mycelialmind:rift_catalyst",
        entries=ch10_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 11: SECRETOS (8), LOGROS (30), REDSTONE Y COMPATIBILIDAD
    # ==================================================================
    ch11_entries = (
        CodexEntry(
            id="secretos_pistas_seguras",
            order=1,
            title=lt("Los 8 Secretos: Pistas de Campo", "The 8 Secrets: Spoiler-Free Hints"),
            subtitle=lt(
                "Indicios de investigación experimental y ecológica sin spoilers",
                "Non-spoiler research & ecological clues for all 8 secrets",
            ),
            icon_item="mycelialmind:bio_scanner",
            x=grid_xy(0, 3)[0],
            y=grid_xy(0, 3)[1],
            parents=(),
            unlock_gate="secret_hint",
            registry_refs=(
                "PlayerDiscoveryData",
                "mycelialmind:discovery",
                "SECRET_EXP_SYNAPTIC_CORE",
                "SECRET_EXP_TRUCE_PHEROMONE",
                "SECRET_EXP_VOLATILE_ENGRAM",
                "SECRET_EXP_NEURAL_ECHO",
                "SECRET_WORLD_VENT_PURIFICATION",
                "SECRET_WORLD_AMBER_CRYSTALLIZATION",
                "SECRET_PRIMORDIAL_SPECIMEN",
                "SECRET_SEALED_BIO_VAULT",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "secrets_hints",
                    "Pistas de Descubrimiento (1–8)",
                    "Discovery Hints (1–8)",
                    [
                        (
                            "• **Secreto 1 (Acto II / Lab N2):** prueba combinar Muestra Genética, Biomasa y Amatista.",
                            "• **Secret 1 (Act II / Lab T2):** test combining Genetic Sample, Biomass & Amethyst.",
                        ),
                        (
                            "• **Secreto 2 (Lab):** experimenta con Antídoto de Esporas, Biomasa y Amatista.",
                            "• **Secret 2 (Lab):** experiment with Spore Antidote, Biomass & Amethyst Shard.",
                        ),
                        (
                            "• **Secreto 3 (Acto II / Lab N2):** combina Muestra Genética, Espora Latente y Núcleo Sináptico.",
                            "• **Secret 3 (Act II / Lab T2):** combine Genetic Sample, Latent Spore & Synaptic Core.",
                        ),
                        (
                            "• **Secreto 4 (Acto III / Lab N3):** prueba Antídoto de Esporas, Espora Latente y Núcleo Sináptico.",
                            "• **Secret 4 (Act III / Lab T3):** test Spore Antidote, Latent Spore & Synaptic Core.",
                        ),
                        (
                            "• **Secreto 5 (Mundo):** aplica un Antídoto directamente sobre un Respiradero tóxico (`spore_vent`).",
                            "• **Secret 5 (World):** apply Spore Antidote directly onto a toxic `spore_vent`.",
                        ),
                        (
                            "• **Secreto 6 (Mundo):** interactúa con Amatista sobre Micelio Rastrero protegido por un Pilón.",
                            "• **Secret 6 (World):** use Amethyst Shard on `creeping_mycelium` inside a Pylon zone.",
                        ),
                        (
                            "• **Secreto 7 (Campo):** escanea, extrae o derrota un espécimen Primordial bioluminiscente.",
                            "• **Secret 7 (Field):** scan, extract, or defeat a glowing Primordial specimen.",
                        ),
                        (
                            "• **Secreto 8 (Campo):** pulsa el Bio-Escáner bajo brotes o fertiliza Sombreros bioluminiscentes.",
                            "• **Secret 8 (Field):** pulse Bio-Scanner near outcrops or fertilize Bioluminescent Caps.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="secretos_revelaciones_spoiler",
            order=2,
            title=lt("Los 8 Secretos: Revelación Exacta", "The 8 Secrets: Exact Solutions"),
            subtitle=lt(
                "[SPOILER] Fórmulas y disparadores exactos en código",
                "[SPOILER] Exact synthesis formulas & code triggers",
            ),
            icon_item="mycelialmind:neural_echo_core",
            x=grid_xy(1, 3)[0],
            y=grid_xy(1, 3)[1],
            parents=("secretos_pistas_seguras",),
            unlock_gate="secret_spoiler",
            registry_refs=(),
            pages=tuple(
                chunk_paired_blocks(
                    "secrets_spoilers",
                    "[SPOILER] Soluciones Exactas (1–8)",
                    "[SPOILER] Exact Solutions (1–8)",
                    [
                        (
                            "• **#1 `SECRET_EXP_SYNAPTIC_CORE`:** `GeneticSample` (con rasgo) + `MycelialBiomass` "
                            "+ `AmethystShard` en Acto II (`AWAKENED`) o Lab N2+ -> fabrica `synaptic_core` artificial.",
                            "• **#1 `SECRET_EXP_SYNAPTIC_CORE`:** `GeneticSample` (typed) + `MycelialBiomass` "
                            "+ `AmethystShard` at Act II (`AWAKENED`) or Lab T2+ -> yields `synaptic_core`.",
                        ),
                        (
                            "• **#2 `SECRET_EXP_TRUCE_PHEROMONE`:** `SporeAntidote` + `MycelialBiomass` + "
                            "`AmethystShard` en Mesa -> genera **x2** `truce_pheromone` (**x3** en Lab N3).",
                            "• **#2 `SECRET_EXP_TRUCE_PHEROMONE`:** `SporeAntidote` + `MycelialBiomass` + "
                            "`AmethystShard` in Bench -> yields **2x** `truce_pheromone` (**3x** at Lab T3).",
                        ),
                        (
                            "• **#3 `SECRET_EXP_VOLATILE_ENGRAM`:** `GeneticSample` (con rasgo) + `LatentSpore` + "
                            "`SynapticCore` (Acto II o Lab N2+) -> sintetiza `volatile_engram`.",
                            "• **#3 `SECRET_EXP_VOLATILE_ENGRAM`:** `GeneticSample` (typed) + `LatentSpore` + "
                            "`SynapticCore` (Act II or Lab T2+) -> yields `volatile_engram`.",
                        ),
                        (
                            "• **#4 `SECRET_EXP_NEURAL_ECHO`:** `SporeAntidote` + `LatentSpore` + `SynapticCore` "
                            "(Acto III `ADAPTIVE` o Lab N3) -> destila `neural_echo_core`.",
                            "• **#4 `SECRET_EXP_NEURAL_ECHO`:** `SporeAntidote` + `LatentSpore` + `SynapticCore` "
                            "(Act III `ADAPTIVE` or Lab T3) -> distills `neural_echo_core`.",
                        ),
                        (
                            "• **#5 `SECRET_WORLD_VENT_PURIFICATION`:** Clic con `spore_antidote` en `SporeVentBlock`.\n"
                            "• **#6 `SECRET_WORLD_AMBER_CRYSTALLIZATION`:** Amatista en `CreepingMycelium` bajo Pilón.",
                            "• **#5 `SECRET_WORLD_VENT_PURIFICATION`:** Use `spore_antidote` on `SporeVentBlock`.\n"
                            "• **#6 `SECRET_WORLD_AMBER_CRYSTALLIZATION`:** Amethyst on `CreepingMycelium` in Pylon zone.",
                        ),
                        (
                            "• **#7 `SECRET_PRIMORDIAL_SPECIMEN`:** escanear, extraer o matar un mob Primordial.\n"
                            "• **#8 `SECRET_SEALED_BIO_VAULT`:** localizar `outbreak_sealed_vault` vía Bio-Escáner "
                            "o fertilizar `BioluminescentMycelialCap`.",
                            "• **#7 `SECRET_PRIMORDIAL_SPECIMEN`:** scan, extract, or kill a Primordial mob.\n"
                            "• **#8 `SECRET_SEALED_BIO_VAULT`:** detect `outbreak_sealed_vault` via Bio-Scanner "
                            "or fertilize `BioluminescentMycelialCap`.",
                        ),
                    ],
                    is_spoiler=True,
                    accent_color="#FFB703",
                )
            ),
        ),
        CodexEntry(
            id="logros_30_advancements",
            order=3,
            title=lt("Los 30 Logros (`advancement`)", "All 30 Advancements"),
            subtitle=lt(
                "25 logros de campaña y 5 desbloqueos automáticos de recetas",
                "25 progression advancements & 5 recipe unlocks",
            ),
            icon_item="mycelialmind:overmind_crown",
            x=grid_xy(2, 3)[0],
            y=grid_xy(2, 3)[1],
            parents=("secretos_pistas_seguras",),
            unlock_gate="always",
            registry_refs=(
                "root",
                "first_adaptation",
                "genetic_sampler",
                "sever_the_network",
                "biotech_research",
                "targeted_purge",
                "chitin_bulwark",
                "tier2_awakening",
                "base_sentinel",
                "bastion_tier3",
                "purifier_drone_deployed",
                "symbiotic_vessel",
                "synaptic_graft_network",
                "pavlovian_deceiver",
                "chemical_diplomat",
                "architect_bane",
                "chimera_bioforge",
                "hive_core_slayer",
                "synaptic_mastery",
                "tide_breaker",
                "counteroffensive_master",
                "enter_deep_mycelium",
                "immune_homeostasis",
                "overmind_ascendant",
                "apex_matriarch_slayer",
                "recipes/unlock_biomass_recipes",
                "recipes/unlock_chitin_recipes",
                "recipes/unlock_hive_heart_recipes",
                "recipes/unlock_latent_spore_recipes",
                "recipes/unlock_synaptic_core_recipes",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "advancements",
                    "Cadena Completa de 30 Logros",
                    "Complete Chain of 30 Advancements",
                    [
                        (
                            "• **Acto I y Base (9):** `root`, `first_adaptation`, `genetic_sampler`, "
                            "`sever_the_network`, `biotech_research`, `targeted_purge`, `chitin_bulwark`, "
                            "`tier2_awakening`, `base_sentinel`.",
                            "• **Act I & Bastion (9):** `root`, `first_adaptation`, `genetic_sampler`, "
                            "`sever_the_network`, `biotech_research`, `targeted_purge`, `chitin_bulwark`, "
                            "`tier2_awakening`, `base_sentinel`.",
                        ),
                        (
                            "• **Táctica y Biotecnología (9):** `bastion_tier3`, `purifier_drone_deployed`, "
                            "`symbiotic_vessel`, `synaptic_graft_network`, `pavlovian_deceiver`, "
                            "`chemical_diplomat`, `architect_bane`, `chimera_bioforge`, `hive_core_slayer`.",
                            "• **Tactics & Biotech (9):** `bastion_tier3`, `purifier_drone_deployed`, "
                            "`symbiotic_vessel`, `synaptic_graft_network`, `pavlovian_deceiver`, "
                            "`chemical_diplomat`, `architect_bane`, `chimera_bioforge`, `hive_core_slayer`.",
                        ),
                        (
                            "• **Endgame y Maestría (7):** `synaptic_mastery`, `tide_breaker`, "
                            "`counteroffensive_master`, `enter_deep_mycelium`, `immune_homeostasis`, "
                            "`overmind_ascendant`, `apex_matriarch_slayer` (`discovery`).",
                            "• **Endgame & Mastery (7):** `synaptic_mastery`, `tide_breaker`, "
                            "`counteroffensive_master`, `enter_deep_mycelium`, `immune_homeostasis`, "
                            "`overmind_ascendant`, `apex_matriarch_slayer` (`discovery`).",
                        ),
                        (
                            "• **Desbloqueos de Receta (5):** `recipes/unlock_biomass_recipes`, "
                            "`recipes/unlock_chitin_recipes`, `recipes/unlock_hive_heart_recipes`, "
                            "`recipes/unlock_latent_spore_recipes`, `recipes/unlock_synaptic_core_recipes`.",
                            "• **Recipe Unlocks (5):** `recipes/unlock_biomass_recipes`, "
                            "`recipes/unlock_chitin_recipes`, `recipes/unlock_hive_heart_recipes`, "
                            "`recipes/unlock_latent_spore_recipes`, `recipes/unlock_synaptic_core_recipes`.",
                        ),
                    ],
                )
            ),
        ),
        CodexEntry(
            id="redstone_y_ecosistema_mods",
            order=4,
            title=lt("Redstone (7) y Ecosistema de Mods", "Redstone (7) & Mod Ecosystem"),
            subtitle=lt(
                "MycelialRedstoneTelemetry + SBL, gdx-AI, Jade, JEI, EMI, Iris, Modonomicon",
                "MycelialRedstoneTelemetry + SBL, gdx-AI, Jade, JEI, EMI, Iris, Modonomicon",
            ),
            icon_item="mycelialmind:purification_turret",
            x=grid_xy(3, 3)[0],
            y=grid_xy(3, 3)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=(
                "MycelialRedstoneTelemetry",
                "MycelialJadePlugin",
                "MycelialJeiPlugin",
                "MycelialEmiPlugin",
                "MycelialIrisCompat",
                "MycelialModonomiconCompat",
                "MycelialAzureLibCompat",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "redstone_compat",
                    "Telemetría Redstone y Compatibilidad",
                    "Redstone Telemetry & Mod Compat",
                    [
                        (
                            "• **Comparadores Redstone 1/2:** `antibody_cyst` (estrés chunk 1–15), "
                            "`bioluminescent_mycelial_cap` y `mycelial_tendrils` (`MATURE`), `mycelial_node`/`symbiotic_node`.",
                            "• **Redstone Comparators 1/2:** `antibody_cyst` (chunk stress 1–15), "
                            "`bioluminescent_mycelial_cap` & `mycelial_tendrils` (`MATURE`), `mycelial_node`/`symbiotic_node`.",
                        ),
                        (
                            "• **Comparadores Redstone 2/2 (`MycelialRedstoneTelemetry`):** `purification_pylon` "
                            "(estado/sofoco), `purification_turret` (tier/atasco) y `synaptic_research_bench` (entradas/salida).",
                            "• **Redstone Comparators 2/2 (`MycelialRedstoneTelemetry`):** `purification_pylon` "
                            "(state/smother), `purification_turret` (tier/jam) & `synaptic_research_bench` (inputs/output).",
                        ),
                        (
                            "• **Compat IA/Mundo:** **Vanilla+** (Trades/Brew/Compost/Loot), **SmartBrainLib** "
                            "(`MycelialSblBridge`), **gdx-AI** (`HiveTacticalTree`) y **Jade** (`MycelialJadePlugin`).",
                            "• **AI/World Compat:** **Vanilla+** (Trades/Brew/Compost/Loot), **SmartBrainLib** "
                            "(`MycelialSblBridge`), **gdx-AI** (`HiveTacticalTree`) & **Jade** (`MycelialJadePlugin`).",
                        ),
                        (
                            "• **Compat UI/Render:** **JEI** (`MycelialJeiPlugin`), **EMI** (`MycelialEmiPlugin`), "
                            "**Iris** (`MycelialIrisCompat` PBR), **Modonomicon** (`MycelialModonomiconCompat`) y **AzureLib**.",
                            "• **UI/Render Compat:** **JEI** (`MycelialJeiPlugin`), **EMI** (`MycelialEmiPlugin`), "
                            "**Iris** (`MycelialIrisCompat` PBR), **Modonomicon** (`MycelialModonomiconCompat`) & **AzureLib**.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch11 = CodexChapter(
        id="secretos_logros",
        number=11,
        title=lt("Secretos, Logros y Compat", "Secrets, Advancements & Compat"),
        subtitle=lt(
            "8 Secretos (Pistas + Spoilers), 30 Logros, 7 Redstone y 9 Compat",
            "8 Secrets (Hints + Spoilers), 30 Advancements, 7 Redstone & 9 Compat",
        ),
        icon_item="mycelialmind:neural_echo_core",
        entries=ch11_entries,
        unlock_gate="always",
    )

    # ==================================================================
    # CHAPTER 12: ARQUITECTURA VISUAL/AUDIO, 48 CONFIGS, RED Y COMANDOS
    # ==================================================================
    all_181_sounds = [
        "hive_mutate_tier2", "synaptic_pulse", "boss_phase_shift", "suppressor_discharge",
        "fungal_tide_horn", "squad_desync", "corrosive_jam", "immune_surge",
        "mob_promote_mutated", "mob_promote_elite", "mob_dawn_retreat", "infected_mob_convert",
        "mycorrhizal_tremor", "homeostasis_heartbeat", "antibody_cyst_encapsulate", "acid_rain_sizzle",
        "necrophagy_feast", "swarm_flank_call", "biome_caverns_loop", "biome_caverns_mood",
        "biome_caverns_additions", "biome_sanctum_loop", "biome_sanctum_mood", "biome_sanctum_additions",
        "biome_forest_loop", "biome_forest_mood", "biome_forest_additions", "fungal_tide_ambience",
        "purification_pylon_hum", "hive_trait_adapt", "hive_synergy_unlock", "hive_threat_milestone",
        "sporeling_chitter", "sporeling_hurt", "sporeling_death", "sporeling_burst",
        "chitin_brute_growl", "chitin_brute_hurt", "chitin_brute_death", "chitin_brute_smash",
        "stalker_hiss", "stalker_hurt", "stalker_death", "stalker_pounce",
        "acid_spitter_gurgle", "acid_spitter_hurt", "acid_spitter_death", "acid_spitter_spit",
        "burrower_grind", "burrower_hurt", "burrower_death", "burrower_tunnel",
        "pyro_shroom_crackle", "pyro_shroom_hurt", "pyro_shroom_death", "pyro_shroom_ignite",
        "synapse_weaver_hum", "synapse_weaver_hurt", "synapse_weaver_death", "synapse_weaver_buff",
        "goliath_carrier_thud", "goliath_carrier_hurt", "goliath_carrier_death", "goliath_carrier_spawn",
        "leech_parasite_screech", "leech_parasite_hurt", "leech_parasite_death", "leech_parasite_siphon",
        "colony_warden_bellow", "colony_warden_hurt", "colony_warden_death", "colony_warden_shockwave",
        "spore_architect_pulse", "spore_architect_hurt", "spore_architect_death", "spore_architect_construct",
        "synaptic_mimic_echo", "synaptic_mimic_hurt", "synaptic_mimic_death", "synaptic_mimic_strike",
        "spore_behemoth_roar", "spore_behemoth_hurt", "spore_behemoth_death", "spore_behemoth_slam",
        "hive_core_resonance", "hive_core_hurt", "hive_core_death", "hive_core_shield",
        "apex_matriarch_wail", "apex_matriarch_hurt", "apex_matriarch_death", "apex_matriarch_barrage",
        "purifier_drone_hum", "purifier_drone_hurt", "purifier_drone_death", "purifier_drone_fire",
        "mycelial_chimera_growl", "mycelial_chimera_hurt", "mycelial_chimera_death", "chimera_roar",
        "mycelial_chimera_strike", "mob_step_chitin", "mob_step_arachnid", "mob_step_heavy",
        "mob_step_colossal", "mob_step_hover", "block_mycelium_break", "block_mycelium_step",
        "block_mycelium_place", "block_mycelium_hit", "block_mycelium_fall", "block_synaptic_node_break",
        "block_synaptic_node_step", "block_synaptic_node_place", "block_synaptic_node_hit", "block_synaptic_node_fall",
        "block_chitin_break", "block_chitin_step", "block_chitin_place", "block_chitin_hit",
        "block_chitin_fall", "block_mycelial_stone_break", "block_mycelial_stone_step", "block_mycelial_stone_place",
        "block_mycelial_stone_hit", "block_mycelial_stone_fall", "block_purification_tech_break",
        "block_purification_tech_step", "block_purification_tech_place", "block_purification_tech_hit",
        "block_purification_tech_fall", "block_antibody_cyst_break", "block_antibody_cyst_step",
        "block_antibody_cyst_place", "block_antibody_cyst_hit", "block_antibody_cyst_fall",
        "block_hive_altar_break", "block_hive_altar_step", "block_hive_altar_place",
        "block_hive_altar_hit", "block_hive_altar_fall", "spore_vent_puff",
        "purification_pylon_pulse", "purification_turret_fire", "purification_turret_upgrade",
        "purification_turret_repair", "synaptic_node_mature", "symbiotic_node_charge",
        "symbiotic_node_teleport", "spore_bulb_burst", "ephemeral_chitin_crumble",
        "mycelial_rift_hum", "hive_altar_awaken", "matriarch_altar_awaken",
        "synaptic_bench_synthesize", "bio_scanner_ping", "bio_scanner_thermal_pulse",
        "bio_extractor_draw", "symbiotic_blade_slash", "enzyme_lance_thrust",
        "spore_launcher_fire", "purifier_grenade_throw", "purifier_grenade_detonate",
        "acid_spit_impact", "neutralizing_spore_impact", "chitin_armor_equip",
        "chitin_armor_evolve", "spore_antidote_cure", "counter_serum_inject",
        "symbiotic_mutagen_implant", "synaptic_graft_splice", "neural_decoy_deploy",
        "volatile_engram_pulse", "schism_injector_sting", "truce_pheromone_spray",
        "quorum_seal_pact", "chimera_ovum_hatch", "purifier_drone_deploy",
        "tide_beacon_activate", "rift_catalyst_ignite", "overmind_crown_surge",
    ]
    sound_blocks = [
        (
            "• **Estudio Visual y Audio:** 17 `Model` multi-articulados (UV manifests), texturas LabPBR "
            "`_n`/`_s`, `ClientSoundscapeDirector` y **181 eventos `ModSounds`** (**489** `.ogg` mono `44.1kHz`).",
            "• **Visual & Audio Studio:** 17 articulated 3D models (UV manifests), LabPBR `_n`/`_s` "
            "maps, `ClientSoundscapeDirector`, and **181 `ModSounds`** (**489** mono `44.1kHz` `.ogg`).",
        ),
        (
            "• **Dinámica Vocal:** tono modulable (`0.45–1.85`) según Rango (`BASE/MUTATED/ELITE`), Cepa "
            "(`AMBER x1.06`, `CRIMSON x0.93`), Frenesí (`x1.10`), Líder (`x0.94`), `Desync` (`x0.86`) u Oclusión (`x0.92`).",
            "• **Vocal Pitch Formula:** dynamic multiplier (`0.45–1.85`) by Rank, Strain (`AMBER x1.06`, "
            "`CRIMSON x0.93`), Frenzy (`x1.10`), Leader (`x0.94`), `Desync` (`x0.86`) & Occlusion (`x0.92`).",
        ),
    ]
    for i in range(0, len(all_181_sounds), 6):
        group_str = ", ".join(all_181_sounds[i : i + 6])
        num_range = f"{i+1}–{min(i+6, len(all_181_sounds))}"
        sound_blocks.append((f"• **Sonidos ({num_range}):** {group_str}.", f"• **Sounds ({num_range}):** {group_str}."))

    client_cfg_blocks = [
        ("• `[hud] hudAnchor` = `TOP_LEFT` (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`).",
         "• `[hud] hudAnchor` = `TOP_LEFT` (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`)."),
        ("• `[hud] hudOffsetX` = `0` (`-200..200`); `hudOffsetY` = `0` (`-200..200`).",
         "• `[hud] hudOffsetX` = `0` (`-200..200`); `hudOffsetY` = `0` (`-200..200`)."),
        ("• `[hud] hudCompactMode` = `false`; `hudRequireHeld` = `true`; `showSynapticVisionBorder` = `true`.",
         "• `[hud] hudCompactMode` = `false`; `hudRequireHeld` = `true`; `showSynapticVisionBorder` = `true`."),
        ("• `[atmosphere] sporeFogDensityScale` = `1.0` (`0.25..1.0`).",
         "• `[atmosphere] sporeFogDensityScale` = `1.0` (`0.25..1.0`)."),
        ("• `[audio] ambientSoundscapeVolume` = `1.0` (`0..1`); `hiveAdaptationAudioAlerts` = `true`; `caveSoundOcclusion` = `true`.",
         "• `[audio] ambientSoundscapeVolume` = `1.0` (`0..1`); `hiveAdaptationAudioAlerts` = `true`; `caveSoundOcclusion` = `true`."),
        ("• `[particles]` `particleDensityScale` = `1.0` (`0.25..1.5`); `enableAdditiveParticleGlow` = `true`.",
         "• `[particles]` `particleDensityScale` = `1.0` (`0.25..1.5`); `enableAdditiveParticleGlow` = `true`."),
    ]
    server_cfg_blocks = [
        ("• `[hive_memory]` `learningMultiplier`=`1.0` (`0.1..10`), `decayPerMinute`=`2.0` (`0..50`), `traitActivationThreshold`=`70.0` (`5..500`).",
         "• `[hive_memory]` `learningMultiplier`=`1.0` (`0.1..10`), `decayPerMinute`=`2.0` (`0..50`), `traitActivationThreshold`=`70.0` (`5..500`)."),
        ("• `[hive_memory]` `tier2ActivationThreshold`=`200.0` (`25..500`), `tier2DecayMultiplier`=`1.75` (`1..5`), `synergiesEnabled`=`true`.",
         "• `[hive_memory]` `tier2ActivationThreshold`=`200.0` (`25..500`), `tier2DecayMultiplier`=`1.75` (`1..5`), `synergiesEnabled`=`true`."),
        ("• `[hive_memory]` `suppressorCooldownSeconds`=`120` (`15..600`), `projectileDeflectChance`=`0.30` (`0..0.95`), `symbiosisThreatMultiplierPerSlot`=`0.15` (`0..1`).",
         "• `[hive_memory]` `suppressorCooldownSeconds`=`120` (`15..600`), `projectileDeflectChance`=`0.30` (`0..0.95`), `symbiosisThreatMultiplierPerSlot`=`0.15` (`0..1`)."),
        ("• `[hive_memory]` `maxMutagenSlots`=`2` (`1..5`), `counterSerumBleedRatio`=`0.50` (`0.1..0.95`).",
         "• `[hive_memory]` `maxMutagenSlots`=`2` (`1..5`), `counterSerumBleedRatio`=`0.50` (`0.1..0.95`)."),
        ("• `[territory]` `myceliumSpreadEnabled`=`true`, `myceliumSpreadChance`=`0.12` (`0..1`), `pylonPurificationRadius`=`24` (`4..64`).",
         "• `[territory]` `myceliumSpreadEnabled`=`true`, `myceliumSpreadChance`=`0.12` (`0..1`), `pylonPurificationRadius`=`24` (`4..64`)."),
        ("• `[territory]` `synapticNodeLinkRadius`=`28` (`8..64`), `nodeDestructionStimulusBleed`=`18.0` (`0..100`), `burrowerBlockBreaking`=`true`.",
         "• `[territory]` `synapticNodeLinkRadius`=`28` (`8..64`), `nodeDestructionStimulusBleed`=`18.0` (`0..100`), `burrowerBlockBreaking`=`true`."),
        ("• `[territory]` `playerCountScaling`=`0.15` (`0..1`), `wavesEnabled`=`true`, `waveCooldownTicks`=`3600` (`200..72000`).",
         "• `[territory]` `playerCountScaling`=`0.15` (`0..1`), `wavesEnabled`=`true`, `waveCooldownTicks`=`3600` (`200..72000`)."),
        ("• `[territory]` `autonomousExpansionEnabled`=`true`, `aggressiveVillageAssimilation`=`true`, `maxAutonomousNodes`=`32` (`4..128`), `minNodeSpacing`=`18` (`8..48`).",
         "• `[territory]` `autonomousExpansionEnabled`=`true`, `aggressiveVillageAssimilation`=`true`, `maxAutonomousNodes`=`32` (`4..128`), `minNodeSpacing`=`18` (`8..48`)."),
        ("• `[territory]` `infectionConversionEnabled`=`true`, `fungalTideEnabled`=`true`, `fungalTideDurationTicks`=`6000` (`600..24000`).",
         "• `[territory]` `infectionConversionEnabled`=`true`, `fungalTideEnabled`=`true`, `fungalTideDurationTicks`=`6000` (`600..24000`)."),
        ("• `[territory]` `fungalTideCooldownTicks`=`24000` (`1200..72000`), `mycorrhizalAcousticsEnabled`=`true`, `maxSymbioticNodes`=`4` (`1..16`).",
         "• `[territory]` `fungalTideCooldownTicks`=`24000` (`1200..72000`), `mycorrhizalAcousticsEnabled`=`true`, `maxSymbioticNodes`=`4` (`1..16`)."),
        ("• `[territory]` `mycorrhizalTravelRange`=`96` (`16..256`), `mycorrhizalTravelCooldownTicks`=`1500` (`100..6000`).",
         "• `[territory]` `mycorrhizalTravelRange`=`96` (`16..256`), `mycorrhizalTravelCooldownTicks`=`1500` (`100..6000`)."),
        ("• `[territory]` `progressiveThreatScaling`=`true`, `deathThreatPenalty`=`5.0` (`0..25`).",
         "• `[territory]` `progressiveThreatScaling`=`true`, `deathThreatPenalty`=`5.0` (`0..25`)."),
        ("• `[dimensional_homeostasis]` `homeostasisEnabled`=`true`, `homeostasisExplosionStressScale`=`1.0` (`0.1..5`), `homeostasisAcidRainDamage`=`2.0` (`0.5..10`).",
         "• `[dimensional_homeostasis]` `homeostasisEnabled`=`true`, `homeostasisExplosionStressScale`=`1.0` (`0.1..5`), `homeostasisAcidRainDamage`=`2.0` (`0.5..10`)."),
        ("• `[dimensional_homeostasis]` `homeostasisAntibodyEncapsulationEnabled`=`true`, `homeostasisMaxCystsPerChunk`=`64` (`8..256`).",
         "• `[dimensional_homeostasis]` `homeostasisAntibodyEncapsulationEnabled`=`true`, `homeostasisMaxCystsPerChunk`=`64` (`8..256`)."),
    ]

    ch12_entries = (
        CodexEntry(
            id="configuracion_cliente_servidor",
            order=1,
            title=lt("Configuraciones (12 Cliente / 39 Servidor)", "Config Catalog (12 Client / 39 Server)"),
            subtitle=lt(
                "MycelialClientConfig, KeyBindings y MycelialConfig TOML completo",
                "MycelialClientConfig, KeyBindings & full MycelialConfig TOML",
            ),
            icon_item="mycelialmind:synaptic_research_bench",
            x=grid_xy(0, 2)[0],
            y=grid_xy(0, 2)[1],
            parents=(),
            unlock_gate="always",
            registry_refs=("MycelialConfig", "MycelialClientConfig", "HudAnchor", "MycelialKeyBindings"),
            pages=tuple(
                chunk_paired_blocks(
                    "config_ref",
                    "Claves de Configuración TOML",
                    "TOML Configuration Keys",
                    client_cfg_blocks + server_cfg_blocks,
                )
            ),
        ),
        CodexEntry(
            id="comandos_red_y_verificacion",
            order=2,
            title=lt("Red v14, `/mycelial` y 22 Verify", "Net v14, `/mycelial` & 22 Verify"),
            subtitle=lt(
                "4 Payloads de red, sintaxis /mycelial y escenarios de prueba",
                "4 network SyncPayloads, /mycelial commands & verify scenarios",
            ),
            icon_item="mycelialmind:bio_scanner",
            x=grid_xy(1, 2)[0],
            y=grid_xy(1, 2)[1],
            parents=("configuracion_cliente_servidor",),
            unlock_gate="admin",
            registry_refs=(
                "ModNetworking",
                "HiveSyncPayload",
                "SymbiosisSyncPayload",
                "DiplomacySyncPayload",
                "DiscoverySyncPayload",
                "MycelialCommands",
                "projectile_adaptation",
                "pylon_containment",
                "boss_encounter",
                "tier2_synergy",
                "synaptic_network",
                "symbiosis",
                "units",
                "expansion",
                "tide",
                "counteroffensive",
                "arsenal",
                "mycorrhizal",
                "hive_ai",
                "chimera",
                "memory",
                "tactical_ai",
                "field_ai",
                "hyphal_siege",
                "loot",
                "homeostasis",
                "polish",
                "dimension_progression",
            ),
            pages=tuple(
                chunk_paired_blocks(
                    "commands_net",
                    "Comandos Admin, Payloads y Verify",
                    "Admin Commands, Payloads & Verify",
                    [
                        (
                            "• **Red (`ModNetworking` v14):** `HiveSyncPayload` (`<=64` nodos, `<=32` pilones, "
                            "`<=64` chunks inflamados), `SymbiosisSyncPayload`, `DiplomacySyncPayload`, "
                            "`DiscoverySyncPayload`.",
                            "• **Networking (`ModNetworking` v14):** `HiveSyncPayload` (`<=64` nodes, `<=32` "
                            "pylons, `<=64` inflamed chunks), `SymbiosisSyncPayload`, `DiplomacySyncPayload`, "
                            "`DiscoverySyncPayload`.",
                        ),
                        (
                            "• **Comandos Operativos (`perm 2`):** `/mycelial status`, `/mycelial reset`, "
                            "`/mycelial stimulus <type> <0.1..500>`, `/mycelial habits`, `/mycelial schism <strain>`, "
                            "`/mycelial diplomacy <strain> <-100..100>`.",
                            "• **Operational Commands (`perm 2`):** `/mycelial status`, `/mycelial reset`, "
                            "`/mycelial stimulus <type> <0.1..500>`, `/mycelial habits`, `/mycelial schism <strain>`, "
                            "`/mycelial diplomacy <strain> <-100..100>`.",
                        ),
                        (
                            "• **Simbiosis, Homeostasis y Auditoría:** `/mycelial symbiosis <status|clear|grant "
                            "<mutagen>>`, `/mycelial homeostasis <status|stress <0..100>|sedate>`, "
                            "`/mycelial worldgen audit [radius] [grid]`.",
                            "• **Symbiosis, Homeostasis & Audit:** `/mycelial symbiosis <status|clear|grant "
                            "<mutagen>>`, `/mycelial homeostasis <status|stress <0..100>|sedate>`, "
                            "`/mycelial worldgen audit [radius] [grid]`.",
                        ),
                        (
                            "• **`/mycelial verify <escenario|worldgen|all>` (1–11):** `projectile_adaptation`, "
                            "`pylon_containment`, `boss_encounter`, `tier2_synergy`, `synaptic_network`, "
                            "`symbiosis`, `units`, `expansion`, `tide`, `counteroffensive`, `arsenal`.",
                            "• **`/mycelial verify <scenario|worldgen|all>` (1–11):** `projectile_adaptation`, "
                            "`pylon_containment`, `boss_encounter`, `tier2_synergy`, `synaptic_network`, "
                            "`symbiosis`, `units`, `expansion`, `tide`, `counteroffensive`, `arsenal`.",
                        ),
                        (
                            "• **`/mycelial verify` (12–22):** `mycorrhizal`, `hive_ai`, `chimera`, `memory`, "
                            "`tactical_ai`, `field_ai`, `hyphal_siege`, `loot`, `homeostasis`, `polish`, `dimension_progression`.",
                            "• **`/mycelial verify` (12–22):** `mycorrhizal`, `hive_ai`, `chimera`, `memory`, "
                            "`tactical_ai`, `field_ai`, `hyphal_siege`, `loot`, `homeostasis`, `polish`, `dimension_progression`.",
                        ),
                    ],
                )
            ),
        ),
    )
    ch12 = CodexChapter(
        id="tecnica_comandos",
        number=12,
        title=lt("Técnica, Config y Comandos", "Tech, Configs & Commands"),
        subtitle=lt(
            "181 Sonidos, LabPBR, 49 Configs, Red v14, /mycelial y 22 Verify",
            "181 Sounds, LabPBR, 49 Configs, Net v14, /mycelial & 22 Verify",
        ),
        icon_item="mycelialmind:bio_scanner",
        entries=ch12_entries,
        unlock_gate="always",
    )

    return (ch9, ch10, ch11, ch12)
