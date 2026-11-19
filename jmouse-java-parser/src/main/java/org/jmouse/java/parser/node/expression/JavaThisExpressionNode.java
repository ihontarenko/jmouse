package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed this-expression syntax, with role-owned children in source order. */
public final class JavaThisExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaThisExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.THIS_EXPRESSION, JavaThisExpressionNode.class);
    public static final ChildRole<JavaQualifiedNameNode> QUALIFIER = ChildRole.optional(JavaChildRoleNames.QUALIFIER, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaThisExpressionNode> SCHEMA = NodeSchema.of(KIND, QUALIFIER);

    public JavaThisExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaQualifiedNameNode> qualifier() {
        return optionalChild(QUALIFIER);
    }

    public void setQualifier(JavaQualifiedNameNode node) {
        setChild(QUALIFIER, node);
    }
}

