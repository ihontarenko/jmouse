package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed assert-statement syntax, with role-owned children in source order. */
public final class JavaAssertStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaAssertStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.ASSERT_STATEMENT, JavaAssertStatementNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> DETAIL = ChildRole.optional(JavaChildRoleNames.DETAIL, JavaExpressionNode.class);
    public static final NodeSchema<JavaAssertStatementNode> SCHEMA = NodeSchema.of(KIND, CONDITION, DETAIL);

    public JavaAssertStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }

    public Optional<JavaExpressionNode> detail() {
        return optionalChild(DETAIL);
    }

    public void setDetail(JavaExpressionNode node) {
        setChild(DETAIL, node);
    }
}

