package org.jmouse.template.build;

import org.jmouse.core.Verify;
import org.jmouse.template.NodeTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for lists of blueprint nodes.
 */
public final class TemplateNodeCollectionBuilder {

    private final List<NodeTemplate> nodes = new ArrayList<>();

    public TemplateNodeCollectionBuilder add(NodeTemplate blueprint) {
        Verify.nonNull(blueprint, "blueprint");
        nodes.add(blueprint);
        return this;
    }

    public TemplateNodeCollectionBuilder addAll(List<NodeTemplate> blueprints) {
        Verify.nonNull(blueprints, "blueprints");
        nodes.addAll(blueprints);
        return this;
    }

    public List<NodeTemplate> build() {
        return List.copyOf(nodes);
    }
}
