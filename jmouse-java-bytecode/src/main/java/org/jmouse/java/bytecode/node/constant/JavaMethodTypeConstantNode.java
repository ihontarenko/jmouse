package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaMethodTypeConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaMethodTypeConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.METHOD_TYPE.name(),
                    JavaMethodTypeConstantNode.class);
    public static final NodeSchema<JavaMethodTypeConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int descriptorIndex;

    public JavaMethodTypeConstantNode(
            BytesSource source, BytecodeSpan span, int index, int descriptorIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.METHOD_TYPE);
        this.descriptorIndex = descriptorIndex;
    }

    public int descriptorIndex() {
        return descriptorIndex;
    }
}
