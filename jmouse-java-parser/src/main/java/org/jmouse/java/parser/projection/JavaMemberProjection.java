package org.jmouse.java.parser.projection;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.model.*;
import org.jmouse.java.parser.node.body.JavaSemicolonBodyNode;
import org.jmouse.java.parser.node.declaration.*;
import org.jmouse.java.parser.source.SourceSpan;

/** One immutable record projection per source declaration, with fields split by written declarator. */
final class JavaMemberProjection {
    private JavaMemberProjection() {
    }

    static List<Declaration> project(JavaProjectionContext context, JavaDeclarationNode node, JavaTypeProjection.Scope scope) {
        return switch (node) {
            case JavaTypeDeclarationNode type -> List.of(JavaTypeProjection.project(context, type, scope.qualifiedName()));
            case JavaFieldDeclarationNode field -> fields(context, field, scope);
            case JavaMethodDeclarationNode method -> List.of(method(context, method, scope));
            case JavaConstructorDeclarationNode constructor -> List.of(constructor(context, constructor, scope));
            case JavaCompactConstructorDeclarationNode compact -> List.of(compact(context, compact, scope));
            case JavaInitializerDeclarationNode initializer -> List.of(initializer(context, initializer, scope));
            case JavaEnumConstantNode constant -> List.of(constant(context, constant, scope));
            case JavaAnnotationElementDeclarationNode element -> List.of(annotationElement(context, element, scope));
            case JavaEmptyMemberDeclarationNode ignored -> List.of();
            default -> throw new IllegalArgumentException("No declaration projection for " + node.schema().kind().name());
        };
    }

    private static List<Declaration> fields(JavaProjectionContext context, JavaFieldDeclarationNode node,
                                             JavaTypeProjection.Scope scope) {
        List<Declaration> declarations = new ArrayList<>();
        SourceSpan signatureSpan = context.source.span(node.span().start().offset(), node.type().span().end().offset());
        String header = context.spelling.collapse(signatureSpan);
        String type = context.type(node.type(), TypeReferenceRole.FIELD_TYPE);
        List<Annotation> annotations = context.annotations(node.modifiers());
        for (JavaVariableDeclaratorNode variable : node.variables()) {
            String name = variable.name().name();
            String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
            SymbolId symbol = context.member(scope.qualifiedName(), name);
            String signature = header + " " + name;
            FieldDeclaration declaration = new FieldDeclaration(name, qualified, symbol, context.modifiers(node.modifiers()),
                    annotations, type + context.spelling.dimensions(variable.dimensions()), signature, signatureSpan,
                    variable.name().span(), variable.span(), node.span(),
                    context.document(node, symbol, DeclarationKind.FIELD, qualified, signature));
            declarations.add(context.declaration(variable, declaration));
        }
        return List.copyOf(declarations);
    }

