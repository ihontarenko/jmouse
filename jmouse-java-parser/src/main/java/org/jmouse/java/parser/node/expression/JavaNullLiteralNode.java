package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed null-literal syntax, with role-owned children in source order. */
public final class JavaNullLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaNullLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.NULL_LITERAL, JavaNullLiteralNode.class);
    public static final NodeSchema<JavaNullLiteralNode> SCHEMA = NodeSchema.of(KIND);

    public JavaNullLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

