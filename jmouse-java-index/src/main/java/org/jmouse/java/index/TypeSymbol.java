package org.jmouse.java.index;

import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A type that is declared somewhere a {@link SymbolTable} knows of: what a resolved name points at.
 *
 * @param symbolId the parser's id of the declaration
 * @param qualifiedName the canonical name, nested types joined by dots
 * @param kind class, interface, enum, record or annotation type
 * @param source the source the type is declared in — whatever name the indexer was given for it,
 *     usually a path
 * @param nameSpan where its name is written in that source
 */
public record TypeSymbol(
        SymbolId symbolId,
        String qualifiedName,
        DeclarationKind kind,
        String source,
        SourceSpan nameSpan) {

    /**
     * Returns the simple name.
     *
     * @return the last segment of the qualified name
     */
    public String simpleName() {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }
}
