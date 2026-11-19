package org.jmouse.java.parser.node.type;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed primitive-type syntax, with role-owned children in source order. */
public final class JavaPrimitiveTypeNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaPrimitiveTypeNode> KIND = new NodeKind<>(JavaNodeKindNames.PRIMITIVE_TYPE, JavaPrimitiveTypeNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final NodeSchema<JavaPrimitiveTypeNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS);
    private JavaPrimitiveKind kind;

    public JavaPrimitiveTypeNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public JavaPrimitiveKind kind() {
        return kind;
    }

    public void setKind(JavaPrimitiveKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

