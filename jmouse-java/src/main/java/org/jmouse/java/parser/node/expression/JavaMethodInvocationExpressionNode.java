package org.jmouse.java.parser.node.expression;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed method-invocation-expression syntax, with role-owned children in source order. */
public final class JavaMethodInvocationExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaMethodInvocationExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.METHOD_INVOCATION_EXPRESSION, JavaMethodInvocationExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> RECEIVER = ChildRole.optional(JavaChildRoleNames.RECEIVER, JavaExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE_ARGUMENTS = ChildRole.list(JavaChildRoleNames.TYPE_ARGUMENTS, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaExpressionNode> ARGUMENTS = ChildRole.list(JavaChildRoleNames.ARGUMENTS, JavaExpressionNode.class);
    public static final NodeSchema<JavaMethodInvocationExpressionNode> SCHEMA = NodeSchema.of(KIND, RECEIVER, TYPE_ARGUMENTS, NAME, ARGUMENTS);

    public JavaMethodInvocationExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaExpressionNode> receiver() {
        return optionalChild(RECEIVER);
    }

    public void setReceiver(JavaExpressionNode node) {
        setChild(RECEIVER, node);
    }

    public List<JavaTypeSyntaxNode> typeArguments() {
        return children(TYPE_ARGUMENTS);
    }

    public void addTypeArgument(JavaTypeSyntaxNode node) {
        addChild(TYPE_ARGUMENTS, node);
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
}

