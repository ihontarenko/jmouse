package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed interface-declaration syntax, with role-owned children in source order. */
public final class JavaInterfaceDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaInterfaceDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.INTERFACE_DECLARATION, JavaInterfaceDeclarationNode.class);
    public static final NodeSchema<JavaInterfaceDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaInterfaceDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

}

