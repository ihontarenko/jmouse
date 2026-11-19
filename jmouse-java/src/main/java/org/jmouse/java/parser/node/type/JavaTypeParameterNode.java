package org.jmouse.java.parser.node.type;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed type-parameter syntax, with role-owned children in source order. */
public final class JavaTypeParameterNode extends JavaSyntaxNode {
    public static final NodeKind<JavaTypeParameterNode> KIND = new NodeKind<>(JavaNodeKindNames.TYPE_PARAMETER, JavaTypeParameterNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> BOUNDS = ChildRole.list(JavaChildRoleNames.BOUNDS, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaTypeParameterNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, NAME, BOUNDS);

    public JavaTypeParameterNode(String sourceName, SourceSpan span) {
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

    public List<JavaTypeSyntaxNode> bounds() {
        return children(BOUNDS);
    }

    public void addBound(JavaTypeSyntaxNode node) {
        addChild(BOUNDS, node);
    }
}

