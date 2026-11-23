package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Discrete prioritized colony task entry with exclusive TTL-bounded worker reservation.
 */
public final class ColonyOrder {
    public enum OrderType {
        HARVEST(SymbiontCaste.HARVESTER),
        EXCAVATE(SymbiontCaste.EXCAVATOR),
        COURIER_PICKUP(SymbiontCaste.COURIER),
        COURIER_DEPOSIT(SymbiontCaste.COURIER),
        REPAIR_STRUCTURE(SymbiontCaste.ARCHITECT),
        DEFEND_THREAT(SymbiontCaste.SENTINEL);

        private final SymbiontCaste preferredCaste;

        OrderType(SymbiontCaste preferredCaste) {
            this.preferredCaste = preferredCaste;
        }

        public SymbiontCaste preferredCaste() {
            return preferredCaste;
        }
    }

    private final UUID orderId;
    private final OrderType type;
    private final BlockPos targetPos;
    private int priority;
    private final long createdTick;
    @Nullable
    private UUID reservedBy;
    private long reservationExpiresAtTick;

    public ColonyOrder(UUID orderId, OrderType type, BlockPos targetPos, int priority, long createdTick) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.type = Objects.requireNonNull(type, "type");
        this.targetPos = Objects.requireNonNull(targetPos, "targetPos").immutable();
        this.priority = clampPriority(priority);
        this.createdTick = createdTick;
    }

    private static int clampPriority(int priority) {
        return Math.max(1, Math.min(100, priority));
    }

    public UUID getOrderId() {
        return orderId;
    }

    public OrderType getType() {
        return type;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = clampPriority(priority);
    }

    public long getCreatedTick() {
        return createdTick;
    }

    @Nullable
    public UUID getReservedBy() {
        return reservedBy;
    }

    public long getReservationExpiresAtTick() {
        return reservationExpiresAtTick;
    }

    public boolean isReserved(long currentTick) {
        return reservedBy != null && currentTick < reservationExpiresAtTick;
    }

    public boolean isReservedBy(UUID workerId, long currentTick) {
        return reservedBy != null && reservedBy.equals(workerId) && currentTick < reservationExpiresAtTick;
    }

    public void reserve(UUID workerId, long currentTick, long leaseTicks) {
        this.reservedBy = Objects.requireNonNull(workerId, "workerId");
        this.reservationExpiresAtTick = currentTick + Math.max(20L, leaseTicks);
    }

    public void clearReservation() {
        this.reservedBy = null;
        this.reservationExpiresAtTick = 0L;
    }
}
