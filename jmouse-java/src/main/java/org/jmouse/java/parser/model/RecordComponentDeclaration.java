package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A component in a record header. It has no comment of its own; the record's {@code @param} tag
 * documents it.
 *
 * @param name          the component name
 * @param qualifiedName the record's qualified name and the component name
 * @param symbolId      {@code Record#name}
 * @param annotations   annotations, in order
 * @param type          the type as written, without the {@code ...} of a variable-arity component
 * @param variableArity whether it is declared with {@code ...}
 * @param signature     the component as written, whitespace collapsed
 * @param nameSpan      the component's name where it is declared
 * @param span          the component
 */
public record RecordComponentDeclaration(String name, String qualifiedName, SymbolId symbolId, List<Annotation> annotations,
                                         String type, boolean variableArity, String signature, SourceSpan nameSpan, SourceSpan span)
        implements Declaration {

    /**
     * Copies the list.
     */
    public RecordComponentDeclaration {
        annotations = List.copyOf(annotations);
    }

    @Override
    public DeclarationKind kind() {
        return DeclarationKind.RECORD_COMPONENT;
    }

    @Override
    public Set<Modifier> modifiers() {
        return Set.of();
    }

    @Override
    public SourceSpan signatureSpan() {
        return span;
    }

    @Override
    public JavaDoc javaDoc() {
        return null;
    }
}
