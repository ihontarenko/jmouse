package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

public final class JavaInvokeDynamicInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaInvokeDynamicInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.INVOKE_DYNAMIC_INSTRUCTION_NODE,
                    JavaInvokeDynamicInstructionNode.class);

    public static final NodeSchema<JavaInvokeDynamicInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int constantIndex;

    public JavaInvokeDynamicInstructionNode(
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
