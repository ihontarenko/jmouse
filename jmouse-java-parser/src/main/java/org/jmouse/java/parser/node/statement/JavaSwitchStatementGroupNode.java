package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed switch-statement-group syntax, with role-owned children in source order. */
public final class JavaSwitchStatementGroupNode extends JavaSyntaxNode {
    public static final NodeKind<JavaSwitchStatementGroupNode> KIND = new NodeKind<>(JavaNodeKindNames.SWITCH_STATEMENT_GROUP, JavaSwitchStatementGroupNode.class);
    public static final ChildRole<JavaCaseLabelNode> LABELS = ChildRole.list(JavaChildRoleNames.LABELS, JavaCaseLabelNode.class);
    public static final ChildRole<JavaSyntaxNode> STATEMENTS = ChildRole.list(JavaChildRoleNames.STATEMENTS, JavaSyntaxNode.class);
    public static final NodeSchema<JavaSwitchStatementGroupNode> SCHEMA = NodeSchema.of(KIND, LABELS, STATEMENTS);

    public JavaSwitchStatementGroupNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaCaseLabelNode> labels() {
        return children(LABELS);
    }

    public void addLabel(JavaCaseLabelNode node) {
        addChild(LABELS, node);
    }

    public List<JavaSyntaxNode> statements() {
        return children(STATEMENTS);
    }

    public void addStatement(JavaSyntaxNode node) {
        addChild(STATEMENTS, node);
    }
}

