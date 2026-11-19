package org.jmouse.java.parser.node.resource;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Resource family in Java source syntax. */
public abstract class JavaResourceNode extends JavaSyntaxNode {
    protected JavaResourceNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

