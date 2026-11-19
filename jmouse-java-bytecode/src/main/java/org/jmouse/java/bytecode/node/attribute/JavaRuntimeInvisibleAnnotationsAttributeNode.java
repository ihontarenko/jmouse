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

public final class JavaRuntimeInvisibleAnnotationsAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaRuntimeInvisibleAnnotationsAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.RUNTIME_INVISIBLE_ANNOTATIONS_ATTRIBUTE_NODE,
                    JavaRuntimeInvisibleAnnotationsAttributeNode.class);
    public static final ChildRole<JavaAnnotationNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaAnnotationNode.class);
    public static final NodeSchema<JavaRuntimeInvisibleAnnotationsAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaRuntimeInvisibleAnnotationsAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaAnnotationNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaAnnotationNode> entries() {
        return children(ENTRIES);
    }
}
