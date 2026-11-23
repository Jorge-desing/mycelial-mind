package com.alex.mycelialmind.hive;

import java.util.List;

/**
 * Pure, deterministic progression engine evaluating player goals across three scales:
 * Short-term (immediate tools/samples), Mid-term (sanctum/dimension/matriarch),
 * and Long-term (Genetic Bestiary, Experimental Secrets, and Perfect Bastion mastery).
 */
public final class ProgressionGoalEngine {
    public static final int TOTAL_SPECIES = 17;
    public static final int TOTAL_TRAITS = 5;
    public static final int TOTAL_STRAINS = 3;
    public static final int TOTAL_SECRETS = 8;

    public static final int MILESTONE_BIO_SCANNER = 0;
    public static final int MILESTONE_SYNAPTIC_BENCH = 1;
    public static final int MILESTONE_BIO_EXTRACTOR = 2;
    public static final int MILESTONE_SYNTHESIZED_BIOTECH = 3;
    public static final int MILESTONE_STATION_UPGRADED = 4;
    public static final int MILESTONE_CHITIN_EVOLVED = 5;
    public static final int MILESTONE_HIVE_CORE_DEFEATED = 6;

    public static final int SECRET_EXP_SYNAPTIC_CORE = 0;
    public static final int SECRET_EXP_TRUCE_PHEROMONE = 1;
    public static final int SECRET_EXP_VOLATILE_ENGRAM = 2;
    public static final int SECRET_EXP_NEURAL_ECHO = 3;
    public static final int SECRET_WORLD_VENT_PURIFICATION = 4;
    public static final int SECRET_WORLD_AMBER_CRYSTALLIZATION = 5;
    public static final int SECRET_PRIMORDIAL_SPECIMEN = 6;
    public static final int SECRET_SEALED_BIO_VAULT = 7;

    public static final int RESEARCH_BIT_SPORE_FOUND = 1 << 0;
    public static final int RESEARCH_BIT_DEFLECTION_WITNESSED = 1 << 1;
    public static final int RESEARCH_BIT_ADAPTATION_IDENTIFIED = 1 << 2;
    public static final int RESEARCH_BIT_SAMPLE_EXTRACTED = 1 << 3;
    public static final int RESEARCH_BIT_BIOTECH_SYNTHESIZED = 1 << 4;
    public static final int RESEARCH_BIT_NODE_THREAT_RECORDED = 1 << 5;
    public static final int RESEARCH_BIT_NODE_DESTROYED = 1 << 6;
    public static final double MIN_OBSERVABLE_STIMULUS_SCORE = 10.0D;

    public static final List<String> BESTIARY_SPECIES_IDS = List.of(
            "sporeling",
            "chitin_brute",
            "mycelial_stalker",
            "acid_spitter",
            "burrower",
            "pyro_shroom",
            "synapse_weaver",
            "goliath_carrier",
            "leech_parasite",
            "colony_warden",
            "spore_architect",
            "synaptic_mimic",
            "spore_behemoth",
            "hive_core",
            "apex_matriarch",
            "purifier_drone",
            "mycelial_chimera"
    );

    public enum ShortTermGoal {
        CRAFT_SCANNER("craft_scanner", 0),
        BUILD_RESEARCH_BENCH("build_research_bench", 1),
        EXTRACT_GENETIC_SAMPLE("extract_genetic_sample", 2),
        SYNTHESIZE_BIOTECH("synthesize_biotech", 3),
        UPGRADE_GEAR_OR_STATION("upgrade_gear_or_station", 4),
        MASTER_EXPERIMENTS("master_experiments", 5);

        private final String id;
        private final int stepIndex;

        ShortTermGoal(String id, int stepIndex) {
            this.id = id;
            this.stepIndex = stepIndex;
        }

        public String id() {
            return id;
        }

        public int stepIndex() {
            return stepIndex;
        }

        public String translationKey() {
            return "codex.mycelialmind.goal.short." + id;
        }
    }

    public enum MidTermGoal {
        ESTABLISH_PERIMETER("establish_perimeter", 0),
        CONQUER_HIVE_SANCTUM("conquer_hive_sanctum", 1),
        BREACH_DEEP_MYCELIUM("breach_deep_mycelium", 2),
        OVERCOME_TIDE_AND_MATRIARCH("overcome_tide_and_matriarch", 3),
        MAINTAIN_DOMINION("maintain_dominion", 4);

