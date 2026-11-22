package com.alex.mycelialmind.symbiosis;

import com.alex.mycelialmind.config.MycelialConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Per-player Symbiosis attachment storing up to MAX_MUTAGEN_SLOTS active mutagens.
 * Intentionally cleared on player death.
 */
public class PlayerSymbiosisData implements INBTSerializable<CompoundTag> {
    private static final String TAG_MUTAGENS = "EquippedMutagens";
    private final EnumSet<SymbioticMutagen> equippedMutagens = EnumSet.noneOf(SymbioticMutagen.class);

    public boolean equipMutagen(SymbioticMutagen mutagen) {
        normalizeSlotLimit();
        if (mutagen == null) {
            return false;
        }
        if (equippedMutagens.contains(mutagen)) {
            return false;
        }
        if (equippedMutagens.size() >= MycelialConfig.MAX_MUTAGEN_SLOTS.get()) {
            return false;
        }
        return equippedMutagens.add(mutagen);
    }

    public boolean hasMutagen(SymbioticMutagen mutagen) {
        normalizeSlotLimit();
        return equippedMutagens.contains(mutagen);
    }

    public void clearMutagens() {
        equippedMutagens.clear();
    }

    public Set<SymbioticMutagen> getEquippedMutagens() {
        normalizeSlotLimit();
        return Collections.unmodifiableSet(equippedMutagens);
    }

    public int getEquippedCount() {
        normalizeSlotLimit();
        return equippedMutagens.size();
    }

    public int encodeMask() {
        normalizeSlotLimit();
        int mask = 0;
        for (SymbioticMutagen mutagen : equippedMutagens) {
            mask |= (1 << mutagen.ordinal());
        }
        return mask;
    }

    public double getThreatAmplifier() {
        normalizeSlotLimit();
        return 1.0 + equippedMutagens.size() * MycelialConfig.SYMBIOSIS_THREAT_MULTIPLIER_PER_SLOT.get();
    }

    private void normalizeSlotLimit() {
        int maxSlots = MycelialConfig.MAX_MUTAGEN_SLOTS.get();
        if (equippedMutagens.size() <= maxSlots) {
            return;
        }
        SymbioticMutagen[] mutagens = SymbioticMutagen.values();
        for (int i = mutagens.length - 1; i >= 0 && equippedMutagens.size() > maxSlots; i--) {
            equippedMutagens.remove(mutagens[i]);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        normalizeSlotLimit();
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SymbioticMutagen mutagen : equippedMutagens) {
            list.add(StringTag.valueOf(mutagen.id()));
        }
        tag.put(TAG_MUTAGENS, list);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        equippedMutagens.clear();
        ListTag list = nbt.getList(TAG_MUTAGENS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            SymbioticMutagen m = SymbioticMutagen.fromId(list.getString(i));
            if (m != null) {
                equippedMutagens.add(m);
            }
        }
        normalizeSlotLimit();
    }
}
