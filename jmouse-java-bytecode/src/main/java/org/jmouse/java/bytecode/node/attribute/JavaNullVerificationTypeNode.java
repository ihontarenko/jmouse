package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaNullVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaNullVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.NULL_VERIFICATION_TYPE_NODE, JavaNullVerificationTypeNode.class);
    public static final NodeSchema<JavaNullVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaNullVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.NULL);
    }
}
