package org.jmouse.java.parser.node.expression;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed lambda-expression syntax, with role-owned children in source order. */
public final class JavaLambdaExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaLambdaExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.LAMBDA_EXPRESSION, JavaLambdaExpressionNode.class);
    public static final ChildRole<JavaParameterNode> PARAMETERS = ChildRole.list(JavaChildRoleNames.PARAMETERS, JavaParameterNode.class);
    public static final ChildRole<JavaLambdaBodyNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaLambdaBodyNode.class);
    public static final NodeSchema<JavaLambdaExpressionNode> SCHEMA = NodeSchema.of(KIND, PARAMETERS, BODY);

    public JavaLambdaExpressionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaParameterNode> parameters() {
        return children(PARAMETERS);
    }

    public void addParameter(JavaParameterNode node) {
        addChild(PARAMETERS, node);
    }

    public JavaLambdaBodyNode body() {
        return child(BODY);
    }

    public void setBody(JavaLambdaBodyNode node) {
        setChild(BODY, node);
    }
}

