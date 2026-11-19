package org.jmouse.vm.runtime;

import java.util.Objects;

/** Logical runtime capability; platform profiles select and validate its implementation. */
public record RuntimeComponentId(String name) {

    public RuntimeComponentId {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A runtime component requires an identity");
        }
    }
}
