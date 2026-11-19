package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed null-case-label syntax, with role-owned children in source order. */
public final class JavaNullCaseLabelNode extends JavaCaseLabelNode {
    public static final NodeKind<JavaNullCaseLabelNode> KIND = new NodeKind<>(JavaNodeKindNames.NULL_CASE_LABEL, JavaNullCaseLabelNode.class);
    public static final NodeSchema<JavaNullCaseLabelNode> SCHEMA = NodeSchema.of(KIND);

    public JavaNullCaseLabelNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

