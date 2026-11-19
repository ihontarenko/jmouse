package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaDeprecatedAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaDeprecatedAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.DEPRECATED_ATTRIBUTE_NODE, JavaDeprecatedAttributeNode.class);

    public static final NodeSchema<JavaDeprecatedAttributeNode> SCHEMA = NodeSchema.of(KIND);

    public JavaDeprecatedAttributeNode(
            BytesSource source, BytecodeSpan span, JavaConstantPoolNode pool, int nameIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
    }
}
