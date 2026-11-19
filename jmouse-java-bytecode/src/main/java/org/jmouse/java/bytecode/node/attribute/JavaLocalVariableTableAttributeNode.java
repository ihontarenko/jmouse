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

public final class JavaLocalVariableTableAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaLocalVariableTableAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LOCAL_VARIABLE_TABLE_ATTRIBUTE_NODE,
                    JavaLocalVariableTableAttributeNode.class);
    public static final ChildRole<JavaLocalVariableNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaLocalVariableNode.class);
    public static final NodeSchema<JavaLocalVariableTableAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaLocalVariableTableAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaLocalVariableNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaLocalVariableNode> entries() {
        return children(ENTRIES);
    }
}
