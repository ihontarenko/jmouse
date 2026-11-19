package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed switch-statement syntax, with role-owned children in source order. */
public final class JavaSwitchStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaSwitchStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.SWITCH_STATEMENT, JavaSwitchStatementNode.class);
    public static final ChildRole<JavaExpressionNode> SELECTOR = ChildRole.required(JavaChildRoleNames.SELECTOR, JavaExpressionNode.class);
    public static final ChildRole<JavaSwitchBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaSwitchBlockNode.class);
    public static final NodeSchema<JavaSwitchStatementNode> SCHEMA = NodeSchema.of(KIND, SELECTOR, BODY);

    public JavaSwitchStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode selector() {
        return child(SELECTOR);
    }

    public void setSelector(JavaExpressionNode node) {
        setChild(SELECTOR, node);
    }

    public JavaSwitchBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaSwitchBlockNode node) {
        setChild(BODY, node);
    }
}

