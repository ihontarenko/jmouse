package org.jmouse.java.parser.node.body;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.JavaRegionKind;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.Objects;

/** An explicit hole in a partial AST, never a valid executable statement. */
public final class JavaOmittedRegionNode extends JavaSyntaxNode {
    public static final NodeKind<JavaOmittedRegionNode> KIND =
            new NodeKind<>(JavaNodeKindNames.OMITTED_REGION, JavaOmittedRegionNode.class);
    public static final NodeSchema<JavaOmittedRegionNode> SCHEMA = NodeSchema.of(KIND);
    private final JavaRegionKind kind;

    public JavaOmittedRegionNode(String sourceName, SourceSpan span, JavaRegionKind kind) {
        super(SCHEMA, sourceName, span);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public JavaRegionKind kind() {
        return kind;
    }
}
