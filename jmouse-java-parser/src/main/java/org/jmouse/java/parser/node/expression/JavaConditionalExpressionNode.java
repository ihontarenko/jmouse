package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed conditional-expression syntax, with role-owned children in source order. */
public final class JavaConditionalExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaConditionalExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.CONDITIONAL_EXPRESSION, JavaConditionalExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> THEN_EXPRESSION = ChildRole.required(JavaChildRoleNames.THEN_EXPRESSION, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> ELSE_EXPRESSION = ChildRole.required(JavaChildRoleNames.ELSE_EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaConditionalExpressionNode> SCHEMA = NodeSchema.of(KIND, CONDITION, THEN_EXPRESSION, ELSE_EXPRESSION);

    public JavaConditionalExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }

    public JavaExpressionNode thenExpression() {
        return child(THEN_EXPRESSION);
    }

    public void setThenExpression(JavaExpressionNode node) {
        setChild(THEN_EXPRESSION, node);
    }

    public JavaExpressionNode elseExpression() {
        return child(ELSE_EXPRESSION);
    }

    public void setElseExpression(JavaExpressionNode node) {
        setChild(ELSE_EXPRESSION, node);
    }
}

