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

public final class JavaInnerClassesAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaInnerClassesAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.INNER_CLASSES_ATTRIBUTE_NODE, JavaInnerClassesAttributeNode.class);
    public static final ChildRole<JavaInnerClassNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaInnerClassNode.class);
    public static final NodeSchema<JavaInnerClassesAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaInnerClassesAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaInnerClassNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaInnerClassNode> entries() {
        return children(ENTRIES);
    }
}
