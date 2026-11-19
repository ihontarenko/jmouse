package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaClassConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaClassConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.CLASS.name(),
                    JavaClassConstantNode.class);
    public static final NodeSchema<JavaClassConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int nameIndex;

    public JavaClassConstantNode(BytesSource source, BytecodeSpan span, int index, int nameIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.CLASS);
        this.nameIndex = nameIndex;
    }

    public int nameIndex() {
        return nameIndex;
    }
}
