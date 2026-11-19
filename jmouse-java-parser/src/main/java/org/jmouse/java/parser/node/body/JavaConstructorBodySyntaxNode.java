package org.jmouse.java.parser.node.body;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Open family for release-specific constructor-body layouts. */
public abstract class JavaConstructorBodySyntaxNode extends JavaSyntaxNode {
    protected JavaConstructorBodySyntaxNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}
