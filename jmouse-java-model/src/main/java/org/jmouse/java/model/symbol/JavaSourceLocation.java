package org.jmouse.java.model.symbol;

import java.util.Objects;

public record JavaSourceLocation(String sourceName, int start, int end) {
    public JavaSourceLocation {
        Objects.requireNonNull(sourceName, "sourceName");
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Invalid source range");
        }
    }
}
