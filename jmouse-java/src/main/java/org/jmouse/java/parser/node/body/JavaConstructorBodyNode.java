package org.jmouse.java.parser.node.body;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed constructor-body syntax, with role-owned children in source order. */
public final class JavaConstructorBodyNode extends JavaConstructorBodySyntaxNode {
    public static final NodeKind<JavaConstructorBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.CONSTRUCTOR_BODY, JavaConstructorBodyNode.class);
    public static final ChildRole<JavaConstructorInvocationNode> INVOCATION = ChildRole.optional(JavaChildRoleNames.INVOCATION, JavaConstructorInvocationNode.class);
    public static final ChildRole<JavaSyntaxNode> STATEMENTS = ChildRole.list(JavaChildRoleNames.STATEMENTS, JavaSyntaxNode.class);
    public static final NodeSchema<JavaConstructorBodyNode> SCHEMA = NodeSchema.of(KIND, INVOCATION, STATEMENTS);

    public JavaConstructorBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaConstructorInvocationNode> invocation() {
        return optionalChild(INVOCATION);
    }

    public void setInvocation(JavaConstructorInvocationNode node) {
        setChild(INVOCATION, node);
    }

    public List<JavaSyntaxNode> statements() {
        return children(STATEMENTS);
    }

    public void addStatement(JavaSyntaxNode node) {
        addChild(STATEMENTS, node);
    }
}

