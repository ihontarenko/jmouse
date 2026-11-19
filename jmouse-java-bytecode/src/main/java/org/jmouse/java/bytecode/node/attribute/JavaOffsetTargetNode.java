package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaOffsetTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaOffsetTargetNode> KIND =
            new NodeKind<>(JvmNodeNames.OFFSET_TARGET_NODE, JavaOffsetTargetNode.class);

    public static final NodeSchema<JavaOffsetTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int offset;

    public JavaOffsetTargetNode(
            BytesSource source, BytecodeSpan span, JvmTypeAnnotationTarget targetType, int offset) {
        super(SCHEMA, source, span, targetType);
        this.offset = offset;
    }

    public int offset() {
        return offset;
    }
}
