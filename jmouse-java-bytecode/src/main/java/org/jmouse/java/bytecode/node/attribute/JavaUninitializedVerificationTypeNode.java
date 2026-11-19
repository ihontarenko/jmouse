package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaUninitializedVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaUninitializedVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.UNINITIALIZED_VERIFICATION_TYPE_NODE,
                    JavaUninitializedVerificationTypeNode.class);
    public static final NodeSchema<JavaUninitializedVerificationTypeNode> SCHEMA =
            NodeSchema.of(KIND);
    private final int newOffset;

    public JavaUninitializedVerificationTypeNode(
            BytesSource source, BytecodeSpan span, int newOffset) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.UNINITIALIZED);
        this.newOffset = newOffset;
    }

    public int newOffset() {
        return newOffset;
    }
}
