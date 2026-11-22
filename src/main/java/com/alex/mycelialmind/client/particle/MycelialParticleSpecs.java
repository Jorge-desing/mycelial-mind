package com.alex.mycelialmind.client.particle;

import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/** Single source of truth for the client-side look of every registered particle. */
public final class MycelialParticleSpecs {
    public record Entry(DeferredHolder<ParticleType<?>, SimpleParticleType> type, ParticleSpec spec) {}

    private static final ParticleSpec.Style GLOW = ParticleSpec.Style.ADDITIVE;
    private static final ParticleSpec.Style LIT = ParticleSpec.Style.TRANSLUCENT;

    public static final List<Entry> SIMPLE = List.of(
            // Status effects
            entry(ModParticles.INFECTION, ParticleSpec.of(ParticleArchetype.DRIFT, LIT, 3).life(24, 36)),
            entry(ModParticles.ROT, ParticleSpec.of(ParticleArchetype.FALL, LIT, 3).life(26, 38)),
            entry(ModParticles.INSIGHT, ParticleSpec.of(ParticleArchetype.RISE, GLOW, 3).life(18, 26).glow()),
            entry(ModParticles.VISION, ParticleSpec.of(ParticleArchetype.ORBIT, GLOW, 3).life(16, 24).glow()),
            // Consumables, serums & scanner emitters
            entry(ModParticles.RECOVERY, ParticleSpec.of(ParticleArchetype.RISE, GLOW, 3)
                    .life(30, 38).emitter(ParticleSpec.Emission.RECOVERY_ARCS).glow()),
            entry(ModParticles.PURGE, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 3)
                    .life(28, 36).emitter(ParticleSpec.Emission.PURGE_RING).glow()),
            entry(ModParticles.SCAN, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 3)
                    .life(26, 34).emitter(ParticleSpec.Emission.SCAN_DOUBLE).glow()),
            entry(ModParticles.SERUM_MELEE, ParticleSpec.of(ParticleArchetype.BURST, GLOW, 3)
                    .life(18, 26).emitter(ParticleSpec.Emission.SERUM_PAIRS).glow()),
            entry(ModParticles.SERUM_PROJECTILE, ParticleSpec.of(ParticleArchetype.BURST, GLOW, 3)
                    .life(18, 26).emitter(ParticleSpec.Emission.SERUM_PAIRS).glow()),
            entry(ModParticles.SERUM_FIRE, ParticleSpec.of(ParticleArchetype.BURST, GLOW, 3)
                    .life(18, 26).emitter(ParticleSpec.Emission.SERUM_PAIRS).glow()),
            entry(ModParticles.SERUM_EXPLOSION, ParticleSpec.of(ParticleArchetype.BURST, GLOW, 3)
                    .life(18, 26).emitter(ParticleSpec.Emission.SERUM_PAIRS).glow()),
            entry(ModParticles.SERUM_MAGIC, ParticleSpec.of(ParticleArchetype.BURST, GLOW, 3)
                    .life(18, 26).emitter(ParticleSpec.Emission.SERUM_PAIRS).glow()),
            entry(ModParticles.ENGRAM, ParticleSpec.of(ParticleArchetype.GLYPH, GLOW, 4)
                    .life(28, 36).glow()),
            entry(ModParticles.IMPACT, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 4)
                    .life(16, 22).emitter(ParticleSpec.Emission.IMPACT_FAN).glow()),
            entry(ModParticles.TRAIL, ParticleSpec.of(ParticleArchetype.DRIFT, LIT, 3).life(12, 18).size(0.07F, 1.0F, 0.8F)),
            // Organic hive, world & combat
            entry(ModParticles.SPORE_MOTE, ParticleSpec.of(ParticleArchetype.DRIFT, LIT, 3).life(22, 34)),
            entry(ModParticles.SPORE_CLOUD, ParticleSpec.of(ParticleArchetype.MIST, LIT, 4).life(36, 48).size(0.65F, 0.9F, 1.6F)),
            entry(ModParticles.SPORE_FALL, ParticleSpec.of(ParticleArchetype.FALL, LIT, 3).life(28, 40)),
            entry(ModParticles.BIO_LUME, ParticleSpec.of(ParticleArchetype.DRIFT, GLOW, 3).life(20, 30).glow()),
            entry(ModParticles.AMBER_GLINT, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 3).life(14, 20).size(0.075F, 1.0F, 0.7F).glow()),
            entry(ModParticles.CHITIN_SHARD, ParticleSpec.of(ParticleArchetype.TUMBLE, LIT, 3).life(22, 30)),
            entry(ModParticles.ACID_DROPLET, ParticleSpec.of(ParticleArchetype.ARC, LIT, 3).life(20, 28).glow()),
            entry(ModParticles.ACID_SPLASH, ParticleSpec.of(ParticleArchetype.BURST, LIT, 4).life(16, 24).glow()),
            entry(ModParticles.NECROTIC_MIST, ParticleSpec.of(ParticleArchetype.MIST, LIT, 4).life(30, 42).size(0.50F, 0.9F, 1.5F)),
            // Neural, bosses & purification
            entry(ModParticles.SYNAPSE_SPARK, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 4).life(10, 16).glow()),
            entry(ModParticles.HIVE_PULSE, ParticleSpec.of(ParticleArchetype.RING, GLOW, 5)
                    .life(22, 30).size(0.55F, 0.5F, 2.2F).glow()),
            entry(ModParticles.PSYCHIC_WAVE, ParticleSpec.of(ParticleArchetype.RING, GLOW, 4)
                    .life(24, 32).size(0.60F, 0.45F, 2.3F).glow()),
            entry(ModParticles.TIDE_MOTE, ParticleSpec.of(ParticleArchetype.VORTEX, GLOW, 4).life(26, 38).glow()),
            entry(ModParticles.HIVE_EMBER, ParticleSpec.of(ParticleArchetype.RISE, GLOW, 3).life(18, 26).glow()),
            entry(ModParticles.PURIFIER_CROSS, ParticleSpec.of(ParticleArchetype.RING, GLOW, 4)
                    .life(18, 26).size(0.35F, 0.7F, 1.4F).glow()),
            entry(ModParticles.THERMAL_RETICLE, ParticleSpec.of(ParticleArchetype.RING, GLOW, 4)
                    .life(20, 28).size(0.90F, 0.7F, 2.4F).glow()),
            entry(ModParticles.HISTAMINE_PULSE, ParticleSpec.of(ParticleArchetype.BURST, LIT, 4).life(18, 26).glow()),
            entry(ModParticles.DESYNC_FRAGMENT, ParticleSpec.of(ParticleArchetype.SPARK, GLOW, 4).life(12, 18).glow())
    );

    /** Tinted dust: provider overrides the tint from {@code ColorParticleOption}. */
    public static final ParticleSpec HIVE_DUST = ParticleSpec.of(ParticleArchetype.DRIFT, GLOW, 3)
            .life(20, 30).size(0.075F, 1.0F, 0.8F).glow();

    private static Entry entry(DeferredHolder<ParticleType<?>, SimpleParticleType> type, ParticleSpec spec) {
        return new Entry(type, spec);
    }

    private MycelialParticleSpecs() {}
}
