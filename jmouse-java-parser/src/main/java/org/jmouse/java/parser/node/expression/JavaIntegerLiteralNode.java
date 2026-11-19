package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.math.BigInteger;
import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed integer-literal syntax, with role-owned children in source order. */
public final class JavaIntegerLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaIntegerLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.INTEGER_LITERAL, JavaIntegerLiteralNode.class);
    public static final NodeSchema<JavaIntegerLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private BigInteger value;

    public JavaIntegerLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String raw() {
        return raw;
    }

    public void setRaw(String value) {
        requireBuilding();
        this.raw = Objects.requireNonNull(value, "raw");
    }

    public BigInteger value() {
        return value;
    }

    public void setValue(BigInteger value) {
        requireBuilding();
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(value, "value");
    }
}

