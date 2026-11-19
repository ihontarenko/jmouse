package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaLongVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaLongVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LONG_VERIFICATION_TYPE_NODE, JavaLongVerificationTypeNode.class);
    public static final NodeSchema<JavaLongVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaLongVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.LONG);
    }
}
