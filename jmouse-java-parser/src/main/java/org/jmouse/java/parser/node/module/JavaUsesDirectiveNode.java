package org.jmouse.java.parser.node.module;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed uses-directive syntax, with role-owned children in source order. */
public final class JavaUsesDirectiveNode extends JavaModuleDirectiveNode {
    public static final NodeKind<JavaUsesDirectiveNode> KIND = new NodeKind<>(JavaNodeKindNames.USES_DIRECTIVE, JavaUsesDirectiveNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> SERVICE = ChildRole.required(JavaChildRoleNames.SERVICE, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaUsesDirectiveNode> SCHEMA = NodeSchema.of(KIND, SERVICE);

    public JavaUsesDirectiveNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode service() {
        return child(SERVICE);
    }

    public void setService(JavaTypeSyntaxNode node) {
        setChild(SERVICE, node);
    }
}

