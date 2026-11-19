package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed postfix-expression syntax, with role-owned children in source order. */
public final class JavaPostfixExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaPostfixExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.POSTFIX_EXPRESSION, JavaPostfixExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> OPERAND = ChildRole.required(JavaChildRoleNames.OPERAND, JavaExpressionNode.class);
    public static final NodeSchema<JavaPostfixExpressionNode> SCHEMA = NodeSchema.of(KIND, OPERAND);
    private JavaUnaryOperator operator;

    public JavaPostfixExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode operand() {
        return child(OPERAND);
    }

    public void setOperand(JavaExpressionNode node) {
        setChild(OPERAND, node);
    }

    public JavaUnaryOperator operator() {
        return operator;
    }

    public void setOperator(JavaUnaryOperator value) {
        requireBuilding();
        this.operator = Objects.requireNonNull(value, "operator");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(operator, "operator");
    }
}

