package org.jmouse.java.parser.node.module;

import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The ModuleDirective family in Java source syntax. */
public abstract class JavaModuleDirectiveNode extends JavaSyntaxNode {
    protected JavaModuleDirectiveNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }
}

