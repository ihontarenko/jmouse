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

public final class JavaRuntimeInvisibleTypeAnnotationsAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaRuntimeInvisibleTypeAnnotationsAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS_ATTRIBUTE_NODE,
                    JavaRuntimeInvisibleTypeAnnotationsAttributeNode.class);
    public static final ChildRole<JavaTypeAnnotationNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaTypeAnnotationNode.class);
    public static final NodeSchema<JavaRuntimeInvisibleTypeAnnotationsAttributeNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaRuntimeInvisibleTypeAnnotationsAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<JavaTypeAnnotationNode> entries) {
        super(SCHEMA, source, span, pool, nameIndex);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaTypeAnnotationNode> entries() {
        return children(ENTRIES);
    }
}