        private final String id;
        private final int stepIndex;

        MidTermGoal(String id, int stepIndex) {
            this.id = id;
            this.stepIndex = stepIndex;
        }

        public String id() {
            return id;
        }

        public int stepIndex() {
            return stepIndex;
        }

        public String translationKey() {
            return "codex.mycelialmind.goal.mid." + id;
        }
    }

    public enum CreatureObservation {
        NONE(0, "codex.mycelialmind.research.obs.none"),
        INSUFFICIENT_EVIDENCE(1, "message.mycelialmind.scanner_obs.insufficient_evidence"),
        LEARNING_PROJECTILE(2, "message.mycelialmind.scanner_obs.learning_projectile"),
        CONFIRMED_PROJECTILE_ADAPTATION(3, "message.mycelialmind.scanner_obs.confirmed_projectile"),
        OTHER_DOMINANT_RESPONSE(4, "message.mycelialmind.scanner_obs.other_dominant");

        private final int id;
        private final String translationKey;

        CreatureObservation(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return translationKey;
        }

        public static CreatureObservation byId(int id) {
            for (CreatureObservation obs : values()) {
                if (obs.id == id) {
                    return obs;
                }
            }
            return NONE;
        }
    }

    public enum ExpeditionGoal {
        NONE(0, "none", "codex.mycelialmind.research.goal.none"),
        USE_TO_ADVANTAGE(1, "use_to_advantage", "codex.mycelialmind.research.goal.use_to_advantage"),
        RECLAIM_CAVE(2, "reclaim_cave", "codex.mycelialmind.research.goal.reclaim_cave");

        private final int id;
        private final String key;
        private final String translationKey;

        ExpeditionGoal(int id, String key, String translationKey) {
            this.id = id;
            this.key = key;
            this.translationKey = translationKey;
        }

        public int id() {
            return id;
        }

        public String key() {
            return key;
        }

        public String translationKey() {
            return translationKey;
        }

        public static ExpeditionGoal byId(int id) {
            for (ExpeditionGoal goal : values()) {
                if (goal.id == id) {
                    return goal;
                }
            }
            return NONE;
        }
    }

    public enum ResearchNextStep {
        AWAITING_FIRST_ADAPTATION("awaiting_first_adaptation", false),
        CHOOSE_EXPEDITION_GOAL("choose_expedition_goal", false),
        MISSING_BIO_EXTRACTOR("missing_bio_extractor", true),
        WEAKEN_AND_EXTRACT_SAMPLE("weaken_and_extract_sample", false),
        SAMPLE_READY_MISSING_BIOMASS("sample_ready_missing_biomass", true),
        SAMPLE_READY_MISSING_CATALYST("sample_ready_missing_catalyst", true),
        SAMPLE_READY_MISSING_BENCH("sample_ready_missing_bench", true),
        SAMPLE_READY_TO_SYNTHESIZE("sample_ready_to_synthesize", false),
        COMPLETED_BIOTECH_SYNTHESIS("completed_biotech_synthesis", false),
        MISSING_BIO_SCANNER("missing_bio_scanner", true),
        THREAT_ALREADY_ZERO("threat_already_zero", true),
        LOCATE_NODE_AND_RECORD_THREAT("locate_node_and_record_threat", false),
        DESTROY_LOCATED_NODE("destroy_located_node", false),
        COMPLETED_NODE_DESTRUCTION("completed_node_destruction", false);

        private final String id;
        private final boolean warning;

        ResearchNextStep(String id, boolean warning) {
            this.id = id;
            this.warning = warning;
        }

        public String id() {
            return id;
        }

        public boolean isWarning() {
            return warning;
        }

        public String translationKey() {
            return "codex.mycelialmind.research.step." + id;
        }

        public String hudTranslationKey() {
            return "hud.mycelialmind.research.step." + id;
        }
    }

    private ProgressionGoalEngine() {
    }

    public static int speciesIndex(String speciesId) {
        if (speciesId == null || speciesId.isEmpty()) {
            return -1;
        }
        return BESTIARY_SPECIES_IDS.indexOf(speciesId);
    }

