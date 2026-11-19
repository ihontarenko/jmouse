package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.vocabulary.JvmNodeNames;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

import java.util.List;

public final class JavaLookupSwitchInstructionNode extends JavaInstructionNode {
    public static final NodeKind<JavaLookupSwitchInstructionNode> KIND =
            new NodeKind<>(
                    JvmNodeNames.LOOKUP_SWITCH_INSTRUCTION_NODE,
                    JavaLookupSwitchInstructionNode.class);

    public static final NodeSchema<JavaLookupSwitchInstructionNode> SCHEMA = NodeSchema.of(KIND);
    private final int defaultDisplacement;
    private final List<JvmLookupSwitchPair> pairs;
    private final int paddingBytes;

    public JavaLookupSwitchInstructionNode(
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding,
            int defaultDisplacement,
            List<JvmLookupSwitchPair> pairs,
            int paddingBytes) {
        super(SCHEMA, source, span, opcode, offset, encoding);
        this.defaultDisplacement = defaultDisplacement;
        this.pairs = List.copyOf(pairs);
        this.paddingBytes = paddingBytes;
    }

    public int defaultDisplacement() {
        return defaultDisplacement;
    }

    public List<JvmLookupSwitchPair> pairs() {
        return pairs;
    }

    public int paddingBytes() {
        return paddingBytes;
    }
}
