package org.jmouse.crawler.runtime.state.persistence.file;

import org.jmouse.core.Verify;
import org.jmouse.crawler.runtime.state.persistence.Codec;
import org.jmouse.crawler.runtime.state.persistence.Durability;
import org.jmouse.crawler.runtime.state.persistence.WalRepository;
import org.jmouse.crawler.runtime.state.persistence.wal.StateEvent;
import org.jmouse.crawler.runtime.state.persistence.wal.StateEventMapping;
import org.jmouse.crawler.runtime.state.persistence.wal.WalEventCodec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.*;

public final class FileWalRepository<E> implements WalRepository<E> {

    private final Path       walFile;
    private final WalEventCodec      walCodec;
    private final Class<E>   eventType;
    private final Durability durability;

    private final ReentrantLock lock = new ReentrantLock();

    private long operationsSinceSynchronization = 0;
    private long lastSyncNanos                  = System.nanoTime();

    public FileWalRepository(Path walFile, Codec codec, Class<E> eventType, Durability durability) {
        this.walFile = Verify.nonNull(walFile, "walFile");
        this.walCodec = new WalEventCodec(Verify.nonNull(codec, "codec"), StateEventMapping.standard());
        this.eventType = Verify.nonNull(eventType, "eventType");
        this.durability = Verify.nonNull(durability, "durability");
    }

    private static Path parentDir(Path file) {
        Path parent = file.getParent();
        return (parent != null) ? parent : Path.of(".");
    }

    @Override
    public void append(E event) {
        Verify.nonNull(event, "event");

        String line  = walCodec.encodeEvent((StateEvent) event);
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);

        lock.lock();
        try {
            Files.createDirectories(parentDir(walFile));

            try (FileChannel channel = FileChannel.open(walFile, CREATE, WRITE, APPEND)) {
                channel.write(ByteBuffer.wrap(bytes));
                operationsSinceSynchronization++;

                if (durability instanceof Durability.Sync) {
                    channel.force(true);
                    markSynchronized();
                    return;
                }

                if (durability instanceof Durability.Batched(int maxRecords, Duration maxDelay)) {
                    if (operationsSinceSynchronization >= maxRecords || elapsedExceeded(maxDelay)) {
                        channel.force(true);
                        markSynchronized();
                    }
                    return;
                }

                if (durability instanceof Durability.Async(Duration flushInterval)) {
                    if (elapsedExceeded(flushInterval)) {
                        channel.force(true);
                        markSynchronized();
                    }
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
                if (line == null || line.isBlank()) {
                    continue;
                }
                events.add((E) walCodec.decodeEvent(line));
            }

            return events;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read WAL: " + walFile, e);
        }
    }

    @Override
    public void truncate() {
        lock.lock();
        try {
            if (Files.exists(walFile)) {
                Files.newByteChannel(walFile, WRITE, TRUNCATE_EXISTING).close();
                operationsSinceSynchronization = 0;
                lastSyncNanos = System.nanoTime();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to truncate WAL: " + walFile, e);
        } finally {
            lock.unlock();
        }
    }

    private void markSynchronized() {
        operationsSinceSynchronization = 0;
        lastSyncNanos = System.nanoTime();
    }

    private boolean elapsedExceeded(Duration duration) {
        return (System.nanoTime() - lastSyncNanos) >= duration.toNanos();
    }
}
