package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed block-lambda-body syntax, with role-owned children in source order. */
public final class JavaBlockLambdaBodyNode extends JavaLambdaBodyNode {
    public static final NodeKind<JavaBlockLambdaBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.BLOCK_LAMBDA_BODY, JavaBlockLambdaBodyNode.class);
    public static final ChildRole<JavaBlockNode> BLOCK = ChildRole.required(JavaChildRoleNames.BLOCK, JavaBlockNode.class);
    public static final NodeSchema<JavaBlockLambdaBodyNode> SCHEMA = NodeSchema.of(KIND, BLOCK);

    public JavaBlockLambdaBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaBlockNode block() {
        return child(BLOCK);
    }

    public void setBlock(JavaBlockNode node) {
        setChild(BLOCK, node);
    }
}

