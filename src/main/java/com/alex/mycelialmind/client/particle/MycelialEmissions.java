package com.alex.mycelialmind.client.particle;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.util.ParticleEmissionBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * Client-side burst geometry for the particles that must not flood the network.
 *
 * <p>The server sends a single emitter particle whose offset vector encodes the
 * target radius / height (the contract used by the antidotes and the scanner);
 * the emitter expands into its tuned fragment count locally.</p>
 */
public final class MycelialEmissions {
    public static void emit(ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                            double x, double y, double z,
                            double radiusX, double radiusY, double radiusZ,
                            RandomSource random) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        switch (spec.emission()) {
            case RECOVERY_ARCS -> recoveryArcs(engine, level, spec, sprites, x, y, z, radiusX, radiusY);
            case PURGE_RING -> purgeRing(engine, level, spec, sprites, x, y, z);
            case SCAN_DOUBLE -> scanDouble(engine, level, spec, sprites, x, y, z);
            case SERUM_PAIRS -> serumPairs(engine, level, spec, sprites, x, y, z);
            case ENGRAM_TRIO -> engramTrio(engine, level, spec, sprites, x, y, z);
            case IMPACT_FAN -> impactFan(engine, level, spec, sprites, x, y, z, radiusX, radiusY, radiusZ);
            case NONE -> { }
        }
    }

    private static void add(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                            double x, double y, double z, double xd, double yd, double zd) {
        engine.add(new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, spec));
    }

    private static void recoveryArcs(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                     double x, double y, double z, double radiusX, double radiusY) {
        double radius = Math.max(0.35D, Math.abs(radiusX));
        double height = Math.max(0.5D, Math.abs(radiusY));
        int count = ParticleEmissionBudget.scaled(20, MycelialClientConfig.particleDensityScale());
        for (int side = 0; side < 2; side++) {
            double angle = side == 0 ? 0.0D : Math.PI;
            int sideCount = count / 2 + (side < count % 2 ? 1 : 0);
            for (int i = 0; i < sideCount; i++) {
                double arc = sideCount == 1 ? 0.5D : (double) i / (sideCount - 1);
                double ring = radius * (Math.sin(Math.PI * arc) * 0.75D + 0.35D);
                double px = x + Math.cos(angle) * ring;
                double pz = z + Math.sin(angle) * ring;
                double py = y + arc * height * 0.7D;
                double inward = -Math.cos(angle) * 0.010D;
                double inz = -Math.sin(angle) * 0.010D;
                add(engine, level, spec, sprites, px, py, pz,
                        inward, 0.020D + Math.sin(Math.PI * arc) * 0.030D, inz);
            }
        }
    }

    private static void purgeRing(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                  double x, double y, double z) {
        int count = ParticleEmissionBudget.scaled(24, MycelialClientConfig.particleDensityScale());
        int cyanCount = (count * 2 + 1) / 3;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * i / count;
            double ring = 0.25D + (i % 2) * 0.20D;
            boolean healing = i >= cyanCount && MycelialParticleProviders.recoverySprites() != null;
            SpriteSet particleSprites = healing ? MycelialParticleProviders.recoverySprites() : sprites;
            ParticleSpec particleSpec = healing ? MycelialParticleSpecs.RECOVERY : spec;
            MycelialParticle particle = new MycelialParticle(level,
                    x + Math.cos(angle) * ring, y + 0.05D, z + Math.sin(angle) * ring,
                    Math.cos(angle) * 0.024D, 0.014D, Math.sin(angle) * 0.024D,
                    particleSprites, particleSpec);
            if (i >= cyanCount) {
                particle.tinted(0.55F, 1.0F, 0.20F);
            }
            engine.add(particle);
        }
    }

    private static void scanDouble(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                   double x, double y, double z) {
        int count = ParticleEmissionBudget.scaled(24, MycelialClientConfig.particleDensityScale());
        for (int front = 0; front < 2; front++) {
            double ring = front == 0 ? 0.25D : 0.60D;
            double speed = front == 0 ? 0.075D : 0.050D;
            int delay = front == 0 ? 0 : 4;
            int frontCount = count / 2 + (front < count % 2 ? 1 : 0);
            for (int i = 0; i < frontCount; i++) {
                double angle = (Math.PI * 2.0D) * i / frontCount;
                MycelialParticle particle = new MycelialParticle(level,
                        x + Math.cos(angle) * ring, y, z + Math.sin(angle) * ring,
                        Math.cos(angle) * speed, 0.0D, Math.sin(angle) * speed,
                        sprites, spec);
                if (delay > 0) {
                    particle.delayed(delay);
                }
                engine.add(particle);
            }
        }
    }

    private static void serumPairs(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                   double x, double y, double z) {
        int count = ParticleEmissionBudget.scaled(10, MycelialClientConfig.particleDensityScale());
        int pairCount = (count + 1) / 2;
        for (int pair = 0; pair < pairCount; pair++) {
            double angle = (Math.PI * 2.0D) * pair / pairCount;
            for (int side = 0; side < 2 && pair * 2 + side < count; side++) {
                double ring = 0.50D + side * 0.12D;
                add(engine, level, spec, sprites,
                        x + Math.cos(angle) * ring, y, z + Math.sin(angle) * ring,
                        -Math.cos(angle) * 0.020D, 0.020D, -Math.sin(angle) * 0.020D);
            }
        }
    }

    private static void engramTrio(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                   double x, double y, double z) {
        int count = ParticleEmissionBudget.scaled(3, MycelialClientConfig.particleDensityScale());
        for (int i = 0; i < count; i++) {
            double offset = count == 1 ? 0.0D : 2.0D * i / (count - 1) - 1.0D;
            add(engine, level, spec, sprites, x + offset * 0.12D, y, z + Math.abs(offset) * 0.05D,
                    offset * 0.004D, 0.012D, 0.0D);
        }
    }

    private static void impactFan(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                  double x, double y, double z, double nx, double ny, double nz) {
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 0.001D) {
            nx = 0.0D;
            ny = 0.2D;
            nz = 1.0D;
            length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        }
        nx /= length;
        ny /= length;
        nz /= length;
        int count = ParticleEmissionBudget.scaled(12, MycelialClientConfig.particleDensityScale());
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * i / count;
            double spread = 0.016D + 0.010D * (i % 3) / 2.0D;
            add(engine, level, spec, sprites, x, y, z,
                    nx * 0.030D + Math.cos(angle) * spread,
                    ny * 0.030D + 0.012D + Math.sin(angle) * spread * 0.5D,
                    nz * 0.030D + Math.sin(angle) * spread);
        }
    }

    private MycelialEmissions() {}
}
