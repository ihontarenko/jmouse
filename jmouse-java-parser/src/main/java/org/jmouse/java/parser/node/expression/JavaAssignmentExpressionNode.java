package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed assignment-expression syntax, with role-owned children in source order. */
public final class JavaAssignmentExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaAssignmentExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.ASSIGNMENT_EXPRESSION, JavaAssignmentExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> TARGET = ChildRole.required(JavaChildRoleNames.TARGET, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> VALUE = ChildRole.required(JavaChildRoleNames.VALUE, JavaExpressionNode.class);
    public static final NodeSchema<JavaAssignmentExpressionNode> SCHEMA = NodeSchema.of(KIND, TARGET, VALUE);
    private JavaAssignmentOperator operator;

    public JavaAssignmentExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode target() {
        return child(TARGET);
    }

    public void setTarget(JavaExpressionNode node) {
        setChild(TARGET, node);
    }

    public JavaExpressionNode value() {
        return child(VALUE);
    }

    public void setValue(JavaExpressionNode node) {
        setChild(VALUE, node);
    }

    public JavaAssignmentOperator operator() {
        return operator;
    }

    public void setOperator(JavaAssignmentOperator value) {
        requireBuilding();
        this.operator = Objects.requireNonNull(value, "operator");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(operator, "operator");
    }
}

