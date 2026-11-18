package org.jmouse.el.node;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Ordered structural roles. The order is the order observed by the common visitor. */
public record NodeSchema<N extends Node>(NodeKind<N> kind, List<ChildRole<?>> roles) {

    public NodeSchema {
        Objects.requireNonNull(kind, "kind");
        roles = List.copyOf(roles);
        var names = new HashSet<String>();
        for (ChildRole<?> role : roles) {
            if (!names.add(role.name())) {
                throw new IllegalArgumentException("Duplicate role " + role.name() + " in " + kind.name());
            }
        }
    }

    public static <N extends Node> NodeSchema<N> of(NodeKind<N> kind, ChildRole<?>... roles) {
        return new NodeSchema<>(kind, List.of(roles));
    }
}
