package com.alex.mycelialmind.entity.ai.brain.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sequential composite: executes children in order until all succeed or one fails.
 */
public final class HiveBtSequence<T> extends HiveBtTask<T> {
    private final List<HiveBtTask<T>> children = new ArrayList<>();
    private int currentChildIndex = 0;

    public HiveBtSequence<T> addChild(HiveBtTask<T> child) {
        if (child != null) {
            this.children.add(child);
        }
        return this;
    }

    public List<HiveBtTask<T>> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    protected void start(T blackboard) {
        this.currentChildIndex = 0;
    }

    @Override
    protected HiveBtStatus run(T blackboard) {
        for (int size = this.children.size(); this.currentChildIndex < size; this.currentChildIndex++) {
            HiveBtTask<T> child = this.children.get(this.currentChildIndex);
            if (!child.checkGuard(blackboard)) {
                this.currentChildIndex = 0;
                return HiveBtStatus.FAILED;
            }
            HiveBtStatus result = child.step(blackboard);
            if (result == HiveBtStatus.RUNNING) {
                return HiveBtStatus.RUNNING;
            }
            if (result != HiveBtStatus.SUCCEEDED) {
                this.currentChildIndex = 0;
                return result;
            }
        }
        this.currentChildIndex = 0;
        return HiveBtStatus.SUCCEEDED;
    }

    @Override
    public void cancel(T blackboard) {
        if (this.currentChildIndex >= 0 && this.currentChildIndex < this.children.size()) {
            this.children.get(this.currentChildIndex).cancel(blackboard);
        }
        this.currentChildIndex = 0;
        super.cancel(blackboard);
    }

    @Override
    public void reset() {
        this.currentChildIndex = 0;
        for (int i = 0, size = this.children.size(); i < size; i++) {
            this.children.get(i).reset();
        }
        super.reset();
    }
}
