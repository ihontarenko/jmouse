package org.jmouse.el.node;

import java.util.Objects;

/** Stable, namespaced identity used by grammar, analysis and writing registrations. */
public record NodeKind<N extends Node>(String name, Class<N> type) {

    public NodeKind {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank() || !name.equals(name.strip()) || name.indexOf('.') < 1
                || name.endsWith(".")) {
            throw new IllegalArgumentException("A node kind needs a qualified name: " + name);
        }
    }
}
