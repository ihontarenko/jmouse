package org.jmouse.java.parser.node.parameter;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed receiver-parameter syntax, with role-owned children in source order. */
public final class JavaReceiverParameterNode extends JavaSyntaxNode {
    public static final NodeKind<JavaReceiverParameterNode> KIND = new NodeKind<>(JavaNodeKindNames.RECEIVER_PARAMETER, JavaReceiverParameterNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaQualifiedNameNode> QUALIFIER = ChildRole.optional(JavaChildRoleNames.QUALIFIER, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaReceiverParameterNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, TYPE, QUALIFIER);

    public JavaReceiverParameterNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public Optional<JavaQualifiedNameNode> qualifier() {
        return optionalChild(QUALIFIER);
    }

    public void setQualifier(JavaQualifiedNameNode node) {
        setChild(QUALIFIER, node);
    }
}

