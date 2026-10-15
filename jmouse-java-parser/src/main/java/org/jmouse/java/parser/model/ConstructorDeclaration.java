package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A constructor, or a record's compact canonical constructor.
 *
 * @param name           the constructor name as written
 * @param qualifiedName  the enclosing type's qualified name and the constructor name
 * @param symbolId       {@code Type#Type(ParameterTypes)}; a compact constructor uses its record's component types
 * @param modifiers      modifiers
 * @param annotations    annotations, in order
 * @param typeParameters declared type variables
 * @param parameters     formal parameters; empty for a compact constructor, which writes none
 * @param thrownTypes    the {@code throws} list as written
 * @param compact        whether this is a compact constructor
 * @param signature      the header, whitespace collapsed
 * @param signatureSpan  the exact header span
 * @param body           the opaque body from its opening to its closing brace
 * @param span           the whole declaration
 * @param javaDoc        the documentation comment, or {@code null}
 */
public record ConstructorDeclaration(String name, String qualifiedName, SymbolId symbolId, Set<Modifier> modifiers,
                                     List<Annotation> annotations, List<TypeParameter> typeParameters,
                                     List<Parameter> parameters, List<String> thrownTypes, boolean compact,
                                     String signature, SourceSpan signatureSpan, SourceSpan body, SourceSpan span,
                                     JavaDoc javaDoc) implements Declaration {

    /**
     * Copies the collections.
     */
    public ConstructorDeclaration {
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }

    @Override
    public DeclarationKind kind() {
        return compact ? DeclarationKind.COMPACT_CONSTRUCTOR : DeclarationKind.CONSTRUCTOR;
    }
}
