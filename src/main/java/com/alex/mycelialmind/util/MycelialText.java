package com.alex.mycelialmind.util;

import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * Common-safe translation helpers for Hive Mind enums, telemetry stages, and directions.
 */
public final class MycelialText {
    private MycelialText() {
    }

    public static MutableComponent stimulus(StimulusType type) {
        return Component.translatable("stimulus.mycelialmind." + type.id());
    }

    public static MutableComponent trait(AdaptationTrait trait) {
        return Component.translatable("trait.mycelialmind." + trait.id());
    }

    public static MutableComponent traitTier2(AdaptationTrait trait) {
        return Component.translatable("trait_t2.mycelialmind." + trait.tier2BehaviorId());
    }

    public static MutableComponent synergy(HiveSynergy synergy) {
        return Component.translatable("synergy.mycelialmind." + synergy.id());
    }

    public static MutableComponent habit(PlayerHabit habit) {
        return Component.translatable("habit.mycelialmind." + habit.id());
    }

    public static MutableComponent mutagen(SymbioticMutagen mutagen) {
        return Component.translatable("mutagen.mycelialmind." + mutagen.id());
    }

    public static MutableComponent progressionTier(HiveProgressionTier tier) {
        return Component.translatable("tier.mycelialmind." + tier.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent homeostasisStage(DimensionalHomeostasisEngine.HomeostasisStage stage) {
        return Component.translatable("homeostasis.mycelialmind." + stage.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent strain(HiveStrain strain) {
        return Component.translatable("strain.mycelialmind." + strain.serializedName());
    }

    public static MutableComponent nodeType(SynapticNodeType type) {
        return Component.translatable("node_type.mycelialmind." + type.id());
    }

    public static MutableComponent standing(PlayerDiplomacyData.Standing standing) {
        return Component.translatable("standing.mycelialmind." + standing.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent rank(EvolutionRank rank) {
        return Component.translatable("rank.mycelialmind." + rank.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent chimeraMode(MycelialChimeraEntity.CommandMode mode) {
        return Component.translatable("chimera_mode.mycelialmind." + mode.name().toLowerCase(Locale.ROOT));
    }

    public static MutableComponent cardinalDirection(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return Component.translatable("hud.mycelialmind.dir.here");
        }
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // 0=+X(E), 90=+Z(S), 180/-180=-X(W), -90=-Z(N)
        if (angle >= -22.5 && angle < 22.5) return Component.translatable("hud.mycelialmind.dir.e");
        if (angle >= 22.5 && angle < 67.5) return Component.translatable("hud.mycelialmind.dir.se");
        if (angle >= 67.5 && angle < 112.5) return Component.translatable("hud.mycelialmind.dir.s");
        if (angle >= 112.5 && angle < 157.5) return Component.translatable("hud.mycelialmind.dir.sw");
        if (angle >= 157.5 || angle < -157.5) return Component.translatable("hud.mycelialmind.dir.w");
        if (angle >= -157.5 && angle < -112.5) return Component.translatable("hud.mycelialmind.dir.nw");
        if (angle >= -112.5 && angle < -67.5) return Component.translatable("hud.mycelialmind.dir.n");
        return Component.translatable("hud.mycelialmind.dir.ne");
    }
}
