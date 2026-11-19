package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.instruction.JavaIncrementInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JavaLocalInstructionNode;
import org.jmouse.java.bytecode.node.instruction.JvmInstructionEncoding;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

class JvmOpcodeCorpusTest {
    /**
     * These fixtures test format decoding; their operand stacks are deliberately not JVM-verified.
     */
    @Test
    void everyStandardOpcodeHasLosslessSyntaxDecoding() {
        JavaBytecodeParser parser = new JavaBytecodeParser();
        for (int opcode = 0; opcode <= 201; opcode++) {
            byte[] bytes = JvmOpcodeFixtures.instruction(opcode);
            JavaCodeAttributeNode code = code(parser.parse("opcode-" + opcode + ".class", bytes));
            JavaInstructionNode first = code.instructions().getFirst();
            assertEquals(
                    opcode == 196 ? Opcodes.ILOAD : opcode,
                    first.opcode().code(),
                    "opcode " + opcode);
            if (opcode == 196) {
                assertEquals(JvmInstructionEncoding.WIDE, first.encoding());
            }
            assertArrayEquals(bytes, code.source().slice(0, bytes.length));
        }
    }

    @Test
    void everyLegalWideLocalFormRetainsItsSlotAndSignedIncrement() {
        int[] operations = {
            Opcodes.ILOAD,
            Opcodes.LLOAD,
            Opcodes.FLOAD,
            Opcodes.DLOAD,
            Opcodes.ALOAD,
            Opcodes.ISTORE,
            Opcodes.LSTORE,
            Opcodes.FSTORE,
            Opcodes.DSTORE,
            Opcodes.ASTORE,
            Opcodes.RET,
            Opcodes.IINC
        };
        for (int opcode : operations) {
            JavaInstructionNode instruction =
                    code(new JavaBytecodeParser()
                                    .parse("wide.class", JvmOpcodeFixtures.wide(opcode)))
                            .instructions()
                            .getFirst();
            assertEquals(opcode, instruction.opcode().code());
            assertEquals(JvmInstructionEncoding.WIDE, instruction.encoding());
            if (instruction instanceof JavaLocalInstructionNode local) {
                assertEquals(300, local.slot());
            } else {
                JavaIncrementInstructionNode increment =
                        assertInstanceOf(JavaIncrementInstructionNode.class, instruction);
                assertEquals(300, increment.slot());
                assertEquals(-300, increment.increment());
            }
        }
    }

    @Test
    void reservedAndUnassignedOpcodesNeverBecomeOrdinaryInstructions() {
        byte[] original = JvmOpcodeFixtures.instruction(Opcodes.NOP);
        JavaCodeAttributeNode code = code(new JavaBytecodeParser().parse("valid.class", original));
        int offset = Math.toIntExact(code.code().start());
        for (int opcode = 202; opcode <= 255; opcode++) {
            byte[] malformed = original.clone();
            malformed[offset] = (byte) opcode;
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new JavaBytecodeParser().parse("reserved.class", malformed));
        }
    }

    private static JavaCodeAttributeNode code(JavaBytecodeResult result) {
        return result.requireComplete().methods().getFirst().attributes().stream()
                .filter(JavaCodeAttributeNode.class::isInstance)
                .map(JavaCodeAttributeNode.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
