package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed do-while-statement syntax, with role-owned children in source order. */
public final class JavaDoWhileStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaDoWhileStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.DO_WHILE_STATEMENT, JavaDoWhileStatementNode.class);
    public static final ChildRole<JavaStatementNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaStatementNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final NodeSchema<JavaDoWhileStatementNode> SCHEMA = NodeSchema.of(KIND, BODY, CONDITION);

    public JavaDoWhileStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaStatementNode body() {
        return child(BODY);
    }

    public void setBody(JavaStatementNode node) {
        setChild(BODY, node);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }
}

