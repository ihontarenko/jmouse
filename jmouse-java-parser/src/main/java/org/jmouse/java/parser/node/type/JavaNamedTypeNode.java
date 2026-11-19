package org.jmouse.java.parser.node.type;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed named-type syntax, with role-owned children in source order. */
public final class JavaNamedTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaNamedTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.NAMED_TYPE, JavaNamedTypeNode.class);
    public static final ChildRole<JavaNamedTypeSegmentNode> SEGMENTS = ChildRole.list(JavaChildRoleNames.SEGMENTS, JavaNamedTypeSegmentNode.class);
    public static final NodeSchema<JavaNamedTypeNode> SCHEMA = NodeSchema.of(KIND, SEGMENTS);

    public JavaNamedTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaNamedTypeSegmentNode> segments() {
        return children(SEGMENTS);
    }

    public void addSegment(JavaNamedTypeSegmentNode node) {
        addChild(SEGMENTS, node);
    }
}