    private static MethodDeclaration method(JavaProjectionContext context, JavaMethodDeclarationNode node,
                                               JavaTypeProjection.Scope scope) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.executable(scope.qualifiedName(), name, JavaParameterProjection.identities(node.parameters()));
        SourceSpan signatureSpan = context.header(node, node.body().span().start().offset());
        String signature = context.spelling.collapse(signatureSpan);
        return context.declaration(node, new MethodDeclaration(name, qualified, symbol, context.modifiers(node.modifiers()),
                context.annotations(node.modifiers()), context.typeParameters(node.typeParameters()),
                context.type(node.resultType(), TypeReferenceRole.RETURN_TYPE) + context.spelling.dimensions(node.dimensions()),
                JavaParameterProjection.parameters(context, node.parameters(), node.receiver().orElse(null)),
                context.types(node.thrownTypes(), TypeReferenceRole.THROWS), null, signature, signatureSpan,
                node.name().span(), node.body() instanceof JavaSemicolonBodyNode ? null : node.body().span(), node.span(),
                context.document(node, symbol, DeclarationKind.METHOD, qualified, signature)));
    }

    private static ConstructorDeclaration constructor(JavaProjectionContext context, JavaConstructorDeclarationNode node,
                                                         JavaTypeProjection.Scope scope) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.executable(scope.qualifiedName(), name, JavaParameterProjection.identities(node.parameters()));
        SourceSpan signatureSpan = context.header(node, node.body().span().start().offset());
        String signature = context.spelling.collapse(signatureSpan);
        return context.declaration(node, new ConstructorDeclaration(name, qualified, symbol,
                context.modifiers(node.modifiers()), context.annotations(node.modifiers()),
                context.typeParameters(node.typeParameters()),
                JavaParameterProjection.parameters(context, node.parameters(), node.receiver().orElse(null)),
                context.types(node.thrownTypes(), TypeReferenceRole.THROWS), false, signature, signatureSpan,
                node.name().span(), node.body().span(), node.span(),
                context.document(node, symbol, DeclarationKind.CONSTRUCTOR, qualified, signature)));
    }

    private static ConstructorDeclaration compact(JavaProjectionContext context, JavaCompactConstructorDeclarationNode node,
                                                     JavaTypeProjection.Scope scope) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.executable(scope.qualifiedName(), name, scope.canonicalParameters());
        SourceSpan signatureSpan = context.header(node, node.body().span().start().offset());
        String signature = context.spelling.collapse(signatureSpan);
        return context.declaration(node, new ConstructorDeclaration(name, qualified, symbol,
                context.modifiers(node.modifiers()), context.annotations(node.modifiers()), List.of(), List.of(), List.of(),
                true, signature, signatureSpan, node.name().span(), node.body().span(), node.span(),
                context.document(node, symbol, DeclarationKind.COMPACT_CONSTRUCTOR, qualified, signature)));
    }

    private static InitializerDeclaration initializer(JavaProjectionContext context, JavaInitializerDeclarationNode node,
                                                         JavaTypeProjection.Scope scope) {
        int ordinal = scope.nextInitializer(node.kind());
        boolean isStatic = node.kind() == JavaInitializerKind.STATIC;
        String name = isStatic ? "<static-initializer>" : "<initializer>";
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.member(scope.qualifiedName(), name + "[" + ordinal + "]");
        SourceSpan signatureSpan = isStatic ? context.header(node, node.body().span().start().offset())
                : context.source.span(node.body().span().start().offset(), node.body().span().start().offset());
        String signature = isStatic ? JavaKeyword.STATIC.spelling() : "";
        return context.declaration(node, new InitializerDeclaration(isStatic, ordinal, qualified, symbol, signature,
                signatureSpan, node.body().span(), node.span(), context.document(node, symbol,
                        isStatic ? DeclarationKind.STATIC_INITIALIZER : DeclarationKind.INITIALIZER, qualified, signature)));
    }

    private static EnumConstantDeclaration constant(JavaProjectionContext context, JavaEnumConstantNode node,
                                                       JavaTypeProjection.Scope scope) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.member(scope.qualifiedName(), name);
        SourceSpan signatureSpan = node.body().isPresent()
                ? context.header(node, node.body().get().span().start().offset()) : node.span();
        String signature = context.spelling.collapse(signatureSpan);
        String arguments = null;
        Entry next = context.source.tokens().get(context.source.tokenIndexAtOrAfter(node.name().span().end().offset()));
        if (next.token() == JavaToken.LEFT_PARENTHESIS) {
            Entry closing = context.source.tokens().get(context.source.tokenIndexAtOrAfter(signatureSpan.end().offset()) - 1);
            arguments = context.source.text().substring(next.offset() + next.length(), closing.offset());
        }
        return context.declaration(node, new EnumConstantDeclaration(name, qualified, symbol, context.annotations(node.annotations()),
                arguments, signature, signatureSpan, node.name().span(), node.body().map(body -> body.span()).orElse(null),
                node.span(), context.document(node, symbol, DeclarationKind.ENUM_CONSTANT, qualified, signature)));
    }

    private static MethodDeclaration annotationElement(JavaProjectionContext context, JavaAnnotationElementDeclarationNode node,
                                                          JavaTypeProjection.Scope scope) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(scope.qualifiedName(), name);
        SymbolId symbol = context.executable(scope.qualifiedName(), name, List.of());
        int boundary = node.defaultValue().map(value -> {
            int index = context.source.tokenIndexAtOrAfter(value.span().start().offset());
            return context.source.tokens().get(index - 1).offset();
        }).orElseGet(() -> context.source.tokens()
                .get(context.source.tokenIndexAtOrAfter(node.span().end().offset()) - 1).offset());
        SourceSpan signatureSpan = context.header(node, boundary);
        String signature = context.spelling.collapse(signatureSpan);
        return context.declaration(node, new MethodDeclaration(name, qualified, symbol, context.modifiers(node.modifiers()),
                context.annotations(node.modifiers()), List.of(),
                context.type(node.type(), TypeReferenceRole.RETURN_TYPE) + context.spelling.dimensions(node.dimensions()),
                List.of(), List.of(), node.defaultValue().map(value -> value.span().slice(context.source.text())).orElse(null),
                signature, signatureSpan, node.name().span(), null, node.span(),
                context.document(node, symbol, DeclarationKind.METHOD, qualified, signature)));
    }
}
