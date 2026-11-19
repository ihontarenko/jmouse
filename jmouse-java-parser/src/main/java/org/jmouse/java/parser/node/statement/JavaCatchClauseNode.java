package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed catch-clause syntax, with role-owned children in source order. */
public final class JavaCatchClauseNode extends JavaSyntaxNode {
    public static final NodeKind<JavaCatchClauseNode> KIND = new NodeKind<>(JavaNodeKindNames.CATCH_CLAUSE, JavaCatchClauseNode.class);
    public static final ChildRole<JavaFormalParameterNode> PARAMETER = ChildRole.required(JavaChildRoleNames.PARAMETER, JavaFormalParameterNode.class);
    public static final ChildRole<JavaBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaBlockNode.class);
    public static final NodeSchema<JavaCatchClauseNode> SCHEMA = NodeSchema.of(KIND, PARAMETER, BODY);

    public JavaCatchClauseNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaFormalParameterNode parameter() {
        return child(PARAMETER);
    }

    public void setParameter(JavaFormalParameterNode node) {
        setChild(PARAMETER, node);
    }

    public JavaBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaBlockNode node) {
        setChild(BODY, node);
    }
}