    public static ShortTermGoal resolveShortTermGoal(
            int craftedMilestonesMask,
            int discoveredTraitsMask,
            int discoveredSecretsMask
    ) {
        if ((craftedMilestonesMask & (1 << MILESTONE_BIO_SCANNER)) == 0) {
            return ShortTermGoal.CRAFT_SCANNER;
        }
        if ((craftedMilestonesMask & (1 << MILESTONE_SYNAPTIC_BENCH)) == 0) {
            return ShortTermGoal.BUILD_RESEARCH_BENCH;
        }
        int traitCount = Integer.bitCount(discoveredTraitsMask & 0x1F);
        if ((craftedMilestonesMask & (1 << MILESTONE_BIO_EXTRACTOR)) == 0 && traitCount == 0) {
            return ShortTermGoal.EXTRACT_GENETIC_SAMPLE;
        }
        if ((craftedMilestonesMask & (1 << MILESTONE_SYNTHESIZED_BIOTECH)) == 0) {
            return ShortTermGoal.SYNTHESIZE_BIOTECH;
        }
        int upgradeBits = (1 << MILESTONE_STATION_UPGRADED) | (1 << MILESTONE_CHITIN_EVOLVED);
        if ((craftedMilestonesMask & upgradeBits) == 0) {
            return ShortTermGoal.UPGRADE_GEAR_OR_STATION;
        }
        return ShortTermGoal.MASTER_EXPERIMENTS;
    }

    public static int computeShortTermProgressPercent(
            ShortTermGoal goal,
            int craftedMilestonesMask,
            int discoveredTraitsMask,
            int discoveredSecretsMask
    ) {
        if (goal == null) {
            return 0;
        }
        return switch (goal) {
            case CRAFT_SCANNER -> 0;
            case BUILD_RESEARCH_BENCH -> 20;
            case EXTRACT_GENETIC_SAMPLE -> 40;
            case SYNTHESIZE_BIOTECH -> 60;
            case UPGRADE_GEAR_OR_STATION -> 80;
            case MASTER_EXPERIMENTS -> {
                int traits = Math.min(TOTAL_TRAITS, Integer.bitCount(discoveredTraitsMask & 0x1F));
                int secrets = Math.min(TOTAL_SECRETS, Integer.bitCount(discoveredSecretsMask & 0xFF));
                int extra = Math.round(((traits + secrets) / (float) (TOTAL_TRAITS + TOTAL_SECRETS)) * 20.0F);
                yield Math.min(100, 80 + extra);
            }
        };
    }

    public static boolean hasMilestone(int craftedMilestonesMask, int milestoneBit) {
        return milestoneBit >= 0 && milestoneBit < 16 && (craftedMilestonesMask & (1 << milestoneBit)) != 0;
    }

    public static MidTermGoal resolveMidTermGoal(
            HiveProgressionTier tier,
            int totalPylons,
            boolean deepBreached,
            boolean matriarchDefeated
    ) {
        return resolveMidTermGoal(tier, totalPylons, false, deepBreached, matriarchDefeated);
    }

    public static MidTermGoal resolveMidTermGoal(
            HiveProgressionTier effectiveTier,
            int totalPylons,
            boolean hiveCoreDefeated,
            boolean deepBreached,
            boolean matriarchDefeated
    ) {
        if (matriarchDefeated) {
            return MidTermGoal.MAINTAIN_DOMINION;
        }
        if (deepBreached || effectiveTier == HiveProgressionTier.OVERMIND) {
            return MidTermGoal.OVERCOME_TIDE_AND_MATRIARCH;
        }
        if (hiveCoreDefeated || (effectiveTier != null && effectiveTier.level() >= HiveProgressionTier.ADAPTIVE.level())) {
            return MidTermGoal.BREACH_DEEP_MYCELIUM;
        }
        if (totalPylons > 0 || (effectiveTier != null && effectiveTier.level() >= HiveProgressionTier.AWAKENED.level())) {
            return MidTermGoal.CONQUER_HIVE_SANCTUM;
        }
        return MidTermGoal.ESTABLISH_PERIMETER;
    }

    public static int computeMidTermProgressPercent(
            MidTermGoal goal,
            HiveProgressionTier tier,
            int totalPylons,
            boolean deepBreached,
            boolean matriarchDefeated
    ) {
        return computeMidTermProgressPercent(goal, tier, totalPylons, false, deepBreached, matriarchDefeated);
    }

