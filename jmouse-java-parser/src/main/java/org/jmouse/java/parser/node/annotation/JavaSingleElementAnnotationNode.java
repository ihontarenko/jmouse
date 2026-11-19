package org.jmouse.java.parser.node.annotation;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed single-element-annotation syntax, with role-owned children in source order. */
public final class JavaSingleElementAnnotationNode extends JavaAnnotationNode {
    public static final NodeKind<JavaSingleElementAnnotationNode> KIND = new NodeKind<>(JavaNodeKindNames.SINGLE_ELEMENT_ANNOTATION, JavaSingleElementAnnotationNode.class);
    public static final ChildRole<JavaSyntaxNode> VALUE = ChildRole.required(JavaChildRoleNames.VALUE, JavaSyntaxNode.class);
    public static final NodeSchema<JavaSingleElementAnnotationNode> SCHEMA = NodeSchema.of(KIND, NAME, VALUE);

    public JavaSingleElementAnnotationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaSyntaxNode value() {
        return child(VALUE);
    }

    public void setValue(JavaSyntaxNode node) {
        setChild(VALUE, node);
    }
}

