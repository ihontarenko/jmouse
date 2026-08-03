package org.jmouse.storage.jpa;

import java.util.stream.Stream;

/**
 * 🔗 A product answering "which registered objects does this table still point at?".
 *
 * <p>One implementation per referring table — a product with uploads and documents supplies two —
 * and the sweeper takes the union of every one it can find. That union <em>is</em> the reference
 * set; nothing else is consulted.</p>
 *
 * <h3>Why this rather than a counter</h3>
 *
 * <p>A reference count was the obvious alternative and was rejected. It drifts the moment anything
 * removes a row outside the service that maintains it — a cascade, a hand-written statement, a
 * transaction that rolls back after the count was raised — and a drifted counter is worse than no
 * counter: too high and storage leaks silently forever, too low and the sweeper destroys a file
 * somebody still holds. Asking the table what it actually references cannot drift, because it is
 * not a cached answer.</p>
 *
 * <p>The cost is honest and small: a query per source per sweep, against a column that already has
 * to be indexed to be a useful foreign key.</p>
 */
public interface StoredFileReferences {

    /**
     * 🏷️ What this source is called in a sweep report, so an operator reading one can tell which
     * table contributed what.
     *
     * @return a short human-readable name, e.g. {@code file_uploads}
     */
    String sourceName();

    /**
     * 📤 Every registry identifier this source still points at.
     *
     * <p>Streamed rather than collected because a source may reference a great many objects and
     * the sweeper only ever adds them to a set. <strong>The caller closes the stream</strong>, so
     * an implementation is free to hold a cursor open behind it.</p>
     *
     * <p>Duplicates are fine, {@code null} entries are not.</p>
     *
     * @return the identifiers still referenced
     */
    Stream<String> referencedIdentifiers();
}
