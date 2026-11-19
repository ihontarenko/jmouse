package org.jmouse.java.parser.node.parameter;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Parameter family in Java source syntax. */
public abstract class JavaParameterNode extends JavaSyntaxNode {
    protected JavaParameterNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

