package org.jmouse.dom.blueprint.dsl;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.Blueprint;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for lists of blueprint nodes.
 */
public final class BlueprintListBuilder {

    private final List<Blueprint> nodes = new ArrayList<>();

    public BlueprintListBuilder add(Blueprint blueprint) {
        Verify.nonNull(blueprint, "blueprint");
        nodes.add(blueprint);
        return this;
    }

    public BlueprintListBuilder addAll(List<Blueprint> blueprints) {
        Verify.nonNull(blueprints, "blueprints");
        nodes.addAll(blueprints);
        return this;
    }

    public List<Blueprint> build() {
        return List.copyOf(nodes);
    }
}
