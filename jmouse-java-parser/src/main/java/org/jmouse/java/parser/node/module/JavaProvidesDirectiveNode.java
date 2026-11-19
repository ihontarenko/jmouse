package org.jmouse.java.parser.node.module;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed provides-directive syntax, with role-owned children in source order. */
public final class JavaProvidesDirectiveNode extends JavaModuleDirectiveNode {
    public static final NodeKind<JavaProvidesDirectiveNode> KIND = new NodeKind<>(JavaNodeKindNames.PROVIDES_DIRECTIVE, JavaProvidesDirectiveNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> SERVICE = ChildRole.required(JavaChildRoleNames.SERVICE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> PROVIDERS = ChildRole.list(JavaChildRoleNames.PROVIDERS, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaProvidesDirectiveNode> SCHEMA = NodeSchema.of(KIND, SERVICE, PROVIDERS);

    public JavaProvidesDirectiveNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode service() {
        return child(SERVICE);
    }

    public void setService(JavaTypeSyntaxNode node) {
        setChild(SERVICE, node);
    }

    public List<JavaTypeSyntaxNode> providers() {
        return children(PROVIDERS);
    }

    public void addProvider(JavaTypeSyntaxNode node) {
        addChild(PROVIDERS, node);
    }
}

