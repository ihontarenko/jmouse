package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaExceptionsAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaExceptionsAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.EXCEPTIONS_ATTRIBUTE_NODE, JavaExceptionsAttributeNode.class);

    public static final NodeSchema<JavaExceptionsAttributeNode> SCHEMA = NodeSchema.of(KIND);
    private final List<Integer> exceptionIndices;

    public JavaExceptionsAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<Integer> exceptionIndices) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.exceptionIndices = List.copyOf(exceptionIndices);
    }

    public List<Integer> exceptionIndices() {
        return exceptionIndices;
    }
}
