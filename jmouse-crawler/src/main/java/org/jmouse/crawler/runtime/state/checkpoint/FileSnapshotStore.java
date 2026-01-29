package org.jmouse.crawler.runtime.state.checkpoint;

import org.jmouse.core.Verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class FileSnapshotStore {

    private final Path file;
    private final Path temporary;

    public FileSnapshotStore(Path file) {
        this.file = Verify.nonNull(file, "file");
        this.temporary = file.resolveSibling(file.getFileName() + ".tmp");
    }

    public String tryRead() {
        if (!Files.exists(file)) {
            return null;
        }

        try {
            String value = Files.readString(file, UTF_8);
            return value.isBlank() ? null : value;
        } catch (IOException e) {
            return null;
        }
    }

    public void write(String content) {
        Verify.nonNull(content, "content");
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(temporary, content, UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);

            try {
                Files.move(temporary, file, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (Exception e) {
                Files.move(temporary, file, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write snapshot: " + file, e);
        }
    }
}
