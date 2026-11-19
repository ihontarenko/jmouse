package org.jmouse.el.lexer;

import org.jmouse.el.BytesSource;
import org.jmouse.el.InputUnit;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/** Byte grammar with explicit byte order, bounded regions and cursor-owned save-points. */
public interface BytesCursor extends SourceCursor<BytesSource> {

    static BytesCursor of(String name, byte[] bytes) {
        return of(BytesSource.copyOf(name, bytes), () -> {});
    }

    static BytesCursor of(BytesSource source, Runnable work) {
        return of(source, ByteOrder.BIG_ENDIAN, work);
    }

    static BytesCursor of(BytesSource source, ByteOrder order, Runnable work) {
        return new DefaultBytesCursor(source, order, work);
    }

    ByteOrder order();

    @Override
    default InputUnit positionUnit() {
        return InputUnit.BYTE;
    }

    @Override
    default boolean hasNext() {
        return remaining() > 0;
    }

    @Override
    default long offset() {
        return position();
    }

    @Override
    default long remainingUnits() {
        return remaining();
    }

    long position();

    long remaining();

    void seek(long position);

    void skip(long length);

    void align(int alignment, long origin);

    default void align(int alignment) {
        align(alignment, start());
    }

    BytesCursor fork();

    long start();

    long end();

    int unsigned8();

    int signed8();

    int unsigned16();

    int signed16();

    long unsigned32();

    int signed32();

    long signed64();

    default BigInteger unsigned64() {
        return unsigned(Long.SIZE);
    }

    /** Reads a byte-aligned width from 8 through 64 bits without losing unsigned values. */
    default BigInteger unsigned(int bitWidth) {
        byte[] value = bytes(byteWidth(bitWidth));
        if (order() == ByteOrder.LITTLE_ENDIAN) {
            for (int left = 0, right = value.length - 1; left < right; left++, right--) {
                byte previous = value[left];
                value[left] = value[right];
                value[right] = previous;
            }
        }
        return new BigInteger(1, value);
    }

    /** Reads a byte-aligned two's-complement value, including 24/40/48/56-bit fields. */
    default long signed(int bitWidth) {
        long value = unsigned(bitWidth).longValue();
        int shift = Long.SIZE - bitWidth;
        return value << shift >> shift;
    }

    default float float32() {
        return Float.intBitsToFloat(signed32());
    }

    default double float64() {
        return Double.longBitsToDouble(signed64());
    }

    default String text(int length, Charset charset) {
        return text(length, charset, CodingErrorAction.REPORT);
    }

    /** Invalid text is explicit by default; callers may deliberately select another policy. */
    default String text(int length, Charset charset, CodingErrorAction errors) {
        CharsetDecoder decoder =
                Objects.requireNonNull(charset, "charset")
                        .newDecoder()
                        .onMalformedInput(Objects.requireNonNull(errors, "errors"))
                        .onUnmappableCharacter(errors);
        Savepoint beginning = savepoint();
        byte[] value = bytes(length);
        try {
            return decoder.decode(ByteBuffer.wrap(value)).toString();
        } catch (CharacterCodingException exception) {
            restore(beginning);
            BytesCursorException failure = failure("Invalid " + charset.name() + " text field");
            failure.initCause(exception);
            throw failure;
        }
    }

    private static int byteWidth(int bitWidth) {
        if (bitWidth <= 0 || bitWidth > Long.SIZE || bitWidth % Byte.SIZE != 0) {
            throw new IllegalArgumentException(
                    "A byte-aligned integer width must be from 8 through 64 bits");
        }
        return bitWidth / Byte.SIZE;
    }

    int lookAt(long offset);

    byte[] bytes(int length);

    BytesCursor region(long length);

    Savepoint savepoint();

    void restore(Savepoint savepoint);

    void requireEnd();

    BytesCursorException failure(String message);

    interface Savepoint {
        long getPosition();
    }
}
