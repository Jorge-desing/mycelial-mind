package com.alex.mycelialmind.client.particle;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.util.ParticleEmissionBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.lang.ref.WeakReference;

/** Registers the client provider for every Mycelial Mind particle type. */
@EventBusSubscriber(modid = MycelialMind.MODID, value = Dist.CLIENT)
public final class MycelialParticleProviders {
    private static SpriteSet recoverySprites;
    private static WeakReference<ClientLevel> sampledLevel = new WeakReference<>(null);
    private static long sampledTick = Long.MIN_VALUE;
    private static int sampledParticleCount;

    static SpriteSet recoverySprites() {
        return recoverySprites;
    }

    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        for (MycelialParticleSpecs.Entry entry : MycelialParticleSpecs.SIMPLE) {
            event.registerSpriteSet(entry.type().get(), sprites -> {
                if (entry.type() == ModParticles.RECOVERY) {
                    recoverySprites = sprites;
                }
                return provider(entry.spec(), sprites, isAmbient(entry));
            });
        }
        event.registerSpriteSet(ModParticles.HIVE_DUST.get(), sprites ->
                (ParticleProvider<ColorParticleOption>) (type, level, x, y, z, xd, yd, zd) -> {
                    double density = MycelialClientConfig.particleDensityScale();
                    if (density < 1.0D && level.random.nextDouble() >= density) {
                        return null;
                    }
                    if (density > 1.0D && level.random.nextDouble() < density - 1.0D) {
                        Minecraft.getInstance().particleEngine.add(
                                new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, MycelialParticleSpecs.HIVE_DUST)
                                        .tinted(type.getRed(), type.getGreen(), type.getBlue()));
                    }
                    return new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, MycelialParticleSpecs.HIVE_DUST)
                            .tinted(type.getRed(), type.getGreen(), type.getBlue());
                });
    }

    private static int sceneParticleCount(ClientLevel level) {
        long tick = level.getGameTime();
        if (sampledLevel.get() != level) {
            sampledLevel = new WeakReference<>(level);
            sampledTick = Long.MIN_VALUE;
        }
        if (sampledTick != tick) {
            sampledTick = tick;
            sampledParticleCount = Integer.parseInt(Minecraft.getInstance().particleEngine.countParticles());
        }
        return sampledParticleCount;
    }

    private static boolean isAmbient(MycelialParticleSpecs.Entry entry) {
        return entry.type() == ModParticles.SPORE_MOTE || entry.type() == ModParticles.SPORE_CLOUD
                || entry.type() == ModParticles.SPORE_FALL || entry.type() == ModParticles.BIO_LUME;
    }

    private static ParticleProvider<SimpleParticleType> provider(ParticleSpec spec, SpriteSet sprites, boolean ambient) {
        return (type, level, x, y, z, xd, yd, zd) -> {
            if (spec.emission() != ParticleSpec.Emission.NONE) {
                MycelialEmissions.emit(level, spec, sprites, x, y, z, xd, yd, zd, level.random);
                return null;
            }
            double density = MycelialClientConfig.particleDensityScale();
            if (ambient && sceneParticleCount(level) >= 48) {
                var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                double distance = camera.distanceToSqr(x, y, z);
                density = ParticleEmissionBudget.ambientDensity(density, sampledParticleCount, distance);
            }
            if (density < 0.999D && level.random.nextDouble() > density) {
                return null;
            }
            if (density > 1.0D && level.random.nextDouble() < density - 1.0D) {
                Minecraft.getInstance().particleEngine.add(
                        new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, spec));
            }
            return new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, spec);
        };
    }

    private MycelialParticleProviders() {}
}
