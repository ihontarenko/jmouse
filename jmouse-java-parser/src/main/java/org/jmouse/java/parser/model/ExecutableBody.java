package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

import java.util.List;

/** One executable declaration's expressions, lexical bindings and unresolved references. */
public record ExecutableBody(
        SymbolId owner,
        SymbolId scopeType,
        SourceSpan span,
        List<Expression> expressions,
        List<LocalBinding> variables,
        List<BodyReference> references) {
    public ExecutableBody {
        expressions = List.copyOf(expressions);
        variables = List.copyOf(variables);
        references = List.copyOf(references);
    }
}
