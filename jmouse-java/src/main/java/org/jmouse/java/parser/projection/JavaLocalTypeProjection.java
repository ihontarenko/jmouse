package org.jmouse.java.parser.projection;

import org.jmouse.el.node.Node;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.LocalTypeDeclaration;
import org.jmouse.java.parser.model.LocalTypeKind;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Declares local types before their members are visited; no syntax is re-read or synthesized. */
final class JavaLocalTypeProjection {
    private JavaLocalTypeProjection() {}

    static void declare(
            JavaProjectionContext context, Node node, List<LocalTypeDeclaration> types) {
        if (context.declarations.containsKey(node)) {
            return;
        }
        if (node instanceof JavaTypeDeclarationNode local) {
            Declaration owner = enclosing(context, node);
            TypeDeclaration enclosingType = enclosingType(context, node);
            String prefix =
                    enclosingType.qualifiedName()
                            + JavaNavigationSymbolNames.LOCAL_TYPE_MARKER
                            + local.span().start().offset();
            TypeDeclaration declaration = JavaTypeProjection.project(context, local, prefix);
            types.add(
                    new LocalTypeDeclaration(
                            declaration,
                            owner.symbolId(),
                            scope(local),
                            LocalTypeKind.NAMED,
                            null));
        } else if (node instanceof JavaAnonymousClassBodyNode body) {
            Declaration owner = enclosing(context, node);
            TypeDeclaration enclosingType = enclosingType(context, node);
            String qualified =
                    enclosingType.qualifiedName()
                            + JavaNavigationSymbolNames.ANONYMOUS_TYPE_MARKER
                            + body.span().start().offset();
            String base =
                    body.getParent() instanceof JavaClassInstanceCreationExpressionNode creation
                            ? context.spelling.text(creation.type())
                            : enclosingType.qualifiedName();
            LocalTypeKind kind =
                    body.getParent() instanceof JavaEnumConstantNode
                            ? LocalTypeKind.ENUM_CONSTANT
                            : LocalTypeKind.ANONYMOUS;
            List<Declaration> members = new ArrayList<>();
            JavaDeclarationScope memberScope = new JavaDeclarationScope(qualified, List.of());
            body.declarations()
                    .forEach(
                            member ->
                                    members.addAll(
                                            JavaMemberProjection.project(
                                                    context, member, memberScope)));
            SourceSpan nameSpan =
                    context.source.span(body.span().start().offset(), body.span().start().offset());
            TypeDeclaration declaration =
                    context.declaration(
                            body,
                            new TypeDeclaration(
                                    DeclarationKind.CLASS,
                                    JavaNavigationSymbolNames.ANONYMOUS_TYPE_NAME,
                                    qualified,
                                    context.symbol(qualified),
                                    Set.of(),
                                    List.of(),
                                    List.of(),
                                    kind == LocalTypeKind.ENUM_CONSTANT ? List.of(base) : List.of(),
                                    List.of(),
                                    List.of(),
                                    List.of(),
                                    members,
                                    "",
                                    nameSpan,
                                    nameSpan,
                                    body.span(),
                                    body.span(),
                                    null));
            types.add(
                    new LocalTypeDeclaration(
                            declaration, owner.symbolId(), body.span(), kind, base));
        }
    }

    static Declaration enclosing(JavaProjectionContext context, Node node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            Declaration declaration = context.declarations.get(parent);
            if (declaration != null) {
                return declaration;
            }
        }
        throw new TranslationRefusedException("A local type requires a declared lexical owner");
    }

    static TypeDeclaration enclosingType(JavaProjectionContext context, Node node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (context.declarations.get(parent) instanceof TypeDeclaration type) {
                return type;
            }
        }
        throw new TranslationRefusedException("A local type requires an enclosing source type");
    }

    private static SourceSpan scope(JavaSyntaxNode node) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JavaBlockNode block) {
                return block.span();
            }
        }
        return node.span();
    }
}
