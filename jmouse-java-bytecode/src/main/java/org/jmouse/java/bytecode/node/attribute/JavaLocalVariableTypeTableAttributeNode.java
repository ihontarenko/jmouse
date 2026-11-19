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

public final class JavaLocalVariableTypeTableAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaLocalVariableTypeTableAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LOCAL_VARIABLE_TYPE_TABLE_ATTRIBUTE_NODE,
                    JavaLocalVariableTypeTableAttributeNode.class);
    public static final ChildRole<JavaLocalVariableNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaLocalVariableNode.class);
    public static final NodeSchema<JavaLocalVariableTypeTableAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaLocalVariableTypeTableAttributeNode(
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
