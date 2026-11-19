package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaIntegerConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaIntegerConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.INTEGER.name(),
                    JavaIntegerConstantNode.class);
    public static final NodeSchema<JavaIntegerConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final int value;

    public JavaIntegerConstantNode(BytesSource source, BytecodeSpan span, int index, int value) {
        super(SCHEMA, source, span, index, JvmConstantTag.INTEGER);
        this.value = value;
    }

    public int value() {
        return value;
    }
}
