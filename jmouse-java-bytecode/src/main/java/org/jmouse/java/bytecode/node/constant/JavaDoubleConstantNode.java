package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaDoubleConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaDoubleConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.DOUBLE.name(),
                    JavaDoubleConstantNode.class);
    public static final NodeSchema<JavaDoubleConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final long bits;

    public JavaDoubleConstantNode(BytesSource source, BytecodeSpan span, int index, long bits) {
        super(SCHEMA, source, span, index, JvmConstantTag.DOUBLE);
        this.bits = bits;
    }

    public long bits() {
        return bits;
    }
}
