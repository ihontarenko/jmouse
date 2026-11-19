package org.jmouse.java.parser.node.parameter;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed inferred-lambda-parameter syntax, with role-owned children in source order. */
public final class JavaInferredLambdaParameterNode extends JavaParameterNode {
    public static final NodeKind<JavaInferredLambdaParameterNode> KIND = new NodeKind<>(JavaNodeKindNames.INFERRED_LAMBDA_PARAMETER, JavaInferredLambdaParameterNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final NodeSchema<JavaInferredLambdaParameterNode> SCHEMA = NodeSchema.of(KIND, NAME);

    public JavaInferredLambdaParameterNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }
}

