package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

/**
 * Tier 2 BLAST_SHIELDED behavioral mutation: coordinates nearby MycelialMobs
 * into a defensive phalanx, granting shared resistance when clustered.
 */
public class HiveAlertSupportGoal extends Goal {
    private final MycelialMob mob;
    private int cooldown = 0;

    public HiveAlertSupportGoal(MycelialMob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        if (!mob.hasTier2Trait(AdaptationTrait.BLAST_SHIELDED)) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.cooldown = 80;
        List<MycelialMob> allies = mob.level().getEntitiesOfClass(
                MycelialMob.class,
                mob.getBoundingBox().inflate(8.0D),
                other -> other != mob && other.isAlive() && !mob.isRivalStrainTarget(other)
        );
        if (!allies.isEmpty()) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 0, false, false));
            for (MycelialMob ally : allies) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 0, false, false));
                if (ally.getTarget() == null && mob.getTarget() != null) {
                    ally.setTarget(mob.getTarget());
                }
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}
