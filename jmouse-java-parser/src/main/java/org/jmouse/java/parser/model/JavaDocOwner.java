package org.jmouse.java.parser.model;

import org.jmouse.java.parser.source.SourceSpan;

/**
 * The declaration a documentation comment belongs to — the explicit link from a comment to its owner.
 *
 * <p>The comment and its owner have separate coordinates: {@link JavaDoc#span()} is the comment,
 * {@link #span()} is the declaration that follows it.
 *
 * @param symbolId      the owner's id, the same value as {@link Declaration#symbolId()}
 * @param kind          what the owner declares
 * @param qualifiedName the owner's qualified name, for example {@code org.example.Type.method}
 * @param signature     the owner's signature as written
 * @param span          the owner's own span, which starts after the comment
 */
public record JavaDocOwner(SymbolId symbolId, DeclarationKind kind, String qualifiedName, String signature, SourceSpan span) {

    /**
     * Validates the components.
     */
    public JavaDocOwner {
        if (symbolId == null || kind == null || qualifiedName == null || signature == null || span == null) {
            throw new IllegalArgumentException("A JavaDoc owner needs every component.");
        }
    }
}
