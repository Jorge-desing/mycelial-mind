package com.alex.mycelialmind.hive;

/**
 * Categories of player tactics observed by the Mycelial Hive Mind.
 */
public enum StimulusType {
    MELEE("melee"),
    PROJECTILE("projectile"),
    FIRE("fire"),
    EXPLOSION("explosion"),
    MAGIC("magic");

    private final String id;

    StimulusType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
