package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaThrowsTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaThrowsTargetNode> KIND =
            new NodeKind<>(JvmNodeNames.THROWS_TARGET_NODE, JavaThrowsTargetNode.class);

    public static final NodeSchema<JavaThrowsTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int throwsIndex;

    public JavaThrowsTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int throwsIndex) {
        super(SCHEMA, source, span, targetType);
        this.throwsIndex = throwsIndex;
    }

    public int throwsIndex() {
        return throwsIndex;
    }
}
