package org.jmouse.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleTest {

    private static final Instant NOON = Instant.parse("2026-03-10T12:00:00Z");

    @Test
    void anIntervalIsAlignedToItsAnchor() {
        Schedule.Interval hourly = Schedule.every(Duration.ofHours(1));
        assertEquals(Instant.parse("2026-03-10T13:00:00Z"), hourly.nextAfter(NOON).orElseThrow());
        assertEquals(Instant.parse("2026-03-10T13:00:00Z"), hourly.nextAfter(NOON.plusSeconds(1)).orElseThrow());

        Schedule.Interval anchored = Schedule.every(Duration.ofMinutes(10), Instant.parse("2026-03-10T12:03:00Z"));
        assertEquals(Instant.parse("2026-03-10T12:03:00Z"), anchored.nextAfter(NOON).orElseThrow());
        assertEquals(Instant.parse("2026-03-10T12:13:00Z"),
                anchored.nextAfter(Instant.parse("2026-03-10T12:03:00Z")).orElseThrow());
    }

    @Test
    void anIntervalIsElapsedTimeAndIgnoresDaylightSaving() {
        // 2024-03-10 in New York loses an hour; an absolute interval does not care.
        Instant beforeJump = Instant.parse("2024-03-10T06:00:00Z");
        assertEquals(Instant.parse("2024-03-10T07:00:00Z"), Schedule.every(Duration.ofHours(1)).nextAfter(beforeJump).orElseThrow());
    }

    @Test
    void anIntervalIsAtLeastOneSecondInWholeMilliseconds() {
        assertThrows(IllegalArgumentException.class, () -> Schedule.every(Duration.ofMillis(999)));
        assertThrows(IllegalArgumentException.class, () -> Schedule.every(Duration.ofSeconds(1).plusNanos(1)));
    }

    @Test
    void aOneOffFiresOnceAndItsFirstFiringIsItselfEvenWhenPast() {
        Schedule.Once once = Schedule.once(NOON);
        assertEquals(Optional.of(NOON), once.nextAfter(NOON.minusSeconds(1)));
        assertEquals(Optional.empty(), once.nextAfter(NOON));
        assertEquals(Optional.of(NOON), once.firstFiring(NOON.plusSeconds(3600)));
    }

    @Test
    void cronSchedulesCompareByMeaning() {
        assertEquals(Schedule.cron("@daily", ZoneId.of("UTC")), Schedule.cron("0 0 * * *", ZoneId.of("UTC")));
    }
}
