package org.jmouse.java.parser.node.unit;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed package-declaration syntax, with role-owned children in source order. */
public final class JavaPackageDeclarationNode extends JavaSyntaxNode {
    public static final NodeKind<JavaPackageDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.PACKAGE_DECLARATION, JavaPackageDeclarationNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATIONS = ChildRole.list(JavaChildRoleNames.ANNOTATIONS, JavaAnnotationNode.class);
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaPackageDeclarationNode> SCHEMA = NodeSchema.of(KIND, ANNOTATIONS, NAME);

    public JavaPackageDeclarationNode(String sourceName, SourceSpan span) {
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
}

