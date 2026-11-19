package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaArrayAnnotationValueNode extends JavaAnnotationValueNode {
    public static final NodeKind<JavaArrayAnnotationValueNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.ARRAY_ANNOTATION_VALUE_NODE, JavaArrayAnnotationValueNode.class);
    public static final ChildRole<JavaAnnotationValueNode> ENTRIES =
            ChildRole.list(JavaBytecodeNames.ENTRIES, JavaAnnotationValueNode.class);
    public static final NodeSchema<JavaArrayAnnotationValueNode> SCHEMA =
            NodeSchema.of(KIND, ENTRIES);

    public JavaArrayAnnotationValueNode(
            BytesSource source,
            BytecodeSpan span,
            JvmAnnotationValueTag tag,
            List<JavaAnnotationValueNode> entries) {
        super(SCHEMA, source, span, tag);
        entries.forEach(value -> addChild(ENTRIES, value));
    }

    public List<JavaAnnotationValueNode> entries() {
        return children(ENTRIES);
    }
}
