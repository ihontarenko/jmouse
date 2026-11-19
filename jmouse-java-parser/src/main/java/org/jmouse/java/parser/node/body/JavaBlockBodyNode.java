package org.jmouse.java.parser.node.body;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed block-body syntax, with role-owned children in source order. */
public final class JavaBlockBodyNode extends JavaSyntaxNode {
    public static final NodeKind<JavaBlockBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.BLOCK_BODY, JavaBlockBodyNode.class);
    public static final ChildRole<JavaBlockNode> BLOCK = ChildRole.required(JavaChildRoleNames.BLOCK, JavaBlockNode.class);
    public static final NodeSchema<JavaBlockBodyNode> SCHEMA = NodeSchema.of(KIND, BLOCK);

    public JavaBlockBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaBlockNode block() {
        return child(BLOCK);
    }

    public void setBlock(JavaBlockNode node) {
        setChild(BLOCK, node);
    }
}

