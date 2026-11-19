package org.jmouse.java.parser.node.unit;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Open import family; a release module may contribute a new form of import. */
public abstract class JavaImportNode extends JavaSyntaxNode {
    protected JavaImportNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}
