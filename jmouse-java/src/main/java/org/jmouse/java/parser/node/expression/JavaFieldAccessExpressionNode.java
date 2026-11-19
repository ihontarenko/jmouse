package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed field-access-expression syntax, with role-owned children in source order. */
public final class JavaFieldAccessExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaFieldAccessExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.FIELD_ACCESS_EXPRESSION, JavaFieldAccessExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> RECEIVER = ChildRole.required(JavaChildRoleNames.RECEIVER, JavaExpressionNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final NodeSchema<JavaFieldAccessExpressionNode> SCHEMA = NodeSchema.of(KIND, RECEIVER, NAME);

    public JavaFieldAccessExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode receiver() {
        return child(RECEIVER);
    }

    public void setReceiver(JavaExpressionNode node) {
        setChild(RECEIVER, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }
}

