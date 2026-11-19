package org.jmouse.java.parser.node.annotation;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed normal-annotation syntax, with role-owned children in source order. */
public final class JavaNormalAnnotationNode extends JavaAnnotationNode {
    public static final NodeKind<JavaNormalAnnotationNode> KIND = new NodeKind<>(JavaNodeKindNames.NORMAL_ANNOTATION, JavaNormalAnnotationNode.class);
    public static final ChildRole<JavaAnnotationValuePairNode> VALUES = ChildRole.list(JavaChildRoleNames.VALUES, JavaAnnotationValuePairNode.class);
    public static final NodeSchema<JavaNormalAnnotationNode> SCHEMA = NodeSchema.of(KIND, NAME, VALUES);

    public JavaNormalAnnotationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaAnnotationValuePairNode> values() {
        return children(VALUES);
    }

    public void addValue(JavaAnnotationValuePairNode node) {
        addChild(VALUES, node);
    }
}

