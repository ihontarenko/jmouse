package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed cast-expression syntax, with role-owned children in source order. */
public final class JavaCastExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaCastExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.CAST_EXPRESSION, JavaCastExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaExpressionNode> OPERAND = ChildRole.required(JavaChildRoleNames.OPERAND, JavaExpressionNode.class);
    public static final NodeSchema<JavaCastExpressionNode> SCHEMA = NodeSchema.of(KIND, TYPE, OPERAND);

    public JavaCastExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public JavaExpressionNode operand() {
        return child(OPERAND);
    }

    public void setOperand(JavaExpressionNode node) {
        setChild(OPERAND, node);
    }
}

