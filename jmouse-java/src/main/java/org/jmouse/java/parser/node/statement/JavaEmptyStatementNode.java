package org.jmouse.java.parser.node.statement;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed empty-statement syntax, with role-owned children in source order. */
public final class JavaEmptyStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaEmptyStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.EMPTY_STATEMENT, JavaEmptyStatementNode.class);
    public static final NodeSchema<JavaEmptyStatementNode> SCHEMA = NodeSchema.of(KIND);

    public JavaEmptyStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

