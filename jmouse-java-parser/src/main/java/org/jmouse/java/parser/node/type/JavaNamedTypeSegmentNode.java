package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed named-type-segment syntax, with role-owned children in source order. */
public final class JavaNamedTypeSegmentNode extends JavaSyntaxNode {
    public static final NodeKind<JavaNamedTypeSegmentNode> KIND = new NodeKind<>(JavaNodeKindNames.NAMED_TYPE_SEGMENT, JavaNamedTypeSegmentNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> ARGUMENTS = ChildRole.list(JavaChildRoleNames.ARGUMENTS, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaDiamondTypeArgumentsNode> DIAMOND = ChildRole.optional(JavaChildRoleNames.DIAMOND, JavaDiamondTypeArgumentsNode.class);
    public static final NodeSchema<JavaNamedTypeSegmentNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, NAME, ARGUMENTS, DIAMOND);

    public JavaNamedTypeSegmentNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public List<JavaTypeSyntaxNode> arguments() {
        return children(ARGUMENTS);
    }

    public void addArgument(JavaTypeSyntaxNode node) {
        addChild(ARGUMENTS, node);
    }

    public Optional<JavaDiamondTypeArgumentsNode> diamond() {
        return optionalChild(DIAMOND);
    }

    public void setDiamond(JavaDiamondTypeArgumentsNode node) {
        setChild(DIAMOND, node);
    }
}

