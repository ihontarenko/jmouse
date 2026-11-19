package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed union-type syntax, with role-owned children in source order. */
public final class JavaUnionTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaUnionTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.UNION_TYPE, JavaUnionTypeNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPES = ChildRole.list(JavaChildRoleNames.TYPES, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaUnionTypeNode> SCHEMA = NodeSchema.of(KIND, TYPES);

    public JavaUnionTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaTypeSyntaxNode> types() {
        return children(TYPES);
    }

    public void addType(JavaTypeSyntaxNode node) {
        addChild(TYPES, node);
    }
}

