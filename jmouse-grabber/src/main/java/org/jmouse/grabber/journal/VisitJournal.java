package org.jmouse.grabber.journal;

import java.nio.file.Path;
import java.util.List;

import org.jmouse.grabber.Visit;
import org.jmouse.grabber.VisitKey;

/**
 * 📓 What has already been done, and what was still outstanding.
 *
 * <p>The brief calls resume a <i>критично важлива вимога</i>, and this is the seam that answers it. A
 * run that dies at half way must not redo the first half on the next start.</p>
 *
 * <p>⚠️ <b>There are two levels because they answer two different questions</b>, and collapsing them
 * loses one of the answers:</p>
 * <ul>
 *   <li><b>address level</b> — {@link #seen(VisitKey)}. Has this page been visited? This is
 *       deduplication within a run and resume across runs, and it is the same question both times.</li>
 *   <li><b>item level</b> — {@link #processed(String)}. Has this <i>thing</i> been handled? Keyed by
 *       whatever the caller says identifies it, because one product reachable at two addresses is
 *       one product, and a grabber that only knows addresses will store it twice.</li>
 * </ul>
 *
 * <p>The pending frontier is journalled alongside, so a restart resumes the queue rather than
 * re-deriving it from the seeds and re-walking everything in between.</p>
 */
public interface VisitJournal extends AutoCloseable {

    /**
     * 🧠 A journal that lives for one run and remembers nothing afterwards.
     */
    static VisitJournal inMemory() {
        return new MemoryVisitJournal();
    }

    /**
     * 💾 A journal on disk, in the given directory, which is what makes a run resumable.
     *
     * <p>Append-only lines. ⚠️ Deliberately not SQLite and not RocksDB — both are a new dependency
     * for a library module, and neither buys anything a resumable crawl needs.</p>
     */
    static VisitJournal onDisk(Path directory) {
        return new FileVisitJournal(directory);
    }

    /**
     * 👁️ Whether this address has been visited before — in this run or in an earlier one.
     */
    boolean seen(VisitKey key);

    /**
     * 👁️ Records that it has. Answers whether this call was the first sighting, so a caller can
     * claim a visit and act on the answer in one step rather than racing between two.
     */
    boolean markSeen(VisitKey key);

    /**
     * 📦 Whether this item has been handled, by whatever key the caller says identifies it.
     */
    boolean processed(String itemKey);

    /**
     * 📦 Records that it has.
     */
    void markProcessed(String itemKey);

    /**
     * ⏳ Adds a visit to the outstanding frontier, under the key that identifies it.
     *
     * <p>⚠️ The key is passed in rather than derived here. Normalisation and any per-route override
     * belong to the run, and a journal that recomputed a key would be a second opinion about what
     * "the same page" means.</p>
     */
    void remember(VisitKey key, Visit visit);

    /**
     * ✅ Takes a visit off the outstanding frontier, because it is finished.
     */
    void forget(VisitKey key);

    /**
     * ⏳ What was outstanding when the last run ended — the frontier a restart resumes from.
     */
    List<Visit> pending();

    /**
     * 🧹 Forgets everything, which is how a run is deliberately started over.
     */
    void clear();

    /**
     * 📊 How many addresses this journal has seen.
     */
    long seenCount();

    @Override
    default void close() {
    }

}
