package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import java.util.List;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.el.node.ChildRole;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The TypeDeclaration family in Java source syntax. */
public abstract class JavaTypeDeclarationNode extends JavaDeclarationNode {
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaTypeParameterNode> TYPE_PARAMETERS = ChildRole.list(JavaChildRoleNames.TYPE_PARAMETERS, JavaTypeParameterNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> INTERFACES = ChildRole.list(JavaChildRoleNames.INTERFACES, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> PERMITTED_TYPES = ChildRole.list(JavaChildRoleNames.PERMITTED_TYPES, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaTypeBodyNode> BODY = ChildRole.required(JavaChildRoleNames.BODY, JavaTypeBodyNode.class);

    protected JavaTypeDeclarationNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }

    public final JavaModifiersNode modifiers() {
        return child(MODIFIERS);
    }

    public final void setModifiers(JavaModifiersNode node) {
        setChild(MODIFIERS, node);
    }

    public final JavaIdentifierNode name() {
        return child(NAME);
    }

    public final void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public final List<JavaTypeParameterNode> typeParameters() {
        return children(TYPE_PARAMETERS);
    }

    public final void addTypeParameter(JavaTypeParameterNode node) {
        addChild(TYPE_PARAMETERS, node);
    }

    public final List<JavaTypeSyntaxNode> interfaces() {
        return children(INTERFACES);
    }

    public final void addInterfaceType(JavaTypeSyntaxNode node) {
        addChild(INTERFACES, node);
    }

    public final List<JavaTypeSyntaxNode> permittedTypes() {
        return children(PERMITTED_TYPES);
    }

    public final void addPermittedType(JavaTypeSyntaxNode node) {
        addChild(PERMITTED_TYPES, node);
    }

    public final JavaTypeBodyNode body() {
        return child(BODY);
    }

    public final void setBody(JavaTypeBodyNode node) {
        setChild(BODY, node);
    }
}
