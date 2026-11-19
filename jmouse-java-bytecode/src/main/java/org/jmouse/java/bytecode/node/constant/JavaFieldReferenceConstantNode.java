package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaFieldReferenceConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaFieldReferenceConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.FIELDREF.name(),
                    JavaFieldReferenceConstantNode.class);
    public static final NodeSchema<JavaFieldReferenceConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int classIndex;
    private final int nameAndTypeIndex;

    public JavaFieldReferenceConstantNode(
            BytesSource source,
            BytecodeSpan span,
            int index,
            int classIndex,
            int nameAndTypeIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.FIELDREF);
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
