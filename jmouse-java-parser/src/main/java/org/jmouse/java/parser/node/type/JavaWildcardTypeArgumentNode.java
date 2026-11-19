package org.jmouse.java.parser.node.type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed wildcard-type-argument syntax, with role-owned children in source order. */
public final class JavaWildcardTypeArgumentNode extends JavaTypeSyntaxNode {
    public static final NodeKind<JavaWildcardTypeArgumentNode> KIND = new NodeKind<>(JavaNodeKindNames.WILDCARD_TYPE_ARGUMENT, JavaWildcardTypeArgumentNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> BOUND = ChildRole.optional(JavaChildRoleNames.BOUND, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaWildcardTypeArgumentNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, BOUND);
    private JavaTypeBoundKind kind;

    public JavaWildcardTypeArgumentNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public Optional<JavaTypeSyntaxNode> bound() {
        return optionalChild(BOUND);
    }

    public void setBound(JavaTypeSyntaxNode node) {
        setChild(BOUND, node);
    }

    public JavaTypeBoundKind kind() {
        return kind;
    }

    public void setKind(JavaTypeBoundKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

