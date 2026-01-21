package org.jmouse.crawler.runtime.state.persistence.file;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.Codec;
import org.jmouse.crawler.runtime.state.persistence.Durability;
import org.jmouse.crawler.runtime.state.persistence.WALRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.*;

public final class FileWALRepository<E> implements WALRepository<E> {

    private final Path       walFile;
    private final Path       rotatedFile;
    private final Codec      codec;
    private final Class<E>   eventType;
    private final Durability durability;

    private final ReentrantLock lock = new ReentrantLock();

    private long operationsSinceFlush = 0;
    private long lastFlushNanos       = System.nanoTime();

    public FileWALRepository(Path walFile, Codec codec, Class<E> eventType, Durability durability) {
        this.walFile = Verify.nonNull(walFile, "walFile");
        this.rotatedFile = walFile.resolveSibling(walFile.getFileName() + ".old");
        this.codec = Verify.nonNull(codec, "codec");
        this.eventType = Verify.nonNull(eventType, "eventType");
        this.durability = Verify.nonNull(durability, "durability");
    }

    @Override
    public void append(E event) {
        String line = codec.encode(event);

        lock.lock();
        try {
            Files.createDirectories(walFile.getParent());
            Files.writeString(walFile, line + "\n", StandardCharsets.UTF_8, CREATE, WRITE, APPEND);

            operationsSinceFlush++;

            if (durability instanceof Durability.Sync) {
                flush();
                return;
            }

            if (durability instanceof Durability.Batched(int maxRecords, Duration maxDelay)) {
                if (operationsSinceFlush >= maxRecords || elapsedExceeded(maxDelay)) {
                    flush();
                }
                return;
            }

            if (durability instanceof Durability.Async(Duration flushInterval)) {
                if (elapsedExceeded(flushInterval)) {
                    flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to append WAL: " + walFile, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<E> readAll() {
        if (!Files.exists(walFile)) {
            return List.of();
        }

        try {
            List<String> lines  = Files.readAllLines(walFile, StandardCharsets.UTF_8);
            List<E>      events = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                events.add(codec.decode(line, eventType));
            }
            return events;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read WAL: " + walFile, e);
        }
    }

    @Override
    public void flush() {
        operationsSinceFlush = 0;
        lastFlushNanos = System.nanoTime();
    }

    @Override
    public void rotate() {
        lock.lock();
        try {
            if (!Files.exists(walFile)) {
                return;
            }
            Files.deleteIfExists(rotatedFile);
            Files.move(walFile, rotatedFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rotate WAL: " + walFile, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        flush();
    }

    private boolean elapsedExceeded(Duration d) {
        long now          = System.nanoTime();
        long elapsedNanos = now - lastFlushNanos;
        return elapsedNanos >= d.toNanos();
    }
}
