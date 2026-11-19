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

/** Typed annotation-type-declaration syntax, with role-owned children in source order. */
public final class JavaAnnotationTypeDeclarationNode extends JavaTypeDeclarationNode {
    public static final NodeKind<JavaAnnotationTypeDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.ANNOTATION_TYPE_DECLARATION, JavaAnnotationTypeDeclarationNode.class);
    public static final NodeSchema<JavaAnnotationTypeDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME, TYPE_PARAMETERS, INTERFACES, PERMITTED_TYPES, BODY);

    public JavaAnnotationTypeDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

}

