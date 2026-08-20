package org.jmouse.storage.jpa.sweeper;

import org.jmouse.storage.FileStores;
import org.jmouse.storage.jpa.StoredFile;
import org.jmouse.storage.jpa.StoredFileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 🧹 One sweep in progress: the reference union taken once, then walked batch by batch.
 *
 * <p>Split from {@link OrphanSweeper} so that <em>the caller</em> decides what a transaction is.
 * Sweeping a large registry inside one transaction would hold locks for as long as it took and
 * lose every reclaimed byte to a single failure at the end; sweeping with no transaction at all is
 * not the library's call to make. Handing back a run the caller pumps means each
 * {@link #sweepNextBatch()} can sit in its own transaction, or not, as the application prefers:</p>
 *
 * <pre>{@code
 * SweepRun run = sweeper.begin();
 *
 * while (run.hasMore()) {
 *     transactionTemplate.executeWithoutResult(status -> run.sweepNextBatch());
 * }
 *
 * log.info("{}", run.report());
 * }</pre>
 *
 * <h3>Safe to interrupt, safe to run twice</h3>
 *
 * <p>Stopping between batches loses nothing: what was reclaimed stays reclaimed and what was not
 * is found again by the next run. Within a batch, bytes are removed <em>before</em> the row that
 * records them — the opposite order would leave bytes behind with nothing able to name them, while
 * this order leaves at worst a row pointing at an object already gone, which the next sweep
 * removes because deleting an absent object is a success.</p>
 */
public class SweepRun {

    private static final Logger LOGGER = LoggerFactory.getLogger(SweepRun.class);

    private final StoredFileRegistry registry;
    private final FileStores         fileStores;
    private final Set<String>        referencedIdentifiers;
    private final LocalDateTime      cutOff;
    private final LocalDateTime      startedAt;
    private final int                sourcesConsulted;
    private final int                batchSize;

    /**
     * ⚠️ Whether this run actually removes anything.
     *
     * <p>A preview counts exactly what a real sweep would take and removes none of it. That matters
     * more than the sweep itself: the sweeper ships disabled in every product, so the first honest
     * measurement of how much is leaking has to be obtainable WITHOUT betting live data on the
     * reference sources being complete. Turning it on to find out is the wrong order.</p>
     */
    private final boolean            reclaiming;

    private String  cursor;
    private boolean exhausted;
    private int     objectsExamined;
    private int     objectsReclaimed;
    private long    bytesFreed;
    private int     failures;

    /**
     * 🏗️ Start a run over a union that has already been collected.
     *
     * @param registry              registry being swept
     * @param fileStores            every backend, so each orphan is reclaimed through the one
     *                              that actually holds it
     * @param referencedIdentifiers union of everything every source still points at
     * @param sourcesConsulted      how many sources contributed to that union
     * @param cutOff                only objects registered strictly before this are candidates
     * @param batchSize             how many rows one batch examines
     * @param reclaiming            {@code false} to count what would go and remove nothing
     */
    SweepRun(StoredFileRegistry registry, FileStores fileStores, Set<String> referencedIdentifiers,
             int sourcesConsulted, LocalDateTime cutOff, int batchSize, boolean reclaiming) {
        this.reclaiming            = reclaiming;
        this.registry              = registry;
        this.fileStores            = fileStores;
        this.referencedIdentifiers = referencedIdentifiers;
        this.sourcesConsulted      = sourcesConsulted;
        this.cutOff                = cutOff;
        this.batchSize             = batchSize;
        this.startedAt             = LocalDateTime.now();
    }

    /**
     * ❓ Whether anything is left to examine.
     *
     * @return {@code true} until a batch comes back short
     */
    public boolean hasMore() {
        return !exhausted;
    }

    /**
     * 🧹 Examine the next batch, reclaiming whatever nothing points at.
     *
     * <p>Wrap this call in a transaction if reclaiming should be all-or-nothing per batch.</p>
     *
     * @return how many objects this batch reclaimed
     */
    public int sweepNextBatch() {
        if (exhausted) {
            return 0;
        }

        List<StoredFile> candidates = registry.listRegisteredBefore(cutOff, cursor, batchSize);

        if (candidates.size() < batchSize) {
            exhausted = true;
        }

        int reclaimedInBatch = 0;

        for (StoredFile candidate : candidates) {
            cursor = candidate.getIdentifier();
            objectsExamined++;

            if (referencedIdentifiers.contains(candidate.getIdentifier())) {
                continue;
            }

            if (!reclaiming) {
                // Counted exactly as a real sweep would count it, and left alone. The size is the
                // registry's rather than the backend's, which is what makes a preview cheap: it asks
                // no store anything.
                objectsReclaimed++;
                bytesFreed += candidate.getSizeBytes();
                reclaimedInBatch++;

                continue;
            }

            if (reclaim(candidate)) {
                reclaimedInBatch++;
            }
        }

        return reclaimedInBatch;
    }

    /**
     * 📊 What the run has done so far, or in full once {@link #hasMore()} turns false.
     *
     * @return the report
     */
    public SweepReport report() {
        return new SweepReport(sourcesConsulted, referencedIdentifiers.size(), objectsExamined,
                               objectsReclaimed, bytesFreed, failures, cutOff,
                               Duration.between(startedAt, LocalDateTime.now()));
    }

    /**
     * 🗑️ Remove one orphan's bytes and then its row.
     *
     * <p>Through the backend the object <em>recorded</em>, never through a default. Deleting an
     * absent object is a success on every backend, so asking the wrong one would report cheerful
     * progress while removing the row that was the last thing able to name the bytes — the exact
     * leak this component exists to stop, caused by the component itself.</p>
     *
     * <p>A backend that refuses to delete, or is no longer configured at all, leaves the row alone:
     * a registry row pointing at bytes that still exist is a state the next sweep can fix, whereas
     * bytes with no row is a state nothing can.</p>
     *
     * @param orphan the unreferenced row
     * @return {@code true} when the object was reclaimed
     */
    private boolean reclaim(StoredFile orphan) {
        try {
            fileStores.require(orphan.getBackend()).delete(orphan.getStorageKey());
        } catch (RuntimeException exception) {
            LOGGER.warn("🧹 Leaving '{}' registered — its bytes could not be removed from backend '{}'",
                        orphan.getStorageKey(), orphan.getBackend(), exception);
            failures++;
            return false;
        }

        registry.remove(orphan);

        objectsReclaimed++;
        bytesFreed += orphan.getSizeBytes();

        LOGGER.debug("🧹 Reclaimed '{}' ({} bytes)", orphan.getStorageKey(), orphan.getSizeBytes());

        return true;
    }
}
