package org.jmouse.java.parser.node.module;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** The package and optional target modules shared by exports and opens. */
public abstract class JavaPackageAccessDirectiveNode extends JavaModuleDirectiveNode {
    public static final ChildRole<JavaQualifiedNameNode> NAME = ChildRole.required(JavaChildRoleNames.NAME, JavaQualifiedNameNode.class);
    public static final ChildRole<JavaQualifiedNameNode> TARGETS = ChildRole.list(JavaChildRoleNames.TARGETS, JavaQualifiedNameNode.class);

    protected JavaPackageAccessDirectiveNode(NodeSchema<?> schema, String sourceName, SourceSpan span) {
        super(schema, sourceName, span);
    }

    public final JavaQualifiedNameNode name() {
        return child(NAME);
    }

    public final void setName(JavaQualifiedNameNode name) {
        setChild(NAME, name);
    }

    public final List<JavaQualifiedNameNode> targets() {
        return children(TARGETS);
    }

    public final void addTarget(JavaQualifiedNameNode target) {
        addChild(TARGETS, target);
    }
}
