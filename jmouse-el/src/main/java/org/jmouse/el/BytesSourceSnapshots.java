package org.jmouse.el;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** Snapshot ownership and stream limits are source concerns, independent from cursor grammars. */
final class BytesSourceSnapshots {
    private BytesSourceSnapshots() {}

    static BytesSource read(String name, InputStream input, BytesSourceOptions options)
            throws IOException {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(options, "options");
        int maximum = options.maximumSnapshotBytes();
        int bufferSize = (int) Math.min(options.bufferSize(), (long) maximum + 1);
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(bufferSize, maximum));
        byte[] buffer = new byte[bufferSize];
        while (true) {
            int remaining = maximum - output.size();
            int requested = (int) Math.min(buffer.length, (long) remaining + 1);
            int count = input.read(buffer, 0, requested);
            if (count < 0) {
                return ArrayBytesSource.owned(name, output.toByteArray());
            }
            if (count == 0) {
                int next = input.read();
                if (next < 0) {
                    return ArrayBytesSource.owned(name, output.toByteArray());
                }
                buffer[0] = (byte) next;
                count = 1;
            }
            if (count > remaining) {
                throw new IOException("Byte source exceeds the configured snapshot limit: " + name);
            }
            output.write(buffer, 0, count);
        }
    }
}
