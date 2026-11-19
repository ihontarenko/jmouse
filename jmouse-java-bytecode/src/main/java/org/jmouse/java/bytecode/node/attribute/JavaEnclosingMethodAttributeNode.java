package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

public final class JavaEnclosingMethodAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaEnclosingMethodAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.ENCLOSING_METHOD_ATTRIBUTE_NODE,
                    JavaEnclosingMethodAttributeNode.class);

    public static final NodeSchema<JavaEnclosingMethodAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final int classIndex;
    private final int methodIndex;

    public JavaEnclosingMethodAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            int classIndex,
            int methodIndex) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.classIndex = classIndex;
        this.methodIndex = methodIndex;
    }

    public int classIndex() {
        return classIndex;
    }

    public int methodIndex() {
        return methodIndex;
    }
}
