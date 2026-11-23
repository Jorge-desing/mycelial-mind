package com.alex.mycelialmind.hive;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, Minecraft-free combat architecture engine shared by all Mycelial Mind bosses
 * (Spore Behemoth, Hive Core, and Apex Matriarch).
 *
 * <p>Covers:
 * <ul>
 *     <li>Exclusive combat state machine ({@link BossCombatState})</li>
 *     <li>Utility-based adaptive attack selection with anti-spam and Hive Mind habit/stimulus awareness</li>
 *     <li>Advanced-phase single-step combo chaining</li>
 *     <li>Physical carapace integrity / shatter math and exposed weak-point damage multipliers</li>
 * </ul>
 */
public final class BossCombatArchitectureEngine {
    public static final float NORMAL_WEAK_POINT_MULTIPLIER = 1.35F;
    public static final float EXPOSED_WEAK_POINT_MULTIPLIER = 1.55F;
    public static final float INTACT_CARAPACE_MULTIPLIER = 0.75F;
    public static final float BROKEN_CARAPACE_MULTIPLIER = 1.00F;

    private BossCombatArchitectureEngine() {
    }

    public enum BossCombatState {
        IDLE,
        WINDUP,
        ACTIVE,
        RECOVERY,
        STAGGERED;

        public static BossCombatState fromOrdinal(int ordinal) {
            BossCombatState[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return IDLE;
            }
            return values[ordinal];
        }
    }

    public enum BossCarapaceProfile {
        SPORE_BEHEMOTH(55.0F, 40, 130, -1.05D),
        HIVE_CORE(80.0F, 45, 145, -1.20D),
        APEX_MATRIARCH(110.0F, 50, 160, -1.45D);

        private final float maxIntegrity;
        private final int staggerTicks;
        private final int brokenWindowTicks;
        private final double weakPointLowerOffset;

        BossCarapaceProfile(float maxIntegrity, int staggerTicks, int brokenWindowTicks, double weakPointLowerOffset) {
            this.maxIntegrity = maxIntegrity;
            this.staggerTicks = staggerTicks;
            this.brokenWindowTicks = brokenWindowTicks;
            this.weakPointLowerOffset = weakPointLowerOffset;
        }

        public float maxIntegrity() {
            return maxIntegrity;
        }

        public int staggerTicks() {
            return staggerTicks;
        }

        public int brokenWindowTicks() {
            return brokenWindowTicks;
        }

        public double weakPointLowerOffset() {
            return weakPointLowerOffset;
        }
    }

    public enum BossAttackId {
        NONE("none", 1, 0, 0, 0, 0, 0.0D, 0.0D, null, null, false),

        // Spore Behemoth (2 phases)
        BEHEMOTH_SEISMIC_CHARGE("behemoth_seismic_charge", 1, 22, 10, 20, 95, 3.5D, 16.0D, StimulusType.PROJECTILE, PlayerHabit.HIGH_MOBILITY, false),
        BEHEMOTH_SEISMIC_SLAM("behemoth_seismic_slam", 1, 18, 6, 18, 70, 0.0D, 5.5D, StimulusType.MELEE, PlayerHabit.COMBAT_MEDIC, false),
        BEHEMOTH_SPORE_RAIN("behemoth_spore_rain", 1, 20, 8, 16, 110, 4.5D, 20.0D, StimulusType.FIRE, PlayerHabit.BASTION_BUILDER, false),
        BEHEMOTH_MYCELIAL_SHIELD("behemoth_mycelial_shield", 1, 16, 6, 14, 150, 0.0D, 24.0D, StimulusType.EXPLOSION, null, true),

        // Hive Core (3 phases)
        HIVE_CORE_ACID_FAN("hive_core_acid_fan", 1, 18, 6, 16, 85, 4.0D, 24.0D, StimulusType.PROJECTILE, PlayerHabit.HIGH_MOBILITY, false),
        HIVE_CORE_SYNAPTIC_SWEEP("hive_core_synaptic_sweep", 1, 16, 6, 16, 75, 0.0D, 6.5D, StimulusType.MELEE, PlayerHabit.SHADOW_INFILTRATOR, false),
        HIVE_CORE_COMMAND_PULSE("hive_core_command_pulse", 1, 24, 8, 18, 145, 0.0D, 28.0D, StimulusType.MAGIC, PlayerHabit.BASTION_BUILDER, false),
        HIVE_CORE_ANCHOR_RAY("hive_core_anchor_ray", 2, 24, 8, 18, 85, 0.0D, 28.0D, StimulusType.PROJECTILE, PlayerHabit.HIGH_MOBILITY, false),
        HIVE_CORE_SPORE_SHOCKWAVE("hive_core_spore_shockwave", 3, 28, 8, 22, 115, 0.0D, 10.5D, StimulusType.MELEE, PlayerHabit.COMBAT_MEDIC, false),

