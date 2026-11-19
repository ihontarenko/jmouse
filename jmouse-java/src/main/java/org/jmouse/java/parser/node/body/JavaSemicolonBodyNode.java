package org.jmouse.java.parser.node.body;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed semicolon-body syntax, with role-owned children in source order. */
public final class JavaSemicolonBodyNode extends JavaSyntaxNode {
    public static final NodeKind<JavaSemicolonBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.SEMICOLON_BODY, JavaSemicolonBodyNode.class);
    public static final NodeSchema<JavaSemicolonBodyNode> SCHEMA = NodeSchema.of(KIND);

    public JavaSemicolonBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

