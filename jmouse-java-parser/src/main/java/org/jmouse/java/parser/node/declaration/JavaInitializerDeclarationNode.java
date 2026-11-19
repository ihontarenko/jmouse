package org.jmouse.java.parser.node.declaration;

import java.util.Objects;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed initializer-declaration syntax, with role-owned children in source order. */
public final class JavaInitializerDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaInitializerDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.INITIALIZER_DECLARATION, JavaInitializerDeclarationNode.class);
    public static final ChildRole<JavaBlockNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaBlockNode.class);
    public static final NodeSchema<JavaInitializerDeclarationNode> SCHEMA = NodeSchema.of(KIND, BODY);
    private JavaInitializerKind kind;

    public JavaInitializerDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaBlockNode body() {
        return child(BODY);
    }

    public void setBody(JavaBlockNode node) {
        setChild(BODY, node);
    }

    public JavaInitializerKind kind() {
        return kind;
    }

    public void setKind(JavaInitializerKind value) {
        requireBuilding();
        this.kind = Objects.requireNonNull(value, "kind");
    }

    @Override
    protected void validateStructure() {
        Objects.requireNonNull(kind, "kind");
    }
}