        // Apex Matriarch (3 phases)
        MATRIARCH_SPORE_BARRAGE("matriarch_spore_barrage", 1, 18, 8, 16, 75, 4.0D, 28.0D, StimulusType.PROJECTILE, PlayerHabit.HIGH_MOBILITY, false),
        MATRIARCH_ECHO_TELEPORT("matriarch_echo_teleport", 1, 22, 8, 16, 115, 7.0D, 32.0D, StimulusType.PROJECTILE, PlayerHabit.HIGH_MOBILITY, false),
        MATRIARCH_HOMEOSTATIC_PULSE("matriarch_homeostatic_pulse", 2, 24, 8, 18, 125, 0.0D, 20.0D, StimulusType.EXPLOSION, PlayerHabit.BASTION_BUILDER, false),
        MATRIARCH_SYNAPTIC_STORM("matriarch_synaptic_storm", 3, 26, 10, 20, 100, 0.0D, 12.0D, StimulusType.MELEE, PlayerHabit.COMBAT_MEDIC, false);

        private final String id;
        private final int minPhase;
        private final int windupTicks;
        private final int activeTicks;
        private final int recoveryTicks;
        private final int cooldownTicks;
        private final double minRange;
        private final double maxRange;
        private final StimulusType counterStimulus;
        private final PlayerHabit counterHabit;
        private final boolean requiresShieldAllowed;

        BossAttackId(
                String id,
                int minPhase,
                int windupTicks,
                int activeTicks,
                int recoveryTicks,
                int cooldownTicks,
                double minRange,
                double maxRange,
                StimulusType counterStimulus,
                PlayerHabit counterHabit,
                boolean requiresShieldAllowed
        ) {
            this.id = id;
            this.minPhase = minPhase;
            this.windupTicks = windupTicks;
            this.activeTicks = activeTicks;
            this.recoveryTicks = recoveryTicks;
            this.cooldownTicks = cooldownTicks;
            this.minRange = minRange;
            this.maxRange = maxRange;
            this.counterStimulus = counterStimulus;
            this.counterHabit = counterHabit;
            this.requiresShieldAllowed = requiresShieldAllowed;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "attack.mycelialmind." + id;
        }

        public int minPhase() {
            return minPhase;
        }

        public int windupTicks() {
            return windupTicks;
        }

        public int activeTicks() {
            return activeTicks;
        }

        public int recoveryTicks() {
            return recoveryTicks;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public double minRange() {
            return minRange;
        }

        public double maxRange() {
            return maxRange;
        }

        public StimulusType counterStimulus() {
            return counterStimulus;
        }

        public PlayerHabit counterHabit() {
            return counterHabit;
        }

        public boolean requiresShieldAllowed() {
            return requiresShieldAllowed;
        }

        public static BossAttackId fromOrdinal(int ordinal) {
            BossAttackId[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return NONE;
            }
            return values[ordinal];
        }
    }

