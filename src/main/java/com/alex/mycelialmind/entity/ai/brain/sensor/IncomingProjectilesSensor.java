package com.alex.mycelialmind.entity.ai.brain.sensor;

import com.alex.mycelialmind.registry.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Scans for nearby airborne {@link Projectile} instances moving toward the entity and
 * stores the closest threat's block position in {@link ModMemoryTypes#INCOMING_PROJECTILE_POS}.
 */
public class IncomingProjectilesSensor<E extends LivingEntity> extends HiveSensor<E> {
    public static final double DEFAULT_MIN_HEADING_DOT = 0.35D;

    protected double radius = 12.0D;
    protected BiPredicate<Projectile, E> projectilePredicate = (projectile, entity) -> true;

    public IncomingProjectilesSensor() {
        setScanRate(entity -> 5);
    }

    public IncomingProjectilesSensor<E> setRadius(double radius) {
        this.radius = Math.max(1.0D, radius);
        return this;
    }

    public IncomingProjectilesSensor<E> setPredicate(BiPredicate<Projectile, E> predicate) {
        this.projectilePredicate = predicate != null ? predicate : (projectile, entity) -> true;
        return this;
    }

    public double getRadius() {
        return this.radius;
    }

    public static boolean isProjectileHeadingToward(Vec3 projPos, Vec3 projVel, Vec3 entityPos, double minDot) {
        if (projPos == null || projVel == null || entityPos == null) {
            return false;
        }
        double velSqr = projVel.lengthSqr();
        if (velSqr <= 0.04D) {
            return false;
        }
        Vec3 toEntity = entityPos.subtract(projPos);
        double distSqr = toEntity.lengthSqr();
        if (distSqr <= 1.0E-6D) {
            return true;
        }
        double dot = projVel.scale(1.0D / Math.sqrt(velSqr))
                .dot(toEntity.scale(1.0D / Math.sqrt(distSqr)));
        return dot >= minDot;
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return List.of(ModMemoryTypes.INCOMING_PROJECTILE_POS.get());
    }

    @Override
    protected void scan(ServerLevel level, E entity) {
        Vec3 targetCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Projectile nearest = level.getEntitiesOfClass(
                        Projectile.class,
                        entity.getBoundingBox().inflate(this.radius),
                        projectile -> projectile.isAlive()
                                && !projectile.onGround()
                                && projectile.getOwner() != entity
                                && this.projectilePredicate.test(projectile, entity)
                                && isProjectileHeadingToward(
                                        projectile.position(),
                                        projectile.getDeltaMovement(),
                                        targetCenter,
                                        DEFAULT_MIN_HEADING_DOT))
                .stream()
                .min(Comparator.comparingDouble(entity::distanceToSqr))
                .orElse(null);

        Brain<?> brain = entity.getBrain();
        MemoryModuleType<BlockPos> memory = ModMemoryTypes.INCOMING_PROJECTILE_POS.get();
        if (nearest != null) {
            brain.setMemory(memory, nearest.blockPosition());
        } else {
            brain.eraseMemory(memory);
        }
    }
}
