package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** The Literal family in Java source syntax. */
public abstract class JavaLiteralNode extends JavaExpressionNode {
    protected JavaLiteralNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

