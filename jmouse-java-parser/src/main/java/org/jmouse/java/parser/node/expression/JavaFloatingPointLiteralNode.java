package org.jmouse.java.parser.node.expression;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed floating-point-literal syntax, with role-owned children in source order. */
public final class JavaFloatingPointLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaFloatingPointLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.FLOATING_POINT_LITERAL, JavaFloatingPointLiteralNode.class);
    public static final NodeSchema<JavaFloatingPointLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private JavaFloatingLiteralValue value;

    public JavaFloatingPointLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String raw() {
        return raw;
    }

    public void setRaw(String value) {
        requireBuilding();
        this.raw = Objects.requireNonNull(value, "raw");
    }

    public JavaFloatingLiteralValue value() {
        return value;
    }

    public void setValue(JavaFloatingLiteralValue value) {
        requireBuilding();
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(value, "value");
    }
}

