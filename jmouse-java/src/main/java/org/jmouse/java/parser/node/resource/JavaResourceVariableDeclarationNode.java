package org.jmouse.java.parser.node.resource;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed resource-variable-declaration syntax, with role-owned children in source order. */
public final class JavaResourceVariableDeclarationNode extends JavaSyntaxNode {
    public static final NodeKind<JavaResourceVariableDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.RESOURCE_VARIABLE_DECLARATION, JavaResourceVariableDeclarationNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaIdentifierNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaIdentifierNode.class);
    public static final ChildRole<JavaExpressionNode> INITIALIZER = ChildRole.required(JavaChildRoleNames.INITIALIZER, JavaExpressionNode.class);
    public static final NodeSchema<JavaResourceVariableDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, NAME, INITIALIZER);

    public JavaResourceVariableDeclarationNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaModifiersNode modifiers() {
        return child(MODIFIERS);
    }

    public void setModifiers(JavaModifiersNode node) {
        setChild(MODIFIERS, node);
    }

    public JavaTypeSyntaxNode type() {
        return child(TYPE);
    }

    public void setType(JavaTypeSyntaxNode node) {
        setChild(TYPE, node);
    }

    public JavaIdentifierNode name() {
        return child(NAME);
    }

    public void setName(JavaIdentifierNode node) {
        setChild(NAME, node);
    }

    public JavaExpressionNode initializer() {
        return child(INITIALIZER);
    }

    public void setInitializer(JavaExpressionNode node) {
        setChild(INITIALIZER, node);
    }
}

