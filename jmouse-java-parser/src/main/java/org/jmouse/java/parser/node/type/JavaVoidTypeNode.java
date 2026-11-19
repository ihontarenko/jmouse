package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed void-type syntax, with role-owned children in source order. */
public final class JavaVoidTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaVoidTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.VOID_TYPE, JavaVoidTypeNode.class);
    public static final NodeSchema<JavaVoidTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaVoidTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

