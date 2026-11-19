package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed return-statement syntax, with role-owned children in source order. */
public final class JavaReturnStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaReturnStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.RETURN_STATEMENT, JavaReturnStatementNode.class);
    public static final ChildRole<JavaExpressionNode> EXPRESSION = ChildRole.optional(JavaChildRoleNames.EXPRESSION, JavaExpressionNode.class);
    public static final NodeSchema<JavaReturnStatementNode> SCHEMA = NodeSchema.of(KIND, EXPRESSION);

    public JavaReturnStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaExpressionNode> expression() {
        return optionalChild(EXPRESSION);
    }

    public void setExpression(JavaExpressionNode node) {
        setChild(EXPRESSION, node);
    }
}

