package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaConstantInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaConstantInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.CONSTANT_INSTRUCTION_NODE, JavaConstantInstructionNode.class);

    public static final NodeSchema<JavaConstantInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;

    public JavaConstantInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int constantIndex) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.constantIndex = constantIndex;
    }

    public int constantIndex() {
        return constantIndex;
    }
}
