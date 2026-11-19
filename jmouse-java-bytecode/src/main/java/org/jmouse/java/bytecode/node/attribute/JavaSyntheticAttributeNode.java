package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaSyntheticAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaSyntheticAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.SYNTHETIC_ATTRIBUTE_NODE, JavaSyntheticAttributeNode.class);

    public static final NodeSchema<JavaSyntheticAttributeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaSyntheticAttributeNode(
            BytesSource source, BytecodeSpan span, JavaConstantPoolNode pool, int nameIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
    }
}
