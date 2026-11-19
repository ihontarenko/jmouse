package org.jmouse.java.parser.node.expression;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed boolean-literal syntax, with role-owned children in source order. */
public final class JavaBooleanLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaBooleanLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.BOOLEAN_LITERAL, JavaBooleanLiteralNode.class);
    public static final NodeSchema<JavaBooleanLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private boolean value;

    public JavaBooleanLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String raw() {
        return raw;
    }

    public void setRaw(String value) {
        requireBuilding();
        this.raw = Objects.requireNonNull(value, "raw");
    }

    public boolean value() {
        return value;
    }

    public void setValue(boolean value) {
        requireBuilding();
        this.value = value;
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(raw, "raw");
    }
}

