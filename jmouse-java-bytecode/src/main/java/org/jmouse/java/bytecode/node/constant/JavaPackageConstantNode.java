package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaPackageConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaPackageConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.PACKAGE.name(),
                    JavaPackageConstantNode.class);
    public static final NodeSchema<JavaPackageConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int nameIndex;

    public JavaPackageConstantNode(
            BytesSource source, BytecodeSpan span, int index, int nameIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.PACKAGE);
        this.nameIndex = nameIndex;
    }

    public int nameIndex() {
        return nameIndex;
    }
}