    public static int computeMidTermProgressPercent(
            MidTermGoal goal,
            HiveProgressionTier effectiveTier,
            int totalPylons,
            boolean hiveCoreDefeated,
            boolean deepBreached,
            boolean matriarchDefeated
    ) {
        if (matriarchDefeated || goal == MidTermGoal.MAINTAIN_DOMINION) {
            return 100;
        }
        if (goal == null) {
            return 0;
        }
        return switch (goal) {
            case ESTABLISH_PERIMETER -> totalPylons > 0 ? 20 : 10;
            case CONQUER_HIVE_SANCTUM -> hiveCoreDefeated
                    ? 50
                    : ((effectiveTier != null && effectiveTier.level() >= HiveProgressionTier.AWAKENED.level()) ? 45 : 30);
            case BREACH_DEEP_MYCELIUM -> deepBreached ? 75 : (hiveCoreDefeated ? 65 : 55);
            case OVERCOME_TIDE_AND_MATRIARCH -> deepBreached ? 85 : 75;
            case MAINTAIN_DOMINION -> 100;
        };
    }

    public static int computeActCampaignCompletionPercent(
            HiveProgressionTier effectiveTier,
            boolean hiveCoreDefeated,
            boolean deepBreached,
            boolean matriarchDefeated
    ) {
        if (matriarchDefeated) {
            return 100;
        }
        int base = effectiveTier == null ? 10 : switch (effectiveTier) {
            case DORMANT -> 10;
            case AWAKENED -> 25;
            case ADAPTIVE -> 50;
            case OVERMIND -> 75;
        };
        int bonus = (hiveCoreDefeated ? 15 : 0) + (deepBreached ? 10 : 0);
        return Math.max(0, Math.min(95, base + bonus));
    }

    public static int computeBastionScore(
            int pylonCount,
            int maxPylonTier,
            int turretCount,
            int maxTurretTier,
            int symbioticNodes,
            int benchTier,
            boolean hasDroneOrChimera
    ) {
        int safePylons = Math.max(0, pylonCount);
        int safeTurrets = Math.max(0, turretCount);
        int safeSym = Math.max(0, symbioticNodes);
        int pylonScore = Math.min(20, safePylons * 10)
                + (safePylons > 0 ? (Math.max(1, Math.min(3, maxPylonTier)) - 1) * 5 : 0);
        int turretScore = Math.min(20, safeTurrets * 10)
                + (safeTurrets > 0 ? (Math.max(1, Math.min(3, maxTurretTier)) - 1) * 5 : 0);
        int symScore = Math.min(15, safeSym * 5);
        int benchScore = benchTier >= 3 ? 15 : (benchTier == 2 ? 10 : (benchTier == 1 ? 5 : 0));
        int companionScore = hasDroneOrChimera ? 10 : 0;
        return Math.max(0, Math.min(100, pylonScore + turretScore + symScore + benchScore + companionScore));
    }

    public static int computeLongTermMasteryPercent(
            int speciesCount,
            int traitCount,
            int secretCount,
            int bastionScore
    ) {
        int s = Math.max(0, Math.min(TOTAL_SPECIES, speciesCount));
        int t = Math.max(0, Math.min(TOTAL_TRAITS, traitCount));
        int sec = Math.max(0, Math.min(TOTAL_SECRETS, secretCount));
        int b = Math.max(0, Math.min(100, bastionScore));
        double weighted = (s / (double) TOTAL_SPECIES) * 35.0D
                + (t / (double) TOTAL_TRAITS) * 15.0D
                + (sec / (double) TOTAL_SECRETS) * 25.0D
                + (b / 100.0D) * 25.0D;
        return Math.max(0, Math.min(100, (int) Math.round(weighted)));
    }

    public static boolean isCodexGateUnlocked(
            String gate,
            int effectiveActNumber,
            boolean deepMyceliumBreached,
            boolean hasAdminPermission,
            boolean isCreative
    ) {
        return isCodexGateUnlocked(
                gate, effectiveActNumber, deepMyceliumBreached, hasAdminPermission, isCreative, 0, false);
    }

    public static boolean isCodexGateUnlocked(
            String gate,
            int effectiveActNumber,
            boolean deepMyceliumBreached,
            boolean hasAdminPermission,
            boolean isCreative,
            int discoveredEngramsMask,
            boolean matriarchDefeated
    ) {
        if (isCreative || gate == null || gate.isEmpty()) {
            return true;
        }
        if (gate.startsWith("engram:")) {
            FieldEngramId engram = FieldEngramId.byIdOrNull(gate.substring("engram:".length()));
            return engram != null && engram.isUnlocked(discoveredEngramsMask);
        }
        return switch (gate) {
            case "act_2" -> effectiveActNumber >= 2;
            case "act_3" -> effectiveActNumber >= 3;
            case "act_4" -> effectiveActNumber >= 4;
            case "deep_mycelium" -> deepMyceliumBreached;
            case "post_matriarch" -> matriarchDefeated;
            case "admin" -> hasAdminPermission;
            default -> true;
        };
    }

