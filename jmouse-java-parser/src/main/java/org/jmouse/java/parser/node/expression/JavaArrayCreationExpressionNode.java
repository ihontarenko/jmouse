package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed array-creation-expression syntax, with role-owned children in source order. */
public final class JavaArrayCreationExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaArrayCreationExpressionNode> KIND = new NodeKind<>(JavaNodeKindNames.ARRAY_CREATION_EXPRESSION, JavaArrayCreationExpressionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> ELEMENT_TYPE = ChildRole.required(JavaChildRoleNames.ELEMENT_TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final ChildRole<JavaArrayInitializerNode> INITIALIZER = ChildRole.optional(JavaChildRoleNames.INITIALIZER, JavaArrayInitializerNode.class);
    public static final NodeSchema<JavaArrayCreationExpressionNode> SCHEMA = NodeSchema.of(KIND, ELEMENT_TYPE, DIMENSIONS, INITIALIZER);

    public JavaArrayCreationExpressionNode(String sourceName, SourceSpan span) {
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

    public Optional<JavaArrayInitializerNode> initializer() {
        return optionalChild(INITIALIZER);
    }

    public void setInitializer(JavaArrayInitializerNode node) {
        setChild(INITIALIZER, node);
    }
}

