package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaNestHostAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaNestHostAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.NEST_HOST_ATTRIBUTE_NODE, JavaNestHostAttributeNode.class);

    public static final NodeSchema<JavaNestHostAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final int hostClassIndex;

    public JavaNestHostAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int hostClassIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.hostClassIndex = hostClassIndex;
    }

    public int hostClassIndex() {
        return hostClassIndex;
    }
}
