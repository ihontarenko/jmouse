package org.jmouse.java.parser.node.name;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Objects;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;
import org.jmouse.java.parser.lexer.JavaKeywordCatalog;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed modifier syntax, with role-owned children in source order. */
public final class JavaModifierNode extends JavaSyntaxNode {
    public static final NodeKind<JavaModifierNode> KIND = new NodeKind<>(JavaNodeKindNames.MODIFIER, JavaModifierNode.class);
    public static final NodeSchema<JavaModifierNode> SCHEMA = NodeSchema.of(KIND);
    private JavaKeywordDefinition keyword;

    public JavaModifierNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaKeywordDefinition keyword() {
        return keyword;
    }

    public void setKeyword(JavaKeywordDefinition value) {
        requireBuilding();
        this.keyword = JavaKeywordCatalog.snapshotDefinition(Objects.requireNonNull(value, "keyword"));
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(keyword, "keyword");
    }
}

