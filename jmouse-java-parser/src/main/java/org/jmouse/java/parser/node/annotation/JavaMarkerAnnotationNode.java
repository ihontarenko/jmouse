package org.jmouse.java.parser.node.annotation;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed marker-annotation syntax, with role-owned children in source order. */
public final class JavaMarkerAnnotationNode extends JavaAnnotationNode {
    public static final NodeKind<JavaMarkerAnnotationNode> KIND = new NodeKind<>(JavaNodeKindNames.MARKER_ANNOTATION, JavaMarkerAnnotationNode.class);
    public static final NodeSchema<JavaMarkerAnnotationNode> SCHEMA = NodeSchema.of(KIND, NAME);

    public JavaMarkerAnnotationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

}

