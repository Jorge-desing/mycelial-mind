package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.EphemeralChitinBlock;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.hive.LivingBattlefieldSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Combat terraforming goal: builds 12s auto-decaying Ephemeral Chitin steps against pillar-cheese,
 * deploys low chitin barricades under projectile fire, and applies corrosive jam to Purification Turrets.
 */
public class TacticalTerraformingGoal extends Goal {
    private final MycelialMob mob;
    private long nextRampTick = 0L;
    private long nextBarricadeTick = 0L;
    private long nextSabotageTick = 0L;

    public TacticalTerraformingGoal(MycelialMob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (mob.isDesynced() || !(mob.level() instanceof ServerLevel)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && (mob.tickCount + mob.getId()) % 10 == 0;
    }

    @Override
    public void start() {
        tick();
    }

    @Override
    public void tick() {
        if (mob.isDesynced() || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        long now = level.getGameTime();

        // 1) Anti-Pillar Ramp (SporeArchitect, ChitinBrute, Burrower)
        if ((mob instanceof SporeArchitectEntity || mob instanceof ChitinBruteEntity || mob instanceof BurrowerEntity)
                && now >= nextRampTick
                && target.getY() - mob.getY() >= 2.2D
                && mob.distanceToSqr(target) <= 100.0D) {
            boolean placedAny = false;
            for (BlockPos stepPos : LivingBattlefieldSystem.computeAntiPillarStepPositions(
                    mob.blockPosition(), target.blockPosition())) {
                if (EphemeralChitinBlock.tryPlaceTemporary(level, stepPos)) {
                    placedAny = true;
                }
            }
            if (placedAny) {
                nextRampTick = now + 80L;
            }
        }

        // 2) Arrow Barricade (ChitinBrute, ColonyWarden)
        DamageSource lastSrc = mob.getLastDamageSource();
        if ((mob instanceof ChitinBruteEntity || mob instanceof ColonyWardenEntity)
                && now >= nextBarricadeTick
                && lastSrc != null
                && lastSrc.is(DamageTypeTags.IS_PROJECTILE)) {
            boolean placedAny = false;
            LivingEntity attacker = lastSrc.getEntity() instanceof LivingEntity sourceAttacker
                    ? sourceAttacker
                    : target;
            for (BlockPos bPos : LivingBattlefieldSystem.computeBarricadePositions(
                    mob.blockPosition(), attacker.position())) {
                if (EphemeralChitinBlock.tryPlaceTemporary(level, bPos)) {
                    placedAny = true;
                }
            }
            if (placedAny) {
                nextBarricadeTick = now + 140L;
            }
        }

        // 3) Corrosive Turret Sabotage (AcidSpitter, Burrower)
        if ((mob instanceof AcidSpitterEntity || mob instanceof BurrowerEntity) && now >= nextSabotageTick) {
            BlockPos center = mob.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-6, -2, -6), center.offset(6, 3, 6))) {
                if (level.getBlockEntity(p) instanceof PurificationTurretBlockEntity turret && !turret.isJammed()) {
                    turret.applyCorrosiveJam(LivingBattlefieldSystem.TURRET_CORROSIVE_JAM_TICKS);
                    level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                            p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5,
                            10, 0.25, 0.25, 0.25, 0.03);
                    nextSabotageTick = now + 160L;
                    break;
                }
            }
        }
    }
}
