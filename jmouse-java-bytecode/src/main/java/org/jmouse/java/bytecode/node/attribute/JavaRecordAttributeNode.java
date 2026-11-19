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

public final class JavaRecordAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaRecordAttributeNode> KIND =
            new NodeKind<>(JvmNodeNames.RECORD_ATTRIBUTE_NODE, JavaRecordAttributeNode.class);
    public static final ChildRole<JavaRecordComponentNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaRecordComponentNode.class);
    public static final NodeSchema<JavaRecordAttributeNode> SCHEMA = NodeSchema.of(KIND, ENTRIES);

    public JavaRecordAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaRecordComponentNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaRecordComponentNode> entries() {
        return children(ENTRIES);
    }
}
