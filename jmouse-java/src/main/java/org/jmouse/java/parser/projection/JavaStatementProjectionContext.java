package org.jmouse.java.parser.projection;

import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.Objects;

public record JavaStatementProjectionContext(
        JavaProjectionContext session, SymbolId owner, SymbolId type, SourceSpan scope) {
    public JavaStatementProjectionContext {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
    }
}
