package com.alex.mycelialmind.block;

import com.alex.mycelialmind.world.SynapticNodeType;

/**
 * Incubator Synaptic Node: accumulates network biomass and dispatches tactical squads
 * toward nearby players or nodes under attack.
 */
public class IncubatorNodeBlock extends MycelialNodeBlock {
    public IncubatorNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public SynapticNodeType nodeType() {
        return SynapticNodeType.INCUBATOR;
    }

    @Override
    protected String tooltipKey() {
        return "tooltip.mycelialmind.block.incubator_node";
    }
}
