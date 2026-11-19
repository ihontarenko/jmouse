package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaReceiverParameterNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed constructor-declaration syntax, with role-owned children in source order. */
public final class JavaConstructorDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaConstructorDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.CONSTRUCTOR_DECLARATION, JavaConstructorDeclarationNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeParameterNode> TYPE_PARAMETERS = ChildRole.list(JavaChildRoleNames.TYPE_PARAMETERS, JavaTypeParameterNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaParameterNode> PARAMETERS = ChildRole.list(JavaChildRoleNames.PARAMETERS, JavaParameterNode.class);
    public static final ChildRole<JavaReceiverParameterNode> RECEIVER = ChildRole.optional(JavaChildRoleNames.RECEIVER, JavaReceiverParameterNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> THROWN_TYPES = ChildRole.list(JavaChildRoleNames.THROWN_TYPES, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaSyntaxNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaSyntaxNode.class);
    public static final NodeSchema<JavaConstructorDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE_PARAMETERS, NAME, RECEIVER, PARAMETERS, DIMENSIONS, THROWN_TYPES, BODY);

    public JavaConstructorDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaModifiersNode modifiers() {
        return child(MODIFIERS);
    }

    public void setModifiers(JavaModifiersNode node) {
        setChild(MODIFIERS, node);
    }

    public List<JavaTypeParameterNode> typeParameters() {
        return children(TYPE_PARAMETERS);
    }

    public void addTypeParameter(JavaTypeParameterNode node) {
        addChild(TYPE_PARAMETERS, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public List<JavaParameterNode> parameters() {
        return children(PARAMETERS);
    }

    public void addParameter(JavaParameterNode node) {
        addChild(PARAMETERS, node);
    }

    public Optional<JavaReceiverParameterNode> receiver() {
        return optionalChild(RECEIVER);
    }

    public void setReceiver(JavaReceiverParameterNode node) {
        setChild(RECEIVER, node);
    }

    public List<JavaArrayDimensionNode> dimensions() {
        return children(DIMENSIONS);
    }

    public void addDimension(JavaArrayDimensionNode node) {
        addChild(DIMENSIONS, node);
    }

    public List<JavaTypeSyntaxNode> thrownTypes() {
        return children(THROWN_TYPES);
    }

    public void addThrownType(JavaTypeSyntaxNode node) {
        addChild(THROWN_TYPES, node);
    }

    public JavaSyntaxNode body() {
        return child(BODY);
    }

    public void setBody(JavaSyntaxNode node) {
        setChild(BODY, node);
    }
}

