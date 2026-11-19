package org.jmouse.java.model.symbol;

import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

import java.util.Objects;

public record JavaImplicitParameter(JavaImplicitParameterKind kind, JavaTypeDescriptor type) {
    public JavaImplicitParameter {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(type, "type");
    }
}
