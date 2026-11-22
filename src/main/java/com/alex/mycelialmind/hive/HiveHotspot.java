package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Bounded spatial combat memory entry recording conflict hotspots and their dominant threat type.
 */
public record HiveHotspot(BlockPos pos, double intensity, long lastUpdatedTick, StimulusType dominantThreat) {
    public HiveHotspot(BlockPos pos, double intensity, long lastUpdatedTick) {
        this(pos, intensity, lastUpdatedTick, StimulusType.MELEE);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putDouble("intensity", intensity);
        tag.putLong("tick", lastUpdatedTick);
        tag.putString("threat", dominantThreat.id());
        return tag;
    }

    public static HiveHotspot fromTag(CompoundTag tag) {
        BlockPos p = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        double inten = Math.max(0.0, tag.getDouble("intensity"));
        long tick = tag.getLong("tick");
        StimulusType threat = StimulusType.MELEE;
        if (tag.contains("threat")) {
            String raw = tag.getString("threat");
            for (StimulusType st : StimulusType.values()) {
                if (st.id().equals(raw)) {
                    threat = st;
                    break;
                }
            }
        }
        return new HiveHotspot(p, inten, tick, threat);
    }
}
