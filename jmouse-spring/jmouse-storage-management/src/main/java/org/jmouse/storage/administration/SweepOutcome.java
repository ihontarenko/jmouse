package org.jmouse.storage.administration;

import org.jmouse.storage.jpa.sweeper.SweepReport;

/**
 * 🧹 What a sweep did — or, for a preview, what it would have done.
 *
 * <p>⚠️ {@code reclaiming} is on the wire on purpose. The two answers are numerically identical and
 * mean opposite things, and an interface that renders "reclaimed 412 objects (3.2 GB)" without saying
 * which of the two it is has told somebody their data is gone when it is not — or, far worse, the
 * reverse.</p>
 *
 * @param reclaiming       whether anything was actually removed
 * @param sourcesConsulted how many reference sources contributed
 * @param referencesSeen   how many identifiers the union held
 * @param objectsExamined  how many rows were walked
 * @param objectsReclaimed how many were reclaimed, or would be
 * @param bytesFreed       how many bytes, or would be
 * @param failures         how many could not be removed from their backend
 * @param cutOff           only objects registered before this were candidates
 * @param elapsedMillis    how long it took
 */
public record SweepOutcome(boolean reclaiming, int sourcesConsulted, int referencesSeen,
                           int objectsExamined, int objectsReclaimed, long bytesFreed, int failures,
                           String cutOff, long elapsedMillis) {

    /**
     * 🏗️ Describe a run.
     *
     * @param report     what the run reported
     * @param reclaiming whether it was a real sweep
     * @return the outcome
     */
    public static SweepOutcome of(SweepReport report, boolean reclaiming) {
        return new SweepOutcome(
            reclaiming, report.sourcesConsulted(), report.referencesSeen(), report.objectsExamined(),
            report.objectsReclaimed(), report.bytesFreed(), report.failures(),
            report.cutOff().toString(), report.elapsed().toMillis());
    }
}
