package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.resource.JavaResourceNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed try-statement syntax, with role-owned children in source order. */
public final class JavaTryStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaTryStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.TRY_STATEMENT, JavaTryStatementNode.class);
    public static final ChildRole<JavaResourceNode> RESOURCES = ChildRole.list(JavaChildRoleNames.RESOURCES, JavaResourceNode.class);
    public static final ChildRole<JavaBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaBlockNode.class);
    public static final ChildRole<JavaCatchClauseNode> CATCHES = ChildRole.list(JavaChildRoleNames.CATCHES, JavaCatchClauseNode.class);
    public static final ChildRole<JavaFinallyClauseNode> FINALLY_CLAUSE = ChildRole.optional(JavaChildRoleNames.FINALLY_CLAUSE, JavaFinallyClauseNode.class);
    public static final NodeSchema<JavaTryStatementNode> SCHEMA = NodeSchema.of(KIND, RESOURCES, BODY, CATCHES, FINALLY_CLAUSE);

    public JavaTryStatementNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaResourceNode> resources() {
        return children(RESOURCES);
    }

    public void addResource(JavaResourceNode node) {
        addChild(RESOURCES, node);
    }

    public JavaBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaBlockNode node) {
        setChild(BODY, node);
    }

    public List<JavaCatchClauseNode> catches() {
        return children(CATCHES);
    }

    public void addCatchClause(JavaCatchClauseNode node) {
        addChild(CATCHES, node);
    }

    public Optional<JavaFinallyClauseNode> finallyClause() {
        return optionalChild(FINALLY_CLAUSE);
    }

    public void setFinallyClause(JavaFinallyClauseNode node) {
        setChild(FINALLY_CLAUSE, node);
    }
}

