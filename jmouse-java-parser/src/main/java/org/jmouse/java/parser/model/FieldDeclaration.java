package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * One variable of a field declaration.
 *
 * <p>{@code int first, second = 2;} produces two field declarations. They share the statement's
 * {@link #span()}, modifiers, annotations and documentation comment, and differ in name,
 * {@link #variableSpan()} and id. Consumers that need statements group by span.
 *
 * @param name          the variable name
 * @param qualifiedName the enclosing type's qualified name and the variable name
 * @param symbolId      {@code Type#name}
 * @param modifiers     modifiers of the statement
 * @param annotations   annotations of the statement
 * @param type          the declared type as written, with any brackets written after the variable name appended
 * @param signature     the statement header followed by the variable name, whitespace collapsed
 * @param signatureSpan the exact statement header, up to the end of the type
 * @param variableSpan  this variable: its name, brackets and initializer
 * @param span          the whole statement, to its semicolon
 * @param javaDoc       the statement's documentation comment, owned by this variable, or {@code null}
 */
public record FieldDeclaration(String name, String qualifiedName, SymbolId symbolId, Set<Modifier> modifiers,
                               List<Annotation> annotations, String type, String signature, SourceSpan signatureSpan,
                               SourceSpan variableSpan, SourceSpan span, JavaDoc javaDoc) implements Declaration {

    /**
     * Copies the collections.
     */
    public FieldDeclaration {
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
    }

    @Override
    public DeclarationKind kind() {
        return DeclarationKind.FIELD;
    }
}
