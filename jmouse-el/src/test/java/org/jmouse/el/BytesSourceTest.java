package org.jmouse.el;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jmouse.el.lexer.BytesCursor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

class BytesSourceTest {
    @TempDir Path directory;

    @Test
    void streamSnapshotsAreBoundedAndDoNotCloseTheCallersStream() throws IOException {
        AtomicInteger closed = new AtomicInteger();
        InputStream stream =
                new ByteArrayInputStream(new byte[] {1, 2, 3}) {
                    @Override
                    public void close() {
                        closed.incrementAndGet();
                    }
                };
        BytesSource source = BytesSource.read("stream", stream, new BytesSourceOptions(2, 3));
        assertArrayEquals(new byte[] {1, 2, 3}, source.slice(0, 3));
        assertEquals(0, closed.get());
        assertThrows(
                IOException.class,
                () ->
                        BytesSource.read(
                                "too large",
                                new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                                new BytesSourceOptions(2, 3)));
    }

    @Test
    void fileAndMemorySourcesExposeTheSameRandomAccessContract() throws IOException {
        byte[] bytes = {0x12, 0x34, 0x56, 0x78, 0x01};
        Path path = directory.resolve("source.bin");
        Files.write(path, bytes);
        BytesSource memory = BytesSource.copyOf("memory", bytes);
        BytesSource file = BytesSource.open(path, new BytesSourceOptions(2, 16));
        try (file) {
            assertEquals(memory.contentLength(), file.contentLength());
            assertEquals(memory.digest(), file.digest());
            assertArrayEquals(memory.slice(1, 3), file.slice(1, 3));
            BytesCursor cursor = BytesCursor.of(file, ByteOrder.LITTLE_ENDIAN, () -> {});
            assertEquals(0x78563412L, cursor.unsigned32());
            assertEquals(1, cursor.unsigned8());
        }
        assertThrows(IllegalStateException.class, () -> file.unsigned8(0));
    }
}
