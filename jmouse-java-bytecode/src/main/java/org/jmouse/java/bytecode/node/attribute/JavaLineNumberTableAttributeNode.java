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

public final class JavaLineNumberTableAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaLineNumberTableAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LINE_NUMBER_TABLE_ATTRIBUTE_NODE,
                    JavaLineNumberTableAttributeNode.class);
    public static final ChildRole<JavaLineNumberNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaLineNumberNode.class);
    public static final NodeSchema<JavaLineNumberTableAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaLineNumberTableAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaLineNumberNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaLineNumberNode> entries() {
        return children(ENTRIES);
    }
}
