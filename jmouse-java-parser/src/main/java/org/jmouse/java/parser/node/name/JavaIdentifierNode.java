package org.jmouse.java.parser.node.name;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed identifier syntax, with role-owned children in source order. */
public final class JavaIdentifierNode extends JavaSyntaxNode {
    public static final NodeKind<JavaIdentifierNode> KIND = new NodeKind<>(JavaNodeKindNames.IDENTIFIER, JavaIdentifierNode.class);
    public static final NodeSchema<JavaIdentifierNode> SCHEMA = NodeSchema.of(KIND);
    private String name;

    public JavaIdentifierNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public String name() {
        return name;
    }

    public void setName(String value) {
        requireBuilding();
        this.name = Objects.requireNonNull(value, "name");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(name, "name");
    }
}

