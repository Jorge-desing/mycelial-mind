package com.alex.mycelialmind.entity.ai.brain.btree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared base for composite Behavior Tree nodes holding an ordered list of child tasks.
 */
public abstract class HiveBtComposite<T, S extends HiveBtComposite<T, S>> extends HiveBtTask<T> {
    protected final List<HiveBtTask<T>> children = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public S addChild(HiveBtTask<T> child) {
        if (child != null) {
            this.children.add(child);
        }
        return (S) this;
    }

    public List<HiveBtTask<T>> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    protected final void resetChildren() {
        for (int i = 0, size = this.children.size(); i < size; i++) {
            this.children.get(i).reset();
        }
    }
}
