package org.jmouse.java.parser.projection;

import java.util.Objects;

public record JavaTypeProjectionContext(JavaProjectionContext session, String enclosing) {
    public JavaTypeProjectionContext {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(enclosing, "enclosing");
    }
}
