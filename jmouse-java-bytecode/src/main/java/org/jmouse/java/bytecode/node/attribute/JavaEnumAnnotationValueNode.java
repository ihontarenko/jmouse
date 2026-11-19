package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaEnumAnnotationValueNode extends JavaAnnotationValueNode {
    public static final NodeKind<JavaEnumAnnotationValueNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.ENUM_ANNOTATION_VALUE_NODE, JavaEnumAnnotationValueNode.class);

    public static final NodeSchema<JavaEnumAnnotationValueNode> SCHEMA = NodeSchema.of(KIND);
    private final int typeNameIndex;
    private final int constantNameIndex;

    public JavaEnumAnnotationValueNode(
            BytesSource source,
            BytecodeSpan span,
            JvmAnnotationValueTag tag,
            int typeNameIndex,
            int constantNameIndex) {
        super(SCHEMA, source, span, tag);
        this.typeNameIndex = typeNameIndex;
        this.constantNameIndex = constantNameIndex;
    }

    public int typeNameIndex() {
        return typeNameIndex;
    }

    public int constantNameIndex() {
        return constantNameIndex;
    }
}
