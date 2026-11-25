package com.alex.mycelialmind.config;

import com.alex.mycelialmind.block.MyceliumSpreadPolicy;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration for Mycelial Mind adaptation, territory spread, and multiplayer scaling.
 */
public final class MycelialConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue LEARNING_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DECAY_PER_MINUTE;
    public static final ModConfigSpec.DoubleValue TRAIT_ACTIVATION_THRESHOLD;
    public static final ModConfigSpec.DoubleValue TIER2_ACTIVATION_THRESHOLD;
    public static final ModConfigSpec.DoubleValue TIER2_DECAY_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue SYNERGIES_ENABLED;
    public static final ModConfigSpec.IntValue SUPPRESSOR_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue PROJECTILE_DEFLECT_CHANCE;
    public static final ModConfigSpec.DoubleValue SYMBIOSIS_THREAT_MULTIPLIER_PER_SLOT;
    public static final ModConfigSpec.IntValue MAX_MUTAGEN_SLOTS;
    public static final ModConfigSpec.DoubleValue COUNTER_SERUM_BLEED_RATIO;

    public static final ModConfigSpec.BooleanValue MYCELIUM_SPREAD_ENABLED;
    public static final ModConfigSpec.DoubleValue MYCELIUM_SPREAD_CHANCE;
    public static final ModConfigSpec.IntValue PYLON_PURIFICATION_RADIUS;
    public static final ModConfigSpec.BooleanValue PYLON_TIER3_CHUNK_LOADING_ENABLED;
    public static final ModConfigSpec.IntValue SYNAPTIC_NODE_LINK_RADIUS;
    public static final ModConfigSpec.DoubleValue NODE_DESTRUCTION_STIMULUS_BLEED;
    public static final ModConfigSpec.BooleanValue BURROWER_BLOCK_BREAKING;
    public static final ModConfigSpec.BooleanValue RESPECT_CLAIMED_CHUNKS;
    public static final ModConfigSpec.DoubleValue PLAYER_COUNT_SCALING;
    public static final ModConfigSpec.BooleanValue WAVES_ENABLED;
    public static final ModConfigSpec.IntValue WAVE_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue AUTONOMOUS_EXPANSION_ENABLED;
    public static final ModConfigSpec.BooleanValue AGGRESSIVE_VILLAGE_ASSIMILATION;
    public static final ModConfigSpec.IntValue MAX_AUTONOMOUS_NODES;
    public static final ModConfigSpec.IntValue MIN_NODE_SPACING;
    public static final ModConfigSpec.BooleanValue INFECTION_CONVERSION_ENABLED;
    public static final ModConfigSpec.BooleanValue FUNGAL_TIDE_ENABLED;
    public static final ModConfigSpec.IntValue FUNGAL_TIDE_DURATION_TICKS;
    public static final ModConfigSpec.IntValue FUNGAL_TIDE_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue MYCORRHIZAL_ACOUSTICS_ENABLED;
    public static final ModConfigSpec.IntValue MAX_SYMBIOTIC_NODES;
    public static final ModConfigSpec.IntValue MYCORRHIZAL_TRAVEL_RANGE;
    public static final ModConfigSpec.IntValue MYCORRHIZAL_TRAVEL_COOLDOWN_TICKS;
    public static final ModConfigSpec.BooleanValue PROGRESSIVE_THREAT_SCALING;
    public static final ModConfigSpec.DoubleValue DEATH_THREAT_PENALTY;

    public static final ModConfigSpec.BooleanValue HOMEOSTASIS_ENABLED;
    public static final ModConfigSpec.DoubleValue HOMEOSTASIS_EXPLOSION_STRESS_SCALE;
    public static final ModConfigSpec.DoubleValue HOMEOSTASIS_ACID_RAIN_DAMAGE;
    public static final ModConfigSpec.BooleanValue HOMEOSTASIS_ANTIBODY_ENCAPSULATION_ENABLED;
    public static final ModConfigSpec.IntValue HOMEOSTASIS_MAX_CYSTS_PER_CHUNK;

    public static final ModConfigSpec.IntValue ROOT_TRAVERSAL_THRESHOLD;
    public static final ModConfigSpec.IntValue GROWTH_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue MAX_TRAIL_POINTS_PER_CHUNK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hive_memory");
        LEARNING_MULTIPLIER = builder
                .comment("Multiplier applied to all combat stimulus recorded by the Hive Mind.")
                .defineInRange("learningMultiplier", 1.0, 0.1, 10.0);

        DECAY_PER_MINUTE = builder
                .comment("Amount of stimulus score that decays per in-game minute when not reinforced.")
                .defineInRange("decayPerMinute", 2.0, 0.0, 50.0);

        TRAIT_ACTIVATION_THRESHOLD = builder
                .comment("Stimulus score required for the Hive Mind to activate the corresponding Tier 1 adaptation trait.")
                .defineInRange("traitActivationThreshold", 70.0, 5.0, 500.0);

        TIER2_ACTIVATION_THRESHOLD = builder
                .comment("Stimulus score required for the Hive Mind to evolve a Tier 2 behavioral mutation.")
                .defineInRange("tier2ActivationThreshold", 200.0, 25.0, 500.0);

        TIER2_DECAY_MULTIPLIER = builder
                .comment("Multiplier applied to stimulus decay when above Tier 1 threshold so players can shift tactics.")
                .defineInRange("tier2DecayMultiplier", 1.75, 1.0, 5.0);

        SYNERGIES_ENABLED = builder
                .comment("Whether pairs of active traits can form elite Hive Synergies.")
                .define("synergiesEnabled", true);

        SUPPRESSOR_COOLDOWN_SECONDS = builder
                .comment("Duration in seconds that Synaptic Suppressor blocks Tier 2 mutations and synergies.")
                .defineInRange("suppressorCooldownSeconds", 120, 15, 600);

        PROJECTILE_DEFLECT_CHANCE = builder
                .comment("Base chance for mobs with PROJECTILE_DEFLECT to deflect incoming arrows/projectiles.")
                .defineInRange("projectileDeflectChance", 0.30, 0.0, 0.95);

        SYMBIOSIS_THREAT_MULTIPLIER_PER_SLOT = builder
                .comment("Additional Hive Threat & stimulus multiplier per equipped Symbiotic Mutagen slot.")
                .defineInRange("symbiosisThreatMultiplierPerSlot", 0.15, 0.0, 1.0);

        MAX_MUTAGEN_SLOTS = builder
                .comment("Maximum number of Symbiotic Mutagens a player can equip simultaneously.")
                .defineInRange("maxMutagenSlots", 2, 1, 5);

        COUNTER_SERUM_BLEED_RATIO = builder
                .comment("Fraction of the targeted StimulusType score bled when using a Counter-Serum.")
                .defineInRange("counterSerumBleedRatio", 0.50, 0.10, 0.95);
        builder.pop();

        builder.push("territory_and_multiplayer");
        MYCELIUM_SPREAD_ENABLED = builder
                .comment("Whether Creeping Mycelium blocks can spread to adjacent natural blocks.")
                .define("myceliumSpreadEnabled", true);

        MYCELIUM_SPREAD_CHANCE = builder
                .comment("Probability (0.0 to 1.0) per random tick that Creeping Mycelium attempts to spread.")
                .defineInRange("myceliumSpreadChance", MyceliumSpreadPolicy.DEFAULT_CHANCE, 0.0, 1.0);

        PYLON_PURIFICATION_RADIUS = builder
                .comment("Protection radius (in blocks) of a Purification Pylon.")
                .defineInRange("pylonPurificationRadius", 24, 4, 64);

        PYLON_TIER3_CHUNK_LOADING_ENABLED = builder
                .comment("Whether active, unsmothered Tier 3 Purification Pylons anchor their central chunk using NeoForge's TicketController.")
                .define("pylonTier3ChunkLoadingEnabled", true);

        SYNAPTIC_NODE_LINK_RADIUS = builder
                .comment("Maximum distance (in blocks) at which two Synaptic Nodes can form a network link.")
                .defineInRange("synapticNodeLinkRadius", 28, 8, 64);

        NODE_DESTRUCTION_STIMULUS_BLEED = builder
                .comment("Amount of stimulus score bled from active categories when a Synaptic Node is destroyed.")
                .defineInRange("nodeDestructionStimulusBleed", 18.0, 0.0, 100.0);

        BURROWER_BLOCK_BREAKING = builder
                .comment("Whether Burrower siege mobs can break non-reinforced blocks to reach targets.")
                .define("burrowerBlockBreaking", true);

        RESPECT_CLAIMED_CHUNKS = builder
                .comment("Whether destructive block griefing (Burrower tunneling and Acid Spit corrosion) is blocked inside FTB Chunks or Open Parties & Claims territories.")
                .define("respectClaimedChunks", true);

        PLAYER_COUNT_SCALING = builder
                .comment("Additional threat/health scaling factor per online player in multiplayer.")
                .defineInRange("playerCountScaling", 0.15, 0.0, 1.0);

        WAVES_ENABLED = builder
                .comment("Whether the Hive Mind triggers raid waves when threat crosses 25/50/75/100% milestones.")
                .define("wavesEnabled", true);

        WAVE_COOLDOWN_TICKS = builder
                .comment("Minimum cooldown in ticks between consecutive Hive Mind raid waves.")
                .defineInRange("waveCooldownTicks", 3600, 200, 72000);

        AUTONOMOUS_EXPANSION_ENABLED = builder
                .comment("Whether Relay nodes mature autonomously and Incubators dispatch Spore Architects.")
                .define("autonomousExpansionEnabled", true);

        AGGRESSIVE_VILLAGE_ASSIMILATION = builder
                .comment("Whether Creeping Mycelium and Spore Architects can assimilate cobblestone, planks, logs, and village paths outside Pylon protection.")
                .define("aggressiveVillageAssimilation", true);

        MAX_AUTONOMOUS_NODES = builder
                .comment("Soft cap on total Synaptic Nodes per dimension for autonomous Spore Architect colony construction.")
                .defineInRange("maxAutonomousNodes", 32, 4, 128);

        MIN_NODE_SPACING = builder
                .comment("Minimum spacing (in blocks) required between autonomous Synaptic Nodes.")
                .defineInRange("minNodeSpacing", 18, 8, 48);

        INFECTION_CONVERSION_ENABLED = builder
                .comment("Whether vanilla villagers and mobs with MYCELIAL_INFECTION convert into mycelial units on death or below 25% HP.")
                .define("infectionConversionEnabled", true);

        FUNGAL_TIDE_ENABLED = builder
                .comment("Whether the Fungal Tide global endgame event triggers when threat reaches 75%.")
                .define("fungalTideEnabled", true);

        FUNGAL_TIDE_DURATION_TICKS = builder
                .comment("Duration (in ticks) of a Fungal Tide global event (6000 = 5 minutes).")
                .defineInRange("fungalTideDurationTicks", 6000, 600, 24000);

        FUNGAL_TIDE_COOLDOWN_TICKS = builder
                .comment("Cooldown (in ticks) between Fungal Tide global events (24000 = 1 Minecraft day).")
                .defineInRange("fungalTideCooldownTicks", 24000, 1200, 72000);

        MYCORRHIZAL_ACOUSTICS_ENABLED = builder
                .comment("Whether sprinting, jumping, or breaking blocks on Creeping Mycelium emits seismic pulses to nearby enemy nodes.")
                .define("mycorrhizalAcousticsEnabled", true);

        MAX_SYMBIOTIC_NODES = builder
                .comment("Maximum number of allied Symbiotic Nodes allowed simultaneously per dimension.")
                .defineInRange("maxSymbioticNodes", 4, 1, 16);

        MYCORRHIZAL_TRAVEL_RANGE = builder
                .comment("Maximum distance (in blocks) for node-to-node Mycorrhizal Fast Travel between Symbiotic Nodes.")
                .defineInRange("mycorrhizalTravelRange", 96, 16, 256);

        MYCORRHIZAL_TRAVEL_COOLDOWN_TICKS = builder
                .comment("Cooldown (in ticks) per player between Mycorrhizal Fast Travel jumps (1500 = 75 seconds).")
                .defineInRange("mycorrhizalTravelCooldownTicks", 1500, 100, 6000);

        PROGRESSIVE_THREAT_SCALING = builder
                .comment("Whether MycelialMobs and bosses scale health, damage, armor, and speed across HiveProgressionTier stages and in Deep Mycelium.")
                .define("progressiveThreatScaling", true);

        DEATH_THREAT_PENALTY = builder
                .comment("Global threat percentage added when a player dies to a MycelialMob, under MYCELIAL_INFECTION, or near an enemy Synaptic Node.")
                .defineInRange("deathThreatPenalty", 5.0, 0.0, 25.0);
        builder.pop();

        builder.push("dimensional_homeostasis");
        HOMEOSTASIS_ENABLED = builder
                .comment("Whether deep_mycelium reacts as a living organism with localized Immune Responses (acid rain, dense spore fog, antibody cysts).")
                .define("homeostasisEnabled", true);

        HOMEOSTASIS_EXPLOSION_STRESS_SCALE = builder
                .comment("Multiplier applied to dimensional immune stress caused by explosions inside deep_mycelium.")
                .defineInRange("homeostasisExplosionStressScale", 1.0, 0.1, 5.0);

        HOMEOSTASIS_ACID_RAIN_DAMAGE = builder
                .comment("Base enzymatic acid rain damage per pulse when exposed to the deep_mycelium cavern ceiling during Immune Response.")
                .defineInRange("homeostasisAcidRainDamage", 2.0, 0.5, 10.0);

        HOMEOSTASIS_ANTIBODY_ENCAPSULATION_ENABLED = builder
                .comment("Whether deep_mycelium grows antibody_cyst tissue around player-placed foreign blocks and smothers Purification Pylons.")
                .define("homeostasisAntibodyEncapsulationEnabled", true);

        HOMEOSTASIS_MAX_CYSTS_PER_CHUNK = builder
                .comment("Maximum number of active antibody_cyst blocks that Dimensional Homeostasis tracks per chunk.")
                .defineInRange("homeostasisMaxCystsPerChunk", 64, 8, 256);
        builder.pop();

        builder.push("hyphal_network");
        ROOT_TRAVERSAL_THRESHOLD = builder
                .comment("Number of distinct player traversals on a block required to turn it into a Hyphal root node.")
                .defineInRange("rootTraversalThreshold", 5, 2, 32);

        GROWTH_INTERVAL_TICKS = builder
                .comment("Ticks between finite-speed Hyphal Vein growth and nutrient propagation steps (10 = 2 blocks/sec).")
                .defineInRange("growthIntervalTicks", 10, 1, 100);

        MAX_TRAIL_POINTS_PER_CHUNK = builder
                .comment("Maximum ordered literal player trail positions retained per chunk for Hyphal Vein growth.")
                .defineInRange("maxTrailPointsPerChunk", 64, 16, 256);
        builder.pop();

        SPEC = builder.build();
    }

    private MycelialConfig() {
    }

    /**
     * Tier 2 is a behavioral evolution of Tier 1, so it must never activate first.
     * The independent config ranges permit overlapping values; normalize them here
     * so server mechanics and client telemetry can use the same effective threshold.
     */
    public static double getEffectiveTier2ActivationThreshold() {
        return Math.max(TIER2_ACTIVATION_THRESHOLD.get(), TRAIT_ACTIVATION_THRESHOLD.get() + 1.0);
    }
}
