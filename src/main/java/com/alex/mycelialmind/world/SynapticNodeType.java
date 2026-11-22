package com.alex.mycelialmind.world;

/**
 * Specialized roles within the active Mycelial Synaptic Network.
 */
public enum SynapticNodeType {
    RELAY("relay"),
    INCUBATOR("incubator"),
    SENTINEL("sentinel"),
    SYMBIOTIC("symbiotic");

    private final String id;

    SynapticNodeType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SynapticNodeType fromId(String id) {
        for (SynapticNodeType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return RELAY;
    }
}
