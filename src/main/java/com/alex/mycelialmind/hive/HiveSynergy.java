package com.alex.mycelialmind.hive;

import java.util.Set;

/**
 * Elite emergent synergies formed when specific pairs of AdaptationTraits are active together,
 * with at least one evolved to Tier 2 (or during Boss Hive Overload).
 */
public enum HiveSynergy {
    PYROLYTIC_SHELL("pyrolytic_shell", AdaptationTrait.FIRE_RESISTANT, AdaptationTrait.BLAST_SHIELDED),
    VENOMOUS_COUNTER("venomous_counter", AdaptationTrait.MELEE_THORNS, AdaptationTrait.MAGIC_CARAPACE),
    KINETIC_MIRROR("kinetic_mirror", AdaptationTrait.PROJECTILE_DEFLECT, AdaptationTrait.MELEE_THORNS),
    ARCANE_ABSORPTION("arcane_absorption", AdaptationTrait.MAGIC_CARAPACE, AdaptationTrait.FIRE_RESISTANT),
    SIEGE_JUGGERNAUT("siege_juggernaut", AdaptationTrait.BLAST_SHIELDED, AdaptationTrait.PROJECTILE_DEFLECT);

    private final String id;
    private final AdaptationTrait first;
    private final AdaptationTrait second;

    HiveSynergy(String id, AdaptationTrait first, AdaptationTrait second) {
        this.id = id;
        this.first = first;
        this.second = second;
    }

    public String id() {
        return id;
    }

    public AdaptationTrait first() {
        return first;
    }

    public AdaptationTrait second() {
        return second;
    }

    public boolean isSatisfiedBy(Set<AdaptationTrait> tier1, Set<AdaptationTrait> tier2) {
        return tier1.contains(first)
                && tier1.contains(second)
                && (tier2.contains(first) || tier2.contains(second));
    }
}
