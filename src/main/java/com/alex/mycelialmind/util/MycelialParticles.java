package com.alex.mycelialmind.util;

import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side facade for every Mycelial Mind particle emission.
 *
 * <p>All helpers clamp to {@link ParticleEmissionBudget#MAX_PER_CALL} and only
 * reference mod particle types, so no vanilla {@code ParticleTypes} leak into
 * gameplay code. Geometry-heavy bursts live client-side in
 * {@code MycelialEmissions}; the single-emitter contract (count 0, offsets carry
 * radius/height, speed 1) is preserved from earlier tuning passes.</p>
 */
public final class MycelialParticles {
    private MycelialParticles() {}

    // ------------------------------------------------------------------
    // Consumables, serums & scanner (emitter contract preserved)
    // ------------------------------------------------------------------
    public static void antidote(ServerLevel level, LivingEntity target, boolean concentrated) {
        level.sendParticles(concentrated ? ModParticles.PURGE.get() : ModParticles.RECOVERY.get(),
                target.getX(), target.getY(0.35), target.getZ(), 0,
                Math.max(0.35, target.getBbWidth() * 0.65), target.getBbHeight(), 0, 1);
    }

    public static void counterSerum(ServerLevel level, LivingEntity target, StimulusType stimulus) {
        var particle = switch (stimulus) {
            case MELEE -> ModParticles.SERUM_MELEE.get();
            case PROJECTILE -> ModParticles.SERUM_PROJECTILE.get();
            case FIRE -> ModParticles.SERUM_FIRE.get();
            case EXPLOSION -> ModParticles.SERUM_EXPLOSION.get();
            case MAGIC -> ModParticles.SERUM_MAGIC.get();
        };
        level.sendParticles(particle, target.getX(), target.getEyeY() - 0.25, target.getZ(),
                1, 0, 0, 0, 0);
    }

    public static void thermalPulse(ServerLevel level, LivingEntity target) {
        level.sendParticles(ModParticles.SCAN.get(), target.getX(), target.getEyeY() - 0.15, target.getZ(),
                1, 0, 0, 0, 0);
    }

    public static void impactBurst(ServerLevel level, Vec3 position, Vec3 normal) {
        level.sendParticles(ModParticles.IMPACT.get(), position.x, position.y, position.z,
                0, normal.x, normal.y, normal.z, 1);
    }

    public static void trail(ServerLevel level, Vec3 position) {
        level.sendParticles(ModParticles.TRAIL.get(), position.x, position.y, position.z, 1, 0, 0, 0, 0);
    }

    public static void engram(ServerLevel level, Vec3 position, Vec3 direction) {
        level.sendParticles(ModParticles.ENGRAM.get(), position.x, position.y, position.z,
                3, direction.x * 0.08, 0.02, direction.z * 0.08, 0.01);
    }

    // ------------------------------------------------------------------
    // Mob scenes
    // ------------------------------------------------------------------
    public static void promotion(ServerLevel level, LivingEntity mob, boolean elite) {
        double cy = mob.getY() + mob.getBbHeight() * 0.5;
        double width = Math.max(0.35, mob.getBbWidth());
        level.sendParticles(ModParticles.HIVE_PULSE.get(), mob.getX(), cy, mob.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(elite ? ModParticles.HIVE_EMBER.get() : ModParticles.SPORE_MOTE.get(),
                mob.getX(), cy, mob.getZ(), 20, width * 0.5, mob.getBbHeight() * 0.4, width * 0.5, 0.08);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(), mob.getX(), cy, mob.getZ(),
                10, width * 0.4, mob.getBbHeight() * 0.3, width * 0.4, 0.06);
    }

    public static void conversion(ServerLevel level, LivingEntity mob) {
        level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                mob.getX(), mob.getY(0.5), mob.getZ(), 14, 0.35, 0.45, 0.35, 0.05);
        level.sendParticles(ModParticles.SPORE_MOTE.get(),
                mob.getX(), mob.getY(0.5), mob.getZ(), 8, 0.35, 0.45, 0.35, 0.03);
    }

    public static void mobDeath(ServerLevel level, LivingEntity mob) {
        level.sendParticles(ModParticles.SPORE_CLOUD.get(), mob.getX(), mob.getY(0.5), mob.getZ(),
                1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SPORE_MOTE.get(), mob.getX(), mob.getY(0.5), mob.getZ(),
                8, 0.3, 0.35, 0.3, 0.02);
    }

    public static void traitPulse(ServerLevel level, LivingEntity mob, int tintArgb) {
        level.sendParticles(ModParticles.HIVE_PULSE.get(), mob.getX(), mob.getY(0.5), mob.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), tintArgb),
                mob.getX(), mob.getY(0.5), mob.getZ(), 8, 0.35, 0.45, 0.35, 0.02);
    }

    public static void feeding(ServerLevel level, LivingEntity mob) {
        level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                mob.getX(), mob.getY(0.6), mob.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
    }

    // ------------------------------------------------------------------
    // Boss & tide scenes
    // ------------------------------------------------------------------
    public static void bossPhase(ServerLevel level, LivingEntity boss, int phase) {
        double y = boss.getY() + boss.getBbHeight() * 0.5;
        level.sendParticles(ModParticles.HIVE_PULSE.get(), boss.getX(), y, boss.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(phase >= 2 ? ModParticles.TIDE_MOTE.get() : ModParticles.HIVE_EMBER.get(),
                boss.getX(), y, boss.getZ(), 24, 1.0, 0.9, 1.0, 0.06);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(), boss.getX(), y, boss.getZ(),
                12, 0.8, 0.6, 0.8, 0.05);
    }

    public static void tideMilestone(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.TIDE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                18, 4.5, 2.0, 4.5, 0.02);
        level.sendParticles(ModParticles.HIVE_PULSE.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                1, 0, 0, 0, 0);
    }

    public static void matriarchWail(ServerLevel level, LivingEntity matriarch) {
        level.sendParticles(ModParticles.PSYCHIC_WAVE.get(), matriarch.getX(), matriarch.getY(0.5), matriarch.getZ(),
                1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.DESYNC_FRAGMENT.get(), matriarch.getX(), matriarch.getY(0.6), matriarch.getZ(),
                14, 0.8, 0.8, 0.8, 0.04);
    }

    // ------------------------------------------------------------------
    // Homeostasis scenes
    // ------------------------------------------------------------------
    public static void homeostasis(ServerLevel level, BlockPos pos, double stress) {
        if (stress >= 70.0) {
            level.sendParticles(ModParticles.NECROTIC_MIST.get(),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.6, 0.4, 0.6, 0.01);
        } else if (stress >= 40.0) {
            level.sendParticles(ModParticles.SPORE_FALL.get(),
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.6, 0.4, 0.6, 0.01);
        }
    }

    public static void acidRainColumn(ServerLevel level, Vec3 position) {
        level.sendParticles(ModParticles.ACID_DROPLET.get(), position.x, position.y, position.z, 3, 0.2, 0.5, 0.2, 0.01);
    }

    public static void acidSplash(ServerLevel level, Vec3 position) {
        level.sendParticles(ModParticles.ACID_SPLASH.get(), position.x, position.y, position.z, 1, 0, 0, 0, 0);
    }

    public static void cyst(ServerLevel level, BlockPos pos, boolean histamine) {
        level.sendParticles(histamine ? ModParticles.HISTAMINE_PULSE.get() : ModParticles.NECROTIC_MIST.get(),
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.ACID_SPLASH.get(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
    }

    // ------------------------------------------------------------------
    // Purification scenes
    // ------------------------------------------------------------------
    public static void purifier(ServerLevel level, Vec3 position, int tier) {
        level.sendParticles(tier >= 2 ? ModParticles.THERMAL_RETICLE.get() : ModParticles.PURIFIER_CROSS.get(),
                position.x, position.y, position.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(), position.x, position.y + 0.4, position.z,
                Math.min(12, 4 + tier * 3), 0.25, 0.2, 0.25, 0.02);
    }

    public static void purifierTick(ServerLevel level, BlockPos pos, int tier) {
        level.sendParticles(ModParticles.BIO_LUME.get(),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                Math.min(8, 2 + tier), 0.3, 0.2, 0.3, 0.01);
    }

    public static void turretFire(ServerLevel level, Vec3 position) {
        level.sendParticles(ModParticles.SYNAPSE_SPARK.get(), position.x, position.y, position.z, 3, 0.1, 0.1, 0.1, 0.02);
    }

    public static void droneRepair(ServerLevel level, LivingEntity drone) {
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(), drone.getX(), drone.getY(0.6), drone.getZ(),
                6, 0.3, 0.25, 0.3, 0.01);
        level.sendParticles(ModParticles.BIO_LUME.get(), drone.getX(), drone.getY(0.6), drone.getZ(),
                4, 0.3, 0.25, 0.3, 0.01);
    }

    // ------------------------------------------------------------------
    // Flora & world scenes
    // ------------------------------------------------------------------
    public static void floraGlow(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.BIO_LUME.get(),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.25, 0.15, 0.25, 0.01);
    }

    public static void floraHarvest(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.BIO_LUME.get(),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.3, 0.25, 0.3, 0.02);
        level.sendParticles(ModParticles.SPORE_FALL.get(),
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 4, 0.3, 0.25, 0.3, 0.01);
    }

    public static void ventJet(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.SPORE_FALL.get(),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.2, 0.35, 0.2, 0.02);
        level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
    }

    public static void amberCrystalize(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.AMBER_GLINT.get(),
                pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.01);
    }

    public static void rift(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.ENGRAM.get(),
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 1, 0, 0.1, 0, 1);
        level.sendParticles(ModParticles.TIDE_MOTE.get(),
                pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.3, 0.5, 0.3, 0.02);
    }

    // ------------------------------------------------------------------
    // Generic single-type emitters (signature mirrors ServerLevel#sendParticles)
    // ------------------------------------------------------------------
    public static void sporeMote(ServerLevel level, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.SPORE_MOTE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void sporeCloud(ServerLevel level, double x, double y, double z, int count,
                                  double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.SPORE_CLOUD.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void sporeFall(ServerLevel level, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.SPORE_FALL.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void bioLume(ServerLevel level, double x, double y, double z, int count,
                               double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.BIO_LUME.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void amberGlint(ServerLevel level, double x, double y, double z, int count,
                                  double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.AMBER_GLINT.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void chitinShard(ServerLevel level, double x, double y, double z, int count,
                                   double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.CHITIN_SHARD.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void acidDroplet(ServerLevel level, double x, double y, double z, int count,
                                   double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.ACID_DROPLET.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void acidSplash(ServerLevel level, double x, double y, double z, int count,
                                  double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.ACID_SPLASH.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void necroticMist(ServerLevel level, double x, double y, double z, int count,
                                    double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.NECROTIC_MIST.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void synapseSpark(ServerLevel level, double x, double y, double z, int count,
                                    double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.SYNAPSE_SPARK.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void hivePulse(ServerLevel level, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.HIVE_PULSE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void psychicWave(ServerLevel level, double x, double y, double z, int count,
                                   double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.PSYCHIC_WAVE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void tideMote(ServerLevel level, double x, double y, double z, int count,
                                double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.TIDE_MOTE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void hiveEmber(ServerLevel level, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.HIVE_EMBER.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void purifierCross(ServerLevel level, double x, double y, double z, int count,
                                     double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.PURIFIER_CROSS.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void thermalReticle(ServerLevel level, double x, double y, double z, int count,
                                      double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.THERMAL_RETICLE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void histaminePulse(ServerLevel level, double x, double y, double z, int count,
                                      double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.HISTAMINE_PULSE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void desyncFragment(ServerLevel level, double x, double y, double z, int count,
                                      double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.DESYNC_FRAGMENT.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void hiveDust(ServerLevel level, double x, double y, double z, int count,
                                double dx, double dy, double dz, double speed, int argbColor) {
        level.sendParticles(ColorParticleOption.create(ModParticles.HIVE_DUST.get(), argbColor),
                x, y, z, ParticleEmissionBudget.clamp(count), dx, dy, dz, speed);
    }

    public static void engram(ServerLevel level, double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.ENGRAM.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void infection(ServerLevel level, double x, double y, double z, int count,
                                 double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.INFECTION.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void rot(ServerLevel level, double x, double y, double z, int count,
                           double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.ROT.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void insight(ServerLevel level, double x, double y, double z, int count,
                               double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.INSIGHT.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void vision(ServerLevel level, double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.VISION.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void recovery(ServerLevel level, double x, double y, double z, int count,
                                double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.RECOVERY.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void purge(ServerLevel level, double x, double y, double z, int count,
                             double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.PURGE.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void scan(ServerLevel level, double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.SCAN.get(), x, y, z, count, dx, dy, dz, speed);
    }

    public static void impact(ServerLevel level, double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        emit(level, ModParticles.IMPACT.get(), x, y, z, count, dx, dy, dz, speed);
    }

    private static void emit(ServerLevel level, net.minecraft.core.particles.ParticleOptions type,
                             double x, double y, double z, int count,
                             double dx, double dy, double dz, double speed) {
        level.sendParticles(type, x, y, z, ParticleEmissionBudget.clamp(count), dx, dy, dz, speed);
    }
}
