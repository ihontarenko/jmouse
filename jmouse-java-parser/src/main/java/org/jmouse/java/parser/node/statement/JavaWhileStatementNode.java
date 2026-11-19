package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed while-statement syntax, with role-owned children in source order. */
public final class JavaWhileStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaWhileStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.WHILE_STATEMENT, JavaWhileStatementNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final ChildRole<JavaStatementNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaStatementNode.class);
    public static final NodeSchema<JavaWhileStatementNode> SCHEMA = NodeSchema.of(KIND, CONDITION, BODY);

    public JavaWhileStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }

    public JavaStatementNode body() {
        return child(BODY);
    }

    public void setBody(JavaStatementNode node) {
        setChild(BODY, node);
    }
}

