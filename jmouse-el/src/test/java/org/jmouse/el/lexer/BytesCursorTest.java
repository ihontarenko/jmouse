package org.jmouse.el.lexer;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.BytesSource;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

class BytesCursorTest {
    @Test
    void fixedAndByteAlignedWidthsRetainUnsigned64AndFloatingPointBits() {
        byte[] input = {
            (byte) 0xfe,
            (byte) 0xff,
            (byte) 0xff,
            0,
            0,
            (byte) 0x80,
            0x3f,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            (byte) 0x80,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff,
            (byte) 0xff
        };
        BytesCursor cursor =
                BytesCursor.of(
                        BytesSource.copyOf("numbers", input), ByteOrder.LITTLE_ENDIAN, () -> {});
        assertEquals(-2, cursor.signed(24));
        assertEquals(1.0f, cursor.float32());
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(cursor.float64()));
        assertEquals(new BigInteger("18446744073709551615"), cursor.unsigned64());
        long end = cursor.position();
        assertThrows(IllegalArgumentException.class, () -> cursor.unsigned(7));
        assertEquals(end, cursor.position());
    }

    @Test
    void malformedTextIsReportedWithoutConsumingTheField() {
        BytesCursor cursor = BytesCursor.of("text", new byte[] {(byte) 0xc3, 0x28});
        assertThrows(BytesCursorException.class, () -> cursor.text(2, StandardCharsets.UTF_8));
        assertEquals(0, cursor.position());
    }

    @Test
    void positionsBeyondTheArrayLimitDoNotOverflowOrRequireALargeAllocation() {
        long extent = (long) Integer.MAX_VALUE + 64;
        BytesSource source =
                new BytesSource() {
                    @Override
                    public String getName() {
                        return "virtual";
                    }

                    @Override
                    public long contentLength() {
                        return extent;
                    }

                    @Override
                    public int unsigned8(long offset) {
                        return (int) (offset & 0xff);
                    }
                };
        BytesCursor cursor = BytesCursor.of(source, () -> {});
        long position = (long) Integer.MAX_VALUE + 1;
        cursor.seek(position);
        assertEquals(position, cursor.position());
        assertEquals(0, cursor.unsigned8());
        assertEquals(position + 1, cursor.position());
        cursor.align(8, 0);
        assertEquals(position + 8, cursor.position());
        assertThrows(BytesCursorException.class, () -> cursor.seek(extent + 1));
        assertEquals(position + 8, cursor.position());
    }

    @Test
    void byteOrderIsExplicitAndInheritedByBoundedRegions() {
        BytesSource source = BytesSource.copyOf("sample", new byte[] {0x12, 0x34, 0x56, 0x78});
        BytesCursor big = BytesCursor.of(source, ByteOrder.BIG_ENDIAN, () -> {});
        BytesCursor little = BytesCursor.of(source, ByteOrder.LITTLE_ENDIAN, () -> {});

        assertEquals(0x12345678L, big.unsigned32());
        BytesCursor region = little.region(4);
        assertEquals(ByteOrder.LITTLE_ENDIAN, region.order());
        assertEquals(0x78563412L, region.unsigned32());
    }

    @Test
    void nestedRegionsCannotReadOutsideTheirExtentOrMutateTheirSource() {
        byte[] bytes = {0x12, 0x34, (byte) 0xff, (byte) 0xfe, 0x55};
        BytesCursor input = BytesCursor.of("sample", bytes);
        bytes[0] = 0;
        assertEquals(0x1234, input.unsigned16());
        BytesCursor region = input.region(2);
        assertEquals(-2, region.signed16());
        assertThrows(BytesCursorException.class, region::unsigned8);
        assertEquals(4, input.position());
        assertEquals(0x55, input.unsigned8());
        assertThrows(BytesCursorException.class, () -> input.region(Integer.MAX_VALUE));
    }
}
