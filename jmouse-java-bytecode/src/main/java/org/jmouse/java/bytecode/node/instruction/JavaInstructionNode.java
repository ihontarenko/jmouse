package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

import java.util.Objects;

public abstract class JavaInstructionNode extends JavaBytecodeNode {
    private final JvmOpcode opcode;
    private final int offset;
    private final JvmInstructionEncoding encoding;

    protected JavaInstructionNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding) {
        super(schema, source, span);
        this.opcode = Objects.requireNonNull(opcode, "opcode");
        this.offset = offset;
        this.encoding = Objects.requireNonNull(encoding, "encoding");
    }

    public final JvmOpcode opcode() {
        return opcode;
    }

    public final int offset() {
        return offset;
    }

    public final JvmInstructionEncoding encoding() {
        return encoding;
    }
}
