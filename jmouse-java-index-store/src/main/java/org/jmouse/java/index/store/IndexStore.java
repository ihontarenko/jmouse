package org.jmouse.java.index.store;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.FileSymbols;

import java.util.List;
import java.util.Optional;

/**
 * Persistence of the active index in one caller-defined scope (project, corpus or generation).
 * Storage adapters own durability/transaction boundaries. Product jobs and access rules stay
 * outside.
 */
public interface IndexStore {
    Optional<FileIndex> read(String source);

    List<FileSymbols> catalog();

    /**
     * Replaces a source contribution. The content hash identifies the source bytes, not its path.
     */
    void write(FileIndex file, String contentHash);

    void remove(String source);

    /** Monotonic modification counter within this store scope. */
    long revision();
}
