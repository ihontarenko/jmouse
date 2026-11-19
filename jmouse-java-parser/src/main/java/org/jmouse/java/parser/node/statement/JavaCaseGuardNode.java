package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed case-guard syntax, with role-owned children in source order. */
public final class JavaCaseGuardNode extends JavaSyntaxNode {
    public static final NodeKind<JavaCaseGuardNode> KIND = new NodeKind<>(JavaNodeKindNames.CASE_GUARD, JavaCaseGuardNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.required(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final NodeSchema<JavaCaseGuardNode> SCHEMA = NodeSchema.of(KIND, CONDITION);

    public JavaCaseGuardNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode condition() {
        return child(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }
}

