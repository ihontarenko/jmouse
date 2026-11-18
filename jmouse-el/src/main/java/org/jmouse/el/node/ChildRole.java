package org.jmouse.el.node;

import java.util.Objects;

/** A typed structural position, interpreted within its owner's {@link NodeSchema}. */
public record ChildRole<N extends Node>(String name, Class<N> type, ChildCardinality cardinality) {

    public ChildRole {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(cardinality, "cardinality");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A child role needs a name");
        }
    }

    public static <N extends Node> ChildRole<N> required(String name, Class<N> type) {
        return new ChildRole<>(name, type, ChildCardinality.REQUIRED);
    }

    public static <N extends Node> ChildRole<N> optional(String name, Class<N> type) {
        return new ChildRole<>(name, type, ChildCardinality.OPTIONAL);
    }

    public static <N extends Node> ChildRole<N> list(String name, Class<N> type) {
        return new ChildRole<>(name, type, ChildCardinality.LIST);
    }
}
