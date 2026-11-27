package com.alex.mycelialmind.block;

import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;

/**
 * Sentinel Synaptic Node: projects defensive auras to nearby MycelialMobs
 * and acts as an invulnerability anchor during Hive Core Phase 2.
 */
public class SentinelNodeBlock extends MycelialNodeBlock {
    public SentinelNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public SynapticNodeType nodeType() {
        return SynapticNodeType.SENTINEL;
    }

    @Override
    protected String tooltipKey() {
        return "tooltip.mycelialmind.block.sentinel_node";
    }

    @Override
    protected ChatFormatting tooltipColor() {
        return ChatFormatting.AQUA;
    }
}
