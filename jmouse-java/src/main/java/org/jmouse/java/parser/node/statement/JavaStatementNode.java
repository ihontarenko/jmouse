package org.jmouse.java.parser.node.statement;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Statement family in Java source syntax. */
public abstract class JavaStatementNode extends JavaSyntaxNode {
    protected JavaStatementNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

