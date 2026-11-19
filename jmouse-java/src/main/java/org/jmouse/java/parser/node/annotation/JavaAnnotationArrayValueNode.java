package org.jmouse.java.parser.node.annotation;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaChildRoleNames;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed annotation-array-value syntax, with role-owned children in source order. */
public final class JavaAnnotationArrayValueNode extends JavaSyntaxNode {
    public static final NodeKind<JavaAnnotationArrayValueNode> KIND = new NodeKind<>(JavaNodeKindNames.ANNOTATION_ARRAY_VALUE, JavaAnnotationArrayValueNode.class);
    public static final ChildRole<JavaSyntaxNode> VALUES = ChildRole.list(JavaChildRoleNames.VALUES, JavaSyntaxNode.class);
    public static final NodeSchema<JavaAnnotationArrayValueNode> SCHEMA = NodeSchema.of(KIND, VALUES);

    public JavaAnnotationArrayValueNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaSyntaxNode> values() {
        return children(VALUES);
    }

    public void addValue(JavaSyntaxNode node) {
        addChild(VALUES, node);
    }
}

