package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerHandler;
import org.jmouse.scheduler.spi.TriggerStore;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registering, changing and reading triggers. Firing is the {@link SchedulerRunner}'s job; this class
 * never dispatches anything, so a process that only administers triggers need not run a loop.
 *
 * <h2>Registering is idempotent</h2>
 *
 * <p>A product typically registers its triggers at every start. Re-registering a trigger with an
 * unchanged schedule keeps its stored next firing — otherwise a restart would quietly move every
 * trigger forward and no misfire would ever be detected. A changed schedule, or a trigger being
 * re-enabled, starts again from the present: re-enabling is a decision to resume, not to replay the
 * pause.
 */
public final class Scheduler {

    private final TriggerStore store;
    private final HandlerRegistry handlers;
    private final GateRegistry gates;
    private final Clock clock;

    /**
     * @param store    where triggers live
     * @param handlers the handlers a trigger may name
     * @param gates    the gates a trigger may name
     * @param clock    the present
     */
    public Scheduler(TriggerStore store, HandlerRegistry handlers, GateRegistry gates, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.gates = Objects.requireNonNull(gates, "gates");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Inserts or replaces a trigger.
     *
     * @param trigger the definition
     * @return the stored trigger
     * @throws IllegalArgumentException when the handler or gate is not registered, or the handler refuses
     *                                  the parameters
     */
    public ScheduledTrigger register(Trigger trigger) {
        Objects.requireNonNull(trigger, "trigger");

        TriggerHandler handler = handlers.require(trigger.handlerKey());
        handler.validate(trigger.parameters());

        trigger.gate().ifPresent(this::requireGate);

        Instant now = clock.instant();
        Optional<ScheduledTrigger> existing = store.find(trigger.id());

        if (existing.isPresent() && keepsNextFiring(existing.get().trigger(), trigger)) {
            // An exhausted one-off stays exhausted: re-registering it at start-up must not fire it again.
            return store.save(trigger, existing.get().nextFireAt());
        }

        return store.save(trigger, trigger.schedule().firstFiring(now).orElse(null));
    }

    /**
     * @param id the trigger
     * @return the trigger, now enabled; it resumes from the present
     * @throws IllegalArgumentException when there is no such trigger
     */
    public ScheduledTrigger enable(TriggerId id) {
        return register(require(id).trigger().withEnabled(true));
    }

    /**
     * @param id the trigger
     * @return the trigger, now disabled; its history is kept
     * @throws IllegalArgumentException when there is no such trigger
     */
    public ScheduledTrigger disable(TriggerId id) {
        return register(require(id).trigger().withEnabled(false));
    }

    /**
     * Removes a trigger and its history.
     *
     * @param id the trigger
     * @return whether there was one
     */
    public boolean remove(TriggerId id) {
        return store.remove(id);
    }

    /** @return the trigger, or empty */
    public Optional<ScheduledTrigger> find(TriggerId id) {
        return store.find(id);
    }

    /** Every trigger, ordered by identifier. */
    public List<ScheduledTrigger> triggers() {
        return store.findAll();
    }

    /**
     * @param id    the trigger
     * @param limit the most to return
     * @return its history, newest first
     */
    public List<OccurrenceRecord> history(TriggerId id, int limit) {
        return store.history(id, limit);
    }

    private ScheduledTrigger require(TriggerId id) {
        return store.find(id).orElseThrow(() -> new IllegalArgumentException("No trigger '" + id + "'."));
    }

    private void requireGate(String gateKey) {
        if (gates.find(gateKey).isEmpty()) {
            throw new IllegalArgumentException("No trigger gate is registered under '" + gateKey + "'; registered: "
                    + gates.keys() + ".");
        }
    }

    private static boolean keepsNextFiring(Trigger stored, Trigger replacement) {
        boolean resumed = !stored.enabled() && replacement.enabled();
        return !resumed && stored.schedule().equals(replacement.schedule());
    }
}
