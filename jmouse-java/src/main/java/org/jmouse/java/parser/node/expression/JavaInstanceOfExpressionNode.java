package org.jmouse.java.parser.node.expression;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.pattern.JavaPatternNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed instance-of-expression syntax, with role-owned children in source order. */
public final class JavaInstanceOfExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaInstanceOfExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.INSTANCE_OF_EXPRESSION, JavaInstanceOfExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.optional(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaPatternNode> PATTERN = ChildRole.optional(JavaChildRoleNames.PATTERN, JavaPatternNode.class);
    public static final NodeSchema<JavaInstanceOfExpressionNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION, TYPE, PATTERN);

    public JavaInstanceOfExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }

    public Optional<JavaTypeSyntaxNode> type() {
        return optionalChild(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public Optional<JavaPatternNode> pattern() {
        return optionalChild(PATTERN);
    }

    public void setPattern(JavaPatternNode node) {
        setChild(PATTERN, node);
    }
}

