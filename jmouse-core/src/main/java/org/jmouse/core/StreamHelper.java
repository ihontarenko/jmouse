package org.jmouse.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * 💾 Stream utility helper.
 *
 * <p>Provides convenience methods for copying data between
 * {@link InputStream}, {@link OutputStream}, and byte/string sources.</p>
 */
public final class StreamHelper {

    /**
     * 📏 Default buffer size (8 KB).
     */
    public static final int BUFFER_SIZE = 8192;

    private StreamHelper() {
        // 🚫 Utility class: prevent instantiation
    }

    /**
     * 📤 Copy all bytes from a byte array to an output stream.
     *
     * @param input  source bytes
     * @param output target output stream
     */
    public static void copy(byte[] input, OutputStream output) {
        try {
            output.write(input);
            output.flush();
        } catch (IOException ignore) {
        }
    }

    /**
     * 📤 Copy all characters from a string to an output stream using the given charset.
     *
     * @param input   source string
     * @param charset encoding charset
     * @param output  target output stream
     */
    public static void copy(String input, Charset charset, OutputStream output) {
        copy(input.getBytes(charset), output);
    }

    /**
     * 📤 Copy all data from an input stream to an output stream.
     *
     * @param input  source stream
     * @param output target stream
     */
    public static void copy(InputStream input, OutputStream output) {
        try {
            input.transferTo(output);
        } catch (IOException ignore) {
        }
    }

    /**
     * 📤 Copy a specific range of bytes from an input stream to an output stream.
     *
     * @param input  source stream
     * @param output target stream
     * @param start  starting byte index (0-based)
     * @param end    ending byte index (inclusive)
     * @return number of bytes successfully copied
     */
    public static long copy(InputStream input, OutputStream output, long start, long end) {
        long bytesCount = end - start + 1;

        try {
            long   skipped = skipFully(input, start);
            byte[] buffer  = new byte[(int) Math.min(BUFFER_SIZE, bytesCount)];

            if (skipped > 0) {
                throw new IOException("Skipped only %d bytes out of %d required".formatted(start - skipped, start));
            }

            while (bytesCount > 0) {
                int chunk = (int) Math.min(buffer.length, bytesCount);
                int count = input.read(buffer, 0, chunk);
                if (count > 0) {
                    bytesCount -= count;
                    output.write(buffer, 0, count);
                } else {
                    break;
                }
            }
        } catch (IOException ignore) {
        }

        return end - start + 1 - bytesCount;
    }

    /**
     * Skip exactly n bytes (looping until all skipped or EOF).
     */
    public static long skipFully(InputStream input, long bytesCount) {
        long remaining = bytesCount;

        try {
            while (remaining > 0) {
                long skipped = input.skip(remaining);
                if (skipped <= 0) {
                    // read-and-discard fallback if skip returns 0
                    if (input.read() == -1) {
                        break;
                    }
                    skipped = 1;
                }
                remaining -= skipped;
            }

        } catch (IOException ignore) {
        }

        return remaining;
    }

}
