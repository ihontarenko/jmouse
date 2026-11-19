package org.jmouse.java.parser.node.declaration;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed field-declaration syntax, with role-owned children in source order. */
public final class JavaFieldDeclarationNode extends JavaDeclarationNode {
    public static final NodeKind<JavaFieldDeclarationNode> KIND = new NodeKind<>(JavaNodeKindNames.FIELD_DECLARATION, JavaFieldDeclarationNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaVariableDeclaratorNode> VARIABLES = ChildRole.list(JavaChildRoleNames.VARIABLES, JavaVariableDeclaratorNode.class);
    public static final NodeSchema<JavaFieldDeclarationNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, VARIABLES);

    public JavaFieldDeclarationNode(String sourceName, SourceSpan span) {
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

    public List<JavaVariableDeclaratorNode> variables() {
        return children(VARIABLES);
    }

    public void addVariable(JavaVariableDeclaratorNode node) {
        addChild(VARIABLES, node);
    }
}

