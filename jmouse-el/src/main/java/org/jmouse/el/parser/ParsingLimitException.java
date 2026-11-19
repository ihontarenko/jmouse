package org.jmouse.el.parser;

import java.util.Objects;

/** A controlled input refusal, not a JVM stack or memory failure. */
public final class ParsingLimitException extends ParseException {
    private final ParsingLimitKind kind;

    public ParsingLimitException(ParsingLimitKind kind, String message) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public ParsingLimitKind kind() {
        return kind;
    }
}
