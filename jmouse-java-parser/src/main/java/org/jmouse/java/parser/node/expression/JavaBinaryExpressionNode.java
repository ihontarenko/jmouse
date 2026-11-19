package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed binary-expression syntax, with role-owned children in source order. */
public final class JavaBinaryExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaBinaryExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.BINARY_EXPRESSION, JavaBinaryExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> LEFT = ChildRole.required(JavaChildRoleNames.LEFT, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> RIGHT = ChildRole.required(JavaChildRoleNames.RIGHT, JavaExpressionNode.class);
    public static final NodeSchema<JavaBinaryExpressionNode> SCHEMA = NodeSchema.of(KIND, LEFT, RIGHT);
    private JavaBinaryOperator operator;

    public JavaBinaryExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode left() {
        return child(LEFT);
    }

    public void setLeft(JavaExpressionNode node) {
        setChild(LEFT, node);
    }

    public JavaExpressionNode right() {
        return child(RIGHT);
    }

    public void setRight(JavaExpressionNode node) {
        setChild(RIGHT, node);
    }

    public JavaBinaryOperator operator() {
        return operator;
    }

    public void setOperator(JavaBinaryOperator value) {
        requireBuilding();
        this.operator = Objects.requireNonNull(value, "operator");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(operator, "operator");
    }
}

