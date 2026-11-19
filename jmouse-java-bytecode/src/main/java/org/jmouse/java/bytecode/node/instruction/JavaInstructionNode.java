package org.jmouse.java.bytecode.node.instruction;

import org.jmouse.el.BytesSource;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.bytecode.BytecodeSpan;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.vocabulary.JvmInstructionKind;
import org.jmouse.java.bytecode.vocabulary.JvmOpcode;

import java.util.Objects;

public abstract class JavaInstructionNode extends JavaBytecodeNode {
    private final JvmInstructionKind opcode;
    private final int offset;
    private final JvmInstructionEncoding encoding;

    protected JavaInstructionNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmOpcode opcode,
            int offset,
            JvmInstructionEncoding encoding) {
        this(schema, source, span, (JvmInstructionKind) opcode, offset, encoding);
    }

    protected JavaInstructionNode(
            NodeSchema<?> schema,
            BytesSource source,
            BytecodeSpan span,
            JvmInstructionKind opcode,
            int offset,
            JvmInstructionEncoding encoding) {
        super(schema, source, span);
        this.opcode = Objects.requireNonNull(opcode, "opcode");
        if (offset < 0 || opcode.code() < 0 || opcode.code() >= 1 << Byte.SIZE) {
            throw new IllegalArgumentException(
                    "An instruction requires a nonnegative program counter and an unsigned-byte"
                            + " opcode");
        }
        this.offset = offset;
        this.encoding = Objects.requireNonNull(encoding, "encoding");
    }

    public final JvmOpcode opcode() {
        if (opcode instanceof JvmOpcode standard) {
            return standard;
        }
        throw new IllegalStateException(
                "Extension instruction "
                        + opcode.name()
                        + " has no standard opcode; use opcodeKind()");
    }

    public final JvmInstructionKind opcodeKind() {
        return opcode;
    }

    public final int offset() {
        return offset;
    }

    public final JvmInstructionEncoding encoding() {
        return encoding;
    }
}
