package com.alex.mycelialmind.client.particle;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Registers the client provider for every Mycelial Mind particle type. */
@EventBusSubscriber(modid = MycelialMind.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MycelialParticleProviders {
    @SubscribeEvent
    public static void register(RegisterParticleProvidersEvent event) {
        for (MycelialParticleSpecs.Entry entry : MycelialParticleSpecs.SIMPLE) {
            event.registerSpriteSet(entry.type().get(), sprites -> provider(entry.spec(), sprites));
        }
        event.registerSpriteSet(ModParticles.HIVE_DUST.get(), sprites ->
                (ParticleProvider<ColorParticleOption>) (type, level, x, y, z, xd, yd, zd) ->
                        new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, MycelialParticleSpecs.HIVE_DUST)
                                .tinted(type.getRed(), type.getGreen(), type.getBlue()));
    }

    private static ParticleProvider<SimpleParticleType> provider(ParticleSpec spec, SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> {
            if (spec.emission() != ParticleSpec.Emission.NONE) {
                MycelialEmissions.emit(level, spec, sprites, x, y, z, xd, yd, zd, level.random);
                return null;
            }
            double density = com.alex.mycelialmind.config.MycelialClientConfig.particleDensityScale();
            if (density < 0.999D && level.random.nextDouble() > density) {
                return null;
            }
            return new MycelialParticle(level, x, y, z, xd, yd, zd, sprites, spec);
        };
    }

    private MycelialParticleProviders() {}
}
