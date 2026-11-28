package com.alex.mycelialmind.mixin;

import com.alex.mycelialmind.util.MycelialMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Suppresses natural / chunk-generation spawns of Mycelial Mind monsters inside active Purification Pylons
 * while preserving spawn eggs, spawners, commands, and vanilla monster farms.
 */
@Mixin(Monster.class)
public abstract class MonsterMixin {

    @ModifyReturnValue(method = "checkMonsterSpawnRules", at = @At("RETURN"))
    private static boolean mycelialmind$filterNaturalMycelialSpawn(
            boolean original,
            EntityType<? extends Monster> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        return MycelialMixinHooks.allowNaturalMycelialSpawn(original, type, level, spawnType, pos);
    }

    @ModifyReturnValue(method = "checkAnyLightMonsterSpawnRules", at = @At("RETURN"))
    private static boolean mycelialmind$filterAnyLightMycelialSpawn(
            boolean original,
            EntityType<? extends Monster> type,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        return MycelialMixinHooks.allowNaturalMycelialSpawn(original, type, level, spawnType, pos);
    }
}
