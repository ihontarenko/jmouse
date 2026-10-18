package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileIndex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Immutable, content-addressed contributions, independent of source paths. */
final class FileIndexCache {
    private static final System.Logger LOGGER = System.getLogger(FileIndexCache.class.getName());
    private final IndexLayout layout;

    FileIndexCache(IndexLayout layout) {
        this.layout = layout;
    }

    Optional<FileIndex> read(String hash, String source) {
        Path path = layout.stub(hash);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            Object value = IndexBinaryCodec.decode(ByteBuffer.wrap(Files.readAllBytes(path)));
            if (!(value instanceof FileIndex file)) {
                throw new IOException("The stub does not contain a FileIndex.");
            }
            return Optional.of(rebase(file, source));
        } catch (IOException exception) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Rebuilding an invalid index stub for {0}: {1}",
                    source,
                    exception.getMessage());
            return Optional.empty();
        }
    }

    void write(String hash, FileIndex file) {
        try {
            IndexFiles.atomicWrite(layout.stub(hash), IndexBinaryCodec.encode(rebase(file, "")));
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not persist a Java index contribution.", exception);
        }
    }

    private static FileIndex rebase(FileIndex file, String source) {
        return file.withSource(source);
    }
}
