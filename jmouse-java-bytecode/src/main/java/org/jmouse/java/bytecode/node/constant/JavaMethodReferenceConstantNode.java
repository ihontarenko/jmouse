package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaMethodReferenceConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaMethodReferenceConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.METHODREF.name(),
                    JavaMethodReferenceConstantNode.class);
    public static final NodeSchema<JavaMethodReferenceConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int classIndex;
    private final int nameAndTypeIndex;

    public JavaMethodReferenceConstantNode(
            BytesSource source,
            BytecodeSpan span,
            int index,
            int classIndex,
            int nameAndTypeIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.METHODREF);
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
