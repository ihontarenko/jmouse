package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaLocalInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaLocalInstructionNode> KIND =
            new NodeKind<>(JvmNodeNames.LOCAL_INSTRUCTION_NODE, JavaLocalInstructionNode.class);

    public static final NodeSchema<JavaLocalInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int slot;

    public JavaLocalInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int slot) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.slot = slot;
    }

    public int slot() {
        return slot;
    }
}
