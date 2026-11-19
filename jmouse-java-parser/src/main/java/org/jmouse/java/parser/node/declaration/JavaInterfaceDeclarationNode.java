package org.jmouse.java.parser.node.declaration;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed interface-declaration syntax, with role-owned children in source order. */
public final class JavaInterfaceDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaInterfaceDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.INTERFACE_DECLARATION, JavaInterfaceDeclarationNode.class);
    public static final NodeSchema<JavaInterfaceDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaInterfaceDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

}

