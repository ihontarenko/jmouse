package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed diamond-type-arguments syntax, with role-owned children in source order. */
public final class JavaDiamondTypeArgumentsNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaDiamondTypeArgumentsNode> KIND = new NodeKind<>(JavaNodeKindNames.DIAMOND_TYPE_ARGUMENTS, JavaDiamondTypeArgumentsNode.class);
    public static final NodeSchema<JavaDiamondTypeArgumentsNode> SCHEMA = NodeSchema.of(KIND);

    public JavaDiamondTypeArgumentsNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

