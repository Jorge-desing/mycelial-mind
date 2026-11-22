package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.LivingBattlefieldSystem;
import com.alex.mycelialmind.hive.StimulusType;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Battlefield biomass recycling goal: guides wounded or BASE-rank scavengers toward
 * recently fallen allied carrion sites (non-fire, outside Pylons) to heal and gain combat XP.
 */
public class NecrophagyGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;
    @Nullable
    private Vec3 targetCarrion;

    public NecrophagyGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.isDesynced() || !(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        if (mob.getHealth() >= mob.getMaxHealth() * 0.85F && mob.getEvolutionRank() != EvolutionRank.BASE) {
            return false;
        }
        this.targetCarrion = HiveBlackboard.findNearestCarrion(level, mob.position(), 12.0D);
        return this.targetCarrion != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetCarrion != null
                && !mob.isDesynced()
                && mob.distanceToSqr(this.targetCarrion) > 2.25D
                && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (this.targetCarrion != null) {
            mob.getNavigation().moveTo(this.targetCarrion.x, this.targetCarrion.y, this.targetCarrion.z, speedModifier);
        }
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level) || this.targetCarrion == null) {
            return;
        }
        if (mob.distanceToSqr(this.targetCarrion) <= 4.0D) {
            Vec3 consumed = HiveBlackboard.consumeNearestCarrion(level, mob.position(), 3.0D);
            if (consumed != null) {
                mob.heal(LivingBattlefieldSystem.CARRION_HEAL_AMOUNT);
                MycelialSblBridge.addCombatXp(level, mob, StimulusType.MELEE, LivingBattlefieldSystem.CARRION_XP_REWARD);
                level.sendParticles(ModParticles.NECROTIC_MIST.get(), mob.getX(), mob.getY(0.6), mob.getZ(),
                        10, 0.3, 0.3, 0.3, 0.03);
                level.playSound(null, mob.blockPosition(), ModSounds.NECROPHAGY_FEAST.get(), SoundSource.HOSTILE, 0.8F, 0.9F);
            }
            this.targetCarrion = null;
        }
    }

    @Override
    public void stop() {
        this.targetCarrion = null;
    }
}
