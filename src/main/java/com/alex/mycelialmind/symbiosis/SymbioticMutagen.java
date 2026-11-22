package com.alex.mycelialmind.symbiosis;

import com.alex.mycelialmind.hive.AdaptationTrait;

import javax.annotation.Nullable;

/**
 * Symbiotic mutagens synthesized from Hive genetic samples that players can implant
 * at the cost of increased Hive Threat attention and infection risk.
 */
public enum SymbioticMutagen {
    CHITIN_THORNS("chitin_thorns", AdaptationTrait.MELEE_THORNS),
    KINETIC_STRAFE("kinetic_strafe", AdaptationTrait.PROJECTILE_DEFLECT),
    PYRO_GLAND("pyro_gland", AdaptationTrait.FIRE_RESISTANT),
    BLAST_CARAPACE("blast_carapace", AdaptationTrait.BLAST_SHIELDED),
    ARCANE_SYNAPSE("arcane_synapse", AdaptationTrait.MAGIC_CARAPACE);

    private final String id;
    private final AdaptationTrait sourceTrait;

    SymbioticMutagen(String id, AdaptationTrait sourceTrait) {
        this.id = id;
        this.sourceTrait = sourceTrait;
    }

    public String id() {
        return id;
    }

    public AdaptationTrait sourceTrait() {
        return sourceTrait;
    }

    @Nullable
    public static SymbioticMutagen fromTrait(AdaptationTrait trait) {
        for (SymbioticMutagen mutagen : values()) {
            if (mutagen.sourceTrait == trait) {
                return mutagen;
            }
        }
        return null;
    }

    @Nullable
    public static SymbioticMutagen fromId(String id) {
        for (SymbioticMutagen mutagen : values()) {
            if (mutagen.id.equals(id)) {
                return mutagen;
            }
        }
        return null;
    }
}
