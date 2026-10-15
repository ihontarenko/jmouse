package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every misfire policy against the same outage: a one-minute interval, fired once on time, then ten
 * minutes and forty seconds without a pass — ten late occurrences, none on time.
 */
class MisfirePolicyTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final SchedulerSettings SETTINGS =
            new SchedulerSettings(Duration.ofSeconds(1), Duration.ZERO, Duration.ofSeconds(30), 100, Duration.ofSeconds(30));

    private ManualClock clock;
    private InMemoryTriggerStore store;
    private RecordingDispatcher dispatcher;
    private Scheduler scheduler;
    private SchedulerRunner runner;

    @BeforeEach
    void setUp() {
        clock = new ManualClock(START);
        store = new InMemoryTriggerStore(100);
        dispatcher = new RecordingDispatcher();
        HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("vectorise-waiting"));
        scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        runner = new SchedulerRunner(store, handlers, GateRegistry.empty(), dispatcher, clock, SETTINGS);
    }

    private TriggerId registerEveryMinute(MisfirePolicy policy) {
        Trigger trigger = Trigger.builder("every-minute")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofMinutes(1)))
                .misfirePolicy(policy)
                .build();
        scheduler.register(trigger);
        return trigger.id();
    }

    private void fireOnceOnTimeThenGoDown(Duration downtime) {
        clock.advance(Duration.ofMinutes(1));
        assertEquals(1, runner.pollOnce());
        clock.advance(downtime);
    }

    private static Instant minute(int minutes) {
        return START.plus(Duration.ofMinutes(minutes));
    }

    private OccurrenceRecord latestSkipped(TriggerId id) {
        return scheduler.history(id, 100).stream()
                .filter(occurrenceRecord -> occurrenceRecord.outcome() == OccurrenceOutcome.SKIPPED)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void anOnTimeOccurrenceFiresWhateverThePolicy() {
        TriggerId id = registerEveryMinute(MisfirePolicy.skip());
        clock.advance(Duration.ofMinutes(1).plusSeconds(20));

        assertEquals(1, runner.pollOnce());
        assertEquals(List.of(minute(1)), dispatcher.scheduledTimes());
        assertFalse(dispatcher.occurrences().getFirst().late());
        assertEquals(minute(2), scheduler.find(id).orElseThrow().nextFireAt());
    }

    @Test
    void skipDropsEveryLateOccurrenceAndResumesInTheFuture() {
        TriggerId id = registerEveryMinute(MisfirePolicy.skip());
        fireOnceOnTimeThenGoDown(Duration.ofMinutes(10).plusSeconds(40));

        assertEquals(0, runner.pollOnce());
        assertEquals(List.of(minute(1)), dispatcher.scheduledTimes());

        OccurrenceRecord skipped = latestSkipped(id);
        assertEquals(10, skipped.count());
        assertEquals(minute(2), skipped.scheduledAt());
        assertTrue(skipped.reason().contains("SKIP"), skipped.reason());
        assertEquals(minute(12), scheduler.find(id).orElseThrow().nextFireAt());
    }

    @Test
    void fireOnceFiresTheMostRecentLateOccurrenceOnly() {
        TriggerId id = registerEveryMinute(MisfirePolicy.fireOnce());
        fireOnceOnTimeThenGoDown(Duration.ofMinutes(10).plusSeconds(40));

        assertEquals(1, runner.pollOnce());
        Occurrence fired = dispatcher.occurrences().getLast();
        assertEquals(minute(11), fired.scheduledAt());
        assertTrue(fired.late());
        assertEquals(9, latestSkipped(id).count());
        assertEquals(minute(12), scheduler.find(id).orElseThrow().nextFireAt());
    }

    @Test
    void fireOnceDoesNotAddALateFiringWhenAnOnTimeOneAlreadyCoversIt() {
        TriggerId id = registerEveryMinute(MisfirePolicy.fireOnce());
        fireOnceOnTimeThenGoDown(Duration.ofMinutes(10).plusSeconds(10)); // minute 11 is 10 s old: on time

        assertEquals(1, runner.pollOnce());
        Occurrence fired = dispatcher.occurrences().getLast();
        assertEquals(minute(11), fired.scheduledAt());
        assertFalse(fired.late());
        assertEquals(9, latestSkipped(id).count());
    }

    @Test
    void catchUpFiresTheMostRecentBoundedNumberOldestFirst() {
        TriggerId id = registerEveryMinute(MisfirePolicy.catchUp(3));
        fireOnceOnTimeThenGoDown(Duration.ofMinutes(10).plusSeconds(40));

        assertEquals(3, runner.pollOnce());
        assertEquals(List.of(minute(1), minute(9), minute(10), minute(11)), dispatcher.scheduledTimes());
        assertEquals(7, latestSkipped(id).count());
    }

    @Test
    void catchUpWithRoomForEverythingDropsNothing() {
        TriggerId id = registerEveryMinute(MisfirePolicy.catchUp(50));
        fireOnceOnTimeThenGoDown(Duration.ofMinutes(10).plusSeconds(40));

        assertEquals(10, runner.pollOnce());
        assertTrue(scheduler.history(id, 100).stream()
                .noneMatch(occurrenceRecord -> occurrenceRecord.outcome() == OccurrenceOutcome.SKIPPED));
    }

    @Test
    void catchUpIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> MisfirePolicy.catchUp(0));
        assertThrows(IllegalArgumentException.class, () -> MisfirePolicy.catchUp(MisfirePolicy.MAXIMUM_CATCH_UP + 1));
    }

    @Test
    void anEnormousOutageIsCountedToACeilingNotEnumeratedForever() {
        Trigger everySecond = Trigger.builder("every-second")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofSeconds(1)))
                .build();
        scheduler.register(everySecond);
        clock.advance(Duration.ofDays(2));

        // 172,800 late occurrences: the count stops at the ceiling, and the thirty seconds inside the
        // threshold still fire on time.
        assertEquals(31, runner.pollOnce());

        OccurrenceRecord skipped = latestSkipped(everySecond.id());
        assertEquals(OccurrencePlan.LATE_SCAN_CEILING, skipped.count());
        assertTrue(skipped.reason().contains("at least"), skipped.reason());
        ScheduledTrigger resumed = scheduler.find(everySecond.id()).orElseThrow();
        assertTrue(resumed.nextFireAt().isAfter(clock.instant()));
    }

    @Test
    void aOneOffRegisteredLateStillMeetsItsPolicy() {
        Trigger once = Trigger.builder("one-off")
                .handler("vectorise-waiting")
                .schedule(Schedule.once(START.minus(Duration.ofHours(1))))
                .misfirePolicy(MisfirePolicy.fireOnce())
                .build();
        scheduler.register(once);

        assertEquals(1, runner.pollOnce());
        assertTrue(dispatcher.occurrences().getFirst().late());
        assertEquals(null, scheduler.find(once.id()).orElseThrow().nextFireAt());

        clock.advance(Duration.ofDays(1));
        scheduler.register(once); // re-registered at the next start-up
        assertEquals(0, runner.pollOnce());
        assertEquals(1, dispatcher.occurrences().size());
    }

    @Test
    void aOneOffRegisteredLateWithSkipIsRecordedNotFired() {
        Trigger once = Trigger.builder("one-off")
                .handler("vectorise-waiting")
                .schedule(Schedule.once(START.minus(Duration.ofHours(1))))
                .build();
        scheduler.register(once);

        assertEquals(0, runner.pollOnce());
        assertEquals(1, latestSkipped(once.id()).count());
    }
}
