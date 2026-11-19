package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed array-initializer syntax, with role-owned children in source order. */
public final class JavaArrayInitializerNode extends JavaExpressionNode {
    public static final NodeKind<JavaArrayInitializerNode> KIND = new NodeKind<>(JavaNodeKindNames.ARRAY_INITIALIZER, JavaArrayInitializerNode.class);
    public static final ChildRole<JavaExpressionNode> ELEMENTS = ChildRole.list(JavaChildRoleNames.ELEMENTS, JavaExpressionNode.class);
    public static final NodeSchema<JavaArrayInitializerNode> SCHEMA = NodeSchema.of(KIND, ELEMENTS);

    public JavaArrayInitializerNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaExpressionNode> elements() {
        return children(ELEMENTS);
    }

    public void addElement(JavaExpressionNode node) {
        addChild(ELEMENTS, node);
    }
}

