package org.jmouse.java.parser.java27;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.unit.JavaImportNode;
import org.jmouse.java.parser.source.SourceSpan;

public final class Java27BundleImportNode extends JavaImportNode {
    public static final NodeKind<Java27BundleImportNode> KIND =
            new NodeKind<>(Java27Names.IMPORT_NODE, Java27BundleImportNode.class);
    public static final ChildRole<JavaQualifiedNameNode> CHILD =
            ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final NodeSchema<Java27BundleImportNode> SCHEMA = NodeSchema.of(KIND, CHILD);

    public Java27BundleImportNode(String name, SourceSpan span) {
        super(SCHEMA, name, span);
    }

    public JavaQualifiedNameNode name() {
        return child(CHILD);
    }

    public void setName(JavaQualifiedNameNode value) {
        setChild(CHILD, value);
    }
}
