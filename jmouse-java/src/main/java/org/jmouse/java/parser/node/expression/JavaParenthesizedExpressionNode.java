package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed parenthesized-expression syntax, with role-owned children in source order. */
public final class JavaParenthesizedExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaParenthesizedExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.PARENTHESIZED_EXPRESSION, JavaParenthesizedExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaParenthesizedExpressionNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaParenthesizedExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

