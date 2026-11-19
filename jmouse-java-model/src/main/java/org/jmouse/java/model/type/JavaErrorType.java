package org.jmouse.java.model.type;

import java.util.Objects;

/** A compiler-reported erroneous type; it can never certify a successful program. */
public record JavaErrorType(String displayName) implements JavaType {
    public JavaErrorType {
        Objects.requireNonNull(displayName, "displayName");
    }
}
