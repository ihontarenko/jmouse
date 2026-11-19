package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed class-declaration syntax, with role-owned children in source order. */
public final class JavaClassDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaClassDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.CLASS_DECLARATION, JavaClassDeclarationNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> SUPERCLASS = ChildRole.optional(JavaChildRoleNames.SUPERCLASS, JavaTypeSyntaxNode.class);
    public static final NodeSchema<JavaClassDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, SUPERCLASS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaClassDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaTypeSyntaxNode> superclass() {
        return optionalChild(SUPERCLASS);
    }

    public void setSuperclass(JavaTypeSyntaxNode node) {
        setChild(SUPERCLASS, node);
    }

}

