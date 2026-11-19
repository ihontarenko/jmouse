package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed enhanced-for-statement syntax, with role-owned children in source order. */
public final class JavaEnhancedForStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaEnhancedForStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.ENHANCED_FOR_STATEMENT, JavaEnhancedForStatementNode.class);
    public static final ChildRole<JavaFormalParameterNode> VARIABLE = ChildRole.required(JavaChildRoleNames.VARIABLE, JavaFormalParameterNode.class);
    public static final ChildRole<JavaExpressionNode> ITERABLE = ChildRole.required(JavaChildRoleNames.ITERABLE, JavaExpressionNode.class);
    public static final ChildRole<JavaStatementNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaStatementNode.class);
    public static final NodeSchema<JavaEnhancedForStatementNode> SCHEMA = NodeSchema.of(KIND, VARIABLE, ITERABLE, BODY);

    public JavaEnhancedForStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaFormalParameterNode variable() {
        return child(VARIABLE);
    }

    public void setVariable(JavaFormalParameterNode node) {
        setChild(VARIABLE, node);
    }

    public JavaExpressionNode iterable() {
        return child(ITERABLE);
    }

    public void setIterable(JavaExpressionNode node) {
        setChild(ITERABLE, node);
    }

    public JavaStatementNode body() {
        return child(BODY);
    }

    public void setBody(JavaStatementNode node) {
        setChild(BODY, node);
    }
}

