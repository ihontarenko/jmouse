package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed empty-member-declaration syntax, with role-owned children in source order. */
public final class JavaEmptyMemberDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaEmptyMemberDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.EMPTY_MEMBER_DECLARATION, JavaEmptyMemberDeclarationNode.class);
    public static final NodeSchema<JavaEmptyMemberDeclarationNode> SCHEMA = NodeSchema.of(KIND);

    public JavaEmptyMemberDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

