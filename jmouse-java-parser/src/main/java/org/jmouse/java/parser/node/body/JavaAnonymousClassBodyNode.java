package org.jmouse.java.parser.node.body;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed anonymous-class-body syntax, with role-owned children in source order. */
public final class JavaAnonymousClassBodyNode extends JavaSyntaxNode {
    public static final NodeKind<JavaAnonymousClassBodyNode> KIND = new NodeKind<>(JavaNodeKindNames.ANONYMOUS_CLASS_BODY, JavaAnonymousClassBodyNode.class);
    public static final ChildRole<JavaDeclarationNode> DECLARATIONS = ChildRole.list(JavaChildRoleNames.DECLARATIONS, JavaDeclarationNode.class);
    public static final NodeSchema<JavaAnonymousClassBodyNode> SCHEMA = NodeSchema.of(KIND, DECLARATIONS);

    public JavaAnonymousClassBodyNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaDeclarationNode> declarations() {
        return children(DECLARATIONS);
    }

    public void addDeclaration(JavaDeclarationNode node) {
        addChild(DECLARATIONS, node);
    }
}

