package org.jmouse.web.servlet;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 🧲 {@link ServletOutputStream} that captures all written bytes
 * into a backing {@link ByteArrayOutputStream}.
 *
 * <p>Used by {@link BufferingOnlyResponseWrapper} to intercept
 * servlet output before it is flushed to the real response.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>📥 Collects all {@code write(...)} calls into an in-memory buffer.</li>
 *   <li>✅ Always reports {@link #isReady()} as {@code true} (non-blocking APIs compatible).</li>
 *   <li>🛑 {@link #setWriteListener(WriteListener)} is a no-op (buffer-only stream).</li>
 * </ul>
 */
public class CapturingServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream buffer;

    /**
     * 🏗️ Create a new capturing stream backed by the given buffer.
     *
     * @param buffer in-memory buffer where written data will be collected
     */
    public CapturingServletOutputStream(ByteArrayOutputStream buffer) {
        this.buffer = buffer;
    }

    /**
     * ✅ Always ready (since writes go to memory).
     */
    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 🚫 No-op: non-blocking listener is not supported.
     */
    @Override
    public void setWriteListener(WriteListener writeListener) {
        // no-op
    }

    /**
     * 📥 Write a subarray of bytes into the buffer.
     *
     * @throws IndexOutOfBoundsException if offset/length are invalid
     */
    @Override
    public void write(byte[] bytes, int offset, int length) {
        if (bytes == null || length == 0) {
            return;
        }

        if (offset < 0 || length < 0 || offset + length > bytes.length) {
            throw new IndexOutOfBoundsException(
                    "offset=%d, length=%d, bytes.length=%d".formatted(offset, length, bytes.length)
            );
        }

        buffer.write(bytes, offset, length);
    }

    /**
     * 📥 Write a single byte into the buffer.
     */
    @Override
    public void write(int value) {
        buffer.write(value);
    }

    /**
     * 📥 Write the entire byte array into the buffer.
     */
    @Override
    public void write(byte[] bytes) throws IOException {
        if (bytes != null) {
            buffer.write(bytes, 0, bytes.length);
        }
    }

    /**
     * 🔄 No-op flush (writes are already in memory).
     */
    @Override
    public void flush() {
        // no-op: buffer writes are immediate
    }
}
