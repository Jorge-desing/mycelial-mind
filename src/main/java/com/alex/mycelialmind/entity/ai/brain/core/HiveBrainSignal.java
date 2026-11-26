package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Immutable reactive signal dispatched immediately or queued onto a {@link HiveBrain}
 * to wake LOD-throttled entities and deliver tactical interrupts.
 */
public record HiveBrainSignal(
        SignalType type,
        @Nullable BlockPos sourcePos,
        @Nullable LivingEntity sourceEntity,
        float intensity,
        String tag,
        boolean urgent,
        long emittedGameTime
) {
    public enum SignalType {
        DAMAGE_TAKEN,
        ALLY_ALERT,
        ACOUSTIC_PULSE,
        DESYNC_TRIGGERED,
        LEADER_ORDER,
        PROJECTILE_INCOMING,
        PHASE_SHIFT,
        CUSTOM
    }

    public HiveBrainSignal {
        Objects.requireNonNull(type, "type");
        sourcePos = sourcePos != null ? sourcePos.immutable() : null;
        intensity = Float.isFinite(intensity) ? Math.max(0.0F, intensity) : 0.0F;
        tag = tag != null ? tag : "";
    }

    public static HiveBrainSignal damageTaken(
            @Nullable LivingEntity attacker, float amount, long gameTime) {
        return new HiveBrainSignal(
                SignalType.DAMAGE_TAKEN,
                attacker != null ? attacker.blockPosition() : null,
                attacker,
                amount,
                "damage",
                true,
                gameTime
        );
    }

    public static HiveBrainSignal allyAlert(
            BlockPos targetPos, @Nullable LivingEntity target, long gameTime) {
        return new HiveBrainSignal(
                SignalType.ALLY_ALERT,
                targetPos,
                target,
                1.0F,
                "ally_alert",
                true,
                gameTime
        );
    }

    public static HiveBrainSignal acousticPulse(
            BlockPos origin, @Nullable LivingEntity source, float intensity, long gameTime) {
        return new HiveBrainSignal(
                SignalType.ACOUSTIC_PULSE,
                origin,
                source,
                intensity,
                "acoustic",
                false,
                gameTime
        );
    }

    public static HiveBrainSignal desyncTriggered(int durationTicks, long gameTime) {
        return new HiveBrainSignal(
                SignalType.DESYNC_TRIGGERED,
                null,
                null,
                Math.max(0, durationTicks),
                "desync",
                true,
                gameTime
        );
    }

    public static HiveBrainSignal leaderOrder(
            String orderName, @Nullable BlockPos pos, long gameTime) {
        return new HiveBrainSignal(
                SignalType.LEADER_ORDER,
                pos,
                null,
                1.0F,
                orderName,
                false,
                gameTime
        );
    }

    public static HiveBrainSignal projectileIncoming(BlockPos pos, long gameTime) {
        return new HiveBrainSignal(
                SignalType.PROJECTILE_INCOMING,
                pos,
                null,
                1.0F,
                "projectile",
                true,
                gameTime
        );
    }

    public static HiveBrainSignal phaseShift(String phaseTag, long gameTime) {
        return new HiveBrainSignal(
                SignalType.PHASE_SHIFT,
                null,
                null,
                1.0F,
                phaseTag,
                true,
                gameTime
        );
    }

    public static HiveBrainSignal custom(
            String tag,
            @Nullable BlockPos pos,
            @Nullable LivingEntity entity,
            float intensity,
            boolean urgent,
            long gameTime) {
        return new HiveBrainSignal(
                SignalType.CUSTOM,
                pos,
                entity,
                intensity,
                tag,
                urgent,
                gameTime
        );
    }
}
