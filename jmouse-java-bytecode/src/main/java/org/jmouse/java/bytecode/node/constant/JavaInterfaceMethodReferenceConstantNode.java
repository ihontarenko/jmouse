package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaInterfaceMethodReferenceConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaInterfaceMethodReferenceConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE
                            + JvmConstantTag.INTERFACE_METHOD_REFERENCE.name(),
                    JavaInterfaceMethodReferenceConstantNode.class);
    public static final NodeSchema<JavaInterfaceMethodReferenceConstantNode> SCHEMA =
            NodeSchema.of(KIND);
    private final int classIndex;
    private final int nameAndTypeIndex;

    public JavaInterfaceMethodReferenceConstantNode(
            BytesSource source,
            BytecodeSpan span,
            int index,
            int classIndex,
            int nameAndTypeIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.INTERFACE_METHOD_REFERENCE);
        this.classIndex = classIndex;
        this.nameAndTypeIndex = nameAndTypeIndex;
    }

    public int classIndex() {
        return classIndex;
    }

    public int nameAndTypeIndex() {
        return nameAndTypeIndex;
    }
}
