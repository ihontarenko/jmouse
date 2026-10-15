package org.jmouse.scheduler;

import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.scheduler.jdbc.JdbcTriggerSchema;
import org.jmouse.scheduler.jdbc.JdbcTriggerStore;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerStore;
import org.jmouse.scheduler.store.InMemoryTriggerStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Several runners polling one store at the same instant, round after round: every occurrence must be
 * dispatched — and confirmed — exactly once. Against the in-memory store (threads), and against H2 through the JDBC
 * store with one store instance per runner — the shape of two JVMs over one database.
 */
class ClaimRaceTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final SchedulerSettings SETTINGS =
            new SchedulerSettings(Duration.ofSeconds(1), Duration.ZERO, Duration.ofSeconds(30), 1000, Duration.ofSeconds(30));

    @Test
    void inMemoryRunnersNeverFireOneOccurrenceTwice() throws Exception {
        InMemoryTriggerStore store = new InMemoryTriggerStore(10);
        race(() -> store, 8, 40, 25);
    }

    @Test
    void jdbcRunnersOverOneDatabaseNeverFireOneOccurrenceTwice() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:race-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTriggerSchema.create(dataSource);
        race(() -> new JdbcTriggerStore(dataSource, 10), 4, 20, 10);
    }

    private static void race(Supplier<TriggerStore> stores, int runnerCount, int triggerCount, int rounds)
            throws Exception {
        ManualClock clock = new ManualClock(START);
        HandlerRegistry handlers = HandlerRegistry.of(RecordingDispatcher.handler("scan-source"));
        Scheduler scheduler = new Scheduler(stores.get(), handlers, GateRegistry.empty(), clock);

        for (int index = 0; index < triggerCount; index++) {
            scheduler.register(Trigger.builder("source-" + index + ".scan")
                    .handler("scan-source")
                    .parameters(Map.of("sourceId", index))
                    .schedule(Schedule.every(Duration.ofMinutes(1)))
                    .build());
        }

        Map<String, AtomicInteger> dispatchesByIdentity = new ConcurrentHashMap<>();
        TriggerDispatcher counting = occurrence -> dispatchesByIdentity
                .computeIfAbsent(occurrence.identity(), identity -> new AtomicInteger())
                .incrementAndGet();
        List<SchedulerRunner> runners = new ArrayList<>();

        for (int index = 0; index < runnerCount; index++) {
            runners.add(new SchedulerRunner(stores.get(), handlers, GateRegistry.empty(), counting, clock, SETTINGS));
        }

        ExecutorService threads = Executors.newFixedThreadPool(runnerCount);
        CyclicBarrier together = new CyclicBarrier(runnerCount);

        try {
            for (int round = 0; round < rounds; round++) {
                clock.advance(Duration.ofMinutes(1));
                List<Future<Integer>> passes = new ArrayList<>();

                for (SchedulerRunner runner : runners) {
                    passes.add(threads.submit(() -> {
                        together.await();
                        return runner.pollOnce();
                    }));
                }

                int dispatchedThisRound = 0;

                for (Future<Integer> pass : passes) {
                    dispatchedThisRound += pass.get();
                }

                assertEquals(triggerCount, dispatchedThisRound, "round " + round);
                assertTrue(stores.get().findAll().stream().noneMatch(ScheduledTrigger::inFlight),
                        "every claim of round " + round + " was confirmed");
            }
        } finally {
            threads.shutdownNow();
        }

        assertEquals(triggerCount * rounds, dispatchesByIdentity.size());
        assertTrue(dispatchesByIdentity.values().stream().allMatch(count -> count.get() == 1),
                "an occurrence was dispatched more than once");
    }
}
