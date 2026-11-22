package com.alex.mycelialmind.symbiosis;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Keeps player attribute modifiers in step with the equipped mutagens. */
public final class SymbiosisAttributeSync {
    private static final ResourceLocation CHITIN_THORNS_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "chitin_thorns_armor");
    private static final ResourceLocation BLAST_CARAPACE_KB_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blast_carapace_knockback");

    private SymbiosisAttributeSync() {
    }

    public static void sync(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        PlayerSymbiosisData symbiosis = player.getData(ModAttachments.SYMBIOSIS);
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (symbiosis.hasMutagen(SymbioticMutagen.CHITIN_THORNS)) {
                if (!armor.hasModifier(CHITIN_THORNS_ARMOR_ID)) {
                    armor.addTransientModifier(new AttributeModifier(
                            CHITIN_THORNS_ARMOR_ID, 2.0D, AttributeModifier.Operation.ADD_VALUE));
                }
            } else {
                armor.removeModifier(CHITIN_THORNS_ARMOR_ID);
            }
        }

        AttributeInstance knockbackResistance = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            if (symbiosis.hasMutagen(SymbioticMutagen.BLAST_CARAPACE)) {
                if (!knockbackResistance.hasModifier(BLAST_CARAPACE_KB_ID)) {
                    knockbackResistance.addTransientModifier(new AttributeModifier(
                            BLAST_CARAPACE_KB_ID, 0.5D, AttributeModifier.Operation.ADD_VALUE));
                }
            } else {
                knockbackResistance.removeModifier(BLAST_CARAPACE_KB_ID);
            }
        }
    }
}
