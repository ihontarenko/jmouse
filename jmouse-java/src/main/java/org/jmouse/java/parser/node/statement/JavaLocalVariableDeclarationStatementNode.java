package org.jmouse.java.parser.node.statement;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed local-variable-declaration-statement syntax, with role-owned children in source order. */
public final class JavaLocalVariableDeclarationStatementNode extends JavaStatementNode {
    public static final NodeKind<JavaLocalVariableDeclarationStatementNode> KIND = new NodeKind<>(JavaNodeKindNames.LOCAL_VARIABLE_DECLARATION_STATEMENT, JavaLocalVariableDeclarationStatementNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaVariableDeclaratorNode> VARIABLES = ChildRole.list(JavaChildRoleNames.VARIABLES, JavaVariableDeclaratorNode.class);
    public static final NodeSchema<JavaLocalVariableDeclarationStatementNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, VARIABLES);

    public JavaLocalVariableDeclarationStatementNode(String sourceName, SourceSpan span) {
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

