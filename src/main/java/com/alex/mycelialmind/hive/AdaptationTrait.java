package com.alex.mycelialmind.hive;

import java.util.EnumSet;

/**
 * Adaptive mutations that the Hive Mind can express across its organisms.
 */
public enum AdaptationTrait {
    MELEE_THORNS("melee_thorns", "flanking_swarm", StimulusType.MELEE),
    PROJECTILE_DEFLECT("projectile_deflect", "evasive_strafe", StimulusType.PROJECTILE),
    FIRE_RESISTANT("fire_resistant", "pyro_surge", StimulusType.FIRE),
    BLAST_SHIELDED("blast_shielded", "phalanx_breach", StimulusType.EXPLOSION),
    MAGIC_CARAPACE("magic_carapace", "psychic_disrupt", StimulusType.MAGIC);

    private final String id;
    private final String tier2BehaviorId;
    private final StimulusType trigger;

    AdaptationTrait(String id, String tier2BehaviorId, StimulusType trigger) {
        this.id = id;
        this.tier2BehaviorId = tier2BehaviorId;
        this.trigger = trigger;
    }

    public String id() {
        return id;
    }

    public String tier2BehaviorId() {
        return tier2BehaviorId;
    }

    public StimulusType trigger() {
        return trigger;
    }

    public static EnumSet<AdaptationTrait> decodeMask(int mask) {
        EnumSet<AdaptationTrait> traits = EnumSet.noneOf(AdaptationTrait.class);
        for (AdaptationTrait trait : values()) {
            if ((mask & (1 << trait.ordinal())) != 0) {
                traits.add(trait);
            }
        }
        return traits;
    }
}
