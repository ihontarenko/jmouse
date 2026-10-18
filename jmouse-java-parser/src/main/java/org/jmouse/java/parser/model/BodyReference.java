package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * A syntactic reference in a body. The index decides whether it names a local, field, method or
 * type.
 */
public record BodyReference(Expression expression, Role role, SymbolId owner, SymbolId scopeType) {
    public enum Role {
        CALLS,
        READS,
        WRITES,
        READS_WRITES,
        INSTANTIATES,
        METHOD_REFERENCE
    }

    public String name() {
        String name = expression.name();
        if (expression.kind() == Expression.Kind.METHOD_REFERENCE
                && name.equals("new")
                && expression.receiver() != null) {
            name = expression.receiver().name();
        }
        int arguments = name.indexOf('<');
        if (arguments >= 0) {
            name = name.substring(0, arguments);
        }
        name = name.replace("[]", "");
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public SourceSpan span() {
        return expression.nameSpan();
    }
}
