package org.jmouse.altium;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A walk along one of Altium's binary streams: little-endian numbers, records behind their four-byte length, names
 * behind a byte's, and {@code |KEY=VALUE} properties behind four bytes' — each read said with the stream it is in, so a
 * file broken past reading says where.
 */
final class AltiumBinaryCursor {

    /** What Altium writes a name in. */
    static final Charset ANSI = Charset.forName("windows-1252");

    private final String where;
    private final ByteBuffer buffer;

    AltiumBinaryCursor(String where, byte[] bytes) {
        this.where = where;
        this.buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    boolean hasMore() {
        return buffer.hasRemaining();
    }

    int unsignedByte() {
        return guarded(() -> buffer.get() & 0xFF);
    }

    int integer() {
        return guarded(buffer::getInt);
    }

    /** A record: four bytes of its length, then that many. */
    AltiumBinaryCursor record(String what) {
        int length = integer();
        if (length < 0 || length > buffer.remaining()) {
            throw new AltiumReadException(where + ": " + what + " says it is " + length + " bytes and " + buffer.remaining()
                    + " are left.");
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new AltiumBinaryCursor(where + " › " + what, bytes);
    }

    /** A record's bytes, read as properties: four bytes of their length, then {@code |KEY=VALUE…}. */
    Map<String, String> properties(String what) {
        return AltiumRecords.properties(record(what).remaining());
    }

    /** A name: a byte of its length, then that many characters. */
    String name() {
        int length = unsignedByte();
        byte[] bytes = new byte[Math.min(length, buffer.remaining())];
        buffer.get(bytes);
        return new String(bytes, ANSI);
    }

    /** A name kept in a record of its own: four bytes of the record's length, then the name. */
    String namedRecord(String what) {
        AltiumBinaryCursor record = record(what);
        return record.hasMore() ? record.name() : "";
    }

    /** What is left of this stream, from here. */
    byte[] remaining() {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    /** A text written in UTF-16, behind four bytes of its length in bytes. */
    String wideText() {
        int length = integer();
        byte[] bytes = new byte[Math.min(Math.max(length, 0), buffer.remaining())];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }

    /** A view of one record's fields, read where they stand rather than in turn. */
    Fields fields() {
        return new Fields(where, buffer.slice().order(ByteOrder.LITTLE_ENDIAN));
    }

    private <T> T guarded(java.util.function.Supplier<T> read) {
        try {
            return read.get();
        } catch (BufferUnderflowException ended) {
            throw new AltiumReadException(where + " ends before what it says it holds.", ended);
        }
    }

    /** One record's fields, each at the offset Altium writes it at. */
    static final class Fields {

        private final String where;
        private final ByteBuffer buffer;

        private Fields(String where, ByteBuffer buffer) {
            this.where = where;
            this.buffer = buffer;
        }

        int length() {
            return buffer.limit();
        }

        int unsignedByte(int offset) {
            return buffer.get(checked(offset, 1)) & 0xFF;
        }

        int unsignedShort(int offset) {
            return buffer.getShort(checked(offset, 2)) & 0xFFFF;
        }

        int integer(int offset) {
            return buffer.getInt(checked(offset, 4));
        }

        double real(int offset) {
            return buffer.getDouble(checked(offset, 8));
        }

        /** Properties written at an offset: four bytes of their length, then {@code |KEY=VALUE…}. */
        Map<String, String> properties(int offset) {
            int length = integer(offset);
            byte[] bytes = new byte[length];
            buffer.get(checked(offset + 4, length), bytes);
            return AltiumRecords.properties(bytes);
        }

        private int checked(int offset, int size) {
            if (offset < 0 || offset + size > buffer.limit()) {
                throw new AltiumReadException(where + ": a field at " + offset + " runs past the record's " + buffer.limit()
                        + " bytes.");
            }
            return offset;
        }
    }
}
