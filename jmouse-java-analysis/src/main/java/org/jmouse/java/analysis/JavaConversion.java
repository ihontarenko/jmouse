package org.jmouse.java.analysis;

import org.jmouse.java.model.type.JavaType;

import java.util.Objects;

public record JavaConversion(JavaType source, JavaType target, JavaConversionKind kind) {
    public JavaConversion {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
    }
}
