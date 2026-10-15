package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.GateVerdict;
import org.jmouse.scheduler.spi.TriggerGate;
import org.jmouse.scheduler.spi.TriggerHandler;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Gates, failures, history, registration and the loop's lifecycle. */
class SchedulerRunnerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final SchedulerSettings SETTINGS =
            new SchedulerSettings(Duration.ofSeconds(1), Duration.ZERO, Duration.ofSeconds(30), 100, Duration.ofSeconds(30));

    private final ManualClock clock = new ManualClock(START);
    private final InMemoryTriggerStore store = new InMemoryTriggerStore(3);
    private final RecordingDispatcher dispatcher = new RecordingDispatcher();
    private final HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("vectorise-waiting"));

    /** The budget gate Raccoon would write: refuses when the spend would exceed what is left. */
    private static TriggerGate budgetGate(BigDecimal remaining, AtomicReference<Occurrence> seen) {
        return new TriggerGate() {
            @Override
            public String key() {
                return "budget-preflight";
            }

            @Override
            public GateVerdict evaluate(Occurrence occurrence) {
                seen.set(occurrence);
                BigDecimal budget = occurrence.parameters().requireDecimal("budget");
                return budget.compareTo(remaining) > 0
                        ? GateVerdict.refuse("Budget preflight refused: " + budget + " requested, " + remaining + " left.")
                        : GateVerdict.pass();
            }
        };
    }

    private Trigger vectoriseHourly(String budget) {
        return Trigger.builder("nightly-vectorise")
                .handler("vectorise-waiting")
                .parameters(Map.of("profile", "default", "maximumDocuments", 500, "budget", new BigDecimal(budget)))
                .schedule(Schedule.every(Duration.ofHours(1)))
                .gate("budget-preflight")
                .build();
    }

    @Test
    void aRefusedGateIsRecordedWithItsReasonAndNothingIsDispatched() {
        AtomicReference<Occurrence> seen = new AtomicReference<>();
        GateRegistry gates = GateRegistry.of(budgetGate(new BigDecimal("1.00"), seen));
        Scheduler scheduler = new Scheduler(store, handlers, gates, clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, gates, dispatcher, clock, SETTINGS);
        Trigger trigger = scheduler.register(vectoriseHourly("2.50")).trigger();

        clock.advance(Duration.ofHours(1));

        assertEquals(0, runner.pollOnce());
        assertTrue(dispatcher.occurrences().isEmpty());
        OccurrenceRecord refused = scheduler.history(trigger.id(), 10).getFirst();
        assertEquals(OccurrenceOutcome.REFUSED, refused.outcome());
        // Decimals are normalised on the way in, so 2.50 reads back as 2.5 — the value, not the spelling.
        assertEquals("Budget preflight refused: 2.5 requested, 1.00 left.", refused.reason());
        assertEquals(500L, seen.get().parameters().requireLong("maximumDocuments"));
        // The refused occurrence is spent: the trigger moves on rather than retrying it every pass.
        assertEquals(START.plus(Duration.ofHours(2)), scheduler.find(trigger.id()).orElseThrow().nextFireAt());
    }

    @Test
    void aPassingGateLetsTheOccurrenceThrough() {
        GateRegistry gates = GateRegistry.of(budgetGate(new BigDecimal("10"), new AtomicReference<>()));
        Scheduler scheduler = new Scheduler(store, handlers, gates, clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, gates, dispatcher, clock, SETTINGS);
        Trigger trigger = scheduler.register(vectoriseHourly("2.50")).trigger();

        clock.advance(Duration.ofHours(1));

        assertEquals(1, runner.pollOnce());
        assertEquals(OccurrenceOutcome.FIRED, scheduler.history(trigger.id(), 10).getFirst().outcome());
        assertEquals("nightly-vectorise@" + START.plus(Duration.ofHours(1)).toEpochMilli(),
                dispatcher.occurrences().getFirst().identity());
    }

    @Test
    void aGateThatThrowsRefuses() {
        TriggerGate broken = new TriggerGate() {
            @Override
            public String key() {
                return "budget-preflight";
            }

            @Override
            public GateVerdict evaluate(Occurrence occurrence) {
                throw new IllegalStateException("pricing table unavailable");
            }
        };
        GateRegistry gates = GateRegistry.of(broken);
        Scheduler scheduler = new Scheduler(store, handlers, gates, clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, gates, dispatcher, clock, SETTINGS);
        Trigger trigger = scheduler.register(vectoriseHourly("1")).trigger();
        clock.advance(Duration.ofHours(1));

        assertEquals(0, runner.pollOnce());
        OccurrenceRecord refused = scheduler.history(trigger.id(), 10).getFirst();
        assertEquals(OccurrenceOutcome.REFUSED, refused.outcome());
        assertTrue(refused.reason().contains("pricing table unavailable"), refused.reason());
    }

    @Test
    void aGateMissingFromTheFiringProcessRefuses() {
        GateRegistry gates = GateRegistry.of(budgetGate(BigDecimal.TEN, new AtomicReference<>()));
        Scheduler scheduler = new Scheduler(store, handlers, gates, clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, GateRegistry.empty(), dispatcher, clock, SETTINGS);
        Trigger trigger = scheduler.register(vectoriseHourly("1")).trigger();
        clock.advance(Duration.ofHours(1));

        assertEquals(0, runner.pollOnce());
        assertEquals(OccurrenceOutcome.REFUSED, scheduler.history(trigger.id(), 10).getFirst().outcome());
    }

    @Test
    void registeringWithAnUnknownGateOrHandlerIsRefused() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);

        assertThrows(IllegalArgumentException.class, () -> scheduler.register(vectoriseHourly("1")));
        assertThrows(IllegalArgumentException.class, () -> scheduler.register(Trigger.builder("scan")
                .handler("scan-source")
                .schedule(Schedule.every(Duration.ofHours(1)))
                .build()));
    }

    @Test
    void aHandlerRefusesMalformedParametersAtRegistration() {
        TriggerHandler strict = new TriggerHandler() {
            @Override
            public String key() {
                return "vectorise-waiting";
            }

            @Override
            public void validate(TriggerParameters parameters) {
                if (parameters.requireLong("maximumDocuments") < 1) {
                    throw new IllegalArgumentException("maximumDocuments must be positive");
                }
            }

            @Override
            public void handle(Occurrence occurrence) {
            }
        };
        Scheduler scheduler = new Scheduler(store, HandlerRegistry.of(strict), GateRegistry.empty(), clock);
        Trigger.Builder builder = Trigger.builder("vectorise")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofHours(1)));

        assertThrows(IllegalArgumentException.class, () -> scheduler.register(builder.build()));
        assertThrows(IllegalArgumentException.class,
                () -> scheduler.register(builder.parameters(Map.of("maximumDocuments", 0)).build()));
        scheduler.register(builder.parameters(Map.of("maximumDocuments", 10)).build());
    }

    @Test
    void aDispatcherFailureIsRecordedAndTheOccurrenceIsNotRetried() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, GateRegistry.empty(),
                occurrence -> {
                    throw new IllegalStateException("queue unavailable");
                }, clock, SETTINGS);
        Trigger trigger = scheduler.register(Trigger.builder("hourly")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofHours(1)))
                .build()).trigger();
        clock.advance(Duration.ofHours(1));

        assertEquals(0, runner.pollOnce());
        assertEquals(0, runner.pollOnce());
        List<OccurrenceRecord> history = scheduler.history(trigger.id(), 10);
        assertEquals(1, history.size());
        assertEquals(OccurrenceOutcome.FAILED, history.getFirst().outcome());
        assertTrue(history.getFirst().reason().contains("queue unavailable"));
    }

    @Test
    void aHandlerMissingFromTheFiringProcessFails() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        SchedulerRunner runner = new SchedulerRunner(store, HandlerRegistry.of(), GateRegistry.empty(), dispatcher,
                clock, SETTINGS);
        Trigger trigger = scheduler.register(Trigger.builder("hourly")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofHours(1)))
                .build()).trigger();
        clock.advance(Duration.ofHours(1));

        assertEquals(0, runner.pollOnce());
        assertEquals(OccurrenceOutcome.FAILED, scheduler.history(trigger.id(), 10).getFirst().outcome());
    }

    @Test
    void historyIsBoundedNewestFirst() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, GateRegistry.empty(), dispatcher, clock, SETTINGS);
        Trigger trigger = scheduler.register(Trigger.builder("minutely")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofMinutes(1)))
                .build()).trigger();

        for (int pass = 0; pass < 5; pass++) {
            clock.advance(Duration.ofMinutes(1));
            runner.pollOnce();
        }

        List<OccurrenceRecord> history = scheduler.history(trigger.id(), 10);
        assertEquals(3, history.size());
        assertEquals(START.plus(Duration.ofMinutes(5)), history.getFirst().scheduledAt());
        assertEquals(START.plus(Duration.ofMinutes(3)), history.getLast().scheduledAt());
    }

    @Test
    void reRegisteringAnUnchangedTriggerKeepsItsNextFiring() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        Trigger trigger = Trigger.builder("hourly")
                .handler("vectorise-waiting")
                .schedule(Schedule.cron("0 * * * *", java.time.ZoneOffset.UTC))
                .build();
        Instant first = scheduler.register(trigger).nextFireAt();

        clock.advance(Duration.ofHours(5)); // down for five hours, then the product starts again
        assertEquals(first, scheduler.register(trigger).nextFireAt());

        Trigger changed = Trigger.builder("hourly")
                .handler("vectorise-waiting")
                .schedule(Schedule.cron("30 * * * *", java.time.ZoneOffset.UTC))
                .build();
        assertEquals(START.plus(Duration.ofHours(5).plusMinutes(30)), scheduler.register(changed).nextFireAt());
    }

    @Test
    void aDisabledTriggerDoesNotFireAndResumesFromThePresent() {
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, GateRegistry.empty(), dispatcher, clock, SETTINGS);
        TriggerId id = scheduler.register(Trigger.builder("minutely")
                .handler("vectorise-waiting")
                .schedule(Schedule.every(Duration.ofMinutes(1)))
                .misfirePolicy(MisfirePolicy.catchUp(100))
                .build()).trigger().id();
        scheduler.disable(id);

        clock.advance(Duration.ofMinutes(30));
        assertEquals(0, runner.pollOnce());

        assertEquals(START.plus(Duration.ofMinutes(31)), scheduler.enable(id).nextFireAt());
        assertEquals(0, runner.pollOnce());
    }

    @Test
    void theLoopStartsFiresAndStops() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);
        SchedulerSettings fast = new SchedulerSettings(Duration.ofMillis(20), Duration.ofMillis(10),
                Duration.ofSeconds(5), 10, Duration.ofSeconds(30));
        Scheduler scheduler = new Scheduler(store, handlers, GateRegistry.empty(), clock);
        SchedulerRunner runner = new SchedulerRunner(store, handlers, GateRegistry.empty(),
                occurrence -> fired.countDown(), clock, fast);
        scheduler.register(Trigger.builder("soon")
                .handler("vectorise-waiting")
                .schedule(Schedule.once(START.plusSeconds(1)))
                .build());
        clock.advance(Duration.ofSeconds(1));

        try (runner) {
            runner.start();
            runner.start(); // idempotent
            assertTrue(runner.running());
            assertTrue(fired.await(5, TimeUnit.SECONDS));
        }

        assertFalse(runner.running());
    }

    @Test
    void settingsRefuseAThresholdOrdinaryPollingWouldCross() {
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerSettings(Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofSeconds(6), 10, Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerSettings(Duration.ofSeconds(5), Duration.ofSeconds(6), Duration.ofMinutes(1), 10, Duration.ofSeconds(30)));
        assertThrows(IllegalArgumentException.class,
                () -> new SchedulerSettings(Duration.ofSeconds(5), Duration.ZERO, Duration.ofMinutes(1), 10, Duration.ZERO));
    }

    @Test
    void parametersSurviveOnlyAsJsonValues() {
        TriggerParameters parameters = TriggerParameters.of(Map.of("count", 5, "ratio", 0.5, "whole", 5.0));
        assertEquals(5L, parameters.requireLong("count"));
        assertEquals(5L, parameters.requireLong("whole"));
        assertEquals(new BigDecimal("0.5"), parameters.requireDecimal("ratio"));
        assertThrows(IllegalArgumentException.class, () -> parameters.requireLong("ratio"));
        assertThrows(IllegalArgumentException.class, () -> parameters.requireText("count"));
        assertThrows(IllegalArgumentException.class, () -> parameters.requireText("absent"));
        assertThrows(IllegalArgumentException.class, () -> TriggerParameters.of(Map.of("when", Instant.EPOCH)));
    }
}
