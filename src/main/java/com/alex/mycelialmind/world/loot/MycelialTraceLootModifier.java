package com.alex.mycelialmind.world.loot;

import com.alex.mycelialmind.world.MycelialLootSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Data-driven NeoForge Global Loot Modifier injecting subtle underground and deep
 * Mycelial trace drops into Mineshafts, Dungeons, and Ancient Cities.
 */
public final class MycelialTraceLootModifier extends LootModifier {
    public static final MapCodec<MycelialTraceLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst)
                    .and(Codec.STRING.fieldOf("pool_preset").forGetter(MycelialTraceLootModifier::poolPreset))
                    .and(Codec.FLOAT.fieldOf("chance").forGetter(MycelialTraceLootModifier::chance))
                    .apply(inst, MycelialTraceLootModifier::new)
    );

    private final String poolPreset;
    private final float chance;

    public MycelialTraceLootModifier(LootItemCondition[] conditionsIn, String poolPreset, float chance) {
        super(conditionsIn);
        this.poolPreset = poolPreset;
        this.chance = chance;
    }

    public String poolPreset() {
        return poolPreset;
    }

    public float chance() {
        return chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < chance) {
            ItemStack bonus = MycelialLootSystem.rollTraceDrop(poolPreset, context.getRandom());
            if (!bonus.isEmpty()) {
                generatedLoot.add(bonus);
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
