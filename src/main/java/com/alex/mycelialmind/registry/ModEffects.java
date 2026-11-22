package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Registers custom MobEffects and NeoForge EffectCures for Mycelial Mind.
 */
public final class ModEffects {
    public static final EffectCure MYCELIAL_ANTIDOTE_CURE = EffectCure.get("mycelialmind:mycelial_antidote");

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MycelialMind.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> MYCELIAL_INFECTION =
            MOB_EFFECTS.register("mycelial_infection", () -> new MycelialStatusEffect(
                    MobEffectCategory.HARMFUL, 0x7B2CBF, ModParticles.INFECTION::get, false));

    public static final DeferredHolder<MobEffect, MobEffect> SPORE_ROT =
            MOB_EFFECTS.register("spore_rot", () -> new MycelialStatusEffect(
                    MobEffectCategory.HARMFUL, 0x9B2226, ModParticles.ROT::get, true));

    public static final DeferredHolder<MobEffect, MobEffect> SYNAPTIC_INSIGHT =
            MOB_EFFECTS.register("synaptic_insight", () -> new MycelialStatusEffect(
                    MobEffectCategory.BENEFICIAL, 0x38B000, ModParticles.INSIGHT::get, false));

    public static final DeferredHolder<MobEffect, MobEffect> SYNAPTIC_VISION =
            MOB_EFFECTS.register("synaptic_vision", () -> new MycelialStatusEffect(
                    MobEffectCategory.BENEFICIAL, 0x00F5D4, ModParticles.VISION::get, false));

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }

    private static final class MycelialStatusEffect extends MobEffect {
        private final java.util.function.Supplier<ParticleOptions> particle;
        private final boolean requiresAntidoteOnly;

        private MycelialStatusEffect(
                MobEffectCategory category,
                int color,
                java.util.function.Supplier<ParticleOptions> particle,
                boolean requiresAntidoteOnly
        ) {
            super(category, color);
            this.particle = particle;
            this.requiresAntidoteOnly = requiresAntidoteOnly;
        }

        @Override
        public ParticleOptions createParticleOptions(MobEffectInstance effect) {
            return particle.get();
        }

        @Override
        public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
            if (this.getCategory() == MobEffectCategory.HARMFUL) {
                if (this.requiresAntidoteOnly) {
                    cures.clear();
                    cures.add(MYCELIAL_ANTIDOTE_CURE);
                } else {
                    super.fillEffectCures(cures, effectInstance);
                    cures.add(MYCELIAL_ANTIDOTE_CURE);
                }
                return;
            }
            super.fillEffectCures(cures, effectInstance);
        }
    }
}
