package org.jmouse.el.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.BytesSource;
import org.jmouse.el.InputUnit;
import org.jmouse.el.StringSource;
import org.junit.jupiter.api.Test;

import java.util.List;

class SourceCursorTest {
    @Test
    void sourceAndCursorUnitsRemainExplicitAtTheCommonBoundary() {
        StringSource text = new StringSource("text", "alpha");
        TokenCursor tokens =
                new Cursor(
                        text,
                        List.of(new Token("alpha", BasicToken.T_IDENTIFIER, 0, 0, text.length())));
        BytesSource source = BytesSource.copyOf("bytes", new byte[] {0x12, 0x34});
        BytesCursor bytes = BytesCursor.of(source, () -> {});

        assertEquals(InputUnit.UTF16_CODE_UNIT, tokens.source().unit());
        assertEquals(InputUnit.BYTE, bytes.source().unit());
        assertProgress(tokens, InputUnit.TOKEN, 0, 1);
        assertProgress(bytes, InputUnit.BYTE, 0, 2);

        tokens.next();
        bytes.unsigned8();

        assertProgress(tokens, InputUnit.TOKEN, 1, 0);
        assertProgress(bytes, InputUnit.BYTE, 1, 1);
        assertEquals(5, text.length());
    }

    @Test
    void failedPrimitiveReadsAreAtomicAndSavepointsCannotCrossByteRegions() {
        BytesCursor bytes = BytesCursor.of("sample", new byte[] {0x12, 0x34, 0x56});
        BytesCursor.Savepoint beginning = bytes.savepoint();
        assertEquals(0x1234, bytes.unsigned16());
        assertThrows(BytesCursorException.class, bytes::unsigned16);
        assertEquals(2, bytes.position());

        bytes.restore(beginning);
        BytesCursor region = bytes.region(1);
        assertEquals(0x12, region.lookAt(0));
        assertEquals(0, region.position());
        assertThrows(IllegalArgumentException.class, () -> region.restore(beginning));
        assertThrows(BytesCursorException.class, () -> region.lookAt(-1));
        assertThrows(BytesCursorException.class, () -> region.lookAt(1));
    }

    private static void assertProgress(
            SourceCursor<?> cursor, InputUnit unit, int position, int remaining) {
        assertEquals(unit, cursor.positionUnit());
        assertEquals(position, cursor.offset());
        assertEquals(remaining, cursor.remainingUnits());
    }
}
