package org.jmouse.java.parser.node.pattern;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed type-pattern syntax, with role-owned children in source order. */
public final class JavaTypePatternNode extends JavaPatternNode {
    public static final NodeKind<JavaTypePatternNode> KIND = new NodeKind<>(JavaNodeKindNames.TYPE_PATTERN, JavaTypePatternNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaTypeSyntaxNode> TYPE = ChildRole.required(JavaChildRoleNames.TYPE, JavaTypeSyntaxNode.class);
    public static final ChildRole<JavaIdentifierNode> BINDING = ChildRole.required(JavaChildRoleNames.BINDING, JavaIdentifierNode.class);
    public static final NodeSchema<JavaTypePatternNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, TYPE, BINDING);

    public JavaTypePatternNode(String sourceName, SourceSpan span) {
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

    public JavaIdentifierNode binding() {
        return child(BINDING);
    }

    public void setBinding(JavaIdentifierNode node) {
        setChild(BINDING, node);
    }
}

