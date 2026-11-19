package org.jmouse.java.parser.node.statement;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed break-statement syntax, with role-owned children in source order. */
public final class JavaBreakStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaBreakStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.BREAK_STATEMENT, JavaBreakStatementNode.class);
    public static final ChildRole<JavaIdentifierNode> LABEL = ChildRole.optional(JavaChildRoleNames.LABEL, JavaIdentifierNode.class);
    public static final NodeSchema<JavaBreakStatementNode> SCHEMA = NodeSchema.of(KIND, LABEL);

    public JavaBreakStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaIdentifierNode> label() {
        return optionalChild(LABEL);
    }

    public void setLabel(JavaIdentifierNode node) {
        setChild(LABEL, node);
    }
}

