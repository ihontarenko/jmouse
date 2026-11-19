package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaNestedAnnotationValueNode extends JavaAnnotationValueNode {
    public static final NodeKind<JavaNestedAnnotationValueNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.NESTED_ANNOTATION_VALUE_NODE, JavaNestedAnnotationValueNode.class);
    public static final ChildRole<JavaAnnotationNode> ANNOTATION =
            ChildRole.required(JavaBytecodeNames.ANNOTATION, JavaAnnotationNode.class);
    public static final NodeSchema<JavaNestedAnnotationValueNode> SCHEMA =
            NodeSchema.of(KIND, ANNOTATION);

    public JavaNestedAnnotationValueNode(
            BytesSource source,
            BytecodeSpan span,
            JvmAnnotationValueTag tag,
            JavaAnnotationNode annotation) {
        super(SCHEMA, source, span, tag);
        setChild(ANNOTATION, annotation);
    }

    public JavaAnnotationNode annotation() {
        return child(ANNOTATION);
    }
}
