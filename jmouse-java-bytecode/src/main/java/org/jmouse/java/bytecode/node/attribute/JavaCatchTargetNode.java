package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaCatchTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaCatchTargetNode> KIND =
            new NodeKind<>(JvmNodeNames.CATCH_TARGET_NODE, JavaCatchTargetNode.class);

    public static final NodeSchema<JavaCatchTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int exceptionIndex;

    public JavaCatchTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int exceptionIndex) {
        super(SCHEMA, source, span, targetType);
        this.exceptionIndex = exceptionIndex;
    }

    public int exceptionIndex() {
        return exceptionIndex;
    }
}
