package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaIncrementInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaIncrementInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.INCREMENT_INSTRUCTION_NODE, JavaIncrementInstructionNode.class);

    public static final NodeSchema<JavaIncrementInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int slot;
    private final int increment;

    public JavaIncrementInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int slot,
            int increment) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.slot = slot;
        this.increment = increment;
    }

    public int slot() {
        return slot;
    }

    public int increment() {
        return increment;
    }
}
