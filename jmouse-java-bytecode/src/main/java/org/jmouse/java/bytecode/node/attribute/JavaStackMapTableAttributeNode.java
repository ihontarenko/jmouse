package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaStackMapTableAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaStackMapTableAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.STACK_MAP_TABLE_ATTRIBUTE_NODE,
                    JavaStackMapTableAttributeNode.class);
    public static final ChildRole<JavaStackMapFrameNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaStackMapFrameNode.class);
    public static final NodeSchema<JavaStackMapTableAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaStackMapTableAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaStackMapFrameNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaStackMapFrameNode> entries() {
        return children(ENTRIES);
    }
}
