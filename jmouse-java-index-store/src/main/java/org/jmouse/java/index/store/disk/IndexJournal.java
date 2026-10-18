package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileSymbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordered, durable metadata deltas and compaction for one writer-locked scope. */
final class IndexJournal implements AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(IndexJournal.class.getName());
    private static final int MAXIMUM_SEGMENTS = 8;
    private static final int MINIMUM_COMPACTION_DELTAS = 1024;

    private final IndexLayout layout;
    private final String scope;
    private final Map<String, IndexEntry> entries = new HashMap<>();
    private final List<IndexEntry> pending = new ArrayList<>();
    private final List<String> segments = new ArrayList<>();
    private final FileChannel lockChannel;
    private final FileLock writerLock;
    private long revision;
    private long recordsWritten;
    private String writableSegment;

    IndexJournal(IndexLayout layout, String scope) throws IOException {
        this.layout = layout;
        this.scope = scope;
        Files.createDirectories(layout.directory());
        Files.createDirectories(layout.stubs());
        lockChannel =
                FileChannel.open(
                        layout.writerLock(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
        } catch (RuntimeException | IOException exception) {
            lockChannel.close();
            throw new IOException(
                    "Could not acquire the index writer lock for " + scope, exception);
        }
        if (acquired == null) {
            lockChannel.close();
            throw new IOException("Another writer owns the index for " + scope);
        }
        writerLock = acquired;
        try {
            restore();
        } catch (RuntimeException | IOException exception) {
            writerLock.release();
            lockChannel.close();
            throw exception;
        }
    }

    IndexEntry entry(String source) {
        return entries.get(source);
    }

    List<FileSymbols> catalog() {
        return entries.values().stream().map(IndexEntry::symbols).toList();
    }

    long revision() {
        return revision;
    }

    void replace(IndexEntry entry) {
        IndexEntry previous = entries.put(entry.source(), entry);
        pending.add(entry);
        if (previous == null || !previous.contentHash().equals(entry.contentHash())) {
            revision++;
        }
    }

    void remove(String source) {
        if (entries.remove(source) != null) {
            pending.add(new IndexEntry(source, null, 0, 0, null));
            revision++;
        }
    }

    void flush() throws IOException {
        if (pending.isEmpty()) {
            return;
        }
        if (writableSegment == null) {
            writableSegment = SegmentName.create().filename();
            segments.add(writableSegment);
        }
        byte[] frame = IndexBinaryCodec.encode(List.copyOf(pending));
        try (FileChannel channel =
                FileChannel.open(
                        layout.segment(writableSegment),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND)) {
            IndexFiles.write(
                    channel, ByteBuffer.allocate(Integer.BYTES).putInt(frame.length).flip());
            IndexFiles.write(channel, ByteBuffer.wrap(frame));
            channel.force(false);
        }
        writeManifest();
        recordsWritten += pending.size();
        pending.clear();
        if (segments.size() > MAXIMUM_SEGMENTS
                || recordsWritten
                        > entries.size()
                                + Math.max(MINIMUM_COMPACTION_DELTAS, entries.size() / 2)) {
            compact();
        }
    }

    private void compact() throws IOException {
        String snapshot = SegmentName.create().filename();
        byte[] contents = IndexBinaryCodec.encode(List.copyOf(entries.values()));
        ByteBuffer framed =
                ByteBuffer.allocate(Integer.BYTES + contents.length)
                        .putInt(contents.length)
                        .put(contents);
        IndexFiles.atomicWrite(layout.segment(snapshot), framed.array());
        segments.clear();
        segments.add(snapshot);
        writableSegment = null;
        recordsWritten = entries.size();
        writeManifest();
    }

    private void restore() throws IOException {
        Path manifestFile = layout.manifest();
        if (!Files.exists(manifestFile)) {
            return;
        }
        try {
            Object value =
                    IndexBinaryCodec.decode(ByteBuffer.wrap(Files.readAllBytes(manifestFile)));
            if (!(value instanceof IndexManifest manifest)
                    || !scope.equals(manifest.projectRoot())
                    || !layout.revisionKey().equals(manifest.revisionKey())) {
                throw new IOException("The manifest belongs to another scope or revision.");
            }
            revision = manifest.modificationCount();
            for (String filename : manifest.segments()) {
                restoreSegment(layout.segment(filename));
                segments.add(filename);
            }
            revision = Math.max(revision, recordsWritten);
        } catch (RuntimeException | IOException exception) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Rebuilding invalid project index metadata: {0}",
                    exception.getMessage());
            entries.clear();
            segments.clear();
            recordsWritten = 0;
            revision++;
        }
        // Only this cache's unreferenced generated segment files are eligible for reclamation.
        try (var files = Files.list(layout.directory())) {
            for (Path file :
                    files.filter(
                                    path ->
                                            SegmentName.parse(path.getFileName().toString())
                                                    .isPresent())
                            .toList()) {
                if (!segments.contains(file.getFileName().toString())) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException exception) {
                        LOGGER.log(
                                System.Logger.Level.DEBUG,
                                "Mapped index segment remains in use: {0}",
                                file);
                    }
                }
            }
        }
    }

    private void restoreSegment(Path segment) throws IOException {
        try (FileChannel channel = FileChannel.open(segment, StandardOpenOption.READ)) {
            if (channel.size() == 0) {
                return;
            }
            if (channel.size() > Integer.MAX_VALUE) {
                throw new IOException("Index segment exceeds its supported size.");
            }
            ByteBuffer bytes = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            while (bytes.hasRemaining()) {
                if (bytes.remaining() < Integer.BYTES) {
                    break; // An interrupted final append has no committed record.
                }
                int length = bytes.getInt();
                if (length < 0 || length > IndexBinaryCodec.MAXIMUM_BYTES) {
                    throw new IOException("Invalid index segment frame size.");
                }
                if (length > bytes.remaining()) {
                    break;
                }
                ByteBuffer frame = bytes.slice();
                frame.limit(length);
                Object decoded = IndexBinaryCodec.decode(frame);
                if (!(decoded instanceof List<?> updates)) {
                    throw new IOException("Segment frame is not a contribution list.");
                }
                for (Object update : updates) {
                    if (!(update instanceof IndexEntry entry)) {
                        throw new IOException("Invalid contribution in index segment.");
                    }
                    if (entry.symbols() == null) {
                        entries.remove(entry.source());
                    } else {
                        new ContentHash(entry.contentHash());
                        entries.put(entry.source(), entry);
                    }
                    recordsWritten++;
                }
                bytes.position(bytes.position() + length);
            }
        }
    }

    private void writeManifest() throws IOException {
        IndexFiles.atomicWrite(
                layout.manifest(),
                IndexBinaryCodec.encode(
                        new IndexManifest(scope, layout.revisionKey(), revision, segments)));
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } finally {
            try {
                writerLock.release();
            } finally {
                lockChannel.close();
            }
        }
    }
}
