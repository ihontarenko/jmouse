package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.GateVerdict;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerGate;
import org.jmouse.scheduler.spi.TriggerLease;
import org.jmouse.scheduler.spi.TriggerStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * The firing loop: one thread that, every {@link SchedulerSettings#pollInterval()} plus up to
 * {@link SchedulerSettings#maximumJitter()}, takes the due triggers from the store, claims each, and
 * fires what it claimed.
 *
 * <h2>One pass, one trigger</h2>
 *
 * <ol>
 *     <li>Plan: which occurrences are on time, which are late and what the misfire policy keeps of
 *     them, where the trigger resumes.</li>
 *     <li>Claim: take a lease with {@link TriggerStore#claim}. A runner that loses the claim does
 *     nothing more — another runner owns those occurrences.</li>
 *     <li>Record the dropped late occurrences as one {@link OccurrenceOutcome#SKIPPED} line.</li>
 *     <li>For each kept occurrence, oldest first: resolve the handler ({@link OccurrenceOutcome#FAILED}
 *     when none is registered), evaluate the gate ({@link OccurrenceOutcome#REFUSED}), dispatch
 *     ({@link OccurrenceOutcome#FIRED}, or {@link OccurrenceOutcome#FAILED} when the dispatcher throws).</li>
 *     <li>Confirm: {@link TriggerStore#confirm} advances the trigger and releases the lease.</li>
 * </ol>
 *
 * <p>⚠️ <strong>At-least-once.</strong> A runner that dies before confirming leaves its lease to expire,
 * and the next runner replays the same plan from {@link org.jmouse.scheduler.spi.ScheduledTrigger#plannedAt()}:
 * the same occurrences, with the same {@link Occurrence#identity()}. An occurrence is never lost; it may
 * be dispatched twice, so a {@link TriggerDispatcher} must be idempotent on the identity.
 *
 * <p>A failure in one trigger is logged and does not stop the pass or the loop.
 *
 * <p>{@link #pollOnce()} runs a single pass on the calling thread, which together with a
 * {@link ManualClock} is how this is tested — and how a product with its own loop can drive it.
 * {@link #start()} and {@link #stop()} (or {@link #close()}) run the loop on a daemon thread of its own.
 */
public final class SchedulerRunner implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerRunner.class);

    private final TriggerStore store;
    private final HandlerRegistry handlers;
    private final GateRegistry gates;
    private final TriggerDispatcher dispatcher;
    private final Clock clock;
    private final SchedulerSettings settings;

    private final Object lifecycleLock = new Object();
    private Thread loopThread;
    private CountDownLatch stopSignal;

    /**
     * @param store      where triggers live
     * @param handlers   the handlers occurrences resolve against
     * @param gates      the gates occurrences are evaluated by
     * @param dispatcher where fired occurrences go
     * @param clock      the present
     * @param settings   how to poll
     */
    public SchedulerRunner(TriggerStore store, HandlerRegistry handlers, GateRegistry gates,
                           TriggerDispatcher dispatcher, Clock clock, SchedulerSettings settings) {
        this.store = Objects.requireNonNull(store, "store");
        this.handlers = Objects.requireNonNull(handlers, "handlers");
        this.gates = Objects.requireNonNull(gates, "gates");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * Runs one pass on the calling thread.
     *
     * @return how many occurrences were dispatched
     */
    public int pollOnce() {
        Instant now = clock.instant();
        List<ScheduledTrigger> due = store.findDue(now, settings.batchSize());
        int dispatched = 0;

        for (ScheduledTrigger scheduled : due) {
            try {
                dispatched += fireDue(scheduled, now);
            } catch (RuntimeException exception) {
                LOGGER.error("jmouse-scheduler: trigger '{}' failed in this pass", scheduled.trigger().id(), exception);
            }
        }

        return dispatched;
    }

    /** Starts the loop on a daemon thread. Does nothing when it is already running. */
    public void start() {
        synchronized (lifecycleLock) {
            if (loopThread != null) {
                return;
            }

            CountDownLatch signal = new CountDownLatch(1);
            stopSignal = signal;
            loopThread = Thread.ofPlatform()
                    .name("jmouse-scheduler")
                    .daemon(true)
                    .start(() -> runLoop(signal));
        }

        LOGGER.info("jmouse-scheduler started: store={}, handlers={}, gates={}, poll={} + up to {} jitter, "
                        + "misfire threshold={}", store.getClass().getSimpleName(), handlers.keys(), gates.keys(),
                settings.pollInterval(), settings.maximumJitter(), settings.misfireThreshold());
    }

    /** Stops the loop and waits for the current pass to finish. Does nothing when it is not running. */
    public void stop() {
        Thread thread;

        synchronized (lifecycleLock) {
            if (loopThread == null) {
                return;
            }

            thread = loopThread;
            stopSignal.countDown();
            loopThread = null;
            stopSignal = null;
        }

        try {
            thread.join(TimeUnit.SECONDS.toMillis(30));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("jmouse-scheduler stopped");
    }

    /** Whether the loop is running. */
    public boolean running() {
        synchronized (lifecycleLock) {
            return loopThread != null;
        }
    }

    /** Same as {@link #stop()}, so a container closes the loop with the context. */
    @Override
    public void close() {
        stop();
    }

    private void runLoop(CountDownLatch signal) {
        try {
            while (!signal.await(nextDelayNanoseconds(), TimeUnit.NANOSECONDS)) {
                try {
                    pollOnce();
                } catch (RuntimeException exception) {
                    LOGGER.error("jmouse-scheduler: a pass failed; the loop continues", exception);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private long nextDelayNanoseconds() {
        long jitter = settings.maximumJitter().toNanos();
        long randomJitter = jitter == 0 ? 0 : ThreadLocalRandom.current().nextLong(jitter + 1);
        return settings.pollInterval().toNanos() + randomJitter;
    }

    private int fireDue(ScheduledTrigger scheduled, Instant now) {
        Trigger trigger = scheduled.trigger();

        // An expired, unconfirmed lease is replayed from the instant it was planned at, so this runner
        // dispatches exactly the occurrences the lost runner did — same scheduled times, same identities.
        Instant plannedAt = scheduled.inFlight() ? scheduled.plannedAt() : now;
        OccurrencePlan plan = OccurrencePlan.plan(trigger.schedule(), trigger.misfirePolicy(),
                scheduled.nextFireAt(), plannedAt, settings.misfireThreshold());
        Optional<TriggerLease> lease = store.claim(trigger.id(), scheduled.version(), plannedAt,
                settings.leaseDuration());

        if (lease.isEmpty()) {
            LOGGER.debug("jmouse-scheduler: trigger '{}' was claimed by another runner", trigger.id());
            return 0;
        }

        if (scheduled.inFlight()) {
            LOGGER.warn("jmouse-scheduler: trigger '{}' had a lease that expired unconfirmed; re-dispatching the "
                    + "occurrences planned at {}", trigger.id(), plannedAt);
        }

        int dispatched = dispatchPlan(trigger, plan, now);

        if (!store.confirm(lease.get(), plan.nextFireAt())) {
            LOGGER.warn("jmouse-scheduler: trigger '{}' lost its lease before confirming; its occurrences will be "
                    + "dispatched again by whichever runner holds it now", trigger.id());
        }

        return dispatched;
    }

    private int dispatchPlan(Trigger trigger, OccurrencePlan plan, Instant now) {

        if (plan.droppedCount() > 0) {
            store.record(new OccurrenceRecord(trigger.id(), plan.firstLate(), now, OccurrenceOutcome.SKIPPED,
                    plan.droppedCount(), describeMisfire(trigger, plan)));
        }

        int dispatched = 0;

        for (Instant scheduledAt : plan.lateToFire()) {
            dispatched += fire(trigger, scheduledAt, now, true) ? 1 : 0;
        }

        for (Instant scheduledAt : plan.onTime()) {
            dispatched += fire(trigger, scheduledAt, now, false) ? 1 : 0;
        }

        return dispatched;
    }

    private boolean fire(Trigger trigger, Instant scheduledAt, Instant now, boolean late) {
        Occurrence occurrence = new Occurrence(trigger.id(), trigger.handlerKey(), trigger.parameters(),
                scheduledAt, now, late);

        if (handlers.find(trigger.handlerKey()).isEmpty()) {
            record(occurrence, OccurrenceOutcome.FAILED,
                    "No trigger handler is registered under '" + trigger.handlerKey() + "' in this process.");
            return false;
        }

        Optional<String> refusal = trigger.gate().flatMap(gateKey -> evaluateGate(gateKey, occurrence));

        if (refusal.isPresent()) {
            record(occurrence, OccurrenceOutcome.REFUSED, refusal.get());
            return false;
        }

        try {
            dispatcher.dispatch(occurrence);
        } catch (RuntimeException exception) {
            LOGGER.warn("jmouse-scheduler: dispatch of {} failed", occurrence.identity(), exception);
            record(occurrence, OccurrenceOutcome.FAILED, "Dispatch failed: " + describe(exception));
            return false;
        }

        record(occurrence, OccurrenceOutcome.FIRED, "");
        return true;
    }

    /** The refusal reason, or empty when the gate passes. Fails closed. */
    private Optional<String> evaluateGate(String gateKey, Occurrence occurrence) {
        Optional<TriggerGate> gate = gates.find(gateKey);

        if (gate.isEmpty()) {
            return Optional.of("No trigger gate is registered under '" + gateKey + "' in this process.");
        }

        GateVerdict verdict;

        try {
            verdict = gate.get().evaluate(occurrence);
        } catch (RuntimeException exception) {
            LOGGER.warn("jmouse-scheduler: gate '{}' failed for {}", gateKey, occurrence.identity(), exception);
            return Optional.of("Gate '" + gateKey + "' failed: " + describe(exception));
        }

        return switch (verdict) {
            case GateVerdict.Pass pass -> Optional.empty();
            case GateVerdict.Refuse refuse -> Optional.of(refuse.reason());
            case null -> Optional.of("Gate '" + gateKey + "' returned no verdict.");
        };
    }

    private void record(Occurrence occurrence, OccurrenceOutcome outcome, String reason) {
        store.record(new OccurrenceRecord(occurrence.triggerId(), occurrence.scheduledAt(), occurrence.firedAt(),
                outcome, 1, reason));
    }

    private static String describeMisfire(Trigger trigger, OccurrencePlan plan) {
        String count = plan.lateCountIsLowerBound() ? "at least " + plan.lateCount() : String.valueOf(plan.lateCount());
        return "Of " + count + " late occurrence(s) between " + plan.firstLate() + " and " + plan.lastLate()
                + ", misfire policy " + trigger.misfirePolicy().name() + " dropped " + plan.droppedCount() + ".";
    }

    private static String describe(RuntimeException exception) {
        String message = exception.getMessage();
        String type = exception.getClass().getSimpleName();
        return message == null || message.isBlank() ? type : type + ": " + message;
    }
}
