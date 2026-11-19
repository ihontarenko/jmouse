package org.jmouse.java.parser.node.expression;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed class-instance-creation-expression syntax, with role-owned children in source order. */
public final class JavaClassInstanceCreationExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaClassInstanceCreationExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.CLASS_INSTANCE_CREATION_EXPRESSION, JavaClassInstanceCreationExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> QUALIFIER = ChildRole.optional(JavaChildRoleNames.QUALIFIER, JavaExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE_ARGUMENTS = ChildRole.list(JavaChildRoleNames.TYPE_ARGUMENTS, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaExpressionNode> ARGUMENTS = ChildRole.list(JavaChildRoleNames.ARGUMENTS, JavaExpressionNode.class);
    public static final ChildRole<JavaAnonymousClassBodyNode> BODY = ChildRole.optional(JavaChildRoleNames.BODY, JavaAnonymousClassBodyNode.class);
    public static final NodeSchema<JavaClassInstanceCreationExpressionNode> SCHEMA = NodeSchema.of(KIND, QUALIFIER, TYPE_ARGUMENTS, TYPE, ARGUMENTS, BODY);

    public JavaClassInstanceCreationExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaExpressionNode> qualifier() {
        return optionalChild(QUALIFIER);
    }

    public void setQualifier(JavaExpressionNode node) {
        setChild(QUALIFIER, node);
    }

    public List<JavaTypeSyntaxNode> typeArguments() {
        return children(TYPE_ARGUMENTS);
    }

    public void addTypeArgument(JavaTypeSyntaxNode node) {
        addChild(TYPE_ARGUMENTS, node);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
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

