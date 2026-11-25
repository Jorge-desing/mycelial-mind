package com.alex.mycelialmind.compat.combat;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.StimulusType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure heuristic classifier for modded {@link net.minecraft.world.damagesource.DamageType} identifiers
 * (such as Ars Nouveau, Iron's Spells 'n Spellbooks, and generic magic/spell mods) that may omit
 * vanilla {@link net.minecraft.tags.DamageTypeTags}.
 */
public final class ModdedDamageClassifier {
    private static final Set<String> SPELL_MAGIC_MOD_NAMESPACES = Set.of(
            "ars_nouveau",
            "irons_spellbooks"
    );

    private static final List<String> FIRE_KEYWORDS = List.of(
            "fire", "burn", "flame", "flare", "scorch", "magma", "ignite", "pyro"
    );
    private static final List<String> EXPLOSION_KEYWORDS = List.of(
            "explosion", "blast", "detonate", "bomb", "shockwave"
    );
    private static final List<String> PROJECTILE_KEYWORDS = List.of(
            "projectile", "arrow", "bolt", "dart", "spear", "needle", "lance", "icicle", "shot"
    );
    private static final List<String> MAGIC_KEYWORDS = List.of(
            "magic", "spell", "arcane", "eldritch", "evocation", "psychic", "mana"
    );

    private ModdedDamageClassifier() {
    }

    /**
     * Maps a modded damage type ID to a {@link StimulusType}, or returns {@code null} for vanilla/own
     * namespaces or unrecognized non-magic modded types so normal fallbacks still apply.
     */
    public static @Nullable StimulusType classifyModdedDamageType(@Nullable ResourceLocation damageTypeId) {
        if (damageTypeId == null) {
            return null;
        }
        String namespace = damageTypeId.getNamespace();
        if ("minecraft".equals(namespace) || MycelialMind.MODID.equals(namespace)) {
            return null;
        }
        String path = damageTypeId.getPath().toLowerCase(Locale.ROOT);
        if (containsAny(path, FIRE_KEYWORDS)) {
            return StimulusType.FIRE;
        }
        if (containsAny(path, EXPLOSION_KEYWORDS)) {
            return StimulusType.EXPLOSION;
        }
        if (containsAny(path, PROJECTILE_KEYWORDS)) {
            return StimulusType.PROJECTILE;
        }
        if (SPELL_MAGIC_MOD_NAMESPACES.contains(namespace) || containsAny(path, MAGIC_KEYWORDS)) {
            return StimulusType.MAGIC;
        }
        return null;
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
