package org.jmouse.vm.executable;

import org.jmouse.vm.program.ExecutableMethodKey;

import java.util.Objects;

/** Identity within a physical method and a frontend/transformation namespace. */
public record ExecutableNodeId(ExecutableMethodKey owner, String namespace, long ordinal) {

    public ExecutableNodeId {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(namespace, "namespace");
        if (namespace.isBlank() || ordinal < 0) {
            throw new IllegalArgumentException("Invalid executable node identity");
        }
    }
}
