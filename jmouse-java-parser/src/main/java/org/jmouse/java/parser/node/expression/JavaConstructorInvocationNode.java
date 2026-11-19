package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed constructor-invocation syntax, with role-owned children in source order. */
public final class JavaConstructorInvocationNode extends JavaStatementNode {
    public static final NodeKind<JavaConstructorInvocationNode> KIND = new NodeKind<>(JavaNodeKindNames.CONSTRUCTOR_INVOCATION, JavaConstructorInvocationNode.class);
    public static final ChildRole<JavaExpressionNode> QUALIFIER = ChildRole.optional(JavaChildRoleNames.QUALIFIER, JavaExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE_ARGUMENTS = ChildRole.list(JavaChildRoleNames.TYPE_ARGUMENTS, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaExpressionNode> ARGUMENTS = ChildRole.list(JavaChildRoleNames.ARGUMENTS, JavaExpressionNode.class);
    public static final NodeSchema<JavaConstructorInvocationNode> SCHEMA = NodeSchema.of(KIND, QUALIFIER, TYPE_ARGUMENTS, ARGUMENTS);
    private JavaConstructorInvocationKind kind;

    public JavaConstructorInvocationNode(String sourceName, SourceSpan span) {
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

    public List<JavaExpressionNode> arguments() {
        return children(ARGUMENTS);
    }

    public void addArgument(JavaExpressionNode node) {
        addChild(ARGUMENTS, node);
    }

    public JavaConstructorInvocationKind kind() {
        return kind;
    }

    public void setKind(JavaConstructorInvocationKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

