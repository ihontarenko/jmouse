package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed class-literal-expression syntax, with role-owned children in source order. */
public final class JavaClassLiteralExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaClassLiteralExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.CLASS_LITERAL_EXPRESSION, JavaClassLiteralExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaClassLiteralExpressionNode> SCHEMA = NodeSchema.of(KIND, TYPE);

    public JavaClassLiteralExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }
}

