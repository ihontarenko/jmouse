package org.jmouse.java.bytecode.validation;

import org.jmouse.java.bytecode.JavaCodeCoverage;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;

import java.util.HashMap;
import java.util.Map;

/** Relative JVM program counters remain separate from absolute source byte offsets. */
public final class JvmCodeLayout {
    private final JavaCodeAttributeNode code;
    private final Map<Integer, JavaInstructionNode> instructions;
    private final JvmValidationContext context;

    public JvmCodeLayout(JavaCodeAttributeNode code, JvmValidationContext context) {
        this.code = code;
        this.context = context;
        Map<Integer, JavaInstructionNode> instructions = new HashMap<>();
        long end = code.code().start();
        for (JavaInstructionNode instruction : code.instructions()) {
            context.session().step();
            context.require(
                    instruction.span().start() == end
                            && instruction.offset() == end - code.code().start()
                            && instruction.span().end() > end,
                    instruction,
                    "Instruction coverage must be contiguous");
            context.require(
                    instructions.putIfAbsent(instruction.offset(), instruction) == null,
                    instruction,
                    "Duplicate instruction program counter");
            end = instruction.span().end();
        }
        if (code.coverage() == JavaCodeCoverage.COMPLETE) {
            context.require(
                    end == code.code().end(),
                    code,
                    "Decoded instructions must cover the entire Code array");
        }
        this.instructions = Map.copyOf(instructions);
    }

    public JavaCodeAttributeNode code() {
        return code;
    }

    public JavaInstructionNode instruction(int offset) {
        return instructions.get(offset);
    }

    public void boundary(long offset, JavaBytecodeNode origin, EndPolicy endPolicy) {
        boolean atEnd = endPolicy == EndPolicy.ALLOW_END && offset == code.code().length();
        boolean inRange = offset >= 0 && offset < code.code().length();
        boolean known =
                code.coverage() == JavaCodeCoverage.OMITTED
                        || offset <= Integer.MAX_VALUE && instructions.containsKey((int) offset);
        context.require(
                atEnd || inRange && known, origin, "Invalid instruction boundary: " + offset);
    }

    public enum EndPolicy {
        INSTRUCTION_ONLY,
        ALLOW_END
    }
}
