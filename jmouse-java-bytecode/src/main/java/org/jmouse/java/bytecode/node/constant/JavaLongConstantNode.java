package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaLongConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaLongConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.LONG.name(),
                    JavaLongConstantNode.class);
    public static final NodeSchema<JavaLongConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final long value;

    public JavaLongConstantNode(BytesSource source, BytecodeSpan span, int index, long value) {
        super(SCHEMA, source, span, index, JvmConstantTag.LONG);
        this.value = value;
    }

    public long value() {
        return value;
    }
}
