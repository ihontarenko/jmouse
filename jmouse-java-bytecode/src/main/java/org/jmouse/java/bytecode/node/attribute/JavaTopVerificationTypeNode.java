package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaTopVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaTopVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.TOP_VERIFICATION_TYPE_NODE, JavaTopVerificationTypeNode.class);
    public static final NodeSchema<JavaTopVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaTopVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.TOP);
    }
}
