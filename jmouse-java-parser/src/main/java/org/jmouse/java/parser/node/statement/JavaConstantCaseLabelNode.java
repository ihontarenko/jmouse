package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed constant-case-label syntax, with role-owned children in source order. */
public final class JavaConstantCaseLabelNode extends JavaCaseLabelNode {
    public static final NodeKind<JavaConstantCaseLabelNode> KIND = new NodeKind<>(JavaNodeKindNames.CONSTANT_CASE_LABEL, JavaConstantCaseLabelNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaConstantCaseLabelNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaConstantCaseLabelNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

