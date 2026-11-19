package org.jmouse.java.parser.node.expression;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed character-literal syntax, with role-owned children in source order. */
public final class JavaCharacterLiteralNode extends JavaLiteralNode {
    public static final NodeKind<JavaCharacterLiteralNode> KIND = new NodeKind<>(JavaNodeKindNames.CHARACTER_LITERAL, JavaCharacterLiteralNode.class);
    public static final NodeSchema<JavaCharacterLiteralNode> SCHEMA = NodeSchema.of(KIND);
    private String raw;
    private char value;

    public JavaCharacterLiteralNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String raw() {
        return raw;
    }

    public void setRaw(String value) {
        requireBuilding();
        this.raw = Objects.requireNonNull(value, "raw");
    }

    public char value() {
        return value;
    }

    public void setValue(char value) {
        requireBuilding();
        this.value = value;
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(raw, "raw");
    }
}

