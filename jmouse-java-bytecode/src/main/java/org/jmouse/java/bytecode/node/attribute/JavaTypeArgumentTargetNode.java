package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaTypeArgumentTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaTypeArgumentTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.TYPE_ARGUMENT_TARGET_NODE, JavaTypeArgumentTargetNode.class);

    public static final NodeSchema<JavaTypeArgumentTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int offset;
    private final int argumentIndex;

    public JavaTypeArgumentTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int offset,
            int argumentIndex) {
        super(SCHEMA, source, span, targetType);
        this.offset = offset;
        this.argumentIndex = argumentIndex;
    }

    public int offset() {
        return offset;
    }

    public int argumentIndex() {
        return argumentIndex;
    }
}
