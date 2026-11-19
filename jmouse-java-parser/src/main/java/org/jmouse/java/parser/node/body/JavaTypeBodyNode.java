package org.jmouse.java.parser.node.body;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed type-body syntax, with role-owned children in source order. */
public final class JavaTypeBodyNode extends JavaSyntaxNode {
    public static final NodeKind<JavaTypeBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.TYPE_BODY, JavaTypeBodyNode.class);
    public static final ChildRole<JavaDeclarationNode> DECLARATIONS = ChildRole.list(JavaChildRoleNames.DECLARATIONS, JavaDeclarationNode.class);
    public static final ChildRole<JavaEnumConstantNode> CONSTANTS = ChildRole.list(JavaChildRoleNames.CONSTANTS, JavaEnumConstantNode.class);
    public static final NodeSchema<JavaTypeBodyNode> SCHEMA = NodeSchema.of(KIND, CONSTANTS, DECLARATIONS);

    public List<JavaEnumConstantNode> constants() {
        return children(CONSTANTS);
    }

    public void addConstant(JavaEnumConstantNode constant) {
        addChild(CONSTANTS, constant);
    }

    public JavaTypeBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaDeclarationNode> declarations() {
        return children(DECLARATIONS);
    }

    public void addDeclaration(JavaDeclarationNode node) {
        addChild(DECLARATIONS, node);
    }
}

