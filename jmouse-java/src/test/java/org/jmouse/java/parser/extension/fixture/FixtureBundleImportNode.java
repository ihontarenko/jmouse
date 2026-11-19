package org.jmouse.java.parser.extension.fixture;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.unit.JavaImportNode;
import org.jmouse.java.parser.source.SourceSpan;

final class FixtureBundleImportNode extends JavaImportNode {
    static final NodeKind<FixtureBundleImportNode> KIND =
            new NodeKind<>(FixtureNames.BUNDLE_KIND, FixtureBundleImportNode.class);
    static final ChildRole<JavaQualifiedNameNode> NAME =
            ChildRole.required(FixtureNames.NAME_CHILD, JavaQualifiedNameNode.class);
    static final NodeSchema<FixtureBundleImportNode> SCHEMA = NodeSchema.of(KIND, NAME);

    FixtureBundleImportNode(String name, SourceSpan span) {
        super(SCHEMA, name, span);
    }

    void setName(JavaQualifiedNameNode name) {
        setChild(NAME, name);
    }
}
