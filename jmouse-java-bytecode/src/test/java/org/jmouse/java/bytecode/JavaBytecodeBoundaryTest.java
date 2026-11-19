package org.jmouse.java.bytecode;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.java.model.version.VersionAdmission;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

class JavaBytecodeBoundaryTest {
    @Test
    void everyTruncatedPrefixAndAnUnconsumedTailAreRejected() {
        byte[] bytes = JvmOpcodeFixtures.instruction(Opcodes.NOP);
        JavaBytecodeParser parser = new JavaBytecodeParser();
        for (int length = 0; length < bytes.length; length++) {
            byte[] prefix = Arrays.copyOf(bytes, length);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> parser.parse("truncated.class", prefix),
                    "length " + length);
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("tail.class", Arrays.copyOf(bytes, bytes.length + 1)));
        assertTrue(parser.parse("complete.class", bytes).isComplete());
    }

    @Test
    void budgetsAndVersionAdmissionCannotPublishACompleteResult() {
        byte[] bytes = JvmOpcodeFixtures.instruction(Opcodes.NOP);
        JavaBytecodeLanguage language = JavaBytecodeLanguage.standard();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new JavaBytecodeParser(
                                        language,
                                        new JavaBytecodeLimits(
                                                bytes.length - 1,
                                                JavaBytecodeLimits.standard().parsing()))
                                .parse("oversized.class", bytes));
        assertThrows(
                ParsingLimitException.class,
                () ->
                        new JavaBytecodeParser(
                                        language,
                                        new JavaBytecodeLimits(
                                                bytes.length, new ParsingBudget(192, 1)))
                                .parse("work.class", bytes));
        assertThrows(
                ParsingLimitException.class,
                () ->
                        new JavaBytecodeParser(
                                        language,
                                        new JavaBytecodeLimits(
                                                bytes.length, new ParsingBudget(1, 10000)))
                                .parse("depth.class", bytes));
        bytes[7] = 66;
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaBytecodeParser().parse("future.class", bytes));
        JavaBytecodeResult inspection =
                new JavaBytecodeParser().parse("future.class", bytes, JavaBytecodeMode.INSPECTION);
        assertEquals(VersionAdmission.UNKNOWN_FORMAT, inspection.admission());
        assertFalse(inspection.isComplete());
        assertThrows(IllegalStateException.class, inspection::requireComplete);
    }
}
