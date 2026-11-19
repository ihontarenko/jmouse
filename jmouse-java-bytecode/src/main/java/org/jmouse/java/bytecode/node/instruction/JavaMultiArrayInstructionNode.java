package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaMultiArrayInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaMultiArrayInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.MULTI_ARRAY_INSTRUCTION_NODE, JavaMultiArrayInstructionNode.class);

    public static final NodeSchema<JavaMultiArrayInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;
    private final int dimensions;

    public JavaMultiArrayInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int constantIndex,
            int dimensions) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.constantIndex = constantIndex;
        this.dimensions = dimensions;
    }

    public int constantIndex() {
        return constantIndex;
    }

    public int dimensions() {
        return dimensions;
    }
}
