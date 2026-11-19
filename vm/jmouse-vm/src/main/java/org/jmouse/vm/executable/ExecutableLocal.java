package org.jmouse.vm.executable;

import org.jmouse.java.model.type.JavaType;

import java.util.Objects;

public record ExecutableLocal(ExecutableLocalId identity, String name, JavaType type) {

    public ExecutableLocal {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A local requires a readable display name");
        }
    }
}
