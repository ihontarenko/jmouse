package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaConstantAnnotationValueNode extends JavaAnnotationValueNode {
    public static final NodeKind<JavaConstantAnnotationValueNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.CONSTANT_ANNOTATION_VALUE_NODE,
                    JavaConstantAnnotationValueNode.class);

    public static final NodeSchema<JavaConstantAnnotationValueNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;

    public JavaConstantAnnotationValueNode(
            BytesSource source, BytecodeSpan span, JvmAnnotationValueTag tag, int constantIndex) {
        super(SCHEMA, source, span, tag);
        this.constantIndex = constantIndex;
    }

    public int constantIndex() {
        return constantIndex;
    }
}
