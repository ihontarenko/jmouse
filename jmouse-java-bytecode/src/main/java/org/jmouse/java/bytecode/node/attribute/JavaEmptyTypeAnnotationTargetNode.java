package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaEmptyTypeAnnotationTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaEmptyTypeAnnotationTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.EMPTY_TYPE_ANNOTATION_TARGET_NODE,
                    JavaEmptyTypeAnnotationTargetNode.class);

    public static final NodeSchema<JavaEmptyTypeAnnotationTargetNode> SCHEMA = NodeSchema.of(KIND);

    public JavaEmptyTypeAnnotationTargetNode(
            BytesSource source, BytecodeSpan span, JvmTypeAnnotationTarget targetType) {
        super(SCHEMA, source, span, targetType);
    }
}
