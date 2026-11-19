package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed block syntax, with role-owned children in source order. */
public final class JavaBlockNode extends JavaStatementNode {
    public static final NodeKind<JavaBlockNode> KIND = new NodeKind<>(JavaNodeKindNames.BLOCK, JavaBlockNode.class);
    public static final ChildRole<JavaSyntaxNode> STATEMENTS = ChildRole.list(JavaChildRoleNames.STATEMENTS, JavaSyntaxNode.class);
    public static final NodeSchema<JavaBlockNode> SCHEMA = NodeSchema.of(KIND, STATEMENTS);

    public JavaBlockNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaSyntaxNode> statements() {
        return children(STATEMENTS);
    }

    public void addStatement(JavaSyntaxNode node) {
        addChild(STATEMENTS, node);
    }
}

