package org.jmouse.java.parser.node.parameter;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed variable-arity-parameter syntax, with role-owned children in source order. */
public final class JavaVariableArityParameterNode extends JavaParameterNode {
    public static final NodeKind<JavaVariableArityParameterNode> KIND = new NodeKind<>(JavaNodeKindNames.VARIABLE_ARITY_PARAMETER, JavaVariableArityParameterNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaAnnotationNode> ELLIPSIS_ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ELLIPSIS_ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaArrayDimensionNode> DIMENSIONS = ChildRole.list(JavaChildRoleNames.DIMENSIONS, JavaArrayDimensionNode.class);
    public static final NodeSchema<JavaVariableArityParameterNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, ELLIPSIS_ANNOTATIONS, NAME, DIMENSIONS);

    public List<JavaAnnotationNode> ellipsisAnnotations() {
        return children(ELLIPSIS_ANNOTATIONS);
    }

    public void addEllipsisAnnotation(JavaAnnotationNode annotation) {
        addChild(ELLIPSIS_ANNOTATIONS, annotation);
    }

    public JavaVariableArityParameterNode(String sourceName, SourceSpan span) {
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
}

