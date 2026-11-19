package org.jmouse.java.parser.node.name;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed modifiers syntax, with role-owned children in source order. */
public final class JavaModifiersNode extends JavaSyntaxNode {
    public static final NodeKind<JavaModifiersNode> KIND = new NodeKind<>(JavaNodeKindNames.MODIFIERS, JavaModifiersNode.class);
    public static final ChildRole<JavaSyntaxNode> ENTRIES = ChildRole.list(JavaChildRoleNames.ENTRIES, JavaSyntaxNode.class);
    public static final NodeSchema<JavaModifiersNode> SCHEMA = NodeSchema.of(KIND, ENTRIES);

    public JavaModifiersNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaSyntaxNode> entries() {
        return children(ENTRIES);
    }

    public void addEntry(JavaSyntaxNode node) {
        addChild(ENTRIES, node);
    }
}

