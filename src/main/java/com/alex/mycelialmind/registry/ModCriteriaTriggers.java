package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Registers custom NeoForge advancement {@link CriterionTrigger} implementations replacing
 * {@code ImpossibleTrigger} for data-driven progression tracking.
 */
public final class ModCriteriaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, HiveMilestoneTrigger> HIVE_MILESTONE =
            TRIGGERS.register("hive_milestone", HiveMilestoneTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, SynapticSynthesisTrigger> SYNAPTIC_SYNTHESIS =
            TRIGGERS.register("synaptic_synthesis", SynapticSynthesisTrigger::new);

    private ModCriteriaTriggers() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }

    public static final class HiveMilestoneTrigger extends SimpleCriterionTrigger<HiveMilestoneTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(@Nullable ServerPlayer player, String milestoneId) {
            if (player != null && milestoneId != null) {
                this.trigger(player, instance -> instance.matches(milestoneId));
            }
        }

        public record TriggerInstance(
                Optional<ContextAwarePredicate> player,
                String milestone
        ) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                            Codec.STRING.fieldOf("milestone").forGetter(TriggerInstance::milestone)
                    ).apply(instance, TriggerInstance::new)
            );

            public static Criterion<TriggerInstance> of(String milestoneId) {
                return HIVE_MILESTONE.get().createCriterion(new TriggerInstance(Optional.empty(), milestoneId));
            }

            public boolean matches(String milestoneId) {
                return this.milestone.equals(milestoneId);
            }
        }
    }

    public static final class SynapticSynthesisTrigger
            extends SimpleCriterionTrigger<SynapticSynthesisTrigger.TriggerInstance> {
        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(@Nullable ServerPlayer player, ItemStack synthesized, boolean isSecret) {
            if (player != null) {
                this.trigger(player, instance -> instance.matches(synthesized, isSecret));
            }
        }

        public record TriggerInstance(
                Optional<ContextAwarePredicate> player,
                Optional<ItemPredicate> result,
                Optional<Boolean> secretOnly
        ) implements SimpleCriterionTrigger.SimpleInstance {
            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                            ItemPredicate.CODEC.optionalFieldOf("result").forGetter(TriggerInstance::result),
                            Codec.BOOL.optionalFieldOf("secret_only").forGetter(TriggerInstance::secretOnly)
                    ).apply(instance, TriggerInstance::new)
            );

            public static Criterion<TriggerInstance> any() {
                return SYNAPTIC_SYNTHESIS.get().createCriterion(
                        new TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
            }

            public boolean matches(ItemStack synthesized, boolean isSecret) {
                if (secretOnly.isPresent() && secretOnly.get() != isSecret) {
                    return false;
                }
                if (result.isPresent() && (synthesized.isEmpty() || !result.get().test(synthesized))) {
                    return false;
                }
                return !synthesized.isEmpty();
            }
        }
    }
}
