package org.jmouse.java.parser.node.statement;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed if-statement syntax, with role-owned children in source order. */
public final class JavaIfStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaIfStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.IF_STATEMENT, JavaIfStatementNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final ChildRole<JavaStatementNode> THEN_STATEMENT = ChildRole.required(JavaChildRoleNames.THEN_STATEMENT, JavaStatementNode.class);
    public static final ChildRole<JavaStatementNode> ELSE_STATEMENT = ChildRole.optional(JavaChildRoleNames.ELSE_STATEMENT, JavaStatementNode.class);
    public static final NodeSchema<JavaIfStatementNode> SCHEMA = NodeSchema.of(KIND, CONDITION, THEN_STATEMENT, ELSE_STATEMENT);

    public JavaIfStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }

    public JavaStatementNode thenStatement() {
        return child(THEN_STATEMENT);
    }

    public void setThenStatement(JavaStatementNode node) {
        setChild(THEN_STATEMENT, node);
    }

    public Optional<JavaStatementNode> elseStatement() {
        return optionalChild(ELSE_STATEMENT);
    }

    public void setElseStatement(JavaStatementNode node) {
        setChild(ELSE_STATEMENT, node);
    }
}

