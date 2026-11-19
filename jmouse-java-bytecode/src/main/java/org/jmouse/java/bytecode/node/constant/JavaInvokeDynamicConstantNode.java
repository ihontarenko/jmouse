package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaInvokeDynamicConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaInvokeDynamicConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.INVOKE_DYNAMIC.name(),
                    JavaInvokeDynamicConstantNode.class);
    public static final NodeSchema<JavaInvokeDynamicConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int bootstrapMethodIndex;
    private final int nameAndTypeIndex;

    public JavaInvokeDynamicConstantNode(
            BytesSource source,
            BytecodeSpan span,
            int index,
            int bootstrapMethodIndex,
            int nameAndTypeIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.INVOKE_DYNAMIC);
        this.bootstrapMethodIndex = bootstrapMethodIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public int bootstrapMethodIndex() {
        return bootstrapMethodIndex;
    }

    public int nameAndTypeIndex() {
        return nameAndTypeIndex;
    }
}
