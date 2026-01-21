package org.jmouse.crawler.runtime.state.persistence.file;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.SnapshotRepository;
import org.jmouse.crawler.runtime.state.persistence.StateCodec;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

public final class FileSnapshotRepository<S> implements SnapshotRepository<S> {

    private final Path       snapshotFile;
    private final Path       temporaryFile;
    private final StateCodec codec;
    private final Class<S>   snapshotType;
    private final S          empty;

    public FileSnapshotRepository(Path snapshotFile, StateCodec codec, Class<S> snapshotType, S empty) {
        this.snapshotFile = Verify.nonNull(snapshotFile, "snapshotFile");
        this.temporaryFile = snapshotFile.resolveSibling(snapshotFile.getFileName() + ".tmp");
        this.codec = Verify.nonNull(codec, "codec");
        this.snapshotType = Verify.nonNull(snapshotType, "snapshotType");
        this.empty = Verify.nonNull(empty, "empty");
    }

    @Override
    public S load() {
        if (!Files.exists(snapshotFile)) {
            return empty;
        }

        try {
            List<String> lines = Files.readAllLines(snapshotFile, UTF_8);

            if (lines.isEmpty()) {
                return empty;
            }

            return codec.decode(lines.getFirst(), snapshotType);
        } catch (Exception e) {
            return empty;
        }
    }

    @Override
    public void save(S snapshot) {
        String line = codec.encode(snapshot);

        try {
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(temporaryFile, line + "\n", UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);

            try {
                Files.move(temporaryFile, snapshotFile, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, snapshotFile, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write snapshot: " + snapshotFile, e);
        }
    }
}