    /**
     * Evaluates a boss's attack kit and picks the highest-utility eligible attack
     * based on phase, target distance, cooldowns, anti-spam history, and Hive Mind telemetry.
     */
    public static BossAttackId selectNextAttack(
            List<BossAttackId> kit,
            int currentPhase,
            double distance,
            Map<BossAttackId, Integer> cooldowns,
            BossAttackId lastAttack,
            StimulusType dominantStimulus,
            Set<PlayerHabit> activeHabits,
            boolean shieldAllowed
    ) {
        if (kit == null || kit.isEmpty()) {
            return BossAttackId.NONE;
        }
        BossAttackId best = BossAttackId.NONE;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (BossAttackId candidate : kit) {
            if (candidate == null || candidate == BossAttackId.NONE) {
                continue;
            }
            if (currentPhase < candidate.minPhase()) {
                continue;
            }
            if (candidate.requiresShieldAllowed() && !shieldAllowed) {
                continue;
            }
            int remainingCd = cooldowns == null ? 0 : cooldowns.getOrDefault(candidate, 0);
            if (remainingCd > 0) {
                continue;
            }
            if (distance < candidate.minRange() || distance > candidate.maxRange()) {
                continue;
            }

            double score = 10.0D;

            // Range center preference: reward attacks whose sweet spot matches current distance
            double midRange = (candidate.minRange() + candidate.maxRange()) * 0.5D;
            double span = Math.max(1.0D, (candidate.maxRange() - candidate.minRange()) * 0.5D);
            double normalizedOffset = Math.min(1.0D, Math.abs(distance - midRange) / span);
            score += (1.0D - normalizedOffset) * 4.0D;

            // Higher-phase signature attacks get a slight priority bump once unlocked
            if (candidate.minPhase() == currentPhase && currentPhase >= 2) {
                score += 2.5D;
            }

            // Adaptive Hive Mind counter-selection
            if (dominantStimulus != null && candidate.counterStimulus() == dominantStimulus) {
                score += 3.5D;
            }
            if (activeHabits != null && candidate.counterHabit() != null && activeHabits.contains(candidate.counterHabit())) {
                score += 4.0D;
            }

            // Anti-spam penalty so bosses weave varied patterns
            if (candidate == lastAttack) {
                score -= 5.5D;
            }

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Resolves whether a completed attack chains into a single short combo follow-up
     * in advanced boss phases.
     */
    public static BossAttackId resolveComboFollowUp(
            BossAttackId completedAttack,
            int currentPhase,
            double distance,
            boolean alreadyChained
    ) {
        if (completedAttack == null || alreadyChained) {
            return BossAttackId.NONE;
        }
        return switch (completedAttack) {
            case BEHEMOTH_SEISMIC_CHARGE -> (currentPhase >= 2 && distance <= 7.0D)
                    ? BossAttackId.BEHEMOTH_SEISMIC_SLAM
                    : BossAttackId.NONE;
            case HIVE_CORE_SYNAPTIC_SWEEP -> (currentPhase >= 3 && distance <= 9.0D)
                    ? BossAttackId.HIVE_CORE_SPORE_SHOCKWAVE
                    : BossAttackId.NONE;
            case MATRIARCH_ECHO_TELEPORT -> (currentPhase >= 3 && distance <= 10.0D)
                    ? BossAttackId.MATRIARCH_SYNAPTIC_STORM
                    : BossAttackId.NONE;
            default -> BossAttackId.NONE;
        };
    }

    /**
     * Computes scaled windup duration for advanced phases (faster combos / enraged tempo).
     */
    public static int resolveEffectiveWindupTicks(BossAttackId attack, int currentPhase, boolean isComboFollowUp) {
        if (attack == null || attack == BossAttackId.NONE) {
            return 0;
        }
        int base = attack.windupTicks();
        if (isComboFollowUp) {
            return Math.max(10, Math.round(base * 0.70F));
        }
        if (currentPhase >= 3) {
            return Math.max(12, Math.round(base * 0.85F));
        }
        return base;
    }

    /**
     * Computes scaled cooldown for advanced phases.
     */
    public static int resolveEffectiveCooldownTicks(BossAttackId attack, int currentPhase) {
        if (attack == null || attack == BossAttackId.NONE) {
            return 0;
        }
        int base = attack.cooldownTicks();
        if (currentPhase >= 3) {
            return Math.max(35, Math.round(base * 0.80F));
        }
        if (currentPhase == 2) {
            return Math.max(40, Math.round(base * 0.90F));
        }
        return base;
    }

    /**
     * Computes damage inflicted to the boss's physical carapace integrity pool when
     * the {@code ARMORED_CARAPACE} part is struck.
     */
    public static float computeCarapaceIntegrityDamage(float baseAmount, boolean bypassesArmor, boolean isMagic) {
        if (baseAmount <= 0.0F) {
            return 0.0F;
        }
        float factor = (bypassesArmor || isMagic) ? 1.25F : 1.0F;
        return baseAmount * factor;
    }

    /**
     * Computes final boss HP damage for a multi-part hit, factoring in whether the
     * boss's physical carapace is currently broken (exposing the weak point to 1.55x
     * and removing frontal carapace mitigation).
     */
    public static float computeBossPartDamageWithCarapace(
            boolean isWeakPoint,
            boolean isArmoredCarapace,
            boolean carapaceBroken,
            boolean bypassesArmor,
            boolean isMagic,
            float baseAmount
    ) {
        if (baseAmount <= 0.0F) {
            return 0.0F;
        }
        if (isWeakPoint) {
            return baseAmount * (carapaceBroken ? EXPOSED_WEAK_POINT_MULTIPLIER : NORMAL_WEAK_POINT_MULTIPLIER);
        }
        if (isArmoredCarapace) {
            if (carapaceBroken) {
                return baseAmount * BROKEN_CARAPACE_MULTIPLIER;
            }
            if (bypassesArmor || isMagic) {
                return baseAmount * 0.90F;
            }
            return baseAmount * INTACT_CARAPACE_MULTIPLIER;
        }
        return baseAmount;
    }

    public static int resolveBehemothPhase(float hpRatio) {
        return hpRatio <= 0.50F ? 2 : 1;
    }

    public static int resolveHiveCorePhase(float hpRatio, boolean phase2ShieldBroken) {
        if (hpRatio <= 0.30F && phase2ShieldBroken) {
            return 3;
        }
        if (hpRatio <= 0.65F) {
            return 2;
        }
        return 1;
    }

    public static int resolveMatriarchPhase(float hpRatio) {
        if (hpRatio <= 0.25F) {
            return 3;
        }
        if (hpRatio <= 0.60F) {
            return 2;
        }
        return 1;
    }
}
