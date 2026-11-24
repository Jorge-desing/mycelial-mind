package com.alex.mycelialmind.entity.ai.brain.btree;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Clean-room Behavior Tree node base operating on a typed blackboard {@code T}
 * without external dependencies, reflection, or per-tick allocations.
 */
public abstract class HiveBtTask<T> {
    protected HiveBtStatus status = HiveBtStatus.FRESH;
    @Nullable
    protected Predicate<T> guard;

    public HiveBtTask<T> setGuard(@Nullable Predicate<T> guard) {
        this.guard = guard;
        return this;
    }

    @Nullable
    public Predicate<T> getGuard() {
        return this.guard;
    }

    public HiveBtStatus getStatus() {
        return this.status;
    }

    public boolean checkGuard(T blackboard) {
        return this.guard == null || this.guard.test(blackboard);
    }

    protected void start(T blackboard) {
    }

    protected void end(T blackboard) {
    }

    public final HiveBtStatus step(T blackboard) {
        if (this.status != HiveBtStatus.RUNNING) {
            start(blackboard);
        }
        HiveBtStatus result = run(blackboard);
        this.status = result;
        if (result != HiveBtStatus.RUNNING) {
            end(blackboard);
        }
        return result;
    }

    public void cancel(T blackboard) {
        if (this.status == HiveBtStatus.RUNNING) {
            this.status = HiveBtStatus.CANCELLED;
            end(blackboard);
        }
    }

    public void reset() {
        this.status = HiveBtStatus.FRESH;
    }

    protected abstract HiveBtStatus run(T blackboard);
}
