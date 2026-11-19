package org.jmouse.java.parser.node.unit;

import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed import-declaration syntax, with role-owned children in source order. */
public final class JavaImportDeclarationNode extends JavaImportNode {
    public static final NodeKind<JavaImportDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.IMPORT_DECLARATION, JavaImportDeclarationNode.class);
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaImportDeclarationNode> SCHEMA = NodeSchema.of(KIND, NAME);
    private JavaImportKind kind;

    public JavaImportDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public void setName(JavaQualifiedNameNode node) {
        setChild(NAME, node);
    }

    public JavaImportKind kind() {
        return kind;
    }

    public void setKind(JavaImportKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

