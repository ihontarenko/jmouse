package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed inferred-type syntax, with role-owned children in source order. */
public final class JavaInferredTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaInferredTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.INFERRED_TYPE, JavaInferredTypeNode.class);
    public static final NodeSchema<JavaInferredTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaInferredTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

