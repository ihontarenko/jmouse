package org.jmouse.java.parser.node.expression;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed super-qualifier syntax, with role-owned children in source order. */
public final class JavaSuperQualifierNode extends JavaExpressionNode {
    public static final NodeKind<JavaSuperQualifierNode> KIND = new NodeKind<>(JavaNodeKindNames.SUPER_QUALIFIER, JavaSuperQualifierNode.class);
    public static final ChildRole<JavaQualifiedNameNode> QUALIFIER = ChildRole.optional(JavaChildRoleNames.QUALIFIER, JavaQualifiedNameNode.class);
    public static final NodeSchema<JavaSuperQualifierNode> SCHEMA = NodeSchema.of(KIND, QUALIFIER);

    public JavaSuperQualifierNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public Optional<JavaQualifiedNameNode> qualifier() {
        return optionalChild(QUALIFIER);
    }

    public void setQualifier(JavaQualifiedNameNode node) {
        setChild(QUALIFIER, node);
    }
}

