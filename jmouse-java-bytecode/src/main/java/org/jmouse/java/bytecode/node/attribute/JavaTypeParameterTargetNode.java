package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaTypeParameterTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaTypeParameterTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.TYPE_PARAMETER_TARGET_NODE, JavaTypeParameterTargetNode.class);

    public static final NodeSchema<JavaTypeParameterTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int parameterIndex;

    public JavaTypeParameterTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int parameterIndex) {
        super(SCHEMA, source, span, targetType);
        this.parameterIndex = parameterIndex;
    }

    public int parameterIndex() {
        return parameterIndex;
    }
}
