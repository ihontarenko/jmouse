package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaNameAndTypeConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaNameAndTypeConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.NAME_AND_TYPE.name(),
                    JavaNameAndTypeConstantNode.class);
    public static final NodeSchema<JavaNameAndTypeConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int nameIndex;
    private final int descriptorIndex;

    public JavaNameAndTypeConstantNode(
            BytesSource source, BytecodeSpan span, int index, int nameIndex, int descriptorIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.NAME_AND_TYPE);
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
    }

    public int nameIndex() {
        return nameIndex;
    }

    public int descriptorIndex() {
        return descriptorIndex;
    }
}
