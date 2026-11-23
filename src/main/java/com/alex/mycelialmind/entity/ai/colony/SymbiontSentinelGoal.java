package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Sentinel caste combat & perimeter patrol goal. Also engages attackers directly in self-defense.
 */
public class SymbiontSentinelGoal extends Goal {
    private final ColonySymbiontEntity worker;
    @Nullable
    private LivingEntity target;
    private int attackCooldown = 0;
    private int patrolIndex = 0;
    private int scanCooldown = 0;

    public SymbiontSentinelGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(worker.level() instanceof ServerLevel) || worker.isStandby() || worker.isResting()) {
            return false;
        }
        if (worker.getCaste() == SymbiontCaste.SENTINEL) {
            return true;
        }
        LivingEntity hurtBy = worker.getLastHurtByMob();
        return hurtBy != null && hurtBy.isAlive() && isValidHostile(hurtBy);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        target = null;
        worker.setTarget(null);
        worker.setTaskState("idle");
    }

    @Override
    public void tick() {
        if (!(worker.level() instanceof ServerLevel level)) {
            return;
        }
        SovereignColonyData colony = worker.getColonyData(level);
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        if (target == null || !target.isAlive() || !isValidHostile(target)) {
            target = findBestThreat(level, colony);
            worker.setTarget(target);
        }

        if (target != null) {
            worker.setTaskState("defending");
            worker.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = worker.distanceToSqr(target);
            if (distSq > 4.2D) {
                if (worker.tickCount % 10 == 0 || worker.getNavigation().isDone()) {
                    worker.getNavigation().moveTo(target, 1.28D);
                }
            } else {
                worker.getNavigation().stop();
                if (attackCooldown <= 0) {
                    worker.swing(InteractionHand.MAIN_HAND);
                    boolean killed = false;
                    float preHp = target.getHealth();
                    if (worker.doHurtTarget(target)) {
                        killed = !target.isAlive() || target.getHealth() <= 0.0F || preHp <= 0.0F;
                        if (colony != null && (worker.getRank() == SymbiontRank.SOVEREIGN
                                || colony.hasTrait(SovereignTrait.APEX_GUARDIAN_MATRIX))) {
                            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                                    target.getX(), target.getY() + 0.8, target.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                    attackCooldown = 16;
                    if (killed) {
                        worker.recordWorkCompleted(level, ColonyOrder.OrderType.DEFEND_THREAT, 16);
                        target = null;
                    }
                }
            }
            return;
        }

        if (worker.getCaste() == SymbiontCaste.SENTINEL && colony != null) {
            worker.setTaskState("patrolling");
            if (scanCooldown > 0) {
                scanCooldown--;
                return;
            }
            BlockPos patrolPoint = pickNextPatrolPoint(colony);
            if (worker.blockPosition().distSqr(patrolPoint) > 9.0D) {
                worker.getNavigation().moveTo(patrolPoint.getX() + 0.5D, patrolPoint.getY(), patrolPoint.getZ() + 0.5D, 1.05D);
            } else {
                patrolIndex++;
                scanCooldown = 40;
            }
        }
    }

    @Nullable
    private LivingEntity findBestThreat(ServerLevel level, @Nullable SovereignColonyData colony) {
        LivingEntity hurtBy = worker.getLastHurtByMob();
        if (hurtBy != null && hurtBy.isAlive() && isValidHostile(hurtBy)) {
            return hurtBy;
        }
        BlockPos anchor = colony != null ? colony.getNexusPos() : worker.blockPosition();
        double range = colony != null ? Math.min(48.0D, colony.getEffectiveRadius()) : 24.0D;
        AABB box = new AABB(anchor).inflate(range);
        List<Monster> threats = level.getEntitiesOfClass(Monster.class, box, this::isValidHostile);
        return threats.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(worker)))
                .orElse(null);
    }

    private boolean isValidHostile(LivingEntity entity) {
        if (!entity.isAlive() || entity == worker || worker.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof MycelialMob mm && mm.isAlliedEscort()) {
            return false;
        }
        return entity instanceof Monster;
    }

    private BlockPos pickNextPatrolPoint(SovereignColonyData colony) {
        var zones = colony.getWorkZones(SovereignColonyData.WorkZoneType.PATROL);
        if (!zones.isEmpty()) {
            int idx = Math.floorMod(patrolIndex, zones.size());
            int i = 0;
            for (BlockPos p : zones) {
                if (i++ == idx) {
                    return p;
                }
            }
        }
        var outposts = colony.getLinkedOutposts();
        if (!outposts.isEmpty()) {
            int total = 1 + outposts.size();
            int idx = Math.floorMod(patrolIndex, total);
            if (idx == 0) {
                return colony.getNexusPos();
            }
            int i = 1;
            for (BlockPos p : outposts) {
                if (i++ == idx) {
                    return p;
                }
            }
        }
        return colony.getNexusPos();
    }
}
