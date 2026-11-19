package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaModuleConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaModuleConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.MODULE.name(),
                    JavaModuleConstantNode.class);
    public static final NodeSchema<JavaModuleConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int nameIndex;

    public JavaModuleConstantNode(BytesSource source, BytecodeSpan span, int index, int nameIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.MODULE);
        this.nameIndex = nameIndex;
    }

    public int nameIndex() {
        return nameIndex;
    }
}
