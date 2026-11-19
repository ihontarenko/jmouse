package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

import java.util.List;

public final class JavaTableSwitchInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaTableSwitchInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.TABLE_SWITCH_INSTRUCTION_NODE,
                    JavaTableSwitchInstructionNode.class);

    public static final NodeSchema<JavaTableSwitchInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int defaultDisplacement;
    private final int low;
    private final int high;
    private final List<Integer> displacements;
    private final int paddingBytes;

    public JavaTableSwitchInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int defaultDisplacement,
            int low,
            int high,
            List<Integer> displacements,
            int paddingBytes) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.defaultDisplacement = defaultDisplacement;
        this.low = low;
        this.high = high;
        this.displacements = List.copyOf(displacements);
        this.paddingBytes = paddingBytes;
    }

    public int defaultDisplacement() {
        return defaultDisplacement;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    public List<Integer> displacements() {
        return displacements;
    }

    public int paddingBytes() {
        return paddingBytes;
    }
}
