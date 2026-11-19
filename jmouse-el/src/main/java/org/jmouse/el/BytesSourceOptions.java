package org.jmouse.el;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Storage/resource policy, separate from a binary format's limits. */
public record BytesSourceOptions(int bufferSize, int maximumSnapshotBytes) {
    public BytesSourceOptions {
        if (bufferSize <= 0 || maximumSnapshotBytes < 0) {
            throw new IllegalArgumentException("Invalid byte source limits");
        }
    }

    public static BytesSourceOptions standard() {
        return Defaults.INSTANCE;
    }

    private static final class Defaults {
        private static final BytesSourceOptions INSTANCE = read();

        private static BytesSourceOptions read() {
            Properties values = new Properties();
            try (InputStream input =
                    BytesSourceOptions.class.getResourceAsStream("bytes-source.properties")) {
                if (input == null) {
                    throw new IllegalStateException("Missing byte source options");
                }
                values.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read byte source options", exception);
            }
            return new BytesSourceOptions(
                    Integer.parseInt(values.getProperty("buffer.size")),
                    Integer.parseInt(values.getProperty("maximum.snapshot.bytes")));
        }
    }
}
