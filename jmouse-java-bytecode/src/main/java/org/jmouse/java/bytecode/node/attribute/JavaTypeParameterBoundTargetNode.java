package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaTypeParameterBoundTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaTypeParameterBoundTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.TYPE_PARAMETER_BOUND_TARGET_NODE,
                    JavaTypeParameterBoundTargetNode.class);

    public static final NodeSchema<JavaTypeParameterBoundTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int parameterIndex;
    private final int boundIndex;

    public JavaTypeParameterBoundTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int parameterIndex,
            int boundIndex) {
        super(SCHEMA, source, span, targetType);
        this.parameterIndex = parameterIndex;
        this.boundIndex = boundIndex;
    }

    public int parameterIndex() {
        return parameterIndex;
    }

    public int boundIndex() {
        return boundIndex;
    }
}
