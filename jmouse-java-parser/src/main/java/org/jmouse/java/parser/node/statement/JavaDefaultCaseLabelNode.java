package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed default-case-label syntax, with role-owned children in source order. */
public final class JavaDefaultCaseLabelNode extends JavaCaseLabelNode {
    public static final NodeKind<JavaDefaultCaseLabelNode> KIND = new NodeKind<>(JavaNodeKindNames.DEFAULT_CASE_LABEL, JavaDefaultCaseLabelNode.class);
    public static final NodeSchema<JavaDefaultCaseLabelNode> SCHEMA = NodeSchema.of(KIND);

    public JavaDefaultCaseLabelNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

