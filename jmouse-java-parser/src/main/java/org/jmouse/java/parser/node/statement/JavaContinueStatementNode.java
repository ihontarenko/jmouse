package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed continue-statement syntax, with role-owned children in source order. */
public final class JavaContinueStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaContinueStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.CONTINUE_STATEMENT, JavaContinueStatementNode.class);
    public static final ChildRole<JavaIdentifierNode> LABEL = ChildRole.optional(JavaChildRoleNames.LABEL, JavaIdentifierNode.class);
    public static final NodeSchema<JavaContinueStatementNode> SCHEMA = NodeSchema.of(KIND, LABEL);

    public JavaContinueStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaIdentifierNode> label() {
        return optionalChild(LABEL);
    }

    public void setLabel(JavaIdentifierNode node) {
        setChild(LABEL, node);
    }
}

