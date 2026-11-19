package org.jmouse.java.parser.node;

import java.util.Objects;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.StructuredNode;
import org.jmouse.java.parser.source.SourceSpan;

/** A complete source-syntax node. It has no evaluator or target-language dependency. */
public abstract class JavaSyntaxNode extends StructuredNode {
    private final String sourceName;
    private SourceSpan span;

    protected JavaSyntaxNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema);
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
        this.span = Objects.requireNonNull(span, "span");
    }

    public final String sourceName() {
        return sourceName;
    }

    public final SourceSpan span() {
        return span;
    }

    /** Completes the consumed range while the parser still owns the building node. */
    public final void setSpan(SourceSpan span) {
        requireBuilding();
        this.span = Objects.requireNonNull(span, "span");
    }
}
