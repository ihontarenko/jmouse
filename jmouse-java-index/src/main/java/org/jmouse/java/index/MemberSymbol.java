package org.jmouse.java.index;

import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.EnumConstantDeclaration;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.RecordComponentDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A declared method, constructor or field, including the written types needed for member
 * resolution.
 */
public record MemberSymbol(
        String source,
        SymbolId symbolId,
        SymbolId container,
        String name,
        String qualifiedName,
        DeclarationKind kind,
        String type,
        List<Parameter> parameters,
        Set<Modifier> modifiers,
        SourceSpan nameSpan) {
    public MemberSymbol {
        parameters = List.copyOf(parameters);
        modifiers = Set.copyOf(modifiers);
    }

    public boolean callable() {
        return kind == DeclarationKind.METHOD
                || kind == DeclarationKind.CONSTRUCTOR
                || kind == DeclarationKind.COMPACT_CONSTRUCTOR;
    }

    static Optional<MemberSymbol> of(String source, Declaration declaration) {
        String type;
        List<Parameter> parameters = List.of();
        switch (declaration) {
            case MethodDeclaration method -> {
                type = method.returnType();
                parameters =
                        method.parameters().stream()
                                .filter(parameter -> !parameter.receiver())
                                .toList();
            }
            case ConstructorDeclaration constructor -> {
                type = constructor.symbolId().value().split("#", 2)[0];
                parameters =
                        constructor.parameters().stream()
                                .filter(parameter -> !parameter.receiver())
                                .toList();
            }
            case FieldDeclaration field -> type = field.type();
            case RecordComponentDeclaration component -> type = component.type();
            case EnumConstantDeclaration constant ->
                    type = constant.symbolId().value().split("#", 2)[0];
            default -> {
                return Optional.empty();
            }
        }
        SymbolId container = new SymbolId(declaration.symbolId().value().split("#", 2)[0]);
        return Optional.of(
                new MemberSymbol(
                        source,
                        declaration.symbolId(),
                        container,
                        declaration.name(),
                        declaration.qualifiedName(),
                        declaration.kind(),
                        type,
                        parameters,
                        declaration.modifiers(),
                        declaration.nameSpan()));
    }
}
