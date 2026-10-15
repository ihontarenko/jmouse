package org.jmouse.scheduler;

import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.scheduler.jdbc.JdbcTriggerSchema;
import org.jmouse.scheduler.jdbc.JdbcTriggerStore;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerLease;
import org.jmouse.scheduler.spi.TriggerStore;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At-least-once: a runner that dies after dispatching but before confirming leaves a lease that
 * expires, and the next runner dispatches the same occurrences again — with the same identities.
 */
class LeaseRecoveryTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    /** What a JVM dying mid-pass looks like to the runner: nothing it catches. */
    private static final class ProcessDeath extends Error {
    }

    private static TriggerDispatcher dyingAfterDispatch(RecordingDispatcher queue) {
        return occurrence -> {
            queue.dispatch(occurrence);
            throw new ProcessDeath();
        };
    }

    private static SchedulerSettings settings(Duration leaseDuration) {
        return new SchedulerSettings(Duration.ofSeconds(1), Duration.ZERO, Duration.ofSeconds(30), 100, leaseDuration);
    }

    private static Trigger everyMinute() {
        return Trigger.builder("source-7.scan")
                .handler("scan-source")
                .schedule(Schedule.every(Duration.ofMinutes(1)))
                .build();
    }

    @Test
    void anExpiredLeaseIsReDispatchedWithTheSameIdentityOnTheStoresClock() {
        ManualClock runnerClock = new ManualClock(START);
        ManualClock storeClock = new ManualClock(START);
        InMemoryTriggerStore store = new InMemoryTriggerStore(10, storeClock);
        HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("scan-source"));
        RecordingDispatcher queue = new RecordingDispatcher();
        SchedulerSettings settings = settings(Duration.ofSeconds(30));
        new Scheduler(store, handlers, GateRegistry.empty(), runnerClock).register(everyMinute());

        SchedulerRunner dying = new SchedulerRunner(store, handlers, GateRegistry.empty(), dyingAfterDispatch(queue),
                runnerClock, settings);
        SchedulerRunner survivor = new SchedulerRunner(store, handlers, GateRegistry.empty(), queue, runnerClock, settings);

        runnerClock.advance(Duration.ofMinutes(1));
        assertThrows(ProcessDeath.class, dying::pollOnce);

        ScheduledTrigger inFlight = store.find(everyMinute().id()).orElseThrow();
        assertTrue(inFlight.inFlight());
        assertEquals(START.plus(Duration.ofMinutes(1)), inFlight.nextFireAt()); // not advanced: unconfirmed

        // The runners' clocks race ahead, but the lease is the store's: still live, nobody takes over.
        runnerClock.advance(Duration.ofSeconds(50));
        assertEquals(0, survivor.pollOnce());

        storeClock.advance(Duration.ofSeconds(31));
        assertEquals(1, survivor.pollOnce());

        List<Occurrence> dispatched = queue.occurrences();
        assertEquals(2, dispatched.size());
        assertEquals(dispatched.get(0).identity(), dispatched.get(1).identity());
        assertEquals(dispatched.get(0).scheduledAt(), dispatched.get(1).scheduledAt());
        assertFalse(dispatched.get(1).late(), "the replay plans from the original instant, so it is still on time");

        ScheduledTrigger confirmed = store.find(everyMinute().id()).orElseThrow();
        assertFalse(confirmed.inFlight());
        assertEquals(START.plus(Duration.ofMinutes(2)), confirmed.nextFireAt());
    }

    @Test
    void theReplayIsTheSamePlanEvenWhenTheOccurrenceWouldNowBeLate() {
        ManualClock runnerClock = new ManualClock(START);
        ManualClock storeClock = new ManualClock(START);
        InMemoryTriggerStore store = new InMemoryTriggerStore(10, storeClock);
        HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("scan-source"));
        RecordingDispatcher queue = new RecordingDispatcher();
        SchedulerSettings settings = settings(Duration.ofMinutes(5));
        new Scheduler(store, handlers, GateRegistry.empty(), runnerClock).register(everyMinute());

        runnerClock.advance(Duration.ofMinutes(1));
        assertThrows(ProcessDeath.class, new SchedulerRunner(store, handlers, GateRegistry.empty(),
                dyingAfterDispatch(queue), runnerClock, settings)::pollOnce);

        // Ten minutes later the original occurrence is far past the threshold; SKIP would drop it if the
        // survivor re-planned from its own present. It replays instead.
        runnerClock.advance(Duration.ofMinutes(10));
        storeClock.advance(Duration.ofMinutes(10));
        SchedulerRunner survivor = new SchedulerRunner(store, handlers, GateRegistry.empty(), queue, runnerClock, settings);
        assertEquals(1, survivor.pollOnce());
        assertEquals(queue.occurrences().get(0).identity(), queue.occurrences().get(1).identity());

        // The occurrences missed after the plan are now ordinary misfires, handled on the next pass.
        assertEquals(1, survivor.pollOnce());
        assertEquals(START.plus(Duration.ofMinutes(12)), store.find(everyMinute().id()).orElseThrow().nextFireAt());
    }

    @Test
    void aLiveLeaseKeepsOtherRunnersOutAndAReRegistrationDropsIt() {
        ManualClock clock = new ManualClock(START);
        TriggerStore store = new InMemoryTriggerStore(10, clock);
        ScheduledTrigger saved = store.save(everyMinute(), START);

        TriggerLease lease = store.claim(saved.trigger().id(), saved.version(), START, Duration.ofSeconds(30)).orElseThrow();
        assertTrue(store.findDue(START, 10).isEmpty());
        assertTrue(store.claim(saved.trigger().id(), lease.version(), START, Duration.ofSeconds(30)).isEmpty());

        store.save(everyMinute(), START); // re-registered while in flight
        assertFalse(store.confirm(lease, START.plusSeconds(60)));
        assertEquals(1, store.findDue(START, 10).size());
    }

    @Test
    void aJdbcLeaseExpiresOnTheDatabaseClockAndIsReDispatched() throws InterruptedException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:lease-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTriggerSchema.create(dataSource);
        // The runners' clock is fixed, far from the database's: it decides schedules, never leases.
        ManualClock runnerClock = new ManualClock(START);
        HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("scan-source"));
        RecordingDispatcher queue = new RecordingDispatcher();
        SchedulerSettings settings = settings(Duration.ofMillis(300));
        new Scheduler(new JdbcTriggerStore(dataSource, 10), handlers, GateRegistry.empty(), runnerClock)
                .register(everyMinute());
        runnerClock.advance(Duration.ofMinutes(1));

        assertThrows(ProcessDeath.class, new SchedulerRunner(new JdbcTriggerStore(dataSource, 10), handlers,
                GateRegistry.empty(), dyingAfterDispatch(queue), runnerClock, settings)::pollOnce);

        SchedulerRunner survivor = new SchedulerRunner(new JdbcTriggerStore(dataSource, 10), handlers,
                GateRegistry.empty(), queue, runnerClock, settings);
        assertEquals(0, survivor.pollOnce()); // the lease is live in the database

        Thread.sleep(500);
        assertEquals(1, survivor.pollOnce());
        assertEquals(2, queue.occurrences().size());
        assertEquals(queue.occurrences().get(0).identity(), queue.occurrences().get(1).identity());
        assertEquals(0, survivor.pollOnce()); // confirmed: nothing more is due
    }
}
