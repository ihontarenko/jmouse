package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaConstantValueAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaConstantValueAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.CONSTANT_VALUE_ATTRIBUTE_NODE,
                    JavaConstantValueAttributeNode.class);

    public static final NodeSchema<JavaConstantValueAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;

    public JavaConstantValueAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int constantIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.constantIndex = constantIndex;
    }

    public int constantIndex() {
        return constantIndex;
    }
}
