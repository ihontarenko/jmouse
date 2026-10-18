package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.FileSymbols;
import org.jmouse.java.index.store.IndexStore;
import org.jmouse.java.parser.JavaParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Standard disk adapter. Content-addressed stubs are shared between scopes; only search metadata is
 * restored eagerly. Delta segments are checksummed, read through mmap and compacted in the
 * background by the caller's flush cadence. No threads, project walking or product lifecycle live
 * in this adapter.
 */
public final class DiskIndexStore implements IndexStore, AutoCloseable {
    private final IndexLayout layout;
    private final JavaParser.Level level;
    private final JavaParser parser = new JavaParser();
    private final IndexJournal journal;
    private final FileIndexCache cache;
    private boolean closed;

    public DiskIndexStore(Path cacheRoot, String scope, JavaParser.Level level) throws IOException {
        this.level = level;
        layout = IndexLayout.create(cacheRoot, scope, level);
        journal = new IndexJournal(layout, scope);
        cache = new FileIndexCache(layout);
    }

    @Override
    public synchronized Optional<FileIndex> read(String source) {
        requireOpen();
        IndexEntry entry = journal.entry(source);
        if (entry == null) {
            return Optional.empty();
        }
        return cache.read(entry.contentHash(), source);
    }

    @Override
    public synchronized List<FileSymbols> catalog() {
        requireOpen();
        return journal.catalog();
    }

    /**
     * Metadata fast path for a source scan. Watch events should force hashing even if timestamps
     * match.
     */
    public synchronized boolean isCurrent(String source, long modifiedNanoseconds, long size) {
        requireOpen();
        IndexEntry entry = journal.entry(source);
        return entry != null
                && entry.modifiedNanoseconds() == modifiedNanoseconds
                && entry.size() == size
                && Files.isRegularFile(layout.stub(entry.contentHash()));
    }

    /**
     * Parses only on a content-cache miss. Timestamp/path differences never invalidate a shared
     * stub.
     */
    public synchronized IndexedFile index(String source, byte[] bytes, long modifiedNanoseconds) {
        requireOpen();
        String contentHash = IndexFiles.hash(bytes);
        FileIndex file = cache.read(contentHash, source).orElse(null);
        boolean parsed = false;
        if (file == null) {
            file =
                    FileIndex.of(
                            parser.parse(source, new String(bytes, StandardCharsets.UTF_8), level));
            cache.write(contentHash, file);
            parsed = true;
        }
        journal.replace(
                new IndexEntry(
                        source,
                        contentHash,
                        modifiedNanoseconds,
                        bytes.length,
                        FileSymbols.of(file)));
        return new IndexedFile(file, parsed);
    }

    public record IndexedFile(FileIndex file, boolean parsed) {}

    @Override
    public synchronized void write(FileIndex file, String contentHash) {
        requireOpen();
        new ContentHash(contentHash);
        cache.write(contentHash, file);
        journal.replace(
                new IndexEntry(
                        file.source(), contentHash, Long.MIN_VALUE, -1, FileSymbols.of(file)));
    }

    @Override
    public synchronized long revision() {
        requireOpen();
        return journal.revision();
    }

    @Override
    public synchronized void remove(String source) {
        requireOpen();
        journal.remove(source);
    }

    public synchronized void flush() throws IOException {
        requireOpen();
        journal.flush();
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("The index store is closed.");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            try {
                journal.close();
            } finally {
                closed = true;
            }
        }
    }
}
