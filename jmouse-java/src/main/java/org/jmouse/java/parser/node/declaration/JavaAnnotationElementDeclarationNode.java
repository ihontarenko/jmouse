package org.jmouse.java.parser.node.declaration;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed annotation-element-declaration syntax, with role-owned children in source order. */
public final class JavaAnnotationElementDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaAnnotationElementDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.ANNOTATION_ELEMENT_DECLARATION, JavaAnnotationElementDeclarationNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final ChildRole<JavaSyntaxNode> DEFAULT_VALUE = ChildRole.optional(JavaChildRoleNames.DEFAULT_VALUE, JavaSyntaxNode.class);
    public static final NodeSchema<JavaAnnotationElementDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, NAME, DIMENSIONS, DEFAULT_VALUE);

    public JavaAnnotationElementDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaModifiersNode modifiers() {
        return child(MODIFIERS);
    }

    public void setModifiers(JavaModifiersNode node) {
        setChild(MODIFIERS, node);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public List<JavaArrayDimensionNode> dimensions() {
        return children(DIMENSIONS);
    }

    public void addDimension(JavaArrayDimensionNode node) {
        addChild(DIMENSIONS, node);
    }

    public Optional<JavaSyntaxNode> defaultValue() {
        return optionalChild(DEFAULT_VALUE);
    }

    public void setDefaultValue(JavaSyntaxNode node) {
        setChild(DEFAULT_VALUE, node);
    }
}

