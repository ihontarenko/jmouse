package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaClassAnnotationValueNode extends JavaAnnotationValueNode {
    public static final NodeKind<JavaClassAnnotationValueNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.CLASS_ANNOTATION_VALUE_NODE, JavaClassAnnotationValueNode.class);

    public static final NodeSchema<JavaClassAnnotationValueNode> SCHEMA = NodeSchema.of(KIND);
    private final int classInfoIndex;

    public JavaClassAnnotationValueNode(
            BytesSource source, BytecodeSpan span, JvmAnnotationValueTag tag, int classInfoIndex) {
        super(SCHEMA, source, span, tag);
        this.classInfoIndex = classInfoIndex;
    }

    public int classInfoIndex() {
        return classInfoIndex;
    }
}
