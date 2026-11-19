package org.jmouse.java.bytecode.node;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaFieldNode extends JavaMemberNode {
    public static final NodeKind<JavaFieldNode> KIND =
            new NodeKind<>(JvmNodeNames.FIELD_NODE, JavaFieldNode.class);
    public static final NodeSchema<JavaFieldNode> SCHEMA = NodeSchema.of(KIND, ATTRIBUTES);

    public JavaFieldNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int accessFlags,
            int nameIndex,
            int descriptorIndex,
            List<JavaAttributeNode> attributes) {
        super(SCHEMA, source, span, pool, accessFlags, nameIndex, descriptorIndex, attributes);
    }
}
