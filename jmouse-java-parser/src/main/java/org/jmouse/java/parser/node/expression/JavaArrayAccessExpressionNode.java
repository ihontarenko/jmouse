package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed array-access-expression syntax, with role-owned children in source order. */
public final class JavaArrayAccessExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaArrayAccessExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.ARRAY_ACCESS_EXPRESSION, JavaArrayAccessExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> RECEIVER = ChildRole.required(JavaChildRoleNames.RECEIVER, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> INDEX = ChildRole.required(JavaChildRoleNames.INDEX, JavaExpressionNode.class);
    public static final NodeSchema<JavaArrayAccessExpressionNode> SCHEMA = NodeSchema.of(KIND, RECEIVER, INDEX);

    public JavaArrayAccessExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode receiver() {
        return child(RECEIVER);
    }

    public void setReceiver(JavaExpressionNode node) {
        setChild(RECEIVER, node);
    }

    public JavaExpressionNode index() {
        return child(INDEX);
    }

    public void setIndex(JavaExpressionNode node) {
        setChild(INDEX, node);
    }
}

