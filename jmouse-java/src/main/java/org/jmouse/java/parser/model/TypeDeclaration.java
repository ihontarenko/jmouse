package org.jmouse.java.parser.model;

import java.util.List;
import java.util.Set;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A class, interface, enum, record or annotation type, top-level or nested in another type.
 *
 * <p>The declared relationships ({@code extends}, {@code implements}, {@code permits}) are the type
 * names as written — syntactic, never resolved to the types they name.
 *
 * @param kind             one of the type kinds
 * @param name             the simple name
 * @param qualifiedName    package and enclosing types joined by dots
 * @param symbolId         the id; equal to the qualified name unless deduplicated
 * @param modifiers        modifiers, including {@code sealed} and {@code non-sealed}
 * @param annotations      annotations, in order
 * @param typeParameters   declared type variables
 * @param extendedTypes    the superclass, or the superinterfaces of an interface, as written
 * @param implementedTypes implemented interfaces, as written
 * @param permittedTypes   the {@code permits} list, as written
 * @param recordComponents a record's header components; empty for other kinds
 * @param members          fields, methods, constructors, initializers, enum constants and nested types, in source order
 * @param signature        the header as written, whitespace collapsed, up to the opening brace
 * @param signatureSpan    the exact header span
 * @param nameSpan         the simple name where it is declared
 * @param bodySpan         from the opening brace to the closing brace inclusive
 * @param span             the whole declaration
 * @param javaDoc          the documentation comment, or {@code null}
 */
public record TypeDeclaration(DeclarationKind kind, String name, String qualifiedName, SymbolId symbolId,
                              Set<Modifier> modifiers, List<Annotation> annotations, List<TypeParameter> typeParameters,
                              List<String> extendedTypes, List<String> implementedTypes, List<String> permittedTypes,
                              List<RecordComponentDeclaration> recordComponents, List<Declaration> members,
                              String signature, SourceSpan signatureSpan, SourceSpan nameSpan, SourceSpan bodySpan, SourceSpan span,
                              JavaDoc javaDoc) implements Declaration {

    /**
     * Validates the kind and copies the collections.
     */
    public TypeDeclaration {
        if (kind == null || !kind.isType()) {
            throw new IllegalArgumentException("A type declaration needs a type kind.");
        }
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
        typeParameters = List.copyOf(typeParameters);
        extendedTypes = List.copyOf(extendedTypes);
        implementedTypes = List.copyOf(implementedTypes);
        permittedTypes = List.copyOf(permittedTypes);
        recordComponents = List.copyOf(recordComponents);
        members = List.copyOf(members);
    }

    /**
     * Returns the types declared directly inside this one.
     *
     * @return nested types, in source order
     */
    public List<TypeDeclaration> nestedTypes() {
        return members.stream()
                .filter(TypeDeclaration.class::isInstance)
                .map(TypeDeclaration.class::cast)
                .toList();
    }
}
