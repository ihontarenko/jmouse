package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaFloatVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaFloatVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.FLOAT_VERIFICATION_TYPE_NODE, JavaFloatVerificationTypeNode.class);
    public static final NodeSchema<JavaFloatVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaFloatVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.FLOAT);
    }
}
