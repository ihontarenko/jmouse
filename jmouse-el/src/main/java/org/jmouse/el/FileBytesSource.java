package org.jmouse.el;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Positional file access with a bounded immutable page cache. The extent is captured at open;
 * callers keep the file stable while parsing and close the source after its consumers finish.
 */
final class FileBytesSource implements BytesSource {
    private enum State {
        OPEN,
        CLOSED
    }

    private record Page(long start, byte[] bytes) {
        boolean contains(long offset) {
            return start <= offset && offset - start < bytes.length;
        }
    }

    private final String name;
    private final FileChannel channel;
    private final long length;
    private final int bufferSize;
    private volatile State state = State.OPEN;
    private volatile Page page;

    private FileBytesSource(Path path, FileChannel channel, long length, int bufferSize) {
        this.name = path.toAbsolutePath().normalize().toString();
        this.channel = channel;
        this.length = length;
        this.bufferSize = bufferSize;
    }

    static BytesSource open(Path path, BytesSourceOptions options) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        try {
            return new FileBytesSource(path, channel, channel.size(), options.bufferSize());
        } catch (IOException | RuntimeException exception) {
            try {
                channel.close();
            } catch (IOException closing) {
                exception.addSuppressed(closing);
            }
            throw exception;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public int unsigned8(long offset) {
        requireOpen();
        Objects.checkIndex(offset, length);
        Page current = page;
        if (current == null || !current.contains(offset)) {
            long start = offset - offset % bufferSize;
            byte[] bytes = new byte[(int) Math.min(bufferSize, length - start)];
            read(start, bytes, 0, bytes.length);
            current = new Page(start, bytes);
            page = current;
        }
        return Byte.toUnsignedInt(current.bytes()[(int) (offset - current.start())]);
    }

    @Override
    public void read(long offset, byte[] target, int targetOffset, int count) {
        requireOpen();
        Objects.checkFromIndexSize(offset, count, length);
        Objects.checkFromIndexSize(targetOffset, count, target.length);
        ByteBuffer buffer = ByteBuffer.wrap(target, targetOffset, count);
        long position = offset;
        try {
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer, position);
                if (read < 0) {
                    throw new EOFException("The byte source changed or was truncated: " + name);
                }
                if (read == 0) {
                    throw new IOException("The byte source made no read progress: " + name);
                }
                position += read;
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Cannot read byte source " + name + " at " + position, exception);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (state == State.CLOSED) {
            return;
        }
        state = State.CLOSED;
        page = null;
        channel.close();
    }

    private void requireOpen() {
        if (state != State.OPEN) {
            throw new IllegalStateException("The byte source is closed: " + name);
        }
    }
}
