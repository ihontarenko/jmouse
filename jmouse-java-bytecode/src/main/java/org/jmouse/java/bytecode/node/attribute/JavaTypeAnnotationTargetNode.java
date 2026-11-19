package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

import java.util.Objects;

public abstract class JavaTypeAnnotationTargetNode extends JavaBytecodeNode {
    private final JvmTypeAnnotationTarget targetType;

    protected JavaTypeAnnotationTargetNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType) {
        super(schema, source, span);
        this.targetType = Objects.requireNonNull(targetType, "targetType");
    }

    public final JvmTypeAnnotationTarget targetType() {
        return targetType;
    }
}