    public static CreatureObservation resolveCreatureObservation(
            boolean mobHasProjectileDeflect,
            boolean hiveHasProjectileDeflect,
            boolean witnessedDeflection,
            double projectileScore,
            StimulusType dominantStimulus,
            double dominantScore,
            double traitActivationThreshold
    ) {
        if (mobHasProjectileDeflect || (hiveHasProjectileDeflect && witnessedDeflection)) {
            return CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION;
        }
        double safeProj = Double.isFinite(projectileScore) ? projectileScore : 0.0D;
        double safeDom = Double.isFinite(dominantScore) ? dominantScore : 0.0D;
        if ((dominantStimulus == StimulusType.PROJECTILE && safeProj >= MIN_OBSERVABLE_STIMULUS_SCORE)
                || (witnessedDeflection && safeProj >= MIN_OBSERVABLE_STIMULUS_SCORE)) {
            return CreatureObservation.LEARNING_PROJECTILE;
        }
        if (dominantStimulus == null || safeDom < MIN_OBSERVABLE_STIMULUS_SCORE) {
            return CreatureObservation.INSUFFICIENT_EVIDENCE;
        }
        return CreatureObservation.OTHER_DOMINANT_RESPONSE;
    }

    public static ResearchNextStep resolveResearchNextStep(
            ExpeditionGoal pinnedGoal,
            int researchStageMask,
            boolean hasExtractor,
            boolean hasScanner,
            boolean hasProjectileSampleInInv,
            boolean hasBiomassInInv,
            boolean hasLatentSporeInInv,
            boolean hasCatalystInInv,
            boolean hasBenchMilestone,
            double currentThreat
    ) {
        if ((researchStageMask & RESEARCH_BIT_ADAPTATION_IDENTIFIED) == 0) {
            return ResearchNextStep.AWAITING_FIRST_ADAPTATION;
        }
        if (pinnedGoal == null || pinnedGoal == ExpeditionGoal.NONE) {
            return ResearchNextStep.CHOOSE_EXPEDITION_GOAL;
        }
        if (pinnedGoal == ExpeditionGoal.USE_TO_ADVANTAGE) {
            if ((researchStageMask & RESEARCH_BIT_BIOTECH_SYNTHESIZED) != 0) {
                return ResearchNextStep.COMPLETED_BIOTECH_SYNTHESIS;
            }
            if (hasProjectileSampleInInv || (researchStageMask & RESEARCH_BIT_SAMPLE_EXTRACTED) != 0) {
                if (!hasBiomassInInv) {
                    return ResearchNextStep.SAMPLE_READY_MISSING_BIOMASS;
                }
                if (!hasCatalystInInv) {
                    return ResearchNextStep.SAMPLE_READY_MISSING_CATALYST;
                }
                if (!hasBenchMilestone) {
                    return ResearchNextStep.SAMPLE_READY_MISSING_BENCH;
                }
                return ResearchNextStep.SAMPLE_READY_TO_SYNTHESIZE;
            }
            return hasExtractor
                    ? ResearchNextStep.WEAKEN_AND_EXTRACT_SAMPLE
                    : ResearchNextStep.MISSING_BIO_EXTRACTOR;
        }
        if ((researchStageMask & RESEARCH_BIT_NODE_DESTROYED) != 0) {
            return ResearchNextStep.COMPLETED_NODE_DESTRUCTION;
        }
        if (!hasScanner) {
            return ResearchNextStep.MISSING_BIO_SCANNER;
        }
        if (!Double.isFinite(currentThreat) || currentThreat <= 0.0D) {
            return ResearchNextStep.THREAT_ALREADY_ZERO;
        }
        if ((researchStageMask & RESEARCH_BIT_NODE_THREAT_RECORDED) == 0) {
            return ResearchNextStep.LOCATE_NODE_AND_RECORD_THREAT;
        }
        return ResearchNextStep.DESTROY_LOCATED_NODE;
    }
}
