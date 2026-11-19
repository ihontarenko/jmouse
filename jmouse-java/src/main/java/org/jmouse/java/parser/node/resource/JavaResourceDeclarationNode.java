package org.jmouse.java.parser.node.resource;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed resource-declaration syntax, with role-owned children in source order. */
public final class JavaResourceDeclarationNode extends JavaResourceNode {
    public static final NodeKind<JavaResourceDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.RESOURCE_DECLARATION, JavaResourceDeclarationNode.class);
    public static final ChildRole<JavaResourceVariableDeclarationNode> DECLARATION = ChildRole.required(JavaChildRoleNames.DECLARATION, JavaResourceVariableDeclarationNode.class);
    public static final NodeSchema<JavaResourceDeclarationNode> SCHEMA = NodeSchema.of(KIND, DECLARATION);

    public JavaResourceDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaResourceVariableDeclarationNode declaration() {
        return child(DECLARATION);
    }

    public void setDeclaration(JavaResourceVariableDeclarationNode node) {
        setChild(DECLARATION, node);
    }
}

