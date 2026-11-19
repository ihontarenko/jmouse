package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaRecordComponentNode extends JavaBytecodeNode {
    public static final NodeKind<JavaRecordComponentNode> KIND =
            new NodeKind<>(JvmNodeNames.RECORD_COMPONENT_NODE, JavaRecordComponentNode.class);
    public static final ChildRole<JavaAttributeNode> ATTRIBUTES =
            ChildRole.list(JavaBytecodeNames.ATTRIBUTES, JavaAttributeNode.class);
    public static final NodeSchema<JavaRecordComponentNode> SCHEMA =
            NodeSchema.of(KIND, ATTRIBUTES);
    private final int nameIndex;
    private final int descriptorIndex;

    public JavaRecordComponentNode(
            BytesSource source,
            BytecodeSpan span,
            int nameIndex,
            int descriptorIndex,
            List<JavaAttributeNode> attributes) {
        super(SCHEMA, source, span);
        this.nameIndex = nameIndex;
        this.descriptorIndex = descriptorIndex;
        attributes.forEach(value -> addChild(ATTRIBUTES, value));
    }

    public int nameIndex() {
        return nameIndex;
    }

    public int descriptorIndex() {
        return descriptorIndex;
    }

    public List<JavaAttributeNode> attributes() {
        return children(ATTRIBUTES);
    }
}
