package org.jmouse.java.parser.node.annotation;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed annotation-value-pair syntax, with role-owned children in source order. */
public final class JavaAnnotationValuePairNode extends JavaSyntaxNode {
    public static final NodeKind<JavaAnnotationValuePairNode> KIND = new NodeKind<>(JavaNodeKindNames.ANNOTATION_VALUE_PAIR, JavaAnnotationValuePairNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaSyntaxNode> VALUE = ChildRole.required(JavaChildRoleNames.VALUE, JavaSyntaxNode.class);
    public static final NodeSchema<JavaAnnotationValuePairNode> SCHEMA = NodeSchema.of(KIND, NAME, VALUE);

    public JavaAnnotationValuePairNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public JavaSyntaxNode value() {
        return child(VALUE);
    }

    public void setValue(JavaSyntaxNode node) {
        setChild(VALUE, node);
    }
}

