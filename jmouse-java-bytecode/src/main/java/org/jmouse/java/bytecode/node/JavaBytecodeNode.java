package org.jmouse.java.bytecode.node;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.StructuredNode;
import org.jmouse.java.bytecode.BytecodeSpan;

import java.util.Objects;

/**
 * Binary syntax has its own positions and shares the same ownership/traversal contract as source.
 */
public abstract class JavaBytecodeNode extends StructuredNode {
    private final BytesSource source;
    private final BytecodeSpan span;

    protected JavaBytecodeNode(NodeSchema<?> schema, BytesSource source, BytecodeSpan span) {
        super(schema);
        this.source = Objects.requireNonNull(source, "source");
        this.span = Objects.requireNonNull(span, "span");
        Objects.checkFromToIndex(span.start(), span.end(), source.contentLength());
    }

    public final BytesSource source() {
        return source;
    }

    public final BytecodeSpan span() {
        return span;
    }

    public final byte[] bytes() {
        return source.slice(span.start(), Math.toIntExact(span.length()));
    }
}
