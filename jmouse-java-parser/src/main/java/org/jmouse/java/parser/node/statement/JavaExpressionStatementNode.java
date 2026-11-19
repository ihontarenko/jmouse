package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed expression-statement syntax, with role-owned children in source order. */
public final class JavaExpressionStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaExpressionStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.EXPRESSION_STATEMENT, JavaExpressionStatementNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaExpressionStatementNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaExpressionStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

