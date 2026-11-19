package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.JavaBytecodeNames;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaAnnotationElementNode extends JavaBytecodeNode {
    public static final NodeKind<JavaAnnotationElementNode> KIND =
            new NodeKind<>(JvmNodeNames.ANNOTATION_ELEMENT_NODE, JavaAnnotationElementNode.class);
    public static final ChildRole<JavaAnnotationValueNode> VALUE =
            ChildRole.required(JavaBytecodeNames.VALUE, JavaAnnotationValueNode.class);
    public static final NodeSchema<JavaAnnotationElementNode> SCHEMA = NodeSchema.of(KIND, VALUE);
    private final int nameIndex;

    public JavaAnnotationElementNode(
            BytesSource source, BytecodeSpan span, int nameIndex, JavaAnnotationValueNode value) {
        super(SCHEMA, source, span);
        this.nameIndex = nameIndex;
        setChild(VALUE, value);
    }

    public JavaAnnotationValueNode value() {
        return child(VALUE);
    }

    public int nameIndex() {
        return nameIndex;
    }
}
