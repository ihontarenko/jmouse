package org.jmouse.java.index.store.disk;

import org.jmouse.java.index.FileSymbols;

/** A disk manifest contribution. Null symbols/hash is a deletion in a delta segment. */
record IndexEntry(
        String source,
        String contentHash,
        long modifiedNanoseconds,
        long size,
        FileSymbols symbols) {}
