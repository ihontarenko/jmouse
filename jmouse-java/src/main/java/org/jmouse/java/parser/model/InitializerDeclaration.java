package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * An instance or {@code static} initializer block.
 *
 * @param isStatic      whether it is a {@code static} initializer
 * @param ordinal       its one-based position among the initializers of the same kind in its type
 * @param qualifiedName the enclosing type's qualified name and {@link #name()}
 * @param symbolId      {@code Type#<initializer>[n]} or {@code Type#<static-initializer>[n]}
 * @param signature     {@code static} or the empty string
 * @param signatureSpan the {@code static} keyword, or an empty span at the opening brace
 * @param body          the opaque block from its opening to its closing brace
 * @param span          the whole initializer
 * @param javaDoc       the documentation comment, or {@code null}
 */
public record InitializerDeclaration(boolean isStatic, int ordinal, String qualifiedName, SymbolId symbolId,
                                     String signature, SourceSpan signatureSpan, SourceSpan body, SourceSpan span,
                                     JavaDoc javaDoc) implements Declaration {

    @Override
    public DeclarationKind kind() {
        return isStatic ? DeclarationKind.STATIC_INITIALIZER : DeclarationKind.INITIALIZER;
    }

    /**
     * Returns the signature span: an initializer has no name to point at.
     *
     * @return the {@code static} keyword, or an empty span at the opening brace
     */
    @Override
    public SourceSpan nameSpan() {
        return signatureSpan;
    }

    @Override
    public String name() {
        return isStatic ? "<static-initializer>" : "<initializer>";
    }

    @Override
    public Set<Modifier> modifiers() {
        return isStatic ? Set.of(Modifier.STATIC) : Set.of();
    }

    @Override
    public List<Annotation> annotations() {
        return List.of();
    }
}
