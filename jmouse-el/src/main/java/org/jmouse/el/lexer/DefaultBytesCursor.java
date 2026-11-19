package org.jmouse.el.lexer;

import org.jmouse.el.BytesSource;

import java.nio.ByteOrder;
import java.util.Objects;

/** Absolute byte positions are retained in child regions; failed reads leave the cursor intact. */
public final class DefaultBytesCursor implements BytesCursor {
    private record Checkpoint(DefaultBytesCursor owner, long position) implements Savepoint {
        @Override
        public long getPosition() {
            return position;
        }
    }

    private final BytesSource source;
    private final ByteOrder order;
    private final long start;
    private final long end;
    private final Runnable work;
    private long position;

    public DefaultBytesCursor(BytesSource source, Runnable work) {
        this(source, ByteOrder.BIG_ENDIAN, work);
    }

    public DefaultBytesCursor(BytesSource source, ByteOrder order, Runnable work) {
        this(source, 0, source.contentLength(), order, work);
    }

    private DefaultBytesCursor(
            BytesSource source, long start, long end, ByteOrder order, Runnable work) {
        this.source = Objects.requireNonNull(source, "source");
        this.order = Objects.requireNonNull(order, "order");
        this.start = start;
        this.end = end;
        this.position = start;
        this.work = Objects.requireNonNull(work, "work");
    }

    @Override
    public BytesSource source() {
        return source;
    }

    @Override
    public ByteOrder order() {
        return order;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public long remaining() {
        return end - position;
    }

    @Override
    public int unsigned8() {
        return (int) readBits(Byte.BYTES);
    }

    @Override
    public int signed8() {
        return (byte) unsigned8();
    }

    @Override
    public int unsigned16() {
        return (int) readBits(Short.BYTES);
    }

    @Override
    public int signed16() {
        return (short) unsigned16();
    }

    @Override
    public long unsigned32() {
        return readBits(Integer.BYTES);
    }

    @Override
    public int signed32() {
        return (int) unsigned32();
    }

    @Override
    public long signed64() {
        return readBits(Long.BYTES);
    }

    @Override
    public int lookAt(long offset) {
        work.run();
        if (offset < start - position || offset >= end - position) {
            throw failure("Lookahead exceeds the byte region");
        }
        return source.unsigned8(position + offset);
    }

    @Override
    public byte[] bytes(int length) {
        require(length);
        byte[] value = source.slice(position, length);
        position += length;
        return value;
    }

    @Override
    public BytesCursor region(long length) {
        if (length < 0 || length > remaining()) {
            throw failure("Region length exceeds the containing input: " + length);
        }
        require(length);
        BytesCursor region =
                new DefaultBytesCursor(source, position, position + length, order, work);
        position += length;
        return region;
    }

    @Override
    public void seek(long target) {
        work.run();
        if (target < start || target > end) {
            throw failure("Seek exceeds the byte region");
        }
        position = target;
    }

    @Override
    public void skip(long length) {
        require(length);
        position += length;
    }

    @Override
    public void align(int alignment, long origin) {
        if (alignment <= 0 || origin < 0 || origin > position) {
            throw new IllegalArgumentException("Invalid byte alignment or origin");
        }
        long remainder = (position - origin) % alignment;
        skip(remainder == 0 ? 0 : alignment - remainder);
    }

    @Override
    public BytesCursor fork() {
        DefaultBytesCursor fork = new DefaultBytesCursor(source, start, end, order, work);
        fork.position = position;
        return fork;
    }

    @Override
    public Savepoint savepoint() {
        return new Checkpoint(this, position);
    }

    @Override
    public void restore(Savepoint savepoint) {
        if (!(savepoint instanceof Checkpoint(DefaultBytesCursor owner, long savedPosition))
                || owner != this) {
            throw new IllegalArgumentException("The byte savepoint belongs to a different cursor");
        }
        position = savedPosition;
    }

    @Override
    public void requireEnd() {
        if (remaining() != 0) {
            throw failure("Unconsumed bytes in binary region: " + remaining());
        }
    }

    @Override
    public BytesCursorException failure(String message) {
        return new BytesCursorException(source.getName(), position, message);
    }

    private long readBits(int length) {
        require(length);
        long value = 0;
        for (int index = 0; index < length; index++) {
            int next = source.unsigned8(position + index);
            if (order == ByteOrder.BIG_ENDIAN) {
                value = value << Byte.SIZE | next;
            } else {
                value |= (long) next << (index * Byte.SIZE);
            }
        }
        position += length;
        return value;
    }

    private void require(long length) {
        work.run();
        if (length < 0 || length > remaining()) {
            throw failure(
                    "Truncated binary region; requested "
                            + length
                            + " bytes, remaining "
                            + remaining());
        }
    }
}
