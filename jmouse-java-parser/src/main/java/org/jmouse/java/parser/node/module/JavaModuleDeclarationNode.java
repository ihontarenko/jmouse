package org.jmouse.java.parser.node.module;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed module-declaration syntax, with role-owned children in source order. */
public final class JavaModuleDeclarationNode extends JavaSyntaxNode {
    public static final NodeKind<JavaModuleDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.MODULE_DECLARATION, JavaModuleDeclarationNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final ChildRole<JavaModuleDirectiveNode> DIRECTIVES = ChildRole.list(JavaChildRoleNames.DIRECTIVES, JavaModuleDirectiveNode.class);
    public static final NodeSchema<JavaModuleDeclarationNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, NAME, DIRECTIVES);
    private JavaModuleKind kind;

    public JavaModuleDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationNode> annotations() {
        return children(ANNOTATIONS);
    }

    public void addAnnotation(JavaAnnotationNode node) {
        addChild(ANNOTATIONS, node);
    }

    public JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public void setName(JavaQualifiedNameNode node) {
        setChild(NAME, node);
    }

    public List<JavaModuleDirectiveNode> directives() {
        return children(DIRECTIVES);
    }

    public void addDirective(JavaModuleDirectiveNode node) {
        addChild(DIRECTIVES, node);
    }

    public JavaModuleKind kind() {
        return kind;
    }

    public void setKind(JavaModuleKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

