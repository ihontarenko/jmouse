package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed synchronized-statement syntax, with role-owned children in source order. */
public final class JavaSynchronizedStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaSynchronizedStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.SYNCHRONIZED_STATEMENT, JavaSynchronizedStatementNode.class);
    public static final ChildRole<JavaExpressionNode> MONITOR = ChildRole.required(JavaChildRoleNames.MONITOR, JavaExpressionNode.class);
    public static final ChildRole<JavaBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaBlockNode.class);
    public static final NodeSchema<JavaSynchronizedStatementNode> SCHEMA = NodeSchema.of(KIND, MONITOR, BODY);

    public JavaSynchronizedStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode monitor() {
        return child(MONITOR);
    }

    public void setMonitor(JavaExpressionNode node) {
        setChild(MONITOR, node);
    }

    public JavaBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaBlockNode node) {
        setChild(BODY, node);
    }
}

