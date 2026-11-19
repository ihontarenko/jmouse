package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;
import org.jmouse.java.bytecode.vocabulary.JvmReferenceKind;

public final class JavaMethodHandleConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaMethodHandleConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.METHOD_HANDLE.name(),
                    JavaMethodHandleConstantNode.class);
    public static final NodeSchema<JavaMethodHandleConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final JvmReferenceKind referenceKind;
    private final int referenceIndex;

    public JavaMethodHandleConstantNode(
            BytesSource source,
            BytecodeSpan span,
            int index,
            JvmReferenceKind referenceKind,
            int referenceIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.METHOD_HANDLE);
        this.referenceKind = referenceKind;
        this.referenceIndex = referenceIndex;
    }

    public JvmReferenceKind referenceKind() {
        return referenceKind;
    }

    public int referenceIndex() {
        return referenceIndex;
    }
}
