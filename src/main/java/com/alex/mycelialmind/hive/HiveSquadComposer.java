package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModEntities;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Counter-tactical squad composer: picks MycelialMob types that counter the player's
 * dominant StimulusType, modulated by the active HiveDoctrine.
 */
public final class HiveSquadComposer {
    private HiveSquadComposer() {
    }

    public static StimulusType findDominantStimulus(HiveMindSavedData hive) {
        StimulusType best = StimulusType.MELEE;
        double bestScore = -1.0;
        for (StimulusType type : StimulusType.values()) {
            double score = hive.getEffectiveThreatScore(type);
            if (score > bestScore) {
                bestScore = score;
                best = type;
            }
        }
        return best;
    }

    public static List<EntityType<? extends MycelialMob>> composeWaveSquad(
            HiveMindSavedData hive,
            HiveDoctrine doctrine,
            int milestone,
            RandomSource random
    ) {
        return composeWaveSquad(hive, doctrine, milestone, random, HiveBlackboard.PacingPhase.BUILD_UP, 0, false);
    }

    public static List<EntityType<? extends MycelialMob>> composeWaveSquad(
            HiveMindSavedData hive,
            HiveDoctrine doctrine,
            int milestone,
            RandomSource random,
            HiveBlackboard.PacingPhase pacingPhase,
            int symbioticCount,
            boolean fungalTideActive
    ) {
        StimulusType dominant = findDominantStimulus(hive);
        int count = switch (milestone) {
            case 25 -> 3;
            case 50 -> 4;
            case 75 -> 5;
            default -> 6;
        };
        if (doctrine == HiveDoctrine.SIEGE_ASSAULT) {
            count++;
        }
        if (fungalTideActive) {
            count++;
        }
        if (pacingPhase == HiveBlackboard.PacingPhase.RELAXATION && count > 2) {
            count--;
        }

        List<EntityType<? extends MycelialMob>> pool = counterPoolFor(dominant);
        List<EntityType<? extends MycelialMob>> squad = new ArrayList<>(count);
        boolean addedDominantCounter = false;
        for (int i = 0; i < count; i++) {
            if (i == 0 && (doctrine == HiveDoctrine.RECOVERY || (symbioticCount > 0 && doctrine != HiveDoctrine.DORMANT))) {
                squad.add(ModEntities.SPORE_ARCHITECT.get());
            } else if (i == count - 1 && (doctrine == HiveDoctrine.SIEGE_ASSAULT || fungalTideActive)) {
                squad.add(milestone >= 75 ? ModEntities.COLONY_WARDEN.get() : ModEntities.GOLIATH_CARRIER.get());
            } else if (i == 1 && hive.getThreatLevel() >= 50.0 && random.nextFloat() < 0.35F) {
                squad.add(ModEntities.SYNAPTIC_MIMIC.get());
            } else if (!addedDominantCounter) {
                // Keep at least one direct counter in each wave; random pool sampling alone
                // could omit the counter completely, especially in small early waves.
                squad.add(pool.get(0));
                addedDominantCounter = true;
            } else {
                squad.add(pool.get(random.nextInt(pool.size())));
            }
        }
        return squad;
    }

    public static List<EntityType<? extends MycelialMob>> composeIncubatorPair(
            HiveMindSavedData hive,
            HiveDoctrine doctrine,
            boolean hasLinks
    ) {
        StimulusType dominant = findDominantStimulus(hive);
        List<EntityType<? extends MycelialMob>> pool = counterPoolFor(dominant);
        EntityType<? extends MycelialMob> lead = doctrine == HiveDoctrine.RECOVERY
                ? ModEntities.SPORE_ARCHITECT.get()
                : (hive.getThreatLevel() >= 50.0 && hasLinks ? ModEntities.SYNAPTIC_MIMIC.get() : pool.get(0));
        EntityType<? extends MycelialMob> escort = (hasLinks && doctrine == HiveDoctrine.SIEGE_ASSAULT)
                ? ModEntities.SYNAPSE_WEAVER.get()
                : pool.get(Math.min(1, pool.size() - 1));
        return List.of(lead, escort);
    }

    private static List<EntityType<? extends MycelialMob>> counterPoolFor(StimulusType dominant) {
        return switch (dominant) {
            case MELEE -> List.of(
                    ModEntities.ACID_SPITTER.get(),
                    ModEntities.MYCELIAL_STALKER.get(),
                    ModEntities.PYRO_SHROOM.get()
            );
            case PROJECTILE -> List.of(
                    ModEntities.CHITIN_BRUTE.get(),
                    ModEntities.BURROWER.get(),
                    ModEntities.MYCELIAL_STALKER.get()
            );
            case FIRE -> List.of(
                    ModEntities.PYRO_SHROOM.get(),
                    ModEntities.COLONY_WARDEN.get(),
                    ModEntities.ACID_SPITTER.get()
            );
            case EXPLOSION -> List.of(
                    ModEntities.GOLIATH_CARRIER.get(),
                    ModEntities.SPORELING.get(),
                    ModEntities.BURROWER.get()
            );
            case MAGIC -> List.of(
                    ModEntities.SYNAPSE_WEAVER.get(),
                    ModEntities.LEECH_PARASITE.get(),
                    ModEntities.ACID_SPITTER.get()
            );
        };
    }
}
