package org.jmouse.java.parser.node.module;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed requires-directive syntax, with role-owned children in source order. */
public final class JavaRequiresDirectiveNode extends JavaModuleDirectiveNode {
    public static final NodeKind<JavaRequiresDirectiveNode> KIND = new NodeKind<>(JavaNodeKindNames.REQUIRES_DIRECTIVE, JavaRequiresDirectiveNode.class);
    public static final ChildRole<JavaModifiersNode> MODIFIERS = ChildRole.required(JavaChildRoleNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaRequiresDirectiveNode> SCHEMA = NodeSchema.of(KIND, MODIFIERS, NAME);

    public JavaRequiresDirectiveNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaModifiersNode modifiers() {
        return child(MODIFIERS);
    }

    public void setModifiers(JavaModifiersNode node) {
        setChild(MODIFIERS, node);
    }

    public JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public void setName(JavaQualifiedNameNode node) {
        setChild(NAME, node);
    }
}

