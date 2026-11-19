package org.jmouse.java.parser.node.expression;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed string-literal syntax, with role-owned children in source order. */
public final class JavaStringLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaStringLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.STRING_LITERAL, JavaStringLiteralNode.class);
    public static final NodeSchema<JavaStringLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private String value;

    public JavaStringLiteralNode(String sourceName, SourceSpan span) {
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

