package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.StructuredNode;

import java.util.Objects;

/** A decoded-text range; it never masquerades as a raw binary offset. */
public abstract class JvmTextNode extends StructuredNode {
    private final TokenizableSource source;
    private final int start;
    private final int end;

    protected JvmTextNode(NodeSchema<?> schema, TokenizableSource source, int start, int end) {
        super(schema);
        this.source = Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(start, end, source.length());
        this.start = start;
        this.end = end;
    }

    public final TokenizableSource source() {
        return source;
    }

    public final int start() {
        return start;
    }

    public final int end() {
        return end;
    }

    public final String text() {
        return source.subSequence(start, end).toString();
    }
}
