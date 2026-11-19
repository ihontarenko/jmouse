package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmVerificationTypeTag;

public final class JavaObjectVerificationTypeNode extends JavaVerificationTypeNode {
    public static final NodeKind<JavaObjectVerificationTypeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.OBJECT_VERIFICATION_TYPE_NODE,
                    JavaObjectVerificationTypeNode.class);
    public static final NodeSchema<JavaObjectVerificationTypeNode> SCHEMA = NodeSchema.of(KIND);
    private final int classIndex;

    public JavaObjectVerificationTypeNode(BytesSource source, BytecodeSpan span, int classIndex) {
        super(SCHEMA, source, span, JvmVerificationTypeTag.OBJECT);
        this.classIndex = classIndex;
    }

    public int classIndex() {
        return classIndex;
    }
}
