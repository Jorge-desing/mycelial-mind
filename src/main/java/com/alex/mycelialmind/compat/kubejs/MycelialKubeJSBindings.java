package com.alex.mycelialmind.compat.kubejs;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Pure server-side scripting facade exposed to KubeJS scripts under the global
 * {@code MycelialMind} binding. Safe to call and unit-test without KubeJS loaded.
 */
public final class MycelialKubeJSBindings {
    private MycelialKubeJSBindings() {
    }

    @Nullable
    public static StimulusType parseStimulusType(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (StimulusType type : StimulusType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static AdaptationTrait parseAdaptationTrait(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if (trait.name().equals(normalized)) {
                return trait;
            }
        }
        return null;
    }

    public static double getThreat(@Nullable ServerLevel level) {
        if (level == null) {
            return 0.0;
        }
        return HiveMindSavedData.get(level).getThreatLevel();
    }

    public static void addThreat(@Nullable ServerLevel level, double delta) {
        if (level == null || !Double.isFinite(delta) || delta == 0.0) {
            return;
        }
        HiveMindSavedData data = HiveMindSavedData.get(level);
        if (delta > 0.0) {
            data.addThreatPenalty(delta);
        } else {
            data.reduceThreat(-delta);
        }
    }

    public static void setThreat(@Nullable ServerLevel level, double targetThreat) {
        if (level == null || !Double.isFinite(targetThreat)) {
            return;
        }
        double clamped = Math.max(0.0, Math.min(100.0, targetThreat));
        HiveMindSavedData data = HiveMindSavedData.get(level);
        double current = data.getThreatLevel();
        if (clamped > current) {
            data.addThreatPenalty(clamped - current);
        } else if (clamped < current) {
            data.reduceThreat(current - clamped);
        }
    }

    public static double getStimulus(@Nullable ServerLevel level, @Nullable String stimulusName) {
        StimulusType type = parseStimulusType(stimulusName);
        if (level == null || type == null) {
            return 0.0;
        }
        return HiveMindSavedData.get(level).getStimulusScore(type);
    }

    public static boolean addStimulus(@Nullable ServerLevel level, @Nullable String stimulusName, double amount) {
        StimulusType type = parseStimulusType(stimulusName);
        if (level == null || type == null || !Double.isFinite(amount) || amount <= 0.0) {
            return false;
        }
        HiveMindSavedData.get(level).recordStimulus(type, amount, level.getGameTime());
        return true;
    }

    public static List<String> getActiveTraits(@Nullable ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return HiveMindSavedData.get(level).getActiveTraits().stream()
                .map(Enum::name)
                .sorted()
                .toList();
    }

    public static List<String> getTier2Traits(@Nullable ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return HiveMindSavedData.get(level).getTier2Traits().stream()
                .map(Enum::name)
                .sorted()
                .toList();
    }

    public static List<String> getActiveSynergies(@Nullable ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return HiveMindSavedData.get(level).getActiveSynergies().stream()
                .map(HiveSynergy::name)
                .sorted()
                .toList();
    }

    public static boolean hasTrait(@Nullable ServerLevel level, @Nullable String traitName) {
        AdaptationTrait trait = parseAdaptationTrait(traitName);
        if (level == null || trait == null) {
            return false;
        }
        return HiveMindSavedData.get(level).hasTrait(trait);
    }

    public static boolean isProtected(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        return PurificationZoneTracker.get(level).isProtected(pos);
    }

    public static int getActivePylonCount(@Nullable ServerLevel level) {
        if (level == null) {
            return 0;
        }
        return PurificationZoneTracker.get(level).countPylons();
    }

    public static int getActiveNodeCount(@Nullable ServerLevel level) {
        if (level == null) {
            return 0;
        }
        return PurificationZoneTracker.get(level).countNodes();
    }

    public static double getHomeostasisStress(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        if (level == null || pos == null) {
            return 0.0;
        }
        return DimensionalHomeostasisSavedData.get(level).getStress(new ChunkPos(pos));
    }

    public static void suppressHive(@Nullable ServerLevel level, int seconds) {
        if (level == null || seconds <= 0) {
            return;
        }
        long now = level.getGameTime();
        long until = now + (long) seconds * 20L;
        HiveMindSavedData.get(level).suppressHigherAdaptations(until, now);
    }

    public static void resetHive(@Nullable ServerLevel level) {
        if (level == null) {
            return;
        }
        HiveMindSavedData.get(level).resetAll();
    }
}
