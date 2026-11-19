package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

import java.util.List;

/** A source expression, without name resolution. Children retain receiver/argument structure. */
public record Expression(
        Kind kind,
        String name,
        SourceSpan nameSpan,
        SourceSpan span,
        Expression receiver,
        List<Expression> arguments) {
    public Expression {
        arguments = List.copyOf(arguments);
    }

    public enum Kind {
        NAME,
        LITERAL,
        FIELD,
        CALL,
        NEW,
        METHOD_REFERENCE,
        OPERATION,
        ARRAY_ACCESS,
        LAMBDA,
        UNKNOWN
    }
}
