package org.jmouse.java.parser.node.pattern;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The Pattern family in Java source syntax. */
public abstract class JavaPatternNode extends JavaSyntaxNode {
    protected JavaPatternNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

