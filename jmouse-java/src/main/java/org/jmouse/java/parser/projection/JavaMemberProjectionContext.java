package org.jmouse.java.parser.projection;

import java.util.Objects;

public record JavaMemberProjectionContext(
        JavaProjectionContext session, JavaDeclarationScope scope) {
    public JavaMemberProjectionContext {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(scope, "scope");
    }
}
