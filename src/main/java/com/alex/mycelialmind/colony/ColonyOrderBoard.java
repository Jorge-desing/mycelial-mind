package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Clean-room colony order queue with deduplication, spatial-priority scoring,
 * and exclusive TTL reservation leases to prevent multi-worker target collisions.
 */
public final class ColonyOrderBoard {
    public static final int MAX_ORDERS = 96;
    public static final long DEFAULT_LEASE_TICKS = 200L; // 10s
    public static final long ORDER_STALE_TICKS = 1200L;  // 60s

    private final List<ColonyOrder> orders = new ArrayList<>();

    public synchronized List<ColonyOrder> getOrdersSnapshot() {
        return List.copyOf(orders);
    }

    public synchronized int size() {
        return orders.size();
    }

    public synchronized void clear() {
        orders.clear();
    }

    /**
     * Posts or refreshes an order for a given (OrderType, BlockPos) tuple.
     * Avoids duplicating identical active spatial tasks.
     */
    public synchronized ColonyOrder postOrRefreshOrder(
            ColonyOrder.OrderType type,
            BlockPos targetPos,
            int priority,
            long currentTick
    ) {
        BlockPos immutable = targetPos.immutable();
        for (ColonyOrder existing : orders) {
            if (existing.getType() == type && existing.getTargetPos().equals(immutable)) {
                existing.setPriority(Math.max(existing.getPriority(), priority));
                return existing;
            }
        }
        if (orders.size() >= MAX_ORDERS) {
            evictLowestUnreservedOrOldest(currentTick);
        }
        ColonyOrder created = new ColonyOrder(UUID.randomUUID(), type, immutable, priority, currentTick);
        orders.add(created);
        return created;
    }

    private void evictLowestUnreservedOrOldest(long currentTick) {
        ColonyOrder candidate = null;
        for (ColonyOrder order : orders) {
            if (!order.isReserved(currentTick)) {
                if (candidate == null || order.getPriority() < candidate.getPriority()) {
                    candidate = order;
                }
            }
        }
        if (candidate != null) {
            orders.remove(candidate);
        } else if (!orders.isEmpty()) {
            orders.remove(0);
        }
    }

    /**
     * Claims the highest-utility unreserved order matching the worker's caste.
     * If the worker already holds an active valid reservation for its caste, returns it.
     */
    @Nullable
    public synchronized ColonyOrder claimBestOrder(
            UUID workerId,
            SymbiontCaste caste,
            BlockPos workerPos,
            long currentTick,
            long leaseTicks
    ) {
        purgeExpired(currentTick);
        for (ColonyOrder existing : orders) {
            if (existing.getType().preferredCaste() == caste && existing.isReservedBy(workerId, currentTick)) {
                existing.reserve(workerId, currentTick, leaseTicks);
                return existing;
            }
        }
        ColonyOrder best = orders.stream()
                .filter(o -> o.getType().preferredCaste() == caste)
                .filter(o -> !o.isReserved(currentTick))
                .max(Comparator.comparingDouble(o -> scoreOrder(o, workerPos)))
                .orElse(null);
        if (best != null) {
            best.reserve(workerId, currentTick, leaseTicks);
        }
        return best;
    }

    private static double scoreOrder(ColonyOrder order, BlockPos workerPos) {
        double dist = Math.sqrt(order.getTargetPos().distSqr(workerPos));
        return (order.getPriority() * 4.0D) - dist;
    }

    public synchronized boolean renewReservation(UUID orderId, UUID workerId, long currentTick, long leaseTicks) {
        for (ColonyOrder order : orders) {
            if (order.getOrderId().equals(orderId)) {
                if (!order.isReserved(currentTick) || order.isReservedBy(workerId, currentTick)) {
                    order.reserve(workerId, currentTick, leaseTicks);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized boolean completeOrder(UUID orderId, UUID workerId) {
        Iterator<ColonyOrder> it = orders.iterator();
        while (it.hasNext()) {
            ColonyOrder order = it.next();
            if (order.getOrderId().equals(orderId)) {
                if (order.getReservedBy() == null || order.getReservedBy().equals(workerId)) {
                    it.remove();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized void releaseWorkerReservations(UUID workerId) {
        for (ColonyOrder order : orders) {
            if (workerId.equals(order.getReservedBy())) {
                order.clearReservation();
            }
        }
    }

    public synchronized void purgeExpired(long currentTick) {
        Iterator<ColonyOrder> it = orders.iterator();
        while (it.hasNext()) {
            ColonyOrder order = it.next();
            if (order.getReservedBy() != null && currentTick >= order.getReservationExpiresAtTick()) {
                order.clearReservation();
            }
            if (!order.isReserved(currentTick) && (currentTick - order.getCreatedTick()) > ORDER_STALE_TICKS) {
                it.remove();
            }
        }
    }

    public synchronized int countReserved() {
        return (int) orders.stream().filter(o -> o.getReservedBy() != null).count();
    }
}
