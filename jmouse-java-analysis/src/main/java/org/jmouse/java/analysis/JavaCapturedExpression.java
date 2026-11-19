package org.jmouse.java.analysis;

import org.jmouse.java.model.type.JavaType;

import java.util.Objects;

/** A bound reference evaluates its receiver expression once when the reference is created. */
public record JavaCapturedExpression(JavaNodeIdentity expression, JavaType type)
        implements JavaCapturedValue {
    public JavaCapturedExpression {
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(type, "type");
    }
}
