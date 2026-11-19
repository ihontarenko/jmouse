package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaUninitializedThisVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaUninitializedThisVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.UNINITIALIZED_THIS_VERIFICATION_TYPE_NODE,
                    JavaUninitializedThisVerificationTypeNode.class);
    public static final NodeSchema<JavaUninitializedThisVerificationTypeNode> SCHEMA =
            NodeSchema.of(KIND);

    public JavaUninitializedThisVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.UNINITIALIZED_THIS);
    }
}
