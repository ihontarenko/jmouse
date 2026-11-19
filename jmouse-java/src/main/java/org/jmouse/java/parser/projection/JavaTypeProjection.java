package org.jmouse.java.parser.projection;

import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.parser.model.Annotation;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.RecordComponentDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.model.TypeParameter;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.node.declaration.JavaAnnotationTypeDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaClassDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;
import org.jmouse.java.parser.node.declaration.JavaEnumDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaInterfaceDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.declaration.JavaRecordDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;

final class JavaTypeProjection {
    private JavaTypeProjection() {}

    static void install(
            NodeOperationRegistry<JavaTypeProjectionContext, TypeDeclaration> registry) {
        registry.bind(
                JavaClassDeclarationNode.KIND,
                JavaTypeProjection.class.getName() + "#" + JavaClassDeclarationNode.KIND.name(),
                (node, context) -> standard(context.session(), node, context.enclosing()));
        registry.bind(
                JavaInterfaceDeclarationNode.KIND,
                JavaTypeProjection.class.getName() + "#" + JavaInterfaceDeclarationNode.KIND.name(),
                (node, context) -> standard(context.session(), node, context.enclosing()));
        registry.bind(
                JavaEnumDeclarationNode.KIND,
                JavaTypeProjection.class.getName() + "#" + JavaEnumDeclarationNode.KIND.name(),
                (node, context) -> standard(context.session(), node, context.enclosing()));
        registry.bind(
                JavaRecordDeclarationNode.KIND,
                JavaTypeProjection.class.getName() + "#" + JavaRecordDeclarationNode.KIND.name(),
                (node, context) -> standard(context.session(), node, context.enclosing()));
        registry.bind(
                JavaAnnotationTypeDeclarationNode.KIND,
                JavaTypeProjection.class.getName()
                        + "#"
                        + JavaAnnotationTypeDeclarationNode.KIND.name(),
                (node, context) -> standard(context.session(), node, context.enclosing()));
    }

    static TypeDeclaration project(
            JavaProjectionContext context, JavaTypeDeclarationNode node, String enclosing) {
        return context.configuration
                .types()
                .apply(node, new JavaTypeProjectionContext(context, enclosing));
    }

    private static TypeDeclaration standard(
            JavaProjectionContext context, JavaTypeDeclarationNode node, String enclosing) {
        String name = node.name().name();
        String qualified = JavaProjectionContext.qualify(enclosing, name);
        SymbolId symbol = context.symbol(qualified);
        DeclarationKind kind = kind(node);
        List<String> extended =
                node instanceof JavaClassDeclarationNode type
                        ? type.superclass()
                                .map(
                                        parent ->
                                                List.of(
                                                        context.type(
                                                                parent, TypeReferenceRole.EXTENDS)))
                                .orElse(List.of())
                        : node instanceof JavaInterfaceDeclarationNode
                                ? context.types(node.interfaces(), TypeReferenceRole.EXTENDS)
                                : List.of();
        List<String> implemented =
                node instanceof JavaInterfaceDeclarationNode
                        ? List.of()
                        : context.types(node.interfaces(), TypeReferenceRole.IMPLEMENTS);
        List<String> permitted = context.types(node.permittedTypes(), TypeReferenceRole.PERMITS);
        List<JavaRecordComponentNode> components =
                node instanceof JavaRecordDeclarationNode record ? record.components() : List.of();
        SourceSpan signatureSpan = context.header(node, node.body().span().start().offset());
        String signature = context.spelling.collapse(signatureSpan);
        List<Annotation> annotations = context.annotations(node.modifiers());
        List<TypeParameter> parameters = context.typeParameters(node.typeParameters());
        JavaDoc javaDoc = context.document(node, symbol, kind, qualified, signature);
        JavaDeclarationScope scope = new JavaDeclarationScope(qualified, components);
        List<RecordComponentDeclaration> projectedComponents = new ArrayList<>();
        for (JavaRecordComponentNode component : components) {
            projectedComponents.add(
                    context.declaration(
                            component,
                            new RecordComponentDeclaration(
                                    component.name().name(),
                                    JavaProjectionContext.qualify(
                                            qualified, component.name().name()),
                                    context.member(qualified, component.name().name()),
                                    context.annotations(component.modifiers()),
                                    context.type(
                                            component.type(),
                                            TypeReferenceRole.RECORD_COMPONENT_TYPE),
                                    component.kind() == JavaParameterKind.VARIABLE_ARITY,
                                    context.spelling.text(component),
                                    component.name().span(),
                                    component.span())));
            component.ellipsisAnnotations().forEach(context.references::annotation);
        }
        List<Declaration> members = new ArrayList<>();
        for (JavaEnumConstantNode constant : node.body().constants()) {
            members.addAll(JavaMemberProjection.project(context, constant, scope));
        }
        for (JavaDeclarationNode member : node.body().declarations()) {
            members.addAll(JavaMemberProjection.project(context, member, scope));
        }
        return context.declaration(
                node,
                new TypeDeclaration(
                        kind,
                        name,
                        qualified,
                        symbol,
                        context.modifiers(node.modifiers()),
                        annotations,
                        parameters,
                        extended,
                        implemented,
                        permitted,
                        projectedComponents,
                        members,
                        signature,
                        signatureSpan,
                        node.name().span(),
                        node.body().span(),
                        node.span(),
                        javaDoc));
    }

    private static DeclarationKind kind(JavaTypeDeclarationNode node) {
        return switch (node) {
            case JavaClassDeclarationNode ignored -> DeclarationKind.CLASS;
            case JavaInterfaceDeclarationNode ignored -> DeclarationKind.INTERFACE;
            case JavaEnumDeclarationNode ignored -> DeclarationKind.ENUM;
            case JavaRecordDeclarationNode ignored -> DeclarationKind.RECORD;
            case JavaAnnotationTypeDeclarationNode ignored -> DeclarationKind.ANNOTATION_TYPE;
            default ->
                    throw new IllegalArgumentException(
                            "No declaration projection for " + node.schema().kind().name());
        };
    }
}
