package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed name-expression syntax, with role-owned children in source order. */
public final class JavaNameExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaNameExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.NAME_EXPRESSION, JavaNameExpressionNode.class);
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaNameExpressionNode> SCHEMA = NodeSchema.of(KIND, NAME);

    public JavaNameExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public void setName(JavaQualifiedNameNode node) {
        setChild(NAME, node);
    }
}

