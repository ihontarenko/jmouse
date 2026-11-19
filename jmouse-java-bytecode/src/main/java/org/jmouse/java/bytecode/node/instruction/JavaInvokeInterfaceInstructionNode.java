package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaInvokeInterfaceInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaInvokeInterfaceInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.INVOKE_INTERFACE_INSTRUCTION_NODE,
                    JavaInvokeInterfaceInstructionNode.class);

    public static final NodeSchema<JavaInvokeInterfaceInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;
    private final int argumentSlots;

    public JavaInvokeInterfaceInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int constantIndex,
            int argumentSlots) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.constantIndex = constantIndex;
        this.argumentSlots = argumentSlots;
    }

    public int constantIndex() {
        return constantIndex;
    }

    public int argumentSlots() {
        return argumentSlots;
    }
}
