package org.jmouse.storage.jpa.sweeper;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 📊 What one sweep did.
 *
 * <p>Reported rather than merely logged because the first run against real data is the first
 * honest measurement anyone has of how much storage has been leaking — a number worth having, not
 * a line to scroll past.</p>
 *
 * @param sourcesConsulted how many reference sources contributed to the union
 * @param referencesSeen   size of that union
 * @param objectsExamined  rows older than the grace period that were considered
 * @param objectsReclaimed rows that had no reference and were removed
 * @param bytesFreed       total size of the reclaimed objects
 * @param failures         objects whose bytes could not be removed, left registered for a rerun
 * @param cutOff           only objects registered before this were candidates
 * @param elapsed          how long the sweep took
 */
public record SweepReport(int sourcesConsulted, int referencesSeen, int objectsExamined,
                          int objectsReclaimed, long bytesFreed, int failures,
                          LocalDateTime cutOff, Duration elapsed) {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    /**
     * 📏 Reclaimed bytes as megabytes, for a log line a human reads rather than counts.
     *
     * @return megabytes freed
     */
    public long megabytesFreed() {
        return bytesFreed / BYTES_PER_MEGABYTE;
    }

    /**
     * 🧾 A one-line summary.
     *
     * @return the summary
     */
    @Override
    public String toString() {
        return ("Swept %d object(s) older than %s: reclaimed %d (%d MB), left %d referenced, "
                + "%d failure(s), %d source(s), took %d ms")
                .formatted(objectsExamined, cutOff, objectsReclaimed, megabytesFreed(),
                           objectsExamined - objectsReclaimed - failures, failures,
                           sourcesConsulted, elapsed.toMillis());
    }
}
