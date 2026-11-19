package org.jmouse.java.parser.node.statement;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed switch-block syntax, with role-owned children in source order. */
public final class JavaSwitchBlockNode extends JavaSyntaxNode {
    public static final NodeKind<JavaSwitchBlockNode> KIND = new NodeKind<>(JavaNodeKindNames.SWITCH_BLOCK, JavaSwitchBlockNode.class);
    public static final ChildRole<JavaSyntaxNode> ENTRIES = ChildRole.list(JavaChildRoleNames.ENTRIES, JavaSyntaxNode.class);
    public static final NodeSchema<JavaSwitchBlockNode> SCHEMA = NodeSchema.of(KIND, ENTRIES);

    public JavaSwitchBlockNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaSyntaxNode> entries() {
        return children(ENTRIES);
    }

    public void addEntry(JavaSyntaxNode node) {
        addChild(ENTRIES, node);
    }
}

