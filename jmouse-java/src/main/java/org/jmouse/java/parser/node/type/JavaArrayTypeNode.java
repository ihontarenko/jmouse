package org.jmouse.java.parser.node.type;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed array-type syntax, with role-owned children in source order. */
public final class JavaArrayTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaArrayTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.ARRAY_TYPE, JavaArrayTypeNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> ELEMENT_TYPE = ChildRole.required(JavaChildRoleNames.ELEMENT_TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final NodeSchema<JavaArrayTypeNode> SCHEMA = NodeSchema.of(KIND, ELEMENT_TYPE, DIMENSIONS);

    public JavaArrayTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaTypeSyntaxNode elementType() {
        return child(ELEMENT_TYPE);
    }

    public void setElementType(JavaTypeSyntaxNode node) {
        setChild(ELEMENT_TYPE, node);
    }

    public List<JavaArrayDimensionNode> dimensions() {
        return children(DIMENSIONS);
    }

    public void addDimension(JavaArrayDimensionNode node) {
        addChild(DIMENSIONS, node);
    }
}

