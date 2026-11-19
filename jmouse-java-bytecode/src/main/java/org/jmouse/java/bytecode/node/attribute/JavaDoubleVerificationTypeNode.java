package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaDoubleVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaDoubleVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.DOUBLE_VERIFICATION_TYPE_NODE,
                    JavaDoubleVerificationTypeNode.class);
    public static final NodeSchema<JavaDoubleVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaDoubleVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.DOUBLE);
    }
}
