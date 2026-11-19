package org.jmouse.java.parser.node.resource;

import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed resource-reference syntax, with role-owned children in source order. */
public final class JavaResourceReferenceNode extends JavaResourceNode {
    public static final NodeKind<JavaResourceReferenceNode> KIND = new NodeKind<>(JavaNodeKindNames.RESOURCE_REFERENCE, JavaResourceReferenceNode.class);
    public static final ChildRole<JavaExpressionNode> REFERENCE = ChildRole.required(JavaChildRoleNames.REFERENCE, JavaExpressionNode.class);
    public static final NodeSchema<JavaResourceReferenceNode> SCHEMA = NodeSchema.of(KIND, REFERENCE);

    public JavaResourceReferenceNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaExpressionNode reference() {
        return child(REFERENCE);
    }

    public void setReference(JavaExpressionNode node) {
        setChild(REFERENCE, node);
    }
}

