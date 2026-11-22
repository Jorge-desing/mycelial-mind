package com.alex.mycelialmind.client.particle;

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
        for (int side = 0; side < 2; side++) {
            double angle = side == 0 ? 0.0D : Math.PI;
            for (int i = 0; i < 10; i++) {
                double arc = i / 9.0D;
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
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0D) * i / 24.0D;
            double ring = 0.25D + (i % 2) * 0.20D;
            MycelialParticle particle = new MycelialParticle(level,
                    x + Math.cos(angle) * ring, y + 0.05D, z + Math.sin(angle) * ring,
                    Math.cos(angle) * 0.024D, 0.014D, Math.sin(angle) * 0.024D,
                    sprites, spec);
            if (i >= 16) {
                particle.tinted(0.55F, 1.0F, 0.20F);
            }
            engine.add(particle);
        }
    }

    private static void scanDouble(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                   double x, double y, double z) {
        for (int front = 0; front < 2; front++) {
            double ring = front == 0 ? 0.25D : 0.60D;
            double speed = front == 0 ? 0.075D : 0.050D;
            int delay = front == 0 ? 0 : 4;
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2.0D) * i / 12.0D;
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
        for (int pair = 0; pair < 5; pair++) {
            double angle = (Math.PI * 2.0D) * pair / 5.0D;
            for (int side = 0; side < 2; side++) {
                double ring = 0.50D + side * 0.12D;
                add(engine, level, spec, sprites,
                        x + Math.cos(angle) * ring, y, z + Math.sin(angle) * ring,
                        -Math.cos(angle) * 0.020D, 0.020D, -Math.sin(angle) * 0.020D);
            }
        }
    }

    private static void engramTrio(ParticleEngine engine, ClientLevel level, ParticleSpec spec, SpriteSet sprites,
                                   double x, double y, double z) {
        for (int i = -1; i <= 1; i++) {
            add(engine, level, spec, sprites, x + i * 0.12D, y, z + Math.abs(i) * 0.05D,
                    i * 0.004D, 0.012D, 0.0D);
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
        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0D) * i / 12.0D;
            double spread = 0.016D + 0.010D * (i % 3) / 2.0D;
            add(engine, level, spec, sprites, x, y, z,
                    nx * 0.030D + Math.cos(angle) * spread,
                    ny * 0.030D + 0.012D + Math.sin(angle) * spread * 0.5D,
                    nz * 0.030D + Math.sin(angle) * spread);
        }
    }

    private MycelialEmissions() {}
}
