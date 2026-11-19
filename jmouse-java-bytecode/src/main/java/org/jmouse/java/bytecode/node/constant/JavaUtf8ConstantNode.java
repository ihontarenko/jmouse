package org.jmouse.java.bytecode.node.constant;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;

public final class JavaUtf8ConstantNode extends JavaConstantNode {
    public static final NodeKind<JavaUtf8ConstantNode> KIND =
            new NodeKind<>(
                    JavaBytecodeNames.NODE_NAMESPACE + JvmConstantTag.UTF8.name(),
                    JavaUtf8ConstantNode.class);
    public static final NodeSchema<JavaUtf8ConstantNode> SCHEMA = NodeSchema.of(KIND);
    private final String value;

    public JavaUtf8ConstantNode(BytesSource source, BytecodeSpan span, int index, String value) {
        super(SCHEMA, source, span, index, JvmConstantTag.UTF8);
        this.value = value;
    }

    public String value() {
        return value;
    }
}
