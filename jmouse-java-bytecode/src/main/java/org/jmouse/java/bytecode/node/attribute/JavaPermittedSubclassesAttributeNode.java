package org.jmouse.java.bytecode.node.attribute;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.constant.JavaConstantPoolNode;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;

import java.util.List;

public final class JavaPermittedSubclassesAttributeNode extends JavaAttributeNode {
    public static final NodeKind<JavaPermittedSubclassesAttributeNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.PERMITTED_SUBCLASSES_ATTRIBUTE_NODE,
                    JavaPermittedSubclassesAttributeNode.class);

    public static final NodeSchema<JavaPermittedSubclassesAttributeNode> SCHEMA =
            NodeSchema.of(KIND);
    private final List<Integer> classIndices;

    public JavaPermittedSubclassesAttributeNode(
            BytesSource source,
            BytecodeSpan span,
            JavaConstantPoolNode pool,
            int nameIndex,
            List<Integer> classIndices) {
        super(SCHEMA, source, span, pool, nameIndex);
        this.classIndices = List.copyOf(classIndices);
    }

    public List<Integer> classIndices() {
        return classIndices;
    }
}
