package org.jmouse.java.parser.extension.fixture;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.source.SourceSpan;

final class FixtureTraceNode extends JavaStatementNode {
    static final NodeKind<FixtureTraceNode> KIND = new NodeKind<>(FixtureNames.TRACE_KIND, FixtureTraceNode.class);
    static final ChildRole<JavaExpressionNode> EXPRESSION =
            ChildRole.required(FixtureNames.EXPRESSION_CHILD, JavaExpressionNode.class);
    static final NodeSchema<FixtureTraceNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    FixtureTraceNode(String name, SourceSpan span) {
        super(SCHEMA, name, span);
    }

    JavaExpressionNode expression() {
        return child(EXPRESSION);
    }

    void setExpression(JavaExpressionNode expression) {
        setChild(EXPRESSION, expression);
    }
}
