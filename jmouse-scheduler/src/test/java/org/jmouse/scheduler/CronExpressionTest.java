package org.jmouse.scheduler;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CronExpressionTest {

    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private static Instant next(String expression, ZoneId zone, Instant after) {
        return CronExpression.parse(expression).nextAfter(after, zone).orElseThrow();
    }

    private static Instant utc(String localDateTime) {
        return LocalDateTime.parse(localDateTime).toInstant(ZoneOffset.UTC);
    }

    @Nested
    class Grammar {

        @Test
        void everyMinuteFiresAtTheNextWholeMinute() {
            assertEquals(utc("2026-01-01T10:16:00"), next("* * * * *", UTC, utc("2026-01-01T10:15:30")));
        }

        @Test
        void theInstantItselfIsExcluded() {
            assertEquals(utc("2026-01-01T10:16:00"), next("* * * * *", UTC, utc("2026-01-01T10:15:00")));
        }

        @Test
        void rangesStepsListsAndNames() {
            CronExpression expression = CronExpression.parse("0,30 9-17/4 * JAN-MAR MON-FRI");
            Instant first = expression.nextAfter(utc("2026-01-03T00:00:00"), UTC).orElseThrow(); // a Saturday
            assertEquals(utc("2026-01-05T09:00:00"), first);
            Instant second = expression.nextAfter(first, UTC).orElseThrow();
            assertEquals(utc("2026-01-05T09:30:00"), second);
            Instant third = expression.nextAfter(second, UTC).orElseThrow();
            assertEquals(utc("2026-01-05T13:00:00"), third);
        }

        @Test
        void aStartWithAStepRunsToTheEndOfTheField() {
            assertEquals(utc("2026-01-01T00:50:00"), next("50/5 * * * *", UTC, utc("2026-01-01T00:46:00")));
            assertEquals(utc("2026-01-01T01:50:00"), next("50/5 * * * *", UTC, utc("2026-01-01T00:55:00")));
        }

        @Test
        void sixFieldsAddSeconds() {
            assertEquals(utc("2026-01-01T12:00:10"), next("*/10 * * * * *", UTC, utc("2026-01-01T12:00:05")));
        }

        @Test
        void sundayIsZeroSevenOrSun() {
            assertEquals(CronExpression.parse("0 0 * * 0"), CronExpression.parse("0 0 * * 7"));
            assertEquals(CronExpression.parse("0 0 * * 0"), CronExpression.parse("0 0 * * sun"));
            assertEquals(utc("2026-01-04T00:00:00"), next("0 0 * * 7", UTC, utc("2026-01-01T00:00:00")));
        }

        @Test
        void macrosExpand() {
            assertEquals(CronExpression.parse("0 0 * * *"), CronExpression.parse("@daily"));
            assertEquals(CronExpression.parse("0 * * * *"), CronExpression.parse("@HOURLY"));
            assertEquals("@daily", CronExpression.parse("@daily").source());
        }

        @Test
        void bothDayFieldsRestrictedMatchEither() {
            CronExpression expression = CronExpression.parse("0 0 13 * FRI");
            Instant first = expression.nextAfter(utc("2024-09-01T00:00:00"), UTC).orElseThrow();
            assertEquals(utc("2024-09-06T00:00:00"), first); // a Friday
            Instant second = expression.nextAfter(first, UTC).orElseThrow();
            assertEquals(utc("2024-09-13T00:00:00"), second); // the 13th, also a Friday
            Instant third = expression.nextAfter(second, UTC).orElseThrow();
            assertEquals(utc("2024-09-20T00:00:00"), third);
        }

        @Test
        void questionMarkIsUnrestricted() {
            assertEquals(CronExpression.parse("0 0 13 * *"), CronExpression.parse("0 0 13 * ?"));
        }

        @Test
        void theYearRollsOver() {
            assertEquals(utc("2025-01-01T00:00:00"), next("@yearly", UTC, utc("2024-06-15T00:00:00")));
            assertEquals(utc("2025-01-01T00:00:00"), next("0 0 * * *", UTC, utc("2024-12-31T00:00:00")));
        }

        @Test
        void differentSpellingsOfOneScheduleAreEqual() {
            assertEquals(CronExpression.parse("0-59 * * * *"), CronExpression.parse("* * * * *"));
            assertNotEquals(CronExpression.parse("0 0 * * *"), CronExpression.parse("0 1 * * *"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "", "* * * *", "* * * * * * *", "60 * * * *", "* 24 * * *", "* * 0 * *", "* * 32 * *",
                "* * * 13 *", "* * * * 8", "5-1 * * * *", "*/0 * * * *", "* * * * MON#2", "* * 15W * *",
                "* * * * 5L", "@reboot", "a * * * *", "1,,2 * * * *"
        })
        void refusesWhatItDoesNotSupport(String expression) {
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse(expression));
        }
    }

    @Nested
    class MonthEnds {

        @Test
        void theThirtyFirstSkipsShorterMonths() {
            CronExpression expression = CronExpression.parse("0 0 31 * *");
            Instant january = expression.nextAfter(utc("2025-01-01T00:00:00"), UTC).orElseThrow();
            assertEquals(utc("2025-01-31T00:00:00"), january);
            assertEquals(utc("2025-03-31T00:00:00"), expression.nextAfter(january, UTC).orElseThrow());
            assertEquals(utc("2025-05-31T00:00:00"),
                    expression.nextAfter(utc("2025-04-01T00:00:00"), UTC).orElseThrow());
        }

        @Test
        void theThirtiethSkipsFebruary() {
            assertEquals(utc("2025-03-30T00:00:00"), next("0 0 30 * *", UTC, utc("2025-02-01T00:00:00")));
        }

        @Test
        void lastDayFollowsTheMonthLength() {
            CronExpression expression = CronExpression.parse("0 0 L * *");
            assertEquals(utc("2024-01-31T00:00:00"), expression.nextAfter(utc("2024-01-15T00:00:00"), UTC).orElseThrow());
            assertEquals(utc("2024-02-29T00:00:00"), expression.nextAfter(utc("2024-02-01T00:00:00"), UTC).orElseThrow());
            assertEquals(utc("2025-02-28T00:00:00"), expression.nextAfter(utc("2025-02-01T00:00:00"), UTC).orElseThrow());
            assertEquals(utc("2025-04-30T00:00:00"), expression.nextAfter(utc("2025-04-01T00:00:00"), UTC).orElseThrow());
        }

        @Test
        void lastDayCombinesWithAList() {
            CronExpression expression = CronExpression.parse("0 0 15,L * *");
            Instant fifteenth = expression.nextAfter(utc("2025-02-01T00:00:00"), UTC).orElseThrow();
            assertEquals(utc("2025-02-15T00:00:00"), fifteenth);
            assertEquals(utc("2025-02-28T00:00:00"), expression.nextAfter(fifteenth, UTC).orElseThrow());
        }
    }

    @Nested
    class LeapYears {

        @Test
        void februaryTwentyNinthWaitsForTheNextLeapYear() {
            assertEquals(utc("2028-02-29T00:00:00"), next("0 0 29 2 *", UTC, utc("2025-03-01T00:00:00")));
        }

        @Test
        void twentyOneHundredIsNotALeapYear() {
            assertEquals(utc("2104-02-29T00:00:00"), next("0 0 29 2 *", UTC, utc("2096-03-01T00:00:00")));
        }

        @Test
        void aDateThatNeverExistsNeverFires() {
            assertEquals(Optional.empty(), CronExpression.parse("0 0 30 2 *").nextAfter(utc("2025-01-01T00:00:00"), UTC));
            assertEquals(Optional.empty(), CronExpression.parse("0 0 31 4,6,9,11 *").nextAfter(utc("2025-01-01T00:00:00"), UTC));
        }
    }

    @Nested
    class DaylightSaving {

        // America/New_York, 2024-03-10: 02:00 EST jumps to 03:00 EDT at 07:00Z.
        private static final Instant SPRING_FORWARD = utc("2024-03-10T07:00:00");

        // America/New_York, 2024-11-03: 02:00 EDT falls back to 01:00 EST at 06:00Z; 01:00-01:59 occurs twice.

        @Test
        void aTimeInTheGapFiresOnceWhenTheClocksJump() {
            CronExpression expression = CronExpression.parse("30 2 * * *");
            Instant gapDay = expression.nextAfter(utc("2024-03-09T17:00:00"), NEW_YORK).orElseThrow();
            assertEquals(SPRING_FORWARD, gapDay);
            assertEquals(utc("2024-03-11T06:30:00"), expression.nextAfter(gapDay, NEW_YORK).orElseThrow());
        }

        @Test
        void slotsInsideTheGapCollapseIntoOneFiring() {
            CronExpression expression = CronExpression.parse("*/15 * * * *");
            Instant last = expression.nextAfter(utc("2024-03-10T06:40:00"), NEW_YORK).orElseThrow(); // 01:45 EST
            assertEquals(utc("2024-03-10T06:45:00"), last);
            Instant jump = expression.nextAfter(last, NEW_YORK).orElseThrow();
            assertEquals(SPRING_FORWARD, jump);
            assertEquals(utc("2024-03-10T07:15:00"), expression.nextAfter(jump, NEW_YORK).orElseThrow()); // 03:15 EDT
        }

        @Test
        void hourlyAcrossTheGapLosesOnlyTheMissingHour() {
            CronExpression expression = CronExpression.parse("0 * * * *");
            Instant oneAm = utc("2024-03-10T06:00:00");
            Instant jump = expression.nextAfter(oneAm, NEW_YORK).orElseThrow();
            assertEquals(SPRING_FORWARD, jump); // 02:00 does not exist; 03:00 EDT is the same instant
            assertEquals(utc("2024-03-10T08:00:00"), expression.nextAfter(jump, NEW_YORK).orElseThrow()); // 04:00 EDT
        }

        @Test
        void aRepeatedTimeFiresOnceAtItsEarlierOccurrence() {
            CronExpression expression = CronExpression.parse("30 1 * * *");
            Instant first = expression.nextAfter(utc("2024-11-03T04:00:00"), NEW_YORK).orElseThrow();
            assertEquals(utc("2024-11-03T05:30:00"), first); // 01:30 EDT
            assertEquals(utc("2024-11-04T06:30:00"), expression.nextAfter(first, NEW_YORK).orElseThrow()); // next day, EST
        }

        @Test
        void searchingFromInsideTheSecondPassDoesNotFireTheRepeatedTimeAgain() {
            CronExpression expression = CronExpression.parse("30 1 * * *");
            Instant secondPass = utc("2024-11-03T06:15:00"); // 01:15 EST
            assertEquals(utc("2024-11-04T06:30:00"), expression.nextAfter(secondPass, NEW_YORK).orElseThrow());
        }

        @Test
        void hourlyAcrossTheOverlapSkipsTheRepeatedHour() {
            CronExpression expression = CronExpression.parse("0 * * * *");
            Instant oneAmDaylight = utc("2024-11-03T05:00:00");
            assertEquals(utc("2024-11-03T07:00:00"), expression.nextAfter(oneAmDaylight, NEW_YORK).orElseThrow()); // 02:00 EST
        }

        @Test
        void theZoneIsWhatTheFieldsMean() {
            Instant after = utc("2026-06-01T00:00:00");
            assertEquals(utc("2026-06-01T09:00:00"), next("0 9 * * *", UTC, after));
            assertEquals(utc("2026-06-01T06:00:00"), next("0 9 * * *", ZoneId.of("Europe/Kyiv"), after));
            assertTrue(next("0 9 * * *", NEW_YORK, after).isAfter(utc("2026-06-01T09:00:00")));
        }
    }
}
