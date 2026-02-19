package org.jmouse.dom.template;

import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies ordered blueprint transformers.
 */
public final class TransformerChain {

    private static final Comparator<Entry> ORDERING = Comparator.comparingInt(Entry::order).reversed();

    private final List<Entry> entries = new ArrayList<>();

    public TransformerChain add(int order, TemplateTransformer transformer) {
        Verify.nonNull(transformer, "transformer");
        entries.add(new Entry(order, transformer));
        entries.sort(ORDERING);
        return this;
    }

    public NodeTemplate apply(NodeTemplate blueprint, RenderingExecution execution) {
        NodeTemplate current = blueprint;
        for (Entry entry : entries) {
            current = entry.transformer().transform(current, execution);
        }
        return current;
    }

    private record Entry(int order, TemplateTransformer transformer) {}
}
