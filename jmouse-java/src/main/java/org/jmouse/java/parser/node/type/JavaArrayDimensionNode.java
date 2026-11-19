package org.jmouse.java.parser.node.type;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed array-dimension syntax, with role-owned children in source order. */
public final class JavaArrayDimensionNode extends JavaSyntaxNode {
    public static final NodeKind<JavaArrayDimensionNode> KIND = new NodeKind<>(JavaNodeKindNames.ARRAY_DIMENSION, JavaArrayDimensionNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaExpressionNode> LENGTH = ChildRole.optional(JavaChildRoleNames.LENGTH, JavaExpressionNode.class);
    public static final NodeSchema<JavaArrayDimensionNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, LENGTH);

    public JavaArrayDimensionNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public Optional<JavaExpressionNode> length() {
        return optionalChild(LENGTH);
    }

    public void setLength(JavaExpressionNode node) {
        setChild(LENGTH, node);
    }
}

