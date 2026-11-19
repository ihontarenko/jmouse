package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Expression family in Java source syntax. */
public abstract class JavaExpressionNode extends JavaSyntaxNode {
    protected JavaExpressionNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

