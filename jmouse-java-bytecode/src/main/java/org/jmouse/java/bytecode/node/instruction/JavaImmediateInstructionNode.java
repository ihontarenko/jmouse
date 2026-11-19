package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaImmediateInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaImmediateInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.IMMEDIATE_INSTRUCTION_NODE, JavaImmediateInstructionNode.class);

    public static final NodeSchema<JavaImmediateInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int value;

    public JavaImmediateInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int value) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.value = value;
    }

    public int value() {
        return value;
    }
}
