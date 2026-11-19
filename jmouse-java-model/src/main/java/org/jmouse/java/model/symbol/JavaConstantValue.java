package org.jmouse.java.model.symbol;

import java.util.Objects;

/** A Java constant variable value, computed by attribution without executing the program. */
public record JavaConstantValue(Object value) {
    public JavaConstantValue {
        Objects.requireNonNull(value, "value");
        if (!(value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Character
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof String)) {
            throw new IllegalArgumentException(
                    "Unsupported Java constant value: " + value.getClass());
        }
    }
}
