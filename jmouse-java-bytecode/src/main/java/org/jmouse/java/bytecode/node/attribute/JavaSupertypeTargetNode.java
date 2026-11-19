package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmTypeAnnotationTarget;

public final class JavaSupertypeTargetNode extends JavaTypeAnnotationTargetNode {
    public static final NodeKind<JavaSupertypeTargetNode> KIND =
            new NodeKind<>(JvmNodeNames.SUPERTYPE_TARGET_NODE, JavaSupertypeTargetNode.class);

    public static final NodeSchema<JavaSupertypeTargetNode> SCHEMA = NodeSchema.of(KIND);
    private final int supertypeIndex;

    public JavaSupertypeTargetNode(
            BytesSource source,
            BytecodeSpan span,
            JvmTypeAnnotationTarget targetType,
            int supertypeIndex) {
        super(SCHEMA, source, span, targetType);
        this.supertypeIndex = supertypeIndex;
    }

    public int supertypeIndex() {
        return supertypeIndex;
    }
}
