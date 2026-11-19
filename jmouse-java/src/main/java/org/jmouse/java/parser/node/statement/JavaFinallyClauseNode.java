package org.jmouse.java.parser.node.statement;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed finally-clause syntax, with role-owned children in source order. */
public final class JavaFinallyClauseNode extends JavaSyntaxNode {
    public static final NodeKind<JavaFinallyClauseNode> KIND = new NodeKind<>(JavaNodeKindNames.FINALLY_CLAUSE, JavaFinallyClauseNode.class);
    public static final ChildRole<JavaBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaBlockNode.class);
    public static final NodeSchema<JavaFinallyClauseNode> SCHEMA = NodeSchema.of(KIND, BODY);

    public JavaFinallyClauseNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaBlockNode node) {
        setChild(BODY, node);
    }
}

