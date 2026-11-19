package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A method, including an annotation type element.
 *
 * @param name           the method name
 * @param qualifiedName  the enclosing type's qualified name and the method name
 * @param symbolId       {@code Type#name(ParameterTypes)}
 * @param modifiers      modifiers
 * @param annotations    annotations, in order
 * @param typeParameters declared type variables
 * @param returnType     the return type as written; {@code void} included
 * @param parameters     formal parameters, receiver parameter included when written
 * @param thrownTypes    the {@code throws} list as written
 * @param defaultValue   an annotation element's {@code default} value as written, or {@code null}
 * @param signature      the header, whitespace collapsed
 * @param signatureSpan  the exact header span
 * @param nameSpan       the method name where it is declared
 * @param body           the opaque body from its opening to its closing brace, or {@code null} when declared with {@code ;}
 * @param span           the whole declaration
 * @param javaDoc        the documentation comment, or {@code null}
 */
public record MethodDeclaration(String name, String qualifiedName, SymbolId symbolId, Set<Modifier> modifiers,
                                List<Annotation> annotations, List<TypeParameter> typeParameters, String returnType,
                                List<Parameter> parameters, List<String> thrownTypes, String defaultValue,
                                String signature, SourceSpan signatureSpan, SourceSpan nameSpan, SourceSpan body, SourceSpan span,
                                JavaDoc javaDoc) implements Declaration {

    /**
     * Copies the collections.
     */
    public MethodDeclaration {
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }

    @Override
    public DeclarationKind kind() {
        return DeclarationKind.METHOD;
    }
}
