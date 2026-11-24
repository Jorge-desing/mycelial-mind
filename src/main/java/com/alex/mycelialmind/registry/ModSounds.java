package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers all 181 custom SoundEvents and 7 DeferredSoundType block surface families
 * for Mycelial Mind (entities, blocks, items, projectiles, hive mind, biomes, and homeostasis).
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MycelialMind.MODID);

    // =========================================================================
    // System, Hive Mind, Tactical AI, Biome Ambience & Homeostasis Events (32)
    // =========================================================================
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_MUTATE_TIER2 =
            register("hive_mutate_tier2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_PULSE =
            register("synaptic_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_PHASE_SHIFT =
            register("boss_phase_shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPPRESSOR_DISCHARGE =
            register("suppressor_discharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> FUNGAL_TIDE_HORN =
            register("fungal_tide_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUAD_DESYNC =
            register("squad_desync");
    public static final DeferredHolder<SoundEvent, SoundEvent> CORROSIVE_JAM =
            register("corrosive_jam");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMMUNE_SURGE =
            register("immune_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_PROMOTE_MUTATED =
            register("mob_promote_mutated");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_PROMOTE_ELITE =
            register("mob_promote_elite");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_DAWN_RETREAT =
            register("mob_dawn_retreat");
    public static final DeferredHolder<SoundEvent, SoundEvent> INFECTED_MOB_CONVERT =
            register("infected_mob_convert");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCORRHIZAL_TREMOR =
            register("mycorrhizal_tremor");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOMEOSTASIS_HEARTBEAT =
            register("homeostasis_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANTIBODY_CYST_ENCAPSULATE =
            register("antibody_cyst_encapsulate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_RAIN_SIZZLE =
            register("acid_rain_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> NECROPHAGY_FEAST =
            register("necrophagy_feast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWARM_FLANK_CALL =
            register("swarm_flank_call");

    // Biome Soundscapes (9: 3 Deep Mycelium biomes x [loop, mood, additions])
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_CAVERNS_LOOP =
            register("biome_caverns_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_CAVERNS_MOOD =
            register("biome_caverns_mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_CAVERNS_ADDITIONS =
            register("biome_caverns_additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_SANCTUM_LOOP =
            register("biome_sanctum_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_SANCTUM_MOOD =
            register("biome_sanctum_mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_SANCTUM_ADDITIONS =
            register("biome_sanctum_additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_FOREST_LOOP =
            register("biome_forest_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_FOREST_MOOD =
            register("biome_forest_mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIOME_FOREST_ADDITIONS =
            register("biome_forest_additions");

    // Positional Soundscape Loops & Hive Adaptation Alerts (5)
    public static final DeferredHolder<SoundEvent, SoundEvent> FUNGAL_TIDE_AMBIENCE =
            register("fungal_tide_ambience");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFICATION_PYLON_HUM =
            register("purification_pylon_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_TRAIT_ADAPT =
            register("hive_trait_adapt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_SYNERGY_UNLOCK =
            register("hive_synergy_unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_THREAT_MILESTONE =
            register("hive_threat_milestone");

    // =========================================================================
    // Living Entity Audio Profiles (17 entities x 4 + CHIMERA_ROAR + 5 steps = 74)
    // =========================================================================
    // 1. Sporeling
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORELING_CHITTER =
            register("sporeling_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORELING_HURT =
            register("sporeling_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORELING_DEATH =
            register("sporeling_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORELING_BURST =
            register("sporeling_burst");

    // 2. Chitin Brute
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_BRUTE_GROWL =
            register("chitin_brute_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_BRUTE_HURT =
            register("chitin_brute_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_BRUTE_DEATH =
            register("chitin_brute_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_BRUTE_SMASH =
            register("chitin_brute_smash");

    // 3. Mycelial Stalker
    public static final DeferredHolder<SoundEvent, SoundEvent> STALKER_HISS =
            register("stalker_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> STALKER_HURT =
            register("stalker_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STALKER_DEATH =
            register("stalker_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STALKER_POUNCE =
            register("stalker_pounce");

    // 4. Acid Spitter
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SPITTER_GURGLE =
            register("acid_spitter_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SPITTER_HURT =
            register("acid_spitter_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SPITTER_DEATH =
            register("acid_spitter_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SPITTER_SPIT =
            register("acid_spitter_spit");

    // 5. Burrower
    public static final DeferredHolder<SoundEvent, SoundEvent> BURROWER_GRIND =
            register("burrower_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BURROWER_HURT =
            register("burrower_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BURROWER_DEATH =
            register("burrower_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BURROWER_TUNNEL =
            register("burrower_tunnel");

    // 6. Pyro-Shroom
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_SHROOM_CRACKLE =
            register("pyro_shroom_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_SHROOM_HURT =
            register("pyro_shroom_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_SHROOM_DEATH =
            register("pyro_shroom_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_SHROOM_IGNITE =
            register("pyro_shroom_ignite");

    // 7. Synapse Weaver
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPSE_WEAVER_HUM =
            register("synapse_weaver_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPSE_WEAVER_HURT =
            register("synapse_weaver_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPSE_WEAVER_DEATH =
            register("synapse_weaver_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPSE_WEAVER_BUFF =
            register("synapse_weaver_buff");

    // 8. Goliath Carrier
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLIATH_CARRIER_THUD =
            register("goliath_carrier_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLIATH_CARRIER_HURT =
            register("goliath_carrier_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLIATH_CARRIER_DEATH =
            register("goliath_carrier_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLIATH_CARRIER_SPAWN =
            register("goliath_carrier_spawn");

    // 9. Leech Parasite
    public static final DeferredHolder<SoundEvent, SoundEvent> LEECH_PARASITE_SCREECH =
            register("leech_parasite_screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEECH_PARASITE_HURT =
            register("leech_parasite_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEECH_PARASITE_DEATH =
            register("leech_parasite_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEECH_PARASITE_SIPHON =
            register("leech_parasite_siphon");

    // 10. Colony Warden
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_WARDEN_BELLOW =
            register("colony_warden_bellow");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_WARDEN_HURT =
            register("colony_warden_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_WARDEN_DEATH =
            register("colony_warden_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_WARDEN_SHOCKWAVE =
            register("colony_warden_shockwave");

    // 11. Spore Architect
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_ARCHITECT_PULSE =
            register("spore_architect_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_ARCHITECT_HURT =
            register("spore_architect_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_ARCHITECT_DEATH =
            register("spore_architect_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_ARCHITECT_CONSTRUCT =
            register("spore_architect_construct");

    // 12. Synaptic Mimic
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_MIMIC_ECHO =
            register("synaptic_mimic_echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_MIMIC_HURT =
            register("synaptic_mimic_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_MIMIC_DEATH =
            register("synaptic_mimic_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_MIMIC_STRIKE =
            register("synaptic_mimic_strike");

    // 13. Spore Behemoth
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_BEHEMOTH_ROAR =
            register("spore_behemoth_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_BEHEMOTH_HURT =
            register("spore_behemoth_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_BEHEMOTH_DEATH =
            register("spore_behemoth_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_BEHEMOTH_SLAM =
            register("spore_behemoth_slam");

    // 14. Hive Core
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CORE_RESONANCE =
            register("hive_core_resonance");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CORE_HURT =
            register("hive_core_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CORE_DEATH =
            register("hive_core_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CORE_SHIELD =
            register("hive_core_shield");

    // 15. Apex Matriarch
    public static final DeferredHolder<SoundEvent, SoundEvent> APEX_MATRIARCH_WAIL =
            register("apex_matriarch_wail");
    public static final DeferredHolder<SoundEvent, SoundEvent> APEX_MATRIARCH_HURT =
            register("apex_matriarch_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> APEX_MATRIARCH_DEATH =
            register("apex_matriarch_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> APEX_MATRIARCH_BARRAGE =
            register("apex_matriarch_barrage");

    // 16. Purifier Drone
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRONE_HUM =
            register("purifier_drone_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRONE_HURT =
            register("purifier_drone_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRONE_DEATH =
            register("purifier_drone_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRONE_FIRE =
            register("purifier_drone_fire");

    // 17. Mycelial Chimera
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCELIAL_CHIMERA_GROWL =
            register("mycelial_chimera_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCELIAL_CHIMERA_HURT =
            register("mycelial_chimera_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCELIAL_CHIMERA_DEATH =
            register("mycelial_chimera_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIMERA_ROAR =
            register("chimera_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCELIAL_CHIMERA_STRIKE =
            register("mycelial_chimera_strike");

    // Anatomical Step Sounds (5)
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_STEP_CHITIN =
            register("mob_step_chitin");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_STEP_ARACHNID =
            register("mob_step_arachnid");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_STEP_HEAVY =
            register("mob_step_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_STEP_COLOSSAL =
            register("mob_step_colossal");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_STEP_HOVER =
            register("mob_step_hover");

    // =========================================================================
    // Block Surface Families (7 DeferredSoundType x 5 = 35)
    // =========================================================================
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIUM_BREAK = register("block_mycelium_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIUM_STEP = register("block_mycelium_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIUM_PLACE = register("block_mycelium_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIUM_HIT = register("block_mycelium_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIUM_FALL = register("block_mycelium_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SYNAPTIC_NODE_BREAK = register("block_synaptic_node_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SYNAPTIC_NODE_STEP = register("block_synaptic_node_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SYNAPTIC_NODE_PLACE = register("block_synaptic_node_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SYNAPTIC_NODE_HIT = register("block_synaptic_node_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SYNAPTIC_NODE_FALL = register("block_synaptic_node_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHITIN_BREAK = register("block_chitin_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHITIN_STEP = register("block_chitin_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHITIN_PLACE = register("block_chitin_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHITIN_HIT = register("block_chitin_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_CHITIN_FALL = register("block_chitin_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIAL_STONE_BREAK = register("block_mycelial_stone_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIAL_STONE_STEP = register("block_mycelial_stone_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIAL_STONE_PLACE = register("block_mycelial_stone_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIAL_STONE_HIT = register("block_mycelial_stone_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_MYCELIAL_STONE_FALL = register("block_mycelial_stone_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PURIFICATION_TECH_BREAK = register("block_purification_tech_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PURIFICATION_TECH_STEP = register("block_purification_tech_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PURIFICATION_TECH_PLACE = register("block_purification_tech_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PURIFICATION_TECH_HIT = register("block_purification_tech_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_PURIFICATION_TECH_FALL = register("block_purification_tech_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ANTIBODY_CYST_BREAK = register("block_antibody_cyst_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ANTIBODY_CYST_STEP = register("block_antibody_cyst_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ANTIBODY_CYST_PLACE = register("block_antibody_cyst_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ANTIBODY_CYST_HIT = register("block_antibody_cyst_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_ANTIBODY_CYST_FALL = register("block_antibody_cyst_fall");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIVE_ALTAR_BREAK = register("block_hive_altar_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIVE_ALTAR_STEP = register("block_hive_altar_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIVE_ALTAR_PLACE = register("block_hive_altar_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIVE_ALTAR_HIT = register("block_hive_altar_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIVE_ALTAR_FALL = register("block_hive_altar_fall");

    // =========================================================================
    // Active / Functional Block Events (14)
    // =========================================================================
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_VENT_PUFF = register("spore_vent_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFICATION_PYLON_PULSE = register("purification_pylon_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFICATION_TURRET_FIRE = register("purification_turret_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFICATION_TURRET_UPGRADE = register("purification_turret_upgrade");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFICATION_TURRET_REPAIR = register("purification_turret_repair");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_NODE_MATURE = register("synaptic_node_mature");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYMBIOTIC_NODE_CHARGE = register("symbiotic_node_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYMBIOTIC_NODE_TELEPORT = register("symbiotic_node_teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_BULB_BURST = register("spore_bulb_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> EPHEMERAL_CHITIN_CRUMBLE = register("ephemeral_chitin_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> MYCELIAL_RIFT_HUM = register("mycelial_rift_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_ALTAR_AWAKEN = register("hive_altar_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> MATRIARCH_ALTAR_AWAKEN = register("matriarch_altar_awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_BENCH_SYNTHESIZE = register("synaptic_bench_synthesize");

    // =========================================================================
    // Items, Weapons, Armor, Consumables, Relics & Projectiles (26)
    // =========================================================================
    public static final DeferredHolder<SoundEvent, SoundEvent> BIO_SCANNER_PING = register("bio_scanner_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIO_SCANNER_THERMAL_PULSE = register("bio_scanner_thermal_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BIO_EXTRACTOR_DRAW = register("bio_extractor_draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYMBIOTIC_BLADE_SLASH = register("symbiotic_blade_slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENZYME_LANCE_THRUST = register("enzyme_lance_thrust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_LAUNCHER_FIRE = register("spore_launcher_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_GRENADE_THROW = register("purifier_grenade_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_GRENADE_DETONATE = register("purifier_grenade_detonate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_SPIT_IMPACT = register("acid_spit_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> NEUTRALIZING_SPORE_IMPACT = register("neutralizing_spore_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_ARMOR_EQUIP = register("chitin_armor_equip");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHITIN_ARMOR_EVOLVE = register("chitin_armor_evolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPORE_ANTIDOTE_CURE = register("spore_antidote_cure");
    public static final DeferredHolder<SoundEvent, SoundEvent> COUNTER_SERUM_INJECT = register("counter_serum_inject");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYMBIOTIC_MUTAGEN_IMPLANT = register("symbiotic_mutagen_implant");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYNAPTIC_GRAFT_SPLICE = register("synaptic_graft_splice");
    public static final DeferredHolder<SoundEvent, SoundEvent> NEURAL_DECOY_DEPLOY = register("neural_decoy_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOLATILE_ENGRAM_PULSE = register("volatile_engram_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHISM_INJECTOR_STING = register("schism_injector_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUCE_PHEROMONE_SPRAY = register("truce_pheromone_spray");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUORUM_SEAL_PACT = register("quorum_seal_pact");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIMERA_OVUM_HATCH = register("chimera_ovum_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRONE_DEPLOY = register("purifier_drone_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> TIDE_BEACON_ACTIVATE = register("tide_beacon_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_CATALYST_IGNITE = register("rift_catalyst_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERMIND_CROWN_SURGE = register("overmind_crown_surge");

    // Sovereign Colony (Endgame Hive Colony — 5)
    public static final DeferredHolder<SoundEvent, SoundEvent> SOVEREIGN_NEXUS_PULSE = register("sovereign_nexus_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_SYMBIONT_AMBIENT = register("colony_symbiont_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_SYMBIONT_WORK = register("colony_symbiont_work");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_SYMBIONT_PROMOTE = register("colony_symbiont_promote");
    public static final DeferredHolder<SoundEvent, SoundEvent> COMMAND_STAFF_MARK = register("command_staff_mark");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_DIALECT_TONE_1 = register("colony_dialect_tone_1");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_DIALECT_TONE_2 = register("colony_dialect_tone_2");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_DIALECT_TONE_3 = register("colony_dialect_tone_3");

    // =========================================================================
    // 7 DeferredSoundType Surface Families
    // =========================================================================
    public static final DeferredSoundType MYCELIAL_FLORA_SOUNDS = new DeferredSoundType(
            0.90F, 0.96F,
            BLOCK_MYCELIUM_BREAK, BLOCK_MYCELIUM_STEP, BLOCK_MYCELIUM_PLACE, BLOCK_MYCELIUM_HIT, BLOCK_MYCELIUM_FALL
    );
    public static final DeferredSoundType SYNAPTIC_NODE_SOUNDS = new DeferredSoundType(
            0.95F, 1.02F,
            BLOCK_SYNAPTIC_NODE_BREAK, BLOCK_SYNAPTIC_NODE_STEP, BLOCK_SYNAPTIC_NODE_PLACE, BLOCK_SYNAPTIC_NODE_HIT, BLOCK_SYNAPTIC_NODE_FALL
    );
    public static final DeferredSoundType CHITIN_RELIC_SOUNDS = new DeferredSoundType(
            1.00F, 0.98F,
            BLOCK_CHITIN_BREAK, BLOCK_CHITIN_STEP, BLOCK_CHITIN_PLACE, BLOCK_CHITIN_HIT, BLOCK_CHITIN_FALL
    );
    public static final DeferredSoundType MYCELIAL_STONE_SOUNDS = new DeferredSoundType(
            1.05F, 0.94F,
            BLOCK_MYCELIAL_STONE_BREAK, BLOCK_MYCELIAL_STONE_STEP, BLOCK_MYCELIAL_STONE_PLACE, BLOCK_MYCELIAL_STONE_HIT, BLOCK_MYCELIAL_STONE_FALL
    );
    public static final DeferredSoundType PURIFICATION_TECH_SOUNDS = new DeferredSoundType(
            0.92F, 1.04F,
            BLOCK_PURIFICATION_TECH_BREAK, BLOCK_PURIFICATION_TECH_STEP, BLOCK_PURIFICATION_TECH_PLACE, BLOCK_PURIFICATION_TECH_HIT, BLOCK_PURIFICATION_TECH_FALL
    );
    public static final DeferredSoundType ANTIBODY_CYST_SOUNDS = new DeferredSoundType(
            0.96F, 0.92F,
            BLOCK_ANTIBODY_CYST_BREAK, BLOCK_ANTIBODY_CYST_STEP, BLOCK_ANTIBODY_CYST_PLACE, BLOCK_ANTIBODY_CYST_HIT, BLOCK_ANTIBODY_CYST_FALL
    );
    public static final DeferredSoundType HIVE_ALTAR_SOUNDS = new DeferredSoundType(
            1.12F, 0.88F,
            BLOCK_HIVE_ALTAR_BREAK, BLOCK_HIVE_ALTAR_STEP, BLOCK_HIVE_ALTAR_PLACE, BLOCK_HIVE_ALTAR_HIT, BLOCK_HIVE_ALTAR_FALL
    );

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
