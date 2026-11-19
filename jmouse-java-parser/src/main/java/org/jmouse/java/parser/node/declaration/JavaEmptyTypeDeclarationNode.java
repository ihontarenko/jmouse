package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed empty-type-declaration syntax, with role-owned children in source order. */
public final class JavaEmptyTypeDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaEmptyTypeDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.EMPTY_TYPE_DECLARATION, JavaEmptyTypeDeclarationNode.class);
    public static final NodeSchema<JavaEmptyTypeDeclarationNode> SCHEMA = NodeSchema.of(KIND);

    public JavaEmptyTypeDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }
}

