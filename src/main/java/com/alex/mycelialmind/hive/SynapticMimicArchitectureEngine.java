package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.entity.SynapticMimicEntity.MimicStance;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Pure decision and telemetry engine for the Synaptic Mimic architecture:
 * infiltration spoofing, acoustic lure, tripartite network sabotage,
 * habit-aware combat stance adaptation, Symbiotic Mutagen parasitism,
 * and bio-scanner unmasking counterplay.
 */
public final class SynapticMimicArchitectureEngine {
    public enum MimicPersona {
        INFILTRATION_SPOOF(0, "infiltration_spoof"),
        ECHO_LURE(1, "echo_lure"),
        NETWORK_SABOTEUR(2, "network_saboteur"),
        UNMASKED_DUELIST(3, "unmasked_duelist");

        private final int id;
        private final String translationKey;

        MimicPersona(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return "persona.mycelialmind." + translationKey;
        }

        public boolean isCovert() {
            return this == INFILTRATION_SPOOF || this == ECHO_LURE;
        }

        public static MimicPersona fromId(int id) {
            for (MimicPersona persona : values()) {
                if (persona.id == id) {
                    return persona;
                }
            }
            return INFILTRATION_SPOOF;
        }
    }

    private SynapticMimicArchitectureEngine() {
    }

    public static MimicPersona evaluatePersona(
            boolean unmasked,
            boolean desynced,
            boolean nearSabotageTarget,
            boolean hasNearbyDefensesOrDiplomacy,
            boolean onMyceliumOrNearDecoy
    ) {
        if (unmasked || desynced) {
            return MimicPersona.UNMASKED_DUELIST;
        }
        if (nearSabotageTarget) {
            return MimicPersona.NETWORK_SABOTEUR;
        }
        if (hasNearbyDefensesOrDiplomacy) {
            return MimicPersona.INFILTRATION_SPOOF;
        }
        if (onMyceliumOrNearDecoy) {
            return MimicPersona.ECHO_LURE;
        }
        return MimicPersona.INFILTRATION_SPOOF;
    }

    public static MimicStance evaluateCombatStance(
            boolean hasProjectileWeapon,
            boolean hasShield,
            int armorValue,
            @Nullable StimulusType dominantStimulus,
            @Nullable Set<PlayerHabit> activeHabits
    ) {
        if (hasProjectileWeapon) {
            return MimicStance.SPORE_MARKSMAN;
        }
        if (hasShield || armorValue >= 15) {
            return MimicStance.AEGIS_MIRROR;
        }

        if (activeHabits != null && !activeHabits.isEmpty()) {
            if (activeHabits.contains(PlayerHabit.HIGH_MOBILITY)
                    || activeHabits.contains(PlayerHabit.BASTION_BUILDER)) {
                return MimicStance.SPORE_MARKSMAN;
            }
            if (activeHabits.contains(PlayerHabit.SHADOW_INFILTRATOR)) {
                return MimicStance.AEGIS_MIRROR;
            }
            if (activeHabits.contains(PlayerHabit.COMBAT_MEDIC)) {
                return MimicStance.BLADE_DANCER;
            }
        }

        if (dominantStimulus == StimulusType.PROJECTILE) {
            return MimicStance.SPORE_MARKSMAN;
        }
        if (dominantStimulus == StimulusType.EXPLOSION || dominantStimulus == StimulusType.MAGIC) {
            return MimicStance.AEGIS_MIRROR;
        }
        return MimicStance.BLADE_DANCER;
    }

    @Nullable
    public static SymbioticMutagen selectParasitizedMutagen(
            @Nullable Set<SymbioticMutagen> equippedMutagens,
            @Nullable MimicStance stance,
            boolean desynced
    ) {
        if (desynced || equippedMutagens == null || equippedMutagens.isEmpty()) {
            return null;
        }
        MimicStance safeStance = stance != null ? stance : MimicStance.BLADE_DANCER;
        SymbioticMutagen[] priority = switch (safeStance) {
            case BLADE_DANCER -> new SymbioticMutagen[]{SymbioticMutagen.CHITIN_THORNS, SymbioticMutagen.PYRO_GLAND};
            case SPORE_MARKSMAN -> new SymbioticMutagen[]{SymbioticMutagen.KINETIC_STRAFE, SymbioticMutagen.PYRO_GLAND};
            case AEGIS_MIRROR -> new SymbioticMutagen[]{SymbioticMutagen.BLAST_CARAPACE, SymbioticMutagen.ARCANE_SYNAPSE};
        };
        for (SymbioticMutagen candidate : priority) {
            if (equippedMutagens.contains(candidate)) {
                return candidate;
            }
        }
        for (SymbioticMutagen any : SymbioticMutagen.values()) {
            if (equippedMutagens.contains(any)) {
                return any;
            }
        }
        return null;
    }

    public static StimulusType selectDeceptiveFalseStimulus(
            @Nullable MimicStance stance,
            @Nullable StimulusType actualDominant
    ) {
        MimicStance safeStance = stance != null ? stance : MimicStance.BLADE_DANCER;
        StimulusType preferred = switch (safeStance) {
            case SPORE_MARKSMAN -> StimulusType.PROJECTILE;
            case AEGIS_MIRROR -> StimulusType.EXPLOSION;
            case BLADE_DANCER -> StimulusType.MELEE;
        };
        if (preferred != actualDominant) {
            return preferred;
        }
        StimulusType secondary = switch (safeStance) {
            case SPORE_MARKSMAN -> StimulusType.FIRE;
            case AEGIS_MIRROR -> StimulusType.MAGIC;
            case BLADE_DANCER -> StimulusType.FIRE;
        };
        if (secondary != actualDominant) {
            return secondary;
        }
        for (StimulusType type : StimulusType.values()) {
            if (type != actualDominant) {
                return type;
            }
        }
        return StimulusType.MELEE;
    }

    public static int computeSymbioticStabilityDrain(int currentStability, @Nullable EvolutionRank rank) {
        EvolutionRank safeRank = rank != null ? rank : EvolutionRank.BASE;
        int drain = switch (safeRank) {
            case BASE -> 15;
            case MUTATED -> 20;
            case ELITE -> 25;
        };
        return Math.max(0, Math.min(100, currentStability) - drain);
    }

    public static float computeUnmaskShockDamageMultiplier(boolean shockActive, boolean desynced) {
        if (shockActive) {
            return 1.25F;
        }
        if (desynced) {
            return 1.10F;
        }
        return 1.0F;
    }

    @Nullable
    public static String computeMimicCounterIntelDropId(
            @Nullable EvolutionRank rank,
            boolean unmaskedByScannerOrShock,
            float roll
    ) {
        EvolutionRank safeRank = rank != null ? rank : EvolutionRank.BASE;
        if (!unmaskedByScannerOrShock && safeRank == EvolutionRank.BASE) {
            return null;
        }
        if (safeRank == EvolutionRank.ELITE && unmaskedByScannerOrShock && roll < 0.35F) {
            return "mycelialmind:false_engram_injector";
        }
        if (roll < 0.65F) {
            return "mycelialmind:neural_echo_core";
        }
        return "mycelialmind:volatile_engram";
    }
}
