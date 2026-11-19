package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmAnnotationValueTag;

import java.util.Objects;

public abstract class JavaAnnotationValueNode extends JavaBytecodeNode {
    private final JvmAnnotationValueTag tag;

    protected JavaAnnotationValueNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmAnnotationValueTag tag) {
        super(schema, source, span);
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    public final JvmAnnotationValueTag tag() {
        return tag;
    }
}
