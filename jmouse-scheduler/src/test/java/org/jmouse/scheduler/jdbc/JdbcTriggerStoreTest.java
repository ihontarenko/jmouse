package org.jmouse.scheduler.jdbc;

import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.scheduler.MisfirePolicy;
import org.jmouse.scheduler.OccurrenceOutcome;
import org.jmouse.scheduler.OccurrenceRecord;
import org.jmouse.scheduler.Schedule;
import org.jmouse.scheduler.Trigger;
import org.jmouse.scheduler.TriggerId;
import org.jmouse.scheduler.spi.ScheduledTrigger;
import org.jmouse.scheduler.spi.TriggerLease;
import org.jmouse.scheduler.spi.TriggerStoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTriggerStoreTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private JdbcTriggerStore store;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:store-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTriggerSchema.create(dataSource);
        JdbcTriggerSchema.create(dataSource); // every statement is IF NOT EXISTS
        store = new JdbcTriggerStore(dataSource, 3);
    }

    @Test
    void everyScheduleKindAndPolicyRoundTrips() {
        List<Trigger> triggers = List.of(
                Trigger.builder("cron")
                        .handler("vectorise-waiting")
                        .parameters(Map.of("profile", "default", "maximumDocuments", 500,
                                "budget", new BigDecimal("2.50"), "sources", List.of(1, 2), "dry", false,
                                "nested", Map.of("note", "ü")))
                        .schedule(Schedule.cron("0 3 * * MON-FRI", ZoneId.of("Europe/Kyiv")))
                        .misfirePolicy(MisfirePolicy.catchUp(5))
                        .gate("budget-preflight")
                        .build(),
                Trigger.builder("interval")
                        .handler("scan-source")
                        .schedule(Schedule.every(Duration.ofMinutes(15), NOW.plusMillis(123)))
                        .misfirePolicy(MisfirePolicy.fireOnce())
                        .enabled(false)
                        .build(),
                Trigger.builder("once")
                        .handler("scan-source")
                        .schedule(Schedule.once(NOW.plusSeconds(10)))
                        .build());

        for (Trigger trigger : triggers) {
            store.save(trigger, NOW);
            assertEquals(trigger, store.find(trigger.id()).orElseThrow().trigger());
        }

        assertEquals(List.of("cron", "interval", "once"),
                store.findAll().stream().map(scheduled -> scheduled.trigger().id().value()).toList());
    }

    @Test
    void claimLeasesAndConfirmAdvances() {
        Trigger trigger = Trigger.builder("hourly").handler("scan-source").schedule(Schedule.every(Duration.ofHours(1))).build();
        ScheduledTrigger first = store.save(trigger, NOW);
        ScheduledTrigger second = store.save(trigger, NOW);
        assertEquals(first.version() + 1, second.version());

        TriggerLease lease = store.claim(trigger.id(), second.version(), NOW, Duration.ofMinutes(5)).orElseThrow();
        assertTrue(store.claim(trigger.id(), second.version(), NOW, Duration.ofMinutes(5)).isEmpty());
        assertTrue(store.claim(trigger.id(), lease.version(), NOW, Duration.ofMinutes(5)).isEmpty()); // live lease
        ScheduledTrigger inFlight = store.find(trigger.id()).orElseThrow();
        assertTrue(inFlight.inFlight());
        assertEquals(NOW, inFlight.plannedAt());
        assertEquals(NOW, inFlight.nextFireAt());
        assertTrue(store.findDue(NOW, 10).isEmpty());

        assertTrue(store.confirm(lease, NOW.plusSeconds(3600)));
        assertFalse(store.confirm(lease, NOW.plusSeconds(7200)));
        ScheduledTrigger confirmed = store.find(trigger.id()).orElseThrow();
        assertFalse(confirmed.inFlight());
        assertEquals(NOW.plusSeconds(3600), confirmed.nextFireAt());

        TriggerLease last = store.claim(trigger.id(), confirmed.version(), NOW, Duration.ofMinutes(5)).orElseThrow();
        assertTrue(store.confirm(last, null));
        assertNull(store.find(trigger.id()).orElseThrow().nextFireAt());
    }

    @Test
    void findDueReturnsEnabledDueTriggersEarliestFirst() {
        Schedule schedule = Schedule.every(Duration.ofHours(1));
        store.save(Trigger.builder("later").handler("scan-source").schedule(schedule).build(), NOW.minusSeconds(10));
        store.save(Trigger.builder("earlier").handler("scan-source").schedule(schedule).build(), NOW.minusSeconds(20));
        store.save(Trigger.builder("future").handler("scan-source").schedule(schedule).build(), NOW.plusSeconds(1));
        store.save(Trigger.builder("disabled").handler("scan-source").schedule(schedule).enabled(false).build(), NOW);
        store.save(Trigger.builder("exhausted").handler("scan-source").schedule(schedule).build(), null);

        assertEquals(List.of("earlier", "later"),
                store.findDue(NOW, 10).stream().map(scheduled -> scheduled.trigger().id().value()).toList());
        assertEquals(1, store.findDue(NOW, 1).size());
    }

    @Test
    void historyIsTrimmedToTheLimitAndRemovedWithTheTrigger() {
        TriggerId id = TriggerId.of("hourly");
        store.save(Trigger.builder("hourly").handler("scan-source").schedule(Schedule.every(Duration.ofHours(1))).build(), NOW);

        for (int index = 0; index < 5; index++) {
            store.record(new OccurrenceRecord(id, NOW.plusSeconds(index), NOW, OccurrenceOutcome.FIRED, 1, ""));
        }

        store.record(new OccurrenceRecord(id, NOW.plusSeconds(5), NOW, OccurrenceOutcome.SKIPPED, 9, "nine dropped"));

        List<OccurrenceRecord> history = store.history(id, 10);
        assertEquals(3, history.size());
        assertEquals(OccurrenceOutcome.SKIPPED, history.getFirst().outcome());
        assertEquals(9, history.getFirst().count());
        assertEquals("nine dropped", history.getFirst().reason());
        assertEquals(NOW.plusSeconds(3), history.getLast().scheduledAt());

        assertTrue(store.remove(id));
        assertFalse(store.remove(id));
        assertTrue(store.history(id, 10).isEmpty());
    }

    @Test
    void dialectsAreDetectedFromTheProductName() {
        assertEquals(JdbcTriggerSchema.Dialect.MYSQL, JdbcTriggerSchema.detect("MySQL"));
        assertEquals(JdbcTriggerSchema.Dialect.MYSQL, JdbcTriggerSchema.detect("MariaDB"));
        assertEquals(JdbcTriggerSchema.Dialect.POSTGRESQL, JdbcTriggerSchema.detect("PostgreSQL"));
        assertEquals(JdbcTriggerSchema.Dialect.POSTGRESQL, JdbcTriggerSchema.detect("H2"));
        assertThrows(TriggerStoreException.class, () -> JdbcTriggerSchema.detect("Oracle"));
        assertEquals(2, JdbcTriggerSchema.statements(JdbcTriggerSchema.Dialect.MYSQL).size());
        assertEquals(4, JdbcTriggerSchema.statements(JdbcTriggerSchema.Dialect.POSTGRESQL).size());
    }
}
