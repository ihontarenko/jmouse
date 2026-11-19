package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed labeled-statement syntax, with role-owned children in source order. */
public final class JavaLabeledStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaLabeledStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.LABELED_STATEMENT, JavaLabeledStatementNode.class);
    public static final ChildRole<JavaIdentifierNode> LABEL = ChildRole.required(JavaChildRoleNames.LABEL, JavaIdentifierNode.class);
    public static final ChildRole<JavaStatementNode> STATEMENT = ChildRole.required(JavaChildRoleNames.STATEMENT, JavaStatementNode.class);
    public static final NodeSchema<JavaLabeledStatementNode> SCHEMA = NodeSchema.of(KIND, LABEL, STATEMENT);

    public JavaLabeledStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaIdentifierNode label() {
        return child(LABEL);
    }

    public void setLabel(JavaIdentifierNode node) {
        setChild(LABEL, node);
    }

    public JavaStatementNode statement() {
        return child(STATEMENT);
    }

    public void setStatement(JavaStatementNode node) {
        setChild(STATEMENT, node);
    }
}

