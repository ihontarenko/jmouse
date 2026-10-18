package org.jmouse.java.index;

import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.model.TypeParameter;
import org.jmouse.java.parser.model.TypeReference;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens one parser result into a {@link FileIndex}, attaching to every type reference the
 * declaration it belongs to and the type body it stands in.
 */
final class FileIndexer {

    private FileIndexer() {}

    static FileIndex index(JavaSourceFile file) {
        List<IndexedDeclaration> declarations = new ArrayList<>();
        for (TypeDeclaration type : file.types()) {
            collect(type, null, declarations);
        }
        List<IndexedReference> references = new ArrayList<>();
        for (TypeReference reference : file.typeReferences()) {
            references.add(
                    new IndexedReference(
                            reference.name(),
                            reference.role(),
                            reference.span(),
                            owner(reference.span(), declarations),
                            scopeType(reference.span(), declarations)));
        }
        List<MemberSymbol> members = new ArrayList<>();
        for (Declaration declaration : file.declarations()) {
            MemberSymbol.of(file.sourceName(), declaration).ifPresent(members::add);
        }
        return new FileIndex(
                file.sourceName(),
                file.packageName(),
                file.imports(),
                declarations,
                references,
                file.problems(),
                members,
                file.bodies(),
                file.javaDocs());
    }

    private static void collect(
            TypeDeclaration type, SymbolId container, List<IndexedDeclaration> declarations) {
        declarations.add(
                new IndexedDeclaration(
                        type.symbolId(),
                        type.kind(),
                        type.name(),
                        type.qualifiedName(),
                        container,
                        typeParameterNames(type.typeParameters()),
                        type.modifiers(),
                        type.signature(),
                        type.nameSpan(),
                        type.bodySpan(),
                        type.span()));
        for (Declaration component : type.recordComponents()) {
            declarations.add(member(component, type.symbolId(), List.of()));
        }
        for (Declaration member : type.members()) {
            switch (member) {
                case TypeDeclaration nested -> collect(nested, type.symbolId(), declarations);
                case MethodDeclaration method ->
                        declarations.add(
                                member(
                                        method,
                                        type.symbolId(),
                                        typeParameterNames(method.typeParameters())));
                case ConstructorDeclaration constructor ->
                        declarations.add(
                                member(
                                        constructor,
                                        type.symbolId(),
                                        typeParameterNames(constructor.typeParameters())));
                default -> declarations.add(member(member, type.symbolId(), List.of()));
            }
        }
    }

    private static IndexedDeclaration member(
            Declaration member, SymbolId container, List<String> typeParameters) {
        return new IndexedDeclaration(
                member.symbolId(),
                member.kind(),
                member.name(),
                member.qualifiedName(),
                container,
                typeParameters,
                member.modifiers(),
                member.signature(),
                member.nameSpan(),
                null,
                member.span());
    }

    private static List<String> typeParameterNames(List<TypeParameter> typeParameters) {
        return typeParameters.stream().map(TypeParameter::name).toList();
    }

    /**
     * The innermost declaration whose header holds the reference. Declarations are outer before
     * inner, so the last containing one is the innermost; a type counts only when the reference is
     * outside its body — inside the body it belongs to a member instead.
     */
    private static SymbolId owner(SourceSpan reference, List<IndexedDeclaration> declarations) {
        SymbolId owner = null;
        for (IndexedDeclaration declaration : declarations) {
            if (!declaration.span().contains(reference)) {
                continue;
            }
            if (declaration.isType()
                    && declaration.bodySpan() != null
                    && declaration.bodySpan().contains(reference)) {
                continue;
            }
            owner = declaration.symbolId();
        }
        return owner;
    }

    /** The innermost type whose body holds the reference. */
    private static SymbolId scopeType(SourceSpan reference, List<IndexedDeclaration> declarations) {
        SymbolId scopeType = null;
        for (IndexedDeclaration declaration : declarations) {
            if (declaration.isType()
                    && declaration.bodySpan() != null
                    && declaration.bodySpan().contains(reference)) {
                scopeType = declaration.symbolId();
            }
        }
        return scopeType;
    }
}
