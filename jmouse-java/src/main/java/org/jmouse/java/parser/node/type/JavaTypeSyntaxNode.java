package org.jmouse.java.parser.node.type;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The TypeSyntax family in Java source syntax. */
public abstract class JavaTypeSyntaxNode extends JavaSyntaxNode {
    protected JavaTypeSyntaxNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

