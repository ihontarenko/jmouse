package org.jmouse.java.parser.node.declaration;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Declaration family in Java source syntax. */
public abstract class JavaDeclarationNode extends JavaSyntaxNode {
    protected JavaDeclarationNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

