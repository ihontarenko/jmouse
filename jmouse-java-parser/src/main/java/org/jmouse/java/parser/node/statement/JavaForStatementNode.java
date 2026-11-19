package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed for-statement syntax, with role-owned children in source order. */
public final class JavaForStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaForStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.FOR_STATEMENT, JavaForStatementNode.class);
    public static final ChildRole<JavaSyntaxNode> INITIALIZERS = ChildRole.list(JavaChildRoleNames.INITIALIZERS, JavaSyntaxNode.class);
    public static final ChildRole<JavaExpressionNode> CONDITION = ChildRole.optional(JavaChildRoleNames.CONDITION, JavaExpressionNode.class);
    public static final ChildRole<JavaExpressionNode> UPDATES = ChildRole.list(JavaChildRoleNames.UPDATES, JavaExpressionNode.class);
    public static final ChildRole<JavaStatementNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaStatementNode.class);
    public static final NodeSchema<JavaForStatementNode> SCHEMA = NodeSchema.of(KIND, INITIALIZERS, CONDITION, UPDATES, BODY);

    public JavaForStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaSyntaxNode> initializers() {
        return children(INITIALIZERS);
    }

    public void addInitializer(JavaSyntaxNode node) {
        addChild(INITIALIZERS, node);
    }

    public Optional<JavaExpressionNode> condition() {
        return optionalChild(CONDITION);
    }

    public void setCondition(JavaExpressionNode node) {
        setChild(CONDITION, node);
    }

    public List<JavaExpressionNode> updates() {
        return children(UPDATES);
    }

    public void addUpdate(JavaExpressionNode node) {
        addChild(UPDATES, node);
    }

    public JavaStatementNode body() {
        return child(BODY);
    }

    public void setBody(JavaStatementNode node) {
        setChild(BODY, node);
    }
}

