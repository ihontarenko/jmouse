package org.jmouse.el.parser;

import org.jmouse.el.node.Node;

import java.util.Objects;

/** A grammar role, independent of its implementation class or input representation. */
public record ParseRole<I, N extends Node>(String name, Class<I> inputType, Class<N> nodeType) {

    public ParseRole {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(nodeType, "nodeType");
        if (name.isBlank() || !name.equals(name.strip()) || name.indexOf('.') < 1 || name.endsWith(".")) {
            throw new IllegalArgumentException("A parse role needs a qualified name: " + name);
        }
    }
}
