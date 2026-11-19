package org.jmouse.java.parser.node.declaration;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed enum-constant syntax, with role-owned children in source order. */
public final class JavaEnumConstantNode extends JavaDeclarationNode {
    public static final NodeKind<JavaEnumConstantNode> KIND = new NodeKind<>(JavaNodeKindNames.ENUM_CONSTANT, JavaEnumConstantNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaExpressionNode> ARGUMENTS = ChildRole.list(JavaChildRoleNames.ARGUMENTS, JavaExpressionNode.class);
    public static final ChildRole<JavaAnonymousClassBodyNode> BODY = ChildRole.optional(JavaChildRoleNames.BODY, JavaAnonymousClassBodyNode.class);
    public static final NodeSchema<JavaEnumConstantNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, NAME, ARGUMENTS, BODY);

    public JavaEnumConstantNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public List<JavaExpressionNode> arguments() {
        return children(ARGUMENTS);
    }

    public void addArgument(JavaExpressionNode node) {
        addChild(ARGUMENTS, node);
    }

    public Optional<JavaAnonymousClassBodyNode> body() {
        return optionalChild(BODY);
    }

    public void setBody(JavaAnonymousClassBodyNode node) {
        setChild(BODY, node);
    }
}

