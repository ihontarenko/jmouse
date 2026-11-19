package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed throw-statement syntax, with role-owned children in source order. */
public final class JavaThrowStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaThrowStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.THROW_STATEMENT, JavaThrowStatementNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaThrowStatementNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaThrowStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

