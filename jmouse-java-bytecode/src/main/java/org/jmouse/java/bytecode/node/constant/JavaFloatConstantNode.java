package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaFloatConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaFloatConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.FLOAT.name(),
                    JavaFloatConstantNode.class);
    public static final NodeSchema<JavaFloatConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int bits;

    public JavaFloatConstantNode(BytesSource source, BytecodeSpan span, int index, int bits) {
        super(SCHEMA, source, span, index, JvmConstantTag.FLOAT);
        this.bits = bits;
    }

    public int bits() {
        return bits;
    }
}
