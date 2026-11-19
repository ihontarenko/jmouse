package org.jmouse.java.parser.node.name;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed qualified-name syntax, with role-owned children in source order. */
public final class JavaQualifiedNameNode extends JavaSyntaxNode {
    public static final NodeKind<JavaQualifiedNameNode> KIND = new NodeKind<>(JavaNodeKindNames.QUALIFIED_NAME, JavaQualifiedNameNode.class);
    public static final ChildRole<JavaIdentifierNode> SEGMENTS = ChildRole.list(JavaChildRoleNames.SEGMENTS, JavaIdentifierNode.class);
    public static final NodeSchema<JavaQualifiedNameNode> SCHEMA = NodeSchema.of(KIND, SEGMENTS);

    public JavaQualifiedNameNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaIdentifierNode> segments() {
        return children(SEGMENTS);
    }

    public void addSegment(JavaIdentifierNode node) {
        addChild(SEGMENTS, node);
    }
}

