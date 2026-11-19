package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaUnknownAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaUnknownAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.UNKNOWN_ATTRIBUTE_NODE, JavaUnknownAttributeNode.class);

    public static final NodeSchema<JavaUnknownAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final BytecodeSpan payload;

    public JavaUnknownAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            BytecodeSpan payload) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.payload = payload;
    }

    public BytecodeSpan payload() {
        return payload;
    }
}
