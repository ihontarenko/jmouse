package org.jmouse.java.parser.java27;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.source.SourceSpan;

public final class Java27InspectNode extends JavaStatementNode {
    public static final NodeKind<Java27InspectNode> KIND =
            new NodeKind<>(Java27Names.INSPECT_NODE, Java27InspectNode.class);
    public static final ChildRole<JavaExpressionNode> CHILD =
            ChildRole.required(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<Java27InspectNode> SCHEMA = NodeSchema.of(KIND, CHILD);

    public Java27InspectNode(String name, SourceSpan span) {
        super(SCHEMA, name, span);
    }

    public JavaExpressionNode expression() {
        return child(CHILD);
    }

    public void setExpression(JavaExpressionNode value) {
        setChild(CHILD, value);
    }
}
