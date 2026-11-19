package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaBootstrapMethodNode extends JavaBytecodeNode {
    public static final NodeKind<JavaBootstrapMethodNode> KIND =
            new NodeKind<>(JvmNodeNames.BOOTSTRAP_METHOD_NODE, JavaBootstrapMethodNode.class);

    public static final NodeSchema<JavaBootstrapMethodNode> SCHEMA = NodeSchema.of(KIND);
    private final int methodHandleIndex;
    private final List<Integer> argumentIndices;

    public JavaBootstrapMethodNode(
            BytesSource source,
            BytecodeSpan span,
            int methodHandleIndex,
            List<Integer> argumentIndices) {
        super(SCHEMA, source, span);
        this.methodHandleIndex = methodHandleIndex;
        this.argumentIndices = List.copyOf(argumentIndices);
    }

    public int methodHandleIndex() {
        return methodHandleIndex;
    }

    public List<Integer> argumentIndices() {
        return argumentIndices;
    }
}
