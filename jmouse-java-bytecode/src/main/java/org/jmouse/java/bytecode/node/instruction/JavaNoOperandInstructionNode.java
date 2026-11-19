package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaNoOperandInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaNoOperandInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.NO_OPERAND_INSTRUCTION_NODE, JavaNoOperandInstructionNode.class);

    public static final NodeSchema<JavaNoOperandInstructionNode> SCHEMA = NodeSchema.of(KIND);

    public JavaNoOperandInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding) {
        super(SCHEMA, source, span, opcode, offset, encoding);
    }
}
