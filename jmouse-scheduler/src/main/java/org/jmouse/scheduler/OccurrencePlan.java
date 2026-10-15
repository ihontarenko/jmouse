package org.jmouse.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * What one pass does with one due trigger: which occurrences fire, how many late ones are dropped,
 * and where the trigger resumes. Pure — computed from the schedule, the policy and two instants.
 *
 * @param lateToFire        late occurrences the misfire policy keeps, oldest first
 * @param onTime            occurrences found within the misfire threshold, oldest first
 * @param lateCount         every late occurrence found; a lower bound when {@code lateCountIsLowerBound}
 * @param lateCountIsLowerBound whether the scan stopped at {@link #LATE_SCAN_CEILING}
 * @param firstLate         the earliest late occurrence, or {@code null}
 * @param lastLate          the latest late occurrence scanned, or {@code null}
 * @param nextFireAt        the first occurrence after now, or {@code null} when the schedule is exhausted
 */
record OccurrencePlan(
        List<Instant> lateToFire,
        List<Instant> onTime,
        long lateCount,
        boolean lateCountIsLowerBound,
        Instant firstLate,
        Instant lastLate,
        Instant nextFireAt
) {

    /** How many late occurrences one pass counts before it stops counting and jumps to the present. */
    static final int LATE_SCAN_CEILING = 100_000;

    /** Late occurrences the policy did not fire. */
    long droppedCount() {
        return lateCount - lateToFire.size();
    }

    /**
     * Plans a due trigger.
     *
     * @param schedule         the trigger's schedule
     * @param policy           its misfire policy
     * @param nextFireAt       the stored next occurrence, at or before {@code now}
     * @param now              the present
     * @param misfireThreshold how late an occurrence may be and still be on time
     * @return the plan
     */
    static OccurrencePlan plan(Schedule schedule, MisfirePolicy policy, Instant nextFireAt, Instant now,
                               Duration misfireThreshold) {
        Instant cutoff = now.minus(misfireThreshold);
        int retained = policy.retainedLateOccurrences();
        Deque<Instant> retainedLate = new ArrayDeque<>();
        long lateCount = 0;
        boolean lowerBound = false;
        Instant firstLate = null;
        Instant lastLate = null;
        Instant cursor = nextFireAt;

        while (cursor != null && cursor.isBefore(cutoff)) {
            lateCount++;

            if (firstLate == null) {
                firstLate = cursor;
            }

            lastLate = cursor;

            if (retained > 0) {
                retainedLate.addLast(cursor);

                if (retainedLate.size() > retained) {
                    retainedLate.removeFirst();
                }
            }

            if (lateCount >= LATE_SCAN_CEILING) {
                lowerBound = true;
                cursor = schedule.nextAfter(cutoff.minusNanos(1)).orElse(null);
                break;
            }

            cursor = schedule.nextAfter(cursor).orElse(null);
        }

        List<Instant> onTime = new ArrayList<>();

        while (cursor != null && !cursor.isAfter(now)) {
            onTime.add(cursor);
            cursor = schedule.nextAfter(cursor).orElse(null);
        }

        boolean coveredByOnTime = policy instanceof MisfirePolicy.FireOnce && !onTime.isEmpty();
        List<Instant> lateToFire = coveredByOnTime ? List.of() : List.copyOf(retainedLate);

        return new OccurrencePlan(lateToFire, List.copyOf(onTime), lateCount, lowerBound, firstLate, lastLate, cursor);
    }
}
