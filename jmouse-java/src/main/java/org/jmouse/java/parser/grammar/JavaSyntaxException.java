package org.jmouse.java.parser.grammar;

import java.util.Objects;
import org.jmouse.el.parser.ParseException;
import org.jmouse.java.parser.source.ProblemKind;
import org.jmouse.java.parser.source.SourceSpan;

/** A grammar failure with a position in the original, untranslated source. */
public final class JavaSyntaxException extends ParseException {
    private final SourceSpan span;
    private final ProblemKind kind;

    public JavaSyntaxException(String message, SourceSpan span) {
        this(ProblemKind.UNEXPECTED_TOKEN, message, span);
    }

    public JavaSyntaxException(ProblemKind kind, String message, SourceSpan span) {
        super(message);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.span = Objects.requireNonNull(span, "span");
    }

    public SourceSpan span() {
        return span;
    }

    public ProblemKind kind() {
        return kind;
    }
}
