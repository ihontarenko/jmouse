package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaBranchInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaBranchInstructionNode> KIND =
            new NodeKind<>(JvmNodeNames.BRANCH_INSTRUCTION_NODE, JavaBranchInstructionNode.class);

    public static final NodeSchema<JavaBranchInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int displacement;

    public JavaBranchInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int displacement) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.displacement = displacement;
    }

    public int displacement() {
        return displacement;
    }
}
