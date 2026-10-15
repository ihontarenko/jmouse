package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A constant of an enum, with its arguments and class body when written.
 *
 * @param name          the constant
 * @param qualifiedName the enum's qualified name and the constant
 * @param symbolId      {@code Enum#CONSTANT}
 * @param annotations   annotations, in order
 * @param arguments     the exact text between the argument parentheses, or {@code null} when none are written
 * @param signature     the annotations, name and arguments as written, whitespace collapsed
 * @param signatureSpan the exact span of that text
 * @param body          the opaque class body, or {@code null}
 * @param span          the whole constant, without the separating comma
 * @param javaDoc       the documentation comment, or {@code null}
 */
public record EnumConstantDeclaration(String name, String qualifiedName, SymbolId symbolId, List<Annotation> annotations,
                                      String arguments, String signature, SourceSpan signatureSpan, SourceSpan body,
                                      SourceSpan span, JavaDoc javaDoc) implements Declaration {

    /**
     * Copies the list.
     */
    public EnumConstantDeclaration {
        annotations = List.copyOf(annotations);
    }

    @Override
    public DeclarationKind kind() {
        return DeclarationKind.ENUM_CONSTANT;
    }

    @Override
    public Set<Modifier> modifiers() {
        return Set.of();
    }
}
