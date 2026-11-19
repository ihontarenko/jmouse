package org.jmouse.java.parser.node.module;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaExportsDirectiveNode extends JavaPackageAccessDirectiveNode {
    public static final NodeKind<JavaExportsDirectiveNode> KIND =
            new NodeKind<>(JavaNodeKindNames.EXPORTS_DIRECTIVE, JavaExportsDirectiveNode.class);
    public static final NodeSchema<JavaExportsDirectiveNode> SCHEMA = NodeSchema.of(KIND, NAME, TARGETS);

    public JavaExportsDirectiveNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}
