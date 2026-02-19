package org.jmouse.dom.template.build;

import org.jmouse.core.Verify;
import org.jmouse.dom.template.NodeTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for lists of blueprint nodes.
 */
public final class BlueprintListBuilder {

    private final List<NodeTemplate> nodes = new ArrayList<>();

    public BlueprintListBuilder add(NodeTemplate blueprint) {
        Verify.nonNull(blueprint, "blueprint");
        nodes.add(blueprint);
        return this;
    }

    public BlueprintListBuilder addAll(List<NodeTemplate> blueprints) {
        Verify.nonNull(blueprints, "blueprints");
        nodes.addAll(blueprints);
        return this;
    }

    public List<NodeTemplate> build() {
        return List.copyOf(nodes);
    }
}
