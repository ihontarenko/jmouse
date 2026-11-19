package org.jmouse.java.parser.node.annotation;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.ChildRole;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Annotation family in Java source syntax. */
public abstract class JavaAnnotationNode extends JavaSyntaxNode {
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);

    public final JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public final void setName(JavaQualifiedNameNode name) {
        setChild(NAME, name);
    }
    protected JavaAnnotationNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

