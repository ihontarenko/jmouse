package org.jmouse.meterializer.build;

import org.jmouse.core.Verify;
import org.jmouse.meterializer.NodeTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for lists of blueprint nodes.
 */
public final class TemplateNodeCollectionBuilder {

    private final List<NodeTemplate> nodes = new ArrayList<>();

    public TemplateNodeCollectionBuilder add(NodeTemplate template) {
        Verify.nonNull(template, "template");
        nodes.add(template);
        return this;
    }

    public TemplateNodeCollectionBuilder addAll(List<NodeTemplate> templates) {
        Verify.nonNull(templates, "templates");
        nodes.addAll(templates);
        return this;
    }

    public List<NodeTemplate> build() {
        return List.copyOf(nodes);
    }
}
