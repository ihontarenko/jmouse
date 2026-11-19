package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed expression-lambda-body syntax, with role-owned children in source order. */
public final class JavaExpressionLambdaBodyNode extends JavaLambdaBodyNode {
    public static final NodeKind<JavaExpressionLambdaBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.EXPRESSION_LAMBDA_BODY, JavaExpressionLambdaBodyNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaExpressionLambdaBodyNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaExpressionLambdaBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

