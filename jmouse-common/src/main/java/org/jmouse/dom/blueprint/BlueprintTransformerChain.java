package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies ordered blueprint transformers.
 */
public final class BlueprintTransformerChain {

    private static final Comparator<Entry> ORDERING = Comparator.comparingInt(Entry::order).reversed();

    private final List<Entry> entries = new ArrayList<>();

    public BlueprintTransformerChain add(int order, BlueprintTransformer transformer) {
        Verify.nonNull(transformer, "transformer");
        entries.add(new Entry(order, transformer));
        entries.sort(ORDERING);
        return this;
    }

    public Blueprint apply(Blueprint blueprint, RenderingExecution execution) {
        Blueprint current = blueprint;
        for (Entry entry : entries) {
            current = entry.transformer().transform(current, execution);
        }
        return current;
    }

    private record Entry(int order, BlueprintTransformer transformer) {}
}
