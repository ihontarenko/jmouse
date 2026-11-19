package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaFormalParameterTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaFormalParameterTargetNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.FORMAL_PARAMETER_TARGET_NODE, JavaFormalParameterTargetNode.class);

    public static final NodeSchema<JavaFormalParameterTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int parameterIndex;

    public JavaFormalParameterTargetNode(
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
