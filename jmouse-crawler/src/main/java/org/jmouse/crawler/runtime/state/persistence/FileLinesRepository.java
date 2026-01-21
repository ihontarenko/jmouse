package org.jmouse.crawler.runtime.state.persistence.file;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.SnapshotRepository;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

public final class FileLinesRepository implements SnapshotRepository<List<String>> {

    private final Path file;

    public FileLinesRepository(Path file) {
        this.file = Verify.nonNull(file, "file");
    }

    @Override
    public List<String> load() {
        if (!Files.exists(file)) return List.of();
        try {
            return Files.readAllLines(file, UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public void save(List<String> lines) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(file, lines, UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write lines: " + file, e);
        }
    }

    public void append(String line) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(file, line + "\n", UTF_8, CREATE, WRITE, APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append WAL line: " + file, e);
        }
    }

    public void truncate() {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(file, new byte[0], CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to truncate file: " + file, e);
        }
    }

}
