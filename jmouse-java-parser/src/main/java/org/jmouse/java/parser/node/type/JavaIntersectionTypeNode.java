package org.jmouse.java.parser.node.type;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed intersection-type syntax, with role-owned children in source order. */
public final class JavaIntersectionTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaIntersectionTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.INTERSECTION_TYPE, JavaIntersectionTypeNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPES = ChildRole.list(JavaChildRoleNames.TYPES, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaIntersectionTypeNode> SCHEMA = NodeSchema.of(KIND, TYPES);

    public JavaIntersectionTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaTypeSyntaxNode> types() {
        return children(TYPES);
    }

    public void addType(JavaTypeSyntaxNode node) {
        addChild(TYPES, node);
    }
}

