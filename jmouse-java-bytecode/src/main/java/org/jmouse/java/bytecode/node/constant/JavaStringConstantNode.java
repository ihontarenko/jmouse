package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaStringConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaStringConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.STRING.name(),
                    JavaStringConstantNode.class);
    public static final NodeSchema<JavaStringConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int stringIndex;

    public JavaStringConstantNode(
            BytesSource source, BytecodeSpan span, int index, int stringIndex) {
        super(SCHEMA, source, span, index, JvmConstantTag.STRING);
        this.stringIndex = stringIndex;
    }

    public int stringIndex() {
        return stringIndex;
    }
}
