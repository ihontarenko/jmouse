package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaAnnotationDefaultAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaAnnotationDefaultAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.ANNOTATION_DEFAULT_ATTRIBUTE_NODE,
                    JavaAnnotationDefaultAttributeNode.class);
    public static final ChildRole<JavaAnnotationValueNode> VALUE =
            ChildRole.required(JavaBytecodeNames.VALUE, JavaAnnotationValueNode.class);
    public static final NodeSchema<JavaAnnotationDefaultAttributeNode> SCHEMA =
            NodeSchema.of(KIND, VALUE);

    public JavaAnnotationDefaultAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            JavaAnnotationValueNode value) {
        super(SCHEMA, source, span, pool, nameIndex);
        setChild(VALUE, value);
    }

    public JavaAnnotationValueNode value() {
        return child(VALUE);
    }
}
