package org.jmouse.java.parser.node.expression;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed text-block-literal syntax, with role-owned children in source order. */
public final class JavaTextBlockLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaTextBlockLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.TEXT_BLOCK_LITERAL, JavaTextBlockLiteralNode.class);
    public static final NodeSchema<JavaTextBlockLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private String value;

    public JavaTextBlockLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String raw() {
        return raw;
    }

    public void setRaw(String value) {
        requireBuilding();
        this.raw = Objects.requireNonNull(value, "raw");
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        requireBuilding();
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(value, "value");
    }
}

