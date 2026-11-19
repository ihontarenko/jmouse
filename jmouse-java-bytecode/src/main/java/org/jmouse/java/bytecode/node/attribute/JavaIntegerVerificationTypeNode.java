package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaIntegerVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaIntegerVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.INTEGER_VERIFICATION_TYPE_NODE,
                    JavaIntegerVerificationTypeNode.class);
    public static final NodeSchema<JavaIntegerVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaIntegerVerificationTypeNode(BytesSource source, BytecodeSpan span) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.INTEGER);
    }
}
