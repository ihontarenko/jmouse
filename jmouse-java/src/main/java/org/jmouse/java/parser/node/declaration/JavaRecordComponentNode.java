package org.jmouse.java.parser.node.declaration;

import java.util.List;
import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed record-component syntax, with role-owned children in source order. */
public final class JavaRecordComponentNode extends JavaSyntaxNode {
    public static final NodeKind<JavaRecordComponentNode> KIND = new NodeKind<>(JavaNodeKindNames.RECORD_COMPONENT, JavaRecordComponentNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaAnnotationNode> ELLIPSIS_ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ELLIPSIS_ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final NodeSchema<JavaRecordComponentNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, ELLIPSIS_ANNOTATIONS, NAME);

    public List<JavaAnnotationNode> ellipsisAnnotations() {
        return children(ELLIPSIS_ANNOTATIONS);
    }

    public void addEllipsisAnnotation(JavaAnnotationNode annotation) {
        addChild(ELLIPSIS_ANNOTATIONS, annotation);
    }
    private JavaParameterKind kind;

    public JavaRecordComponentNode(String sourceName, SourceSpan span) {
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

    public JavaParameterKind kind() {
        return kind;
    }

    public void setKind(JavaParameterKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

