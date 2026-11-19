package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

import java.util.Objects;

/**
 * A type declared in executable code, with its lexical owner instead of an invented member
 * relationship.
 */
public record LocalTypeDeclaration(
        TypeDeclaration declaration,
        SymbolId enclosing,
        SourceSpan scope,
        LocalTypeKind kind,
        String baseType) {
    public LocalTypeDeclaration {
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(enclosing, "enclosing");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(kind, "kind");
    }
}
