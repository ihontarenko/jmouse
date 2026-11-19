package org.jmouse.java.parser.node.module;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaOpensDirectiveNode extends JavaPackageAccessDirectiveNode {
    public static final NodeKind<JavaOpensDirectiveNode> KIND =
            new NodeKind<>(JavaNodeKindNames.OPENS_DIRECTIVE, JavaOpensDirectiveNode.class);
    public static final NodeSchema<JavaOpensDirectiveNode> SCHEMA = NodeSchema.of(KIND, NAME, TARGETS);

    public JavaOpensDirectiveNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}
