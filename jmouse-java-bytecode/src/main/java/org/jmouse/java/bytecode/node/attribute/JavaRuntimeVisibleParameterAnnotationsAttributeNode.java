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

public final class JavaRuntimeVisibleParameterAnnotationsAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaRuntimeVisibleParameterAnnotationsAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS_ATTRIBUTE_NODE,
                    JavaRuntimeVisibleParameterAnnotationsAttributeNode.class);
    public static final ChildRole<JavaParameterAnnotationsNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaParameterAnnotationsNode.class);
    public static final NodeSchema<JavaRuntimeVisibleParameterAnnotationsAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaRuntimeVisibleParameterAnnotationsAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaParameterAnnotationsNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaParameterAnnotationsNode> entries() {
        return children(ENTRIES);
    }
}
