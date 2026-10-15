package org.jmouse.scheduler.spi;

import org.jmouse.scheduler.OccurrenceRecord;
import org.jmouse.scheduler.Trigger;
import org.jmouse.scheduler.TriggerId;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Where triggers, their state and their history live — and the one place two processes agree on who
 * fires an occurrence.
 *
 * <h2>Claim, dispatch, confirm — at-least-once</h2>
 *
 * <ol>
 *     <li>{@link #claim} takes a <em>lease</em>: a compare-and-set on {@link ScheduledTrigger#version()}
 *     that marks the trigger's due occurrences in flight until the lease expires. It succeeds for
 *     exactly one caller among any number holding the same version. {@code nextFireAt} does not move.</li>
 *     <li>The runner dispatches.</li>
 *     <li>{@link #confirm} advances {@code nextFireAt} and releases the lease.</li>
 * </ol>
 *
 * <p>A runner that dies between claim and confirm leaves the lease to expire; {@link #findDue} then
 * returns the trigger again with its {@link ScheduledTrigger#plannedAt()}, and the next runner replays
 * the same plan — the same occurrences, with the same {@link org.jmouse.scheduler.Occurrence#identity()}.
 * So an occurrence is never lost, and may be dispatched more than once: ⚠️ <strong>dispatchers must be
 * idempotent on the identity.</strong>
 *
 * <h2>Whose clock</h2>
 *
 * <p>A lease's expiry is set and checked against <em>the store's</em> notion of time — for a database,
 * the database's own current time — so the clocks of the machines running the runners never decide
 * whether a lease has expired. Schedules are still computed from the runner's {@link java.time.Clock}.
 *
 * <h2>History</h2>
 *
 * <p>{@link #record} keeps a bounded number of records per trigger — the implementation's limit —
 * dropping the oldest. A replayed plan records its occurrences again.
 *
 * <p>Implementations must be safe for concurrent use.
 */
public interface TriggerStore {

    /**
     * Writes a trigger's definition and next firing, inserting or replacing, increments its version and
     * drops any lease — a runner holding one will fail to confirm, and the occurrences are planned afresh.
     *
     * @param trigger    the definition
     * @param nextFireAt the next firing, or {@code null} when there is none
     * @return the stored state
     */
    ScheduledTrigger save(Trigger trigger, Instant nextFireAt);

    /**
     * @param id the trigger
     * @return the stored trigger, or empty
     */
    Optional<ScheduledTrigger> find(TriggerId id);

    /** Every stored trigger, ordered by identifier. */
    List<ScheduledTrigger> findAll();

    /**
     * Removes a trigger and its history.
     *
     * @param id the trigger
     * @return whether there was one
     */
    boolean remove(TriggerId id);

    /**
     * Enabled triggers whose next firing is at or before {@code now} and which hold no live lease,
     * earliest first. A trigger whose lease expired unconfirmed is included, with its
     * {@link ScheduledTrigger#plannedAt()}.
     *
     * @param now   the runner's present, compared with {@code nextFireAt}
     * @param limit the most to return
     * @return the due triggers
     */
    List<ScheduledTrigger> findDue(Instant now, int limit);

    /**
     * Takes a lease on a trigger's due occurrences — only if the stored version is still
     * {@code expectedVersion}.
     *
     * @param id              the trigger
     * @param expectedVersion the version the caller read
     * @param plannedAt       the instant the caller planned the occurrences from
     * @param leaseDuration   how long the lease lasts, measured on the store's clock
     * @return the lease for exactly one caller per version; empty when another runner (or a
     *         re-registration) got there first
     */
    Optional<TriggerLease> claim(TriggerId id, long expectedVersion, Instant plannedAt, Duration leaseDuration);

    /**
     * Confirms a lease's occurrences as dispatched: advances the trigger and releases the lease.
     *
     * @param lease      the lease
     * @param nextFireAt the next occurrence after the plan, or {@code null} when the schedule is exhausted
     * @return {@code false} when the lease was lost — it expired and another runner took the trigger over,
     *         or the trigger was re-registered — in which case the occurrences will be (or were)
     *         dispatched again
     */
    boolean confirm(TriggerLease lease, Instant nextFireAt);

    /**
     * Appends a history record, then trims the trigger's history to the store's limit.
     *
     * @param occurrenceRecord the record
     */
    void record(OccurrenceRecord occurrenceRecord);

    /**
     * @param id    the trigger
     * @param limit the most to return
     * @return the trigger's history, newest first
     */
    List<OccurrenceRecord> history(TriggerId id, int limit);
}
